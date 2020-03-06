package ricm.channels.nio;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.util.PriorityQueue;
import java.util.Queue;

public class Writer {
	Queue<byte[]> msgs;
	SocketChannel sc;
	ByteBuffer bufferMsg;
	ByteBuffer bufferLen = ByteBuffer.allocate(4);
	byte[] msg;
	int offset, size;
	
	String state = "IDLE";

	public Writer(SocketChannel sc) {
		this.sc = sc;
		msgs = new PriorityQueue<byte[]>();
	}

	public void write(byte[] data) {
		msgs.add(data);
		state = "IDLE";
	}
	
	public boolean handleWrite() throws IOException {
		switch(state) {
		case "IDLE":

			msg = msgs.poll();
			if(msg == null) {
				return true;
			}
			
			size = msg.length;
			bufferLen.rewind();
			bufferLen.putInt(size);
			bufferLen.rewind();
			state = "Send Size";
			break;

		case "Send Size":
			int res = sc.write(bufferLen);
			if(res == -1) {
				return true;
			}
			if(!bufferLen.hasRemaining()) {
				bufferMsg =  ByteBuffer.wrap(msg);
				state = "Send Message";
			}
			break;

		case "Send Message":
			int res1 = sc.write(bufferMsg);
			if(res1 == -1) {
				return true;
			}
			
			if(!bufferMsg.hasRemaining()) {	
					 state = "IDLE";
			}
			break;
		}
		return false;
	}
}
