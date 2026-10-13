/*
 * Some trig methods, accessed statically
 */
package location2012.utils;

import java.awt.geom.Point2D;

/**
 *
 * @author Olner Dan
 */
public class Trig {

    //some re-usable vars
    private static double randir, randist, xx, yy;

    //point to return
    //private static Point2D.Double pt;

    /**
     * Returns random point in circle, used for e.g choosing random samplepoints
     * a day's distance from home location
     *
     * Note: processing sketch to test this -
     * test\testChooseRandInCircle
     *
     * @param radius
     * @return
     */
    public static Point2D.Double getRandPointInCircleOfRadius(double radius) {

        //random direction in the range of 2*PI radians
        randir = Randoms.nextDouble() * 2 * Math.PI;
        //random distance from centre, up to 1 day away is just a Randoms.
        randist = Randoms.nextDouble()*radius;

        //find new x and y
        xx = randist * Math.cos(randir);
        yy = randist * Math.sin(randir);

        //p.a("selected new x,y: " + xx + "," + yy);
        return(new Point2D.Double(xx, yy));

    }

    //Moves a point to a new random spot within the given radius
    public static void moveToRandPointInCircleOfRadius(Point2D.Double ptt, double radius) {

        //random direction in the range of 2*PI radians
        randir = Randoms.nextDouble() * 2 * Math.PI;
        //random distance from centre, up to 1 day away is just a Randoms.
        randist = Randoms.nextDouble()*radius;

        //find new x and y
        xx = randist * Math.cos(randir);
        yy = randist * Math.sin(randir);

        //p.a("selected new x,y: " + xx + "," + yy);
        //return(new Point2D.Double(xx, yy));
        ptt.x += xx;
        ptt.y += yy;

    }

    //chooses a random direction, moves there by the magnitude passed in
    public static Point2D.Double getPointInRandomDirectionOfDistance(double dist) {

          //random direction in the range of 2*PI radians
        randir = Randoms.nextDouble() * 2 * Math.PI;

        //find new x and y
        xx = dist * Math.cos(randir);
        yy = dist * Math.sin(randir);

        //p.a("selected new x,y: " + xx + "," + yy);
        return(new Point2D.Double(xx, yy));

    }

    //Gives a vector in point-form from an angle and magnitude
    public static Point2D.Double getVecFromDistPlusAngle(double dist, double angle) {

        //find new x and y
        xx = dist * Math.cos(angle);
        yy = dist * Math.sin(angle);

        //p.a("selected new x,y: " + xx + "," + yy);
        return(new Point2D.Double(xx, yy));

    }
}
