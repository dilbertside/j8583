/**
 * TestParsingPojo
 */
package com.solab.iso8583.pojo;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.nio.charset.StandardCharsets;
import java.text.ParseException;

import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.solab.iso8583.IsoMessage;
import com.solab.iso8583.MessageFactoryPojo;
import com.solab.iso8583.parse.ConfigParserPojo;

/**
 * @author dilbertside on Sep 24, 2016
 * @version 1.0
 * @since V1.12.0
 *
 */
public class TestPojoParsing {

	protected final Logger log = LoggerFactory.getLogger(getClass());
	private MessageFactoryPojo<IsoMessage> mf;

	@Before
	public void init() throws IOException {
		mf = ConfigParserPojo.createFromPojo(NetworkMgmtRequest.class);
		mf.setCharacterEncoding(StandardCharsets.UTF_8.name());
		mf.setUseBinaryMessages(false);
		mf.setAssignDate(true);
	}

	@Test(expected=ParseException.class)
	public void testEmpty() throws ParseException, UnsupportedEncodingException {
		mf.parseMessage(new byte[0], 0);
		IsoMessage m = mf.parseMessage("060002000000000000000125213456".getBytes(), 0);
	}
	
	/**
	 * Test method for {@link com.solab.iso8583.MessageFactory#parseMessage(com.solab.iso8583.IsoMessage, java.lang.Class)}.
	 */
	@Test
	public void testPojoParse() throws ParseException, UnsupportedEncodingException {
		String isoStr = "0800822000000000000004000000000000000922115010999999001";
		IsoMessage m = mf.parseMessage(isoStr.getBytes(), 0);
		NetworkMgmtRequest networkMgmtRequest = mf.parseMessage(m, NetworkMgmtRequest.class);
		
		log.debug("testPojoParse\n{}\n {}", isoStr, networkMgmtRequest.toString());
	}
}
