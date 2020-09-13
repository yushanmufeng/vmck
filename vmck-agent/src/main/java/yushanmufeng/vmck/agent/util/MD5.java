package yushanmufeng.vmck.agent.util;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class MD5 {
	public static String encrypByMd5(byte[] context) {
		StringBuffer buf = new StringBuffer("");
		try {
			MessageDigest md = MessageDigest.getInstance("MD5");
			md.update(context);// update处理
			byte[] encryContext = md.digest();// 调用该方法完成计算

			int i;
			for (int offset = 0; offset < encryContext.length; offset++) {// 做相应的转化（十六进制）
				i = encryContext[offset];
				if (i < 0)
					i += 256;
				if (i < 16)
					buf.append("0");
				buf.append(Integer.toHexString(i));
			}
		} catch (NoSuchAlgorithmException e) {
			e.printStackTrace();
		}
		return  buf.toString().substring(8, 24);
	}
}
