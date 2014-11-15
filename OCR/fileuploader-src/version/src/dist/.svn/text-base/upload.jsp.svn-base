
<%@ page import="org.apache.commons.fileupload.*, 
				org.apache.commons.fileupload.servlet.ServletFileUpload, 
				org.apache.commons.fileupload.disk.DiskFileItemFactory, 
				org.apache.commons.io.FilenameUtils, 
				org.apache.commons.lang.StringEscapeUtils,
				java.util.*, 
				java.io.*, 
				java.lang.Exception,
				com.claytablet.intel.fileuploader.*" %>
				
<% 

response.setContentType("text/html");
response.setHeader("Cache-control", "no-cache");

TmsFileUploader job = TmsFileUploader.upload(request);
TmsFileUploader.Status status = job.upload();
String statusString = job.getStatusString();

if (status==TmsFileUploader.Status.COMPLETED) {
	out.println("<script type='text/javascript'>");
	out.println("parent.document.getElementById('status').style.visibility=\"hidden\";");
	out.println("parent.document.getElementById('uploadButton').disabled=\"\";");
	out.println("parent.document.getElementById('successPane').innerHTML=\"" + 
			StringEscapeUtils.escapeHtml(statusString) + "\";");
	out.println("parent.document.getElementById('successPane').style.color=\"darkgreen\";");
	out.println("parent.document.getElementById('successPane').style.visibility=\"visible\";");
	out.println("parent.document.getElementById('errorPane').style.visibility=\"hidden\";");
	out.println("parent.document.getElementById('errorDetailPane').style.visibility=\"hidden\";");
	out.println("parent.document.getElementById('toggleErrorDetail').value=\"Show detail\";");
	out.println("</script>");
} else {
	String color = (TmsFileUploader.Status.ERROR==status)? "red" : "DarkOrange";
	
	out.println("<script type='text/javascript'>");
	out.println("parent.document.getElementById('status').style.visibility=\"hidden\";"); 
	out.println("parent.document.getElementById('uploadButton').disabled=\"\";");
	out.println("parent.document.getElementById('successPane').style.color=\"" + color + "\";");
	out.println("parent.document.getElementById('successPane').innerHTML=\"" + 
			StringEscapeUtils.escapeHtml(statusString) + "\";");
	out.println("parent.document.getElementById('successPane').style.visibility=\"visible\";");
	out.println("parent.document.getElementById('errorPane').style.visibility=\"visible\";");
	out.println("parent.document.getElementById('errorDetailPane').style.visibility=\"hidden\";");
	out.println("parent.document.getElementById('toggleErrorDetail').value=\"Show detail\";");
	
	out.println("</script>");
    StringWriter sw = new StringWriter();
    PrintWriter printWriter = new PrintWriter(sw);
    
    for (Object warning : job.getWarnings()) {
    	if (warning instanceof Throwable) {
    		Throwable t = (Throwable)warning;
		    t.printStackTrace(printWriter);
			String stackTrace = sw.toString();
			out.print(stackTrace.replace(System.getProperty("line.separator"), "<br/>\n"));
			out.print("<br/><br/>\n");
    	} else {
    		out.print(warning);
    		out.print("<br>");
    	}
    }
}
%>

