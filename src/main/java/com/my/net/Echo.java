package com.my.net;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Echo implements Protocol {

	private static final Logger log = LoggerFactory.getLogger(Echo.class);
	private static final int BUFFER_SIZE = 2 * 1024;

	private Map<SocketChannel, List<byte[]>> keepDataTrack = new HashMap<>();
	
	@Override
	public void handleKey(SelectionKey key, Selector selector)
			throws IOException {
		if (key.isAcceptable()) {
			acceptOP(key, selector);
		} else if (key.isReadable()) {
			this.readOP(key);
		} else if (key.isWritable()) {
			this.writeOP(key);
		}
	}

	private void acceptOP(SelectionKey key, Selector selector)
			throws IOException {
		ServerSocketChannel serverChannel = (ServerSocketChannel) key.channel();
		SocketChannel socketChannel = serverChannel.accept();
		socketChannel.configureBlocking(false);
		log.info("Incoming connection from: "
				+ socketChannel.getRemoteAddress());
		// write a welcome message
		socketChannel.write(ByteBuffer.wrap("Hello!\n".getBytes("UTF-8")));
		// register channel with selector for further I/O
		keepDataTrack.put(socketChannel, new ArrayList<byte[]>());
		socketChannel.register(selector, SelectionKey.OP_READ);
	}

	private void readOP(SelectionKey key) {
		ByteBuffer buffer = ByteBuffer.allocate(BUFFER_SIZE);
		try {
			SocketChannel socketChannel = (SocketChannel) key.channel();
			buffer.clear();
			int numRead = -1;
			try {
				numRead = socketChannel.read(buffer);
			} catch (IOException e) {
				log.error("Cannot read error!");
			}
			if (numRead == -1) {
				this.keepDataTrack.remove(socketChannel);
				log.info("Connection closed by: " + socketChannel.getRemoteAddress());
				socketChannel.close();
				key.cancel();
				return;
			}
			byte[] data = new byte[numRead];
			System.arraycopy(buffer.array(), 0, data, 0, numRead);
			System.out.println(new String(data, "UTF-8") + " from "
					+ socketChannel.getRemoteAddress());
			// write back to client
			doEchoJob(key, data);
		} catch (IOException ex) {
			System.err.println(ex);
		}
	}

	private void writeOP(SelectionKey key) throws IOException {
		SocketChannel socketChannel = (SocketChannel) key.channel();
		List<byte[]> channelData = keepDataTrack.get(socketChannel);
		Iterator<byte[]> its = channelData.iterator();
		while (its.hasNext()) {
			byte[] it = its.next();
			its.remove();
			socketChannel.write(ByteBuffer.wrap(it));
		}
		key.interestOps(SelectionKey.OP_READ);
	}



	private void doEchoJob(SelectionKey key, byte[] data) {
		SocketChannel socketChannel = (SocketChannel) key.channel();
		List<byte[]> channelData = keepDataTrack.get(socketChannel);
		channelData.add(data);
		key.interestOps(SelectionKey.OP_WRITE);
	}
}