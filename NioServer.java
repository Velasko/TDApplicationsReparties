package ricm.channels.nio;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.nio.channels.spi.SelectorProvider;
import java.nio.charset.Charset;
import java.util.Iterator;

/**
 * NIO elementary server RICM4 TP F. Boyer
 */

public class NioServer {

	public static int DEFAULT_SERVER_PORT = 8888;

	// The channel used to accept connections from server-side
	private ServerSocketChannel ssc;
	private SelectionKey sscKey;

	// Unblocking selector
	private Selector selector;
	
	/**
	 * NIO server initialization
	 * 
	 * @param the host address and port of the server
	 * @throws IOException
	 */
	public NioServer(int port) throws IOException {

		// create a new selector
		selector = SelectorProvider.provider().openSelector();

		// create a new non-blocking server socket channel
		ssc = ServerSocketChannel.open();
		ssc.configureBlocking(false);

		// bind the server socket to the given address and port
		InetAddress hostAddress;
		hostAddress = InetAddress.getByName("localhost");
		InetSocketAddress isa = new InetSocketAddress(hostAddress, port);
		ssc.socket().bind(isa);

		// be notified when connection requests arrive
		sscKey = ssc.register(selector, SelectionKey.OP_ACCEPT);

	}

	/**
	 * NIO mainloop Wait for selected events on registered channels Selected events
	 * for a given channel may be ACCEPT, CONNECT, READ, WRITE Selected events for a
	 * given channel may change over time
	 */
	public void loop() throws IOException {
		System.out.println("NioServer running");
		while (true) {
			selector.select();

			Iterator<?> selectedKeys = this.selector.selectedKeys().iterator();

			while (selectedKeys.hasNext()) {

				SelectionKey key = (SelectionKey) selectedKeys.next();
				selectedKeys.remove();
				if (key.isValid() && key.isAcceptable())
					handleAccept(key);
				if (key.isValid() && key.isReadable())
					handleRead(key);
				if (key.isValid() && key.isWritable())
					handleWrite(key);
				if (key.isValid() && key.isConnectable())
					handleConnect(key);
			}
		}
	}

	/**
	 * Accept a connection and make it non-blocking
	 * 
	 * @param the key of the channel on which a connection is requested
	 */
	private void handleAccept(SelectionKey key) throws IOException {
		assert (this.sscKey == key);
		assert (ssc == key.channel());
		SocketChannel sc;

		// do the actual accept on the server-socket channel
		sc = ssc.accept();
		sc.configureBlocking(false);

		// register the read interest for the new socket channel
		// in order to know when there are bytes to read
		SelectionKey socketKey = sc.register(this.selector, SelectionKey.OP_READ);
		socketKey.attach(new ChannelIO(sc));
	}

	/**
	 * Handle a connect, this should never happen
	 * 
	 * @param the key of the channel on which a connection is requested
	 * @throws Error since this should never happen
	 */
	private void handleConnect(SelectionKey key) throws IOException {
		throw new Error("Unexpected connect");
	}

	/**
	 * Handle incoming data event
	 * 
	 * @param the key of the channel on which the incoming data waits to be received
	 */
	private void handleRead(SelectionKey key) throws IOException {
		assert (sscKey != key);
		assert (ssc != key.channel());
		
		Reader rd = ((ChannelIO) key.attachment()).reader();
		if(rd != null) {
			byte[] data = rd.handleRead();

			if(data != null) { //a msg recieved
				String msg = new String(data);
				send(rd.sc, msg.getBytes(Charset.forName("UTF-8")));
			}
		}
	}


	/**
	 * Handle outgoing data event
	 * 
	 * @param the key of the channel on which data can be sent
	 */
	private void handleWrite(SelectionKey key) throws IOException {
		assert (sscKey != key);
		assert (ssc != key.channel());

		// get the socket channel for the client to whom we
		// need to send something

		Writer wr = ((ChannelIO) key.attachment()).writer();
		if(wr.handleWrite()){
			key.interestOps(SelectionKey.OP_READ);
		}

	}
		
	/**
	 * Send data
	 * 
	 * @param the key of the channel on which data that should be sent
	 * @param the data that should be sent
	 */
	public void send(SocketChannel sc, byte[] data) {
		// register a write interest for the given client socket channel
		SelectionKey key = sc.keyFor(selector);
		key.interestOps(SelectionKey.OP_WRITE);
		
		((ChannelIO) key.attachment()).writer().write(data);
	}

	public static void main(String args[]) throws IOException {
		
		int serverPort = DEFAULT_SERVER_PORT;
		String arg;

		for (int i = 0; i < args.length; i++) {
			arg = args[i];
			if (arg.equals("-p")) {
				serverPort = new Integer(args[++i]).intValue();
			}
		}
		NioServer ns;
		ns = new NioServer(serverPort);
		ns.loop();
	}

}
