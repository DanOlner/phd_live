/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package location2012;

import java.awt.Color;
import java.awt.geom.Point2D;
import java.awt.geom.Point2D.Double;
import java.util.ArrayList;
import location2012.actiontools.Action;
import location2012.econs.ContributedTime;
import location2012.geog.HasLocation;
import location2012.geog.LocMemory;
import location2012.geog.LocMemory.ActorNLocation;
import location2012.geog.SpaceTools;
import location2012.utils.gl;

/**
 *
 * @author User
 */
public abstract class Actor implements HasLocation {

    //test: permute distance between daily actions. These are raw position values
    //so will be related to actor number
    public double permutePos, yesterdayPermutePos;
    //Torus reference for implementing HasLocation
    SpaceTools space;
    //Location, for implementing HasLocation
    Point2D.Double xy = new Point2D.Double(0, 0);
//    //and handy direct versions
    public double x, y;
    //whosInRange searches return ActorNLocations with location relative to my centre.
    //zp can be used to represent this centre point.
    Point2D.Double zp = new Point2D.Double(0, 0);
    //Just one LocMemory for now...
    //Setting a new one to avoid any nullPointerExceptions when e.g. drawing
    public LocMemory lm = new LocMemory();
    //public ID
    public int ID;
    //ref to whoever is implementing
    //Object parent;
    //Action vars
    public ArrayList<Action> actions = new ArrayList<Action>();
    /**
     * Data-collection-related vars
     */
    public double nearestNeighbour;

    /*
     * These are all backyard-related vars. Now is the time of hacking
     */
//    public double totalTimeInput;
//    //Wrappers for storing who is contributing time, and how much
//    public ArrayList<ContributedTime> contribTime = new ArrayList<ContributedTime>();
//    //The time I'm currently contributing to someone (including myself)
//    public ContributedTime myContribTime;
//    //Used for temporarily storing arrived-at productivity level for when
//    //other actors are considering collaboration
//    double tempProductivity;
//    //used for doing the same with contributed time
//    double tempContribTime;
//    //Used in various places, including for drawing and data output - my final productivity
//    public double productivity;
//    //a record of the density cost for the spot I ended up on
//    public double chosenDensityCost;
    public Color colour;

    //pass in an ID
    public Actor(int ID) {
        this.ID = ID;

    }

    /**
     * @param a
     */
    public void addAction(Action a) {
        actions.add(a);
    }

    public void setColour() {
        //give em a distinct colour, so that can be consistent throughout vizes.
        colour = gl.nextActorColour();

    }

    /*
     * Implemented haslocation methods
     */
    public void setSpace(SpaceTools t) {
        space = t;
    }

    public SpaceTools getSpace() {
        return space;
    }

    public double getx() {
        return xy.x;
    }

    public double gety() {
        return xy.y;
    }

    public void setx(double x) {
        xy.x = x;

        this.x = x;

//        System.out.println("are these used?");

    }

    public void sety(double y) {
        xy.y = y;

        this.y = y;

//        System.out.println("are these used?");

    }

    public void setPoint(Point2D.Double pt) {
        xy = pt;

//        x = xy.x;
//        y = xy.y;

    }

    public Point2D.Double getPoint() {
        return xy;
    }

    //move to location relative to my current spot
    public void moveTo(Double pt) {

        //adjust absolute position by relative point
        xy.x += pt.x;
        xy.y += pt.y;

        //make sure that's in the space
        xy = space.checkInTorus(xy);
        
    }

    public void addLocMemory(LocMemory lm) {

        this.lm = lm;

    }

    /**
     * For data collection: find nearest neighbour
     */
    public void findNearestNeighbour() {

        //will be testing for lower values
        nearestNeighbour = 9999;

        lm = getSpace().whosInRange(this, getPoint(), 1);
//        System.out.println("PersonAction. number of firms found: " + me.lm.actors.size());

        //set nearest neighbour val
        //find my nearest neighbour, for data output
        for (ActorNLocation a : lm.actors) {

            if (nearestNeighbour > zp.distance(a.p)) {
                nearestNeighbour = zp.distance(a.p);
            }

        }//end set nearest neighbour val

    }
}
