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
		//携带InitMapInfo对象
		public InitMapInfo Mapinfo;
		//服务器参数
		public int Pc_Number;
	/*	public int First_bd;//第一个bundle包包含的bundle数量
*/		/*public int Number_bds;//每一个编号包含的bundle数量
*/		public int StatPoint=1;
		public int Index_bds;//当前所切的bundle包编号
		//客户端参数
		public int cuted;//切片包中的索引号
		public int cut_index;
		public int cut_finished=0;//判断是否已经完成任务，0为未完成，1为完成
		//public long Sum;
		
		 public Object clone() {
			 Object o=null; 
			  try 
			   { 
			   o=(ThreadExportOption)super.clone();//Object 中的clone()识别出你要复制的是哪一个对象。 
			   } 
			  catch(CloneNotSupportedException e) 
			   { 
			    System.out.println(e.toString()); 
			   } 
			  return o; 
			 
		 }
	} 

