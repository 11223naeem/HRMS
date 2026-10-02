package com.hrms.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class FaceService {
    @Value("${face.threshold}")
    private double threshold;

    private final ObjectMapper om = new ObjectMapper();

    public double threshold() { return threshold; }

    /** Parses [[128 numbers], ...]; throws IllegalArgumentException when malformed. */
    public double[][] parse(String json) {
        try {
            double[][] a = om.readValue(json, double[][].class);
            if (a == null || a.length == 0) throw new IllegalArgumentException("empty");
            for (double[] v : a) if (v == null || v.length != 128) throw new IllegalArgumentException("bad length");
            return a;
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException("bad json");
        }
    }

    public static double dist(double[] a, double[] b) {
        double s = 0;
        for (int i = 0; i < a.length; i++) { double d = a[i] - b[i]; s += d * d; }
        return Math.sqrt(s);
    }

    /** Smallest distance between the probe and any stored sample. */
    public double best(double[][] stored, double[] probe) {
        double m = Double.MAX_VALUE;
        for (double[] s : stored) m = Math.min(m, dist(s, probe));
        return m;
    }
}
