/*
 * Copyright 2011 - 2014 Cetas Software, Inc. All rights reserved. This is Cetas
 * proprietary and confidential material and its use is subject to license terms.
 */

package net.gtl.movieanalytics.linearregression;

import java.sql.*;
import java.util.*;

/**
 * Created by Julia on 3/4/14.
 */
public class DBReader {

    private static final String idStr = "Id";
    private Connection conn = null;
    private Statement stmt = null;
    private ResultSet rs = null;

    private String numericField = DataSetGenerator.resultFieldName;
    private String anyOne = "ANYONE_HASNORECORD";

    private List<Map<String, Object>> trainingSet;
    private List<Map<String, Object>> testSet;

    private List<Integer> testDataIdList;

    private Map<String, Map<String, Float>> numericValues;


    private String tableName = "mtime_revenue_3";
    private String condition = numericField + " IS NOT NULL and " + numericField + " > 0"
            + " and director <> '' and length(director) != character_length(director) "
            + " and starring <> '' and length(starring) != character_length(starring) ";
    private String[] fieldNames = {idStr, "chinese_name", "director", "starring", "rate", "release_date", "type", "region", "votes", numericField};


    private double[][] x;
    private double[] y;

    private List<Map<String,Float>> testDataSet;

    public DBReader() {
        try {
            Class.forName("com.mysql.jdbc.Driver");
            conn = DriverManager.getConnection("jdbc:mysql://192.168.3.166:3306/movie_data?characterEncoding=UTF-8", "movie", "movie");
            stmt = conn.createStatement();
            getDataSets();
            getInputValues();
            convertTestDataSet();
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

    private void getDataSets() throws SQLException {
        trainingSet = new ArrayList<Map<String, Object>>();
        testSet = new ArrayList<Map<String, Object>>();
        String countQuery = "select count(*) as tn from " + tableName + " where " + condition;
        rs = stmt.executeQuery(countQuery);
        rs.next();
        int totalRecordNum = rs.getInt("tn");
        Set<Integer> testDataRowNumbers = DataSetGenerator.getTestDataRowNumbers(totalRecordNum);

        String query = "select * from " + tableName + " where " + condition;
        rs = stmt.executeQuery(query);

        testDataIdList = new ArrayList<Integer>();

        int rowNum = 1;
        while (rs.next()) {

            HashMap<String, Object> map = new HashMap<String, Object>();
            for (int i = 0; i < fieldNames.length; i++) {
                map.put(fieldNames[i], rs.getObject(fieldNames[i]));
            }

            if (testDataRowNumbers.contains(rowNum)) {
                this.testSet.add(map);

                int id = rs.getInt(idStr);
                testDataIdList.add(id);
            } else {
                this.trainingSet.add(map);

            }
            rowNum++;
        }
        return;
    }

    private Map<String, Float> getOneDimension(String query, String nameFieldName, String valueFieldName) throws SQLException {
        rs = stmt.executeQuery(query);
        Map<String, Float> map = new HashMap<String, Float>();
        while (rs.next()) {
            map.put(rs.getString(nameFieldName), rs.getFloat(valueFieldName));
        }

        float avg = getAverageOfOneDimension(map);
        map.put(anyOne, avg);
        return map;
    }


    private float getAverageOfOneDimension(Map<String, Float> map) {
        if ((map == null) || (map.size() == 0)){
            return 0;
        }

        float average = 0;
        for (float value : map.values())
            average += value;
        average /= map.size();

        return average;
    }

    private void getNumericValueFromDB() throws SQLException {
        numericValues = new HashMap<String, Map<String, Float>>();

        String valueFieldName = "average";
        String idString = "";

        for (Integer id : this.testDataIdList) {
            idString += id + ",";
        }

        idString = idString.substring(0, idString.length() - 1);

        String commonCondition = " " + idStr + " NOT in (" + idString + ")";

        for (int i = 0; i < DataSetGenerator.paramFields.length; i ++) {

            String fieldName = DataSetGenerator.paramFields[i];
            String newFieldName = fieldName + "_new";

            String newFieldDef = "";

            DBFieldType fieldType =  DataSetGenerator.paramFieldTypes[i];
            if (fieldType == DBFieldType.String) {
                newFieldDef = "substring_index(" + fieldName + ", ' ', 1) as " + newFieldName;
            } else if (fieldType == DBFieldType.Numeric) {
                newFieldDef = fieldName + " as " + newFieldName;
            } else if (fieldType == DBFieldType.Date) {
                newFieldDef = "substring_index(" + fieldName + ", '-', 1) as " + newFieldName;
            }

            String query = "select avg(" + numericField + ") as " +  valueFieldName + ", " + newFieldDef + " from "
                    + tableName +  " where " + condition + " and " + commonCondition + " group by " + newFieldName;

            if (fieldType == DBFieldType.String) {
                    query += " having " + newFieldName+ "<>''";
                    //query += " and length(" + newFieldName + ") != character_length(" + newFieldName + ")";
            }

            Map<String, Float> map = getOneDimension(query, newFieldName, valueFieldName);
            numericValues.put(fieldName, map);
        }
    }

    private void getInputValues() throws SQLException {
        getNumericValueFromDB();

        x = new double[trainingSet.size()][DataSetGenerator.paramFields.length];
        y = new double[trainingSet.size()];

        for (int i = 0; i < trainingSet.size(); i ++) {
            Map<String, Object> map = trainingSet.get(i);
            for (int j = 0; j < DataSetGenerator.paramFields.length; j ++) {
                String fieldName = DataSetGenerator.paramFields[j];
                String itemName = "" + map.get(fieldName);
                Float itemValue = numericValues.get(fieldName).get(itemName);
                if (itemValue != null) {
                    x[i][j] = itemValue.floatValue();
                } else {
                    x[i][j] = numericValues.get(fieldName).get(anyOne).floatValue();
                }
            }

            double actualRevenue = ((Double) map.get(DataSetGenerator.resultFieldName)).doubleValue();
            y[i] = actualRevenue;
        }
    }

    private void convertTestDataSet() {
        testDataSet = new ArrayList<Map<String, Float>> ();

        for (int i = 0; i < testSet.size(); i ++) {
            Map<String, Float> dataMap = new HashMap<String, Float>();

            Map<String, Object> map = testSet.get(i);
            for (int j = 0; j < DataSetGenerator.paramFields.length; j ++) {
                String fieldName = DataSetGenerator.paramFields[j];
                String itemName = "" + map.get(fieldName);

                float numbericValue = 0;

                Float itemValue = numericValues.get(fieldName).get(itemName);
                if (itemValue != null) {
                    numbericValue = itemValue.floatValue();
                } else {
                    numbericValue = numericValues.get(fieldName).get(anyOne).floatValue();
                }
                dataMap.put(fieldName, numbericValue);
            }

            float revenue = ((Double) map.get(DataSetGenerator.resultFieldName)).floatValue();
            dataMap.put(DataSetGenerator.resultFieldName, revenue);
            testDataSet.add(dataMap);
        }

        return;
    }

    public double[] getY() {
        return y;
    }

    public double[][] getX() {
        return x;
    }

    public List<Map<String,Float>> getTestDataSet() {
        return testDataSet;
    }

    public String getTestDataString(int seqNum) {
        StringBuffer sBuff = new StringBuffer();
        Map<String, Object> map = testSet.get(seqNum);
        Set<String> keySet = map.keySet();
        Iterator<String> iterator = keySet.iterator();
        while (iterator.hasNext()) {
            String key = iterator.next();
            String value = "" + map.get(key);
            sBuff.append(key + ": " + value + ", ");
        }

        return sBuff.toString();
    }
}