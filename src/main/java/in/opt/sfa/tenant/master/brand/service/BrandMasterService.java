package in.opt.sfa.tenant.master.brand.service;

import in.opt.sfa.common.util.Strings;
import in.opt.sfa.storage.FileUploadService;
import in.opt.sfa.storage.StoredFile;
import in.opt.sfa.tenant.master.brand.dto.BrandMasterDto;
import in.opt.sfa.tenant.master.brand.entity.BrandMaster;
import in.opt.sfa.tenant.master.brand.repository.BrandMasterRepository;
import in.opt.sfa.tenant.master.empdetail.service.EmpNameResolver;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.multipart.MultipartFile;

import java.util.*;
import java.util.stream.Stream;

/**
 * Brand master with an image. The image goes through the global
 * FileUploadService (module folder "brandMaster", thumbnail auto-created):
 * an uploaded file keeps the user's own file name (auto-generated names like
 * "WhatsApp Image …" are refused), and a brand with no uploaded image gets its
 * own copy of the default "no image" picture named after the brand
 * ("<brand name>.jpg", made unique). Files and the row stay consistent: a new
 * file is deleted again if the save fails, and the previous file is deleted
 * only after the row is committed.
 */
@Service
public class BrandMasterService {

    public static final String MODULE = "brandMaster";

    private final BrandMasterRepository brands;
    private final FileUploadService uploads;
    private final EmpNameResolver employeeNames;

    public BrandMasterService(BrandMasterRepository brands, FileUploadService uploads, EmpNameResolver employeeNames) {
        this.brands = brands;
        this.uploads = uploads;
        this.employeeNames = employeeNames;
    }

    /** All brands; a brand still without any image (older rows) gets its default picture here. */
    @Transactional(transactionManager = "tenantTransactionManager")
    public List<BrandMasterDto> list() throws Exception {
        Map<String, String> names = employeeNames.nameMap();
        List<BrandMaster> all = brands.findAllByOrderByBrandNameAsc();
        for (BrandMaster b : all) {
            if (b.getImagePath() == null) {
                setDefaultImage(b);
                brands.save(b);
            }
        }
        return all.stream().map(b -> withNames(toDto(b), names)).toList();
    }

    @Transactional(transactionManager = "tenantTransactionManager", readOnly = true)
    public BrandMasterDto get(Long id) {
        return withNames(toDto(find(id)), employeeNames.nameMap());
    }

    @Transactional(transactionManager = "tenantTransactionManager")
    public BrandMasterDto save(BrandMasterDto form, MultipartFile image) throws Exception {
        boolean isNew = form.getId() == null;
        BrandMaster b = isNew ? new BrandMaster() : find(form.getId());
        String oldName = b.getBrandName();
        String code = Strings.isBlank(form.getBrandCode()) ? null : form.getBrandCode().trim().toUpperCase();
        String name = Strings.isBlank(form.getBrandName()) ? null : form.getBrandName().trim();
        validate(code, name, isNew ? null : b.getId());

        b.setBrandCode(code);
        b.setBrandName(name);
        if (form.getStatus() != null) b.setStatus(form.getStatus());

        String[] oldFiles = { b.getImagePath(), b.getThumbnailPath() };
        boolean wasDefault = Boolean.TRUE.equals(b.getImageIsDefault());
        boolean renamed = !isNew && !name.equals(oldName);
        boolean replace = image != null && !image.isEmpty();
        boolean toDefault = !replace && (b.getImagePath() == null
                || (Boolean.TRUE.equals(form.getRemoveImage()) && !wasDefault)
                || (wasDefault && renamed));   // default picture follows the brand name
        if (replace) {
            // the user's own file name is kept; it may overwrite only this brand's own current file
            StoredFile f = uploads.uploadKeepingName(MODULE, image, FileUploadService.Kind.IMAGE,
                    Boolean.TRUE.equals(form.getCompressImage()), b.getImagePath());
            setImage(b, f, false);
        } else if (toDefault) {
            setDefaultImage(b);
        }
        brands.saveAndFlush(b);
        if (replace || toDefault) {
            // never delete a path the new image reuses (same file name uploaded again)
            Set<String> keep = new HashSet<>(Arrays.asList(b.getImagePath(), b.getThumbnailPath()));
            afterCommit(() -> uploads.delete(Arrays.stream(oldFiles).filter(p -> p != null && !keep.contains(p)).toArray(String[]::new)));
        }
        return withNames(toDto(b), employeeNames.nameMap());
    }

    /** Add Multiple (each brand gets the default picture): invalid / duplicate rows are skipped with the reason. */
    @Transactional(transactionManager = "tenantTransactionManager")
    public Map<String, Object> saveMultiple(List<BrandMasterDto> forms) throws Exception {
        int saved = 0, skipped = 0, row = 0;
        List<String> errors = new ArrayList<>();
        Set<String> seenCodes = new HashSet<>(), seenNames = new HashSet<>();
        for (BrandMasterDto f : forms) {
            row++;
            String code = Strings.isBlank(f.getBrandCode()) ? null : f.getBrandCode().trim().toUpperCase();
            String name = Strings.isBlank(f.getBrandName()) ? null : f.getBrandName().trim();
            try {
                if (code != null && !seenCodes.add(code)) throw new IllegalStateException("code " + code + " repeated in this list");
                if (name != null && !seenNames.add(name.toLowerCase())) throw new IllegalStateException("name " + name + " repeated in this list");
                validate(code, name, null);
            } catch (IllegalStateException e) {
                skipped++;
                errors.add("Row " + row + ": " + e.getMessage());
                continue;
            }
            BrandMaster b = new BrandMaster();
            b.setBrandCode(code);
            b.setBrandName(name);
            b.setStatus(true);
            setDefaultImage(b);
            brands.save(b);
            saved++;
        }
        Map<String, Object> res = new LinkedHashMap<>();
        res.put("saved", saved);
        res.put("skipped", skipped);
        res.put("errors", errors);
        return res;
    }

    @Transactional(transactionManager = "tenantTransactionManager")
    public BrandMasterDto updateStatus(Long id, boolean status) {
        BrandMaster b = find(id);
        b.setStatus(status);
        brands.save(b);
        return withNames(toDto(b), employeeNames.nameMap());
    }

    // ---- helpers ------------------------------------------------------------

    private void validate(String code, String name, Long selfId) {
        if (code == null) throw new IllegalStateException("Code is required");
        if (code.length() > 30) throw new IllegalStateException("Code can be at most 30 characters");
        if (name == null) throw new IllegalStateException("Name is required");
        if (name.length() > 150) throw new IllegalStateException("Name can be at most 150 characters");
        brands.findByBrandCode(code).filter(o -> !o.getId().equals(selfId))
                .ifPresent(o -> { throw new IllegalStateException("Code " + code + " already exists (" + o.getBrandName() + ")"); });
        brands.findByBrandName(name).filter(o -> !o.getId().equals(selfId))
                .ifPresent(o -> { throw new IllegalStateException("Brand name " + name + " already exists (" + o.getBrandCode() + ")"); });
    }

    /** Give the brand its own copy of the default picture, named after the brand. */
    private void setDefaultImage(BrandMaster b) throws Exception {
        setImage(b, uploads.storeDefaultImage(MODULE, b.getBrandName()), true);
    }

    private void setImage(BrandMaster b, StoredFile f, boolean isDefault) {
        // if the row does not commit, the new files must not stay behind
        // (except a path the brand already had: the same file name uploaded again)
        Set<String> had = new HashSet<>(Arrays.asList(b.getImagePath(), b.getThumbnailPath()));
        afterRollback(() -> uploads.delete(Stream.of(f.path(), f.thumbnailPath())
                .filter(p -> p != null && !had.contains(p)).toArray(String[]::new)));
        b.setImagePath(f.path());
        b.setImageType(f.type());
        b.setImageSize(f.size());
        b.setImageName(isDefault ? null : f.originalName());
        b.setThumbnailPath(f.thumbnailPath());
        b.setThumbnailType(f.thumbnailType());
        b.setImageIsDefault(isDefault);
    }

    private static void afterCommit(Runnable r) {
        if (!TransactionSynchronizationManager.isSynchronizationActive()) { r.run(); return; }
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override public void afterCommit() { r.run(); }
        });
    }

    private static void afterRollback(Runnable r) {
        if (!TransactionSynchronizationManager.isSynchronizationActive()) return;
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override public void afterCompletion(int status) { if (status != STATUS_COMMITTED) r.run(); }
        });
    }

    private BrandMaster find(Long id) {
        return brands.findById(id).orElseThrow(() -> new IllegalStateException("Brand not found: " + id));
    }

    private BrandMasterDto withNames(BrandMasterDto d, Map<String, String> names) {
        d.setCreatedBy(employeeNames.resolve(names, d.getCreatedBy()));
        d.setUpdatedBy(employeeNames.resolve(names, d.getUpdatedBy()));
        return d;
    }

    private static BrandMasterDto toDto(BrandMaster b) {
        BrandMasterDto d = new BrandMasterDto();
        d.setId(b.getId());
        d.setBrandCode(b.getBrandCode());
        d.setBrandName(b.getBrandName());
        d.setStatus(b.getStatus());
        d.setImagePath(b.getImagePath());
        d.setImageType(b.getImageType());
        d.setImageSize(b.getImageSize());
        d.setImageName(b.getImageName());
        d.setThumbnailPath(b.getThumbnailPath());
        d.setThumbnailType(b.getThumbnailType());
        d.setImageIsDefault(b.getImageIsDefault());
        d.setCreatedAt(b.getCreatedAt());
        d.setCreatedBy(b.getCreatedBy());
        d.setUpdatedAt(b.getUpdatedAt());
        d.setUpdatedBy(b.getUpdatedBy());
        return d;
    }
}
