package org.change.p4.model;

import org.change.p4.model.control.BlockStatement;
import org.change.p4.model.table.TableDeclaration;

import java.util.SortedMap;
import java.util.TreeMap;

public class ControlBlock implements Comparable<ControlBlock> {
  private String name;
  private SortedMap<TableDeclaration, Integer> tables = new TreeMap<>();
  // issue new id here
  private SortedMap<ControlBlock, Integer> controlBlocks = new TreeMap<>();

  private BlockStatement statement = null;

  public ControlBlock setStatement(BlockStatement statement) {
    this.statement = statement;
    return this;
  }

  public ControlBlock(String name) {
    assert name != null;
    this.name = name;
  }

  public int newControlCall(ControlBlock block) {
    int crt = controlBlocks.getOrDefault(block, 0);
    controlBlocks.put(block, crt + 1);
    return crt;
  }

  public int newControlCall(String block) {
    return newControlCall(new ControlBlock(block));
  }

  public int newTableCall(TableDeclaration td) {
    int crt = tables.getOrDefault(td, 0);
    tables.put(td, crt + 1);
    return crt;
  }

  public int newTableCall(String table) {
    return newTableCall(new TableDeclaration(table));
  }

  public Iterable<TableDeclaration> getTables() {
    return tables.keySet();
  }

  public String getName() {
    return name;
  }

  public BlockStatement getStatement() {
    return statement;
  }

  @Override
  public int compareTo(ControlBlock o) {
    return name.compareTo(o.name);
  }
}
