package yushanmufeng.vmck.agent.advice;



import net.bytebuddy.asm.Advice;

public final class ObjectAdvice {
	
	private ObjectAdvice() {
//		Object
	}
	
	
	// @Advice.This Thread thread,  @Advice.Argument(value = 0, readOnly = false) Long ms
	@Advice.OnMethodExit
    public static void waitEnter() {
    	System.out.println("this is wait enter~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~");
//		Thread thread = Thread.currentThread();
//    		if(thread.getName().contains("$$vmck_")) {
//    			return 0L;
//    		}
//		try { 
//			long originMs = ms.longValue();
//    		 Class<?> vmckDriverFacade = Class.forName("yushanmufeng.vmck.driver.VmckDriverFacade");
//    		 Class<?>[] clazzs = new Class[3];
//    		 clazzs[0] =  Thread.class;
//    		 clazzs[1] =  Object.class;
//    		 clazzs[2] =  long.class;
//   		     java.lang.reflect.Method method = vmckDriverFacade.getMethod("object_wait_before", clazzs); 
//   		     Object[] objs = new Object[3];
//   		     objs[0] = thread;
//   		     objs[1] = thread;
//   		     objs[2] = ms.longValue();
//   		     Object result = method.invoke(null, objs); 
//   		     ms = (Long)result;
//   		     return originMs;
//   		 } catch (ClassNotFoundException | NoSuchMethodException | SecurityException | IllegalAccessException | IllegalArgumentException | InvocationTargetException e) {
//   			 System.out.println(e);
//   			 e.printStackTrace();
//   		 }
//    	return 0L;
    }
    
    // 
//    @Advice.OnMethodExit(onThrowable = InterruptedException.class)
//    public static void exitWait(@Advice.Argument(0) Long newMs, @Advice.Enter Long originMs, @Advice.Thrown(readOnly = false) Throwable throwable) {
//		Thread thread = Thread.currentThread();
//		if(thread.getName().contains("$$vmck_")) {
//			return;
//		}
//		try { 
//    		 Class<?> vmckDriverFacade = Class.forName("yushanmufeng.vmck.driver.VmckDriverFacade");
//   		     Class<?>[] clazzs = new Class[5];
//   		     clazzs[0] =  Thread.class;
//    		 clazzs[1] =  Object.class;
//    		 clazzs[2] =  long.class;
//    		 clazzs[3] =  long.class;
//    		 clazzs[4] =  Throwable.class;
//   		     java.lang.reflect.Method method = vmckDriverFacade.getMethod("object_wait_after", clazzs); 
//   		     Object[] objs = new Object[5];
//   		     objs[0] = thread;
//   		     objs[1] = thread;
//   		     objs[2] = newMs.longValue();
//   		     objs[3] = originMs.longValue();
//   		     objs[4] = throwable;
//   		     throwable = (Throwable)method.invoke(null, objs); 
//   		 } catch (ClassNotFoundException | NoSuchMethodException | SecurityException | IllegalAccessException | IllegalArgumentException | InvocationTargetException e) { 
//   			System.out.println(e);
//   			 e.printStackTrace();
//   		 }
//    }

}
