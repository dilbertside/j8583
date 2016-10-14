/**
 * NumberCustomField
 */
package com.solab.iso8583.codecs;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.math.NumberUtils;

import com.solab.iso8583.CustomField;

/**
 * pojo use, custom encoder/decoder to avoid during parsing a number 
 * which is handled by AlphaNumericFieldParseInfo return a String and not a Number 
 * @author dbs on Oct 12, 2016 11:27:31 AM
 * @since 1.0.0
 * @version 1.0.0
 *
 */
public class NumberCustomField implements CustomField<Number>{
	
	/**
	 * @see com.solab.iso8583.CustomField#decodeField(java.lang.String)
	 */
	@Override
	public Number decodeField(String value) {
		if(StringUtils.isNotBlank(value)){
			value = value.replaceAll("\\D+", "").trim();//remove all non numeric chars
			if(StringUtils.isNotBlank(value))//once more at it happens sometimes value sent is "null" as string or garbage
				return NumberUtils.createNumber(value);
		}
		return null;
	}
	
	/**
	 * @see com.solab.iso8583.CustomField#encodeField(java.lang.Object)
	 */
	@Override
	public String encodeField(Number value) {
		if(null != value)
			return value.toString();
		return "";
	}
}
