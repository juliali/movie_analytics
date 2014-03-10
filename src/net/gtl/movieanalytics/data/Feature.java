/*
 * Copyright 2011 - 2014 Cetas Software, Inc. All rights reserved. This is Cetas
 * proprietary and confidential material and its use is subject to license terms.
 */

package net.gtl.movieanalytics.data;

/**
 * Created by Julia on 3/10/14.
 */
public class Feature {
    private String name;
    private FeatureFunction function = null;

    public Feature(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public FeatureFunction getFunction() {
        return function;
    }

    public void setFunction(FeatureFunction function) {
        this.function = function;
    }
}
