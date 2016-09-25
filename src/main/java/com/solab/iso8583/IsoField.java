/**
 * IsoField
 */
package com.solab.iso8583;

import java.math.BigDecimal;
import java.math.BigInteger;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.math.NumberUtils;

import com.solab.iso8583.annotation.Iso8583Field;

/**
 * Represents the definition of a field from a pojo annotated property
 * @author lch
 *
 */
public class IsoField<T extends Object> {

	public static IsoField<Number> stan = new IsoField<Number>(11, "stan", IsoType.NUMERIC, 6, Number.class);
	
	/**
	 * ISO 8583 index, unique per pojo 
	 */
	int index;
	
	/**
	 * ISO 8583 field length
	 */
	int length = 0;

	/**
	 * ISO 8583 pojo {@link IsoType}
	 */
	IsoType isoType;

	/**
	 * Iso 8583 pojo field name
	 */
	String name;
	
	/**
	 * real pojo property name holding the value
	 */
	String propertyName;
	
	/**
	 * track the pojo field type
	 * http://stackoverflow.com/questions/75175/create-instance-of-generic-type-in-java
	 */
	Class<T> fieldClass;

	/**
	 * @param index
	 * @param name
	 * @param isoType
	 * @param length
	 */
	public IsoField(int index, String name, IsoType isoType, int length, Class<T> fieldClass) {
		this.isoType = isoType;
		this.length = length;
		this.name = name;
		this.index = index;
		this.fieldClass = fieldClass;
	}

	/**
	 * 
	 * @param annotation pojo field annotated with {@link Iso8583Field}
	 * @param fieldClass field type
	 */
	public IsoField(Iso8583Field annotation, Class<T> fieldClass) {
		this(annotation.index(), annotation.name(), annotation.type(), annotation.length(), fieldClass);
		switch (annotation.type()) {
		case ALPHA:
		case BINARY:
		case NUMERIC:
			if (length < 1) {
				throw new IllegalArgumentException("length is not set correctly");
			}
			break;
		default:
		}
		
	}

	/**
	 * @return the index
	 */
	public int getIndex() {
		return index;
	}

	/**
	 * @param index the index to set
	 * @return IsoField for convenience chaining
	 */
	public IsoField<T> setIndex(int index) {
		this.index = index;
		return this;
	}

	/**
	 * @return the length
	 */
	public int getLength() {
		return length;
	}

	/**
	 * @param length the length to set
	 * @return IsoField for convenience chaining
	 */
	public IsoField<T> setLength(int length) {
		this.length = length;
		return this;
	}

	/**
	 * @return the isoType
	 */
	public IsoType getIsoType() {
		return isoType;
	}

	/**
	 * @param isoType the isoType to set
	 * @return IsoField for convenience chaining
	 */
	public IsoField<T> setIsoType(IsoType isoType) {
		this.isoType = isoType;
		return this;
	}

	/**
	 * @return the name
	 */
	public String getName() {
		return name;
	}

	/**
	 * @param name the name to set
	 * @return IsoField for convenience chaining
	 */
	public IsoField<T> setName(String name) {
		this.name = name;
		return this;
	}

	/**
	 * @return the propertyName
	 */
	public String getPropertyName() {
		return propertyName;
	}
	
	/**
	 * @param propertyName the propertyName to set
	 */
	public void setPropertyName(String propertyName) {
		this.propertyName = propertyName;
	}
	
	/**
	 * used with pojo dynamic instantiation
	 * @param value to cast to pojo field target type 
	 * @return dynamically type of value from template pojo introspection
	 * @throws ClassCastException
	 */
	public T getValueSafeCast(Object value) throws ClassCastException {
		return fieldClass.cast(value);
	}
	
	/**
	 * used with pojo dynamic instantiation<br>
	 * cast an IsoValue to pojo field target type
	 * @param isoValue {@link IsoValue}
	 * @return null if param null or IsoValue null
	 * @throws ClassCastException
	 */
	@SuppressWarnings("unchecked")
	public T getValueSafeCast(IsoValue<?> isoValue) throws ClassCastException {
		if(null != isoValue){
			if(null != isoValue.getEncoder()){
				return (T) isoValue.getEncoder().decodeField(isoValue.getValue().toString());
			}
			if(isoValue.getType() == IsoType.NUMERIC)
				return convertNumberToTargetClass((Number)isoValue.getValue(), fieldClass);
			return fieldClass.cast(isoValue.getValue());
		}
		return null;
	}

	/**
	 * Convert the given number into an instance of the given target class.
	 * @param number the number to convert
	 * @param targetClass the target class to convert to
	 * @return the converted number
	 * @throws IllegalArgumentException if the target class is not supported
	 * (i.e. not a standard Number subclass as included in the JDK)
	 * @see java.lang.Byte
	 * @see java.lang.Short
	 * @see java.lang.Integer
	 * @see java.lang.Long
	 * @see java.math.BigInteger
	 * @see java.lang.Float
	 * @see java.lang.Double
	 * @see java.math.BigDecimal
	 */
	@SuppressWarnings({ "unchecked", "hiding" })
	public <T> T convertNumberToTargetClass(Number number, Class<T> targetClass)
			throws IllegalArgumentException {

		assert null != number: "Number must not be null";
		assert null != targetClass : "Target class must not be null";

		if (targetClass.isInstance(number)) {
			return (T) number;
		}
		else if (Byte.class == targetClass) {
			byte value = number.byteValue();
			if (value < Byte.MIN_VALUE || value > Byte.MAX_VALUE) {
				throw new IllegalArgumentException(String.format("Could not convert number [%d] of type [%s] to target class [%s]: overflow", number, number.getClass().getName(), targetClass.getName()));
			}
			return (T) Byte.valueOf(number.byteValue());
		}
		else if (Short.class == targetClass) {
			short value = number.shortValue();
			if (value < Short.MIN_VALUE || value > Short.MAX_VALUE) {
				throw new IllegalArgumentException(String.format("Could not convert number [%d] of type [%s] to target class [%s]: overflow", number, number.getClass().getName(), targetClass.getName()));
			}
			return (T) Short.valueOf(number.shortValue());
		}
		else if (Integer.class == targetClass) {
			long value = number.intValue();
			if (value < Integer.MIN_VALUE || value > Integer.MAX_VALUE) {
				throw new IllegalArgumentException(String.format("Could not convert number [%d] of type [%s] to target class [%s]: overflow", number, number.getClass().getName(), targetClass.getName()));
			}
			return (T) Integer.valueOf(number.intValue());
		}
		else if (Long.class == targetClass) {
			long value = number.longValue();
			return (T) Long.valueOf(value);
		}
		else if (BigInteger.class == targetClass) {
			if (number instanceof BigDecimal) {
				// do not lose precision - use BigDecimal's own conversion
				return (T) ((BigDecimal) number).toBigInteger();
			} else {
				// original value is not a Big* number - use standard long conversion
				return (T) BigInteger.valueOf(number.longValue());
			}
		}
		else if (Float.class == targetClass) {
			return (T) Float.valueOf(number.floatValue());
		}
		else if (Double.class == targetClass) {
			return (T) Double.valueOf(number.doubleValue());
		}
		else if (BigDecimal.class == targetClass) {
			// always use BigDecimal(String) here to avoid unpredictability of BigDecimal(double)
			// (see BigDecimal javadoc for details)
			return (T) new BigDecimal(number.toString());
		}
		else {
			throw new IllegalArgumentException(String.format("Could not convert number [%d] of type [%s] to target class [%s]: overflow", number, number.getClass().getName(), targetClass.getName()));
		}
	}
	/**
	 * @return the fieldClass
	 */
	public Class<T> getFieldClass() {
		return fieldClass;
	}

	/**
	 * @param fieldClass the fieldClass to set
	 */
	public void setFieldClass(Class<T> fieldClass) {
		this.fieldClass = fieldClass;
	}

	/* (non-Javadoc)
	 * @see java.lang.Object#hashCode()
	 */
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + index;
		result = prime * result + ((isoType == null) ? 0 : isoType.hashCode());
		result = prime * result + length;
		result = prime * result + ((name == null) ? 0 : name.hashCode());
		result = prime * result + ((propertyName == null) ? 0 : propertyName.hashCode());
		result = prime * result + ((fieldClass == null) ? 0 : fieldClass.hashCode());
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
		IsoField<?> other = (IsoField<?>) obj;
		if (index != other.index)
			return false;
		if (isoType != other.isoType)
			return false;
		if (length != other.length)
			return false;
		if (name == null) {
			if (other.name != null)
				return false;
		} else if (!name.equals(other.name))
			return false;
		if (propertyName == null) {
			if (other.propertyName != null)
				return false;
		} else if (!propertyName.equals(other.propertyName))
			return false;
		if (fieldClass == null) {
			if (other.fieldClass != null)
				return false;
		} else if (!fieldClass.equals(other.fieldClass))
			return false;
		return true;
	}

	/* (non-Javadoc)
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append("IsoField [index=");
		builder.append(index);
		builder.append(", length=");
		builder.append(length);
		builder.append(", ");
		if (isoType != null) {
			builder.append("isoType=");
			builder.append(isoType);
			builder.append(", ");
		}
		if (name != null) {
			builder.append("name=");
			builder.append(name);
			builder.append(", ");
		}
		if (propertyName != null) {
			builder.append("propertyName=");
			builder.append(propertyName);
			builder.append(", ");
		}
		if (fieldClass != null) {
			builder.append("fieldClass=");
			builder.append(fieldClass);
		}
		builder.append("]");
		return builder.toString();
	}
	
	/**
	 * pojo use, custom encoder/decoder to avoid during parsing a number 
	 * which is handled by AlphaNumericFieldParseInfo return a String and not a Number 
	 * @author dbs
	 *
	 */
	public static class NumberCustomField implements CustomField<Number>{
		@Override
		public Number decodeField(String value) {
			if(StringUtils.isNotBlank(value)){
				value = value.replaceAll("\\D+", "").trim();//rmove all non numeric chars
				if(StringUtils.isNotBlank(value))//once more at it happens sometimes value sent is "null" as string or garbage
					return NumberUtils.createNumber(value);
			}
			return null;
		}
		@Override
		public String encodeField(Number value) {
			if(null != value)
				return value.toString();
			return "";
		}
	}//class NumberCustomField 
	
	public static class AmountCustomField implements CustomField<Number>{
		@Override
		public Number decodeField(String value) {
			if(StringUtils.isNotBlank(value))
				return NumberUtils.createNumber(value.replaceAll("\\D+", "").trim());
			return null;
		}
		@Override
		public String encodeField(Number value) {
			if(null != value)
				return value.toString();
			return "";
		}
	}//class NumberCustomField 
}
