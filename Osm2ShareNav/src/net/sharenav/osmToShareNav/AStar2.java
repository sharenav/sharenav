/**
 * This file is part of OSM2ShareNav 
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License version 2 as published by
 * the Free Software Foundation.
 *
 * Copyright (C) 2007 Harald Mueller
 */ 
/** 	g // total cost 
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
 * 	        Remove occurences of node_successor from OPEN and CLOSED
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
public class AStar2 {

	private final Hashtable<RouteNode, Node> open = new Hashtable<RouteNode, Node>(500);
	private final Hashtable<RouteNode, Node> closed = new Hashtable<RouteNode, Node>(500);
	public int evaluated = 0;
	public int expanded = 0;
	public long bestTotal = 0;
	public boolean ready = false;
	private boolean newBest = false;
	private final Vector<Node> nodes = new Vector<Node>();
	private RouteNode dest;
	private boolean bestTime = true;
	private boolean noHeuristic = false;
	
	private Node search(RouteNode dest) {
		this.dest = dest;
		Node currentNode;
//		ArrayList<Connection> childStates;
		long successorCost;
		Vector<Node> children = new Vector<Node>();
		while (!(nodes.isEmpty())) {
			currentNode = nodes.firstElement();
//			System.out.println("AStar2 " + currentNode + " size of nodes " + nodes.size() + " size of open " + open.size() + " size of closed " + closed.size());
			if (closed.get(currentNode.state.to) != null) { // to avoid having to remove
				nodes.removeElementAt(0);// improved nodes from nodes
				continue;
			}
			if (!(currentNode.total == bestTotal)) {
				setBest(currentNode.total);
			} 
			if (currentNode.state.to == dest) {
				return currentNode;
			}
			children.removeAllElements();

			expanded++;
//			System.out.println("" + currentNode + " has " +  currentNode.state.to.connected.size() +" successors");
			for (Connection nodeSuccessor: currentNode.state.to.getConnected()) {
				int dTurn=currentNode.fromBearing-nodeSuccessor.startBearing;
				long turnCost=getTurnCost(dTurn);
				if (bestTime) {
					System.out.println("AStar2.search(): only first route mode");
					successorCost = currentNode.costs + nodeSuccessor.times[0] + turnCost;
				} else { 
					successorCost = currentNode.costs + nodeSuccessor.length + turnCost;
				}
				Node openNode = null;
				Node theNode = null;
				Node closedNode =  closed.get(nodeSuccessor.to);
//				if (closedNode == null) {
					openNode = (Node) open.get(nodeSuccessor.to);
//				}
				theNode = (openNode != null) ? openNode : closedNode;
				// in open or closed
				
				if (theNode != null) {
//					System.out.println("" + successorCost + "<" + theNode.costs);
					if (successorCost < theNode.costs) {
						if (closedNode != null) {
							open.put(nodeSuccessor.to, theNode);
							closed.remove(nodeSuccessor.to);
//							System.out.println("add to open;remove form closed " + theNode );
						} else {
//							System.out.println("add to open" + theNode + " set dist to "+ theNode.distance);
							long dist = theNode.distance;
							theNode = new Node(nodeSuccessor, currentNode, successorCost, dist, currentNode.fromBearing);
							open.put(nodeSuccessor.to, theNode); 
						} 
						theNode.costs = successorCost;
						theNode.total = theNode.costs + theNode.distance;
						theNode.parent = currentNode; 
						theNode.fromBearing=currentNode.state.endBearing;
//						System.out.println("add children " + theNode);
						children.addElement(theNode);
					}
					// not in open ore closed
				} else { 
					long estimation;
					Node newNode;
					estimation = estimate(currentNode.state,nodeSuccessor, dest);
					newNode = new Node(nodeSuccessor, currentNode, successorCost, estimation, currentNode.fromBearing);
//					System.out.println("NewNode add to open" + newNode + " set dist to "+ estimation);
					open.put(nodeSuccessor.to, newNode);
					evaluated++;
					children.addElement(newNode);
				}
			}
			open.remove(currentNode.state.to);
			closed.put(currentNode.state.to, currentNode);
			nodes.removeElementAt(0);
			addToNodes(children); // update nodes
		} 
		return null;
		// no open nodes and no solution

	}

	/**
	 * @param nodeSuccessor
	 * @param dest
	 * @return
	 */
	private long estimate(Connection from,Connection to, RouteNode dest) {
		if (noHeuristic) {
			return 0;
		}
		int dTurn = from.endBearing-to.startBearing;
		long turnCost = getTurnCost(dTurn);
		long dist = MyMath.dist(to.to.node, dest.node, 1.2);
		long estimatedSpeed = speed(30);
		if (bestTime){
			if (dist > 100000) {
				estimatedSpeed = speed(100);
			} else if (dist > 50000) {
				estimatedSpeed = speed(80);
			} else if (dist > 10000) {
				estimatedSpeed = speed(60);
			} else if (dist > 5000) {
				estimatedSpeed = speed(45);
			}
			return (long)(dist / estimatedSpeed) + turnCost;
		} else {
			return (long)(dist * 1.1f + turnCost);
		}
	} 
	
	private long speed(int kmh) {
		return (long)(kmh / 3.6);
	}
	
	/**
	 * @param turn
	 * @return
	 */
	private long getTurnCost(int turn) {
		long cost;
		String turnString;
		int adTurn = Math.abs(turn * 2);
		if (adTurn > 150) {
			cost = 150;
			turnString = "wende ";
		} else if (adTurn > 120) {
			cost = 15;
			turnString = "scharf ";
		} else if (adTurn > 60) {
			cost = 10;
			turnString = "";
		} else if (adTurn > 30) {
			cost = 5;
			turnString = "halb ";
		} else {
			cost = 0;
			turnString = "gerade ";			
		}
		if (cost == 0) {
//			System.out.println("gerade aus");
		} else {
//			System.out.println(turnString + ((turn > 0) ? "rechts " : "links ") + adTurn);
		}
		return cost;
//		return 0;
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
			Node newNode = children.elementAt(i);
			long newTotal = newNode.total;
			long newCosts = newNode.costs;
			int idx = bsearch(0, nodes.size()-1, newTotal, newCosts);
			nodes.insertElementAt(newNode, idx); 
		}
	}
	// {{{ public final Vector solve (State initialState) 
	public final Vector<Connection> solve (RouteNode start,RouteNode dest) {
		long totalDist = MyMath.dist(start.node, dest.node, 1.2);
		long estimateSpeed = 0;
		System.out.println("AStar2.solve(): only first route mode");
		int times[] = new int[1];
		times[0] = 0;
		Connection initialState = new Connection(start, (short)0, times, (byte)0, (byte)0, null);
		Node solution;
		Node firstNode;
		long estimation;
		expanded = 0;
		evaluated = 1;
		estimation=0;
//		estimation = estimate(initialState, dest);
		firstNode = new Node(initialState, null, 0l, estimation, (byte)0);
		open.put(initialState.to, firstNode);
		nodes.addElement(firstNode);
		solution = search(dest);
		nodes.removeAllElements();
		open.clear();
		closed.clear();
		ready = true;
		setBest(bestTotal);
		System.out.println("best=" + bestTotal + " expanded=" + expanded);
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
		while (!newBest) {
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
		byte fromBearing;
		Node parent;

		Node(Connection theState, Node theParent, long theCosts, long theDistance, byte bearing) {
//			theDistance = 1000000 - theDistance;
			state = theState;
			parent = theParent;
			costs = theCosts;
			distance = theDistance;
			total = theCosts + (theDistance);
			fromBearing = bearing;
		}

		public String toString() {
			return "Node id=" + state.to.node.id + " g=" + costs + " h=" + distance + " f=" + total;
		}
	}
}
