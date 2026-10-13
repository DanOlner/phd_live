/*
 * Actor: in the "Backyard" model there's only one type of agent.
 * I'm tempted to leave out the 'actions' framework for now and work a little more
 * nimbly. Let's see how that goes... 
 */
package location2012;

import java.awt.geom.Point2D;
import location2012.econs.Bundle;

/**
 * Actions take actions on factors
 *
 * @author Olner Dan
 */
public class Person extends Actor {

    //used as record of single firm I buy from, if von Thunen mode on
    public int vonThunenFirmID;
    //Costs are multiplied by this. ActorMaker can set them differently for different actors (e.g. to cost a certain amount if matching... 
    public int vonThunenWageIndex = 1;
    public int vonThunenCommuteIndex = 1;
    public int vonThunenGoodsIndex = 1;
    public int vonThunenDeliveryIndex = 1;
    public Firm vonThunenFirm;
    //record of maxUtility to avoid losing it when Bundles get trashed
    public double maxUtility;
    //record of density cost
    public double chosenDensityCost;
    //record of discrete number of People I detected while working out density cost
    public int numPeopleInDensityCostRadius;
    //record of mean distance from me. I suspect often they'll be on top of each other
    public double meanDistanceOfPeopleInDensityCostRadius;
    //for which I probably need a ref to the best bundle...
    public Bundle bestBundle;
    //for data recording in monocentric models
    public double distanceToCentralFirm;
    //for recording distance moved since last turn
    public double distanceMovedToday;
    public Point2D.Double locationYesterday = new Point2D.Double();

    //Hacks for transmission action to work
    public double ratioOfWageSpentOnNearestProducer;
    //Hacks for Hotelling
    public double firm0demand, firm1demand;      
    
    
    public Person(int ID) {
        super(ID);
    }
    //Person-specific vars
    public double myCurrentWorkTime;
    //Will only be briefly passing through my hands!
    public double myMoney;
}

/*
 * Cuttinz
 */
// //bolting on a time variable for productivity; encapsulation drifts further away
//    //But a running model approatheth...
//    //Later I can maybe interface it, lark.
//    //Could also do with looking into some design pattern cos interfaces are RUBBISH
//    //Total amount of time being contributed at any point
//    //Other actors are responsible for adding and removing their own time
//    //Set to 1 initially: this 'seeds' everyone with their own backyard production
//    //level so that if others collaborate, there's already time being put in.
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
//
//    public void setTempProductivity(double d) {
//
//        tempProductivity = d;
//
//    }
//
//    public double getTempProductivity() {
//
//        return tempProductivity;
//
//    }
//
//    public void setTempContribTime(double d) {
//
//        tempContribTime = d;
//
//    }
//
//    public double getTempContribTime() {
//
//        return tempContribTime;
//
//    }
//
//    /**
//     * return double value of total contributed time
//     * This is re-calculated each time people are added or removed
//     * @return
//     */
//    public double getContributedTime() {
//
//        return totalTimeInput;
//
//    }
//
//    /**
//     * return double value of total contributed time, minus my own
//     * This is re-calculated each time people are added or removed
//     * @return
//     */
//    public double getContributedTimeMinusMe() {
//
//        for (ContributedTime findme : contribTime) {
//
//            if (findme.from.equals(this)) {
//
//                p.p("In GetcontribMinusMe: Found myself!" + (totalTimeInput - findme.getTime()));
//                return totalTimeInput - findme.getTime();
//
//            }
//
//        }
//
//        p.p("In GetcontribMinusMe: Didn't find myself and returning " + totalTimeInput);
//        return totalTimeInput;
//
//
//
//    }
//
//    /**
//     * add a person's time to the total... recalculate that total
//     *
//     * @param ct
//     */
//    public void addMyTime(ContributedTime ct) {
//
//        contribTime.add(ct);
//
//        totalTimeInput += ct.getTime();
//
//    }
//
//    public void removeMyTime(ContributedTime ct) {
//
//        //if contibution successfully removed, also
//        //remove the time. If it wasn't, it means that
//        //time isn't here and thus doesn't need to be removed
//        if (contribTime.remove(ct)) {
//            totalTimeInput -= ct.getTime();
//        }
//
//    }
//
//    public void removeAllTimeContribs() {
//
//        contribTime.clear();
//        totalTimeInput = 0;
//
//    }
//    /**
//     * Move me to another spot, making sure my wrapper sorts the virtual points
//     * Gosh: accessing wrapper from here - again, hardly encapsulated, are we?
//     */
////    public void moveTo(Point2D.Double mt) {
////
////        torus.checkInTorus(mt);
////
////        setPoint(mt);
////
////        //and set virtual points
////        myLocActor.setVirtualPoints();
////
////    }
