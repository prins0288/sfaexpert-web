package in.opt.sfa.tenant.master.meetingtype.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
public class MeetingTypeMasterDto {
    private Long id;
    private String meetingTypeCode;
    private String meetingTypeName;
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
