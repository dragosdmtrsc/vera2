package org.change.v2.model.openflow.actions;

import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.change.v2.model.openflow.Action;
import org.change.v2.model.openflow.ActionType;
import org.change.v2.model.openflow.Decoder;
import org.change.v2.model.openflow.FlowVisitor;

public class ResubmitAction extends Action {
	private Optional<Long> table = Optional.empty();
	private Optional<Long> inPort = Optional.empty();

	@Override
	public String toString() {
		return "ResubmitAction [table=" + table + ", inPort=" + inPort + "]";
	}

	public Optional<Long> getTable() {
		return table;
	}

	public Optional<Long> getInPort() {
		return inPort;
	}

	public ResubmitAction() {
		super(ActionType.Resubmit);
	}

	@Override
	public void accept(FlowVisitor visitor) {
		visitor.visit(this);
	}

	public static ResubmitAction fromString(String text) {
		ResubmitAction rActions = new ResubmitAction();
		Pattern rex = Pattern.compile("[ ]*resubmit[ ]*\\([ ]*([0-9A-Z]*),[ ]*([0-9]*)\\)");
		Matcher theMatcher = rex.matcher(text);
		if (theMatcher.matches())
		{
			String inPort = theMatcher.group(1);
			String table = theMatcher.group(2);
			if (table !=  null && table.trim().length() > 0)
				rActions.table = Optional.of(Decoder.decodeLong(table));
			if (inPort != null && inPort.trim().length() > 0)
				rActions.inPort = Optional.of(Decoder.decodePort(inPort));
		}
		else
		{
			rex = Pattern.compile("resubmit:([0-9A-Z]*)");
			theMatcher = rex.matcher(text);
			if (theMatcher.matches())
			{
				String inPort = theMatcher.group(1);
				if (inPort != null && inPort.trim().length() > 0)
					rActions.inPort = Optional.of(Decoder.decodePort(inPort));
			}
			else
				throw new IllegalArgumentException(text + " is not in the form resubmit\\(([0-9]*),([0-9]*)\\)");
		}
		return rActions;
	}
	
	
	public static void main(String[] argv)
	{
		String in = "resubmit(12, 31)";
		System.out.println(fromString(in));
		assert fromString(in).inPort.get() == 12L && fromString(in).table.get() == 31L;
		
		in = "resubmit(, 31)";
		System.out.println(fromString(in));
		assert !fromString(in).inPort.isPresent() && fromString(in).table.get().equals(31L);
		
		in = "resubmit(31,)";
		System.out.println(fromString(in));
		assert fromString(in).inPort.get().equals(31L) && !fromString(in).table.isPresent();
		
		in = "resubmit:31";
		System.out.println(fromString(in));
		assert fromString(in).inPort.get().equals(31L) && !fromString(in).table.isPresent();
	}
}