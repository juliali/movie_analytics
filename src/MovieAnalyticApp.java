import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.List;
import java.util.Map;

/**
 * Created by pivotal on 2/24/14.
 */

public class MovieAnalyticApp extends javax.servlet.http.HttpServlet implements javax.servlet.Servlet {

    public MovieAnalyticApp() {
        super();
    }

    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        doPost(request, response);
    }

    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        response.setContentType("text/html");

//        // get PA service config info first
//        String svcConfStr = System.getenv("VCAP_SERVICES");
//
//        try {
//            svcConfStr = new JSONObject(svcConfStr).toString(2);
//        } catch (JSONException e) {
//        }
//
//        ObjectMapper objectMapper = new ObjectMapper();
//        Map<String, List<Map<String, Object>>> svcConfMap =
//                objectMapper.readValue(svcConfStr, Map.class);
        String name = request.getParameter("name");
        int revenue = new LRVotesToRevenue().predictRevenueByVotes(new LinearRegressionVotes().calculate(name));
        response.getWriter().write("{revenue: "+revenue+"}");
    }


}