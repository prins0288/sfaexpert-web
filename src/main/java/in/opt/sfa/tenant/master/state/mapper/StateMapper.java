package in.opt.sfa.tenant.master.state.mapper;

import in.opt.sfa.tenant.master.state.dto.StateDto;
import in.opt.sfa.tenant.master.state.entity.State;
import org.springframework.stereotype.Component;

@Component
public class StateMapper {

    public StateDto toDto(State e) {
        StateDto d = new StateDto();
        d.setOid(e.getOid());
        d.setStateCode(e.getStateCode());
        d.setStateName(e.getStateName());
        d.setCountryOid(e.getCountryOid());
        d.setStatus(e.getStatus());
        d.setCountryName(e.getCountryName());
        return d;
    }

    public State toEntity(StateDto d) {
        State e = new State();
        e.setOid(d.getOid());
        e.setStateCode(d.getStateCode());
        e.setStateName(d.getStateName());
        e.setCountryOid(d.getCountryOid());
        e.setStatus(d.getStatus());
        return e;
    }
}
