package com.claytablet.intel.fileuploader;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Locale;
import java.util.Map;

import org.apache.commons.io.FileUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.thoughtworks.xstream.XStream;

public class LanguageMapping {
	private static final String LANG_MAPPING_XML = "langs.xml";
	private static final String SRC_TARGET_SEPARATOR = ">";

	static LanguageMapping fromXml(File xml) throws IOException {
		return (LanguageMapping) getXStream().fromXML(FileUtils.readFileToString(xml));
	}
	
	private static XStream getXStream() {
		XStream xstream = new XStream();
		xstream.alias("language-mappings", LanguageMapping.class);

		return xstream;
	}

	public static LanguageMapping getLanguageMapping() throws IOException {
		if (!Home.getFile(LANG_MAPPING_XML).isFile()) {
			// load bundled resource file to the configuration location
			InputStream is = LanguageMapping.class.getClassLoader().getResourceAsStream(
					LANG_MAPPING_XML);
			FileOutputStream os = FileUtils.openOutputStream(Home.getFile(LANG_MAPPING_XML));
			Util.dumpStream(is, os);
			Util.closeStream(is);
			Util.closeStream(os);
		}
		return fromXml(Home.getFile(LANG_MAPPING_XML)).normalize();
	}
	
	private LanguageMapping normalize() {
		// convert all language code to upper-case
		Map<String, String> convertedMappings = new HashMap<String, String>(4);
		
		for (Map.Entry<String, String> entry : mappings.entrySet()) {
			convertedMappings.put(entry.getKey().toUpperCase(Locale.getDefault()), 
					entry.getValue().toUpperCase(Locale.getDefault()));
		}
		mappings.clear();
		mappings = new Hashtable<String, String>(convertedMappings);
		return this;
	}

	private Hashtable<String, String> mappings = new Hashtable<String, String>(4);

	public String getSourceLang(String langCode) {
		langCode = langCode.toUpperCase(Locale.getDefault());
		if (mappings.containsKey(langCode)) {
			return parseSourceLang(mappings.get(langCode));
		} else {
			return null;
		}
	}

	private String parseSourceLang(String lang) {
		int index = lang.indexOf(SRC_TARGET_SEPARATOR);
		if (index>0) {
			return lang.substring(0, index).trim();
		} else {
			return "EN-US";
		}
	}

	public String getTargetLang(String langCode) {
		langCode = langCode.toUpperCase(Locale.getDefault());
		if (mappings.containsKey(langCode)) {
			return parseTargetLang(mappings.get(langCode));
		} else {
			return null;
		}
	}

	private String parseTargetLang(String lang) {
		int index = lang.indexOf(SRC_TARGET_SEPARATOR);
		if (index>=0) {
			return lang.substring(index+1).trim();
		} else {
			return lang;
		}
	}
}
