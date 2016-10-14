package com.solab.iso8583.parse;

import java.io.IOException;
import java.util.HashMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.solab.iso8583.IsoMessage;
import com.solab.iso8583.IsoType;
import com.solab.iso8583.IsoValue;
import com.solab.iso8583.MessageFactory;
import com.solab.iso8583.MessageFactoryPojo;
import com.solab.iso8583.codecs.CompositeFieldPojo;

/** This class is used to parse a XML configuration file and configure
 * a MessageFactory with the values from it.
 * 
 * @author dbs
 * @author extension based on work made by Enrique Zamudio Lopez
 */
public class ConfigParserPojo extends ConfigParser{

	private final static Logger log = LoggerFactory.getLogger(ConfigParserPojo.class);


	/**
	 * Creates a message factory configured from an array of pojo class definitions
	 * @param clazzz array of pojo to register, if empty return {@link #createDefault()} built message factory 
	 * @return {@link MessageFactory}
	 * @throws IOException
	 */
	public static MessageFactoryPojo<IsoMessage> createFromPojo(Class<?>... clazzz) throws IOException{
		if(null != clazzz && clazzz.length > 0){
			MessageFactoryPojo<IsoMessage> mfact = new MessageFactoryPojo<IsoMessage>();
			for (Class<?> clazz : clazzz) {
				IsoMessage isoMessage = mfact.registerMessage(clazz);
				//set the parsing guides
				parseGuides(isoMessage, mfact);
			}
			return mfact;
		}
		//returning empty message factory
		return new MessageFactoryPojo<IsoMessage>();
	}
	

	protected static <T extends IsoMessage> void parseGuides(final T isoMessage, final MessageFactoryPojo<T> mfact) {
		HashMap<Integer, FieldParseInfo> parseMap = new HashMap<>();
		for (int indexField : isoMessage.getAllFields()) {
			IsoValue<?> isoValue =  isoMessage.getField(indexField);
			FieldParseInfo fieldParseInfo = null;
			if(null != isoValue.getEncoder()){//nested field or custom field
				if(isoValue.getEncoder() instanceof CompositeFieldPojo){
					final CompositeFieldPojo combo = (CompositeFieldPojo) isoValue.getEncoder();
					for (IsoValue<?> isoValue2 : combo.getValues()) {
						combo.addParser(getParser(isoValue2, mfact));
					}
				}
			}
			fieldParseInfo = getParser(isoValue, mfact);
			parseMap.put(indexField, fieldParseInfo);
		}
		mfact.setParseMap(isoMessage.getType(), parseMap);
	}

	protected static <T extends IsoMessage> FieldParseInfo getParser(IsoValue<?> isoValue, MessageFactory<T> mfact) {
        IsoType itype = isoValue.getType();
        int length = 0;
        if (isoValue.getLength() > 0)
            length = isoValue.getLength();
        FieldParseInfo fpi = FieldParseInfo.getInstance(itype, length, mfact.getCharacterEncoding());
        fpi.setDecoder(isoValue.getEncoder());
        return fpi;
    }
}
