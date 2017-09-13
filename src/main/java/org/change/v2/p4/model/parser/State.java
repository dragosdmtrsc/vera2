package org.change.v2.p4.model.parser;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * Created by dragos on 12.09.2017.
 */
public class State {
    private List<Statement> statements = new ArrayList<Statement>();
    private String name;

    public State() {
    }

    public String getName() {
        return name;
    }

    public State setName(String name) {
        this.name = name;
        return this;
    }

    public State add(Statement statement) {
        statements.add(statement);
        return this;
    }

    public Collection<Statement> getStatements() {
        return statements;
    }
}
