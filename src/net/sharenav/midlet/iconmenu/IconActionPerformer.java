package net.sharenav.midlet.iconmenu;

public interface IconActionPerformer {
	public final static int BACK_ACTIONID = Byte.MAX_VALUE;
	
	public void performIconAction(int actionId, String choiceName);

	/** recreate the icon menu from scratch and show it (introduced for reflecting size change of the Canvas) */
	public void recreateAndShowIconMenu();

}
