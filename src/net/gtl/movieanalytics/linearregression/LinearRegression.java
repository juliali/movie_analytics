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
    }

    private double[] estimateParameter(double[][] x, double[] y) {
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
