package in.opt.sfa.tenant.master.clientmaster.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Client Master (TENANT db, client_master — 76_client_master.sql): one doctor /
 * chemist / stockist ... Masters are ids: client_type.oid, category_master.oid,
 * speciality_master.oid, degree_master.id, state_master.oid, district.oid,
 * city_master.oid (Area master); mapped_emp_code = emp_detail.emp_id.
 * created_on / updated_on are filled by MySQL.
 */
@Entity
@Table(name = "client_master")
@Getter
@Setter
public class ClientMaster {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "client_code", length = 30, nullable = false)
    private String clientCode;

    @Column(name = "client_name", length = 150, nullable = false)
    private String clientName;

    @Column(name = "client_type_id")
    private Long clientTypeId;

    @Column(name = "category_id")
    private Long categoryId;

    @Column(name = "sub_category", length = 100)
    private String subCategory;

    @Column(name = "speciality_id")
    private Long specialityId;

    @Column(name = "sub_speciality", length = 100)
    private String subSpeciality;

    @Column(name = "degree_id")
    private Long degreeId;

    @Column(name = "qualification", length = 150)
    private String qualification;

    @Column(name = "designation", length = 100)
    private String designation;

    @Column(name = "is_prescriber", nullable = false)
    private Boolean prescriber = false;

    @Column(name = "experience_years", length = 100)
    private String experienceYears;

    @Column(name = "mapped_emp_code", length = 40)
    private String mappedEmpCode;

    @Column(name = "state_id")
    private Long stateId;

    @Column(name = "district_id")
    private Long districtId;

    @Column(name = "city_id")
    private Long cityId;

    @Column(name = "locality_type", columnDefinition = "enum('Urban','Semi-Urban','Rural')")
    private String localityType;

    @Column(name = "pin_code", length = 10)
    private String pinCode;

    @Column(name = "address", length = 500)
    private String address;

    @Column(name = "landmark", length = 200)
    private String landmark;

    @Column(name = "latitude", precision = 10, scale = 7)
    private BigDecimal latitude;

    @Column(name = "longitude", precision = 10, scale = 7)
    private BigDecimal longitude;

    @Column(name = "has_geo_tag", nullable = false)
    private Boolean hasGeoTag = false;

    @Column(name = "sitting_place", length = 100)
    private String sittingPlace;

    @Column(name = "property_name", length = 150)
    private String propertyName;

    @Column(name = "star_rating", nullable = false, columnDefinition = "tinyint unsigned")
    private Integer starRating = 0;

    @Column(name = "visit_frequency", length = 50)
    private String visitFrequency;

    @Column(name = "visit_days", columnDefinition = "set('MON','TUE','WED','THU','FRI','SAT','SUN')")
    private String visitDays;

    @Column(name = "preferred_visit_time", length = 100)
    private String preferredVisitTime;

    @Column(name = "patients_per_day", nullable = false, columnDefinition = "smallint unsigned")
    private Integer patientsPerDay = 0;

    @Column(name = "no_of_machines", nullable = false, columnDefinition = "smallint unsigned")
    private Integer noOfMachines = 0;

    @Column(name = "is_multi_location", nullable = false)
    private Boolean multiLocation = false;

    @Column(name = "is_pool_client", nullable = false)
    private Boolean poolClient = false;

    @Column(name = "mapped_campaign_code", length = 100)
    private String mappedCampaignCode;

    @Column(name = "mobile_number_1", length = 15)
    private String mobileNumber1;

    @Column(name = "mobile_number_2", length = 15)
    private String mobileNumber2;

    @Column(name = "email", length = 120)
    private String email;

    @Column(name = "contact_person_1_name", length = 150)
    private String contactPerson1Name;

    @Column(name = "contact_person_1_mobile", length = 15)
    private String contactPerson1Mobile;

    @Column(name = "contact_person_2_name", length = 150)
    private String contactPerson2Name;

    @Column(name = "contact_person_2_mobile", length = 15)
    private String contactPerson2Mobile;

    @Column(name = "gender", columnDefinition = "enum('Male','Female','Other')")
    private String gender;

    @Column(name = "date_of_birth")
    private LocalDate dateOfBirth;

    @Column(name = "anniversary_date")
    private LocalDate anniversaryDate;

    @Column(name = "spouse_date_of_birth")
    private LocalDate spouseDateOfBirth;

    @Column(name = "gst_number", length = 20)
    private String gstNumber;

    @Column(name = "pan_number", columnDefinition = "char(10)")
    private String panNumber;

    @Column(name = "aadhaar_number", columnDefinition = "char(12)")
    private String aadhaarNumber;

    @Column(name = "drug_license_no", length = 80)
    private String drugLicenseNo;

    @Column(name = "medical_registration_no", length = 100)
    private String medicalRegistrationNo;

    @Column(name = "photo_path", length = 255)
    private String photoPath;

    @Column(name = "photo_thumbnail_path", length = 255)
    private String photoThumbnailPath;

    @Column(name = "remarks", columnDefinition = "text")
    private String remarks;

    @Column(name = "extra_info_1", length = 255)
    private String extraInfo1;

    @Column(name = "extra_info_2", length = 255)
    private String extraInfo2;

    @Column(name = "extra_info_3", length = 255)
    private String extraInfo3;

    @Column(name = "is_active", nullable = false)
    private Boolean active = true;

    @Column(name = "is_delete_requested", nullable = false)
    private Boolean deleteRequested = false;

    @Column(name = "delete_requested_by", length = 40)
    private String deleteRequestedBy;

    @Column(name = "delete_request_reason", length = 255)
    private String deleteRequestReason;

    @Column(name = "is_approved", nullable = false)
    private Boolean approved = false;

    @Column(name = "approved_by_emp_code", length = 40)
    private String approvedByEmpCode;

    @Column(name = "approved_on")
    private LocalDateTime approvedOn;

    @Column(name = "has_fund_support", nullable = false)
    private Boolean fundSupport = false;

    @Column(name = "created_by", length = 40, updatable = false)
    private String createdBy;

    @Column(name = "updated_by", length = 40)
    private String updatedBy;

    @Column(name = "created_on", insertable = false, updatable = false)
    private LocalDateTime createdOn;

    @Column(name = "updated_on", insertable = false, updatable = false)
    private LocalDateTime updatedOn;
}
