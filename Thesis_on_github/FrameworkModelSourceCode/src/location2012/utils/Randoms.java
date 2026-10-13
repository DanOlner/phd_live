package location2012.utils;

import java.util.Random;
import location2012.Person;

/**
 * For generating consistent random numbers throughout - looked at
 * http://exampledepot.com/egs/java.util/GenRandom.html and adapted
 *
 * @author gy06do
 */
public class Randoms {

    private static Random rn;
    //Used for calcs
    private static double d;
    private static int i;

    //for changing seed in many-run runs
    private static int seed;

    //takes in a seed from the start-up page
    /**
     *
     * @param seed
     */
    public Randoms(int seed) {

        this.seed = seed;

        rn = new Random(seed);

    }

    /**
     * Increments seed by one. Extra benefit: don't need to data output it cos it's just a sequence from the original value.
     */
    public static void incrementSeed() {

        rn.setSeed(seed++);

    }

    /**
     *
     * @return
     */
    public static double nextDouble() {

        double a = rn.nextDouble();

        return a;

    }

    public static boolean nextBoolean() {

        double a = rn.nextDouble();

        if (a < 0.5) {
            return true;
        } else {
            return false;
        }

    }

    /*
     * rangeInt: returns a random int between 0 and
     * one less than the int value provided. So: 10 gives between 0 and 9.
     */
    public static int rangeInt(int n) {

        d = rn.nextDouble();

        i = (int) (d * (double) n);

        //p.p("testing random int range: number given = " + n + ", output num: " + i);

        return i;

    }

    /*
     * rangeDouble: returns a random double between 0 and
     * one less than the double value provided. So: 10 gives between 0 and 9.
     */
    public static double rangeDouble(double n) {

        d = rn.nextDouble() * n;

        //p.p("testing random int range: number given = " + n + ", output num: " + i);

        return d;

    }

    /*
     * rangeDouble: returns a random double between low and high values
     * one less than the double value provided. So: 10 gives between 0 and 9.
     */
    public static double lowHighDouble(double low, double high) {

        d = (rn.nextDouble() * (high - low)) - high;

        //p.p("testing random int range: number given = " + n + ", output num: " + i);

        return d;

    }

    /**
     *
     * @return
     */
    public static int nextInt(int n) {

        int a = rn.nextInt(n);

        return a;

    }
}
