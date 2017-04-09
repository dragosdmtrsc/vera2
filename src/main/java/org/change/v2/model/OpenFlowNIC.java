package org.change.v2.model;


public class OpenFlowNIC extends NIC {

	private Long portNo;
	
	public void setPortNo(Long portNo) {
		this.portNo = portNo;
	}

	public Long getPortNo() {
		return portNo;
	}

	@Override
	public String toString() {
		return "OpenFlowNIC [portNo=" + portNo + ", toString()="
				+ super.toString() + "]";
	}

	
	
}
