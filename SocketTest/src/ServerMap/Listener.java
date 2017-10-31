package ServerMap;

import java.util.concurrent.ThreadPoolExecutor;

/*
 * �ú�����������������Ƿ��Լ��Ѿ����겢Ϊ�������˲�����ͼ�߳�
 */
public class Listener implements Runnable{
	ThreadExportOption  threadOpt_server;
	ThreadPoolExecutor executor;
	MapTile mp = null;
	BundleCut bundlecut=null;
	public void Get_ThreadPool(ThreadPoolExecutor Executor_In){
		executor=Executor_In;
	}
	public void Get_threadOpt(ThreadExportOption in_threadOpt){
		this.threadOpt_server=in_threadOpt;
	}
	public synchronized void addIndex_bds() {
		mp.StatPoint=(int) bundlecut.indexlist.get(threadOpt_server.Index_bds);
		threadOpt_server.StatPoint=mp.StatPoint;
		threadOpt_server.Index_bds++;
		threadOpt_server.cuted=(int) bundlecut.indexlist.get(threadOpt_server.Index_bds);//��Ϊ������һ��Ҫ�е���ǰ��
		
	}

	public void run(){
		//�޸ļ������»�ȡ�ķ�������Ϊ���̴߳������ʱ��������
		while(true){
			if(executor.getActiveCount()<=2)break;
			try {
				Thread.sleep(5000);//�ȴ�5�������鿴�̳߳��Ƿ����
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
//		threadOpt_server.Index_bds=mp.First-mp.StatPoint;
		if(threadOpt_server.cut_finished==0&&threadOpt_server.Index_bds <= bundlecut.indexlist.size()-2){
			System.out.println(threadOpt_server.Index_bds+ "Listener!!");
			System.out.println(mp.StatPoint+ "Listener!!");
			System.out.println(threadOpt_server.Index_bds+ "Listener!!");

//			StatPoint=(int) bundlecut.indexlist.get(indexclient);
			int end_to=(int) bundlecut.indexlist.get(threadOpt_server.Index_bds);//�޸�Ϊ��ͷ��ʼ����ǰ�濪ʼȡһ������
			for (int i = mp.StatPoint; i < end_to; i++) {
				ThreadExportOption threadOpt2=new ThreadExportOption();
				GetImageData runnable = new GetImageData();
				threadOpt2 = (ThreadExportOption)threadOpt_server.clone();
				threadOpt2.cut_index = i;
				runnable.setParameter(threadOpt2);
				executor.execute(runnable);
			}
			addIndex_bds();
			if(threadOpt_server.Index_bds > bundlecut.indexlist.size()-2) 
			{
				System.out.println( "�ַ����������񣬵ȴ�������ͼ����....");
				while(true){
					System.out.println("ExecutorNum:"+executor.getActiveCount());
					//ֻ��2���̵߳�ʱ�򣬼��ް���ȡ���ַ��̺߳ͼ����̻߳������е����
					if(executor.getActiveCount()<=2){
						System.out.println( "�رճ���!!");
						mp=MapTile.getInstance();
						mp.wirteLog(
								"ʱ��ͳ�� " + mp.Alltime + "==����ʱ��==" + mp.getNowString() + "=IO�ܼ�:" + mp.iotime + "=����ʱ��:" + mp.seqtime );

						System.exit(0);
					}
					try {
						Thread.sleep(10000);
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
				}
			}
			System.out.println("ExecutorNum:"+executor.getActiveCount());
			executor.execute(this);//��������
		}else{
			System.out.println( "�ַ����������񣬵ȴ�������ͼ����....");
			while(true){
				System.out.println("ExecutorNum:"+executor.getActiveCount());
				//ֻ��2���̵߳�ʱ�򣬼��ް���ȡ���ַ��̺߳ͼ����̻߳������е����
				if(executor.getActiveCount()<=2){
					System.out.println( "�رճ���!!");
					mp=MapTile.getInstance();
					mp.wirteLog(
							"ʱ��ͳ�� " + mp.Alltime + "==����ʱ��==" + mp.getNowString() + "=IO�ܼ�:" + mp.iotime + "=����ʱ��:" + mp.seqtime );

					System.exit(0);
				}
				try {
					Thread.sleep(10000);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}
		}
	}
}
