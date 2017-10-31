package ServerMap;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;

import org.apache.commons.lang.text.StrBuilder;
import org.codehaus.jettison.json.JSONObject;

import com.ecity.server.response.geometry.Envelope;
import com.esri.schemas.arcgis._10.ArrayOfInt;

public class BundleCut {
	int bundleRows;
	int bundleCols;
	int Pc_Num;
	MapTile mp = null;
	int tileCol;
	int tileRow;
	double BundHeight;
	double BundWidth;
	
	ThreadExportOption threadOpt = null;
	int Totalnum;// 整个范围内的矢量数据量
	public static ArrayList indexlist;
	BundleCut() {
		threadOpt = mp.getThreadOptInstance();
		BundHeight = 128 * threadOpt.tileHeigh;
		BundWidth = 128 * threadOpt.tileWidth;
		this.Pc_Num = threadOpt.Pc_Number;
	    indexlist=new ArrayList();
	    Totalnum= GetTotalNum(threadOpt.Mapinfo.fullextent);
	    this.bundleCols=threadOpt.bundleCols;
	    this.bundleRows=threadOpt.bundleRows;
	    GetNumBund();
	}

	public int GetTotalNum(Envelope fullextent) // 获取某范围内的矢量数据总量
	{int num = 0;
		try {
			for (int i = 0; i < 3; i++) {
				String urls = "http://localhost:6080/arcgis/rest/services/MapCutting/" + "MapServer/" + i
						+ "/query?&geometry=" + fullextent.xmin + "%2C" + fullextent.ymin + "%2C" + fullextent.xmax
						+ "%2C" + fullextent.ymax
						+ "&geometryType=esriGeometryEnvelope&spatialRel=esriSpatialRelIntersects&returnCountOnly=true&f=json";
				//System.out.println(urls);
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
				// 将json字符串转换为json对象
				JSONObject jsonObj = new JSONObject(str);
				// 得到指定json key对象的value对象
				/* JSONObject NumObj = jsonObj.getJSONObject("count"); */
				// 获取之对象的所有属性
				num += jsonObj.getInt("count");
				//System.out.print(num);
			}

		} catch (Exception e) {
			e.printStackTrace();
		}
		return num;
	}

	public void GetNumBund() {
		int Cut_number = 0;// 每台客户机分配到的矢量数据量
		Cut_number = Totalnum / Pc_Num;
		int count_Num = 0;
		int index_Num = 0;
		int i = 0;
		int j = 0;
		if (bundleRows < Pc_Num) {
		
			for (i = 0; i < bundleRows; i++) {
				for (j = 0; j <bundleCols ; j++) // 如果完全按照列的循环次数太多，此时折半
				{
					Envelope fullextent = new Envelope();
					fullextent.xmin = threadOpt.Mapinfo.tileInfo.origin.x + j * BundWidth ;
					fullextent.ymin = threadOpt.Mapinfo.tileInfo.origin.y - i * BundHeight;
					fullextent.xmax = threadOpt.Mapinfo.tileInfo.origin.x + ( j + 1) * BundWidth;
					fullextent.ymax = threadOpt.Mapinfo.tileInfo.origin.y - (i + 1) * BundHeight;
					count_Num += GetTotalNum(fullextent);
					if (count_Num > Cut_number) {
						index_Num = i * bundleCols + (j + 1) ;// 计算出已经到了bundle的个数，从1
																	// 开始
						indexlist.add(index_Num);
						count_Num = 0;
					}
				}

			}
			index_Num = bundleRows*bundleCols;// 计算边界值
			indexlist.add(index_Num);
		} else
		{   int n=(int)Math.ceil(bundleCols/2);
			for (i = 0; i < bundleRows; i++) {
				for (j = 0; j <n ; j++) // 如果完全按照列的循环次数太多，此时折半
				{
					Envelope fullextent = new Envelope();
					fullextent.xmin = threadOpt.Mapinfo.tileInfo.origin.x + j * BundWidth*2 ;
					fullextent.ymin = threadOpt.Mapinfo.tileInfo.origin.y - i * BundHeight;
					fullextent.xmax = threadOpt.Mapinfo.tileInfo.origin.x + (2*j + 2) * BundWidth;
					fullextent.ymax = threadOpt.Mapinfo.tileInfo.origin.y - (i + 1) * BundHeight;
					count_Num += GetTotalNum(fullextent);
					if (count_Num > Cut_number) {
						index_Num = i * bundleCols + (j + 1)*2 ;// 计算出已经到了bundle的个数，从1
																	// 开始
						indexlist.add(index_Num);
						count_Num = 0;
					}
				}

			}
			index_Num = bundleRows*bundleCols;// 计算边界值
			indexlist.add(index_Num);
			
			
			
			
			
			/*for (i = 0; i < bundleRows; i++) {
				// 按照每行的范围去扫描
				Envelope fullextent = new Envelope();
				fullextent.xmin = threadOpt.Mapinfo.tileInfo.origin.x;
				fullextent.ymin = threadOpt.Mapinfo.tileInfo.origin.y - i * BundHeight;
				fullextent.xmax = threadOpt.Mapinfo.tileInfo.origin.x+ bundleCols*BundWidth;
				fullextent.ymax = threadOpt.Mapinfo.tileInfo.origin.y - (i + 1) * BundHeight;
				count_Num += GetTotalNum(fullextent);
				if (count_Num > Cut_number) {
					index_Num = (i + 1) * bundleCols; //从1开始计数bundle数量
					indexlist.add(index_Num);
					count_Num = 0;
				}

			}
			index_Num = i* bundleCols;
			indexlist.add(index_Num);*/
		}

	}
	}