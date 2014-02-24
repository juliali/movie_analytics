import org.apache.commons.math3.stat.regression.OLSMultipleLinearRegression;

import java.sql.*;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.HashMap;

/**
 * Created by pivotal on 2/21/14.
 */
public class LinearRegressionVotes {
    private HashMap<String, Integer> directorMap = new HashMap<String, Integer>();
    private HashMap<String, Integer> actorMap = new HashMap<String, Integer>();
    private int actorAvr = 0;
    private int directorAvr = 0;

    public int calculate(String name) {
        Connection conn = null;
        Statement stmt = null;
        ResultSet rs = null;
        try {
            Class.forName("com.mysql.jdbc.Driver");
            conn = DriverManager.getConnection("jdbc:mysql://192.168.3.166:3306/movie_data?characterEncoding=UTF-8", "movie", "movie");
            stmt = conn.createStatement();
            buildAvgVotesMap(stmt, rs);
            double[] p = estimateParameter(stmt, rs);
            rs = stmt.executeQuery("select chinese_name, substring_index(director, ' ', 1) as dir, substring_index(starring, ' ', 1) as lead, " +
                    " substring_index(substring_index(starring, ' ', -2), ' ', 1) as support1, " +
                    " substring_index(substring_index(starring, ' ', -3), ' ', 1) as support2, votes" +
                    " from mtime_revenue where chinese_name like '%" + name + "%'");
            while (rs.next()) {
                String director = rs.getString("dir");
                String lead = rs.getString("lead");
                String support1 = rs.getString("support1");
                String support2 = rs.getString("support2");
                String chineseName = rs.getString("chinese_name");
                int votes = rs.getInt("votes");
                float directorValue;
                if (directorMap.containsKey(director))
                    directorValue = directorMap.get(director);
                else
                    directorValue = directorAvr;
                float actorVotes = calculateActorVotes(lead, support1, support2);
                Double predict = p[0] + p[1] * directorValue + p[2] * actorVotes;
                return predict.intValue();
            }
            return 0;
        } catch (Exception e) {
            e.printStackTrace();
            return 0;
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

    private void printResult(String chineseName, double predict, float votes) {
        System.out.println("movie name : " + chineseName + " predict votes " + new DecimalFormat("#.0").format(predict) + "; real votes :" + votes);
    }

    private void buildAvgVotesMap(Statement stmt, ResultSet rs) throws SQLException {
        // director avg votes
        rs = stmt.executeQuery("select avg(votes) as avr, substring_index(director, ' ', 1) as dir from mtime_revenue " +
                "where votes>=100 and release_date<'2013-01-01' " +
                "group by dir having   dir<>'' and length(dir) != character_length(dir)");
        while (rs.next()) {
            directorMap.put(rs.getString("dir"), rs.getInt("avr"));
        }

        // supporting lead avg votes
        rs = stmt.executeQuery("select avg(votes) as avr, substring_index(substring_index(starring, ' ', -2), ' ', 1) as support from mtime_revenue " +
                "where votes>=100 and release_date<'2013-01-01' " +
                "group by support having support<>''" +
                "and length(support) != character_length(support)");
        while (rs.next()) {
            actorMap.put(rs.getString("support"), rs.getInt("avr"));
        }

        // supporting lead avg votes
        rs = stmt.executeQuery("select avg(votes) as avr, substring_index(substring_index(starring, ' ', -3), ' ', 1) as support from mtime_revenue " +
                "where votes>=100 and release_date<'2013-01-01' " +
                "group by support having support<>''" +
                "and length(support) != character_length(support)");
        while (rs.next()) {
            String actor = rs.getString("support");
            int votes = rs.getInt("avr");
            if (actorMap.containsKey(actor))
                actorMap.put(actor, (actorMap.get(actor) + votes) / 2);
            actorMap.put(rs.getString("support"), votes);
        }

        // lead lead avg votes
        rs = stmt.executeQuery("select avg(votes) as avr, substring_index(starring, ' ', 1) as lead from mtime_revenue " +
                "where votes>=100 " +
                "group by lead having count(*)>1 and lead<>'' and length(lead) != character_length(lead)");
        while (rs.next()) {
            String actor = rs.getString("lead");
            int votes = rs.getInt("avr");
            if (actorMap.containsKey(actor))
                actorMap.put(actor, (actorMap.get(actor) + votes) / 2);
            actorMap.put(actor, votes);
        }
    }


    private double[] estimateParameter(Statement stmt, ResultSet rs) throws SQLException {
        ArrayList<Double> dList = new ArrayList<Double>();
        ArrayList<Double> aList = new ArrayList<Double>();
        ArrayList<Double> rList = new ArrayList<Double>();
        rs = stmt.executeQuery("select substring_index(director, ' ', 1) as dir, substring_index(starring, ' ', 1) as lead, " +
                "substring_index(substring_index(starring, ' ', -2), ' ', 1) as support1, " +
                "substring_index(substring_index(starring, ' ', -3), ' ', 1) as support2, votes " +
                "from mtime_revenue where votes>=100");
        while (rs.next()) {
            String director = rs.getString("dir");
            String lead = rs.getString("lead");
            String support1 = rs.getString("support1");
            String support2 = rs.getString("support2");
            Integer votes = rs.getInt("votes");
            Integer directorValue;
            if (directorMap.containsKey(director))
                directorValue = directorMap.get(director);
            else
                directorValue = directorAvr;
            double actorVotes = calculateActorVotes(lead, support1, support2);
            dList.add(directorValue.doubleValue());
            aList.add(actorVotes);
            rList.add(votes.doubleValue());

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
                "substring_index(substring_index(starring, ' ', -3), ' ', 1) as support2, votes " +
                "from mtime_revenue where votes>=100 and release_date >= '2013-01-01' and release_date<'2014-01-01'");

        while (rs.next()) {
            String name = rs.getString("chinese_name");
            String director = rs.getString("dir");
            String lead = rs.getString("lead");
            String support1 = rs.getString("support1");
            String support2 = rs.getString("support2");
            Integer votes = rs.getInt("votes");
            float directorValue;
            if (directorMap.containsKey(director))
                directorValue = directorMap.get(director);
            else
                directorValue = directorAvr;
            float actorVotes = calculateActorVotes(lead, support1, support2);
            double predict = p[0] + p[1] * directorValue + p[2] * actorVotes;
            System.out.println(name + " : predict votes " + predict + " , real votes " + votes);


        }
    }


    private float calculateActorVotes(String lead, String support1, String support2) {
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
        float actorVotes = (leadValue + (support1Value + support2Value) / 2) / 2;
        return actorVotes;
    }

    public static void main(String[] args) {
        System.out.print(new LRVotesToRevenue().predictRevenueByVotes(new LinearRegressionVotes().calculate("美国队长")));
    }
}
