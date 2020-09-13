package yushanmufeng.vmck.agent;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.HashMap;

import yushanmufeng.vmck.agent.driverimpl.VmckDriverImpl;

public class Config {
	
	/** vmck工作根目录 */
	public static String VMCK_DIR;
	
	/** 分隔符 */
	public static String SEPARATOR;

	/** 工程名称, 将会在用户目录创建此文件夹作为工作空间 */
	public static String name;
	
	/** 端口号, 默认6480 */
	public static int port;
	
	/** 参数持久化的文件名 */
	public static final String PARAMS_FILE_NAME = "params.obj";
	
	/** 参数持久化的文件全路径 */
	public static String PARAMS_FILE_PATH;
	
	/** 通过文件保存的参数 */
	public static PersistParams params;
	
	public static void init() {
		// 初始化vmck工作根目录
		String USER_HOME = System.getProperties().getProperty("user.home");
		SEPARATOR = System.getProperties().getProperty("file.separator");
		VMCK_DIR = USER_HOME + SEPARATOR + "vmck";
		File file = new File(VMCK_DIR);
		if(!file.exists()) {
			file.mkdir();
		}
		// 初始化当前项目的工作空间
		name = System.getProperty("vmck.name");
		String CUR_WORKSPACE = VMCK_DIR + SEPARATOR + name;
		file = new File(CUR_WORKSPACE);
		if(!file.exists()) {
			file.mkdir();
		}
		// 初始化持久化参数文件
		PARAMS_FILE_PATH = CUR_WORKSPACE + SEPARATOR + PARAMS_FILE_NAME;
		readParams();
		try {
			port = Integer.parseInt(System.getProperty("vmck.port"));
		}catch(Exception e) {
			port = 6480;
		}
		
		
	}
	
	/**
	 * 从文件中读取params
	 */
	private static void readParams() {
		File file = new File(PARAMS_FILE_PATH);
		if(!file.exists()) {
			params = new PersistParams();
			params.setStandardTime(System.currentTimeMillis());
		} else {
			FileInputStream fileIn = null;
			ObjectInputStream in = null;
			try {
				fileIn = new FileInputStream(PARAMS_FILE_PATH);
				in = new ObjectInputStream(fileIn);
				params = (PersistParams) in.readObject();
				in.close();
				fileIn.close();
			} catch (Exception e) {
				e.printStackTrace();
			} finally {
				try {
					if (in != null) in.close();
					if (fileIn != null) fileIn.close();
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		}
	}
	
	/**
	 * 保存params到文件
	 */
	public static void saveParams() {
		params.setStandardTime(System.currentTimeMillis());
		FileOutputStream fileOut = null;
		ObjectOutputStream out = null;
		try {
			fileOut = new FileOutputStream(PARAMS_FILE_PATH);
			out = new ObjectOutputStream(fileOut);
			out.writeObject(params);
		} catch (IOException i) {
			i.printStackTrace();
	      }finally {
			try {
				if(out != null) out.close();
				if(fileOut != null) fileOut.close();
			}catch(Exception e) {}
		}
	}
	
	/**
	 * 可持久化map
	 */
	public static class PersistParams extends HashMap<String, String>{
		private static final long serialVersionUID = 1L;
		/** 虚拟基准时间，单位毫秒 */
		private static final String STANDARD_TIME = "standardTime";
		public long getStandardTime() {
			if(containsKey(STANDARD_TIME)) {
				return Long.parseLong(get(STANDARD_TIME));
			}else {
				return 0;
			}
		}
		public void setStandardTime(long standardTime) {
			put(STANDARD_TIME, String.valueOf(standardTime));
		}
	}
	
}
