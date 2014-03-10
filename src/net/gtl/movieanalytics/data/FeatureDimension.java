/*
 * Copyright 2011 - 2014 Cetas Software, Inc. All rights reserved. This is Cetas
 * proprietary and confidential material and its use is subject to license terms.
 */

package net.gtl.movieanalytics.data;

/**
 * Created by Julia on 3/7/14.
 */
public class FeatureDimension {

    private DBFieldType fieldType;
    private String fieldName;
    private int subFieldNum;

    public FeatureDimension(DBFieldType type, String name) {
        this.fieldType = type;
        this.fieldName = name;
        this.subFieldNum = 1;
    }

    public FeatureDimension(DBFieldType type, String name, int num) {
        this.fieldType = type;
        this.fieldName = name;
        this.subFieldNum = num;
    }

    public DBFieldType getFieldType() {
        return fieldType;
    }

    public String getFieldName() {
        return fieldName;
    }

    public int getSubFieldNum() {
        return subFieldNum;
    }

    public boolean isFeature(String name) {
        if ((name != null) && (this.fieldName != null) && (name.equals(this.fieldName))) {
            return true;
        } else {
            return false;
        }
    }
}
