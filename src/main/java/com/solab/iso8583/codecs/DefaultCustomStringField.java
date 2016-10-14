/**
 * DefaultCustomStringField
 */
package com.solab.iso8583.codecs;

import com.solab.iso8583.CustomBinaryField;

/**
 * default CustomField encoder when a sub-field is defined in the ISO message<br>
 *  This default implementation does nothing, and is not encoding aware
 * @author lch on Oct 12, 2016 11:29:32 AM
 * @since 1.0.0
 * @version 1.0.0
 *
 */
public class DefaultCustomStringField implements CustomBinaryField<String> {

	/**
	 * @see com.solab.iso8583.CustomField#decodeField(java.lang.String)
	 */
	@Override
	public String decodeField(String value) {
		return value;
	}

	/**
	 * @see com.solab.iso8583.CustomField#encodeField(java.lang.Object)
	 */
	@Override
	public String encodeField(String value) {
		return value;
	}

	/**
	 * @see com.solab.iso8583.CustomBinaryField#decodeBinaryField(byte[], int, int)
	 */
	@Override
	public String decodeBinaryField(byte[] value, int offset, int length) {
		return null != value ? new String(value) : null;
	}

	/**
	 * @see com.solab.iso8583.CustomBinaryField#encodeBinaryField(java.lang.Object)
	 */
	@Override
	public byte[] encodeBinaryField(String value) {
		return null != value ? value.getBytes() : null;
	}

}
