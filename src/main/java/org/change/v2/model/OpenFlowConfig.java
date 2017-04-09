package org.change.v2.model;

import java.util.ArrayList;
import java.util.List;

public class OpenFlowConfig {
	private List<OpenFlowNIC> nics = new ArrayList<OpenFlowNIC>();
	private List<OpenFlowTable> tables = new ArrayList<OpenFlowTable>();
	public List<OpenFlowNIC> getNics() {
		return nics;
	}
	public List<OpenFlowTable> getTables() {
		return tables;
	}
	@Override
	public String toString() {
		return "OpenFlowConfig [nics=" + nics + ", tables=" + tables + "]";
	}
	
	
	public OpenFlowConfig merge(OpenFlowConfig with)
	{
		this.nics.addAll(with.getNics());
		this.tables.addAll(with.getTables());
		return this;
	}
	
}
