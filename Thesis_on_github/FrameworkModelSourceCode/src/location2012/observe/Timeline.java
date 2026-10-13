package location2012.observe;

import java.util.ArrayList;
import java.util.Collections;
import location2012.Actor;
import location2012.Firm;
import location2012.utils.Timer;
import location2012.utils.gl;

/**
 * Timeline: schedules all events for the model, including 1. When (in model
 * time) to draw to the screen [different from when Processing decides to draw]
 * 2. When calculators will ask for parameters from the model 3. When agent
 * events will need to happen
 *
 * @author Olner Dan
 */
public class Timeline implements Audible {

    public static ArrayList<Shouter> shouters = new ArrayList<Shouter>();
    //for predicting model run length
    //avDayTime: average time to work out one day
    long nowSecs = 0, lastSecs;
    double lastTime;
    double avDayTime = 0;
    public int day, modelRunDays;
    //ArrayList shouters - available via interface Audible
    ArrayList<Shouter> endShouters = new ArrayList<Shouter>();

    /**
     *
     */
    public Timeline(int modelRunDays) {

        day = 0;
        this.modelRunDays = modelRunDays;

    }

    //increment by one day
    /**
     *
     * @return
     */
    public int nextDay() {


//        Firm f;
//
//        for (Actor a : gl.firms) {
//
//            f = (Firm) a;
//            System.out.println("Timeline. ID: " + f.ID + ", x: "
//                    + f.x + ", contribTime: " + f.currentContributedTime
//                    + ", del: " + f.getDeliveryCost());
//
//        }

        //to give data chance to settle down. If these seems weird, it's because it is.
        //There are horrible sync problems I'm hacking the mofo out of
        if (gl.day < 200) {
            try {
                if (gl.timeDelay < 5) {
                    Thread.sleep(5);
                } else {
                    Thread.sleep(gl.timeDelay);
                }
//                System.out.println("awng!");
            } catch (InterruptedException e) {
            }
        }

        if (gl.timeDelay > 0) {
            try {
                Thread.sleep(gl.timeDelay);
//                System.out.println("awng!");
            } catch (InterruptedException e) {
            }

        }


//        if(day==4) {
//            p.print(1);
//        }

        //check to see if killSwitch has been triggered
        if (gl.killSwitch) {
            System.out.println("Killswitch called");
            gl.dataWritePointNumber = day / gl.dataWriteGap;
            gl.killSwitch = false;
            itsTheEndOfTheWorld();
        }

        day++;
        gl.day = day;
        //hack for pausing screen at set point
        if (false) {
//        if (day == 150) {

            //Only works on those set to silenceable
            for (Shouter s : Timeline.shouters) {
                System.out.println(s.toggleSilenced());
            }

            gl.pauseGraphs = true;

        }




        //testing Processing ticks vs days
//        System.out.println("day " + day + ", MainViz: " + gl.MainVizTicks + ", graph: " + gl.graphVizTicks);

//        lastTime = nowTime;
        nowSecs = Timer.getElapsedSecondsNow();
        //what's average time per day's calculations?
        avDayTime = (double) nowSecs / (double) day;

        //p.p("day " + day);
        if (gl.paramSweepVal != -1) {
            System.out.print("Sweepval: " + gl.paramSweepVal + ", ");
        }


        if (gl.modelRunDays > 0) {
            System.out.println("Model run " + (gl.currentRun + 1) + " |--| day " + day
                    + ", iteration time: " + (Timer.getTimeNow() - lastTime) + ", elapsed time: " + (nowSecs / 60)
                    + " mins " + nowSecs % 60 + " secs");

            System.out.println("|--| predicted to finish remaining "
                    + (gl.modelRunDays - day) + " days in: " + ((int) (avDayTime * (gl.modelRunDays - day)) / 60)
                    + " mins " + ((int) (avDayTime * (gl.modelRunDays - day) % 60) + " secs"));
        }

        lastTime = Timer.getTimeNow();

//        System.out.println("day: " + day);


        //At some point, other things will happen when model runs finish -
        //allowing access to visualising key data etc.

        //if its the end of the model, do end of modelly things.
        //if modelRunDays was zero, take that to mean "run forever"
        if (modelRunDays < day + 1 && modelRunDays != 0) {
            itsTheEndOfTheWorld();
        }

        //p.p("day " + day);
        //check to see if any shouters need to tell their listeners
        //This *should* be an implemented method to *make sure* audibles tell their
        //shouters to shout.
        //Well... it *is* an implemented method, but it's not guaranteed that it
        //will be called in a consistent way. Will have to think about this some more
        checkValueChange();

        return day;

    }

    //In case one wants to set the modelRunDays from somewhere other than
    //instance variables for the Canvas class
    public void setModelRunDays(int modelRunDays) {

        this.modelRunDays = modelRunDays;


    }

    //Implemented - to check to see if any shouters need to know
    /**
     *
     */
    public void checkValueChange() {

//        System.out.println("shouter num: " + shouters.size());

        for (Shouter s : shouters) {

            //for now we're just checking for equality
            //later we'll need to ask the shouters to say
            //whether they're interested in a particular change
            if (day > 0 && day % s.value == 0) {

                shout(s);

            }

        }
    }//end method check value change

    /**
     *
     * @param s
     */
    public void shout(Shouter s) {

        if (modelRunDays > day) {
//            s.lstr.heard(new ShoutEvent(day));
            s.shout(new ShoutEvent(day));
        } else {
            //else its the end of the world! Yet another horrible hack
//            s.lstr.heard(new ShoutEvent(-1));
            s.shout(new ShoutEvent(-1));
        }

    }

    public void itsTheEndOfTheWorld() {


        //output end time
        Timer.printElapsedTimeNow("End of model run");

        //temporary measure - endShouter array checked
        for (Shouter s : endShouters) {

            shout(s);

        }

        System.out.println("Ping!");

        //stop this model run
        gl.modelRunning = false;

    }

    /**
     *
     * @param s
     */
    public void registerShouter(Shouter s) {

        shouters.add(s);

    }

    //Temporary measure! Hackery!
    public void registerEndOfWorldShouter(Shouter s) {

        endShouters.add(s);

    }

    public void sortListenersByWeight() {

        //sort shouters by their weight
        //Collections.sort(shouters, new ListenerCompare());
        Collections.sort(shouters);

    }

//    /*
//     * Currently not an implemented method of Audible; just putting into timeline
//     * to see how that goes...
//     */
//    public void addListenerDirect(Listener l, int val) {
//
//        l.giveShouterTo(this, val);
//
//    }
    //Just for checking the order...
    public void checkShouterOrder() {

        //check order of Listeners
        for (Shouter sh : shouters) {

            System.out.println("ListWeight: " + sh.lstr.getWeight() + ", name: " + sh.lstr.getClass().getName());

        }

    }
}
