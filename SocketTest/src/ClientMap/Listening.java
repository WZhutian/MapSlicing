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
	String ServerIP;//������IP��ַ
	int Port;//�������˿�
	String Save_Place;//��Ƭ�ļ������ַ
	public void Get_Info(String ServerIP,int Port,String Save_Place){
		this.ServerIP=ServerIP;
		this.Port=Port;
		this.Save_Place=Save_Place;
	}
	public void run(){
		//�޸ļ������»�ȡ�ķ�������Ϊ���̴߳������ʱ��������
		while(true){
			if(executor.getActiveCount()<=1)break;
			try {
				Thread.sleep(5000);//�ȴ�5�������鿴�̳߳��Ƿ����
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
		Socket socket = null;
		ObjectInputStream is = null;
		System.out.println("New Req start!");
		try {
			//��������IP��ַ��Ϊ��̬��XML ��ȡ 
			socket = new Socket(ServerIP, Port);
			ThreadExportOption threadexp_client = new ThreadExportOption();
			is = new ObjectInputStream(new BufferedInputStream(socket.getInputStream()));
			Object obj = is.readObject();
			if (obj != null) {
				threadexp_client = (ThreadExportOption) obj;
				System.out.println(threadexp_client);
				//������Ŀ¼���¶���ΪXML�ļ��е�Ŀ¼ 
				threadexp_client.levelDir=Save_Place;

				System.out.println("Index_bds: " + threadexp_client.Index_bds);
				if (threadexp_client.cut_finished == 1) {// ���Ѿ����꣬����ʾ���
					System.out.println("���������ַ���ϣ��ͻ��˲��ٽ��л�ȡ���ȴ�������ͼ����...");
					while(true){
						System.out.println("ExecutorNum:"+executor.getActiveCount());
						if(executor.getActiveCount()<=1){
							System.out.println("�ͻ��˽���");
							mp=MapTile.getInstance();
							mp.wirteLog(
									"ʱ��ͳ�� " + mp.Alltime + "==����ʱ��==" + mp.getNowString() + "=IO�ܼ�:" + mp.iotime + "=����ʱ��:" + mp.seqtime );
						
							System.exit(0);
						}
						try {
							Thread.sleep(10000);//�ȴ�10�������鿴�̳߳��Ƿ����
						} catch (InterruptedException e) {
							e.printStackTrace();
						}
					}
				} else {// ����ʼ��ͼ
					for (int i = threadexp_client.StatPoint; i <threadexp_client.cuted; i++) {
						ThreadExportOption threadOpt2=new ThreadExportOption();
						GetImageData runnable = new GetImageData();
						threadOpt2 = (ThreadExportOption)threadexp_client.clone();
						threadOpt2.cut_index = i;
						runnable.setParameter(threadOpt2);
						executor.execute(runnable);
					}
					executor.execute(this);//������Ӽ���
				}
			}
		} catch (IOException ex) {
			System.out.println("����˹رգ��ȴ�������ͼ����...");
			while(true){
				System.out.println("ExecutorNum:"+executor.getActiveCount());
				if(executor.getActiveCount()<=1){
					System.out.println("�ͻ��˽���");
					mp=MapTile.getInstance();
					mp.wirteLog(
							"ʱ��ͳ�� " + mp.Alltime + "==����ʱ��==" + mp.getNowString() + "=IO�ܼ�:" + mp.iotime + "=����ʱ��:" + mp.seqtime );
				
					System.exit(0);
				}
				try {
					Thread.sleep(10000);//�ȴ�10�������鿴�̳߳��Ƿ����
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
				System.out.println("�޷��رտͻ���socket");
			}
		}
	}
}
