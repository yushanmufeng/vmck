package yushanmufeng.vmck.agent.driverimpl;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import yushanmufeng.vmck.agent.Config;
import yushanmufeng.vmck.core.IVmckDriver;
import yushanmufeng.vmck.core.VmckDriverFacade;


/**
 * 注入的字节码执行时的实际调用逻辑，由AppClassLoader加载
 * 
 * @author yushanmufeng
 */
public class VmckDriverImpl implements IVmckDriver{

	private volatile static VmckDriverImpl instance;
	
	public int test = 0;
	
	static {
		// VmckDriverFacade和driver接口由BootstrapClassLoader加载，来保证所有jvm加载的所有类包括系统类，都能对动态注入的字节码正确执行
		instance = new VmckDriverImpl();
		VmckDriverFacade.registerDriver(instance);
	}
	
	public static VmckDriverImpl getInstance() {
		return instance;
	}
	
	private long lastOriginalTimeMillis = 0;
	private VmClock vmClock;
	
	public VmckDriverImpl() {
		vmClock = new VmClock(Config.params.getStandardTime() - System.currentTimeMillis());
		threadSleepEndTime = new ConcurrentHashMap<Thread, Long>();
		jumpTimeThreadSet = ConcurrentHashMap.newKeySet();
		objectWaitEndTime = new ConcurrentHashMap<Thread, Long>();
		jumpTimeThreadSet = ConcurrentHashMap.newKeySet();
	}
	
	public VmClock getVmClock() {
		return vmClock;
	}

	public long realCurrentTimeMillis() {
		System.currentTimeMillis();
		return lastOriginalTimeMillis;
	}
	
	@Override
	public long system_currentTimeMillis(long originalTimeMillis) {
		long value = vmClock.calcVmTime(originalTimeMillis);
		return value;
	}


	public volatile Map<Thread, Long> threadSleepEndTime;
	public volatile Set<Thread> jumpTimeThreadSet;
	
	@Override
	public long thread_sleep_before(Thread currentThread, long ms) {
		threadSleepEndTime.put(currentThread, System.currentTimeMillis() + ms);
		return ms;
	}
	
	@Override
	public Throwable thread_sleep_after(Thread currentThread, long newMs, long originMs, Throwable throwable) throws Throwable {
		threadSleepEndTime.remove(currentThread);
		if(throwable != null) {
			if(throwable instanceof InterruptedException && jumpTimeThreadSet.contains(currentThread)) {
				// 跳跃时间操作：为了结束线程休眠主动抛出的异常，捕获异常
				jumpTimeThreadSet.remove(currentThread);
//				System.out.println("catch Exception, and clear interrupt state!!");
				try {
					Thread.interrupted();	// 清理线程中断状态，防止影响业务逻辑
				}catch(Exception e) {
					
				}
				return null;
			}else {
				// 业务代码发生的异常，与vmck无关，不作处理
//				System.out.println("throw Exception!! e:" + throwable + " , size:" + jumpTimeThreadSet.size());
			}
		}
		return throwable;
	}

	public volatile Map<Thread, Long> objectWaitEndTime;
	public volatile Set<Thread> objectJumpTimeSet;
	
	@Override
	public long object_wait_before(Thread currentThread, Object originObj, long ms) {
		if(ms > 0) {
			objectWaitEndTime.put(currentThread, System.currentTimeMillis() + ms);
		}
		return ms;
	}

	@Override
	public Throwable object_wait_after(Thread currentThread, Object originObj, long newMs, long originMs, Throwable throwable) {
		if(originMs > 0) {
			objectWaitEndTime.remove(currentThread);
			if(throwable != null) {
				if(throwable instanceof InterruptedException && objectJumpTimeSet.contains(currentThread)) {
					// 跳跃时间操作：为了结束线程休眠主动抛出的异常，捕获异常
					objectJumpTimeSet.remove(currentThread);
//					System.out.println("catch Exception, and clear interrupt state!!");
					try {
						Thread.interrupted();	// 清理线程中断状态，防止影响业务逻辑
					}catch(Exception e) {
						
					}
					return null;
				}else {
					// 业务代码发生的异常，与vmck无关，不作处理
//					System.out.println("throw Exception!! e:" + throwable + " , size:" + jumpTimeThreadSet.size());
				}
			}
		}
		return throwable;
	}


}
