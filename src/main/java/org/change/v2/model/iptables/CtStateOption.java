/*******************************************************************************
 * 2017, All rights reserved.
 *******************************************************************************/
package org.change.v2.model.iptables;

import java.util.ArrayList;
import java.util.List;

import org.change.v2.model.IVisitor;
// Start of user code (user defined imports)

// End of user code

/**
 * Description of CtStateOption.
 * 
 * @author Dragos
 */
public class CtStateOption extends SimpleOption {
	/**
	 * Description of the property isTag.
	 */
	private Boolean isTag = Boolean.FALSE;
	
	/**
	 * Description of the property list.
	 */
	private List<Integer> list = new ArrayList<Integer>();
	
	/**
	 * Description of the property name.
	 */
	private String name = "";
	
	// Start of user code (user defined attributes for CtStateOption)
	
	// End of user code
	
	/**
	 * The constructor.
	 */
	public CtStateOption() {
		// Start of user code constructor for CtStateOption)
		super();
		// End of user code
	}
	
	// Start of user code (user defined methods for CtStateOption)
	
	public CtStateOption(boolean neg, String string, List<Integer> collect, boolean b) {
		this.setNeg(neg);
		this.setName(string);
		this.list = collect;
		this.setIsTag(b);
	}

	// End of user code
	/**
	 * Returns isTag.
	 * @return isTag 
	 */
	public Boolean getIsTag() {
		return this.isTag;
	}
	
	/**
	 * Sets a value to attribute isTag. 
	 * @param newIsTag 
	 */
	public void setIsTag(Boolean newIsTag) {
	    this.isTag = newIsTag;
	}

	/**
	 * Returns list.
	 * @return list 
	 */
	public List<Integer> getList() {
		return this.list;
	}

	/**
	 * Returns name.
	 * @return name 
	 */
	public String getName() {
		return this.name;
	}
	
	/**
	 * Sets a value to attribute name. 
	 * @param newName 
	 */
	public void setName(String newName) {
	    this.name = newName;
	}

	@Override
	public void accept(IVisitor visitor) {
		// TODO Auto-generated method stub
		super.accept(visitor);
		visitor.visit(this);
	}

	@Override
	public String toString() {
		return "CtStateOption [isTag=" + isTag + ", list=" + list + ", name=" + name + "]";
	}



}
