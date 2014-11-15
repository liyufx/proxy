package weibo;

import java.io.IOException;
import java.io.PrintWriter;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public class WeiboTaskServlet extends HttpServlet {
	private static final long serialVersionUID = 1L;

	public void doGet(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {
		response.setContentType("text/plain");
		PrintWriter out = response.getWriter();

		try {
			String function = request.getParameter("fn");
			
			if (function.equals("repost")) {
				
			} else if (function.equals("vote")) {
				
				
			} else if (function.equals("fans")) {
				
			} else if (function.equals("get")) {
				String email = request.getParameter("email");
				
			} else if (function.equals("report")) {
				String email = request.getParameter("email");
				
			}
		} finally {
			out.close();
		}
	}
}
