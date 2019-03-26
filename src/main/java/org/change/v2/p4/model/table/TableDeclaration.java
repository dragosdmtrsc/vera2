package org.change.v2.p4.model.table;

import org.change.v2.p4.model.actions.ActionRegistrar;
import org.change.v2.p4.model.actions.P4Action;
import org.change.v2.p4.model.actions.P4ActionProfile;
import org.change.v2.p4.model.actions.P4ActionType;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class TableDeclaration implements Comparable<TableDeclaration> {
    private String name;
    private List<TableMatch> matches = new ArrayList<>();
    private List<P4Action> allowedActions = new ArrayList<>();
    private P4ActionProfile p4ActionProfile = null;
    public TableDeclaration(String name) {
        assert name != null;
        this.name = name;
    }
    public TableDeclaration addMatch(TableMatch tableMatch) {
        matches.add(tableMatch);
        return this;
    }
    public TableDeclaration addAction(P4Action action) {
        allowedActions.add(action);
        return this;
    }
    public TableDeclaration addAction(String action) {
        P4Action act = new P4Action(P4ActionType.Complex);
        act.setActionName(action);
        return addAction(act);
    }
    public TableDeclaration setProfile(P4ActionProfile profile) {
        p4ActionProfile = profile;
        return this;
    }
    public TableDeclaration setProfile(String profile) {
        p4ActionProfile = new P4ActionProfile(profile);
        return this;
    }
    public boolean hasProfile() {
        return p4ActionProfile != null;
    }
    public P4ActionProfile actionProfile() {
        return p4ActionProfile;
    }

    public String getName() {
        return name;
    }
    public List<TableMatch> getMatches() {
        return matches;
    }
    public List<P4Action> getAllowedActions() {
        return allowedActions;
    }
    @Override
    public int compareTo(TableDeclaration o) {
        return name.compareTo(o.name);
    }
}
