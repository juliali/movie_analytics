/*
 * Copyright 2011 - 2014 Cetas Software, Inc. All rights reserved. This is Cetas
 * proprietary and confidential material and its use is subject to license terms.
 */

package net.gtl.movieanalytics.data;

/**
 * Created by Julia on 3/10/14.
 */
public class AverageCountPair {
    private float average;
    private int count;

    public AverageCountPair(float avg, int cnt) {
        average = avg;
        count = cnt;
    }

    public float getAverage() {
        return average;
    }

    public void setAverage(float average) {
        this.average = average;
    }

    public int getCount() {
        return count;
    }

    public void setCount(int count) {
        this.count = count;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        AverageCountPair that = (AverageCountPair) o;

        if (Float.compare(that.average, average) != 0) return false;
        if (count != that.count) return false;

        return true;
    }
}
