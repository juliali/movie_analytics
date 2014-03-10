/*
 * Copyright 2011 - 2014 Cetas Software, Inc. All rights reserved. This is Cetas
 * proprietary and confidential material and its use is subject to license terms.
 */

package net.gtl.movieanalytics.data;

/**
 * Created by Julia on 3/10/14.
 */
public enum FeatureFunctionType {
    MULTIPLE, LOGE, POWER;

    public int getArgumentsNumber(String functionName) {
        if (functionName == null) {
            return -1;
        }

        if (FeatureFunctionType.valueOf(functionName.toUpperCase()) == MULTIPLE) {
            return 1;
        } else if (FeatureFunctionType.valueOf(functionName.toUpperCase()) == POWER) {
            return 1;
        } else {
            return 0;
        }
    }

}
