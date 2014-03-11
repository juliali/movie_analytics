/*
 * Copyright 2011 - 2014 Cetas Software, Inc. All rights reserved. This is Cetas
 * proprietary and confidential material and its use is subject to license terms.
 */

package net.gtl.movieanalytics.utils;

import java.io.*;

/**
 * Created by Julia on 3/11/14.
 */
public class FileIO {

    public static String readTextFile(String fileName) {

        String returnValue = "";
        FileReader file = null;

        try {
            file = new FileReader(fileName);
            BufferedReader reader = new BufferedReader(file);
            String line = "";
            while ((line = reader.readLine()) != null) {
                returnValue += line + "\n";
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        } finally {
            if (file != null) {
                try {
                    file.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        return returnValue;
    }

    public static void writeTextFile(String fileName, String s) {
        FileWriter output = null;
        BufferedWriter writer = null;
        try {
            output = new FileWriter(fileName);
            writer = new BufferedWriter(output);
            writer.write(s);
        } catch (Exception e) {
            throw new RuntimeException(e);
        } finally {
            if (writer != null) {
                try {
                    writer.close();
                    output.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }

    }

    public static void main(String[] args) {
        FileIO.writeTextFile("/tmp/abc.txt", "abcdefg");
        String ns = FileIO.readTextFile("/tmp/abc.txt");
        System.out.println(ns);
    }
}
