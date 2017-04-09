package org.change.v2.model.openflow.actions;

import org.change.v2.model.openflow.Action;
import org.change.v2.model.openflow.FlowVisitor;
import org.change.v2.model.openflow.ActionType;
import org.change.v2.model.openflow.QualifiedField;

public class MoveAction extends Action {
	private QualifiedField from;
	private QualifiedField to;

	public QualifiedField getFrom() {
		return from;
	}

	public QualifiedField getTo() {
		return to;
	}

	public MoveAction() {
		super(ActionType.Move);
	}

	@Override
	public void accept(FlowVisitor visitor) {
		visitor.visit(this);
	}
	
	public static MoveAction fromString(String text) {
		MoveAction mAction = new MoveAction();
		if (text.contains("move:"))
		{
			text = text.substring("move:".length());
			if (text.contains("->"))
			{
				String[] allSplit = text.split("->");
				String firstOne = allSplit[0];
				String secondOne = allSplit[1];
				mAction.from = QualifiedField.fromString(firstOne);
				mAction.to = QualifiedField.fromString(secondOne);
				return mAction;
			}
			else
			{
				throw new IllegalArgumentException("please input move action move:reg[start..end]->reg[start..end]");
			}
		}
		else
		{
			throw new IllegalArgumentException("please input move action move:reg[start..end]->reg[start..end]");
		}
	}
	
	
	@Override
	public String toString() {
		return "MoveAction [from=" + from + ", to=" + to + "]";
	}

	public static void main(String []argv)
	{
		System.out.println(MoveAction.fromString("move:dl_dst[0..12]->dl_src[]"));
	}
	
}