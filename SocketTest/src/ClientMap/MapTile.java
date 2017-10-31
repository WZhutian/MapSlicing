package ClientMap;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import org.dom4j.Document;
import org.dom4j.DocumentException;
import org.dom4j.Element;
import org.dom4j.io.SAXReader;
//import org.geotools.coverage.processing.operation.Scale;
import java.util.List;
import com.ecity.map.define.TileLevel;
import com.esri.schemas.arcgis._10.HttpException;

import ClientMap.GetImageData;
import ServerMap.InitMapInfo;
public class MapTile {
	private File logFile = null;
	private static MapTile mp = null;
	private int imageCount = 0;
	static InitMapInfo init = null;
	static GetImageData getdata = new GetImageData();
	// static CountDownLatch countdownLatch = new CountDownLatch(2);
	// static long[] Sum;
	public static double Alltime=0;
    public static double iotime=0;
    public static double seqtime=0;
	// ���캯���Է����б���г�ʼ��
	MapTile() {
		String filepath = "resouce/service.xml";// ע��filepath�����ݣ�
		File file = new File(filepath);
		if (!file.exists())
			return;
		SAXReader saxReader = new SAXReader();
		Document document = null;
		try {
			document = saxReader.read(file);
		} catch (DocumentException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		init = getinitInstance();
		// jason
		Element root = document.getRootElement();
		Element nodeTileCacheInfo = (Element) root.selectSingleNode("ServiceJason");
		init.infourl = nodeTileCacheInfo.selectSingleNode("url").getStringValue();
		//
		Element CacheInfo = (Element) root.selectSingleNode("CachePath");
		init.cachePath = CacheInfo.selectSingleNode("Curl").getStringValue();
	}

	public static MapTile getInstance() {
		if (null == mp) {
			synchronized (MapTile.class) {
				if (null == mp) {
					mp = new MapTile();
				}
			}
		}
		return mp;
	}

	public static InitMapInfo getinitInstance() {
		if (null == init) {
			synchronized (MapTile.class) {
				if (null == init) {
					init = new InitMapInfo();
				}

			}
		}
		return init;
	}

	/**
	 * ��ȡ��ǰʱ����ַ�������
	 * 
	 * @return
	 */
	public String getNowString() {
		return new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss:SSS").format(new Date());
	}

	/**
	 * ��ʽ�����֣�ǰ��λ
	 * 
	 * @param in
	 * @param length
	 * @param ch
	 *            ��λ�ַ�
	 * @return
	 */
	public String formatLeftString(String in, int length, String ch) {
		while (in.length() < length) {
			in = ch + in;
		}
		return in;
	}

	/**
	 * z ��д��־�ļ�
	 * 
	 * @param args
	 */
	public void wirteLog(String message) {
		try {
			String sDate = new java.text.SimpleDateFormat("MM-dd-HH:mm:ss:SSS").format(new Date());
			if (this.logFile == null) {
				this.logFile = new File(init.cachePath + "\\tile" + ".log");
				if (!this.logFile.exists())
					this.logFile.createNewFile();
			}
			BufferedWriter bw = new BufferedWriter(new FileWriter(logFile, true));
			bw.write(message);
			bw.newLine();
			bw.flush();
			bw.close();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	/**
	 * ת��intΪbyte����
	 * 
	 * @param bb
	 * @param x
	 * @param index
	 */
	byte[] int2byte(int x) {
		byte[] bb = new byte[4];
		bb[3] = (byte) (x >> 24);
		bb[2] = (byte) (x >> 16);
		bb[1] = (byte) (x >> 8);
		bb[0] = (byte) (x >> 0);
		return bb;
	}

	/**
	 * ��ȡ��������Ƭ�ļ������� ��ȡ��������Ƭ�ļ������� �����·�������� �򴴽�һ��·��
	 * 
	 * @param bundleRow
	 *            ��
	 * @param bundleCol
	 *            ��
	 * @param level
	 *            ����
	 * @param levelPath
	 *            ·��
	 * @return
	 */
	public String getCompactBundlePathName(int bundleRow, int bundleCol, int level, String levelPath) {
		int rGroup = 128 * bundleRow;
		String r = "R" + formatLeftString(Integer.toHexString(rGroup), 4, "0");

		int cGroup = 128 * bundleCol;
		String c = "C" + formatLeftString(Integer.toHexString(cGroup), 4, "0");// תΪ16����

		String bundleBase = String.format("%s%s%s%s", levelPath, File.separator, r, c);
		return bundleBase;
	}
	/**
	 * ������ת��Ϊʱ����
	 */
	
}
