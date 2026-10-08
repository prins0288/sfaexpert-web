package in.opt.sfa.tenant.dcr.view.repository;

import java.time.LocalDate;
import java.time.LocalTime;

/** Interface projection for the DCR report's native filtered query — see DcrRepository.report(). */
public interface DcrReportRow {
    Long getOid();
    LocalDate getDcrDate();
    String getDcrDateNp();
    String getWorkType();
    String getRemarks();
    LocalTime getTimeIn();
    LocalTime getTimeOut();
    String getEmpName();
    String getClientName();
    String getClientTypeName();
    String getAreaName();
    String getRouteName();
    String getDivisionName();
    String getCountryName();
}
