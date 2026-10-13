/*
 * 
 */
package coremodelviz;

import java.util.ArrayList;
import processing.core.*;

/**
 * Main class used also as Main PApplet window. Can drop in as Applet (in
 * theory.)
 *
 * @author Dan
 */
public class Main extends PApplet {

    //Exogenously set variables; using simple letters, but note their greek name
    //versions for comparing to the analytic model
    //delta: fraction of income spent on manufactures;
    //also normalised so that d = g, where g is the fraction of workers in manufacturing
    //Krugman assumes that right from the start; Brakman et al go at it rather more circuitously
    double d;
    //epsilon: elasticity of substitution (as well as, because of the way the model's structured,
    //price elasticity of demand and economies of scale. Phew!)
    //e = 1/1-p - where p [rho] is "love of variety" that can be used in the manufacture-demand CES
    //0 < p < 1: 1 is perfect substitutes, anything down to zero, imperfect substitutes.
    double e;
    //transport costs, iceberg form. E.g. if T = 1.7 (the default value in Brakman)
    //1 unit 'arrives' of a good for 1.7 produced. 
    double t;
    //Total amount of labour - pretty much always going to keep this to 1. 
    double L;
    //split of agri workers between regions. Only doing the two region version here, so
    //setting even across the two regions
    double phi;
    //OTHER MODEL VARS
    //Lambda: split of manufacturing workers between regions. This parameter is swept
    //See ites below...
    double la;
    //Income in region x
    double[] Y;
    //Price index for manufactures
    double[] I;
    //Wage rate for each region
    double[] W;
    //for storing 'last wage' for break condition comparison
    double[] lastW;
    //Real wage rate for the two regions, to be used to compare regions
    double[] wage;
    //CODE VARIABLES
    //Stopping criterion: if the difference between wage rates found between iterations is less than this,
    //Stop the model run
    double breakPoint;
    //for breaking out of iteration
    boolean dontstop = true;
    //number of iterations to break lambda sweep into - how many steps to take to get from la = 0 to la = 1
    int lambdaIterations = 500;
    //If drawing tomahawk, number of iterations to break transport cost sweep into
    //Transport cost start and end values vary 
    int transportIterations = 500;
    //ArrayList for keeping all data for equilibrium points for tomahawk diagram
    ArrayList<TomahawkDatum> tomahawkData = new ArrayList<TomahawkDatum>();
    //low and high sweep vals for transport if tomaHawk
    double transportLow = 1, transportHigh = 3.5;
    //For storing calculated relative wage rates before drawing them
    double realWageRatio[] = new double[lambdaIterations];
    //other arrays for seeing what other vars do...
    double income[] = new double[lambdaIterations];
    float zoom = 5000;
    int viewm = lambdaIterations / 2;
    PFont font;
    //For drawing the data and sticking inside the axes
    PGraphics p;
    PImage pi;
    //Relative size of graph where 1 = same size as main window
    float graphX = 0.8f, graphY = 0.7f;
    //for easier referencing of graph edges
    float leftEdge, topEdge;
    //
    boolean printScreen = false;
    //Draw short run equilibrium or tomahawk for a range of transport values?
    boolean drawShortRun = false;
    //Get tomahawk data once, store. Remember. Don't do again unless something changes
    boolean tomahawkDataCalculated = false;
    //to save the first output PGraphics as an image
    boolean savePGraphicsImage = false;
    //to use previously saved image instead of calculating data directly
    boolean usePGraphicsImage = false;
    //Index for sweeping transport costs for the tomahawk calculation
    //Used instead of for loop to allow Processing to draw after each calculation
    int tomaHawkCalcIndex = 0;

    @Override
    public void setup() {

        //set exogenous vars
        d = 0.5;
        e = 5;
        t = 2.25;
        L = 1;
        phi = 0.5;

        //la = 0.5;

        //set model run vars
        breakPoint = 0.00005;

        //set up regional variables
        Y = new double[2];
        I = new double[2];
        W = new double[2];
        wage = new double[2];
        lastW = new double[2];

        // The font must be located in the sketch's 
        // "data" directory to load successfully
        font = loadFont("ArialMT-12.vlw");
        textFont(font, 48);
//        textMode(SCREEN);

        //set up graphics
        stroke(0);
        size(1000, 800);

        p = createGraphics((int) ((float) width * graphX), (int) ((float) height * graphY), JAVA2D);

//        System.out.println(p.width + ", " + p.height);
//        System.out.println(width + ", " + height);
        leftEdge = (width / 2) - (p.width / 2);
        topEdge = (height / 2) - (p.height / 2);

    }

    @Override
    public void draw() {

        if (drawShortRun) {
            drawShortRun();
        } else {
            drawTomahawk();
        }


        if (printScreen) {
            System.out.println("printing");
            printScreen = false;
            if (drawShortRun) {
                saveFrame("C://saveframes/CM_T=" + t + ".png");
            } else {
                saveFrame("C://saveframes/tomaHawk.png");
            }
        }

    }//draw

    void drawShortRun() {

        background(255);
        //do a model run
        runSim();

        //now we have data for that run, draw. 
//        drawData();
        //Shift it all up a little
        translate(width * 0.03f, -height * 0.1f);

        drawShortRunDataInPGraphics();

        //position in centre
        image(p, leftEdge, topEdge, p.width, p.height);
        //Surround with box
        strokeWeight(2);
        noFill();
        rect(leftEdge, topEdge, p.width, p.height);

        strokeWeight(3);
        fill(0);
        //Bottom lambda axis (0, 0.5, 1)
        //zero
        line(leftEdge, topEdge + p.height, leftEdge, topEdge + p.height + 10);
        textAlign(CENTER, TOP);
        textSize(30);
        text("0", leftEdge, topEdge + p.height + 15);
        //0.5, assuming the graph is centred correctly
        line(width / 2, topEdge + p.height, width / 2, topEdge + p.height + 10);
        text("0.5", width / 2, topEdge + p.height + 15);
        //1
        line(leftEdge + p.width - 1, topEdge + p.height, leftEdge + p.width - 1, topEdge + p.height + 10);
        text("1", leftEdge + p.width - 1, topEdge + p.height + 15);

        //x axis label
        textSize(25);
        text("share of mobile workforce in region 1 (lambda[1])", width / 2, topEdge + p.height + 70);

        //y axis (ratio of w1/w2)
        //nabbed from http://forum.processing.org/topic/vertical-text
        pushMatrix();
        translate(10, height / 2);
        rotate(-HALF_PI);
        text("ratio of w1/w2", 0, 0);
        popMatrix();

        textSize(30);

        pushMatrix();
        translate(width * 0.065f, (height / 2) - (height * 0.1f));
        rotate(-HALF_PI);
        text("> 1", 0, 0);
        popMatrix();

        pushMatrix();
        translate(width * 0.065f, (height / 2) + (height * 0.1f));
        rotate(-HALF_PI);
        text("< 1", 0, 0);
        popMatrix();

    }

    void drawTomahawk() {

        background(255);
        textAlign(CENTER, TOP);

        //create data first. Only once initially.
        if (!tomahawkDataCalculated && !usePGraphicsImage) {
            calculateTomahawkData();
        }

        //Shift it all up a little
        translate(width * 0.03f, -height * 0.1f);


        //position in centre
        //use saved image or use directly calculated data?
        if (usePGraphicsImage) {
            pi = loadImage("C://saveframes/PGraphicsOutput.png");
            image(pi, leftEdge, topEdge, p.width, p.height);
        } else {
            drawTomahawkInPGraphics();
            image(p, leftEdge, topEdge, p.width, p.height);
        }
        //Surround with box
        strokeWeight(2);
        noFill();
        rect(leftEdge, topEdge, p.width, p.height);

        fill(0);

        //Draw axes

        //x axis
        strokeWeight(3);
        fill(0);

        //x axis label
        textSize(25);
        text("transport cost", width / 2, topEdge + p.height + 70);

        line(leftEdge, topEdge + p.height, leftEdge, topEdge + p.height + 10);
        textAlign(CENTER, TOP);
        textSize(30);
        text("" + transportLow, leftEdge, topEdge + p.height + 15);
        //0.5, assuming the graph is centred correctly
        line(width / 2, topEdge + p.height, width / 2, topEdge + p.height + 10);
        text("" + (transportLow + ((transportHigh - transportLow) / 2)), width / 2, topEdge + p.height + 15);
        //1
        line(leftEdge + p.width - 1, topEdge + p.height, leftEdge + p.width - 1, topEdge + p.height + 10);
        text("" + transportHigh, leftEdge + p.width - 1, topEdge + p.height + 15);

        textSize(25);

        //y axis (lambda with zero at the bottom.)
        //nabbed from http://forum.processing.org/topic/vertical-text
        pushMatrix();
        translate(-5, height / 2);
        rotate(-HALF_PI);
        text("share of workers in region 1", 0, 0);
        popMatrix();

        textAlign(CENTER, CENTER);

        float textXAdjust = 25;

        line(leftEdge - 10, topEdge + p.height, leftEdge, topEdge + p.height);
//        textAlign(CENTER, TOP);
        textSize(30);
        text("0", leftEdge - textXAdjust, topEdge + p.height);
        //0.5, assuming the graph is centred correctly
        line(leftEdge - 10, topEdge + (p.height / 2), leftEdge, topEdge + (p.height / 2));
        text("0.5", leftEdge - textXAdjust - 10, topEdge + (p.height / 2));
        //1
//        line(leftEdge - 10, topEdge + p.height, leftEdge + p.width - 1, topEdge + p.height + 10);
        line(leftEdge - 10, topEdge, leftEdge, topEdge);
        text("1", leftEdge - textXAdjust, topEdge);


        //draw a line to indicate central transport value
        strokeWeight(1);
        line((width / 2) - 2, topEdge + p.height, (width / 2) - 2, topEdge);
        line((width / 2) + 2, topEdge + p.height, (width / 2) + 2, topEdge);
//        line(width / 2, topEdge + p.height, width / 2, topEdge);

    }

    void drawTomahawkInPGraphics() {

        p.beginDraw();
        p.rectMode(CENTER);
        p.noStroke();

        //positioning for datapoints:
        //Lambda on y axis, zero (all in region 2) at the bottom.
        //Transport costs on x axis, spread between set values 
        //of tranportLow and transportHigh

        //Iterate over all found tomaHawk datapoints
        for (TomahawkDatum td : tomahawkData) {

            //Mark stable equilibria in black
            if (td.stable) {
                p.fill(0);
            } else {
                p.fill(200);
            }

            //translate lambda and transport values to PGraphics coordinates
            float x = (float) (p.width * (td.t - transportLow) / (transportHigh - transportLow));
            float y = (float) (p.height * (td.l));
            //shrink y a little to show region stable equilibria more clearly
            float shrinkFactor = 0.97f;
            y *= shrinkFactor;
            y += ((p.height / 2) * (1 - shrinkFactor));

            //Draw the equilibrium point
            p.ellipse(x, y, 15, 15);

        }//foreach

        p.noFill();

        p.endDraw();

        if (savePGraphicsImage) {
            savePGraphicsImage = false;
            p.save("C://saveframes/PGraphicsOutput.png");
        }

    }

    void calculateTomahawkData() {

        if (tomaHawkCalcIndex == transportIterations) {
            tomahawkDataCalculated = true;
        }

        //for each value of transport costs
//        for (int ts = 0; ts < transportIterations; ts++) {
//set transport cost, spread between iterations
        t = transportLow
                + (((transportHigh - transportLow) / (double) transportIterations)
                * tomaHawkCalcIndex++);

        //don't check 1, datapoint is false
        t = (t == 1 ? 1.01 : t);

        System.out.println("T = " + t);

        //get array of short-run data stored in realWageRatio[]
        runSim();

        //check lambda at zero for stability. 
        //for the extremes, this only means the wage dynamic is pointing away from the centre
        //It may not be crossing w1/w2=1 
        //One will be stable if zero is, so only need the one check
        //If the real wage ratio at lambda=0 is less than one
        //Then wages in region 2 are higher and the region is a stable core
        if (realWageRatio[0] < 1) {
            tomahawkData.add(new TomahawkDatum(0, t, true));
            tomahawkData.add(new TomahawkDatum(1, t, true));
//                System.out.println("stable");
        } else {
            //otherwise the two regions are unstable
            tomahawkData.add(new TomahawkDatum(0, t, false));
            tomahawkData.add(new TomahawkDatum(1, t, false));
        }

        //We know if the regions are unstable the centre will be stable
        //but let's find that from the data itself
        //Searching for actual wage equilibrium points where w1/w2 = 1
        double lastWageRatio = -1;
        for (int i = 0; i < lambdaIterations; i++) {

//                System.out.println("ratio: " + realWageRatio[i]);

            if (i > 0) {

                //Subtracting 1 from the ratio makes the equilibrium value zero
                //If the last value OR the current value are either side of zero
                //multiplying them will produce a negative number
                //Meaning it passed an equilibrium point between turns
                //If they're of the same polarity, it'll end up positive
                if ((lastWageRatio - 1) * (realWageRatio[i] - 1) < 0) {

                    //If we've found an equilibrium point
                    //check if it's stable or not
                    //To be stable, the wage each side needs to be moving people
                    //back to the same point
                    //This only happens where:
                    //wage ratio > 1 for the lower value of lambda
                    //i.e. w1/w2 > 1 or real wages are higher in region 1
                    //pushing workers "to the right" towards region 1
                    //The stable value should only be at lambda = 0.5...
                    if (lastWageRatio > 1) {
                        tomahawkData.add(new TomahawkDatum((double) i / lambdaIterations, t, true));
//                            System.out.println("stable");
                    } else {
                        tomahawkData.add(new TomahawkDatum((double) i / lambdaIterations, t, false));
//                            System.out.println("unstable");
                    }

                }//if

            }//if

            lastWageRatio = realWageRatio[i];

        }//for checkLambdas

//        }//for ts

    }

    void drawShortRunDataInPGraphics() {

        p.beginDraw();

        p.smooth();
        p.background(255);

        //Mark top and bottom half with arrows indicating where gravity of real wage is pulling
        //Do this first so line draws over the top
        p.noStroke();
        p.fill(200);
        p.rectMode(CENTER);
        //boolean: true = point arrow left
        //top arrow first
        triangle(p.width / 2, p.height / 4, 100f, false);
        p.rect((p.width / 2) - (p.width * 0.075f), p.height / 4, 200, 60);
        //bottom arrow
        triangle(p.width / 2, p.height * 0.75f, 100f, true);
        p.rect((p.width / 2) + (p.width * 0.075f), p.height * 0.75f, 200, 60);

        p.stroke(0);

        //midpoint marker; two lines to match long-run
        p.stroke(0);
        p.strokeWeight(1);
        p.line(0, (p.height / 2) - 2, p.width, (p.height / 2) - 2);
        p.line(0, (p.height / 2) + 2, p.width, (p.height / 2) + 2);


        //draw a line for each data point, assuming relative wage rate is 1 at centre of screen. 
        //Left to right is lambda, the parameter we're sweeping
        for (int i = 1; i < lambdaIterations; i++) {


            //data line
            p.stroke(0);
            p.strokeWeight(6);
            //line((i - 1)*(width/ites), height - ((float) data[i-1] * (height/2)), (i)*(width/ites), height-((float) data[i] * (height/2)));

            //On making the relative wage rate fit on screen: 
            //subtract datapoint from 1 (so that values of 1 are zero)
            //multiply it by a factor to 'zoom'
            //add to centrepoint of screen (height/2)
            p.line((i - 1) * ((float) p.width / (float) lambdaIterations),
                    (float) p.height / 2 + (zoom * (1 - (float) realWageRatio[i - 1])),
                    (i) * ((float) p.width / (float) lambdaIterations),
                    (float) p.height / 2 + (zoom * (1 - (float) realWageRatio[i])));

        }

        p.endDraw();

    }//end drawDataInPGraphics method

    void runSim() {

        //Run iterations across a range of la
        for (int m = 0; m < lambdaIterations; m++) {
//        for (int m = 1; m < lambdaIterations - 1; m++) {

            //We're sweeping lambda between 0 and 1
            la = ((double) m / (double) lambdaIterations);

            //Set initial wage values to start iteration
            W[0] = 0.01;
            W[1] = 0.01;
            dontstop = true;

            int count = 0;

            //number of iterations taken to find a suitable W
            //start iteration
            while (dontstop) {

//                System.out.println(count++);

                //Just doing two regions here, so no for loop...

                //Find Y0
                Y[0] = (d * la * W[0]) + ((1 - d) * phi);
                //test with no agri
//                Y[0] = (d * la * W[0]);

                //Find I0
                I[0] =
                        //region 1: no transport costs within the region
                        (la * Math.pow(W[0], 1 - e))
                        + //region 2: transport costs to other region
                        ((1 - la) * (Math.pow(t, 1 - e)) * Math.pow(W[1], 1 - e));

                //raise to power
                I[0] = Math.pow(I[0], 1 / (1 - e));

                //Find new W0
                //Remember last one too
                lastW[0] = W[0];

                W[0] =
                        //region 1: no transport costs in own region
                        (Y[0] * Math.pow(I[0], e - 1))
                        + //region 2
                        (Y[1] * Math.pow(t, 1 - e) * Math.pow(I[1], e - 1));

                //raise to power
                W[0] = Math.pow(W[0], 1 / e);



                //Find Y1
                Y[1] = (d * (1 - la) * W[1]) + ((1 - d) * (1 - phi));
                //test with no agri
//                Y[1] = (d * (1 - la) * W[1]);

                //Find I1 - change transport costs
                I[1] =
                        //region 1
                        (la * (Math.pow(t, 1 - e)) * Math.pow(W[0], 1 - e))
                        + //region 2: no transport costs to own region
                        ((1 - la) * Math.pow(W[1], 1 - e));

                //raise to power
                I[1] = Math.pow(I[1], 1 / (1 - e));

                //Find new W1
                //Remember last one too
                lastW[1] = W[1];

                W[1] =
                        //region 1
                        (Y[0] * Math.pow(t, 1 - e) * Math.pow(I[0], e - 1))
                        + //region 2: no transport costs in own region
                        (Y[1] * Math.pow(I[1], e - 1));

                //raise to power
                W[1] = Math.pow(W[1], 1 / e);



                if (Math.abs(W[0] - lastW[0]) / lastW[0] < breakPoint
                        && Math.abs(W[1] - lastW[1]) / lastW[1] < breakPoint) {
                    dontstop = false;
                } else {
//                    System.out.println("not stopping! " + count++);
                }

            }//end while

            //calculate relative real wage rate
            wage[0] = W[0] * Math.pow(I[0], -d);
            wage[1] = W[1] * Math.pow(I[1], -d);

            realWageRatio[m] = (double) wage[0] / wage[1];

            income[m] = (double) W[0] / W[1];

        }//end for loop, runs through iterations

    }//end runsim method

    public void keyPressed() {

        if (key == CODED) {
            if (keyCode == UP) {
                t += 0.05;
                //values < 1 make no sense. 
                //T: "the number of goods that need to be shipped to ensure that one unit of a variety arrives"
                t = (t < 1 ? 1 : t);
                System.out.println("T = " + t);

            } else if (keyCode == DOWN) {

                t -= 0.05;
                t = (t < 1 ? 1 : t);
                System.out.println("T = " + t);

            } else if (keyCode == LEFT) {
                d += 0.05;
            } else if (keyCode == RIGHT) {
                d -= 0.05;
            }
        }

        if (key == 'k' || key == 'K') {
            zoom -= 100;
        } else if (key == 'l' || key == 'L') {
            zoom += 100;
        } else if (key == 'p' || key == 'P') {
            printScreen = true;
            //swap between short and long run.
            //If long-run, redraw
        } else if (key == 'a' || key == 'A') {

            drawShortRun = (drawShortRun ? false : true);
            tomahawkData.clear();
            tomahawkDataCalculated = false;
            tomaHawkCalcIndex = 0;

            p = createGraphics((int) ((float) width * graphX), (int) ((float) height * graphY), JAVA2D);
            
        }

    }//end keypressed method

    private void triangle(float x, float y, float size, boolean left) {

//        p.smooth();
//        size *= 0.8;

        //equilateral triangle. Coordinates for two bottom angles: x = sqrt(3)/2. y = 1/2. Decimal versions used.
        p.translate(x, y);

        if (left) {
            p.rotate(-p.PI / 2);
        } else {
            p.rotate(p.PI / 2);
        }
        //see pentagon def in var defs above
        p.beginShape();
        p.vertex(0, -1f * size);
        p.vertex(0.8660254037844386f * size, 0.5f * size);
        p.vertex(-0.8660254037844386f * size, 0.5f * size);
        p.endShape(p.CLOSE);


        if (left) {
            p.rotate(p.PI / 2);
        } else {
            p.rotate(-p.PI / 2);
        }

        p.translate(-x, -y);

//        p.noSmooth();

    }

    public static void main(String[] args) {
        PApplet.main(new String[]{"coremodelviz.Main"});
    }

    //inner class for storing tomaHawk datapoints
    private class TomahawkDatum {

        double l, t;
        boolean stable;

        /**
         * Store equilibrium points. Take in lambda value, transport cost value
         * and boolean for stable/unstable (true = stable)
         */
        public TomahawkDatum(double l, double t, boolean stable) {

            this.t = t;
            this.l = l;
            this.stable = stable;

        }
    }
}
