
import java.sql.Connection;
import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.sql.Date;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.text.DecimalFormat;
import java.util.List;
import java.util.Map;

/**
 * Created by pivotal on 2/24/14.
 */

public class MovieAnalyticApp extends javax.servlet.http.HttpServlet implements javax.servlet.Servlet {

    private static DecimalFormat format = new DecimalFormat("#.0");
    public MovieAnalyticApp() {
        super();
    }

    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        doPost(request, response);
    }

    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        request.setCharacterEncoding("UTF-8");
        String name = request.getParameter("name");
        Connection conn = null;
        try {
            conn = buildMysqlConnection();
            Map<String, Object[]> rateMap = new LinearRegressionRaterV2(conn).calculate(name);
            Map<String, int[]> revenueMap = new LRVotesToRevenue(conn).predictRevenueByVotes(new LinearRegressionVotes(conn).calculate(name));
            response.setCharacterEncoding("UTF-8");
            response.setHeader("content-type","text/html;charset=UTF-8");
            String r = "[";
            for (Map.Entry<String, Object[]> entry : rateMap.entrySet()) {
                String movieName = entry.getKey();
                String predictRate = format.format(entry.getValue()[0]);
                String realRate = entry.getValue()[1].toString();
                String releaseDate = null;
                if (entry.getValue()[2] != null)
                    releaseDate = ((Date)entry.getValue()[2]).toString().substring(0,4);
                Integer predictRevenue = revenueMap.get(movieName)[0];
                Integer realRevenue = revenueMap.get(movieName)[1];
                if(r.length() == 1){
                    r+="{movieName: \""+movieName+"\", predictRate: "+predictRate+", realRate: "+realRate+", predictRevenue: "+predictRevenue+", realRevenue: "+realRevenue+", releaseDate: "+releaseDate+"}";
                }else{
                    r+=",{movieName: \""+movieName+"\", predictRate: "+predictRate+", realRate: "+realRate+", predictRevenue: "+predictRevenue+", realRevenue: "+realRevenue+", releaseDate: "+releaseDate+"}";
                }
            }
            r+="]";
            System.out.println(r);
            response.getWriter().write(r);
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (conn != null)
                try {
                    conn.close();
                } catch (SQLException e) {
                    e.printStackTrace();
                }
        }
    }

    private Connection buildMysqlConnection() throws Exception {
        // get PA service config info first
        String svcConfStr = System.getenv("VCAP_SERVICES");

        try {
            svcConfStr = new JSONObject(svcConfStr).toString(2);
        } catch (JSONException e) {
        }

        ObjectMapper objectMapper = new ObjectMapper();
        Map<String, List<Map<String, Object>>> svcConfMap =
                objectMapper.readValue(svcConfStr, Map.class);
        List<Map<String, Object>> list = svcConfMap.get("p-mysql");
        if (list == null || list.size()!=1) {
            throw new Exception("mysql env is wrong");
        }
        Map<String, Object> map = list.get(0);
        Object credentials = map.get("credentials");
        if (credentials == null) {
            throw new Exception("mysql env is wrong");
        }
        Map<String, Object> connMap = (Map<String, Object>)credentials;
        String host = connMap.get("hostname").toString();
        String port = connMap.get("port").toString();
        String db = connMap.get("name").toString();
        String userName = connMap.get("username").toString();
        String password = connMap.get("password").toString();
        Class.forName("com.mysql.jdbc.Driver");
        Connection conn = DriverManager.getConnection("jdbc:mysql://"+host+":"+port+"/"+db+"?characterEncoding=UTF-8", userName, password);
        return conn;
    }

}