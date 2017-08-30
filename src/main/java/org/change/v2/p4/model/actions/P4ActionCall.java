package org.change.v2.p4.model.actions;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Created by dragos on 30.08.2017.
 */
public class P4ActionCall {
    private P4Action p4Action;
    private List<P4ParameterInstance> p4ParameterInstances = new ArrayList<P4ParameterInstance>();

    public P4Action getP4Action() {
        return p4Action;
    }

    public P4ActionCall(P4Action p4Action) {
        this.p4Action = p4Action;
    }
    public P4ActionCall addParameter(P4ParameterInstance instance) {
        this.p4ParameterInstances.add(instance);
        return this;
    }

    public Iterable<P4ParameterInstance> parameterInstances() {
        return Collections.unmodifiableCollection(p4ParameterInstances);
    }

    @Override
    public String toString() {
        return "P4ActionCall{" +
                "p4Action=" + p4Action +
                ", p4ParameterInstances=" + p4ParameterInstances +
                '}';
    }
}
