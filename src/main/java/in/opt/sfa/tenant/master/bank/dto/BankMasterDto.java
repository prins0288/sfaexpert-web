package in.opt.sfa.tenant.master.bank.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
public class BankMasterDto {
    private Long id;
    private String bankCode;
    private String bankName;
    private String shortName;
    private String description;
    private Integer displayOrder;
    private Boolean isActive;

    private String createdBy;
    @JsonFormat(pattern = "dd-MM-yyyy")
    private LocalDateTime createdAt;
    private String updatedBy;
    @JsonFormat(pattern = "dd-MM-yyyy")
    private LocalDateTime updatedAt;
}
