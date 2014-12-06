package fileuploader;

import java.io.*;

import javax.servlet.ServletException;
import javax.servlet.http.*;

import org.apache.commons.io.FilenameUtils;


public class FileUploadServlet extends HttpServlet {
	private static final long serialVersionUID = 1L;
	
//	private final static Logger LOGGER = 
//            Logger.getLogger(FileUploadServlet.class.getCanonicalName());
// 
	public FileUploadServlet() {
		System.out.println("Init FileUploadServlet");
	}
	
    protected void processRequest(HttpServletRequest request,
            HttpServletResponse response)
            throws ServletException, IOException {
        response.setContentType("text/html;charset=UTF-8");

        // Create path components to save the file
        final Part filePart = request.getPart("file");
        final String fileName = getFileName(filePart);
        
        File outFile = File.createTempFile("upload", FilenameUtils.getExtension(fileName));

        OutputStream out = null;
        InputStream filecontent = null;
        final PrintWriter writer = response.getWriter();

        try {
            out = new FileOutputStream(outFile);
            filecontent = filePart.getInputStream();

            int read = 0;
            final byte[] bytes = new byte[1024];

            while ((read = filecontent.read(bytes)) != -1) {
                out.write(bytes, 0, read);
            }
            writer.println("New file created at " + outFile.getAbsolutePath());
//            LOGGER.log(Level.INFO, "File being uploaded to {0}", 
//                    new Object[]{outFile.getAbsolutePath()});
        } catch (FileNotFoundException fne) {
            writer.println("You either did not specify a file to upload or are "
                    + "trying to upload a file to a protected or nonexistent "
                    + "location.");
            writer.println("<br/> ERROR: " + fne.getMessage());

//            LOGGER.log(Level.SEVERE, "Problems during file upload. Error: {0}", 
//                    new Object[]{fne.getMessage()});
        } finally {
            if (out != null) {
                out.close();
            }
            if (filecontent != null) {
                filecontent.close();
            }
            if (writer != null) {
                writer.close();
            }
        }
    }
    
    private String getFileName(final Part part) {
        final String partHeader = part.getHeader("content-disposition");
//        LOGGER.log(Level.INFO, "Part Header = {0}", partHeader);
        for (String content : part.getHeader("content-disposition").split(";")) {
            if (content.trim().startsWith("filename")) {
                return content.substring(
                        content.indexOf('=') + 1).trim().replace("\"", "");
            }
        }
        return null;
    }
}
