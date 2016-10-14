/**
 * CompositeFieldPojo
 */
package com.solab.iso8583.codecs;

import com.solab.iso8583.IsoValue;

/**
 * @author lch on Oct 12, 2016 11:25:12 AM
 * @since 1.0.0
 * @version 1.0.0
 *
 */
public class CompositeFieldPojo extends CompositeField{

	/**
	 * set an {@link IsoValue} to a field index
	 * @param idx base 1
	 * @param isoValue 
	 * @return
	 */
	public CompositeFieldPojo setField(int idx, IsoValue<?> isoValue) {
		assert idx > 0 : "index is in Base 1";
		getValues().set(idx-1, isoValue);
		return this;
	}
	
	/**
	 * retrieve a field by index
	 * @param idx index base 1
	 */
	public IsoValue<?> getField(int idx){
		assert idx > 0 : "index is in Base 1";
		return getValues().get(idx-1);
	}
}
