package org.change.v2.p4.model.control;

import org.change.v2.p4.model.table.TableDeclaration;

public class ApplyTableStatement implements ControlStatement {
    private TableDeclaration table;
    private int nr = 0;
    public ApplyTableStatement(String tableName, int nr) {
        assert tableName != null;
        table = new TableDeclaration(tableName);
        this.nr = nr;
    }
    public TableDeclaration getTable() {
        return table;
    }
    public ApplyTableStatement setTable(TableDeclaration table) {
        this.table = table;
        return this;
    }

    public int getNr() {
        return nr;
    }
    public String toString() {
        return getTable().getName() + "_" + nr + ".apply()";
    }
}
