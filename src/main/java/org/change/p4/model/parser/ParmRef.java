package org.change.p4.model.parser;

import org.change.p4.model.actions.P4Action;
import org.change.p4.model.actions.P4ActionParameter;

public class ParmRef extends Ref {
  private P4Action action;
  private P4ActionParameter parameter;

  public ParmRef(P4Action a, P4ActionParameter parameter) {
    this.parameter = parameter;
    this.action = a;
    setPath(parameter.getParamName());
  }

  public P4Action getAction() {
    return action;
  }

  public P4ActionParameter getParameter() {
    return parameter;
  }

  public ParmRef setParameter(P4ActionParameter parameter) {
    this.parameter = parameter;
    return this;
  }
}