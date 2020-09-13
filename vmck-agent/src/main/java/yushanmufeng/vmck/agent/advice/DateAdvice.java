package yushanmufeng.vmck.agent.advice;

import java.util.Date;

import net.bytebuddy.asm.Advice;

public final class DateAdvice {
	
	private DateAdvice() {
	}

    @Advice.OnMethodExit
    public static void newDateExit( @Advice.This Date date) {
    	System.out.println("advice new Date!!!!!");
    }

}
