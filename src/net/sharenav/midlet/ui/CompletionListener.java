/*
 * ShareNav - Copyright (c) 2008 Kai Krueger apm at users dot sourceforge dot net 
 * See COPYING
 */

package net.sharenav.midlet.ui;

/** Interface to inform a listener when an arbitrary operation has completed.
 * Classes which want to use ProgressDisplay or CustomMenu must implement this.
 */
public interface CompletionListener {
	
	/** Called when the operation was completed.
	 */
	public void actionCompleted();
}
