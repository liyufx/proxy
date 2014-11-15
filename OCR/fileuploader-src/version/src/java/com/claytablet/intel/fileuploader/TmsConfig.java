package com.claytablet.intel.fileuploader;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;

import Sdl.Tms.Cta.BaseResponse;
import Sdl.Tms.Cta.ConfigRequest;
import Sdl.Tms.Cta.ConfigResponse;
import Sdl.Tms.Cta.CtaException;
import Sdl.Tms.Cta.PostInfo;
import Sdl.Tms.Cta.SystemError;

import com.thoughtworks.xstream.XStream;

public class TmsConfig {
	private String configName;
	private String tmsServer;
	private String configGUID;
	
	// default constructor for XStream
	public TmsConfig() {
	}
	
	protected TmsConfig(String configName, String tmsServer, String configGUID) {
		super();
		this.configName = configName.trim();
		this.tmsServer = tmsServer.trim();
		this.configGUID = configGUID.trim();
	}

	public String getConfigName() {
		if (configName==null) {
			return "";
		} else {
			return configName;
		}
	}

	public String getTmsServer() {
		return tmsServer;
	}

	public UUID getConfigGUID() {
		return UUID.fromString(configGUID);
	}

	public boolean isUseSSL() {
		return tmsServer.startsWith("https://");
	}

	PostInfo createPostInfo() throws CtaException {
		PostInfo postInfo = new PostInfo();
		postInfo.setPostUrl(getTmsServer());
		postInfo.setUseSsl(isUseSSL());
		return postInfo;
	}
}