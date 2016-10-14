package com.solab.iso8583.pojo;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Date;
import java.util.GregorianCalendar;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.math.NumberUtils;

import com.solab.iso8583.CustomBinaryField;
import com.solab.iso8583.IsoType;
import com.solab.iso8583.codecs.DefaultCustomStringField;
import com.solab.iso8583.annotation.Iso8583Field;

/**
 * copied from http://j8583.sourceforge.net/xmlconf.html<br>
 * field name comes from https://github.com/kpavlov/jreactive-8583/blob/master/src/main/resources/org/jreactive/iso8583/iso8583fields.properties
 * 
 * @author dilbertside on Sep 29, 2016
 * @since 1.13.0
 * @version 1.0.0
 *
 */
@SuppressWarnings("serial")
public class AbstractMessage implements Serializable{

	
	public static class PrivateData implements Serializable{
		
	private static final long serialVersionUID = 5863255007081836317L;

	@Iso8583Field(index=1, type=IsoType.ALPHA, length=40)
  	protected String privateDataStr = "Life, the Universe, and Everything";
  	
  	@Iso8583Field(index=2, type=IsoType.NUMERIC, length=2)
  	protected Number privateDataNum = 42;

  	public PrivateData(){}
  	
		/**
		 * @param privateDataStr
		 * @param privateDataNum
		 */
		public PrivateData(PrivateData toClone) {
			this.privateDataStr = toClone.privateDataStr;
			this.privateDataNum = toClone.privateDataNum;
		}
		
		/* (non-Javadoc)
		 * @see java.lang.Object#hashCode()
		 */
		@Override
		public int hashCode() {
			return HashCodeBuilder.reflectionHashCode(this);
		}
		/* (non-Javadoc)
		 * @see java.lang.Object#equals(java.lang.Object)
		 */
		@Override
		public boolean equals(Object obj) {
			return EqualsBuilder.reflectionEquals(this, obj);
		}
		/* (non-Javadoc)
		 * @see java.lang.Object#toString()
		 */
		@Override
		public String toString() {
			return ToStringBuilder.reflectionToString(this);
		}

		/**
		 * @return the privateDataStr
		 */
		public String getPrivateDataStr() {
			return privateDataStr;
		}

		/**
		 * @param privateDataStr the privateDataStr to set
		 * @return AbstractMessage.PrivateData for convenience chaining
		 */
		public PrivateData setPrivateDataStr(String privateDataStr) {
			this.privateDataStr = privateDataStr;
			return this;
		}

		/**
		 * @return the privateDataNum
		 */
		public Number getPrivateDataNum() {
			return privateDataNum;
		}

		/**
		 * @param privateDataNum the privateDataNum to set
		 * @return AbstractMessage.PrivateData for convenience chaining
		 */
		public PrivateData setPrivateDataNum(Number privateDataNum) {
			this.privateDataNum = privateDataNum;
			return this;
		}
  	
  }

		
	
	public enum ProcessingCode{

		none(0, "None"),
		yes(1000, "YES"),
		no(2000, "NO"),
		maybe(3000, "MAYBE");
		
		final private int code;
		final private String label;
		
		ProcessingCode(int code, String label){
			this.code =code;
			this.label = label;
		}
		/**
		 * @return the code
		 */
		public int getCode() {
			return code;
		}
		/**
		 * @return the label
		 */
		public String getLabel() {
			return label;
		}

		public ProcessingCode searchCode(int code){
			for (ProcessingCode processingCode : ProcessingCode.values()) {
				if(processingCode.getCode() == code)
					return processingCode;
			}
			return none;
		}
		
		public String toString(){
			return "" + getCode();
		}

	}
	
	public static class EnumProcessingCodeCustomField implements CustomBinaryField<ProcessingCode>{
		
		/**
		 * work around, java spec http://docs.oracle.com/javase/specs/jls/se7/html/jls-8.html#jls-8.9
		 * 
		 * @param value enum to build from String
		 * @return
		 */
		@Override
		public ProcessingCode decodeField(String value) {
			if(StringUtils.isBlank(value))
				return ProcessingCode.none;
			return ProcessingCode.none.searchCode(NumberUtils.createNumber(value).intValue());
		}
		
		@Override
		public String encodeField(ProcessingCode value) {
			return value.toString();
		}

		@Override
		public ProcessingCode decodeBinaryField(byte[] value, int offset, int length) {
			return decodeField(new String(value));
		}

		@Override
		public byte[] encodeBinaryField(ProcessingCode value) {
			return encodeField(value).getBytes();
		}
	}
	
	@Iso8583Field(index=3, type=IsoType.NUMERIC, length=4, customField = true, customFieldMapper=EnumProcessingCodeCustomField.class)
	protected ProcessingCode processingCode = ProcessingCode.yes;
	
	@Iso8583Field(index=7, type=IsoType.DATE10)
	protected Date dateTransaction = new GregorianCalendar(2000, 1, 1, 1 ,1, 1).getTime();
	
	@Iso8583Field(index=11, type=IsoType.NUMERIC, length=6)
	protected Number systemTraceAuditNumber = 999999;

	@Iso8583Field(index=28, type=IsoType.AMOUNT)
    protected BigDecimal amountTransactionFee = BigDecimal.TEN;
		
	@Iso8583Field(index=32, type=IsoType.LLVAR)
    protected String acquiringInstitutionIdentificationCode= "456";
    
    @Iso8583Field(index=35, type=IsoType.LLVAR)
    protected String track2Data = "4591700012340000=";
    
    //Card acceptor name/location (1-23 address 24-36 city 37-38 state 39-40 country)
    @Iso8583Field(index=43, type=IsoType.ALPHA, length=40)
    protected String cardAcceptorName= "Fixed-width data                        ";
    
    //national
    @Iso8583Field(index=47, type=IsoType.LLLVAR, customField=true, customFieldMapper= DefaultCustomStringField.class)
    protected String additionalDataNational;
    
    //private
    @Iso8583Field(index=48, type=IsoType.LLLVAR, nestedField=true)
    protected PrivateData additionalData = new PrivateData();
    
    @Iso8583Field(index=49, type=IsoType.ALPHA, length=3)
    protected String currencyCodeTransaction= "840";
    
    @Iso8583Field(index=60, type=IsoType.LLLVAR)
    protected String reservedNational = "B456PRO1+000";
    
    @Iso8583Field(index=61, type=IsoType.LLLVAR)
    protected String reservedPrivate = "This field can have a value up to 999 characters long.";
    
    @Iso8583Field(index=100, type=IsoType.LLVAR)
    protected String receivingInstitutionIdentificationCode= "999";
    
    @Iso8583Field(index=102, type=IsoType.LLVAR)
    protected String accountIdentification1= "ABCD";
    
    public AbstractMessage() {}

		public AbstractMessage(AbstractMessage toClone) {
			this.processingCode = toClone.processingCode;
			this.acquiringInstitutionIdentificationCode = toClone.acquiringInstitutionIdentificationCode;
			this.track2Data = toClone.track2Data;
			this.cardAcceptorName = toClone.cardAcceptorName;
			this.additionalData = toClone.additionalData;
			this.currencyCodeTransaction = toClone.currencyCodeTransaction;
			this.reservedNational = toClone.reservedNational;
			this.reservedPrivate = toClone.reservedPrivate;
			this.receivingInstitutionIdentificationCode = toClone.receivingInstitutionIdentificationCode;
			this.accountIdentification1 = toClone.accountIdentification1;
		}


		/**
		 * @return the processingCode
		 */
		public ProcessingCode getProcessingCode() {
			return processingCode;
		}

		/**
		 * @param processingCode the processingCode to set
		 * @return AbstractMessage for convenience chaining
		 */
		public AbstractMessage setProcessingCode(ProcessingCode processingCode) {
			this.processingCode = processingCode;
			return this;
		}

		/**
		 * @return the acquiringInstitutionIdentificationCode
		 */
		public String getAcquiringInstitutionIdentificationCode() {
			return acquiringInstitutionIdentificationCode;
		}

		/**
		 * @param acquiringInstitutionIdentificationCode the acquiringInstitutionIdentificationCode to set
		 * @return AbstractMessage for convenience chaining
		 */
		public AbstractMessage setAcquiringInstitutionIdentificationCode(String acquiringInstitutionIdentificationCode) {
			this.acquiringInstitutionIdentificationCode = acquiringInstitutionIdentificationCode;
			return this;
		}

		/**
		 * @return the track2Data
		 */
		public String getTrack2Data() {
			return track2Data;
		}

		/**
		 * @param track2Data the track2Data to set
		 * @return AbstractMessage for convenience chaining
		 */
		public AbstractMessage setTrack2Data(String track2Data) {
			this.track2Data = track2Data;
			return this;
		}

		/**
		 * @return the cardAcceptorName
		 */
		public String getCardAcceptorName() {
			return cardAcceptorName;
		}

		/**
		 * @param cardAcceptorName the cardAcceptorName to set
		 * @return AbstractMessage for convenience chaining
		 */
		public AbstractMessage setCardAcceptorName(String cardAcceptorName) {
			this.cardAcceptorName = cardAcceptorName;
			return this;
		}

		/**
		 * @return the additionalData
		 */
		public PrivateData getAdditionalData() {
			return additionalData;
		}

		/**
		 * @param additionalData the additionalData to set
		 * @return AbstractMessage for convenience chaining
		 */
		public AbstractMessage setAdditionalData(PrivateData additionalData) {
			this.additionalData = additionalData;
			return this;
		}

		/**
		 * @return the currencyCodeTransaction
		 */
		public String getCurrencyCodeTransaction() {
			return currencyCodeTransaction;
		}

		/**
		 * @param currencyCodeTransaction the currencyCodeTransaction to set
		 * @return AbstractMessage for convenience chaining
		 */
		public AbstractMessage setCurrencyCodeTransaction(String currencyCodeTransaction) {
			this.currencyCodeTransaction = currencyCodeTransaction;
			return this;
		}

		/**
		 * @return the reservedNational
		 */
		public String getReservedNational() {
			return reservedNational;
		}

		/**
		 * @param reservedNational the reservedNational to set
		 * @return AbstractMessage for convenience chaining
		 */
		public AbstractMessage setReservedNational(String reservedNational) {
			this.reservedNational = reservedNational;
			return this;
		}

		/**
		 * @return the reservedPrivate
		 */
		public String getReservedPrivate() {
			return reservedPrivate;
		}

		/**
		 * @param reservedPrivate the reservedPrivate to set
		 * @return AbstractMessage for convenience chaining
		 */
		public AbstractMessage setReservedPrivate(String reservedPrivate) {
			this.reservedPrivate = reservedPrivate;
			return this;
		}

		/**
		 * @return the receivingInstitutionIdentificationCode
		 */
		public String getReceivingInstitutionIdentificationCode() {
			return receivingInstitutionIdentificationCode;
		}

		/**
		 * @param receivingInstitutionIdentificationCode the receivingInstitutionIdentificationCode to set
		 * @return AbstractMessage for convenience chaining
		 */
		public AbstractMessage setReceivingInstitutionIdentificationCode(String receivingInstitutionIdentificationCode) {
			this.receivingInstitutionIdentificationCode = receivingInstitutionIdentificationCode;
			return this;
		}

		/**
		 * @return the accountIdentification1
		 */
		public String getAccountIdentification1() {
			return accountIdentification1;
		}

		/**
		 * @param accountIdentification1 the accountIdentification1 to set
		 * @return AbstractMessage for convenience chaining
		 */
		public AbstractMessage setAccountIdentification1(String accountIdentification1) {
			this.accountIdentification1 = accountIdentification1;
			return this;
		}
		
		/* (non-Javadoc)
		 * @see java.lang.Object#hashCode()
		 */
		@Override
		public int hashCode() {
			return HashCodeBuilder.reflectionHashCode(this);
		}
		/* (non-Javadoc)
		 * @see java.lang.Object#equals(java.lang.Object)
		 */
		@Override
		public boolean equals(Object obj) {
			return EqualsBuilder.reflectionEquals(this, obj);
		}
		/* (non-Javadoc)
		 * @see java.lang.Object#toString()
		 */
		@Override
		public String toString() {
			return ToStringBuilder.reflectionToString(this);
		}

		/**
		 * @return the dateTransaction
		 */
		public Date getDateTransaction() {
			return dateTransaction;
		}

		/**
		 * @param dateTransaction the dateTransaction to set
		 * @return AbstractMessage for convenience chaining
		 */
		public AbstractMessage setDateTransaction(Date dateTransaction) {
			this.dateTransaction = dateTransaction;
			return this;
		}

		/**
		 * @return the systemTraceAuditNumber
		 */
		public Number getSystemTraceAuditNumber() {
			return systemTraceAuditNumber;
		}

		/**
		 * @param systemTraceAuditNumber the systemTraceAuditNumber to set
		 * @return AbstractMessage for convenience chaining
		 */
		public AbstractMessage setSystemTraceAuditNumber(Number systemTraceAuditNumber) {
			this.systemTraceAuditNumber = systemTraceAuditNumber;
			return this;
		}

		/**
		 * @return the amountTransactionFee
		 */
		public BigDecimal getAmountTransactionFee() {
			return amountTransactionFee;
		}

		/**
		 * @param amountTransactionFee the amountTransactionFee to set
		 * @return AbstractMessage for convenience chaining
		 */
		public AbstractMessage setAmountTransactionFee(BigDecimal amountTransactionFee) {
			this.amountTransactionFee = amountTransactionFee;
			return this;
		}

		/**
		 * @return the additionalDataNational
		 */
		public String getAdditionalDataNational() {
			return additionalDataNational;
		}

		/**
		 * @param additionalDataNational the additionalDataNational to set
		 * @return AbstractMessage for convenience chaining
		 */
		public AbstractMessage setAdditionalDataNational(String additionalDataNational) {
			this.additionalDataNational = additionalDataNational;
			return this;
		}
}
