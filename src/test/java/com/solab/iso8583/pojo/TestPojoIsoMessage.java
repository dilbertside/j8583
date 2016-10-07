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

import com.solab.iso8583.IsoField.CompositeFieldPojo;
import com.solab.iso8583.IsoMessage;
import com.solab.iso8583.IsoType;
import com.solab.iso8583.MessageFactory;
import com.solab.iso8583.annotation.Iso8583;
import com.solab.iso8583.annotation.Iso8583Field;
import com.solab.iso8583.impl.SimpleTraceGenerator;
import com.solab.iso8583.parse.ConfigParser;

/**
 * @author dilbertside
 *
 */
@SuppressWarnings({ "deprecation", "unused" })
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
		mf = ConfigParser.createFromPojo(NetworkMgmtRequest.class, AcquirerFinancialRequest.class);
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

	@Iso8583(type=0x200)
	public static class NotRegisteredPojo{
		@Iso8583Field(index=70, type=IsoType.NUMERIC, length=3)
		public Number codeInformationNetwork;
	}
	
	/**
	 * Test method for {@link com.solab.iso8583.MessageFactory#newMessage(Object)}.
	 */
	@Test(expected=IllegalArgumentException.class)
	public void testNewMessageNotRegistered(){
		mf.newMessage(new NotRegisteredPojo());
	}
	
	/**
	 * Test method for {@link com.solab.iso8583.MessageFactory#newMessage(Object)}.
	 */
	@Test
	public void testNewMessageAcquirerFinancialRequest() {
		AcquirerFinancialRequest request = new AcquirerFinancialRequest();
		request.setSystemTraceAuditNumber(systemTraceAuditNumber);
		//request.setReservedNational("hello world!");
		request.setAdditionalDataNational("AdditionalDataNational!");
		//make it!!
		IsoMessage iso = mf.newMessage(request);
		
		Assert.assertEquals(0x200, iso.getType());
	    Assert.assertTrue(iso.hasEveryField(3,7,11,28,32,35,43,47,48,49,60,61,100,102));
	    
	    Assert.assertEquals(IsoType.NUMERIC, iso.getField(3).getType());
	    Assert.assertEquals(request.getProcessingCode(), iso.getObjectValue(3));
	    
	    //IsoType.DATE10 is year insensitive
	    Assert.assertEquals(IsoType.DATE10, iso.getField(7).getType());
	    Date dt = (Date) iso.getField(7).getValue(); dt.setYear(2000);
	    Assert.assertEquals(request.getDateTransaction(), dt);
	    
	    Assert.assertEquals(IsoType.NUMERIC, iso.getField(11).getType());
	    Assert.assertEquals(systemTraceAuditNumber, iso.getObjectValue(11));
	    
	    Assert.assertEquals(IsoType.AMOUNT, iso.getField(28).getType());
	    Assert.assertEquals(request.getAmountTransactionFee(), iso.getObjectValue(28));
	    
	    Assert.assertEquals(IsoType.LLVAR, iso.getField(32).getType());
	    Assert.assertEquals(request.getAcquiringInstitutionIdentificationCode(), iso.getObjectValue(32));
	    
	    Assert.assertEquals(IsoType.LLVAR, iso.getField(35).getType());
	    Assert.assertEquals(request.getTrack2Data(), iso.getObjectValue(35));
	    
	    Assert.assertEquals(IsoType.ALPHA, iso.getField(43).getType());
	    Assert.assertEquals(request.getCardAcceptorName(), iso.getObjectValue(43));
	    
	    Assert.assertEquals(IsoType.LLLVAR, iso.getField(47).getType());
	    log.debug("nested pojo\n {}", iso.getObjectValue(47));
	    //Assert.assertTrue("must be of type CompositeFieldPojo", iso.getObjectValue(47) instanceof CompositeFieldPojo);
	    //CompositeFieldPojo cf = iso.getObjectValue(47);
	    //Assert.assertEquals(request.getAdditionalDataNational(), cf.getField(1).getValue());
	    Assert.assertEquals(request.getAdditionalDataNational(), iso.getObjectValue(47));
	    
	    //here we go the nested pojo
	    Assert.assertEquals(IsoType.LLLVAR, iso.getField(48).getType());
	    log.debug("nested pojo\n {}", iso.getObjectValue(48));
	    Assert.assertTrue("must be of type CompositeFieldPojo", iso.getObjectValue(48) instanceof CompositeFieldPojo);
	    CompositeFieldPojo cf1 = iso.getObjectValue(48);
	    Assert.assertEquals(request.getAdditionalData().getPrivateDataStr(), cf1.getField(1).getValue());
	    Assert.assertEquals(request.getAdditionalData().getPrivateDataNum(), cf1.getField(2).getValue());
	    
	    Assert.assertEquals(IsoType.ALPHA, iso.getField(49).getType());
	    Assert.assertEquals(request.getCurrencyCodeTransaction(), iso.getObjectValue(49));
	    
	    Assert.assertEquals(IsoType.LLLVAR, iso.getField(60).getType());
	    Assert.assertEquals(request.getReservedNational(), iso.getObjectValue(60));
	    
	    Assert.assertEquals(IsoType.LLLVAR, iso.getField(61).getType());
	    Assert.assertEquals(request.getReservedPrivate(), iso.getObjectValue(61));
	    
	    Assert.assertEquals(IsoType.LLVAR, iso.getField(100).getType());
	    Assert.assertEquals(request.getReceivingInstitutionIdentificationCode(), iso.getObjectValue(100));
	    
	    Assert.assertEquals(IsoType.LLVAR, iso.getField(102).getType());
	    Assert.assertEquals(request.getAccountIdentification1(), iso.getObjectValue(102));
	    
	    log.debug("testNewMessageAcquirerFinancialRequest\n {}", iso.debugString());
	}
	
	
	/**
	 * Test method for {@link com.solab.iso8583.MessageFactory#newMessage(Object)}.
	 * Test method for {@link com.solab.iso8583.MessageFactory#parseMessage(IsoMessage, Class)}.
	 * @throws ParseException 
	 * @throws UnsupportedEncodingException 
	 */
	@Test
	public void testNewMessageAcquirerFinancialRequestRoundTrip() throws UnsupportedEncodingException, ParseException {
		AcquirerFinancialRequest request = new AcquirerFinancialRequest();
		request.setSystemTraceAuditNumber(systemTraceAuditNumber);
		//request.setReservedNational("hello world!");
		request.setAdditionalDataNational("AdditionalDataNational!");
		//make it!!
		IsoMessage iso = mf.newMessage(request);
		
		Assert.assertEquals(0x200, iso.getType());
	    Assert.assertTrue(iso.hasEveryField(3,7,11,28,32,35,43,47,48,49,60,61,100,102));
	    
	    Assert.assertEquals(IsoType.NUMERIC, iso.getField(3).getType());
	    Assert.assertEquals(request.getProcessingCode(), iso.getObjectValue(3));
	    
	    //IsoType.DATE10 is year insensitive
	    Assert.assertEquals(IsoType.DATE10, iso.getField(7).getType());
	    Date dt = (Date) iso.getField(7).getValue(); dt.setYear(2000);
	    Assert.assertEquals(request.getDateTransaction(), dt);
	    
	    Assert.assertEquals(IsoType.NUMERIC, iso.getField(11).getType());
	    Assert.assertEquals(systemTraceAuditNumber, iso.getObjectValue(11));
	    
	    Assert.assertEquals(IsoType.AMOUNT, iso.getField(28).getType());
	    Assert.assertEquals(request.getAmountTransactionFee(), iso.getObjectValue(28));
	    
	    Assert.assertEquals(IsoType.LLVAR, iso.getField(32).getType());
	    Assert.assertEquals(request.getAcquiringInstitutionIdentificationCode(), iso.getObjectValue(32));
	    
	    Assert.assertEquals(IsoType.LLVAR, iso.getField(35).getType());
	    Assert.assertEquals(request.getTrack2Data(), iso.getObjectValue(35));
	    
	    Assert.assertEquals(IsoType.ALPHA, iso.getField(43).getType());
	    Assert.assertEquals(request.getCardAcceptorName(), iso.getObjectValue(43));
	    
	    Assert.assertEquals(IsoType.LLLVAR, iso.getField(47).getType());
	    log.debug("nested pojo\n {}", iso.getObjectValue(47));
	    Assert.assertTrue("must be of type CompositeFieldPojo", iso.getObjectValue(47) instanceof CompositeFieldPojo);
	    CompositeFieldPojo cf = iso.getObjectValue(47);
	    Assert.assertEquals(request.getAdditionalDataNational(), cf.getField(1).getValue());
	    
	    //here we go the nested pojo
	    Assert.assertEquals(IsoType.LLLVAR, iso.getField(48).getType());
	    log.debug("nested pojo\n {}", iso.getObjectValue(48));
	    Assert.assertTrue("must be of type CompositeFieldPojo", iso.getObjectValue(48) instanceof CompositeFieldPojo);
	    CompositeFieldPojo cf1 = iso.getObjectValue(48);
	    Assert.assertEquals(request.getAdditionalData().getPrivateDataStr(), cf1.getField(1).getValue());
	    Assert.assertEquals(request.getAdditionalData().getPrivateDataNum(), cf1.getField(2).getValue());
	    
	    Assert.assertEquals(IsoType.ALPHA, iso.getField(49).getType());
	    Assert.assertEquals(request.getCurrencyCodeTransaction(), iso.getObjectValue(49));
	    
	    Assert.assertEquals(IsoType.LLLVAR, iso.getField(60).getType());
	    Assert.assertEquals(request.getReservedNational(), iso.getObjectValue(60));
	    
	    Assert.assertEquals(IsoType.LLLVAR, iso.getField(61).getType());
	    Assert.assertEquals(request.getReservedPrivate(), iso.getObjectValue(61));
	    
	    Assert.assertEquals(IsoType.LLVAR, iso.getField(100).getType());
	    Assert.assertEquals(request.getReceivingInstitutionIdentificationCode(), iso.getObjectValue(100));
	    
	    Assert.assertEquals(IsoType.LLVAR, iso.getField(102).getType());
	    Assert.assertEquals(request.getAccountIdentification1(), iso.getObjectValue(102));
	    
	    String isoStr = iso.debugString();
	    log.debug("testNewMessageAcquirerFinancialRequestRoundTrip\n {}", isoStr);
	    
	    //decode here, second leg of the round trip
	    IsoMessage m = mf.parseMessage(isoStr.getBytes(), 0);
	    AcquirerFinancialRequest afr = mf.parseMessage(m, AcquirerFinancialRequest.class);
	    
		// field 3
	    Assert.assertEquals(request.getProcessingCode(), afr.getProcessingCode());

	    // field 7
	    Assert.assertEquals(request.getDateTransaction().getMonth(), afr.getDateTransaction().getMonth());
	    //IsoType.DATE10 is year insensitive, we need to set the year otherwise week day number will be wrong
	    afr.getDateTransaction().setYear(request.getDateTransaction().getYear());
	    Assert.assertEquals(request.getDateTransaction().getDay(), afr.getDateTransaction().getDay());
	    Assert.assertEquals(request.getDateTransaction().getHours(), afr.getDateTransaction().getHours());
	    Assert.assertEquals(request.getDateTransaction().getMinutes(), afr.getDateTransaction().getMinutes());
	    Assert.assertEquals(request.getDateTransaction().getSeconds(), afr.getDateTransaction().getSeconds());
		
	    // field 11
	    Assert.assertEquals(request.getSystemTraceAuditNumber(), afr.getSystemTraceAuditNumber());
		
	    // field 28
	    Assert.assertEquals(request.getAmountTransactionFee().longValue(), afr.getAmountTransactionFee().longValue());
	    
	    // field 32
	    Assert.assertEquals(request.getAcquiringInstitutionIdentificationCode(), afr.getAcquiringInstitutionIdentificationCode());
	    
	    // field 35
	    Assert.assertEquals(request.getTrack2Data(), afr.getTrack2Data());
	    
	    // field 43
	    Assert.assertEquals(request.getCardAcceptorName(), afr.getCardAcceptorName());
	    
	    // field 47
	    Assert.assertEquals(request.getAdditionalDataNational(), afr.getAdditionalDataNational());
	    
	    // field 48
	    Assert.assertEquals(request.getAdditionalData().getPrivateDataStr(), afr.getAdditionalData().getPrivateDataStr());
	    Assert.assertEquals(request.getAdditionalData().getPrivateDataNum(), afr.getAdditionalData().getPrivateDataNum());
	    
	    // field 49
	    Assert.assertEquals(request.getCurrencyCodeTransaction(), afr.getCurrencyCodeTransaction());
	    
	    // field 60
	    Assert.assertEquals(request.getReservedNational(), afr.getReservedNational());
	    
	    // field 61
	    Assert.assertEquals(request.getReservedPrivate(), afr.getReservedPrivate());
	    
	    // field 100
	    Assert.assertEquals(request.getReceivingInstitutionIdentificationCode(), afr.getReceivingInstitutionIdentificationCode());
	    
	    // field 102
	    Assert.assertEquals(request.getAccountIdentification1(), afr.getAccountIdentification1());
	    
	    
	}
}
