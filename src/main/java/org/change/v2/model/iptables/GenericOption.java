/*******************************************************************************
 * 2017, All rights reserved.
 *******************************************************************************/
package org.change.v2.model.iptables;

import org.change.v2.model.IVisitor;
import org.change.v2.model.iptables.SimpleOption;
// Start of user code (user defined imports)

// End of user code

/**
 * Description of GenericOption.
 * 
 * @author Dragos
 */
public class GenericOption extends SimpleOption {
	/**
	 * Description of the property endValue.
	 */
	public Long endValue = null;
	
	/**
	 * Description of the property startValue.
	 */
	public Long startValue = null;
	
	/**
	 * Description of the property isInterval.
	 */
	public Boolean isInterval = Boolean.FALSE;
	
	/**
	 * Description of the property isTag.
	 */
	public Boolean isTag = Boolean.FALSE;
	
	/**
	 * Description of the property tagName.
	 */
	public String tagName = "";
	
	// Start of user code (user defined attributes for GenericOption)
	
	// End of user code
	
	/**
	 * The constructor.
	 */
	public GenericOption() {
		// Start of user code constructor for GenericOption)
		super();
		// End of user code
	}
	
	// Start of user code (user defined methods for GenericOption)
	
	// End of user code
	/**
	 * Returns endValue.
	 * @return endValue 
	 */
	public Long getEndValue() {
		return this.endValue;
	}
	
	/**
	 * Sets a value to attribute endValue. 
	 * @param newEndValue 
	 */
	public void setEndValue(Long newEndValue) {
	    this.endValue = newEndValue;
	}

	/**
	 * Returns startValue.
	 * @return startValue 
	 */
	public Long getStartValue() {
		return this.startValue;
	}
	
	/**
	 * Sets a value to attribute startValue. 
	 * @param newStartValue 
	 */
	public void setStartValue(Long newStartValue) {
	    this.startValue = newStartValue;
	}

	/**
	 * Returns isInterval.
	 * @return isInterval 
	 */
	public Boolean getIsInterval() {
		return this.isInterval;
	}
	
	/**
	 * Sets a value to attribute isInterval. 
	 * @param newIsInterval 
	 */
	public void setIsInterval(Boolean newIsInterval) {
	    this.isInterval = newIsInterval;
	}

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
	 * Returns tagName.
	 * @return tagName 
	 */
	public String getTagName() {
		return this.tagName;
	}
	
	/**
	 * Sets a value to attribute tagName. 
	 * @param newTagName 
	 */
	public void setTagName(String newTagName) {
	    this.tagName = newTagName;
	}

	@Override
	public void accept(IVisitor visitor) {
		// TODO Auto-generated method stub
		super.accept(visitor);
		visitor.visit(this);
	}

	public GenericOption(Long endValue, Long startValue, Boolean isInterval, Boolean isTag, String tagName) {
		super();
		this.endValue = endValue;
		this.startValue = startValue;
		this.isInterval = isInterval;
		this.isTag = isTag;
		this.tagName = tagName;
	}



}
