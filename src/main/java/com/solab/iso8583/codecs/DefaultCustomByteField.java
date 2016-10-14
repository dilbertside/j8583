package com.solab.iso8583.codecs;

import com.solab.iso8583.CustomBinaryField;

/**
 * default CustomField encoder when a sub-field is defined in the ISO message<br>
 *  This default implementation does nothing, and is not encoding aware
 * @author dbs on Oct 12, 2016 11:33:07 AM
 * @since 1.0.0
 * @version 1.0.0
 *
 */
public class DefaultCustomByteField implements CustomBinaryField<byte[]> {

	/**
	 * @see com.solab.iso8583.CustomField#decodeField(java.lang.String)
	 */
	@Override
	public byte[] decodeField(String value) {
		return value.getBytes();
	}

	/**
	 * @see com.solab.iso8583.CustomField#encodeField(java.lang.Object)
	 */
	@Override
	public String encodeField(byte[] value) {
		return null != value ? new String(value) : null;
	}

	/**
	 * @see com.solab.iso8583.CustomBinaryField#decodeBinaryField(byte[], int, int)
	 */
	@Override
	public byte[] decodeBinaryField(byte[] value, int offset, int length) {
		return value;
	}

	/**
	 * @see com.solab.iso8583.CustomBinaryField#encodeBinaryField(java.lang.Object)
	 */
	@Override
	public byte[] encodeBinaryField(byte[] value) {
		return value;
	}

}
