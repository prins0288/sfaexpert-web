package in.opt.sfa.tenant.dcr.view.repository;

import java.time.LocalDate;
import java.time.LocalTime;

/** One DCR call (listed or unlisted client) for the day-wise report. */
public interface DcrReportRow {
    Long getOid();
    String getEmpId();
    LocalDate getDcrDate();
    String getWorkType();
    String getRemarks();
    LocalTime getTimeIn();
    LocalTime getTimeOut();
    String getEmpName();
    String getClientName();
    String getClientTypeName();
    String getAreaName();
    String getDivisionName();
    /** Listed / Unlisted. */
    String getCallKind();
    java.math.BigDecimal getPobValue();
    Integer getDcrStatus();
}
