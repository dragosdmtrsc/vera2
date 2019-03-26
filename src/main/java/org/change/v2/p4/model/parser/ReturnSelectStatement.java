package org.change.v2.p4.model.parser;

import org.change.v2.p4.model.control.exp.P4Expr;

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

    public ReturnSelectStatement(ReturnSelectStatement rss) {
        caseEntryList = rss.caseEntryList;
    }
    public ReturnSelectStatement() {}
    @Override
    public String toString() {
        CaseEntry h = caseEntryList.get(0);
        StringBuilder crt = new StringBuilder("case(");
        boolean first= true;
        for (P4Expr e : h.getBVExpressions()) {
            if (!first)
                crt.append(",");
            first = false;
            crt.append(e.toString());
        }
        crt.append(")");
        return crt.toString();
    }
}
