package ricm.channels.nio;

import java.io.IOException;
import java.io.PrintStream;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.nio.channels.spi.SelectorProvider;
import java.util.Iterator;

import ricm.channels.IChannel;
import ricm.channels.IChannelListener;
import ricm.channels.nio.Channel;
import ricm.channels.nio.Reader;

/**
 * NIO elementary client RICM4 TP F. Boyer
 */

public class Channel extends Thread implements IChannel{

	// The channel used to communicate with the server
	private SocketChannel sc;
	private SelectionKey scKey;

	//writer and reader
	private Writer wr = new Writer(sc);
	private Reader rd = new Reader(sc);

	// Java NIO selector
	private Selector selector;

	IChannelListener listener;

	/**
	 * NIO client initialization
	 * 
	 * @param serverName: the server name
	 * @param port: the server port
	 * @param msg: the message to send to the server
	 * @throws IOException
	 */
	public Channel(String serverName, int port) throws IOException {
		// create a new selector
		selector = SelectorProvider.provider().openSelector();

		// create a new non-blocking server socket channel
		sc = SocketChannel.open();
		sc.configureBlocking(false);

		// register an connect interested in order to get a
		// connect event, when the connection will be established
		scKey = sc.register(selector, SelectionKey.OP_CONNECT);

		// request a connection to the given server and port
		InetAddress addr;
		addr = InetAddress.getByName(serverName);
		sc.connect(new InetSocketAddress(addr, port));
	}

	public Channel(SocketChannel sc) throws ClosedChannelException {
		this.sc = sc;
		try {
			selector = SelectorProvider.provider().openSelector();
			scKey = sc.register(selector, SelectionKey.OP_READ);
		} catch (ClosedChannelException e) {
			if(closed()) {
				listener.closed(this, e);
			}else {
				throw e;
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public void run() {
		try {
			loop();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	/**
	 * The client forever-loop on the NIO selector - wait for events on registered
	 * channels - possible events are ACCEPT, CONNECT, READ, WRITE
	 */
	public void loop() throws IOException {
		while (true) {
			selector.select();

			// get the keys for which an event occurred
			Iterator<?> selectedKeys = this.selector.selectedKeys().iterator();
			while (selectedKeys.hasNext()) {
				SelectionKey key = (SelectionKey) selectedKeys.next();
				// process key's events
				if (key.isValid() && key.isAcceptable())
					handleAccept(key);
				if (key.isValid() && key.isReadable())
					handleRead(key);
				if (key.isValid() && key.isWritable())
					handleWrite(key);
				if (key.isValid() && key.isConnectable())
					handleConnect(key);
				// remove the key from the selected-key set
				selectedKeys.remove();
			}
		}
	}

	/**
	 * Accept a connection and make it non-blocking
	 * 
	 * @param the key of the channel on which a connection is requested
	 */
	private void handleAccept(SelectionKey key) throws IOException {
		throw new Error("Unexpected accept");
	}

	/**
	 * Finish to establish a connection
	 * 
	 * @param the key of the channel on which a connection is requested
	 * @throws IOException 
	 */
	private void handleConnect(SelectionKey key) throws IOException  {
		assert (this.scKey == key);
		assert (sc == key.channel());
		try {
			sc.finishConnect();
			key.interestOps(SelectionKey.OP_READ);

		} catch (IOException e) {
			if(closed()) {
				listener.closed(this, e);
			}else {
				throw e;
			}
		}
	}

	/**
	 * Handle incoming data event
	 * 
	 * @param the key of the channel on which the incoming data waits to be received
	 * @throws IOException 
	 */
	private void handleRead(SelectionKey key) throws IOException {
		assert (this.scKey == key);
		assert (sc == key.channel());

		byte[] data;

		try {
			data = rd.handleRead();

			if(data != null) { //a msg recieved
				if(listener != null)
					listener.received(this, data);
			}

		} catch (IOException e) {
			if(closed()) {
				listener.closed(this, e);
			}else {
				throw e;
			}
		}
	}

	public void handleReadServer() throws IOException {
		if(rd != null) {
			byte[] data;
			try {
				data = rd.handleRead();

				if(data != null) { //a msg recieved
					listener.received(this, data);
				}

			} catch (IOException e) {
				if(closed()) {
					listener.closed(this, e);
				}else {
					throw e;
				}
			}
		}
	}

	/**
	 * Handle outgoing data event
	 * 
	 * @param the key of the channel on which data can be sent
	 * @throws IOException 
	 */
	private void handleWrite(SelectionKey key) throws IOException {
		assert (this.scKey == key);
		assert (sc == key.channel());

		try {
			if(wr.handleWrite()){
				key.interestOps(SelectionKey.OP_READ);
			}
		} catch (IOException e) {
			if(closed()) {
				listener.closed(this, e);
			}else {
				throw e;
			}
		}

	}

	public boolean handleWriteServer() throws IOException {
		try {
			return wr.handleWrite();
		} catch (IOException e) {
			if(closed()) {
				listener.closed(this, e);
			}else {
				throw e;
			}
		}
		return false;
	}

	@Override
	public void send(byte[] data) {
		wr.write(data);

		// register a write interests to know when there is room to write
		// in the socket channel.
		SelectionKey key = sc.keyFor(selector);
		key.interestOps(SelectionKey.OP_WRITE);
	}


	/**
	 * Send the given data
	 * 
	 * @param data: the byte array that should be sent
	 */
	public void send(byte[] data, int offset, int count) {
		System.out.println("channel's unimplemented send method called");
	}

	public static void echo(PrintStream ps, byte[] digest) {
		//		for (int i = 0; i < digest.length; i++)
		//			ps.print(digest[i] + ", ");
		ps.println(digest.length);
	}


	@Override
	public void setListener(IChannelListener l) {
		listener = l;
	}

	@Override
	public void close() {
		try {
			sc.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	@Override
	public boolean closed() {
		// TODO Auto-generated method stub
		return !sc.isOpen();
	}

	public Writer writer() {
		return wr;
	}

	public Reader reader() {
		return rd;
	}

}
