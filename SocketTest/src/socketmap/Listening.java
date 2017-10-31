package socketmap;

public class Listening implements Runnable {
	client Temp;
	public void setTemp(client Temp){
		this.Temp=Temp;
	}
	public void run(){
		System.out.println(Temp.RunningThread);
		while(true){
			if(Temp.RunningThread==0){
				System.out.println("New Req start!");
				client runanotherReq=new client();
				runanotherReq.main(new String[]{"one","two","three"});
				break;
			}
		}
	}
}
