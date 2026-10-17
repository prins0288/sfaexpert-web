package in.opt.sfa.tenant.master.empdetail.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * One uploaded document of an employee (TENANT db, emp_document — table from
 * 61_employee_v2_schema.sql, + document_no / remarks / thumbnail from 74).
 * The file is stored by FileUploadService under <company>/employeeDocument/.
 * Removing a document only sets is_active = 0 (the file is kept for audit).
 */
@Entity
@Table(name = "emp_document")
@Getter
@Setter
public class EmpDocument {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "emp_id", nullable = false, length = 40)
    private String empId;

    /** -> document_master.id (PAN, Aadhaar, Offer letter ...). */
    @Column(name = "document_type_id", nullable = false)
    private Long documentTypeId;

    @Column(name = "document_no", length = 50)
    private String documentNo;

    @Column(length = 255)
    private String remarks;

    /** Name of the uploaded file, shown and used for download. */
    @Column(name = "original_name", nullable = false, length = 255)
    private String originalName;

    /** Relative storage path, e.g. employeeDocument/ab12.pdf. */
    @Column(name = "storage_key", nullable = false, length = 500)
    private String storageKey;

    @Column(name = "thumbnail_path", length = 255)
    private String thumbnailPath;

    @Column(name = "thumbnail_type", length = 100)
    private String thumbnailType;

    @Column(name = "mime_type", length = 100)
    private String mimeType;

    @Column(name = "file_size_bytes")
    private Long fileSizeBytes;

    @Column(length = 64)
    private String sha256;

    @Column(name = "is_active", nullable = false)
    private boolean active = true;

    @Column(name = "uploaded_by", length = 30)
    private String uploadedBy;

    @Column(name = "uploaded_at", nullable = false)
    private LocalDateTime uploadedAt;
}
