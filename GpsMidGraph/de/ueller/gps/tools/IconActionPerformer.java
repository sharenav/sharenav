package de.ueller.gps.tools;

public interface IconActionPerformer {
	public final static int BACK_ACTIONID = Integer.MAX_VALUE;
	
	public void performIconAction(int actionId);

	/** recreate the icon menu from scratch and show it (introduced for reflecting size change of the Canvas) */
	public void recreateAndShowIconMenu();

}
