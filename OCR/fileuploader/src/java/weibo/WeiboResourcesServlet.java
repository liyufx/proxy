package weibo;

import java.io.IOException;
import java.io.PrintWriter;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public class WeiboResourcesServlet extends HttpServlet {
	private static final long serialVersionUID = 1L;

	public void doGet(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {
		response.setContentType("text/plain");
		PrintWriter out = response.getWriter();

		try {
			String function = request.getParameter("fn");
			
			if (function.equals("pic")) {
				
			} else if (function.equals("comment")) {
				
			} else if (function.equals("post")) {
				
			} else if (function.equals("topic_post")) {
				
			}
		} finally {
			out.close();
		}
	}

}
