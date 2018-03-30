package com.lucene;

import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;

import com.lucene.model.SpatialSearch;
import com.lucene.pojo.Status;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

import com.lucene.pojo.RPolygon;
import com.lucene.pojo.SearchResult;
import com.spatial4j.core.shape.Point;

/**
 * @author  abel-sun
 */
@ComponentScan("com.lucene.model")
@RestController
public class IndexController {
	private final Logger log = LoggerFactory.getLogger(this.getClass());

	@Autowired
	public SpatialSearch spatial;

	@RequestMapping(value = "/build", method = RequestMethod.GET)
	@ResponseBody
	public Status build() {

		Status status = new Status();
		long from = System.currentTimeMillis();
		log.info("start to build quad tree..." + from);

		try {
			spatial.createIndex(300 * 10000, true);
			status.setStatus(0);
		} catch (Exception e) {
			status.setStatus(1);
			e.printStackTrace();
		}
		long to = System.currentTimeMillis();
		try {
			status.setDocNum(spatial.getDocNum());
		} catch (IOException e) {
			e.printStackTrace();
		}
		log.info("finish build quad tree..." + to);
		status.setTimeMillis(to - from);

		return status;
	}

	@RequestMapping(value = "/loop", method = RequestMethod.GET)
	@ResponseBody
	public void loop() {
		while (true) {
			long from = System.currentTimeMillis();
			log.info("start to build quad tree..." + from);

			try {
				spatial.createIndex(100000, false);
			} catch (Exception e) {
				e.printStackTrace();
			}
			long to = System.currentTimeMillis();
			log.info("finish build quad tree..." + to);
			log.info("speed: " + 100000 * 1000 / (to - from) + "/s");
		}
	}

	@RequestMapping(value = "/searchcircle", method = RequestMethod.GET)
	@ResponseBody
	public SearchResult searchcircle(@RequestParam(value = "lat", required = true) String lat,
			@RequestParam(value = "lon", required = true) String lon,
			@RequestParam(value = "radius", required = true) String radius) {
		SearchResult r = new SearchResult();
		r.setStatus(0);
		try {
			r = spatial.searchCircle(Double.parseDouble(radius),
					spatial.makePoint(Double.parseDouble(lat), Double.parseDouble(lon)));
		} catch (Exception e) {
			log.error("error search, ", e);
			r.status = 1;
		}
		return r;
	}

	@RequestMapping(value = "/searchbbox", method = RequestMethod.GET)
	@ResponseBody
	public SearchResult searchbbox(@RequestParam(value = "maxlat", required = true) String maxlat,
			@RequestParam(value = "maxlon", required = true) String maxlon,
			@RequestParam(value = "minlat", required = true) String minlat,
			@RequestParam(value = "minlon", required = true) String minlon) {
		SearchResult r = new SearchResult();
		r.setStatus(0);
		try {
			r = spatial.searchBBox(Double.parseDouble(minlat), Double.parseDouble(minlon), Double.parseDouble(maxlat),
					Double.parseDouble(maxlon));
		} catch (Exception e) {
			log.error("error search, ", e);
			r.status = 1;
		}
		return r;
	}

	@RequestMapping(value = "/searchpolygon", method = RequestMethod.POST)
	@ResponseBody
	public SearchResult searchpolygon(@RequestBody RPolygon polygon) {
		SearchResult r = new SearchResult();
		r.setStatus(0);
		try {
			List<Point> points = polygon.getPolygon().stream().map(x -> spatial.makePoint(x.getLat(), x.getLon()))
					.collect(Collectors.toList());
			r = spatial.searchPolygon(points);
		} catch (Exception e) {
			log.error("error search, ", e);
			r.status = 1;
		}
		return r;
	}

}
