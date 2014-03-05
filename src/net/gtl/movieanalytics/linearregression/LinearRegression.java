/*
 * Copyright 2011 - 2014 Cetas Software, Inc. All rights reserved. This is Cetas
 * proprietary and confidential material and its use is subject to license terms.
 */

package net.gtl.movieanalytics.linearregression;

import org.apache.commons.math3.stat.regression.OLSMultipleLinearRegression;

/**
 * Created by Julia on 3/4/14.
 */
public class LinearRegression {

    private double[] parameters;

    public LinearRegression(double[][] x, double[] y) {
        parameters = estimateParameter(x,y);
        for (int i = 0; i < parameters.length; i ++) {
            System.out.print("p" + i + ": " + parameters[i] + "; ");
        }
        System.out.println("\n");
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
        /// printTestData(x, y);

        OLSMultipleLinearRegression ols = new OLSMultipleLinearRegression();
        ols.newSampleData(y, x);
        return ols.estimateRegressionParameters();
    }

    public double predict(double[] dataset) {
        if ((dataset == null) || (dataset.length == 0)) {
            return -1;
        }

        if (dataset.length != this.parameters.length - 1) {
            System.err.print("Length of dataset doesn't equals to the length of parameters.");
        }

        double predict = parameters[0];
        for (int i = 0; i < dataset.length; i ++) {
            predict += parameters[i + 1] * dataset[i];
        }
        return predict;
    }
}
