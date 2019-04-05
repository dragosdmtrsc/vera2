package org.change.v2.p4.model.actions.primitives;
import org.change.v2.p4.model.actions.*;
import java.util.Collections;
import java.util.List;

public class ModifyFieldWithHashBasedOffset extends P4Action {
    public ModifyFieldWithHashBasedOffset() {  super(P4ActionType.ModifyFieldWithHashBasedOffset, "modify_field_with_hash_based_offset"); init(); }

    @Override
    public List<P4ActionParameter> getParameterList() {
        return Collections.unmodifiableList(super.getParameterList());
    }
    protected void init() {
        super.getParameterList().add(new P4ActionParameter(4, "dest"));
        super.getParameterList().add(new P4ActionParameter(16, "base"));
        super.getParameterList().add(new P4ActionParameter(256, "field_list_calc"));
        super.getParameterList().add(new P4ActionParameter(16, "size"));

    }

}