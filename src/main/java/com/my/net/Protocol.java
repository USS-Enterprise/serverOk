package com.my.net;

import java.io.IOException;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;

public interface Protocol {

	void handleKey(SelectionKey key, Selector selector) throws IOException;

}