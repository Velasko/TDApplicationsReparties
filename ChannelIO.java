package ricm.channels.nio;

import java.nio.channels.SocketChannel;

public class ChannelIO {
	private Reader rd;
	private Writer wr;
	
	ChannelIO(Reader reader, Writer writer) {
		rd = reader;
		wr = writer;
	}
	
	ChannelIO(SocketChannel sc){
		wr = new Writer(sc);
		rd = new Reader(sc);
	}
	
	public Writer writer() {
		return wr;
	}
	
	public Reader reader() {
		return rd;
	}

}
