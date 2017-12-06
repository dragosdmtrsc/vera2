package org.change.v2.stategraph;

import javax.swing.tree.DefaultMutableTreeNode;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.LinkedList;
import java.util.List;


public class StateGraph {

    public static enum NodeTag {
        CONSTANT, SYMBOL, AND, OR, PLUS, MINUS, REFERENCE, PLUS_E, MINUS_E, LT, LTE, GT, GTE, EQ, NOT, VALUE, STATE, ROOT;


        public boolean isExpression() {
            return this == CONSTANT ||
                    this == PLUS ||
                    this == MINUS ||
                    this == MINUS_E ||
                    this == PLUS_E ||
                    this == SYMBOL ||
                    this == REFERENCE;
        }
    }


    public static DefaultMutableTreeNode deepClone(DefaultMutableTreeNode crt) {
        DefaultMutableTreeNode newNode = (DefaultMutableTreeNode) crt.clone();
        Enumeration<DefaultMutableTreeNode> enumer = crt.children();
        while (enumer.hasMoreElements()) {
            DefaultMutableTreeNode n = enumer.nextElement();
            newNode.add(deepClone(n));
        }
        return newNode;
    }

    public static boolean removeNOT(DefaultMutableTreeNode crt) {
        if (crt.getUserObject() instanceof NodeTag &&
                ((NodeTag) crt.getUserObject()).equals(NodeTag.NOT)) {

            DefaultMutableTreeNode child =
                    (DefaultMutableTreeNode) crt.children().nextElement();
            NodeTag childTag = (NodeTag) child.getUserObject();
            switch (childTag) {
                case OR:
                    child.setUserObject(NodeTag.AND);
                    List<DefaultMutableTreeNode> children =
                            new ArrayList<DefaultMutableTreeNode>();
                    Enumeration<DefaultMutableTreeNode> childNodes =
                            child.children();
                    while (childNodes.hasMoreElements()) {
                        children.add(childNodes.nextElement());
                    }
                    for (DefaultMutableTreeNode n : children) {
                        n.removeFromParent();
                        DefaultMutableTreeNode notOne = new DefaultMutableTreeNode(NodeTag.NOT);
                        notOne.add(n);
                        child.add(notOne);
                    }
                    break;
                case AND:
                    child.setUserObject(NodeTag.OR);
                    List<DefaultMutableTreeNode> children1 =
                            new ArrayList<DefaultMutableTreeNode>();
                    Enumeration<DefaultMutableTreeNode> childNodes1 =
                            child.children();
                    while (childNodes1.hasMoreElements()) {
                        children1.add(childNodes1.nextElement());
                    }
                    for (DefaultMutableTreeNode n : children1) {
                        n.removeFromParent();
                        DefaultMutableTreeNode notOne = new DefaultMutableTreeNode(NodeTag.NOT);
                        notOne.add(n);
                        child.add(notOne);
                    }
                    break;
                case EQ:
                    child.setUserObject(NodeTag.OR);
                    List<DefaultMutableTreeNode> children2 =
                            new ArrayList<DefaultMutableTreeNode>();
                    Enumeration<DefaultMutableTreeNode> childNodes2 =
                            child.children();
                    while (childNodes2.hasMoreElements()) {
                        children2.add(childNodes2.nextElement());
                    }
                    for (DefaultMutableTreeNode n : children2) {
                        n.removeFromParent();
                        DefaultMutableTreeNode gtOne = new DefaultMutableTreeNode(NodeTag.GT);
                        DefaultMutableTreeNode ltOne = new DefaultMutableTreeNode(NodeTag.LT);
                        gtOne.add(deepClone(n));
                        ltOne.add(deepClone(n));
                        child.add(gtOne);
                        child.add(ltOne);
                    }
                    break;
                case GT:
                    child.setUserObject(NodeTag.LTE);
                    break;
                case GTE:
                    child.setUserObject(NodeTag.LT);
                    break;
                case LT:
                    child.setUserObject(NodeTag.GTE);
                    break;
                case LTE:
                    child.setUserObject(NodeTag.GT);
                    break;
                case NOT:
                    DefaultMutableTreeNode grandChild =
                            (DefaultMutableTreeNode) child.children().nextElement();
                    child = grandChild;
                    break;
                default:
                    break;
            }

            DefaultMutableTreeNode parent = (DefaultMutableTreeNode) crt.getParent();
            crt.removeFromParent();
            parent.add(child);
            return true;
        } else {
            Enumeration<DefaultMutableTreeNode> enumer = crt.children();
            while (enumer.hasMoreElements()) {
                if (removeNOT(enumer.nextElement()))
                    return true;
            }
            return false;
        }
    }


    private static void replace(DefaultMutableTreeNode subtree,
                                LinkedList<Integer> path, DefaultMutableTreeNode with) {
        if (path.isEmpty()) {
            ((DefaultMutableTreeNode) subtree.getParent()).add(with);
            subtree.removeFromParent();
        } else {
            LinkedList<Integer> copy = new LinkedList<Integer>(path);
            subtree = (DefaultMutableTreeNode)
                    subtree.getChildAt(copy.removeFirst());
            replace(subtree, copy, with);
        }
    }


    private static boolean removeOR(DefaultMutableTreeNode crt,
                                    LinkedList<Integer> path) {
        if (crt.getUserObject() instanceof NodeTag &&
                ((NodeTag) crt.getUserObject()).equals(NodeTag.OR)) {
            DefaultMutableTreeNode oldParent = (DefaultMutableTreeNode) crt.getParent();
            List<DefaultMutableTreeNode> nodes = new ArrayList<DefaultMutableTreeNode>();
            Enumeration<DefaultMutableTreeNode> enumer = crt.children();
            while (enumer.hasMoreElements()) {
                DefaultMutableTreeNode a1 = enumer.nextElement();
                nodes.add(a1);
            }

            DefaultMutableTreeNode stateNode =
                    (DefaultMutableTreeNode) crt.getRoot().getChildAt(path.removeFirst());
            for (int i = 1; i < nodes.size(); i++) {
                DefaultMutableTreeNode cloned = deepClone(stateNode);
                ((DefaultMutableTreeNode) crt.getRoot()).add(cloned);
                replace(cloned, path, nodes.get(i));
            }


            DefaultMutableTreeNode cloned = stateNode;
            replace(cloned, path, nodes.get(0));

            return true;
        } else {
            Enumeration<DefaultMutableTreeNode> enumer = crt.children();
            int i = 0;
            while (enumer.hasMoreElements()) {
                LinkedList<Integer> newPath = new LinkedList<Integer>(path);
                newPath.add(i++);
                if (removeOR(enumer.nextElement(), newPath))
                    return true;
            }
            return false;
        }
    }


    public static boolean removeOR(DefaultMutableTreeNode crt) {
        return removeOR(crt, new LinkedList<Integer>());
    }

    public static boolean removeAND(DefaultMutableTreeNode crt) {
        if (crt.getUserObject() instanceof NodeTag &&
                ((NodeTag) crt.getUserObject()).equals(NodeTag.AND)) {
            DefaultMutableTreeNode oldParent = (DefaultMutableTreeNode) crt.getParent();
            crt.removeFromParent();
            List<DefaultMutableTreeNode> nodes = new ArrayList<DefaultMutableTreeNode>();
            Enumeration<DefaultMutableTreeNode> enumer = crt.children();
            while (enumer.hasMoreElements()) {
                DefaultMutableTreeNode a1 = enumer.nextElement();
                nodes.add(a1);
            }
            for (DefaultMutableTreeNode n : nodes) {
                n.removeFromParent();
                oldParent.add(n);
            }
            return true;
        } else {
            Enumeration<DefaultMutableTreeNode> enumer = crt.children();
            while (enumer.hasMoreElements()) {
                if (removeAND(enumer.nextElement()))
                    return true;
            }
            return false;
        }
    }

    public static void removeReferences(DefaultMutableTreeNode root) {
        while (dfs(root)) ;
    }

    public static boolean dfs(DefaultMutableTreeNode crt) {
        if (crt.getUserObject() instanceof NodeTag && ((NodeTag) crt.getUserObject()).equals(NodeTag.REFERENCE)) {
            DefaultMutableTreeNode valueNode = (DefaultMutableTreeNode) crt.getChildAt(0);
            DefaultMutableTreeNode eNode = null;
            Enumeration<DefaultMutableTreeNode> enumer = valueNode.children();
            while (enumer.hasMoreElements()) {
                DefaultMutableTreeNode child = enumer.nextElement();
                Object userObj = child.getUserObject();
                if (userObj instanceof NodeTag) {
                    NodeTag cTag = (NodeTag) child.getUserObject();
                    if (cTag.isExpression()) {
                        eNode = child;
                        break;
                    }
                }
            }
            DefaultMutableTreeNode root = (DefaultMutableTreeNode) crt.getRoot().getChildAt(0);
            DefaultMutableTreeNode oldParent = (DefaultMutableTreeNode) crt.getParent();
            crt.removeFromParent();
            valueNode.removeFromParent();
            root.add(valueNode);
            oldParent.add(deepClone(eNode));
            return true;
        } else {
            Enumeration<DefaultMutableTreeNode> enumer = crt.children();
            while (enumer.hasMoreElements()) {

                if (dfs(enumer.nextElement()))
                    return true;
            }
            return false;
        }
    }

}
