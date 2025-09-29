package advRocketry;

import net.minecraft.world.phys.Vec3;

import java.util.Locale;

public class CelestialUtils {

    // --- Static inner class for 4x4 matrix math ---
    public static class Mat4 {
        public final double[] m = new double[16]; // Column-major order

        public Mat4() {
            identity();
        }

        public final void identity() {
            for (int i = 0; i < 16; i++) m[i] = 0;
            m[0] = m[5] = m[10] = m[15] = 1;
        }

        public Mat4 mul(Mat4 o) {
            Mat4 result = new Mat4();
            for (int r = 0; r < 4; r++) {
                for (int c = 0; c < 4; c++) {
                    double sum = 0;
                    for (int i = 0; i < 4; i++) {
                        sum += this.m[c * 4 + i] * o.m[i * 4 + r];
                    }
                    result.m[c * 4 + r] = sum;
                }
            }
            return result;
        }

        public static Mat4 fromAxisAngle(Vec3 axis, double angleDeg) {
            Mat4 mat = new Mat4();
            double angleRad = Math.toRadians(angleDeg);
            double c = Math.cos(angleRad);
            double s = Math.sin(angleRad);
            double t = 1.0 - c;
            Vec3 a = axis.normalize();

            mat.m[0] = c + a.x * a.x * t;
            mat.m[5] = c + a.y * a.y * t;
            mat.m[10] = c + a.z * a.z * t;

            double tmp1 = a.x * a.y * t;
            double tmp2 = a.z * s;
            mat.m[4] = tmp1 + tmp2;
            mat.m[1] = tmp1 - tmp2;

            tmp1 = a.x * a.z * t;
            tmp2 = a.y * s;
            mat.m[8] = tmp1 - tmp2;
            mat.m[2] = tmp1 + tmp2;

            tmp1 = a.y * a.z * t;
            tmp2 = a.x * s;
            mat.m[9] = tmp1 + tmp2;
            mat.m[6] = tmp1 - tmp2;

            return mat;
        }

        @Override
        public String toString() {
            StringBuilder sb = new StringBuilder();
            sb.append("Mat4[\n");
            for (int r = 0; r < 4; r++) {
                sb.append(String.format(Locale.US, "  %.3f, %.3f, %.3f, %.3f\n", m[r], m[r + 4], m[r + 8], m[r + 12]));
            }
            sb.append("]");
            return sb.toString();
        }
    }

    /**
     * Rotates a vector 'v' around an 'axis' by 'angleDeg' degrees
     * using Rodrigues' rotation formula.
     */
    public static Vec3 rotate(Vec3 v, Vec3 axis, double angleDeg) {
        double angleRad = Math.toRadians(angleDeg);
        Vec3 k = axis.normalize();
        double cosA = Math.cos(angleRad);
        double sinA = Math.sin(angleRad);

        Vec3 term1 = v.scale(cosA);
        Vec3 term2 = k.cross(v).scale(sinA);
        Vec3 term3 = k.scale(k.dot(v) * (1 - cosA));

        return term1.add(term2).add(term3);
    }

    /**
     * Calculates the direction vector of a celestial body (e.g., a sun) from the
     * perspective of an observer on a rotating planet.
     *
     * @param bodyPos The position of the celestial body in world coordinates.
     * @param planetPos The position of the planet in world coordinates.
     * @param planetAxis The rotational axis of the planet (e.g., from South to North pole).
     * @param timeOfDayAngle The rotation of the planet in degrees (e.g., 0=sunrise, 90=noon).
     * @param observerLatitude The latitude of the observer on the planet's surface in degrees.
     * @return A normalized Vec3 representing the body's direction in the observer's
     * local coordinate system (x=East, y=Up/Zenith, z=North).
     */
    public static Vec3 getBodyDirectionLocal(Vec3 bodyPos, Vec3 planetPos, Vec3 planetAxis, double timeOfDayAngle, double observerLatitude) {
        // Step 1: Get the sun's direction vector in the fixed world frame.
        Vec3 sunDirWorld = bodyPos.subtract(planetPos).normalize();

        // Step 2: Rotate the sun's direction into a co-rotating planetary frame.
        // The +270 offset aligns 0 degrees with sunrise in the East.
        // The negative angle makes the sun appear to move from East to West.
        Vec3 sunInPlanetFrame = rotate(sunDirWorld, planetAxis, -timeOfDayAngle + 270);

        // Step 3: Define the observer's local coordinate system (zenith, north, east)
        // in the planetary frame, assuming the observer is at a reference longitude.
        Vec3 planetNorth = planetAxis.normalize();

        // Start with a vector pointing from the planet's center to the reference longitude on the equator.
        Vec3 upOnEquator = new Vec3(1, 0, 0);
        // The axis for latitude rotation is the East-West axis at that point.
        Vec3 eastWestAxis = planetNorth.cross(upOnEquator).normalize();
        // The observer's zenith ('up') is found by tilting the equatorial 'up' by the latitude.
        Vec3 observerZenith = rotate(upOnEquator, eastWestAxis, observerLatitude);

        // The observer's 'east' vector is derived from their zenith and the planet's north.
        Vec3 observerEast = planetNorth.cross(observerZenith).normalize();
        // The observer's 'north' is perpendicular to their zenith and east.
        Vec3 observerNorth = observerZenith.cross(observerEast);

        // Step 4: Project the sun's direction onto the observer’s local sky frame.
        double eastComp = sunInPlanetFrame.dot(observerEast);
        double upComp = sunInPlanetFrame.dot(observerZenith);
        double northComp = sunInPlanetFrame.dot(observerNorth);

        // Final sun direction in local sky coordinates.
        return new Vec3(eastComp, upComp, northComp).normalize();
    }

    /**
     * Calculates the orientation matrix for a celestial body relative to a local observer.
     * This matrix can be used to correctly rotate the body's model in a renderer.
     *
     * @param planetAxis The rotational axis of the planet.
     * @param timeOfDayAngle The rotation of the planet in degrees.
     * @param observerLatitude The latitude of the observer on the planet's surface in degrees.
     * @param bodyAxis The celestial body's own rotational axis in world coordinates.
     * @param bodyRotationAngle The celestial body's own rotation around its axis in degrees.
     * @return A Mat4 representing the body's orientation in the observer's local frame.
     */
    public static Mat4 getBodyOrientationMatrix(Vec3 planetAxis, double timeOfDayAngle, double observerLatitude, Vec3 bodyAxis, double bodyRotationAngle) {
        // Transform the body's world axis into the co-rotating planetary frame, same as the sun vector.
        Vec3 bodyAxisInPlanetFrame = rotate(bodyAxis, planetAxis, -timeOfDayAngle + 270);

        // Define the observer's local coordinate system, same as in getBodyDirectionLocal
        Vec3 planetNorth = planetAxis.normalize();
        Vec3 upOnEquator = new Vec3(1, 0, 0);
        Vec3 eastWestAxis = planetNorth.cross(upOnEquator).normalize();
        Vec3 observerZenith = rotate(upOnEquator, eastWestAxis, observerLatitude);
        Vec3 observerEast = planetNorth.cross(observerZenith).normalize();
        Vec3 observerNorth = observerZenith.cross(observerEast);

        // Project the body's axis onto the observer's local frame to get its local orientation.
        double localAxisEast = bodyAxisInPlanetFrame.dot(observerEast);
        double localAxisUp = bodyAxisInPlanetFrame.dot(observerZenith);
        double localAxisNorth = bodyAxisInPlanetFrame.dot(observerNorth);
        Vec3 localBodyAxis = new Vec3(localAxisEast, localAxisUp, localAxisNorth);

        // The final orientation is a rotation around this local axis by the body's own rotation angle.
        return Mat4.fromAxisAngle(localBodyAxis.normalize(), bodyRotationAngle);
    }


    public static void test() {
        System.out.println("--- Test Case 1: Equator at Noon ---");
        // Expected result: Sun is directly overhead (0, 1, 0)
        Vec3 bodyPos1 = new Vec3(0, 0, 0);
        Vec3 planetPos1 = new Vec3(100, 0, 0);
        Vec3 planetAxis1 = new Vec3(0, 1, 0);
        double time1 = 90; // 90 degrees = noon
        double lat1 = 0;   // 0 degrees = equator
        Vec3 sunDir1 = getBodyDirectionLocal(bodyPos1, planetPos1, planetAxis1, time1, lat1);
        System.out.println("Expected Direction: Vec3(0.000, 1.000, 0.000)");
        System.out.println("Actual Direction:   " + sunDir1);
        System.out.println();

        System.out.println("--- Test Case 2: 50deg North Latitude at Noon ---");
        // Expected result: Sun is high, but tilted south (positive y, negative z)
        double lat2 = 50;
        Vec3 sunDir2 = getBodyDirectionLocal(bodyPos1, planetPos1, planetAxis1, time1, lat2);
        System.out.println("Expected Direction: Sun high and to the south -> (0.000, positive, negative)");
        System.out.println("Actual Direction:   " + sunDir2);
        System.out.println();

        System.out.println("--- Test Case 3: Equator at Sunrise ---");
        // Expected result: Sun is on the eastern horizon (1, 0, 0)
        double time3 = 0; // 0 degrees = sunrise
        Vec3 sunDir3 = getBodyDirectionLocal(bodyPos1, planetPos1, planetAxis1, time3, lat1);
        System.out.println("Expected Direction: Vec3(1.000, 0.000, 0.000)");
        System.out.println("Actual Direction:   " + sunDir3);
        System.out.println();

        System.out.println("--- Test Case 4: Body Orientation ---");
        // A sun with its own axial tilt (like Uranus, on its side)
        Vec3 sunAxis = new Vec3(1, 0, 0);
        double sunRotation = 45; // It has rotated 45 degrees on its axis
        Mat4 sunOrientation = getBodyOrientationMatrix(planetAxis1, time1, lat2, sunAxis, sunRotation);
        System.out.println("Calculated Orientation Matrix for Sun at 50N Noon:");
        System.out.println(sunOrientation);
    }

    // custom set Gravitational constant2
    public static final double G = 6.67430e-3;

    /**
     * Calculates real distance from value where 100 = 1 astronomical unit
     * @param value 100 equals 1 astronomical unit
     * @return real distance in m
     */
    public static  double getRealDistanceFromValue(double value){
        return value / 100f * 1.496 * Math.pow(10, 11);
    }

    /**
     * Calculates real mass from value where 100 = 1 earth mass
     * @param value 100 = 1 earth mass
     * @return real math in kg
     */
    public static double getRealMassFromValue(double value){
        double massEarth = 5.972 * Math.pow(10, 24); // kg
        return value / 100f * massEarth;
    }

    /**
     * Calculate the orbital period of two bodies in seconds
     * @param mass1 Mass of the first body (kg)
     * @param mass2 Mass of the second body (kg)
     * @param distance Distance between the centers of the two bodies (meters)
     * @return Orbital period in seconds
     */
    public static double calculateOrbitalPeriodTicks(double mass1, double mass2, double distance) {
        double combinedMass = getRealMassFromValue(mass1)  + getRealDistanceFromValue(mass2);
        return 2 * Math.PI * Math.sqrt(Math.pow(getRealDistanceFromValue(distance), 3) / (G * combinedMass)) * 20;
    }
}

