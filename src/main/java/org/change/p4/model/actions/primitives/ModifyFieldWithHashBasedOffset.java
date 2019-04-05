package org.change.p4.model.actions.primitives;

import org.change.p4.model.actions.P4Action;
import org.change.p4.model.actions.P4ActionParameter;
import org.change.p4.model.actions.P4ActionType;

import java.util.Collections;
import java.util.List;

public class ModifyFieldWithHashBasedOffset extends P4Action {
  public ModifyFieldWithHashBasedOffset() {
    super(P4ActionType.ModifyFieldWithHashBasedOffset, "modify_field_with_hash_based_offset");
    init();
  }

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