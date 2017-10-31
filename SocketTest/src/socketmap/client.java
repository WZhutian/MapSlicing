package socketmap;

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

public class client {

	private final static Logger logger = Logger.getLogger(client.class.getName());
	public int RunningThread=0;
	public synchronized void cutdownThread(){
		RunningThread--;
	}
	public static void main(String[] args)   {
		client temp=new client();
		try{
			temp.GetCutinfo(temp);
		}catch(Exception ex){
			System.out.println("wrong!");
		}	
	}
	public void GetCutinfo(client temp) throws Exception{
		Socket socket = null;
		ObjectInputStream is = null;
		try {
			socket = new Socket("localhost", 10000);//需要修改的地方，改成主机IP地址
			ThreadExportOption threadexp_client = new ThreadExportOption();
			is = new ObjectInputStream(new BufferedInputStream(socket.getInputStream()));
			Object obj = is.readObject();
			if (obj != null) {
				threadexp_client = (ThreadExportOption) obj;
				System.out.println("Index_bds: " + threadexp_client.Index_bds);
				if (threadexp_client.cut_finished == 1) {// 若已经切完，则显示完成
					System.out.println("finished!");
				} else {// 否则开始切图
					System.out.println("Index_bds: " + threadexp_client.Index_bds);
					ThreadPoolExecutor executor = null;
					executor = new ThreadPoolExecutor(4, 4, 3, TimeUnit.HOURS, new LinkedBlockingQueue<Runnable>());
					RunningThread = threadexp_client.Number_bds;//设置当前运行的总线程数
					System.out.println(threadexp_client.First_bd+","+threadexp_client.First_bd);
					for (int i = 0; i < threadexp_client.Number_bds; i++) {
						ThreadExportOption threadOpt2=new ThreadExportOption();
						GetImageData runnable = new GetImageData();
						threadOpt2 = (ThreadExportOption)threadexp_client.clone();
						threadOpt2.cut_index = i;
						runnable.setParameter(threadOpt2,temp);
						executor.execute(runnable);
					}
					Listening listenOver=new Listening();//添加监听线程结束函数
					listenOver.setTemp(temp);
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
			}
		}
	}
}