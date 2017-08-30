package org.change.v2.p4.model.actions;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by dragos on 30.08.2017.
 */
public class P4ComplexAction extends P4Action {

    private List<P4ActionCall> actionList = new ArrayList<P4ActionCall>();

    public P4ComplexAction(String actionName) {
        super(P4ActionType.Complex, actionName);
    }

    public List<P4ActionCall> getActionList() {
        return actionList;
    }

    @Override
    public String toString() {
        return "P4ComplexAction{name=" + this.getActionName() + "," +
                "actionList=" + actionList +
                '}';
    }
}
