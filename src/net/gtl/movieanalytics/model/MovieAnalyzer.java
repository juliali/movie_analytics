/*
 * Copyright 2011 - 2014 Cetas Software, Inc. All rights reserved. This is Cetas
 * proprietary and confidential material and its use is subject to license terms.
 */

package net.gtl.movieanalytics.model;

import net.gtl.movieanalytics.data.FeatureStore;
import net.gtl.movieanalytics.data.InfoStore;
import net.gtl.movieanalytics.data.TestDataStore;

import java.text.NumberFormat;

/**
 * Created by Julia on 3/4/14.
 */
public class MovieAnalyzer {

    private static InfoStore infoStore = InfoStore.getInstance();
    private static TestDataStore tdStore = TestDataStore.getInstance();
    private static FeatureStore featureStore = FeatureStore.getInstance();

    private static final double[] errorTolerance = infoStore.getErrorToleranceRate();
    private double[] parameters;

    public MovieAnalyzer() {
        DataSetGenerator dsGen = new DataSetGenerator();
        dsGen.doTrainning();
        parameters = featureStore.getParameters();
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
        DataSetGenerator dsGen = new DataSetGenerator();

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
        DataSetGenerator dsGen = new DataSetGenerator();
        dsGen.getTestDataSet();

        int size = tdStore.getTestDataSize();
        int[] bingoNum = new int[errorTolerance.length];
        for (int i = 0; i < bingoNum.length; i ++) {
            bingoNum[i] = 0;
        }

        for (int i = 0; i < size; i ++) {
            String info = tdStore.getTestDataString(i);
            double[] inputs = tdStore.getTestInput(i);
            double actualResult = tdStore.getTestActualResult(i);

            double predictResult = predict(inputs);

            double errorRate = (predictResult - actualResult) / actualResult;
            if (errorRate < 0)  {
                errorRate = 0 - errorRate;
            }

            System.out.println("[test case " + (i + 1) + "]: " + info);
            System.out.print("    predictResult: " + predictResult + "; actualResult: " + actualResult + "; errorRate: " + nf.format(errorRate) + " -- ") ;
                    String result[] = new String[errorTolerance.length];

            for (int j = 0; j < result.length; j ++) {
                if (errorRate <= errorTolerance[j]) {
                    bingoNum[j] ++;
                    result[j] = "Bingo!";
                } else {
                    result[j] = "Failed";
                }
                System.out.print(" result for tolerance " + errorTolerance[j] + ": " + result[j]);
                if (j < result.length -1) {
                    System.out.print("; ");
                }
            }

            System.out.println("");
        }

        double[] accurency = new double[errorTolerance.length];
        System.out.print("\nTotal Test Case Number: " + size);
        for (int i = 0; i < accurency.length; i ++) {
            accurency[i] = (double) bingoNum[i]/ (double) size;
            System.out.print("; for [errorTolerance " + errorTolerance[i]+ "]: Bingo Number: " + bingoNum[i] + ", Accurency: " + nf.format(accurency[i]));
            if (i < accurency.length - 1) {
                System.out.print(", ");
            }
        }

        System.out.println("\n");

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
