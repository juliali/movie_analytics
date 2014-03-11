/*
 * Copyright 2011 - 2014 Cetas Software, Inc. All rights reserved. This is Cetas
 * proprietary and confidential material and its use is subject to license terms.
 */

package net.gtl.movieanalytics.data;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;

import java.io.FileReader;
import java.util.*;

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

    private boolean isNewGeneratedTestDataSet = true;
    private String testDataSetIdFilePath;

    private List<Feature> featuresInModel;

    double testRecordPercentage;
    double[] errorToleranceRate;

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

        if (feature != null) {
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

    public double[] getErrorToleranceRate() {
        return errorToleranceRate;
    }

    public void setErrorToleranceRate(double[] errorToleranceRate) {
        this.errorToleranceRate = errorToleranceRate;
    }

    public List<Feature> getFeaturesInModel() {
        return featuresInModel;
    }

    public void setFeaturesInModel(List<Feature> featuresInModel) {
        this.featuresInModel = featuresInModel;
    }

    public boolean isNewGeneratedTestDataSet() {
        return isNewGeneratedTestDataSet;
    }

    public void setNewGeneratedTestDataSet(boolean isNewGeneratedTestDataSet) {
        this.isNewGeneratedTestDataSet = isNewGeneratedTestDataSet;
    }

    public String getTestDataSetIdFilePath() {
        return testDataSetIdFilePath;
    }

    public void setTestDataSetIdFilePath(String testDataSetIdFilePath) {
        this.testDataSetIdFilePath = testDataSetIdFilePath;
    }

    private void readJsonFile(String path) {
        JSONParser parser = new JSONParser();

        try {
            JSONObject root = (JSONObject) parser.parse(new FileReader(path));
            double testRecordPercentage = (Double) root.get("testRecordPercentage");
            this.setTestRecordPercentage(testRecordPercentage);

            JSONArray tolerances = (JSONArray) root.get("errorToleranceRate");

            double errorToleranceRate[] = new double[tolerances.size()];
            Iterator<Double> iter = tolerances.iterator();
            int i = 0;
            while (iter.hasNext()) {
                errorToleranceRate[i] = iter.next();
                i++;
            }
            this.setErrorToleranceRate(errorToleranceRate);
            String tableName = (String) root.get("tableName");
            this.setTableName(tableName);
            String sourceTableName = (String) root.get("sourceTableName");
            this.setSourceTableName(sourceTableName);
            String resultFieldName = (String) root.get("resultFieldName");
            this.setResultFieldName(resultFieldName);

            JSONObject tdObj = (JSONObject) root.get("testDataSource");
            boolean isNew = (Boolean) tdObj.get("newGenerated");
            String tdPath = (String) tdObj.get("saveToFilePath");
            if ((tdPath == null) || (tdPath.equals(""))) {
                isNew = true;
            }
            this.setNewGeneratedTestDataSet(isNew);
            this.setTestDataSetIdFilePath(tdPath);

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
            List<Feature> featuresInModel = new ArrayList<Feature>();
            Iterator<JSONObject> iterS = fm.iterator();
            while (iterS.hasNext()) {
                JSONObject currentFeature = iterS.next();
                Boolean isEnabled = (Boolean) currentFeature.get("enabled");
                if ((isEnabled != null) && (isEnabled.booleanValue() == false)) {
                    continue;
                }

                String featureName = (String) currentFeature.get("name");
                Feature feature = new Feature(featureName);

                JSONObject function = (JSONObject) currentFeature.get("function");
                if (function != null) {
                    String functionName = (String) function.get("name");
                    FeatureFunction featureFunction = new FeatureFunction(functionName);
                    JSONArray args = (JSONArray) function.get("arguments");

                    if ((args != null) && (args.size() > 0)) {
                        double arguments[] = new double[tolerances.size()];
                        Iterator<Double> iterA = args.iterator();
                        i = 0;
                        while (iterA.hasNext()) {
                            arguments[i] = iterA.next();
                            i++;
                        }
                        featureFunction.setArguments(arguments);
                    }
                    feature.setFunction(featureFunction);
                }
                featuresInModel.add(feature);
            }

            this.setFeaturesInModel(featuresInModel);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
