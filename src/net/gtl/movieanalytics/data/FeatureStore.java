/*
 * Copyright 2011 - 2014 Cetas Software, Inc. All rights reserved. This is Cetas
 * proprietary and confidential material and its use is subject to license terms.
 */

package net.gtl.movieanalytics.data;

import java.util.Map;
import java.util.Set;

/**
 * Created by Julia on 3/10/14.
 */
public class FeatureStore {
    //private static InfoStore infoStore = InfoStore.getInstance();
    //private static FeatureStore featureStore = FeatureStore.getInstance();
    private static volatile FeatureStore instance = new FeatureStore();

    private double[] parameters;
    private Map<String, Map<String, AverageCountPair>> numericValues;
    private Set<Integer> testRowNumber;

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
        for (int i = 0; i < this.parameters.length; i ++) {
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

    public Set<Integer> getTestRowNumber() {
        return testRowNumber;
    }

    public void setTestRowNumber(Set<Integer> testRowNumber) {
        this.testRowNumber = testRowNumber;
    }
}
