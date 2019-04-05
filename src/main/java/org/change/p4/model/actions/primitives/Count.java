package org.change.v2.p4.model.actions.primitives;
import org.change.v2.p4.model.actions.*;
import java.util.Collections;
import java.util.List;

public class Count extends P4Action {
    public Count() {  super(P4ActionType.Count, "count"); init(); }

    @Override
    public List<P4ActionParameter> getParameterList() {
        return Collections.unmodifiableList(super.getParameterList());
    }
    protected void init() {
        super.getParameterList().add(new P4ActionParameter(32, "counter_ref"));
        super.getParameterList().add(new P4ActionParameter(16, "index"));

    }

}