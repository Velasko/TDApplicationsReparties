package ricm.channels.nio;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.nio.channels.spi.SelectorProvider;
import java.util.Iterator;

import ricm.channels.IBroker;
import ricm.channels.IBrokerListener;
import ricm.channels.IChannel;
import ricm.channels.IChannelListener;

public class Broker extends Thread implements IBroker{
	IBrokerListener listener;
	private ServerSocketChannel ssc;
	private SelectionKey sscKey;


	private Selector selector;


	public Broker() throws IOException {
		// create a new selector
		selector = SelectorProvider.provider().openSelector();

		// create a new non-blocking server socket channel
		ssc = ServerSocketChannel.open();
		ssc.configureBlocking(false);
	}

	@Override
	public void setListener(IBrokerListener l) {
		listener = l;
	}

	@Override
	public boolean connect(String host, int port) {
		try {
			Channel chan = new Channel(host, port);
			listener.connected(chan);
			chan.setDaemon(true);
			chan.start();
		} catch (IOException e) {
			listener.refused(host, port);
		} catch (Exception e) {
			return false;
		}
		return true;
	}

	@Override
	public boolean accept(int port) {
		// bind the server socket to the given address and port
		InetAddress hostAddress;
		try {
			hostAddress = InetAddress.getByName("localhost");
			InetSocketAddress isa = new InetSocketAddress(hostAddress, port);
			ssc.socket().bind(isa);
			
			// be notified when connection requests arrive
			sscKey = ssc.register(selector, SelectionKey.OP_ACCEPT);
			return true;
		} catch (IOException e) {
			return false;
		}		
	}
	
	
	public void run() {
		try {
			loop();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	
	public void loop() throws IOException {
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

	private void handleConnect(SelectionKey key) {
		throw new Error("Unexpected connect");
	}

	private void handleWrite(SelectionKey key) throws IOException {
		assert (sscKey != key);
		assert (ssc != key.channel());
		
		Channel ch = (Channel) key.attachment();
		
		if(ch.handleWriteServer())
			key.interestOps(SelectionKey.OP_READ);
		
	}

	private void handleRead(SelectionKey key) throws IOException {
		assert (sscKey != key);
		assert (ssc != key.channel());
		
		Channel ch = (Channel) key.attachment();
		ch.handleReadServer();

	}

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
		
		Channel ch = new Channel(sc);
		socketKey.attach(ch);
		
		if(listener != null)
			listener.accepted(ch);
//		ch.setDaemon(true);
//		ch.start();
	}

}
