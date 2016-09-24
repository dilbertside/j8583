/**
 * TestPojoIsoMessage
 */
package com.solab.iso8583.pojo;

import static org.junit.Assert.*;

import java.io.UnsupportedEncodingException;
import java.nio.charset.StandardCharsets;
import java.text.ParseException;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.solab.iso8583.IsoMessage;
import com.solab.iso8583.IsoType;
import com.solab.iso8583.MessageFactory;
import com.solab.iso8583.impl.SimpleTraceGenerator;
import com.solab.iso8583.parse.ConfigParser;

/**
 * @author dilbertside
 *
 */
@SuppressWarnings("deprecation")
public class TestPojoIsoMessage {

	protected final Logger log = LoggerFactory.getLogger(getClass());
	private MessageFactory<IsoMessage> mf;
	
	NetworkMgmtRequest request;
	Date dateTransaction = new GregorianCalendar(2000, 1, 1, 1 ,1, 1).getTime();
	Number systemTraceAuditNumber = 999999;
	Number codeInformationNetwork = 1;
	/**
	 * @throws java.lang.Exception
	 */
	@Before
	public void setUp() throws Exception {
		mf = ConfigParser.createFromPojo(NetworkMgmtRequest.class);
		mf.setCharacterEncoding(StandardCharsets.UTF_8.name());
		mf.setUseBinaryMessages(false);
		mf.setAssignDate(true);
		request = new NetworkMgmtRequest(dateTransaction, systemTraceAuditNumber, codeInformationNetwork);
	}

	/**
	 * Test method for {@link com.solab.iso8583.MessageFactory#newMessage(Object)}.
	 */
	@Test
	public void testNewMessage() {
		IsoMessage iso = mf.newMessage(request);
		Assert.assertEquals(0x800, iso.getType());
        Assert.assertTrue(iso.hasEveryField(7, 11, 70));
        Assert.assertEquals(IsoType.DATE10, iso.getField(7).getType());
        Assert.assertEquals(IsoType.NUMERIC, iso.getField(11).getType());
        Assert.assertEquals(IsoType.NUMERIC, iso.getField(70).getType());
        Assert.assertEquals(systemTraceAuditNumber, iso.getObjectValue(11));
        Assert.assertEquals(codeInformationNetwork, iso.getObjectValue(70));
		
        log.debug("testNewMessage\n {}", iso.debugString());
	}
	
	/**
	 * Test method for {@link com.solab.iso8583.MessageFactory#newMessage(Object)}.
	 */
	@Test
	public void testNewMessageAutoStan() {
		mf.setTraceNumberGenerator(new SimpleTraceGenerator(20));
		request.setSystemTraceAuditNumber(null);
		IsoMessage iso = mf.newMessage(request);
		Assert.assertEquals(0x800, iso.getType());
        Assert.assertTrue(iso.hasEveryField(7, 11, 70));
        Assert.assertEquals(IsoType.DATE10, iso.getField(7).getType());
        Assert.assertEquals(IsoType.NUMERIC, iso.getField(11).getType());
        Assert.assertEquals(IsoType.NUMERIC, iso.getField(70).getType());
        
        Assert.assertEquals(20, iso.getObjectValue(11));
        Assert.assertEquals(codeInformationNetwork, iso.getObjectValue(70));
        Date dt = iso.getObjectValue(7);
        Assert.assertEquals(dateTransaction.getMonth(), dt.getMonth());
        Assert.assertEquals(dateTransaction.getDay(), dt.getDay());
        Assert.assertEquals(dateTransaction.getHours(), dt.getHours());
        Assert.assertEquals(dateTransaction.getMinutes(), dt.getMinutes());
        Assert.assertEquals(dateTransaction.getSeconds(), dt.getSeconds());
        
        
        log.debug("testNewMessageAutoStan\n {}", iso.debugString());
        
        mf.setTraceNumberGenerator(null);
	}
	
	/**
	 * Test method for {@link com.solab.iso8583.MessageFactory#newMessage(Object)}.
	 * @throws ParseException 
	 * @throws UnsupportedEncodingException 
	 */
	@Test
	public void testRoundTrip() throws UnsupportedEncodingException, ParseException  {
		//encode
		IsoMessage iso = mf.newMessage(request);
		Assert.assertEquals(0x800, iso.getType());
        Assert.assertTrue(iso.hasEveryField(7, 11, 70));
        Assert.assertEquals(IsoType.DATE10, iso.getField(7).getType());
        Assert.assertEquals(IsoType.NUMERIC, iso.getField(11).getType());
        Assert.assertEquals(IsoType.NUMERIC, iso.getField(70).getType());
        Assert.assertEquals(systemTraceAuditNumber, iso.getObjectValue(11));
        Assert.assertEquals(codeInformationNetwork, iso.getObjectValue(70));
        Date dt = iso.getObjectValue(7);
        Assert.assertEquals(dateTransaction.getMonth(), dt.getMonth());
        Assert.assertEquals(dateTransaction.getDay(), dt.getDay());
        Assert.assertEquals(dateTransaction.getHours(), dt.getHours());
        Assert.assertEquals(dateTransaction.getMinutes(), dt.getMinutes());
        Assert.assertEquals(dateTransaction.getSeconds(), dt.getSeconds());
        
        String isoStr = iso.debugString();
        log.debug("testRoundTrip\n {}", isoStr);
        
        //decode
        IsoMessage m = mf.parseMessage(isoStr.getBytes(), 0);
		NetworkMgmtRequest nmr = mf.parseMessage(m, NetworkMgmtRequest.class);
		Assert.assertEquals(request.getCodeInformationNetwork(), nmr.getCodeInformationNetwork());
        Assert.assertEquals(request.getDateTransaction().getMonth(), nmr.getDateTransaction().getMonth());
        //IsoType.DATE10 is year insensitive, we need to set the year otherwise week day number will be wrong
        nmr.getDateTransaction().setYear(request.getDateTransaction().getYear());
        Assert.assertEquals(request.getDateTransaction().getDay(), nmr.getDateTransaction().getDay());
        Assert.assertEquals(request.getDateTransaction().getHours(), nmr.getDateTransaction().getHours());
        Assert.assertEquals(request.getDateTransaction().getMinutes(), nmr.getDateTransaction().getMinutes());
        Assert.assertEquals(request.getDateTransaction().getSeconds(), nmr.getDateTransaction().getSeconds());
		Assert.assertEquals(request.getSystemTraceAuditNumber(), nmr.getSystemTraceAuditNumber());
	}

}
