package yushanmufeng.vmck.agent.util;

import java.io.InputStream;
import java.nio.ByteBuffer;

import yushanmufeng.vmck.agent.AgentBootstrap;

public class ResourceUtil {

	public static byte[] readResource(String path){
		InputStream inputStream = null;
		int nbyte = 0, cap = 1024;
		ByteBuffer byteBuffer = ByteBuffer.allocate(cap);
		try {
			inputStream =  AgentBootstrap.class.getResourceAsStream(path) ;
			while( (nbyte = inputStream.read()) != -1 ) {
				if (!byteBuffer.hasRemaining()) {	// 缓冲区满，扩容
					cap = cap + cap/3;
					byteBuffer = ByteBuffer.allocate(cap).put( (ByteBuffer)byteBuffer.flip() );
				}
				byteBuffer.put((byte) nbyte);
			}
			
		} catch(Exception e) {
			e.printStackTrace();
		}finally{
			try {
				if( inputStream != null ) inputStream.close();
			} catch (Exception e) {}
		}
		byteBuffer.flip();
		return byteBuffer.array();
	}
	
}
