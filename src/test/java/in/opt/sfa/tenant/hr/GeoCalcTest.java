package in.opt.sfa.tenant.hr;

import in.opt.sfa.tenant.dcr.geo.GeoCalc;
import in.opt.sfa.tenant.dcr.geo.GeoCalc.*;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class GeoCalcTest {
    @Test void samePointIsZero() { assertEquals(0, GeoCalc.distanceM(25.5941, 85.1376, 25.5941, 85.1376)); }
    @Test void oneDegreeOfLatitudeIsAbout111Km() { long d = GeoCalc.distanceM(25, 85, 26, 85); assertTrue(d > 111000 && d < 111400, "d=" + d); }
    @Test void hundredMetresNorth() { long d = GeoCalc.distanceM(25.5941, 85.1376, 25.5941 + 100 / 111195.0, 85.1376); assertTrue(Math.abs(d - 100) <= 1, "d=" + d); }
    @Test void symmetric() { assertEquals(GeoCalc.distanceM(25.59, 85.13, 25.61, 85.15), GeoCalc.distanceM(25.61, 85.15, 25.59, 85.13)); }
    @Test void patnaToDelhiIsAbout900Km() { long d = GeoCalc.distanceM(25.5941, 85.1376, 28.6139, 77.2090); assertTrue(d > 850000 && d < 950000, "d=" + d); }
    @Test void crossesTheDateLineShortWay() { assertTrue(GeoCalc.distanceM(0, 179.9, 0, -179.9) < 25000); }
    @Test void validCoordinates() {
        assertTrue(GeoCalc.validLatLng(25.5, 85.1));
        assertFalse(GeoCalc.validLatLng(null, 85.1)); assertFalse(GeoCalc.validLatLng(95.0, 85.1)); assertFalse(GeoCalc.validLatLng(25.0, 200.0)); assertFalse(GeoCalc.validLatLng(0.0, 0.0)); assertFalse(GeoCalc.validLatLng(Double.NaN, 1.0));
    }
    @Test void nearestOrdersAndLimits() {
        List<Point> pts = List.of(new Point(1, "Far", 26.0, 85.0), new Point(2, "Near", 25.001, 85.0), new Point(3, "Mid", 25.1, 85.0));
        List<Near> n = GeoCalc.nearest(25.0, 85.0, pts, 2);
        assertEquals(2, n.size()); assertEquals("Near", n.get(0).name()); assertEquals("Mid", n.get(1).name());
    }
    @Test void prettyDistance() { assertEquals("850 m", GeoCalc.pretty(850)); assertEquals("1.4 km", GeoCalc.pretty(1400)); assertEquals("12.0 km", GeoCalc.pretty(12000)); }
}
