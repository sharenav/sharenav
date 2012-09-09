/**
 * This file is part of OSM2ShareNav 
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License version 2 as published by
 * the Free Software Foundation.
 *
 * Copyright (C) 2007 Harald Mueller
 */
/**
 * 	g // total cost 
 *	h //heuristic
 *	f //sum of g and h; 
 *
 * 
 *  Create a node containing the goal state node_goal
 *	Create a node containing the start state node_start
 *	Put node_start on the open list
 *	while the OPEN list is not empty
 *	{
 *	Get the node off the open list with the lowest f and call it node_current
 *	if node_current is the same state as node_goal we have found the solution; break from the while loop
 *	    Generate each state node_successor that can come after node_current
 *	    for each node_successor of node_current
 * 	    {
 * 	        Set the cost of node_successor to be the cost of node_current plus the cost to get to node_successor from node_current
 * 	        find node_successor on the OPEN list
 * 	        if node_successor is on the OPEN list but the existing one is as good or better then discard this successor and continue
 * 	        if node_successor is on the CLOSED list but the existing one is as good or better then discard this successor and continue
 * 	        Remove occurrences of node_successor from OPEN and CLOSED
 * 	        Set the parent of node_successor to node_current
 * 	        Set h to be the estimated distance to node_goal (Using the heuristic function)
 * 	         Add node_successor to the OPEN list
 * 	    }
 * 	    Add node_current to the CLOSED list
 * 	}
 */


package net.sharenav.osmToShareNav;

import java.util.Hashtable;
import java.util.Vector;

import net.sharenav.osmToShareNav.model.Connection;
import net.sharenav.osmToShareNav.model.RouteNode;

/**
 * @author hmueller
 * 
 */
public class AStar {

	private final Hashtable<Connection, Node> open = new Hashtable<Connection, Node>(500);
	private final Hashtable<Connection, Node> closed = new Hashtable<Connection, Node>(500);
	public int evaluated = 0;
	public int expanded = 0;
	public long bestTotal = 0;
	public boolean ready = false;
	private boolean newBest = false;
	private final Vector<Node> nodes = new Vector<Node>();
	private RouteNode dest;

	private Node search(RouteNode dest) {
		this.dest = dest;
		Node best;
//		ArrayList<Connection> childStates;
		long childCosts;
		Vector<Node> children = new Vector<Node>();
		while (!(nodes.isEmpty())) {
			best = nodes.firstElement();
			System.out.println(best);
			if (closed.get(best.state) != null) { // to avoid having to
				// remove
				nodes.removeElementAt(0);// improved nodes from nodes
				continue;
			}
			if (!(best.total == bestTotal)) {
				setBest(best.total);
			} 
			if (best.state.to == dest) {
				return best;
			}
			children.removeAllElements();
			//childCosts = 1 + best.costs;
			expanded++;
			for (Connection childState: best.state.to.getConnected()) {
				childCosts = best.costs + childState.length;
				Node closedNode = null;
				Node openNode = null;
				Node theNode = null;
				if ((closedNode =  closed.get(childState)) == null) 
					openNode = (Node) open.get(childState);
				theNode = (openNode != null) ? openNode : closedNode;
				if (theNode != null) {
					if (childCosts < theNode.costs) {
						if (closedNode != null) {
							open.put(childState, theNode);
							closed.remove(childState);
						} else { 
							long dist = theNode.distance;
							theNode = new Node(childState, best, childCosts, dist);
							open.put(childState, theNode); 
							// nodes.removeElement(theNode); //get rid
							// of this
						} 
						theNode.costs = childCosts;
						theNode.total = theNode.costs + theNode.distance;
						theNode.parent = best; 
						children.addElement(theNode);
					}
				} else { 
					long estimation;
					Node newNode;
					estimation = MyMath.dist(childState.to.node, dest.node);
					newNode = new Node(childState, best, childCosts, estimation);
					open.put(childState, newNode);
					evaluated++;
					children.addElement(newNode);
				}
			}
			open.remove(best.state);
			closed.put(best.state, best);
			nodes.removeElementAt(0);
			addToNodes(children); // update nodes
		} 
		return null;
		// no open nodes and no solution

	} 
	
	private int rbsearch(int l, int h, long tot, long costs) {
		if (l > h) {
			return l; //insert before l
		}
		int cur = (l + h) / 2;
		long ot = nodes.elementAt(cur).total;
		if ((tot < ot) || (tot == ot && costs >= nodes.elementAt(cur).costs)) { 
			return rbsearch(l, cur-1, tot, costs);
		}
		return rbsearch(cur + 1, h, tot, costs);
	} 

	private int bsearch(int l, int h, long tot, long costs) {
		int lo = l;
		int hi = h;
		while (lo <= hi) {
			int cur = (lo + hi) / 2;
			long ot = nodes.elementAt(cur).total;
			if ((tot < ot) || (tot == ot && costs >= ((Node) nodes.elementAt(cur)).costs)) { 
				hi = cur - 1;
			} else {
				lo = cur + 1;
			}
		} 
		return lo; //insert before lo 
	} 

	// {{{ private void addToNodes(Vector children) 

	private void addToNodes(Vector<Node> children) {
		for (int i = 0; i < children.size(); i++) { 
			Node newNode = (Node) children.elementAt(i);
			long newTotal = newNode.total;
			long newCosts = newNode.costs;
			int idx = bsearch(0, nodes.size()-1, newTotal, newCosts);
			nodes.insertElementAt(newNode, idx); 
		}
	}

	// {{{ public final Vector solve (State initialState) 
	public final Vector<Connection> solve (RouteNode start, RouteNode dest) {
		System.out.println("AStar.solve(): only first route mode");
		int times[] = new int[1];
		times[0] = 0;
		Connection initialState=new Connection(start, (short)0, times, (byte)0, (byte)0, null);
		Node solution;
		Node firstNode;
		long estimation;
		expanded = 0;
		evaluated = 1;
		estimation = MyMath.dist(initialState.to.node, dest.node);
		firstNode = new Node(initialState, null, 0l, estimation);
		open.put(initialState, firstNode);
		nodes.addElement(firstNode);
		solution = search(dest);
		nodes.removeAllElements();
		open.clear();
		closed.clear();
		ready = true;
		setBest(bestTotal);
		return getSequence(solution);
	}

	private Vector<Connection> getSequence(Node n) { 
		Vector<Connection> result;
		if (n == null) {
			result = new Vector<Connection>();
		} else { 
			result = getSequence (n.parent);
			result.addElement(n.state);
		} 
		return result; 
	} 
	
	private synchronized void setBest (long value) {
		bestTotal = value;
		newBest = true;
		notify(); // All? 
		Thread.yield(); //for getNewBest 
	} 

	public synchronized long getNewBest() {
		while(!newBest) {
			try { 
				wait(); 
			} catch (InterruptedException e) {
			} 
		} 
		newBest = false;
		return bestTotal; 
	}

	final class Node {
		Connection state;
		long costs;
		long distance;
		long total;
		Node parent;

		Node(Connection theState, Node theParent, long theCosts, long theDistance) {
//			theDistance *=4;
			state = theState;
			parent = theParent;
			costs = theCosts;
			distance = theDistance;
			total = theCosts + (theDistance);
		}

		public String toString() {
			return "Node id=" + state.to.node.id + " g=" + costs + " h=" + distance + " g=" + total;
		}
	}
}
