package org.change.p4.model.actions.primitives;

import org.change.p4.model.actions.P4Action;
import org.change.p4.model.actions.P4ActionParameter;
import org.change.p4.model.actions.P4ActionType;

import java.util.Collections;
import java.util.List;

public class GenerateDigest extends P4Action {
  public GenerateDigest() {
    super(P4ActionType.GenerateDigest, "generate_digest");
    init();
  }

  @Override
  public List<P4ActionParameter> getParameterList() {
    return Collections.unmodifiableList(super.getParameterList());
  }

  protected void init() {
    super.getParameterList().add(new P4ActionParameter(16, "receiver"));
    super.getParameterList().add(new P4ActionParameter(8, "field_list"));

  }

}