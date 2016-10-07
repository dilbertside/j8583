/**
 * PojoUtils
 */
package com.solab.iso8583.util;

import java.lang.ref.Reference;
import java.lang.ref.SoftReference;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Collections;
import java.util.Map;
import java.util.WeakHashMap;

import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.reflect.FieldUtils;
import org.apache.commons.lang3.reflect.TypeUtils;

/**
 * Simple utility class for working with the reflection API<br>
 * Wrapper to Apache reflection utilities<br> 
 * this implementation add cache mechanism
 * Inspiration from Spring Core utilities<br>
 * @author dbs on Sep 24, 2016 8:36:21 AM
 * @version 1.0
 * @since V1.12.0
 *
 */
public class PojoUtils {

  /**
   * initial default size Fields cache (256) 
   */
  protected static int declaredFieldsCacheSize = 256;
  
  /**
   * Cache for {@link Class#getDeclaredFields()}, allowing for fast iteration.
   */
  private static Map<Class<?>, Reference<Field[]>> declaredFieldsCache = 
      Collections.synchronizedMap(new WeakHashMap<Class<?>, Reference<Field[]>>(declaredFieldsCacheSize));
  
  /**
   * Attempt to find a {@link Field field} on the supplied {@link Class} with the
   * supplied {@code name} and/or {@link Class type}. Searches all superclasses
   * up to {@link Object}.<br>
   * Make the field explicitly accessible if necessary
   * @param clazz the class to introspect
   * @param name the name of the field (may be {@code null} if type is specified)
   * @param type the type of the field (may be {@code null} if name is specified)
   * @return the corresponding Field object, or {@code null} if not found
   */
  protected static Field findField(Class<? extends Object> clazz, String name, Class<?> type) {
    Class<?> searchType = clazz;
    while (Object.class != searchType && searchType != null) {
      Field[] fields = getDeclaredFields(searchType);
      for (Field field : fields) {
        if ((name == null || name.equals(field.getName())) &&
            (type == null || type.equals(field.getType()))) {
          
          if ((!Modifier.isPublic(field.getModifiers()) ||
              !Modifier.isPublic(field.getDeclaringClass().getModifiers()) ||
              Modifier.isFinal(field.getModifiers())) && !field.isAccessible()) {
            field.setAccessible(true);
          }
          return field;
        }
      }
      searchType = searchType.getSuperclass();
    }
    return null;
  }

  /**
   * Read a field from a pojo instance<br>
   * Call effectively {@link FieldUtils#readField(Field, Object, boolean)} after caching the field
   * @param instance pojo instance
   * @param propName field name to read
   * @param type field target class to read
   * @return the field value
   * @throws IllegalArgumentException
   */
  @SuppressWarnings("unchecked")
  public static <T> T readField(Object instance, String propName, Class<T> type) throws IllegalArgumentException {
    T value = null;
    try {
      Field field = findField(instance.getClass(), propName, type);
      value = (T) FieldUtils.readField(field, instance, true);
    } catch (Exception e) {
      throw new IllegalArgumentException(String.format("Failed to read value for property [%s]", propName), e);
    }
    return value;
  }
  
  /**
   * 
   * @param instance pojo instance
   * @param propName field name to set
   * @param type field target class to set
   * @param value the value to set
   * @throws IllegalArgumentException
   */
  public static <T> void writeField(Object instance, String propName, Class<T> type, Object value) throws IllegalArgumentException {
    try {
      Field field = findField(instance.getClass(), propName, type);
      FieldUtils.writeField(field, instance, value, true);
    } catch (Exception e) {
      throw new IllegalArgumentException(String.format("Failed to read value for property [%s]", propName), e);
    }
  }

	/**
	 * This variant retrieves {@link Class#getDeclaredFields()} from a local cache
	 * in order to avoid the JVM's SecurityManager check and defensive array copying.
	 * @param clazz the class to introspect
	 * @return the cached array of fields
	 * @see Class#getDeclaredFields()
	 */
	protected static Field[] getDeclaredFields(Class<?> clazz) {
		Reference<Field[]> ref = declaredFieldsCache.get(clazz);
		if(null != ref && ref.get() != null)
			return ref.get();
		Field[] result = clazz.getDeclaredFields();
		ref = new SoftReference<Field[]>(result.length == 0 ? new Field[]{} : result);
		declaredFieldsCache.put(clazz, ref);
		return result;
	}

	/**
	 * This variant retrieves {@link Class#getDeclaredFields()} from a local cache
	 * in order to avoid the JVM's SecurityManager check and defensive array copying.
	 * @param clazz the class to introspect
	 * @return the cached array of fields
	 * @see #getDeclaredFields(Class)
	 */
	public static Field[] getAllDeclaredFields(Class<?> clazz) {
		Field[] fields = new Field[0];
		Class<?> searchType = clazz;
    while (Object.class != searchType && searchType != null) {
    	fields = (Field[]) ArrayUtils.addAll(fields, getDeclaredFields(searchType));
    	searchType = searchType.getSuperclass();
    }
    return fields;
	}
  /**
   * @return the declaredFieldsCacheSize
   */
  public static int getDeclaredFieldsCacheSize() {
    return declaredFieldsCacheSize;
  }

  /**
   * setter to change size of Fields cache<br>
   * Rebuild the map if cache size changes 
   * @param declaredFieldsCacheSizeNew the declaredFieldsCacheSize to set
   */
  public static void setDeclaredFieldsCacheSize(int declaredFieldsCacheSizeNew) {
    if(declaredFieldsCacheSize != declaredFieldsCacheSizeNew){
      declaredFieldsCacheSize = declaredFieldsCacheSizeNew;
      declaredFieldsCache.clear();
      declaredFieldsCache = null;//force GC notice
      declaredFieldsCache = 
          Collections.synchronizedMap(new WeakHashMap<Class<?>, Reference<Field[]>>(declaredFieldsCacheSize));
    }
  }

  /**
	 * search and detect if parameterized class is of type needed<br>
	 * Example this method will search if<br> 
	 * <code>class DefaultCustomStringField implements CustomBinaryField<String></code><br>
	 * has generic parameterized as String<br>
	 * the above to avoid cast which fails at runtime.
	 * @param parameterizedClass to search
	 * @param classParameterizedType target
	 * @return true 
	 */
	public static boolean isParameterizedAssignable(final Class<?> parameterizedClass, final Class<?> classParameterizedType) {
		ParameterizedType parameterizedType = extractParameterizedTypeFromInterface(parameterizedClass);
		if (parameterizedType == null) {
			//Class and any of its interfaces are not parameterized
			parameterizedType = extractParameterizedTypeFromClass(parameterizedClass);
			if (parameterizedType == null)
				return false;
		}
		Type[] typeArguments = parameterizedType.getActualTypeArguments();
		if(null != typeArguments && typeArguments.length >= 1 ){
			Class<?> typeArgumentClass = (Class<?>) typeArguments[0]; 
			return TypeUtils.isAssignable(typeArgumentClass, classParameterizedType);
		}
		return false;
	}
	
	private static ParameterizedType extractParameterizedTypeFromClass(Class<?> parameterizedClass) {
		Type genericSuperclass = parameterizedClass.getGenericSuperclass();
		if (genericSuperclass instanceof ParameterizedType) {
			return (ParameterizedType) genericSuperclass;
		}
		if (genericSuperclass != null) {
			return extractParameterizedTypeFromClass((Class<?>) genericSuperclass);
		}
		return null;
	}

	private static ParameterizedType extractParameterizedTypeFromInterface(Class<?> parameterizedClass) {
		Type[] genericInterfaces = parameterizedClass.getGenericInterfaces();
		for (Type genericInterface : genericInterfaces) {
			if (genericInterface instanceof ParameterizedType) {
				return (ParameterizedType) genericInterface;
			}
			ParameterizedType parameterizedType = extractParameterizedTypeFromInterface((Class<?>) genericInterface);
			if (parameterizedType != null) {
				return parameterizedType;
			}
		}
		return null;
	}
	
	/**
	 * wrapper around {@link TypeUtils#isAssignable(Type, Type)}<br>
	 * to avoid non necessary imports
	 * @param type the subject type to be assigned to the target type
     * @param toType the target type
     * @return {@code true} if {@code type} is assignable to {@code toType}.
	 */
	public static boolean isAssignable(final Class<?> type, final Class<?> toType) {
		return TypeUtils.isAssignable(type, toType);
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
	@SuppressWarnings({ "unchecked" })
	public static <T> T convertNumberToTargetClass(Number number, Class<T> targetClass)
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
}
