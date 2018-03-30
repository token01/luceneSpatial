package org.tko.lucene.pojo;

import java.util.ArrayList;


/**
 *  实体Rp
 * @author  abel-sun
 */
public class RPolygon {
	ArrayList<RPoint> polygon = new ArrayList<RPoint>();

	public ArrayList<RPoint> getPolygon() {
		return polygon;
	}

	public void setPolygon(ArrayList<RPoint> polygon) {
		this.polygon = polygon;
	}

}
