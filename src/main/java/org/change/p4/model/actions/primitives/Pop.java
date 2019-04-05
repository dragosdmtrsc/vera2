package org.change.v2.p4.model.actions.primitives;
import org.change.v2.p4.model.actions.*;
import java.util.Collections;
import java.util.List;

public class Pop extends P4Action {
    public Pop() {  super(P4ActionType.Pop, "pop"); init(); }

    @Override
    public List<P4ActionParameter> getParameterList() {
        return Collections.unmodifiableList(super.getParameterList());
    }
    protected void init() {
        super.getParameterList().add(new P4ActionParameter(2, "array"));
        super.getParameterList().add(new P4ActionParameter(16, "count"));

    }

}