package org.change.p4.model.control;

import org.change.p4.model.table.TableDeclaration;

import java.util.ArrayList;
import java.util.List;

public class ApplyAndSelectTableStatement implements ControlStatement {
    private TableDeclaration table;
    private int nr;
    public ApplyAndSelectTableStatement(String tableName, int nr) {
        assert tableName != null;
        this.table = new TableDeclaration(tableName);
        this.nr = nr;
    }
    public boolean isFullyCovered() {
        if (isHitMiss) {
            boolean missFound = false;
            boolean hitFound = false;
            for (TableCaseEntry tce : caseEntries) {
                if (tce.miss()) missFound = true;
                else if (tce.hit()) hitFound = true;
                else if (tce.defaultCase()) hitFound = missFound = true;
            }
            return missFound && hitFound;
        } else {
            boolean defaultFound = false;
            for (TableCaseEntry tce : caseEntries) {
                if (tce.defaultCase()) return true;
            }
        }
        return false;
    }
    private void resolveDefaults() {
        TableCaseEntry theDefault = null;
        List<TableCaseEntry> others = new ArrayList<>();
        for (TableCaseEntry entry : caseEntries) {
            if (entry.defaultCase()) {
                theDefault = entry;
            } else {
                others.add(entry);
            }
        }
        if (theDefault != null)
            theDefault.setNegated(others);
    }
    public void createDefault() {
        if (!isFullyCovered()) {
            caseEntries.add(TableCaseEntry.fromString("default"));
        }
        resolveDefaults();
    }

    public int getNr() {
        return nr;
    }

    private boolean isHitMiss;
    private List<TableCaseEntry> caseEntries = new ArrayList<>();
    public ApplyAndSelectTableStatement addEntry(TableCaseEntry tce) {
        if (caseEntries.isEmpty()) {
            isHitMiss = tce.isHitMiss();
        } else {
            if (isHitMiss != tce.isHitMiss()) {
                throw new IllegalStateException("all case entries must be either hit/miss" +
                        "or action cases");
            }
        }
        caseEntries.add(tce);
        return this;
    }

    public List<TableCaseEntry> getCaseEntries() {
        return caseEntries;
    }

    public boolean isHitMiss() {
        return isHitMiss;
    }

    public TableDeclaration getTable() {
        return table;
    }
    public ApplyAndSelectTableStatement setTable(TableDeclaration table) {
        this.table = table;
        return this;
    }

    @Override
    public String toString() {
        return getTable().getName() + "_" + getNr() + ".apply()";
    }
}
