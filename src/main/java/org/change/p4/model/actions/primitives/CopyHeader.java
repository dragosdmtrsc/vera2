package org.change.v2.p4.model.actions.primitives;
import org.change.v2.p4.model.actions.*;
import java.util.Collections;
import java.util.List;

public class CopyHeader extends P4Action {
    public CopyHeader() {  super(P4ActionType.CopyHeader, "copy_header"); init(); }

    @Override
    public List<P4ActionParameter> getParameterList() {
        return Collections.unmodifiableList(super.getParameterList());
    }
    protected void init() {
        super.getParameterList().add(new P4ActionParameter(1, "destination"));
        super.getParameterList().add(new P4ActionParameter(1, "source"));

    }

}