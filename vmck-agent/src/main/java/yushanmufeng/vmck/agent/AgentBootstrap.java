package yushanmufeng.vmck.agent;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
//import static net.bytebuddy.matcher.ElementMatchers.*;
import java.lang.instrument.Instrumentation;
import java.nio.ByteBuffer;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.jar.JarFile;
import net.bytebuddy.agent.builder.AgentBuilder;
import net.bytebuddy.agent.builder.AgentBuilder.Transformer;
import net.bytebuddy.asm.Advice;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.dynamic.DynamicType.Builder;
import net.bytebuddy.matcher.ElementMatchers;
import net.bytebuddy.matcher.StringMatcher;
import net.bytebuddy.utility.JavaModule;
import yushanmufeng.vmck.agent.admin.AdminService;
import yushanmufeng.vmck.agent.admin.MiniHttpServer;
import yushanmufeng.vmck.agent.admin.MiniHttpServer.HttpRequest;
import yushanmufeng.vmck.agent.admin.MiniHttpServer.HttpResponse;
import yushanmufeng.vmck.agent.admin.MiniHttpServer.MatchListener;
import yushanmufeng.vmck.agent.advice.DateAdvice;
import yushanmufeng.vmck.agent.advice.SystemAdvice;
import yushanmufeng.vmck.agent.advice.ThreadAdvice;
import yushanmufeng.vmck.agent.util.MD5;

/**
 * 代理启动类
 * @author yushanmufeng
 */
final public class AgentBootstrap {

	public static final String VMCK_DRIVER_FACADE = "yushanmufeng.vmck.core.VmckDriverFacade";
	public static final String VMCK_DRIVER_IMP_CLASS = "yushanmufeng.vmck.agent.driverimpl.VmckDriverImpl";
	public static String VMCK_CORE_JAR_NAME = "vmck-core";
	
	/**
	 * 
	 * @param agentArgs
	 * @param inst
	 * @throws Exception
	 */
	public static void premain(String agentArgs, Instrumentation inst) throws Exception {
		try {
			System.out.println("Vmck Is Starting...");
			// 初始化参数配置，工作目录等
			Config.init();
			// 由BootstrapClassLoader加载vmck-core.jar,通过其中声明的公开方法及接口，完成BootstrapClassLoader加载的类对AppClassLoader类的调用转换
			appendToBootstrapClassLoader(inst);
			// 注册驱动类具体实现
			Class.forName(VMCK_DRIVER_IMP_CLASS);
			// 使用byte-buddy对jdk系统类字节码修改，对时间、休眠相关的函数做监听处理
			rebaseSystemClass(inst);
			// 开启http监听
			initHttpControl();
			// 进程结束时，保存vmck状态
			Runtime.getRuntime().addShutdownHook(new Thread(new Runnable() {
				@Override
				public void run() {
					Config.saveParams();
				}
			}));
		} catch (Exception e) {
			System.out.println(e);
			e.printStackTrace();
		}
	}
	

	/**
	 * 由BootstrapClassLoader加载vmck-core.jar,通过其中声明的公开方法及接口，完成BootstrapClassLoader加载的类对AppClassLoader类的调用转换
	 */
	private static void appendToBootstrapClassLoader(Instrumentation inst) throws Exception{
		// 读到core.jar到内存，通过检查文件md5判断是否用户目录jar文件已存在
		InputStream inputStream = null;
		int nbyte = 0, cap = 1024;
		ByteBuffer byteBuffer = ByteBuffer.allocate(cap);
		try {
			inputStream =  AgentBootstrap.class.getResourceAsStream("/" + VMCK_CORE_JAR_NAME + ".jar") ;
			while( (nbyte = inputStream.read()) != -1 ) {
				if (!byteBuffer.hasRemaining()) {	// 缓冲区满，扩容
					cap = cap + cap/3;
					byteBuffer = ByteBuffer.allocate(cap).put( (ByteBuffer)byteBuffer.flip() );
				}
				byteBuffer.put((byte) nbyte);
			}
			
		} finally{
			if( inputStream != null ) inputStream.close();
		}
		byteBuffer.flip();
		// 根据jar内容将md5拼接到文件名上。如果文件已存在，则无需重复解压
		byte[] jarBytes = byteBuffer.array();
		String md5 = MD5.encrypByMd5(jarBytes);
		String JAR_PATH = Config.VMCK_DIR + Config.SEPARATOR + VMCK_CORE_JAR_NAME + "-" + md5 + ".jar";
		File vmckCoreJar = null;
		vmckCoreJar = new File(JAR_PATH);
		if(!vmckCoreJar.exists()) {
			// 将jar解压至用户目录
			OutputStream outputStream = null;
			try {
				outputStream = new FileOutputStream(vmckCoreJar);
				outputStream.write(jarBytes, 0, jarBytes.length);
				
			} finally{
				if( outputStream != null ) outputStream.close();
			}
		}
		inst.appendToBootstrapClassLoaderSearch(new JarFile(JAR_PATH));
		System.out.println("VMCK Temp Path:" + JAR_PATH);
	}
	
	/**
	 * 使用byte-buddy对jdk系统类字节码修改，对时间、休眠相关的函数做监听处理
	 */
	private static void rebaseSystemClass(Instrumentation inst) {
		new AgentBuilder.Default()
    	.with(AgentBuilder.RedefinitionStrategy.RETRANSFORMATION)
        .with(AgentBuilder.InitializationStrategy.NoOp.INSTANCE)
        .with(AgentBuilder.TypeStrategy.Default.REBASE)
        .enableNativeMethodPrefix("$$vmck_")
        .ignore(
            new AgentBuilder.RawMatcher.ForElementMatchers(
                    ElementMatchers.nameStartsWith("net.bytebuddy.")
                            .or(ElementMatchers.isSynthetic())
                            .or(ElementMatchers.nameStartsWith("yushanmufeng")),
                    ElementMatchers.any(),
                    ElementMatchers.any())
        )
        .with(
                new AgentBuilder.Listener.Filtering(
                       new StringMatcher("java.lang.Object", StringMatcher.Mode.EQUALS_FULLY),
                        AgentBuilder.Listener.StreamWriting.toSystemOut()))
        .with(
                new AgentBuilder.Listener.Filtering(
                       new StringMatcher("java.lang.Thread", StringMatcher.Mode.EQUALS_FULLY),
                        AgentBuilder.Listener.StreamWriting.toSystemOut()))
        .with(
                new AgentBuilder.Listener.Filtering(
                       new StringMatcher("java.lang.System", StringMatcher.Mode.EQUALS_FULLY),
                        AgentBuilder.Listener.StreamWriting.toSystemOut()))
//        .type(ElementMatchers.is(Object.class))
//        .transform(new Transformer() {
//			@Override
//			public Builder<?> transform(Builder<?> builder, TypeDescription typeDescription, ClassLoader classLoader,JavaModule module) {
////              return builder.method(ElementMatchers.named("toString")).intercept(Advice.to(ObjectAdvice.class));
//              return builder.method(ElementMatchers.named("toString")).intercept(FixedValue.value("helloaaaaaaaaaaaaaaaaaaaaaa"));
//              // .and(ElementMatchers.takesArguments(long.class))
////              return builder.visit(Advice.to(ObjectAdvice.class).on(ElementMatchers.named("wait").and(ElementMatchers.takesArguments(long.class))));
//			}
//		})
        .type(ElementMatchers.is(System.class))
        .transform(new Transformer() {
			@Override
			public Builder<?> transform(Builder<?> builder, TypeDescription typeDescription, ClassLoader classLoader,JavaModule module) {
              return builder.method(ElementMatchers.named("currentTimeMillis")).intercept(Advice.to(SystemAdvice.class));
			}
		})
        .type(ElementMatchers.named("java.lang.Thread"))
        .transform(new Transformer() {
			@Override
			public Builder<?> transform(Builder<?> builder, TypeDescription typeDescription, ClassLoader classLoader,JavaModule module) {
              return builder.method(ElementMatchers.named("sleep").and(ElementMatchers.takesArguments(long.class))).intercept(Advice.to(ThreadAdvice.class));
			}
		})
        .type(ElementMatchers.named("java.util.Date"))
        .transform(new Transformer() {
			@Override
			public Builder<?> transform(Builder<?> builder, TypeDescription typeDescription, ClassLoader classLoader,JavaModule module) {
              return builder.method(ElementMatchers.named("Date").and(ElementMatchers.takesNoArguments())).intercept(Advice.to(DateAdvice.class));
			}
		})
        .installOn(inst);
	}
	
	/**
	 * 初始化http服务，用来操控及设置vmck系统
	 */
	private static void initHttpControl() {
		final AdminService adminService = new AdminService();
    	new MiniHttpServer()
    		.get("/admin/passtime", new MatchListener() {
				public void work(HttpRequest request, HttpResponse response) {
					int minute = Integer.parseInt( request.params.get("minute") );
					String startDate = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format( new Date(System.currentTimeMillis()) );
					String endDate = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format( new Date(System.currentTimeMillis() + minute * 60 * 1000) );
					adminService.passThroughTime(minute);
					response.res("Start Pass Time, " + startDate + " -> " + endDate);
				}
			})
    		.startBackstage(Config.port);
	}

}
