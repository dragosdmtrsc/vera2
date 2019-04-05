package org.change.v2.p4.model.actions.primitives;
import org.change.v2.p4.model.actions.*;
import java.util.Collections;
import java.util.List;

public class ModifyFieldRngUniform extends P4Action {
    public ModifyFieldRngUniform() {  super(P4ActionType.ModifyFieldRngUniform, "modify_field_rng_uniform"); init(); }

    @Override
    public List<P4ActionParameter> getParameterList() {
        return Collections.unmodifiableList(super.getParameterList());
    }
    protected void init() {
        super.getParameterList().add(new P4ActionParameter(4, "dest"));
        super.getParameterList().add(new P4ActionParameter(20, "lower_bound"));
        super.getParameterList().add(new P4ActionParameter(20, "upper_bound"));

    }

}