package org.change.v2.model;

import generated.openflow_grammar.OpenflowLexer;
import generated.openflow_grammar.OpenflowParser;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.antlr.v4.runtime.ANTLRInputStream;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.tree.ParseTreeWalker;
import org.change.v2.listeners.openflow.FlowEntryListener;
import org.change.v2.model.openflow.FlowAcceptor;
import org.change.v2.model.openflow.FlowEntry;
import org.change.v2.model.openflow.FlowVisitor;
import org.change.v2.model.openflow.PortParser;

public class OpenFlowTable implements FlowAcceptor {
	private List<FlowEntry> entries = new ArrayList<FlowEntry>();
	private int tableId;
	
	public int getTableId() {
		return tableId;
	}


	public List<FlowEntry> getEntries() {
		return entries;
	}
	
	
	public static OpenFlowConfig fromStream (InputStream is) throws IOException	
	{
		return fromStream(is, null);
	}
	public static OpenFlowConfig fromStream(InputStream is, OpenFlowConfig cfg) throws IOException
	{
		if (cfg == null)
			cfg = new OpenFlowConfig();
		ANTLRInputStream input = new ANTLRInputStream(is);
        OpenflowLexer lexer = new OpenflowLexer(input);
        CommonTokenStream tokens = new CommonTokenStream(lexer);
        OpenflowParser parser = new OpenflowParser(tokens); 
        ParseTreeWalker walker = new ParseTreeWalker();
        FlowEntryListener listener = new FlowEntryListener();
        walker.walk(listener, parser.flows());
        Map<Integer,List<FlowEntry>> perTable = listener.getEntries().stream().sorted(new Comparator<FlowEntry>() {
			@Override
			public int compare(FlowEntry o1, FlowEntry o2) {
				if (o1.getTable() - o2.getTable() == 0)
					return - o1.getPriority() + o2.getPriority();
				return - o1.getTable() + o2.getTable();
			}     	
        }).collect(Collectors.groupingBy(FlowEntry::getTable));
        
        for (Integer key : perTable.keySet())
        {
        	OpenFlowTable table = new OpenFlowTable();
        	table.tableId = key;
            cfg.getTables().add(table);
        	List<FlowEntry> entries = perTable.get(key);
        	for (FlowEntry e : entries)
        	{
        		table.getEntries().add(e);
        	}
        }
        return cfg;
	}
	
	public static OpenFlowConfig  appendPorts(InputStream is, OpenFlowConfig cfg) throws IOException 
	{
		if (cfg == null)
			cfg = new OpenFlowConfig();
		
		List<OpenFlowNIC> nics = PortParser.parseNics(is);
		for (OpenFlowNIC nic : nics)
		{
			cfg.getNics().add(nic);
		}
		
		return cfg;
	}
	
	
	
	@Override
	public String toString() {
		return "OpenFlowTable [table=" + tableId + ", entries=" + entries + "]";
	}


	public static void main(String[] argv) throws FileNotFoundException, IOException
	{
		OpenFlowConfig cfg = fromStream(
				new FileInputStream("stack-inputs/flows-br-int-controller.txt"));
//		System.out.println(cfg);
		cfg = appendPorts(new FileInputStream("stack-inputs/ports-br-int-controller.txt"), cfg);
		System.out.println(cfg);
	}


	@Override
	public void accept(FlowVisitor visitor) {
		visitor.visit(this);
		for (FlowEntry fe : this.getEntries())
		{
			fe.accept(visitor);
		}
	}
	
}
