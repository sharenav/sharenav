package de.ueller.gps.tools;

public interface IconMenuPageInterface {
	public static final int IMP_ACTION_NONE = 0;
	public static final int IMP_ACTION_NEXT_TAB = 1;
	public static final int IMP_ACTION_PREV_TAB = 2;
	public static final int IMP_ACTION_ENTER_TAB_BUTTONS = 3;
	public static final int IMP_ACTION_END_MENU = 4;
	
	public void iconMenuPageAction(int impAction);
}
