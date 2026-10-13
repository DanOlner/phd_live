/*
 * 
 */
package location2012.graphics;

import location2012.observe.Audible;
import location2012.observe.Listener;
import location2012.observe.ShoutEvent;
import location2012.observe.Shouter;
import processing.core.*;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Frame;
import java.awt.event.*;
import java.lang.reflect.Field;
import java.util.ArrayList;
import location2012.io.staticArrayWriter;
import location2012.utils.FileName;
import location2012.utils.Map;
import location2012.utils.MathsBits;
import location2012.utils.gl;
import location2012.variables.MinMaxValues;
import location2012.variables.VariableStore;
import location2012.variables.VariableStoreTwoFirmRatioTrigger;

/**
 * Produce correlation of two single variables stored in VariableStores So, two
 * datapoints for each 'day' (or other sample point e.g. when movement stability
 * reached)
 *
 * Contrast to: CorrelateArrayGraph, that correlates e.g. all People each day
 * for two variables, with previous days only shown as blue-to-orange coloured
 * circles.
 *
 * @author Olner Dan
 */
public class CorrelateSingleVarsGraph extends Frame implements Listener, PrintPApplet {

    MinMaxValues minMaxX, minMaxY;
    //if true, use min max x y vals passed in.
    boolean setLow, setHigh;
    //otherwise use these (passed in)
    double[] setLows, setHighs;
    int weight;
    int xsize, ysize;
    MyPapplet pa;
    //MovieMaker mm;
    //Camera camera1;
    //PeasyCam cam;
    //Array for drawables
//    ArrayList<Drawable> drawables = new ArrayList<Drawable>();
    //control
    public boolean resetMaxVal = false;
    public boolean resetValues = false;
    public static boolean printScreen = false;
    public boolean saveVariableStoreClone = false;
//    public boolean followMode = false;
    CorrelateSingleVarsGraph thisPG;
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
    VariableStore vs_xAxis;
    VariableStore vs_yAxis;
    //Local clone. Needed to avoid concurrency issues
    public ArrayList<Double[]> xAxis = new ArrayList<Double[]>();
    public ArrayList<Double[]> yAxis = new ArrayList<Double[]>();
    //cos I'm using foreach loop, need record of which datapoint is being used
    int i = 0;
    //Size of array for correlating. Both arrays will need to be same size of course
    int dataSize;
    //
    Color lowc, highc;
    int followWidth;

    /*
     * Including an audible and a val for letting Processing know when to draw
     */
//    public TimeSeriesGraph(Drawable drawable, int xsize, int ysize, Audible a, double val) {
    public CorrelateSingleVarsGraph(Audible a, VariableStore vs_x, VariableStore vs_y,
            int xsize, int ysize, double daysBetweenDraw,
            int followWidth, int weight, double[] setLows, double[] setHighs) {

        this.setLows = setLows;
        this.setHighs = setHighs;
        this.followWidth = followWidth;
        //if minmax_xy is null, find minmax from incoming values
        setLow = (setLows == null ? false : true);
        setHigh = (setHighs == null ? false : true);

        //so I can pass in ref to whole object for inner class.
        thisPG = this;

        this.vs_xAxis = vs_x;
        this.vs_yAxis = vs_y;

        //Need to change from gl.people
//        minMaxX = new MinMaxValues(null, 10, 2, vs_x.objects, vs_x.varName, true);
//        minMaxY = new MinMaxValues(null, 10, 2, vs_y.objects, vs_y.varName, true);
        minMaxX = new MinMaxValues();
        minMaxY = new MinMaxValues();

        setWeight(weight);

        float square = 0.8f;
        //set graph position
        x1 = 0.15f;
        y1 = 0.05f;
//        x2 = 0.95f;
        x2 = x1 + (square * ((float) ysize / (float) xsize));
//        y2 = 0.85f;
        y2 = y1 + square;

//        lowc = new Color(180, 209, 219);
//        lowc = new Color(200, 200, 200);
        lowc = new Color(50, 50, 50);

//        highc = new Color(255, 100, 50);
        highc = new Color(50, 50, 50);

        //Add listener details to audible
        giveShouterTo(a, daysBetweenDraw);

        this.xsize = xsize;
        this.ysize = ysize;

        setLayout(new BorderLayout());

        //set up inner class where Processing will run
        pa = new MyPapplet();

        setTitle(vs_x.varName + " vs " + vs_y.varName);

        add(pa, BorderLayout.CENTER);
        makeCloseable();
        pa.init();

        //position at bottom right of screen
        setLocation(0, pa.screen.height - ysize);
//        setLocation(pa.screen.width - xsize - graphPos, pa.screen.height - ysize);
//        setLocation(pa.screen.width - xsize - graphPos, 0);
//        setLocation(0, graphPos);
        graphPos += 500;
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

//        System.out.println("HEEEAAARD");

        //Grab a clone copy of the data
        //Doing this at the start of the draw method as it keeps it within the
        //PApplet thread and avoids weird artefacts 
        xAxis = (ArrayList<Double[]>) vs_xAxis.variableData.clone();
        yAxis = (ArrayList<Double[]>) vs_yAxis.variableData.clone();

        if (followWidth > 0) {

            if (xAxis.size() > followWidth) {


                //keep only those arrays up to followwith
                //well, that worked. Gotta be better way!
                xAxis = new ArrayList(xAxis.subList(xAxis.size() - 1 - followWidth, xAxis.size() - 1));
                yAxis = new ArrayList(yAxis.subList(yAxis.size() - 1 - followWidth, yAxis.size() - 1));

            }

        }

        //Use followWidth negative values to mark start point for data
        //To avoid starting randomnesses
        if (followWidth < 0 && xAxis.size() > -followWidth) {
            xAxis = new ArrayList(xAxis.subList(-followWidth, xAxis.size() - 1));
            yAxis = new ArrayList(yAxis.subList(-followWidth, yAxis.size() - 1));
        }


        //both axes need to be same size, so only need this number once.
        dataSize = xAxis.size();

        minMaxX.findMinMaxFromArrayListOfArrayDoubles(xAxis);
        minMaxY.findMinMaxFromArrayListOfArrayDoubles(yAxis);

        //reset those if we passed in any to keep permanent
        if (setLow) {
            //index in order: x low, x high, y low y high
            minMaxX.lowVal = setLows[0];
            minMaxY.lowVal = setLows[1];
        }

        if (setHigh) {
            //index in order: x low, x high, y low y high
            minMaxX.highVal = setHighs[0];
            minMaxY.highVal = setHighs[1];
        }




        //If the same, need to make a little viewing space to avoid crashes
//        if (minMaxX.lowVal >= minMaxX.highVal) {
////            System.out.println("correcting minMaxXs");
//            minMaxX.lowVal -= 0.1;
//            minMaxX.highVal = minMaxX.lowVal + 0.2;
//        }
//
//        if (minMaxY.lowVal >= minMaxY.highVal) {
////            System.out.println("correcting minMaxYs");
//            minMaxY.lowVal -= 0.1;
//            minMaxY.highVal = minMaxY.lowVal + 0.2;
//        }

        //testing if enforcing a gap stops crashes
        double gap = 0.001;

        if (minMaxX.highVal - minMaxX.lowVal < gap) {
//            System.out.println("correcting minMaxXs");
            minMaxX.lowVal -= 0.1;
            minMaxX.highVal = minMaxX.lowVal + 0.2;
        }

        if (minMaxY.highVal - minMaxY.lowVal < gap) {
//            System.out.println("correcting minMaxYs");
            minMaxY.lowVal -= 0.1;
            minMaxY.highVal = minMaxY.lowVal + 0.2;
        }


//        System.out.println("mixMaxX: " + minMaxX.lowVal + "," + minMaxX.highVal 
//                + ", minMaxY: " + minMaxY.lowVal + "," + minMaxY.highVal);

//        System.out.println("Testing some shizzle: ");
//        System.out.println("xAxis details: " + vs_xAxis.varName + ", size: " + vs_xAxis.variableData.size()
//                + ", length: " + vs_xAxis.variableData.get(vs_xAxis.variableData.size() - 1).length
//                + ", val: " + vs_xAxis.variableData.get(vs_xAxis.variableData.size() - 1)[0]);
//        System.out.println("yAxis details: " + vs_yAxis.varName + ", size: " + vs_yAxis.variableData.size()
//                + ", length: " + vs_yAxis.variableData.get(vs_yAxis.variableData.size() - 1).length
//                + ", val: " + vs_yAxis.variableData.get(vs_yAxis.variableData.size() - 1)[0]);

        //testing data, get a CSV output if w pressed
        if (saveVariableStoreClone) {

            saveVariableStoreClone = false;

            staticArrayWriter.write2DArrayOfDoubles(xAxis, "testinGraph");

        }

//        minMaxX.lowVal = 0;
//        minMaxX.highVal = 10;
//        minMaxY.lowVal = 0;
//        minMaxY.highVal = 10;

//        System.out.println("day: " + gl.day + ", before redraw");

//        minMaxX.highVal = 0.31;

//        System.out.println("minMaxX: " + minMaxX.lowVal + ", " + minMaxX.highVal);
//        System.out.println("minMaxY: " + minMaxY.lowVal + ", " + minMaxY.highVal);
//        System.out.println("minMaxX float: " + (float) minMaxX.lowVal + ", " + (float) minMaxX.highVal);
//        System.out.println("minMaxY float: " + (float) minMaxY.lowVal + ", " + (float) minMaxY.highVal);



        pa.redraw();

//        System.out.println("day: " + gl.day + ", after redraw");

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
        double xMaxVal = -99999, yMaxVal = -99999, xMinVal = 99999, yMinVal = 99999;
        //if lowest value is below zero
        double minVal, useMinVal;
        //testing with Firm data
//    Firm f;
        //drawing vars
        double lastVal;
        boolean firstVal;
        //cos I'm using foreach loop, need record of which datapoint is being used
        float h1, v1, h2, v2;
        float xwidth, yheight, xcorner, ycorner;
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
        //Double used for getting each point to draw
        Double[] d = new Double[2];
        double[] xy = new double[2], xyLast = new double[2];
        //if fading out previous values
        //0 = don't
        //Other values = alpha value of over-writing 
        int fade = 0;
        //
        PVector v;

        @Override
        public void setup() {

            size(xsize, ysize, P2D);

            rectMode(CORNERS);

            font = createFont("Arial-BoldMT-14.vlw", (int) width / 35);
            textFont(font);

            textMode(SCREEN);

            smooth();
            noLoop();

        }

        @Override
        public void draw() {

//            System.out.println("draw called successfully");

            //HACKEROOOOO!
            //Reason: Data may not have been stored yet (less agents there are, faster it runs, this correlator may have run too early. 
            //Damnable threading. Worth it though.
            if (xAxis.size() > 0) {

                assert xAxis.get(xAxis.size() - 1).length == 1 : "Single Var correlator x vals shouldn't have more than 1 index.";
                assert yAxis.get(yAxis.size() - 1).length == 1 : "Single Var correlator y vals shouldn't have more than 1 index.";

//                System.out.println("day: " + gl.day + ", size of arrays: " + xAxis.size() + "," + yAxis.size());
//            if (followWidth > 0) {
//
//                if (xAxis.size() > followWidth) {
//
//
//                    //keep only those arrays up to followwith
//                    //well, that worked. Gotta be better way!
//                    xAxis = new ArrayList(xAxis.subList(xAxis.size() - 1 - followWidth, xAxis.size() - 1));
//                    yAxis = new ArrayList(yAxis.subList(yAxis.size() - 1 - followWidth, yAxis.size() - 1));
//
//                }
//
//            }

//            minMaxY.lowVal = 0.63;
//            minMaxY.highVal = 0.68;

//            System.out.println("minMaxY: " + minMaxY.lowVal + "," + minMaxY.highVal);


                //this gets Processing to resize to correct proportion in parent frame
//                if (gl.day % 20 == 0) {
//                    thisPG.setSize(xsize + 1, ysize + 1);
//                }

//            fill(255, fade);
                background(255);

//            rect(0, 0, width, height);

                noFill();

                drawData();

                stroke(0);
                rect(x1 * width, y1 * height, x2 * width, y2 * height);


                if (printScreen) {
                    printScreen();
                }

//                if (gl.rho > 0.8) {
//                    printScreen();
//                }
                //mm.addFrame();
                //camera1.feed();

            }//end if gl.day

        }

        public void printScreen() {

            printScreen = false;
//            saveFrame(FileName.getFileName() + "_"
//                    + vs_xAxis.varName + "_vs_" + vs_yAxis.varName + ".tif");
            saveFrame(FileName.getFileName() + "_"
                    + vs_xAxis.varName + "_vs_" + vs_yAxis.varName + ".png");

        }

        private void drawData() {

            xwidth = (x2 - x1);
            yheight = (y2 - y1);
            //And these set the x and y offsets using the first two proportional coordinates
            xcorner = x1 * width;
            ycorner = y1 * height;

            //both datasets should be same size in the time axis,            
            //tho not nec in number of objects (since it could be e.g. one value like rho)
            for (int j = 0; j < dataSize; j++) {

                //this dimension of the array is model time
//                for (int k = 0; k < xAxis.size(); k++) {

                //a bit of hacking to deal with single-value vars like rho
                //just check if there's available data
                //p.s. since int j is set to match x axis length
                //single vals need to be on y axis. 
                //I repeat, HACKEROO! Needs fixing so it doesn't matter                    
//                d[0] = xAxis.get(j)[0];
//                d[1] = yAxis.get(j)[0];

//                float[] xy = new float[2];
//                float[] xyLast = new float[2];



                xy[0] = xAxis.get(j)[0];
                xy[1] = yAxis.get(j)[0];

                if (j > 0) {
                    xyLast[0] = xAxis.get(j - 1)[0];
                    xyLast[1] = yAxis.get(j - 1)[0];
                } else {
                    xyLast[0] = xAxis.get(j)[0];
                    xyLast[1] = yAxis.get(j)[0];
                }


                double xRange = minMaxX.highVal - minMaxX.lowVal;
                double yRange = minMaxY.highVal - minMaxY.lowVal;

                //for adjusting the angle of the marker vector given that
                //the two axes will be at differing scales.
                //See thesisDiary11-13 "Vector for singleVar correlator"
                double rangeRatio = xRange / yRange;

                //Y axis reversed because the graph starts with zero at the bottom
                //while the graphics is the other way round. See? 
                v = new PVector((float) (xy[0] - xyLast[0]),
                        -(float) ((xy[1] - xyLast[1]) * rangeRatio));

//                System.out.println("x,y: " + xy[0] + ","
//                        + xy[1]
//                        + " || lastx,y: " + xyLast[0] + "," + xyLast[1]
//                        + " || diff: " + (xy[0] - xyLast[0]) + "," + (xy[1] - xyLast[1])
//                        + " || heading: " + v.heading2D());


                //                v = new PVector(xyLast[0] - xy[0], xyLast[1] - xy[1]);

                float x = (width / ((float) minMaxX.highVal - (float) minMaxX.lowVal))
                        * ((float) xy[0] - (float) minMaxX.lowVal);
                float y = height - (height / ((float) minMaxY.highVal - (float) minMaxY.lowVal))
                        * ((float) xy[1] - (float) minMaxY.lowVal);
//                float x = (width / ((float) minMaxX.highVal - (float) minMaxX.lowVal))
//                        * (d[0].floatValue() - (float) minMaxX.lowVal);
//                float y = height - (height / ((float) minMaxY.highVal - (float) minMaxY.lowVal))
//                        * (d[1].floatValue() - (float) minMaxY.lowVal);


                //colour to indicate time 
//                    fill(Map.mapLowHighToColour_DoublesColorsInColorOut(k, 0,
//                            (double) xAxis.size(), Color.red, Color.green).getRGB());
//                    fill(Map.mapLowHighToColour_DoublesColorsInColorOut(k, 0,
//                            (double) xAxis.size(), lowc, highc).getRGB());
//                    
                //colour based on polarity of y-axis change between t and t-1.
                if (yAxis.size() > 1 && j > 1) {

                    //if today's value is higher than yesterday's, direction of change is positive
                    if (yAxis.get(j)[0] > yAxis.get(j - 1)[0]) {

                        fill(highc.getRGB());

                    } else {
                        fill(lowc.getRGB());
                    }

                } else {
                    fill(highc.getRGB());
//                        fill(lowc.getRGB());
                }

                //hack shade for symmetric/non-symmetric parameter sweep
                //Duh: won't work, we don't know the previous state
//                if (VariableStoreTwoFirmPriceRatioTrigger.symmetric) {
//                    fill(0);
//                } else {
//                    fill(200);
//                }

                fill(200);

                stroke(0);
                strokeWeight(Map.mapToVizX(0.004));
//                strokeWeight(1);

//                    ellipse(x, y, 5f, 5f);
//                    ellipse(xo + (x * xsf), yo + (y * ysf), 14f, 14f);
//                ellipse(xcorner + (x * xwidth), ycorner + (y * yheight), 0.017f * xsize, 0.017f * ysize);
                triangle(xcorner + (x * xwidth), ycorner + (y * yheight), 0.017f * ysize, v.heading2D());
//                    ellipse(x, y, 5f, 5f);
//                    ellipse(width / 2, height / 2, 5f, 5f);
                noFill();

//                }//end for k

            }//end for j


            verticalTickPoints();
            horizTickPoints();

            //Axis labels
            textAlign(CENTER);
            //x axis
            text(vs_xAxis.varNameLabel, xcorner + ((width / 2) * xwidth), (y2 * height) + font.size * 3);

            float x = (xcorner + 5) - (font.size * 4);
            float y = ycorner + ((height / 2) * yheight);
//            textAlign(CENTER, BOTTOM);

//            float x = width / 2;
//            float y = height / 2;

            textMode(MODEL);
//            noSmooth();

            //nabbed from http://forum.processing.org/topic/vertical-text
            pushMatrix();
            translate(x, y);
            rotate(-HALF_PI);
            text(vs_yAxis.varNameLabel, 0, 0);
            popMatrix();

//            rotate(HALF_PI);
//            translate(-x, -y);

            textMode(SCREEN);
            textAlign(RIGHT);
//            smooth();


        }//end method drawData

        private void verticalTickPoints() {

//            textAlign(RIGHT);
//            text("Is this appearing? ", 100, 100);


            /**
             * Tickmarks
             */
            tickPoint = findTickPoints(4, minMaxY.lowVal, minMaxY.highVal);

            stroke(100);
            textToggle = true;
            fill(0);

            //positive
            //First, check we're not above zero
//            double start = (minMaxY.lowVal > 0 ? minMaxY.lowVal : 0);

            for (double i = 0; i < minMaxY.highVal; i += tickPoint) {

                v1 = height - ((height / ((float) minMaxY.highVal
                        - (float) minMaxY.lowVal)) * (float) (i - minMaxY.lowVal));
                stroke(200);

                //check to make sure we're not drawing lines beyond the box
                if (ycorner + (v1 * yheight) < (y2 * height)) {
                    line(xcorner, ycorner + (v1 * yheight), xcorner + (width * xwidth), ycorner + (v1 * yheight));
                    textAlign(RIGHT);
                    stroke(255);
                    text("" + MathsBits.roundToDecimals(i, 2), xcorner - 4, ycorner + (v1 * yheight) + 4);
                }

            }

            //negative. This is actually easier than finding the correct negative starting point.
            for (double i = 0; i > minMaxY.lowVal; i -= tickPoint) {

                v1 = height - ((height / ((float) minMaxY.highVal
                        - (float) minMaxY.lowVal)) * (float) (i - minMaxY.lowVal));
                stroke(200);
                line(xcorner, ycorner + (v1 * yheight), xcorner + (width * xwidth), ycorner + (v1 * yheight));

                //i!=0: don't draw zero twice
                if (i != 0) {

                    stroke(255);
                    text("" + MathsBits.roundToDecimals(i, 3, true), xcorner - 4, ycorner + (v1 * yheight) + 4);

                }//end if i

            }//end for double i

            noFill();

            //if minVal < 0, draw the zero line
            if (minMaxY.lowVal < 0) {
                stroke(255, 100, 100, 70);

                v1 = height - ((height / ((float) minMaxY.highVal - (float) minMaxY.lowVal))
                        * (float) (0 - minMaxY.lowVal));
                line(xcorner, ycorner + (v1 * yheight), xcorner + (width * xwidth), ycorner + (v1 * yheight));

            }

        }//end method verticalTickPoints

        private void horizTickPoints() {
            /**
             * Tickmarks
             */
            tickPoint = findTickPoints(4, minMaxX.lowVal, minMaxX.highVal);

            stroke(100);
            textToggle = true;
            fill(0);

            //positive
            //First, check we're not above zero
//            double start = (minMaxX.lowVal > 0 ? minMaxX.lowVal : 0);


            for (double i = 0; i < minMaxX.highVal; i += tickPoint) {

//                float x = (width / ((float) minMaxX.highVal - (float) minMaxX.lowVal))
//                        * (d[0].floatValue() - (float) minMaxX.lowVal);

                v1 = ((width / ((float) minMaxX.highVal
                        - (float) minMaxX.lowVal)) * (float) (i - minMaxX.lowVal));

                stroke(200);

                //check to make sure we're not drawing lines beyond the box
                if (xcorner + (v1 * xwidth) > xcorner) {
                    line(xcorner + (v1 * xwidth), ycorner + (height * yheight), xcorner + (v1 * xwidth), ycorner);
                    textAlign(RIGHT);
                    stroke(255);
                    text("" + MathsBits.roundToDecimals(i, 2, true), xcorner + (v1 * xwidth),
                            ycorner + (height * yheight) + font.size);
                }

            }


            //negative. This is actually easier than finding the correct negative starting point.
            for (double i = 0; i > minMaxX.lowVal; i -= tickPoint) {

                v1 = ((width / ((float) minMaxX.highVal
                        - (float) minMaxX.lowVal)) * (float) (i - minMaxX.lowVal));
                stroke(200);
                line(xcorner + (v1 * xwidth), ycorner + (height * yheight), xcorner + (v1 * xwidth), ycorner);

                //i!=0: don't draw zero twice
                if (i != 0) {

                    stroke(255);
                    text("" + MathsBits.roundToDecimals(i, 2, true), xcorner + (v1 * xwidth), ycorner
                            + (height * yheight) + font.size);

                }//end if i

            }//end for double i

            noFill();

            //if minVal < 0, draw the zero line
            if (minMaxX.lowVal < 0) {
                stroke(255, 100, 100, 70);

                v1 = ((width / ((float) minMaxX.highVal
                        - (float) minMaxX.lowVal)) * (float) (0 - minMaxX.lowVal));

                line(xcorner + (v1 * xwidth), ycorner + (height * yheight), xcorner + (v1 * xwidth), ycorner);

            }

        }//end method horizTickPoints

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

            //transform tickPoint to a two-value number using found power. See modelmaker2011, `Sidejob: axes for graphs'

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

        public double findTickPoints(int tickNum, double minVal, double maxVal) {

            powerOfTen = -5;
            tickTest = -1;
//            tickNum = 4;
            tickPoint = (maxVal - minVal) / (double) tickNum;

//            tickPoint = 2545.657464;
//            tickPoint = 0.002453;
            //find point where 0 < tickNum < 10
            while (tickTest < 1 || tickTest > 10) {
                powerOfTen++;
                tickTest = (int) (tickPoint / Math.pow(10, powerOfTen));
//                System.out.println("Found value of test: " + range % Math.pow(10, powerOfTen));
            }

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

            return tickPoint;

        }//end method "tickPoint wot takes numbers in"

        public void keyPressed() {

            if (key == 'm' || key == 'M') {
//                resetMaxVal = true;
                maxVal = -99999;
            } else if (key == 'r' || key == 'R') {
                resetValues = true;
//                xMaxVal = -99999;
//                yMaxVal = -99999;
//                xMinVal = 99999;
//                yMinVal = 99999;

                minMaxX.resetVals();
                minMaxY.resetVals();

            } else if (key == 'p' || key == 'P') {
                printScreen = true;
            } else if (key == 'w' || key == 'W') {
                saveVariableStoreClone = true;
            }

        }

        private void triangle(float x, float y, float shapeSize, float heading) {


            smooth();
            float size = shapeSize * 0.8f;

            //equilateral triangle. Coordinates for two bottom angles: x = sqrt(3)/2. y = 1/2. Decimal versions used.
            translate(x, y);
            //add 90 degrees (PI/2 radians) since the arrow by itself is pointing up and 
            //the coordinate system for the PVector has zero pointing along the x axis
            rotate((PI / 2) + heading);
            //see pentagon def in var defs above
            beginShape();
            vertex(0, -1f * (size * 2.3f));
            vertex(0.8660254037844386f * size, 0.5f * size);
            vertex(-0.8660254037844386f * size, 0.5f * size);
            endShape(CLOSE);

            rotate(-((PI / 2) + heading));

            translate(-x, -y);

        }
    }//end of inner class MyPapplet
}
