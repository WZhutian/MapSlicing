package ClientMap;

import java.io.BufferedInputStream;

import java.io.File;
import org.dom4j.Document;
import org.dom4j.DocumentException;
import org.dom4j.Element;
import org.dom4j.io.SAXReader;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.LinkedBlockingQueue;

import ServerMap.ThreadExportOption;
public class Client {

	private final static Logger logger = Logger.getLogger(Client.class.getName());
	String ServerIP;//服务器IP地址
	int Port;//服务器端口
	String Save_Place;//切片文件保存地址
	MapTile mp=null;
	public void Read_XML(){
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
		// jason
		Element root = document.getRootElement();
		Element Get_ServerIP = (Element) root.selectSingleNode("ServerIP");
		ServerIP= Get_ServerIP.selectSingleNode("Address").getStringValue();
		Port=Integer.parseInt(Get_ServerIP.selectSingleNode("Port").getStringValue());
		Element Get_SavePlace = (Element) root.selectSingleNode("ClientSavePlace");
		Save_Place= Get_SavePlace.selectSingleNode("Index").getStringValue();
	}

	public static void main(String[] args)   {
		Client temp=new Client();
		temp.Read_XML();
		try{
			temp.GetCutinfo(temp);
		}catch(Exception ex){
			System.out.println("wrong!");
		}	
	}
	public void GetCutinfo(Client temp) throws Exception{
		Socket socket = null;
		ObjectInputStream is = null;
		//轮询，服务端是否打开
		int connected_e=0;
		while(connected_e==0){
			try{
				System.out.println(ServerIP+Port);
				socket = new Socket(ServerIP, Port);
				connected_e=1;
			}catch(Exception e){
				Thread.sleep(3000);//未找到则等待3秒
				connected_e=0;
			}
		}
		try {
			ThreadExportOption threadexp_client = new ThreadExportOption();
			is = new ObjectInputStream(new BufferedInputStream(socket.getInputStream()));
			Object obj = is.readObject();
			if (obj != null) {
				threadexp_client = (ThreadExportOption) obj;
				//将保存目录重新定义为XML文件中的目录 
				System.out.println(threadexp_client.levelDir);
				threadexp_client.levelDir=Save_Place;
				File file =new File(Save_Place);    
				//如果文件夹不存在则创建    
				if  (!file .exists()  && !file .isDirectory())      
				{       
					file .mkdir();    
				}

				System.out.println("Index_bds: " + threadexp_client.Index_bds);
				if (threadexp_client.cut_finished == 1) {// 若已经切完，则显示完成
					System.out.println("finished!");
				} else {// 否则开始切图
					ThreadPoolExecutor executor = null;
					executor = new ThreadPoolExecutor(5,5, 4, TimeUnit.HOURS, new LinkedBlockingQueue<Runnable>());
					//					mp.wirteLog("比例尺" + threadexp_client.scale + "====" + mp.getNowString() + "====");
					System.out.println("StatPoint: " + threadexp_client.StatPoint);
					System.out.println("cuted: " + threadexp_client.cuted);

					for (int i = threadexp_client.StatPoint; i <threadexp_client.cuted; i++) {
						ThreadExportOption threadOpt2=new ThreadExportOption();
						GetImageData runnable = new GetImageData();
						threadOpt2 = (ThreadExportOption)threadexp_client.clone();
						threadOpt2.cut_index = i;
						runnable.setParameter(threadOpt2);
						executor.execute(runnable);
					}
					Listening listenOver=new Listening();//添加监听线程结束函数
					listenOver.setExecutor(executor);//将线程池引用给监听类
					listenOver.Get_Info(ServerIP, Port, Save_Place);//将相关信息传给监听类
					executor.execute(listenOver);
				}	
			}
		} catch (IOException ex) {
			logger.log(Level.SEVERE, null, ex);
		} finally {
			try {
				is.close();
				socket.close();
			} catch (Exception ex) {
				System.out.print("关闭socket/io失败");
			}
		}
	}
}