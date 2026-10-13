package location2012;

//package location1;
//
//import java.awt.geom.Point2D;
//import java.util.ArrayList;
//import location1.actiontools.Action;
//import location1.econs.Productivity;
//import location1.observe.ShoutEvent;
//import location1.utils.Trig;
//import location1.utils.gl;
//
//
///**
// * "Backyard" actors will only be making one decision, weighing up all their options
// * at once.
// * @author Olner Dan
// */
//public class BackyardSingleActorActionCopiedFrombackyard2 extends Action {
//
//    Actor me;
//    //For storing people I'm considering
//    //A location wrapper so I can use virtual points -
//    //there's my encapsulation stuffed!
//    ArrayList<Actor> people;
//    //Another list for checking distances for weighted density cost calcs
//    ArrayList<Actor> dpeople;
//    //for summing weights, before multiplying density cost
//    double weightTot;
//    //Number of times to sample space around me looking for more productive spots
//    int samplePoints;
//    //Memory of how much I last added.
//    double myLastTimeInput = 0;
//    //who I choose to work with - my job to add and remove my time from them
//    //in modelmaker2010 20th April
//    //Selected for intermediate...
//    Actor collaborator, selected;
//    //temporary variables
//    //Used for intermediary checking of outputs
//    double myWorkTime, remainingWorkTime, tempOutput, currentBestOutput, densityCost, tempCommute, bestCommute;
//    //temporary Point2D used for sampling space
//    Point2D.Double sampleP;
//    //for data output
//    public double nearestNeighb;
//
//    //Take in the me torus, to use its spatial methods
//    //to find out about other actors
//    //And of course who I belong to
//    public BackyardSingleActorActionCopiedFrombackyard2(Actor p, int samplePoints) {
//
//        me = p;
//
//        this.samplePoints = samplePoints;
//
//    }
//
//    public void heard(ShoutEvent s) {
//
//        //Hackery
//        tempCommute = 0;
//        bestCommute = 0;
//        //me.chosenDensityCost = 0;
//        nearestNeighb = 100;
//
//        //decision algorithm 1: Random spots:
////        a)Choose x number of random spots within range, as well as checking current spot
////        b)Work out ‘home’ and PC productivities for each, keeping the one that’s the most productive
//
//        //Notes: Modelmaker2010 20th April
//
//        //RESET my contributed time
//        //Oh my god, there has to be a better way of doing that...!
//        me.myContribTime.to.removeMyTime(me.myContribTime);
//
//
//        //RESET my temp vars
//        me.tempProductivity = 0;
//        me.tempContribTime = 0;
//        //me.myLocActor.commuteDistance = 0;
//        //me.myLocActor.moveDistance = 0;
//
//        //CHECK MY OWN SPOT
//        //May return myself...
//
//        //select my own location wrapper by default
//        collaborator = me;
//
//        //p.a("Checking I have no productivity as I start: " + collaborator.getActor().tempProductivity);
//
//        //Compare my backyard to neighbour collaboration
//        collaborator = whosMostProductive(collaborator, me.getPoint());
//
////        if (collaborator.getActor().equals(me)) {
////            p.s("chose myself, going to produce " + collaborator.getActor().tempProductivity, this);
////        } else {
////            p.s("Chose a collaborator; output commuting from my spot: "
////                    + collaborator.getActor().tempProductivity, this);
////        }
//
//
//        //TEST:
//        //Breaking up sample points into a 1 day radius check
//        //and a much nearer check
//        //Reason: it's just annealling, really. If actors choose randomly up to
//        //1 day away, they're less likely to find a better spot nearby.
//        //With the samplepoint algorithm, there is already a bias towards the centre
//        //but it's not very strong.
//
//        //CHECK DISTANT SAMPLEPOINTS
//        for (int i = 0; i < samplePoints / 2; i++) {
//
//            //samplepoints a maximum of 1 day away from current position
//            //Note: later this should likely be weighted towards being closer
//            //to orig location, since being 1 day away leaves zero time for
//            //production...
//            //Note: I'm pretty sure this Trig method has a bias towards points
//            //closer to the centre. That's actually fine, but worth noting
//            sampleP = Trig.getRandPointInCircleOfRadius(1);
//
//            //adjust for my position
//            sampleP.x += me.getx();
//            sampleP.y += me.gety();
//
//            collaborator = whosMostProductive(collaborator, sampleP);
//
////            if (collaborator.getActor().equals(me)) {
////                p.s("Currently sticking with myself, going to produce " + collaborator.getActor().tempProductivity, this);
////            } else {
////                p.s("Currently chosen a collaborator, going to produce " + collaborator.getActor().tempProductivity, this);
////            }
//
//        }//end check samplepoints
//
//        //CHECK NEAR SAMPLEPOINTS
//        for (int i = 0; i < samplePoints / 2; i++) {
//
//            //samplepoints a maximum of 1 day away from current position
//            //Note: later this should likely be weighted towards being closer
//            //to orig location, since being 1 day away leaves zero time for
//            //production...
//            //Note: I'm pretty sure this Trig method has a bias towards points
//            //closer to the centre. That's actually fine, but worth noting
//            sampleP = Trig.getRandPointInCircleOfRadius(0.05);
//
//            //adjust for my position
//            sampleP.x += me.getx();
//            sampleP.y += me.gety();
//
//            collaborator = whosMostProductive(collaborator, sampleP);
//
////            if (collaborator.getActor().equals(me)) {
////                p.s("Currently sticking with myself, going to produce " + collaborator.getActor().tempProductivity, this);
////            } else {
////                p.s("Currently chosen a collaborator, going to produce " + collaborator.getActor().tempProductivity, this);
////            }
//
//        }//end check samplepoints
//
////        p.s("final collaborator: " + collaborator.getActor().tempProductivity, this);
////
////        if (collaborator.getActor().equals(me)) {
////            p.s("final: chose myself, going to produce " + collaborator.getActor().tempProductivity, this);
////        } else {
////            p.s("final: chose a collaborator; output commuting from my spot: " + collaborator.getActor().tempProductivity, this);
////        }
////
////        p.s("My temp work time: " + me.getTempContribTime() + ", my temp productivity: " + me.getTempProductivity(), this);
//
//
//        //p.p(" ");
//
//        //p.s("Testing object output.", this);
//
//        //MAKE FINAL CHECKS
//        //Now I have my final collaborator (which is possibly me) I need to:
//        //Move, if I decided to
//
//        //Add any time contributions that need doing, including if I move and need to dump all collaborators
//        //Note: my time contribution is removed when it's next my turn to find work
//        me.myContribTime.setFromToTime((Person) me, (Person) collaborator, me.tempContribTime);
//        collaborator.addMyTime(me.myContribTime);
//
//        //test my productivity for this turn
//        me.productivity = (Productivity.giveOutput(me.myContribTime.to.getContributedTime())
//                * (me.myContribTime.getTime() / me.myContribTime.to.getContributedTime()));
//
//        //Testing what's happened with commute being  > 1
//        //store a record of my commute time for data output. That's the distance between my
//        //current position and my collaborator * 2
//        //me.myLocActor.setCommute(me.myLocActor.distanceBetween(me.getTempPoint(), me.myContribTime.to.myLocActor) * 2);
//
//        //hackery
////        me.setCommute(bestCommute);
//        //p.a("bestCommute committed to file: " + bestCommute);
//
//
//
//        //testing broken vpt method: note that bestCommute and commute found with current vpt don't match
//        //but that the commute value found *is* in the list of vpts.
//        //Conclusion: a later search sets the vpt to another value. Prob reaching all the way round the torus
////        if ((me.myLocActor.distanceBetween(me.getTempPoint(), me.myContribTime.to.myLocActor) * 2) > 1) {
////            p.a("commute > 1: " + me.myLocActor.distanceBetween(me.getTempPoint(), me.myContribTime.to.myLocActor) * 2);
////            p.a("bestCommute: " + bestCommute);
////            if (me.myContribTime.to.myLocActor.vpt == 9) {
////                p.a("VPT == 9: " + me.myContribTime.to.myLocActor.vpt);
////            }
////            //test values for all other vpts for this actor
////            for (int i = 0; i < 9; i++) {
////                p.a("VPT " + i + ", val: " + me.getTempPoint().distance(me.myContribTime.to.myLocActor.vp[i]) * 2);
////            }
////
////        }
//
//        //p.a("commute committed to file = " + me.myLocActor.commuteDistance);
//        //If I moved, I need to move(!) and dump all contributors who were with me
//
//        //Did I move?
//        /*
//        if (me.getPoint().distance(me.getTempPoint()) != 0) {
//
//            //p.a("I moved from " + me.getPoint().x + "," + me.getPoint().y);
//            //me.getPoint().setLocation(me.getTempPoint());
//            me.moveTo(me.getTempPoint());
//
//            //p.a(" to "+ me.getPoint().x + "," + me.getPoint().y);
//            me.removeAllTimeContribs();
//
//        }
//         *
//         */
//
//
//
//    }//end heard method
//
//    /**
//     * Pass in a comparator - is anything else in range of point pt more productive?
//     *
//     * @param comparator
//     * @param pt
//     * @return
//     */
////    private LocActor whosMostProductive(LocActor comparator, Point2D.Double pt) {
//
//        /*
//        //get currentBestOutput from comparator for, you know, comparison
//        currentBestOutput = comparator.getActor().tempProductivity;
//
//        //Check productivity of backyard production on that spot
//        //For backyard production:
//        //Output = total time * (density cost per person * density)
//        //Note: here I'm keeping it very simple - lowest density (zero) = no cost
//        //double normal density = costs all.
//        //densityCost = (2 - (gl.DENSITYCOST * people.size()));
//        //densityCost = 1;
//
//        //1. Get density cost first - will need to do generalwhosInRadius check
//        //afterwards for virtual points to be right.
//
//        //trying a different density cost
//        //int nm = me.myLocActor.whosInRadius(pt, gl.DENSITYCOSTRADIUS).size();
//
//        //Testing weighted density cost - linear for now.
//        //Quick and dirty; if it works, will encapsulate some methods somewhere
//        dpeople = me.myLocActor.whosInRadius(pt, gl.DENSITYCOSTRADIUS);
//
//        //trying the following:
////        0.25 total radius (sorry… kernel bandwidth!)
////        Weight by actual distance to all agents
////        Half of Densityradius = 1; edge = 0; on top of me = 2
////        We could just do that with a straight line weighting, don’t think I need to try anything quadratic just yet.
//
//        //reset WeightTot for each turn
//        weightTot = 0;
//
//        //sum weights based on distance
//        for (LocActor la : dpeople) {
//
//            // = to get 2x for nearer, 0 for on edge of density radius
//            // = 2 - (distance * (2 / densityrad))
//            //Which could just be 2 - (2*distance)/densityrad too, but less clear to read!
//
//
//            weightTot += 2 - ((me.myLocActor.distanceBetween(pt, la) * (2 / gl.DENSITYCOSTRADIUS)));
//
////            p.s("weighttot: " + weightTot + ", My point loc: " + pt.x + "," + pt.y + ", la point loc: " + la.vp[la.vpt].x + "," + la.vp[la.vpt].y
////                    + ", vpt: " + la.vpt + ", Distance to la: " + me.myLocActor.distanceBetween(pt, la) + ", weight: "
////                    + (2 - (me.myLocActor.distanceBetween(pt, la) * (2 / gl.DENSITYCOSTRADIUS))), this);
//
//        }
//
//        //divide by number of detects actors to get final weight
//        weightTot /= dpeople.size();
//
//        //gonna try testing weighting it towards near, something like y=((2-x)^3)/4
//        //To cube it then get back to a 2 to 0 range
////        if (weightTot > 1) {
////            p.p("WeightTot before power: " + weightTot);
////        }
//        weightTot = (Math.pow(weightTot, 2) / 2);
//
////        if (weightTot > 1) {
////            p.p("WeightTot after power: " + weightTot);
////        }
//
////        p.s("final weight: " + weightTot, this);
//
//        //densityCost = (2 - (gl.DENSITYCOST * (weightTot)) * 100);
//        //densityCost = (2 - weightTot);
//
//        //Set density cost proportional to radius in Torus. I have a weight, from 0 to 2. Now...
//
//        //value between 0 and 1 for below-av to average density, more than 1 for higher
//        densityCost = gl.DENSITYCOST * dpeople.size();
////        p.s("Straight density cost: " + densityCost, this);
//
//        //NUmber will now be higher if detected density is nearer
//        densityCost *= weightTot;
////        p.s("Weighted density cost: " + densityCost, this);
//
//        //adjust density cost
//        //densityCost *= 7.5;
//
//
//        //The problem here: I want density cost to work either side of
//
//
//
////        p.s("Density cost: " + densityCost + ", people detected: " + dpeople.size(), this);
//
//        //2. Get a list of who's in 1 days' radius
//        //Note: testing all the way out to 1 probably pointless
//        //since the closer to 1, the closer to have zero time for actual production
//        //need to come back here and get some output on what distances
//        //end up being chosen.
//        people = me.myLocActor.whosInRadius(pt, 1);
//
//        //initialise my work time; 1 minus any proposed move - which is distance from my current location
//        //to the one I'm proposing to move to here.
//        //This will be zero if I'm checking my own position
//        myWorkTime = 1 - (me.getPoint().distance(pt));
////        p.s("my worktime: " + myWorkTime, this);
//
//        //check productivity on the point I'm proposing to move to
//
//        //If that point is my current location, I can keep the time that others
//        //may have already put in (which of course gives an incentive to stay put)
//        if (me.getPoint().distance(pt) == 0) {
//
//            //find my nearest neighbour, for data output
//            for (LocActor a : people) {
//
//                if (nearestNeighb > me.myLocActor.distanceBetween(pt, a)) {
//                    nearestNeighb = me.myLocActor.distanceBetween(pt, a);
//                }
//
//            }
//
//            //Don't need to use my work time here cos we know I'm on the same spot,
//            //See "if (me.getPoint().distance(pt) == 0)" above. So I have 'one' left.
//            tempOutput = Productivity.giveOutput(me.getContributedTime() + 1)
//                    * (1 / (me.getContributedTime() + 1));
//
////            p.s("testing my own spot: tempoutput = " + tempOutput, this);
//            //p.s("Productivity.giveOutput from   " + Productivity.giveOutput(me.getContributedTime() + myWorkTime), this);
//            //p.s("me.getContributedTimeMinusMe: " + me.getContributedTime(), this);
//
//            //only output to screen if I'm collaborating on my own spot. At the moment there's
//            //only one of these per cluster - I want to know how much time they're getting
////            if (me.contribTime.size() > 0) {
////                p.a("on my own spot and I have more contribtime than just me: "
////                        + me.contribTime.size() + ", tot time: " + me.getContributedTime() + ", tempOutput: " + tempOutput);
////
////            }
////            else {
////                p.a("on my own spot and its just me! " + me.getContributedTime() + ", tempOutput: " + tempOutput);
////            }
//
//
//            //otherwise I have to dump all that and just use my own time
//        } else {
//
//            //my time will all be able to go into my work...
//            tempOutput = Productivity.giveOutput(myWorkTime);
//            //Note: can't remove everyone until I know what my final decision is
//            //me.removeAllTimeContribs();
////            p.s("testing samplepoints: tempOutput = " + tempOutput, this);
//
//        }
//
//        //for either of those two, apply the density cost so the total
//        //productivity can be compared to possible collaborators
//        //tempOutput *= densityCost;
//
////        p.s("Backyard check: tempOutput before density cost: " + tempOutput, this);
//
//        //just trying it as a direct tax
//        tempOutput -= densityCost;
////        p.s("Backyard check: tempOutput after  density cost: " + tempOutput, this);
//
//        //if one of those produces more than the given comparator, use my spot
//        if (tempOutput > currentBestOutput) {
//
//            //then we have a new current best
//            currentBestOutput = tempOutput;
//
//            comparator = me.myLocActor;
//
//            //also set the amount of time I'm able to contribute - use later if selected
//            me.setTempContribTime(myWorkTime);
//
//            me.setTempProductivity(tempOutput);
//
//            //set location in case this ends up being where I move to
//            me.setTempPoint(pt);
//
//            //keeping a record of the density cost for the spot I ended up at
//            me.chosenDensityCost = densityCost;
//
//            //hackery: need to get back to testing commute chosen directly
//            bestCommute = 0;
//
//        }
//
//        //Now check others in the vicinity of the chosen point -
//        //If the output I can get is more, change tempOutput
//        for (LocActor a : people) {
//
//            //current myWorkTime includes move. Need to take off commute time,
//            //which is distance from the proposed point to the proposed worksite *2
//            //remainingWorkTime = myWorkTime - (pt.distance(a.vp[a.vpt]) * 2);
//            tempCommute = me.myLocActor.distanceBetween(pt, a) * 2;
//
//            //p.a("vpt in comparator method: " + a.vpt);
//
//            remainingWorkTime = myWorkTime - tempCommute;
//
//            //If there's any worktime left over after commuting...
//            if (remainingWorkTime > 0) {
//
//                //p.a("commute distance found when testing = " + tempCommute);
//                //.. find out how much productivity I'd get from this PC
//                //If productivity from its existing time contribs plus my own is
//                //more than previously found above...
//
//                //What output will I get, as a proportion of the time I put in?
//                double tempCollabOutput = (Productivity.giveOutput(remainingWorkTime
//                        + a.getActor().getContributedTime())) * (remainingWorkTime / (remainingWorkTime
//                        + a.getActor().getContributedTime()));
//
//                //apply the density cost so the total
//                //productivity can be compared to possible collaborators
//
//                //trying direct tax
//                //tempCollabOutput *= densityCost;
////                p.s("Collaborators check: tempOutput before density cost: " + tempCollabOutput, this);
//                tempCollabOutput -= densityCost;
////                p.s("Collaborators check: tempOutput after  density cost: " + tempCollabOutput, this);
//
//                if (tempCollabOutput > currentBestOutput) {
//
//                    currentBestOutput = tempCollabOutput;
//
//                    //hackery
//                    bestCommute = tempCommute;
//
//                    comparator = a;
//
//                    //Set temporary Output for this actor...
//                    comparator.getActor().setTempProductivity(tempCollabOutput);
//
//                    //Set tempWorkTime for me - what I am contributing
//                    me.setTempContribTime(remainingWorkTime);
//
//                    //set location in case this ends up being where I move to
//                    me.setTempPoint(pt);
//
//                    //keeping a record of the density cost for the spot I ended up at
//                    me.chosenDensityCost = densityCost;
//
//
////                    p.s("selected another: output will be " + tempCollabOutput, this);
//
//                }//end if
//                //p.s("My work time: " + myWorkTime + ", minus commute: " + remainingWorkTime, this);
//
//            }//end if remainingWorkTime
//
//        }//end for LocActor
//
//
//
//        //if selected wasn't changed either to myself or to a new collaborator,
//        //the same comparator will be returned as was given
//        return comparator;
//
//         *
//         */
//
//
//
////    }
//}
//
