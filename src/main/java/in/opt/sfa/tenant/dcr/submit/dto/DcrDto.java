package in.opt.sfa.tenant.dcr.submit.dto;

import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/**
 * One day's DCR as the form sends / reads it. Times are "HH:mm", dates ISO yyyy-MM-dd.
 * Calls to LISTED clients are {@link Visit}s (client_master), calls to clients NOT in
 * client_master are {@link Unlisted}s; both carry their own items, leaflets, treatments and
 * joint-working people. An unlisted call is referred to (by an expense) through its {@code key}.
 */
@Getter
@Setter
public class DcrDto {
    private Long dcrId;
    private String empId;
    private String dcrDate;
    private String workType = "WORKING";
    private Long startAreaId;
    private Long workingAreaId;
    private String timeIn;
    private String timeOut;
    private String travelMode;
    private BigDecimal meterStart;
    private BigDecimal meterEnd;
    private String remarks;
    private Long dayEndAreaId;
    private String dayEndAt;          // yyyy-MM-ddTHH:mm
    private String dayEndRemarks;
    private Long stayAreaId;
    private String stayHotel;
    /** true = submit now, false = save as a draft. */
    private boolean submit;
    private List<Visit> visits = new ArrayList<>();
    private List<Unlisted> unlisted = new ArrayList<>();
    private List<Expense> expenses = new ArrayList<>();
    /** People who worked with the owner the whole day (emp_ids). */
    private List<String> jointWith = new ArrayList<>();

    // ---- read-only, filled by the server ----
    private String empName;
    private String designation;
    private Integer status;
    private String statusName;
    private Integer listedCalls;
    private Integer unlistedCalls;
    private BigDecimal pobTotal;
    private BigDecimal expenseTotal;
    private String submittedAt;
    private String approvedByName;
    private String approvedAt;
    private String approvalRemarks;
    private Boolean canEdit;
    private Boolean canApprove;

    /** What every call (listed or unlisted) has in common. */
    @Getter
    @Setter
    public static class Call {
        /** The tab (app_dcr_worktype.worktype_code) it was entered under. */
        private String worktypeCode;
        private String visitType = "CALL";
        private String activityType;
        private Long areaId;
        private String inTime;
        private String outTime;
        private String checkinAt;
        private BigDecimal checkinLat;
        private BigDecimal checkinLng;
        private BigDecimal pobValue;
        private String remarks;
        private boolean refused;
        private String nextVisit;
        private String nextVisitPriority;
        private String nextVisitRemarks;
        private String signaturePath;
        private List<Item> items = new ArrayList<>();
        private List<Leaflet> leaflets = new ArrayList<>();
        private List<Integer> treatments = new ArrayList<>();
        /** People who worked with the owner at this call (emp_ids). */
        private List<String> jointWith = new ArrayList<>();
    }

    @Getter
    @Setter
    public static class Visit extends Call {
        private Long clientId;
        // read-only
        private String clientName;
        private String clientCode;
    }

    @Getter
    @Setter
    public static class Unlisted extends Call {
        /** Form-side handle, so an expense can point at this call before it has an id. */
        private String key;
        private String clientName;
        private Long clientTypeId;
        private Long specialityId;
        private Long categoryId;
        private String qualification;
        private String mobile;
        private String contactPerson;
        private String address;
        // read-only
        private Integer approvalStatus;
    }

    @Getter
    @Setter
    public static class Item {
        private Long itemId;
        private String itemType;
        private Integer qty;
        private Integer pobQty;
        private BigDecimal rate;
        private boolean detailed;
        private String remarks;
        // read-only
        private String itemName;
    }

    @Getter
    @Setter
    public static class Leaflet {
        private Long itemId;
        private Integer qty;
        private BigDecimal value;
        private String itemName;
    }

    @Getter
    @Setter
    public static class Expense {
        private Integer expenseId;
        private BigDecimal amount;
        private String description;
        /** 0 = not about a client, 1 = a listed client (clientId), 2 = an unlisted call (unlistedKey). */
        private int clientKind;
        private Long clientId;
        private String unlistedKey;
        private Long fromAreaId;
        private Long toAreaId;
        private BigDecimal startKm;
        private BigDecimal endKm;
        private String startImagePath;
        private String endImagePath;
        private String fileName;
        private String fileType;
        private String filePath;
        // read-only
        private String expenseName;
    }
}
