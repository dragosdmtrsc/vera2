package org.change.v2.p4.model.parser;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by dragos on 18.09.2017.
 */
public class CaseNotEntry extends Statement {
    private List<CaseEntry> caseEntryList = new ArrayList<CaseEntry>();

    public CaseNotEntry addCaseEntry(CaseEntry ce) {
        this.caseEntryList.add(ce);
        return this;
    }

    public List<CaseEntry> getCaseEntryList() {
        return caseEntryList;
    }
}
