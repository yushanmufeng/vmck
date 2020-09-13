package yushanmufeng.vmck.agent.advice;

import net.bytebuddy.asm.Advice;
import net.bytebuddy.implementation.bytecode.assign.Assigner.Typing;

public final class SystemAdvice {
	
	private SystemAdvice() {}

    @Advice.OnMethodExit
    public static void currentTimeMillis(@Advice.Return(readOnly = false, typing=Typing.DYNAMIC) Long originalReturnValue) {
		 try { 
			 Class<?> vmckDriverFacade = Class.forName("yushanmufeng.vmck.core.VmckDriverFacade");
		     Class<?>[] clazzs = new Class[1];
		     clazzs[0] = long.class;
		     java.lang.reflect.Method method = vmckDriverFacade.getMethod("system_currentTimeMillis", clazzs); 
		     Object[] objs = new Object[1];
		     objs[0] = originalReturnValue;
		     Object result = method.invoke(null, objs); 
		     originalReturnValue = (Long)result; 
		 } catch (Exception e) { 
			e.printStackTrace(); 
		 }
         
    }

}
