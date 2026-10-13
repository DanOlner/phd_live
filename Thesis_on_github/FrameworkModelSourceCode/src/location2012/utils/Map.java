/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package location2012.utils;

import java.awt.Color;

/**
 *
 * @author geodo
 */
public class Map {

    private static float val;
    private static int r, g, b;
    private Color c;

    /**
     * Map low to high values in an input range to an output range
     *
     * @param mapVal
     * @param lowInput
     * @param highInput
     * @param lowMapVal
     * @param highMapVal
     * @return
     */
    public static float mapLowToHigh_DoublesInFloatOut(
            double mapVal, double lowInput, Double highInput,
            double lowMapVal, double highMapVal) {

        //make sure it's within the bounds (these may be slightly different from
        //when those values were first found)
        mapVal = constrainBoundsDouble(mapVal, lowInput, highInput);

        //gives normalised value 0 to 1
        val = (float) (((mapVal - lowInput) / (highInput - lowInput)));
//        System.out.println("normalised val: " + colVal);

        //range to map to
        double range = highMapVal - lowMapVal;

        //multiply normalised val
        val *= range;

        //add low map val
        val += lowMapVal;

        //check in bounds

        return val;

    }//end method mapLowToHigh_DoublesInFloatOut

    public static Color mapLowHighToColour_DoublesColorsInColorOut(
            double mapVal, double lowInput, Double highInput, Color low, Color high) {

        //map along the three axes: r,g,b

        r = (int) mapLowToHigh_DoublesInFloatOut(mapVal, lowInput, highInput,
                low.getRed(), high.getRed());
        g = (int) mapLowToHigh_DoublesInFloatOut(mapVal, lowInput, highInput,
                low.getGreen(), high.getGreen());
        b = (int) mapLowToHigh_DoublesInFloatOut(mapVal, lowInput, highInput,
                low.getBlue(), high.getBlue());

//        System.out.println("test color vals:" + low.getGreen());

//        System.out.println("found colour vals: " + r + "," + g + "," + b);

        //check in range
        r = constrainBoundsInt(r, 0, 255);
        g = constrainBoundsInt(g, 0, 255);
        b = constrainBoundsInt(b, 0, 255);

        try {
            return new Color(r, g, b);
        } catch (Throwable e) {

            System.out.println("failed color: " + r + "," + g + "," + b);
//            System.exit(0);
            return Color.white;

        }


    }//end method mapLowHighToColour

    /**
     * Map an x coordinate to a value useable by Processing viz
     *
     * @param x
     * @return
     */
    public static float mapToVizX(double x) {
        return (float) (x * (gl.pappletWidth / gl.width));
    }

    /**
     * Map a y coordinate to a value useable by Processing viz
     *
     * @param y
     * @return
     */
    public static float mapToVizY(double y) {
        return (float) (y * (gl.pappletHeight / gl.width));
    }
    
//    public static float mapToGraphX(double x) {
//        return (float) (x * (gl.pappletHeight / gl.width));
//    }
//    
//    public static float mapToGraphY(double y) {
//        return (float) (y * (gl.pappletHeight / gl.width));
//    }

    /**
     * Map an x coordinate to a value useable by Processing PGraphics
     *
     * @param x
     * @return
     */
    public static float mapToPGraphicsX(double x, double pwidth) {
        return (float) (x * (pwidth / gl.width));
    }

    /**
     * Map a y coordinate to a value useable by Processing PGraphics
     *
     * @param y
     * @return
     */
    public static float mapToPGraphicsY(double y, double pheight) {
        return (float) (y * (pheight / gl.width));
    }

    public static int constrainBoundsInt(int in, int low, int high) {

        //test
        if (in < low) {
            System.err.println("In < low: " + in + " < " + low);
        }
        if (in > high) {
            System.err.println("In > high: " + in + " < " + low);
        }

        in = (in < low ? low : in);
        in = (in > high ? high : in);

        return in;

    }//end method constrainBoundsInt

    public static double constrainBoundsDouble(double in, double low, double high) {

        in = (in < low ? low : in);
        in = (in > high ? high : in);

        return in;

    }//end method constrainBoundsInt
}
