/*
 * Torus: takes care of actors' location issues, and deals with a space wraparound
 * e.g. when actors are searching space
 * Deals with actors implementing the location interface; later, we need to look at
 * how to deal with other entities that implement that interface. Later.
 */
package location2012.geog;

import location2012.utils.gl;
import location2012.utils.PullOneClassFromArray;
import location2012.utils.Randoms;
import location2012.utils.Trig;
import location2012.utils.p;
import java.awt.Color;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.awt.geom.RectangularShape;
import java.util.ArrayList;
import java.util.Random;
import processing.core.PApplet;
import location2012.Actor;
import location2012.Firm;
import location2012.Person;
import location2012.PersonAction;
import location2012.SingleActorAction;
import location2012.econs.Good;
import location2012.econs.GoodSeller;
import location2012.utils.*;

/**
 *
 * @author Olner Dan
 */
public class SpaceTools {
//public class SpaceTools implements Drawable {

    PApplet pa;
    //Who is in this space?
    public static ArrayList<HasLocation> isInSpace = new ArrayList<HasLocation>();
    //list of actors
    public ArrayList<Actor> actors = new ArrayList<Actor>();
    //What firms are in this space? HACKEROO! But firmsInRange can use - massive speed up
    public ArrayList<HasLocation> firmsInSpace = new ArrayList<HasLocation>();
    //Used for 'contains' searches
    Ellipse2D.Double circle;
    //Torus is square; no height needed
    public static int width;
    //if using two discrete regions, distance between regions.
    //Space will have to work out width to accommodate it.
    //Need to keep 'width' above for drawing operations.
    public double distance;
    //used for referencing locations here and there
//    Point2D.Double pt = new Point2D.Double();
    Point2D.Double pt;
    //int lookatme = 0; //ref for actor we want to see output for
    //Used by whosInRadius for finding... whos in radius
    //private RadiusSearcher rad = new RadiusSearcher();

    /*
     * Vars for space methods
     */
    //radius - keeping outside isInRadius method to use elsewhere e.g. setCircles
    double range;
    //Array for four circles for isInRadius check
    //Or just two of them for the continuousRaceTrack check
    RectangularShape[] rangeShape;
    //Checks for moving the above circles to the correct detection points
    boolean isInTop, isInLeft;
    //
    /*
     * Drawing vars
     *
     */
    //Used for finding upper, lower bound of people's utility for colouring the range
    //These two found "this turn"
    double findHighUtil, findLowUtil;
    //These two used "this turn", set when the other two above are found
    //after running through them all.
    double useHighUtil, useLowUtil;
    //same for good amounts
    double findHighAmount, findLowAmount, useHighAmount, useLowAmount;
    /*
     * Vars used by static methods
     */
    //Used by density cost calculator
    static double weight;
    Firm f;
    LocMemory lm;

    /**
     * Constructor if you're adding a HasLocation arraylist to start with
     *
     * width = the size of the world in days to traverse numradius = the average
     * number of agents a day's radius from any point (Actual agent location is
     * randomised, hence it's an average) The total number of agents is worked
     * out from these two.
     *
     * @param actors
     * @param width
     * @param numinradius
     */
    public SpaceTools(double width) {

        //Set vars

        //space-specific setups
        switch (gl.space) {

            case Point:

                this.width = (int) width;
                break;

            case TwoRegion:

                //for two region, width passed in is actually distance between
                //the two regions. So we need to:
                //1. set distance between two points, by giving them relative locations
                //2. Make width large enough that no wraparound effects will occur
                //For this last, just need to make sure width > distance * 2
                //and then make sure the two locations are equidistance from the centre point
//                this.width = ((int) width) * 2;
//                //round up
//                System.out.println(this.width);
//                this.width += 2;
//                System.out.println(this.width);
//                //set distance between regions
//                distance = width;

                //just need the one circle
                rangeShape = new Ellipse2D.Double[1];

                rangeShape[0] = new Ellipse2D.Double(0, 0, 0, 0);

                break;

            case Line:

                this.width = (int) width;

                //if space isn't being wrapped, only need the one circle
                if (gl.wrapSpace) {
                    rangeShape = new Ellipse2D.Double[2];
                    for (int i = 0; i < 2; i++) {
                        rangeShape[i] = new Ellipse2D.Double(0, 0, 0, 0);
                    }
                } else {
                    rangeShape = new Ellipse2D.Double[1];
                    rangeShape[0] = new Ellipse2D.Double(0, 0, 0, 0);
                }

                //Instantiate circles for isInRaceTrack checking
                break;



            case Torus:

                this.width = (int) width;

                if (gl.wrapSpace) {
                    //if all firms are under consideration, need rectangle check, not circle
                    if (gl.peopleConsiderAll) {

                        rangeShape = new Rectangle2D.Double[4];
                        for (int i = 0; i < 4; i++) {
                            rangeShape[i] = new Rectangle2D.Double(0, 0, 0, 0);
                        }

                    } else {
                        rangeShape = new Ellipse2D.Double[4];
                        for (int i = 0; i < 4; i++) {
                            rangeShape[i] = new Ellipse2D.Double(0, 0, 0, 0);
                        }
                    }
                    //if not wrapSpace...
                } else {
                    rangeShape = new Ellipse2D.Double[1];
                    rangeShape[0] = new Ellipse2D.Double(0, 0, 0, 0);
                }

                //Instantiate circles for isInTorus checking
                break;

        }

    }

    /**
     * Set initial positions for agents: defaults are different for different
     * spatial models For many models, overridden by methods placing actors in
     * bespoke patterns
     */
    public static void setDefaultActorPositions() {

        //space-specific setups
        switch (gl.space) {

            case Point:
                //currently nothing to do here: location value defaults to zero
                //when Actors are constructed
                break;

            case TwoRegion:

                //Randomisation would, on average, just mean a 50/50 split between the two regions.
                //Need to actually define how many in each region to start.
                //That's done when the space is set up. So.
                //Make the two regions equidistant for the space - note, width is set above as
                //2 or more times distance between regions
                double centrePoint = gl.width / 2;

                //Set region locations, to be used by code. From point of view of actors in regions
                //only the distance between them should matter
                gl.TwoR_Region0x = centrePoint - (gl.width / 2);
                gl.TwoR_Region1x = centrePoint + (gl.width / 2);

                System.out.println("two region initpositions. width: "
                        + gl.width + ", region 0 x: " + gl.TwoR_Region0x + ", region 1 x: " + gl.TwoR_Region1x);

                //Set up actor locations, in proportions set in Main
                //We need a temporary record of firms and people separately

//                ArrayList<Actor> people = new ArrayList<Actor>();
////                PullOneClassFromArray.PULLCLASS(isInSpace, people, Person.class);
//                PullOneClassFromArray.PULLCLASS(isInSpace, people, new Person(0));
//
//                //same for firms
//                ArrayList<Actor> firms = new ArrayList<Actor>();
////                PullOneClassFromArray.PULLCLASS(isInSpace, firms, Firm.class);
//                PullOneClassFromArray.PULLCLASS(isInSpace, firms, new Firm(0));

                //gl.TwoR_PeoplePopRegZero is 'population in region 0.'
                //If it's zero, all pop should be in region 1. Clear as mud?
                int popInRegZero = (int) (gl.people.size() * gl.TwoR_PeoplePopRegZero);

                //split actors and firms between the two regions
                for (int i = 0; i < gl.people.size(); i++) {

                    //i/people size will be 1 at the end of the loop.
                    if (i < popInRegZero) {
                        gl.people.get(i).setx(gl.TwoR_Region0x);
                    } else {
                        gl.people.get(i).setx(gl.TwoR_Region1x);
                    }

//                    System.out.println("Person " + i + " loc: " + gl.people.get(i).getPoint().x);
                    
                }

                popInRegZero = (int) (gl.firms.size() * gl.TwoR_FirmPopRegZero);


                for (int i = 0; i < gl.firms.size(); i++) {

                    //i/people size will be 1 at the end of the loop.
                    if (i < popInRegZero) {
                        gl.firms.get(i).setx(gl.TwoR_Region0x);
                    } else {
                        gl.firms.get(i).setx(gl.TwoR_Region1x);
                    }

                    System.out.println("Firm " + i + " loc: " + gl.firms.get(i).getPoint().x);

                }

                break;

            //on a horizontal line, at vertical position zero
            case Line:

                //randomise positions relative to the size of the space
                for (HasLocation a : isInSpace) {

                    //Remember: wrapper has a "HasLocation" in it,
                    //We can access with h. Sod getters.

                    //Random x position relative to space width
                    a.setx(Randoms.nextDouble() * (double) width);
                    //p.p("setx at: " + a.getx());

                }

                //using landlords? Spread them across the line
                if (gl.landCostsOn > 0) {

                    //spread them across line
                    putActorsInAHorizontalLine(gl.landLords);

                }

                break;

            case Torus:
                //randomise positions relative to the size of the space
                for (HasLocation a : isInSpace) {

                    //Remember: wrapper has a "HasLocation" in it,
                    //We can access with h. Sod getters.

                    //Random x position relative to space width
                    a.setx(Randoms.nextDouble() * (double) width);
                    //p.p("setx at: " + a.getx());

                    //Random y position relative to space width
                    a.sety(Randoms.nextDouble() * (double) width);

                }

                //using landlords? Spread them across a grid
                if (gl.landCostsOn > 0) {

                    //spread them across line
                    putActorsOnAGrid(gl.landLords);

                }

                break;
        }//end switch

    }//end method initialisePositions

    /**
     * Return array of Actors within 'range' which will mean different things
     * for different space types
     *
     * For Torus: Who is inside a radius of Point p? Unit radius is 1 day's
     * distance Get Buffer to do virtual point checks
     *
     * 2nd version for finding particular types of object
     *
     * @param pt
     * @return
     */
//    public LocMemory whosInRangeOfType(HasLocation me, Point2D.Double loc, String type, double range) {
//
//        //so setCircles can use it
//        this.range = range;
//
//        //for storing found info - relative locations and references to actors
//        LocMemory lm = new LocMemory();
//
////        //If we're getting all firms, skip this exercise and return all
//        if (gl.peopleConsiderAllFirms) {
//
//            //If no wrap, just add Firms, since we know that's what we're doing...
//
//            if (!gl.wrapSpace) {
//                switch (gl.space) {
//                    case Point:
////                            lm.addActor((Actor) h, new Point2D.Double(0, 0));
//                    case Line:
////                            lm.addActor((Actor) h, new Point2D.Double(h.getPoint().x - pt.x, 0));
//                    case Torus:
//
//                        for (Actor a : gl.firms) {
//
//                            //need to return firm location relative to my location
//                            //hacking currently - will need to deal with space properly at some point
//                            lm.addActor(a, new Point2D.Double(a.getPoint().x - loc.x,
//                                    a.getPoint().y - loc.y));
//
//                        }
//
//                }//end switch
//
//                return lm;
//
//            } else {
//                //if space is wrapping and all firms under consideration, change the range
//                //so rectangles will check whole space from actor's position
//                this.range = width / 2;
//            }
//
//
//        }//end if gl.peopleConsiderAllFirms
//
//        //make sure the pt given is in the space. It may not be if e.g. a samplepoint
//        //has been taken from a point already close to the edge
//        //Not attaching directly to pt; altering it in checkInTorus
//        pt = (Point2D.Double) loc.clone();
//
//        switch (gl.space) {
//
//            case Point:
//                //Just need to return all appropriate Actors
//                for (HasLocation h : isInSpace) {
//                    if (h.getClass().getSimpleName().equals(type) && !h.equals(me)) {
//                        lm.addActor((Actor) h, new Point2D.Double(0, 0));
//                    }
//                }
//
//                break;// break point switch
//
//            case TwoRegion:
//
//                //only need the one circle check
//                //So, set one circle where the point actually is...
//                rangeShape[0].setFrameFromCenter(pt.x, 0, pt.x - range, 0 - range);
//
//                //check who's in those circles...
//                for (HasLocation h : isInSpace) {
//
//                    //check I'm not adding myself
//                    //check it's the type asked for
////                    if ((circles[0].contains(h.getPoint()) && !h.equals(me) && h.getClass().getSimpleName().equals(type))
////                            || (gl.peopleConsiderAllFirms && !h.equals(me) && h.getClass().getSimpleName().equals(type))) {
//                    if ((rangeShape[0].contains(h.getPoint()) || gl.peopleConsiderAllFirms) && !h.equals(me) && h.getClass().getSimpleName().equals(type)) {
//
//                        //pass in ref to actor
//                        //and relative vector
//                        //not messing with wraparound here so can use actor's loc directly
//                        lm.addActor((Actor) h, new Point2D.Double(h.getPoint().x - pt.x, 0));
//
//                    }
//
//                }//end foreach HasLocation
//
//                break;
//
//
//
//            case Line:
//
//                pt = checkInTorus(pt);
//
//                setLineCircles();
//
//                //System.out.println("isInSpace line check, size of circle array: " + circles.length);
//
//                //check who's in those circles...
//                for (HasLocation h : isInSpace) {
//
//                    for (RectangularShape d : rangeShape) {
//
//                        //check I'm not adding myself
//                        //check it's the type asked for
//                        if ((d.contains(h.getPoint()) || gl.peopleConsiderAllFirms) && !h.equals(me) && h.getClass().getSimpleName().equals(type)) {
//
//                            //pass in ref to actor
//                            //and relative vector
//                            //recall: if we use the centre of the circle doing the testing
//                            //then the relative vector will be correct. Magic!
//                            lm.addActor((Actor) h, new Point2D.Double(h.getPoint().x - d.getCenterX(), 0));
//                        }
//                    }//end foreach Ellipse
//                }//end foreach HasLocation
//
//                break;
//
//            case Torus:
//
//                pt = checkInTorus(pt);
//
//                //Position testing circles
//                setTorusCircles();
//
//                //check who's in those circles...
//                for (HasLocation h : isInSpace) {
//
//                    //System.out.println("whosinrange type: " + type + ", " + h.getClass().getSimpleName());
//
//                    for (RectangularShape d : rangeShape) {
//
//                        //check I'm not adding myself
//                        //check it's the type asked for
//                        if ((d.contains(h.getPoint()) || gl.peopleConsiderAllFirms) && !h.equals(me) && h.getClass().getSimpleName().equals(type)) {
////                        if ((d.contains(h.getPoint()) && !h.equals(me) && h.getClass().getSimpleName().equals(type))
////                                || (gl.peopleConsiderAllFirms && !h.equals(me) && h.getClass().getSimpleName().equals(type))) {
//
//                            //pass in ref to actor
//                            //and relative vector
//                            //recall: if we use the centre of the circle doing the testing
//                            //then the relative vector will be correct. Magic!
//                            lm.addActor((Actor) h, new Point2D.Double(h.getPoint().x - d.getCenterX(),
//                                    h.getPoint().y - d.getCenterY()));
//
//                            //System.out.println("added firm...");
//                        }
//                    }//end foreach Ellipse
//                }//end foreach HasLocation
//                break; //end space switch
//
//        }//end switch
//
//        return lm;
//
//    }
    /**
     * Used only by People: find out which Firms are in range. Much faster than
     * checking every agent
     *
     * @param me
     * @param loc
     * @param range
     * @return
     */
    public LocMemory firmsInRange(HasLocation me, Point2D.Double loc, double range) {

        //so setCircles can use it
        this.range = range;

        //for storing found info - relative locations and references to actors
        LocMemory lm = new LocMemory();

//        //If we're getting all firms, skip this exercise and return all
        if (gl.peopleConsiderAll) {

            //If no wrap, just add Firms, since we know that's what we're doing...

            if (!gl.wrapSpace) {
                switch (gl.space) {
                    case Point:
//                            lm.addActor((Actor) h, new Point2D.Double(0, 0));
                    case Line:
//                            lm.addActor((Actor) h, new Point2D.Double(h.getPoint().x - pt.x, 0));
                    case Torus:

                        for (Actor a : gl.firms) {

                            //need to return firm location relative to my location
                            //hacking currently - will need to deal with space properly at some point
                            lm.addActor(a, new Point2D.Double(a.getPoint().x - loc.x,
                                    a.getPoint().y - loc.y));

                        }

                }//end switch

                return lm;

            } else {
                //if space is wrapping and all firms under consideration, change the range
                //so rectangles will check whole space from actor's position
//                System.out.println("set range");
                this.range = width / 2;
                this.range -= (width / 10000);

            }


        }//end if gl.peopleConsiderAllFirms

        //make sure the pt given is in the space. It may not be if e.g. a samplepoint
        //has been taken from a point already close to the edge
        //Not attaching directly to pt; altering it in checkInTorus
        pt = (Point2D.Double) loc.clone();

        switch (gl.space) {

            case Point:
                //Just need to return all appropriate Actors
                for (HasLocation h : firmsInSpace) {
                    lm.addActor((Actor) h, new Point2D.Double(0, 0));
                }

                break;// break point switch

            case TwoRegion:

                //only need the one circle check
                //So, set one circle where the point actually is...
                rangeShape[0].setFrameFromCenter(pt.x, 0, pt.x - range, 0 - range);

                //check who's in those circles...
                for (HasLocation h : firmsInSpace) {

                    //check I'm not adding myself
                    //check it's the type asked for
//                    if ((circles[0].contains(h.getPoint()) && !h.equals(me) && h.getClass().getSimpleName().equals(type))
//                            || (gl.peopleConsiderAllFirms && !h.equals(me) && h.getClass().getSimpleName().equals(type))) {
                    if ((rangeShape[0].contains(h.getPoint()))) {

                        //pass in ref to actor
                        //and relative vector
                        //not messing with wraparound here so can use actor's loc directly
                        lm.addActor((Actor) h, new Point2D.Double(h.getPoint().x - pt.x, 0));

                    }

                }//end foreach HasLocation

                break;



            case Line:

                pt = checkInTorus(pt);

                setLineCircles();

                //System.out.println("isInSpace line check, size of circle array: " + circles.length);

                //check who's in those circles...
                for (HasLocation h : firmsInSpace) {

                    for (RectangularShape d : rangeShape) {

                        //check I'm not adding myself
                        //check it's the type asked for
                        if ((d.contains(h.getPoint()))) {

                            //pass in ref to actor
                            //and relative vector
                            //recall: if we use the centre of the circle doing the testing
                            //then the relative vector will be correct. Magic!
                            lm.addActor((Actor) h, new Point2D.Double(h.getPoint().x - d.getCenterX(), 0));
                        }
                    }//end foreach Ellipse
                }//end foreach HasLocation

                break;

            case Torus:

                pt = checkInTorus(pt);

                //Position testing circles
                setTorusShapes();

//                System.out.println("firmsInSpace size: " + firmsInSpace.size());

                //check who's in those circles...
                for (HasLocation h : firmsInSpace) {

                    //System.out.println("whosinrange type: " + type + ", " + h.getClass().getSimpleName());

                    for (RectangularShape d : rangeShape) {

                        //check I'm not adding myself
                        //check it's the type asked for
                        if ((d.contains(h.getPoint()))) {
//                        if ((d.contains(h.getPoint()) && !h.equals(me) && h.getClass().getSimpleName().equals(type))
//                                || (gl.peopleConsiderAllFirms && !h.equals(me) && h.getClass().getSimpleName().equals(type))) {

                            //pass in ref to actor
                            //and relative vector
                            //recall: if we use the centre of the circle doing the testing
                            //then the relative vector will be correct. Magic!
                            lm.addActor((Actor) h, new Point2D.Double(h.getPoint().x - d.getCenterX(),
                                    h.getPoint().y - d.getCenterY()));

                            //System.out.println("added firm...");
                        }
                    }//end foreach Ellipse
                }//end foreach HasLocation
                break; //end space switch

        }//end switch

        return lm;

    }

    /**
     * Specific single-actor search: find all actors, including myself.
     *
     * @param me
     * @param loc
     * @param range
     * @return
     */
    public LocMemory allActorsInRange(HasLocation me, Point2D.Double loc, double range) {

        //If I'm excluding myself, go to a different method
        if (gl.excludeMe) {
//            System.out.println("BOING!");
            return (getLocMemoryForAllButNotMe(me, loc));
        }

        //so setCircles can use it
        this.range = range;

        //for storing found info - relative locations and references to actors
        LocMemory lm = new LocMemory();

//        //If we're getting all firms, skip this exercise and return all
        if (gl.peopleConsiderAll) {

            //If no wrap, just add Firms, since we know that's what we're doing...

            if (!gl.wrapSpace) {
                switch (gl.space) {
                    case Point:
//                            lm.addActor((Actor) h, new Point2D.Double(0, 0));
                    case Line:
//                            lm.addActor((Actor) h, new Point2D.Double(h.getPoint().x - pt.x, 0));
                    case Torus:

                        for (Actor a : gl.people) {

                            //need to return firm location relative to my location
                            //hacking currently - will need to deal with space properly at some point
                            lm.addActor(a, new Point2D.Double(a.getPoint().x - loc.x,
                                    a.getPoint().y - loc.y));

                        }

                }//end switch

                return lm;

            } else {
                //if space is wrapping and all firms under consideration, change the range
                //so rectangles will check whole space from actor's position
//                System.out.println("set range");
                this.range = width / 2;
                this.range -= (width / 10000);

            }


        }//end if gl.peopleConsiderAllFirms

        //make sure the pt given is in the space. It may not be if e.g. a samplepoint
        //has been taken from a point already close to the edge
        //Not attaching directly to pt; altering it in checkInTorus
        pt = (Point2D.Double) loc.clone();

        switch (gl.space) {

            case Point:
                //Just need to return all appropriate Actors
                for (HasLocation h : isInSpace) {
                    lm.addActor((Actor) h, new Point2D.Double(0, 0));
                }

                break;// break point switch

            case TwoRegion:

                //only need the one circle check
                //So, set one circle where the point actually is...
                rangeShape[0].setFrameFromCenter(pt.x, 0, pt.x - range, 0 - range);

                //check who's in those circles...
                for (HasLocation h : isInSpace) {

                    //check I'm not adding myself
                    //check it's the type asked for
//                    if ((circles[0].contains(h.getPoint()) && !h.equals(me) && h.getClass().getSimpleName().equals(type))
//                            || (gl.peopleConsiderAllFirms && !h.equals(me) && h.getClass().getSimpleName().equals(type))) {
                    if ((rangeShape[0].contains(h.getPoint()))) {

                        //pass in ref to actor
                        //and relative vector
                        //not messing with wraparound here so can use actor's loc directly
                        lm.addActor((Actor) h, new Point2D.Double(h.getPoint().x - pt.x, 0));

                    }

                }//end foreach HasLocation

                break;



            case Line:

                pt = checkInTorus(pt);

                setLineCircles();

                //System.out.println("isInSpace line check, size of circle array: " + circles.length);

                //check who's in those circles...
                for (HasLocation h : isInSpace) {

                    for (RectangularShape d : rangeShape) {

                        //check I'm not adding myself
                        //check it's the type asked for
                        if ((d.contains(h.getPoint()))) {

                            //pass in ref to actor
                            //and relative vector
                            //recall: if we use the centre of the circle doing the testing
                            //then the relative vector will be correct. Magic!
                            lm.addActor((Actor) h, new Point2D.Double(h.getPoint().x - d.getCenterX(), 0));
                        }
                    }//end foreach Ellipse
                }//end foreach HasLocation

                break;

            case Torus:

                pt = checkInTorus(pt);

                //Position testing circles
                setTorusShapes();

//                System.out.println("firmsInSpace size: " + firmsInSpace.size());

                //check who's in those circles...
                for (HasLocation h : isInSpace) {

                    //System.out.println("whosinrange type: " + type + ", " + h.getClass().getSimpleName());

                    for (RectangularShape d : rangeShape) {

                        //check I'm not adding myself
                        //check it's the type asked for
                        if ((d.contains(h.getPoint()))) {
//                        if ((d.contains(h.getPoint()) && !h.equals(me) && h.getClass().getSimpleName().equals(type))
//                                || (gl.peopleConsiderAllFirms && !h.equals(me) && h.getClass().getSimpleName().equals(type))) {

                            //pass in ref to actor
                            //and relative vector
                            //recall: if we use the centre of the circle doing the testing
                            //then the relative vector will be correct. Magic!
                            lm.addActor((Actor) h, new Point2D.Double(h.getPoint().x - d.getCenterX(),
                                    h.getPoint().y - d.getCenterY()));

                            //System.out.println("added firm...");
                        }
                    }//end foreach Ellipse
                }//end foreach HasLocation
                break; //end space switch

        }//end switch

        return lm;

    }

    /**
     * Specific single-actor search: find all actors, including myself.
     *
     * @param me
     * @param loc
     * @param range
     * @return
     */
    public LocMemory getLocMemoryForAllButNotMe(HasLocation me, Point2D.Double loc) {

        //for storing found info - relative locations and references to actors
        lm = new LocMemory();


        //If we're getting all firms, skip this exercise and return all

        //If no wrap, just add Firms, since we know that's what we're doing...

        switch (gl.space) {
            case Point:
//                            lm.addActor((Actor) h, new Point2D.Double(0, 0));
            case Line:
//                            lm.addActor((Actor) h, new Point2D.Double(h.getPoint().x - pt.x, 0));
            case Torus:

                for (Actor a : gl.people) {

                    //need to return firm location relative to my location
                    //hacking currently - will need to deal with space properly at some point
                    if (!a.equals(me)) {
                        lm.addActor(a, new Point2D.Double(a.getPoint().x - loc.x,
                                a.getPoint().y - loc.y));

                    }
                }//end for actor a

        }//end switch

        return lm;

    }

    /**
     * Return array of Actors within 'range' - all actors which will mean
     * different things for different space types
     *
     * For Torus: Who is inside a radius of Point p? Unit radius is 1 day's
     * distance Get Buffer to do virtual point checks
     *
     * 2nd version for finding particular types of object
     *
     * @param pt
     * @return
     */
    public LocMemory whosInRange(HasLocation me, Point2D.Double loc, double range) {

        //so setCircles can use it
        this.range = range;

        //for storing found info - relative locations and references to actors
        lm = new LocMemory();

        //make sure the pt given is in the space. It may not be if e.g. a samplepoint
        //has been taken from a point already close to the edge
        //Not attaching directly to pt; altering it in checkInTorus
        pt = (Point2D.Double) loc.clone();
//        pt.x = loc.x;
//        pt.y = loc.y;


        //if considering all regardless of distance 

        switch (gl.space) {

            case Point:
                //Just need to return all appropriate Actors
                for (HasLocation h : isInSpace) {

                    lm.addActor((Actor) h, new Point2D.Double(0, 0));

                }

                break;// break point switch

            case TwoRegion:

                //only need the one circle check
                //So, set one circle where the point actually is...
                rangeShape[0].setFrameFromCenter(pt.x, 0, pt.x - range, 0 - range);

                //check who's in those circles...
                for (HasLocation h : isInSpace) {

                    //check I'm not adding myself
                    //check it's the type asked for
//                    if ((circles[0].contains(h.getPoint()) || gl.peopleConsiderAllFirms) && !h.equals(me)) {
                    if (rangeShape[0].contains(h.getPoint()) && !h.equals(me)) {

                        //pass in ref to actor
                        //and relative vector
                        //not messing with wraparound here so can use actor's loc directly
                        lm.addActor((Actor) h, new Point2D.Double(h.getPoint().x - pt.x, 0));

                    }

                }//end foreach HasLocation

                break;



            case Line:

                pt = checkInTorus(pt);

                setLineCircles();

                //System.out.println("isInSpace line check, size of circle array: " + circles.length);

                //check who's in those circles...
                for (HasLocation h : isInSpace) {

                    for (RectangularShape d : rangeShape) {

                        //check I'm not adding myself
                        //check it's the type asked for
//                        if ((d.contains(h.getPoint()) || gl.peopleConsiderAllFirms) && !h.equals(me)) {
                        if (d.contains(h.getPoint()) && !h.equals(me)) {

                            //pass in ref to actor
                            //and relative vector
                            //recall: if we use the centre of the circle doing the testing
                            //then the relative vector will be correct. Magic!
                            lm.addActor((Actor) h, new Point2D.Double(h.getPoint().x - d.getCenterX(), 0));
                        }
                    }//end foreach Ellipse
                }//end foreach HasLocation

                break;

            case Torus:

                pt = checkInTorus(pt);

                //Position testing shapes
                setTorusShapes();

                //check who's in those circles...
                for (HasLocation h : isInSpace) {

                    //System.out.println("whosinrange type: " + type + ", " + h.getClass().getSimpleName());

                    for (RectangularShape d : rangeShape) {

                        //check I'm not adding myself
                        //check it's the type asked for
//                        if ((d.contains(h.getPoint()) || gl.peopleConsiderAllFirms) && !h.equals(me)) {
                        if (d.contains(h.getPoint()) && !h.equals(me)) {

                            //pass in ref to actor
                            //and relative vector
                            //recall: if we use the centre of the circle doing the testing
                            //then the relative vector will be correct. Magic!
                            lm.addActor((Actor) h, new Point2D.Double(h.getPoint().x - d.getCenterX(),
                                    h.getPoint().y - d.getCenterY()));

                            //System.out.println("added firm...");
                        }
                    }//end foreach Ellipse
                }//end foreach HasLocation
                break; //end space switch

        }//end switch

        return lm;

    }

    /*
     * Quick hack for stopping whosInRange from picking up firms too.
     */
    public LocMemory peopleInRange(HasLocation me, Point2D.Double loc, double range) {

        //so setCircles can use it
        this.range = range;

        //for storing found info - relative locations and references to actors
        lm = new LocMemory();

        //make sure the pt given is in the space. It may not be if e.g. a samplepoint
        //has been taken from a point already close to the edge
        //Not attaching directly to pt; altering it in checkInTorus
        pt = (Point2D.Double) loc.clone();
//        pt.x = loc.x;
//        pt.y = loc.y;


        //if considering all regardless of distance 

        switch (gl.space) {

            case Point:
                //Just need to return all appropriate Actors
                for (HasLocation h : gl.people) {

                    lm.addActor((Actor) h, new Point2D.Double(0, 0));

                }

                break;// break point switch

            case TwoRegion:

                //only need the one circle check
                //So, set one circle where the point actually is...
                rangeShape[0].setFrameFromCenter(pt.x, 0, pt.x - range, 0 - range);

                //check who's in those circles...
                for (HasLocation h : gl.people) {

                    //check I'm not adding myself
                    //check it's the type asked for
//                    if ((circles[0].contains(h.getPoint()) || gl.peopleConsiderAllFirms) && !h.equals(me)) {
                    if (rangeShape[0].contains(h.getPoint()) && !h.equals(me)) {

                        //pass in ref to actor
                        //and relative vector
                        //not messing with wraparound here so can use actor's loc directly
                        lm.addActor((Actor) h, new Point2D.Double(h.getPoint().x - pt.x, 0));

                    }

                }//end foreach HasLocation

                break;



            case Line:

                pt = checkInTorus(pt);

                setLineCircles();

                //System.out.println("isInSpace line check, size of circle array: " + circles.length);

                //check who's in those circles...
                for (HasLocation h : gl.people) {

                    for (RectangularShape d : rangeShape) {

                        //check I'm not adding myself
                        //check it's the type asked for
//                        if ((d.contains(h.getPoint()) || gl.peopleConsiderAllFirms) && !h.equals(me)) {
                        if (d.contains(h.getPoint()) && !h.equals(me)) {

                            //pass in ref to actor
                            //and relative vector
                            //recall: if we use the centre of the circle doing the testing
                            //then the relative vector will be correct. Magic!
                            lm.addActor((Actor) h, new Point2D.Double(h.getPoint().x - d.getCenterX(), 0));
                        }
                    }//end foreach Ellipse
                }//end foreach HasLocation

                break;

            case Torus:

                pt = checkInTorus(pt);

                //Position testing shapes
                setTorusShapes();

                //check who's in those circles...
                for (HasLocation h : gl.people) {

                    Actor a = (Actor) h;

                    //System.out.println("whosinrange type: " + type + ", " + h.getClass().getSimpleName());

                    for (RectangularShape d : rangeShape) {

                        //check I'm not adding myself
                        //check it's the type asked for
//                        if ((d.contains(h.getPoint()) || gl.peopleConsiderAllFirms) && !h.equals(me)) {
                        if (d.contains(h.getPoint()) && !h.equals(me)) {

                            //pass in ref to actor
                            //and relative vector
                            //recall: if we use the centre of the circle doing the testing
                            //then the relative vector will be correct. Magic!
                            lm.addActor((Actor) h, new Point2D.Double(h.getPoint().x - d.getCenterX(),
                                    h.getPoint().y - d.getCenterY()));

                            //for data
//                            lm.meanDistanceOfPeopleInDensityCostRadius += lm.actors.get(lm.actors.size()-1).actor.getPoint().distance(me.getPoint());
                            lm.meanDistanceOfPeopleInDensityCostRadius += a.getPoint().distance(me.getPoint());

                        }
                    }//end foreach Ellipse
                }//end foreach HasLocation
                break; //end space switch

        }//end switch

        //for data, find average distance
        lm.meanDistanceOfPeopleInDensityCostRadius /= lm.actors.size();

        return lm;

    }//people in range hack

    /**
     * Return array of Actors within 'range' - all actors which will mean
     * different things for different space types
     *
     * This version takes in array of Actors to check who's in range
     *
     * Also tests for whether to return myself
     *
     * For Torus: Who is inside a radius of Point p? Unit radius is 1 day's
     * distance Get Buffer to do virtual point checks
     *
     *
     * @param pt
     * @return
     */
    public LocMemory whosInRangeOfArray(ArrayList<Actor> actorsIn, HasLocation me, Point2D.Double loc,
            boolean returnMyself, double range) {

        if (returnMyself) {
            actors = (ArrayList<Actor>) actorsIn.clone();
//            System.out.println("returning myself");
        } else {
            actors = getArrayListSubset.GETALLBUTTHIS(actorsIn, me);
        }

        
//        Firm f;
//        
//        for(Actor a: actors) {
//            
//            f = (Firm) a;
//            System.out.println("Timeline. ID: " + f.ID + ", x: "
//                    + f.x + ", contribTime: " + f.currentContributedTime);
//            
//        }

        //so setCircles can use it
        this.range = range;

        //for storing found info - relative locations and references to actors
        lm = new LocMemory();

        //make sure the pt given is in the space. It may not be if e.g. a samplepoint
        //has been taken from a point already close to the edge
        //Not attaching directly to pt; altering it in checkInTorus
        pt = (Point2D.Double) loc.clone();
//        pt.x = loc.x;
//        pt.y = loc.y;
//        if (gl.peopleConsiderAll) {
        if (gl.peopleConsiderAll) {

            //If no wrap, just add Firms, since we know that's what we're doing...
            //using wrapspace = a trickier job
            if (!gl.wrapSpace) {
                
                switch (gl.space) {
                    case Point:
//                            lm.addActor((Actor) h, new Point2D.Double(0, 0));
                    case Line:
//                            lm.addActor((Actor) h, new Point2D.Double(h.getPoint().x - pt.x, 0));
                    case TwoRegion:

                        for (Actor a : actors) {

                            //need to return firm location relative to my location
                            //random random test
                            //reset random seed based on ID:
                            //A simple way to make People use the same subset of Firms each time

//                            Random rn = new Random(mea.ID);

//                            if (Randoms.nextBoolean()) {
//                            if (Randoms.nextDouble() > 0.3) {
//                            if (rn.nextDouble() > 0.7) {
                            if (true) {
                                lm.addActor(a, new Point2D.Double(a.getPoint().x - loc.x,
                                        a.getPoint().y - loc.y));
                            }

                        }
                        
                        break;

                    case Torus:

//                        Actor mea = (Actor) me;

                        for (Actor a : actors) {

                            //need to return firm location relative to my location
                            //random random test
                            //reset random seed based on ID:
                            //A simple way to make People use the same subset of Firms each time

//                            Random rn = new Random(mea.ID);

//                            if (Randoms.nextBoolean()) {
//                            if (Randoms.nextDouble() > 0.3) {
//                            if (rn.nextDouble() > 0.7) {
                            if (true) {
                                lm.addActor(a, new Point2D.Double(a.getPoint().x - loc.x,
                                        a.getPoint().y - loc.y));
                            }

                        }

                }//end switch

                return lm;

                //end if wrapspace
            } else {
                System.err.println("Trying to use wrapspace with peopleConsiderAll? Not coded yet.");
                System.exit(0);
            }

            //if not considering all
        } else {

            switch (gl.space) {

                case Point:
                    //Just need to return all appropriate Actors
                    for (HasLocation h : actors) {

                        lm.addActor((Actor) h, new Point2D.Double(0, 0));

                    }

                    break;// break point switch

                case TwoRegion:

                    //only need the one circle check
                    //So, set one circle where the point actually is...
                    rangeShape[0].setFrameFromCenter(pt.x, 0, pt.x - range, 0 - range);

                    //check who's in those circles...
                    for (HasLocation h : actors) {

                        //check I'm not adding myself
                        //check it's the type asked for
//                    if ((circles[0].contains(h.getPoint()) || gl.peopleConsiderAllFirms) && !h.equals(me)) {
                        if (rangeShape[0].contains(h.getPoint()) && !h.equals(me)) {

                            //pass in ref to actor
                            //and relative vector
                            //not messing with wraparound here so can use actor's loc directly
                            lm.addActor((Actor) h, new Point2D.Double(h.getPoint().x - pt.x, 0));

                        }

                    }//end foreach HasLocation

                    break;

                case Line:

                    pt = checkInTorus(pt);

                    setLineCircles();

                    //System.out.println("isInSpace line check, size of circle array: " + circles.length);

                    //check who's in those circles...
                    for (HasLocation h : actors) {

                        for (RectangularShape d : rangeShape) {

                            //check I'm not adding myself
                            //check it's the type asked for
//                        if ((d.contains(h.getPoint()) || gl.peopleConsiderAllFirms) && !h.equals(me)) {
                            if (d.contains(h.getPoint()) && !h.equals(me)) {

                                //pass in ref to actor
                                //and relative vector
                                //recall: if we use the centre of the circle doing the testing
                                //then the relative vector will be correct. Magic!
                                lm.addActor((Actor) h, new Point2D.Double(h.getPoint().x - d.getCenterX(), 0));
                            }
                        }//end foreach Ellipse
                    }//end foreach HasLocation

                    break;

                case Torus:

                    pt = checkInTorus(pt);

                    //Position testing shapes
                    setTorusShapes();

                    //check who's in those circles...
                    for (HasLocation h : actors) {

                        //System.out.println("whosinrange type: " + type + ", " + h.getClass().getSimpleName());

                        for (RectangularShape d : rangeShape) {

                            //check I'm not adding myself
                            //check it's the type asked for
//                        if ((d.contains(h.getPoint()) || gl.peopleConsiderAllFirms) && !h.equals(me)) {
                            if (d.contains(h.getPoint()) && !h.equals(me)) {

                                //pass in ref to actor
                                //and relative vector
                                //recall: if we use the centre of the circle doing the testing
                                //then the relative vector will be correct. Magic!
                                lm.addActor((Actor) h, new Point2D.Double(h.getPoint().x - d.getCenterX(),
                                        h.getPoint().y - d.getCenterY()));

                                //System.out.println("added firm...");
                            }
                        }//end foreach Ellipse
                    }//end foreach HasLocation
                    break; //end space switch

            }//end switch

        }//end else

        return lm;

    }

    //method used by isInRadius - putting outside cos it's messy
    private void setTorusShapes() {

        //First-off, move the circles to where they're supposed to be
//      How to creat the correct 4 circles:
//	Is querying point < ½ height?
//	Yes: set circles bottom
//	No: set circles top
//	Is querying point < ½ width?
//	Yes: set circles left
//	No: set circles right

        //At some point I'm going to make sure it's necessary to use all the circles in testing
        //for now, just getting up and working

        //So, set one circle where the point actually is...
        rangeShape[0].setFrameFromCenter(pt.x, pt.y, pt.x - range, pt.y - range);

        if (gl.wrapSpace) {

            //set the other three
            isInTop = (pt.y < width / 2) ? true : false;
            isInLeft = (pt.x < width / 2) ? true : false;

            //set main vertical circle
            //setFrameFromCentre: centerX,centerY,cornerX,cornerY
            if (isInTop) {
                //set direct bottom
                rangeShape[1].setFrameFromCenter(pt.x, (pt.y + width), pt.x - range, (pt.y + width) - range);
                //do horizontal check here...


                if (isInLeft) {
                    //set horizontal
                    rangeShape[2].setFrameFromCenter((pt.x + width), pt.y, (pt.x + width) - range, pt.y - range);

                    //now we also know to set diagonal opposite to top left
                    rangeShape[3].setFrameFromCenter((pt.x + width), (pt.y + width), (pt.x + width) - range, (pt.y + width) - range);



                } else {
                    rangeShape[2].setFrameFromCenter((pt.x - width), pt.y, (pt.x - width) - range, pt.y - range);

                    //now we also know to set diagonal opposite to top right
                    rangeShape[3].setFrameFromCenter((pt.x - width), (pt.y + width), (pt.x - width) - range, (pt.y + width) - range);



                } //else it's in bottom half...
            } else {
                //set direct top
                rangeShape[1].setFrameFromCenter(pt.x, (pt.y - width), pt.x - range, (pt.y - width) - range);

                //do horizontal check here...


                if (isInLeft) {
                    //set horizontal
                    rangeShape[2].setFrameFromCenter((pt.x + width), pt.y, (pt.x + width) - range, pt.y - range);

                    //now we also know to set diagonal opposite to bottom left
                    rangeShape[3].setFrameFromCenter((pt.x + width), (pt.y - width), (pt.x + width) - range, (pt.y - width) - range);



                } else {
                    rangeShape[2].setFrameFromCenter((pt.x - width), pt.y, (pt.x - width) - range, pt.y - range);

                    //now we also know to set diagonal opposite to bottom right
                    rangeShape[3].setFrameFromCenter((pt.x - width), (pt.y - width), (pt.x - width) - range, (pt.y - width) - range);



                }
            }//end else

        }//end if gl.wrapspace

        //test shapes:
//        System.out.println("");
//        System.out.println("Main loc: " + pt.x + "," + pt.y);
//
//        for (int i = 0; i < rangeShape.length; i++) {
//            System.out.println("centre: " + rangeShape[i].getCenterX() + "," + rangeShape[i].getCenterY());
//            System.out.println("size: " + rangeShape[i].getWidth() + "," + rangeShape[i].getHeight());
//        }
//
//        System.out.println("");

    }

    //method used by isInRadius for finding actors on the 1D racetrack model
    private void setLineCircles() {

        //At some point I'm going to make sure it's necessary to use all the circles in testing
        //for now, just getting up and working

        //So, set one circle where the point actually is...
        rangeShape[0].setFrameFromCenter(pt.x, 0, pt.x - range, 0 - range);

        //if not wrapping, only need the one circle
        if (gl.wrapSpace) {

            //set the other
            isInLeft = (pt.x < width / 2) ? true : false;

            //set main vertical circle
            //setFrameFromCentre: centerX,centerY,cornerX,cornerY
            if (isInLeft) {
                rangeShape[1].setFrameFromCenter((pt.x + width), 0, (pt.x + width) - range, 0 - range);
            } else {
                rangeShape[1].setFrameFromCenter((pt.x - width), 0, (pt.x - width) - range, 0 - range);
            }

        }//end if gl.wrapSpace


        //test circles:
//        System.out.println("");
//        System.out.println("Main loc: " + pt.x + "," + pt.y);
//
//        for (int i = 0; i < circles.length; i++) {
//            System.out.println("Circle: " + circles[i].getCenterX() + "," + circles[i].getCenterY() + "," + circles[i].getWidth());
//        }
//
//        System.out.println("");

    }

    /**
     * Make sure a point is within the space. If it isn't, shift it to the
     * correct position
     *
     * @param pt
     * @return
     */
    public Point2D.Double checkInTorus(Point2D.Double ch) {

        //p.a("Entered ch coords: " + ch.x + "," + ch.y);
        pt = (Point2D.Double) ch.clone();


        //if wrapping, send the point round
        if (gl.wrapSpace) {

//            System.out.println("wrapspace checkinz");

            //horizontal checks
            if (pt.x < 0) {
                //plus because already negative
                //p.p("Doing pt.x<0 ptange: " + pt.x);
                pt.x = gl.width + pt.x;
                //p.p("DONE ptx<0 ptange: " + pt.x);


            } else if (pt.x > gl.width) {
                pt.x -= gl.width;


            } //vertical checks - remember, space is square so width is both lengths
            if (pt.y < 0) {
                //plus because already negative
                //p.p("Doing pty<0 ptange: " + pt.y);
                pt.y = gl.width + pt.y;
                //p.p("DONE pty<0 ptange: " + pt.y);


            } else if (pt.y > gl.width) {
                pt.y -= gl.width;


            } //p.a("Retrned ch coords: " + ch.x + "," + ch.y);

            //if not wrapping: if the point was off the edge, set to the closest edge
        } else {

            //horizontal checks
            if (pt.x < 0) {
                pt.x = 0;
            } else if (pt.x > gl.width) {
//                System.out.println("here. pt.x = " + pt.x + ", width = " + gl.width);
                pt.x = gl.width;

            } //vertical checks
            if (pt.y < 0) {
                pt.y = 0;
            } else if (pt.y > gl.width) {
                pt.y = gl.width;

            }

        }

        return pt;

    }//end method checkInTorus

    /**
     * Return the nearest actor to me from an ArrayList of actors
     *
     * @param me
     * @param actors
     * @return
     */
    public static Actor whosNearest(Point2D.Double me, ArrayList<Actor> actors) {

        Actor ret = new Firm(0);
        double nearest = 9999;

        for (Actor a : actors) {

            if (me.distance(a.getPoint()) < nearest) {
                nearest = me.distance(a.getPoint());
                ret = a;
            }

        }

        return ret;

    }//end method whosNearest

    /*
     * findDensityCost. Bit of a mess, this. Since whosInRangeOfArray returns a
     * LocMemory currently having to work with that.
     */
    public static double findDensityCost(LocMemory dlm) {

        weight = 0;
        Actor ac;
        Point2D.Double zp = new Point2D.Double(0, 0);

        for (LocMemory.ActorNLocation a : dlm.actors) {

            //Logic here:
            //1. Weighting for each actor found in range: 1 for someone right on top of me,
            //Close to zero for one at the edge of the radius. Linear otherwise.
            weight += (1 - (zp.distance(a.p) / gl.DENSITYCOSTRADIUS));

            //check for low weight
            double lowWeightVal = 0.0001;

            if ((1 - (zp.distance(a.p) / gl.DENSITYCOSTRADIUS)) < lowWeightVal) {
//                System.out.println("Found weight < " + lowWeightVal + ", distance to actor as % of rad: " + (zp.distance(a.p) / gl.DENSITYCOSTRADIUS) 
//                        + ", weight given: " + (1 - (zp.distance(a.p) / gl.DENSITYCOSTRADIUS)));
            }

//            System.out.println("Distance to actor as % of rad: " + (zp.distance(a.p) / gl.DENSITYCOSTRADIUS) + ", weight given: " + (1 - (zp.distance(a.p) / gl.DENSITYCOSTRADIUS)));

        }

        //test just a straight actor-number-based density cost
//        weight = dlm.actors.size();

//        weight /= dlm.actors.size();
        weight /= gl.numTotalActors;

//        System.out.println("Final dc weight: " + weight + ", number of actors: " + dlm.actors.size());

        if (Double.isNaN(weight)) {
            return 0;
        } else {
            return weight;
        }

    }

    /*
     * Actor moving classes
     */

    /*
     * Stick em around a circle's perimeter (so for three actors, makes a
     * triangle)
     */
    public static void putActorsInACircle(ArrayList<Actor> actors, double radius) {

        double circlePoint = 0;
        //Number of steps round circle, one for each actor
        double stepsize = (2 * Math.PI / actors.size());

        for (Actor a : actors) {

            a.setPoint(new Point2D.Double(((double) width / 2)
                    + (radius * Math.cos(circlePoint)), ((double) width / 2) + (radius * Math.sin(circlePoint))));

            //move round circle
            circlePoint += stepsize;

        }

    }
    /*
     * Stick em around a circle's perimeter (so for three actors, makes a
     * triangle) Position described by an x,y of 0 to 1, so proportional to
     * width.
     */

    public static void putActorsInACircleWithCentreAt(ArrayList<Actor> actors, double radius, double xpos, double ypos) {

        double circlePoint = 0;
        //Number of steps round circle, one for each actor
        double stepsize = (2 * Math.PI / actors.size());

        for (Actor a : actors) {

            a.setPoint(new Point2D.Double(((double) width * xpos)
                    + (radius * Math.cos(circlePoint)), ((double) width * ypos) + (radius * Math.sin(circlePoint))));

            //move round circle
            circlePoint += stepsize;

        }

    }

    /*
     * stick em randomly in the area of a circle: radius and angle method, which
     * automatically weights density towards centre
     */
    public static void putActorsInCircleRandomRadiusAndAngle(ArrayList<Actor> actors, double radius) {

        for (Actor a : actors) {

            //set actor to centre
            a.setPoint(new Point2D.Double((double) width / 2, (double) width / 2));
            //randomly place around that centre
            Trig.moveToRandPointInCircleOfRadius(a.getPoint(), radius);

        }

    }

    /**
     * Truly random positioning of actors in a circle, no default weighting to
     * centre
     *
     * @param actors
     * @param radius
     */
    public static void putActorsInsideCircleAreaRandomly(ArrayList<Actor> actors, double radius) {

        //Use an ellipse to make sure random siting is within circle
//        Ellipse2D.Double d = new Ellipse2D.Double(width / 2, width / 2, radius * 2, radius * 2);
        Ellipse2D.Double cd = new Ellipse2D.Double();
        //"Sets the framing rectangle of this Shape based on the specified center point coordinates and corner point coordinates"
        cd.setFrameFromCenter(width / 2, width / 2, (width / 2) + radius, (width / 2) + radius);

        for (Actor a : actors) {

            //randomise location within a square of the right dimensions; check it's in the circle; if not, repeat.
            do {
                a.setPoint(new Point2D.Double(((double) width / 2) + ((Randoms.nextDouble() - 0.5) * radius * 2),
                        ((double) width / 2) + ((Randoms.nextDouble() - 0.5) * radius * 2)));
            } while (!cd.contains(a.getPoint()));

        }

    }//end method putActorsInCircleRandomly

    public static void putActorsOnAGrid(ArrayList<Actor> actors) {

        if (gl.space == gl.SpaceType.Torus) {

            System.out.println("trying to set landlord location on grid. Size of actor array: " + actors.size());

            for (Actor a : actors) {

                for (int i = 0; i < Math.sqrt(actors.size()); i++) {

                    for (int j = 0; j < Math.sqrt(actors.size()); j++) {

                        a.setPoint(new Point2D.Double((double) i * (width / Math.sqrt(actors.size())), (double) j * (width / Math.sqrt(actors.size()))));

                    }

                }

            }
        } else {
            System.out.println("Tried to set actors on a grid in a non-2D space.");
            System.exit(0);
        }


    }

    /*
     * Stick em in a vertical line
     */
    public static void putActorsInAVerticalLine(ArrayList<Actor> actors) {

        //check correct space
        if (gl.space == gl.SpaceType.Torus) {

            double stepsize = (double) width / (actors.size() + 2);
            double loc = 0;

            for (Actor a : actors) {
                loc += stepsize;
                a.setPoint(new Point2D.Double((double) width / 2, loc));
            }

        } else {
            System.out.println("Tried to set actors in a vertical line in a space with no vertical. Silly person.");
            System.exit(0);
        }

    }

    /**
     * Stick em in a horizontal line
     *
     * @param actors
     */
    public static void putActorsInAHorizontalLine(ArrayList<Actor> actors) {


        //two actors splits the line into 3; 3 into 4, etc
        double stepsize = (double) width / (actors.size() + 1);
        //start one actor in: this is added below before setting
        double loc = 0;

        //if we're using line-space, the y axis is on zero...
        if (gl.space == gl.SpaceType.Torus) {

            for (Actor a : actors) {
                loc += stepsize;
                a.setPoint(new Point2D.Double(loc, (double) width / 2));
            }
        } else if (gl.space == gl.SpaceType.Line) {
            for (Actor a : actors) {
                loc += stepsize;
                a.setPoint(new Point2D.Double(loc, 0));
            }

        } else {
            System.out.println("Tried to set actors in a horizontal line in an inappropriate space. Very silly person.");
            System.exit(0);
        }

    }//end method putActorsInAHorizontalLine

    /**
     * Stick em in a horizontal line
     *
     * @param actors
     */
    public static void putActorsInAHorizontalLineVerticalPos(ArrayList<Actor> actors, double vp) {


        //two actors splits the line into 3; 3 into 4, etc
        double stepsize = (double) width / (actors.size() + 1);
        //start one actor in: this is added below before setting
        double loc = 0;

        //if we're using line-space, the y axis is on zero...
        if (gl.space == gl.SpaceType.Torus) {

            for (Actor a : actors) {
                loc += stepsize;
                a.setPoint(new Point2D.Double(loc, (double) width * vp));
            }
        } else if (gl.space == gl.SpaceType.Line) {
            for (Actor a : actors) {
                loc += stepsize;
                a.setPoint(new Point2D.Double(loc, 0));
            }

        } else {
            System.out.println("Tried to set actors in a horizontal line in an inappropriate space. Very silly person.");
            System.exit(0);
        }

    }//end method putActorsInAHorizontalLine

    /**
     * Stick em in a horizontal line
     *
     * @param actors
     * @param start: start point, 0<start<1, proportional to width
     * @param end: end point, 0<end<1, proportional to width
     */
    public static void putActorsInAHorizontalLineStartEnd(ArrayList<Actor> actors, double start, double end) {


        //two actors splits the line into 1; 2 into 3, etc
        double stepsize = (double) (width * (end - start)) / (actors.size() - 1);
        //start one actor in: this is added below before setting
        double loc = start * width;

        //if we're using line-space, the y axis is on zero...
        if (gl.space == gl.SpaceType.Torus) {

            for (Actor a : actors) {
                a.setPoint(new Point2D.Double(loc, (double) width / 2));
                loc += stepsize;
            }
        } else if (gl.space == gl.SpaceType.Line) {
            for (Actor a : actors) {
                a.setPoint(new Point2D.Double(loc, 0));
                loc += stepsize;
                
//                System.out.println("set actor to line: " + a.getx() + "," + a.gety());
                
            }

        } else {
            System.out.println("Tried to set actors in a horizontal line in an inappropriate space. Very silly person.");
            System.exit(0);
        }

    }//end method putActorsInAHorizontalLine

    /**
     * splitActorsBetweenTwo1DPoints: toggles between actors, placing them at
     * point1 and point2 in turn Assumes 1D model
     *
     * @param actors
     * @param point1
     * @param point2
     */
    public static void splitActorsBetweenTwo1DPoints(ArrayList<Actor> actors, double point1, double point2) {

        boolean toggle = false;

        for (Actor a : actors) {
            if (toggle) {
                a.setPoint(new Point2D.Double(point1, 0));
                toggle = false;
            } else {
                a.setPoint(new Point2D.Double(point2, 0));
                toggle = true;
            }
        }

    }//end method splitActorsBetweenTwo1DPoints

    /**
     * Stick em in a horizontal line
     *
     * @param actors
     * @param start: start point, 0<start<1, proportional to width
     * @param end: end point, 0<end<1, proportional to width
     *
     * global actor array used
     *
     */
    public static void putActorsInAHorizontalLineStartEnd(double start, double end) {


        //two actors splits the line into 1; 2 into 3, etc
        double stepsize = (double) (width * (end - start)) / (gl.allActors.size() - 1);
        //start one actor in: this is added below before setting
        double loc = start * width;

        //if we're using line-space, the y axis is on zero...
        if (gl.space == gl.SpaceType.Torus) {

            for (Actor a : gl.allActors) {
                a.setPoint(new Point2D.Double(loc, (double) width / 2));
                loc += stepsize;
            }
        } else if (gl.space == gl.SpaceType.Line) {
            for (Actor a : gl.allActors) {
                a.setPoint(new Point2D.Double(loc, 0));
                loc += stepsize;
            }

        } else {
            System.out.println("Tried to set actors in a horizontal line in an inappropriate space. Very silly person.");
            System.exit(0);
        }

    }//end method putActorsInAHorizontalLine

    public static void putActorsInAHorizontalLineStartEndVertical(ArrayList<Actor> actors, double start, double end, double vertical) {


        //two actors splits the line into 3; 3 into 4, etc
        double stepsize = (double) (width * (end - start)) / (actors.size() + 1);
        //start one actor in: this is added below before setting
        double loc = 0;

        //if we're using line-space, the y axis is on zero...
        if (gl.space == gl.SpaceType.Torus) {

            for (Actor a : actors) {
                loc += stepsize;
                a.setPoint(new Point2D.Double((width * start) + loc, (double) width * vertical));
            }
        } else if (gl.space == gl.SpaceType.Line) {
            for (Actor a : actors) {
                loc += stepsize;
                a.setPoint(new Point2D.Double((width * start) + loc, 0));
            }

        } else {
            System.out.println("Tried to set actors in a horizontal line in an inappropriate space. Very silly person.");
            System.exit(0);
        }

    }//end method putActorsInAHorizontalLine

    /**
     * Set actors' location to a single point
     *
     * @param actors
     * @param pt
     */
    public static void putActorsOnThisPoint(ArrayList<Actor> actors, Point2D.Double pt) {

        for (Actor a : actors) {
            a.setx(pt.x);
            a.sety(pt.y);
        }

    }//end method putActorsOnThisPoint

    /**
     * Set actors' location to a single point (double x,y version)
     *
     * @param actors
     * @param pt
     */
    public static void putActorsOnThisPoint(ArrayList<Actor> actors, double x, double y) {

        for (Actor a : actors) {
            a.setx(x);
            a.sety(y);
        }

    }//end method putActorsOnThisPoint

    /*
     * Drawing 2D version of SpaceTools visualisation
     */
    public void draw2D(PApplet pa) {

        //so other internal methods can use it easily
        this.pa = pa;

        if (gl.space == gl.SpaceType.Line) {
            draw2DLine();
        } else if (gl.space == gl.SpaceType.Torus) {
            draw2DTorus();
        }

    }

    private void draw2DLine() {
    }

    private void draw2DTorus() {

        Person p;

        //reset vars
        findLowUtil = 9999;
        findHighUtil = -1;
        findLowAmount = 9999;
        findHighAmount = -1;

        for (HasLocation h : isInSpace) {

            Actor i = (Actor) h;

            if (i.getClass().getSimpleName().equals("Firm")) {

                pa.stroke(i.colour.getRGB());

                pa.rect((float) (i.getx() * (pa.width / width)),
                        (float) (i.gety() * (pa.height / width)), (float) 5, (float) 5);

            } else {

                PersonAction pac = (PersonAction) i.actions.get(0);

                //draw densitycost circle
                pa.stroke(100, 100);
                pa.ellipse((float) (i.getx() * (pa.width / width)),
                        (float) (i.gety() * (pa.height / width)), (float) gl.DENSITYCOSTRADIUS * 2 * (pa.width / width), (float) gl.DENSITYCOSTRADIUS * 2 * (pa.width / width));

                if (pac.bestBundle.employer == null) {
                    pa.stroke(255);
                } else {
                    pa.stroke(0, 255, 0, 100);
                }

                pa.rect((float) (i.getx() * (pa.width / width)),
                        (float) (i.gety() * (pa.height / width)), (float) 5, (float) 5);

                //If you don't use this, the initialBundles sometimes get thrown away between translates...
                double maxU = pac.bestBundle.maxUtility;

                if (gl.singleActor) {
                    SingleActorAction sl = (SingleActorAction) i.actions.get(0);
//                    maxU = sl.bestBundle.maxUtility;
                    maxU = sl.me.currentContributedTime;
                }


//                System.out.println("bestBundle utility in space: " + maxU);

                //record highest, lowest utility, use next time
                if (maxU > findHighUtil) {
                    findHighUtil = maxU;
                }
                if (maxU < findLowUtil) {
                    findLowUtil = maxU;
                }



                //translate highest, lowest utility values to 0-255 range
                if (gl.visUtilityRangeOn) {
                    float colVal = (float) (((maxU - useLowUtil) / (useHighUtil - useLowUtil)) * 255f);
                    //System.out.println(colVal);
                    pa.strokeWeight(1);
                    pa.stroke(colVal, 255 - colVal, 0, 200);
                } else {
                    pa.stroke(0, 255, 0, 200);
                }

                //if von Thunen style model connecting to single Firm, use Firm's colour instead
                if (gl.colourByFirm && (gl.vonThunenGoods || gl.vonThunenWage)) {

                    p = (Person) i;
                    pa.stroke(p.vonThunenFirm.colour.getRGB(), 200);

                }

                //hacking colour test
//                if (gl.singleActor) {
//                    SingleActorAction sl = (SingleActorAction) i.actions.get(0);
//                    pa.stroke((int) (sl.me.deliverycost * 620), 0, 0, 150);
//                }


                //factor to multiply utility by - will need to change depending on vis
                float utilMult = gl.visFactorUtility;

                //if using topView, don't raise their height.

                if (!gl.topView) {
                    pa.translate(0f, 0f, (float) (maxU) * utilMult);
//                    pa.translate(0f, 0f, (float) (pac.bestBundle.maxUtility) * utilMult);
                }
                //System.out.println("pac.bestBundle.maxUtility" + pac.bestBundle.maxUtility + "pac.bestBundle.maxUtility  * 3f: " + pac.bestBundle.maxUtility * 3f);

                pa.rect((float) (i.getx() * (pa.width / width)),
                        (float) (i.gety() * (pa.height / width)),
                        (float) (maxU) * (utilMult / 5), (float) (maxU) * (utilMult / 5));


            }//end else person

        }



    }//end method draw2DTorus

    /**
     * Draw stuff for the space...
     */
    public void draw3D(PApplet pa) {

        //reset vars
        findLowUtil = 9999;
        findHighUtil = -1;
        findLowAmount = 9999;
        findHighAmount = -1;

        Person p;

        //so other internal methods can use it easily
        this.pa = pa;

        pa.noFill();

        if (gl.space == gl.SpaceType.Point) {
            //if point-space, hive drawing off to a different method
            drawPointModel();
            return;
        } else if (gl.space == gl.SpaceType.Line) {
            //if drawing the racetrack, move the line down to the centre
            pa.translate(0, (float) pa.width / 2, 0);
        }



        for (HasLocation h : isInSpace) {

            Actor i = (Actor) h;
            pa.strokeWeight(2);



            if (i.getClass().getSimpleName().equals("Firm")) {

                Firm fm = (Firm) i;

//                if (fm.goodType == gl.GoodType.Luxury) {
//                    pa.stroke(255, 0, 0);
//
//
//                } else {
//                    pa.stroke(0, 0, 255);
//
//                }
                pa.stroke(i.colour.getRGB());


                //square in proportion to time input

                //pa.translate(0f, 0f, (float) fm.currentContributedTime * 3);

                pa.rect((float) (i.getx() * (pa.width / width)),
                        (float) (i.gety() * (pa.height / width)), (float) 5, (float) 5);

                //circle, radius 1
                if (gl.drawFirmOneDayCircles) {
                    pa.stroke(255, 20);
                    pa.ellipse((float) i.getx() * (pa.width / width),
                            (float) i.gety() * (pa.height / width),
                            1f * (pa.width / width),
                            1f * (pa.height / width));
                }


                //pa.translate(0f, 0f, -(float) fm.currentContributedTime * 3);

                //stick a line between them
                //pa.stroke(255, 0, 0);
//
//                pa.line((float) (i.getx() * (pa.width / width)),
//                        (float) (i.gety() * (pa.height / width)), 0,
//                        (float) (i.getx() * (pa.width / width)),
//                        (float) (i.gety() * (pa.height / width)), (float) fm.currentContributedTime * 3);


                /*
                 * Set up for drawing racetrack vars
                 */
                if (gl.space == gl.SpaceType.Line && gl.lineViewVars) {

                    //money, revenue, costs, goodstock, price, wageoffer
                    String[] varnames = {"m", "r", "c", "g", "p", "w"};

                    double[] values = {fm.money / 10, fm.revenue * 10, fm.costs * 10, fm.getGoodStock() / 10, fm.goodCost * 40, fm.wage * 40};

                    boolean[] draw = {true, true, true, fm.shiftedStock, fm.shiftedStock, fm.hiredLabour};

                    Color[] cols = {new Color(255, 0, 0), new Color(155, 0, 0), new Color(155, 0, 0),
                        new Color(0, 255, 0), new Color(0, 255, 255), new Color(50, 240, 50), new Color(40, 200, 240), new Color(0, 0, 255)};

                    for (int dr = 0; dr < varnames.length; dr++) {
                        drawVar(varnames[dr], (dr + 1) * 40, (float) i.getx(), (float) values[dr], cols[dr], draw[dr]);
                    }

                }

                /*
                 * End drawing vars
                 */


            } else if (i.getClass().getSimpleName().equals("Person")
                    || i.getClass().getSimpleName().equals("SingleActorPerson")) {
                pa.strokeWeight(1);

                PersonAction pac = (PersonAction) i.actions.get(0);




                //draw densitycost circle
                pa.stroke(100, 100);
                pa.ellipse((float) (i.getx() * (pa.width / width)),
                        (float) (i.gety() * (pa.height / width)), (float) gl.DENSITYCOSTRADIUS * 2 * (pa.width / width), (float) gl.DENSITYCOSTRADIUS * 2 * (pa.width / width));

                if (pac.bestBundle.employer == null) {
                    pa.stroke(255);
                } else {
                    pa.stroke(0, 255, 0, 100);
                }
                //ground square; don't draw3D if viewing from above
//                if (!gl.topView) {
                pa.rect((float) (i.getx() * (pa.width / width)),
                        (float) (i.gety() * (pa.height / width)), (float) 5, (float) 5);
//                }

                //If you don't use this, the initialBundles sometimes get thrown away between translates...

                double maxU = pac.bestBundle.maxUtility;

                if (gl.singleActor) {
                    SingleActorAction sl = (SingleActorAction) i.actions.get(0);
//                    maxU = sl.bestBundle.maxUtility;
                    maxU = sl.me.currentContributedTime;
                }

//                System.out.println("bestBundle utility in space: " + maxU);

                //record highest, lowest utility, use next time
                if (maxU > findHighUtil) {
                    findHighUtil = maxU;
                }
                if (maxU < findLowUtil) {
                    findLowUtil = maxU;
                }



                //translate highest, lowest utility values to 0-255 range
                if (gl.visUtilityRangeOn) {
                    float colVal = (float) (((maxU - useLowUtil) / (useHighUtil - useLowUtil)) * 255f);
                    //System.out.println(colVal);
                    pa.strokeWeight(1);
                    pa.stroke(colVal, 255 - colVal, 0, 200);
                } else {
                    pa.stroke(0, 255, 0, 200);
                }

                //if von Thunen style model connecting to single Firm, use Firm's colour instead
                if (gl.colourByFirm && (gl.vonThunenGoods || gl.vonThunenWage)) {

                    p = (Person) i;
                    pa.stroke(p.vonThunenFirm.colour.getRGB(), 200);

                }

                //hacking colour test
//                if (gl.singleActor) {
//                    SingleActorAction sl = (SingleActorAction) i.actions.get(0);
//                    pa.stroke((int) (sl.me.deliverycost * 620), 0, 0, 150);
//                }


                //factor to multiply utility by - will need to change depending on vis
                float utilMult = gl.visFactorUtility;

                //if using topView, don't raise their height.

                if (!gl.topView) {
                    pa.translate(0f, 0f, (float) (maxU) * utilMult);
//                    pa.translate(0f, 0f, (float) (pac.bestBundle.maxUtility) * utilMult);
                }
                //System.out.println("pac.bestBundle.maxUtility" + pac.bestBundle.maxUtility + "pac.bestBundle.maxUtility  * 3f: " + pac.bestBundle.maxUtility * 3f);

                pa.rect((float) (i.getx() * (pa.width / width)),
                        (float) (i.gety() * (pa.height / width)), (float) (maxU) * (utilMult / 5), (float) (maxU) * (utilMult / 5));
//
                if (!gl.topView) {
                    pa.translate(0f, 0f, -(float) (maxU) * utilMult);
                }
                //System.out.println("Maxutility in draw3D: " + pac.bestBundle.maxUtility);

                //draw line
                if (!gl.topView) {
                    pa.stroke(200, 100);
                    //pa.strokeWeight(4);
//
                    pa.line((float) (i.getx() * (pa.width / width)),
                            (float) (i.gety() * (pa.height / width)), 0,
                            (float) (i.getx() * (pa.width / width)),
                            (float) (i.gety() * (pa.height / width)), (float) (maxU) * utilMult);

                }


                //should be able to use bestBundle to draw3D lines between me and firms I bought from
                //indicating quantities

                //System.out.println("size of bestB goodslist: " + pac.bestBundle.GoodsList.size());



                //drawing only one person's trade lines
                //if (gl.LOOKATME == pac.me.ID) {

//                if (pac.bestBundle.GoodsList.size() > 0) {
//                    System.out.println("size of goods list: " + pac.bestBundle.GoodsList.size());
//                }




                for (Good g : pac.bestBundle.GoodsList) {

                    Actor f = (Actor) g.gs;

                    //Make line more solid the higher the quantity... in a minute, let's just get lines working firs
                    //Note: this won't work with buyers across the space. Will have to write a specific method to deal
                    //with that.

//                        System.out.println("optimch: " + g.optimalChosenAmount);
                    //Start line at me, draw3D to firm
                    if (g.optimalChosenAmount > 0) {

                        //System.out.println("g.opt  = " + g.optimalChosenAmount);

                        //record highest, lowest goodAmount, use next time
//                        if (g.chosenAmount > findHighAmount) {
//                            findHighAmount = g.chosenAmount;
//                        }
//                        if (g.chosenAmount < findLowAmount) {
//                            findLowAmount = g.chosenAmount;
//                        }
//
//                        //double chmMult = 600;
//                        colVal = (float) ((useHighAmount - useLowAmount - g.chosenAmount) / (useHighAmount - useLowAmount) * 255f);
//
//                        System.out.println("useHighAmount: " + useHighAmount + ", useLowAm: " + useLowAmount + ", g.chosenAmount: " + g.chosenAmount);
//
//                        if (colVal < 0) {
//                            colVal = 0;
//                        }

                        //they're mostly high so try halving it. Really, I could do with a standard deviation, but not right now!
                        //colVal -= 125;

                        float chmMult = gl.visFactorBuyLines;

                        //System.out.println(colVal);

//                            if (g.f.goodType == gl.GoodType.Luxury) {
//                                pa.stroke(255, 50, 50, 100 + (float) (g.chosenAmount * 80));
//                            } else {
//                                pa.stroke(50, 50, 255, 100 + (float) (g.chosenAmount * 80));
//                            }

                        //pa.strokeWeight(colVal / 100);
                        pa.strokeWeight((float) (g.optimalChosenAmount * (chmMult) / 50));


//                        if (g.gs.getGoodType() == gl.GoodType.Luxury) {
//                            //pa.stroke(255, 50, 50, (float) (g.chosenAmount * chmMult));
//                            //pa.stroke((float) 255, 50, 50, (float) (g.chosenAmount * chmMult));
////                            pa.stroke((float) (g.optimalChosenAmount * chmMult), 50, 50, (float) (g.optimalChosenAmount * chmMult));
//                            //pa.stroke(colVal, 50, 50, colVal);
//                        } else {
//                            //pa.stroke(50, 50, 255, (float) (g.chosenAmount * chmMult));
//                            //pa.stroke(50, 50, 255, (float) (g.chosenAmount * chmMult));
////                            pa.stroke(50, 50, (float) (g.optimalChosenAmount * chmMult), (float) (g.optimalChosenAmount * chmMult));
//                            //pa.stroke(50, 50, colVal, colVal);
//
//                        }
                        float alpha = (float) (g.optimalChosenAmount * chmMult > 255 ? 255 : g.optimalChosenAmount * chmMult);

                        if (!gl.singleActor) {
                            Firm fi = (Firm) g.gs;
                            //note: this is, I think, where all the crazy random colours come in, so may want to keep when I come back
                            //to doing the arty stuff!
                            //float alpha = (float) (g.optimalChosenAmount * chmMult > 255 ? 255 : g.optimalChosenAmount * chmMult);
                            pa.stroke(fi.colour.getRGB(), alpha);
                        } else {
                            pa.stroke(255, alpha);
                        }


//                        curve if on line
                        if (gl.space == gl.SpaceType.Torus) {

                            pa.line((float) (i.getx() * (pa.width / width)),
                                    (float) (i.gety() * (pa.height / width)), 0,
                                    (float) (f.getx() * (pa.width / width)),
                                    (float) (f.gety() * (pa.height / width)), 0);

                            if (gl.reliefLines) {

                                float midx = ((float) Math.abs((f.getx() - i.getx())) / 2);
                                float midy = ((float) Math.abs((f.gety() - i.gety())) / 2);

                                pa.curve((float) (i.getx() + midx) * (pa.width / width), (float) (i.gety() + midy) * (pa.width / width), -(float) (g.optimalChosenAmount * chmMult) * 3,
                                        (float) (i.getx() * (pa.width / width)), (float) (i.gety() * (pa.height / width)), 0,
                                        (float) (f.getx() * (pa.width / width)), (float) (f.gety() * (pa.height / width)), 0,
                                        (float) (i.getx() + midx) * (pa.width / width), (float) (i.gety() + midy) * (pa.width / width), -(float) (g.optimalChosenAmount * chmMult) * 3);

                            }


                        } else if (gl.space == gl.SpaceType.Line) {

                            float midPoint = 0;
                            midPoint = ((float) Math.abs((f.getx() - i.getx())) / 2);

                            if (f.getx() > i.getx()) {



                                //put on z axis if side view
                                if (gl.topView) {
                                    pa.curve((float) (i.getx() + midPoint) * (pa.width / width), (float) (g.optimalChosenAmount * chmMult) * 3, (float) (i.getx() * (pa.width / width)),
                                            (float) (i.gety() * (pa.height / width)), (float) (f.getx() * (pa.width / width)),
                                            (float) (f.gety() * (pa.height / width)), (float) (i.getx() + midPoint) * (pa.width / width), (float) (g.optimalChosenAmount * chmMult * 3));
                                } else {
                                    pa.curve((float) (i.getx() + midPoint) * (pa.width / width), -(float) (g.optimalChosenAmount * chmMult) * 3, 0,
                                            (float) (i.getx() * (pa.width / width)), (float) (i.gety() * (pa.height / width)), 0,
                                            (float) (f.getx() * (pa.width / width)), (float) (f.gety() * (pa.height / width)), 0,
                                            (float) (i.getx() + midPoint) * (pa.width / width), -(float) (g.optimalChosenAmount * chmMult * 3), 0);
//                                pa.curve((float) (i.getx() + midPoint) * (pa.width / width), 0, -(float) (g.optimalChosenAmount * chmMult) * 3,
//                                        (float) (i.getx() * (pa.width / width)), (float) (i.gety() * (pa.height / width)), 0,
//                                        (float) (f.getx() * (pa.width / width)), (float) (f.gety() * (pa.height / width)), 0,
//                                        (float) (i.getx() + midPoint) * (pa.width / width), 0, -(float) (g.optimalChosenAmount * chmMult * 3));

                                }
//
                            } else {

                                if (gl.topView) {
                                    pa.curve((float) (f.getx() + midPoint) * (pa.width / width), (float) (g.optimalChosenAmount * chmMult) * 3, (float) (i.getx() * (pa.width / width)),
                                            (float) (i.gety() * (pa.height / width)), (float) (f.getx() * (pa.width / width)),
                                            (float) (f.gety() * (pa.height / width)), (float) (f.getx() + midPoint) * (pa.width / width), (float) (g.optimalChosenAmount * chmMult * 3));
                                } else {

//                               
                                    pa.curve((float) (f.getx() + midPoint) * (pa.width / width), 0, -(float) (g.optimalChosenAmount * chmMult) * 3,
                                            (float) (i.getx() * (pa.width / width)), (float) (i.gety() * (pa.height / width)), 0,
                                            (float) (f.getx() * (pa.width / width)), (float) (f.gety() * (pa.height / width)), 0,
                                            (float) (f.getx() + midPoint) * (pa.width / width), 0, -(float) (g.optimalChosenAmount * chmMult * 3));
//                                pa.curve((float) (f.getx() + midPoint) * (pa.width / width), 0, -(float) (g.optimalChosenAmount * chmMult) * 3,
//                                        (float) (i.getx() * (pa.width / width)),(float) (i.gety() * (pa.height / width)), 0,
//                                        (float) (f.getx() * (pa.width / width)),(float) (f.gety() * (pa.height / width)), 0,
//                                        (float) (f.getx() + midPoint) * (pa.width / width), 0, -(float) (g.optimalChosenAmount * chmMult * 3));
                                }

                            }
                            //System.out.println("Firm: " + g.f.getx() + ", person: " + i.getx() + ", Midpoint:" + midPoint);



                        }

                    }


                }//end for good...

                //}//end if LOOKATME

                pa.strokeWeight(1);



            }//end actor-type if
            //otherwise it's going to be a landlord agent. Mark price with circle
            else {

                GoodSeller gs;

                if (gl.space == gl.SpaceType.Line) {

                    pa.translate(0, 20);
                    for (Actor a : gl.landLords) {
                        gs = (GoodSeller) a;
                        pa.stroke(100, 100, 100, 100);
                        pa.ellipse((float) a.getx() * (pa.width / width), (float) a.gety() * (pa.width / width), (float) gs.getGoodCost() * 50, (float) gs.getGoodCost() * 50);
                        //System.out.println("in space: landlords xy: " + (float) a.getx() + "," + (float) a.gety());
                    }
                    pa.translate(0, -20);

                } else if (gl.space == gl.SpaceType.Torus) {
                    //they're on a grid.
                    for (Actor a : gl.landLords) {
                        gs = (GoodSeller) a;
                        pa.stroke(100, 100, 100, 100);
                        pa.ellipse((float) a.getx() * (pa.width / width), (float) a.gety() * (pa.width / width), (float) gs.getGoodCost() * 50, (float) gs.getGoodCost() * 50);
                        //System.out.println("in space: landlords xy: " + (float) a.getx() + "," + (float) a.gety());
                    }

                }

            }//end landlord agent draw3D.



            pa.stroke(0);

            /*
             * LOOKATME CIRCLE
             */
            //1 day's radius
            //or, if I'm testing changing space costs... oh yes, still 1 day's radius!
            //But might change
//            pa.ellipse(((float) isInSpace.get(gl.LOOKATME).getx()) * (pa.width / width),
//                    ((float) isInSpace.get(gl.LOOKATME).gety()) * (pa.height / width),
//                    (2 * (float) gl.SPACECOST) * (pa.width / width),
//                    (2 * (float) gl.SPACECOST) * (pa.height / width));
//
//            //and mark the actor it's supposed to relate to...
//            pa.strokeWeight(5);
//
//            pa.translate(0f, 0f, 10f);
//            pa.rect((float) (isInSpace.get(gl.LOOKATME).getx() * (pa.width / width)),
//                    (float) (isInSpace.get(gl.LOOKATME).gety() * (pa.height / width)), (float) 5, (float) 5);
//            pa.translate(0f, 0f, -10f);

            //p.a(" -> " + ((float) isInTorus.get(lookatme).h.getx() - 1) + ", " + ((float) isInTorus.get(lookatme).h.gety() - 1));



        }//for HasLocation isInSpace

//        drawMeanCentrePoint();

        //draw an agent's detected actors to check location...
        pa.strokeWeight(3);

        //note: if this picks up a firm, or some actor that doesn't have a populated LocMemory
        //you'll get a nullpointerexception

////        so let's make sure I'm getting a person
//        Actor dr = (Actor) isInSpace.get(gl.LOOKATME);
////
//        for (ActorNLocation anl : dr.lm.actors) {
//
////            if (anl.actor.getClass().getSimpleName().equals("Person")) {
////
////                pa.stroke(100, 0, 110);
////
////
////
////            } else {
//            pa.stroke(255, 30, 255);
//
//
//            //} //Draw the found objects, relative to the location of me
//            pa.rect((float) ((dr.getx() + anl.p.x) * (pa.width / width)),
//                    (float) ((dr.gety() + anl.p.y) * (pa.height / width)), (float) 10, (float) 10);
//
//            //System.out.println("Loc of drawn actor: " + dr.getx() + "," + dr.gety());
//
//        }//end for ActorNLocation

        //set col vals for next time
        if (findLowUtil > 0) {
            useHighUtil = findHighUtil;
            useLowUtil = findLowUtil;
        }
//        System.out.println("low high utils found: " + useLowUtil + "," + useHighUtil);
        useHighAmount = findHighAmount;
        useLowAmount = findLowAmount;

    }//end method draw3D

    private void drawMeanCentrePoint() {

        //draw mean centrepoint
        float mx = 0, my = 0;
        for (HasLocation h : isInSpace) {

            mx += h.getx();
            my += h.gety();

        }

        mx /= isInSpace.size();
        my /= isInSpace.size();

//        System.out.println("mx, my: " + mx + "," + my);

        pa.translate(0, 0, 20);

        pa.fill(255);
        pa.ellipse(mx * (pa.width / width), my * (pa.width / width), 10, 10);
        pa.noFill();
        pa.stroke(255);
        pa.ellipse(mx * (pa.width / width), my * (pa.width / width), 15, 15);
        //end draw3D mean centrepoint
        pa.translate(0, 0, -20);

    }

    //Harvest off for drawing point-space model
    public void drawPointModel() {

        int counter = -1;
        int pcounter = -1, fcounter = 0;

        pa.noFill();


        for (HasLocation h : isInSpace) {


            counter++;

            Actor i = (Actor) h;
            pa.strokeWeight(2);

            if (i.getClass().getSimpleName().equals("Firm")) {

                fcounter++;

                //move the line down to the centre
                pa.translate(0, (float) pa.width / 2, 0);

                //fcounter++;

                Firm fm = (Firm) i;

                if (fm.makingGood1) {
                    pa.stroke(255, 0, 0);


                } else {
                    pa.stroke(0, 0, 255);


                }

                //square in proportion to time input

                //one on ground position
                pa.rect((float) fcounter / (gl.numFirms * 2) * width * (pa.width / width), 0, (float) 5, (float) 5);

                pa.translate(0f, 0f, (float) fm.currentContributedTime * 3);

                //one reflecting contributed time
                pa.rect((float) fcounter / (gl.numFirms * 2) * width * (pa.width / width), 0, (float) 5, (float) 5);

//                System.out.println((float) ((float) fcounter/(gl.numFirms)) * width * (pa.width/width));
//                System.out.println("numfirms: " + gl.numFirms);

                pa.translate(0f, 0f, -(float) fm.currentContributedTime * 3);

                //stick a line between them
                pa.stroke(255, 0, 0);
//
                pa.line((float) fcounter / (gl.numFirms * 2) * width * (pa.width / width),
                        0, 0,
                        (float) fcounter / (gl.numFirms * 2) * width * (pa.width / width),
                        0, (float) fm.currentContributedTime * 3);

                /*
                 * Set up for drawing vars
                 */

                String[] varnames = {"m", "g", "p", "w"};

                double[] values = {fm.money / 10, fm.getGoodStock() / 10, fm.goodCost * 40, fm.wage * 40};

                boolean[] draw = {true, fm.shiftedStock, fm.shiftedStock, fm.hiredLabour};

                Color[] cols = {new Color(255, 0, 0), new Color(0, 255, 0), new Color(0, 255, 255), new Color(50, 240, 50), new Color(40, 200, 240), new Color(0, 0, 255)};

                for (int dr = 0; dr < varnames.length; dr++) {

                    drawVar(varnames[dr], (dr + 1) * 40, (float) fcounter / (gl.numFirms * 2) * width, (float) values[dr], cols[dr], draw[dr]);

                }

                /*
                 * End drawing vars
                 */

//                


                //move back
                pa.translate(0, -(float) pa.width / 2, 0);




            } else {


                pcounter++;

                //move drawing off-centre
                pa.translate(0, (float) 2 * (pa.width / 5), 0);


                PersonAction pac = (PersonAction) i.actions.get(0);
                //if backyard

                if (pac.bestBundle.employer == null) {
                    pa.stroke(255);


                } else {
                    pa.stroke(0, 255, 0);


                }

                //circle at height of utility
                //pa.translate(0f, 0f, 40);
//                pa.translate(0f, 0f, (float) (1 - pac.bestBundle.maxUtility) * 20f);
//                //System.out.println(pac.bestBundle.maxUtility  * 10);
//
//                pa.ellipse((float) (i.getx() * (pa.width / width)),
//                        (float) (i.gety() * (pa.height / width)), (float) 5, (float) 5);
//
//                //pa.translate(0f, 0f, -40);
//                pa.translate(0f, 0f, -(float) (1 - pac.bestBundle.maxUtility) * 20f);

//                pa.rect((float) pcounter / (float) (gl.numPeople) * width * (pa.width / width), 0, (float) (pac.bestBundle.maxUtility - 1) * 100f, (float) (pac.bestBundle.maxUtility - 1) * 100f);

                //move drawing back
                pa.translate(0, -(float) 2 * (pa.width / 5), 0);


            }

            //two squares: one one ground position...


            //pa.stroke(0, 255, 0);

        }//end for HasLocation 


    }

    /*
     * Used by draw3D methods for drawing vars in defined ways
     */
    private void drawVar(String name, float trans, float xloc, float var, Color c, boolean draw) {


        //var = 200;

        pa.translate(0, trans, 0);

        //label
        pa.fill(255);
        pa.text(name, 35, 10);
        pa.noFill();

        if (draw) {
            pa.stroke(c.getRGB());
        } else {
            pa.stroke(100);
        }

        //if the circle's too big, don't bother drawing. Mark with triangle to indicate this fact.
        if (var > 200) {
            //System.out.println("var more'n 200");

//            pa.triangle(xloc * (pa.width / width) - 7, 7, xloc * (pa.width / width) + 7, 7, xloc * (pa.width / width), -7);
            pa.triangle(xloc * (pa.width / width) - (var / 200), (var / 200), xloc * (pa.width / width) + (var / 200), (var / 200), xloc * (pa.width / width), -(var / 200));

        } else {

            pa.ellipse(xloc * (pa.width / width),
                    0, var, var);
        }


        pa.translate(0, -trans, 0);


    }

    /**
     * Returns a new absolute-location point from a relative-to-zero point and
     * the absolute point that stands in for zero
     *
     * @param relativePoint
     * @param absolutePoint
     * @return
     */
    public static Point2D.Double convertRelativeToAbsolutePoint(Point2D.Double relativePoint, Point2D.Double absolutePoint) {

        return new Point2D.Double(absolutePoint.x + relativePoint.x, absolutePoint.y + relativePoint.y);

    }
}
