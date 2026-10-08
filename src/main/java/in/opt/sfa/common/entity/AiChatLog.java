package in.opt.sfa.common.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * One AI assistant turn (COMMON db, table ai_chat): the user's prompt and the
 * AI's HTML response, stamped with who asked (emp_id) and their company
 * (company_code). All tenants' AI history lives here so it can be reviewed centrally.
 */
@Entity
@Table(name = "ai_chat")
@Getter
@Setter
public class AiChatLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "company_code", nullable = false, length = 64)
    private String companyCode;

    @Column(name = "emp_id", length = 40)
    private String empId;

    @Column(columnDefinition = "TEXT")
    private String prompt;

    @Column(columnDefinition = "MEDIUMTEXT")
    private String response;

    @Column(length = 64)
    private String model;

    @Column(name = "created_at")
    private LocalDateTime createdAt;
}
