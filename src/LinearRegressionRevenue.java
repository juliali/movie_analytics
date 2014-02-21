import org.apache.commons.math3.stat.regression.OLSMultipleLinearRegression;

import java.sql.*;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.HashMap;

/**
 * Created by pivotal on 2/21/14.
 */
public class LinearRegressionRevenue {
    private HashMap<String, Float> directorMap = new HashMap<String, Float>();
    private HashMap<String, Float> actorMap = new HashMap<String, Float>();
    private float actorAvr = 0;
    private float directorAvr = 0;

    public void calculate(String name) {
        Connection conn = null;
        Statement stmt = null;
        ResultSet rs = null;
        try {
            Class.forName("com.mysql.jdbc.Driver");
            conn = DriverManager.getConnection("jdbc:mysql://192.168.3.166:3306/movie_data?characterEncoding=UTF-8", "movie", "movie");
            stmt = conn.createStatement();
            buildAvgRevenueMap(stmt, rs);
            double[] p = estimateParameter(stmt, rs);
            testModel(stmt, rs, p);
            rs = stmt.executeQuery("select chinese_name, substring_index(director, ' ', 1) as dir, substring_index(starring, ' ', 1) as lead, " +
                    " substring_index(substring_index(starring, ' ', -2), ' ', 1) as support1, " +
                    " substring_index(substring_index(starring, ' ', -3), ' ', 1) as support2,box_office_revenue" +
                    " from mtime_revenue where chinese_name like '%" + name + "%'");
            while (rs.next()) {
                String director = rs.getString("dir");
                String lead = rs.getString("lead");
                String support1 = rs.getString("support1");
                String support2 = rs.getString("support2");
                String chineseName = rs.getString("chinese_name");
                float box_office_revenue = rs.getFloat("box_office_revenue");
                float directorValue;
                if (directorMap.containsKey(director))
                    directorValue = directorMap.get(director);
                else
                    directorValue = directorAvr;
                float actorRevenue = calculateActorRevenue(lead, support1, support2);
                double predict = p[0] + p[1] * directorValue + p[2] * actorRevenue;
                printResult(chineseName, predict, box_office_revenue);

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

    private void printResult(String chineseName, double predict, float box_office_revenue) {
        System.out.println("movie name : " + chineseName + " predict box_office_revenue " + new DecimalFormat("#.0").format(predict) + "; real box_office_revenue :" + box_office_revenue);
    }

    private void buildAvgRevenueMap(Statement stmt, ResultSet rs) throws SQLException {
        // director avg box_office_revenue
        rs = stmt.executeQuery("select avg(box_office_revenue) as avr, substring_index(director, ' ', 1) as dir from mtime_revenue " +
                "where votes>=100 and release_date<'2013-01-01' " +
                "group by dir having   dir<>'' and length(dir) != character_length(dir)");
        while (rs.next()) {
            directorMap.put(rs.getString("dir"), rs.getFloat("avr"));
        }

        // supporting lead avg box_office_revenue
        rs = stmt.executeQuery("select avg(box_office_revenue) as avr, substring_index(substring_index(starring, ' ', -2), ' ', 1) as support from mtime_revenue " +
                "where votes>=100 and release_date<'2013-01-01' " +
                "group by support having support<>''" +
                "and length(support) != character_length(support)");
        while (rs.next()) {
            actorMap.put(rs.getString("support"), rs.getFloat("avr"));
        }

        // supporting lead avg box_office_revenue
        rs = stmt.executeQuery("select avg(box_office_revenue) as avr, substring_index(substring_index(starring, ' ', -3), ' ', 1) as support from mtime_revenue " +
                "where votes>=100 and release_date<'2013-01-01' " +
                "group by support having support<>''" +
                "and length(support) != character_length(support)");
        while (rs.next()) {
            String actor = rs.getString("support");
            float box_office_revenue = rs.getFloat("avr");
            if (actorMap.containsKey(actor))
                actorMap.put(actor, (actorMap.get(actor) + box_office_revenue) / 2);
            actorMap.put(rs.getString("support"), box_office_revenue);
        }

        // lead lead avg box_office_revenue
        rs = stmt.executeQuery("select avg(box_office_revenue) as avr, substring_index(starring, ' ', 1) as lead from mtime_revenue " +
                "where votes>=100 " +
                "group by lead having count(*)>1 and lead<>'' and length(lead) != character_length(lead)");
        while (rs.next()) {
            String actor = rs.getString("lead");
            float box_office_revenue = rs.getFloat("avr");
            if (actorMap.containsKey(actor))
                actorMap.put(actor, (actorMap.get(actor) + box_office_revenue) / 2);
            actorMap.put(actor, box_office_revenue);
        }
    }


    private double[] estimateParameter(Statement stmt, ResultSet rs) throws SQLException {
        ArrayList<Double> dList = new ArrayList<Double>();
        ArrayList<Double> aList = new ArrayList<Double>();
        ArrayList<Double> rList = new ArrayList<Double>();
        rs = stmt.executeQuery("select substring_index(director, ' ', 1) as dir, substring_index(starring, ' ', 1) as lead, " +
                "substring_index(substring_index(starring, ' ', -2), ' ', 1) as support1, " +
                "substring_index(substring_index(starring, ' ', -3), ' ', 1) as support2, box_office_revenue " +
                "from mtime_revenue where votes>=100");
        while (rs.next()) {
            String director = rs.getString("dir");
            String lead = rs.getString("lead");
            String support1 = rs.getString("support1");
            String support2 = rs.getString("support2");
            Float box_office_revenue = rs.getFloat("box_office_revenue");
            Float directorValue;
            if (directorMap.containsKey(director))
                directorValue = directorMap.get(director);
            else
                directorValue = directorAvr;
            double actorRevenue = calculateActorRevenue(lead, support1, support2);
            dList.add(directorValue.doubleValue());
            aList.add(actorRevenue);
            rList.add(box_office_revenue.doubleValue());

        }
        int m = 2;
        int n = rList.size();
        double[][] x = new double[n][m];
        double[] y = new double[n];
        for (int i = 0; i < rList.size(); i++) {
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
                "substring_index(substring_index(starring, ' ', -3), ' ', 1) as support2, box_office_revenue " +
                "from mtime_revenue where votes>=100 and release_date >= '2013-01-01' and release_date<'2014-01-01'");

        while (rs.next()) {
            String name = rs.getString("chinese_name");
            String director = rs.getString("dir");
            String lead = rs.getString("lead");
            String support1 = rs.getString("support1");
            String support2 = rs.getString("support2");
            Float box_office_revenue = rs.getFloat("box_office_revenue");
            float directorValue;
            if (directorMap.containsKey(director))
                directorValue = directorMap.get(director);
            else
                directorValue = directorAvr;
            float actorRevenue = calculateActorRevenue(lead, support1, support2);
            double predict = p[0] + p[1] * directorValue + p[2] * actorRevenue;
            System.out.println(name + " : predict box_office_revenue " + predict + " , real box_office_revenue " + box_office_revenue);


        }
    }


    private float calculateActorRevenue(String lead, String support1, String support2) {
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
        float actorRevenue = (leadValue + (support1Value + support2Value) / 2) / 2;
        return actorRevenue;
    }

    public static void main(String[] args) {
        new LinearRegressionRevenue().calculate("泰囧");
    }
}
