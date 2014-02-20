import org.apache.commons.math3.stat.regression.OLSMultipleLinearRegression;

import java.sql.*;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.HashMap;

/**
 * Created by pivotal on 2/20/14.
 */
public class LinearRegressionRaterV1 {

    private HashMap<String, Float> directorMap = new HashMap<String, Float>();
    private HashMap<String, Float> leadMap = new HashMap<String, Float>();
    private HashMap<String, Float> supportMap = new HashMap<String, Float>();

    public void calculate(String name) {
        Connection conn = null;
        Statement stmt = null;
        ResultSet rs = null;
        try {
            Class.forName("com.mysql.jdbc.Driver");
            conn = DriverManager.getConnection("jdbc:mysql://192.168.3.166:3306/movie_data?characterEncoding=UTF-8", "movie", "movie");
            stmt = conn.createStatement();
            buildAvgRateMap(stmt, rs);
            double[] p = estimateParameter(stmt, rs);
            testModel(stmt, rs, p);
            rs = stmt.executeQuery("select chinese_name, substring_index(director, ' ', 1) as dir, substring_index(starring, ' ', 1) as lead, " +
                    " substring_index(substring_index(starring, ' ', -2), ' ', 1) as support, rate" +
                    " from movie where chinese_name like '%" + name +"%'");
            while (rs.next()){
                String director = rs.getString("dir");
                String lead = rs.getString("lead");
                String support = rs.getString("support");
                String chineseName = rs.getString("chinese_name");
                float rate = rs.getFloat("rate");
                if (directorMap.containsKey(director) && leadMap.containsKey(lead) && supportMap.containsKey(support)) {
                    double predict = p[0]+p[1]*directorMap.get(director)+p[2]*leadMap.get(lead)+p[3]*supportMap.get(support);
                    printResult(chineseName, predict, rate);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
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

    private void printResult(String chineseName, double predict, float rate) {
        System.out.println("movie name : " + chineseName +" predict rate " + new DecimalFormat("#.0").format(predict) + "; real rate :" + rate);
    }

    private void buildAvgRateMap(Statement stmt, ResultSet rs) throws SQLException {
        // director avg rate
        rs = stmt.executeQuery("select avg(rate) as avr, substring_index(director, ' ', 1) as dir from movie " +
                "where votes>=100 and release_date<'2013-01-01' " +
                "group by dir having  count(*) > 1 and dir<>'' and length(dir) != character_length(dir)");
        while (rs.next()) {
            directorMap.put(rs.getString("dir"), rs.getFloat("avr"));
        }
        // lead lead avg rate
        rs = stmt.executeQuery("select avg(rate) as avr, substring_index(starring, ' ', 1) as lead from movie " +
                "where votes>=100 and release_date<'2013-01-01' " +
                "group by lead having count(*)>1 and lead<>'' and length(lead) != character_length(lead)");
        while (rs.next()) {
            leadMap.put(rs.getString("lead"), rs.getFloat("avr"));
        }
        // supporting lead avg rate
        rs = stmt.executeQuery("select avg(rate) as avr, substring_index(substring_index(starring, ' ', -2), ' ', 1) as support from movie " +
                "where votes>=100 and release_date<'2013-01-01' " +
                "group by support having support<>''" +
                "and count(*)>1 and length(support) != character_length(support)");
        while (rs.next()) {
            supportMap.put(rs.getString("support"), rs.getFloat("avr"));
        }
    }


    private double[] estimateParameter(Statement stmt, ResultSet rs) throws SQLException {
        ArrayList<Double> dList = new ArrayList<Double>();
        ArrayList<Double> lList = new ArrayList<Double>();
        ArrayList<Double> sList = new ArrayList<Double>();
        ArrayList<Double> rList = new ArrayList<Double>();
        rs = stmt.executeQuery("select substring_index(director, ' ', 1) as dir, substring_index(starring, ' ', 1) as lead, " +
                "substring_index(substring_index(starring, ' ', -2), ' ', 1) as support, rate " +
                "from movie where votes>=100");
        while (rs.next()) {
            String director = rs.getString("dir");
            String lead = rs.getString("lead");
            String support = rs.getString("support");
            Float rate = rs.getFloat("rate");
            if (directorMap.containsKey(director) && leadMap.containsKey(lead) && supportMap.containsKey(support)) {
                dList.add(directorMap.get(director).doubleValue());
                lList.add(leadMap.get(lead).doubleValue());
                sList.add(supportMap.get(support).doubleValue());
                rList.add(rate.doubleValue());
            }
        }
        int m = 3;
        int n = rList.size();
        double[][] x = new double[n][m];
        double[] y = new double[n];
        for (int i=0; i<rList.size(); i++) {
            x[i][0] = dList.get(i);
            x[i][1] = lList.get(i);
            x[i][2] = sList.get(i);
            y[i] = rList.get(i);
        }
        OLSMultipleLinearRegression ols = new OLSMultipleLinearRegression();
        ols.newSampleData(y, x);
        return ols.estimateRegressionParameters();
    }

    private void testModel(Statement stmt, ResultSet rs, double[] p) throws SQLException {
        rs = stmt.executeQuery("select chinese_name, substring_index(director, ' ', 1) as dir, substring_index(starring, ' ', 1) as lead, " +
                "substring_index(substring_index(starring, ' ', -2), ' ', 1) as support, rate " +
                "from movie where votes>=100 and release_date >= '2013-01-01' and release_date<'2014-01-01'");
        int success = 0;
        int fail = 0;
        while (rs.next()) {
            String name = rs.getString("chinese_name");
            String director = rs.getString("dir");
            String lead = rs.getString("lead");
            String support = rs.getString("support");
            Float rate = rs.getFloat("rate");
            if (directorMap.containsKey(director) && leadMap.containsKey(lead)&& supportMap.containsKey(support)) {
                double predict = p[0]+p[1]*directorMap.get(director)+p[2]*leadMap.get(lead)+p[3]*supportMap.get(support);
                if (rate-predict >= 1 || predict - rate <=-1)
                    System.out.println(name + " : predict rate " + predict + " , real rate " + rate );
                else
                    success++;
            } else {
                fail++;
            }
        }
        System.out.println(success + " movies are predicted as expected");
        System.out.println(fail+ " movies cant not be predicted");
    }

    public static void main(String[] args) {
        new LinearRegressionRaterV1().calculate("大话西游");

    }
}