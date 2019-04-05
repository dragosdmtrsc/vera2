package org.change.p4.model.actions;

import org.change.p4.control.types.P4Type;
import org.change.p4.model.parser.Expression;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Created by dragos on 30.08.2017.
 */
public class P4ActionCall {
  public interface Param {
  }

  public static class ParamExpression implements Param {
    private Expression expression;

    public ParamExpression(Expression expression) {
      this.expression = expression;
    }

    public Expression getExpression() {
      return expression;
    }

    public ParamExpression setExpression(Expression expression) {
      this.expression = expression;
      return this;
    }

    private P4Type castTo;

    public P4Type getCastTo() {
      return castTo;
    }

    public ParamExpression setCastTo(P4Type castTo) {
      this.castTo = castTo;
      return this;
    }

    @Override
    public String toString() {
      return "parm(" + expression.toString() + ")";
    }
  }

  private P4Action p4Action;
  private List<P4ParameterInstance> p4ParameterInstances = new ArrayList<>();
  private List<Param> parms = new ArrayList<>();

  public P4Action getP4Action() {
    return p4Action;
  }

  public P4ActionCall setAction(P4Action p4Action) {
    this.p4Action = p4Action;
    return this;
  }

  public P4ActionCall(P4Action p4Action) {
    this.p4Action = p4Action;
  }

  public P4ActionCall addParameter(P4ParameterInstance instance) {
    p4ParameterInstances.add(instance);
    return this;
  }

  public P4ActionCall addParameter(Expression instance) {
    parms.add(new ParamExpression(instance));
    return this;
  }

  public List<Param> params() {
    return parms;
  }

  public Iterable<P4ParameterInstance> parameterInstances() {
    return Collections.unmodifiableCollection(p4ParameterInstances);
  }

  @Override
  public String toString() {
    StringBuilder crt = new StringBuilder(p4Action.getActionName() + "(");
    boolean first = true;
    for (Param p : params()) {
      if (!first)
        crt.append(",");
      first = false;
      if (p instanceof ParamExpression) {
        ParamExpression pe = (ParamExpression) p;
        crt.append(pe.expression.toString());
      }
    }
    crt.append(")");
    return crt.toString();
  }
}
