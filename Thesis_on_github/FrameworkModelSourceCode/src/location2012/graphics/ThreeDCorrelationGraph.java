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
import java.util.ArrayList;
import location2012.Actor;
import location2012.Person;
import location2012.io.staticArrayWriter;
import location2012.utils.FileName;
import location2012.utils.Map;
import location2012.utils.MathsBits;
import location2012.utils.gl;
import location2012.variables.MinMaxValues;
import location2012.variables.VariableStore;
import peasy.PeasyCam;

/**
 *
 * @author Olner Dan
 */
public class ThreeDCorrelationGraph extends Frame implements Listener, PrintPApplet {

    MinMaxValues minMaxX, minMaxY, minMaxZ;
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
    public boolean printScreen = false;
    public boolean saveVariableStoreClone = false;
//    public boolean followMode = false;
    ThreeDCorrelationGraph thisPG;
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
    VariableStore vs_zAxis;
    //Local clone. Needed to avoid concurrency issues
    public ArrayList<Double[]> xAxis = new ArrayList<Double[]>();
    public ArrayList<Double[]> yAxis = new ArrayList<Double[]>();
    public ArrayList<Double[]> zAxis = new ArrayList<Double[]>();
    //cos I'm using foreach loop, need record of which datapoint is being used
    int i = 0;
    //followWidthRemember for toggling this on and off
    int followWidth, followWidthRemember = followWidth;
    int numXPoints, numYPoints, numZPoints;
    //
    Color lowc, highc;

    /*
     * Including an audible and a val for letting Processing know when to draw
     */
//    public TimeSeriesGraph(Drawable drawable, int xsize, int ysize, Audible a, double val) {
    public ThreeDCorrelationGraph(VariableStore vs_x, VariableStore vs_y, VariableStore vs_z,
            int xsize, int ysize, double daysBetweenDraw, int followWidth, int weight) {

        //so I can pass in ref to whole object for inner class.
        thisPG = this;
        this.followWidth = followWidth;

        this.vs_xAxis = vs_x;
        this.vs_yAxis = vs_y;
        this.vs_zAxis = vs_z;

        //Need to change from gl.people
//        minMaxX = new MinMaxValues(null, 10, 2, vs_x.objects, vs_x.varName, true);
//        minMaxY = new MinMaxValues(null, 10, 2, vs_y.objects, vs_y.varName, true);
        minMaxX = new MinMaxValues();
        minMaxY = new MinMaxValues();
        minMaxZ = new MinMaxValues();

        setWeight(weight);

        //set graph position
        x1 = 0.2f;
        y1 = 0.05f;
        x2 = 0.95f;
        y2 = 0.85f;

        lowc = new Color(180, 209, 219);
//        highc = new Color(214,255,216);
//        highc = new Color(112, 204, 55);
        highc = new Color(255, 100, 50);

        //Add listener details to audible
        giveShouterTo(gl.time, daysBetweenDraw);

        this.xsize = xsize;
        this.ysize = ysize;

        setLayout(new BorderLayout());

        //set up inner class where Processing will run
        pa = new MyPapplet();

        setTitle(vs_x.varName + " vs " + vs_y.varName + " vs " + vs_z.varName);

        add(pa, BorderLayout.CENTER);
        makeCloseable();
        pa.init();

        //position at bottom right of screen
        setLocation(pa.screen.width - xsize, pa.screen.height - ysize);
//        setLocation(0, graphPos);
        graphPos += 310;
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

        //testing data, get a CSV output if w pressed
        if (saveVariableStoreClone) {

            saveVariableStoreClone = false;

            staticArrayWriter.write2DArrayOfDoubles(xAxis, "testinGraph");

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
        double xMaxVal = -99999, yMaxVal = -99999, zMaxVal = -99999,
                xMinVal = 99999, yMinVal = 99999, zMinVal = 99999;
        //if lowest value is below zero
        double minVal, useMinVal;
        //testing with Firm data
//    Firm f;
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
        //Double used for getting each point to draw
        Double[] d = new Double[3];
        //if fading out previous values
        //0 = don't
        //Other values = alpha value of over-writing 
        int fade = 0;
        
        PeasyCam cam;

        @Override
        public void setup() {

            size(xsize, ysize, P3D);
            cam = new PeasyCam(this, 100);

            rectMode(CORNERS);

            font = createFont("Arial-BoldMT-14.vlw", (int) width / 35);
            textFont(font);

            textMode(SCREEN);

//            smooth();
            noLoop();

        }

        @Override
        public void draw() {

            //Grab a clone copy of the data
            //Doing this at the start of the draw method as it keeps it within the
            //PApplet thread and avoids weird artefacts 
            xAxis = (ArrayList<Double[]>) vs_xAxis.variableData.clone();
            yAxis = (ArrayList<Double[]>) vs_yAxis.variableData.clone();
            zAxis = (ArrayList<Double[]>) vs_zAxis.variableData.clone();

            if (followWidth > 0) {

                if (xAxis.size() > followWidth) {


                    //keep only those arrays up to followwith
                    //well, that worked. Gotta be better way!
//                    xAxis = new ArrayList(xAxis.subList(xAxis.size() - 1 - followWidth, xAxis.size() - 1));
//                    yAxis = new ArrayList(yAxis.subList(yAxis.size() - 1 - followWidth, yAxis.size() - 1));
//                    zAxis = new ArrayList(zAxis.subList(zAxis.size() - 1 - followWidth, zAxis.size() - 1));
                    xAxis = new ArrayList(xAxis.subList(xAxis.size() - 1, xAxis.size() - 1));
                    yAxis = new ArrayList(yAxis.subList(yAxis.size() - 1, yAxis.size() - 1));
                    zAxis = new ArrayList(zAxis.subList(zAxis.size() - 1, zAxis.size() - 1));

                }

            }

            //call minMaxFinder here
            minMaxX.findMinMaxFromArrayListOfArrayDoubles(xAxis);
            minMaxY.findMinMaxFromArrayListOfArrayDoubles(yAxis);
            minMaxZ.findMinMaxFromArrayListOfArrayDoubles(zAxis);

//            minMaxY.lowVal = 0.63;
//            minMaxY.highVal = 0.68;

//            System.out.println("minMaxY: " + minMaxY.lowVal + "," + minMaxY.highVal);

            numXPoints = xAxis.size();
            numYPoints = yAxis.size();
            numZPoints = zAxis.size();

            //this gets Processing to resize to correct proportion in parent frame
            if (gl.day % 20 == 0) {
                thisPG.setSize(xsize + 1, ysize + 1);
            }

//            fill(255, fade);
            background(255);

//            rect(0, 0, width, height);

            noFill();

            drawData();

            stroke(0);
//            rect(x1 * width, y1 * height, x2 * width, y2 * height);


            if (printScreen) {
                printScreen();
            }
            //mm.addFrame();
            //camera1.feed();

        }

        public void printScreen() {

            printScreen = false;
            saveFrame(FileName.getFileName() + "_"
                    + vs_xAxis.varName + "_vs_" + vs_yAxis.varName + ".png");

        }
        
        private void drawData() {
            
            Person p;
            
            float t = 1000;
            
            for(Actor a: gl.people) {
                
                p = (Person) a;
                
                translate((float) p.maxUtility * t, (float) p.distanceToCentralFirm * t, 
                        (float) p.chosenDensityCost * t);
                
                sphere(5);
                
                translate(-(float) p.maxUtility * t, -(float) p.distanceToCentralFirm * t, 
                        -(float) p.chosenDensityCost * t);
                
                
            }
            
            
        }

        private void drawDataxxx() {

            int actorNum = 0;

            xsf = (x2 - x1);
            ysf = (y2 - y1);
            //And these set the x and y offsets using the first two proportional coordinates
            xo = x1 * width;
            yo = y1 * height;

            //both datasets should be same size in the time axis,            
            //tho not nec in number of objects (since it could be e.g. one value like rho)
            for (int j = 0; j < xAxis.get(0).length; j++) {

                //this dimension of the array is model time
//                for (int k = 0; k < xAxis.size(); k++) {

                    //a bit of hacking to deal with single-value vars like rho
                    //just check if there's available data
                    //p.s. since int j is set to match x axis length
                    //single vals need to be on y axis. 
                    //I repeat, HACKEROO! Needs fixing so it doesn't matter                    
                    d[0] = xAxis.get(0)[j];
                    //if the variable store has only one object its storing for each day...
                    if (yAxis.get(0).length == 1) {

//                        System.out.println("ping!");

                        d[1] = yAxis.get(0)[0];
                    } else {
                        d[1] = yAxis.get(0)[j];
                    }
                    
                    d[2] = zAxis.get(0)[j];

//                    xMaxVal = (d[0] > xMaxVal ? d[0] : xMaxVal);
//                    xMinVal = (d[0] < xMinVal ? d[0] : xMinVal);
//                    yMaxVal = (d[0] > yMaxVal ? d[0] : yMaxVal);
//                    yMinVal = (d[0] < yMinVal ? d[0] : yMinVal);

//                    float x = (width / ((float) minMaxX.highVal - (float) minMaxX.lowVal))
//                            * (d[0].floatValue() - (float) minMaxX.lowVal);
//                    float y = height - (height / ((float) minMaxY.highVal - (float) minMaxY.lowVal))
//                            * (d[1].floatValue() - (float) minMaxY.lowVal);
//                    float z = height - (height / ((float) minMaxZ.highVal - (float) minMaxZ.lowVal))
//                            * (d[2].floatValue() - (float) minMaxZ.lowVal);
//                    float x = (width / ((float) xMaxVal - (float) xMinVal))
//                            * (d[0].floatValue() - (float) xMinVal);
//                    float y = (height / ((float) yMaxVal - (float) yMinVal))
//                            * (d[1].floatValue() - (float) yMinVal);

//                    System.out.println(" ");
//                    System.out.print("minmax x: " + minMaxX.lowVal + "," + minMaxX.highVal);
//                    System.out.println("|| minmax y: " + minMaxY.lowVal + "," + minMaxY.highVal);
//                    System.out.println("Vals: " + d[0] + "," + d[1] + ", x,y: " + x + "," + y);
//                    System.out.println(" ");

                    //colour to indicate time 
//                    fill(Map.mapLowHighToColour_DoublesColorsInColorOut(k, 0,
//                            (double) xAxis.size(), Color.red, Color.green).getRGB());
                    fill(Map.mapLowHighToColour_DoublesColorsInColorOut(0, 0,
                            (double) xAxis.size(), lowc, highc).getRGB());
//                    
                    stroke(0, 50);
//                    noStroke();

                    fill(0);
//                    ellipse(x, y, 5f, 5f);
//                    translate(xo + (x * xsf), yo + (y * ysf), yo + (z * ysf));

                    translate(d[0].floatValue(), d[1].floatValue(), d[2].floatValue());
                    ellipse(0,0, 14f, 14f);
                    translate(-d[0].floatValue(), -d[1].floatValue(), -d[2].floatValue());

//                    translate(-xo + (x * xsf), -yo + (y * ysf), -yo + (z * ysf));
//                    ellipse(xo + (x * xsf), yo + (y * ysf), 14f, 14f);
//                    ellipse(x, y, 5f, 5f);
//                    ellipse(width / 2, height / 2, 5f, 5f);
                    noFill();

//                }//end for k

            }//end for j


//            verticalTickPoints();
//            horizTickPoints();

            //Axis labels
//            textAlign(CENTER);
//            //x axis
//            text(vs_xAxis.varNameLabel, xo + ((width / 2) * xsf), (y2 * height) + font.size * 3);
//
//            float x = xo - (font.size * 4);
//            float y = yo + ((height / 2) * ysf);
////            textAlign(CENTER, BOTTOM);
//
////            float x = width / 2;
////            float y = height / 2;
//
//            textMode(MODEL);
////            noSmooth();
//
//            //nabbed from http://forum.processing.org/topic/vertical-text
//            pushMatrix();
//            translate(x, y);
//            rotate(-HALF_PI);
//            text(vs_yAxis.varNameLabel, 0, 0);
//            popMatrix();
//
////            rotate(HALF_PI);
////            translate(-x, -y);
//
//            textMode(SCREEN);
//            textAlign(RIGHT);
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
                if (yo + (v1 * ysf) < (y2 * height)) {
                    line(xo, yo + (v1 * ysf), xo + (width * xsf), yo + (v1 * ysf));
                    textAlign(RIGHT);
                    stroke(255);
                    text("" + MathsBits.roundToDecimals(i, 2), xo - 4, yo + (v1 * ysf) + 4);
                }

            }

            //negative. This is actually easier than finding the correct negative starting point.
            for (double i = 0; i > minMaxY.lowVal; i -= tickPoint) {

                v1 = height - ((height / ((float) minMaxY.highVal
                        - (float) minMaxY.lowVal)) * (float) (i - minMaxY.lowVal));
                stroke(200);
                line(xo, yo + (v1 * ysf), xo + (width * xsf), yo + (v1 * ysf));

                //i!=0: don't draw zero twice
                if (i != 0) {

                    stroke(255);
                    text("" + MathsBits.roundToDecimals(i, 2), xo - 4, yo + (v1 * ysf) + 4);

                }//end if i

            }//end for double i

            noFill();

            //if minVal < 0, draw the zero line
            if (minMaxY.lowVal < 0) {
                stroke(255, 100, 100, 70);

                v1 = height - ((height / ((float) minMaxY.highVal - (float) minMaxY.lowVal))
                        * (float) (0 - minMaxY.lowVal));
                line(xo, yo + (v1 * ysf), xo + (width * xsf), yo + (v1 * ysf));

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
                if (xo + (v1 * xsf) > xo) {
                    line(xo + (v1 * xsf), yo + (height * ysf), xo + (v1 * xsf), yo);
                    textAlign(RIGHT);
                    stroke(255);
                    text("" + MathsBits.roundToDecimals(i, 2), xo + (v1 * xsf),
                            yo + (height * ysf) + font.size);
                }

            }


            //negative. This is actually easier than finding the correct negative starting point.
            for (double i = 0; i > minMaxX.lowVal; i -= tickPoint) {

                v1 = ((width / ((float) minMaxX.highVal
                        - (float) minMaxX.lowVal)) * (float) (i - minMaxX.lowVal));
                stroke(200);
                line(xo + (v1 * xsf), yo + (height * ysf), xo + (v1 * xsf), yo);

                //i!=0: don't draw zero twice
                if (i != 0) {

                    stroke(255);
                    text("" + MathsBits.roundToDecimals(i, 2), xo + (v1 * xsf), yo
                            + (height * ysf) + font.size);

                }//end if i

            }//end for double i

            noFill();

            //if minVal < 0, draw the zero line
            if (minMaxX.lowVal < 0) {
                stroke(255, 100, 100, 70);

                v1 = ((width / ((float) minMaxX.highVal
                        - (float) minMaxX.lowVal)) * (float) (0 - minMaxX.lowVal));

                line(xo + (v1 * xsf), yo + (height * ysf), xo + (v1 * xsf), yo);

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
//                resetValues = true;
//                xMaxVal = -99999;
//                yMaxVal = -99999;
//                xMinVal = 99999;
//                yMinVal = 99999;

                minMaxX.resetVals();
                minMaxY.resetVals();

            } else if (key == 'f' || key == 'F') {

                if (followWidth == 0) {
                    followWidth = followWidthRemember;
                } else {
                    followWidth = 0;
                }


            } else if (key == 'p' || key == 'P') {
                printScreen = true;
            } else if (key == 'w' || key == 'W') {
                saveVariableStoreClone = true;
            }

        }
    }//end of inner class MyPapplet
}
