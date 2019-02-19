package org.change.v2.p4.model.parser;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * Created by dragos on 12.09.2017.
 */
public class ReturnSelectStatement extends Statement {
    public ReturnSelectStatement add(CaseEntry caseEntry) {
        caseEntryList.add(caseEntry);
        return this;
    }

    private List<CaseEntry> caseEntryList = new ArrayList<CaseEntry>();

    public Collection<CaseEntry> getCaseEntryList() {
        return caseEntryList;
    }

    @Override
    public String toString() {
        CaseEntry h = caseEntryList.get(0);
        StringBuilder crt = new StringBuilder("case(");
        boolean first= true;
        for (Expression e : h.getExpressions()) {
            if (!first)
                crt.append(",");
            first = false;
            crt.append(e.toString());
        }
        crt.append(")");
        return crt.toString();
    }
}
