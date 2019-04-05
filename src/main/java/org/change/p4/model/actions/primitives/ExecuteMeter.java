package org.change.v2.p4.model.actions.primitives;
import org.change.v2.p4.model.actions.*;
import java.util.Collections;
import java.util.List;

public class ExecuteMeter extends P4Action {
    public ExecuteMeter() {  super(P4ActionType.ExecuteMeter, "execute_meter"); init(); }

    @Override
    public List<P4ActionParameter> getParameterList() {
        return Collections.unmodifiableList(super.getParameterList());
    }
    protected void init() {
        super.getParameterList().add(new P4ActionParameter(64, "meter_ref"));
        super.getParameterList().add(new P4ActionParameter(16, "index"));
        super.getParameterList().add(new P4ActionParameter(4, "field"));

    }

}