package org.change.v2.p4.model.actions;

import org.change.v2.p4.model.actions.primitives.*;

import java.util.*;

/**
 * Created by dragos on 30.08.2017.
 */
public class ActionRegistrar {
    private List<P4Action> declaredActions = new ArrayList<P4Action>();
    private Map<String, P4Action> actionMap = new HashMap<String, P4Action>();


    public ActionRegistrar() {
        init();
    }

    protected void init() {
        this.register(new AddHeader());
        this.register(new CopyHeader());
        this.register(new RemoveHeader());
        this.register(new ModifyField());
        this.register(new AddToField());
        this.register(new Add());
        this.register(new SubtractFromField());
        this.register(new Subtract());
        this.register(new ModifyFieldWithHashBasedOffset());
        this.register(new ModifyFieldRngUniform());
        this.register(new BitAnd());
        this.register(new BitOr());
        this.register(new BitXor());
        this.register(new ShiftLeft());
        this.register(new ShiftRight());
        this.register(new Truncate());
        this.register(new Drop());
        this.register(new NoOp());
        this.register(new Push());
        this.register(new Pop());
        this.register(new Count());
        this.register(new ExecuteMeter());
        this.register(new RegisterRead());
        this.register(new RegisterWrite());
        this.register(new GenerateDigest());
        this.register(new Resubmit());
        this.register(new Recirculate());
        this.register(new CloneIngressPktToIngress());
        this.register(new CloneEgressPktToIngress());
        this.register(new CloneIngressPktToEgress());
        this.register(new CloneEgressPktToEgress());
    }

    public Iterable<P4Action> getDeclaredActions() {
        return Collections.unmodifiableCollection(declaredActions);
    }

    public P4Action getAction(String byName) {
        if (actionMap.containsKey(byName)) {
            return actionMap.get(byName);
        }
        return null;
    }

    public void register(P4Action action) {
        if (actionMap.containsKey(action.getActionName())) {
            return;
        }
        actionMap.put(action.getActionName(), action);
        declaredActions.add(action);
    }

}
