package org.change.v2.model.iptables;

import java.util.Optional;

import org.change.v2.model.IVisitor;

public class MasqueradeTarget extends IPTablesTarget {

	private Optional<Integer> start = Optional.empty(), end = Optional.empty();
	
	@Override
	public void accept(IVisitor visitor) {
		visitor.visit(this);
	}

	public void setEnd(Integer decode) {
		this.end = Optional.of(decode);
	}

	public void setStart(Integer decode) {
		this.start = Optional.of(decode);
	}

	
	public boolean isSpecified()
	{
		return start.isPresent();
	}
	
	public boolean isSingle()
	{
		return this.isSpecified() && !this.end.isPresent();
	}
	
	public int getStart()
	{
		if (this.isSpecified())
		{
			return this.start.get();
		}
		else
		{
			return 0;
		}
	}
	
	
	public int getEnd()
	{
		if (!this.isSingle())
		{
			return this.end.get();
		}
		else
		{
			return 65536;
		}
	}
	
}
