package in.opt.sfa.tenant.dcr.geo;

import java.util.Comparator;
import java.util.List;

/** Pure distance maths for the geo fencing (great-circle distance in metres). */
public final class GeoCalc {

    private static final double R = 6371008.8;   // mean earth radius, metres

    private GeoCalc() { }

    public record Point(long id, String name, double lat, double lng) { }
    public record Near(long id, String name, long distanceM) { }

    public static boolean validLatLng(Double lat, Double lng) {
        return lat != null && lng != null && !lat.isNaN() && !lng.isNaN() && lat >= -90 && lat <= 90 && lng >= -180 && lng <= 180 && !(lat == 0 && lng == 0);
    }

    public static long distanceM(double lat1, double lng1, double lat2, double lng2) {
        double p1 = Math.toRadians(lat1), p2 = Math.toRadians(lat2), dp = p2 - p1, dl = Math.toRadians(lng2 - lng1);
        double a = Math.sin(dp / 2) * Math.sin(dp / 2) + Math.cos(p1) * Math.cos(p2) * Math.sin(dl / 2) * Math.sin(dl / 2);
        return Math.round(2 * R * Math.asin(Math.min(1, Math.sqrt(a))));
    }

    /** The {@code limit} points nearest to the position, nearest first. */
    public static List<Near> nearest(double lat, double lng, List<Point> points, int limit) {
        return points.stream().map(p -> new Near(p.id(), p.name(), distanceM(lat, lng, p.lat(), p.lng()))).sorted(Comparator.comparingLong(Near::distanceM)).limit(limit).toList();
    }

    /** "850 m" / "1.4 km" for messages. */
    public static String pretty(long m) { return m < 1000 ? m + " m" : String.format(java.util.Locale.ENGLISH, "%.1f km", m / 1000.0); }
}
