package org.change.v2.p4.model.actions.primitives;
import org.change.v2.p4.model.actions.*;
import java.util.Collections;
import java.util.List;

public class ShiftLeft extends P4Action {
    public ShiftLeft() {  super(P4ActionType.ShiftLeft, "shift_left"); init(); }

    @Override
    public List<P4ActionParameter> getParameterList() {
        return Collections.unmodifiableList(super.getParameterList());
    }
    protected void init() {
        super.getParameterList().add(new P4ActionParameter(4, "dest"));
        super.getParameterList().add(new P4ActionParameter(20, "value1"));
        super.getParameterList().add(new P4ActionParameter(20, "value2"));

    }

}