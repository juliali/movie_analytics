/*
 * Copyright 2011 - 2014 Cetas Software, Inc. All rights reserved. This is Cetas
 * proprietary and confidential material and its use is subject to license terms.
 */

package net.gtl.movieanalytics.model;

import java.text.NumberFormat;

/**
 * Created by Julia on 3/4/14.
 */
public class MovieAnalyzer {

    private static InfoStore infoStore = InfoStore.getInstance();
    private static final double errorTolerance = infoStore.getErrorToleranceRate();
    DataSetGenerator dsGen;
    double[] parameters;

    public MovieAnalyzer() {
        dsGen = new DataSetGenerator();
        double[][] x = dsGen.getX();
        double[] y = dsGen.getY();
        LinearRegression lr = new LinearRegression(x, y);
        parameters = lr.getParameters();
    }

    private double predict(double[] dataset) {
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

    public PredictionResult predictOneMovie(String chineseName) {
        double[] data = dsGen.getPredictionInput(chineseName);
        NumberFormat nf = NumberFormat.getPercentInstance();
        String result = "";
        PredictionResult res = null;
        if (data == null) {
            result = "Fail to predict revenue for " + chineseName;
        } else {
            double predictResult = predict(data);
            double actualResult = dsGen.getTestActualResult(chineseName);

            res = new PredictionResult(chineseName, predictResult, actualResult);

            result = chineseName + " -- predictRevenue: " + predictResult;
            if (actualResult > 0) {
                result += "; actualRevenue: " + actualResult;
                double errorRate = res.getErrorRate();
                result += "; errorRate: " + nf.format(errorRate);
            } else {
                result += "; no actual revenue";
            }
        }
        System.out.println(result);
        return res;
    }

    public void executeTest() {
        NumberFormat nf   =   NumberFormat.getPercentInstance();
        dsGen.getTestDataSet();
        int size = dsGen.getTestDataSize();
        int bingoNum = 0;

        for (int i = 0; i < size; i ++) {
            String info = dsGen.getTestDataString(i);
            double[] inputs = dsGen.getTestInput(i);
            double actualResult = dsGen.getTestActualResult(i);

            double predictResult = predict(inputs);

            double errorRate = (predictResult - actualResult) / actualResult;
            if (errorRate < 0)  {
                errorRate = 0 - errorRate;
            }

            String result = "Failed";
            if (errorRate <= errorTolerance) {
                bingoNum ++;
                result = "Bingo!";
            }

            System.out.println("[test case " + (i + 1) + "]: " + info);

            System.out.println("    predictResult: " + predictResult + "; actualResult: " + actualResult + "; errorRate: " + nf.format(errorRate) + " -- " + result);
        }

        double accurency = (double) bingoNum/ (double) size;
        System.out.println("\nTotal Test Case Number: " + size + "; Bingo Number: " + bingoNum + "; and Accurency: " + nf.format(accurency));
    }

    public static void main(String[] args) {
        MovieAnalyzer ma = new MovieAnalyzer();
        ma.executeTest();
        //ma.predictOneMovie("101次求婚");
    }

    public static class PredictionResult {
        private String movieName;
        private double predictedResult;
        private double actualResult;
        private double errorRate;

        private void calculateErrorRate() {
            if (this.actualResult > 0) {
                errorRate = (this.predictedResult - this.actualResult) / this.actualResult;
            } else {
                errorRate = -1;
            }
        }

        public PredictionResult(String name, double predictedResult, double actualResult) {
            this.movieName = name;
            this.predictedResult = predictedResult;
            this.actualResult = actualResult;
            calculateErrorRate();
        }

        public String getMovieName() {
            return movieName;
        }

        public double getPredictedResult() {
            return predictedResult;
        }

        public double getActualResult() {
            return actualResult;
        }

        public double getErrorRate() {
            return errorRate;
        }
    }
}
