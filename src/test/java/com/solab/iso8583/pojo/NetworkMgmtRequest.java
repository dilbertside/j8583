package com.solab.iso8583.pojo;

import java.util.Date;

import com.solab.iso8583.IsoType;
import com.solab.iso8583.annotation.*;

@Iso8583(type=0x800)
public class NetworkMgmtRequest {
	
	protected String bogus;
	
	@Iso8583Field(index=7, type=IsoType.DATE10)
	protected Date dateTransaction;
	
	@Iso8583Field(index=11, type=IsoType.NUMERIC, length=6)
	protected Number systemTraceAuditNumber;
	
	
	@Iso8583Field(index=70, type=IsoType.NUMERIC, length=3)
	protected Number codeInformationNetwork;

	/**
	 * newInstance requires an empty pojo constructor
	 */
	public NetworkMgmtRequest(){
		
	}
	/**
	 * @param dateTransaction
	 * @param systemTraceAuditNumber
	 * @param codeInformationNetwork
	 */
	public NetworkMgmtRequest(Date dateTransaction, Number systemTraceAuditNumber, Number codeInformationNetwork) {
		this.dateTransaction = dateTransaction;
		this.systemTraceAuditNumber = systemTraceAuditNumber;
		this.codeInformationNetwork = codeInformationNetwork;
	}

	/**
	 * @return the dateTransaction
	 */
	public Date getDateTransaction() {
		return dateTransaction;
	}


	/**
	 * @param dateTransaction the dateTransaction to set
	 */
	public void setDateTransaction(Date dateTransaction) {
		this.dateTransaction = dateTransaction;
	}


	/**
	 * @return the systemTraceAuditNumber
	 */
	public Number getSystemTraceAuditNumber() {
		return systemTraceAuditNumber;
	}


	/**
	 * @param systemTraceAuditNumber the systemTraceAuditNumber to set
	 */
	public void setSystemTraceAuditNumber(Number systemTraceAuditNumber) {
		this.systemTraceAuditNumber = systemTraceAuditNumber;
	}


	/**
	 * @return the codeInformationNetwork
	 */
	public Number getCodeInformationNetwork() {
		return codeInformationNetwork;
	}


	/**
	 * @param codeInformationNetwork the codeInformationNetwork to set
	 */
	public void setCodeInformationNetwork(Number codeInformationNetwork) {
		this.codeInformationNetwork = codeInformationNetwork;
	}

	/* (non-Javadoc)
	 * @see java.lang.Object#hashCode()
	 */
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((bogus == null) ? 0 : bogus.hashCode());
		result = prime * result + ((codeInformationNetwork == null) ? 0 : codeInformationNetwork.hashCode());
		result = prime * result + ((dateTransaction == null) ? 0 : dateTransaction.hashCode());
		result = prime * result + ((systemTraceAuditNumber == null) ? 0 : systemTraceAuditNumber.hashCode());
		return result;
	}

	/* (non-Javadoc)
	 * @see java.lang.Object#equals(java.lang.Object)
	 */
	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		NetworkMgmtRequest other = (NetworkMgmtRequest) obj;
		if (bogus == null) {
			if (other.bogus != null)
				return false;
		} else if (!bogus.equals(other.bogus))
			return false;
		if (codeInformationNetwork == null) {
			if (other.codeInformationNetwork != null)
				return false;
		} else if (!codeInformationNetwork.equals(other.codeInformationNetwork))
			return false;
		if (dateTransaction == null) {
			if (other.dateTransaction != null)
				return false;
		} else if (!dateTransaction.equals(other.dateTransaction))
			return false;
		if (systemTraceAuditNumber == null) {
			if (other.systemTraceAuditNumber != null)
				return false;
		} else if (!systemTraceAuditNumber.equals(other.systemTraceAuditNumber))
			return false;
		return true;
	}

	/* (non-Javadoc)
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		return "NetworkMgmtRequest [bogus=" + bogus + ", dateTransaction=" + dateTransaction
				+ ", systemTraceAuditNumber=" + systemTraceAuditNumber + ", codeInformationNetwork="
				+ codeInformationNetwork + "]";
	}
	
	
}
