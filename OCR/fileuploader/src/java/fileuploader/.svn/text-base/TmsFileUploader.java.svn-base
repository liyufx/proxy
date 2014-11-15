package com.claytablet.intel.fileuploader;

import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.zip.ZipInputStream;

import org.apache.commons.fileupload.*;
import org.apache.commons.fileupload.servlet.ServletFileUpload;
import org.apache.commons.fileupload.disk.DiskFileItemFactory;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import Sdl.Tms.Cta.BaseResponse;
import Sdl.Tms.Cta.ConfigRequest;
import Sdl.Tms.Cta.ConfigResponse;
import Sdl.Tms.Cta.CtaException;
import Sdl.Tms.Cta.Document;
import Sdl.Tms.Cta.ItemRequest;
import Sdl.Tms.Cta.JobRequest;
import Sdl.Tms.Cta.LanguageRequest;
import Sdl.Tms.Cta.SystemError;
import Sdl.Tms.Cta.TargetLanguage;

import java.util.*;
import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;

import javax.servlet.http.HttpServletRequest;

public class TmsFileUploader implements Runnable {
	private static Log log = LogFactory.getLog(TmsFileUploader.class);
	
	private final static Map<String, TmsFileUploader> sActiveJobs = 
			new ConcurrentHashMap<String, TmsFileUploader>();
	private final static AtomicInteger sJobIdSeq = new AtomicInteger(0);
	private final String id;
	private final HttpServletRequest request;

	private final AtomicInteger progress = new AtomicInteger(0);
	private Status status = Status.UPLOAD_IN_PROGESS;
	private String error = null;
	private String name;
	private File dest;
	private int totalFileCount = 0;
	private int skippedFileCount = 0;

	private final List<Object> warnings = new ArrayList<Object>();

	private String jobName;
	private String jobDescription;
	private String dueDate;
	private String tmsConfigName;

	public static TmsFileUploader upload(HttpServletRequest request) {
		ServletFileUpload.isMultipartContent(request);
		TmsFileUploader job = new TmsFileUploader(
				String.valueOf(sJobIdSeq.incrementAndGet()), request);
		sActiveJobs.put(job.getId(), job);
		
//		sThreadPool.execute(job);
		return job;
	}

	public static TmsFileUploader getJob(String id) {
		return sActiveJobs.get(id);
	}

	private TmsFileUploader(String id, HttpServletRequest request) {
		assert ServletFileUpload.isMultipartContent(request);
		this.id = id;
		this.request = request;
	}

	public String getId() {
		return id;
	}

	public String getName() {
		return name;
	}
	
	@Override
	public String toString() {
		return name;
	}

	public Status getStatus() {
		return status;
	}

	public String getStatusString() {
		switch (status) {
		case UPLOAD_IN_PROGESS:
			return name + " is being uploaded (" + status.getProgress(progress) + "%)";
		case SUBMITTING_TO_TMS:
			return name + " is being submitted to TMS configuration " + tmsConfigName + 
					" (" + status.getProgress(progress) + "%)";
		case COMPLETED: 
			return name + " has been submitted to TMS configuration " + tmsConfigName + " successfully";
		case PARTIALLY_COMPLETED:
			return name + " has been submitted to TMS configuration " + tmsConfigName + ", but " + 
					skippedFileCount + " out of " + totalFileCount + " contained files were not submitted";
		case ERROR:
			return "Failed to submit " + name + " to TMS configuration " + tmsConfigName + ". " + error;
		default:
			return "Unknonw status";
		}
	}

	public Status upload() {
		run();
		return status;
	}
	
	@Override
	public void run() {
		try {
			Collection<File> exportDirs = uploadFile();
			if (!exportDirs.isEmpty()) {
				status = Status.SUBMITTING_TO_TMS;
				submitToTms(exportDirs);
				if (status!=Status.ERROR) {
					status = skippedFileCount==0? Status.COMPLETED : Status.PARTIALLY_COMPLETED;
				}
			} else {
				setError("uploaded ZIP file does not contain export directory to submit");
			}
		} catch (Throwable e) {
			log.error("Failed to submit job " + jobName, e);
			setError(e);
		} finally {
			if (dest!=null && dest.isDirectory()) {
				// delete tmp file
				try {
					log.info("Cleaning up " + dest);
					FileUtils.deleteDirectory(dest);
				} catch (IOException e) {
					log.warn("Ignore clean up error:", e);
				}
			}
		}
	}

	private void setError(Throwable e) {
		error = "Error: " + String.valueOf(e);
		addWarning(error);
		status = Status.ERROR;
	}

	private void setError(String msg) {
		error = "Error: " + msg;
		addWarning(error);
		status = Status.ERROR;
	}

	private void addWarning(Throwable e) {
		warnings.add(e);
	}

	private void addWarning(String warning) {
		warnings.add(warning);
	}

	private Collection<File> uploadFile() throws Exception {
		ServletFileUpload servletFileUpload = new ServletFileUpload(
				new DiskFileItemFactory());
		List fileItemsList = servletFileUpload.parseRequest(request);

		String optionalFileName = "";
		FileItem fileItem = null;

		Iterator it = fileItemsList.iterator();
		while (it.hasNext()) {
			FileItem fileItemTemp = (FileItem) it.next();
			if (fileItemTemp.isFormField()) {
				String fieldName = fileItemTemp.getFieldName();
				if (fieldName.equals("filename")) {
					optionalFileName = fileItemTemp.getString();
				} else if (fieldName.equals("tmsconfig")) {
					tmsConfigName = fileItemTemp.getString();
				} else if (fieldName.equals("jobname")) {
					jobName = fileItemTemp.getString();
				} else if (fieldName.equals("duedate")) {
					dueDate = fileItemTemp.getString();
				} else if (fieldName.equals("jobdescription")) {
					jobDescription = fileItemTemp.getString();
				}
			} else
				fileItem = fileItemTemp;
		}

		if (fileItem != null) {
			String fileName = fileItem.getName();
			if (fileItem.getSize() > 0) {
				if (optionalFileName.trim().equals(""))
					fileName = FilenameUtils.getName(fileName);
				else
					fileName = optionalFileName;
				
				this.name = fileName;
				this.dest = new File(Home.getFile("tmp"), id+"_" + fileName);
				for (int prefix=0; prefix<100; prefix++) {
					// never overwrite existing files
					if (dest.exists()) {
						this.dest = new File(Home.getFile("tmp"), id+"_" + prefix + "_" + fileName);
					} else {
						break;
					}
				}
				if (dest.exists()) {
					// never overwrite existing files for security reasons
					throw new IOException("Cannot find a unique location to store " + name + 
							"temporarily, please wait a while and try again" );
				}
				log.info("Uploading " + this.name + " to " + this.dest);
				if (dest.mkdirs()) {
					ZipInputStream zis = new ZipInputStream(fileItem.getInputStream());
					Util.unzip(zis, dest);
				} else {
					throw new IOException("Cannot create directory " + dest);
				}
				Collection<File> exportDirs = new ArrayList<File>();
				searchForExports(dest, exportDirs);
				return exportDirs;
			}
		}
		// if we get here, we either don't have a
		throw new IOException("Cannot process empty upload request");
	}

	private Collection<File> searchForExports(File dir, Collection<File> exportDirs) {
		File[] files = dir.listFiles();
		
		for (File file : files) {
			if (file.isDirectory()) {
				if (file.getName().equalsIgnoreCase("export")) {
					// found an export dir
					exportDirs.add(file);
				} else {
					// continue to search in sub-dir
					searchForExports(file, exportDirs);
				}
			} else {
				log.debug("Ignore file outside of export directory: " + file);
			}
		}
		return exportDirs;
	}

	private void submitToTms(Collection<File> exportDirs) throws Exception {
		log.info("Submitting " + name + " to " + tmsConfigName + 
				" under the job name " + jobName + " (due date=" + dueDate + ")");
		TmsConfig tmsConfig = TmsConfigurations.getInstance().getConfig(tmsConfigName);
		
		ConfigResponse configResponse = validateLanguageSupport(tmsConfig);
		
		Set<String> targetLangs = new HashSet<String>();
		for (TargetLanguage targetLanguage : configResponse.getTargetLanguages()) {
			targetLangs.add(targetLanguage.getLanguage());
		}
		
		String manifestTargetLang = null;
		
		JobRequest jobRequest = new JobRequest();
		
		jobRequest.setClientId(tmsConfig.getConfigGUID());
		jobRequest.setJobRequestId(UUID.randomUUID().toString());
		jobRequest.setJobDescription(getJobDescription());
		
		LanguageMapping langMapping = LanguageMapping.getLanguageMapping();
		for (File exportDir: exportDirs) {
			log.info("Appending content of " + exportDir + "to job " + jobName);
			File langs[] = exportDir.listFiles();
			for (File lang : langs) {
				if (lang.isDirectory()) {
					int fileCount = countFile(lang);
					totalFileCount += fileCount;
					
					String langCode = lang.getName();
					String srcLang = langMapping.getSourceLang(langCode);
					String targetLang = langMapping.getTargetLang(langCode);
					if (StringUtils.isEmpty(srcLang) || StringUtils.isEmpty(targetLang)) {
						skippedFileCount += fileCount;
						addWarning(langCode + " is an invalid language code, " + 
								fileCount + " files under " + Util.getRelativePath(lang, dest) + 
								" will not be submitted");
					} else if (!configResponse.getLanguage().equals(srcLang)){
						skippedFileCount += fileCount;
						addWarning("Source language " + srcLang + 
								" does not match source language of the TMS configuration, " + 
								langCode + " is not a supported language code, " + 
								fileCount + " files under " + Util.getRelativePath(lang, dest) + 
								" will not be submitted");
					} else if (!targetLangs.contains(targetLang)) {
						skippedFileCount += fileCount;
						addWarning("Target language " + targetLang + 
								" is not among supported target lanugauges of the TMS configuration, " + 
								langCode + " is not a supported language code, " + 
								fileCount + " files under " + Util.getRelativePath(lang, dest) + 
								" will not be submitted");
					} else { 
						try {
							addItemRequests(jobRequest, lang, exportDir, srcLang, targetLang);
							manifestTargetLang = targetLang;
						} catch (Exception e) {
							skippedFileCount += fileCount;
							addWarning("Error while appending to the job, " + 
									fileCount + " files under " + Util.getRelativePath(lang, dest) + 
									" will not be submitted : " + String.valueOf(e));
							addWarning(e);

						}
					}
				} else {
					log.debug("Ignore file outside of export language directory: " + lang);
				}
			}
		}
		
		if (totalFileCount>skippedFileCount) {
			// creating and append manifest
			jobRequest.add(createItemRequest(new ManifestFactory().createManifest(), dest,
					configResponse.getLanguage(), manifestTargetLang));
			
			log.info("Submitting job " + jobName + " to TMS configuration " +tmsConfigName);
			BaseResponse response = jobRequest.Post(tmsConfig.createPostInfo());
			
			if (isSuccess(response)) {
				String msg = name + " has been submitted to TMS successfully";
				checkCtaResponse(msg, response);
			} else {
				String msg = "Failed to submit " + name + " to TMS";
				checkCtaResponse(msg, response);
			}
		} else {
			setError("no file to submit to TMS");
		}
	}

	private String getJobDescription() {
		if (StringUtils.isEmpty(jobDescription)) {
			return name + " submitted via Intel TMS file uploader";
		} else {
			return jobDescription;
		}
	}

	private void addItemRequests(JobRequest jobRequest, 
			File dir, File base, String srcLang, String targetLang) throws IOException {
		File[] files = dir.listFiles();
		
		for (File file : files) {
			if (file.isDirectory()) {
				addItemRequests(jobRequest, file, base, srcLang, targetLang);
			} else {
				jobRequest.add(createItemRequest(file, base, srcLang, targetLang));
			}
		}
	}

	private ItemRequest createItemRequest(File itemFile, File baseDir, String srcLang, String targetLang) 
			throws IOException {
		ItemRequest item = new ItemRequest();
		
		String filename = itemFile.getName();
		String fileExt = filename.contains(".") ? 
				filename.substring(filename.lastIndexOf(".")+1) : "";
		item.setDocument(createDocument(itemFile, fileExt));
		item.setItemName(Util.getRelativePath(itemFile, baseDir));
		item.setItemRequestId(UUID.randomUUID().toString());
		item.setLanguage(srcLang);
		item.setEncoding("Default");
		item.add(newTargetLangRequest(targetLang, "Default"));
		return item;
	}

	private static LanguageRequest newTargetLangRequest(String lang, String encoding) {
		LanguageRequest langRequest = new LanguageRequest();
		langRequest.setEncoding(encoding);
		langRequest.setLanguage(lang);
		return langRequest;
	}

	private static Document createDocument(File file, String fileExt) throws IOException {
		Document document = new Document();
		
		document.setContent(FileUtils.readFileToByteArray(file));
		document.setContentType(fileExt);
		return document;
	}

	ConfigResponse validateLanguageSupport(TmsConfig tmsConfig) throws CtaException, IOException {
		ConfigResponse configResponse;
		
		ConfigRequest configRequest = new ConfigRequest();
		configRequest.setClientId(tmsConfig.getConfigGUID());
		configResponse = (ConfigResponse) configRequest.Post(tmsConfig.createPostInfo());
			
		if (isSuccess(configResponse)) {
			checkCtaResponse("Successfully retrieved TMS configuration informaiton", configResponse);
		} else {
			String msg = "Cannot validate language support by configuration " + 
					tmsConfig.getConfigGUID();
			checkCtaResponse(msg, configResponse);
		}
		return configResponse;
	}
	
	static void checkCtaResponse(String msg, BaseResponse response) throws IOException {
		StringWriter writer = new StringWriter();
		PrintWriter ps = new PrintWriter(writer);
		msg = msg + " (" + response.getResponse().getSuccessCode() + ")";

		if (isSuccess(response)) {
			log.info(msg);
		} else {
			log.error(msg);
			ps.println(msg);
			ps.println("TMS CTA errors: ");
		}
		if (response.getResponse().getSystemErrors().size()>0) {
			for (SystemError error : response.getResponse().getSystemErrors()) {
				String err = "(" + error.getCode() + ") " + error.getDescription();
				if (isSuccess(response)) {
					log.info(err);
				} else {
					log.error(err);
					ps.println(err);
				}
			}
		}
		ps.close();
		
		if (!isSuccess(response)) {
			throw new IOException(writer.toString());
		}
	}

	static boolean isSuccess(BaseResponse response) {
		int successCode = response.getResponse().getSuccessCode();
		return successCode==200 || successCode==201;
	}

	private int countFile(File dir) {
		int total = 0;
		File[] files = dir.listFiles();
		
		for (File file : files) {
			if (file.isDirectory()) {
				total += countFile(file);
			} else {
				total++;
			}
		}
		return total;
	}

	public List<Object> getWarnings() {
		return warnings;
	}
	
	public enum Status {
		UPLOAD_IN_PROGESS("Uploading the file", 0, 30, false), 
		SUBMITTING_TO_TMS("Submitting to TMS server", 30, 95, false), 
		PARTIALLY_COMPLETED("Partially Completed", 100, 100, false), 
		COMPLETED("Completed", 100, 100, false), 
		ERROR("Error", 0, 0, false);

		private final String statusString;
		private final int minProgess;
		private final int maxProgess;
		private final boolean autoIncProgess;

		private Status(String statusString, int minProgess, int maxProgess,
				boolean autoIncProgess) {
			this.statusString = statusString;
			this.minProgess = minProgess;
			this.maxProgess = maxProgess;
			this.autoIncProgess = autoIncProgess;
		}

		public String getStatusString() {
			return statusString;
		}

		private int getProgress(AtomicInteger progress) {
			int pr = progress.get();
			if (pr < minProgess) {
				progress.set(minProgess);
				return minProgess;
			} else if (pr < maxProgess && autoIncProgess) {
				return progress.incrementAndGet();
			} else {
				return pr;
			}
		}
	}
	
	private class ManifestFactory {
		private static final String MANIFEST_TEMPLATE = 
				"<?xml version=\"1.0\" encoding=\"utf-8\"?>\n" +
				"<NEXUSMETADATA>\n" +
				"<JOBNAME>$$JN$$</JOBNAME>\n" +
				"<JOBDESCRIPTION>$$JD$$</JOBDESCRIPTION>\n" +
				"</NEXUSMETADATA>";

		private static final String MANIFEST_TEMPLATE_WITH_DUEDATE = 
				"<?xml version=\"1.0\" encoding=\"utf-8\"?>\n" +
				"<NEXUSMETADATA>\n" +
				"<JOBNAME>$$JN$$</JOBNAME>\n" +
				"<JOBDESCRIPTION>$$JD$$</JOBDESCRIPTION>\n" +
				"<JOBDUEDATE>$$JDD$$</JOBDUEDATE>\n" +
				"</NEXUSMETADATA>";

		private static final String SUFFIX_MANIFEST = ".nexmeta";

		private File createManifest() throws IOException {
			File manifestFile = new File(dest, name + SUFFIX_MANIFEST);
			
			if (dueDate!=null) {
				FileUtils.writeStringToFile(manifestFile, 
						MANIFEST_TEMPLATE_WITH_DUEDATE.replace("$$JN$$", jobName).replace(
								"$$JD$$", getJobDescription()).replace(
										"$$JDD$$", dueDate));
			} else {
				FileUtils.writeStringToFile(manifestFile, 
						MANIFEST_TEMPLATE.replace("$$JN$$", jobName).replace(
								"$$JD$$", getJobDescription()));
			}
			return manifestFile;
		}
	}
}
