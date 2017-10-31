package socketmap;

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

import socketmap.GetImageData;

public class MapTile {
	private File logFile = null;
	private static MapTile mp = null;
	private int imageCount = 0;
	static InitMapInfo init = null;
	static GetImageData getdata = new GetImageData();
	// static CountDownLatch countdownLatch = new CountDownLatch(2);
	// static long[] Sum;

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
		//
		//
		// List nodes = root.selectNodes("ServiceCaches/ServiceCache");
		// String url[]=new String[nodes.size()];
		// for (int i = 0; i < nodes.size(); i++)
		// {
		// Server server = new Server();
		// Element lodNode = (Element) nodes.get(i);
		// server.url = lodNode.selectSingleNode(
		// "Surl").getStringValue();
		// listServer.add(server);
		// server.isuse = true;
		// }
		//
		/*
		 * listServer = new ArrayList<Server>(); for (int i = 0; i < 3; i++) {
		 * Server server = new Server(); server.url = "http://localhost:808" + i
		 * + "/ServiceEngine/rest/services/hmap/MapServer"; server.isuse = true;
		 * listServer.add(server); }
		 */

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
	 * �Գ�ʼ�������Ƭ��Ϣ���зָ����ɶ�Ӧ����
	 * 
	 * @param args
	 * @throws IOException
	 */
	public static void main(String[] args) throws IOException {
		// new ExportMapTileWindow();

		mp = getInstance();

		init.InitMapTile();
		mp.ExportCompactTileMap();

	}
	//

	/**
	 * ������������Ƭ
	 */
	public void ExportCompactTileMap() {

		List<TileLevel> lods = init.tileInfo.lods;// �����Ϣ
		// long[] Sum=new long[lods.size()];
		List<ThreadExportOption> listRunExportOption = new ArrayList<ThreadExportOption>();
		ThreadPoolExecutor executor = null;
		executor = new ThreadPoolExecutor(4, 4, 3, TimeUnit.HOURS, new LinkedBlockingQueue<Runnable>());
		System.out.println(lods.size() + "��");
		for (int il = 0; il < lods.size(); il++)// ��ÿһ���ڽ��м�����
		{
			TileLevel levelInfo = lods.get(il);
			int level = levelInfo.level;

			double scale = levelInfo.scale;
			double reslution = levelInfo.resolution;
			int threadExecCnt = 0;
			double tileWidth = init.width * reslution;
			double tileHeigh = init.height * reslution;
			String levelDir = init.cachePath + "\\_alllayers" + "\\L" + formatLeftString(String.valueOf(level), 2, "0");

			File f = new File(levelDir);
			if (!f.exists() && !f.mkdirs()) {
				throw new HttpException("create dir faile");
			}
			// ///////////////////////////////////////////////////////////////////////
			// ���������� ����
			// ���ﲻ��ֱ����mapEnvp�Ŀ�͸������� �������׳��ֶ�һ�л��е����
			int cols = (int) (Math.ceil((init.fullextent.xmax - init.tileInfo.origin.x) / tileWidth));
			int rows = (int) (Math.ceil((init.tileInfo.origin.y - init.fullextent.ymin) / tileHeigh));

			if (imageCount == 0) {
				double scale0 = lods.get(0).scale;
				for (int i = 0; i < lods.size(); i++) {
					double scalei = lods.get(i).scale;
					imageCount += (scale0 / scalei) * (scale0 / scalei) * cols * rows;
				}
			}
			// Ҫ��ʣ�µĽ��зֿ�洢 128 x 128Ϊһ��bundle��
			int bundleCols = cols / 128 + 1;
			int bundleRows = rows / 128 + 1;
			int ThreadNum = 0;
			wirteLog("������" + scale + "  " + "��" + il + "��" + "����Rows:" + rows + ",����Cols:" + cols + ",����bundleRows:"
					+ bundleRows + ",����bundleCols:" + bundleCols + "====" + getNowString() + "====");

			// ThreadNum=bundleCols*bundleRows;

			// int threadIndex = 0;
			////////////////// �����޸ĵı����������ܹ������Ⱥ��PC����,�����зֳ�����������6
			int Pc_Number = 6;
			int allnumber = bundleRows * bundleCols;
			int Cut_number = allnumber / Pc_Number;// ����ÿһ��bundle��������
			int First = allnumber - Cut_number * (Pc_Number - 1);// ��������+һ��bundle����Ϊ����������

			ThreadExportOption threadOpt = new ThreadExportOption();
			threadOpt.bundleRows = bundleRows;
			threadOpt.bundleCols = bundleCols;
			threadOpt.cols = cols;
			threadOpt.rows = rows;
			threadOpt.level = level;
			// ���ϲ���Ϊ�����㼶�ı�ʾ
			threadOpt.levelDir = levelDir;
			threadOpt.tileHeigh = tileHeigh;
			threadOpt.tileWidth = tileWidth;
			// threadOpt.index = threadIndex;
			////////
			threadOpt.Pc_Number = Pc_Number;
			threadOpt.Index_bds = 2;
			threadOpt.Number_bds = Cut_number;
			threadOpt.First_bd = First;
			threadOpt.Mapinfo=init;//Я��init����
			// �̲߳���
			System.out.println(threadOpt.bundleRows + "+" + threadOpt.bundleCols);
			
			System.out.println("ok");
			socketmapserver runofit = new socketmapserver();
			runofit.setthread(threadOpt);
			executor.execute(runofit);
			// listRunExportOption.add(threadOpt);
			
			// threadOpt.edRows = eEd;
			// threadOpt.Sum=Sum[il];
			// threadOpt.edCols=j+1;
			// ���������һ���߳�һ��bundle
			client Server_Req=new client();
			Server_Req.RunningThread=First;//���õ�ǰ���е����߳���
			for (int i = 0; i < First; i++) {// ������Լ�ִ����ͼ����
				ThreadExportOption threadOpt2 = (ThreadExportOption)threadOpt.clone();
				threadOpt2.Index_bds = 1;	
				threadOpt2.cut_index = i;
				 GetImageData runnable = new GetImageData();
				 runnable.setParameter(threadOpt2,Server_Req);
				 executor.execute(runnable);
			}
			Listening listenOver=new Listening();//��Ӽ����߳̽�������
			listenOver.setTemp(Server_Req);
			executor.execute(listenOver);
			threadExecCnt++;
			// threadIndex++;

			/*
			 * try { countdownLatch.await(); } catch (InterruptedException e) {
			 * // TODO Auto-generated catch block e.printStackTrace(); }
			 */
		}
		// executor.shutdown();
		/*
		 * while (true) { //���ÿ�������Ϣ��IOʱ�䳤�� if (executor.isTerminated()) {
		 * for(int i=0;i<Sum.length;i++){
		 * mp.wirteLog("��"+i+"��IOʱ���ܹ���"+mp.GetTime(Sum[i])); } break; }
		 * 
		 * }
		 */
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
	/*
	 * public String GetTime(long mss){ long hours = (mss % (1000 * 60 * 60 *
	 * 24)) / (1000 * 60 * 60); long minutes = (mss % (1000 * 60 * 60)) / (1000
	 * * 60); long seconds = (mss % (1000 * 60)) / 1000; return
	 * hours+"ʱ"+minutes+"��"+seconds+"��"; }
	 */
}
