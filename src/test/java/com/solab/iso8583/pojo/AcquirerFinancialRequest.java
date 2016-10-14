/**
 * AcquirerFinancialRequest
 */
package com.solab.iso8583.pojo;

import com.solab.iso8583.annotation.Iso8583;

/**
 * @author dilbertside on Sep 29, 2016 9:41:18 AM
 * @since 1.13.0
 * @version 1.0.0
 *
 */
@Iso8583(type=0x200)
public class AcquirerFinancialRequest extends AbstractMessage {

	private static final long serialVersionUID = -4352711673146090451L;

	public AcquirerFinancialRequest() { 
		super();
	}

	/**
	 * @param toClone
	 */
	public AcquirerFinancialRequest(AbstractMessage toClone) {
		super(toClone);
	}

}
