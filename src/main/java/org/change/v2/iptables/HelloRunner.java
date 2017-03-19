package org.change.v2.iptables;

import java.io.FileInputStream;

import org.antlr.v4.runtime.ANTLRInputStream;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.tree.ParseTreeWalker;

import generated.iptables_grammar.IptablesLexer;
import generated.iptables_grammar.IptablesParser;

public class HelloRunner 
{
    public static void main( String[] args) throws Exception 
    {

        ANTLRInputStream input = new ANTLRInputStream(new FileInputStream("C:\\Users\\Dragos\\Documents\\GitHub\\symnet-neutron\\src\\main\\resources\\iptables_grammar\\iptables_controller"));

        IptablesLexer lexer = new IptablesLexer(input);

        CommonTokenStream tokens = new CommonTokenStream(lexer);

        IptablesParser parser = new IptablesParser(tokens);
        ParseTreeWalker walker = new ParseTreeWalker();
        Iptables2SeflListener listener = new Iptables2SeflListener();
        walker.walk(listener, parser.table());
    }
}
