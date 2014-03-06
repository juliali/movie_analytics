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

    private Map<String, Map<String, AverageCountPair>> numericValues;

    private String[] fieldNames = {idStr, "chinese_name", "director", "starring", "rate", "release_date", "type", "region", "votes", "language", "company", numericField};

    private String tableName = "mtime_revenue_3";
    private String condition = numericField + " IS NOT NULL and " + numericField + " > 0"
            + " and director <> '' and length(director) != character_length(director) "
            + " and starring <> '' and length(starring) != character_length(starring) ";


    private double[][] x;
    private double[] y;

    private List<Map<String,Double>> testDataSet;

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

    private Map<String, AverageCountPair> getOneDimension(String query, String nameFieldName, String valueFieldName, String countFieldName)
            throws SQLException {

        rs = stmt.executeQuery(query);
        Map<String, AverageCountPair> map = new HashMap<String, AverageCountPair>();
        float sumValue = 0;
        int sumCount = 0;
        while (rs.next()) {
            String name =  rs.getString(nameFieldName);
            Float avgValue = rs.getFloat(valueFieldName);
            Integer count = rs.getInt(countFieldName);

            if (map.get(name) != null) {
                System.err.println("The sub-dimension(" + nameFieldName + ") has existed.");
            } else {
                AverageCountPair pair = new AverageCountPair(avgValue, count);

                map.put(name, pair);

                sumValue += (avgValue * count);
                sumCount += count;
            }
        }

        float avg = sumValue/ sumCount;
        map.put(anyOne, new AverageCountPair(avg, sumCount));
        return map;
    }

    private void mergeMaps(String fieldName, Map<String, AverageCountPair> resultMap, Map<String, AverageCountPair> inputMap) {
        Set<Map.Entry<String, AverageCountPair>> entries = inputMap.entrySet();

        for ( Map.Entry<String, AverageCountPair> entry : entries ) {
            String inputMapKey = entry.getKey();
            AverageCountPair inputMapValue = inputMap.get(inputMapKey);
            if (resultMap.get(inputMapKey) != null) {
                AverageCountPair oldValue = resultMap.get(inputMapKey);
                if (!inputMapValue.equals(oldValue)) {
                    //System.out.println("*** For Field(" + fieldName + ") The key (" + inputMapKey + "): value (" + inputMapValue.getAverage() + ") has existed in result map, value (" + oldValue.getAverage() + ").");
                    int totalCount = inputMapValue.getCount() + oldValue.getCount();
                    float newAverage = ((inputMapValue.getAverage() * inputMapValue.getCount()) + (oldValue.getAverage() * oldValue.getCount())) / (totalCount);
                    resultMap.put(inputMapKey, new AverageCountPair(newAverage, totalCount));
                } else {
                    //System.out.println("--- For Field(" + fieldName + ") The key (" + inputMapKey + "): value (" + inputMapValue + ") has existed in result map, value (" + oldValue + ").");
                }
            } else {

                resultMap.put(inputMapKey, inputMapValue);
            }
        }
    }
    private void getNumericValueFromDB() throws SQLException {
        numericValues = new HashMap<String, Map<String, AverageCountPair>>();

        String countFieldName = "cnt";
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

            DBFieldType fieldType =  DataSetGenerator.paramFieldTypes[i];

            Map<String, AverageCountPair> map = new HashMap<String, AverageCountPair>();

            if (fieldType == DBFieldType.String) {
                int subFieldNum = DataSetGenerator.paramFieldItemNumber[i];
                Map<String,AverageCountPair> subMap; //= new HashMap<String, Float>();
                for (int n = 0; n < subFieldNum; n ++) {
                    String newFieldDef = "substring_index(substring_index(" + fieldName + ", ' ', " + (n + 1) +"), ' ', -1) as " + newFieldName;
                    String query = "select avg(" + numericField + ") as " +  valueFieldName + ", "
                            + " count(" + numericField + ") as " + countFieldName + ", "
                            + newFieldDef + " from " + tableName
                            +  " where " + "substring_index(substring_index(" + fieldName + ", ' ', " + (n + 1) +"), ' ', -1) <> substring_index(substring_index(" + fieldName + ", ' ', " + n +"), ' ', -1) and "
                            + condition + " and " + commonCondition + " group by " + newFieldName;

                    query += " having " + newFieldName+ "<>''";
                    subMap = getOneDimension(query, newFieldName, valueFieldName, countFieldName);
                    mergeMaps(fieldName, map, subMap);
                }

            } else if (fieldType == DBFieldType.Numeric) {
                String query = "select avg(" + numericField + ") as " +  valueFieldName + ", "
                        + " count(" + numericField + ") as " + countFieldName + ", "
                        + fieldName + " as " + newFieldName + " from "
                        + tableName +  " where " + condition + " and " + commonCondition + " group by " + newFieldName;

                map = getOneDimension(query, newFieldName, valueFieldName,countFieldName);

            } else if (fieldType == DBFieldType.Date) {
                String query = "select avg(" + numericField + ") as " +  valueFieldName + ", "
                        + " count(" + numericField + ") as " + countFieldName + ", "
                        + "substring_index(" + fieldName + ", '-', 1) as " + newFieldName + " from "
                        + tableName +  " where " + condition + " and " + commonCondition + " group by " + newFieldName;

                map = getOneDimension(query, newFieldName, valueFieldName,countFieldName);

            }

            numericValues.put(fieldName, map);
        }
    }

    private double[] getNumericValueForOneRecord(Map<String, Object> map) {
        double[] result = new double[DataSetGenerator.paramFields.length];

        for (int j = 0; j < DataSetGenerator.paramFields.length; j ++) {
            String fieldName = DataSetGenerator.paramFields[j];

            int itemNum = DataSetGenerator.paramFieldItemNumber[j];
            String itemName = "" + map.get(fieldName);


            String[] items;

            if (itemNum > 1) {
                String[] tmps = itemName.split(" ");
                if (tmps.length < itemNum) {
                    itemNum = tmps.length;
                }

                items = tmps;
            } else {
                items = new String[1];
                items[0] = itemName;
            }

            float itemValue = 0;
            for (int n = 0; n < itemNum; n ++) {
                String subItemName = items[n];
                if ((subItemName == null) || (subItemName.equals(""))) {
                    subItemName = anyOne;
                }

                AverageCountPair subItemValue = numericValues.get(fieldName).get(subItemName);

                if (subItemValue != null) {
                    itemValue += subItemValue.getAverage();
                } else {
                    itemValue += numericValues.get(fieldName).get(anyOne).getAverage();
                }
            }

            itemValue = itemValue/itemNum;
            result[j] = itemValue;
        }
        return result;
    }

    private void getInputValues() throws SQLException {
        getNumericValueFromDB();

        x = new double[trainingSet.size()][DataSetGenerator.paramFields.length];
        y = new double[trainingSet.size()];

        for (int i = 0; i < trainingSet.size(); i ++) {
            Map<String, Object> map = trainingSet.get(i);
            x[i] = getNumericValueForOneRecord(map);

            double actualRevenue = ((Double) map.get(DataSetGenerator.resultFieldName)).doubleValue();
            y[i] = actualRevenue;
        }
    }

    private void convertTestDataSet() {
        testDataSet = new ArrayList<Map<String, Double>> ();

        for (int i = 0; i < testSet.size(); i ++) {
            Map<String, Double> dataMap = new HashMap<String, Double>();

            Map<String, Object> map = testSet.get(i);
            /*for (int j = 0; j < DataSetGenerator.paramFields.length; j ++) {
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
            }*/
            double[] numericValues = getNumericValueForOneRecord(map);
            for (int j = 0; j < DataSetGenerator.paramFields.length; j ++) {
                //String fieldName = DataSetGenerator.paramFields[j];
                dataMap.put(DataSetGenerator.paramFields[j], numericValues[j]);
            }

            double revenue = ((Double) map.get(DataSetGenerator.resultFieldName)).doubleValue();
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

    public List<Map<String,Double>> getTestDataSet() {
        return testDataSet;
    }

    public String getTestDataString(int seqNum) {
        StringBuffer sBuff = new StringBuffer();
        Map<String, Object> map = testSet.get(seqNum);

        String name = "" + map.get("chinese_name");

        for (int i = 0; i < DataSetGenerator.paramFields.length; i ++) {
            String key = DataSetGenerator.paramFields[i];
            String value = "" + map.get(key);
            String[] tmps = value.split(" ");
            if (tmps.length > DataSetGenerator.paramFieldItemNumber[i] ) {
                value = "";
                for (int j = 0; j < DataSetGenerator.paramFieldItemNumber[i]; j ++) {
                    //try {
                    value += tmps[j] + " ";
                    //} catch (ArrayIndexOutOfBoundsException e) {
                    //    e.printStackTrace();
                    //}
                }
            }
            sBuff.append(key + ": " + value + ", ");
        }

        return "[" + name + "] -- " + sBuff.toString();
    }

    class AverageCountPair {
        private float average;
        private int count;

        public AverageCountPair(float avg, int cnt) {
            average = avg;
            count = cnt;
        }

        public float getAverage() {
            return average;
        }

        public void setAverage(float average) {
            this.average = average;
        }

        public int getCount() {
            return count;
        }

        public void setCount(int count) {
            this.count = count;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            AverageCountPair that = (AverageCountPair) o;

            if (Float.compare(that.average, average) != 0) return false;
            if (count != that.count) return false;

            return true;
        }
    }
}