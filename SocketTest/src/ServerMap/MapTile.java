package ServerMap;

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
import com.ecity.server.response.geometry.Envelope;
import com.esri.schemas.arcgis._10.HttpException;

import ServerMap.GetImageData;

public class MapTile {
	private File logFile = null;
	public static MapTile mp = null;
	private int imageCount = 0;
	public static int StatPoint;
	public static int First;
	static InitMapInfo init = null;
	static GetImageData getdata = new GetImageData();
	static ThreadExportOption threadOpt=null;
	public static double Alltime=0;
    public static double iotime=0;
    public static double seqtime=0;
/*	static BundleCut bundlecut=null;*/
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
	public static ThreadExportOption getThreadOptInstance() {
		if (null == threadOpt) {
			synchronized (MapTile.class) {
				if (null == threadOpt) {
					threadOpt = new ThreadExportOption();
				}

			}
		}
		return threadOpt;
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
			int Pc_Number = 2;
			// int threadIndex = 0;
			////////////////// 可以修改的变量：根据总共计算机群的PC数量,设置切分出来的数量：6
			threadOpt=new ThreadExportOption();
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
			threadOpt.Index_bds = 0;
		/*	threadOpt.Number_bds = Cut_number;
			threadOpt.First_bd = First;*/
			threadOpt.Mapinfo=init;//携带init对象
			// 线程测试
			System.out.println(threadOpt.bundleRows + "+" + threadOpt.bundleCols);
			System.out.println("ok");
			BundleCut bundlecut=new BundleCut();
			System.out.print("预处理结束");
			socketmapserver runofit = new socketmapserver();
			runofit.setthread(threadOpt);
			threadOpt.StatPoint=0;
			executor.execute(runofit);
			int allnumber = bundleRows * bundleCols;
			/*int Cut_number = allnumber / Pc_Number;// 计算每一份bundle包的数量
*/			
			int indexclient=bundlecut.indexlist.size()-2;
			First = allnumber ;// 本机处理为边界数据，即数组的最后一位

			StatPoint=(int) bundlecut.indexlist.get(indexclient);
			// listRunExportOption.add(threadOpt);
			
			// threadOpt.edRows = eEd;
			// threadOpt.Sum=Sum[il];
			// threadOpt.edCols=j+1;
			// 分配策略以一个线程一个bundle
//			client Server_Req=new client();
//			Server_Req.RunningThread=First;//设置当前运行的总线程数
			
			for (int i = StatPoint; i < First; i++) {// 服务端自己执行切图任务
				ThreadExportOption threadOpt2 = (ThreadExportOption)threadOpt.clone();//使用克隆的方法，保证引用不被改变
				threadOpt2.cut_index = i;
				 GetImageData runnable = new GetImageData();
				 runnable.setParameter(threadOpt2);
				 executor.execute(runnable);
			}
			StatPoint=0;//重置Statpoint
			Listener probe_Server=new Listener();
			probe_Server.Get_ThreadPool(executor);
			probe_Server.Get_threadOpt(threadOpt);//引用传递当前任务情况
			executor.execute(probe_Server);//线程池末尾添加补充探针，用来保持服务端继续切图
//			Listening listenOver=new Listening();//添加监听线程结束函数
//			listenOver.setTemp(Server_Req);
//			executor.execute(listenOver);
		}
	}

	private int GetTotalNum(Envelope fullextent) {
		// TODO Auto-generated method stub
		return 0;
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

}
