package yushanmufeng.vmck.core;

public interface IVmckDriver {

	public long system_currentTimeMillis(long originalTimeMillis);
	
	public long thread_sleep_before(Thread currentThread, long ms);

	public Throwable thread_sleep_after(Thread currentThread, long newMs, long originMs, Throwable throwable) throws Throwable ;
	
	public long object_wait_before(Thread currentThread, Object originObj, long ms);
	
	public Throwable object_wait_after(Thread currentThread, Object originObj, long newMs, long originMs, Throwable throwable);
	
}
