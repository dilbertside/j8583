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
	public static boolean isAssignable(final Class<?> parameterizedClass, final Class<?> classParameterizedType) {
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
}
