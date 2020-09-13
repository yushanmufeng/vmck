package yushanmufeng.vmck.core;

public class VmckDriverFacade {

	public static IVmckDriver vmckDriver = null;
	
	public static void registerDriver(IVmckDriver driver) {
		VmckDriverFacade.vmckDriver = driver;
	}
	
	public static long system_currentTimeMillis(long originalTimeMillis) {
		return vmckDriver.system_currentTimeMillis(originalTimeMillis);
	}
	
	public static long thread_sleep_before(Thread currentThread, long ms) {
		return vmckDriver.thread_sleep_before(currentThread, ms);
	}
	
	public static Throwable thread_sleep_after(Thread currentThread, long newMs, long originMs, Throwable throwable) throws Throwable {
		return vmckDriver.thread_sleep_after(currentThread, newMs, originMs, throwable);
	}
	
	public static long object_wait_before(Thread currentThread, Object originObj, long ms) {
		return vmckDriver.object_wait_before(currentThread, originObj, ms);
	}
	
	public static Throwable object_wait_after(Thread currentThread, Object originObj, long newMs, long originMs, Throwable throwable) {
		return vmckDriver.object_wait_after(currentThread, originObj, newMs, originMs, throwable);
	}
	
}
