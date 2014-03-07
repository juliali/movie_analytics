/*
 * Copyright 2011 - 2014 Cetas Software, Inc. All rights reserved. This is Cetas
 * proprietary and confidential material and its use is subject to license terms.
 */

package net.gtl.movieanalytics.model;

import java.sql.SQLException;
import java.util.*;

/**
 * Created by Julia on 3/4/14.
 */
public class DataSetGenerator {
    private static InfoStore infoStore = InfoStore.getInstance();

    private List<Map<String, Object>> testSet;
    private List<Map<String, Double>> testDataSet;
    private double[][] x;
    private double[] y;

    private DBReader reader;

    public static Set<Integer> getTestDataRowNumbers(int totalRecordNum) {
        int testRecordNum = (int) (totalRecordNum * DBReader.testRecordPercentage);
        Set<Integer> randomSet = new HashSet<Integer>();
        Random random = new Random();
        while (randomSet.size() < testRecordNum) {
            int randomNumber = random.nextInt(totalRecordNum - 1) + 1;
            if (randomNumber != 0) {
                randomSet.add(randomNumber);
            }
        }

        return randomSet;
    }

    public DataSetGenerator() {
        reader = new DBReader();

        reader.trainingData();

        this.x = reader.getX();
        this.y = reader.getY();


        reader.closeDBConnection();
    }

    public void getTestDataSet() {
        reader.convertTestDataSet();
        this.testDataSet = reader.getTestDataSet();
        this.testSet = reader.getTestSet();
    }

    public int getTestDataSize() {
        return testDataSet.size();
    }

    public double[] getPredictionInput(String chineseName) {
        //DBReader reader = new DBReader();
        Map<String, Double> map = null;
        try {
            reader.initDBConnection();
            map = reader.getOneRecordData(chineseName);
        } catch (SQLException e) {
            e.printStackTrace();
        } finally {
            reader.closeDBConnection();
        }


        if (map == null) {
            return null;
        } else {
            return getTestInput(map);
        }
    }

    public double[] getTestInput(int seqNum) {
        Map<String, Double> map = this.testDataSet.get(seqNum);
        return getTestInput(map);
    }

    public double[] getTestInput(Map<String, Double> map) {
        int len = DBReader.paramFields.length;
        double[] inputs = new double[len];
        for (int i = 0; i < len; i++) {
            String fieldName = DBReader.paramFields[i];
            double value = map.get(fieldName).doubleValue();
            inputs[i] = value;
        }

        return inputs;
    }

    public double getTestActualResult(String chineseName) {
        double revenue = -1;
        //DBReader reader = new DBReader();
        try {
            reader.initDBConnection();
            revenue = reader.getActualRevenue(chineseName);
        } catch (SQLException e) {
            e.printStackTrace();
        } finally {
            reader.closeDBConnection();
        }
        return revenue;
    }

    public double getTestActualResult(int seqNum) {
        Map<String, Double> map = this.testDataSet.get(seqNum);
        return getTestActualResult(map);
    }

    public double getTestActualResult(Map<String, Double> map) {
        //Map<String, Double> map = this.testDataSet.get(seqNum);
        double value = map.get(DBReader.resultFieldName).doubleValue();
        return value;
    }

    public String getTestDataString(int seqNum) {
        StringBuffer sBuff = new StringBuffer();
        Map<String, Object> map = testSet.get(seqNum);

        String name = "" + map.get("chinese_name");

        for (int i = 0; i < DBReader.paramFields.length; i++) {
            String key = DBReader.paramFields[i];
            String value = "" + map.get(key);
            String[] tmps = value.split(" ");
            int subNum = infoStore.getFeatureSubItemNum(key);
            if (tmps.length > subNum){ //DBReader.paramFieldItemNumber[i]) {
                value = "";
                for (int j = 0; j < subNum /*DBReader.paramFieldItemNumber[i]*/; j++) {
                    value += tmps[j] + " ";
                }
            }
            sBuff.append(key + ": " + value + ", ");
        }

        return "[" + name + "] -- " + sBuff.toString();
    }

    public double[] getY() {
        return y;
    }

    public double[][] getX() {
        return x;
    }
}
