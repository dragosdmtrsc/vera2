package org.change.v2.iptables;

import java.io.FileInputStream;

import org.antlr.v4.runtime.ANTLRInputStream;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.tree.ParseTreeWalker;
import org.change.v2.listeners.iptables.TableMatcher;

import generated.iptables_grammar.IptablesLexer;
import generated.iptables_grammar.IptablesParser;

public class HelloRunner 
{
    public static void main( String[] args) throws Exception 
    {

        ANTLRInputStream input = new ANTLRInputStream(new FileInputStream("stack-inputs/iptables_controller.txt"));
 
        IptablesLexer lexer = new IptablesLexer(input);

        CommonTokenStream tokens = new CommonTokenStream(lexer);

        IptablesParser parser = new IptablesParser(tokens);
        ParseTreeWalker walker = new ParseTreeWalker();
        TableMatcher listener = new TableMatcher("nat");
        walker.walk(listener, parser.table());
        System.out.println(listener.getTable());
        
    }
}
