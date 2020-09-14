package yushanmufeng.vmck.agent.admin;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashSet;
import java.util.Map.Entry;
import java.util.Set;

import yushanmufeng.vmck.agent.Config;
import yushanmufeng.vmck.agent.driverimpl.VmClock;
import yushanmufeng.vmck.agent.driverimpl.VmckDriverImpl;

public class AdminService {

	private VmckDriverImpl driver;
	private VmClock vmClock;

	public AdminService() {
		this.driver = VmckDriverImpl.getInstance();
		this.vmClock = driver.getVmClock();
	}
	
	/**
	 * 获取vmck状态
	 */
	public String getStatus() {
		StringBuilder result = new StringBuilder();
		String vmtime = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format( new Date(System.currentTimeMillis()) );
		result.append("{")
			.append("\"name\":\"").append(Config.name).append("\",")
			.append("\"temppath\":\"").append(Config.VMCK_DIR + Config.SEPARATOR + Config.name).append("\",")
			.append("\"vmtime\":\"").append( vmtime ).append("\"")
			.append("}")
		;
		return result.toString();
	}
	
	/**
	 * 恢复时间为正常时间。 回退时间可能会导致程序发生逻辑异常，重置时间后最好重启程序
	 */
	public void recoverTime() {
		vmClock.setOffsetTime(0);
	}
	
	/**
	 * 穿越时空
	 */
	public void passThroughTime(int minute) {
		Thread thread = new Thread(new PassThroughTimeThread(System.currentTimeMillis() + minute * 60 * 1000), "$$vmck_passThroughTime");
		thread.start();
	}
	
	// 加速时间
	class PassThroughTimeThread implements Runnable{

		private VmckDriverImpl driver;
		private VmClock vmClock;
		private long targetTime; 
		
		public PassThroughTimeThread(long targetTime) {
			this.driver = VmckDriverImpl.getInstance();
			this.vmClock = driver.getVmClock();
			this.targetTime = targetTime;
		}
		
		@Override
		public void run() {
			long STEP_MS = 1000;
			long curMs;
			for(vmClock.addOffsetTime(STEP_MS); (curMs = System.currentTimeMillis()) < targetTime; vmClock.addOffsetTime(STEP_MS)) {
				Set<Entry<Thread, Long>> threadSet = driver.threadSleepEndTime.entrySet();
				// 向所有线程发送通知
				Set<Thread> interruptThread = new HashSet<Thread>();
				for(Entry<Thread, Long> entry : threadSet) {
					Thread thread = entry.getKey();
					long sleepEndTime = entry.getValue();
					if(sleepEndTime <= curMs) {
						driver.jumpTimeThreadSet.add(thread);
						interruptThread.add(thread);
						thread.interrupt();
					}
				}
				// 等待所有interrupt通知生效
				while(driver.jumpTimeThreadSet.size() > 0) {
					try {
						Thread.sleep(10);
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
				}
				// 阻塞状态线程达到历史水平
				boolean allInterrupted = true;
				for(int count = 1; count <= 10; count++) {
					allInterrupted = true;
					for(Thread thread  : interruptThread) {
						if(thread.getState()==Thread.State.TIMED_WAITING || thread.getState()==Thread.State.TERMINATED || !thread.isAlive()) {
						}else {
//							System.out.println(thread.getName() + "interrupted:" + thread.isInterrupted() + ", alive:" + thread.isAlive());
							allInterrupted = false;
							break;
						}
					}
					if(allInterrupted) {
						break;
					}else {
						try {
							Thread.sleep(5 + count * 8);
						} catch (InterruptedException e) {
							e.printStackTrace();
						}
					}
				}
				System.out.println("allInterrupted:" + allInterrupted);
			}
			Config.saveParams();
			System.out.println("=================Pass Time Finish=================");
		}
		
	}
	
}
