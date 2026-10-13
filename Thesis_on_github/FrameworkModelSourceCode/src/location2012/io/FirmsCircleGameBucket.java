/*
 * For the two-Firm circle game. This bucket needs to actually kill the model run too if both
 * Firms have got into the defined circle. Multi-run model, next should start.
 */
package location2012.io;

import java.awt.geom.Ellipse2D;
import java.util.ArrayList;
import location2012.Firm;
import location2012.utils.gl;

/**
 *
 * @author Olner Dan
 */
public class FirmsCircleGameBucket extends Bucket {

    ArrayList<Firm> firms;
    Firm f;
    //So People can use to set their own circle size
    public static double circleSize = 0.2;
    //used for the test
    Ellipse2D.Double circle;
    //tests: if both true, both Firms have got into circle at least once
    public static boolean firm1 = false, firm2 = false;
    //to avoid infinite runs. Oo, that sounds unpleasant.
    int timeLimit = 10000;

    public FirmsCircleGameBucket(String str, ArrayList entities, int samplelength) {
        super(str, entities, samplelength); //- not passing these up here,

//        System.out.println("Making new FirmsCircleBucket");

        //only grabbing two numbers: first Firm's time of arrival in circle
        //and second.
        vals = new double[2][1];

        //set them here, to be replaced only if model run doesn't finish
        vals[0][0] = -1;
        vals[1][0] = -1;


        firms = entities;

        //need to alter circle position depending on spacetype
        switch (gl.space) {

            case Line:
                //if line, vertical is at zero.
                circle = new Ellipse2D.Double();
                circle.setFrameFromCenter(0.5 * gl.width, 0, (0.5 * gl.width) - (circleSize / 2), 0 - (circleSize / 2));

                break;

            case Torus:
                circle = new Ellipse2D.Double();
                circle.setFrameFromCenter(0.5 * gl.width, 0.5 * gl.width, (0.5 * gl.width) - (circleSize / 2), (0.5 * gl.width) - (circleSize / 2));



        }//end switch 


    }

    @Override
    public void grabData() {

//        System.out.println("Circle game bucket checking firms");


//        System.out.println("Point: " + f.getx() + ", " + f.gety());
//        System.out.println("vals now: " + vals[0][0] + "," + vals[1][0]);

        //00 = 'first firm in'
        //01 = 'both firms in' - can kill run then

        //check if either Firm got into defined circle.
        f = firms.get(0);

        //if the first firm is already in, I'm the last one.
        if (circle.contains(f.getPoint())) {

            if (vals[0][0] != -1) {
                vals[1][0] = gl.day;
//                System.out.println("killed from bucket");
                gl.killSwitch = true;
            } else {
                vals[0][0] = gl.day;
            }

        }

        //check if either Firm got into defined circle.
        f = firms.get(1);

        //if the first firm is already in, I'm the last one.
        if (circle.contains(f.getPoint())) {

            if (vals[0][0] != -1) {
                vals[1][0] = gl.day;
//                System.out.println("killed from bucket");
                gl.killSwitch = true;
            } else {
                vals[0][0] = gl.day;
            }

        }


//        if (circle.contains(f.getPoint())&& vals[0][0]==-1) {
//
//            firm1 = true;
//            vals[0][0] = gl.day;
//
//        }
//
//        f = firms.get(1);
//
//        if (circle.contains(f.getPoint())&& vals[1][0]==-1) {
//
//            firm2 = true;
//            vals[1][0] = gl.day;
//
//        }

        //if both are in the circle, or if the time limit is reached, kill model run
//        if (firm1 && firm2) {
//
//            firm1 = false;
//            firm2 = false;
//            gl.killSwitch = true;
//
//            //probably don't need this - time limit on model run will finish first
//        }
//        } else if (gl.day > timeLimit) {
//
//            vals[0][0] = -1;
//            firm1 = false;
//            firm2 = false;
//            gl.killSwitch = true;
//
//        }


    }//end method grabdata
}
