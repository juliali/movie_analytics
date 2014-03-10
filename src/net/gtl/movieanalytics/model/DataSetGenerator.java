/*
 * Copyright 2011 - 2014 Cetas Software, Inc. All rights reserved. This is Cetas
 * proprietary and confidential material and its use is subject to license terms.
 */

package net.gtl.movieanalytics.model;

import net.gtl.movieanalytics.data.TestDataStore;

import java.sql.SQLException;
import java.util.*;

/**
 * Created by Julia on 3/4/14.
 */
public class DataSetGenerator {

    private static TestDataStore tdStore = TestDataStore.getInstance();


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
    }

    public void doTrainning() {
        DBReader reader = new DBReader();
        reader.trainingData();

        double[][] x = reader.getX();
        double[] y = reader.getY();

        reader.closeDBConnection();
        LinearRegression lr = new LinearRegression(x, y);
    }

    public void getTestDataSet()  {
        DBReader reader = new DBReader();
        try {
            tdStore.setTestSet(reader.getTestDataSets());
            tdStore.setTestDataSet(reader.convertTestDataSet(tdStore.getTestSet()));
        } catch (SQLException e) {
            e.printStackTrace();
        }
        reader.closeDBConnection();
    }

    public double[] getPredictionInput(String chineseName) {
        DBReader reader = new DBReader();
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



    public static double[] getTestInput(Map<String, Double> map) {
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
        DBReader reader = new DBReader();
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
}
