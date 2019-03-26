package org.change.v2.p4.model.control;

import java.util.ArrayList;
import java.util.List;

public class BlockStatement implements ControlStatement {
    private List<ControlStatement> statements = new ArrayList<>();
    public List<ControlStatement> getStatements() {
        return statements;
    }
    public BlockStatement addStatement(ControlStatement stat) {
        statements.add(stat);
        return this;
    }
}
