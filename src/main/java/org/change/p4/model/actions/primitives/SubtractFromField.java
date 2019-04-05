package org.change.p4.model.actions.primitives;
import org.change.p4.model.actions.*;
import java.util.Collections;
import java.util.List;

public class SubtractFromField extends P4Action {
    public SubtractFromField() {  super(P4ActionType.SubtractFromField, "subtract_from_field"); init(); }

    @Override
    public List<P4ActionParameter> getParameterList() {
        return Collections.unmodifiableList(super.getParameterList());
    }
    protected void init() {
        super.getParameterList().add(new P4ActionParameter(4, "dest"));
        super.getParameterList().add(new P4ActionParameter(20, "value"));

    }

}