package ricm.channels.nio;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;

public class Reader {
	SocketChannel sc;
	ByteBuffer bufferMsg;
	byte[] msg;
	int offset, size;

	String state = "Read int";
	ByteBuffer bufferLen = ByteBuffer.allocate(4);

	public Reader(SocketChannel sc) {
		this.sc = sc;
	}

	public byte[] handleRead() throws IOException {
		switch(state) {
//		case "IDLE":
//			break;
		case "Read int":
			int res = sc.read(bufferLen);
			if (res == -1)
				throw new IOException();
			if(bufferLen.remaining()==0) {
				bufferLen.rewind();
				int len = bufferLen.getInt();
				bufferLen.rewind();
				
				bufferMsg =  ByteBuffer.allocate(len);
				state = "Read Message";
			}
			break;
		case "Read Message":
			int res1 = sc.read(bufferMsg);
			if (res1 == -1) 
				throw new IOException();
			if(!bufferMsg.hasRemaining()) {
				msg = new byte[bufferMsg.limit()];
				bufferMsg.rewind();
				bufferMsg.get(msg);
				state = "Read int";
				return msg;
			}
			break;
		}
		return null;
	}
}
