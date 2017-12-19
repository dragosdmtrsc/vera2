package org.change.v2.p4.model;

import scala.collection.JavaConversions;
import generated.parse.p4.P4GrammarLexer;
import generated.parse.p4.P4GrammarParser;
import org.antlr.v4.runtime.CharStream;
import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.tree.ParseTreeWalker;
import org.change.parser.p4.P4GrammarListener;
import org.change.v2.abstractnet.click.sefl.StripIPHeader;
import org.change.v2.analysis.processingmodels.Instruction;
import org.change.v2.p4.model.actions.ActionRegistrar;
import org.change.v2.p4.model.actions.P4Action;
import org.change.v2.p4.model.parser.State;
import org.change.v2.p4.model.table.TableMatch;
import scala.collection.JavaConversions;

import java.io.IOException;
import java.util.*;

/**
 * Created by dragos on 31.08.2017. Represents a switch specification,
 * which is basically a holder for info regarding the P4 program comprised therein
 */
public class Switch {
    private Map<String, List<String>> allowedActions = new HashMap<String, List<String>>();
    private Map<String, HeaderInstance> instances = new HashMap<String, HeaderInstance>();
    private Map<String, RegisterSpecification> registerSpecificationMap = null;
    private ActionRegistrar actionRegistrar = null;
    private Map<String, FieldList> fieldListMap = null;
    public Map<String, RegisterSpecification> getRegisterSpecificationMap() {
        return registerSpecificationMap;
    }

    private Map<String, String> controlFlowLinks = null;
    private Map<String, Instruction> controlFlowInstructions = null;


    private Map<String, List<TableMatch>> matches = new HashMap<String, List<TableMatch>>();

    public State getParserState(Object o) {
        return parserStates.get(o);
    }

    public boolean hasParserState(String o) {
        return parserStates.containsKey(o);
    }

    public Set<String> parserStates() {
        return parserStates.keySet();
    }

    private Map<String, State> parserStates = new HashMap<String, State>();

    public List<String> getAllowedActions(String perTable) {
        if (!allowedActions.containsKey(perTable))
            return Collections.emptyList();
        return allowedActions.get(perTable);
    }

    public Switch setRegisterSpecificationMap(Map<String, RegisterSpecification> registerSpecificationMap) {
        this.registerSpecificationMap = registerSpecificationMap;
        return this;
    }

    public List<TableMatch> getTableMatches(String perTable) {
        if (this.matches.containsKey(perTable))
            return this.matches.get(perTable);
        return null;
    }

    public int getSize(String value) {
        String[] split = value.split("\\.");
        String hName = split[0];
        HeaderInstance instance = this.getInstance(hName);
        return instance.getLayout().getField(split[1]).getLength();
    }

    public Switch createTable(String table) {
        if (!this.matches.containsKey(table)) {
            this.matches.put(table, new ArrayList<TableMatch>());
        }
        return this;
    }

    public Iterable<String> getDeclaredTables() {
        return this.matches.keySet();
    }

    public Switch addTableMatch(TableMatch tm) {
        if (!this.matches.containsKey(tm.getTable())) {
            this.matches.put(tm.getTable(), new ArrayList<TableMatch>());
        }
        this.matches.get(tm.getTable()).add(tm);
        return this;
    }

    public Map<String, String> getControlFlowLinks() {
        return controlFlowLinks;
    }

    public Switch setControlFlowLinks(Map<String, String> controlFlowLinks) {
        this.controlFlowLinks = new HashMap<String, String>(controlFlowLinks);
        return this;
    }

    public Map<String, Instruction> getControlFlowInstructions() {
        return controlFlowInstructions;
    }

    public Switch setControlFlowInstructions(Map<String, Instruction> controlFlowInstructions) {
        this.controlFlowInstructions =  new HashMap<String, Instruction>(controlFlowInstructions);
        return this;
    }

    public ActionRegistrar getActionRegistrar() {
        return actionRegistrar;
    }

    public Switch setActionRegistrar(ActionRegistrar actionRegistrar) {
        this.actionRegistrar = actionRegistrar;
        return this;
    }

    public Map<String, FieldList> getFieldListMap() {
        return fieldListMap;
    }

    public Switch setFieldListMap(Map<String, FieldList> fieldListMap) {
        this.fieldListMap = fieldListMap;
        return this;
    }

    public HeaderInstance getInstance(String o) {
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
        org.change.parser.p4.P4GrammarListener lsn = new P4GrammarListener();
        CharStream p4Input = CharStreams.fromFileName(p4File);
        P4GrammarLexer lexer = new P4GrammarLexer(p4Input);
        CommonTokenStream tokens = new CommonTokenStream(lexer);
        P4GrammarParser parser = new P4GrammarParser(tokens);
        P4GrammarParser.P4_programContext tree = parser.p4_program();
        ParseTreeWalker walker = new ParseTreeWalker();
        P4GrammarListener listener = new P4GrammarListener();

        walker.walk(listener,tree);

        Switch sw = new Switch().
                setActionRegistrar(listener.actionRegistrar()).
                setFieldListMap(listener.fieldLists()).
                setRegisterSpecificationMap(listener.registerMap());
        for (String table : listener.tables())
            sw = sw.createTable(table);
        sw.matches = listener.tableDeclarations();
        sw.allowedActions = listener.tableAllowedActions();
        sw.parserStates = listener.parserFunctions();
        sw.setControlFlowInstructions(JavaConversions.mapAsJavaMap(listener.instructions()));
        sw.setControlFlowLinks(JavaConversions.mapAsJavaMap(listener.links()));
        sw.instances = new HashMap<>(listener.instances());
        return sw;
    }

}
