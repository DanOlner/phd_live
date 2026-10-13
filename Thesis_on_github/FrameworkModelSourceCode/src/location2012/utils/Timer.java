/*
 * Timer: code adapted from http://www.devdaily.com/java/java-timestamp-example-current-time-now
 */
package location2012.utils;

import java.util.Calendar;
import java.util.Date;

/**
 *
 * @author Olner Dan
 */
public class Timer {

    public static Calendar c;
    static Date now;
    static long time, compT, mills;

    public Timer() {

        // create a java calendar instance
        c = Calendar.getInstance();

        now = c.getTime();


        //For working out total time elapsed
        time = now.getTime();
        p.p("Start time: " + (time - now.getTime()));

    }

    public static double getTimeNow() {

        return (double) now.getTime();

    }

    public static void printElapsedTimeNow(String evt) {

        c = Calendar.getInstance();

        now = c.getTime();

        //get no of milliseconds
        mills = (now.getTime() - time);
        //get number of seconds
        compT = mills / (long) 1000.0;

        //p.p(evt + ": " + (now.getTime()-time));

        if (compT > 60) {
            p.p(evt + ": " + compT / 60 + " minutes, " + (compT % 60) + " seconds");
        } else {
            p.p(evt + ": " + compT + " seconds, " + mills + " milliseconds");
        }

    }
    public static long getElapsedSecondsNow() {

        c = Calendar.getInstance();

        now = c.getTime();

        //get no of milliseconds
        mills = (now.getTime() - time);
        //get number of seconds
        compT = mills / (long) 1000.0;

        //p.p(evt + ": " + (now.getTime()-time));

//        if (compT > 60) {
//            p.p(evt + ": " + compT / 60 + " minutes, " + (compT % 60) + " seconds");
//        } else {
//            p.p(evt + ": " + compT + " seconds, " + mills + " milliseconds");
//        }
        return compT;

    }
}
