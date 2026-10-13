/*
 * I'll be making the actors, hard-code, here for now. 
 */
package location2012.factories;

import location2012.Firm;
import location2012.Actor;
import location2012.LandLord;
import location2012.SingleActorAction;
import location2012.SingleActorPerson;
import location2012.LandLordAction;
import java.util.ArrayList;
import location2012.FirmActMarkup;
import location2012.FirmActRespondAverage;
import location2012.Main;
import location2012.Person;
import location2012.PersonActionTransmission;
import location2012.econs.GoodSeller;
import location2012.geog.HasLocation;
import location2012.geog.SpaceTools;
import location2012.utils.PullOneClassFromArray;
import location2012.utils.gl;

/**
 *
 * @author User
 */
public class ActorMaker {

    private static int numPeople, numFirms, numLandLords;
    //iterated to give actors a unique ID
    private static int ID;

    public static void makeActors(ArrayList<Actor> actors, Main m, SpaceTools t) {

        ID = 0;

        //just trying this here...
        gl.people = new ArrayList<Actor>();

        //make people
        //First, work out the number of agents from the size of the world
        //and the number of agents (av) in a unit radius
        //Number of agents within a unit radius is just PI since r=1.
        //Then divide total area of world by this to get total number of agents
        //we need to produce
        switch (gl.space) {
            case Point:

                numPeople = gl.numAgents;
                numFirms = gl.numAgents / 3;

                break;

            case Line:

                numPeople = gl.numAgents;
                numFirms = gl.numAgents / 3;
                //Just enough to span the line
                numLandLords = gl.landCostsOn * (int) gl.width;

                break;

            case TwoRegion:

                numPeople = gl.numAgents;
                numFirms = gl.numAgents / 3;

                break;

            case Torus:

                numPeople = (int) (((double) (t.width * t.width)) / (Math.PI / (double) gl.numAgents));

                //for now, let's just make a tenth the number of firms as people
                numFirms = (int) (((double) (t.width * t.width)) / (Math.PI / (double) gl.numAgents));
                numFirms /= 3;

                //enough to form a grid
                numLandLords = gl.landCostsOn * gl.landCostsOn * (int) gl.width;

                break;
        }//end switch

        //For overriding firm numbers
        numFirms = (gl.overRideNumFirms > -1 ? gl.overRideNumFirms : numFirms);
        numPeople = (gl.overRideNumPeople > -1 ? gl.overRideNumPeople : numPeople);

        //p.p("number of people: " + numPeople);
        System.out.println("number of people: " + numPeople);

        gl.numPeople = numPeople;

        for (int i = 0; i < numPeople; i++) {

            if (gl.singleActor) {
                actors.add(new SingleActorPerson(ID++));
                actors.get(i).addAction(new SingleActorAction(actors.get(i)));
            } else {
                actors.add(new Person(ID++));

                //hacking transmission action
                if (gl.transmissionFirms) {
                    actors.get(i).addAction(new PersonActionTransmission(actors.get(i)));
                } else {
//                    actors.get(i).addAction(new PersonAction(actors.get(i)));
                    actors.get(i).addAction(new PersonActionTransmission(actors.get(i)));
                }
            }

            actors.get(i).setColour();

            //add to people array
            gl.people.add(actors.get(i));

        }

        //p.p("number of firms: " + numFirms);
        System.out.println("number of firms: " + numFirms);
        System.out.println("number of landlords: " + numLandLords);


        gl.numFirms = numFirms;

        boolean necessity = false;

        Firm f;

//        System.out.print("Testing ++: ");
//        int val = 0;
//        System.out.print(val++);
//        System.out.println(", " + val);

        //hacking variable good cost
//        double goodCost = 2;

        gl.colCount = 0;
        //create firms, add Firm's action.
        for (int i = 0; i < numFirms; i++) {

            actors.add(new Firm(ID++));
            //make firms array for SpaceTools to use
//            t.isInSpace.add((HasLocation) actors.get(ID - 1));
            t.firmsInSpace.add((HasLocation) actors.get(ID - 1));

//            actors.get(numPeople + i).addAction(new FirmActDummy(actors.get(numPeople + i)));
//            actors.get(numPeople + i).addAction(new FirmActMarkup(actors.get(numPeople + i)));
//            actors.get(numPeople + i).addAction(new FirmActRespondAverageMCMRtest(actors.get(numPeople + i)));
//            actors.get(numPeople + i).addAction(new FirmActRespondAvMCMR(actors.get(numPeople + i)));
            actors.get(numPeople + i).addAction(new FirmActRespondAverage(actors.get(numPeople + i)));
//            actors.get(numPeople + i).addAction(new FirmActRespondAverageOneMove(actors.get(numPeople + i)));
//            actors.get(numPeople + i).addAction(new FirmActFeb11_v2(actors.get(numPeople + i)));
//            actors.get(numPeople + i).addAction(new FirmActFeb11(actors.get(numPeople + i)));
//            actors.get(numPeople + i).addAction(new FirmAct2(actors.get(numPeople + i)));
            actors.get(numPeople + i).setColour();

            //hacking variable good cost
//            f = (Firm) actors.get(numPeople + i);

//            f.deliverycost = goodCost;

//            goodCost += 2;

//            System.out.println("Firm good cost: " + f.goodCost);

            //add a type of good for each firm
            //hacking different values for delivery and price
            if (necessity) {

                f = (Firm) actors.get(numPeople + i);
                f.goodType = gl.GoodType.Necessity;
                necessity = false;

            } else {

                f = (Firm) actors.get(numPeople + i);
                f.goodType = gl.GoodType.Luxury;
                necessity = true;

            }


        }//for i
        
        //Testing: give to a single firm
        actors.get(numPeople).addAction(new FirmActMarkup(actors.get(numPeople)));

        if (gl.landCostsOn > 0) {
            //create landlords
            for (int i = 0; i < numLandLords; i++) {

                actors.add(new LandLord(ID++));
                actors.get(numPeople + numFirms + i).addAction(new LandLordAction(actors.get(numPeople + numFirms + i)));
//                actors.get(numPeople + numFirms + i).addAction(new LandLordSimpleAction(actors.get(numPeople + numFirms + i)));

            }
        }//end if gl.landCostsOn

        //Give actors one set of actions only for this model
        //torus spatial methods accessible via Actor's LocActor
        //Last int = samplePoints for sampling space

        //Start with just action for people
        for (Actor a : actors) {
            //a.addAction(new TestAction(a));
            //Actors implement HasLocation, so need to set a ref to the space
            a.setSpace(t);

            //I'll also be kind and give the space its own HasLocation refs
            t.isInSpace.add((HasLocation) a);


        }



        //set up global actor references
        gl.allActors = actors;

//        gl.people = new ArrayList<Actor>();
//        PullOneClassFromArray.PULLCLASS(actors, gl.people, new Person(0));

        //same for firms
        gl.firms = new ArrayList<Actor>();
//        PullOneClassFromArray.PULLCLASS(actors, gl.firms, new Firm(0));
        PullOneClassFromArray.PULLCLASS(actors, gl.firms, new Firm(0));

        //same for goodSellers (may be firms or people)
//        try {
        gl.goodSellers = new ArrayList<Actor>();
//            PullOneClassFromArray.PULLCLASS(actors, gl.goodSellers, Class.forName("location2012.econs.GoodSeller"));
//            PullOneClassFromArray.PULLCLASS(actors, gl.goodSellers, GoodSeller);

//        } catch (ClassNotFoundException e) {
//            System.err.println("oop! " + e.toString());
//
//        }

        //can't get this fuckin' thing to work with PULLCLASS method
        //http://stackoverflow.com/questions/766106/test-if-object-implements-interface
        for (Object o : actors) {
            if (o instanceof GoodSeller) {
                gl.goodSellers.add((Actor) o);
            }
        }

//        System.out.println("no of gss: " + gl.goodSellers.size());
//        System.out.println("no of firms in separate array: " + gl.firms.size());

        //Pull out landlords
        gl.landLords = new ArrayList<Actor>();
//        PullOneClassFromArray.PULLCLASS(actors, gl.landLords, new LandLord(0));


        //pick one BY_Action to observe
        //p.singleObjAdd(actors.get(gl.LOOKATME).actions.get(0));

        //assign a single firm to each person
        if (gl.vonThunenMode) {
//        if (gl.vonThunenGoods || gl.vonThunenWage || gl.vonThunenDelivery || gl.vonThunenCommute) {

            Person p;

            int FirmNumCounter = 0;
            for (Actor a : gl.people) {

                p = (Person) a;
                f = (Firm) gl.firms.get(FirmNumCounter++);
//                p.vonThunenFirmID = f.ID - gl.people.size();
                p.vonThunenFirmID = f.ID;
                p.vonThunenFirm = f;

//                if (gl.vonThunenWage) {
//                    p.vonThunenWageIndex = f.ID - gl.people.size() + 1;//starts at one as it's used to multiply base costs.
//                }
//                if (gl.vonThunenGoods) {
//                    p.vonThunenGoodsIndex = f.ID - gl.people.size() + 1;//starts at one as it's used to multiply base costs.
//                }
//                if (gl.vonThunenCommute) {
//                    p.vonThunenCommuteIndex = f.ID - gl.people.size() + 1;//starts at one as it's used to multiply base costs.
//                }
//                if (gl.vonThunenDelivery) {
//                    p.vonThunenDeliveryIndex = f.ID - gl.people.size() + 1;//starts at one as it's used to multiply base costs.
//                }

                FirmNumCounter = (FirmNumCounter == gl.firms.size() ? 0 : FirmNumCounter);
//                System.out.println("Person " + a.ID + ", firm ID: " + p.vonThunenFirmID);

            }


        }//end if gl.vonThunenMode

        //create multiples of four main costs for each firm
        for (int i = 0; i < gl.firms.size(); i++) {

            f = (Firm) gl.firms.get(i);
            //hack, reversing order. Leave this here to add flag later
//            double j = gl.firms.size() - i - 1;
            double j = i;

            if (gl.deliveryRange) {
                f.deliverycost = gl.deliveryCost * (j + 1);
                System.out.println("Firm " + f.ID + " delcost: " + f.deliverycost);
//                    f.goodCost = minCH + (((maxCH - minCH) / (double) gl.firms.size()) * i);
            }
            if (gl.goodsRange) {
                f.goodCost = gl.good * (j + 1);
                System.out.println("Firm " + f.ID + " good: " + f.goodCost);
//                    f.goodCost = minCH + (((maxCH - minCH) / (double) gl.firms.size()) * i);
            }
            if (gl.wageRange) {
                f.wage = gl.wage * (j + 1);
                System.out.println("Firm " + f.ID + " wage: " + f.wage);
//                    f.goodCost = minCH + (((maxCH - minCH) / (double) gl.firms.size()) * i);
            }
            if (gl.commuteRange) {
                f.commuteCost = gl.commuteCost * (j + 1);
                System.out.println("Firm " + f.ID + " commute: " + f.commuteCost);
//                    f.goodCost = minCH + (((maxCH - minCH) / (double) gl.firms.size()) * i);
            }


        }

        SingleActorPerson p;

        //hacking single actor delivery cost spread for five values
//        if (gl.singleActor) {
//            double val = 0.01;
//
//            for (Actor a : gl.people) {
//
//                p = (SingleActorPerson) a;
//
//                p.deliverycost = val;
//                val += 0.1;
//
//                //cycle through 1 to 4
//                if (val > 0.5) {
//                    val = 0.01;
//                }
//
//            }
//
//        }//end if gl.singleActor

        //test
//        for (Actor a : gl.people) {
//
//            p = (SingleActorPerson) a;
//
//            System.out.println("SAP del: " + p.deliverycost);
//
//        }


        //Randomise starting positions in space
        //may be overwritten by default position setting in Main
        t.setDefaultActorPositions();


    }
}
