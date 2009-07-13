package de.ueller.gps.tools;

public interface IconActionPerformer {
	public final static int BACK_ACTIONID = Integer.MAX_VALUE;
	
	public void performIconAction(int actionId);
}
