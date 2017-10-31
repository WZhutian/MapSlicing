package ClientMap;

import java.awt.Rectangle;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.concurrent.CountDownLatch;

import javax.imageio.ImageIO;
import javax.imageio.ImageReadParam;
import javax.imageio.ImageReader;
import javax.imageio.stream.ImageInputStream;

import com.ecity.server.response.geometry.Envelope;
import com.esri.schemas.arcgis._10.HttpException;

import ServerMap.ThreadExportOption;
import ServerMap.InitMapInfo;
public class GetImageData implements Runnable {
	long iotimeall;// 统计bundle的IO时间
	long requesttimeall;// 统计请求的时间
	long indextimeall;// 统计索引bundlx的IO时间
	long picturetimeall;
	long jishu;
	private int rows;
	private int cols;
	private int bundleCols;
	private int bundleRows;
	private double tileWidth;
	private double tileHeigh;
	private int level;
	private String levelDir;
	MapTile ExFunc = null;
	private int width = 256;
	private int height = 256;
	private int maxImageSize = 1024;
	InitMapInfo in = null;
	private Hashtable<String, byte[]> serverImageCache = new Hashtable<String, byte[]>();
	// private CountDownLatch countdownLatch;
	private int index;
	String key;
	int stRows;
	int edRows;
	int stCols;
	int edCols;
	long SBundletime;
	long Ebundletime;
	long interval = 0;
	/*
	 * int MM=0; int DD=0; int HH=0; // long sum;
	 */
	public void setParameter(ThreadExportOption threadOpt) {
		// TODO Auto-generated method stub
		this.rows = threadOpt.rows;
		this.cols = threadOpt.cols;
		this.bundleCols = threadOpt.bundleCols;
		this.bundleRows = threadOpt.bundleRows;
		this.tileWidth = threadOpt.tileWidth;
		this.tileHeigh = threadOpt.tileHeigh;
		this.level = threadOpt.level;
		this.levelDir = threadOpt.levelDir;
		this.ExFunc = MapTile.getInstance();
		// this.countdownLatch = countdownLatch;
		this.index = threadOpt.index;
		this.in=threadOpt.Mapinfo;
		// TODO计算rowscols
		/*if (threadOpt.Index_bds == 1) {
			int Cuted = threadOpt.cuted;// 已经被切好的bundle数量
			int Rs = Cuted / threadOpt.bundleRows;// 计算已经切了多少行
			int Cs = Cuted - Rs * threadOpt.bundleRows;// 计算切到了第几列
			int Rs = Cuted / threadOpt.bundleCols;// 计算已经切了多少行
            int Cs = Cuted % threadOpt.bundleCols-1;// 计算切到了第几列
			this.stRows = Rs;// this.edRows = threadOpt.edRows;
			this.stCols = Cs; // this.edCols=threadOpt.edCols;
		} else {}*/
			int Cuted = threadOpt.cut_index;// 已经被切好的bundle数量
			/*int Rs = Cuted / threadOpt.bundleRows;// 计算已经切了多少行
			int Cs = Cuted - Rs * threadOpt.bundleRows;// 计算切到了第几列
			
*/			int Rs = Cuted / threadOpt.bundleCols;// 计算已经切了多少行
            int Cs = Cuted % threadOpt.bundleCols;// 计算切到了第几列 
            this.stRows = Rs;// this.edRows = threadOpt.edRows;
			this.stCols = Cs; // this.edCols=threadOpt.edCols;
			ExFunc.wirteLog(stRows+"##"+stCols+"==");

		System.out.println(stRows+"##"+stCols);
	}

	//
	//
	// synchronized Server GetServer() {
	// while (true) {
	// for (int i = 0; i < ExFunc.listServer.size(); i++) {
	// Server server = ExFunc.listServer.get(i);
	// if (server.isuse == true) {
	// server.isuse = false;
	// return server;
	// }
	// }// 若所有的服务都不可用，那么把执行时间让给别的线程
	// }
	//
	// }

	public byte[] GetData(int level, int tileRow, int tileCol, int rows, int cols, double tileWidth, double tileHeigh) {
		long Stime;// 该时间计算bundle的IO时间
		long Etime;

		byte[] tempData = null;
		String key = level + "_" + tileRow + "_" + tileCol;
		if (this.serverImageCache.containsKey(key)) {
			tempData = this.serverImageCache.get(key).clone();
			this.serverImageCache.remove(key);
			return tempData;
		}
		// Server server = GetServer();
		int widthcount = this.maxImageSize / this.width;
		int heightcount = this.maxImageSize / this.height;
		if (rows < heightcount || cols < widthcount) {
			heightcount = rows;
			widthcount = cols;
		}
		// b.设置小切片范围,生成一张大图
		Envelope tileEnv = new Envelope();
		tileEnv.xmin = in.tileInfo.origin.x + tileCol * tileWidth;
		tileEnv.ymin = in.tileInfo.origin.y - (tileRow + heightcount) * tileHeigh;
		tileEnv.xmax = in.tileInfo.origin.x + (tileCol + widthcount) * tileWidth;
		tileEnv.ymax = in.tileInfo.origin.y - tileRow * tileHeigh;
		// System.out.println(tileEnv.xmin+","+tileEnv.ymin+","+tileEnv.xmax+","+tileEnv.ymax);

		// c.获取图片数据
		try {
			// String fullurls = server.url + "/export?token=&bbox="
			// + tileEnv.xmin + "%2C" + tileEnv.ymin + "%2C"
			// + tileEnv.xmax + "%2C" + tileEnv.ymax + "&bboxSR=&size="
			// + width * widthcount + "," + height * heightcount + "%2C&"
			// + "&format=" + in.tileInfo.format
			// + "&transparent=false&f=image&dpi=" + in.tileInfo.dpi;

			String fullurls = "http://localhost:6080/arcgis/rest/services/MapCutting/MapServer/" + "export?token=&bbox="
					+ tileEnv.xmin + "%2C" + tileEnv.ymin + "%2C" + tileEnv.xmax + "%2C" + tileEnv.ymax
					+ "&bboxSR=&layers=&layerDefs=&size=" + width * widthcount + "%2C" + height * heightcount
					+ "%2C&imageSR=" + "&format=" + in.tileInfo.format + "&transparent=false&f=image&dpi="
					+ in.tileInfo.dpi;
			// 地址已验证
			// System.out.println(fullurls);
			ByteArrayOutputStream out = new ByteArrayOutputStream();
			URL url = new URL(fullurls);
			HttpURLConnection con = (HttpURLConnection) url.openConnection();
			con.setDoOutput(true);
			con.setRequestMethod("GET");
			long reqstarttime = System.currentTimeMillis(); // 请求开始时间
			InputStream in = con.getInputStream();
			long reqendtime = System.currentTimeMillis();// 请求结束时间
			requesttimeall += reqendtime - reqstarttime;// 计算请求时间
			// server.isuse = true;
			final int length = 5000;
			byte[] bytes = new byte[length];
			int bytesRead = 0;
			while ((bytesRead = in.read(bytes, 0, length)) > 0) {

				out.write(bytes, 0, bytesRead);
			}
			tempData = out.toByteArray();

			// 切小图
			String format = this.in.tileInfo.format.toLowerCase().indexOf("png") > -1 ? "png" : this.in.tileInfo.format;
			Iterator<ImageReader> readers = ImageIO.getImageReadersByFormatName(format);
			ImageReader reader = readers.next();
			ByteArrayInputStream instr = new ByteArrayInputStream(tempData); // 将b作为输入流；
			// BufferedImage image = ImageIO.read(InputStream in);
			ImageInputStream iis = ImageIO.createImageInputStream(instr);
			reader.setInput(iis);
			for (int i = 0; i < heightcount; i++) {
				int t_row = tileRow + i;
				for (int j = 0; j < widthcount; j++) {
					int t_col = tileCol + j;
					ImageReadParam param = reader.getDefaultReadParam();
					Rectangle rect = new Rectangle(j * width, i * height, width, height);

					// 提供一个 BufferedImage，将其用作解码像素数据的目标。
					param.setSourceRegion(rect);
					/*
					 * 使用所提供的 ImageReadParam 读取通过索引 imageIndex 指定的对象，并将 它作为一个完整的
					 * BufferedImage 返回。
					 */
					BufferedImage bi = reader.read(0, param);
					// byte[] tempData = ((DataBufferByte)
					// bi.getData().getDataBuffer()).getData();
					ByteArrayOutputStream outstr = new ByteArrayOutputStream();

					try {
						// Stime=System.currentTimeMillis();
						long tx = System.currentTimeMillis();
						ImageIO.write(bi, format, outstr);
						long tc = System.currentTimeMillis();
						picturetimeall += tc - tx;
						// Etime=System.currentTimeMillis();
						// this.sum=this.sum+(Etime-Stime);

					} catch (IOException e) {
						System.out.println("ImageIO.write fail");
					}
					byte[] tempImageData = outstr.toByteArray();
					/*
					 * File imageFile = new
					 * File("E:\\test6\\pic"+heightcount+widthcount+".png");
					 * //创建输出流 FileOutputStream outStream = new
					 * FileOutputStream(imageFile); //写入数据
					 * outStream.write(tempImageData); //关闭输出流
					 * outStream.close();
					 */
					this.serverImageCache.put(level + "_" + t_row + "_" + t_col, tempImageData);

				}
			}
		} catch (IOException e) {
			e.printStackTrace();
			// 切图失败
			throw new HttpException("clip image fail" + e.getMessage());
		}

		if (this.serverImageCache.containsKey(key)) {
			tempData = this.serverImageCache.get(key).clone();
			this.serverImageCache.remove(key);
			return tempData;
		} else {
			System.out.println("getMap fail");
			throw new HttpException("getMap fail");
		}
	}

	@Override
	public void run() {
		this.SBundletime = System.currentTimeMillis(); // 设置切片的开始时间
		int br = this.stRows;// 线程进行切片对应的bundle行列号
		int bl = this.stCols;
		long start = System.currentTimeMillis();

		// long Stime;
		// long Etime;
		// 每一个都是一个bundle文件
		// 1.自动生成对应的文件夹路径和文件名
		/*this.ExFunc.wirteLog("并行==线程号=开始" + this.index + "==level:" + level + "==bundlerow:" + br + "==bundlecol:" + bl
				+ "==" + ExFunc.getNowString() + "====");// 将参数传递给thread类
*/
		String baseName = ExFunc.getCompactBundlePathName(br, bl, level, levelDir);
		String bundleName = baseName + ".bundle";
		String bundlxName = baseName + ".bundlx";

		FileOutputStream bundleFileStream = null;
		FileOutputStream bundlxFileStream = null;
		File bundleFile = null;
		File bundlxFile = null;
		try {
			bundleFile = new File(bundleName);
			if (!bundleFile.exists()) {
				bundleFile.createNewFile();
			}
			bundlxFile = new File(bundlxName);
			if (!bundlxFile.exists()) {
				bundlxFile.createNewFile();
			}
			bundleFileStream = new FileOutputStream(bundleFile);
			bundlxFileStream = new FileOutputStream(bundlxFile);

			// 初始化 bundlxFileStream 文件流 起始的16个字节无效 （暂时不可读取）
			byte[] wipeHeaderData = { 3, 0, 0, 0, 16, 0, 0, 0, 0, 64, 0, 0, 5, 0, 0, 0 };
			// Stime=System.currentTimeMillis();
			bundlxFileStream.write(wipeHeaderData, 0, wipeHeaderData.length);
			// Etime=System.currentTimeMillis();
			// sum=sum+(Etime-Stime);

			// 2.声明索引文件记录值 并将其初始化为0 这里记录的是 各个切片的长度 不是真正的索引值
			// 计算时 按照行优先 存储时按照列优先
			int[][] indexData = new int[128][128];// 初始化的索引文件
			indexData[0][0] = 0;
			int offsetLabel = 0;// 跟踪偏移量
			// 3.计算并写bundle文件 格式 长度+图片

			for (int i = 0; i < 128; i++) {
				for (int j = 0; j < 128; j++) {
					indexData[i][j] = offsetLabel;// 记录下来偏移量 开始的位置
					// 获取该切片的图片信息
					// a.计算切片所在的行列
					int tileRow = 128 * br + i;
					int tileCol = 128 * bl + j;

					if (tileRow < rows && tileCol < cols) // 超出范围
						// 直接把长度赋为0
						// 偏移量加4
					{
						byte[] imagedata = GetData(level, tileRow, tileCol, rows, cols, tileWidth, tileHeigh);
						// d.写bundle文件 数据长度+图片数据 （低位到高位写长度 长度为4）

						byte[] lengthBytes = ExFunc.int2byte(imagedata.length);
						byte[] emptydata = new byte[156];

						if (imagedata.length == 921) {
							imagedata[35] = 0;
							imagedata[36] = 3;
							int tcc = 0;
							for (int ct = 0; ct < 156; ct++) {
								emptydata[ct] = imagedata[ct + tcc];
								if (ct == 43)
									tcc = 765;
							}
							long t1 = System.currentTimeMillis();
							bundleFileStream.write(emptydata, 0, emptydata.length);
							long t2 = System.currentTimeMillis();
							iotimeall += t2 - t1;
						} else {
							long t1 = System.currentTimeMillis();
							bundleFileStream.write(imagedata, 0, imagedata.length);
							long t2 = System.currentTimeMillis();
							iotimeall += t2 - t1;

						}
						
						offsetLabel += 4 + imagedata.length;// 移动偏移量
						// currentNum++;
						// 进度 = Convert.ToInt32(currentNum * 100 /
						// imgSum);
					} else {
						byte[] lengthBytes = ExFunc.int2byte(0);
						// Stime=System.currentTimeMillis();
						long t1 = System.currentTimeMillis();
						bundleFileStream.write(lengthBytes, 0, 4);
						long t2 = System.currentTimeMillis();
						iotimeall += t2 - t1;
						// Etime=System.currentTimeMillis();
						// sum=sum+(Etime-Stime);
						offsetLabel += 4;
					}
				}
			}
			// 关闭bundle文件
			bundleFileStream.flush();
			bundleFileStream.close();

			// 写入索引文件
			long bundlxtimeallstart = System.currentTimeMillis();
			for (int i = 0; i < 128; i++) {
				for (int j = 0; j < 128; j++) {
					byte[] lenBytes = ExFunc.int2byte(indexData[j][i]);
					// Stime=System.currentTimeMillis();
					if (lenBytes.length < 5) {
						bundlxFileStream.write(lenBytes, 0, lenBytes.length);
						byte[] temp = { 0 };
						bundlxFileStream.write(temp);
					} else
						bundlxFileStream.write(lenBytes, 0, 5);
					// Etime=System.currentTimeMillis();
					// sum=sum+(Etime-Stime);
				}
			}
			byte[] wipeEnderData = { 0, 0, 0, 0, 16, 0, 0, 0, 16, 0, 0, 0, 0, 0, 0, 0 };
			// Stime=System.currentTimeMillis();
			bundlxFileStream.write(wipeEnderData, 0, wipeEnderData.length);
			long bundlxtimeallend = System.currentTimeMillis();
			indextimeall = bundlxtimeallend - bundlxtimeallstart;
			// Etime=System.currentTimeMillis();
			// sum=sum+(Etime-Stime);
			bundlxFileStream.flush();
			bundlxFileStream.close();
		} catch (Exception e) {
			e.printStackTrace();
			try {
				if (bundleFileStream != null) {
					bundleFileStream.close();
					bundleFile.delete();
				}
				if (bundlxFileStream != null) {
					bundlxFileStream.close();
					bundleFile.delete();
				}
			} catch (IOException e1) {
				e1.printStackTrace();
			}
			throw new HttpException(e.getMessage());
		}
		long end = System.currentTimeMillis(); // 记录结束时间
		long alltime = end - start;
		// countdownLatch.countDown();
		// Ebundletime=System.currentTimeMillis();
		/*this.ExFunc.wirteLog(
				"并行====线程 " + this.index + "level" + level + "==finish==bundlerow:" + stRows + "==bundlecol:" + stCols
				+ "==结束时间==" + ExFunc.getNowString() + "===时间分析:" + alltime + "(总共的时间)" + iotimeall + "(io时间)"
				+ requesttimeall + "(请求时间)" + indextimeall + "(索引bundlx时间)" + picturetimeall + "(图片IO时间)");*/
		ExFunc.Alltime+=alltime;
		ExFunc.iotime+=picturetimeall;
		ExFunc.seqtime+=requesttimeall;
		// this.ExFunc.Sum[level]+=this.sum;
		// System.out.println(this.ExFunc.Sum[level]);
		// this.ExFunc.wirteLog(this.ExFunc.GetTime(this.ExFunc.Sum[level]));

	}

}
