package com.solab.iso8583;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.nio.charset.Charset;
import java.time.LocalDateTime;
import java.time.ZonedDateTime;
import java.util.*;
import java.util.Map.Entry;

import com.solab.iso8583.codecs.*;
import com.solab.iso8583.annotation.Iso8583;
import com.solab.iso8583.annotation.Iso8583Field;
import com.solab.iso8583.util.PojoUtils;

/** 
 * {@link MessageFactory}
 * 
 * @author dbs
 * @author extension based on work made by Enrique Zamudio Lopez
 */
public class MessageFactoryPojo<T extends IsoMessage> extends MessageFactory{

	public MessageFactoryPojo() {
	}
	
	/**
	 * Map retaining pojo fields type for each message template<br> key is
	 * template index value is a map of field index / pojo property definition
	 */
	protected Map<Integer, Map<Integer, IsoField<?>>> templateIsoFieldsMap;

	/**
	 * see {@link #templateIsoFieldsMap}
	 * 
	 * @return the templateIsoFieldsMap
	 */
	public Map<Integer, Map<Integer, IsoField<?>>> getTemplateIsoFieldsMap() {
		return templateIsoFieldsMap;
	}

	/**
	 * register a pojo annotated with Iso8583
	 * 
	 * @param clazz
	 *            Pojo class type to parse
	 * @return {@link IsoMessage} definition just added
	 */
	public T registerMessage(Class<?> clazz) {
		if(null == clazz){
			throw new IllegalArgumentException("pojo type is empty");
		}
		if (null == templateIsoFieldsMap)
			templateIsoFieldsMap = new TreeMap<>();
		T isoMessageTemplate = registerPojo(clazz);
		if(null == isoMessageTemplate){
			throw new IllegalArgumentException(clazz.getName() + " does not contain an @Iso8583 annotation to register a message type");
		}
		Map<Integer, IsoField<?>> isoFields = setIsoPojoFields(clazz, isoMessageTemplate, false);
		templateIsoFieldsMap.put(isoMessageTemplate.getType(), isoFields);
		return isoMessageTemplate;
	}

	/**
	 * parse a pojo class definition to create a message template definition<br>
	 * register headers if any<br>
	 * set binary type if set<br>
	 * set encoding if set
	 * 
	 * @param clazz
	 *            pojo class to register annotated with {@link Iso8583}
	 * @return new Message Template definition, may be null
	 */
	protected T registerPojo(final Class<?> clazz) {
		T isoMessageTemplate = null;
		Annotation annotation = clazz.getAnnotation(Iso8583.class);
		if (annotation instanceof Iso8583) {
			Iso8583 iso8583 = (Iso8583) annotation;
			isoMessageTemplate = (T) newMessage(iso8583.type());
			isoMessageTemplate.setBinary(iso8583.binary());
			if (iso8583.header() != null && !iso8583.header().isEmpty()) {
				if (iso8583.binary())
					isoMessageTemplate.setBinaryIsoHeader(iso8583.header().getBytes());
				else
					isoMessageTemplate.setIsoHeader(iso8583.header());
			}
			if (iso8583.encoding() != null && !iso8583.encoding().isEmpty()) {
				Charset charset = Charset.forName(iso8583.encoding());
				isoMessageTemplate.setCharacterEncoding(charset.name());
			}
			addMessageTemplate(isoMessageTemplate);
		}
		return isoMessageTemplate;
	}

	/**
	 * parse all pojo fields annotated, and annotated nested objects 
	 * 
	 * @param clazz pojo class to parse for annotated fields with {@link Iso8583Field}
	 * @param isoMessageTemplate message template to define fields from annotations
	 * @param nested <code>false</code> for primary field, <code>true</code> for nested
	 * @return {@link Map} never <code>null</code>, may be empty if no properties are
	 *         annotated
	 */
	protected Map<Integer, IsoField<?>> setIsoPojoFields(final Class<?> clazz, T isoMessageTemplate, boolean nested) {
		Field[] fields = PojoUtils.getAllDeclaredFields(clazz);
		Map<Integer, IsoField<?>> indexIsoFieldMap = new HashMap<>();
		Iso8583Field iso8583FieldAnno;
		for (Field field : fields) {
			iso8583FieldAnno = field.getAnnotation(Iso8583Field.class);
			if (null != iso8583FieldAnno) {
				IsoField<?> isoField = new IsoField<>(iso8583FieldAnno, field.getType());
				if (null == isoField.getName() || isoField.getName().isEmpty())
					isoField.setName(field.getName());
				isoField.setPropertyName(field.getName());
				if(field.getType().isEnum()){//special treatment for enums
					if(!iso8583FieldAnno.customField())
						throw new IllegalArgumentException(String.format("Enumeration if used as ISO8583 field, customField flag must be set to true and implement interface \"CustomBinaryField.class\" for %s",
								field.getType().getSimpleName()));
					CustomBinaryField<?> customBinaryField = defineCustomField(isoMessageTemplate, iso8583FieldAnno, isoField);
					if(!PojoUtils.isParameterizedAssignable(customBinaryField.getClass(), field.getType())){
						throw new IllegalArgumentException(String.format("Enumeration if used as ISO8583 field, \"CustomBinaryField.class\" must be paramaterized as your enum %s",
								field.getType().getSimpleName()));
					}
				} else if(iso8583FieldAnno.nestedField()){
					if(!field.getDeclaringClass().isPrimitive()){//check if is not a primitive
						Map<Integer, IsoField<?>> nestedIndexIsoFieldMap = setIsoPojoFields(field.getType(), isoMessageTemplate, true);
						CompositeFieldPojo compositeField = new CompositeFieldPojo();
						for (Entry<Integer, IsoField<?>> entry : nestedIndexIsoFieldMap.entrySet()) {
							//here the index does not have the same meaning, not used as a ISO 8583 unique index, 
							//used for ordering fields in the private L...VAR
							IsoField<?> nestedIsoField = entry.getValue();
							isoField.addNestedField(isoField.getPropertyName(), entry.getValue());
							compositeField.addValue(buildIsoField(nestedIsoField, nestedIsoField.getFieldClass(), null));
						}
						isoMessageTemplate.setField(iso8583FieldAnno.index(), new IsoValue<>(isoField.getIsoType(), isoField.getLength(), compositeField));
					} else {
						//blatant mis-configuration, we better notice the dev
						throw new IllegalArgumentException(String.format("A nested Field cannot be of a primitive type [%s]",
								field.getType().getSimpleName()));
					}
				} else if(iso8583FieldAnno.customField()){
					defineCustomField(isoMessageTemplate, iso8583FieldAnno, isoField);
				} else {
					if(!nested)
						defineField(isoField, field.getType(), isoMessageTemplate);
				}
				indexIsoFieldMap.put(iso8583FieldAnno.index(), isoField);
			}
		}
		return indexIsoFieldMap;
	}

	protected CustomBinaryField<?> defineCustomField(T isoMessageTemplate, Iso8583Field iso8583FieldAnno, IsoField<?> isoField) {
		isoField.setCustom(true);
		CustomBinaryField<?> customBinaryField = null;
		try {
			customBinaryField = iso8583FieldAnno.customFieldMapper().newInstance();
		} catch (InstantiationException | IllegalAccessException e) {
			//that fails often when a default constructor is not provided
			throw new IllegalArgumentException(String.format("Failed to instantiate a new CustomField derived class [%s], with message [%s]", 
					iso8583FieldAnno.customFieldMapper().getTypeName(), e.getMessage() ), e);
		}
		IsoValue<?> isoValue = buildIsoField(isoField, isoField.getFieldClass(), customBinaryField);
		isoMessageTemplate.setField(iso8583FieldAnno.index(), isoValue);
		setCustomField(iso8583FieldAnno.index(), customBinaryField);
		return customBinaryField;
	}
	
	/**
	 * build a message from a pojo instance<br>
	 * must be registered priorly with {@link #registerMessage(Class)}
	 * 
	 * @param instance
	 *            pojo instance to set values in
	 * @return the T message
	 * @throws IllegalArgumentException
	 */
	public T newMessage(Object instance) throws IllegalArgumentException {
		if (null != instance) {
			T isoMessage = null;
			Annotation annotation = instance.getClass().getAnnotation(Iso8583.class);
			int templateType = 0;
			if (null != annotation) {
				templateType = ((Iso8583) annotation).type();
				isoMessage = (T) getMessageTemplate(templateType);
			}
			if (templateType == 0 || null == annotation || null == templateIsoFieldsMap || templateIsoFieldsMap.get(templateType) == null) {
				// should happened only during hot debugging session
				// warning to developer, see example in
				// com.solab.iso8583.TestPojoIsoMessage#setup and other tests
				throw new IllegalArgumentException(String.format("Please register this class [%s] as ISO8583 POJO [%s]",
						instance.getClass().getSimpleName()));
			}
			Map<Integer, IsoField<?>> isoFields = templateIsoFieldsMap.get(templateType);
			for (IsoField<?> isoField : isoFields.values()) {
				//IsoField<?> isoField = entry.getValue();
				String propName = isoField.getPropertyName();
				Object value = PojoUtils.readField(instance, propName, isoField.getFieldClass());
				switch (isoField.index) {
				case 7:
					if(null == value && getAssignDate()){
						isoMessage.setValue(7, new Date(), IsoType.DATE10, 10);
						continue;
					}
					break;
				case 11:
					if(null == value && getTraceNumberGenerator() != null){
						isoMessage.setValue(11, getTraceNumberGenerator().nextTrace(), IsoType.NUMERIC, 6);
						continue;
					}
					break;
				default:
					break;
				}
				log.debug(isoField.toString());
				if(isoField.isNested()){
					Object objectCompositeField = isoMessage.getField(isoField.index).getEncoder();
					if(objectCompositeField instanceof CompositeFieldPojo){
						CompositeFieldPojo compositeField = (CompositeFieldPojo) objectCompositeField; 
						for (IsoField<?> nestedIsoField : isoField.getAllNestedField(isoField.getPropertyName())) {
							Object nestedValue = PojoUtils.readField(value, nestedIsoField.getPropertyName(), nestedIsoField.getFieldClass());
							compositeField.setField(nestedIsoField.index, 
									new IsoValue<>(nestedIsoField.getIsoType(), nestedValue, nestedIsoField.length));
						}
						isoMessage.setValue(isoField.index, compositeField, compositeField, isoField.getIsoType(), isoField.length);
					}
						
				} else if(isoField.isCustom()){
					Object objectCompositeField = isoMessage.getField(isoField.index).getEncoder();
					if(objectCompositeField instanceof CustomField){
						CustomField customField = (CustomField<?>) objectCompositeField;
						isoMessage.setField(isoField.index, 
								new IsoValue<>(isoField.getIsoType(), value, isoField.length, customField));
					}
						
				}else{
					isoMessage.setField(isoField.index, 
							new IsoValue<>(isoField.getIsoType(), value, isoField.length));
				}
			}
			return isoMessage;
		}
		return null;
	}

	/**
	 * used during pojo template registration
	 * 
	 * @param fieldDef
	 * @param clazz
	 * @param isoMessageTemplate
	 * @return isoMessageTemplate
	 */
	protected T defineField(IsoField<?> fieldDef, Class<?> clazz, T isoMessageTemplate) {
		if(null != fieldDef && null != clazz)
			isoMessageTemplate.setField(fieldDef.getIndex(), buildIsoField(fieldDef, clazz, null));
		return isoMessageTemplate;
	}
	
	@SuppressWarnings("unchecked")
	protected IsoValue<?> buildIsoField(final IsoField<?> fieldDef, Class<?> clazz, CustomField<?> customField){
		IsoValue<?> isoValue;
		switch (fieldDef.getIsoType()) {
		case ALPHA:
		case LLLLVAR:
		case LLVAR:
		case LLLVAR:
			CustomField<String> customFieldString = null;
			if(null != customField && PojoUtils.isParameterizedAssignable(customField.getClass(), String.class)){
			  customFieldString = (CustomField<String>) customField;
			}
			isoValue = new IsoValue<String>(fieldDef.getIsoType(), fieldDef.getLength(), customFieldString);
			break;
		case NUMERIC:
			if(null == customField)
				isoValue = new IsoValue<Number>(fieldDef.getIsoType(),	fieldDef.getLength(), new NumberCustomField());
			else
				isoValue = new IsoValue<>(fieldDef.getIsoType(), fieldDef.getLength(), customField);
			break;
		case BINARY:
		case LLBIN:
		case LLLBIN:
		case LLLLBIN:
			CustomField<Byte[]> customFieldByte = null;
			if(null != customField && PojoUtils.isParameterizedAssignable(customField.getClass(), Byte[].class)){
				customFieldByte = (CustomField<Byte[]>) customField;
			}
			isoValue = new IsoValue<Byte[]>(fieldDef.getIsoType(), fieldDef.getLength(), customFieldByte);
			break;
		case AMOUNT:
			isoValue = new IsoValue<BigDecimal>(fieldDef.getIsoType(), fieldDef.getIsoType().getLength(), new AmountCustomField());
			break;
		default:
			isoValue = new IsoValue<>(fieldDef.getIsoType(), fieldDef.getLength() == 0 ? fieldDef.getIsoType().getLength() : fieldDef.getLength(), customField);
			break;
		}
		return isoValue;
	}

	/**
	 * Convenience method for translating a {@link IsoMessage} to a registered
	 * pojo template<br>
	 * 
	 * @param isoMessage
	 *            iso message to convert
	 * @param clazz
	 *            pojo type to convert to
	 * @return pojo of type clazz, or null if parameters are not set
	 */
	public <P> P parseMessage(final T isoMessage, Class<P> clazz) {
		if (null == isoMessage || clazz == null)
			return null;
		P instance;
		try {
			instance = clazz.newInstance();
			Annotation annotation = instance.getClass().getAnnotation(Iso8583.class);
			int templateType = 0;
			T isoMessageTemplate = null;
			if (null != annotation) {
				templateType = ((Iso8583) annotation).type();
				isoMessageTemplate = (T) getMessageTemplate(templateType);
			}
			if (null == isoMessageTemplate || templateType == 0 || null == annotation || null == templateIsoFieldsMap) {
				// should happened only during hot debugging session
				// warning to developer, see example in
				// com.solab.iso8583.TestPojoIsoMessage#setup and others test
				throw new IllegalArgumentException(String.format("Please register this class [%s] as ISO8583 POJO ",
						instance.getClass().getSimpleName()));
			}
			if (isoMessageTemplate.getType() != isoMessage.getType())
				throw new IllegalArgumentException(
						String.format("iso message type %d and pojo template definition do not match, %s",
								isoMessage.getType(), clazz.getSimpleName()));
			Map<Integer, IsoField<?>> isoFields = templateIsoFieldsMap.get(templateType);
			for (Entry<Integer, IsoField<?>> entry : isoFields.entrySet()) {
				IsoField<?> isoField = entry.getValue();
				String propName = isoField.getPropertyName();
				if (isoMessage.hasField(isoField.getIndex())) {
					IsoValue<?> isoValue = isoMessage.getField(isoField.getIndex());
					if(isoField.isCustom()){
						Object objectCompositeField = isoMessage.getField(isoField.index).getEncoder();
						if(objectCompositeField instanceof CompositeFieldPojo){
							CompositeFieldPojo compositeField = (CompositeFieldPojo) objectCompositeField; 
							Object customValue = isoField.getValueSafeCast(compositeField.getField(1));
							PojoUtils.writeField(instance, isoField.getPropertyName(), isoField.getFieldClass(), customValue);
						}else{
							Object value = isoField.getValueSafeCast(isoValue);
							PojoUtils.writeField(instance, propName, isoField.getFieldClass(), value);
						}
					} else if(isoField.isNested()){
						Object objectCompositeField = isoMessage.getField(isoField.index).getEncoder();
						if(objectCompositeField instanceof CompositeFieldPojo){
							Object nestedInstance = isoField.getFieldClass().newInstance();//beware pojo must provide a default no-arg constructor
							CompositeFieldPojo compositeField = (CompositeFieldPojo) objectCompositeField; 
							for (IsoField<?> nestedIsoField : isoField.getAllNestedField(isoField.getPropertyName())) {
								Object nestedValue = nestedIsoField.getValueSafeCast(compositeField.getField(nestedIsoField.index));
								PojoUtils.writeField(nestedInstance, nestedIsoField.getPropertyName(), nestedIsoField.getFieldClass(), nestedValue);
							}
						}
					} else {
						Object value = isoField.getValueSafeCast(isoValue);
						PojoUtils.writeField(instance, propName, isoField.getFieldClass(), value);
					}
				}
			}
		} catch (InstantiationException | IllegalAccessException e) {
			log.trace("pojo parse message failed with error %s", e.getMessage());
			throw new IllegalArgumentException(e);
		}
		return instance;
	}
}
