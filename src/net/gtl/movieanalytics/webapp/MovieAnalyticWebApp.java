/*
 * Copyright 2011 - 2014 Cetas Software, Inc. All rights reserved. This is Cetas
 * proprietary and confidential material and its use is subject to license terms.
 */

package net.gtl.movieanalytics.webapp;

import net.gtl.movieanalytics.model.MovieAnalyzer;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.text.DecimalFormat;

/**
 * Created by pivotal on 2/24/14.
 */

public class MovieAnalyticWebApp extends javax.servlet.http.HttpServlet implements javax.servlet.Servlet {

    private static DecimalFormat format = new DecimalFormat("#.0");

    public MovieAnalyticWebApp() {
        super();
    }

    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        doPost(request, response);
    }

    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        request.setCharacterEncoding("UTF-8");
        String name = request.getParameter("name");

        response.setCharacterEncoding("UTF-8");
        response.setHeader("content-type", "text/html;charset=UTF-8");

        MovieAnalyzer ma = new MovieAnalyzer();
        MovieAnalyzer.PredictionResult res = ma.predictOneMovie(name);

        String r = "";
        if (res != null) {
            r += "{status: OK, results: ";
            r += "[";
            r += "{movieName: \"" + res.getMovieName() + "\", predictRevenue: " + res.getPredictedResult() + ", actualRevenue: " + res.getActualResult() + ", errorRate: " + res.getErrorRate() + "}";
            r += "]";
            r += "}";
        } else {
            r += "{status: Failed}";
        }
        response.getWriter().write(r);
    }
}