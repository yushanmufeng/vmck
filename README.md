# VMCK
基于Javaagent与JVM运行时编辑字节码技术，通过对JDK时间与线程相关函数代理，实现非侵入式控制运行时系统时间。使用此JAVA代理程序，可在不修改操作系统时间的情况下，调整JAVA程序运行时获取到的当前时间; 以及加速程序的时间，触发定时任务。

[![standard-readme compliant](https://img.shields.io/badge/Download-0.0.1-blue.svg?style=flat-square)](http://www.fanguang.fun/vmck-agent-0.0.1.jar)
[![standard-readme compliant](https://img.shields.io/badge/ByteBuddy-1.10.14-brightgreen.svg?style=flat-square)](https://github.com/raphw/byte-buddy)

## 适用场景

此项目比较适合在Java项目的开发与测试阶段使用，可以方便的部署在开发环境与测试环境，无需修改原有的项目代码。尤其适合需要频繁修改时间测试功能、希望任何情况（如停止程序或修改系统时间）都能保证定时任务执行逻辑与顺序的项目。

## 如何使用

首先创建一个简单的main方法，每隔1秒会打印一次当前系统时间。代码如下：
```
public static void main(String[] args) {
	new Thread(() -> {
		while (true) {
			String timeStr = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss")
					.format(new Date(System.currentTimeMillis()));
			System.out.println("new date() : " + timeStr);
			try {
				Thread.sleep(1000);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
	}).start();
}
```

从
[![standard-readme compliant](https://img.shields.io/badge/Download-0.0.1-blue.svg?style=flat-square)](http://www.fanguang.fun/vmck-agent-0.0.1.jar)
下载vmck-agent-0.0.1.jar, 在JVM启动参数中，增加：
```
-javaagent:yourpath/vmck-agent-0.0.1.jar -Dvmck.name=test1
```
运行程序。如果此时如果没有看到报错日志，并且程序每隔一秒钟打印了一次当前时间，表示程序已经启动成功了。

然后停掉程序，等待一分钟，再重新启动程序，就会发现神奇的情况：此时日志打印的时间与系统的时间不一致，慢了1分钟。这是因为使用了VMCK代理的Java程序，时间将不会按照系统时间流逝，程序停止后，时间流逝也会随之停止。

那么该如何调整时间？ VMCK代理程序在启动时，同时会开启一个http服务，可通过http接口或其自带的管理页面对其操作。访问页面： http://127.0.0.1:6480

![adminpage](http://www.fanguang.fun/vmckpic.jpg)

在第一个输入框，输入1，然后点击“PASS MINUTES”按钮。此时观察后台日志，程序会迅速打印60次不同的当前时间，也就是时间加速了60秒，然后恢复为正常的1秒1次日志输出。