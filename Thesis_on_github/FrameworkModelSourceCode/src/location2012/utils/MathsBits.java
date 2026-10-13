/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package location2012.utils;

/**
 *
 * @author Dan
 */
public class MathsBits {

    /**
     * From
     * http://www.java-forums.org/advanced-java/4130-rounding-double-two-decimal-places.html
     *
     * @param d
     * @param c
     * @return
     */
    public static double roundToDecimals(double d, int c) {

        int temp = (int) ((d * Math.pow(10, c)));
        return (((double) temp) / Math.pow(10, c));
    }

    /**
     * From
     * http://www.java-forums.org/advanced-java/4130-rounding-double-two-decimal-places.html
     *
     * With additional boolean flag, true = "round up"
     * See 'Alteration to rounding algorithm', ThesisDiary_2011_13.docx
     *
     * @param d
     * @param c
     * @param roundUp
     * @return
     */
    public static double roundToDecimals(double d, int c, boolean roundUp) {

        if (roundUp) {
            d += (0.5 * Math.pow(10, -c));
        }

        int temp = (int) ((d * Math.pow(10, c)));
        return (((double) temp) / Math.pow(10, c));
    }

    /**
     * Return only c number of digits from a number. E.g. 0.375 = 0.3; 253.67 =
     * 250.
     *
     * @param d
     * @param c
     * @return
     */
    public static double getDigits(double d, int c) {
        double temp = ((int) ((d * Math.pow(10, -c))) * Math.pow(10, c));

//                (int) ((d * Math.pow(10, -c)));

        return temp;
    }
}
