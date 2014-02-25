import org.apache.commons.math3.stat.regression.OLSMultipleLinearRegression;

import java.sql.*;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by pivotal on 2/20/14.
 */
public class LinearRegressionRaterV2 {

    private Connection conn = null;
    private HashMap<String, Float> directorMap = new HashMap<String, Float>();
    private HashMap<String, Float> actorMap = new HashMap<String, Float>();
    private float actorAvr = 0;
    private float directorAvr = 0;

    public LinearRegressionRaterV2(Connection conn) {
        this.conn = conn;
    }

    public Map<String, Double> calculate(String name) {

        Statement stmt = null;
        ResultSet rs = null;
        Map<String, Double> map = new HashMap<String, Double>();
        try {
//            Class.forName("com.mysql.jdbc.Driver");
//            conn = DriverManager.getConnection("jdbc:mysql://192.168.3.166:3306/movie_data?characterEncoding=UTF-8", "movie", "movie");
            stmt = conn.createStatement();
            buildAvgRateMap(stmt, rs);
            double[] p = estimateParameter(stmt, rs);
//            testModel(stmt, rs, p);
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
                float directorValue;
                if (directorMap.containsKey(director))
                    directorValue = directorMap.get(director);
                else
                    directorValue = directorAvr;
                float actorRate = calculateActorRate(lead, support1, support2);
                double predict = p[0]+p[1]*directorValue+p[2]*actorRate;
                map.put(chineseName, predict);
                //printResult(chineseName, predict, rate);
            }
            return map;
        } catch (Exception e) {
            e.printStackTrace();
            return map;
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
        for (float value : directorMap.values())
            directorAvr += value;
        directorAvr /= directorMap.size();
        for (float value : actorMap.values())
            actorAvr += value;
        actorAvr /= actorMap.size();
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
            Float directorValue;
            if (directorMap.containsKey(director))
                directorValue = directorMap.get(director);
            else
                directorValue = directorAvr;
            double actorRate = calculateActorRate(lead, support1, support2);
            dList.add(directorValue.doubleValue());
            aList.add(actorRate);
            rList.add(rate.doubleValue());

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
            float directorValue;
            if (directorMap.containsKey(director))
                directorValue = directorMap.get(director);
            else
                directorValue = directorAvr;
            float actorRate = calculateActorRate(lead, support1, support2);
            double predict = p[0]+p[1]*directorValue+p[2]*actorRate;
            if (rate-predict >= 1 || predict - rate <=-1)
                System.out.println(name + " : predict rate " + predict + " , real rate " + rate );
            else
                success++;
        }
        System.out.println(success + " movies are predicted as expected");
    }

    private float calculateActorRate(String lead, String support1, String support2) {
        float leadValue;
        if (actorMap.containsKey(lead))
            leadValue = actorMap.get(lead);
        else
            leadValue = actorAvr;
        float support1Value;
        if (actorMap.containsKey(support1))
            support1Value = actorMap.get(support1);
        else
            support1Value = actorAvr;
        float support2Value;
        if (actorMap.containsKey(support2))
            support2Value = actorMap.get(support2);
        else
            support2Value = actorAvr;
        float actorRate = (leadValue+(support1Value+support2Value)/2)/2;
        return actorRate;
    }

    public static void main(String[] args) {
        //new LinearRegressionRaterV2().calculate("霍比特人");

    }
}