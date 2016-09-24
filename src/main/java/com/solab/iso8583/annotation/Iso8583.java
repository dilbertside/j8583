package com.solab.iso8583.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import com.solab.iso8583.TraceNumberGenerator;
import com.solab.iso8583.impl.SimpleTraceGenerator;

/**
 * Annotated to a pojo, this annotation defines an ISO 8583 template similar to XML definition 
 * @author dbs
 *@version 1.0
 * @since V1.12.0
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface Iso8583 {
	
	/**
	 * mandatory, template number, in hexadecimal ie \"0x200\"
	 */
	int type();

	/**
	 * header to send, sent in binary if binary flag set
	 */
	String header() default "";

	/**
	 * flag to build a ISO 8583 in a binary form
	 */
	boolean binary() default false;
	
	/**
	 * encoding for message template, default UTF-8   
	 */
	String encoding() default "UTF-8";
	
	//following would need to store per message template, currently at the factory level
	/**
	 * auto fill a system trace audit number  <code>11</code> if one is not already set. inapplicable for parsing 
	 */
	//boolean stan() default false;
	
	/**
	 * generator to use, {@link #stan()} flag is activated 
	 */
	//Class<? extends TraceNumberGenerator> traceNumberGenerator() default SimpleTraceGenerator.class;
	
	/**
	 * auto fill a date <code>7</code> if one is not already set. inapplicable for parsing
	 */
	//boolean date() default false;
}
