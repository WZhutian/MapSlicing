package ServerMap;

import java.util.ArrayList;

import org.apache.xmlbeans.impl.xb.xsdschema.Public;

public class ThreadExportOption implements java.io.Serializable,Cloneable  {

		public int bundleRows;
		public int bundleCols ;
		public int cols;
		public int rows;
		public int level;
		public String levelDir;
//		public int stRows;
//		public int edRows;
		public double tileHeigh;
		public double tileWidth;
		public String storage;
		public String scale;
		public int width;
		public int height;
		public int dpi;
		public String format; 
		public int index = 1;
		public int stCols;
		public int edCols;
		//Я��InitMapInfo����
		public InitMapInfo Mapinfo;
		//����������
		public int Pc_Number;
	/*	public int First_bd;//��һ��bundle��������bundle����
*/		/*public int Number_bds;//ÿһ����Ű�����bundle����
*/		public int StatPoint=1;
		public int Index_bds;//��ǰ���е�bundle�����
		//�ͻ��˲���
		public int cuted;//��Ƭ���е�������
		public int cut_index;
		public int cut_finished=0;//�ж��Ƿ��Ѿ��������0Ϊδ��ɣ�1Ϊ���
		//public long Sum;
		
		 public Object clone() {
			 Object o=null; 
			  try 
			   { 
			   o=(ThreadExportOption)super.clone();//Object �е�clone()ʶ�����Ҫ���Ƶ�����һ������ 
			   } 
			  catch(CloneNotSupportedException e) 
			   { 
			    System.out.println(e.toString()); 
			   } 
			  return o; 
			 
		 }
	} 

