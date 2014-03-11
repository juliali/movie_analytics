/*
 * Copyright 2011 - 2014 Cetas Software, Inc. All rights reserved. This is Cetas
 * proprietary and confidential material and its use is subject to license terms.
 */

package net.gtl.movieanalytics.data;

import net.gtl.movieanalytics.utils.FileIO;

import java.util.List;
import java.util.Map;

/**
 * Created by Julia on 3/10/14.
 */
public class FeatureStore {
    private static InfoStore infoStore = InfoStore.getInstance();
    //private static FeatureStore featureStore = FeatureStore.getInstance();
    private static volatile FeatureStore instance = new FeatureStore();

    private double[] parameters;
    private Map<String, Map<String, AverageCountPair>> numericValues;
    //private Set<Integer> testRowNumber;
    //private List<Integer> testDataSetIds;

    private FeatureStore() {
    }

    public static FeatureStore getInstance() {
        return instance;
    }

    public double[] getParameters() {
        return parameters;
    }

    public void setParameters(double[] parameters) {
        this.parameters = parameters;
        for (int i = 0; i < this.parameters.length; i++) {
            System.out.print("p" + i + ": " + this.parameters[i] + "; ");
        }
        System.out.println("\n");
    }

    public Map<String, Map<String, AverageCountPair>> getNumericValues() {
        return numericValues;
    }

    public void setNumericValues(Map<String, Map<String, AverageCountPair>> numericValues) {
        this.numericValues = numericValues;
    }

    //public List<Integer> getTestDataSetIds() {
    //    return testDataSetIds;
    //}

    public String getTestDataSetIds() {
        String filePath = infoStore.getTestDataSetIdFilePath();
        String ids = FileIO.readTextFile(filePath);
        return ids;
    }
    public void setTestDataSetIds(List<Integer> testDataSetIds) {
        //this.testDataSetIds = testDataSetIds;
        //if (infoStore.isNewGeneratedTestDataSet()) {
            String filePath = infoStore.getTestDataSetIdFilePath();

            StringBuilder sb = new StringBuilder();
            for (int id : testDataSetIds) {
                sb.append("" + id);
                sb.append(",");
            }
            String str = sb.toString();
            if ((str != null) && (!str.equals(""))) {
                str = str.substring(0, str.length() -1);
            }

            FileIO.writeTextFile(filePath, str);
        //}
    }

    public static void setInstance(FeatureStore instance) {

        FeatureStore.instance = instance;
    }
}
