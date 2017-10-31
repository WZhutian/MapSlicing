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
	int Totalnum;// ������Χ�ڵ�ʸ��������
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

	public int GetTotalNum(Envelope fullextent) // ��ȡĳ��Χ�ڵ�ʸ����������
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
				// ��json�ַ���ת��Ϊjson����
				JSONObject jsonObj = new JSONObject(str);
				// �õ�ָ��json key�����value����
				/* JSONObject NumObj = jsonObj.getJSONObject("count"); */
				// ��ȡ֮�������������
				num += jsonObj.getInt("count");
				//System.out.print(num);
			}

		} catch (Exception e) {
			e.printStackTrace();
		}
		return num;
	}

	public void GetNumBund() {
		int Cut_number = 0;// ÿ̨�ͻ������䵽��ʸ��������
		Cut_number = Totalnum / Pc_Num;
		int count_Num = 0;
		int index_Num = 0;
		int i = 0;
		int j = 0;
		if (bundleRows < Pc_Num) {
		
			for (i = 0; i < bundleRows; i++) {
				for (j = 0; j <bundleCols ; j++) // �����ȫ�����е�ѭ������̫�࣬��ʱ�۰�
				{
					Envelope fullextent = new Envelope();
					fullextent.xmin = threadOpt.Mapinfo.tileInfo.origin.x + j * BundWidth ;
					fullextent.ymin = threadOpt.Mapinfo.tileInfo.origin.y - i * BundHeight;
					fullextent.xmax = threadOpt.Mapinfo.tileInfo.origin.x + ( j + 1) * BundWidth;
					fullextent.ymax = threadOpt.Mapinfo.tileInfo.origin.y - (i + 1) * BundHeight;
					count_Num += GetTotalNum(fullextent);
					if (count_Num > Cut_number) {
						index_Num = i * bundleCols + (j + 1) ;// ������Ѿ�����bundle�ĸ�������1
																	// ��ʼ
						indexlist.add(index_Num);
						count_Num = 0;
					}
				}

			}
			index_Num = bundleRows*bundleCols;// ����߽�ֵ
			indexlist.add(index_Num);
		} else
		{   int n=(int)Math.ceil(bundleCols/2);
			for (i = 0; i < bundleRows; i++) {
				for (j = 0; j <n ; j++) // �����ȫ�����е�ѭ������̫�࣬��ʱ�۰�
				{
					Envelope fullextent = new Envelope();
					fullextent.xmin = threadOpt.Mapinfo.tileInfo.origin.x + j * BundWidth*2 ;
					fullextent.ymin = threadOpt.Mapinfo.tileInfo.origin.y - i * BundHeight;
					fullextent.xmax = threadOpt.Mapinfo.tileInfo.origin.x + (2*j + 2) * BundWidth;
					fullextent.ymax = threadOpt.Mapinfo.tileInfo.origin.y - (i + 1) * BundHeight;
					count_Num += GetTotalNum(fullextent);
					if (count_Num > Cut_number) {
						index_Num = i * bundleCols + (j + 1)*2 ;// ������Ѿ�����bundle�ĸ�������1
																	// ��ʼ
						indexlist.add(index_Num);
						count_Num = 0;
					}
				}

			}
			index_Num = bundleRows*bundleCols;// ����߽�ֵ
			indexlist.add(index_Num);
			
			
			
			
			
			/*for (i = 0; i < bundleRows; i++) {
				// ����ÿ�еķ�Χȥɨ��
				Envelope fullextent = new Envelope();
				fullextent.xmin = threadOpt.Mapinfo.tileInfo.origin.x;
				fullextent.ymin = threadOpt.Mapinfo.tileInfo.origin.y - i * BundHeight;
				fullextent.xmax = threadOpt.Mapinfo.tileInfo.origin.x+ bundleCols*BundWidth;
				fullextent.ymax = threadOpt.Mapinfo.tileInfo.origin.y - (i + 1) * BundHeight;
				count_Num += GetTotalNum(fullextent);
				if (count_Num > Cut_number) {
					index_Num = (i + 1) * bundleCols; //��1��ʼ����bundle����
					indexlist.add(index_Num);
					count_Num = 0;
				}

			}
			index_Num = i* bundleCols;
			indexlist.add(index_Num);*/
		}

	}
	}