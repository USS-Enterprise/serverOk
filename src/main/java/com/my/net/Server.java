package com.my.net;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.StandardSocketOptions;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.util.Iterator;

import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.configuration.XMLConfiguration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Main class for nonblocking tcp server
 * 
 * @author mike
 * 
 */
public class Server {

	private static Logger log = LoggerFactory.getLogger("SERVER");

	private static final boolean REUSE_ADDRESS = true;
	private static final int BUFFER_SIZE = 256 * 1024;

	private String host;
	private int port;
	private ServerSocketChannel serverSocketChannel;
	private Selector selector;
	private Protocol protocol = new Echo();

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		Server server = new Server();
		if (server.setUp()) {
			log.info("Server set up Ok.");
		}
		try {
			if (server.start()) {
				log.info("Server started");
				server.serveClients();
			}
		} catch (IOException e) {
			log.error("Could not start up server", e);
		}
	}

	private void serveClients() throws IOException {
		while (true) {
			// wait for incomming events
			selector.select();
			// there is something to process on selected keys
			Iterator<SelectionKey> keys = selector.selectedKeys().iterator();
			while (keys.hasNext()) {
				SelectionKey key = (SelectionKey) keys.next();
				// prevent the same key from coming up again
				keys.remove();
				if (!key.isValid()) {
					continue;
				}
				protocol.handleKey(key, selector);
			}
		}
	}



	/**
	 * Get ready for serving clients
	 * 
	 * @return
	 * @throws IOException
	 */
	private boolean start() throws IOException {
		serverSocketChannel = ServerSocketChannel.open();
		if (!serverSocketChannel.isOpen()) {
			log.error("Server socket channel was not open");
			return false;
		}
		selector = Selector.open();
		if (!selector.isOpen()) {
			log.error("Selector was not open");
			return false;
		}
		// configure non-blocking mode
		serverSocketChannel.configureBlocking(false);
		// set some options
		serverSocketChannel.setOption(StandardSocketOptions.SO_RCVBUF,
				BUFFER_SIZE);
		serverSocketChannel.setOption(StandardSocketOptions.SO_REUSEADDR,
				REUSE_ADDRESS);
		// bind the server socket channel to socket
		serverSocketChannel.bind(new InetSocketAddress(host, port));
		// register the current channel with the given selector
		serverSocketChannel.register(selector, SelectionKey.OP_ACCEPT);
		return true;
	}

	/**
	 * Read configuration file and set up host and port for server socket
	 * 
	 * @return
	 */
	private boolean setUp() {
		XMLConfiguration conf = new XMLConfiguration();
		try {
			conf.load("conf.xml");
		} catch (ConfigurationException e) {
			log.error("Could not load configuration", e);
			return false;
		}
		host = conf.getString("host");
		port = conf.getInt("host[@port]");
		log.info("host: " + host);
		log.info("port: " + port);
		return true;
	}

}
