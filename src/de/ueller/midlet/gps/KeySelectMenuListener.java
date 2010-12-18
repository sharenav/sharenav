package de.ueller.midlet.gps;

import de.ueller.midlet.gps.data.KeySelectMenuItem;

public interface KeySelectMenuListener {
	public void keySelectMenuSearchString(String searchString);
	public void keySelectMenuResetMenu();
	public void keySelectMenuItemSelected(KeySelectMenuItem item);
	public void keySelectMenuCancel();

}
