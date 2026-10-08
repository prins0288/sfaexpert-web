package in.opt.sfa.tenant.dcr.view.service;

import in.opt.sfa.tenant.dcr.view.repository.DcrReportRow;
import in.opt.sfa.tenant.dcr.view.repository.DcrRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Service
public class DcrReportService {

    private final DcrRepository dcrs;

    public DcrReportService(DcrRepository dcrs) {
        this.dcrs = dcrs;
    }

    @Transactional(transactionManager = "tenantTransactionManager", readOnly = true)
    public List<DcrReportRow> report(Long divisionOid, Long routeOid, Long areaOid, Long employeeOid,
                                      LocalDate fromDate, LocalDate toDate) {
        return dcrs.report(divisionOid, routeOid, areaOid, employeeOid, fromDate, toDate);
    }
}
