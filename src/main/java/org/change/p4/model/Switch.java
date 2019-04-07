package org.change.p4.model;

import generated.parser.p4.P4GrammarLexer;
import generated.parser.p4.P4GrammarParser;
import org.antlr.v4.runtime.CharStream;
import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.tree.ParseTreeWalker;
import org.change.p4.P4GrammarListener;
import org.change.p4.model.actions.ActionRegistrar;
import org.change.p4.model.actions.P4Action;
import org.change.p4.model.actions.P4ActionProfile;
import org.change.p4.model.fieldlists.FieldList;
import org.change.p4.model.parser.FieldRef;
import org.change.p4.model.parser.HeaderRef;
import org.change.p4.model.parser.IndexedHeaderRef;
import org.change.p4.model.parser.State;
import org.change.p4.model.table.TableDeclaration;
import org.change.p4.model.table.TableMatch;

import java.io.IOException;
import java.util.*;

/**
 * Created by dragos on 31.08.2017. Represents a switch specification,
 * which is basically a holder for info regarding the P4 program comprised therein
 */
public class Switch {

  public Switch() {
    //start counting field lists from 1,
    //zero is reserved to mean that no change is to be performed
    idsToFieldLists.add(null);
    fl2ids.put("", 0);
  }

  // the good and the new. All declarations go here:
  private Map<String, Calculation> calculationDeclarations = new HashMap<>();
  private Map<String, TableDeclaration> tableDeclarations = new HashMap<>();
  private Map<String, ControlBlock> controlBlockDeclarations = new HashMap<>();
  private Map<String, Header> headerDeclarations = new HashMap<>();
  private Map<String, HeaderInstance> instances = new HashMap<>();
  private Map<String, RegisterSpecification> registerSpecificationMap = new HashMap<>();
  private ActionRegistrar actionRegistrar = new ActionRegistrar();
  private Map<String, P4ActionProfile> actionProfiles = new HashMap<>();
  private Map<String, State> parserStates = new HashMap<>();
  private Map<String, org.change.p4.model.fieldlists.FieldList> fieldListMap =
          new HashMap<>();
  private Map<String, List<FieldRef>> fieldListExpansion = new HashMap<>();

  private List<org.change.p4.model.fieldlists.FieldList> idsToFieldLists =
          new ArrayList<>();
  private Map<String, Integer> fl2ids = new HashMap<>();

  public Iterable<ControlBlock> controlBlocks() {
    return controlBlockDeclarations.values();
  }

  public Iterable<TableDeclaration> tables() {
    return tableDeclarations.values();
  }

  public Iterable<P4Action> actions() {
    return actionRegistrar.getDeclaredActions();
  }

  public Calculation calculation(String name) {
    return calculationDeclarations.getOrDefault(name, null);
  }

  public org.change.p4.model.fieldlists.FieldList fieldList(String fieldList) {
    return fieldListMap.getOrDefault(fieldList, null);
  }

  public P4Action action(String actionName) {
    return actionRegistrar.getAction(actionName);
  }

  public RegisterSpecification register(String registerName) {
    return registerSpecificationMap.getOrDefault(registerName, null);
  }

  public Switch declareCalculation(Calculation calc, boolean allowOverride) {
    Calculation old = calculationDeclarations.put(calc.getName(), calc);
    if (old != null && !allowOverride)
      throw new IllegalStateException("calculation " + old + " already declared");
    return this;
  }

  public Switch declareHeader(Header hdr, boolean allowOverride) {
    Header old = headerDeclarations.put(hdr.getName(), hdr);
    if (!allowOverride && old != null) {
      throw new IllegalStateException("header " + hdr.getName() + " already declared");
    }
    return this;
  }

  public Switch declareAction(P4Action action) {
    actionRegistrar.register(action);
    return this;
  }

  public Switch declareHeaderInstance(HeaderInstance instance, boolean allowOverride) {
    Header layout = headerDeclarations.getOrDefault(instance.getLayout().getName(),
            null);
    if (layout == null) {
      // first declare a header type, then the instance => resolution
      // is performed straight up
      throw new IllegalStateException(instance.getLayout().getName() +
              " header type not declared");
    }
    if (layout != instance.getLayout()) {
      // the switch holds the ground truth
      instance.setLayout(layout);
    }
    HeaderInstance old = instances.put(instance.getName(), instance);
    if (!allowOverride && old != null)
      throw new IllegalStateException("header instance " + old + " already declared");
    return this;
  }

  public Switch declareControlBlock(ControlBlock block, boolean allowOverride) {
    ControlBlock old = controlBlockDeclarations.put(block.getName(), block);
    if (old != null && !allowOverride)
      throw new IllegalStateException("control block " + old + " already declared");
    return this;
  }

  public ControlBlock controlBlock(String controlName) {
    return controlBlockDeclarations.get(controlName);
  }

  public TableDeclaration table(String table) {
    return tableDeclarations.get(table);
  }

  public Switch declareTable(TableDeclaration tableDeclaration, boolean allowOverride) {
    TableDeclaration old = tableDeclarations.put(tableDeclaration.getName(), tableDeclaration);
    if (old != null && !allowOverride)
      throw new IllegalStateException("table " + old + " already declared");
    return this;
  }

  public Switch declareActionProfile(P4ActionProfile actProf, boolean allowOverride) {
    P4ActionProfile old = actionProfiles.put(actProf.getName(), actProf);
    if (old != null && !allowOverride)
      throw new IllegalStateException("action profile " + old + " already declared");
    return this;
  }

  public HeaderInstance intrinsic() {
    HeaderInstance inst = getInstance("intrinsic_metadata");
    if (inst != null) {
      return inst;
    }
    return null;
  }

  public boolean hasIntrinsic() {
    return intrinsic() != null;
  }

  public int mcastGrpWidth() {
    HeaderInstance intr = intrinsic();
    if (intr != null) {
      Field fMcastGrp = intr.getLayout().getField("mcast_grp");
      if (fMcastGrp != null)
        return fMcastGrp.getLength();
    }
    return 0;
  }

  public Switch declareParserState(State state, boolean allowOverride) {
    State old = parserStates.put(state.getName(), state);
    if (old != null && !allowOverride)
      throw new IllegalStateException("parser state " + old + " already declared");
    return this;
  }

  public Switch declareFieldList(org.change.p4.model.fieldlists.FieldList fieldList, boolean allowOverride) {
    org.change.p4.model.fieldlists.FieldList old = fieldListMap.put(fieldList.getName(), fieldList);
    if (old != null && !allowOverride)
      throw new IllegalStateException("field list " + old + " already declared");
    idsToFieldLists.add(fieldList);
    fl2ids.put(fieldList.getName(), idsToFieldLists.size() - 1);
    return this;
  }

  public FieldList getFieldListByIndex(int idx) {
    if (idx < 1 || idx >= idsToFieldLists.size()) {
      return null;
    }
    return idsToFieldLists.get(idx);
  }

  public int getFieldListIndex(String fieldList) {
    return fl2ids.getOrDefault(fieldList, -1);
  }
  public Iterable<Integer> allowedFieldListIndices() {
    return fl2ids.values();
  }
  public int nrOfFieldLists() {
    return fl2ids.size();
  }

  public List<FieldRef> expandFieldList(String pfieldList) {
    if (fieldListExpansion.size() != fieldListMap.size()) {
      for (Map.Entry<String, FieldList> entry : fieldListMap.entrySet()) {
        FieldList fl = entry.getValue();
        String fieldList = entry.getKey();
        List<FieldRef> refs = new ArrayList<>();
        for (FieldList.Entry fe : fl.getEntries()) {
          if (fe instanceof FieldList.FieldRefEntry) {
            FieldRef fr = ((FieldList.FieldRefEntry) fe).getFieldRef();
            HeaderInstance inst = getInstance(fr.getHeaderRef().getPath());
            fr.getHeaderRef().setInstance(inst);
            fr.setFieldReference(inst.getLayout().getField(fr.getField()));
            refs.add(fr);
          } else if (fe instanceof FieldList.HeaderRefEntry) {
            HeaderInstance href = getInstance(((FieldList.HeaderRefEntry) fe).getHeaderRef().getPath());
            if (href instanceof ArrayInstance) {
              ArrayInstance ai = (ArrayInstance) href;
              for (int i = 0; i != ai.getLength(); ++i) {
                HeaderRef h = new IndexedHeaderRef().setIndex(i).setInstance(ai);
                for (Field f : href.getLayout().getFields()) {
                  FieldRef fr = new FieldRef()
                          .setField(f.getName()).setFieldReference(f).setHeaderRef(h);
                  refs.add(fr);
                }
              }
            } else {
              HeaderRef h = new HeaderRef().setInstance(href).setPath(href.getName());
              for (Field f : href.getLayout().getFields()) {
                FieldRef fr = new FieldRef()
                        .setField(f.getName()).setFieldReference(f).setHeaderRef(h);
                refs.add(fr);
              }
            }
          } else {
            //TODO: is there anything else that we may handle?
          }
        }
        fieldListExpansion.put(fieldList, refs);
      }
    }
    return fieldListExpansion.get(pfieldList);
  }

  public Switch declareRegister(RegisterSpecification registerSpecification, boolean allowOverride) {
    RegisterSpecification old = registerSpecificationMap.put(registerSpecification.getName(), registerSpecification);
    if (old != null && !allowOverride)
      throw new IllegalStateException("register " + old + " already declared");
    return this;
  }

  // the bad and the old
  public Map<String, RegisterSpecification> getRegisterSpecificationMap() {
    return registerSpecificationMap;
  }

  public P4ActionProfile getProfile(String name) {
    if (actionProfiles.containsKey(name))
      return actionProfiles.get(name);
    return null;
  }

  public Collection<String> getActionProfiles() {
    return this.actionProfiles.keySet();
  }

  private Map<String, List<TableMatch>> matches = new HashMap<>();
  private Map<String, String> tableControl = new HashMap<>();

  public State getParserState(Object o) {
    return parserStates.get(o);
  }

  public boolean hasParserState(String o) {
    return parserStates.containsKey(o);
  }

  public Set<String> parserStates() {
    return parserStates.keySet();
  }

  public Iterable<State> states() {
    return parserStates.values();
  }

  public List<TableMatch> getTableMatches(String perTable) {
    if (this.matches.containsKey(perTable))
      return this.matches.get(perTable);
    return null;
  }

  public Field getField(String matchKey) {
    String[] spl = matchKey.split("\\.");
    String hdr = spl[0];
    HeaderInstance instance = getInstance(hdr);
    return instance.getLayout().getField(spl[1]);
  }

  public int getSize(String value) {
    String[] split = value.split("\\.");
    String hName = split[0];

    HeaderInstance instance = this.getInstance(hName);
    if (instance == null) {
      throw new IllegalArgumentException("Can't find header " + hName);
    }
    return instance.getLayout().getField(split[1]).getLength();
  }

  public Iterable<String> getDeclaredTables() {
    return this.matches.keySet();
  }

  public Switch addTableMatch(TableMatch tm) {
    if (!this.matches.containsKey(tm.getTable())) {
      this.matches.put(tm.getTable(), new ArrayList<>());
    }
    this.matches.get(tm.getTable()).add(tm);
    return this;
  }


  public ActionRegistrar getActionRegistrar() {
    return actionRegistrar;
  }

  public Map<String, org.change.p4.model.fieldlists.FieldList> getFieldListMap() {
    return fieldListMap;
  }

  public HeaderInstance getInstance(String o) {
    if (o.contains("[")) {
      o = o.split("\\[")[0];
    }
    return instances.getOrDefault(o, null);
  }

  public Iterable<HeaderInstance> getInstances() {
    return instances.values();
  }

  public Switch addHeaderInstance(HeaderInstance instance) {
    this.instances.put(instance.getName(), instance);
    return this;
  }

  public static Switch fromFile(String p4File) throws IOException {
    CharStream p4Input = CharStreams.fromFileName(p4File);
    P4GrammarLexer lexer = new P4GrammarLexer(p4Input);
    CommonTokenStream tokens = new CommonTokenStream(lexer);
    P4GrammarParser parser = new P4GrammarParser(tokens);
    P4GrammarParser.P4_programContext tree = parser.p4_program();
    ParseTreeWalker walker = new ParseTreeWalker();
    P4GrammarListener listener = new P4GrammarListener();

    walker.walk(listener, tree);
    return tree.switchSpec;
  }
}
