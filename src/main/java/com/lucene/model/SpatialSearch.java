package com.lucene.model;

import java.io.IOException;
import java.text.ParseException;
import java.util.List;
import java.util.Random;
import java.util.stream.Collectors;

import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.FieldType;
import org.apache.lucene.document.StoredField;
import org.apache.lucene.index.CorruptIndexException;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.index.IndexableField;
import org.apache.lucene.index.Term;
import org.apache.lucene.search.Filter;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.MatchAllDocsQuery;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.spatial.SpatialStrategy;
import org.apache.lucene.spatial.prefix.RecursivePrefixTreeStrategy;
import org.apache.lucene.spatial.prefix.tree.GeohashPrefixTree;
import org.apache.lucene.spatial.prefix.tree.SpatialPrefixTree;
import org.apache.lucene.spatial.query.SpatialArgs;
import org.apache.lucene.spatial.query.SpatialOperation;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.LockObtainFailedException;
import org.apache.lucene.store.RAMDirectory;
import org.apache.lucene.util.Version;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.lucene.pojo.SearchResult;
import com.spatial4j.core.context.SpatialContext;
import com.spatial4j.core.context.jts.JtsSpatialContext;
import com.spatial4j.core.distance.DistanceUtils;
import com.spatial4j.core.shape.Point;
import com.spatial4j.core.shape.Shape;

@Component
public class SpatialSearch {
	private final Logger logger = LoggerFactory.getLogger(this.getClass());
	StandardAnalyzer a = new StandardAnalyzer();
	IndexWriterConfig iwc = new IndexWriterConfig(Version.LUCENE_4_10_4, a);
	private static Directory ramDirectory = new RAMDirectory();
	private SpatialContext ctx;
	private JtsSpatialContext jtsCtx;
	private SpatialStrategy strategy;
	private IndexSearcher searcher;
	private IndexReader indexReader;
	private IndexWriter indexWriter;
	Random r = new Random();

	public SpatialSearch() {
		this.ctx = SpatialContext.GEO;
		jtsCtx = JtsSpatialContext.GEO;

		SpatialPrefixTree grid = new GeohashPrefixTree(ctx, 11);
		this.strategy = new RecursivePrefixTreeStrategy(grid, "location");
		try {
			indexWriter = new IndexWriter(ramDirectory, iwc);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	public void createIndex(int count, boolean isClose)
			throws CorruptIndexException, LockObtainFailedException, IOException {

		for (int i = 0; i < count; i++) {
			int id = r.nextInt(3000000);
			indexWriter.updateDocument(new Term("id", "" + id),
					newGeoDocument(id, "number_" + id, randomSingaporePoint()));
		}
		indexWriter.commit();
		if (isClose)
			indexWriter.close();
	}

	private void setSearchPath() {
		try {
			this.indexReader = DirectoryReader.open(ramDirectory);
			this.searcher = new IndexSearcher(indexReader);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	private Point randomSingaporePoint() {
		int Low = 1239352;
		int High = 1469751;
		int lon = r.nextInt(High - Low) + Low;

		Low = 103635882;
		High = 104004620;
		int lat = r.nextInt(High - Low) + Low;

		return ctx.makePoint(lat / 1000000.0, lon / 1000000.0);
	}

	public Point makePoint(double lat, double lon) {
		return ctx.makePoint(lat, lon);
	}

	private Document newGeoDocument(int id, String misisdn, Shape shape) {
		FieldType ft = new FieldType();
		ft.setIndexed(true);
		ft.setStored(true);

		Document doc = new Document();
		doc.add(new Field("id", "" + id, ft));
		doc.add(new Field("misisdn", misisdn, ft));
		for (IndexableField f : strategy.createIndexableFields(shape)) {
			doc.add(f);
		}
		doc.add(new StoredField(strategy.getFieldName(), ctx.toString(shape)));
		return doc;
	}

	public int getDocNum() throws IOException {
		setSearchPath();
		return indexReader.numDocs();
	}

	public SearchResult searchCircle(double radius, Point center) throws IOException {
		setSearchPath();
		SpatialArgs args = new SpatialArgs(SpatialOperation.IsWithin, ctx.makeCircle(center.getX(), center.getY(),
				DistanceUtils.dist2Degrees(radius, DistanceUtils.EARTH_MEAN_RADIUS_KM)));

		Filter filter = strategy.makeFilter(args);
		TopDocs topDocs = searcher.search(new MatchAllDocsQuery(), filter, indexReader.numDocs());

		ScoreDoc[] scoreDocs = topDocs.scoreDocs;
		SearchResult result = new SearchResult();
		for (ScoreDoc s : scoreDocs) {
			result.result.add(searcher.doc(s.doc).get("misisdn"));
		}
		result.total = topDocs.totalHits;
		return result;
	}

	public SearchResult searchBBox(Double minLat, Double minLng, Double maxLat, Double maxLng) throws IOException {
		setSearchPath();
		SpatialArgs args = new SpatialArgs(SpatialOperation.IsWithin,
				ctx.makeRectangle(minLat, maxLat, minLng, maxLng));

		Filter filter = strategy.makeFilter(args);
		TopDocs topDocs = searcher.search(new MatchAllDocsQuery(), filter, indexReader.numDocs());

		SearchResult result = new SearchResult();
		ScoreDoc[] scoreDocs = topDocs.scoreDocs;
		for (ScoreDoc s : scoreDocs) {
			result.result.add(searcher.doc(s.doc).get("misisdn"));
		}
		result.total = topDocs.totalHits;
		return result;
	}

	public SearchResult searchPolygon(List<Point> points) throws IOException, ParseException {
		String joinedPoints = points.stream().map(x -> x.getX() + " " + x.getY()).collect(Collectors.joining(","));
		String points_str = "POLYGON((" + joinedPoints + "))";
		Shape shape = jtsCtx.readShapeFromWkt(points_str);

		setSearchPath();
		SpatialArgs args = new SpatialArgs(SpatialOperation.IsWithin, shape);

		Filter filter = strategy.makeFilter(args);
		TopDocs topDocs = searcher.search(new MatchAllDocsQuery(), filter, indexReader.numDocs());

		SearchResult result = new SearchResult();
		ScoreDoc[] scoreDocs = topDocs.scoreDocs;
		for (ScoreDoc s : scoreDocs) {
			result.result.add(searcher.doc(s.doc).get("misisdn"));
		}
		result.total = topDocs.totalHits;
		return result;
	}
}