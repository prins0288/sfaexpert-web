package in.opt.sfa.tenant.master.item.service;

import in.opt.sfa.common.util.ExcelUtil;
import in.opt.sfa.common.util.Strings;
import in.opt.sfa.storage.FileUploadService;
import in.opt.sfa.storage.StoredFile;
import in.opt.sfa.tenant.master.brand.entity.BrandMaster;
import in.opt.sfa.tenant.master.brand.repository.BrandMasterRepository;
import in.opt.sfa.tenant.master.division.entity.Division;
import in.opt.sfa.tenant.master.division.repository.DivisionRepository;
import in.opt.sfa.tenant.master.empdetail.service.EmpNameResolver;
import in.opt.sfa.tenant.master.item.dto.ItemCompositionDto;
import in.opt.sfa.tenant.master.item.dto.ItemMasterDto;
import in.opt.sfa.tenant.master.item.dto.ItemRateDto;
import in.opt.sfa.tenant.master.item.dto.SampleCreateRequest;
import in.opt.sfa.tenant.master.item.entity.ItemComposition;
import in.opt.sfa.tenant.master.item.entity.ItemMaster;
import in.opt.sfa.tenant.master.item.entity.ItemRate;
import in.opt.sfa.tenant.master.item.repository.ItemCompositionRepository;
import in.opt.sfa.tenant.master.item.repository.ItemMasterRepository;
import in.opt.sfa.tenant.master.item.repository.ItemRateRepository;
import in.opt.sfa.tenant.master.itemtype.entity.ItemTypeMaster;
import in.opt.sfa.tenant.master.itemtype.repository.ItemTypeMasterRepository;
import in.opt.sfa.tenant.master.state.entity.State;
import in.opt.sfa.tenant.master.state.repository.StateRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.ResolverStyle;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Item master: one item (item_code unique) with many rates — state-wise
 * (null state = All India) and date-wise — and many salts typed by the user.
 *
 * Rate rules: a rate starts on effective_from and runs to effective_to (blank
 * = still current). Two rates of one item + state may not overlap; a current
 * rate is closed automatically the day before a newer one for the same state
 * starts. MRP >= PTR >= PTS, nothing negative, discount 0-100. For the item
 * type PRODUCT, PTS / PTR / MRP are required; Sample / Gift / Input need none.
 *
 * Item code of a new item is generated from its type: Product PD01, PD02 ...,
 * Gift GP01, Input IP01, Sample SP01 (other types: first 2 letters of the type
 * code). Numbers grow past 99 as PD100. A code typed by the user is used as
 * it is (must be unique); codes can also be changed on edit.
 */
@Service
public class ItemMasterService {

    public static final String MODULE = "itemMaster";
    public static final String PRODUCT_TYPE = "PRODUCT";

    private final ItemMasterRepository items;
    private final ItemRateRepository rates;
    private final ItemCompositionRepository compositions;
    private final ItemTypeMasterRepository types;
    private final DivisionRepository divisions;
    private final BrandMasterRepository brands;
    private final StateRepository states;
    private final FileUploadService uploads;
    private final EmpNameResolver employeeNames;

    public ItemMasterService(ItemMasterRepository items, ItemRateRepository rates, ItemCompositionRepository compositions,
                             ItemTypeMasterRepository types, DivisionRepository divisions, BrandMasterRepository brands,
                             StateRepository states, FileUploadService uploads, EmpNameResolver employeeNames) {
        this.items = items;
        this.rates = rates;
        this.compositions = compositions;
        this.types = types;
        this.divisions = divisions;
        this.brands = brands;
        this.states = states;
        this.uploads = uploads;
        this.employeeNames = employeeNames;
    }

    // ---- read ------------------------------------------------------------------

    /** All items with rate count and today's rate (All India first). Rates / composition lines are not included. */
    @Transactional(transactionManager = "tenantTransactionManager", readOnly = true)
    public List<ItemMasterDto> list() {
        Names n = names();
        Map<Long, List<ItemRate>> byItem = rates.findAllByOrderByItemIdAscEffectiveFromAsc().stream()
                .collect(Collectors.groupingBy(ItemRate::getItemId));
        LocalDate today = LocalDate.now();
        return items.findAllByOrderByItemNameAsc().stream().map(i -> {
            ItemMasterDto d = toDto(i, n);
            List<ItemRate> rs = byItem.getOrDefault(i.getId(), List.of());
            d.setRateCount(rs.size());
            rs.stream().filter(r -> Boolean.TRUE.equals(r.getStatus()) && validOn(r, today))
                    .min(Comparator.comparing((ItemRate r) -> r.getStateId() != null)
                            .thenComparing(ItemRate::getEffectiveFrom, Comparator.reverseOrder()))
                    .ifPresent(r -> d.setCurrentRate(toDto(r, n)));
            return d;
        }).toList();
    }

    /** One item with all its rates and composition lines. */
    @Transactional(transactionManager = "tenantTransactionManager", readOnly = true)
    public ItemMasterDto get(Long id) {
        return fullDto(find(id), names());
    }

    /** Salt names already used in any item — suggestions while typing. */
    @Transactional(transactionManager = "tenantTransactionManager", readOnly = true)
    public List<String> moleculeNames() {
        return compositions.findDistinctMoleculeNames();
    }

    /** The rate of an item for a state on a date: the state's own rate, else the All-India rate. */
    @Transactional(transactionManager = "tenantTransactionManager", readOnly = true)
    public ItemRateDto rateOn(Long itemId, Long stateId, LocalDate onDate) {
        List<ItemRate> found = rates.findValid(itemId, stateId, onDate == null ? LocalDate.now() : onDate);
        return found.isEmpty() ? null : toDto(found.get(0), names());
    }

    // ---- item code ---------------------------------------------------------------

    /** Prefix of generated item codes per item type code. */
    private static final Map<String, String> CODE_PREFIX = Map.of("PRODUCT", "PD", "GIFT", "GP", "INPUT", "IP", "SAMPLE", "SP");

    static String codePrefix(ItemTypeMaster t) {
        String code = t.getItemTypeCode() == null ? "" : t.getItemTypeCode().toUpperCase(Locale.ROOT);
        String p = CODE_PREFIX.get(code);
        if (p != null) return p;
        String letters = code.replaceAll("[^A-Z0-9]", "");
        return letters.length() >= 2 ? letters.substring(0, 2) : (letters + "IT").substring(0, 2);
    }

    /** The code the next new item of this type will get (preview for the form; taken for real on save). */
    @Transactional(transactionManager = "tenantTransactionManager", readOnly = true)
    public String nextCode(Long itemTypeId) {
        ItemTypeMaster t = types.findById(itemTypeId).orElseThrow(() -> new IllegalStateException("Unknown item type: " + itemTypeId));
        return formatCode(codePrefix(t));
    }

    /** Generate a new code inside the saving transaction (type row locked, so no two saves get the same). */
    private String generateCode(Long itemTypeId, Names n) {
        if (itemTypeId == null) throw new IllegalStateException("Item type is required");
        ItemTypeMaster t = n.typesById.get(itemTypeId);
        if (t == null) throw new IllegalStateException("Unknown item type: " + itemTypeId);
        items.lockItemType(itemTypeId);
        return formatCode(codePrefix(t));
    }

    private String formatCode(String prefix) {
        Long max = items.maxCodeNumber(prefix.length() + 1, "^" + prefix + "[0-9]+$");
        long next = (max == null ? 0 : max) + 1;
        return prefix + (next < 10 ? "0" + next : String.valueOf(next));
    }

    // ---- save (form) -------------------------------------------------------------

    @Transactional(transactionManager = "tenantTransactionManager")
    public ItemMasterDto save(ItemMasterDto form, MultipartFile image) throws Exception {
        boolean isNew = form.getId() == null;
        ItemMaster item = isNew ? new ItemMaster() : find(form.getId());
        if (!isNew && form.getVersion() != null && !form.getVersion().equals(item.getVersion())) {
            throw new IllegalStateException("This item was changed by someone else meanwhile — close the form, reload and try again.");
        }
        Names n = names();
        // new item without a code: generated from the type; a typed code is used as it is
        if (isNew && Strings.isBlank(form.getItemCode())) form.setItemCode(generateCode(form.getItemTypeId(), n));
        applyHeader(item, form, n, true);
        validateUnique(item);

        String[] oldFiles = { item.getImagePath(), item.getThumbnailPath() };
        boolean replace = image != null && !image.isEmpty();
        boolean remove = !replace && Boolean.TRUE.equals(form.getRemoveImage()) && item.getImagePath() != null;
        if (replace) {
            StoredFile f = uploads.uploadKeepingName(MODULE, image, FileUploadService.Kind.IMAGE,
                    Boolean.TRUE.equals(form.getCompressImage()), item.getImagePath());
            Set<String> had = new HashSet<>(Arrays.asList(oldFiles));
            afterRollback(() -> uploads.delete(Arrays.stream(new String[]{ f.path(), f.thumbnailPath() })
                    .filter(p -> p != null && !had.contains(p)).toArray(String[]::new)));
            item.setImagePath(f.path());
            item.setImageType(f.type());
            item.setImageSize(f.size());
            item.setImageName(f.originalName());
            item.setThumbnailPath(f.thumbnailPath());
            item.setThumbnailType(f.thumbnailType());
        } else if (remove) {
            item.setImagePath(null);
            item.setImageType(null);
            item.setImageSize(null);
            item.setImageName(null);
            item.setThumbnailPath(null);
            item.setThumbnailType(null);
        }

        items.saveAndFlush(item);
        if (form.getCompositions() != null) replaceCompositions(item, cleanCompositions(form.getCompositions()));
        if (form.getRates() != null) replaceRates(item, form.getRates(), isProduct(item, n), n);
        items.saveAndFlush(item);

        if (replace || remove) {
            Set<String> keep = new HashSet<>(Arrays.asList(item.getImagePath(), item.getThumbnailPath()));
            afterCommit(() -> uploads.delete(Arrays.stream(oldFiles).filter(p -> p != null && !keep.contains(p)).toArray(String[]::new)));
        }
        return fullDto(item, n);
    }

    // ---- Create Multiple Sample ------------------------------------------------------

    public static final String SAMPLE_TYPE = "SAMPLE";

    /**
     * Active Product items for the Create Multiple Sample page: pack size, today's
     * All-India PTS, and the code of the sample already made from it (if any).
     */
    @Transactional(transactionManager = "tenantTransactionManager", readOnly = true)
    public List<Map<String, Object>> sampleCandidates() {
        Names n = names();
        List<ItemMaster> all = items.findAllByOrderByItemNameAsc();
        Map<String, ItemMaster> sampleByMain = samplesByMainCode(all, n);
        Map<Long, List<ItemRate>> ratesByItem = rates.findAllByOrderByItemIdAscEffectiveFromAsc().stream()
                .collect(Collectors.groupingBy(ItemRate::getItemId));
        LocalDate today = LocalDate.now();
        List<Map<String, Object>> out = new ArrayList<>();
        for (ItemMaster i : all) {
            if (!Boolean.TRUE.equals(i.getStatus()) || !isProduct(i, n)) continue;
            BigDecimal pts = ratesByItem.getOrDefault(i.getId(), List.of()).stream()
                    .filter(r -> Boolean.TRUE.equals(r.getStatus()) && validOn(r, today))
                    .min(Comparator.comparing((ItemRate r) -> r.getStateId() != null)
                            .thenComparing(ItemRate::getEffectiveFrom, Comparator.reverseOrder()))
                    .map(ItemRate::getPts).orElse(null);
            ItemMaster sample = sampleByMain.get(i.getItemCode().toUpperCase(Locale.ROOT));
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("id", i.getId());
            m.put("itemCode", i.getItemCode());
            m.put("itemName", i.getItemName());
            m.put("packSize", i.getSamplePackSize() != null ? i.getSamplePackSize() : i.getPackSize());
            m.put("pts", pts);
            m.put("sampleCode", sample == null ? null : sample.getItemCode());
            out.add(m);
        }
        return out;
    }

    /**
     * One Sample item per ticked product: type Sample, code SP01 ..., item_code_main
     * = the product's item_code, details and composition copied from the product,
     * pack size + PTS from the page, a rate for each chosen state (none = All India)
     * from today. A product that already has a sample gets NO second one — only the
     * rates of chosen states its sample does not have yet are added.
     */
    @Transactional(transactionManager = "tenantTransactionManager")
    public Map<String, Object> createSamples(SampleCreateRequest req) {
        if (req == null || req.getItems() == null || req.getItems().isEmpty()) throw new IllegalStateException("Tick at least one product");
        Names n = names();
        ItemTypeMaster sampleType = n.typesById.values().stream().filter(t -> SAMPLE_TYPE.equalsIgnoreCase(t.getItemTypeCode()))
                .findFirst().orElseThrow(() -> new IllegalStateException("Item type SAMPLE not found — add it in Item Type Master (code SAMPLE)"));
        List<Long> stateIds = req.getStateIds() == null ? List.of() : req.getStateIds().stream().filter(Objects::nonNull).distinct().toList();
        for (Long s : stateIds) if (!n.statesById.containsKey(s)) throw new IllegalStateException("Unknown state: " + s);
        List<Long> rateStates = stateIds.isEmpty() ? Collections.singletonList(null) : stateIds;   // null = All India
        Map<String, ItemMaster> sampleByMain = samplesByMainCode(items.findAllByOrderByItemNameAsc(), n);
        LocalDate today = LocalDate.now();

        List<String> created = new ArrayList<>(), updated = new ArrayList<>(), skipped = new ArrayList<>();
        Set<Long> seen = new HashSet<>();
        for (SampleCreateRequest.Line line : req.getItems()) {
            if (line == null || line.getProductId() == null || !seen.add(line.getProductId())) continue;
            ItemMaster product = items.findById(line.getProductId()).orElse(null);
            if (product == null) { skipped.add("Item " + line.getProductId() + ": not found"); continue; }
            String label = product.getItemName() + " (" + product.getItemCode() + ")";
            try {
                if (!isProduct(product, n)) throw new IllegalStateException("is not a Product");
                String pack = trim(line.getPackSize());
                if (pack != null && pack.length() > 50) throw new IllegalStateException("pack size can be at most 50 characters");
                BigDecimal pts = line.getPts();
                if (pts != null && pts.signum() < 0) throw new IllegalStateException("PTS cannot be negative");
                if (pts != null && pts.stripTrailingZeros().scale() > 2) throw new IllegalStateException("PTS can have at most 2 decimals");

                ItemMaster sample = sampleByMain.get(product.getItemCode().toUpperCase(Locale.ROOT));
                boolean isNew = sample == null;
                if (isNew) {
                    sample = new ItemMaster();
                    sample.setItemTypeId(sampleType.getId());
                    sample.setItemCode(generateCode(sampleType.getId(), n));
                    String name = product.getItemName() + " (Sample)";
                    sample.setItemName(name.length() > 200 ? name.substring(0, 200) : name);
                    sample.setShortName(product.getShortName());
                    sample.setBrandId(product.getBrandId());
                    sample.setDivisionId(product.getDivisionId());
                    sample.setItemClass(product.getItemClass());
                    sample.setManufacturer(product.getManufacturer());
                    sample.setUom(product.getUom());
                    sample.setHsnCode(product.getHsnCode());
                    sample.setGstRate(product.getGstRate());
                    sample.setItemCodeMain(product.getItemCode());   // link back to the product
                    sample.setPackSize(pack);
                    sample.setStatus(true);
                    items.saveAndFlush(sample);
                    List<ItemCompositionDto> comp = compositions.findByItemIdOrderBySortOrderAscIdAsc(product.getId()).stream().map(c -> {
                        ItemCompositionDto x = new ItemCompositionDto();
                        x.setMoleculeName(c.getMoleculeName());
                        x.setStrength(c.getStrength());
                        x.setStrengthUnit(c.getStrengthUnit());
                        x.setPerQty(c.getPerQty());
                        return x;
                    }).toList();
                    if (!comp.isEmpty()) replaceCompositions(sample, comp);
                    sampleByMain.put(product.getItemCode().toUpperCase(Locale.ROOT), sample);
                } else if (pack != null && !pack.equals(sample.getPackSize())) {
                    sample.setPackSize(pack);
                }

                // add a rate only for chosen states the sample has no rate for yet
                List<ItemRateDto> wanted = new ArrayList<>(rates.findByItemIdOrderByEffectiveFromAscIdAsc(sample.getId())
                        .stream().map(r -> toDto(r, n)).toList());
                Set<Long> has = wanted.stream().map(r -> r.getStateId() == null ? 0L : r.getStateId()).collect(Collectors.toSet());
                List<String> added = new ArrayList<>();
                for (Long st : rateStates) {
                    if (has.contains(st == null ? 0L : st)) continue;
                    ItemRateDto r = new ItemRateDto();
                    r.setStateId(st);
                    r.setEffectiveFrom(today);
                    r.setPts(pts);
                    r.setSampleRate(pts);
                    wanted.add(r);
                    added.add(stateName(st, n));
                }
                if (!added.isEmpty()) replaceRates(sample, wanted, false, n);
                items.saveAndFlush(sample);

                if (isNew) created.add(sample.getItemCode() + " = " + label + (added.isEmpty() ? "" : " · rate: " + String.join(", ", added)));
                else if (!added.isEmpty()) updated.add(sample.getItemCode() + " (already the sample of " + label + ") · rate added: " + String.join(", ", added));
                else skipped.add(label + ": sample " + sample.getItemCode() + " already exists with these states");
            } catch (IllegalStateException e) {
                skipped.add(label + ": " + e.getMessage());
            }
        }
        Map<String, Object> res = new LinkedHashMap<>();
        res.put("created", created);
        res.put("updated", updated);
        res.put("skipped", skipped);
        return res;
    }

    /** Sample items by the product code in their item_code_main (upper case). */
    private static Map<String, ItemMaster> samplesByMainCode(List<ItemMaster> all, Names n) {
        Map<String, ItemMaster> m = new HashMap<>();
        for (ItemMaster i : all) {
            ItemTypeMaster t = n.typesById.get(i.getItemTypeId());
            if (t != null && SAMPLE_TYPE.equalsIgnoreCase(t.getItemTypeCode()) && i.getItemCodeMain() != null) {
                m.putIfAbsent(i.getItemCodeMain().toUpperCase(Locale.ROOT), i);
            }
        }
        return m;
    }

    @Transactional(transactionManager = "tenantTransactionManager")
    public ItemMasterDto updateStatus(Long id, boolean status) {
        ItemMaster i = find(id);
        i.setStatus(status);
        items.save(i);
        return toDto(i, names());
    }

    // ---- bulk upload (3 sheets) ----------------------------------------------------

    public static final List<String> ITEM_COLUMNS = List.of("item_code", "item_name", "item_type", "short_name", "division",
            "brand", "item_class", "manufacturer", "pack_size", "sample_pack_size", "uom", "hsn_code", "gst_rate",
            "item_code_main", "status");
    public static final List<String> RATE_COLUMNS = List.of("item_code", "state", "effective_from", "effective_to",
            "pts", "ptr", "mrp", "purchase_rate", "sample_rate", "discount_pct", "tax_included");
    public static final List<String> COMPOSITION_COLUMNS = List.of("item_code", "molecule_name", "strength", "unit", "per_qty");

    public byte[] template() throws Exception {
        return ExcelUtil.template(List.of(
                new ExcelUtil.SheetSpec("Items", ITEM_COLUMNS, List.of(
                        List.of("DOLO650", "Dolo 650", "Product", "Dolo", "", "", "Analgesic", "Micro Labs", "15 tab",
                                "2 tab", "strip", "30049099", "12", "", "Y"),
                        List.of("DOLOCOLD", "Dolo Cold", "Product", "", "", "", "", "Micro Labs", "10 tab", "", "strip", "30049099", "12", "", "Y"))),
                new ExcelUtil.SheetSpec("Rates", RATE_COLUMNS, List.of(
                        List.of("DOLO650", "", "2026-04-01", "", "24.50", "27.00", "33.60", "", "", "", "N"),
                        List.of("DOLO650", "Bihar", "2026-04-01", "", "24.00", "26.50", "33.60", "", "", "", "N"))),
                new ExcelUtil.SheetSpec("Composition", COMPOSITION_COLUMNS, List.of(
                        List.of("DOLO650", "Paracetamol", "650", "mg", "per tablet"),
                        List.of("DOLOCOLD", "Paracetamol", "500", "mg", "per tablet"),
                        List.of("DOLOCOLD", "Phenylephrine", "10", "mg", "per tablet"),
                        List.of("DOLOCOLD", "Cetirizine", "5", "mg", "per tablet"))),
                new ExcelUtil.SheetSpec("Help", List.of("Notes"), List.of(
                        List.of("Items: one row per item. item_code blank = new item, code generated from the type (Product PD01, Gift GP01, Input IP01, Sample SP01)."),
                        List.of("Items: an existing item_code updates that item (blank cell = keep stored value); a new code you type is used as it is."),
                        List.of("Rates / Composition of a new item need its code: upload Items first, then Rates / Composition with the generated codes."),
                        List.of("item_type, division, brand, state: code or name from the masters. state blank = All India."),
                        List.of("Rates: one row per item x state x effective_from. Same item + state + effective_from again = update that rate."),
                        List.of("Rates: a current rate is closed automatically the day before a newer one for the same state starts."),
                        List.of("Rates for type Product need pts, ptr, mrp (mrp >= ptr >= pts). Dates: 2026-04-01 or 01-04-2026."),
                        List.of("Composition: the lines of an item in this sheet REPLACE its stored composition. Order = row order."),
                        List.of("Each sheet is optional: e.g. upload only Rates to change prices.")))));
    }

    /** Upload Items / Rates / Composition sheets (each optional). Bad rows are skipped with the reason. */
    @Transactional(transactionManager = "tenantTransactionManager")
    public Map<String, Object> upload(MultipartFile file) throws Exception {
        if (file == null || file.isEmpty()) throw new IllegalStateException("Please choose an Excel file");
        Map<String, List<Map<String, String>>> sheets;
        try (var in = file.getInputStream()) {
            sheets = ExcelUtil.readAll(in);
        } catch (Exception e) {
            throw new IllegalStateException("Could not read the file — use the .xlsx template");
        }
        List<Map<String, String>> itemRows = sheet(sheets, "items");
        List<Map<String, String>> rateRows = sheet(sheets, "rates");
        List<Map<String, String>> compRows = sheet(sheets, "composition");
        if (itemRows == null && rateRows == null && compRows == null) {
            throw new IllegalStateException("No Items / Rates / Composition sheet found — download the template");
        }

        Names n = names();
        List<String> errors = new ArrayList<>();
        int created = 0, updated = 0, rateRowsSaved = 0, compItems = 0, skipped = 0;
        Map<String, ItemMaster> byCode = new HashMap<>();
        List<String> generated = new ArrayList<>();

        // ---- Items ----
        int r = 1;
        for (Map<String, String> raw : itemRows == null ? List.<Map<String, String>>of() : itemRows) {
            r++;
            Cells c = new Cells(raw);
            String code = upper(c.get("item_code"));
            try {
                ItemMaster item = code == null ? null : byCode.containsKey(code) ? byCode.get(code) : items.findByItemCode(code).orElse(null);
                boolean isNew = item == null;
                if (isNew) item = new ItemMaster();
                if (code == null) {   // blank item_code = new item with a generated code
                    if (!c.has("item_type")) throw new IllegalStateException("item_type is required (the item code is made from it)");
                    code = generateCode(n.typeId(c.get("item_type")), n);
                    generated.add("Items row " + r + ": " + code + " = " + Objects.toString(c.get("item_name"), ""));
                }
                ItemMasterDto f = new ItemMasterDto();
                f.setItemCode(code);
                f.setItemName(c.get("item_name"));
                f.setShortName(c.get("short_name"));
                f.setItemTypeId(c.has("item_type") ? n.typeId(c.get("item_type")) : null);
                f.setDivisionId(c.has("division") ? n.divisionId(c.get("division")) : null);
                f.setBrandId(c.has("brand") ? n.brandId(c.get("brand")) : null);
                f.setItemClass(c.get("item_class"));
                f.setManufacturer(c.get("manufacturer"));
                f.setPackSize(c.get("pack_size"));
                f.setSamplePackSize(c.get("sample_pack_size"));
                f.setUom(c.get("uom"));
                f.setHsnCode(c.get("hsn_code"));
                f.setGstRate(c.decimal("gst_rate"));
                f.setItemCodeMain(c.get("item_code_main"));
                f.setStatus(c.yesNo("status"));
                applyHeader(item, f, n, isNew);   // update: blank cell keeps the stored value
                validateUnique(item);
                items.saveAndFlush(item);
                byCode.put(code, item);
                if (isNew) created++; else updated++;
            } catch (IllegalStateException e) {
                skipped++;
                errors.add("Items row " + r + (code == null ? "" : " (" + code + ")") + ": " + e.getMessage());
            }
        }

        // ---- Rates: validate each row, then merge per item with what is stored ----
        Map<ItemMaster, List<Object[]>> rateByItem = new LinkedHashMap<>();   // item -> [row number, dto]
        r = 1;
        for (Map<String, String> raw : rateRows == null ? List.<Map<String, String>>of() : rateRows) {
            r++;
            Cells c = new Cells(raw);
            String code = upper(c.get("item_code"));
            try {
                ItemMaster item = existingItem(code, byCode);
                ItemRateDto d = new ItemRateDto();
                d.setStateId(c.has("state") ? n.stateId(c.get("state")) : null);
                d.setEffectiveFrom(c.date("effective_from"));
                if (d.getEffectiveFrom() == null) throw new IllegalStateException("effective_from is required");
                d.setEffectiveTo(c.date("effective_to"));
                d.setPts(c.decimal("pts"));
                d.setPtr(c.decimal("ptr"));
                d.setMrp(c.decimal("mrp"));
                d.setPurchaseRate(c.decimal("purchase_rate"));
                d.setSampleRate(c.decimal("sample_rate"));
                d.setDiscountPct(c.decimal("discount_pct"));
                d.setTaxIncluded(Boolean.TRUE.equals(c.yesNo("tax_included")));
                checkRate(d, isProduct(item, n), n);
                rateByItem.computeIfAbsent(item, k -> new ArrayList<>()).add(new Object[]{ r, d });
            } catch (IllegalStateException e) {
                skipped++;
                errors.add("Rates row " + r + (code == null ? "" : " (" + code + ")") + ": " + e.getMessage());
            }
        }
        for (var e : rateByItem.entrySet()) {
            ItemMaster item = e.getKey();
            // stored rates, with the uploaded rows replacing the same state + effective_from
            List<ItemRateDto> merged = new ArrayList<>(rates.findByItemIdOrderByEffectiveFromAscIdAsc(item.getId())
                    .stream().map(x -> toDto(x, n)).toList());
            for (Object[] row : e.getValue()) {
                ItemRateDto d = (ItemRateDto) row[1];
                Optional<ItemRateDto> same = merged.stream().filter(m -> Objects.equals(m.getStateId(), d.getStateId())
                        && m.getEffectiveFrom().equals(d.getEffectiveFrom())).findFirst();
                same.ifPresent(m -> { d.setId(m.getId()); d.setStatus(m.getStatus()); merged.remove(m); });
                merged.add(d);
            }
            try {
                replaceRates(item, merged, isProduct(item, n), n);
                rateRowsSaved += e.getValue().size();
            } catch (IllegalStateException ex) {
                skipped += e.getValue().size();
                errors.add("Rates of " + item.getItemCode() + " (rows " + e.getValue().stream().map(x -> String.valueOf(x[0]))
                        .collect(Collectors.joining(", ")) + "): " + ex.getMessage());
            }
        }

        // ---- Composition: the sheet's lines replace an item's composition ----
        Map<ItemMaster, List<Object[]>> compByItem = new LinkedHashMap<>();
        r = 1;
        for (Map<String, String> raw : compRows == null ? List.<Map<String, String>>of() : compRows) {
            r++;
            Cells c = new Cells(raw);
            String code = upper(c.get("item_code"));
            try {
                ItemMaster item = existingItem(code, byCode);
                ItemCompositionDto d = new ItemCompositionDto();
                d.setMoleculeName(c.get("molecule_name"));
                if (d.getMoleculeName() == null) throw new IllegalStateException("molecule_name is required");
                d.setStrength(c.decimal("strength"));
                d.setStrengthUnit(c.get("unit"));
                d.setPerQty(c.get("per_qty"));
                compByItem.computeIfAbsent(item, k -> new ArrayList<>()).add(new Object[]{ r, d });
            } catch (IllegalStateException e) {
                skipped++;
                errors.add("Composition row " + r + (code == null ? "" : " (" + code + ")") + ": " + e.getMessage());
            }
        }
        for (var e : compByItem.entrySet()) {
            try {
                replaceCompositions(e.getKey(), cleanCompositions(e.getValue().stream().map(x -> (ItemCompositionDto) x[1]).toList()));
                items.saveAndFlush(e.getKey());
                compItems++;
            } catch (IllegalStateException ex) {
                skipped += e.getValue().size();
                errors.add("Composition of " + e.getKey().getItemCode() + " (rows " + e.getValue().stream().map(x -> String.valueOf(x[0]))
                        .collect(Collectors.joining(", ")) + "): " + ex.getMessage());
            }
        }

        Map<String, Object> res = new LinkedHashMap<>();
        res.put("created", created);
        res.put("updated", updated);
        res.put("rates", rateRowsSaved);
        res.put("compositionItems", compItems);
        res.put("generatedCodes", generated);
        res.put("skipped", skipped);
        res.put("errors", errors);
        return res;
    }

    // ---- header ---------------------------------------------------------------------

    /**
     * Copy the item's own fields from the form. full = form save / new item:
     * blank clears an optional field; otherwise (bulk update) blank keeps it.
     */
    private void applyHeader(ItemMaster i, ItemMasterDto f, Names n, boolean full) {
        String code = upper(f.getItemCode());
        if (code != null || full) {
            if (code == null) throw new IllegalStateException("Item code is required");
            if (code.length() > 50) throw new IllegalStateException("Item code can be at most 50 characters");
            if (!code.matches("[A-Z0-9][A-Z0-9 ._/-]*")) {
                throw new IllegalStateException("Item code may use letters, digits, space and . _ / - only");
            }
            i.setItemCode(code);
        }
        String name = trim(f.getItemName());
        if (name != null || full) {
            if (name == null) throw new IllegalStateException("Item name is required");
            if (name.length() > 200) throw new IllegalStateException("Item name can be at most 200 characters");
            i.setItemName(name);
        }
        if (f.getItemTypeId() != null || full) {
            if (f.getItemTypeId() == null) throw new IllegalStateException("Item type is required");
            ItemTypeMaster t = n.typesById.get(f.getItemTypeId());
            if (t == null) throw new IllegalStateException("Unknown item type: " + f.getItemTypeId());
            if (!Boolean.TRUE.equals(t.getIsActive()) && !f.getItemTypeId().equals(i.getItemTypeId())) {
                throw new IllegalStateException("Item type " + t.getItemTypeName() + " is inactive");
            }
            i.setItemTypeId(f.getItemTypeId());
        }
        if (f.getDivisionId() != null || full) {
            if (f.getDivisionId() != null && !n.divisionsById.containsKey(f.getDivisionId())) {
                throw new IllegalStateException("Unknown division: " + f.getDivisionId());
            }
            i.setDivisionId(f.getDivisionId());
        }
        if (f.getBrandId() != null || full) {
            if (f.getBrandId() != null && !n.brandsById.containsKey(f.getBrandId())) {
                throw new IllegalStateException("Unknown brand: " + f.getBrandId());
            }
            i.setBrandId(f.getBrandId());
        }
        set(f.getShortName(), full, 100, "Short name", i::setShortName);
        set(f.getItemClass(), full, 100, "Class", i::setItemClass);
        set(f.getManufacturer(), full, 150, "Manufacturer", i::setManufacturer);
        set(f.getPackSize(), full, 50, "Pack size", i::setPackSize);
        set(f.getSamplePackSize(), full, 50, "Sample pack size", i::setSamplePackSize);
        set(f.getUom(), full, 20, "Unit", i::setUom);
        set(f.getItemCodeMain(), full, 100, "Main item code", i::setItemCodeMain);
        String hsn = trim(f.getHsnCode());
        if (hsn != null && !hsn.matches("[0-9]{4,8}")) throw new IllegalStateException("HSN code must be 4 to 8 digits");
        if (hsn != null || full) i.setHsnCode(hsn);
        BigDecimal gst = f.getGstRate();
        if (gst != null && (gst.signum() < 0 || gst.compareTo(BigDecimal.valueOf(100)) > 0)) {
            throw new IllegalStateException("GST % must be between 0 and 100");
        }
        if (gst != null && gst.stripTrailingZeros().scale() > 2) throw new IllegalStateException("GST % can have at most 2 decimals");
        if (gst != null || full) i.setGstRate(gst);
        if (f.getStatus() != null) i.setStatus(f.getStatus());
    }

    private void validateUnique(ItemMaster i) {
        items.findByItemCode(i.getItemCode()).filter(o -> !o.getId().equals(i.getId()))
                .ifPresent(o -> { throw new IllegalStateException("Item code " + i.getItemCode() + " already exists (" + o.getItemName() + ")"); });
    }

    private static void set(String value, boolean full, int max, String label, java.util.function.Consumer<String> setter) {
        String v = trim(value);
        if (v != null && v.length() > max) throw new IllegalStateException(label + " can be at most " + max + " characters");
        if (v != null || full) setter.accept(v);
    }

    // ---- composition ------------------------------------------------------------------

    /** Drop empty lines, trim, and check names / strengths; one salt only once per item. */
    private static List<ItemCompositionDto> cleanCompositions(List<ItemCompositionDto> lines) {
        List<ItemCompositionDto> out = new ArrayList<>();
        Set<String> seen = new HashSet<>();
        int no = 0;
        for (ItemCompositionDto l : lines) {
            no++;
            String name = trim(l.getMoleculeName());
            String unit = trim(l.getStrengthUnit());
            String per = trim(l.getPerQty());
            if (name == null && l.getStrength() == null && unit == null && per == null) continue;
            String at = "Composition line " + no + ": ";
            if (name == null) throw new IllegalStateException(at + "salt / molecule name is required");
            name = name.replaceAll("\\s+", " ");
            if (name.length() > 150) throw new IllegalStateException(at + "name can be at most 150 characters");
            if (!seen.add(name.toLowerCase(Locale.ROOT))) throw new IllegalStateException(at + name + " is listed twice");
            if (l.getStrength() != null && l.getStrength().signum() <= 0) throw new IllegalStateException(at + "strength must be more than 0");
            if (l.getStrength() != null && l.getStrength().stripTrailingZeros().scale() > 3) {
                throw new IllegalStateException(at + "strength can have at most 3 decimals");
            }
            if (l.getStrength() != null && l.getStrength().compareTo(new BigDecimal("999999999")) > 0) {
                throw new IllegalStateException(at + "strength is too large");
            }
            if (unit != null && unit.length() > 20) throw new IllegalStateException(at + "unit can be at most 20 characters");
            if (per != null && per.length() > 30) throw new IllegalStateException(at + "'per' can be at most 30 characters");
            ItemCompositionDto c = new ItemCompositionDto();
            c.setMoleculeName(name);
            c.setStrength(l.getStrength());
            c.setStrengthUnit(unit);
            c.setPerQty(per);
            out.add(c);
        }
        if (out.size() > 50) throw new IllegalStateException("At most 50 composition lines per item");
        return out;
    }

    private void replaceCompositions(ItemMaster item, List<ItemCompositionDto> lines) {
        compositions.deleteByItemId(item.getId());
        compositions.flush();
        int order = 0;
        for (ItemCompositionDto l : lines) {
            ItemComposition c = new ItemComposition();
            c.setItemId(item.getId());
            c.setMoleculeName(l.getMoleculeName());
            c.setStrength(l.getStrength());
            c.setStrengthUnit(l.getStrengthUnit());
            c.setPerQty(l.getPerQty());
            c.setSortOrder(++order);
            compositions.save(c);
        }
        compositions.flush();
        item.setCompositionText(compositionText(lines));
    }

    /** "Paracetamol 500 mg + Caffeine 30 mg" (the "per" part is left out to keep it short). */
    static String compositionText(List<ItemCompositionDto> lines) {
        if (lines.isEmpty()) return null;
        String text = lines.stream().map(l -> {
            StringBuilder b = new StringBuilder(l.getMoleculeName());
            if (l.getStrength() != null) b.append(' ').append(l.getStrength().stripTrailingZeros().toPlainString());
            if (l.getStrengthUnit() != null) b.append(' ').append(l.getStrengthUnit());
            return b.toString();
        }).collect(Collectors.joining(" + "));
        return text.length() > 1000 ? text.substring(0, 997) + "..." : text;
    }

    // ---- rates ------------------------------------------------------------------------------

    /** Field checks of one rate (no overlap check — that needs all rates of the item). */
    private static void checkRate(ItemRateDto d, boolean product, Names n) {
        if (d.getEffectiveFrom() == null) throw new IllegalStateException("'From' date is required");
        if (d.getEffectiveTo() != null && d.getEffectiveTo().isBefore(d.getEffectiveFrom())) {
            throw new IllegalStateException("'To' date is before the 'From' date");
        }
        if (d.getStateId() != null && !n.statesById.containsKey(d.getStateId())) {
            throw new IllegalStateException("Unknown state: " + d.getStateId());
        }
        String[] labels = { "PTS", "PTR", "MRP", "Purchase rate", "Sample rate" };
        BigDecimal[] values = { d.getPts(), d.getPtr(), d.getMrp(), d.getPurchaseRate(), d.getSampleRate() };
        for (int k = 0; k < values.length; k++) {
            BigDecimal v = values[k];
            if (v == null) continue;
            if (v.signum() < 0) throw new IllegalStateException(labels[k] + " cannot be negative");
            if (v.stripTrailingZeros().scale() > 2) throw new IllegalStateException(labels[k] + " can have at most 2 decimals");
            if (v.compareTo(new BigDecimal("9999999999")) > 0) throw new IllegalStateException(labels[k] + " is too large");
        }
        if (product && (d.getPts() == null || d.getPtr() == null || d.getMrp() == null)) {
            throw new IllegalStateException("PTS, PTR and MRP are required for a Product");
        }
        if (d.getMrp() != null && d.getPtr() != null && d.getMrp().compareTo(d.getPtr()) < 0) {
            throw new IllegalStateException("MRP (" + d.getMrp() + ") cannot be less than PTR (" + d.getPtr() + ")");
        }
        if (d.getPtr() != null && d.getPts() != null && d.getPtr().compareTo(d.getPts()) < 0) {
            throw new IllegalStateException("PTR (" + d.getPtr() + ") cannot be less than PTS (" + d.getPts() + ")");
        }
        BigDecimal disc = d.getDiscountPct();
        if (disc != null && (disc.signum() < 0 || disc.compareTo(BigDecimal.valueOf(100)) > 0)) {
            throw new IllegalStateException("Discount % must be between 0 and 100");
        }
        if (disc != null && disc.stripTrailingZeros().scale() > 2) throw new IllegalStateException("Discount % can have at most 2 decimals");
    }

    /**
     * Make the item's rates exactly `wanted` (a row with an id of this item
     * edits that rate, without id adds one, stored rates not listed are deleted).
     * Checks every rate, closes a current rate the day before a newer one of the
     * same state starts, and refuses overlapping dates.
     */
    private void replaceRates(ItemMaster item, List<ItemRateDto> wanted, boolean product, Names n) {
        List<ItemRate> stored = item.getId() == null ? List.of() : rates.findByItemIdOrderByEffectiveFromAscIdAsc(item.getId());
        Map<Long, ItemRate> storedById = stored.stream().collect(Collectors.toMap(ItemRate::getId, Function.identity()));

        List<ItemRate> result = new ArrayList<>();
        int no = 0;
        for (ItemRateDto d : wanted) {
            no++;
            if (d == null) continue;
            try {
                // an inactive rate is kept as history only; Product checks apply to active rates
                boolean active = d.getStatus() == null || d.getStatus();
                checkRate(d, product && active, n);
            } catch (IllegalStateException e) {
                throw new IllegalStateException("Rate line " + no + " (" + stateName(d.getStateId(), n) + "): " + e.getMessage());
            }
            ItemRate r = d.getId() != null ? storedById.remove(d.getId()) : null;
            if (r == null) {
                r = new ItemRate();
                r.setItemId(item.getId());
            }
            r.setStateId(d.getStateId());
            r.setEffectiveFrom(d.getEffectiveFrom());
            r.setEffectiveTo(d.getEffectiveTo());
            r.setPts(d.getPts());
            r.setPtr(d.getPtr());
            r.setMrp(d.getMrp());
            r.setPurchaseRate(d.getPurchaseRate());
            r.setSampleRate(d.getSampleRate());
            r.setDiscountPct(d.getDiscountPct());
            r.setTaxIncluded(Boolean.TRUE.equals(d.getTaxIncluded()));
            r.setStatus(d.getStatus() == null || d.getStatus());
            result.add(r);
        }

        // per state: sort by start date, refuse duplicates / overlaps, close open rates
        Map<Long, List<ItemRate>> byState = result.stream()
                .collect(Collectors.groupingBy(r -> r.getStateId() == null ? 0L : r.getStateId(), LinkedHashMap::new, Collectors.toList()));
        for (List<ItemRate> list : byState.values()) {
            list.sort(Comparator.comparing(ItemRate::getEffectiveFrom));
            for (int k = 0; k + 1 < list.size(); k++) {
                ItemRate a = list.get(k), b = list.get(k + 1);
                String where = stateName(a.getStateId(), n);
                if (a.getEffectiveFrom().equals(b.getEffectiveFrom())) {
                    throw new IllegalStateException("Two rates for " + where + " start on " + fmt(a.getEffectiveFrom()));
                }
                if (a.getEffectiveTo() == null) {
                    a.setEffectiveTo(b.getEffectiveFrom().minusDays(1));   // the newer rate takes over
                } else if (!a.getEffectiveTo().isBefore(b.getEffectiveFrom())) {
                    throw new IllegalStateException("Rates for " + where + " overlap: " + fmt(a.getEffectiveFrom()) + " – "
                            + fmt(a.getEffectiveTo()) + " and from " + fmt(b.getEffectiveFrom()));
                }
            }
        }

        // delete first (the unique key item + state + from may be reused), then save
        if (!storedById.isEmpty()) {
            rates.deleteAll(storedById.values());
            rates.flush();
        }
        rates.saveAll(result);
        rates.flush();
    }

    private static boolean validOn(ItemRate r, LocalDate d) {
        return !r.getEffectiveFrom().isAfter(d) && (r.getEffectiveTo() == null || !r.getEffectiveTo().isBefore(d));
    }

    private static boolean isProduct(ItemMaster i, Names n) {
        ItemTypeMaster t = n.typesById.get(i.getItemTypeId());
        return t != null && PRODUCT_TYPE.equalsIgnoreCase(t.getItemTypeCode());
    }

    private static String stateName(Long stateId, Names n) {
        if (stateId == null) return "All India";
        State s = n.statesById.get(stateId);
        return s == null ? "state " + stateId : s.getStateName();
    }

    private static String fmt(LocalDate d) {
        return d == null ? "" : d.format(DateTimeFormatter.ofPattern("dd-MM-yyyy"));
    }

    // ---- mapping ----------------------------------------------------------------------------

    private ItemMasterDto fullDto(ItemMaster i, Names n) {
        ItemMasterDto d = toDto(i, n);
        if (i.getId() != null) {
            List<ItemRate> rs = rates.findByItemIdOrderByEffectiveFromAscIdAsc(i.getId());
            d.setRates(rs.stream().map(r -> toDto(r, n)).toList());
            d.setRateCount(rs.size());
            d.setCompositions(compositions.findByItemIdOrderBySortOrderAscIdAsc(i.getId()).stream().map(c -> {
                ItemCompositionDto x = new ItemCompositionDto();
                x.setMoleculeName(c.getMoleculeName());
                x.setStrength(c.getStrength() == null ? null : c.getStrength().stripTrailingZeros());
                x.setStrengthUnit(c.getStrengthUnit());
                x.setPerQty(c.getPerQty());
                return x;
            }).toList());
        }
        return d;
    }

    private ItemMasterDto toDto(ItemMaster i, Names n) {
        ItemMasterDto d = new ItemMasterDto();
        d.setId(i.getId());
        d.setItemCode(i.getItemCode());
        d.setItemName(i.getItemName());
        d.setShortName(i.getShortName());
        d.setItemTypeId(i.getItemTypeId());
        ItemTypeMaster t = n.typesById.get(i.getItemTypeId());
        if (t != null) {
            d.setItemTypeName(t.getItemTypeName());
            d.setItemTypeCode(t.getItemTypeCode());
        }
        d.setDivisionId(i.getDivisionId());
        Division dv = i.getDivisionId() == null ? null : n.divisionsById.get(i.getDivisionId());
        if (dv != null) d.setDivisionName(dv.getDivisionName());
        d.setBrandId(i.getBrandId());
        BrandMaster b = i.getBrandId() == null ? null : n.brandsById.get(i.getBrandId());
        if (b != null) d.setBrandName(b.getBrandName());
        d.setItemClass(i.getItemClass());
        d.setManufacturer(i.getManufacturer());
        d.setPackSize(i.getPackSize());
        d.setSamplePackSize(i.getSamplePackSize());
        d.setUom(i.getUom());
        d.setHsnCode(i.getHsnCode());
        d.setGstRate(i.getGstRate() == null ? null : i.getGstRate().stripTrailingZeros());
        d.setCompositionText(i.getCompositionText());
        d.setItemCodeMain(i.getItemCodeMain());
        d.setLegacySetId(i.getLegacySetId());
        d.setLegacyNewSetId(i.getLegacyNewSetId());
        d.setStatus(i.getStatus());
        d.setVersion(i.getVersion());
        d.setImagePath(i.getImagePath());
        d.setImageType(i.getImageType());
        d.setImageSize(i.getImageSize());
        d.setImageName(i.getImageName());
        d.setThumbnailPath(i.getThumbnailPath());
        d.setThumbnailType(i.getThumbnailType());
        d.setCreatedAt(i.getCreatedAt());
        d.setCreatedBy(employeeNames.resolve(n.employees, i.getCreatedBy()));
        d.setUpdatedAt(i.getUpdatedAt());
        d.setUpdatedBy(employeeNames.resolve(n.employees, i.getUpdatedBy()));
        return d;
    }

    private static ItemRateDto toDto(ItemRate r, Names n) {
        ItemRateDto d = new ItemRateDto();
        d.setId(r.getId());
        d.setStateId(r.getStateId());
        d.setStateName(stateName(r.getStateId(), n));
        d.setEffectiveFrom(r.getEffectiveFrom());
        d.setEffectiveTo(r.getEffectiveTo());
        d.setPts(r.getPts());
        d.setPtr(r.getPtr());
        d.setMrp(r.getMrp());
        d.setPurchaseRate(r.getPurchaseRate());
        d.setSampleRate(r.getSampleRate());
        d.setDiscountPct(r.getDiscountPct());
        d.setTaxIncluded(r.getTaxIncluded());
        d.setStatus(r.getStatus());
        d.setLegacyItemId(r.getLegacyItemId());
        return d;
    }

    private ItemMaster find(Long id) {
        return items.findById(id).orElseThrow(() -> new IllegalStateException("Item not found: " + id));
    }

    private ItemMaster existingItem(String code, Map<String, ItemMaster> byCode) {
        if (code == null) throw new IllegalStateException("item_code is required");
        ItemMaster item = byCode.get(code);
        if (item == null) item = items.findByItemCode(code).orElse(null);
        if (item == null) throw new IllegalStateException("item " + code + " not found (add it in the Items sheet or on the page first)");
        byCode.put(code, item);
        return item;
    }

    private static List<Map<String, String>> sheet(Map<String, List<Map<String, String>>> sheets, String name) {
        return sheets.get(name);
    }

    // ---- master lookups (one load per request) ------------------------------------------------

    private Names names() {
        return new Names(types.findAll(), divisions.findAll(), brands.findAll(), states.findAll(), employeeNames.nameMap());
    }

    /** Masters by id, and code / name (case-insensitive) -> id for the upload. */
    private static final class Names {
        final Map<Long, ItemTypeMaster> typesById;
        final Map<Long, Division> divisionsById;
        final Map<Long, BrandMaster> brandsById;
        final Map<Long, State> statesById;
        final Map<String, String> employees;
        private final Map<String, Long> typeKeys = new HashMap<>(), divisionKeys = new HashMap<>(),
                brandKeys = new HashMap<>(), stateKeys = new HashMap<>();

        Names(List<ItemTypeMaster> t, List<Division> d, List<BrandMaster> b, List<State> s, Map<String, String> employees) {
            typesById = t.stream().collect(Collectors.toMap(ItemTypeMaster::getId, Function.identity()));
            divisionsById = d.stream().collect(Collectors.toMap(Division::getOid, Function.identity()));
            brandsById = b.stream().collect(Collectors.toMap(BrandMaster::getId, Function.identity()));
            statesById = s.stream().collect(Collectors.toMap(State::getOid, Function.identity()));
            this.employees = employees;
            t.forEach(x -> { key(typeKeys, x.getItemTypeName(), x.getId()); key(typeKeys, x.getItemTypeCode(), x.getId()); });
            d.forEach(x -> { key(divisionKeys, x.getDivisionName(), x.getOid()); key(divisionKeys, x.getDivisionCode(), x.getOid()); });
            b.forEach(x -> { key(brandKeys, x.getBrandName(), x.getId()); key(brandKeys, x.getBrandCode(), x.getId()); });
            s.forEach(x -> { key(stateKeys, x.getStateName(), x.getOid()); key(stateKeys, x.getStateCode(), x.getOid()); });
        }

        private static void key(Map<String, Long> m, String k, Long id) {
            if (k != null) m.put(k.trim().toLowerCase(Locale.ROOT), id);   // codes are put last, so a code wins
        }

        private static Long find(Map<String, Long> m, String v, String what) {
            Long id = m.get(v.trim().toLowerCase(Locale.ROOT));
            if (id == null) throw new IllegalStateException(what + " '" + v + "' not found in the master");
            return id;
        }

        Long typeId(String v) { return find(typeKeys, v, "item_type"); }
        Long divisionId(String v) { return find(divisionKeys, v, "division"); }
        Long brandId(String v) { return find(brandKeys, v, "brand"); }
        Long stateId(String v) { return find(stateKeys, v, "state"); }
    }

    /** One Excel row: header names are matched without case, spaces or "*". */
    private static final class Cells {
        private static final DateTimeFormatter[] DATES = {
                DateTimeFormatter.ofPattern("uuuu-MM-dd").withResolverStyle(ResolverStyle.STRICT),
                DateTimeFormatter.ofPattern("dd-MM-uuuu").withResolverStyle(ResolverStyle.STRICT),
                DateTimeFormatter.ofPattern("dd/MM/uuuu").withResolverStyle(ResolverStyle.STRICT),
                DateTimeFormatter.ofPattern("d-M-uuuu").withResolverStyle(ResolverStyle.STRICT),
                DateTimeFormatter.ofPattern("d/M/uuuu").withResolverStyle(ResolverStyle.STRICT) };
        private final Map<String, String> cells = new HashMap<>();

        Cells(Map<String, String> raw) {
            raw.forEach((k, v) -> cells.put(k.replace("*", "").trim().toLowerCase(Locale.ROOT).replaceAll("[\\s-]+", "_"), v));
        }

        String get(String col) {
            String v = cells.get(col);
            return v == null || v.isBlank() ? null : v.trim();
        }

        boolean has(String col) { return get(col) != null; }

        BigDecimal decimal(String col) {
            String v = get(col);
            if (v == null) return null;
            try { return new BigDecimal(v.replace(",", "")); }
            catch (NumberFormatException e) { throw new IllegalStateException(col + " '" + v + "' is not a number"); }
        }

        LocalDate date(String col) {
            String v = get(col);
            if (v == null) return null;
            for (DateTimeFormatter f : DATES) {
                try { return LocalDate.parse(v, f); } catch (Exception ignored) { }
            }
            throw new IllegalStateException(col + " '" + v + "' is not a valid date (use 2026-04-01 or 01-04-2026)");
        }

        Boolean yesNo(String col) {
            String v = get(col);
            if (v == null) return null;
            return switch (v.toLowerCase(Locale.ROOT)) {
                case "y", "yes", "1", "true", "active" -> true;
                case "n", "no", "0", "false", "inactive" -> false;
                default -> throw new IllegalStateException(col + " must be Y or N");
            };
        }
    }

    private static String upper(String s) {
        return Strings.isBlank(s) ? null : s.trim().toUpperCase(Locale.ROOT);
    }

    private static String trim(String s) {
        return Strings.isBlank(s) ? null : s.trim();
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
