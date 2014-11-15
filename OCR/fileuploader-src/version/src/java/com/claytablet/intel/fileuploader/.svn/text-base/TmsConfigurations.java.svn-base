package com.claytablet.intel.fileuploader;

import java.io.*;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.io.FileUtils;

import com.thoughtworks.xstream.XStream;

public class TmsConfigurations {
	private static final String TMS_CONFIGS_XML = "tmsconfigs.xml";

	static TmsConfigurations fromXml(File xml) throws IOException {
		TmsConfigurations tmsConfigs = new TmsConfigurations();
		tmsConfigs.configs.addAll(
				(ArrayList<TmsConfig>) getXStream().fromXML(FileUtils.readFileToString(xml)));
		return tmsConfigs;
	}
	
	static TmsConfigurations toXml(TmsConfigurations configs, File xml) throws IOException {
		FileUtils.writeStringToFile(xml, getXStream().toXML(configs.configs));
		return configs;
	}
	
	private static XStream getXStream() {
		XStream xstream = new XStream();
		xstream.alias("tms-config", TmsConfig.class);
		return xstream;
	}

	private ArrayList<TmsConfig> configs = new ArrayList<TmsConfig>();
	
	public static TmsConfigurations getInstance() throws IOException {
		File configFile = Home.getFile(TMS_CONFIGS_XML);
		if (!configFile.isFile()) {
			// load bundled resource file to the configuration location
			InputStream is = TmsConfigurations.class.getClassLoader().getResourceAsStream(
					TMS_CONFIGS_XML);
			FileOutputStream os = FileUtils.openOutputStream(Home.getFile(TMS_CONFIGS_XML));
			Util.dumpStream(is, os);
			Util.closeStream(is);
			Util.closeStream(os);
//			TmsConfigurations tmsConfigs = new TmsConfigurations();
//			tmsConfigs.configs.add(new TmsConfig("Test", "https://inteltms.sdlproducts.com",
//					"26191711-F347-40DA-918C-D6D0F3D5D979"));
//			return toXml(tmsConfigs, configFile);
		}
		return fromXml(configFile);
	}

	public Iterable<String> getConfigNames() {
		List<String> names = new ArrayList<String>();
		
		for (TmsConfig config : configs) {
			names.add(config.getConfigName());
		}
		return names;
	}
	
	public TmsConfig getConfig(String name) {
		for (TmsConfig config : configs) {
			if (config.getConfigName().equals(name)) {
				return config;
			}
		}
		return null;
	}
}
