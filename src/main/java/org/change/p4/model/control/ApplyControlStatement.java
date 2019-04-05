package org.change.p4.model.control;

import org.change.p4.model.ControlBlock;

public class ApplyControlStatement implements ControlStatement {
  private ControlBlock block;
  private int nr = 0;

  public ApplyControlStatement(String controlName, int nr) {
    block = new ControlBlock(controlName);
    this.nr = nr;
  }

  public ControlBlock getBlock() {
    return block;
  }

  public ApplyControlStatement setBlock(ControlBlock block) {
    this.block = block;
    return this;
  }

  public int getNr() {
    return nr;
  }

  @Override
  public String toString() {
    return getBlock().getName() + "_" + getNr() + "()";
  }
}
