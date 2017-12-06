package org.change.v2.p4.model.actions.primitives;
import org.change.v2.p4.model.actions.*;
import java.util.Collections;
import java.util.List;

public class Truncate extends P4Action {
    public Truncate() {  super(P4ActionType.Truncate, "truncate"); init(); }

    @Override
    public List<P4ActionParameter> getParameterList() {
        return Collections.unmodifiableList(super.getParameterList());
    }
    protected void init() {
        super.getParameterList().add(new P4ActionParameter(16, "length"));

    }

}