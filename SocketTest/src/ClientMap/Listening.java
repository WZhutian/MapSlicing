package ClientMap;
import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.LinkedBlockingQueue;

import ServerMap.MapTile;
import ServerMap.ThreadExportOption;
public class Listening implements Runnable {
	//	private final static Logger logger = Logger.getLogger(Client.class.getName());
	//	
	MapTile mp = null;
	ThreadPoolExecutor executor;
	public void setExecutor(ThreadPoolExecutor exe_Temp){
		this.executor=exe_Temp;
	}
	String ServerIP;//服务器IP地址
	int Port;//服务器端口
	String Save_Place;//切片文件保存地址
	public void Get_Info(String ServerIP,int Port,String Save_Place){
		this.ServerIP=ServerIP;
		this.Port=Port;
		this.Save_Place=Save_Place;
	}
	public void run(){
		//修改监听重新获取的方法，改为当线程处理结束时进行请求
		while(true){
			if(executor.getActiveCount()<=1)break;
			try {
				Thread.sleep(5000);//等待5秒后继续查看线程池是否结束
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
		Socket socket = null;
		ObjectInputStream is = null;
		System.out.println("New Req start!");
		try {
			//将服务器IP地址改为动态从XML 获取 
			socket = new Socket(ServerIP, Port);
			ThreadExportOption threadexp_client = new ThreadExportOption();
			is = new ObjectInputStream(new BufferedInputStream(socket.getInputStream()));
			Object obj = is.readObject();
			if (obj != null) {
				threadexp_client = (ThreadExportOption) obj;
				System.out.println(threadexp_client);
				//将保存目录重新定义为XML文件中的目录 
				threadexp_client.levelDir=Save_Place;

				System.out.println("Index_bds: " + threadexp_client.Index_bds);
				if (threadexp_client.cut_finished == 1) {// 若已经切完，则显示完成
					System.out.println("服务端任务分发完毕，客户端不再进行获取，等待本地切图结束...");
					while(true){
						System.out.println("ExecutorNum:"+executor.getActiveCount());
						if(executor.getActiveCount()<=1){
							System.out.println("客户端结束");
							mp=MapTile.getInstance();
							mp.wirteLog(
									"时间统计 " + mp.Alltime + "==结束时间==" + mp.getNowString() + "=IO总计:" + mp.iotime + "=请求时间:" + mp.seqtime );
						
							System.exit(0);
						}
						try {
							Thread.sleep(10000);//等待10秒后继续查看线程池是否结束
						} catch (InterruptedException e) {
							e.printStackTrace();
						}
					}
				} else {// 否则开始切图
					for (int i = threadexp_client.StatPoint; i <threadexp_client.cuted; i++) {
						ThreadExportOption threadOpt2=new ThreadExportOption();
						GetImageData runnable = new GetImageData();
						threadOpt2 = (ThreadExportOption)threadexp_client.clone();
						threadOpt2.cut_index = i;
						runnable.setParameter(threadOpt2);
						executor.execute(runnable);
					}
					executor.execute(this);//继续添加监听
				}
			}
		} catch (IOException ex) {
			System.out.println("服务端关闭，等待本地切图结束...");
			while(true){
				System.out.println("ExecutorNum:"+executor.getActiveCount());
				if(executor.getActiveCount()<=1){
					System.out.println("客户端结束");
					mp=MapTile.getInstance();
					mp.wirteLog(
							"时间统计 " + mp.Alltime + "==结束时间==" + mp.getNowString() + "=IO总计:" + mp.iotime + "=请求时间:" + mp.seqtime );
				
					System.exit(0);
				}
				try {
					Thread.sleep(10000);//等待10秒后继续查看线程池是否结束
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}
		} catch (ClassNotFoundException e) {
			e.printStackTrace();
		} finally {
			try {
				is.close();
				socket.close();
			} catch (Exception ex) {
				System.out.println("无法关闭客户端socket");
			}
		}
	}
}
