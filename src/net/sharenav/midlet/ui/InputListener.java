/*
 * ShareNav - Copyright (c) 2008 Kai Krueger apm at users dot sourceforge dot net 
 * See COPYING
 */

package net.sharenav.midlet.ui;

/** Interface to return a string that the user enters in a separate screen.
 * Classes which want to use GuiNameEnter must implement this.
 */
public interface InputListener {
	
	/** Called when the user has finished the string entering operation.
	 * @param strResult String entered by the user or null if he cancelled the operation.
	 */
	public void inputCompleted(String strResult);
}
