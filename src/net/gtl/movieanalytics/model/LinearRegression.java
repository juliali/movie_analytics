/*
 * Copyright 2011 - 2014 Cetas Software, Inc. All rights reserved. This is Cetas
 * proprietary and confidential material and its use is subject to license terms.
 */

package net.gtl.movieanalytics.model;

import net.gtl.movieanalytics.data.FeatureStore;
import org.apache.commons.math3.stat.regression.OLSMultipleLinearRegression;

/**
 * Created by Julia on 3/4/14.
 */
public class LinearRegression {
    private static FeatureStore featureStore = FeatureStore.getInstance();

    public LinearRegression(double[][] x, double[] y) {
        double[] parameters = estimateParameter(x,y);
        featureStore.setParameters(parameters);
    }

    private void printTestData(double[][] x, double[] y) {
        for (int i = 0; i < y.length; i ++) {
            System.out.print("Training DS " + i + ": ");
            for (int j = 0; j < x[i].length; j ++) {
                System.out.print("x" + (j+1) + ": " + x[i][j]);
                if (j < x[i].length - 1) {
                    System.out.print("; ");
                }

            }
            System.out.println(" || y: " + y[i]);
        }
    }
    private double[] estimateParameter(double[][] x, double[] y) {
        //printTestData(x, y);

        OLSMultipleLinearRegression ols = new OLSMultipleLinearRegression();
        ols.newSampleData(y, x);
        return ols.estimateRegressionParameters();
    }
}
