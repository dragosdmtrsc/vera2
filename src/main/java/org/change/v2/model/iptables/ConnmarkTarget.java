/*******************************************************************************
 * 2017, All rights reserved.
 *******************************************************************************/
package org.change.v2.model.iptables;

import org.change.v2.model.IVisitor;

// End of user code

/**
 * Description of ConnmarkTarget.
 * 
 * @author Dragos
 */
public class ConnmarkTarget extends IPTablesTarget {
	private boolean restore;
	private int nfMask;
	private int ctMask;

	public boolean isRestore() {
		return restore;
	}

	public void setRestore(boolean restore) {
		this.restore = restore;
	}

	/**
	 * The constructor.
	 */
	public ConnmarkTarget() {
		// Start of user code constructor for ConnmarkTarget)
		super();
		// End of user code
	}

	public ConnmarkTarget(int ctMask, int nfMask, boolean restore) {
		this.nfMask = nfMask;
		this.ctMask = ctMask;
		this.restore = restore;
	}

	@Override
	public void accept(IVisitor visitor) {
		visitor.visit(this);
		
	}

	public int getNfMask() {
		return nfMask;
	}

	public void setNfMask(int nfMask) {
		this.nfMask = nfMask;
	}

	public int getCtMask() {
		return ctMask;
	}

	public void setCtMask(int ctMask) {
		this.ctMask = ctMask;
	}

	@Override
	public String toString() {
		return "ConnmarkTarget [restore=" + restore + ", nfMask=" + nfMask + ", ctMask=" + ctMask + "]";
	}

	
	
	// Start of user code (user defined methods for ConnmarkTarget)
	
	// End of user code


}
