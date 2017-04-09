package org.change.v2.model.openflow.actions;

import java.util.Optional;

import org.change.v2.model.openflow.Action;
import org.change.v2.model.openflow.ActionType;
import org.change.v2.model.openflow.Decoder;
import org.change.v2.model.openflow.FlowVisitor;
import org.change.v2.model.openflow.QualifiedField;

public class LoadAction extends Action {
	
	private Optional<QualifiedField> to = Optional.empty();
	private Optional<Long> value = Optional.empty();
	private Optional<QualifiedField> from = Optional.empty();
	
	public Optional<QualifiedField> getTo() {
		return to;
	}

	public Optional<Long> getValue() {
		return value;
	}

	public Optional<QualifiedField> getFrom() {
		return from;
	}

	public LoadAction() {
		super(ActionType.Load);
	}

	@Override
	public void accept(FlowVisitor visitor) {
		visitor.visit(this);
	}

	public static LoadAction fromString(String text) {
		LoadAction mAction = new LoadAction();
		if (text.contains("load:"))
		{
			text = text.substring("load:".length());
			if (text.contains("->"))
			{
				String[] allSplit = text.split("->");
				String firstOne = allSplit[0];
				String secondOne = allSplit[1];
				
				QualifiedField secondField = QualifiedField.fromString(secondOne);
				if (Decoder.isField(firstOne))
				{
					mAction.from = Optional.of(QualifiedField.fromString(firstOne));
				}
				else
				{
					mAction.value = Optional.of(Decoder.decodeType(secondField.getName(), firstOne));
				}
				mAction.to = Optional.of(secondField);
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
		return "LoadAction [to=" + to + ", value=" + value + ", from=" + from
				+ "]";
	}
	
	
	
	
}