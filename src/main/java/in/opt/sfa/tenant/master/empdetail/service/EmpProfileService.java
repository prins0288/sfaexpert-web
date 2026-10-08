package in.opt.sfa.tenant.master.empdetail.service;

import in.opt.sfa.common.util.Strings;
import in.opt.sfa.tenant.master.empdetail.dto.EmpProfileDto;
import in.opt.sfa.tenant.master.empdetail.dto.EmpProfileDto.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.sql.Date;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.*;
import java.util.regex.Pattern;

/**
 * The optional profile sections of an employee, one per emp_* table
 * (personal, address, bank, nominee, emergency contact, statutory, children,
 * previous employment). Plain JDBC on the @Primary (tenant-routed)
 * JdbcTemplate: these tables have no JPA entities, and this way schema
 * validation never trips over their MySQL-specific column types.
 *
 * save() must run inside the caller's tenant transaction AFTER emp_detail is
 * flushed (every table has an FK to emp_detail.emp_id).
 */
@Service
public class EmpProfileService {

    /** emp_nominee / emp_emergency_contact.relationship_id has no master table — this is the list. */
    public static final Map<Integer, String> RELATIONSHIPS = new LinkedHashMap<>();
    static {
        String[] names = {"Father", "Mother", "Spouse", "Son", "Daughter", "Brother", "Sister", "Friend", "Other"};
        for (int i = 0; i < names.length; i++) RELATIONSHIPS.put(i + 1, names[i]);
    }
    public static final List<String> NOMINEE_TYPES = List.of("PF", "ESI", "GRATUITY", "MEDICLAIM");
    public static final List<String> BLOOD_GROUPS = List.of("A+", "A-", "B+", "B-", "AB+", "AB-", "O+", "O-");
    public static final List<String> MARITAL = List.of("SINGLE", "MARRIED");
    private static final List<String> GENDERS = List.of("MALE", "FEMALE", "OTHER");

    // same rules as the tables' CHECK constraints, so errors are readable
    private static final Pattern PIN = Pattern.compile("^[1-9][0-9]{5}$");
    private static final Pattern IFSC = Pattern.compile("^[A-Z]{4}0[A-Z0-9]{6}$");
    private static final Pattern ACCOUNT = Pattern.compile("^[0-9]{6,20}$");
    private static final Pattern MOBILE10 = Pattern.compile("^[0-9]{10}$");
    private static final Pattern PHONE = Pattern.compile("^[0-9+\\- ]{6,15}$");
    private static final Pattern PAN = Pattern.compile("^[A-Z]{5}[0-9]{4}[A-Z]$");
    private static final Pattern UAN = Pattern.compile("^[0-9]{12}$");
    private static final Pattern EMAIL = Pattern.compile("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$");

    private final JdbcTemplate jdbc;   // tenant-routed (@Primary)

    public EmpProfileService(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    /** Dropdown values the form needs that have no master table. */
    public Map<String, Object> options() {
        List<Map<String, Object>> rel = new ArrayList<>();
        RELATIONSHIPS.forEach((id, label) -> rel.add(Map.of("oid", id, "label", label)));
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("relationships", rel);
        m.put("nomineeTypes", NOMINEE_TYPES);
        m.put("bloodGroups", BLOOD_GROUPS);
        m.put("maritalStatus", MARITAL);
        return m;
    }

    // ---- read -----------------------------------------------------------------

    @Transactional(transactionManager = "tenantTransactionManager", readOnly = true)
    public EmpProfileDto load(String empId) {
        EmpProfileDto p = new EmpProfileDto();
        p.setPersonal(jdbc.query("SELECT * FROM emp_personal WHERE emp_id = ?", rs -> {
            if (!rs.next()) return null;
            Personal x = new Personal();
            x.setDob(iso(rs, "dob"));
            x.setFatherName(rs.getString("father_name"));
            x.setMotherName(rs.getString("mother_name"));
            x.setMaritalStatus(rs.getString("marital_status"));
            x.setAnniversaryDate(iso(rs, "anniversary_date"));
            x.setSpouseName(rs.getString("spouse_name"));
            x.setQualificationId(longOrNull(rs, "qualification_id"));
            x.setQualificationDetail(rs.getString("qualification_detail"));
            x.setBloodGroup(rs.getString("blood_group"));
            x.setPersonalEmail(rs.getString("personal_email"));
            x.setLandlineNo(rs.getString("landline_no"));
            x.setTotalExperienceYrs(rs.getBigDecimal("total_experience_yrs"));
            x.setShirtSize(intOrNull(rs, "shirt_size"));
            x.setRemarks(rs.getString("remarks"));
            return x;
        }, empId));

        jdbc.query("SELECT * FROM emp_address WHERE emp_id = ?", rs -> {
            Address a = new Address();
            a.setAddressLine(rs.getString("address_line"));
            a.setCity(rs.getString("city"));
            a.setDistrict(rs.getString("district"));
            a.setStateId(longOrNull(rs, "state_id"));
            a.setPincode(rs.getString("pincode"));
            if ("PRESENT".equals(rs.getString("address_type"))) p.setPresentAddress(a); else p.setPermanentAddress(a);
        }, empId);

        p.setBankAccounts(jdbc.query("SELECT * FROM emp_bank_account WHERE emp_id = ? ORDER BY is_primary DESC, id",
                (rs, i) -> {
                    BankAccount b = new BankAccount();
                    b.setBankId(longOrNull(rs, "bank_id"));
                    b.setIfscCode(rs.getString("ifsc_code"));
                    b.setAccountNo(rs.getString("account_no"));
                    b.setBranchName(rs.getString("branch_name"));
                    b.setPrimary(rs.getBoolean("is_primary"));
                    b.setActive(rs.getBoolean("is_active"));
                    return b;
                }, empId));

        p.setNominees(jdbc.query("SELECT * FROM emp_nominee WHERE emp_id = ? ORDER BY nominee_type, id", (rs, i) -> {
            Nominee n = new Nominee();
            n.setNomineeType(rs.getString("nominee_type"));
            n.setNomineeName(rs.getString("nominee_name"));
            n.setRelationshipId(intOrNull(rs, "relationship_id"));
            n.setNomineeDob(iso(rs, "nominee_dob"));
            n.setContactNo(rs.getString("contact_no"));
            n.setSharePct(rs.getBigDecimal("share_pct"));
            return n;
        }, empId));

        p.setEmergencyContacts(jdbc.query("SELECT * FROM emp_emergency_contact WHERE emp_id = ? ORDER BY priority, id", (rs, i) -> {
            EmergencyContact c = new EmergencyContact();
            c.setContactName(rs.getString("contact_name"));
            c.setRelationshipId(intOrNull(rs, "relationship_id"));
            c.setContactNo1(rs.getString("contact_no_1"));
            c.setContactNo2(rs.getString("contact_no_2"));
            return c;
        }, empId));

        p.setStatutory(jdbc.query("SELECT pan_no, pf_no, uan_no, esi_no, mediclaim_policy_no, aadhaar_last4 FROM emp_statutory WHERE emp_id = ?", rs -> {
            if (!rs.next()) return null;
            Statutory s = new Statutory();
            s.setPanNo(rs.getString("pan_no"));
            s.setPfNo(rs.getString("pf_no"));
            s.setUanNo(rs.getString("uan_no"));
            s.setEsiNo(rs.getString("esi_no"));
            s.setMediclaimPolicyNo(rs.getString("mediclaim_policy_no"));
            s.setAadhaarLast4(rs.getString("aadhaar_last4"));
            return s;
        }, empId));

        p.setChildren(jdbc.query("SELECT * FROM emp_child WHERE emp_id = ? ORDER BY sort_order, id", (rs, i) -> {
            Child c = new Child();
            c.setChildName(rs.getString("child_name"));
            c.setGender(rs.getString("gender"));
            c.setDob(iso(rs, "dob"));
            return c;
        }, empId));

        p.setPrevEmployment(jdbc.query("SELECT * FROM emp_prev_employment WHERE emp_id = ? ORDER BY from_date DESC, id", (rs, i) -> {
            PrevEmployment w = new PrevEmployment();
            w.setCompanyName(rs.getString("company_name"));
            w.setDesignation(rs.getString("designation"));
            w.setFromDate(iso(rs, "from_date"));
            w.setToDate(iso(rs, "to_date"));
            w.setExperienceYrs(rs.getBigDecimal("experience_yrs"));
            w.setLastCtc(rs.getBigDecimal("last_ctc"));
            return w;
        }, empId));
        return p;
    }

    // ---- write ----------------------------------------------------------------

    /**
     * Validate without writing — callers run this BEFORE inserting emp_detail so a
     * bad profile never leaves a half-saved employee. empId may be "" for a new one.
     */
    public void check(String empId, EmpProfileDto p) {
        if (p != null) validate(empId == null ? "" : empId, p);
    }

    /** Validate + store every NON-null section. Runs in the caller's tenant transaction. */
    public void save(String empId, EmpProfileDto p) {
        if (p == null) return;
        validate(empId, p);
        if (p.getPersonal() != null) savePersonal(empId, p.getPersonal());
        if (p.getPresentAddress() != null) saveAddress(empId, "PRESENT", p.getPresentAddress());
        if (p.getPermanentAddress() != null) saveAddress(empId, "PERMANENT", p.getPermanentAddress());
        if (p.getBankAccounts() != null) saveBanks(empId, p.getBankAccounts());
        if (p.getNominees() != null) saveNominees(empId, p.getNominees());
        if (p.getEmergencyContacts() != null) saveEmergency(empId, p.getEmergencyContacts());
        if (p.getStatutory() != null) saveStatutory(empId, p.getStatutory());
        if (p.getChildren() != null) saveChildren(empId, p.getChildren());
        if (p.getPrevEmployment() != null) savePrev(empId, p.getPrevEmployment());
    }

    private void savePersonal(String empId, Personal x) {
        if (allBlank(x.getDob(), x.getFatherName(), x.getMotherName(), x.getMaritalStatus(), x.getAnniversaryDate(),
                x.getSpouseName(), x.getQualificationId(), x.getQualificationDetail(), x.getBloodGroup(), x.getPersonalEmail(),
                x.getLandlineNo(), x.getTotalExperienceYrs(), x.getShirtSize(), x.getRemarks())) {
            jdbc.update("DELETE FROM emp_personal WHERE emp_id = ?", empId);
            return;
        }
        boolean married = "MARRIED".equals(blankToNull(x.getMaritalStatus()));
        jdbc.update("""
                INSERT INTO emp_personal (emp_id, dob, father_name, mother_name, marital_status, anniversary_date, spouse_name,
                    qualification_id, qualification_detail, blood_group, personal_email, landline_no, total_experience_yrs,
                    shirt_size, remarks)
                VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)
                ON DUPLICATE KEY UPDATE dob = VALUES(dob), father_name = VALUES(father_name), mother_name = VALUES(mother_name),
                    marital_status = VALUES(marital_status), anniversary_date = VALUES(anniversary_date),
                    spouse_name = VALUES(spouse_name), qualification_id = VALUES(qualification_id),
                    qualification_detail = VALUES(qualification_detail), blood_group = VALUES(blood_group),
                    personal_email = VALUES(personal_email), landline_no = VALUES(landline_no),
                    total_experience_yrs = VALUES(total_experience_yrs), shirt_size = VALUES(shirt_size),
                    remarks = VALUES(remarks), version = version + 1
                """,
                empId, date(x.getDob()), blankToNull(x.getFatherName()), blankToNull(x.getMotherName()),
                blankToNull(x.getMaritalStatus()), married ? date(x.getAnniversaryDate()) : null,
                married ? blankToNull(x.getSpouseName()) : null, x.getQualificationId(), blankToNull(x.getQualificationDetail()),
                blankToNull(x.getBloodGroup()), blankToNull(x.getPersonalEmail()), blankToNull(x.getLandlineNo()),
                x.getTotalExperienceYrs(), x.getShirtSize(), blankToNull(x.getRemarks()));
    }

    private void saveAddress(String empId, String type, Address a) {
        if (Strings.isBlank(a.getAddressLine())) {
            jdbc.update("DELETE FROM emp_address WHERE emp_id = ? AND address_type = ?", empId, type);
            return;
        }
        jdbc.update("""
                INSERT INTO emp_address (emp_id, address_type, address_line, city, district, state_id, pincode)
                VALUES (?,?,?,?,?,?,?)
                ON DUPLICATE KEY UPDATE address_line = VALUES(address_line), city = VALUES(city),
                    district = VALUES(district), state_id = VALUES(state_id), pincode = VALUES(pincode)
                """, empId, type, a.getAddressLine().trim(), blankToNull(a.getCity()), blankToNull(a.getDistrict()),
                a.getStateId(), blankToNull(a.getPincode()));
    }

    private void saveBanks(String empId, List<BankAccount> rows) {
        jdbc.update("DELETE FROM emp_bank_account WHERE emp_id = ?", empId);
        List<BankAccount> kept = rows.stream().filter(b -> b != null && !Strings.isBlank(b.getAccountNo())).toList();
        boolean anyPrimary = kept.stream().anyMatch(b -> Boolean.TRUE.equals(b.getPrimary()) && !Boolean.FALSE.equals(b.getActive()));
        for (int i = 0; i < kept.size(); i++) {
            BankAccount b = kept.get(i);
            boolean active = !Boolean.FALSE.equals(b.getActive());
            // exactly one active primary: the one marked, else the first active account
            boolean primary = active && (anyPrimary ? Boolean.TRUE.equals(b.getPrimary()) : i == firstActive(kept));
            jdbc.update("INSERT INTO emp_bank_account (emp_id, bank_id, ifsc_code, account_no, branch_name, is_primary, is_active) VALUES (?,?,?,?,?,?,?)",
                    empId, b.getBankId(), upperOrNull(b.getIfscCode()), b.getAccountNo().trim(), blankToNull(b.getBranchName()), primary, active);
        }
    }

    private static int firstActive(List<BankAccount> rows) {
        for (int i = 0; i < rows.size(); i++) if (!Boolean.FALSE.equals(rows.get(i).getActive())) return i;
        return -1;
    }

    private void saveNominees(String empId, List<Nominee> rows) {
        jdbc.update("DELETE FROM emp_nominee WHERE emp_id = ?", empId);
        for (Nominee n : rows) {
            if (n == null || Strings.isBlank(n.getNomineeName())) continue;
            jdbc.update("INSERT INTO emp_nominee (emp_id, nominee_type, nominee_name, relationship_id, nominee_dob, contact_no, share_pct) VALUES (?,?,?,?,?,?,?)",
                    empId, n.getNomineeType(), n.getNomineeName().trim(), n.getRelationshipId(), date(n.getNomineeDob()),
                    blankToNull(n.getContactNo()), n.getSharePct() == null ? new BigDecimal("100") : n.getSharePct());
        }
    }

    private void saveEmergency(String empId, List<EmergencyContact> rows) {
        jdbc.update("DELETE FROM emp_emergency_contact WHERE emp_id = ?", empId);
        int priority = 1;
        for (EmergencyContact c : rows) {
            if (c == null || Strings.isBlank(c.getContactNo1())) continue;
            jdbc.update("INSERT INTO emp_emergency_contact (emp_id, contact_name, relationship_id, contact_no_1, contact_no_2, priority) VALUES (?,?,?,?,?,?)",
                    empId, blankToNull(c.getContactName()), c.getRelationshipId(), c.getContactNo1().trim(),
                    blankToNull(c.getContactNo2()), priority++);
        }
    }

    /** Only the editable columns — the encrypted Aadhaar columns are never touched here. */
    private void saveStatutory(String empId, Statutory s) {
        boolean blank = allBlank(s.getPanNo(), s.getPfNo(), s.getUanNo(), s.getEsiNo(), s.getMediclaimPolicyNo());
        Integer exists = jdbc.queryForObject("SELECT COUNT(*) FROM emp_statutory WHERE emp_id = ?", Integer.class, empId);
        if (blank && (exists == null || exists == 0)) return;
        jdbc.update("""
                INSERT INTO emp_statutory (emp_id, pan_no, pf_no, uan_no, esi_no, mediclaim_policy_no) VALUES (?,?,?,?,?,?)
                ON DUPLICATE KEY UPDATE pan_no = VALUES(pan_no), pf_no = VALUES(pf_no), uan_no = VALUES(uan_no),
                    esi_no = VALUES(esi_no), mediclaim_policy_no = VALUES(mediclaim_policy_no), version = version + 1
                """, empId, upperOrNull(s.getPanNo()), blankToNull(s.getPfNo()), blankToNull(s.getUanNo()),
                blankToNull(s.getEsiNo()), blankToNull(s.getMediclaimPolicyNo()));
    }

    private void saveChildren(String empId, List<Child> rows) {
        jdbc.update("DELETE FROM emp_child WHERE emp_id = ?", empId);
        int order = 1;
        for (Child c : rows) {
            if (c == null || Strings.isBlank(c.getChildName())) continue;
            jdbc.update("INSERT INTO emp_child (emp_id, child_name, gender, dob, sort_order) VALUES (?,?,?,?,?)",
                    empId, c.getChildName().trim(), blankToNull(c.getGender()), date(c.getDob()), order++);
        }
    }

    private void savePrev(String empId, List<PrevEmployment> rows) {
        jdbc.update("DELETE FROM emp_prev_employment WHERE emp_id = ?", empId);
        for (PrevEmployment w : rows) {
            if (w == null || Strings.isBlank(w.getCompanyName())) continue;
            jdbc.update("INSERT INTO emp_prev_employment (emp_id, company_name, designation, from_date, to_date, experience_yrs, last_ctc) VALUES (?,?,?,?,?,?,?)",
                    empId, w.getCompanyName().trim(), blankToNull(w.getDesignation()), date(w.getFromDate()), date(w.getToDate()),
                    w.getExperienceYrs(), w.getLastCtc());
        }
    }

    // ---- validation -------------------------------------------------------------

    private void validate(String empId, EmpProfileDto p) {
        Personal x = p.getPersonal();
        if (x != null) {
            if (!Strings.isBlank(x.getMaritalStatus()) && !MARITAL.contains(x.getMaritalStatus())) fail("Personal: marital status must be SINGLE or MARRIED");
            if (!Strings.isBlank(x.getBloodGroup()) && !BLOOD_GROUPS.contains(x.getBloodGroup())) fail("Personal: invalid blood group");
            if (!Strings.isBlank(x.getPersonalEmail())) {
                String mail = x.getPersonalEmail().trim();
                if (!EMAIL.matcher(mail).matches()) fail("Personal: personal email is not valid");
                Integer used = jdbc.queryForObject("SELECT COUNT(*) FROM emp_personal WHERE personal_email = ? AND emp_id <> ?",
                        Integer.class, mail, empId);
                if (used != null && used > 0) fail("Personal: personal email is already used by another employee");
            }
            if (x.getTotalExperienceYrs() != null && (x.getTotalExperienceYrs().signum() < 0 || x.getTotalExperienceYrs().compareTo(new BigDecimal("999.9")) > 0)) {
                fail("Personal: experience must be between 0 and 999.9 years");
            }
            if (x.getShirtSize() != null && (x.getShirtSize() < 0 || x.getShirtSize() > 255)) fail("Personal: shirt size must be 0-255");
            if (x.getDob() != null && date(x.getDob()) != null && date(x.getDob()).isAfter(LocalDate.now())) fail("Personal: date of birth cannot be in the future");
        }
        checkAddress("Present address", p.getPresentAddress());
        checkAddress("Permanent address", p.getPermanentAddress());

        if (p.getBankAccounts() != null) {
            Set<String> seen = new HashSet<>();
            long primaries = 0;
            for (BankAccount b : p.getBankAccounts()) {
                if (b == null || allBlank(b.getAccountNo(), b.getIfscCode(), b.getBankId(), b.getBranchName())) continue;
                if (Strings.isBlank(b.getAccountNo())) fail("Bank: account number is required for every bank row");
                if (!ACCOUNT.matcher(b.getAccountNo().trim()).matches()) fail("Bank: account number must be 6-20 digits");
                if (!Strings.isBlank(b.getIfscCode()) && !IFSC.matcher(b.getIfscCode().trim().toUpperCase()).matches()) {
                    fail("Bank: IFSC must look like SBIN0001234");
                }
                if (!seen.add(b.getAccountNo().trim() + "|" + (b.getIfscCode() == null ? "" : b.getIfscCode().trim().toUpperCase()))) {
                    fail("Bank: the same account is entered twice");
                }
                if (Boolean.TRUE.equals(b.getPrimary()) && !Boolean.FALSE.equals(b.getActive())) primaries++;
            }
            if (primaries > 1) fail("Bank: only one active account can be primary");
        }

        if (p.getNominees() != null) {
            Map<String, BigDecimal> share = new HashMap<>();
            for (Nominee n : p.getNominees()) {
                if (n == null || Strings.isBlank(n.getNomineeName())) continue;
                if (n.getNomineeType() == null || !NOMINEE_TYPES.contains(n.getNomineeType())) fail("Nominee: choose a type (PF, ESI, Gratuity or Mediclaim) for " + n.getNomineeName());
                checkRelationship("Nominee", n.getRelationshipId());
                if (!Strings.isBlank(n.getContactNo()) && !MOBILE10.matcher(n.getContactNo().trim()).matches()) fail("Nominee: contact number must be 10 digits");
                BigDecimal pct = n.getSharePct() == null ? new BigDecimal("100") : n.getSharePct();
                if (pct.signum() <= 0 || pct.compareTo(new BigDecimal("100")) > 0) fail("Nominee: share must be more than 0 and at most 100%");
                BigDecimal total = share.merge(n.getNomineeType(), pct, BigDecimal::add);
                if (total.compareTo(new BigDecimal("100")) > 0) fail("Nominee: " + n.getNomineeType() + " shares add up to more than 100%");
            }
        }

        if (p.getEmergencyContacts() != null) {
            for (EmergencyContact c : p.getEmergencyContacts()) {
                if (c == null || allBlank(c.getContactName(), c.getContactNo1(), c.getContactNo2(), c.getRelationshipId())) continue;
                if (Strings.isBlank(c.getContactNo1())) fail("Emergency contact: phone 1 is required");
                if (!PHONE.matcher(c.getContactNo1().trim()).matches()) fail("Emergency contact: phone 1 is not valid");
                if (!Strings.isBlank(c.getContactNo2()) && !PHONE.matcher(c.getContactNo2().trim()).matches()) fail("Emergency contact: phone 2 is not valid");
                checkRelationship("Emergency contact", c.getRelationshipId());
            }
        }

        Statutory s = p.getStatutory();
        if (s != null) {
            if (!Strings.isBlank(s.getPanNo()) && !PAN.matcher(s.getPanNo().trim().toUpperCase()).matches()) fail("Statutory: PAN must look like ABCDE1234F");
            if (!Strings.isBlank(s.getUanNo()) && !UAN.matcher(s.getUanNo().trim()).matches()) fail("Statutory: UAN must be 12 digits");
            unique("pan_no", upperOrNull(s.getPanNo()), "PAN", empId);
            unique("pf_no", blankToNull(s.getPfNo()), "PF number", empId);
            unique("uan_no", blankToNull(s.getUanNo()), "UAN", empId);
            unique("esi_no", blankToNull(s.getEsiNo()), "ESI number", empId);
        }

        if (p.getChildren() != null) {
            for (Child c : p.getChildren()) {
                if (c == null || Strings.isBlank(c.getChildName())) continue;
                if (!Strings.isBlank(c.getGender()) && !GENDERS.contains(c.getGender())) fail("Children: invalid gender for " + c.getChildName());
            }
        }

        if (p.getPrevEmployment() != null) {
            for (PrevEmployment w : p.getPrevEmployment()) {
                if (w == null || allBlank(w.getCompanyName(), w.getDesignation(), w.getFromDate(), w.getToDate(), w.getLastCtc())) continue;
                if (Strings.isBlank(w.getCompanyName())) fail("Previous employment: company name is required");
                LocalDate from = date(w.getFromDate()), to = date(w.getToDate());
                if (from != null && to != null && to.isBefore(from)) fail("Previous employment: 'to' date is before 'from' date at " + w.getCompanyName());
                if (w.getLastCtc() != null && w.getLastCtc().signum() < 0) fail("Previous employment: CTC cannot be negative");
            }
        }
    }

    private void checkAddress(String label, Address a) {
        if (a == null) return;
        boolean any = !allBlank(a.getAddressLine(), a.getCity(), a.getDistrict(), a.getStateId(), a.getPincode());
        if (any && Strings.isBlank(a.getAddressLine())) fail(label + ": address line is required");
        if (!Strings.isBlank(a.getPincode()) && !PIN.matcher(a.getPincode().trim()).matches()) fail(label + ": pincode must be 6 digits (not starting with 0)");
    }

    private static void checkRelationship(String label, Integer id) {
        if (id != null && !RELATIONSHIPS.containsKey(id)) fail(label + ": invalid relationship");
    }

    private void unique(String column, String value, String label, String empId) {
        if (value == null) return;
        Integer used = jdbc.queryForObject("SELECT COUNT(*) FROM emp_statutory WHERE " + column + " = ? AND emp_id <> ?",
                Integer.class, value, empId);
        if (used != null && used > 0) fail("Statutory: this " + label + " is already used by another employee");
    }

    // ---- helpers ----------------------------------------------------------------

    private static void fail(String message) { throw new IllegalStateException(message); }

    private static boolean allBlank(Object... values) {
        for (Object v : values) {
            if (v == null) continue;
            if (v instanceof String s) { if (!s.isBlank()) return false; } else return false;
        }
        return true;
    }

    private static String blankToNull(String s) { return Strings.isBlank(s) ? null : s.trim(); }
    private static String upperOrNull(String s) { return Strings.isBlank(s) ? null : s.trim().toUpperCase(); }

    private static LocalDate date(String s) {
        if (Strings.isBlank(s)) return null;
        try { return LocalDate.parse(s.trim()); } catch (Exception e) { return null; }
    }

    private static String iso(ResultSet rs, String col) throws SQLException {
        Date d = rs.getDate(col);
        return d == null ? null : d.toLocalDate().toString();
    }

    private static Long longOrNull(ResultSet rs, String col) throws SQLException {
        long v = rs.getLong(col);
        return rs.wasNull() ? null : v;
    }

    private static Integer intOrNull(ResultSet rs, String col) throws SQLException {
        int v = rs.getInt(col);
        return rs.wasNull() ? null : v;
    }
}
