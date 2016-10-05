/**
 * IsoField
 */
package com.solab.iso8583;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.math.NumberUtils;

import com.solab.iso8583.annotation.Iso8583Field;
import com.solab.iso8583.codecs.CompositeField;
import com.solab.iso8583.util.PojoUtils;

/**
 * Represents the definition of a field from a pojo annotated property
 * @author dilbertside
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
	 * map retaining the list of nested fields retaining all properties definition of private or national reserved fields<br>
	 * Key is the  {@link #propertyName}<br>
	 * Value is the list of all properties composing this pojo object<br>
	 * it's filled while registration by detecting properties annotated with {@link Iso8583Field}<br>
	 * Please note in this sub-pojo, index does not have the same meaning as Iso8583 message template 
	 *  
	 */
	Map<String, Set<IsoField<?>>> mapNestedField = null;
	
	boolean custom = false;

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
	 * @return <code>null</code> if parameter is <code>null</code> or IsoValue <code>null</code>
	 * @throws ClassCastException
	 */
	public T getValueSafeCast(IsoValue<?> isoValue) throws ClassCastException {
		return getValueSafeCast(isoValue, fieldClass);
	}
	
	/**
	 * used with pojo dynamic instantiation<br>
	 * cast an IsoValue to a field target type
	 * @param isoValue {@link IsoValue}
	 * @param fieldClass the target type which the value will be cast to
	 * @return <code>null</code> if parameter is <code>null</code> or IsoValue <code>null</code>
	 * @throws ClassCastException
	 */
	@SuppressWarnings("unchecked")
	public static <T> T getValueSafeCast(IsoValue<?> isoValue, Class<T> fieldClass) throws ClassCastException {
		if(null != isoValue){
			if(null != isoValue.getEncoder()){
				return (T) isoValue.getEncoder().decodeField(isoValue.getValue().toString());
			}
			if(isoValue.getType() == IsoType.NUMERIC)
				return PojoUtils.convertNumberToTargetClass((Number)isoValue.getValue(), fieldClass);
			return fieldClass.cast(isoValue.getValue());
		}
		return null;
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
		return HashCodeBuilder.reflectionHashCode(this);
	}
	
	/* (non-Javadoc)
	 * @see java.lang.Object#equals(java.lang.Object)
	 */
	@Override
	public boolean equals(Object obj) {
		return EqualsBuilder.reflectionEquals(this, obj);
	}
	
	/* (non-Javadoc)
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		return ToStringBuilder.reflectionToString(this);
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
	
	public static class AmountCustomField implements CustomField<BigDecimal>{
		@Override
		public BigDecimal decodeField(String value) {
			if(StringUtils.isNotBlank(value))
				return NumberUtils.createBigDecimal(value.replaceAll("\\D+", "").trim());
			return null;
		}
		@Override
		public String encodeField(BigDecimal value) {
			if(null != value)
				return value.toString();
			return "";
		}
	}//class AmountCustomField 

	/**
	 * default CustomField encoder when a sub-field is defined in the ISO message<br>
	 * This default implementation does nothing, and is not encoding aware
	 */
	public static class DefaultCustomStringField implements CustomBinaryField<String>{
		@Override
		public String decodeField(String value) {
			return value;
		}
		@Override
		public String encodeField(String value) {
			return value;
		}
		@Override
		public String decodeBinaryField(byte[] value, int offset, int length) {
			return null != value ? new String(value) : null;
		}
		@Override
		public byte[] encodeBinaryField(String value) {
			return null != value ? value.getBytes() : null;
		}
	}//class DefaultCustomStringField 
	
	/**
	 * default CustomField encoder when a sub-field is defined in the ISO message<br>
	 * This default implementation does nothing, and is not encoding aware
	 */
	public static class DefaultCustomByteField implements CustomBinaryField<byte[]>{
		@Override
		public byte[] decodeField(String value) {
			return value.getBytes();
		}
		@Override
		public String encodeField(byte[] value) {
			return null != value ? new String(value) : null;
		}
		@Override
		public byte[] decodeBinaryField(byte[] value, int offset, int length) {
			return value;
		}
		@Override
		public byte[] encodeBinaryField(byte[] value) {
			return value;
		}
	}//class DefaultCustomByteField 

	/**
	 * @return the mapNestedField
	 */
	public Map<String, Set<IsoField<?>>> getMapNestedField() {
		if(null == mapNestedField)
			mapNestedField = new HashMap<>();
		return mapNestedField;
	}
	
	/**
	 * retrieve a list of all nested fields
	 * @param key primary template field name
	 * @param HashSet never null 
	 */
	public Set<IsoField<?>> getAllNestedField(String key) {
		if (StringUtils.isNotBlank(key) && getMapNestedField().containsKey(key))
			return getMapNestedField().get(key);
		return new HashSet<>();
	}
	/**
	 * add a nested field to primary template field
	 * @param key primary template field name
	 * @param nestedIsoField nested IsoField 
	 */
	public void addNestedField(String key, IsoField<?> nestedIsoField) {
		if(getMapNestedField().containsKey(key)){
			mapNestedField.get(key).add(nestedIsoField);
		}else{
			Set<IsoField<?>> set = new HashSet<>();
			set.add(nestedIsoField);
			mapNestedField.put(key, set);
		}
	}
	
	/**
	 * @param mapNestedField the mapNestedField to set
	 */
	public void setMapNestedField(Map<String, Set<IsoField<?>>> mapNestedField) {
		this.mapNestedField = mapNestedField;
	}

	public boolean isNested() {
		return this.mapNestedField != null;
	}
	
	public static class CompositeFieldPojo extends CompositeField{

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

	/**
	 * @return the custom
	 */
	public boolean isCustom() {
		return custom;
	}

	/**
	 * @param custom the custom to set
	 * @return IsoField for convenience chaining
	 */
	public IsoField<?> setCustom(boolean custom) {
		this.custom = custom;
		return this;
	}
}
