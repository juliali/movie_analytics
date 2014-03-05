/*
 * Copyright 2011 - 2014 Cetas Software, Inc. All rights reserved. This is Cetas
 * proprietary and confidential material and its use is subject to license terms.
 */

package net.gtl.movieanalytics.linearregression;

import java.text.NumberFormat;

/**
 * Created by Julia on 3/4/14.
 */
public class MovieAnalyzer {

    private static final double errorTolerance = 10;

    public MovieAnalyzer() {

    }

    public void execute() {
        DataSetGenerator dsGen = new DataSetGenerator();
        double[][] x = dsGen.getX();
        double[] y = dsGen.getY();
        NumberFormat nf   =   NumberFormat.getPercentInstance();
        LinearRegression lr = new LinearRegression(x, y);

        int size = dsGen.getTestDataSize();
        int bingoNum = 0;

        for (int i = 0; i < size; i ++) {
            String info = dsGen.getTestDataString(i);
            double[] inputs = dsGen.getTestInput(i);
            double actualResult = dsGen.getTestActualResult(i);

            double predictResult = lr.predict(inputs);

            double errorRate = (predictResult - actualResult) / actualResult;
            if (errorRate < 0)  {
                errorRate = 0 - errorRate;
            }

            if (errorRate <= errorTolerance) {
                bingoNum ++;
            }

            System.out.println("[test case " + (i + 1) + "]: " + info);

            System.out.println("    predictResult: " + predictResult + "; actualResult: " + actualResult + "; errorRate: " + nf.format(errorRate));
        }

        double accurency = (double) bingoNum/ (double) size;
        System.out.println("\nTotal Test Case Number: " + size + "; Bingo Number: " + bingoNum + "; and Accurency: " + nf.format(accurency));
    }

    public static void main(String[] args) {
        MovieAnalyzer ma = new MovieAnalyzer();
        ma.execute();
    }
}
