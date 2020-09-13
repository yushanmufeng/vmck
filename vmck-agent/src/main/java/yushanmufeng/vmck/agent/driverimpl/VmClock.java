package yushanmufeng.vmck.agent.driverimpl;

import java.util.concurrent.atomic.AtomicLong;

public class VmClock {

	// 向将来偏移的差值。vmTime = realTime + offsetTime
	private volatile AtomicLong futureOffsetTime;
	
	/**
	 * @param futureOffsetTime 向将来偏移的差值 , vmTime = realTime + offsetTime
	 */
	public VmClock(long futureOffsetTime) {
		this.futureOffsetTime = new AtomicLong(futureOffsetTime);
	}
	
	public long calcVmTime(long realTime) {
		long offset = futureOffsetTime.get();
		return realTime + offset;
	}
	
	/**
	 * 增加偏移量
	 */
	public void addOffsetTime(long addOffsetTime) {
		futureOffsetTime.addAndGet(addOffsetTime);
	}
	
	public long getOffsetTime() {
		return futureOffsetTime.get();
	}
}
