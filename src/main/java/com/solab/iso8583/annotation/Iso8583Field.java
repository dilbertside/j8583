package com.solab.iso8583.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import com.solab.iso8583.IsoType;

/**
 * Annotation to decorate pojo fields which must be part of a ISO 8583 template definition
 * @author dbs on Sep 24, 2016
 * @version 1.0
 * @since V1.12.0
 */
@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
public @interface Iso8583Field {
	/**
	 * mandatory, unique index for a pojo template
	 */
	public int index();
	
	/**
	 * if empty the pojo field name will be used
	 */
	public String name() default "";
	
	/**
	 * default is {@link com.solab.iso8583.IsoType#ALPHA}
	 */
	public IsoType type() default IsoType.ALPHA;

	/**
	 * used with {@link com.solab.iso8583.IsoType#NUMERIC}
	 */
	public int length() default 0;
	
}