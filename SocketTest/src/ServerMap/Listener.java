package ServerMap;

import java.util.concurrent.ThreadPoolExecutor;

/*
 * 该函数用来监听服务端是否自己已经切完并为服务器端补充切图线程
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
		threadOpt_server.cuted=(int) bundlecut.indexlist.get(threadOpt_server.Index_bds);//改为计算下一个要切的提前量
		
	}

	public void run(){
		//修改监听重新获取的方法，改为当线程处理结束时进行请求
		while(true){
			if(executor.getActiveCount()<=2)break;
			try {
				Thread.sleep(5000);//等待5秒后继续查看线程池是否结束
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
			int end_to=(int) bundlecut.indexlist.get(threadOpt_server.Index_bds);//修改为从头开始，从前面开始取一个包。
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
				System.out.println( "分发完所有任务，等待本地切图结束....");
				while(true){
					System.out.println("ExecutorNum:"+executor.getActiveCount());
					//只有2个线程的时候，即无包可取，分发线程和监听线程还在运行的情况
					if(executor.getActiveCount()<=2){
						System.out.println( "关闭程序!!");
						mp=MapTile.getInstance();
						mp.wirteLog(
								"时间统计 " + mp.Alltime + "==结束时间==" + mp.getNowString() + "=IO总计:" + mp.iotime + "=请求时间:" + mp.seqtime );

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
			executor.execute(this);//继续监听
		}else{
			System.out.println( "分发完所有任务，等待本地切图结束....");
			while(true){
				System.out.println("ExecutorNum:"+executor.getActiveCount());
				//只有2个线程的时候，即无包可取，分发线程和监听线程还在运行的情况
				if(executor.getActiveCount()<=2){
					System.out.println( "关闭程序!!");
					mp=MapTile.getInstance();
					mp.wirteLog(
							"时间统计 " + mp.Alltime + "==结束时间==" + mp.getNowString() + "=IO总计:" + mp.iotime + "=请求时间:" + mp.seqtime );

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
