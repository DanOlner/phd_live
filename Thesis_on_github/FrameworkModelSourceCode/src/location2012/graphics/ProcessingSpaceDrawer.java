/*
 * Processing Drawer - takes in an array of drawables and...
 * ??? Something to tell it if it's constant draw3D, or called by timeline, etc
 *
 * Question: does it need to run on its own thread? May have to implement runnable
 * Or maybe applets do that already, I forget
 *
 */
package location2012.graphics;

import location2012.observe.Audible;
import location2012.observe.Listener;
import location2012.observe.Shouter;
import location2012.observe.ShoutEvent;
import location2012.control.EoSCurveMouseChanger;
import location2012.control.VarChanger;
import location2012.control.SinglePersonPriceMouseChanger;
import location2012.control.UtilityNoiseChanger;
import location2012.control.AllFirmsPriceMouseChanger;
import location2012.control.AllFirmsDelCostMouseChanger;
import location2012.control.AllFirmsWageMouseChanger;
import location2012.control.DensityCostMouseChanger;
import location2012.control.MouseButtons;
import location2012.control.UtilityMouseChanger;
import location2012.control.CommuteCostMouseChanger;
import location2012.control.SingleActorsDelCostMouseChanger;
import controlP5.ControlEvent;
import controlP5.ControlFont;
import controlP5.ControlP5;
import controlP5.MultiList;
import controlP5.MultiListButton;
import controlP5.Textlabel;
import location2012.utils.gl;
import processing.core.*;
import java.awt.BorderLayout;
import java.awt.Frame;
import java.awt.event.*;
import java.util.ArrayList;
import location2012.Actor;
import location2012.Firm;
import location2012.FirmActDummy;
import location2012.SingleActorPerson;
import location2012.control.AllFirmsMarkupMouseChanger;
import location2012.control.EoSMagnitudeMouseChanger;
import location2012.control.SingleFirmMarkupMouseChanger;
import location2012.control.SingleFirmPriceMouseChanger;
import location2012.control.SomeFirmsPriceMouseChanger;
import location2012.control.TimeDelayMouseChanger;
import location2012.econs.CESOneTypeUtility;
import location2012.econs.UtilityShell;
import location2012.geog.SpaceTools;
import location2012.observe.Timeline;
import location2012.utils.*;

/**
 *
 * @author gy06do
 */
public class ProcessingSpaceDrawer extends Frame implements Listener, PrintPApplet {

    public boolean setup = false;
    public static float hRotateVal = 0;
    public static float vRotateVal = 0;
    int xsize, ysize;
    MyPapplet processor;
    //MovieMaker mm;
    ControlP5 controlP5;
    //PeasyCam cam;
    //Array for drawables
    ArrayList<Drawable> drawables = new ArrayList<Drawable>();
    //weight
    int weight;
    Mouse1 mouse1;
    //list of varchangers to be assignable to mousebuttons
    ArrayList<VarChanger> mouseButtonVarChangers = new ArrayList<VarChanger>();
    //The places they will be added
    MultiList mList;
    MultiListButton m1, m2, m3;
    //used for setting number refs
    int b1 = 1, b2 = 1;
    //Actual mousebutton controller interface doobrie.
    //controlP5 can assign a MouseButtons to either
//    MouseButtons left;
//    MouseButtons right;
    public static VarChanger left;
    public static VarChanger right;
    //need to cast back to this to get/set the value
    VarChanger v;
    MouseButtons mb;
    ProcessingSpaceDrawer thisPSD;
    private boolean printScreen = false;
    SingleActorPerson sp;
    //power to raise mouse drag input by. Changed with left/right cursor
    int multiplier = 1;
//    double powerVal;
    public static PFont presentFont;

    //not used
    private enum Mouse1 {

        Wage,
        Model,
        Goods,
        People;
    }

    /*
     * Including an audible and a val for letting Processing know when to draw3D
     */
    public ProcessingSpaceDrawer(ArrayList<Drawable> drawables, int xsize, int ysize,
            Audible a, double val, int weight) {

        thisPSD = this;

        setWeight(weight);

        //Add listener details to audible
        giveShouterTo(a, val);

        this.drawables = drawables;

        this.xsize = xsize;
        this.ysize = ysize;

        //set global ref to viz size too
        gl.pappletWidth = xsize;
        gl.pappletHeight = ysize;

        setLayout(new BorderLayout());

        //set up inner class where Processing will run
        processor = new MyPapplet();

        //Need to set up controls here so they can be added from Main

        add(processor, BorderLayout.CENTER);
        makeCloseable();
        processor.init();

        setLocation(0, 0);
        //these additions arrived at through trial and error
        //without them, there's some cut-off of view
        setSize(xsize + 16, ysize + 37);

        setVisible(true);

    }

    //just for making frame window closeable. 
    private void makeCloseable() {

        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent evt) {

                //Close this Frame and the Processing window in it
                processor.stop();
                dispose();

                //option: close everything
                System.exit(0);

            }
        });

    }

    public void giveShouterTo(Audible a, double val) {

        a.registerShouter(new Shouter(this, val));

    }

    /*
     * Heard: so run through all registered drawables and draw3D them
     */
    public void heard(ShoutEvent s) {

        gl.someoneHasBaton = true;

//        if(gl.day < 9) {
        if (false) {
            printScreen = true;
        }

//        gl.processingHasDrawn = false;

        processor.redraw();

//        gl.processingHasDrawn = true;

    }

    public void setWeight(int weight) {

        this.weight = weight;
    }

    public int getWeight() {

        return weight;

    }

    /**
     *
     * @param v
     */
    public void addMouseButtonVarChanger(VarChanger v) {

        mouseButtonVarChangers.add(v);

        System.out.println("size of mouseButtonVarChangers:" + mouseButtonVarChangers.size());

        MouseButtons b;
        b = (MouseButtons) v;

        System.out.println(b.getName());

        if (b.useLeft()) {
            m1.add(b.getName() + "left", 10 + b1++).setLabel(b.getName());
        }

        if (b.useRight()) {
            m2.add(b.getName() + "right", 20 + b2++).setLabel(b.getName());
        }

        System.out.println("b1 and b2: " + b1 + "," + b2);

    }

    /**
     * If printing all, call here first.
     */
    @Override
    public void printScreen() {

        printScreen = true;

    }

    //Inner class for running Processor
    public class MyPapplet extends PApplet {

//        ParticleSystem physics;
        PFont font, font2;
        Textlabel myTextlabelA;
        Textlabel myTextlabelB;
        MultiList mList, mList2;
//        RadioButton r1;
        //offscreen buffer
        PGraphics pg;
        //hack for gentle raising of rho
        boolean incRho = false;
        //assuming CES...
        CESOneTypeUtility util = (CESOneTypeUtility) UtilityShell.u;

        @Override
        public void setup() {

            pg = createGraphics(xsize, ysize, JAVA2D);

            if (gl.threeDview) {
                size(xsize, ysize, P3D);
            } else {
                size(xsize, ysize);
            }
//            smooth();

            //camera1 = new Camera(this, 0, 0, 750);
            //camera1.arc(0.7f);
            //camera1.boom(100f);

//            mm = new MovieMaker(this, width, height, "drawing.mov",
//            30, MovieMaker.H263, MovieMaker.MEDIUM);

            colorMode(RGB);

//            if (gl.space == gl.SpaceType.Line) {
//                font = createFont("Arial-BoldMT-48.vlw", 48);
////                font = loadFont("ArialMT12.vlw");
//                textFont(font, 24);
//            }

//            hint(ENABLE_NATIVE_FONTS);

            //these appear to be making no difference to what font actually gets displayed
//            font = createFont("Arial-BoldMT-14.vlw", 14);
//            font = createFont("Arial-Black-14.vlw", 14);
//            font = createFont("AgencyFB-Reg-14.vlw", 15);
            font = createFont("AgencyFB-Reg-14.vlw", 15);
            font2 = createFont("AgencyFB-Reg-48.vlw", 20);
            presentFont = createFont("Arial-BoldMT-14.vlw", (int) height / 25);
            textFont(font, 15);
//            pg.textFont(font, 15);

//            textMode(SCREEN);


            setUpControls();
            noLoop();

            setup = true;

        }

        private void setUpControls() {

            controlP5 = new ControlP5(this);
            controlP5.setColorLabel(0);
            controlP5.setColorBackground(200);

//            controlP5.setControlFont(new ControlFont(createFont("ArialMT", 20, false), 20));
//            controlP5.addButton("mouse1", 0, width - 130, height - 100, 100, 25).setId(1);
//            controlP5.addButton("mouse2", 0, width - 130, height - 70, 100, 25).setId(2);
            controlP5.setAutoDraw(false);

//            myTextlabelA = controlP5.addTextlabel("mouse1label", "current mouse1", width - 270, height - 90);

            mList = controlP5.addMultiList("myList", 0, 10, 100, 20);
//            mList2 = controlP5.addMultiList("list2", 0, 50, 100, 20);

            m1 = mList.add("mouse1", 1);
            m2 = mList.add("mouse2", 2);
//            m3 = mList2.add("space", 3);
//            m3.add("line", 31).setLabel("line");
//            m3.add("plane", 32).setLabel("plane");

            //some controlP5 things are better done here
            controlP5.setControlFont(new ControlFont(font, 15));
            controlP5.addToggle("peoplemove", gl.peopleMobile, 0, 70, 20, 20);
            controlP5.addToggle("firmsmove", gl.firmsMobile, 0, 110, 20, 20);
            controlP5.addToggle("wageTime", gl.peopleWageDistanceCost, 0, 150, 20, 20);

            //just for the one example
//            controlP5.addToggle("stockzero", gl.setStockToZero, 0, 190, 20, 20);
            controlP5.addToggle("reset", gl.setStockToZero, 0, 190, 20, 20);


            //HACKEROO! BOING!
            if (gl.singleActor) {
                addMouseButtonVarChanger(new SingleActorsDelCostMouseChanger(null, 1, gl.people, "deliv", true, true));
                addMouseButtonVarChanger(new EoSCurveMouseChanger(null, 1, "EoS_C", true, true));
                addMouseButtonVarChanger(new EoSMagnitudeMouseChanger(null, 1, "EoS_M", true, true));
                addMouseButtonVarChanger(new SinglePersonPriceMouseChanger(null, 1, gl.people, "1cost", true, true));
            } else if (gl.transmissionFirms) {
                addMouseButtonVarChanger(new SingleFirmPriceMouseChanger(null, 1, "1cost", true, true));
                addMouseButtonVarChanger(new EoSCurveMouseChanger(null, 1, "EoS_C", true, true));
                addMouseButtonVarChanger(new EoSMagnitudeMouseChanger(null, 1, "EoS_M", true, true));
                addMouseButtonVarChanger(new SingleFirmMarkupMouseChanger(null, 1, "s_markup", true, true));
                addMouseButtonVarChanger(new AllFirmsMarkupMouseChanger(null, 1, gl.firms, "markup", true, true));
                addMouseButtonVarChanger(new AllFirmsDelCostMouseChanger(null, 1, gl.firms, "deliv", true, true));
                addMouseButtonVarChanger(new SomeFirmsPriceMouseChanger(null, 1, gl.firms, "some_price", true, true));
//                addMouseButtonVarChanger(new SomeFirmsPriceMouseChanger(null, 1, gl.firms, "some_price", true, true));
            } else {
                addMouseButtonVarChanger(new SingleFirmPriceMouseChanger(null, 1, "1cost", true, true));
                addMouseButtonVarChanger(new AllFirmsDelCostMouseChanger(null, 1, gl.firms, "deliv", true, true));
                addMouseButtonVarChanger(new AllFirmsWageMouseChanger(null, 1, gl.firms, "wage", true, true));
                addMouseButtonVarChanger(new AllFirmsPriceMouseChanger(null, 1, gl.firms, "price", true, true));
            }

            addMouseButtonVarChanger(new CommuteCostMouseChanger(null, 1, gl.firms, "commute cost", true, true));
//            addMouseButtonVarChanger(new FirstFirmPositionMouseChanger(null, 1, gl.firms, "x1firm", true, true));
//            addMouseButtonVarChanger(new GreedyAl_IterationsMouseChanger(null, 1, "g-ites", true, true));
//            addMouseButtonVarChanger(new SomeFirmsDelCostMouseChanger(null, 1, gl.firms, "some_del", true, true));
            addMouseButtonVarChanger(new TimeDelayMouseChanger(null, 1, "delay", true, true));
            addMouseButtonVarChanger(new UtilityNoiseChanger(null, 1, "noise", true, true));
            addMouseButtonVarChanger(new UtilityMouseChanger(null, 1, "CES", true, true));

//            addMouseButtonVarChanger(new SetPeopleInRandomCircleMouseChanger(null, 1, gl.people, "people", true, true));
//            addMouseButtonVarChanger(new ModelViewMouseChanger(null, 1, "model", true, true));
            addMouseButtonVarChanger(new DensityCostMouseChanger(null, 1, "density", true, true));
//            addMouseButtonVarChanger(new UtilityVisMouseChanger(null, 1, "uViz", true, true));
//            addMouseButtonVarChanger(new LineVisMouseChanger(null, 1, "lViz", true, true));


            setDefaultMouseControls("CES", "deliv");
//            setDefaultMouseControls("EoS_C", "EoS_M");

        }

        public void controlEvent(ControlEvent evt) {

//          //check it's not event from parent list
            if (!evt.controller().name().equals("myList")) {

                //check through mouseButtonVarChangers array first
                for (VarChanger v : mouseButtonVarChangers) {

                    mb = (MouseButtons) v;

//                    System.out.println("Mouse event reassignment: " + evt.value() + ", " + evt.stringValue() + ", " + evt.controller().name()
//                            + "," + evt.controller().captionLabel());

//                    System.out.println("mb getName: " + mb.getName());

                    if (mb.getName().equals(evt.controller().captionLabel().getText())) {

                        //check mousebuttons prefs and assign
                        if ((int) evt.value() / 10 == 1) {
                            gl.leftMouseButton.setListener(v);
                            left = v;
                        }

                        if ((int) evt.value() / 10 == 2) {
                            gl.rightMouseButton.setListener(v);
                            right = v;
                        }

                    }

                }//end for varchanger

                if (evt.controller().name().equals("peoplemove")) {
                    gl.peopleMobile = (gl.peopleMobile ? false : true);
                    System.out.println("peoplMobile " + gl.peopleMobile);
                } else if (evt.controller().name().equals("wageTime")) {
                    gl.peopleWageDistanceCost = (gl.peopleWageDistanceCost ? false : true);
                } else if (evt.controller().name().equals("firmsmove")) {
                    gl.firmsMobile = (gl.firmsMobile ? false : true);
                } else if (evt.controller().name().equals("stockzero")) {
                    gl.setStockToZero = (gl.setStockToZero ? false : true);

                } else if (evt.controller().name().equals("reset")) {

                    for (int i = 0; i < gl.firms.size(); i++) {
//            
                        Firm f = new Firm(gl.firms.size());
                        //dummy action for now
                        f.addAction(new FirmActDummy(f));

                        gl.firms.set(i, (Actor) f);
                        gl.goodSellers.set(i, (Actor) f);

                        SpaceTools.setDefaultActorPositions();

                    }

//                    for (Actor a : gl.people) {
//
//                        sp = (SingleActorPerson) a;
//
//                        sp.goodCost = (Randoms.nextDouble() * 2) + 1;
//                        sp.stock = 0;
//
//                    }
//
//                    SpaceTools.putActorsInACircle(gl.firms, 0.95);

                }

            }

        }//end method controlevent

        private void setDefaultMouseControls(String leftB, String rightB) {

            //check through mouseButtonVarChangers array first
            for (VarChanger v : mouseButtonVarChangers) {

                mb = (MouseButtons) v;

//                    System.out.println("Mouse event reassignment: " + evt.value() + ", " + evt.stringValue() + ", " + evt.controller().name()
//                            + "," + evt.controller().captionLabel());

//                    System.out.println("mb getName: " + mb.getName());

                if (mb.getName().equals(leftB)) {

                    gl.leftMouseButton.setListener(v);
                    left = v;
                } else if (mb.getName().equals(rightB)) {
                    gl.rightMouseButton.setListener(v);
                    right = v;
                }

            }//end for varchanger

        }//end setDefaultMouseControls

        public void draw() {

            //hack for gentle raising of rho
            if (incRho) {
                gl.rho += 0.001;
                gl.rho = (gl.rho > 0.99 ? 0.99 : gl.rho);
                util.rho = gl.rho;
                System.out.println("rho: " + gl.rho);
            }

            gl.MainVizTicks++;

            if (gl.threeDview) {
                draw3D();
            } else {
                draw2D();
            }

            gl.someoneHasBaton = false;

        }

        public void draw3D() {

            //this gets Processing to resize to correct proportion in parent frame
            if (gl.day < 50) {
                thisPSD.setSize(xsize + 1, ysize + 1);
            }

            hint(ENABLE_DEPTH_TEST);

//            background(0);

            background(255);
//            if (!FirmsCircleGameBucket.firm1 && !FirmsCircleGameBucket.firm2) {
//            } else if (((FirmsCircleGameBucket.firm1 || FirmsCircleGameBucket.firm2) && !(FirmsCircleGameBucket.firm1 && FirmsCircleGameBucket.firm2))) {
//
//                background(0,255,0);
//
//            } else if(FirmsCircleGameBucket.firm1 && FirmsCircleGameBucket.firm2) {
//
//                background(255,0,0);
//
//            }

//            fill(20, 20, 20);
//            stroke(20, 20, 20);
//            fill(200, 200, 200);
//            stroke(200, 200, 200);
            fill(255);
            stroke(255);

            //So, to attempt to explain all the transformations here
            //First, translate the top matrix out to half the width
            //Shift to a new matrix
            //Rotate appropriately
            //translate again so that the drawn coordinates centre on zero
            //pop back to previous matrix. 

            translate(width / 2, width / 2);
            pushMatrix();

            //rotateX(PI/2 - ((sin((testRot/2)))*PI/2));
            if (!gl.topView) {
//                rotateX(PI / 4);
            }

            rotateX(vRotateVal);

            rotateZ(hRotateVal);

            if (gl.rotateView) {
                hRotateVal += ((Math.PI * 2) / 300);
            }

            translate(-width / 2, -width / 2, -100);
//            translate(-width / 2, -width / 2);

            //rotateZ(-testRot);


            fill(240);

            rectMode(CORNER);
            rect(0, 0, width, height);

            //for drawing actors
            rectMode(CENTER);

            //p.p("In drawing");


            //Draw all the various drawable things...

            for (Drawable d : drawables) {

                d.draw3D(this);

            }


            popMatrix();


            //just testing 2D screendraw
//            hint(DISABLE_DEPTH_TEST);
//
//            noFill();
//            stroke(255);
//            rect(10, 10, 50, 50);


            //Need a different method now for this... 
            if (gl.outputJpegs) {
                //hack: add ref to firm delivery cost being used
//                Firm f = (Firm) gl.firms.get(0);
//                String fdc = Double.toString(f.deliverycost);
//                saveFrame("C://saveframes/torus-####_FirmDelCost-" + fdc + ".jpg");
                //                System.out.println("writing frame");
//                printScreen();
            }

            if (printScreen) {
                printScreen();
            }


            translate(-width / 2, -width / 2);
            hint(DISABLE_DEPTH_TEST);

            fill(0);

            //draw mouseButton vars, if displayVal ! = -999            
            if (left != null && left.displayVal != -999) {
                mb = (MouseButtons) left;
                text(mb.getName() + ": " + MathsBits.roundToDecimals(left.displayVal, 2), 5, height - 70);
            }


            if (right != null && right.displayVal != -999) {
                mb = (MouseButtons) right;
                text(mb.getName() + ": " + MathsBits.roundToDecimals(right.displayVal, 2), 5, height - 50);
            }

            //indicate multiplier
            text(" " + multiplier, width - 50, height - 80);

            //check framerate
            text(Math.round(frameRate), width - 50, height - 50);

//            stroke(255);
//            ellipse(mouseX, mouseY, 5, 5);
            controlP5.draw();

        }

        public void draw2D() {

            pg.beginDraw();

//            textFont(font);
            //0.038? Trial n error
            //works for von Thunen
//            pg.textFont(font2, Map.mapToVizX(0.038));
            //for other
            pg.textFont(font2, Map.mapToVizX(0.050));
//            pg.textFont(font2);
//            pg.smooth();
            pg.colorMode(RGB);

            pg.stroke(0);
            pg.fill(0);

            //this gets Processing to resize to correct proportion in parent frame
            if (gl.day < 50) {
                thisPSD.setSize(xsize + 1, ysize + 1);
            }

            pg.background(255);
//            pg.text("Does text work?", 80,200);
            pg.noFill();
            pg.stroke(255);

            //for drawing actors
            pg.rectMode(CENTER);

            for (Drawable d : drawables) {
                d.draw2D(pg);
            }

            pg.endDraw();


            if (gl.outputJpegs) {
                printScreen();
            }

            if (printScreen) {
                printScreen();
            }

            background(192, 186, 230);

            image(pg, 0, 0, pg.width, pg.height);

            //test if off edge of screen - give smaller version
            if (screen.height < ysize) {
                fill(0);
                rect(0, 0,
                        (screen.height / 2) + 4, (screen.height / 2) + 4);

                image(pg, 2, 2,
                        screen.height / 2, screen.height / 2);
//                rect(screen.width - ((screen.height / 4) * 1.1f) - 2, 48,
//                        (screen.height / 2) + 4, (screen.height / 2) + 4);
//
//                image(pg, screen.width - ((screen.height / 4) * 1.1f), 50,
//                        screen.height / 2, screen.height / 2);

            }


            //draw mouseButton vars, if displayVal ! = -999            
            if (left != null && left.displayVal != -999) {
                mb = (MouseButtons) left;
                text(mb.getName() + ": " + MathsBits.roundToDecimals(left.displayVal, 4), 5, height - 70);
            }


            if (right != null && right.displayVal != -999) {
                mb = (MouseButtons) right;
                text(mb.getName() + ": " + MathsBits.roundToDecimals(right.displayVal, 4), 5, height - 50);
            }

            noSmooth();

            //add model day
            text(" " + gl.day, width - 50, 20);

            //indicate multiplier
            text(" " + Math.pow(2, multiplier), width - 50, height - 80);

            //check framerate
            text(Math.round(frameRate), width - 50, height - 50);

//            stroke(255);
//            ellipse(mouseX, mouseY, 5, 5);
            controlP5.draw();

        }

        //called by toggle from ControlP5
//        public void toggle(boolean b) {
//
//            if (b == true) {
//                gl.
//            } else {
//            }
////            println("a toggle event.");
//        }
        public void mousePressed() {
//
////            gl.changeVal = (double) mouseY/50;
//            if (mouseButton == LEFT) {
//                gl.changeVal = Math.pow(10, (double) mouseY / (width / 2)) - 1;
//                gl.changeVal = (gl.changeVal < 0 ? 0 : gl.changeVal);
//                //change changeVal!
////                gl.changeVal /= 10;
//            } else {
//                gl.changeVal1 = (double) mouseY / height;
//                gl.changeVal1 *= 2;
//                System.out.println("gl.ChangeVal1: " + gl.changeVal1);
////                gl.changeVal1 = (gl.changeVal < 0 ? 0 : gl.changeVal1);
//            }
//            mouseDragged();
        }

        public void mouseDragged() {

//            double val = (double) (((pmouseY - mouseY) / (width * 2)) * multiplier);
            double val = (double) (pmouseY - mouseY) / (width * 2);

            val *= Math.pow(2, multiplier);
//            double val = Math.pow((double) (pmouseY - mouseY) / (width * 2), multiplier);

//            left.inVals[0] += (double) (pmouseY - mouseY);
            if (mouseButton == LEFT) {//                
                left.heard(new ShoutEvent(val));
            } else {
                right.heard(new ShoutEvent(val));
            }

//            System.out.print("pmousey - mousey: " + (pmouseY - mouseY));
//            System.out.println(", " + (double) (pmouseY - mouseY) / width);

        }

        public void mouseDraggedxxx() {

            //Don't react in area reserved for controls
            if (mouseX > 200 || mouseY > 260) {

                //give appropriate mouseListener a normalised value from the window
                if (mouseButton == LEFT) {
                    //gl.leftMouseButton.getListener();
//                    v = (VarChanger) left;
                    if (left != null) {

                        left.inVals[0] = (double) (width - mouseX) / width;
                        left.inVals[1] = (double) (height - (mouseY + 40)) / height;
//                        System.out.println("value changed to: " + left.val);
                    }
                    //right button
                } else {
                    if (right != null) {
                        right.inVals[0] = (double) (width - mouseX) / width;
                        right.inVals[1] = (double) (height - (mouseY + 40)) / height;
                    }
                }

//                if (mouseButton == LEFT) {
//                    //gl.changeVal = (double) mouseY / 50;
////                if (Math.abs(pmouseX-mouseX) < Math.abs(pmouseY-mouseY)) {
//                    switch (mouse1) {
//
//                        case Wage:
//                            //some hackery until I can design this properly
//                            gl.wageMouse = true;
//                            gl.changeVal = (double) (height - mouseY) / height;
//                            gl.changeVal *= 2;
//                            break;
//                        case Model:
//                            hRotateVal -= (float) (mouseX - pmouseX) / 200f;
//                            vRotateVal += (float) (mouseY - pmouseY) / 200f;
//                            break;
//                        case People:
//                            break;
//                        case Goods:
//                            gl.wageMouse = false;
//                            gl.changeVal = Math.pow(10, (double) (height - mouseY) / (width / 2)) - 1;
//                            gl.changeVal = (gl.changeVal < 0 ? 0 : gl.changeVal);
//                            break;
//                    }
//
//
//                } else {
//                    gl.changeVal1 = (double) (height - mouseY) / height;
//                    gl.changeVal1 *= 10;
////                    gl.changeVal1 += (double) ((mouseY-pmouseY)) / height;
////                    gl.changeVal1 /= 10;
////                    System.out.println("gl.ChangeVal1: " + gl.changeVal1);
////                gl.changeVal1 = (gl.changeVal < 0 ? 0 : gl.changeVal1);
//                }
            }


        }

        //for manually changing some global values - taking advantage of
        //processing's built-in key detection
        public void keyPressed() {

            if (key == CODED) {

                if (keyCode == UP) {

                    //gl.SPACECOST += 0.05;
                    gl.LOOKATME++;
                    System.out.println("LOOKATME: " + gl.LOOKATME);

                } else if (keyCode == DOWN) {

                    if (gl.LOOKATME > 0) {
                        gl.LOOKATME--;
                    }
                    System.out.println("LOOKATME: " + gl.LOOKATME);

                } else if (keyCode == RIGHT) {

//                    System.out.println("ping!");

                    multiplier++;

                } else if (keyCode == LEFT) {

                    multiplier--;
//                    multiplier = (multiplier == 0 ? 1 : multiplier);
//                    multiplier--;

                }

            }//if key == CODED

            //k: kill the model, write the data, shut it down. Shut it all down.
            if (key == 'k' || key == 'K') {
                gl.killSwitch = true;
            } else if (key == 'p' || key == 'P') {

                printScreen = true;

            } else if (key == ' ') {
                gl.topView = (gl.topView ? false : true);
            } else if (key == 'o' || key == 'O') {
                p.print = (p.print ? false : true);
            } //            } else if (key == 'r' || key == 'R') {
            //                gl.rotateView = (gl.rotateView ? false : true);
            //            } //            else if (key == 's' || key == 'S') {
            //                gl.stockTarget = (gl.stockTarget ? false : true);
            //                //a: uses to 'print-*A*ll' open drawing windows
            //            }
            else if (key == 'a' || key == 'A') {

                gl.printingAll = true;
                //I currently can't think of a better way of doing this
                PersistentAlphabeticNumberer.currentFileName =
                        PersistentAlphabeticNumberer.getNextAlphabetSequence();

                PrintPApplet pp;

                for (Shouter s : gl.time.shouters) {

                    try {
                        pp = (PrintPApplet) s.lstr;
                        pp.printScreen();
                    } catch (Throwable t) {
                        System.out.println("wasn't a PrintPApplet");
                    }

                }

                gl.printingAll = false;

            } else if (key == 'v' || key == 'V') {

                gl.viewPeopleVar = (gl.viewPeopleVar ? false : true);


            } else if (key == 'b' || key == 'B') {

                gl.viewPeopleVar2 = (gl.viewPeopleVar2 ? false : true);

            } else if (key == 'n' || key == 'N') {

                gl.viewPeopleVar3 = (gl.viewPeopleVar3 ? false : true);

                //Pause all but drawing 
            } else if (key == 'g' || key == 'G') {

                //Only works on those set to silenceable
                for (Shouter s : Timeline.shouters) {
                    System.out.println(s.toggleSilenced());
                }

                gl.pauseGraphs = (gl.pauseGraphs ? false : true);


            } else if (key == '[') {
                gl.LOOKATFIRM--;
            } else if (key == ']') {
                gl.LOOKATFIRM++;
            } else if (key == 'r' || key == 'R') {
                //remove random firm

                Firm f = (Firm) gl.firms.get(Randoms.rangeInt(gl.firms.size()));

                gl.firms.remove(f);
                gl.goodSellers.remove(f);

                SpaceTools.putActorsInAHorizontalLineStartEnd(gl.firms, 0.1, 0.9);

            } else if (key == 't' || key == 'T') {

                //add firm
                //Set average price
                Firm g;
                double goodCost = 0;

                for (Actor a : gl.firms) {

                    g = (Firm) a;
                    goodCost += g.goodCost;

                }

                goodCost /= gl.firms.size();

                Firm f = new Firm(gl.people.size() + gl.firms.size());

                f.goodCost = goodCost;

                //dummy action for now
                f.addAction(new FirmActDummy(f));

                gl.firms.add(f);
                gl.goodSellers.add((Actor) f);

                SpaceTools.putActorsInAHorizontalLineStartEnd(gl.firms, 0.1, 0.9);

            } else if (key == 'q' || key == 'Q') {

                //assuming CES...
                CESOneTypeUtility util = (CESOneTypeUtility) UtilityShell.u;
                gl.rho = 0.8;
                util.rho = gl.rho;

                //Hack to toggle gentle raising up of rho
            } else if (key == 'j' || key == 'J') {

                incRho = (incRho ? false : true);

            }

            //System.out.println("Spacecost: " + gl.SPACECOST);

        }

        public void printScreen() {

            printScreen = false;
            String fileName = FileName.getFileName();

//            pg.save(fileName + "_MainViz.tif");
            pg.save(fileName + "_MainViz.png");
//            pg.save(fileName + "_MainViz.jpg");
//            pg.save(FileName.getFileName() + "_MainViz.png");

            try {
                Thread.sleep(200);
            } catch (InterruptedException e) {
            }

        }
    }//end of inner class MyPapplet
}//end of class ProcessingTorus

