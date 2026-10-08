package in.opt.sfa.tenant.report.activity.mapper;

import in.opt.sfa.tenant.report.activity.dto.ActivityRow;
import in.opt.sfa.tenant.report.activity.entity.ReportActivity;
import org.springframework.stereotype.Component;

import java.time.format.DateTimeFormatter;

@Component
public class ReportActivityMapper {

    private static final DateTimeFormatter TS = DateTimeFormatter.ofPattern("dd-MM-yyyy HH:mm:ss");

    public ActivityRow toRow(ReportActivity a) {
        return new ActivityRow(a.getOid(), a.getReportKey(), a.getReportLabel(),
                a.getFiltersJson(), a.getSummary(),
                a.getCreatedAt() == null ? null : a.getCreatedAt().format(TS));
    }
}
