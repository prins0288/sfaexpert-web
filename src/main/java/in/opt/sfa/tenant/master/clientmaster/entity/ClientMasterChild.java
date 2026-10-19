package in.opt.sfa.tenant.master.clientmaster.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

/** Social handles of a client (TENANT db, client_master_child — one row per client). */
@Entity
@Table(name = "client_master_child")
@Getter
@Setter
public class ClientMasterChild {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "client_id", nullable = false)
    private Long clientId;

    @Column(name = "mapped_emp_code", length = 40)
    private String mappedEmpCode;

    @Column(name = "instagram_handle", length = 150)
    private String instagramHandle;

    @Column(name = "facebook_handle", length = 150)
    private String facebookHandle;

    @Column(name = "twitter_handle", length = 150)
    private String twitterHandle;

    @Column(name = "is_active", nullable = false)
    private Boolean active = true;

    @Column(name = "created_by", length = 40, updatable = false)
    private String createdBy;

    @Column(name = "created_on", insertable = false, updatable = false)
    private LocalDateTime createdOn;

    @Column(name = "updated_on", insertable = false, updatable = false)
    private LocalDateTime updatedOn;
}
