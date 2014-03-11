/*
 * Copyright 2011 - 2014 Cetas Software, Inc. All rights reserved. This is Cetas
 * proprietary and confidential material and its use is subject to license terms.
 */

package net.gtl.movieanalytics.data;

/**
 * Created by Julia on 3/10/14.
 */
public class FeatureFunction {
    private FeatureFunctionType type;
    private double[] arguments = null;

    public FeatureFunction(String name) {
        this.type = FeatureFunctionType.valueOf(name.toUpperCase());
    }

    public FeatureFunctionType getType() {
        return type;
    }

    public double[] getArguments() {
        return arguments;
    }

    public void setArguments(double[] arguments) {
        this.arguments = arguments;
    }

    public int getArgumentsNum() {
        if (arguments == null) {
            return -1;
        } else {
            return arguments.length;
        }
    }
}
