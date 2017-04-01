/*******************************************************************************
 * 2017, All rights reserved.
 *******************************************************************************/
package org.change.v2.model;

import org.change.v2.model.iptables.AcceptTarget;
import org.change.v2.model.iptables.AddrOption;
import org.change.v2.model.iptables.ChecksumTarget;
import org.change.v2.model.iptables.ConnmarkOption;
import org.change.v2.model.iptables.ConnmarkTarget;
import org.change.v2.model.iptables.CtStateOption;
import org.change.v2.model.iptables.DNATTarget;
import org.change.v2.model.iptables.DropTarget;
import org.change.v2.model.iptables.FragmentOption;
import org.change.v2.model.iptables.GenericOption;
import org.change.v2.model.iptables.IPTablesChain;
import org.change.v2.model.iptables.IPTablesMatch;
import org.change.v2.model.iptables.IPTablesRule;
import org.change.v2.model.iptables.IPTablesTable;
import org.change.v2.model.iptables.IPTablesTarget;
import org.change.v2.model.iptables.IcmpCodeOption;
import org.change.v2.model.iptables.IcmpTypeOption;
import org.change.v2.model.iptables.IfaceOption;
import org.change.v2.model.iptables.InstructionTarget;
import org.change.v2.model.iptables.JumpyTarget;
import org.change.v2.model.iptables.MACOption;
import org.change.v2.model.iptables.MarkOption;
import org.change.v2.model.iptables.MarkTarget;
import org.change.v2.model.iptables.MatchOption;
import org.change.v2.model.iptables.NoTrackTarget;
import org.change.v2.model.iptables.PhysdevInOption;
import org.change.v2.model.iptables.PhysdevIsBridgedOption;
import org.change.v2.model.iptables.PhysdevOutOption;
import org.change.v2.model.iptables.PortOption;
import org.change.v2.model.iptables.ProtocolOption;
import org.change.v2.model.iptables.RedirectTarget;
import org.change.v2.model.iptables.RejectTarget;
import org.change.v2.model.iptables.ReturnTarget;
import org.change.v2.model.iptables.SNATTarget;
import org.change.v2.model.iptables.StateOption;
import org.change.v2.model.openflow.FlowEntry;

// End of user code

/**
 * Description of BaseVisitor.
 * 
 * @author Dragos
 */
public class BaseVisitor implements IVisitor {
	// Start of user code (user defined attributes for BaseVisitor)
	
	// End of user code
	
	/**
	 * The constructor.
	 */
	public BaseVisitor() {
		// Start of user code constructor for BaseVisitor)
		super();
		// End of user code
	}
	
	/**
	 * Description of the method visit.
	 * @param ipTablesTable 
	 */
	public void visit(IPTablesTable ipTablesTable) {
		// Start of user code for method visit
		// End of user code
	}
	 
	/**
	 * Description of the method visit.
	 * @param ipTablesChain 
	 */
	public void visit(IPTablesChain ipTablesChain) {
		// Start of user code for method visit
		// End of user code
	}
	 
	/**
	 * Description of the method visit.
	 * @param ovs 
	 */
	public void visit(OVSBridge ovs) {
		// Start of user code for method visit
		// End of user code
	}
	 
	/**
	 * Description of the method visit.
	 * @param rule 
	 */
	public void visit(IPTablesRule rule) {
		// Start of user code for method visit
		// End of user code
	}
	 
	/**
	 * Description of the method visit.
	 * @param match 
	 */
	public void visit(IPTablesMatch match) {
		// Start of user code for method visit
		// End of user code
	}
	 
	/**
	 * Description of the method visit.
	 * @param target 
	 */
	public void visit(IPTablesTarget target) {
		// Start of user code for method visit
		// End of user code
	}
	 
	/**
	 * Description of the method visit.
	 * @param option 
	 */
	public void visit(MatchOption option) {
		// Start of user code for method visit
		// End of user code
	}
	 
	/**
	 * Description of the method visit.
	 * @param table 
	 */
	public void visit(RoutingTable table) {
		// Start of user code for method visit
		// End of user code
	}
	 
	/**
	 * Description of the method visit.
	 * @param route 
	 */
	public void visit(Route route) {
		// Start of user code for method visit
		// End of user code
	}
	 
	/**
	 * Description of the method visit.
	 * @param match 
	 */
	public void visit(AddrOption match) {
		// Start of user code for method visit
		// End of user code
	}
	 
	/**
	 * Description of the method visit.
	 * @param option 
	 */
	public void visit(ConnmarkOption option) {
		// Start of user code for method visit
		// End of user code
	}
	 
	/**
	 * Description of the method visit.
	 * @param option 
	 */
	public void visit(CtStateOption option) {
		// Start of user code for method visit
		// End of user code
	}
	 
	/**
	 * Description of the method visit.
	 * @param option 
	 */
	public void visit(FragmentOption option) {
		// Start of user code for method visit
		// End of user code
	}
	 
	/**
	 * Description of the method visit.
	 * @param option 
	 */
	public void visit(GenericOption option) {
		// Start of user code for method visit
		// End of user code
	}
	 
	/**
	 * Description of the method visit.
	 * @param option 
	 */
	public void visit(IcmpCodeOption option) {
		// Start of user code for method visit
		// End of user code
	}

	@Override
	public void visit(IcmpTypeOption option) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void visit(IfaceOption option) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void visit(MACOption option) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void visit(PhysdevInOption option) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void visit(PhysdevOutOption option) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void visit(PhysdevIsBridgedOption option) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void visit(PortOption option) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void visit(ProtocolOption option) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void visit(StateOption option) {	
	}

	@Override
	public void visit(ConnmarkTarget target) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void visit(DNATTarget target) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void visit(InstructionTarget target) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void visit(JumpyTarget target) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void visit(MarkTarget target) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void visit(NoTrackTarget target) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void visit(Computer computer) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void visit(RejectTarget rejectTarget) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void visit(ReturnTarget returnTarget) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void visit(SNATTarget snatTarget) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void visit(AcceptTarget acceptTarget) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void visit(ChecksumTarget checksumTarget) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void visit(DropTarget dropTarget) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void visit(RedirectTarget redirectTarget) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void visit(MarkOption option) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void visit(FlowEntry flowEntry) {
		// TODO Auto-generated method stub
		
	}
	 
	// Start of user code (user defined methods for BaseVisitor)
	
	// End of user code


}
