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

	// 构造函数对服务列表进行初始化
	MapTile() {
		String filepath = "resouce/service.xml";// 注意filepath的内容；
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
	 * 对初始化后的瓦片信息进行分割生成对应参数
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
	 * 导出紧凑型瓦片
	 */
	public void ExportCompactTileMap() {

		List<TileLevel> lods = init.tileInfo.lods;// 层次信息
		// long[] Sum=new long[lods.size()];
		List<ThreadExportOption> listRunExportOption = new ArrayList<ThreadExportOption>();
		ThreadPoolExecutor executor = null;
		executor = new ThreadPoolExecutor(4, 4, 3, TimeUnit.HOURS, new LinkedBlockingQueue<Runnable>());
		System.out.println(lods.size() + "层");
		for (int il = 0; il < lods.size(); il++)// 对每一层在进行计算了
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
			// 计算总行数 列数
			// 这里不能直接用mapEnvp的宽和高来计算 否则容易出现多一行或列的情况
			int cols = (int) (Math.ceil((init.fullextent.xmax - init.tileInfo.origin.x) / tileWidth));
			int rows = (int) (Math.ceil((init.tileInfo.origin.y - init.fullextent.ymin) / tileHeigh));

			if (imageCount == 0) {
				double scale0 = lods.get(0).scale;
				for (int i = 0; i < lods.size(); i++) {
					double scalei = lods.get(i).scale;
					imageCount += (scale0 / scalei) * (scale0 / scalei) * cols * rows;
				}
			}
			// 要对剩下的进行分块存储 128 x 128为一个bundle中
			int bundleCols = cols / 128 + 1;
			int bundleRows = rows / 128 + 1;
			int ThreadNum = 0;
			wirteLog("比例尺" + scale + "  " + "第" + il + "层" + "共有Rows:" + rows + ",共有Cols:" + cols + ",共有bundleRows:"
					+ bundleRows + ",共有bundleCols:" + bundleCols + "====" + getNowString() + "====");

			// ThreadNum=bundleCols*bundleRows;

			// int threadIndex = 0;
			////////////////// 可以修改的变量：根据总共计算机群的PC数量,设置切分出来的数量：6
			int Pc_Number = 6;
			int allnumber = bundleRows * bundleCols;
			int Cut_number = allnumber / Pc_Number;// 计算每一份bundle包的数量
			int First = allnumber - Cut_number * (Pc_Number - 1);// 计算余数+一份bundle包作为本机处理量

			ThreadExportOption threadOpt = new ThreadExportOption();
			threadOpt.bundleRows = bundleRows;
			threadOpt.bundleCols = bundleCols;
			threadOpt.cols = cols;
			threadOpt.rows = rows;
			threadOpt.level = level;
			// 以上参数为整个层级的表示
			threadOpt.levelDir = levelDir;
			threadOpt.tileHeigh = tileHeigh;
			threadOpt.tileWidth = tileWidth;
			// threadOpt.index = threadIndex;
			////////
			threadOpt.Pc_Number = Pc_Number;
			threadOpt.Index_bds = 2;
			threadOpt.Number_bds = Cut_number;
			threadOpt.First_bd = First;
			threadOpt.Mapinfo=init;//携带init对象
			// 线程测试
			System.out.println(threadOpt.bundleRows + "+" + threadOpt.bundleCols);
			
			System.out.println("ok");
			socketmapserver runofit = new socketmapserver();
			runofit.setthread(threadOpt);
			executor.execute(runofit);
			// listRunExportOption.add(threadOpt);
			
			// threadOpt.edRows = eEd;
			// threadOpt.Sum=Sum[il];
			// threadOpt.edCols=j+1;
			// 分配策略以一个线程一个bundle
			client Server_Req=new client();
			Server_Req.RunningThread=First;//设置当前运行的总线程数
			for (int i = 0; i < First; i++) {// 服务端自己执行切图任务
				ThreadExportOption threadOpt2 = (ThreadExportOption)threadOpt.clone();
				threadOpt2.Index_bds = 1;	
				threadOpt2.cut_index = i;
				 GetImageData runnable = new GetImageData();
				 runnable.setParameter(threadOpt2,Server_Req);
				 executor.execute(runnable);
			}
			Listening listenOver=new Listening();//添加监听线程结束函数
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
		 * while (true) { //输出每个层次信息的IO时间长短 if (executor.isTerminated()) {
		 * for(int i=0;i<Sum.length;i++){
		 * mp.wirteLog("第"+i+"层IO时间总共："+mp.GetTime(Sum[i])); } break; }
		 * 
		 * }
		 */
	}

	/**
	 * 获取当前时间的字符串描述
	 * 
	 * @return
	 */
	public String getNowString() {
		return new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss:SSS").format(new Date());
	}

	/**
	 * 格式化数字，前补位
	 * 
	 * @param in
	 * @param length
	 * @param ch
	 *            补位字符
	 * @return
	 */
	public String formatLeftString(String in, int length, String ch) {
		while (in.length() < length) {
			in = ch + in;
		}
		return in;
	}

	/**
	 * z 书写日志文件
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
	 * 转换int为byte数组
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
	 * 获取紧凑型切片文件的名字 获取紧凑型切片文件的名字 如果该路径不存在 则创建一个路径
	 * 
	 * @param bundleRow
	 *            行
	 * @param bundleCol
	 *            列
	 * @param level
	 *            级别
	 * @param levelPath
	 *            路径
	 * @return
	 */
	public String getCompactBundlePathName(int bundleRow, int bundleCol, int level, String levelPath) {
		int rGroup = 128 * bundleRow;
		String r = "R" + formatLeftString(Integer.toHexString(rGroup), 4, "0");

		int cGroup = 128 * bundleCol;
		String c = "C" + formatLeftString(Integer.toHexString(cGroup), 4, "0");// 转为16进制

		String bundleBase = String.format("%s%s%s%s", levelPath, File.separator, r, c);
		return bundleBase;
	}
	/**
	 * 将毫秒转换为时分秒
	 */
	/*
	 * public String GetTime(long mss){ long hours = (mss % (1000 * 60 * 60 *
	 * 24)) / (1000 * 60 * 60); long minutes = (mss % (1000 * 60 * 60)) / (1000
	 * * 60); long seconds = (mss % (1000 * 60)) / 1000; return
	 * hours+"时"+minutes+"分"+seconds+"秒"; }
	 */
}
