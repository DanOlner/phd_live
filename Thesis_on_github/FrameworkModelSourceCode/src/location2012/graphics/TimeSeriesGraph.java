/*
 * Processing Drawer - takes in an array of drawables and...
 * ??? Something to tell it if it's constant draw, or called by timeline, etc
 *
 * Question: does it need to run on its own thread? May have to implement runnable
 * Or maybe applets do that already, I forget
 *
 */
package location2012.graphics;

import location2012.observe.Audible;
import location2012.observe.Listener;
import location2012.observe.ShoutEvent;
import location2012.observe.Shouter;
import processing.core.*;
import java.awt.BorderLayout;
import java.awt.Frame;
import java.awt.event.*;
import java.util.ArrayList;
import location2012.Firm;
import location2012.Person;
import location2012.io.staticArrayWriter;
import location2012.utils.FileName;
import location2012.utils.Map;
import location2012.utils.MathsBits;
import location2012.utils.gl;
import location2012.variables.MinMaxValues;
import location2012.utils.Stats;
import location2012.variables.VariableStore;

/**
 *
 * @author Olner Dan
 */
public class TimeSeriesGraph extends Frame implements Listener, PrintPApplet {

    boolean resetMinMax = false;
    int weight;
    int xsize, ysize;
    MyPapplet pa;
    //MovieMaker mm;
    //Camera camera1;
    //PeasyCam cam;
    //Array for drawables
//    ArrayList<Drawable> drawables = new ArrayList<Drawable>();
    //control
//    public boolean resetMaxVal = false;
//    public boolean resetValues = false;
    public boolean printScreen = false;
    public boolean saveVariableStoreClone = false;
    public boolean followMode = true;
    TimeSeriesGraph thisPG;
    private static int graphPos = 0;
    //For locating graph within window
    //Location of top-left and bottom-right corners as proportions of 1
    //Using Processing rectMode(CORNERS) below as a consequence
    float x1, y1, x2, y2;
    //
    //variable name to get - should be in Object contained in ArrayList
    //Exception thrown otherwise
//    String varName;
    ArrayList array;
    VariableStore vs, stockHack;
    //Local clone. Needed to avoid concurrency issues
//    public ArrayList<ArrayList<Double>> variableDataClone = new ArrayList<ArrayList<Double>>();
    public ArrayList<Double[]> variableDataClone = new ArrayList<Double[]>();
    //cos I'm using foreach loop, need record of which datapoint is being used
    int i = 0;
    int followWidth = 250;
    int numPoints;
    //record of min and max vals
    MinMaxValues minMax;
    //toggle view.
    boolean showEach = true, showMeanSD = false;

    /*
     * Including an audible and a val for letting Processing know when to draw
     */
//    public TimeSeriesGraph(Drawable drawable, int xsize, int ysize, Audible a, double val) {
    public TimeSeriesGraph(VariableStore vs, int xsize, int ysize,
            double daysBetweenDraw, int followWidth, int weight) {

        //so I can pass in ref to whole object. Might work!
        thisPG = this;
        this.vs = vs;
        this.followWidth = followWidth;

        //hack for showing zero crossings for stock
        //get variableStore
        stockHack = gl.mainDataStore.getVariableStoreByVariableName("stock");

        minMax = new MinMaxValues();
//        minMax = new MinMaxValues(followWidth * 2);

        setWeight(weight);

        //set graph position
        x1 = 0.13f;
        y1 = 0.1f;
        x2 = 0.98f;
        y2 = 0.85f;

        //Add listener details to audible
        giveShouterTo(gl.time, daysBetweenDraw);

        this.xsize = xsize;
        this.ysize = ysize;

        setLayout(new BorderLayout());

        //set up inner class where Processing will run
        pa = new MyPapplet();

        setTitle(vs.varName);

        add(pa, BorderLayout.CENTER);
        makeCloseable();
        pa.init();

        //position at bottom right of screen
//        setLocation(pa.screen.width - xsize, pa.screen.height - ysize);
//        setLocation(gl.vizWidth + 5, graphPos);
        setLocation(pa.screen.width - xsize, graphPos);
        graphPos += (ysize) + 5;
        setSize(xsize, ysize);

        setVisible(true);

    }

    //just for making frame window closeable.
    private void makeCloseable() {

        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent evt) {

                //Close this Frame and the Processing window in it
                pa.stop();
                dispose();

            }
        });

    }

    public void giveShouterTo(Audible a, double val) {

        a.registerShouter(new Shouter(this, val));

    }

    /*
     * Heard: so run through all registered drawables and draw them
     */
    public void heard(ShoutEvent s) {


//        if (gl.day == 5000) {
//            saveVariableStoreClone = true;
//        }

        //testing data, get a CSV output if w pressed
        if (saveVariableStoreClone) {

            saveVariableStoreClone = false;

            String fileName = FileName.getFileName();
            staticArrayWriter.write2DArrayOfDoubles(variableDataClone, fileName + vs.varName + "_timeSeries");

        }

        pa.redraw();

    }

    public void setWeight(int weight) {

        this.weight = weight;
//        System.out.println("and?" + this.weight);

    }

    public int getWeight() {

        return weight;

    }

    @Override
    public void printScreen() {
        printScreen = true;
    }

    //Inner class for running Processor
    public class MyPapplet extends PApplet {

        PFont font;
        //keep a record of maximum value for adjusting drawing
        //need 'use maxVal' so drawing doesn't change during one draw
        double maxVal, useMaxVal;
        //if lowest value is below zero
        double minVal, useMinVal;
        //testing with Firm data
        //drawing vars
        double lastVal;
        boolean firstVal;
        //cos I'm using foreach loop, need record of which datapoint is being used
        float h1, v1, h2, v2;
        float xsf, ysf, xo, yo;
        //vars for drawing tickmarks on y axis
        //total range min to max
        double tickPoint;
        //power of ten to draw
        int powerOfTen;
        //ticks to draw
        int tickNum;
        //to check correct power is being used
        int tickTest;
        //for toggling text between tickmarks
        boolean textToggle;
        //Another clone to check if it's a thread problem (and if this fixes it)
        public ArrayList<ArrayList<Double>> variableDataCloneForProcessing = new ArrayList<ArrayList<Double>>();

        @Override
        public void setup() {

//            actorData = new ArrayList[vs.objects.size()];

            //initialise each actor slot in actorData
//        for (ArrayList<Double> d : actorData) {
//            d = new ArrayList<Double>();
//            d.add(3.0);
////            System.out.println("Val: " + actorData[0].get(0));
//            System.out.println("Val: " + d.get(0));
//        }
//
//            for (int i = 0; i < actorData.length; i++) {
//                actorData[i] = new ArrayList<Double>();
//            }

            //System.out.println("ping!");
            size(xsize, ysize, P2D);
//            pg = createGraphics((int) (xsize*0.8), (int) (ysize*0.8), P3D);
//            camera1 = new Camera(this, 0, 0, 500);
//            camera1.arc(0.7f);
//            camera1.boom(60f);

//            mm = new MovieMaker(this, width, height, "drawing.mov",
//            30, MovieMaker.H263, MovieMaker.MEDIUM);

//            colorMode(RGB);

            rectMode(CORNERS);

//            font = createFont("ArialMT14.vlw", 48);
//            font = createFont("Arial-BoldMT-16.vlw", 16);
//            font = loadFont("ArialBoldMT20.vlw");

//            font = createFont("Arial-BoldMT-14.vlw", 14);
            font = createFont("Arial-BoldMT-14.vlw", (int) height / 20);
//            font = createFont("Dialog.plain-12.vlw", 12);
            textFont(font);
//            textFont(font, 16);

            textMode(SCREEN);

//            hint(ENABLE_NATIVE_FONTS);
//            font = createFont("Arial-BoldMT-48.vlw", 18);
//            textFont(font, 24);

            smooth();

            noLoop();

        }

        @Override
        public void draw() {

            smooth();

//            gl.graphVizTicks++;

            //Grab a clone copy of the data
            //Doing this at the start of the draw method as it keeps it within the
            //PApplet thread and avoids weird artefacts 
            variableDataClone = (ArrayList<Double[]>) vs.variableData.clone();

            if (followMode) {

                if (variableDataClone.size() > followWidth) {

//                System.err.println("WAAA");

                    //keep only those arrays up to followwith
                    //well, that worked. Gotta be better way!
                    //Unless we're paused..
//                    if (!gl.pauseGraphs) {
                    variableDataClone = new ArrayList(variableDataClone.subList(variableDataClone.size() - 1 - followWidth,
                            variableDataClone.size() - 1));
                }
//                    variableDataClone = Arrays.copyOfRange(variableDataClone, 0, 1);

//                }

            }

            numPoints = variableDataClone.size();

//            if (variableDataClone.size() > (followWidth * 2) + 1) {
//                minMax.findMinMaxFromArrayListOfArrayDoubles(
//                        new ArrayList(variableDataClone.subList(variableDataClone.size() - 1 - (followWidth * 2),
//                        variableDataClone.size() - 1)));
//            }

//            minMax.addValuesToFollow(variableDataClone.get(variableDataClone.size() - 1));
//            minMax.findMinMaxFromArrayListOfArrayDoubles(variableDataClone);


            //this gets Processing to resize to correct proportion in parent frame
            if (gl.day % 5 == 0) {
                thisPG.setSize(xsize + 1, ysize + 1);
            }

            noFill();
            noFill();

            background(255);

//            drawable.draw(thisPG);

            drawData();

            stroke(0);
            strokeWeight(Map.mapToVizX(0.003));
            rect(x1 * width, y1 * height, x2 * width, y2 * height);


            if (printScreen) {
//                saveFrame("C://saveframes/grapher-####.jpg");
                printScreen();
            }
            //mm.addFrame();
            //camera1.feed();

        }

        public void printScreen() {

            printScreen = false;
            saveFrame(FileName.getFileName() + "_" + vs.varName + "_vs_Time.png");

        }

        private void drawData() {

            fill(0);

            int actorNum = 0;

            //x1,y1 and x2, y2 from the parent Grapher are values between 0 and 1, for describing
            //the top-left and bottom-right corners of the graph box, proportional to the size of the frame / processing window

            //These 'shrink factors' use those to reduce the line coordinates
            xsf = (x2 - x1);
            ysf = (y2 - y1);
            //And these set the x and y offsets using the first two proportional coordinates
            xo = x1 * width;
            yo = y1 * height;

            //need to do before drawing axis lines

            try {
                findMinMax();
            } catch (Exception e) {
                System.out.println("minMax spazzed out");
                minMax.lowVal = 0;
                minMax.highVal = 1;
            }

            drawAxisLines();

            //variableDataClone is each object's data in each sub-ArrayList.
            //To draw lines, we need all datapoints for each object. 
            //That takes a little faff since we need to rotate by 90 degrees
            Double d;

//            System.out.println("variableDataClone.get(0).size():" + variableDataClone.get(0).size());

            //so first, for each object we draw a line for (and for now assuming all the same size
//            for (int j = 0; j < variableDataCloneForProcessing.get(0).size(); j++) {

            if (showEach) {
                for (int j = 0; j < variableDataClone.get(0).length; j++) {

//                System.out.println("size of actor data: " + actorData.length);

                    firstVal = true;
                    //reset draw line counter
                    i = 0;


                    //draw a series of lines spanning the viz

                    //for each object's value through model time
                    for (int k = 0; k < variableDataClone.size(); k++) {

                        d = variableDataClone.get(k)[j];

                        if (d > maxVal) {
                            maxVal = d;
                        }

                        if (d < minVal) {
                            minVal = d;
                        }

                        //keep them a little distance apart
                        if (maxVal - minVal < 0.001) {
                            minVal -= 0.001;
                            maxVal += 0.001;
                        }

                        if (resetMinMax) {
                            resetMinMax = false;
//                        System.out.println("In draw, minVal: " + minVal + ", maxVal: " + maxVal);
                        }

                        //if first one, don't draw: just store value to be used as
                        //start of line next time
                        if (firstVal) {

                            lastVal = d;
                            firstVal = false;

                        } else {


                            //Should probably try and explain what's going on here for when I come back to it
                            //and it makes absolutely no sense
                            //h1 - v2: co-ordinates for the start and endpoints of the line. 
                            //In this form, they're proportional to the whole Processing window
                            //maxVal - minVal = e.g. 80 - (-20) = 100. If minVal is zero, maxVal unchanged
                            h1 = (width / (float) numPoints) * (float) i++;
                            v1 = height - ((height / ((float) maxVal - (float) minVal)) * (float) (lastVal - minVal));
                            h2 = (width / (float) numPoints) * (float) i + 1;
                            v2 = height - ((height / ((float) maxVal - (float) minVal)) * (d.floatValue() - (float) minVal));
                            //Finally, the line itself
//                    p.pa.stroke(200);
//                    p.pa.strokeWeight(6);
//                    p.pa.stroke(gl.firms.get(actorNum).colour.getRed()+100,gl.firms.get(actorNum).colour.getGreen()+100,gl.firms.get(actorNum).colour.getBlue()+100);
//                    p.pa.line(xo + (h1 * xsf), yo + (v1 * ysf), xo + (h2 * xsf), yo + (v2 * ysf));
                            //p = (SingleActorPerson) gl.people.get(actorNum);
                            Firm f;

                            try {
                                f = (Firm) vs.objects.get(0);
                                stroke(gl.firms.get(actorNum).colour.getRGB());
                            } catch (Exception e) {
//                                System.out.println("Fail! Must be Person");
                                stroke(0, 150);
                            }

                            stroke(0, 150);
//                            try {
////                            System.out.println("WANG!");
////                            stroke(gl.people.get(actorNum).colour.getRGB());
//                            } catch (Throwable e) {
////                            System.err.println("failz:" + e.toString());
//                                stroke(0, 150);
//                            }

                            //hack singleactor colour//

                            strokeWeight(Map.mapToVizX(0.01));

                            line(xo + (h1 * xsf), yo + (v1 * ysf), xo + (h2 * xsf), yo + (v2 * ysf));

                            stroke(0, 150);
//                    pa.line(horiz(), vert(lastVal), horiz(), vert(d));
//                    p.pa.line((p.pa.width / (float) numPoints) * (float) i++,
//                            p.pa.height - ((p.pa.height / (float) maxVal) * (float) lastVal),
//                            (p.pa.width / (float) numPoints) * (float) i+1,
//                            p.pa.height - ((p.pa.height / (float) maxVal) * d.floatValue()));
                        }

                        lastVal = d;

                    }//end for double d

                    //increment actor num to get ID. 
                    actorNum++;

                }//end for ArrayList actorData

            }//end if showEach

            if (showMeanSD) {
                drawMean();
            }

            //Axis labels
            textAlign(CENTER);
            //x axis
//            if (gl.day > followWidth) {
            //hack to set day
//                text("model time (" + followWidth + " iterations after day " + gl.day + ")",
//                        xo + ((width / 2) * xsf), (y2 * height * 0.96f) + font.size * 2);
//            } else {
//            text("model time (" + followWidth + " iterations after day 350)",
//                    xo + ((width / 2) * xsf), (y2 * height * 0.96f) + font.size * 2);
            text("model time (" + followWidth + " iterations)",
                    xo + ((width / 2) * xsf), (y2 * height * 0.96f) + font.size * 2);
//            }

            float x = (xo * 0.8f) - (font.size * 2);
            float y = yo + ((height / 2) * ysf);
//            textAlign(CENTER, BOTTOM);

//            float x = width / 2;
//            float y = height / 2;

            textMode(MODEL);
//            noSmooth();

            //nabbed from http://forum.processing.org/topic/vertical-text
            pushMatrix();
            translate(x, y);
            rotate(-HALF_PI);
            text(vs.varNameLabel, 0, 0);
            popMatrix();

//            rotate(HALF_PI);
//            translate(-x, -y);

            textMode(SCREEN);
            textAlign(RIGHT);
//            smooth();

            //hack for showing zero-crossing point of stock
//            hackShowZeroCrossingForStock();


        }//end method drawActorData

        //hack for showing zero-crossing point of stock
        private void hackShowZeroCrossingForStock() {


            variableDataClone = (ArrayList<Double[]>) stockHack.variableData.clone();

            //get the right sub-range from the array
            if (followMode) {

                if (variableDataClone.size() > followWidth) {

                    variableDataClone = new ArrayList(variableDataClone.subList(variableDataClone.size() - 1 - followWidth,
                            variableDataClone.size() - 1));
                }

            }

            double lastx = -99999;

            //If two consecutive values multiplied are negative, it must have just
            //crossed from positive to negative or vice versa
            //For this hack we're assuming there's only a single firm
            //That's hacks for you!
            //So there should be only one Double in each VariableStore slot
            for (int j = 0; j < variableDataClone.size(); j++) {
//            for (Double[] thisx : variableDataClone) {

                //we need two values to work out if it's crossed the y axis
                if (lastx != -99999) {


                    //check if current and last value cross the y axis
                    if (lastx * variableDataClone.get(j)[0] < 0) {

                        lastx = variableDataClone.get(j)[0];
//                        System.out.println("crossed! " + lastx + "," + variableDataClone.get(j)[0]);

                        //if crossed, draw a vertical line (between the two?)
                        h1 = (width / (float) variableDataClone.size()) * (float) (j - 0.5f);

                        stroke(0);
//                        strokeWeight(Map.mapToVizX(0.008));
                        strokeWeight(2);
                        line(xo + (h1 * xsf), y1 * height, xo + (h1 * xsf), y2 * height);

                    }

                } else {

                    lastx = variableDataClone.get(j)[0];
                }

            }//end for double[] d

        }//end method hackShowZeroCrossingForStock

        private void drawAxisLines() {

            /**
             * Tickmarks
             */
            findTickPoints();

            stroke(0, 200, 255, 20);
            stroke(150);
            stroke(255);
            strokeWeight(Map.mapToVizX(0.005));
            textToggle = true;
            fill(0);

            //positive
            for (double i = 0; i < maxVal; i += tickPoint) {

                v1 = height - ((height / ((float) maxVal - (float) minVal)) * (float) (i - minVal));
                stroke(200);

                if (v1 < yo + (y2 * height)) {
                    line(xo, yo + (v1 * ysf), xo + (width * xsf), yo + (v1 * ysf));
                }

                stroke(255);
                textAlign(RIGHT);

                //hackeroo to fix stray labels
                if (yo + (v1 * ysf) + 4 < y2 * height) {
                    text("" + MathsBits.roundToDecimals(i, 2, true), xo - 4, yo + (v1 * ysf) + 4);
                }

            }

            //negative. This is actually easier than finding the correct negative starting point.
            for (double i = 0; i > minVal; i -= tickPoint) {

                v1 = height - ((height / ((float) maxVal - (float) minVal)) * (float) (i - minVal));
                stroke(200);
                line(xo, yo + (v1 * ysf), xo + (width * xsf), yo + (v1 * ysf));

                //i!=0: don't draw zero twice
                if (i != 0) {
                    stroke(255);
                    text("" + MathsBits.roundToDecimals(i, 2, true), xo - 4, yo + (v1 * ysf) + 4);

                }//end if i

            }//end for double i

            noFill();

            //if minVal < 0, draw the zero line
            if (minVal < 0) {
//                stroke(255, 100, 100, 70);
                stroke(150);
//                line(xo + (0), yo + (1 * ysf), xo + xsf, yo + (1 * ysf));
                v1 = height - ((height / ((float) maxVal - (float) minVal)) * (float) (0 - minVal));
                line(xo, yo + (v1 * ysf), xo + (width * xsf), yo + (v1 * ysf));

            }


        }

        private void findMinMax() {

            maxVal = -99999;
            minVal = 99999;
            Double d;

            variableDataClone = (ArrayList<Double[]>) vs.variableData.clone();

            if (followMode) {

                if (variableDataClone.size() > followWidth) {
                    variableDataClone = new ArrayList(variableDataClone.subList(variableDataClone.size() - 1 - followWidth,
                            variableDataClone.size() - 1));
                }

            }

            for (int j = 0; j < variableDataClone.get(0).length; j++) {
                for (int k = 0; k < variableDataClone.size(); k++) {

                    d = variableDataClone.get(k)[j];

                    if (d > maxVal) {
                        maxVal = d;
                    }

                    if (d < minVal) {
                        minVal = d;
                    }
                }//for k
            }//for j

            //keep them a little distance apart
            if (maxVal - minVal < 0.001) {
                minVal -= 0.001;
                maxVal += 0.001;
            }

        }

        public void findTickPoints() {

            powerOfTen = -5;
            tickTest = -1;
            tickNum = 4;
            tickPoint = (maxVal - minVal) / (double) tickNum;

//            tickPoint = 2545.657464;
//            tickPoint = 0.002453;
            //find point where 0 < tickNum < 10
            while (tickTest < 1 || tickTest > 10) {
                powerOfTen++;
                tickTest = (int) (tickPoint / Math.pow(10, powerOfTen));
//                System.out.println("Found value of test: " + range % Math.pow(10, powerOfTen));
            }

//        System.out.println("Min, max: " + minVal + "," + maxVal + ", full range: " + (maxVal - minVal) + ", power of ten: " + powerOfTen + ", tickPoint: " + tickPoint);

            //transform tickPoint to a two-value number using found power. 
            //See modelmaker2011, `Sidejob: axes for graphs'

//        System.out.println("-ten: " + (-powerOfTen));

            //powerOfTen - 1 will give two digits. Leaving as is returns 1.
//            tickPoint *= Math.pow(10, -(powerOfTen - 1));
            tickPoint *= Math.pow(10, -(powerOfTen));
            tickPoint = (int) tickPoint;
//            tickPoint *= Math.pow(10, powerOfTen - 1);
            tickPoint *= Math.pow(10, powerOfTen);
            //need to round because some small numbers pick up floating point artifacts
            tickPoint = (tickPoint > 1 ? tickPoint : MathsBits.roundToDecimals(tickPoint, 7));

            //divide range again by found tickPoint to get total number to draw
            tickNum = (int) ((maxVal - minVal) / tickPoint);

//        System.out.println("TickPoint: " + tickPoint + ", final tickNum: " + tickNum);

        }

        private void drawMean() {

            //get list of means
//            ArrayList<Double[]> means = Stats.returnArrayListWithMean(variableDataClone);
            ArrayList<Double[]> means = Stats.returnArrayListWithMeanAndSD(variableDataClone);

            Double d;

            firstVal = true;
            //reset draw line counter
            i = 0;

            //draw a mean line. Grrr!

            //for each object's value through model time
            //Mean first
            for (int k = 0; k < means.size(); k++) {

                d = means.get(k)[0];

                //if first one, don't draw: just store value to be used as
                //start of line next time
                if (firstVal) {

                    lastVal = d;
                    firstVal = false;

                } else {

                    h1 = (width / (float) numPoints) * (float) i++;
                    v1 = height - ((height / ((float) maxVal - (float) minVal)) * (float) (lastVal - minVal));
                    h2 = (width / (float) numPoints) * (float) i + 1;
                    v2 = height - ((height / ((float) maxVal - (float) minVal)) * (d.floatValue() - (float) minVal));

                    stroke(0);
                    strokeWeight(Map.mapToVizX(0.012));
                    line(xo + (h1 * xsf), yo + (v1 * ysf), xo + (h2 * xsf), yo + (v2 * ysf));

                    //thick triple line                    
//                    strokeJoin(ROUND);
//                    stroke(0);
//                    strokeWeight(Map.mapToVizX(0.024));
//                    line(xo + (h1 * xsf), yo + (v1 * ysf), xo + (h2 * xsf), yo + (v2 * ysf));
//
//                    stroke(255);
//                    strokeWeight(Map.mapToVizX(0.02));
//                    line(xo + (h1 * xsf), yo + (v1 * ysf), xo + (h2 * xsf), yo + (v2 * ysf));
//
//                    stroke(0);
//                    strokeWeight(Map.mapToVizX(0.006));
//                    line(xo + (h1 * xsf), yo + (v1 * ysf), xo + (h2 * xsf), yo + (v2 * ysf));

                }

                lastVal = d;

            }//end for double d

            //fails if only mean available
            try {

                firstVal = true;
                //reset draw line counter
                i = 0;

                //SD+
                for (int k = 0; k < means.size(); k++) {

                    d = means.get(k)[0] + means.get(k)[1];

                    //if first one, don't draw: just store value to be used as
                    //start of line next time
                    if (firstVal) {

                        lastVal = d;
                        firstVal = false;

                    } else {

                        h1 = (width / (float) numPoints) * (float) i++;
                        v1 = height - ((height / ((float) maxVal - (float) minVal)) * (float) (lastVal - minVal));
                        h2 = (width / (float) numPoints) * (float) i + 1;
                        v2 = height - ((height / ((float) maxVal - (float) minVal)) * (d.floatValue() - (float) minVal));

                        stroke(50);
                        strokeWeight(Map.mapToVizX(0.005));
                        line(xo + (h1 * xsf), yo + (v1 * ysf), xo + (h2 * xsf), yo + (v2 * ysf));


                    }

                    lastVal = d;

                }//end for double d

                firstVal = true;
                //reset draw line counter
                i = 0;

                //SD-
                for (int k = 0; k < means.size(); k++) {

                    d = means.get(k)[0] - means.get(k)[1];

                    //if first one, don't draw: just store value to be used as
                    //start of line next time
                    if (firstVal) {

                        lastVal = d;
                        firstVal = false;

                    } else {

                        h1 = (width / (float) numPoints) * (float) i++;
                        v1 = height - ((height / ((float) maxVal - (float) minVal)) * (float) (lastVal - minVal));
                        h2 = (width / (float) numPoints) * (float) i + 1;
                        v2 = height - ((height / ((float) maxVal - (float) minVal)) * (d.floatValue() - (float) minVal));

                        stroke(50);
                        strokeWeight(Map.mapToVizX(0.005));
                        line(xo + (h1 * xsf), yo + (v1 * ysf), xo + (h2 * xsf), yo + (v2 * ysf));

                    }

                    lastVal = d;

                }//end for double d

            } catch (Exception e) {
            }


        }

        public void keyPressed() {

            if (key == 'm' || key == 'M') {
//                resetMaxVal = true;
                maxVal = -99999;
            } else if (key == 'r' || key == 'R') {
                resetMinMax = true;
                maxVal = -99999;
                minVal = 99999;
                Double d;

                variableDataClone = (ArrayList<Double[]>) vs.variableData.clone();

                if (followMode) {

                    if (variableDataClone.size() > followWidth) {
                        variableDataClone = new ArrayList(variableDataClone.subList(variableDataClone.size() - 1 - followWidth,
                                variableDataClone.size() - 1));
                    }

                }

                for (int j = 0; j < variableDataClone.get(0).length; j++) {
                    for (int k = 0; k < variableDataClone.size(); k++) {

                        d = variableDataClone.get(k)[j];

                        if (d > maxVal) {
                            maxVal = d;
                        }

                        if (d < minVal) {
                            minVal = d;
                        }
                    }//for k
                }//for j

                //keep them a little distance apart
                if (maxVal - minVal < 0.001) {
                    minVal -= 0.001;
                    maxVal += 0.001;
                }

//                System.out.println("minVal: " + minVal + ", maxVal: " + maxVal);


            } else if (key == 'f' || key == 'F') {
                followMode = (followMode ? false : true);
            } else if (key == 'p' || key == 'P') {
                printScreen = true;
            } else if (key == 'w' || key == 'W') {
                saveVariableStoreClone = true;
            } else if (key == '1') {

                //and this manages to toggle quite successfully! Cunning huh?
                showEach = (showEach && showMeanSD ? false : true);
                showMeanSD = (showEach && showMeanSD ? false : true);

            }

        }
    }//end of inner class MyPapplet
}
