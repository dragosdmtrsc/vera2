package org.change.v2.model.openflow.actions;

import org.change.v2.model.openflow.Action;
import org.change.v2.model.openflow.ActionType;
import org.change.v2.model.openflow.Decoder;
import org.change.v2.model.openflow.FlowVisitor;
import org.change.v2.model.openflow.QualifiedField;

public class SetFieldAction extends Action {
	public SetFieldAction() {
		super(ActionType.SetField);
	}
	
	private long value;
	public long getValue() {
		return value;
	}
	public QualifiedField getField() {
		return field;
	}

	private QualifiedField field;
	@Override
	public void accept(FlowVisitor visitor) {
		visitor.visit(this);
	}
	
	
	@Override
	public String toString() {
		return "SetFieldAction [value=" + value + ", field=" + field + "]";
	}
	public static SetFieldAction fromString(String text) {
		SetFieldAction sfAction = new SetFieldAction();
		if (text.contains("set_field:"))
		{
			text = text.substring("set_field:".length());
			if (text.contains("->"))
			{
				String[] texts = text.split("->");
				QualifiedField theField = QualifiedField.fromString(texts[1]);
				sfAction.value = Decoder.decodeType(theField.getName(), texts[0]);
				sfAction.field = theField;
			}
			else
			{
				throw new IllegalArgumentException("must be of the form set_field:value->dst");
			}
		}
		else
		{
			throw new IllegalArgumentException("must be of the form set_field:value->dst");
		}
		return sfAction;
	}
	
	
	
	public static void main(String[] argv)
	{
		System.out.println(fromString("set_field:00:11:22:33:44:55->eth_src"));
	}
}