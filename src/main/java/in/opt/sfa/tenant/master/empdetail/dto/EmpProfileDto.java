package in.opt.sfa.tenant.master.empdetail.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.List;

/**
 * The optional "full profile" sections of an employee — one per emp_* table.
 * Every section is optional: a NULL section is left untouched on save, a
 * present one REPLACES what is stored (an empty list clears it).
 * Dates are ISO yyyy-MM-dd strings, like the rest of the employee form.
 */
@Getter
@Setter
@JsonIgnoreProperties(ignoreUnknown = true)
public class EmpProfileDto {

    private Personal personal;                       // emp_personal (1:1)
    private Address presentAddress;                  // emp_address, type PRESENT
    private Address permanentAddress;                // emp_address, type PERMANENT
    private List<BankAccount> bankAccounts;          // emp_bank_account
    private List<Nominee> nominees;                  // emp_nominee
    private List<EmergencyContact> emergencyContacts;// emp_emergency_contact
    private Statutory statutory;                     // emp_statutory (1:1)
    private List<Child> children;                    // emp_child
    private List<PrevEmployment> prevEmployment;     // emp_prev_employment

    @Getter @Setter @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Personal {
        private String dob;
        private String fatherName;
        private String motherName;
        private String maritalStatus;     // SINGLE | MARRIED
        private String anniversaryDate;
        private String spouseName;
        private Long qualificationId;     // -> degree_master.id
        private String qualificationDetail;
        private String bloodGroup;        // A+ A- B+ B- AB+ AB- O+ O-
        private String personalEmail;
        private String landlineNo;
        private BigDecimal totalExperienceYrs;
        private Integer shirtSize;
        private String remarks;
    }

    @Getter @Setter @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Address {
        private String addressLine;
        private String city;
        private String district;
        private Long stateId;             // -> state_master.oid
        private String pincode;
    }

    @Getter @Setter @JsonIgnoreProperties(ignoreUnknown = true)
    public static class BankAccount {
        private Long bankId;              // -> bank_master.id
        private String ifscCode;
        private String accountNo;
        private String branchName;
        private Boolean primary;
        private Boolean active;
    }

    @Getter @Setter @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Nominee {
        private String nomineeType;       // PF | ESI | GRATUITY | MEDICLAIM
        private String nomineeName;
        private Integer relationshipId;   // see EmpProfileService.RELATIONSHIPS
        private String nomineeDob;
        private String contactNo;
        private BigDecimal sharePct;
    }

    @Getter @Setter @JsonIgnoreProperties(ignoreUnknown = true)
    public static class EmergencyContact {
        private String contactName;
        private Integer relationshipId;
        private String contactNo1;
        private String contactNo2;
    }

    @Getter @Setter @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Statutory {
        private String panNo;
        private String pfNo;
        private String uanNo;
        private String esiNo;
        private String mediclaimPolicyNo;
        /** Read-only: last 4 digits of the stored (encrypted) Aadhaar, if any. */
        private String aadhaarLast4;
    }

    @Getter @Setter @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Child {
        private String childName;
        private String gender;            // MALE | FEMALE | OTHER
        private String dob;
    }

    @Getter @Setter @JsonIgnoreProperties(ignoreUnknown = true)
    public static class PrevEmployment {
        private String companyName;
        private String designation;
        private String fromDate;
        private String toDate;
        private BigDecimal experienceYrs;
        private BigDecimal lastCtc;
    }
}
