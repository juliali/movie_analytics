/*
 * Copyright 2011 - 2014 Cetas Software, Inc. All rights reserved. This is Cetas
 * proprietary and confidential material and its use is subject to license terms.
 */

package net.gtl.movieanalytics.data;

import java.util.List;

/**
 * Created by Julia on 3/10/14.
 */
public class Feature {
    private String name;
    private List<FeatureFunction> functions = null;

    public Feature(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public List<FeatureFunction> getFunctions() {
        return functions;
    }

    public void setFunctions(List<FeatureFunction> functions) {
        this.functions = functions;
    }
}
