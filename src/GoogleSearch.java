import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.select.Elements;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.sql.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created by pivotal on 3/4/14.
 */
public class GoogleSearch {
    public int getResult(String movieName) throws Exception {

        String url = "http://www.google.com/search?q=《" + movieName + "》+-+电影&safe=strict";

        URL obj = new URL(url);
        HttpURLConnection con = (HttpURLConnection) obj.openConnection();

        // optional default is GET
        con.setRequestMethod("GET");

        //add request header
        con.setRequestProperty("User-Agent", "Mozilla/5.0");

        int responseCode = con.getResponseCode();
        System.out.println("\nSending 'GET' request to URL : " + url);
        System.out.println("Response Code : " + responseCode);

        BufferedReader in = new BufferedReader(
                new InputStreamReader(con.getInputStream()));
        String inputLine;
        StringBuffer response = new StringBuffer();

        while ((inputLine = in.readLine()) != null) {
            response.append(inputLine);
        }
        in.close();

        //print result
        String result = response.toString();
        Document doc = Jsoup.parse(result);
        Elements elements = doc.select("div.sd");
        if (elements != null && elements.size() == 1) {
            String str = elements.first().text();
            String regEx = "[^0-9]";
            Pattern p = Pattern.compile(regEx);
            Matcher m = p.matcher(str);
            return Integer.valueOf(m.replaceAll("").trim());
        }
        return 0;
    }

    public void getFilm() {
        Connection conn = null;
        PreparedStatement stmt = null;
        ResultSet rs = null;
        int id = 0;
        try {
            Class.forName("com.mysql.jdbc.Driver");
            conn = DriverManager.getConnection("jdbc:mysql://10.34.32.35:3306/movie_data?characterEncoding=UTF-8", "movie", "movie");
            String sql = "select id, chinese_name from movie where id > ?";
            stmt = conn.prepareStatement(sql);
            while (true) {
                stmt.setInt(1, id);
                rs = stmt.executeQuery();
                try {
                    while (rs.next()) {
                        id = rs.getInt("id");
                        String chineseName = rs.getString("chinese_name");
                        String url = "http://www.google.com/search?q=《" + chineseName + "》+-+电影&safe=strict";

                        URL obj = new URL(url);
                        HttpURLConnection con = (HttpURLConnection) obj.openConnection();

                        // optional default is GET
                        con.setRequestMethod("GET");

                        //add request header
                        con.setRequestProperty("User-Agent", "Mozilla/5.0");

                        int responseCode = con.getResponseCode();

                        BufferedReader in = new BufferedReader(
                                new InputStreamReader(con.getInputStream()));
                        String inputLine;
                        StringBuffer response = new StringBuffer();
                        Thread.sleep(1000);
                        while ((inputLine = in.readLine()) != null) {
                            response.append(inputLine);
                        }
                        in.close();

                        //print result
                        String result = response.toString();
                        Document doc = Jsoup.parse(result);
                        Elements elements = doc.select("div.sd");
                        if (elements != null && elements.size() == 1) {
                            String str = elements.first().text();
                            String regEx = "[^0-9]";
                            Pattern p = Pattern.compile(regEx);
                            Matcher m = p.matcher(str);
                            System.out.println(id + " " + chineseName + " " + Integer.valueOf(m.replaceAll("").trim()));
                        }


                    }
                } catch (Exception e) {
                    e.printStackTrace();
                    Thread.sleep(10000);
                    continue;
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

    public static void main(String[] args) throws Exception {
        GoogleSearch search = new GoogleSearch();
        search.getFilm();

    }


}
