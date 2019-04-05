package org.change.p4.model.control;

import org.change.p4.model.table.TableDeclaration;

import java.util.List;

public class TableCaseEntry implements ControlStatement {
  private boolean isHit = false, isMiss = false, isDefault = false;
  private String actionName = null;
  private ControlStatement statement;

  private TableCaseEntry() {
  }

  private TableDeclaration tableDeclaration;

  private List<TableCaseEntry> negated;

  void setNegated(List<TableCaseEntry> negated) {
    this.negated = negated;
  }

  public TableCaseEntry setTableDeclaration(TableDeclaration tableDeclaration) {
    this.tableDeclaration = tableDeclaration;
    return this;
  }

  public TableDeclaration getTableDeclaration() {
    return tableDeclaration;
  }

  public static TableCaseEntry fromString(String nm) {
    assert nm != null;
    TableCaseEntry tce = new TableCaseEntry();
    if ("hit".equals(nm))
      tce.isHit = true;
    else if ("miss".equals(nm))
      tce.isMiss = true;
    else if ("default".equals(nm))
      tce.isDefault = true;
    else tce.actionName = nm;
    return tce;
  }

  public static TableCaseEntry from(String nm, BlockStatement statement) {
    return fromString(nm).with(statement);
  }

  public TableCaseEntry with(BlockStatement statement) {
    this.statement = statement;
    return this;
  }

  public boolean isHitMiss() {
    return isHit || isMiss;
  }

  public boolean isActionCase() {
    return !isHitMiss();
  }

  public boolean hit() {
    return isHit;
  }

  public boolean miss() {
    return isMiss;
  }

  public Iterable<TableCaseEntry> getNegated() {
    return negated;
  }

  public boolean defaultCase() {
    return isDefault;
  }

  public String action() {
    return actionName;
  }

  public ControlStatement getStatement() {
    return statement;
  }

  @Override
  public String toString() {
    if (isHitMiss()) {
      if (isHit) return "hit";
      else return "miss";
    } else {
      if (isDefault) return "default";
      else return actionName;
    }
  }
}
