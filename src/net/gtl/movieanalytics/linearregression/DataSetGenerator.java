/*
 * Copyright 2011 - 2014 Cetas Software, Inc. All rights reserved. This is Cetas
 * proprietary and confidential material and its use is subject to license terms.
 */

package net.gtl.movieanalytics.linearregression;

import java.util.*;

/**
 * Created by Julia on 3/4/14.
 */
public class DataSetGenerator {
    public static final DBFieldType[] paramFieldTypes = {DBFieldType.String, DBFieldType.String, DBFieldType.String, DBFieldType.String};
    public static final String[] paramFields = {"director", "starring", "type", "region"};
    public static final String resultFieldName = "revenue";

    private static final double testRecordPercentage = 0.3;

    private List<Map<String,Float>> testDataSet;
    private double[][] x;
    private double[] y;

    private DBReader reader;

    public static Set<Integer> getTestDataRowNumbers(int totalRecordNum) {
        int testRecordNum = (int) (totalRecordNum * testRecordPercentage);
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

        this.x = reader.getX();
        this.y = reader.getY();
        this.testDataSet = reader.getTestDataSet();
    }

    public int getTestDataSize() {
        return testDataSet.size();
    }

    public double[] getTestInput(int seqNum) {
        Map<String, Float> map = this.testDataSet.get(seqNum);
        int len = DataSetGenerator.paramFields.length;
        double[] inputs = new double[len];
        for (int i = 0; i < len; i ++ ) {
            String fieldName = paramFields[i];
            double value = map.get(fieldName).doubleValue();
            inputs[i] = value;
        }

        return inputs;
    }

    public double getTestActualResult(int seqNum) {
        Map<String, Float> map = this.testDataSet.get(seqNum);
        double value = map.get(resultFieldName).doubleValue();
        return value;
    }

    public String getTestDataString(int seqNum) {
        return reader.getTestDataString(seqNum);
    }

    public double[] getY() {
        return y;
    }

    public double[][] getX() {
        return x;
    }
}
