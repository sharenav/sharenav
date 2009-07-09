package de.ueller.midlet.gps;

import de.ueller.midlet.gps.data.KeySelectMenuItem;

public interface KeySelectMenuListener {
	public void searchString(String searchString);
	public void resetMenu();
	public void itemSelected(KeySelectMenuItem item);
	public void cancel();

}
