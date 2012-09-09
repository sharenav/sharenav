/*
 * ShareNav - Copyright (c) 2007 Harald Mueller james22 at users dot sourceforge dot net
 * 			Copyright (c) 2008, 2009 Kai Krueger apmonkey at users dot sourceforge dot net
 * See COPYING
 */

package net.sharenav.sharenav.data;
//#if polish.api.fileconnection
import javax.microedition.io.Connection;
import javax.microedition.io.Connector;
import javax.microedition.io.file.FileConnection;
//#endif

import de.enough.polish.util.Locale;
import net.sharenav.sharenav.ui.ShareNav;
import net.sharenav.sharenav.ui.GuiDiscover;
import net.sharenav.sharenav.ui.Trace;
import net.sharenav.util.Logger;


public class ConfigExportImport {
	//#if polish.api.fileconnection
	private final static Logger logger = Logger.getInstance(GuiDiscover.class, Logger.DEBUG);

	public static void exportConfig(String url) {
		try {
			FileConnection con = (FileConnection)Connector.open(url);
			if (!con.exists()) {
				con.create();
			}
			Configuration.serialise(con.openOutputStream());
			String name = con.getName();
			con.close();
			ShareNav.getInstance().alert(Locale.get("generic.Info")/*Info*/, 
						   Locale.get("guidiscover.CfgExported", url)/*Configuration exported to '<file>'*/, 3000);
		} catch (Exception e) {
			logger.exception(Locale.get("guidiscover.CouldNotSaveCfg")/*Could not save configuration*/
					 + ": " + e.getMessage(), e);
		}
	}

	public static void importConfig(String url) {
		try {
			FileConnection con = (FileConnection)Connector.open(url);
			Configuration.deserialise(con.openInputStream());
			con.close();
			Trace.uncacheIconMenu();
			ShareNav.getInstance().alert(Locale.get("generic.Info")/*Info*/, 
						   Locale.get("guidiscover.CfgImported", url)/*Configuration imported from '<file>'*/, 3000);
		} catch (Exception e) {
			logger.exception(Locale.get("guidiscover.CouldNotLoadCfg")/*Could not load configuration*/
					 + ": " + e.getMessage(), e);
		}
	}
	//#endif
}
