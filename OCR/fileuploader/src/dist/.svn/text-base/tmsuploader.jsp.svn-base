<%@ page import="com.claytablet.intel.fileuploader.*,
 				 org.apache.commons.lang.StringUtils"
%>

<% 
response.setContentType("text/html");

if (!StringUtils.isEmpty(System.getProperty("tms.uploader.home"))) {
	Home.init(System.getProperty("tms.uploader.home"));
} else {
	Home.init(pageContext.getServletConfig().getInitParameter("tmsUploaderHome"));
}

boolean showHomeDir = "true".equals(request.getParameter("showHomeDir"));

// Initialize language mapping
LanguageMapping.getLanguageMapping();
%>

<html>
<head>
	<title>Intel TMS Translation File Uploader (1.0.0)</title>
</head>

<script language="javascript">
var intervalID = null;

function trim(stringToTrim) {
	return stringToTrim.replace(/^\s+|\s+$/g,"");
}

function clearForm() {
	var uploadForm = document.getElementById("uploadform");
	uploadForm.reset();	
	document.getElementById("tmsconfig_warning").style.visibility="hidden";
	document.getElementById("jobname_warning").style.visibility="hidden";
	document.getElementById("uploadedfile_warning").style.visibility="hidden";
	document.getElementById("successPane").style.visibility="hidden";
	document.getElementById("errorPane").style.visibility="hidden";
	document.getElementById("errorDetailPane").style.visibility="hidden";
}

function upload(){
	var uploadForm = document.getElementById("uploadform");
	var hasError = false;
/*	
	if (trim(uploadForm["tmsconfig"].value)=="") {
		document.getElementById("tmsconfig_warning").style.visibility="visible";
		uploadForm["tmsconfig"].focus();
		hasError = true;
	} else {
		document.getElementById("tmsconfig_warning").style.visibility="hidden";
	}
*/	
	if (trim(uploadForm["jobname"].value)=="") {
		document.getElementById("jobname_warning").style.visibility="visible";
		uploadForm["jobname"].focus();
		hasError = true;
	} else {
		document.getElementById("jobname_warning").style.visibility="hidden";
	}
	
	if (trim(uploadForm["uploadedfile"].value)=="") {
		document.getElementById("uploadedfile_warning").style.visibility="visible";
		uploadForm["uploadedfile"].focus();
		hasError = true;
	} else {
		document.getElementById("uploadedfile_warning").style.visibility="hidden";
	}
	
	if (!hasError) {
		// have all the required fields, proceed to submit
		uploadForm.target="errorDetailPane";
		document.getElementById('progressDiv').innerHTML="";
		document.getElementById('status').style.visibility="visible";
		document.getElementById('uploadButton').disabled = "disabled";
		document.getElementById("successPane").style.visibility="hidden";
		document.getElementById("errorPane").style.visibility="hidden";
		document.getElementById("errorDetailPane").style.visibility="hidden";
		if (intervalID!=null) {
			clearInterval(intervalID);
		}
		intervalID = setInterval(showProgress, 2000);
		uploadForm.submit();
	}
}

function showProgress() {
	statusDiv = document.getElementById('status');
	if (statusDiv.style.visibility=="hidden") {
		clearInterval(intervalID);
		intervalId = null;
	} else {
		var progressBarDiv = document.getElementById('progressDiv');
		progress = progressBarDiv.innerHTML;
		if (progress.length>10) {
			progressBarDiv.innerHTML="&#9632;";
		} else {
			progressBarDiv.innerHTML=progress + "&#9632;";
		}
	}
}

function toggleErrorDetailPane() {
	toggleForm = document.getElementById("toggleErrorDetailForm");
	toggleButton = toggleForm["toggleErrorDetail"];
	if (toggleButton.value=="Show detail") {
		toggleButton.value="Hide detail";
		document.getElementById("errorDetailPane").style.visibility="visible";
	} else {
		toggleButton.value="Show detail";
		document.getElementById("errorDetailPane").style.visibility="hidden";
	}
}

</script>

<body>

<h3>Intel TMS Translation File Uploader</h3>
<%= showHomeDir? "Home=" + Home.getFile(".").getAbsolutePath() + "<br><br>" : "" %>
<form id="uploadform" name="uploadform" enctype="multipart/form-data" 
		action="upload.jsp" method="POST" target="target_iframe">
	<input type="hidden" name="MAX_FILE_SIZE" value="10000000" />
	<table border="0">
	<tr>
		<td>* Select a ZIP file: </td> 
		<td> <input name="uploadedfile" type="file""  style="width: 100%" accept="application/zip"/></td>
		<td><div id="uploadedfile_warning" style="visibility: hidden; color:red">
				* Please pick a ZIP file to submit
		</div></td>
	</tr>
	<tr>
		<td>* Select TMS Configuration:&nbsp;</td> 
		<td> <select name="tmsconfig"  style="width: 100%">
				<% for (String configName : TmsConfigurations.getInstance().getConfigNames()) {%> 
					<option><%=configName%></option>
				<% } %>
			 </select>
		</td>
		<td><div id="tmsconfig_warning" style="visibility: hidden; color:red">
				* Please pick a TMS configuration
		</div></td>
	</tr>
	<tr>
		<td>* Specify job name: </td> 
		<td> <input name="jobname" type="text" style="width: 100%""></td>
		<td> <div id="jobname_warning" style="visibility: hidden; color:red">
				* Please enter a job name
		</div></td>
	</tr>
	<tr>
		<td>Enter job description: </td> 
		<td> <TEXTAREA NAME="jobdescription" ROWS="3" COLS="40"></TEXTAREA></td>
	</tr>
	<tr>
		<td>Specify due date: </td> 
		<td> <input name="duedate" type="text" size="12">
			 <label style="color: grey">&nbsp;&nbsp;(mm/dd/yyyy)</label>
		</td>
	</tr>
	<tr>
		<td><label style="color: grey">* required</label></td>
		<td align="right"><input type="button" value="Clear" onclick="clearForm()"/>
		<input id="uploadButton" type="button" value="Submit" onclick="upload()"/></td>
	<tr>
	</table>
</form>

<div id="status"  style="visibility: hidden;"> <table border="0">
	<tr><td>Uploading</td>
		<td><div id="progressDiv" style="color: grey"></div></td></tr>
</table> </div>

<div id="successPane"  style="visibility: hidden; color: orange">File submitted to TMS successfully.</div>
<div id="errorPane" style="visibility: hidden;">
	<form  id="toggleErrorDetailForm">
		<input id="toggleErrorDetail" name="toggleErrorDetail" type="button" 
				value="Show detail" onclick="toggleErrorDetailPane()"/></td>
	</form>
</div>
<iframe id="errorDetailPane" width="100%" height="100%" frameborder="0" style="visibility: visible;" name="errorDetailPane" />

</body>
</html>