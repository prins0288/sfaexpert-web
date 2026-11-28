package in.opt.sfa.tenant.master.empdetail.service;

import in.opt.sfa.common.util.Strings;
import in.opt.sfa.security.UserContext;
import in.opt.sfa.storage.FileUploadService;
import in.opt.sfa.storage.StoredFile;
import in.opt.sfa.tenant.master.document.entity.DocumentMaster;
import in.opt.sfa.tenant.master.document.repository.DocumentMasterRepository;
import in.opt.sfa.tenant.master.empdetail.entity.EmpDetail;
import in.opt.sfa.tenant.master.empdetail.entity.EmpDocument;
import in.opt.sfa.tenant.master.empdetail.repository.EmpDetailRepository;
import in.opt.sfa.tenant.master.empdetail.repository.EmpDocumentRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.multipart.MultipartFile;

import java.security.MessageDigest;
import java.time.LocalDateTime;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Employee photo and documents from the Create Employee form, through the
 * global FileUploadService.
 *   photo     -> <company>/employeePhoto/ (+ thumbnail); emp_detail.photo_path is the
 *                same column My Profile uses, so the photo shows in the top-right
 *                avatar when that employee logs in
 *   documents -> <company>/employeeDocument/, one emp_document row each, type from document_master
 */
@Service
public class EmpFileService {

    public static final String PHOTO_MODULE = "employeePhoto";
    public static final String DOCUMENT_MODULE = "employeeDocument";

    private final EmpDetailRepository employees;
    private final EmpDocumentRepository documents;
    private final DocumentMasterRepository documentTypes;
    private final FileUploadService uploads;

    public EmpFileService(EmpDetailRepository employees, EmpDocumentRepository documents,
                          DocumentMasterRepository documentTypes, FileUploadService uploads) {
        this.employees = employees;
        this.documents = documents;
        this.documentTypes = documentTypes;
        this.uploads = uploads;
    }

    // ---- photo --------------------------------------------------------------------

    /** Set (file given) or remove (file null) the employee's photo. Images only. */
    @Transactional(transactionManager = "tenantTransactionManager")
    public Map<String, Object> setPhoto(String empId, MultipartFile file) throws Exception {
        EmpDetail e = employee(empId);
        String[] old = { e.getPhotoPath(), e.getPhotoThumbnailPath() };
        if (file != null && !file.isEmpty()) {
            StoredFile f = uploads.upload(PHOTO_MODULE, file, FileUploadService.Kind.IMAGE, true);
            afterRollback(() -> uploads.delete(f.path(), f.thumbnailPath()));
            e.setPhotoPath(f.path());
            e.setPhotoThumbnailPath(f.thumbnailPath());
        } else {
            e.setPhotoPath(null);
            e.setPhotoThumbnailPath(null);
        }
        employees.save(e);
        afterCommit(() -> uploads.delete(old));
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("photoPath", e.getPhotoPath());
        m.put("photoThumbnailPath", e.getPhotoThumbnailPath());
        return m;
    }

    // ---- documents ----------------------------------------------------------------

    @Transactional(transactionManager = "tenantTransactionManager", readOnly = true)
    public List<Map<String, Object>> list(String empId) {
        Map<Long, DocumentMaster> types = documentTypes.findAll().stream()
                .collect(Collectors.toMap(DocumentMaster::getId, Function.identity()));
        return documents.findByEmpIdAndActiveTrueOrderByUploadedAtDescIdDesc(empId).stream()
                .map(d -> toMap(d, types.get(d.getDocumentTypeId()))).toList();
    }

    /** Upload one document (PDF or image) of a type from Document Master. */
    @Transactional(transactionManager = "tenantTransactionManager")
    public Map<String, Object> upload(String empId, Long documentTypeId, String documentNo, String remarks,
                                      MultipartFile file) throws Exception {
        employee(empId);
        if (documentTypeId == null) throw new IllegalStateException("Choose the document type");
        DocumentMaster type = documentTypes.findById(documentTypeId)
                .orElseThrow(() -> new IllegalStateException("Unknown document type: " + documentTypeId));
        String no = Strings.isBlank(documentNo) ? null : documentNo.trim();
        String rem = Strings.isBlank(remarks) ? null : remarks.trim();
        if (no != null && no.length() > 50) throw new IllegalStateException("Document number can be at most 50 characters");
        if (rem != null && rem.length() > 255) throw new IllegalStateException("Remarks can be at most 255 characters");

        StoredFile f = uploads.upload(DOCUMENT_MODULE, file, FileUploadService.Kind.DOCUMENT, false);
        afterRollback(() -> uploads.delete(f.path(), f.thumbnailPath()));
        EmpDocument d = new EmpDocument();
        d.setEmpId(empId);
        d.setDocumentTypeId(type.getId());
        d.setDocumentNo(no);
        d.setRemarks(rem);
        String name = f.originalName() == null ? type.getDocumentName() : f.originalName();
        d.setOriginalName(name.length() > 255 ? name.substring(name.length() - 255) : name);
        d.setStorageKey(f.path());
        d.setThumbnailPath(f.thumbnailPath());
        d.setThumbnailType(f.thumbnailType());
        d.setMimeType(f.type());
        d.setFileSizeBytes(f.size());
        d.setSha256(sha256(file.getBytes()));
        d.setActive(true);
        d.setUploadedBy(currentUser());
        d.setUploadedAt(LocalDateTime.now());
        documents.save(d);
        return toMap(d, type);
    }

    /** Remove a document from the employee (row kept as inactive, file kept for audit). */
    @Transactional(transactionManager = "tenantTransactionManager")
    public void remove(String empId, Long documentId) {
        EmpDocument d = documents.findByIdAndEmpId(documentId, empId)
                .orElseThrow(() -> new IllegalStateException("Document not found: " + documentId));
        d.setActive(false);
        documents.save(d);
    }

    private static Map<String, Object> toMap(EmpDocument d, DocumentMaster type) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", d.getId());
        m.put("documentTypeId", d.getDocumentTypeId());
        m.put("documentTypeName", type == null ? null : type.getDocumentName());
        m.put("documentNo", d.getDocumentNo());
        m.put("remarks", d.getRemarks());
        m.put("fileName", d.getOriginalName());
        m.put("path", d.getStorageKey());
        m.put("thumbnailPath", d.getThumbnailPath());
        m.put("type", d.getMimeType());
        m.put("size", d.getFileSizeBytes());
        m.put("uploadedAt", d.getUploadedAt() == null ? null
                : d.getUploadedAt().format(java.time.format.DateTimeFormatter.ofPattern("dd-MM-yyyy HH:mm")));
        m.put("uploadedBy", d.getUploadedBy());
        return m;
    }

    private EmpDetail employee(String empId) {
        return employees.findById(empId).orElseThrow(() -> new IllegalStateException("Employee not found: " + empId));
    }

    private static String sha256(byte[] data) throws Exception {
        return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(data));
    }

    private static String currentUser() {
        UserContext.CurrentUser u = UserContext.get();
        String who = (u == null || u.empId() == null) ? "system" : u.empId();
        return who.length() > 30 ? who.substring(0, 30) : who;
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
}
