package socketmap;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.logging.Level;
import java.util.logging.Logger;

public class socketmapserver implements Runnable {

	private final static Logger logger = Logger.getLogger(socketmapserver.class.getName());
	public ThreadExportOption threadexp;
	public Socket socket;

	public void setthread(ThreadExportOption threadexp) {
		this.threadexp = threadexp;
	}

	public synchronized void addIndex_bds() {
		threadexp.Index_bds++;
	}

	public void run() {
		ServerSocket server = null;
		try {
			server = new ServerSocket(10000);
			// 等待新请求、否则一直阻塞
			while (true) {
				socket = server.accept();
				ObjectOutputStream os = null;
				try {
					os = new ObjectOutputStream(socket.getOutputStream());
					if (threadexp.Index_bds > threadexp.Pc_Number) {
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
					addIndex_bds();
				}
			}
		} catch (Exception e) {
			try {
				socket.close();
			} catch (IOException e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			}
		} finally {
			try {
				server.close();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}

		}

	}
}