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
}
