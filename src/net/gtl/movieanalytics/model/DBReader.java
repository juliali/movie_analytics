/*
 * Copyright 2011 - 2014 Cetas Software, Inc. All rights reserved. This is GTL
 * proprietary and confidential material and its use is subject to license terms.
 */

package net.gtl.movieanalytics.model;

import net.gtl.movieanalytics.data.*;

import java.sql.*;
import java.util.*;

/**
 * Created by Julia on 3/4/14.
 */
public class DBReader {

    private static InfoStore infoStore = InfoStore.getInstance();
    public static final List<Feature> paramFields = infoStore.getFeaturesInModel();
    public static final String resultFieldName = infoStore.getResultFieldName();



    private String numericField = resultFieldName;
    private String[] fieldNames = infoStore.getAllFeatureNames();

    private String tableName = infoStore.getTableName();
    private String condition = numericField + " IS NOT NULL and " + numericField + " > 0"
            + " and director <> '' and length(director) != character_length(director) "
            + " and starring <> '' and length(starring) != character_length(starring) ";

    private String datasetOrder = " order by id asc ";

    private String sourceTableName = infoStore.getSourceTableName();
    private String hostIP = infoStore.getDbHost();

    private static FeatureStore featureStore = FeatureStore.getInstance();

    private static final String idStr = "Id";
    private String anyOne = "ANYONE_HASNORECORD";

    private Connection conn = null;
    private Statement stmt = null;
    private ResultSet rs = null;

    private double[][] x;
    private double[] y;

    public DBReader() {
        initDBConnection();
    }

    public void trainingData() {
        try {
            List<Map<String, Object>> trainingSet = getTrainingDataSets();
            getInputValues(trainingSet);
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public void initDBConnection() {
        try {
            Class.forName("com.mysql.jdbc.Driver");
            conn = DriverManager.getConnection("jdbc:mysql://" + hostIP + "/movie_data?characterEncoding=UTF-8", "movie", "movie");
            stmt = conn.createStatement();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void closeDBConnection() {
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

    public double getActualRevenue(String chineseName) throws SQLException {
        String query = "select " + resultFieldName + " from " + tableName + " where chinese_name = '" + chineseName + "'";
        rs = stmt.executeQuery(query);
        if (rs.next()) {
            double revenue = (double) rs.getFloat(resultFieldName);
            return revenue;
        } else {
            return -1;
        }
    }

    public Map<String, Double> getOneRecordData(String chineseName) throws SQLException {
        String query = "select *, max(release_date) as latest_release from " + sourceTableName + " where chinese_name = '" + chineseName + "' group by chinese_name";
        rs = stmt.executeQuery(query);
        HashMap<String, Object> map = new HashMap<String, Object>();

        if (rs.next()) {
            for (int i = 0; i < paramFields.size(); i++) {
                String fieldName = paramFields.get(i).getName();
                Object value = null;
                try {
                    value = rs.getObject(fieldName);
                } catch (SQLException e) {
                    e.printStackTrace();
                }

                if (value != null) {
                    map.put(fieldName, value);
                }
            }

            Map<String, Double> dataMap = new HashMap<String, Double>();

            double[] numericValues = getNumericValueForOneRecord(map);
            for (int j = 0; j < paramFields.size(); j++) {
                dataMap.put(paramFields.get(j).getName(), numericValues[j]);
            }
            return dataMap;

        } else {
            return null;
        }

    }

    private List<Map<String, Object>> getTrainingDataSets() throws SQLException {
        boolean isNewGenerated = infoStore.isNewGeneratedTestDataSet();

        if (isNewGenerated) {
            return getNewGeneratedTrainingDataSets();
        } else {
            return getTrainingDataSetsBasedOnExistingTestDataSet();
        }
    }

    private List<Map<String, Object>> getTrainingDataSetsBasedOnExistingTestDataSet()  throws  SQLException {
        String commonCondition = getIdsCondition(true);
        List<Map<String, Object>> trainingSet = new ArrayList<Map<String, Object>>();
        String query  = "select * from " + tableName + " where " + condition + commonCondition;
        rs = stmt.executeQuery(query);

        while (rs.next()) {

            HashMap<String, Object> map = new HashMap<String, Object>();
            for (int i = 0; i < fieldNames.length; i++) {

                try {
                    Object value = rs.getObject(fieldNames[i]);
                    map.put(fieldNames[i], value);
                } catch (SQLException e) {
                    System.err.println("The field of " + fieldNames[i] + " doesn't exist in training dataset.");
                }
            }
            trainingSet.add(map);
        }

        return trainingSet;
    }

    private List<Map<String, Object>> getNewGeneratedTrainingDataSets() throws SQLException {
        List<Map<String, Object>> trainingSet = new ArrayList<Map<String, Object>>();

        String countQuery = "select count(*) as tn from " + tableName + " where " + condition;
        rs = stmt.executeQuery(countQuery);
        rs.next();
        int totalRecordNum = rs.getInt("tn");
        Set<Integer> testDataRowNumbers = DataSetGenerator.getTestDataRowNumbers(totalRecordNum);

        String query = "select * from " + tableName + " where " + condition + datasetOrder;
        rs = stmt.executeQuery(query);

        List<Integer> testDataIdList = new ArrayList<Integer>();

        int rowNum = 1;
        while (rs.next()) {

            HashMap<String, Object> map = new HashMap<String, Object>();
            for (int i = 0; i < fieldNames.length; i++) {

                try {
                    Object value = rs.getObject(fieldNames[i]);
                    map.put(fieldNames[i], value);
                } catch (SQLException e) {
                    System.err.println("The field of " + fieldNames[i] + " doesn't exist in training dataset.");
                }
            }

            if (testDataRowNumbers.contains(rowNum)) {
                int id = rs.getInt(idStr);
                testDataIdList.add(id);
            } else {
                trainingSet.add(map);
            }
            rowNum++;
        }
        featureStore.setTestDataSetIds(testDataIdList);

        return trainingSet;
    }

    private String getIdsCondition(boolean isExcluded) {
        String commonCondition = "";
        String idString = featureStore.getTestDataSetIds();//featureStore.getTestDataSetIds();

        if (!idString.equals("")) {
            String str = "";
            if (isExcluded) {
                str = " NOT ";
            }
            commonCondition =   " and " + idStr + str + " in (" + idString + ")";
        }

        return commonCondition;
    }

    public List<Map<String, Object>> getTestDataSets() throws SQLException {
        List<Map<String, Object>> testSet = new ArrayList<Map<String, Object>>();
        String countQuery = "select count(*) as tn from " + tableName + " where " + condition;
        rs = stmt.executeQuery(countQuery);
        rs.next();

        String commonCondition = getIdsCondition(false);
        String query = "select * from " + tableName + " where " + condition + commonCondition;
        rs = stmt.executeQuery(query);

        while (rs.next()) {
            HashMap<String, Object> map = new HashMap<String, Object>();
            for (int i = 0; i < fieldNames.length; i++) {
                try {
                    Object value = rs.getObject(fieldNames[i]);
                    map.put(fieldNames[i], value);
                } catch (SQLException e) {
                    System.err.println("The field of " + fieldNames[i] + " doesn't exist in test dataset.");
                }
            }
            testSet.add(map);
        }
        return testSet;
    }

    private Map<String, AverageCountPair> getOneDimension(String query, String nameFieldName, String valueFieldName, String countFieldName)
            throws SQLException {

        System.out.println(query);
        rs = stmt.executeQuery(query);
        Map<String, AverageCountPair> map = new HashMap<String, AverageCountPair>();
        float sumValue = 0;
        int sumCount = 0;
        while (rs.next()) {
            String name = rs.getString(nameFieldName);
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

        float avg = sumValue / sumCount;
        map.put(anyOne, new AverageCountPair(avg, sumCount));
        return map;
    }

    private void mergeMaps(String fieldName, Map<String, AverageCountPair> resultMap, Map<String, AverageCountPair> inputMap) {
        Set<Map.Entry<String, AverageCountPair>> entries = inputMap.entrySet();

        for (Map.Entry<String, AverageCountPair> entry : entries) {
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
        Map<String, Map<String, AverageCountPair>> numericValues = new HashMap<String, Map<String, AverageCountPair>>();

        String countFieldName = "cnt";
        String valueFieldName = "average";

        String commonCondition = getIdsCondition(true);

        for (int i = 0; i < paramFields.size(); i++) {

            String fieldName = paramFields.get(i).getName();
            String newFieldName = fieldName + "_new";

            DBFieldType fieldType = infoStore.getFeatureType(fieldName);

            Map<String, AverageCountPair> map = new HashMap<String, AverageCountPair>();

            if (fieldType == DBFieldType.Numeric) {
                System.out.println("For Numeric Field(" + fieldName + "), we put its value into parameter input data x[i] directly.");

            } else {

                if (fieldType == DBFieldType.String) {
                    int subFieldNum = infoStore.getFeatureSubItemNum(fieldName);
                    Map<String, AverageCountPair> subMap;
                    for (int n = 0; n < subFieldNum; n++) {
                        String newFieldDef = "substring_index(substring_index(" + fieldName + ", ' ', " + (n + 1) + "), ' ', -1) as " + newFieldName;
                        String query = "select avg(" + numericField + ") as " + valueFieldName + ", "
                                + " count(" + numericField + ") as " + countFieldName + ", "
                                + newFieldDef + " from " + tableName
                                + " where " + "substring_index(substring_index(" + fieldName + ", ' ', " + (n + 1) + "), ' ', -1) <> substring_index(substring_index(" + fieldName + ", ' ', " + n + "), ' ', -1) and "
                                + condition + commonCondition + " group by " + newFieldName;

                        query += " having " + newFieldName + "<>''";
                        subMap = getOneDimension(query, newFieldName, valueFieldName, countFieldName);
                        mergeMaps(fieldName, map, subMap);
                    }

                } else if (fieldType == DBFieldType.Date) {
                    String query = "select avg(" + numericField + ") as " + valueFieldName + ", "
                            + " count(" + numericField + ") as " + countFieldName + ", "
                            + "substring_index(" + fieldName + ", '-', 1) as " + newFieldName + " from "
                            + tableName + " where " + condition + commonCondition + " group by " + newFieldName;

                    map = getOneDimension(query, newFieldName, valueFieldName, countFieldName);

                }

                numericValues.put(fieldName, map);
            }
        }
        featureStore.setNumericValues(numericValues);
    }

    private double[] getNumericValueForOneRecord(Map<String, Object> map) {
        double[] result = new double[paramFields.size()];

        for (int j = 0; j < paramFields.size(); j++) {
            String fieldName = paramFields.get(j).getName();
            if (map.get(fieldName) == null) {
                continue;
            }

            DBFieldType fieldType = infoStore.getFeatureType(fieldName);
            double itemValue = 0;
            if (fieldType == DBFieldType.Numeric) {
                itemValue = getNumericDataFromDB(map, fieldName);
            } else {

                int itemNum = infoStore.getFeatureSubItemNum(fieldName);
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

                itemValue = 0;
                for (int n = 0; n < itemNum; n++) {
                    String subItemName = items[n];
                    if ((subItemName == null) || (subItemName.equals(""))) {
                        subItemName = anyOne;
                    }

                    Map<String, Map<String, AverageCountPair>> numericValues = featureStore.getNumericValues();
                    AverageCountPair subItemValue = numericValues.get(fieldName).get(subItemName);

                    if (subItemValue != null) {
                        itemValue += subItemValue.getAverage();
                    } else {
                        itemValue += numericValues.get(fieldName).get(anyOne).getAverage();
                    }
                }

                itemValue = itemValue / itemNum;

            }

            FeatureFunction function = this.paramFields.get(j).getFunction();
            if (function == null) {
                result[j] = itemValue;
            } else {
                FeatureFunctionType type = function.getType();
                //System.out.println("Function type is " + type.toString());
                double[] arguments = function.getArguments();
                if (type == FeatureFunctionType.LOGE) {
                    if (itemValue == 0) {
                        result[j] = 0;
                    } else {
                        result[j] = Math.log(itemValue);
                    }
                } else if (type == FeatureFunctionType.MULTIPLE) {
                    result[j] = itemValue * arguments[0];
                } else if (type == FeatureFunctionType.POWER) {
                    result[j] = Math.pow(itemValue, arguments[0]);
                }

                //if ((result[j] == Double.POSITIVE_INFINITY) || (result[j] == Double.NEGATIVE_INFINITY)) {
                //    System.out.println(type.toString() + " : " + itemValue);
                //}
            }
        }
        return result;
    }

    private void getInputValues(List<Map<String, Object>> trainingSet) throws SQLException {
        getNumericValueFromDB();

        x = new double[trainingSet.size()][paramFields.size()];
        y = new double[trainingSet.size()];

        for (int i = 0; i < trainingSet.size(); i++) {
            Map<String, Object> map = trainingSet.get(i);
            x[i] = getNumericValueForOneRecord(map);

            double actualResult = getNumericDataFromDB(map, resultFieldName);
            y[i] = actualResult;
        }
    }

    private double getNumericDataFromDB(Map<String, Object> map, String fieldName) {
        Object obj = map.get(fieldName);
        String className = obj.getClass().toString();
        double actualResult = 0;
        if (className.indexOf("Double") != -1) {
            Double itemValue = (Double) obj;
            actualResult = itemValue.doubleValue();
        } else if (className.indexOf("Integer") != -1) {
            Integer itemValue = (Integer) obj;
            actualResult = itemValue.doubleValue();
        } else {
            Float itemValue = (Float) obj;
            actualResult = itemValue.doubleValue();
        }
        return actualResult;
    }
    public List<Map<String, Double>> convertTestDataSet(List<Map<String, Object>> testSet) {

        List<Map<String, Double>> testDataSet = new ArrayList<Map<String, Double>>();

        for (int i = 0; i < testSet.size(); i++) {
            Map<String, Double> dataMap = new HashMap<String, Double>();

            Map<String, Object> map = testSet.get(i);

            double[] numericValues = getNumericValueForOneRecord(map);
            for (int j = 0; j < paramFields.size(); j++) {
                dataMap.put(paramFields.get(j).getName(), numericValues[j]);
            }

            double revenue = getNumericDataFromDB(map, resultFieldName);
            dataMap.put(resultFieldName, revenue);
            testDataSet.add(dataMap);
        }

        return testDataSet;
    }

    public double[] getY() {
        return y;
    }

    public double[][] getX() {
        return x;
    }
}