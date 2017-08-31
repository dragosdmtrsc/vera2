package org.change.v2.p4.model;

import generated.parse.p4.P4GrammarLexer;
import generated.parse.p4.P4GrammarParser;
import org.antlr.v4.runtime.CharStream;
import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.tree.ParseTreeWalker;
import org.change.parser.p4.P4GrammarListener;
import org.change.v2.p4.model.actions.ActionRegistrar;
import org.change.v2.p4.model.actions.P4Action;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by dragos on 31.08.2017. Represents a switch specification,
 * which is basically a holder for info regarding the P4 program comprised therein
 */
public class Switch {
    private P4GrammarListener ctx = null;

    private Map<String, RegisterSpecification> registerSpecificationMap = null;
    private ActionRegistrar actionRegistrar = null;
    private Map<String, FieldList> fieldListMap = null;

    public Map<String, RegisterSpecification> getRegisterSpecificationMap() {
        return registerSpecificationMap;
    }

    public Switch setRegisterSpecificationMap(Map<String, RegisterSpecification> registerSpecificationMap) {
        this.registerSpecificationMap = registerSpecificationMap;
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

    public Switch setCtx(P4GrammarListener ctx) {
        this.ctx = ctx;
        return this;
    }

    public P4GrammarListener getCtx() {
        return ctx;
    }

    public Switch setFieldListMap(Map<String, FieldList> fieldListMap) {
        this.fieldListMap = fieldListMap;
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

        return new Switch().
                setActionRegistrar(listener.actionRegistrar()).
                setFieldListMap(listener.fieldLists()).
                setRegisterSpecificationMap(listener.registerMap()).
                setCtx(listener);
    }

}
