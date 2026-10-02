package com.hrms.service;

public final class GeoUtil {
    private GeoUtil() {}

    /** Haversine distance in metres. */
    public static double meters(double lat1, double lng1, double lat2, double lng2) {
        double r = 6371000.0;
        double p1 = Math.toRadians(lat1), p2 = Math.toRadians(lat2);
        double dp = Math.toRadians(lat2 - lat1), dl = Math.toRadians(lng2 - lng1);
        double a = Math.sin(dp / 2) * Math.sin(dp / 2) + Math.cos(p1) * Math.cos(p2) * Math.sin(dl / 2) * Math.sin(dl / 2);
        return 2 * r * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
    }
}
