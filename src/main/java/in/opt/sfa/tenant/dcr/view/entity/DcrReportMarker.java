package in.opt.sfa.tenant.dcr.view.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

/** Read-only anchor for the DCR report's native query (Spring Data needs an entity); never written. */
@Entity
@Table(name = "dcr_parent_summary")
@Getter
@Setter
public class DcrReportMarker {
    @Id
    @jakarta.persistence.Column(name = "dcr_id")
    private Long dcrId;
}
