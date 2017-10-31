package ServerMap;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.logging.Level;
import java.util.logging.Logger;


public class socketmapserver implements Runnable {

	private final static Logger logger = Logger.getLogger(socketmapserver.class.getName());
	public ThreadExportOption threadexp;
	public Socket socket;
    public int i=0;
	MapTile mp = null;
    BundleCut bundlecut=null;
	public void setthread(ThreadExportOption threadexp) {
		this.threadexp = threadexp;
		threadexp.cuted=(int) bundlecut.indexlist.get(0);
	}

	public synchronized void addIndex_bds() {
		System.out.println("Statpoint"+mp.StatPoint);
		mp.StatPoint=(int) bundlecut.indexlist.get(threadexp.Index_bds);
		threadexp.Index_bds++;
		threadexp.cuted=(int) bundlecut.indexlist.get(threadexp.Index_bds);//��Ϊ������һ��Ҫ�е���ǰ��

		threadexp.StatPoint=mp.StatPoint;
		System.out.println("�޸�Statpoint"+mp.StatPoint);
		System.out.println("�鿴cuted"+threadexp.cuted);
		//		for(i=1;i<bundlecut.indexlist.size();i++){
//			threadexp.cuted=(int) bundlecut.indexlist.get(i);
//		}
	}

	public void run() {
		ServerSocket server = null;
		try {
			server = new ServerSocket(10000);
			while(true){
				socket = server.accept();
				// �ȴ������󡢷���һֱ����
				System.out.println(threadexp.Index_bds+ "socketmapserver!!");
				
				ObjectOutputStream os = null;
				try {
					os = new ObjectOutputStream(socket.getOutputStream());
//					threadexp.Index_bds=bundlecut.indexlist.size();
					if (threadexp.Index_bds > bundlecut.indexlist.size()-2) {
						threadexp.cut_finished = 1;
					} else {
						threadexp.cut_finished = 0;
					}
					os.writeObject(threadexp);
					os.flush();
					
				} catch (IOException ex) {
					logger.log(Level.SEVERE, null, ex);
				} finally {
					os.close();
					socket.close();
//					threadexp.StatPoint+=threadexp.cuted; //���ֵΪ��һ��������и�Ⱥ����
					addIndex_bds(); 
					
				}
			}
		} catch (Exception e) {
			try {
				socket.close();
			} catch (IOException e1) {
				e1.printStackTrace();
			}
		} finally {
			try {
				server.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}

	}
}