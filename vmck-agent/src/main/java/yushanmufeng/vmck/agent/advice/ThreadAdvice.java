package yushanmufeng.vmck.agent.advice;

import net.bytebuddy.asm.Advice;

public final class ThreadAdvice {
	
	private ThreadAdvice() {
	}

    @Advice.OnMethodEnter
    public static Long sleepEnter(@Advice.Origin String methodStr, @Advice.Argument(value = 0, readOnly = false) Long ms) {
		Thread thread = Thread.currentThread();
		if(thread.getName().contains("$$vmck_")) {
			return 0L;
		}
		try { 
			long originMs = ms.longValue();
    		 Class<?> vmckDriverFacade = Class.forName("yushanmufeng.vmck.core.VmckDriverFacade");
    		 Class<?>[] clazzs = new Class[2];
    		 clazzs[0] = Thread.class;
    		 clazzs[1] = long.class;
   		     java.lang.reflect.Method method = vmckDriverFacade.getMethod("thread_sleep_before", clazzs); 
   		     Object[] objs = new Object[2];
   		     objs[0] = thread;
   		     objs[1] = ms.longValue();
   		     Object result = method.invoke(null, objs); 
   		     ms = (Long)result;
   		     return originMs;
   		 } catch (Exception  e) {
   			 System.out.println(e);
   			 e.printStackTrace();
   		 }
    	return 0L;
    }
    
    @Advice.OnMethodExit(onThrowable = InterruptedException.class)
    public static void sleepExit( @Advice.Argument(0) Long newMs, @Advice.Enter Long originMs, @Advice.Thrown(readOnly = false) Throwable throwable) {
		Thread thread = Thread.currentThread();
		if(thread.getName().contains("$$vmck_")) {
			return;
		}
		try { 
    		 Class<?> vmckDriverFacade = Class.forName("yushanmufeng.vmck.core.VmckDriverFacade");
   		     Class<?>[] clazzs = new Class[4] ;
   		     clazzs[0] = Thread.class;
    		 clazzs[1] = long.class;
    		 clazzs[2] = long.class;
    		 clazzs[3] = Throwable.class;
   		     java.lang.reflect.Method method = vmckDriverFacade.getMethod("thread_sleep_after", clazzs); 
   		     Object[] objs = new Object[4];
   		     objs[0] = thread;
   		     objs[1] = newMs.longValue();
   		     objs[2] = originMs.longValue();
   		     objs[3] = throwable;
   		     throwable = (Throwable)method.invoke(null, objs); 
   		 } catch (Exception e) { 
   			System.out.println(e);
   			 e.printStackTrace();
   		 }
    }

}
