package org.change.p4.model.parser;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by dragos on 18.09.2017.
 */
public class CaseNotEntry extends Statement {
  private List<CaseEntry> caseEntryList = new ArrayList<CaseEntry>();
  private ReturnStatement returnStatement;

  public CaseNotEntry addCaseEntry(CaseEntry ce) {
    this.caseEntryList.add(ce);
    return this;
  }

  public CaseNotEntry(CaseNotEntry cne) {
    this.caseEntryList = cne.caseEntryList;
    this.returnStatement = cne.returnStatement;
  }

  public CaseNotEntry() {
  }

  public ReturnStatement getReturnStatement() {
    return returnStatement;
  }

  public CaseNotEntry setReturnStatement(ReturnStatement returnStatement) {
    this.returnStatement = returnStatement;
    return this;
  }

  public List<CaseEntry> getCaseEntryList() {
    return caseEntryList;
  }
}
