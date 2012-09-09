package net.sharenav.osmToShareNav.model.name;

import java.util.Hashtable;
import java.lang.Object;
import java.lang.Long;

/**
 * @author jkpj
 *
 */
public class WayRedirect {
	private Hashtable<Long,Long> redirectWays = null;
	
	public WayRedirect() {
		redirectWays = new Hashtable<Long,Long>();
	}
	public void put(Long id, Long target) {
		redirectWays.put(id, target);
	}
	public Long get(Long id) {
		return redirectWays.get(id);
	}
}
