package socketmap;

import java.awt.image.BufferedImage;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.Date;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;

import javax.imageio.ImageIO;
import javax.imageio.ImageReadParam;
import javax.imageio.ImageReader;
import javax.imageio.stream.ImageInputStream;

import org.apache.commons.lang.text.StrBuilder;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;
import org.dom4j.Document;
import org.dom4j.DocumentException;
import org.dom4j.DocumentHelper;
import org.dom4j.Element;
import org.dom4j.io.OutputFormat;
import org.dom4j.io.SAXReader;
import org.dom4j.io.XMLWriter;

import com.ecity.map.define.TileInfo;
import com.ecity.map.define.TileLevel;

import com.ecity.server.response.geometry.Envelope;
import com.esri.schemas.arcgis._10.HttpException;

public class InitMapInfo implements java.io.Serializable {

	public Envelope fullextent;
	public String cachePath = null;// "E:\\test6\\";
	public int width = 256;
	public int height = 256;
	public TileInfo tileInfo;
	public String infourl; // ="http://localhost:8080/ServiceEngine/rest/services/hmap/MapServer?f=json";
	private String storageMode = "esriMapCacheStorageModeCompact";

	/**
	 * 创建瓦片缓存 ExFunc
	 * 
	 * @param tileMapOpt
	 * @throws IOException
	 */

	public void InitMapTile() throws IOException {
		fullextent = new Envelope();
		// 根据服务初始化瓦片信息

		// 读取瓦片元数据
		getTileInfoFromFile(initMap(infourl));
		// 初始化元数据

		// 创建缓存文件
		this.saveTileConfFile();
		this.saveTileCdiFile();
	}

	private Envelope initMap(String urls) {

		// 根据服务初始化瓦片信息
		try {
			URL url = new URL(urls);
			HttpURLConnection con = (HttpURLConnection) url.openConnection();
			con.setDoOutput(true);
			con.setRequestMethod("GET");
			InputStream in = con.getInputStream();
			BufferedReader bf = new BufferedReader(new InputStreamReader(in));
			String str = "";
			StrBuilder sb = new StrBuilder();
			while ((str = bf.readLine()) != null) {
				sb.append(str);
			}
			str = sb.toString();
			return GetFullextent(str);
		} catch (Exception e) {
			return null;
		}
	}

	public Envelope GetFullextent(String jsonStr) {
		try {
			// 将json字符串转换为json对象
			JSONObject jsonObj = new JSONObject(jsonStr);
			// 得到指定json key对象的value对象
			JSONObject ExtentObj = jsonObj.getJSONObject("fullExtent");
			// 获取之对象的所有属性
			fullextent.xmin = ExtentObj.getDouble("xmin");
			fullextent.xmax = ExtentObj.getDouble("xmax");
			fullextent.ymin = ExtentObj.getDouble("ymin");
			fullextent.ymax = ExtentObj.getDouble("ymax");
			JSONObject SpatialObj = ExtentObj.getJSONObject("spatialReference");
			// fullextent.spatialReference.wkid= SpatialObj.getInt("wkid");
		} catch (JSONException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		return fullextent;
	}

	public void getTileInfoFromFile(Envelope fullextent) {
		String configCdi = this.cachePath + "\\conf.xml";
		File f = new File(configCdi);
		if (!f.exists())
			return;
		SAXReader saxReader = new SAXReader();
		TileInfo aTileInfo = new TileInfo();
		double mapWidth = fullextent.xmax - fullextent.xmin;
		double mapHeight = fullextent.ymax - fullextent.ymin;
		try {
			Document document = saxReader.read(f);
			Element root = document.getRootElement();
			Element nodeTileCacheInfo = (Element) root.selectSingleNode("TileCacheInfo");

			aTileInfo.cols = Integer.valueOf(nodeTileCacheInfo.selectSingleNode("TileCols").getStringValue());
			aTileInfo.rows = Integer.valueOf(nodeTileCacheInfo.selectSingleNode("TileRows").getStringValue());
			aTileInfo.dpi = Integer.valueOf(nodeTileCacheInfo.selectSingleNode("DPI").getStringValue());

			this.width = aTileInfo.rows;
			this.height = aTileInfo.cols;
			// SpatialReference

			aTileInfo.spatialReference.wkid = fullextent.spatialReference.wkid;
			// LODInfos
			List nodes = nodeTileCacheInfo.selectNodes("LODInfos/LODInfo");
			double scales[] = new double[nodes.size()];
			for (int i = 0; i < nodes.size(); i++) {
				TileLevel LODInfo = new TileLevel();
				Element lodNode = (Element) nodes.get(i);
				LODInfo.level = Integer.parseInt(lodNode.selectSingleNode("LevelID").getStringValue());
				LODInfo.scale = Double.parseDouble(lodNode.selectSingleNode("Scale").getStringValue());
				scales[i] = LODInfo.scale;
				LODInfo.resolution = Double.parseDouble(lodNode.selectSingleNode("Resolution").getStringValue());
				aTileInfo.lods.add(LODInfo);
			}
			Element scaleNode = (Element) nodes.get(nodes.size() - 1);

			double reslution = (25.39999918 / aTileInfo.dpi) * scales[scales.length - 1] / 1000;// 米/像素
			int cols = (int) Math.ceil(mapWidth / this.width / reslution);
			int rows = (int) Math.ceil(mapHeight / this.height / reslution);
			aTileInfo.origin.x = fullextent.xmin - (cols * this.width * reslution - mapWidth) / 2;
			aTileInfo.origin.y = fullextent.ymax + (rows * this.height * reslution - mapHeight) / 2;// 这里是推荐的切片原点。注意切片的原点在视图中的左上角，所以Y值有所区别
			// TileImageInfo
			Element nodeTileImageInfo = (Element) root.selectSingleNode("TileImageInfo");
			String format = nodeTileImageInfo.selectSingleNode("CacheTileFormat").getStringValue();
			if (format.startsWith("png"))
				aTileInfo.format = "png";
			else if (format.endsWith("gif"))
				aTileInfo.format = "gif";
			else
				aTileInfo.format = "jpg";

			// CacheStorageInfo
			Element nodeTileStorageFormat = (Element) root.selectSingleNode("CacheStorageInfo");
			this.storageMode = nodeTileStorageFormat.selectSingleNode("StorageFormat").getStringValue();
			this.tileInfo = aTileInfo;
		} catch (DocumentException e) {
			throw new HttpException(e.getMessage());
		}
	}

	/**
	 * 构造瓦片元数据信息
	 * 
	 * @param tileMapOpt
	 */

	/**
	 * 生成并保存cdi文件
	 * 
	 * @throws IOException
	 */
	private void saveTileCdiFile() throws IOException {
		String configCdi = this.cachePath + "\\conf.cdi";
		Document document = DocumentHelper.createDocument();

		Element root = document.addElement("EnvelopeN");
		// xsi:type="typens:EnvelopeN"
		// xmlns:typens="http://www.esri.com/schemas/ArcGIS/10.0"
		// root.addAttribute("xsi:type", "typens:EnvelopeN");
		// root.addAttribute("xmlns:typens",
		// "http://www.ecitychina.com/serviceeninge/1.0");
		root.addElement("XMin").setText(String.valueOf(this.fullextent.xmin));
		root.addElement("YMin").setText(String.valueOf(this.fullextent.ymin));
		root.addElement("XMax").setText(String.valueOf(this.fullextent.xmax));
		root.addElement("YMax").setText(String.valueOf(this.fullextent.ymax));

		String wkt = this.tileInfo.spatialReference.wkt == null ? "" : this.tileInfo.spatialReference.wkt;
		String wkid = this.tileInfo.spatialReference.wkid == null ? "5412"
				: this.tileInfo.spatialReference.wkid.toString();

		Element eSpatialReference = root.addElement("SpatialReference");
		// eSpatialReference.addAttribute("xsi:type",
		// "typens:ProjectedCoordinateSystem");
		eSpatialReference.addElement("WKT").setText(wkt);
		eSpatialReference.addElement("XOrigin").setText(String.valueOf(this.tileInfo.origin.x));
		eSpatialReference.addElement("YOrigin").setText(String.valueOf(this.tileInfo.origin.y));
		eSpatialReference.addElement("XYScale").setText("");
		eSpatialReference.addElement("ZOrigin").setText("");
		eSpatialReference.addElement("ZScale").setText("");
		eSpatialReference.addElement("MOrigin").setText("");
		eSpatialReference.addElement("MScale").setText("");
		eSpatialReference.addElement("XYTolerance").setText("");
		eSpatialReference.addElement("ZTolerance").setText("");
		eSpatialReference.addElement("MTolerance").setText("");
		eSpatialReference.addElement("HighPrecisi").setText("");
		eSpatialReference.addElement("WKID").setText(wkid);

		OutputFormat format = OutputFormat.createPrettyPrint();
		format.setEncoding("utf-8");
		XMLWriter writer = new XMLWriter(new FileWriter(new File(configCdi)), format);
		writer.write(document);
		writer.close();
	}

	/**
	 * 生成并保存conf.xml文件
	 * 
	 * @throws IOException
	 */
	private void saveTileConfFile() throws IOException {
		String configCdi = this.cachePath + "\\conf.xml";
		Document document = DocumentHelper.createDocument();

		Element root = document.addElement("CacheInfo");
		// xsi:type="typens:EnvelopeN"
		// xmlns:typens="http://www.esri.com/schemas/ArcGIS/10.0"
		// root.addAttribute("xsi:type", "typens:CacheInfo");
		// root.addAttribute("xmlns:typens",
		// "http://www.ecitychina.com/serviceeninge/1.0");

		// TileCacheInfo
		Element TileCacheInfo = root.addElement("TileCacheInfo");
		// TileCacheInfo.addAttribute("xsi:type", "typens:TileCacheInfo");
		String wkt = this.tileInfo.spatialReference.wkt == null ? "" : this.tileInfo.spatialReference.wkt;
		String wkid = this.tileInfo.spatialReference.wkid == null ? "5412"
				: this.tileInfo.spatialReference.wkid.toString();

		Element eSpatialReference = TileCacheInfo.addElement("SpatialReference");
		eSpatialReference.addElement("WKT").setText(wkt);
		eSpatialReference.addElement("XOrigin").setText("");
		eSpatialReference.addElement("YOrigin").setText("");
		eSpatialReference.addElement("XYScale").setText("");
		eSpatialReference.addElement("ZOrigin").setText("");
		eSpatialReference.addElement("ZScale").setText("");
		eSpatialReference.addElement("MOrigin").setText("");
		eSpatialReference.addElement("MScale").setText("");
		eSpatialReference.addElement("XYTolerance").setText("");
		eSpatialReference.addElement("ZTolerance").setText("");
		eSpatialReference.addElement("MTolerance").setText("");
		eSpatialReference.addElement("HighPrecisi").setText("");
		eSpatialReference.addElement("WKID").setText(wkid);

		Element TileOrigin = TileCacheInfo.addElement("TileOrigin");
		// TileOrigin.addAttribute("xsi:type", "typens:PointN");
		TileOrigin.addElement("X").setText(String.valueOf(this.tileInfo.origin.x));
		TileOrigin.addElement("Y").setText(String.valueOf(this.tileInfo.origin.y));

		TileCacheInfo.addElement("TileCols").setText(String.valueOf(this.tileInfo.cols));
		TileCacheInfo.addElement("TileRows").setText(String.valueOf(this.tileInfo.rows));
		TileCacheInfo.addElement("DPI").setText(String.valueOf(this.tileInfo.dpi));

		Element LODInfos = TileCacheInfo.addElement("LODInfos");
		for (int i = 0; i < this.tileInfo.lods.size(); i++) {
			Element LODInfo = LODInfos.addElement("LODInfo");
			LODInfo.addElement("LevelID").setText(String.valueOf(this.tileInfo.lods.get(i).level));
			LODInfo.addElement("Scale").setText(String.valueOf(this.tileInfo.lods.get(i).scale));
			LODInfo.addElement("Resolution").setText(String.valueOf(this.tileInfo.lods.get(i).resolution));
		}

		// TileImageInfo
		Element TileImageInfo = root.addElement("TileImageInfo");
		// TileImageInfo.addAttribute("xsi:type", "typens:TileImageInfo");
		TileImageInfo.addElement("CacheTileFormat").setText(this.tileInfo.format);
		TileImageInfo.addElement("CompressionQuality").setText(String.valueOf(this.tileInfo.compressionQuality));
		TileImageInfo.addElement("Antialiasing").setText("false");

		// CacheStorageInfo
		Element CacheStorageInfo = root.addElement("CacheStorageInfo");
		// CacheStorageInfo.addAttribute("xsi:type", "typens:CacheStorageInfo");
		CacheStorageInfo.addElement("StorageFormat").setText(this.storageMode);// esriMapCacheStorageModeCompact
		CacheStorageInfo.addElement("PacketSize").setText("128");

		OutputFormat format = OutputFormat.createPrettyPrint();
		format.setEncoding("utf-8");
		XMLWriter writer = new XMLWriter(new FileWriter(new File(configCdi)), format);
		writer.write(document);
		writer.close();
	}

}
