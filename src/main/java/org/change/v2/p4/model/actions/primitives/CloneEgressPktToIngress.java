package org.change.v2.p4.model.actions.primitives;
import org.change.v2.p4.model.actions.*;
import java.util.Collections;
import java.util.List;

public class CloneEgressPktToIngress extends P4Action {
    public CloneEgressPktToIngress() {  super(P4ActionType.CloneEgressPktToIngress, "clone_egress_pkt_to_ingress"); init(); }

    @Override
    public List<P4ActionParameter> getParameterList() {
        return Collections.unmodifiableList(super.getParameterList());
    }
    protected void init() {
        super.getParameterList().add(new P4ActionParameter(16, "clone_spec"));
        super.getParameterList().add(new P4ActionParameter(8, "field_list"));

    }

}