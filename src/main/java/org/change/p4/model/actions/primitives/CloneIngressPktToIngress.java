package org.change.p4.model.actions.primitives;

import org.change.p4.model.actions.P4Action;
import org.change.p4.model.actions.P4ActionParameter;
import org.change.p4.model.actions.P4ActionType;

import java.util.Collections;
import java.util.List;

public class CloneIngressPktToIngress extends P4Action {
  public CloneIngressPktToIngress() {
    super(P4ActionType.CloneIngressPktToIngress, "clone_ingress_pkt_to_ingress");
    init();
  }

  @Override
  public List<P4ActionParameter> getParameterList() {
    return Collections.unmodifiableList(super.getParameterList());
  }

  protected void init() {
    super.getParameterList().add(new P4ActionParameter(16, "clone_spec"));
    super.getParameterList().add(new P4ActionParameter(8, "field_list"));

  }

}