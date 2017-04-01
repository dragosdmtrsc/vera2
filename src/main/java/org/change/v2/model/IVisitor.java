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
import org.change.v2.model.iptables.IcmpCodeOption;
import org.change.v2.model.iptables.IcmpTypeOption;
import org.change.v2.model.iptables.IfaceOption;
import org.change.v2.model.iptables.InstructionTarget;
import org.change.v2.model.iptables.JumpyTarget;
import org.change.v2.model.iptables.MACOption;
import org.change.v2.model.iptables.MarkOption;
import org.change.v2.model.iptables.MarkTarget;
import org.change.v2.model.iptables.NoTrackTarget;
// Start of user code (user defined imports)
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

// End of user code

/**
 * Description of IVisitor.
 * 
 * @author Dragos
 */
public interface IVisitor {
	// Start of user code (user defined attributes for IVisitor)
	
	// End of user code
	
	/**
	 * Description of the method visit.
	 * @param ipTablesTable 
	 */
	public void visit(IPTablesTable ipTablesTable);
	
	/**
	 * Description of the method visit.
	 * @param ipTablesChain 
	 */
	public void visit(IPTablesChain ipTablesChain);
	
	/**
	 * Description of the method visit.
	 * @param ovs 
	 */
	public void visit(OVSBridge ovs);
	
	/**
	 * Description of the method visit.
	 * @param rule 
	 */
	public void visit(IPTablesRule rule);
	
	/**
	 * Description of the method visit.
	 * @param match 
	 */
	public void visit(IPTablesMatch match);
	
	/**
	 * Description of the method visit.
	 * @param target 
	 */
//	public void visit(IPTablesTarget target);
	
	/**
	 * Description of the method visit.
	 * @param option 
	 */
//	public void visit(MatchOption option);
	
	/**
	 * Description of the method visit.
	 * @param table 
	 */
	public void visit(RoutingTable table);
	
	/**
	 * Description of the method visit.
	 * @param route 
	 */
	public void visit(Route route);
	
	/**
	 * Description of the method visit.
	 * @param match 
	 */
	public void visit(AddrOption match);
	
	/**
	 * Description of the method visit.
	 * @param option 
	 */
	public void visit(ConnmarkOption option);
	
	/**
	 * Description of the method visit.
	 * @param option 
	 */
	public void visit(CtStateOption option);
	
	/**
	 * Description of the method visit.
	 * @param option 
	 */
	public void visit(FragmentOption option);
	
	/**
	 * Description of the method visit.
	 * @param option 
	 */
	public void visit(GenericOption option);
	
	/**
	 * Description of the method visit.
	 * @param option 
	 */
	public void visit(IcmpCodeOption option);
	
	
	
	/**
	 * 
	 */
	public void visit(IcmpTypeOption option);
	
	public void visit(IfaceOption option);
	
	public void visit(MACOption option);
	
	public void visit(PhysdevInOption option);
	
	public void visit(PhysdevOutOption option);
	
	public void visit(PhysdevIsBridgedOption option);
	
	public void visit(PortOption option);
	
	public void visit(ProtocolOption option);
	
	public void visit(StateOption option);
	
	public void visit(ConnmarkTarget target);
	
	public void visit(DNATTarget target);
	
	public void visit(InstructionTarget target);
	
	public void visit(JumpyTarget target);
	
	public void visit(MarkTarget target);
	
	public void visit(NoTrackTarget target);
	// Start of user code (user defined methods for IVisitor)

	public void visit(Computer computer);

	public void visit(RejectTarget rejectTarget);

	public void visit(ReturnTarget returnTarget);

	public void visit(SNATTarget snatTarget);

	public void visit(AcceptTarget acceptTarget);

	public void visit(ChecksumTarget checksumTarget);

	public void visit(DropTarget dropTarget);

	public void visit(RedirectTarget redirectTarget);
	
	public void visit(MarkOption option);
	
	// End of user code


}
