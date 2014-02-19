
import com.sun.swing.internal.plaf.metal.resources.metal_pt_BR;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.sql.*;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;

/**
 * Created by pivotal on 2/19/14.
 */
public class MysqlToArff {
    public static void main(String[] args) throws IOException {
        Connection conn =null;
        Statement stmt = null;
        ResultSet rs = null;
        BufferedWriter bw = new BufferedWriter(new FileWriter("/tmp/movie.arff"));
        writeHead(bw);
        HashMap<String, HashSet<String>> map = new HashMap<String, HashSet<String>>();
        String[] attributeList = {"director", "starring", "type", "releaseDate", "region", "company"};
        for (String attr : attributeList) {
            map.put(attr, new HashSet<String>());
        }
        int count = 0;
        try {
            Class.forName("com.mysql.jdbc.Driver");
            conn = DriverManager.getConnection("jdbc:mysql://192.168.3.166:3306/movie", "root", "");
            stmt = conn.createStatement();
            rs = stmt.executeQuery("select * from movie");
            while (rs.next()) {
                for (String key : map.keySet()) {
                    String att;
                    if (key.equals("releaseDate"))
                        att = rs.getDate("releaseDate").toString();
                    else
                        att = rs.getString(key);
                    map.get(key).add(att);
                    bw.write(att);
                    bw.write(",");
                }
                bw.write(rateToClass(rs.getFloat("rate")));
                bw.newLine();
                if (++count % 100 == 0)
                    bw.flush();
            }
            bw.flush();
            for (String key : map.keySet()) {
                writeTail(bw, key, map.get(key));
            }

        } catch (Exception e) {

        } finally {
            if (stmt!=null)
                try {
                    stmt.close();
                } catch (SQLException e) {
                    e.printStackTrace();
                }
            if (conn!=null)
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
        bw.write("{");
        Iterator<String> iter = set.iterator();
        while (iter.hasNext()) {
            bw.write(iter.next());
            if (iter.hasNext())
                bw.write(",");
        }
        bw.write("}");
        bw.flush();
    }

    private static void writeHead(BufferedWriter bw) throws IOException {
        bw.write("@RELATION PERSON"); bw.newLine();
        bw.write("@DATA"); bw.newLine();
        bw.flush();
    }

    private static String rateToClass(float rate) {
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
    }
}
