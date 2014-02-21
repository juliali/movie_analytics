import org.apache.commons.math3.stat.regression.OLSMultipleLinearRegression;

import java.sql.*;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.HashMap;

/**
 * Created by pivotal on 2/20/14.
 */
public class LinearRegressionRaterV2 {

    private HashMap<String, Float> directorMap = new HashMap<String, Float>();
    private HashMap<String, Float> actorMap = new HashMap<String, Float>();

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
                    " substring_index(substring_index(starring, ' ', -2), ' ', 1) as support1, " +
                    " substring_index(substring_index(starring, ' ', -3), ' ', 1) as support2,rate" +
                    " from movie where chinese_name like '%" + name +"%'");
            while (rs.next()){
                String director = rs.getString("dir");
                String lead = rs.getString("lead");
                String support1 = rs.getString("support1");
                String support2 = rs.getString("support2");
                String chineseName = rs.getString("chinese_name");
                float rate = rs.getFloat("rate");
                if (directorMap.containsKey(director) && actorMap.containsKey(lead)
                        && actorMap.containsKey(support1) && actorMap.containsKey(support2)) {
                    float actorRate = (actorMap.get(lead)+(actorMap.get(support1)+actorMap.get(support2))/2)/2;
                    double predict = p[0]+p[1]*directorMap.get(director)+p[2]*actorRate;
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
                "group by dir having   dir<>'' and length(dir) != character_length(dir)");
        while (rs.next()) {
            directorMap.put(rs.getString("dir"), rs.getFloat("avr"));
        }

        // supporting lead avg rate
        rs = stmt.executeQuery("select avg(rate) as avr, substring_index(substring_index(starring, ' ', -2), ' ', 1) as support from movie " +
                "where votes>=100 and release_date<'2013-01-01' " +
                "group by support having support<>''" +
                "and length(support) != character_length(support)");
        while (rs.next()) {
            actorMap.put(rs.getString("support"), rs.getFloat("avr"));
        }

        // supporting lead avg rate
        rs = stmt.executeQuery("select avg(rate) as avr, substring_index(substring_index(starring, ' ', -3), ' ', 1) as support from movie " +
                "where votes>=100 and release_date<'2013-01-01' " +
                "group by support having support<>''" +
                "and length(support) != character_length(support)");
        while (rs.next()) {
            String actor = rs.getString("support");
            float rate = rs.getFloat("avr");
            if (actorMap.containsKey(actor))
                actorMap.put(actor, (actorMap.get(actor)+rate)/2);
            actorMap.put(rs.getString("support"), rate);
        }

        // lead lead avg rate
        rs = stmt.executeQuery("select avg(rate) as avr, substring_index(starring, ' ', 1) as lead from movie " +
                "where votes>=100 " +
                "group by lead having count(*)>1 and lead<>'' and length(lead) != character_length(lead)");
        while (rs.next()) {
            String actor = rs.getString("lead");
            float rate = rs.getFloat("avr");
            if (actorMap.containsKey(actor))
                actorMap.put(actor, (actorMap.get(actor)+rate)/2);
            actorMap.put(actor, rate);
        }
    }


    private double[] estimateParameter(Statement stmt, ResultSet rs) throws SQLException {
        ArrayList<Double> dList = new ArrayList<Double>();
        ArrayList<Double> aList = new ArrayList<Double>();
        ArrayList<Double> rList = new ArrayList<Double>();
        rs = stmt.executeQuery("select substring_index(director, ' ', 1) as dir, substring_index(starring, ' ', 1) as lead, " +
                "substring_index(substring_index(starring, ' ', -2), ' ', 1) as support1, " +
                "substring_index(substring_index(starring, ' ', -3), ' ', 1) as support2, rate " +
                "from movie where votes>=100");
        while (rs.next()) {
            String director = rs.getString("dir");
            String lead = rs.getString("lead");
            String support1 = rs.getString("support1");
            String support2 = rs.getString("support2");
            Float rate = rs.getFloat("rate");
            if (directorMap.containsKey(director) && actorMap.containsKey(lead)
                    && actorMap.containsKey(support1) && actorMap.containsKey(support2)) {
                dList.add(directorMap.get(director).doubleValue());
                double actorRate = (actorMap.get(lead)+(actorMap.get(support1)+actorMap.get(support2))/2)/2;
                aList.add(actorRate);
                rList.add(rate.doubleValue());
            }
        }
        int m = 2;
        int n = rList.size();
        double[][] x = new double[n][m];
        double[] y = new double[n];
        for (int i=0; i<rList.size(); i++) {
            x[i][0] = dList.get(i);
            x[i][1] = aList.get(i);
            y[i] = rList.get(i);
        }
        OLSMultipleLinearRegression ols = new OLSMultipleLinearRegression();
        ols.newSampleData(y, x);
        return ols.estimateRegressionParameters();
    }

    private void testModel(Statement stmt, ResultSet rs, double[] p) throws SQLException {
        rs = stmt.executeQuery("select chinese_name, substring_index(director, ' ', 1) as dir, substring_index(starring, ' ', 1) as lead, " +
                "substring_index(substring_index(starring, ' ', -2), ' ', 1) as support1, " +
                "substring_index(substring_index(starring, ' ', -3), ' ', 1) as support2, rate " +
                "from movie where votes>=100 and release_date >= '2013-01-01' and release_date<'2014-01-01'");
        int success = 0;
        int fail = 0;
        while (rs.next()) {
            String name = rs.getString("chinese_name");
            String director = rs.getString("dir");
            String lead = rs.getString("lead");
            String support1 = rs.getString("support1");
            String support2 = rs.getString("support2");
            Float rate = rs.getFloat("rate");
            if (directorMap.containsKey(director) && actorMap.containsKey(lead)
                    && actorMap.containsKey(support1) && actorMap.containsKey(support2)) {
                double actorRate = (actorMap.get(lead)+(actorMap.get(support1)+actorMap.get(support2))/2)/2;
                double predict = p[0]+p[1]*directorMap.get(director)+p[2]*actorRate;
                if (rate-predict >= 1 || predict - rate <=-1)
                    System.out.println(name + " : predict rate " + predict + " , real rate " + rate );
                else
                    success++;
            } else {
                fail++;
                //System.out.println(name + " :  " + director + "  " + lead );
            }
        }
        System.out.println(success + " movies are predicted as expected");
        System.out.println(fail+ " movies cant not be predicted");
    }

    public static void main(String[] args) {
        new LinearRegressionRaterV2().calculate("泰囧");

    }
}