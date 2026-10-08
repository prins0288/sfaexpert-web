package in.opt.sfa.common.entity;

import in.opt.sfa.preference.converter.ColumnConfigListConverter;
import in.opt.sfa.preference.dto.ColumnConfig;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * A company's saved column layout for one grid (COMMON db, table
 * user_table_preferences) — shared by every user of that company.
 * company_code is always taken from the verified token, never from the client.
 */
@Entity
@Table(name = "user_table_preferences",
        uniqueConstraints = @UniqueConstraint(name = "uq_user_table_preferences",
                columnNames = {"company_code", "screen_key"}))
@Getter
@Setter
public class UserTablePreference {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "company_code", nullable = false, length = 64)
    private String companyCode;

    @Column(name = "screen_key", nullable = false, length = 100)
    private String screenKey;

    @Convert(converter = ColumnConfigListConverter.class)
    @Column(name = "column_config", nullable = false, columnDefinition = "json")
    private List<ColumnConfig> columnConfig = new ArrayList<>();

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
