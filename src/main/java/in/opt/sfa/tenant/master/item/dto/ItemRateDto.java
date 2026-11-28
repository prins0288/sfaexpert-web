package in.opt.sfa.tenant.master.item.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;

/** One rate row: state (null = All India) x effective dates. */
@Getter
@Setter
public class ItemRateDto {
    private Long id;
    private Long stateId;
    private String stateName;          // read-only
    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate effectiveFrom;
    /** Blank = current rate; filled automatically when a newer rate for the same state starts. */
    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate effectiveTo;
    private BigDecimal pts;
    private BigDecimal ptr;
    private BigDecimal mrp;
    private BigDecimal purchaseRate;
    private BigDecimal sampleRate;
    private BigDecimal discountPct;
    private Boolean taxIncluded;
    private Boolean status;
    private String legacyItemId;       // read-only
}
