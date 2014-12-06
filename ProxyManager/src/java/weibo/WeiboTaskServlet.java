package weibo;

import java.io.IOException;
import java.io.PrintWriter;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

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
				List<RepostTask> reposts = RepostTask.loadActiveReposts();
				List<String> repostIds = new ArrayList<String>(3);
				for (RepostTask repost : reposts) {
					repostIds.add(repost.getId());
					for (String helper : repost.getHelperPosts()) {
						repostIds.add(helper);
					}
				}
				int pos; 
				int count = repostIds.size();
				if (count>3) {
					count = 3;
					pos = (int)(Math.random()*repostIds.size());
				} else {
					pos = 0;
				}
				for (int j=0; j<count; j++) {
					out.println(repostIds.get(pos));
					pos++;
					if (pos>=repostIds.size()) {
						pos = 0;
					}
				}
			} else if (function.equals("vote")) {
				List<VoteTask> votes = VoteTask.loadActiveVotes();
				for (VoteTask vote : votes) {
					out.println(vote.getId() + ", " + vote.getVotePos());
				}
			} else if (function.equals("fans")) {
			} else if (function.equals("get")) {
				String email = request.getParameter("email");
				
			} else if (function.equals("report")) {
				String email = request.getParameter("email");
				
			}
		} catch (SQLException e) {
			throw new ServletException(e);
		} finally {
			out.close();
		}
	}
}
