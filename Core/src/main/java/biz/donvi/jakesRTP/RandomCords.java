package biz.donvi.jakesRTP;

import java.util.Random;

/**
 * Utility class for generating random coordinates in various shapes.
 * Replaces the external biz.donvi.evenDistribution library.
 */
public final class RandomCords {

    private static final Random random = new Random();

    private RandomCords() {}

    /**
     * Generates random coordinates within a circle (donut shape with min/max radius).
     */
    public static double[] getRandXyCircle(int radiusMax, int radiusMin, double gaussianShrink, double gaussianCenter) {
        double angle = random.nextDouble() * 2 * Math.PI;
        double radius;

        if (gaussianShrink > 0) {
            // Gaussian distribution
            double gaussian = random.nextGaussian() * gaussianShrink + gaussianCenter;
            gaussian = Math.max(0, Math.min(1, (gaussian + 3) / 6)); // Normalize to 0-1
            radius = radiusMin + gaussian * (radiusMax - radiusMin);
        } else {
            // Uniform distribution within the annulus
            double minSq = (double) radiusMin * radiusMin;
            double maxSq = (double) radiusMax * radiusMax;
            radius = Math.sqrt(minSq + random.nextDouble() * (maxSq - minSq));
        }

        return new double[]{
            radius * Math.cos(angle),
            radius * Math.sin(angle)
        };
    }

    /**
     * Generates random coordinates within a square (with exclusion zone).
     */
    public static double[] getRandXySquare(int radiusMax, int radiusMin) {
        return getRandXySquare(radiusMax, radiusMin, 0, 0);
    }

    /**
     * Generates random coordinates within a square with optional gaussian distribution.
     */
    public static double[] getRandXySquare(int radiusMax, int radiusMin, double gaussianShrink, double gaussianCenter) {
        double x, z;

        do {
            if (gaussianShrink > 0) {
                // Gaussian distribution
                double gx = random.nextGaussian() * gaussianShrink + gaussianCenter;
                double gz = random.nextGaussian() * gaussianShrink + gaussianCenter;
                gx = Math.max(-1, Math.min(1, gx / 3));
                gz = Math.max(-1, Math.min(1, gz / 3));
                x = gx * radiusMax;
                z = gz * radiusMax;
            } else {
                // Uniform distribution
                x = (random.nextDouble() * 2 - 1) * radiusMax;
                z = (random.nextDouble() * 2 - 1) * radiusMax;
            }
        } while (Math.abs(x) < radiusMin && Math.abs(z) < radiusMin);

        return new double[]{x, z};
    }

    /**
     * Generates random coordinates within a rectangle.
     */
    public static double[] getRandXyRectangle(int xRadius, int zRadius) {
        double x = (random.nextDouble() * 2 - 1) * xRadius;
        double z = (random.nextDouble() * 2 - 1) * zRadius;
        return new double[]{x, z};
    }

    /**
     * Generates random coordinates within a rectangle with a gap (exclusion zone).
     */
    public static double[] getRandXyRectangle(int xRadius, int zRadius, int gapXRadius, int gapZRadius, int gapXCenter, int gapZCenter) {
        double x, z;

        do {
            x = (random.nextDouble() * 2 - 1) * xRadius;
            z = (random.nextDouble() * 2 - 1) * zRadius;
        } while (Math.abs(x - gapXCenter) < gapXRadius && Math.abs(z - gapZCenter) < gapZRadius);

        return new double[]{x, z};
    }

    /**
     * Converts a double array to an int array (truncating decimals).
     */
    public static int[] asIntArray2w(double[] arr) {
        return new int[]{(int) arr[0], (int) arr[1]};
    }
}
