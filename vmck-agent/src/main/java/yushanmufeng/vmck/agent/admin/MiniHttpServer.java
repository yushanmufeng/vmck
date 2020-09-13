package yushanmufeng.vmck.agent.admin;

import java.io.*;
import java.util.*;
import java.util.concurrent.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.ByteBuffer;


/**
 * @author yushanmufeng
 */
public class MiniHttpServer{

	public String headCharset = "utf-8";
	// 匹配规则，http请求过来时会去遍历匹配，通过正则匹配则调用对应的监听器方法
	public List<Matcher> matchers = new ArrayList<Matcher>();
	
	private ServerSocket serverSocket = null;
	private ThreadPoolExecutor threadPoolExecutor = null;
	
	/** 启服,方法为同步方法，启动后会一直阻塞当前进程至服务器停止 */
	public void start(int port) throws IOException{
		try {
			serverSocket = new ServerSocket(port);
			System.out.println("Mini Http Server started at " + new Date() + ", listening " + port + " port...");

			if (threadPoolExecutor == null)  
				threadPoolExecutor = new ThreadPoolExecutor(4, 8, 180, TimeUnit.MILLISECONDS,new ArrayBlockingQueue<Runnable>(8), Executors.defaultThreadFactory(), new ThreadPoolExecutor.AbortPolicy());
			while (serverSocket != null) {
				try{
					Socket socket = serverSocket.accept();
					threadPoolExecutor.execute(new RequestHandler(socket));
				}catch(Exception e) {
					e.printStackTrace();
				}
			}
		} finally {
			if (serverSocket != null) {
				serverSocket.close();
			}
		}
	}
	
	/** 后台启动 */
	public void startBackstage(final int port) {
		new Thread(new Runnable() {
			public void run() {
				try {
					start(port);
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}).start();;
	}
	
	
	/** 停服 */
	public void stop() {
		if (serverSocket != null) {
			try {
				serverSocket.close();
				serverSocket = null;
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}
	
	/**
	 * http请求处理器，每次连接会新建一个handler去处理请求
	 */
	class RequestHandler implements Runnable{
		
		public Socket socket;

		public RequestHandler(Socket socket) {
			this.socket = socket;
		}

		@Override
		public void run(){
			try {
				HttpRequest req = new HttpRequest(socket.getInputStream());
				HttpResponse res = new HttpResponse(req, socket.getOutputStream());
				System.out.println(req.toString());
				for(Matcher matcher : matchers) {
					boolean matchedMethod = matcher.method.equals(MethodName.ALL) || req.method.equals(matcher.method.toString());
					if( matchedMethod && req.url.matches(matcher.regex) ) {
						req.matcher = matcher;
						matcher.listerner.work(req, res);
					}
					if( res.end ) 
						break;
				}
				Thread.sleep(5000);
			}catch(RuntimeException e) {
				System.out.println("WARN: " + e);
			} 
			catch (Exception e) {
				e.printStackTrace();
			} finally {
				try {
					if(socket != null) {
						socket.getOutputStream().close();
						socket.close();
					}
				} catch (Exception e) { 
					e.printStackTrace(); 
				}
			}
		}
	}
	
	/** 路由映射相关方法 */
	public MiniHttpServer all(String mapping, MatchListener matchListener) {
		addMatchersByRouter( "", newRouter().mapping(mapping).listener(matchListener) ); return this;
	}
	
	public MiniHttpServer all(String mapping, Router router) {
		addMatchersByRouter(mapping, router); return this;
	}
	
	public MiniHttpServer get(String mapping, MatchListener matchListener) {
		addMatchersByRouter( "", newRouter().mapping(mapping).listener(matchListener).method(MethodName.GET) ); return this;
	}
	
	public MiniHttpServer get(String mapping, Router router) {
		addMatchersByRouter( mapping, router.method(MethodName.GET) ); return this;
	}
	
	public MiniHttpServer post(String mapping, MatchListener matchListener) {
		addMatchersByRouter( "", newRouter().mapping(mapping).listener(matchListener).method(MethodName.POST) ); return this;
	}
	
	public MiniHttpServer post(String mapping, Router router) {
		addMatchersByRouter( mapping, router.method(MethodName.POST) ); return this;
	}
	
	/**
	 * 核心方法：添加路由映射。将路径匹配规则最终挂载在server上的方法
	 */
	private void addMatchersByRouter(String parentUrl, Router router) {
		if( parentUrl == null || router.mapping == null) {
			System.out.println("Add Mapping Error! Any Url Element Is Null : {" + parentUrl + "}/{" + router.mapping + "}");
			throw new RuntimeException("Add Mapping Error! Any Url Element Is Null : {" + parentUrl + "}/{" + router.mapping + "}") ;
		}
		if(parentUrl.length() > 0 && parentUrl.charAt(parentUrl.length() - 1) == '/') {
			parentUrl = parentUrl.substring(0, parentUrl.length() - 1);
		}
		String curUrl = parentUrl;
		if(router.mapping.length() == 0 || router.mapping.charAt(0) != '/') {
			router.mapping = "/" + router.mapping;
		}
		curUrl = parentUrl + router.mapping;
		// 先添加当前router.如果router没有设置逻辑处理监听器，表示只是用来帮助其他url设置映射，不添加
		if(router.listerner != null) {
			try {
				Matcher matcher = new Matcher();
				matcher.method = router.method;
				matcher.listerner = router.listerner;
				matcher.mapping = curUrl.indexOf("/") != 0 ? "/" + curUrl : curUrl;
				matcher.regex = genRegex(matcher.mapping);
				matchers.add(matcher);
				System.out.println("Added URL Matcher: " + matcher);
			}catch(Exception e) {
				e.printStackTrace();
			}
		}
		// 再递归添加子router
		if(router.subRouters != null && router.subRouters.size() > 0) {
			for(Router subRouter : router.subRouters) {
				addMatchersByRouter(curUrl, subRouter);
			}
		}
	}
	
	/**
	 * 根据mapping生成正则
	 */
	private String genRegex(String mapping) {
		if(mapping.indexOf(".*") != -1 ) {
			if(mapping.indexOf(".*") < mapping.length()-2) 
				throw new RuntimeException("Add URL Matcher Error! Check URL:" + mapping);
			mapping = mapping.replaceAll("\\.\\*", "\\\\.\\\\w+");
		}
		mapping = mapping.replaceAll("\\*", ".+");
		int leftIndex, rightIndex;
		while( (leftIndex = mapping.indexOf("{")) != -1) {
			if((rightIndex = mapping.indexOf("}")) != -1 && rightIndex > leftIndex) {
				// TODO 没有对 {abc} 的变量格式做检测，中间可能会有 / ，暂时先不处理
				mapping = mapping.substring(0, leftIndex) + "\\w+" + mapping.substring(rightIndex+1, mapping.length()); 
			}else {
				throw new RuntimeException("Add URL Matcher Error! Check URL:" + mapping);
			}
			mapping.indexOf("}");
		}
		return "^" + mapping + "$";
	}
	
	public Router newRouter() {
		return new Router().mapping("").method(MethodName.ALL).listener(null);
	}
	
	/**
	 * 监听器，url匹配成功时会执行
	 */
	public interface MatchListener{
		public void work(HttpRequest request, HttpResponse response);
	}
	
	/**
	 * 匹配规则
	 */
	class Matcher{
		// Http Method
		public MethodName method;
		// 匹配的路径、生成的正则
		public String mapping, regex;
		// 监听器
		public MatchListener listerner;
		@Override
		public String toString() {
			return method.toString() + " " + mapping + ", " + "Generate Regex: " + regex;
		}
	}
	
	/**
	 * 路由匹配规则，较matcher增加嵌套功能
	 */
	public class Router extends Matcher{
		
		List<Router> subRouters = new ArrayList<Router>(2);
		
		public Router all(String mapping, MatchListener matchListener) {
			add( newRouter().mapping(mapping).listener(matchListener) ); return this;
		}
		
		public Router all(String mapping, Router router) {
			add( newRouter().mapping(mapping).add(router) ); return this;
		}
		
		public Router get(String mapping, MatchListener matchListener) {
			add( newRouter().mapping(mapping).listener(matchListener).method(MethodName.GET) ); return this;
		}
		
		public Router get(String mapping, Router router) {
			add( newRouter().mapping(mapping).add(router).method(MethodName.GET) ); return this;
		}
		
		public Router post(String mapping, MatchListener matchListener) {
			add( newRouter().mapping(mapping).listener(matchListener).method(MethodName.POST) ); return this;
		}
		
		public Router post(String mapping, Router router) {
			add( newRouter().mapping(mapping).add(router).method(MethodName.POST) ); return this;
		}
		
		public Router method(MethodName method) {
			this.method = method; return this;
		}
		
		public Router mapping(String mapping) {
			this.mapping = mapping; return this;
		}
		
		public Router listener(MatchListener listerner) {
			this.listerner = listerner; return this;
		}
		
		public Router add(Router router) {
			this.subRouters.add(router); return this;
		}
	}
	
	public class HttpRequest{
		// 请求方法, 请求路径, 协议名称, 请求体
		public String method, url, portal, body; 
		public byte[] bodyBytes; 
		public Map<String, String> headers = new HashMap<String, String>(); // headers参数
		public Map<String, String> params; // 请求参数
		public volatile Matcher matcher;	// 匹配成功的映射

		public HttpRequest(InputStream in) throws IOException {
			String lineStr = readLine0(in, headCharset);
			String[] splitElements = lineStr.split(" ");
			if(splitElements.length < 3) throw new RuntimeException("Read Request Line Failed! Status Line:" + lineStr);
			// 请求行: 协议get/post
			this.method = splitElements[0];
			// 请求行: url
			String[] urlWithParams = java.net.URLDecoder.decode(splitElements[1], "utf-8").split("\\?");
			this.url = urlWithParams[0];
			params = urlWithParams.length > 1 ? dealParametersStr(urlWithParams[1]) : new HashMap<String, String>();
			// 请求行: 协议版本
			this.portal = splitElements[2];
			// 读取请求报头
			while ( !(lineStr = readLine0(in, headCharset)).trim().isEmpty() ) {
				splitElements = lineStr.split(":");
				this.headers.put(splitElements[0], splitElements[1].trim());
			}
			// 读取请求体
			int lenStr = Integer.parseInt(this.headers.containsKey("Content-Length") ? this.headers.get("Content-Length") : "0");
			if(lenStr > 0) {
				this.bodyBytes = readBytes0(in, lenStr);
				if (bodyBytes == null) throw new IOException("Accept Http Body Failed!");
				this.body = new String(bodyBytes, "utf-8");
			}
		}

		@Override
		public String toString() {
			return new StringBuilder().append(method).append(" ").append(url).append(" ").append(portal).toString();
		}

	}
	
	public class HttpResponse {
		private OutputStream out;
		public String charset = "utf-8";
		public boolean end = false;	// 处理终止标记，设置为true则不再执行后续操作
		public String portal = "HTTP/1.1", status = "200", statusDescribe = "OK";
		public Map<String, String> headers;

		public HttpResponse(HttpRequest request, OutputStream out) {
			this.out = out;
			this.headers = new HashMap<String, String>();
			headers.put("Content-Type", ContentType.TEXT.name + "; charset=" + charset);
			for(ContentType contentType : ContentType.values()) {
				if( request.url.matches(contentType.regex) ) {
					headers.put("Content-Type", contentType.name + "; charset=" + charset);
					break;
				}
			}
		}
		
		public HttpResponse resJsonData(String body) {
			return setHeader("Content-Type", ContentType.JSON.name + "; charset=" + charset).res(body);
		}
		
		public HttpResponse resHtml(String body) {
			return setHeader("Content-Type", ContentType.HTML.name + "; charset=" + charset).res(body);
		}

		public HttpResponse res(String body) {
			try {
				return res( body.getBytes(charset) );
			} catch (UnsupportedEncodingException e) {
				e.printStackTrace();
			}
			return null;
		}
		
		public HttpResponse res(byte[] b) {
			headers.put("Content-Length", "" + b.length);
			this.writeHeader();
			this.writeBytes(b);
			return this;
		}

		public HttpResponse setHeader(String key, String value) {
			this.headers.put(key, value); return this;
		}

		public HttpResponse writeHeader() {
			try {
				String tmp = portal + " " + status + " " + statusDescribe + "\r\n";
				this.writeBytes(tmp.getBytes());
				if (!headers.isEmpty()) {
					Iterator<Map.Entry<String, String>> entries = headers.entrySet().iterator();
					while (entries.hasNext()) {
						Map.Entry<String, String> entry = entries.next();
						String s = entry.getKey() + ": " + entry.getValue() + "\r\n";
						this.writeBytes(s.getBytes());
					}
				}
				this.out.write("\r\n".getBytes());
			} catch (IOException e) {
				e.printStackTrace();
			}
			return this;
		}

		public HttpResponse writeBytes(byte[] b) {
			writeBytes(b, 0, b.length); return this;
		}
		
		public HttpResponse writeBytes(byte[] b,int offset, int length) {
			try {
				this.out.write(b, offset, length);
				this.out.flush();
			} catch (IOException e) {
				e.printStackTrace();
			}
			return this;
		}
		
		public HttpResponse end() {
			this.end = true; return this;
		}
	}
	
	/**
	 * 关联处理静态文件目录的监听器，所设置的mapping必须为 "/abc/*" 或 "/abc/*.jpg" 这种格式，否则无法正确匹配静态文件
	 * @param rootPath 静态文件的根目录
	 */
	public static StaticFileListener staticFileListener(String rootPath) {
		StaticFileListener staticFileListener = new StaticFileListener();
		if(rootPath.length() > 0 && rootPath.charAt(rootPath.length() - 1) != '/')
			rootPath = rootPath + "/";
		staticFileListener.rootPath = rootPath;
		return staticFileListener;
	}
	
	/**
	 * 关联处理静态文件目录的监听器，所设置的mapping必须为 "/abc/*" 或 "/abc/*.jpg" 这种格式，否则无法正确匹配静态文件
	 * @param rootPath 静态文件的根目录
	 * @param alwaysContinue 处理成功后是否继续执行后续监听逻辑，true:总是继续执行, false:总是终止
	 * @return
	 */
	public static StaticFileListener staticFileListener(String rootPath, boolean alwaysContinue) {
		StaticFileListener staticFileListener =staticFileListener(rootPath);
		staticFileListener.endCondition = alwaysContinue ? 3 : 2;
		return staticFileListener;
	}
	
	/** 关联处理静态文件目录的监听器 */
	public static class StaticFileListener implements MatchListener{
		/** 静态文件的根目录, 忽略的目录 */
		private String rootPath;
		/** 处理终止条件, 1为发送成功结束, 2为成功失败都结束, 3为成功失败都不结束 */
		public int endCondition = 1;
		
		@Override
		public void work(HttpRequest request, HttpResponse response) {
			String url = request.url;
			String mapping = request.matcher.mapping;
			if( mapping.length() > 0 && mapping.indexOf("*") != -1 ) {
				mapping = mapping.substring( 0, mapping.indexOf("*") );
			}
			File file = new File( rootPath + url.substring(url.indexOf(mapping) + mapping.length()) );
			InputStream fin = null;
			boolean success = true;
			try {
				if( !file.exists() ) throw new RuntimeException("Not Found File: " + rootPath + request.url);
				fin = new FileInputStream(file);
				int len = 0;
				byte[] buf = new byte[1024];
				response.writeHeader();
				while ((len = fin.read(buf)) > 0)
					response.writeBytes(buf, 0, len);
			}catch(RuntimeException e) {
				success = false;
				System.out.println(e);
			}catch(Exception e) {
				success = false;
				e.printStackTrace();
			}finally {
				try {
					if(fin != null) {
						fin.close();
					}
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
			if( (endCondition==1&&success) || endCondition == 2)
				response.end();
		}
	}
	
	
	/**
	 * 工具方法，从流读取至换行符
	 */
	private String readLine0(InputStream in, String charset) throws IOException {
		int i, cap = 256;
		ByteBuffer buffer = ByteBuffer.allocate(cap);	// 缓冲区满了会扩容1/3
		while ( (i = in.read()) != -1 && i != '\n' && i != '\r' ) {
			if (!buffer.hasRemaining()) {	// 缓冲区满，扩容
				buffer = ByteBuffer.allocate(cap + cap/3).put((ByteBuffer)buffer.flip());
			}
			buffer.put((byte) i);
		}
		return new String(buffer.array(), charset);
	}
	
	/**
	 * 工具方法，从流读取指定长度
	 */
	private byte[] readBytes0(InputStream input, int len) throws IOException{
		byte[] buffer = new byte[len];
		len = 0;
		while (len < buffer.length) {
			int length = input.read(buffer, len, buffer.length - len);
			if (length == -1) return null;
			len += length;
		}
		return buffer;
	}
	
	/**
	 * 工具方法，字符串处理相关
	 */
	private Map<String, String> dealParametersStr(String str) {
		Map<String, String> parameters = new HashMap<String, String>();
		String[] parStrs = this.split$(str);
		for (String parStr : parStrs) {
			int i = parStr.indexOf('=');
			if (i == -1) continue;
			parameters.put(parStr.substring(0, i), parStr.substring(i + 1));
		}
		return parameters;
	}

	private String[] split$(String str) {
		List<String> list = new LinkedList<String>();
		while (!str.isEmpty()) {
			int i = this.find$(str);
			if (i == -1) break;
			list.add(str.substring(0, i));
			str = str.substring(i + 1);
		}
		if (!str.isEmpty()) list.add(str);
		String[] result = new String[list.size()];
		return list.toArray(result);
	}

	private int find$(String str) {
		boolean inString = false;
		for (int i = 0; i < str.length(); i++) {
			char ch = str.charAt(i);
			if (ch == '"') {
				if (inString) {
					if (str.charAt(i - 1) != '\\') inString = false;
				} else inString = true;
			}
			if (inString) continue;
			if (ch == '&') return i;
		}
		return -1;
	}
	
	/** HTTP METHOD NAME */
	public enum MethodName{
		ALL, POST, GET
	}
	
	public enum ContentType {
		TEXT("text/plain", "^\\s+$"),	// 无法匹配到任何url的正则，作为其他都匹配不上时的默认值 
		HTML("text/html", "^.*\\.(htm|html)$"), 
		CSS("text/css", "^.*\\.css$"), 
		JPEG("image/jpeg", "^.*\\.(jpeg|jpg)$"),
		GIF("image/gif", "^.*\\.gif$"), 
		PNG("image/png", "^.*\\.png$"), 
		JAVASCRIPT("application/javascript", "^.*\\.js$"), 
		JSON("application/json", "^.*\\.json$"),
		XML("application/xml", "^.*\\.xml$"), 
		PDF("application/pdf", "^.*\\.pdf$");

		// contentType, 文件名匹配正则
		public String name, regex;

		ContentType(String name, String regex) {
			this.name = name;
			this.regex = regex;
		}
		@Override
		public String toString() {
			return name;
		}
	}
	
}
