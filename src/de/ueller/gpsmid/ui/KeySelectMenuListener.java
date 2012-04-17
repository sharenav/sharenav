package de.ueller.gpsmid.ui;

import de.ueller.midlet.ui.KeySelectMenuItem;

public interface KeySelectMenuListener {
	public void keySelectMenuSearchString(String searchString);
	public void keySelectMenuResetMenu();
	public void keySelectMenuItemSelected(short poiType);
	public void keySelectMenuCancel();

}
