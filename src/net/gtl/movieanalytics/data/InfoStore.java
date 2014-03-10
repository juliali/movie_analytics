/*
 * Copyright 2011 - 2014 Cetas Software, Inc. All rights reserved. This is Cetas
 * proprietary and confidential material and its use is subject to license terms.
 */

package net.gtl.movieanalytics.data;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;

import java.io.FileReader;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

/**
 * Created by Julia on 3/7/14.
 */
public class InfoStore {

    private static volatile InfoStore instance = new InfoStore();
    private Map<String, FeatureDimension> featureMap = null;

    private String tableName;
    private String sourceTableName;
    private String resultFieldName;
    private String dbHost;

    private String[] featuresInModel;

    double testRecordPercentage;
    double errorToleranceRate;

    //private double[] parameters;

    private InfoStore() {
        featureMap = new HashMap<String, FeatureDimension>();
        readJsonFile(System.getenv("INFO_FILE_PATH"));
    }

    public static InfoStore getInstance() {
        return instance;
    }

    public void addFeature(FeatureDimension feature) {
        if (feature == null) {
            return;
        }

        if (featureMap.get(feature.getFieldName()) == null) {
            featureMap.put(feature.getFieldName(), feature);
        }
    }

    public DBFieldType getFeatureType(String featureName) {
        FeatureDimension feature = featureMap.get(featureName);

        if ( feature != null) {
            return feature.getFieldType();
        } else {
            return null;
        }
    }

    public int getFeatureSubItemNum(String featureName) {
        FeatureDimension feature = featureMap.get(featureName);

        if (feature != null) {
            return feature.getSubFieldNum();
        } else {
            return -1;
        }
    }

    public String getTableName() {
        return tableName;
    }

    public void setTableName(String tableName) {
        this.tableName = tableName;
    }

    public String getSourceTableName() {
        return sourceTableName;
    }

    public void setSourceTableName(String sourceTableName) {
        this.sourceTableName = sourceTableName;
    }

    public String getResultFieldName() {
        return resultFieldName;
    }

    public void setResultFieldName(String resultFieldName) {
        this.resultFieldName = resultFieldName;
    }

    public String getDbHost() {
        return dbHost;
    }

    public void setDbHost(String dbHost) {
        this.dbHost = dbHost;
    }

    public String[] getAllFeatureNames() {
        Set<String> names = featureMap.keySet();
        String[] array = new String[names.size()];
        array = names.toArray(array);
        return array;
    }

    public double getTestRecordPercentage() {
        return testRecordPercentage;
    }

    public void setTestRecordPercentage(double testRecordPercentage) {
        this.testRecordPercentage = testRecordPercentage;
    }

    public double getErrorToleranceRate() {
        return errorToleranceRate;
    }

    public void setErrorToleranceRate(double errorToleranceRate) {
        this.errorToleranceRate = errorToleranceRate;
    }

    public String[] getFeaturesInModel() {
        return featuresInModel;
    }

    public void setFeaturesInModel(String[] featuresInModel) {
        this.featuresInModel = featuresInModel;
    }

    /*public double[] getParameters() {
        return parameters;
    }

    public void setParameters(double[] parameters) {
        this.parameters = parameters;
        for (int i = 0; i < this.parameters.length; i ++) {
            System.out.print("p" + i + ": " + this.parameters[i] + "; ");
        }
        System.out.println("\n");
    }
    */

    private void readJsonFile(String path) {
        JSONParser parser = new JSONParser();

        try {
            JSONObject root = (JSONObject) parser.parse(new FileReader(path));
            double testRecordPercentage = (Double) root.get("testRecordPercentage");
            this.setTestRecordPercentage(testRecordPercentage);
            double errorToleranceRate = (Double) root.get("errorToleranceRate");
            this.setErrorToleranceRate(errorToleranceRate);
            String tableName = (String) root.get("tableName");
            this.setTableName(tableName);
            String sourceTableName = (String) root.get("sourceTableName");
            this.setSourceTableName(sourceTableName);
            String resultFieldName = (String) root.get("resultFieldName");
            this.setResultFieldName(resultFieldName);
            String dbHost = (String) root.get("dbHost");
            this.setDbHost(dbHost);

            JSONArray features = (JSONArray) root.get("features");
            Iterator<JSONObject> iterator = features.iterator();
            while (iterator.hasNext()) {
                JSONObject featureObj = iterator.next();
                String name = (String) featureObj.get("name");
                String type = (String) featureObj.get("type");
                DBFieldType fType = DBFieldType.valueOf(type);
                int subNum = ((Long) featureObj.get("subNum")).intValue();

                FeatureDimension feature = new FeatureDimension(fType, name, subNum);
                this.addFeature(feature);
            }

            JSONArray fm = (JSONArray) root.get("featuresInModel");
            String[] featuresInModel = new String[fm.size()];
            Iterator<String> iter = fm.iterator();
            int i = 0;
            while (iter.hasNext()) {
                featuresInModel[i] = iter.next();
                i ++;
            }
            this.setFeaturesInModel(featuresInModel);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
