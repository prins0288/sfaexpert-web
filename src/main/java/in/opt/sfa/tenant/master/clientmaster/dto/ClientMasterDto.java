package in.opt.sfa.tenant.master.clientmaster.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Client Master row for the API. Same field names as the entity; read-only
 * names of the linked masters and the social handles (client_master_child) on top.
 */
@Getter
@Setter
public class ClientMasterDto {
    private Long id;
    private String clientCode;
    /** "Dr.", "Mr." ... — the text from the Salutation master. */
    private String salutation;
    private String clientName;
    private Long clientTypeId;
    private Long categoryId;
    private String subCategory;
    private Long specialityId;
    private String subSpeciality;
    private Long degreeId;
    private String qualification;
    private String designation;
    private Boolean prescriber;
    private String experienceYears;
    private String mappedEmpCode;
    private Long stateId;
    private Long districtId;
    private Long cityId;
    private String localityType;
    private String pinCode;
    private String address;
    private String landmark;
    private BigDecimal latitude;
    private BigDecimal longitude;
    private Boolean hasGeoTag;
    private String sittingPlace;
    private String propertyName;
    private Integer starRating;
    private String visitFrequency;
    private String visitDays;
    private String preferredVisitTime;
    private Integer patientsPerDay;
    private Integer noOfMachines;
    private Boolean multiLocation;
    private Boolean poolClient;
    private String mappedCampaignCode;
    private String mobileNumber1;
    private String mobileNumber2;
    private String email;
    private String contactPerson1Name;
    private String contactPerson1Mobile;
    private String contactPerson2Name;
    private String contactPerson2Mobile;
    private String gender;
    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate dateOfBirth;
    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate anniversaryDate;
    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate spouseDateOfBirth;
    private String gstNumber;
    private String panNumber;
    private String aadhaarNumber;
    private String drugLicenseNo;
    private String medicalRegistrationNo;
    private String photoPath;
    private String photoThumbnailPath;
    private String remarks;
    private String extraInfo1;
    private String extraInfo2;
    private String extraInfo3;
    private Boolean active;
    private Boolean deleteRequested;
    private String deleteRequestedBy;
    private String deleteRequestReason;
    private Boolean approved;
    private String approvedByEmpCode;
    @JsonFormat(pattern = "dd-MM-yyyy HH:mm")
    private LocalDateTime approvedOn;
    private Boolean fundSupport;
    private String createdBy;
    private String updatedBy;
    @JsonFormat(pattern = "dd-MM-yyyy HH:mm")
    private LocalDateTime createdOn;
    @JsonFormat(pattern = "dd-MM-yyyy HH:mm")
    private LocalDateTime updatedOn;

    // ---- approval steps (read-only) ----
    private String approvalStatus;
    private String managerApprovedBy;
    @JsonFormat(pattern = "dd-MM-yyyy HH:mm")
    private LocalDateTime managerApprovedOn;
    private String rejectedBy;
    @JsonFormat(pattern = "dd-MM-yyyy HH:mm")
    private LocalDateTime rejectedOn;
    private String rejectReason;
    private String managerApprovedByName;
    private String rejectedByName;
    /** For the signed-in user: may they approve / reject this client now, decide its delete request? */
    private Boolean canApprove;
    private Boolean canDecideDelete;

    // ---- social handles (client_master_child) ----
    private String instagramHandle;
    private String facebookHandle;
    private String twitterHandle;

    // ---- read-only display names ----
    private String clientTypeName;
    private String categoryName;
    private String specialityName;
    private String degreeName;
    private String stateName;
    private String districtName;
    private String cityName;
    private String mappedEmpName;
    private String approvedByName;
    private String createdByName;
    private String updatedByName;
}
