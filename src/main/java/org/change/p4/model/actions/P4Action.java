package org.change.p4.model.actions;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Created by dragos on 30.08.2017.
 */
public class P4Action {
  private String actionName;
  private P4ActionType actionType;
  private List<P4ActionParameter> parameterList = new ArrayList<P4ActionParameter>();

  public P4Action(P4ActionType complex) {
    this(complex, complex.toString());
  }

  public P4Action(P4ActionType type, String name) {
    this.actionType = type;
    this.actionName = name;
  }

  public String getActionName() {
    return actionName;
  }

  public P4ActionType getActionType() {
    return actionType;
  }

  public void setActionName(String actionName) {
    this.actionName = actionName;
  }

  public void setActionType(P4ActionType actionType) {
    this.actionType = actionType;
  }

  public List<P4ActionParameter> getParameterList() {
    return parameterList;
  }

  @Override
  public String toString() {
    return actionName + "(" + parameterList +
            ')';
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    P4Action p4Action = (P4Action) o;
    return actionName.equals(p4Action.actionName);
  }

  @Override
  public int hashCode() {
    return Objects.hash(actionName);
  }
}
