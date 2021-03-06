/*
 * Copyright 2011 - 2014 Cetas Software, Inc. All rights reserved. This is Cetas
 * proprietary and confidential material and its use is subject to license terms.
 */

package net.gtl.movieanalytics.data;

import net.gtl.movieanalytics.model.DBReader;
import net.gtl.movieanalytics.model.DataSetGenerator;

import java.util.List;
import java.util.Map;

/**
 * Created by Julia on 3/10/14.
 */
public class TestDataStore {

    private static InfoStore infoStore = InfoStore.getInstance();

    private static volatile TestDataStore instance = new TestDataStore();
    private List<Map<String, Object>> testSet;
    private List<Map<String, Double>> testDataSet;
    private int testDataSize;

    private TestDataStore() {
    }

    public static TestDataStore getInstance() {
        return instance;
    }

    public List<Map<String, Object>> getTestSet() {
        return testSet;
    }

    public void setTestSet(List<Map<String, Object>> testSet) {
        this.testSet = testSet;
    }

    public void setTestDataSet(List<Map<String, Double>> testDataSet) {
        this.testDataSet = testDataSet;
        this.testDataSize = testDataSet.size();
    }

    public int getTestDataSize() {
        return testDataSize;
    }

    public double[] getTestInput(int seqNum) {
        Map<String, Double> map = this.testDataSet.get(seqNum);
        return DataSetGenerator.getTestInput(map);
    }

    public double getTestActualResult(int seqNum) {
        Map<String, Double> map = this.testDataSet.get(seqNum);
        return getTestActualResult(map);
    }

    public double getTestActualResult(Map<String, Double> map) {
        double value = map.get(DBReader.resultFieldName).doubleValue();
        return value;
    }


    public String getTestDataString(int seqNum) {
        StringBuffer sBuff = new StringBuffer();
        Map<String, Object> map = testSet.get(seqNum);

        String name = "" + map.get("chinese_name");

        for (int i = 0; i < DBReader.paramFields.size(); i++) {
            String key = DBReader.paramFields.get(i).getName();
            String value = "" + map.get(key);
            String[] tmps = value.split(" ");
            int subNum = infoStore.getFeatureSubItemNum(key);
            if (tmps.length > subNum){
                value = "";
                for (int j = 0; j < subNum; j++) {
                    value += tmps[j] + " ";
                }
            }
            sBuff.append(key + ": " + value + ", ");
        }

        return "[" + name + "] -- " + sBuff.toString();
    }
}
