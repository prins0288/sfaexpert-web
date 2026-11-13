package in.opt.sfa.tenant.master.salutation.service;

import in.opt.sfa.common.util.Strings;
import in.opt.sfa.security.UserContext;
import in.opt.sfa.tenant.master.salutation.entity.SalutationMaster;
import in.opt.sfa.tenant.master.salutation.repository.SalutationMasterRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Locale;
import java.util.Optional;

/**
 * Salutation master. Forms show the active ones; the chosen text is stored
 * (not the id), so {@link #normalise} turns what a user / Excel row typed into
 * the master's exact spelling ("dr" / "DR." -> "Dr.").
 */
@Service
public class SalutationMasterService {

    private final SalutationMasterRepository repo;

    public SalutationMasterService(SalutationMasterRepository repo) {
        this.repo = repo;
    }

    @Transactional(transactionManager = "tenantTransactionManager", readOnly = true)
    public List<SalutationMaster> list() {
        return repo.findAllByOrderByDisplayOrderAscSalutationAsc();
    }

    @Transactional(transactionManager = "tenantTransactionManager", readOnly = true)
    public List<String> activeValues() {
        return repo.findByActiveTrueOrderByDisplayOrderAscSalutationAsc().stream().map(SalutationMaster::getSalutation).toList();
    }

    /** The master's spelling of an active salutation (dots, case and spaces ignored); unknown = error. */
    @Transactional(transactionManager = "tenantTransactionManager", readOnly = true)
    public String normalise(String typed) {
        if (Strings.isBlank(typed)) return null;
        String key = key(typed);
        return repo.findByActiveTrueOrderByDisplayOrderAscSalutationAsc().stream()
                .map(SalutationMaster::getSalutation).filter(s -> key(s).equals(key)).findFirst()
                .orElseThrow(() -> new IllegalStateException("Salutation '" + typed.trim() + "' is not in the Salutation master"));
    }

    @Transactional(transactionManager = "tenantTransactionManager")
    public SalutationMaster save(Long id, String salutation, Integer displayOrder) {
        String v = Strings.isBlank(salutation) ? null : salutation.trim().replaceAll("\\s+", " ");
        if (v == null) throw new IllegalStateException("Salutation is required");
        if (v.length() > 20) throw new IllegalStateException("Salutation can be at most 20 characters");
        SalutationMaster m = id == null ? new SalutationMaster()
                : repo.findById(id).orElseThrow(() -> new IllegalStateException("Salutation not found: " + id));
        Optional<SalutationMaster> same = repo.findAll().stream()
                .filter(o -> key(o.getSalutation()).equals(key(v)) && !o.getId().equals(m.getId())).findFirst();
        if (same.isPresent()) throw new IllegalStateException("Salutation " + same.get().getSalutation() + " already exists");
        m.setSalutation(v);
        if (displayOrder != null) m.setDisplayOrder(displayOrder);
        else if (m.getId() == null) {   // new without an order: at the end of the list
            m.setDisplayOrder(repo.findAll().stream().mapToInt(o -> o.getDisplayOrder() == null ? 0 : o.getDisplayOrder()).max().orElse(0) + 1);
        }
        if (m.getId() == null) m.setCreatedBy(currentUser());
        m.setUpdatedBy(currentUser());
        return repo.save(m);
    }

    @Transactional(transactionManager = "tenantTransactionManager")
    public SalutationMaster setActive(Long id, boolean active) {
        SalutationMaster m = repo.findById(id).orElseThrow(() -> new IllegalStateException("Salutation not found: " + id));
        m.setActive(active);
        m.setUpdatedBy(currentUser());
        return repo.save(m);
    }

    private static String key(String s) {
        return s.trim().toLowerCase(Locale.ROOT).replaceAll("[.\\s]", "");
    }

    private static String currentUser() {
        UserContext.CurrentUser u = UserContext.get();
        return (u == null || u.empId() == null) ? "system" : u.empId();
    }
}
