import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.sql.*;
import java.util.*;

/**
 * Created by pivotal on 2/19/14.
 */
public class MysqlToArff {
    private static final int testRecordNum = 500;

    public static void main(String[] args) {
        HashMap<String, HashSet<String>> map = new HashMap<String, HashSet<String>>();
        String[] attributeList = {"director", "starring", "type", "release_date", "region", "certification", "rate"};
        for (String attr : attributeList) {
            map.put(attr, new HashSet<String>());
        }
        try {
            BufferedWriter bw = new BufferedWriter(new FileWriter("/tmp/movies_trainning.arff"));
            BufferedWriter testBw = new BufferedWriter(new FileWriter("/tmp/movies_test.arff"));
            exportMysqlToArff(map, bw, testBw);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static void exportMysqlToArff(HashMap<String, HashSet<String>> map, BufferedWriter bw, BufferedWriter testBw) throws IOException {
        Connection conn = null;
        Statement stmt = null;
        ResultSet rs = null;
        String path;

        writeHead(bw);

        int count = 0;
        try {
            Class.forName("com.mysql.jdbc.Driver");
            conn = DriverManager.getConnection("jdbc:mysql://192.168.3.166:3306/movie_data", "movie", "movie");
            stmt = conn.createStatement();

            String condition = "votes > 0 and runtime > 0";
            rs = stmt.executeQuery("select count(*) as tn from movie where " + condition);
            rs.next();
            int totalRecordNum = rs.getInt("tn");

            Set<Integer> randomSet = new HashSet<Integer>();
            Random random = new Random();
            while (randomSet.size() < testRecordNum) {
                int randomNumber = random.nextInt(totalRecordNum - 1) + 1;
                randomSet.add(randomNumber);
            }

            rs = stmt.executeQuery("select * from movie where " + condition);
            int rowCount = 0;
            while (rs.next()) {
                rowCount ++;
                StringBuffer buf = new StringBuffer();
                for (String key : map.keySet()) {
                    if (key.equals("rate"))
                        continue;
                    String att;
                    if (key.equals("release_date")) {
                        if (rs.getDate("release_date") == null)
                            att = "";
                        else
                            att = rs.getDate("release_date").toString().substring(0,4);
                    }
                    else
                        att = rs.getString(key).trim().replaceAll(" ", ";").replaceAll(",", ";");
                    map.get(key).add(att);
                    //bw.write(att);
                    //bw.write(",");
                    buf.append(att);
                    buf.append(",");
                    /*
                    if (count < 200 ) {
                        testBw.write(att);
                        testBw.write(",");
                    }
                    */
                }
                String rate = rateToClass(rs.getFloat("rate"));

                /*bw.write(rate);
                bw.newLine();
                if (count < 200) {
                    testBw.write(rate);
                    testBw.newLine();
                }
                */
                buf.append(rate);

                if (randomSet.contains(rowCount)) {
                    testBw.write(buf.toString());
                    testBw.newLine();
                } else {
                    bw.write(buf.toString());
                    bw.newLine();
                }

                if (++count % 100 == 0) {
                    bw.flush();
                    testBw.flush();
                }
                map.get("rate").add(rate);
            } // end while

            bw.flush();
            testBw.flush();
            for (String key : map.keySet()) {
                if (key != "rate")
                    writeTail(bw, key, map.get(key));
            }
            writeTail(bw, "rate", map.get("rate"));

        } catch (Exception e) {
            e.printStackTrace();
        } finally {
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


    private static void writeTail(BufferedWriter bw, String name, HashSet<String> set) throws IOException {
        bw.write("@ATTRIBUTE ");
        bw.write(name);
        bw.write(" {");
        Iterator<String> iter = set.iterator();
        while (iter.hasNext()) {
            bw.write(iter.next());
            if (iter.hasNext())
                bw.write(",");
        }
        bw.write("}");
        bw.newLine();
        bw.flush();
    }

    private static void writeHead(BufferedWriter bw) throws IOException {
        bw.write("@RELATION PERSON");
        bw.newLine();
        bw.write("@DATA");
        bw.newLine();
        bw.flush();
    }

    private static String rateToClass(float rate) {
        /*if (rate < 6)
            return "class_3";
        else if (rate < 8.5)
            return "class_2";
        else
            return "class_3";
        */
        if (rate >= 0 && rate < 3)
            return "class_8";
        else if (rate >= 3 && rate < 5)
            return "class_7";
        else if (rate >= 5 && rate < 6)
            return "class_6";
        else if (rate >= 6 && rate < 7)
            return "class_5";
        else if (rate >= 7 && rate < 8)
            return "class_4";
        else if (rate >= 8 && rate < 8.5)
            return "class_3";
        else if (rate >= 8.5 && rate < 9)
            return "class_2";
        else if (rate >= 9 && rate <= 10)
            return "class_1";
        return "class_n";

//        if (rate <=5)
//            return "class_5";
//        if (rate <= 6)
//            return "class_6";
//        if (rate <= 6.5)
//            return "class_6.5";
//        if (rate <= 6.5)
//            return "class_6.5";
//        if (rate >=9)
//            return "class_9";
//        if (rate >=8.5)
//            return "class_8.5";
//        if (rate >=8.2)
//            return "class_8.2";
//        else
//            return "class_" + rate;

    }
}
