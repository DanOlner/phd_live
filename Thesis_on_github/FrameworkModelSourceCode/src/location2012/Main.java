/*
 * twogoods1: modelling agents who demand a mix of two goods
 * March 2010
 */
package location2012;

import java.awt.geom.Point2D;
import location2012.observe.Timeline;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import location2012.io.DataStore;
import location2012.io.DataReader;
import location2012.io.CSVWriter;
import location2012.graphics.Drawable;
import location2012.graphics.ProcessingSpaceDrawer;
import location2012.geog.SpaceTools;
import location2012.utils.Randoms;
import location2012.utils.Timer;
import location2012.utils.gl;
import location2012.utils.p;
import java.util.StringTokenizer;
import location2012.actiontools.RandPermuteActorListener;
import location2012.factories.ActorMaker;
import location2012.graphics.*;
import location2012.variables.VariableStore;
import location2012.variables.VariableStoreCoords;
import location2012.variables.VariableStoreMeanFollow;
import location2012.variables.VariableStoreMeanSD;
import location2012.variables.VariableStoreSDFollow;

/**
 *
 * @author Olner Dan
 */
public class Main {

    /**
     * Objects
     */
    //Model-wide random number generator
    //Simply incremented by one for Monte Carlo /
    //; actual random seed doesn't matter.
    static Randoms rn = new Randoms(12);
    public static int paramPos;
    //new CESOptimiseHillClimb(0.5);
    //start timer off
    //Y'know, I'm not sure I needed this. I think there's a class that does this already.
    static Timer timer = new Timer();
    //Array for drawable classes
    //Note: can have more than one set of drawables, e  ach for a different
    //Processing viewer. Question: how to get same data viewed in different way,
    //if there's only one 'draw3D' method per drawable?
    //Answer is going to be some intermediary class - I'll worry about that later!
    ArrayList<Drawable> drawables = new ArrayList<Drawable>();
    //testing a second drawable...
    ArrayList<Drawable> bundleOptDrawables = new ArrayList<Drawable>();
    /**
     * Class scope so metadata DataStore can quiz model run buckets to get
     * many-run data
     */
    DataStore ds;
    /**
     * Model entities
     */
    ArrayList<Actor> actors;

    /**
     * Number variables
     */
    //Will have different meanings for different spacetypes
    //point and region: total number of agents
    //Torus: number within unit radius
    //width of world.
    //public double width = 2;
    //if two-region discrete, distance between regions
    public Main() {

        //All model setup farmed out to this one location. It can then tell other
        //classes what setup needs doing. The alternative: flitting around changing
        //them in-class, or making some front-end. Neither very appealing.
        gl.setUpFromPropertiesFile();

        //check number of processors
        System.out.println("Number of processors available: " + Runtime.getRuntime().availableProcessors());

        //This in theory may end up in an external control GUI, hence not just commenting out one of 'ModelRun' or 'DataRead'
        gl.setMode(gl.Mode.Model);

        //set up many-run data collection. Call manually, since I'm hacking for England now.
        DataStore allRunsData = new DataStore();

//        metadata.addVariableStore(new NearestNeighBucketMeansSDsOnly("NearestNeighbourMeanAndSD", gl.allActors, gl.modelRunDays));
//        allRunsData.addVariableStore(new NearestNeighMetaBucket("NearestNeighbourMeta", gl.dataWritePointNumber));

        //iterate through runs
//        for (int i = 0; i < gl.numberOfRuns; i++) {
        //Used for data writing, needs to know which position we're at.
        paramPos = 0;

        //getting number of param points
//        for (double sweepVal = 0.95; sweepVal > 0.1; sweepVal -= 0.05) {
        for (double sweepVal = 0; sweepVal < 0.99; sweepVal += 0.01) {
//        for (double sweepVal = 1; sweepVal < 1000; sweepVal += 1) {
            paramPos++;
        }

        //one row for each paramPos
//        allRunsData.addVariableStore(new FirmsCircleGameMetaBucket("FirmsCircleGameMeta", paramPos));
//        allRunsData.addVariableStore(new NumFirmsCESMetaBucket("NumFirmsCESMeta", paramPos));

        paramPos = 0;

        //param sweep
//        for (double sweepVal = 0.95; sweepVal > 0.1; sweepVal -= 0.05) {


//        for (double sweepVal = 0; sweepVal < 0.95; sweepVal += 0.05) {
//        for (double sweepVal = 1; sweepVal < 1000; sweepVal += 1) {
//        for (double sweepVal = 0; sweepVal < 0.99; sweepVal += 0.01) {

        //change param
//            CESOneTypeUtility.rho = sweepVal;
//            gl.overRideNumFirms = (int) sweepVal;
//            gl.paramSweepVal = sweepVal;

        paramPos++;

        //randomisation number
        for (int i = 0; i < gl.numberOfRandomSeedChanges; i++) {


            //I can't see why this is necessary, but it works: stops timeLine from dying.
            gl.killSwitch = false;

            gl.currentRun = i;

            switch (gl.mode) {
                case Model:
                    ModelRun();
                    break;
                case DataRead:
                    DataRead();
            }

            //write this run's data from ds's stored bucket
//                allRunsData.getBucket("NumFirmsCESMeta").grabData(ds.getBucket("NumFirmsCESBucket"));

            //inc seed
            rn.incrementSeed();
            //clear out arrays
            gl.time.shouters.clear();
//            gl.time = null;
            gl.time = new Timeline(gl.modelRunDays);
            gl.allActors.clear();
            gl.people.clear();
            gl.firms.clear();

            drawables.clear();

        }//for numberofRuns

//        }//for paramsweep

        //write many-run data
//        CSVWriter manyRunsWriter = new CSVWriter(allRunsData);

//        manyRunsWriter.heard(null);

        //writing if not based on number of days
//        staticArrayWriter.write2DDoubleArray(allRunsData.getBucket("NumFirmsCESMeta").vals, "NumFirmsCes");

        System.exit(0);

    }//end of BY_Main constructor / model init

    private void ModelRun() {

        //if two region, use distance define distance between regions
        //code will take care of making world to fit those regions in.
        //Also, when initialising positions:
        //Randomisation would, on average, just mean a 50/50 split between the two regions.
        //Need to actually define how many in each region to start. So, for both firms and people:
        //See gl for explanation
        System.out.println("Space used: " + gl.space);

        /**
         * *
         * SETTING UP THE MODEL *
         */
        //Make actors home
        actors = new ArrayList<Actor>();

        //new GlobalDensityCostChanger(time, 1);
        //"Av number of agents in one day's radius" is now passed in to ActorMaker
        //or rather a ref to this Main is, and it's accessed directly
        SpaceTools space = new SpaceTools(gl.width);

//          drawables.add(space);

        //Hard-coding creation of actors in ActorMaker. Factory Schmactory.
        ActorMaker.makeActors(actors, this, space);

        new RandPermuteActorListener(gl.time, 1, gl.people);
//        new ArrayListActorListener(gl.time, 1, gl.people);

        if (!gl.singleActor) {
//            new ArrayListActorListener(gl.time, 1, gl.firms);
            new RandPermuteActorListener(gl.time, 1, gl.firms);
//            new OnlyOnePerDayActorListener(gl.time, 1, gl.firms);
        }


        //HACKEROO!!! Couldn't do this before because the actors hadn't been made
        if (gl.peopleFixWage > 0) {
            gl.peopleFixWage(gl.peopleFixWage);
        }

        System.out.println(" ");

        //this was in gl.writeData test before. Using for viz now, testing...
        ds = new DataStore(gl.time, gl.dataWriteGap, 1);


        //give gl a reference for others to use
        gl.mainDataStore = ds;

        //Need specific coords variableStore so I can make sure I'm not having
        //To have two copies of the coords (one a publicly accessible x and y double)
        //Can lead to confusion elsewhere!
        ds.addVariableStore(new VariableStoreCoords(gl.people, "x", "people_x"));
        ds.addVariableStore(new VariableStoreCoords(gl.firms, "x", "firm_x"));
        ds.addVariableStore(new VariableStoreCoords(gl.people, "y", "people_y"));
        ds.addVariableStore(new VariableStoreCoords(gl.firms, "y", "firm_y"));

        ds.addVariableStore(new VariableStore(gl.people, "maxUtility", "utility"));
        ds.addVariableStore(new VariableStore(gl.people, "chosenDensityCost", "density cost"));
        ds.addVariableStore(new VariableStore(gl.people, "distanceToCentralFirm", "distance to centre"));
//        ds.addVariableStore(new VariableStore(gl.people, "numPeopleInDensityCostRadius", "num people in density cost radius"));
////        ds.addVariableStore(new VariableStore(gl.people, "meanDistanceOfPeopleInDensityCostRadius", "mean distance, people in density cost radius"));
//        ds.addVariableStore(new VariableStoreMeanSD(gl.people, "distanceMovedToday", "mean distance moved today"));
////        ds.addVariableStore(new VariableStoreMeansOnlyTrigger(gl.people, "distanceMovedToday", "mean distance moved today"));
        //and mean version
//        ds.addVariableStore(new VariableStoreMeanSD(gl.people,
//                "distanceToCentralFirm", "mean distance to central Firm"));
        ds.addVariableStore(new VariableStoreMeanSD(gl.people,
                "maxUtility", "utility mean"));
        ds.addVariableStore(new VariableStoreMeanSD(gl.people,
                "chosenDensityCost", "density cost mean"));
//        ds.addVariableStore(new VariableStoreMeanSD(gl.people, "distanceToCentralFirm", "mean distance to centre"));
        //version when there are two Firms. 
//        ds.addVariableStore(new VariableStoreMeanSD(gl.people, "distanceToCentralFirm", "mean distance to Firm 0"));

        //OK, I've put the public x,y back into Firm. Stupid system! Easiest way to get reflections stuff to work
        //Just to make clear name of first varstore isn't used...
        ds.addVariableStore(new VariableStoreMeanFollow(gl.firms, "x",
                "firm follow x",
                gl.mainDataStore.getVariableStoreByVariableLabel("firm_x"),
                gl.followAverage));
        ds.addVariableStore(new VariableStoreSDFollow(gl.firms, "x",
                "firm follow x sd",
                gl.mainDataStore.getVariableStoreByVariableLabel("firm_x"),
                gl.followAverage));

//        ds.addVariableStore(new VariableStore(gl.people, "firm0demand", "firm 0 demand"));
//        ds.addVariableStore(new VariableStore(gl.people, "firm1demand", "firm 1 demand"));
//
//
//        //and for People's demand from each firm (for two-firm hotelling-esque model.)
//        ds.addVariableStore(new VariableStoreMeanFollow(gl.people, "firm0demand",
//                "demand0followmean",
//                gl.mainDataStore.getVariableStoreByVariableName("firm0demand"),
//                gl.followAverage));
//        ds.addVariableStore(new VariableStoreMeanFollow(gl.people, "firm1demand",
//                "demand1followmean",
//                gl.mainDataStore.getVariableStoreByVariableName("firm1demand"),
//                gl.followAverage));
//
//        //EoS difference to markup
//        ds.addVariableStore(new VariableStoreFirm1PriceDiffFromEquilibrium(gl.firms,
//                "priceDiff", "firm 1 price difference"));
//
//        ds.addVariableStore(new VariableStoreMeanFollow(gl.people, "priceDiffFollow",
//                "firm 1 price difference average",
//                gl.mainDataStore.getVariableStoreByVariableName("priceDiff"),
//                gl.followAverage));




//        ArrayList glHolder = new ArrayList();
//        glHolder.add(new gl());
//        ds.addVariableStore(new VariableStore(glHolder, "deliveryCost", "delivery cost"));

//        VariableStoreTwoFirmRatioTrigger trigger = new VariableStoreTwoFirmRatioTrigger(gl.firms,
//                "goodCost", "Firm good cost ratio");
        //Boolean: if true, highest value is always ratio numerator
        //so ratio is always > 1
//        ds.addVariableStore(new VariableStoreTwoFirmRatio(gl.firms,
//                "productionLevel", "Firm efficiency ratio", true));
//        VariableStoreMarkupCrashTrigger trigger = new VariableStoreMarkupCrashTrigger(gl.firms,
//                "countPositiveGoodCostChange");

//        ds.addVariableStore(trigger);

        //to store data from the hacked trigger variable store above
//


        //add the data that will go into the graph
        //subset of one firm for each; needs to take ArrayList, sorry...!
        //Note 'hacking for England' in gl!
        if (gl.firms.size() == 2) {
            gl.firm0 = new ArrayList();
            gl.firm0.add(gl.firms.get(0));
            gl.firm1 = new ArrayList();
            gl.firm1.add(gl.firms.get(1));
        }

//        triggerData.addVariableStore(new VariableStoreOneAgentOnly(gl.firm0, "goodCost", "Firm 0 good cost"));
//        triggerData.addVariableStore(new VariableStoreOneAgentOnly(gl.firm1, "goodCost", "Firm 1 good cost"));
//        ds.addVariableStore(new VariableStoreOneAgentOnly(gl.firm0, "goodCost", "Firm 0 good cost"));
//        ds.addVariableStore(new VariableStoreOneAgentOnly(gl.firm1, "goodCost", "Firm 1 good cost"));
//        triggerData.addVariableStore(new VariableStoreTwoFirmPriceRatio(gl.firms, "lastGoodCostAtMarkupChange", "Firm 0 / Firm 1 good cost ratio"));
//        
//        ArrayList glHolder = new ArrayList();
//        glHolder.add(new gl());
//        triggerData.addVariableStore(new VariableStore(glHolder, "rho", "rho"));
//
//        glHolder = new ArrayList();
//        glHolder.add(new gl());
//        triggerData.addVariableStore(new VariableStore(glHolder, "deliveryCost", "distance cost"));

//        triggerData.addVariableStore(new VariableStoreMeansOnly(gl.firms, "goodCost", "good cost"));
        //bespoke, for making 'cost of money in time' from wage offer
//        triggerData.addVariableStore(new VariableStoreMeansOnlyCostofMoney(gl.firms, "wageOffer", "cost of money"));
//        triggerData.addVariableStore(new VariableStore(glHolder, "rho"));
//        triggerData.addVariableStore(new VariableStoreMeansOnly(gl.people,
//                "distanceToCentralFirm", "mean distance to low-cost / from high-cost"));


//        new ActorParamSweeper(trigger, 1, 0.005, gl.firms);
        //add another audible
//        aps.giveShouterTo(trigger, paramPos);

//        ShouterParamSweeper sps = new ShouterParamSweeper(gl.time, gl.followAverage, 0.001, gl.firms);

//        DataStore triggerData = new DataStore(sps, 1, 1);

//        triggerData.addVariableStore(new VariableStoreMeanFollow(gl.people, "priceDiffFollow",
//                "firm 1 price difference average",
//                gl.mainDataStore.getVariableStoreByVariableName("priceDiff"),
//                gl.followAverage));
//
//        ArrayList glHolder = new ArrayList();
//        glHolder.add(new gl());
//        triggerData.addVariableStore(new VariableStore(glHolder, "EoS_Curve", "economies of scale curve"));
//
//        triggerData.addVariableStore(new VariableStoreOneAgentOnly(gl.firm0, "goodCost", "Firm 0 good cost (markup)"));
//        triggerData.addVariableStore(new VariableStoreOneAgentOnly(gl.firm1, "goodCost", "Firm 1 good cost (dependent)"));

//        ds.addVariableStore(new VariableStore(gl.people, "currentContributedTime"));
//        ds.addVariableStore(new VariableStore(gl.firms, "yesterdayRevenue"));




//            ds.addVariableStore(new VariableStore(gl.people, "ratioOfWageSpentOnNearestProducer", "ratio spent on nearest firm"));
        ds.addVariableStore(new VariableStore(gl.firms, "stock"));
        ds.addVariableStore(new VariableStore(gl.firms, "demandFromOnePersonForCESTest", "demand"));
        ds.addVariableStore(new VariableStore(gl.firms, "goodCost", "good cost"));
        ds.addVariableStore(new VariableStore(gl.firms, "currentContributedTime", "contributed time"));
        ds.addVariableStore(new VariableStore(gl.firms, "Output_per_unitTime", "output per unit time"));
        ds.addVariableStore(new VariableStore(gl.firms, "stockDiffBetweenDays"));
        ds.addVariableStore(new VariableStore(gl.firms, "productionLevel", "output per unit input"));
//            ds.addVariableStore(new VariableStoreOneAgentOnly(gl.firm0, "goodCost", "Firm 0 good cost"));
//            ds.addVariableStore(new VariableStoreOneAgentOnly(gl.firm1, "goodCost", "Firm 1 good cost"));

//            ds.addVariableStore(new VariableStore(gl.firms, "stockMinusStockDifference"));

        //there's now no need for this double layering, really! Just not sure time well spent
        //refactoring
//            new TimeSeriesGraphxxx(gl.firms, "yesterdayRevenue", 650, 300,1);
        int follow = 1000;
//            int x = 1200;
        int x = 600;
        gl.timeSeriesGraphWidth = x;
        //golden ratio!
//            int y = (int) ((double) x / 1.61803398875);
        //I don't like the golden ratio!
        int y = (int) ((double) x * 0.5);
//            int y = (int) ((double) x * 0.666);
        //thinner version to fit three on page. 
//        int y = (int) ((double) x * 0.4);
        gl.timeSeriesGraphHeight = y;

//            new TimeSeriesGraph(ds.getVariableStoreByVariableName("ratioOfWageSpentOnNearestProducer"), x, y, 1, follow, 6);
//            new TimeSeriesGraph(ds.getVariableStoreByVariableName("stock"), x, y, 1, follow, 6);
//        new TimeSeriesGraph(ds.getVariableStoreByVariableLabel("mean distance to Firm 0"), x, y, 1, follow, 6);
//        new TimeSeriesGraph(ds.getVariableStoreByVariableLabel("mean distance to centre"), x, y, 1, follow, 6);
        new TimeSeriesGraph(ds.getVariableStoreByVariableLabel("good cost"), x, y, 1, follow, 6);
//            new TimeSeriesGraph(ds.getVariableStoreByVariableLabel("firm 1 price difference"), x, y, 1, follow, 6);
//            new TimeSeriesGraph(ds.getVariableStoreByVariableLabel("firm 1 price difference average"), x, y, 1, follow, 6);
//            new TimeSeriesGraph(ds.getVariableStoreByVariableLabel("demand"), x, y, 1, follow, 6);
//        new TimeSeriesGraph(ds.getVariableStoreByVariableLabel("output per unit input"), x, y, 1, follow, 6);
//        new TimeSeriesGraph(ds.getVariableStoreByVariableLabel("Firm efficiency ratio"), x, y, 1, follow, 6);
//            new TimeSeriesGraph(ds.getVariableStoreByVariableLabel("Firm 0 good cost"), x, y, 1, follow, 6);
//            new TimeSeriesGraph(ds.getVariableStoreByVariableLabel("Firm 1 good cost"), x, y, 1, follow, 6);
//            new TimeSeriesGraph(ds.getVariableStoreByVariableLabel("output per unit time"), x, y, 1, follow, 6);

//            new TimeSeriesGraph(ds.getVariableStoreByVariableName("stockMinusStockDifference"), 650, 300, 1, follow, 6);
//            new TimeSeriesGraph(ds.getVariableStoreByVariableName("stockDiffBetweenDays"), x, y, 1, follow, 6);
//        new TimeSeriesGraph(ds.getVariableStoreByVariableLabel("utility"), x, y, 1, follow, 6);
//            new TimeSeriesGraph(ds.getVariableStoreByVariableLabel("utility mean"), 650, 300, 1, 5001, 6);
//            new TimeSeriesGraph(ds.getVariableStoreByVariableLabel("mean distance moved today"), 650, 300, 1, 5001, 6);
//            new TimeSeriesGraph(ds.getVariableStoreByVariableLabel("mean distance to central Firm"), 650, 300, 1, 5001, 6);
//            new TimeSeriesGraph(ds.getVariableStoreByVariableLabel("distance to centre"), 650, 300, 1, 250, 6);
//            new TimeSeriesGraph(ds.getVariableStoreByVariableLabel("yesterdayRevenue"), 650, 300, 1, 6);
//            new TimeSeriesGraphxxx(gl.people, "maxUtility", 650, 300,1);
//            Person p = (Person) gl.people.get(0);
//            p.maxUtility;

//            new CorrelateArraysGraph(ds.getVariableStoreByVariableName("distanceToCentralFirm"),
//                    //            new CorrelateArraysGraph(ds.getVariableStoreByVariableName("maxUtility"),
//                    ////            new CorrelationGraph(ds.getVariableStoreByVariableName("maxUtility"),
//                    ds.getVariableStoreByVariableName("chosenDensityCost"),
//                    800, 800, 1, 10, 6);


        double[] setLow = {0, 0};
        double[] setHigh = {1, 0.6};
        boolean screenDisplay = true;
        //version including set values
        //last boolean flag: if true, distribute across screen, two stacked vertically, starting on right
        if (true) {
//                new CorrelateArraysGraph(ds.getVariableStoreByVariableLabel("demand"),
            //            new CorrelateArraysGraph(ds.getVariableStoreByVariableLabel("firm_x"),
//            new CorrelateArraysGraph(
//                    ds.getVariableStoreByVariableName("distanceToCentralFirm"),
//                    //                        ds.getVariableStoreByVariableName("maxUtility"),
//                    //                    ds.getVariableStoreByVariableName("currentContributedTime"),
//                    ds.getVariableStoreByVariableName("chosenDensityCost"),
//                    //                    540, 540, 1, 10, 6, setLow, null);
//                    //                    900, 900, 1, 10, 6, setLow, null, true);
//                    //                        1200, 1200, 1, 100, 6, null, null, false);
//                    //                        1600, 1600, 1, 10, 6, setLow, null, false);
//                    800, 800, 1, 10, 6, setLow, null, true);
            new CorrelateArraysGraph(
                    ds.getVariableStoreByVariableName("distanceToCentralFirm"),
                    ds.getVariableStoreByVariableName("maxUtility"),
                    //                    ds.getVariableStoreByVariableName("currentContributedTime"),
                    //                                            ds.getVariableStoreByVariableName("chosenDensityCost"),
                    //                    540, 540, 1, 10, 6, setLow, null);
                    //                    900, 900, 1, 10, 6, setLow, null, true);
                    //                        1200, 1200, 1, 100, 6, null, null, false);
                    //                        1600, 1600, 1, 10, 6, setLow, null, false);
                    800, 800, 1, 10, 6, setLow, null, true);
        }
//
//            new CorrelateArraysGraph(ds.getVariableStoreByVariableName("distanceToCentralFirm"),
//                    //            new CorrelateArraysGraph(ds.getVariableStoreByVariableName("maxUtility"),
//                    //                                ds.getVariableStoreByVariableName("maxUtility"),
//                    ds.getVariableStoreByVariableName("chosenDensityCost"),
//                    //                    540, 540, 1, 10, 6, setLow, null);
//                    //                    900, 900, 1, 10, 6, null, null, true);
//                    900, 900, 1, 10, 6, setLow, null, screenDisplay);
//
//            new CorrelateArraysGraph(ds.getVariableStoreByVariableName("distanceToCentralFirm"),
//                    //            new CorrelateArraysGraph(ds.getVariableStoreByVariableName("maxUtility"),
//                    //                                ds.getVariableStoreByVariableName("maxUtility"),
//                    ds.getVariableStoreByVariableName("numPeopleInDensityCostRadius"),
//                    //                    540, 540, 1, 10, 6, setLow, null);
//                    //                    900, 900, 1, 10, 6, null, null, true);
//                    900, 900, 1, 10, 6, setLow, null, screenDisplay);

//            new CorrelateArraysGraph(ds.getVariableStoreByVariableName("distanceToCentralFirm"),
//                    //            new CorrelateArraysGraph(ds.getVariableStoreByVariableName("maxUtility"),
//                    //                                ds.getVariableStoreByVariableName("maxUtility"),
//                    ds.getVariableStoreByVariableName("meanDistanceOfPeopleInDensityCostRadius"),
////                    540, 540, 1, 10, 6, setLow, null);
//                    900, 900, 1, 10, 6, null, null, true);
//                    900, 900, 1, 10, 6, setLow, null, true);



//            new CorrelateSingleVarsGraph(ds.getVariableStoreByVariableLabel("mean distance to centre"),


//            new CorrelateSingleVarsGraph(gl.time,
//                    ds.getVariableStoreByVariableLabel("utility standard deviation"),
////                    ds.getVariableStoreByVariableLabel("mean distance to low delivery cost Firm"),
//                    ds.getVariableStoreByVariableLabel("rho"),
//                    400, 400, 1, 6, null, null);
//        if (false) {
//            new CorrelateSingleVarsGraph(sps,
//                    //                        triggerData.getVariableStoreByVariableName("markUp"),
//                    //                        triggerData.getVariableStoreByVariableName("lastGoodCostAtMarkupChange"),
//                    //                        triggerData.getVariableStoreByVariableLabel("economies of scale curve"),
//                    triggerData.getVariableStoreByVariableLabel("Firm 0 good cost (markup)"),
//                    //                    triggerData.getVariableStoreByVariableLabel("commute cost"),
//                    //                                        triggerData.getVariableStoreByVariableLabel("delivery cost"),
//                    //                    triggerData.getVariableStoreByVariableLabel("cost of money"),
//                    //                        triggerData.getVariableStoreByVariableLabel("firm 1 price difference average"),
//                    triggerData.getVariableStoreByVariableLabel("Firm 1 good cost (dependent)"),
//                    //                    800, 800, 1, 6, setLow, setHigh);
//                    1200, 1200, 1, 6, null, null);
////                                                800, 800, 1, 6, null, null);
////                        700, 700, 1, 6, null, null);
//        }

        //Use followWidth negative values to mark start point for data
        //To avoid starting randomnesses
        if (false) {
            new CorrelateSingleVarsGraph(gl.time,
                    //                        triggerData.getVariableStoreByVariableName("markUp"),
                    //                        triggerData.getVariableStoreByVariableName("lastGoodCostAtMarkupChange"),
                    //                    ds.getVariableStoreByVariableLabel("Firm 0 good cost"),
                    ds.getVariableStoreByVariableLabel("mean distance to Firm 0"),
                    ds.getVariableStoreByVariableLabel("Firm efficiency ratio"),
                    //                    triggerData.getVariableStoreByVariableLabel("commute cost"),
                    //                                        triggerData.getVariableStoreByVariableLabel("delivery cost"),
                    //                    triggerData.getVariableStoreByVariableLabel("cost of money"),
                    //                    ds.getVariableStoreByVariableLabel("Firm 1 good cost"),
                    //                    800, 800, 1, 6, setLow, setHigh);
                    //                        1200, 1200, 1, 6, null, null);
                    //                    800, 800, 1, 6, null, null);
                    600, 600, 1, -20, 6, null, null);
//                    1200, 1200, 1, -20, 6, null, null);
        }


//            trigger.sortListenersByWeight();
//            trigger.checkShouterOrder();

//            new CorrelateArraysGraph(ds.getVariableStoreByVariableName("distanceToCentralFirm"),
//                    ds.getVariableStoreByVariableName("chosenDensityCost"),                    
//                    600, 500, 1, 250, 6);
//            new ThreeDCorrelationGraph(ds.getVariableStoreByVariableName("maxUtility"),
//                    ds.getVariableStoreByVariableName("chosenDensityCost"),
//                    ds.getVariableStoreByVariableName("distanceToCentralFirm"),
//                    600, 500, 1, 250, 6);




        //Visualiser elements knocked together in Visualiser's constructor
        //Note: order important. If this is before actors made, actor arrays not created
        //Also some elements of Visualiser will look for variable names
        //so needs to be after variable Stores are added
        drawables.add(new Visualiser());

        new ProcessingSpaceDrawer(drawables, gl.vizWidth, gl.vizWidth, gl.time, gl.dataWriteGap, 5);


        //USING REFLECTION TO SET UP METHODS VIA CONFIG FILE (FROM ARRAY CREATED IN GL)
        //object and method name
        String[] objMethodName = new String[2];
        //method inputs
        ArrayList<String> inputs = new ArrayList<String>();

        StringTokenizer st;

        for (String[] pair : gl.configFilePairs) {

            inputs.clear();

            //separate object and method name
            st = new StringTokenizer(pair[0], ".");

            objMethodName[0] = st.nextToken();
            objMethodName[1] = st.nextToken();

            //get method inputs
            st = new StringTokenizer(pair[1], ",");

            while (st.hasMoreTokens()) {
                inputs.add(st.nextToken());
            }

            //Reflections that mofo
            //Quite a lot of hackery going on at the mo
            try {

                Class cls = Class.forName("location2012.geog." + objMethodName[0]);
                Class partypes[] = new Class[inputs.size()];
                partypes[0] = ArrayList.class;
                partypes[1] = Double.TYPE;

                try {
                    partypes[2] = Double.TYPE;
                } catch (Exception e) {
                }

                Method meth = cls.getMethod(objMethodName[1], partypes);

                Object arglist[] = new Object[inputs.size()];

                try {

                    Class cls2 = Class.forName("location2012.utils.gl");
                    Field fld = cls2.getField(inputs.get(0).toString());

                    arglist[0] = (ArrayList<Actor>) fld.get(fld);
//                    System.out.println(fld.getName().toString() + " = " + fld.getBoolean(cls2));

                } catch (Throwable e) {

                    System.err.println("pah! " + e);
                }

                arglist[1] = Double.parseDouble(inputs.get(1));

                try {
                    arglist[2] = Double.parseDouble(inputs.get(2));
                } catch (Exception e) {
                }

                meth.invoke(null, arglist);

            } catch (Throwable e) {
                System.err.println("x: " + e);
            }

        }//end for configFilePairs


        //add minmaxfinders
//        new MinMaxBetweenDays(gl.time, 1, 0, gl.people, "maxUtility");


        if (true) {
            switch (gl.space) {

                case Line:

//                    space.putActorsInAHorizontalLine(gl.people);
//                    space.putActorsOnThisPoint(gl.firms, new Point2D.Double(gl.width / 2, 0));
                    SpaceTools.putActorsInAHorizontalLineStartEnd(gl.firms, 0, 1);
//                    space.putActorsInAHorizontalLineStartEnd(getArrayListSubset.GETRANGE(gl.firms, 0, (gl.firms.size() / 2) - 1), 0, 0.5);
//                    space.putActorsInAHorizontalLineStartEnd(gl.firms, 0.1, 2);
                    //version for firm circle game test
                    //You'd think it should be /2 not /4. I think I'm laying them out wrong. No time to fix. Hackeroo strikes again! Boing.
//                    space.putActorsInAHorizontalLineStartEnd(gl.people, 0.5 - (FirmsCircleGameBucket.circleSize / 4), 0.5 + (FirmsCircleGameBucket.circleSize / 4));
                    SpaceTools.putActorsInAHorizontalLineStartEnd(gl.people, 0, 1);
//                    space.putActorsOnThisPoint(gl.people, new Point2D.Double(0, 0));
//                    space.putActorsOnThisPoint(gl.people, new Point2D.Double(gl.width / 2, 0));
//                    space.putActorsInAHorizontalLineStartEnd(getArrayListSubset.GETRANGE(gl.firms, gl.firms.size() / 2, gl.firms.size() - 1), 0.1, 1.9);
//                    space.putActorsInACircle(gl.people, 0.1);

                    break;

                case Torus:

                    //two circles, half number of each lot of firms
//                    space.putActorsInACircleWithCentreAt(getArrayListSubset.GETRANGE(gl.firms, 0, (gl.firms.size()/2)-1), 0.2, 0.2, 0.5);
//                    space.putActorsInACircleWithCentreAt(getArrayListSubset.GETRANGE(gl.firms, gl.firms.size()/2, gl.firms.size()-1), 0.1, 0.8, 0.5);

//                    SpaceTools.putActorsInACircle(gl.firms, 0);
//                    gl.firms.get(0).setx(0.1);
//                    space.putActorsInAHorizontalLineStartEndVertical(getArrayListSubset.GETRANGE(gl.firms, 0, (gl.firms.size() / 2) - 1), 0.4, 0.6, 0.4);
//                    space.putActorsInAHorizontalLineStartEndVertical(getArrayListSubset.GETRANGE(gl.firms, gl.firms.size() / 2, gl.firms.size() - 1), 0.2, 0.8, 0.6);
//                    space.putActorsInAHorizontalLineVerticalPos(getArrayListSubset.GETRANGE(gl.firms, 0, (gl.firms.size()/2)-1), 0.3);
//                    space.putActorsInAHorizontalLineVerticalPos(getArrayListSubset.GETRANGE(gl.firms, gl.firms.size()/2, gl.firms.size()-1), 0.7);
//                    space.putActorsInAHorizontalLineStartEnd(getArrayListSubset.GETRANGE(gl.firms, 0, (gl.firms.size()/2)-1), 0, 0.5);
//                    space.putActorsInAHorizontalLineStartEnd(gl.people, 0.1, 0.9);
//                    space.putActorsInAHorizontalLineStartEnd(getArrayListSubset.GETRANGE(gl.firms, gl.firms.size()/2, gl.firms.size()-1), 0.7, 0.8);
//                    space.putActorsInACircle(gl.people, 0.4);
//                    if (gl.firms.size() == 1) {
//                        space.putActorsInAHorizontalLineStartEnd(gl.firms, 0.5, 0.5);
//                    } else if (gl.firms.size() == 2) {
//                        space.putActorsInAHorizontalLineStartEnd(gl.firms, 0.3, 0.7);
//                    } else {
                    //CES test: 1 person on left, nine Firms from 0.1 to 1.
//                        space.putActorsInAHorizontalLineStartEnd(gl.firms, 0, 1);
//                        space.putActorsInAHorizontalLineStartEnd(gl.people, 0, 0);
//                        space.putActorsInAHorizontalLineStartEnd(gl.firms, 0, 1);
//                    space.putActorsInAHorizontalLineStartEnd(gl.people, 0, 1);
//                    space.putActorsInAHorizontalLineStartEnd(gl.firms, 0.25, 0.75);
//                        space.putActorsInAHorizontalLineStartEnd(gl.people, 0.25, 0.75);
                    space.putActorsOnThisPoint(gl.firms, new Point2D.Double(gl.width / 2, gl.width / 2));
//                    }
//                    space.putActorsInAHorizontalLine(gl.people);
//                    space.putActorsInsideCircleAreaRandomly(gl.people, 0.4);

                    //firm circle game specific
//                    space.putActorsInACircle(gl.people, FirmsCircleGameBucket.circleSize / 2);
//                    SpaceTools.putActorsInACircle(gl.firms, 0.4);
//                    space.putActorsInCircleRandomRadiusAndAngle(gl.people, 0.3);
//                    space.putActorsInsideCircleAreaRandomly(gl.people, 0.1);
//                    space.putActorsInsideCircleAreaRandomly(gl.firms, 0.1);
//                    space.putActorsOnThisPoint(gl.firms, new Point2D.Double(gl.width / 2, gl.width / 2));
//                    space.putActorsOnThisPoint(gl.people, new Point2D.Double(gl.width / 2, gl.width / 2));
//                    space.putActorsRandomlyInAreaOfCircle(gl.firms, 0.2);

                    break;

            }//end switch
        }//end if 

        for (int i = 0; i < gl.firms.size(); i++) {

            System.out.println("Firm loc for " + i + ": " + gl.firms.get(i).getx());

        }


        //Needs to be a copy so they keep the same order, for datawriting
        ArrayList<Actor> actorCopy = new ArrayList<Actor>();
        actorCopy = (ArrayList<Actor>) actors.clone();
        //ds.addVariableStore(new CESOptTestBucket("CES_Opt_test", cesWrap, modelRunDays));
        //turning off buckets for now...


        //add a data outputter for the end of the model
        //Currently doesn't need any shout value - called directly when the
        //model's days have ended
        if (gl.writeData && !gl.writeMultiRunOnly) {
            CSVWriter writer = new CSVWriter(ds, gl.time, 0);
        }

//        gl.time.checkShouterOrder();
        //Run world - time closes things down when done.
        //modelRunning is set to false in the timeline when a single run finishes. 
        gl.modelRunning = true;

        gl.time.sortListenersByWeight();
        gl.time.checkShouterOrder();

        while (gl.modelRunning) {
            gl.time.nextDay();
        }
    }

    private void DataRead() {

        gl.setSpace(gl.SpaceType.Torus);

        System.out.println("Space used: " + gl.space);

        /**
         * *
         * SETTING UP THE MODEL *
         */
        //General output switch
        p.print(1);
        //Actor decision output switch
        p.agent(0);

        //set actor ref to look at
        gl.LOOKATME = 0;

        //Make actors home
        actors = new ArrayList<Actor>();

        System.out.println("width is now: " + gl.width);

        //Width will be set in DataReader
        SpaceTools sp = new SpaceTools(0);
//        drawables.add(sp);


        //Hard-coding creation of actors in ActorMaker. Factory Schmactory.
        //ActorMaker.makeActors(actors, this, sp);

        //Make a gl.gl.timeline
        gl.time = new Timeline(gl.modelRunDays);

        //create the reader and listener -
        //will read data into arrays,
        //Create dummy agents and
        //when called will set agents vars to the correct data
        //ready to be used as dummies for visualisation etc
        DataReader reader = new DataReader(actors, this, sp);

        //Set data write days to the number of days - may change of model run
        //is killed early
        gl.dataWritePointNumber = gl.modelRunDays;

        //last int is weight
        new ProcessingSpaceDrawer(drawables, 750, 750, gl.time, 1, 2);

        //add a data outputter for the end of the model
        //Currently doesn't need any shout value - called directly when the
        //model's days have ended
        //CSVWriter writer = new CSVWriter(ds, gl.time, 0);
        gl.time.sortListenersByWeight();
        gl.time.checkShouterOrder();

        //Run world - gl.time closes things down when done.


        while (true) {
            gl.time.nextDay();

        }


    }//end method DataRead

    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) {

        new Main();


    }
}
/*
 * cuttinz
 */
//double addition and subtraction test
//        double tester = Math.sqrt(Math.pow(3.12345, 5));
//        double add = 0;
//
//        //now add to zero, then subtract precisely the same number of times
//        for(int i = 0; i < 100; i++) {
//            add += tester;
//        }
//
//        for(int i = 0; i < 100; i++) {
//            add -= tester;
//        }
//
//        System.out.println("add: " + add);
//
//test
//        for (int i = 0; i < 100; i++) {
//
//            double c = Randoms.rangeDouble(10)-5;
//
//            System.out.print(c +", ");
//
//              if(c < 0) {
//            //find the modulus, will give us a value between -1 and 0.
//            c %= 1;
//            //subtract that from one (note, already negative number)
//            c = 1 + c;
//        } else if (c > 1) {
//            //find the modulus, will give us a value between 0 and 1.
//            c %= 1;
//            //add to zero... oh, don't need to!
//        }
//
//            System.out.println(c);
//
//        }
//Testing Reflections methods 
//System.out.println("Object: " + objMethodName[0] + ", method: " + objMethodName[1]);
//        System.out.print("Inputs: ");
//        
//        for(String s : inputs) {
//            System.out.print(s + " ");
//        }
//        
//        System.out.println(" ");