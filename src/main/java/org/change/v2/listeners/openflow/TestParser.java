package org.change.v2.listeners.openflow;

import java.io.FileInputStream;
import java.math.BigInteger;

import org.antlr.v4.runtime.ANTLRInputStream;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.tree.ParseTreeWalker;

import generated.openflow_grammar.OpenflowLexer;
import generated.openflow_grammar.OpenflowParser;

public class TestParser {
    public static void main( String[] args) throws Exception 
    {
        ANTLRInputStream input = new ANTLRInputStream(
        		new FileInputStream(
        				"stack-inputs/flows-br-tun-controller.txt"));
        OpenflowLexer lexer = new OpenflowLexer(input);
        CommonTokenStream tokens = new CommonTokenStream(lexer);
        OpenflowParser parser = new OpenflowParser(tokens); 
        ParseTreeWalker walker = new ParseTreeWalker();
        FlowEntryListener listener = new FlowEntryListener();
        walker.walk(listener, parser.flows());
    }
}
