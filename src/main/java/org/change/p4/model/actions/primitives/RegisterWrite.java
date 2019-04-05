package org.change.p4.model.actions.primitives;

import org.change.p4.model.actions.P4Action;
import org.change.p4.model.actions.P4ActionParameter;
import org.change.p4.model.actions.P4ActionType;

import java.util.Collections;
import java.util.List;

public class RegisterWrite extends P4Action {
  public RegisterWrite() {
    super(P4ActionType.RegisterWrite, "register_write");
    init();
  }

  @Override
  public List<P4ActionParameter> getParameterList() {
    return Collections.unmodifiableList(super.getParameterList());
  }

  protected void init() {
    super.getParameterList().add(new P4ActionParameter(128, "register_ref"));
    super.getParameterList().add(new P4ActionParameter(16, "index"));
    super.getParameterList().add(new P4ActionParameter(20, "value"));

  }

}