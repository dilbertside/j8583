/*
j8583 A Java implementation of the ISO8583 protocol
Copyright (C) 2007 Enrique Zamudio Lopez

This library is free software; you can redistribute it and/or
modify it under the terms of the GNU Lesser General Public
License as published by the Free Software Foundation; either
version 2.1 of the License, or (at your option) any later version.

This library is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
Lesser General Public License for more details.

You should have received a copy of the GNU Lesser General Public
License along with this library; if not, write to the Free Software
Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA
*/
package com.solab.iso8583;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.nio.charset.Charset;
import java.text.ParseException;
import java.util.*;
import java.util.Map.Entry;

import com.solab.iso8583.parse.DateTimeParseInfo;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.solab.iso8583.IsoField.CompositeFieldPojo;
import com.solab.iso8583.annotation.Iso8583;
import com.solab.iso8583.annotation.Iso8583Field;
import com.solab.iso8583.parse.ConfigParser;
import com.solab.iso8583.parse.FieldParseInfo;
import com.solab.iso8583.util.PojoUtils;

/** This class is used to create messages, either from scratch or from an existing String or byte
 * buffer. It can be configured to put default values on newly created messages, and also to know
 * what to expect when reading messages from an InputStream.
 * <P>
 * The factory can be configured to know what values to set for newly created messages, both from
 * a template (useful for fields that must be set with the same value for EVERY message created)
 * and individually (for trace [field 11] and message date [field 7]).
 * <P>
 * It can also be configured to know what fields to expect in incoming messages (all possible values
 * must be stated, indicating the date type for each). This way the messages can be parsed from
 * a byte buffer.
 * 
 * @author Enrique Zamudio
 */
public class MessageFactory<T extends IsoMessage> {

	protected final Logger log = LoggerFactory.getLogger(getClass());

	/** This map stores the message template for each message type. */
	private Map<Integer, T> typeTemplates = new HashMap<>();
	/** Stores the information needed to parse messages sorted by type. */
	protected Map<Integer, Map<Integer, FieldParseInfo>> parseMap = new HashMap<>();
	/** Stores the field numbers to be parsed, in order of appearance. */
    protected Map<Integer, List<Integer>> parseOrder = new HashMap<>();

	private TraceNumberGenerator traceGen;
	/** The ISO header to be included in each message type. */
	private Map<Integer, String> isoHeaders = new HashMap<>();
    private Map<Integer, byte[]> binIsoHeaders = new HashMap<>();
	/** A map for the custom field encoder/decoders, keyed by field number. */
	@SuppressWarnings("rawtypes")
	private Map<Integer, CustomField> customFields = new HashMap<>();
	/** Indicates if the current date should be set on new messages (field 7). */
	private boolean setDate;
	/** Indicates if the factory should create binary messages and also parse binary messages. */
	private boolean useBinary;
	private int etx = -1;
	/** Flag to specify if missing fields should be ignored as long as they're at
	 * the end of the message. */
	private boolean ignoreLast;
	private boolean forceb2;
    private boolean binBitmap;
    private boolean forceStringEncoding;
	private String encoding = System.getProperty("file.encoding");

    /** This flag gets passed on to newly created messages and also sets this value for all
     * field parsers in parsing guides. */
    public void setForceStringEncoding(boolean flag) {
        forceStringEncoding = flag;
        for (Map<Integer,FieldParseInfo> pm : parseMap.values()) {
            for (FieldParseInfo parser : pm.values()) {
                parser.setForceStringDecoding(flag);
            }
        }
    }
    public boolean isForceStringEncoding() {
        return forceStringEncoding;
    }

    /** Tells the factory to create messages that encode their bitmaps in binary format
     * even when they're encoded as text. Has no effect on binary messages. */
    public void setUseBinaryBitmap(boolean flag) {
        binBitmap = flag;
    }
    /** Returns true if the factory is set to create and parse bitmaps in binary format
     * when the messages are encoded as text. */
    public boolean isUseBinaryBitmap() {
        return binBitmap;
    }

	/** Sets the character encoding used for parsing ALPHA, LLVAR and LLLVAR fields. */
	public void setCharacterEncoding(String value) {
        if (encoding == null) {
            throw new IllegalArgumentException("Cannot set null encoding.");
        }
		encoding = value;
		if (!parseMap.isEmpty()) {
			for (Map<Integer, FieldParseInfo> pt : parseMap.values()) {
				for (FieldParseInfo fpi : pt.values()) {
					fpi.setCharacterEncoding(encoding);
				}
			}
		}
        if (!typeTemplates.isEmpty()) {
            for (T tmpl : typeTemplates.values()) {
                tmpl.setCharacterEncoding(encoding);
                for (int i = 2 ; i<129; i++) {
                    IsoValue<?> v = tmpl.getField(i);
                    if (v != null) {
                        v.setCharacterEncoding(encoding);
                    }
                }
            }
        }
	}

	/** Returns the encoding used to parse ALPHA, LLVAR and LLLVAR fields. The default is the
	 * file.encoding system property. */
	public String getCharacterEncoding() {
		return encoding;
	}

	/** Sets or clears the flag to pass to new messages, to include a secondary bitmap
	 * even if it's not needed. */
	public void setForceSecondaryBitmap(boolean flag) {
		forceb2 = flag;
	}
	public boolean isForceSecondaryBitmap() {
		return forceb2;
	}

	/** Setting this property to true avoids getting a ParseException when parsing messages that don't have
	 * the last field specified in the bitmap. This is common with certain providers where field 128 is
	 * specified in the bitmap but not actually included in the messages. Default is false, which has
	 * been the behavior in previous versions when this option didn't exist. */
	public void setIgnoreLastMissingField(boolean flag) {
		ignoreLast = flag;
	}
	/** This flag indicates if the MessageFactory throws an exception if the last field of a message
	 * is not really present even though it's specified in the bitmap. Default is false which means
	 * an exception is thrown. */
	public boolean getIgnoreLastMissingField() {
		return ignoreLast;
	}

	/** Specifies a map for custom field encoder/decoders. The keys are the field numbers. */
	@SuppressWarnings("rawtypes")
	public void setCustomFields(Map<Integer, CustomField> value) {
		customFields = value;
	}

	/** Sets the CustomField encoder for the specified field number. */
	public void setCustomField(int index, CustomField<?> value) {
		customFields.put(index, value);
	}
	/** Returns a custom field encoder/decoder for the specified field number, if one is available. */
	@SuppressWarnings("unchecked")
	public <F> CustomField<F> getCustomField(int index) {
		return customFields.get(index);
	}
	/** Returns a custom field encoder/decoder for the specified field number, if one is available. */
	@SuppressWarnings("unchecked")
	public <F> CustomField<F> getCustomField(Integer index) {
		return customFields.get(index);
	}

	/** Tells the receiver to read the configuration at the specified path. This just calls
	 * ConfigParser.configureFromClasspathConfig() with itself and the specified path at arguments,
	 * but is really convenient in case the MessageFactory is being configured from within, say, Spring. */
	public void setConfigPath(String path) throws IOException {
		ConfigParser.configureFromClasspathConfig(this, path);
        //Now re-set some properties that need to be propagated down to the recently assigned objects
        setCharacterEncoding(encoding);
        setForceStringEncoding(forceStringEncoding);
	}

	/** Tells the receiver to create and parse binary messages if the flag is true.
	 * Default is false, that is, create and parse ASCII messages. */
	public void setUseBinaryMessages(boolean flag) {
		useBinary = flag;
	}
	/** Returns true is the factory is set to create and parse binary messages,
	 * false if it uses ASCII messages. Default is false. */
	public boolean getUseBinaryMessages() {
		return useBinary;
	}

	/** Sets the ETX character to be sent at the end of the message. This is optional and the
	 * default is -1, which means nothing should be sent as terminator.
	 * @param value The ASCII value of the ETX character or -1 to indicate no terminator should be used. */
	public void setEtx(int value) {
		etx = value;
	}
	public int getEtx() {
		return etx;
	}

	/** Creates a new message of the specified type, with optional trace and date values as well
	 * as any other values specified in a message template. If the factory is set to use binary
	 * messages, then the returned message will be written using binary coding.
	 * @param type The message type, for example 0x200, 0x400, etc. */
	public T newMessage(int type) {
		T m;
        if (binIsoHeaders.get(type) != null) {
            m = createIsoMessageWithBinaryHeader(binIsoHeaders.get(type));
        } else {
            m = createIsoMessage(isoHeaders.get(type));
        }
		m.setType(type);
		m.setEtx(etx);
		m.setBinary(useBinary);
		m.setForceSecondaryBitmap(forceb2);
        m.setBinaryBitmap(binBitmap);
		m.setCharacterEncoding(encoding);
        m.setForceStringEncoding(forceStringEncoding);

		//Copy the values from the template
		IsoMessage templ = typeTemplates.get(type);
		if (templ != null) {
			for (int i = 2; i <= 128; i++) {
				if (templ.hasField(i)) {
					//We could detect here if there's a custom object with a CustomField,
					//but we can't copy the value so there's no point.
					m.setField(i, templ.getField(i).clone());
				}
			}
		}
		if (traceGen != null) {
			m.setValue(11, traceGen.nextTrace(), IsoType.NUMERIC, 6);
		}
		if (setDate) {
			m.setValue(7, new Date(), IsoType.DATE10, 10);
		}
		return m;
	}

	/** Creates a message to respond to a request. Increments the message type by 16,
	 * sets all fields from the template if there is one, and copies all values from the request,
	 * overwriting fields from the template if they overlap.
	 * @param request An ISO8583 message with a request type (ending in 00). */
	public T createResponse(T request) {
		T resp = createIsoMessage(isoHeaders.get(request.getType() + 16));
		resp.setCharacterEncoding(request.getCharacterEncoding());
		resp.setBinary(request.isBinary());
        resp.setBinaryBitmap(request.isBinaryBitmap());
		resp.setType(request.getType() + 16);
		resp.setEtx(etx);
		resp.setForceSecondaryBitmap(forceb2);
		//Copy the values from the template or the request (request has preference)
		IsoMessage templ = typeTemplates.get(resp.getType());
		if (templ == null) {
			for (int i = 2; i < 128; i++) {
				if (request.hasField(i)) {
					resp.setField(i, request.getField(i).clone());
				}
			}
		} else {
			for (int i = 2; i < 128; i++) {
				if (request.hasField(i)) {
					resp.setField(i, request.getField(i).clone());
				} else if (templ.hasField(i)) {
					resp.setField(i, templ.getField(i).clone());
				}
			}
		}
		return resp;
	}

    /** Sets the timezone for the specified FieldParseInfo, if it's needed for parsing dates. */
    public void setTimezoneForParseGuide(int messageType, int field, TimeZone tz) {
        Map<Integer, FieldParseInfo> guide = parseMap.get(messageType);
        if (guide != null) {
            FieldParseInfo fpi = guide.get(field);
            if (fpi instanceof DateTimeParseInfo) {
                ((DateTimeParseInfo) fpi).setTimeZone(tz);
                return;
            }
        }
        log.warn("Field {} for message type {} is not for dates, cannot set timezone",
                field, messageType);
    }

    /** Convenience for parseMessage(buf, isoHeaderLength, false) */
    public T parseMessage(byte[] buf, int isoHeaderLength)
           	throws ParseException, UnsupportedEncodingException {
        return parseMessage(buf, isoHeaderLength, false);
    }

	/** Creates a new message instance from the buffer, which must contain a valid ISO8583
	 * message. If the factory is set to use binary messages then it will try to parse
	 * a binary message.
	 * @param buf The byte buffer containing the message. Must not include the length header.
	 * @param isoHeaderLength The expected length of the ISO header, after which the message type
	 * and the rest of the message must come. */
	public T parseMessage(byte[] buf, int isoHeaderLength, boolean binaryIsoHeader)
        	throws ParseException, UnsupportedEncodingException {
		final int minlength = isoHeaderLength+(useBinary?2:4)+(binBitmap||useBinary ? 8:16);
		if (buf.length < minlength) {
			throw new ParseException("Insufficient buffer length, needs to be at least " + minlength, 0);
		}
		final T m;
        if (binaryIsoHeader && isoHeaderLength > 0) {
            byte[] _bih = new byte[isoHeaderLength];
            System.arraycopy(buf, 0, _bih, 0, isoHeaderLength);
            m = createIsoMessageWithBinaryHeader(_bih);
        } else {
            m = createIsoMessage(isoHeaderLength > 0 ?
    				new String(buf, 0, isoHeaderLength, encoding) : null);
        }
		m.setCharacterEncoding(encoding);
		final int type;
		if (useBinary) {
			type = ((buf[isoHeaderLength] & 0xff) << 8) | (buf[isoHeaderLength + 1] & 0xff);
        } else if (forceStringEncoding) {
            type = Integer.parseInt(new String(buf, isoHeaderLength, 4, encoding), 16);
		} else {
			type = ((buf[isoHeaderLength] - 48) << 12)
			| ((buf[isoHeaderLength + 1] - 48) << 8)
			| ((buf[isoHeaderLength + 2] - 48) << 4)
			| (buf[isoHeaderLength + 3] - 48);
		}
		m.setType(type);
		//Parse the bitmap (primary first)
		final BitSet bs = new BitSet(64);
		int pos = 0;
		if (useBinary || binBitmap) {
            final int bitmapStart = isoHeaderLength + (useBinary ? 2 : 4);
			for (int i = bitmapStart; i < 8+bitmapStart; i++) {
				int bit = 128;
				for (int b = 0; b < 8; b++) {
					bs.set(pos++, (buf[i] & bit) != 0);
					bit >>= 1;
				}
			}
			//Check for secondary bitmap and parse if necessary
			if (bs.get(0)) {
				if (buf.length < minlength + 8) {
					throw new ParseException("Insufficient length for secondary bitmap", minlength);
				}
				for (int i = 8+bitmapStart; i < 16+bitmapStart; i++) {
					int bit = 128;
					for (int b = 0; b < 8; b++) {
						bs.set(pos++, (buf[i] & bit) != 0);
						bit >>= 1;
					}
				}
				pos = minlength + 8;
			} else {
				pos = minlength;
			}
		} else {
			//ASCII parsing
			try {
                final byte[] bitmapBuffer;
                if (forceStringEncoding) {
                    byte[] _bb = new String(buf, isoHeaderLength+4, 16, encoding).getBytes();
                    bitmapBuffer = new byte[36+isoHeaderLength];
                    System.arraycopy(_bb, 0, bitmapBuffer, 4+isoHeaderLength, 16);
                } else {
                    bitmapBuffer = buf;
                }
                for (int i = isoHeaderLength + 4; i < isoHeaderLength + 20; i++) {
                    if (bitmapBuffer[i] >= '0' && bitmapBuffer[i] <= '9') {
                        bs.set(pos++, ((bitmapBuffer[i] - 48) & 8) > 0);
                        bs.set(pos++, ((bitmapBuffer[i] - 48) & 4) > 0);
                        bs.set(pos++, ((bitmapBuffer[i] - 48) & 2) > 0);
                        bs.set(pos++, ((bitmapBuffer[i] - 48) & 1) > 0);
                    } else if (bitmapBuffer[i] >= 'A' && bitmapBuffer[i] <= 'F') {
                        bs.set(pos++, ((bitmapBuffer[i] - 55) & 8) > 0);
                        bs.set(pos++, ((bitmapBuffer[i] - 55) & 4) > 0);
                        bs.set(pos++, ((bitmapBuffer[i] - 55) & 2) > 0);
                        bs.set(pos++, ((bitmapBuffer[i] - 55) & 1) > 0);
                    } else if (bitmapBuffer[i] >= 'a' && bitmapBuffer[i] <= 'f') {
                        bs.set(pos++, ((bitmapBuffer[i] - 87) & 8) > 0);
                        bs.set(pos++, ((bitmapBuffer[i] - 87) & 4) > 0);
                        bs.set(pos++, ((bitmapBuffer[i] - 87) & 2) > 0);
                        bs.set(pos++, ((bitmapBuffer[i] - 87) & 1) > 0);
                    }
                }
				//Check for secondary bitmap and parse it if necessary
				if (bs.get(0)) {
					if (buf.length < minlength + 16) {
						throw new ParseException("Insufficient length for secondary bitmap", minlength);
					}
                    if (forceStringEncoding) {
                        byte[] _bb = new String(buf, isoHeaderLength+20, 16, encoding).getBytes();
                        System.arraycopy(_bb, 0, bitmapBuffer, 20+isoHeaderLength, 16);
                    }
					for (int i = isoHeaderLength + 20; i < isoHeaderLength + 36; i++) {
						if (bitmapBuffer[i] >= '0' && bitmapBuffer[i] <= '9') {
							bs.set(pos++, ((bitmapBuffer[i] - 48) & 8) > 0);
							bs.set(pos++, ((bitmapBuffer[i] - 48) & 4) > 0);
							bs.set(pos++, ((bitmapBuffer[i] - 48) & 2) > 0);
							bs.set(pos++, ((bitmapBuffer[i] - 48) & 1) > 0);
						} else if (bitmapBuffer[i] >= 'A' && bitmapBuffer[i] <= 'F') {
							bs.set(pos++, ((bitmapBuffer[i] - 55) & 8) > 0);
							bs.set(pos++, ((bitmapBuffer[i] - 55) & 4) > 0);
							bs.set(pos++, ((bitmapBuffer[i] - 55) & 2) > 0);
							bs.set(pos++, ((bitmapBuffer[i] - 55) & 1) > 0);
						} else if (bitmapBuffer[i] >= 'a' && bitmapBuffer[i] <= 'f') {
							bs.set(pos++, ((bitmapBuffer[i] - 87) & 8) > 0);
							bs.set(pos++, ((bitmapBuffer[i] - 87) & 4) > 0);
							bs.set(pos++, ((bitmapBuffer[i] - 87) & 2) > 0);
							bs.set(pos++, ((bitmapBuffer[i] - 87) & 1) > 0);
						}
					}
					pos = 16 + minlength;
				} else {
					pos = minlength;
				}
			} catch (NumberFormatException ex) {
				ParseException _e = new ParseException("Invalid ISO8583 bitmap", pos);
				_e.initCause(ex);
				throw _e;
			}
		}
		//Parse each field
		Map<Integer, FieldParseInfo> parseGuide = parseMap.get(type);
		List<Integer> index = parseOrder.get(type);
		if (index == null) {
			log.error(String.format("ISO8583 MessageFactory has no parsing guide for message type %04x [%s]",
				type, new String(buf)));
			throw new ParseException(String.format(
					"ISO8583 MessageFactory has no parsing guide for message type %04x [%s]",
					type,
					new String(buf)), 0);
		}
		//First we check if the message contains fields not specified in the parsing template
		boolean abandon = false;
		for (int i = 1; i < bs.length(); i++) {
			if (bs.get(i) && !index.contains(i+1)) {
				log.warn("ISO8583 MessageFactory cannot parse field {}: unspecified in parsing guide", i+1);
				abandon = true;
			}
		}
		if (abandon) {
			throw new ParseException("ISO8583 MessageFactory cannot parse fields", 0);
		}
		//Now we parse each field
		if (useBinary) {
			for (Integer i : index) {
				FieldParseInfo fpi = parseGuide.get(i);
				if (bs.get(i - 1)) {
					if (ignoreLast && pos >= buf.length && i.intValue() == index.get(index.size() -1)) {
						log.warn("Field {} is not really in the message even though it's in the bitmap", i);
						bs.clear(i - 1);
					} else {
                        CustomField<?> decoder = fpi.getDecoder();
                        if (decoder == null) {
                            decoder = getCustomField(i);
                        }
						IsoValue<?> val = fpi.parseBinary(i, buf, pos, decoder);
						m.setField(i, val);
						if (val != null) {
							if (val.getType() == IsoType.NUMERIC || val.getType() == IsoType.DATE10
									|| val.getType() == IsoType.DATE4 || val.getType() == IsoType.DATE12
									|| val.getType() == IsoType.DATE_EXP
									|| val.getType() == IsoType.AMOUNT
									|| val.getType() == IsoType.TIME) {
								pos += (val.getLength() / 2) + (val.getLength() % 2);
							} else {
								pos += val.getLength();
							}
							if (val.getType() == IsoType.LLVAR || val.getType() == IsoType.LLBIN) {
								pos++;
							} else if (val.getType() == IsoType.LLLVAR
									|| val.getType() == IsoType.LLLBIN
                                    || val.getType() == IsoType.LLLLVAR
									|| val.getType() == IsoType.LLLLBIN) {
                                pos += 2;
                            }
						}
					}
				}
			}
		} else {
			for (Integer i : index) {
				FieldParseInfo fpi = parseGuide.get(i);
				if (bs.get(i - 1)) {
					if (ignoreLast && pos >= buf.length && i.intValue() == index.get(index.size() -1)) {
						log.warn("Field {} is not really in the message even though it's in the bitmap", i);
						bs.clear(i - 1);
					} else {
                        CustomField<?> decoder = fpi.getDecoder();
                        if (decoder == null) {
                            decoder = getCustomField(i);
                        }
						IsoValue<?> val = fpi.parse(i, buf, pos, decoder);
						m.setField(i, val);
						//To get the correct next position, we need to get the number of bytes, not chars
						pos += val.toString().getBytes(fpi.getCharacterEncoding()).length;
						if (val.getType() == IsoType.LLVAR || val.getType() == IsoType.LLBIN) {
							pos += 2;
						} else if (val.getType() == IsoType.LLLVAR || val.getType() == IsoType.LLLBIN) {
							pos += 3;
						} else if (val.getType() == IsoType.LLLLVAR || val.getType() == IsoType.LLLLBIN) {
                            pos += 4;
                        }
					}
				}
			}
		}
		m.setBinary(useBinary);
        m.setBinaryBitmap(binBitmap);
		return m;
	}

	/** Creates a Iso message, override this method in the subclass to provide your 
	 * own implementations of IsoMessage.
	 * @param header The optional ISO header that goes before the message type
	 * @return IsoMessage
	 */
    @SuppressWarnings("unchecked")
	protected T createIsoMessage(String header) {
        return (T)new IsoMessage(header);
	}

    /** Creates a Iso message with the specified binary ISO header.
     * Override this method in the subclass to provide your
   	 * own implementations of IsoMessage.
   	 * @param binHeader The optional ISO header that goes before the message type
   	 * @return IsoMessage
   	 */
    @SuppressWarnings("unchecked")
    protected T createIsoMessageWithBinaryHeader(byte[] binHeader) {
        return (T)new IsoMessage(binHeader);
    }

    /** Sets whether the factory should set the current date on newly created messages,
	 * in field 7. Default is false. */
	public void setAssignDate(boolean flag) {
		setDate = flag;
	}
	/** Returns true if the factory is assigning the current date to newly created messages
	 * (field 7). Default is false. */
	public boolean getAssignDate() {
		return setDate;
	}

	/** Sets the generator that this factory will get new trace numbers from. There is no
	 * default generator. */
	public void setTraceNumberGenerator(TraceNumberGenerator value) {
		traceGen = value;
	}
	/** Returns the generator used to assign trace numbers to new messages. */
	public TraceNumberGenerator getTraceNumberGenerator() {
		return traceGen;
	}

	/** Sets the ISO header to be used in each message type.
	 * @param value A map where the keys are the message types and the values are the ISO headers.
	 */
	public void setIsoHeaders(Map<Integer, String> value) {
		isoHeaders.clear();
		isoHeaders.putAll(value);
	}

	/** Sets the ISO header for a specific message type.
	 * @param type The message type, for example 0x200.
	 * @param value The ISO header, or NULL to remove any headers for this message type. */
	public void setIsoHeader(int type, String value) {
		if (value == null) {
			isoHeaders.remove(type);
		} else {
			isoHeaders.put(type, value);
            binIsoHeaders.remove(type);
		}
	}

	/** Returns the ISO header used for the specified type. */
	public String getIsoHeader(int type) {
		return isoHeaders.get(type);
	}

    /** Sets the ISO header for a specific message type, in binary format.
   	 * @param type The message type, for example 0x200.
   	 * @param value The ISO header, or NULL to remove any headers for this message type. */
    public void setBinaryIsoHeader(int type, byte[] value) {
        if (value == null) {
            binIsoHeaders.remove(type);
        } else {
            binIsoHeaders.put(type, value);
            isoHeaders.remove(type);
        }
    }
    /** Returns the binary ISO header used for the specified type. */
    public byte[] getBinaryIsoHeader(int type) {
        return binIsoHeaders.get(type);
    }

	/** Adds a message template to the factory. If there was a template for the same
	 * message type as the new one, it is overwritten. */
	public void addMessageTemplate(T templ) {
		if (templ != null) {
			typeTemplates.put(templ.getType(), templ);
		}
	}

	/** Removes the message template for the specified type. */
	public void removeMessageTemplate(int type) {
		typeTemplates.remove(type);
	}

	/** Returns the template for the specified message type. This allows templates to be modified
	 * programmatically. */
	public T getMessageTemplate(int type) {
		return typeTemplates.get(type);
	}

	/** Invoke this method in case you want to freeze the configuration, making message and parsing
	 * templates, as well as iso headers and custom fields, immutable. */
	public void freeze() {
		typeTemplates = Collections.unmodifiableMap(typeTemplates);
		parseMap = Collections.unmodifiableMap(parseMap);
		parseOrder = Collections.unmodifiableMap(parseOrder);
		isoHeaders = Collections.unmodifiableMap(isoHeaders);
        binIsoHeaders = Collections.unmodifiableMap(binIsoHeaders);
		customFields = Collections.unmodifiableMap(customFields);
	}

	/** Sets a map with the fields that are to be expected when parsing a certain type of
	 * message.
	 * @param type The message type.
	 * @param map A map of FieldParseInfo instances, each of which define what type and length
	 * of field to expect. The keys will be the field numbers. */
	public void setParseMap(int type, Map<Integer, FieldParseInfo> map) {
		parseMap.put(type, map);
		ArrayList<Integer> index = new ArrayList<>();
		index.addAll(map.keySet());
		Collections.sort(index);
		log.trace(String.format("ISO8583 MessageFactory adding parse map for type %04x with fields %s",
				type, index));
		parseOrder.put(type, index);
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
			isoMessageTemplate = newMessage(iso8583.type());
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
				isoMessage = getMessageTemplate(templateType);
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
					if(null == value && setDate){
						isoMessage.setValue(7, new Date(), IsoType.DATE10, 10);
						continue;
					}
					break;
				case 11:
					if(null == value && traceGen != null){
						isoMessage.setValue(11, traceGen.nextTrace(), IsoType.NUMERIC, 6);
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
				isoValue = new IsoValue<Number>(fieldDef.getIsoType(),	fieldDef.getLength(), new IsoField.NumberCustomField());
			else
				isoValue = new IsoValue<>(fieldDef.getIsoType(), fieldDef.getLength(), customField);
			break;
		case BINARY:
		case LLBIN:
		case LLLBIN:
		case LLLLBIN:
			CustomField<Byte[]> customFieldByte = null;
			if(null != customField && PojoUtils.isParameterizedAssignable(customField.getClass(), Byte[].class)){
			  customFieldString = (CustomField<String>) customField;
			}
			isoValue = new IsoValue<Byte[]>(fieldDef.getIsoType(), fieldDef.getLength(), customFieldByte);
			break;
		case AMOUNT:
			isoValue = new IsoValue<BigDecimal>(fieldDef.getIsoType(), fieldDef.getIsoType().getLength(), new IsoField.AmountCustomField());
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
				isoMessageTemplate = getMessageTemplate(templateType);
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
