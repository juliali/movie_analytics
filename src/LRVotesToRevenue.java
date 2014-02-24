import org.apache.commons.math3.stat.regression.OLSMultipleLinearRegression;

import java.sql.*;
import java.text.DecimalFormat;
import java.util.ArrayList;

/**
 * Created by pivotal on 2/21/14.
 */
public class LRVotesToRevenue {

    private ArrayList<Integer> vList = new ArrayList<Integer>();
    private ArrayList<Integer> rList = new ArrayList<Integer>();

    public int predictRevenueByVotes(int votes) {
        double[] p = estimateParameters();
        return  Double.valueOf(p[0]+p[1]*votes).intValue();
    }

    private double[] estimateParameters() {
        Connection conn = null;
        Statement stmt = null;
        ResultSet rs = null;
        try {
            Class.forName("com.mysql.jdbc.Driver");
            conn = DriverManager.getConnection("jdbc:mysql://192.168.3.166:3306/movie_data?characterEncoding=UTF-8", "movie", "movie");
            stmt = conn.createStatement();
            rs = stmt.executeQuery("select mr.chinese_name , votes, box_office_revenue from movie_revenue_china mr, (select votes, chinese_name from movie m group by chinese_name having count(*) = 1) m where m.chinese_name = mr.chinese_name");
            while (rs.next()) {
                vList.add(rs.getInt("votes"));
                rList.add(rs.getInt("box_office_revenue"));
            }
            int m = 1;
            int n = rList.size();
            double[][] x = new double[n][m];
            double[] y = new double[n];
            for (int i=0; i<rList.size(); i++) {
                x[i][0] = vList.get(i);
                y[i] = rList.get(i);
            }
            OLSMultipleLinearRegression ols = new OLSMultipleLinearRegression();
            ols.newSampleData(y, x);
            return ols.estimateRegressionParameters();
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        } finally {
            if (rs != null)
                try {
                    rs.close();
                } catch (SQLException e) {
                    e.printStackTrace();
                }
            if (stmt != null)
                try {
                    stmt.close();
                } catch (SQLException e) {
                    e.printStackTrace();
                }
            if (conn != null)
                try {
                    conn.close();
                } catch (SQLException e) {
                    e.printStackTrace();
                }
        }
    }

    private void testData() {
        double[] p = estimateParameters();
        DecimalFormat df = new DecimalFormat("#.0");
        int count =0;
        for (int i =0; i<vList.size(); i++) {
            double ratio = rList.get(i) / (p[0] + p[1] * vList.get(i));
            //System.out.println(df.format(ratio));
            if (ratio>=2 || ratio<=0.5) {
                System.out.println("差太多 " + ratio);
                count++;
            }
        }
        System.out.println(count + " in all " + vList.size() +" 差太多");
    }
    public static void main(String[] args) {
        //System.out.println(new LRVotesToRevenue().predictRevenueByVotes(597));
        new LRVotesToRevenue().testData();
    }
}
