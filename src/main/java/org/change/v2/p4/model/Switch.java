package org.change.v2.p4.model;

import generated.parse.p4.P4GrammarLexer;
import generated.parse.p4.P4GrammarParser;
import org.antlr.v4.runtime.CharStream;
import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.tree.ParseTreeWalker;
import org.change.parser.p4.P4GrammarListener;
import org.change.v2.analysis.processingmodels.Instruction;
import org.change.v2.p4.model.actions.ActionRegistrar;
import org.change.v2.p4.model.actions.P4Action;
import org.change.v2.p4.model.actions.P4ActionProfile;
import org.change.v2.p4.model.fieldlists.FieldList;
import org.change.v2.p4.model.parser.State;
import org.change.v2.p4.model.table.TableDeclaration;
import org.change.v2.p4.model.table.TableMatch;
import scala.Tuple2;
import scala.collection.JavaConversions;

import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Created by dragos on 31.08.2017. Represents a switch specification,
 * which is basically a holder for info regarding the P4 program comprised therein
 */
public class Switch {

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
    private Map<String, org.change.v2.p4.model.fieldlists.FieldList> fieldListMap =
            new HashMap<>();

    public Iterable<ControlBlock> controlBlocks() {
        return controlBlockDeclarations.values();
    }
    public Iterable<TableDeclaration> tables() {
        return tableDeclarations.values();
    }
    public Iterable<P4Action> actions() { return actionRegistrar.getDeclaredActions(); }
    public Calculation calculation(String name) {
        return calculationDeclarations.getOrDefault(name, null);
    }

    public org.change.v2.p4.model.fieldlists.FieldList fieldList(String fieldList) {
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

    public Switch declareParserState(State state, boolean allowOverride) {
        State old = parserStates.put(state.getName(), state);
        if (old != null && !allowOverride)
            throw new IllegalStateException("parser state " + old + " already declared");
        return this;
    }
    public Switch declareFieldList(org.change.v2.p4.model.fieldlists.FieldList fieldList, boolean allowOverride) {
        org.change.v2.p4.model.fieldlists.FieldList old = fieldListMap.put(fieldList.getName(), fieldList);
        if (old != null && !allowOverride)
            throw new IllegalStateException("field list " + old + " already declared");
        return this;
    }

    public Switch declareRegister(RegisterSpecification registerSpecification, boolean allowOverride) {
        RegisterSpecification old = registerSpecificationMap.put(registerSpecification.getName(), registerSpecification);
        if (old != null && !allowOverride)
            throw new IllegalStateException("register " + old + " already declared");
        return this;
    }

    // the bad and the old
    private Map<String, List<String>> allowedActions = new HashMap<>();

    private Map<scala.Tuple2<String, String>, Boolean> tableSelectors = null;
    public Map<String, RegisterSpecification> getRegisterSpecificationMap() {
        return registerSpecificationMap;
    }

    private Map<String, String> controlFlowLinks = null;
    private Map<String, Instruction> controlFlowInstructions = null;

    public P4ActionProfile getProfile(String name) {
        if (actionProfiles.containsKey(name))
            return actionProfiles.get(name);
        return null;
    }

    public Collection<String> getActionProfiles() {
        return this.actionProfiles.keySet();
    }

    public P4ActionProfile getProfileByTable(String table) {
        List<String> tableProfile = this.getAllowedActions(table);
        if (tableProfile != null && !tableProfile.isEmpty() && tableProfile.size() == 1) {
            return this.getProfile(tableProfile.get(0));
        } else {
            return null;
        }
    }
    public List<P4Action> getActionsPerProfile(String name) {
        P4ActionProfile profile = getProfile(name);
        return getActionsPerProfile(profile);
    }

    public List<P4Action> getActionsPerProfile(P4ActionProfile profile) {
        if (profile != null)
            return profile.getActions().stream().map(r -> this.getActionRegistrar().getAction(r)).
                    collect(Collectors.toList());
        else
            return null;
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

    public List<String> getAllowedActions(String perTable) {
        if (!allowedActions.containsKey(perTable))
            return Collections.emptyList();
        return allowedActions.get(perTable);
    }

    public Map<Tuple2<String, String>, Boolean> getTableSelectors() {
        return tableSelectors;
    }

    public Switch setTableSelectors(Map<Tuple2<String, String>, Boolean> tableSelectors) {
        this.tableSelectors = tableSelectors;
        return this;
    }
    public List<TableMatch> getTableMatches(String perTable) {
        if (this.matches.containsKey(perTable))
            return this.matches.get(perTable);
        return null;
    }
    public Field getField(String matchKey) {
        String []spl = matchKey.split("\\.");
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

    public Map<String, String> getControlFlowLinks() {
        return controlFlowLinks;
    }

    public Switch setControlFlowLinks(Map<String, String> controlFlowLinks) {
        this.controlFlowLinks = new HashMap<>(controlFlowLinks);
        return this;
    }

    public Map<String, Instruction> getControlFlowInstructions() {
        return controlFlowInstructions;
    }

    public Switch setControlFlowInstructions(Map<String, Instruction> controlFlowInstructions) {
        this.controlFlowInstructions = new HashMap<>(controlFlowInstructions);
        return this;
    }

    public ActionRegistrar getActionRegistrar() {
        return actionRegistrar;
    }

    public Map<String, org.change.v2.p4.model.fieldlists.FieldList> getFieldListMap() {
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
        return tree.switchSpec
                .setTableSelectors(JavaConversions.mapAsJavaMap(listener.tableSelectors()));
    }
}
