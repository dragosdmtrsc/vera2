package org.change.v2.p4.model;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by dragos on 31.08.2017.
 */
public class FlowInstance {
    private List<Object> matchParams = new ArrayList<Object>();
    private String fireAction = "";
    private List<Object> actionParams = new ArrayList<Object>();
    private String table;
    private int priority = -1;

    public int getPriority() {
        return priority;
    }

    public FlowInstance setPriority(int priority) {
        this.priority = priority;
        return this;
    }

    public String getTable() {
        return table;
    }

    public FlowInstance setTable(String table) {
        this.table = table;
        return this;
    }

    public List<Object> getMatchParams() {
        return matchParams;
    }

    public FlowInstance addMatchParams(Object matchParams) {
        this.matchParams.add(matchParams);
        return this;
    }

    public String getFireAction() {
        return fireAction;
    }

    public FlowInstance setFireAction(String fireAction) {
        this.fireAction = fireAction;
        return this;
    }

    public List<Object> getActionParams() {
        return actionParams;
    }

    public FlowInstance addActionParams(Object matchParams) {
        this.actionParams.add(matchParams);
        return this;
    }
}
