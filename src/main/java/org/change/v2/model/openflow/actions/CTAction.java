package org.change.v2.model.openflow.actions;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.change.v2.model.openflow.Action;
import org.change.v2.model.openflow.ActionType;
import org.change.v2.model.openflow.FlowVisitor;
import org.change.v2.model.openflow.QualifiedField;

public class CTAction extends Action {

	public static class ExecAction extends Action {

		private long val;
		private QualifiedField to;

		public QualifiedField getTo() {
			return to;
		}

		public void setTo(QualifiedField to) {
			this.to = to;
		}

		public long getValue() {
			return val;
		}

		public void setValue(long val) {
			this.val = val;
		}
		
		public ExecAction() {
			super(ActionType.Load);
		}

		@Override
		public void accept(FlowVisitor visitor) {
		}

		@Override
		public String toString() {
			return "ExecAction [val=" + val + ", to=" + to + "]";
		}
		
	}
	
	private boolean force, commit, regZone = false, zone = false;
	private QualifiedField zoneReg;
	private int zoneVal;
	
	public boolean isForce() {
		return force;
	}

	public void setForce(boolean force) {
		this.force = force;
	}

	public boolean isCommit() {
		return commit;
	}

	public void setCommit(boolean commit) {
		this.commit = commit;
	}

	public QualifiedField getZoneReg() {
		return zoneReg;
	}

	public void setZoneReg(QualifiedField zoneReg) {
		this.zone = true;
		this.regZone = true;
		this.zoneReg = zoneReg;
	}

	public int getZoneVal() {
		return zoneVal;
	}

	public void setZoneVal(int zoneVal) {
		this.zone = true;
		this.regZone = false;
		this.zoneVal = zoneVal;
	}

	public boolean isRegZone() {
		return regZone;
	}

	public boolean isZone() {
		return zone;
	}

	private List<ExecAction> actions = new ArrayList<ExecAction>();
	private Optional<Integer> table = Optional.empty();
	
	public List<ExecAction> getActions() {
		return actions;
	}

	public CTAction() {
		super(ActionType.CTAction);
	}

	@Override
	public void accept(FlowVisitor visitor) {
	}

	@Override
	public String toString() {
		return "CTAction [force=" + force + ", commit=" + commit + ", regZone="
				+ regZone + ", zone=" + zone + ", zoneReg=" + zoneReg
				+ ", zoneVal=" + zoneVal + ", table=" + this.table + ", actions=" + actions + "]";
	}

	public void setTable(int intValue) {
		this.table = Optional.of(intValue);
	}
	
	public boolean hasTable() {
		return this.table.isPresent();
	}
	
	public int getTable()
	{
		if (this.table.isPresent())
			return this.table.get();
		return 0;
	}
	
	

}
