package com.solab.iso8583.codecs;

import java.math.BigDecimal;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.math.NumberUtils;

import com.solab.iso8583.CustomField;

/**
 * 
 * @author dbs on Oct 12, 2016 11:34:44 AM
 * @since 1.0.0
 * @version 1.0.0
 *
 */
public class AmountCustomField implements CustomField<BigDecimal> {

	/**
	 * @see com.solab.iso8583.CustomField#decodeField(java.lang.String)
	 */
	@Override
	public BigDecimal decodeField(String value) {
		if(StringUtils.isNotBlank(value))
			return NumberUtils.createBigDecimal(value.replaceAll("\\D+", "").trim());
		return null;
	}
	
	/**
	 * @see com.solab.iso8583.CustomField#encodeField(java.lang.Object)
	 */
	@Override
	public String encodeField(BigDecimal value) {
		if(null != value)
			return value.toString();
		return "";
	}

}
