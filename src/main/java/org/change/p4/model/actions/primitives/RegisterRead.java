package org.change.p4.model.actions.primitives;

import org.change.p4.model.actions.P4Action;
import org.change.p4.model.actions.P4ActionParameter;
import org.change.p4.model.actions.P4ActionType;

import java.util.Collections;
import java.util.List;

public class RegisterRead extends P4Action {
  public RegisterRead() {
    super(P4ActionType.RegisterRead, "register_read");
    init();
  }

  @Override
  public List<P4ActionParameter> getParameterList() {
    return Collections.unmodifiableList(super.getParameterList());
  }

  protected void init() {
    super.getParameterList().add(new P4ActionParameter(4, "dest"));
    super.getParameterList().add(new P4ActionParameter(128, "register_ref"));
    super.getParameterList().add(new P4ActionParameter(16, "index"));

  }

}