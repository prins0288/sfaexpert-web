package in.opt.sfa.tenant.menu.copy.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class MenuCopyTargetResultDto {
    private String companyCode;
    private int created;
    private int reused;
    private String error;

    public MenuCopyTargetResultDto() {}

    public MenuCopyTargetResultDto(String companyCode, int created, int reused, String error) {
        this.companyCode = companyCode;
        this.created = created;
        this.reused = reused;
        this.error = error;
    }
}
