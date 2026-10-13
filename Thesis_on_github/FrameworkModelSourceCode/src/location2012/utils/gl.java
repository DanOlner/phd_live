package location2012.utils;

import location2012.econs.SmoothEoS;
import location2012.econs.ProductionShell;
import location2012.econs.CESOneTypeUtility;
import location2012.econs.UtilityShell;
import java.awt.Color;
import java.io.FileInputStream;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.Properties;
import location2012.Actor;
import location2012.Firm;
import location2012.observe.Timeline;
import location2012.control.ListenerWrapper;
import location2012.econs.CoreModelEoS;
import location2012.graphics.ProcessingSpaceDrawer;
import location2012.io.DataStore;

/**
 * gl = globals: used for quick global reference to cross-model variables.
 *
 * @author Olner Dan
 */
public class gl {
    
    //Hacking for England
    public static ArrayList firm0, firm1;
    //using to cut back the size of variableStores if the data isn't needed
    public static int globalFollowWidth;
    //used by Hotelling model to show data averaged over model time
    public static int followAverage = 2000;
    public static boolean markUpSingleFirmOnly = false;

    //Temp testing vars
    public static int MainVizTicks = 0;
    public static int graphVizTicks = 0;
    public static boolean someoneHasBaton = false;
    
    //hackeroo for CEUS
    public static boolean processingHasDrawn = false;

    /**
     *
     */
    public static enum SpaceType {

        /**
         * Err... does this appear in the javadoc then?
         */
        Point,
        /**
         *
         */
        TwoRegion,
        /**
         *
         */
        N_Region,
        /**
         * 1D continuous line. Control whether it's a wraparound 'racetrack'
         * with wrapSpace switch
         */
        Line,
        /**
         *
         */
        Torus;
    }

    //mode: model running or data reading?
    /**
     *
     */
    public static enum Mode {

        /**
         *
         */
        Model,
        /**
         *
         */
        DataRead;
    }

    /**
     *
     */
    public static enum GoodType {

        /**
         *
         */
        Necessity,
        /**
         *
         */
        Luxury;
    }

    /**
     * Optimiser: which utility optimising method to use?
     */
    public static enum Optimiser {

        /**
         * Greedy algorithm jiggles way to optima
         */
        Greedy,
        /**
         * Constrained optima: produces 'objective demand' for a given budget;
         * Can be used when stock is not a limiting factor
         */
        Constrained;
    }
    //some random vars
    public static int colCount = 0;
    private static ArrayList<Color> colours = new ArrayList<Color>();
    /*
     * configFilePairs: stores any remaining pairs of values from loaded
     * .properties files for use as method calls
     */
    public static ArrayList<String[]> configFilePairs = new ArrayList<String[]>();
    /**
     * Global ref to timeline
     */
    public static Timeline time;
    /**
     * Global ref to main DataStore
     */
    public static DataStore mainDataStore;
    /**
     * Note if it's a single actor run or not
     */
    public static boolean singleActor;
    /*
     * TWO REGION SETTINGS
     */
    /**
     * Used if two region space is used. Defines what the split of people and
     * firms will be between the two regions to start with. Between 0 and 1.
     * Let's designate the number 'population in region 0'
     */
    public static double TwoR_FirmPopRegZero;
    /**
     * For two region runs: sets initial firm population in region zero.
     */
    public static double TwoR_PeoplePopRegZero;
    /**
     * If two-region, useful to have a permanent record of the location of the
     * two regions being used by the code
     */
    public static double TwoR_Region0x;
    /**
     *
     */
    public static double TwoR_Region1x;
    //Density cost: the cost for each person in the unit circle
    //Currently making it so that if there's the average number of
    //people in the radius, it comes out as one.
    /**
     * ******************
     * SPACE COST SETTINGS ******************
     */
    /**
     *
     */
    public static double DENSITYCOST;
    /**
     *
     */
    public static double DENSITYCOSTRADIUS;
    //For changing... space cost?
    /**
     *
     */
    public static double SPACECOST;
    //Distance in the model - defined relative to whatever a day's distance
    //is at the start of the model
    /**
     *
     */
    public static double STARTSPACECOST;
    /**
     * If land cost agents (landlords) are being used, set this to > 0 Number
     * indicates amount of 'landlords' per unit width
     */
    public static int landCostsOn = 0;
    /**
     * ******************
     * MAIN MODEL SETTINGS ******************
     */
    /**
     * Width (1 unit equivalent to a day in the basic spacecost models)
     */
    public static double width;
    /**
     * Global ref to main viz PApplet width, set in ProcessingSpaceDrawer inner
     * class
     */
    public static double pappletWidth;
    /**
     * Global ref to main viz PApplet width, set in ProcessingSpaceDrawer inner
     * class
     */
    public static double pappletHeight;
    /**
     * Same for time series graphs
     */
    public static double timeSeriesGraphWidth;
    public static double timeSeriesGraphHeight;
    
   
    /**
     * Enum reference for setting space type
     */
    public static SpaceType space;
    /**
     * Switch: changes line to racetrack (and at some point plane to torus;
     * leaving as Torus for now.)
     */
    public static boolean wrapSpace = true;
    /**
     * Mode: either model or data. Latter used for visualisation previously
     * written data
     */
    public static Mode mode;
    /**
     * Optimisation method for utility, set with Enum
     */
    public static Optimiser optimiser;
    /**
     * Number of runs to go through (with new random seed for each.)
     */
    public static int numberOfRandomSeedChanges;
    /**
     * If this is being used, will be used in data
     */
    public static double paramSweepVal = -1;
    /**
     * currentRun: record of which one we're on
     */
    public static int currentRun = 0;
    /**
     * Iteration number for whole model run
     */
    public static int modelRunDays;
    /*
     * Number of iterations between data writing points
     */
    public static int dataWriteGap;
    /**
     * how many data days to write? May change if model is killed early
     * Otherwise, will be number of days
     */
    public static int dataWritePointNumber;
    /**
     * some awful hackery for writing optimisation data for graphing
     */
    public static boolean writePog = false;
    /*
     * If true, the default agent location options (set in Main) will be used.
     */
    public static boolean defaultSpaceAgentPositions = true;
    /*
     * Global control of data writing. Writing is turned off if e.g. the model
     * is running for an open-ended number of days
     */
    public static boolean writeData = true;
    public static boolean writeMultiRunOnly = false;
    /*
     * Reference to config File name used to set model run up. Will be used 
     * to write output files
     */
    public static String configFileName;
    /**
     * ******************
     * AGENT SETTINGS ******************
     */
    /**
     * Permanent reference to actor types
     */
    public static ArrayList<Actor> allActors = new ArrayList<Actor>();
    public static ArrayList<Actor> people = new ArrayList<Actor>();
    public static ArrayList<Actor> firms = new ArrayList<Actor>();
    public static ArrayList<Actor> goodSellers = new ArrayList<Actor>();
    public static ArrayList<Actor> landLords = new ArrayList<Actor>();
    /*
     * Total number of agents - both firms and people Can be overwritten by
     * group-specific settings
     */
    public static int numAgents;
    /**
     * Total number of actors - may have been changed by over-riding number of
     * created actors
     */
    public static int numTotalActors;
    /*
     * Number of people: over-rides number set with numAgents if > 0
     */
    public static int overRideNumPeople;
    /*
     * Number of firms: over-rides number set with numAgents if > 0
     */
    public static int overRideNumFirms;
    /**
     * If people's wages are being fixed, set this to more than zero
     */
    public static double peopleFixWage = 0;
    /**
     * If people's wages are being fixed, set this to more than zero
     */
    /**
     * Cost of commute. Distance is multiplied by this. See PersonAction
     */
    public static double commuteCost;
    public static double deliveryCost;
    public static double markUp;    
    public static double good;
    public static double wage;
    /**
     * If people's wages are not affected by distance set to false If false,
     * this means people will get a firm's direct wage offer
     */
    public static boolean peopleWageDistanceCost = true;
    /**
     * Single actor model: whether to target a stock level or utility
     */
    public static boolean stockTarget = true;
    /**
     * Firms: goods flow. If false, no goods will move when sales take place
     */
    public static boolean firmsGoodsFlowOn = true;
    /*
     * Firms: money flow. If false, no money will change hands when sales take
     * place
     */
    public static boolean firmsMoneyFlowOn = true;
    /**
     * Firms: if this is true, they get the delivery cost money paid by
     * consumers Otherwise, their income from a sale is just their own good
     * price times units bought
     */
    public static boolean goodSellersGetDeliveryCost = true;
    
    /**
     * Quick hack to get Firms working with the transmission model; switch in buyGoods
     */
    public static boolean transmissionFirms;
    
    /**
     * Level of economies of scale used by whatever production function we're
     * using
     */
    public static double EoS_Curve;
    public static double EoS_Magnitude;
    public static double rho;
    /**
     * LOOKATME: the ID of an actor used in various places for observing them.
     */
    public static int LOOKATME;
    public static int LOOKATFIRM;
    /**
     *
     */
    public static double BY_OPTIMUM;
    /**
     * Global record of the number of people Doesn't set the number of people -
     * this can be done with overRideNumPeople
     */
    public static int numPeople;
    /**
     * Global record of the number of firms Doesn't set the number of firms -
     * this can be done with overRideNumFirms
     */
    public static int numFirms;
    /**
     * If set to false, people/firms won't move. They will still optimise on the
     * spot they're currently on, however. This means they will only have the
     * one bundle.
     */
    public static boolean peopleMobile;
    public static boolean firmsMobile;
    /**
     * For adding noise to constrained utility
     */
    public static double noiseUtil;
    /**
     * If true, all firms are added to a Person's search (not limited by space)
     */
    public static boolean peopleConsiderAll = false;
    /**
     * Used for single actor LoV models: should I not buy from myself?
     */
    public static boolean excludeMe = false;
    /**
     * Set stock to zero on each turn if true
     */
    public static boolean setStockToZero = false;
    /**
     * **************
     * PROCESSING SWITCHES and settings switches set with keyboard in processing
     * viewer, used elsewhere **************
     */
    //changevals used for using the Processing window for changing values live
    //One for each mousekey
    public static double changeVal = 0;
    public static double changeVal1 = 0;
    /**
     * Width of main Visualisation window
     */
    public static int vizWidth = 500;
    /*
     * 2D or 3D view in ProcessingSpaceDrawer
     */
    public static boolean threeDview = false;
    /**
     * Controls whether Processing visualisations set up in Main are viewed or
     * not
     */
    public static boolean viewVis = false;
    /**
     * Show variable markers in 1d view?
     */
    public static boolean lineViewVars = false;
    /**
     * time delay between days. For slowing down model for visualisation
     */
    public static int timeDelay;
    public static int ID;
    /**
     * Sub-switch used by Space when drawing to size buy lines from people to
     * firms
     */
    public static float visFactorBuyLines;
    /**
     * Sub-switch used by Space when drawing utility square size and height
     */
    public static float visFactorUtility;
    /**
     * If true, highest and lowest utility levels on one turn will be used to
     * visualise the range in the next
     */
    public static boolean visUtilityRangeOn;
    /**
     * Draw firm one-day circles or not?
     */
    public static boolean drawFirmOneDayCircles = false;
    /**
     * Kills model run, saving data at that point
     */
    public static boolean killSwitch = false;
    //Switches between top and side view. Allows drawers to adjust accordingly.
    /**
     * Whether Space Drawer is viewed from top or a side-angle. Can be changed
     * with spacebar (with focus on Processing window.)
     */
    public static boolean topView = true;
    /**
     * 3d buylines
     */
    public static boolean reliefLines = true;
    /**
     * Turns rotation on and off in Space Drawer. Can be toggled with r (with
     * focus on Processing window.)
     */
    public static boolean rotateView = false;
    /**
     * If in data read mode, for making sure Processing has a chance to draw
     */
    public static boolean drawing = false;
    /*
     * If true, Processing space drawer will output jpeg frames
     */
    public static boolean outputJpegs = false;
    /**
     * String for setting output format for images
     */
    public static String outputFormat = "png";
    /**
     * Should jpegs be output on dataWriteDays?
     */
    public static boolean outputJpegsOnDataDays = false;
    /**
     * MouseButtonListeners used by Timeline to change values.
     */
    public static ListenerWrapper leftMouseButton;
    public static ListenerWrapper rightMouseButton;
    /**
     * ***************
     * MISC ***************
     */
    /**
     * Global reference to current day
     */
    public static int day;
    /**
     * Global reference to timeline finishing a single model run
     */
    public static boolean modelRunning;
    /**
     * Bit of hackery, hopefully will improve! Used for toggling between wage
     * and good delivery manipulation in Processing
     */
    public static boolean wageMouse = false;
    /**
     * Von thunen mode: assign one firm to each person, spread delivery costs
     */
    public static boolean vonThunenGoods = false;
    public static boolean vonThunenDelivery = false;
    public static boolean vonThunenWage = false;
    public static boolean vonThunenCommute = false;
    //spreads costs out in firms, multiples of base costs
    public static boolean commuteRange = false;
    public static boolean wageRange = false;
    public static boolean goodsRange = false;
    public static boolean deliveryRange = false;
    /**
     * Will be set to true if any of the four above are true
     */
    public static boolean vonThunenMode = false;
    public static boolean rangeMode = false;
    /**
     * Will colour Person utility boxes by Firm's colour, if Person tied to
     * specific Firm
     */
    public static boolean colourByFirm = false;
    /*
     * For using ProcessingSpaceDrawer to instruct all PApplets to write an image
     */
    public static boolean printingAll = false;
    /**
     * Switch for PeopleDrawToSpace to use to toggle variable assignment
     */
    public static boolean viewPeopleVar = true;
    public static boolean viewPeopleVar2 = true;
    public static boolean viewPeopleVar3 = false;

    /**
     * Used along with silencing shouters that call agents to pause screen
     */
    public static boolean pauseGraphs = false;
    //For setting the kind of space used in the model
    //See the enum at the top of this class
    /**
     *
     * @param sp
     */
    public static void setSpace(SpaceType sp) {
        space = sp;
    }

    /**
     *
     * @param m
     */
    public static void setMode(Mode m) {
        mode = m;
    }

    /**
     * Set up model via a *.properties file in the \config folder
     */
    public static void setUpFromPropertiesFile() {

        //REFLECTION GUBBINZ

        //Load name of .properties file from .init file
        Properties configFile = new Properties();

        try {
            configFile.load(new FileInputStream("configs/location2012.init"));
        } catch (Exception e) {
            System.err.println("file not found");
        }

        Enumeration keys = configFile.keys();

        //Filename should be the first element in the file. If there's more than one uncommented
        //This will not necessarily pick the first.
        Object key = keys.nextElement();
        Object value = configFile.get(key);

        configFileName = value.toString();
        //Note: http://www.rgagnon.com/javadetails/java-0438.html
        configFileName = configFileName.split("\\.")[0];

        //Load vars
        configFile = new Properties();

        try {
            configFile.load(new FileInputStream("configs/" + value.toString()));
        } catch (Exception e) {
            System.err.println("file not found");
        }

        //Get values from file, attach to variables
        keys = configFile.keys();
        System.out.println("LOADING VALS FROM " + configFileName + ".properties FILE (in more or less random order...)");
        System.out.println("----------------------------------");

        while (keys.hasMoreElements()) {

            key = keys.nextElement();
            value = configFile.get(key);

            //try boolean first (try catch nest 1)
            try {

                Class cls = Class.forName("location2012.utils.gl");
                Field fld = cls.getField(key.toString());
//                fld.setBoolean(cls, Boolean.parseBoolean(configFile.getProperty(key.toString())));
                fld.setBoolean(cls, Boolean.parseBoolean(value.toString()));

                System.out.println(fld.getName().toString() + " = " + fld.getBoolean(cls));

            } catch (Throwable e) {

//                System.err.println(key.toString() + "=" + value.toString() + " not a boolean");

                //try double (try catch nest 2)
                try {

                    Class cls = Class.forName("location2012.utils.gl");
                    Field fld = cls.getField(key.toString());
                    fld.setDouble(cls, Double.valueOf(value.toString()));

                    System.out.println(fld.getName().toString() + " = " + fld.getDouble(cls));

                } catch (Throwable f) {

//                    System.err.println(key.toString() + "=" + value.toString() + " not a double");

                    //try Integer
                    try {

                        Class cls = Class.forName("location2012.utils.gl");
                        Field fld = cls.getField(key.toString());
                        fld.setInt(cls, Integer.valueOf(value.toString()));

                        System.out.println(fld.getName().toString() + " = " + fld.getInt(cls));

                    } catch (Throwable g) {

//                        System.err.println(key.toString() + "=" + value.toString() + " not an int either");
                        //Try float
                        try {

                            Class cls = Class.forName("location2012.utils.gl");
                            Field fld = cls.getField(key.toString());
                            fld.setFloat(cls, Float.valueOf(value.toString()));

                            System.out.println(fld.getName().toString() + " = " + fld.getFloat(cls));

                        } catch (Throwable h) {
//                            System.err.println(key.toString() + "=" + value.toString() + " not a float either");


                            //try Enum
                            try {

                                Class cls = Class.forName("location2012.utils.gl");
                                Field fld = cls.getField(key.toString());

//                                System.err.println("Enum name: " + key.toString());
//                                System.err.println("Enum value: " + configFile.getProperty(key.toString()));
//                                System.err.println("field type:" + (Class<Enum>) fld.getType());

                                fld.set(cls, Enum.valueOf((Class<Enum>) fld.getType(), value.toString()));

                                System.err.println("Enum: " + fld.getName().toString() + " = "
                                        + fld.get((Class<Enum>) fld.getType()).toString());

                                //Store remaining as String pairs to be used elsewhere in object creation
                            } catch (Throwable i) {

                                //Store any remaining pairs for use later as method calls
//                                String[] s = {key.toString(), value.toString()};
//                                configFilePairs.add(s);                                
                                configFilePairs.add(new String[]{key.toString(), value.toString()});

                                System.err.println("method: " + key.toString() + ", inputs: " + value.toString());

                            }


                        }//close catch nest 4

                    }//close catch nest 3

                }//close catch nest 2

            }//close catch nest 1


        }//end while for iterating over .properties file

        System.out.println("----------------------------------");

        //VARS SET INTERNALLY
        //Some lovelee hackinz
        if (gl.vonThunenGoods || gl.vonThunenWage || gl.vonThunenDelivery || gl.vonThunenCommute) {
            gl.vonThunenMode = true;
        }

        if (gl.goodsRange || gl.wageRange || gl.deliveryRange || gl.commuteRange) {
            gl.rangeMode = true;
        }
        currentRun = 0;
        gl.time = new Timeline(gl.modelRunDays);
        //add mouseListenerwrappers now. They get called to change values; ProcessingSpaceDrawer decides
        //which actual VarChanger is changed...
        leftMouseButton = new ListenerWrapper(null, 1);
        rightMouseButton = new ListenerWrapper(null, 1);
//        leftMouseButton = new ListenerWrapper(time, 1);
//        rightMouseButton = new ListenerWrapper(time, 1);

        if (modelRunDays < dataWritePointNumber) {
            //otherwise, division by zero because these are integers.
            dataWriteGap = 1;
            dataWritePointNumber = modelRunDays;

        } else {
            dataWriteGap = modelRunDays / dataWritePointNumber;
        }
        //dataWritePointNumber = modelRunDays / dataWriteGap;
        System.out.println("dataWriteGap: " + dataWriteGap);

        //if zero, turn off...
        if (modelRunDays == 0) {
            writeData = false;
            outputJpegs = false;
        }

        //set model view
        ProcessingSpaceDrawer.hRotateVal = 0;

        if (!gl.topView) {
            ProcessingSpaceDrawer.vRotateVal = (float) ((Math.PI / 2.5));
        }
//        ProcessingSpaceDrawer.vRotateVal = 0;



//        gl.changeVal = 1;

        //General output switch
        p.print(0);
        //Actor decision output switch
        p.agent(0);

        /**
         * ************
         * MODEL SETUP
         */
        //Set the kind of space used
//        gl.setSpace(gl.SpaceType.Point);
//        setSpace(SpaceType.TwoRegion);
//        gl.TwoR_FirmPopRegZero = 0;
//        gl.TwoR_PeoplePopRegZero = 1;
//        width = 0.25;
//        numagents = 100;
//        setSpace(SpaceType.Line);
//        setSpace(SpaceType.Torus);
        numTotalActors = overRideNumFirms + overRideNumPeople;
        //can people move?
        //testing turning this into a method so a second one can take a %
//        peopleMobile(false);

        //Utility optimising method
        //noiseUtil: adding noise to the constrained opt to mimic the `natural' noise of the greedy al.
        optimiser = Optimiser.Constrained;
//        optimiser = Optimiser.Greedy;
//          new UtilityShell(new CESplusNecessityUtility(0.25));
        UtilityShell.u = new CESOneTypeUtility(rho);
//        UtilityShell.u = new CESPlusNecessityGoodUtility(1, 0.5, 1);
//        UtilityShell.u = new NecessityLuxuryUtility(0.5, 1);
//        1 unit input is always just itself - (x^p)^1/p = x
        //presuming 1 day input = 1 unit output
        //So backyard production is always just linear
        //This is assuming all agents, including people, can only produce one good
//        BY_OPTIMUM = 1;
        //Production
        ProductionShell.p = new SmoothEoS();
//        ProductionShell.p = new CoreModelEoS();

//        for (int i = 0; i < 60; i++) {
//            System.out.println(ProductionShell.giveOutput(i));
//        }

    }

    /**
     * One-stop shop for model-wide settings. Comments in the class describe
     * where the settings are used (if not, they're used in Main.) Alt-F7 for
     * 'find usages' in Netbeans a handy way of finding where they're used.
     *
     * Note that any post-model-creation tinkering with actors needs to happen
     * in Main's ModelRun method - e.g. placing firms in circles, setting up
     * actual data output classes etc.
     *
     * The aim here is just to bring together as many model-wide settings as
     * possible to avoid having to skip around.
     */
    public static void setUp() {

        //don't change this: true for all models run with setUp()
        //There's a separate setUpSingleActor() method.
        singleActor = false;

        /**
         * ***************
         * DATA AND OUTPUT SETUP ***************
         */
        numberOfRandomSeedChanges = 1;
        currentRun = 0;
        modelRunDays = 00;
        //Make a timeline
        gl.time = new Timeline(gl.modelRunDays);
        //add mouseListenerwrappers now. They get called to change values; ProcessingSpaceDrawer decides
        //which actual VarChanger is changed...
        leftMouseButton = new ListenerWrapper(time, 1);
        rightMouseButton = new ListenerWrapper(time, 1);

        //Number of data points to write. If higher than modelRunDays,
        //defaults to modelRunDays below
//        dataWritePointNumber = modelRunDays / 4;
        dataWritePointNumber = 1000;

        if (modelRunDays < dataWritePointNumber) {
            //otherwise, division by zero because these are integers.
            dataWriteGap = 1;
            dataWritePointNumber = modelRunDays;

        } else {
            dataWriteGap = modelRunDays / dataWritePointNumber;
        }
        //dataWritePointNumber = modelRunDays / dataWriteGap;
        System.out.println("dataWriteGap: " + dataWriteGap);

        //open Processing Visualisations?
        viewVis = true;
//        viewVis = false;
        lineViewVars = false;


        //output jpegs from space drawer?
        outputJpegs = false;
        writeData = true;
        //if true, hill climb data will be outputted for one Person on day 3
        //I can't remember what PoG stands for... possibly person optimisation graph, but I'm guessing!
        writePog = true;
        writeMultiRunOnly = false;
        outputJpegsOnDataDays = false;
        //if zero, turn off...
        if (modelRunDays == 0) {
            writeData = false;
            outputJpegs = false;
        }

        topView = false;
        reliefLines = false;
        //Used by space for size of person visualisation factors
        gl.visFactorBuyLines = 250f;
        gl.visFactorUtility = 50f;
        gl.visUtilityRangeOn = true;

        //set model view
        ProcessingSpaceDrawer.hRotateVal = 0;
        ProcessingSpaceDrawer.vRotateVal = (float) ((Math.PI / 2.5));
//        ProcessingSpaceDrawer.vRotateVal = 0;


        drawFirmOneDayCircles = false;

        //gl.changeVal = 1;

        //General output switch
        p.print(0);
        //Actor decision output switch
        p.agent(0);

        /**
         * ***************
         * MODEL SETUP ***************
         */
        //Set the kind of space used
//        gl.setSpace(gl.SpaceType.Point);
//        numagents = 200;
//        numagents = 200;
//        setSpace(SpaceType.TwoRegion);
//        gl.TwoR_FirmPopRegZero = 0;
//        gl.TwoR_PeoplePopRegZero = 1;
//        width = 0.25;
//        numagents = 100;
//        setSpace(SpaceType.Line);
        setSpace(SpaceType.Torus);
        width = 1;

        vonThunenGoods = true;
        vonThunenWage = false;
        colourByFirm = true;

        defaultSpaceAgentPositions = true;

        //if >0 landlord agents used to impose land cost
        //Number indicates amount of 'landlords' per unit width
        landCostsOn = 0;
        //Density cost! Yay
        DENSITYCOST = 1;
        //DENSITYCOSTRADIUS = width / 16;
        DENSITYCOSTRADIUS = (double) 1 / 16;

        commuteCost = 1;

        numAgents = 50;
        //set overRideNumFirms to zero for single-actor model
        //(Also change action type in ActorMaker)
        overRideNumFirms = 8;
        overRideNumPeople = 800;
        numTotalActors = overRideNumFirms + overRideNumPeople;
        //can people move?
        //testing turning this into a method so a second one can take a %
        peopleMobile(true);
        firmsMobile = false;
        //fix wages. If zero, not fixed
        peopleFixWage = 0;
        deliveryCost = 1;
        //should distance affect wage via time? False if no.
        peopleWageDistanceCost = false;
        //stop goods and money flow through firms
        firmsGoodsFlowOn = false;
        firmsMoneyFlowOn = false;
        goodSellersGetDeliveryCost = false;

        wrapSpace = false;

        peopleConsiderAll = true;

        //set actor ref to look at. (Each actor has a unique ID.)
        LOOKATME = 0;

        //Utility optimising method
        //noiseUtil: adding noise to the constrained opt to mimic the `natural' noise of the greedy al.
        gl.noiseUtil = 0;
        optimiser = Optimiser.Constrained;
//        optimiser = Optimiser.Greedy;
        //speed of visualisation: may need to slow down for constrained opt
        //in milliseconds
        timeDelay = 7;

        //Make global factor changer here, so it's changed before anything else acts
        //(Though I could fiddle with the weight, this is the easiest way of doing it for now...)
        //new GlobalSpaceCostChanger(time, 1);
        //SPACECOST = 1;
        //STARTSPACECOST = 0.5;
        //CESplusNecessityUtility: pass in rho
        //love of variety
        //0<rho<1
        //As rho -> 0, consumer is better off with a mix of goods.
//        new UtilityShell(new CESplusNecessityUtility(0.25));
        UtilityShell.u = new CESOneTypeUtility(0.5);
//        UtilityShell.u = new CESPlusNecessityGoodUtility(1, 0.5, 1);
//        UtilityShell.u = new NecessityLuxuryUtility(0.5, 1);
//        1 unit input is always just itself - (x^p)^1/p = x
        //presuming 1 day input = 1 unit output
        //So backyard production is always just linear
        //This is assuming all agents, including people, can only produce one good
        BY_OPTIMUM = 1;
        //Production
        ProductionShell.p = new SmoothEoS();

//        for (int i = 0; i < 60; i++) {
//            System.out.println(ProductionShell.giveOutput(i));
//        }

    }

    public static void setUpSingleActor() {

        //don't change this: true for all models run with setUp()
        //There's a separate setUpSingleActor() method.
        singleActor = true;

        /**
         * ***************
         * DATA AND OUTPUT SETUP ***************
         */
        numberOfRandomSeedChanges = 1;
        currentRun = 0;
        modelRunDays = 0;
        //Make a timeline
        gl.time = new Timeline(gl.modelRunDays);
        //add mouseListenerwrappers now. They get called to change values; ProcessingSpaceDrawer decides
        //which actual VarChanger is changed...
        leftMouseButton = new ListenerWrapper(time, 1);
        rightMouseButton = new ListenerWrapper(time, 1);

        //Number of data points to write. If higher than modelRunDays,
        //defaults to modelRunDays below
//        dataWritePointNumber = modelRunDays / 4;
        dataWritePointNumber = 2000;

        if (modelRunDays < dataWritePointNumber) {
            //otherwise, division by zero because these are integers.
            dataWriteGap = 1;
            dataWritePointNumber = modelRunDays;

        } else {
            dataWriteGap = modelRunDays / dataWritePointNumber;
        }
        //dataWritePointNumber = modelRunDays / dataWriteGap;
        System.out.println("dataWriteGap: " + dataWriteGap);

        //open Processing Visualisations?
        viewVis = true;
        lineViewVars = false;

        //set model view
        ProcessingSpaceDrawer.hRotateVal = 0;
        ProcessingSpaceDrawer.vRotateVal = (float) ((Math.PI / 2.5));
//        ProcessingSpaceDrawer.vRotateVal = 0;


        topView = false;
        reliefLines = false;
        //Used by space for size of person visualisation factors
        gl.visFactorBuyLines = 30f;
        gl.visFactorUtility = 40f;
        gl.visUtilityRangeOn = true;

        drawFirmOneDayCircles = false;



        //output jpegs from space drawer?
        outputJpegs = false;
        writeData = true;
        //if true, hill climb data will be outputted for one Person on day 3
        //I can't remember what PoG stands for... possibly person optimisation graph, but I'm guessing!
        writePog = true;
        writeMultiRunOnly = false;
        outputJpegsOnDataDays = false;
        //if zero, turn off...
        if (modelRunDays == 0) {
            writeData = false;
            outputJpegs = false;
        }


        //gl.changeVal = 1;

        //General output switch
        p.print(0);
        //Actor decision output switch
        p.agent(0);

        /**
         * ***************
         * MODEL SETUP ***************
         */
        //Set the kind of space used
//        gl.setSpace(gl.SpaceType.Point);
//        numagents = 200;
//        numagents = 200;
//        setSpace(SpaceType.TwoRegion);
//        gl.TwoR_FirmPopRegZero = 0;
//        gl.TwoR_PeoplePopRegZero = 1;
//        width = 0.25;
//        numagents = 100;
        setSpace(SpaceType.Line);
//        setSpace(SpaceType.Torus);
        width = 1;

        vonThunenGoods = false;
        vonThunenWage = false;
        colourByFirm = false;

        defaultSpaceAgentPositions = true;

        //if >0 landlord agents used to impose land cost
        //Number indicates amount of 'landlords' per unit width
        landCostsOn = 0;
        //Density cost! Yay
        DENSITYCOST = 0;
        //DENSITYCOSTRADIUS = width / 16;
        DENSITYCOSTRADIUS = (double) 1 / 16;

        commuteCost = 1;

        numAgents = 50;
        //set overRideNumFirms to zero for single-actor model
        //(Also change action type in ActorMaker)
        overRideNumFirms = 0;
        overRideNumPeople = 50;
        numTotalActors = overRideNumFirms + overRideNumPeople;
        //can people move?
        //testing turning this into a method so a second one can take a %
//        peopleMobile(false);
        peopleMobile(false);
//        firmsMobile = false;
        //fix wages. If zero, not fixed
        peopleFixWage = 0;
        deliveryCost = 20;
        //whether to use price to target a stock level or to maximise utility
        stockTarget = true;
        setStockToZero = false;
        //should distance affect wage via time? False if no.
        peopleWageDistanceCost = false;
        //stop goods and money flow through firms
//        firmsGoodsFlowOn = false;
//        firmsMoneyFlowOn = false;
        goodSellersGetDeliveryCost = false;

        wrapSpace = false;

        peopleConsiderAll = true;
        excludeMe = true;

        //set actor ref to look at. (Each actor has a unique ID.)
        LOOKATME = 5;

        //Utility optimising method
        //noiseUtil: adding noise to the constrained opt to mimic the `natural' noise of the greedy al.
        noiseUtil = 0;
        optimiser = Optimiser.Constrained;
//        optimiser = Optimiser.Greedy;
        //speed of visualisation: may need to slow down for constrained opt
        //in milliseconds
        timeDelay = 3;




        //Make global factor changer here, so it's changed before anything else acts
        //(Though I could fiddle with the weight, this is the easiest way of doing it for now...)
        //new GlobalSpaceCostChanger(time, 1);
        //SPACECOST = 1;
        //STARTSPACECOST = 0.5;
        //CESplusNecessityUtility: pass in rho
        //love of variety
        //0<rho<1
        //As rho -> 0, consumer is better off with a mix of goods.
//        new UtilityShell(new CESplusNecessityUtility(0.25));
        rho = 0.6;
        UtilityShell.u = new CESOneTypeUtility(rho);
//        UtilityShell.u = new CESPlusNecessityGoodUtility(1, 0.5, 1);
//        UtilityShell.u = new NecessityLuxuryUtility(0.5, 0);
//        1 unit input is always just itself - (x^p)^1/p = x
        //presuming 1 day input = 1 unit output
        //So backyard production is always just linear
        //This is assuming all agents, including people, can only produce one good
        BY_OPTIMUM = 1;
        //Production
        EoS_Curve = 0.5;
        ProductionShell.p = new SmoothEoS();
//        ProductionShell.p = new CoreModelEoS();

//        for (int i = 0; i < 60; i++) {
//            System.out.println(ProductionShell.giveOutput(i));
//        }

    }

    /**
     * Specific version of setUp for single actor model. Easiest way since so
     * many settings are specific to each type
     */
    public static void setUpSingleActorOlderVersion() {

        singleActor = true;

        /**
         * ***************
         * DATA AND OUTPUT SETUP ***************
         */
        numberOfRandomSeedChanges = 1;
        currentRun = 0;
        modelRunDays = 0;
        //Make a timeline
        gl.time = new Timeline(gl.modelRunDays);
        //add mouseListenerwrappers now. They get called to change values; ProcessingSpaceDrawer decides
        //which actual VarChanger is changed...
        leftMouseButton = new ListenerWrapper(time, 1);
        rightMouseButton = new ListenerWrapper(time, 1);

        //Number of data points to write. If higher than modelRunDays,
        //defaults to modelRunDays below
//        dataWritePointNumber = modelRunDays / 4;
        dataWritePointNumber = 1000;

        if (modelRunDays < dataWritePointNumber) {
            //otherwise, division by zero because these are integers.
            dataWriteGap = 1;
            dataWritePointNumber = modelRunDays;

        } else {
            dataWriteGap = modelRunDays / dataWritePointNumber;
        }
        //dataWritePointNumber = modelRunDays / dataWriteGap;
        System.out.println("dataWriteGap: " + dataWriteGap);

        //open Processing Visualisations?
        viewVis = true;
        //output jpegs from space drawer?
        outputJpegs = false;
        writeData = true;
        writeMultiRunOnly = true;
        outputJpegsOnDataDays = false;
        //if zero, turn off...
        if (modelRunDays == 0) {
            writeData = false;
            outputJpegs = false;
        }

        topView = false;
        //Used by space for size of person visualisation factors
        gl.visFactorBuyLines = 50f;
        gl.visFactorUtility = 50f;
        gl.visUtilityRangeOn = true;

        drawFirmOneDayCircles = false;

        //gl.changeVal = 1;

        //General output switch
        p.print(0);
        //Actor decision output switch
        p.agent(0);

        /**
         * ***************
         * MODEL SETUP ***************
         */
        //Set the kind of space used
//        gl.setSpace(gl.SpaceType.Point);
//        numagents = 200;
//        numagents = 200;
//        setSpace(SpaceType.TwoRegion);
//        gl.TwoR_FirmPopRegZero = 0;
//        gl.TwoR_PeoplePopRegZero = 1;
//        width = 0.25;
//        numagents = 100;
        setSpace(SpaceType.Line);
//        setSpace(SpaceType.Torus);
        width = 2;

        defaultSpaceAgentPositions = false;

        //if >0 landlord agents used to impose land cost
        //Number indicates amount of 'landlords' per unit width
        landCostsOn = 0;
        //Density cost! Yay
        DENSITYCOST = 0;
        //DENSITYCOSTRADIUS = width / 16;
        DENSITYCOSTRADIUS = (double) 1 / 16;

        numAgents = 50;
        //set overRideNumFirms to zero for single-actor model
        //(Also change action type in ActorMaker)
        overRideNumFirms = 0;
        overRideNumPeople = 100;
        numTotalActors = overRideNumFirms + overRideNumPeople;
        //can people move?
        //testing turning this into a method so a second one can take a %
        peopleMobile(false);
//        firmsMobile = true;
        //In single actor model, this will be their total time amount
        peopleFixWage = 1;
        //should distance affect wage via time? False if no.
        peopleWageDistanceCost = true;
        //stop goods and money flow through firms
//        firmsGoodsFlowOn = false;
//        firmsMoneyFlowOn = true;
//        firmsGetDeliveryCost = false;

        wrapSpace = false;

        peopleConsiderAll = true;

        //set actor ref to look at. (Each actor has a unique ID.)
        LOOKATME = 0;

        //Utility optimising method
        optimiser = Optimiser.Constrained;
//        optimiser = Optimiser.Greedy;

        //Make global factor changer here, so it's changed before anything else acts
        //(Though I could fiddle with the weight, this is the easiest way of doing it for now...)
        //new GlobalSpaceCostChanger(time, 1);
        //SPACECOST = 1;
        //STARTSPACECOST = 0.5;
        //CESplusNecessityUtility: pass in rho
        //love of variety
        //0<rho<1
        //As rho -> 0, consumer is better off with a mix of goods.
//        new UtilityShell(new CESplusNecessityUtility(0.25));
        UtilityShell.u = new CESOneTypeUtility(0.5);
        //UtilityShell.u = new CESPlusNecessityGoodUtility(1, 0.5, 1);
//        UtilityShell.u = new NecessityLuxuryUtility(0.5, 0.5);
//        1 unit input is always just itself - (x^p)^1/p = x
        //presuming 1 day input = 1 unit output
        //So backyard production is always just linear
        //This is assuming all agents, including people, can only produce one good
        BY_OPTIMUM = 1;
        //Production
        ProductionShell.p = new SmoothEoS();

//        for (int i = 0; i < 60; i++) {
//            System.out.println(ProductionShell.giveOutput(i));
//        }

    }

    private static void peopleMobile(boolean yes) {
        gl.peopleMobile = yes;
    }

    public static void peopleFixWage(double w) {

        //used by other classes
        peopleFixWage = w;

        Firm f;

        if (w > 0) {

            //change wage set in firms. Firm actions also coded to not change the wage.
            for (Actor a : gl.firms) {
                f = (Firm) a;
                f.wage = w;
            }

        }

    }

    private static void peopleMobile(double percent) {
    }

    public static Color nextActorColour() {

        //if first one, set up colour array
        if (colCount == 0) {

//            for (int i = 15; i < 255; i += 60) {
            for (int i = 0; i < 255; i += 150) {
                colours.add(new Color(i, i, i));
            }
//            colours.add(new Color(220,220,220));
//            colours.add(new Color(190,190,190));
//            colours.add(new Color(160,160,160));
//            colours.add(new Color(130,130,130));
//            colours.add(new Color(255, 0, 0));
//            colours.add(new Color(0, 255, 0));
//            colours.add(new Color(0, 0, 255));
//            colours.add(new Color(0, 255, 255));
        }

        //return from pre-set list of colours. If none left, return random.
        if (colCount < colours.size()) {
//            System.out.println("col!");
            return colours.get(colCount++);
        } else {
            return new Color((int) (Randoms.nextDouble() * 255), (int) (Randoms.nextDouble() * 255), (int) (Randoms.nextDouble() * 255));
        }

    }

    public static synchronized void holdPassingBaton() {

        someoneHasBaton = true;

        while (someoneHasBaton) {
//            System.out.println("stuckeroo!");
        }

    }//end holdPassingBaton
}
