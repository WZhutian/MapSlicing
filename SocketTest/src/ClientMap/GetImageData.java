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
	long iotimeall;// ͳ��bundle��IOʱ��
	long requesttimeall;// ͳ�������ʱ��
	long indextimeall;// ͳ������bundlx��IOʱ��
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
		// TODO����rowscols
		/*if (threadOpt.Index_bds == 1) {
			int Cuted = threadOpt.cuted;// �Ѿ����кõ�bundle����
			int Rs = Cuted / threadOpt.bundleRows;// �����Ѿ����˶�����
			int Cs = Cuted - Rs * threadOpt.bundleRows;// �����е��˵ڼ���
			int Rs = Cuted / threadOpt.bundleCols;// �����Ѿ����˶�����
            int Cs = Cuted % threadOpt.bundleCols-1;// �����е��˵ڼ���
			this.stRows = Rs;// this.edRows = threadOpt.edRows;
			this.stCols = Cs; // this.edCols=threadOpt.edCols;
		} else {}*/
			int Cuted = threadOpt.cut_index;// �Ѿ����кõ�bundle����
			/*int Rs = Cuted / threadOpt.bundleRows;// �����Ѿ����˶�����
			int Cs = Cuted - Rs * threadOpt.bundleRows;// �����е��˵ڼ���
			
*/			int Rs = Cuted / threadOpt.bundleCols;// �����Ѿ����˶�����
            int Cs = Cuted % threadOpt.bundleCols;// �����е��˵ڼ��� 
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
	// }// �����еķ��񶼲����ã���ô��ִ��ʱ���ø�����߳�
	// }
	//
	// }

	public byte[] GetData(int level, int tileRow, int tileCol, int rows, int cols, double tileWidth, double tileHeigh) {
		long Stime;// ��ʱ�����bundle��IOʱ��
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
		// b.����С��Ƭ��Χ,����һ�Ŵ�ͼ
		Envelope tileEnv = new Envelope();
		tileEnv.xmin = in.tileInfo.origin.x + tileCol * tileWidth;
		tileEnv.ymin = in.tileInfo.origin.y - (tileRow + heightcount) * tileHeigh;
		tileEnv.xmax = in.tileInfo.origin.x + (tileCol + widthcount) * tileWidth;
		tileEnv.ymax = in.tileInfo.origin.y - tileRow * tileHeigh;
		// System.out.println(tileEnv.xmin+","+tileEnv.ymin+","+tileEnv.xmax+","+tileEnv.ymax);

		// c.��ȡͼƬ����
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
			// ��ַ����֤
			// System.out.println(fullurls);
			ByteArrayOutputStream out = new ByteArrayOutputStream();
			URL url = new URL(fullurls);
			HttpURLConnection con = (HttpURLConnection) url.openConnection();
			con.setDoOutput(true);
			con.setRequestMethod("GET");
			long reqstarttime = System.currentTimeMillis(); // ����ʼʱ��
			InputStream in = con.getInputStream();
			long reqendtime = System.currentTimeMillis();// �������ʱ��
			requesttimeall += reqendtime - reqstarttime;// ��������ʱ��
			// server.isuse = true;
			final int length = 5000;
			byte[] bytes = new byte[length];
			int bytesRead = 0;
			while ((bytesRead = in.read(bytes, 0, length)) > 0) {

				out.write(bytes, 0, bytesRead);
			}
			tempData = out.toByteArray();

			// ��Сͼ
			String format = this.in.tileInfo.format.toLowerCase().indexOf("png") > -1 ? "png" : this.in.tileInfo.format;
			Iterator<ImageReader> readers = ImageIO.getImageReadersByFormatName(format);
			ImageReader reader = readers.next();
			ByteArrayInputStream instr = new ByteArrayInputStream(tempData); // ��b��Ϊ��������
			// BufferedImage image = ImageIO.read(InputStream in);
			ImageInputStream iis = ImageIO.createImageInputStream(instr);
			reader.setInput(iis);
			for (int i = 0; i < heightcount; i++) {
				int t_row = tileRow + i;
				for (int j = 0; j < widthcount; j++) {
					int t_col = tileCol + j;
					ImageReadParam param = reader.getDefaultReadParam();
					Rectangle rect = new Rectangle(j * width, i * height, width, height);

					// �ṩһ�� BufferedImage���������������������ݵ�Ŀ�ꡣ
					param.setSourceRegion(rect);
					/*
					 * ʹ�����ṩ�� ImageReadParam ��ȡͨ������ imageIndex ָ���Ķ��󣬲��� ����Ϊһ��������
					 * BufferedImage ���ء�
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
					 * //��������� FileOutputStream outStream = new
					 * FileOutputStream(imageFile); //д������
					 * outStream.write(tempImageData); //�ر������
					 * outStream.close();
					 */
					this.serverImageCache.put(level + "_" + t_row + "_" + t_col, tempImageData);

				}
			}
		} catch (IOException e) {
			e.printStackTrace();
			// ��ͼʧ��
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
		this.SBundletime = System.currentTimeMillis(); // ������Ƭ�Ŀ�ʼʱ��
		int br = this.stRows;// �߳̽�����Ƭ��Ӧ��bundle���к�
		int bl = this.stCols;
		long start = System.currentTimeMillis();

		// long Stime;
		// long Etime;
		// ÿһ������һ��bundle�ļ�
		// 1.�Զ����ɶ�Ӧ���ļ���·�����ļ���
		/*this.ExFunc.wirteLog("����==�̺߳�=��ʼ" + this.index + "==level:" + level + "==bundlerow:" + br + "==bundlecol:" + bl
				+ "==" + ExFunc.getNowString() + "====");// ���������ݸ�thread��
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

			// ��ʼ�� bundlxFileStream �ļ��� ��ʼ��16���ֽ���Ч ����ʱ���ɶ�ȡ��
			byte[] wipeHeaderData = { 3, 0, 0, 0, 16, 0, 0, 0, 0, 64, 0, 0, 5, 0, 0, 0 };
			// Stime=System.currentTimeMillis();
			bundlxFileStream.write(wipeHeaderData, 0, wipeHeaderData.length);
			// Etime=System.currentTimeMillis();
			// sum=sum+(Etime-Stime);

			// 2.���������ļ���¼ֵ �������ʼ��Ϊ0 �����¼���� ������Ƭ�ĳ��� ��������������ֵ
			// ����ʱ ���������� �洢ʱ����������
			int[][] indexData = new int[128][128];// ��ʼ���������ļ�
			indexData[0][0] = 0;
			int offsetLabel = 0;// ����ƫ����
			// 3.���㲢дbundle�ļ� ��ʽ ����+ͼƬ

			for (int i = 0; i < 128; i++) {
				for (int j = 0; j < 128; j++) {
					indexData[i][j] = offsetLabel;// ��¼����ƫ���� ��ʼ��λ��
					// ��ȡ����Ƭ��ͼƬ��Ϣ
					// a.������Ƭ���ڵ�����
					int tileRow = 128 * br + i;
					int tileCol = 128 * bl + j;

					if (tileRow < rows && tileCol < cols) // ������Χ
						// ֱ�Ӱѳ��ȸ�Ϊ0
						// ƫ������4
					{
						byte[] imagedata = GetData(level, tileRow, tileCol, rows, cols, tileWidth, tileHeigh);
						// d.дbundle�ļ� ���ݳ���+ͼƬ���� ����λ����λд���� ����Ϊ4��

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
						
						offsetLabel += 4 + imagedata.length;// �ƶ�ƫ����
						// currentNum++;
						// ���� = Convert.ToInt32(currentNum * 100 /
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
			// �ر�bundle�ļ�
			bundleFileStream.flush();
			bundleFileStream.close();

			// д�������ļ�
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
		long end = System.currentTimeMillis(); // ��¼����ʱ��
		long alltime = end - start;
		// countdownLatch.countDown();
		// Ebundletime=System.currentTimeMillis();
		/*this.ExFunc.wirteLog(
				"����====�߳� " + this.index + "level" + level + "==finish==bundlerow:" + stRows + "==bundlecol:" + stCols
				+ "==����ʱ��==" + ExFunc.getNowString() + "===ʱ�����:" + alltime + "(�ܹ���ʱ��)" + iotimeall + "(ioʱ��)"
				+ requesttimeall + "(����ʱ��)" + indextimeall + "(����bundlxʱ��)" + picturetimeall + "(ͼƬIOʱ��)");*/
		ExFunc.Alltime+=alltime;
		ExFunc.iotime+=picturetimeall;
		ExFunc.seqtime+=requesttimeall;
		// this.ExFunc.Sum[level]+=this.sum;
		// System.out.println(this.ExFunc.Sum[level]);
		// this.ExFunc.wirteLog(this.ExFunc.GetTime(this.ExFunc.Sum[level]));

	}

}
