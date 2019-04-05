package org.change.v2.p4.model.control;

import org.change.v2.p4.model.control.exp.P4BExpr;

public class IfElseStatement implements ControlStatement {
    private P4BExpr condition;
    private ControlStatement ifbranch = null, elseBranch = null;

    public IfElseStatement(P4BExpr condition, ControlStatement ifbranch, ControlStatement elseBranch) {
        this.condition = condition;
        this.ifbranch = ifbranch;
        this.elseBranch = elseBranch;
    }
    public IfElseStatement(P4BExpr condition, ControlStatement ifbranch) {
        this.condition = condition;
        this.ifbranch = ifbranch;
    }

    public P4BExpr getCondition() {
        return condition;
    }

    public ControlStatement getIfbranch() {
        return ifbranch;
    }

    public ControlStatement getElseBranch() {
        return elseBranch;
    }
    public String toString() {
        return getCondition().toString();
    }
}
