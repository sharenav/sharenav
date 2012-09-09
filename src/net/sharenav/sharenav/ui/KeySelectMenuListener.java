package net.sharenav.sharenav.ui;

import net.sharenav.midlet.ui.KeySelectMenuItem;

public interface KeySelectMenuListener {
	public void keySelectMenuSearchString(String searchString);
	public void keySelectMenuResetMenu();
	public void keySelectMenuItemSelected(short poiType);
	public void keySelectMenuCancel();

}
