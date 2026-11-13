package in.opt.sfa.tenant.master.item.dto;

import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.List;

/** Create Multiple Sample: the ticked products (with pack size + PTS) and the states for the rate. */
@Getter
@Setter
public class SampleCreateRequest {
    private List<Line> items;
    /** States of the sample's rate; empty = one All-India rate. */
    private List<Long> stateIds;

    @Getter
    @Setter
    public static class Line {
        private Long productId;
        private String packSize;
        private BigDecimal pts;
    }
}
