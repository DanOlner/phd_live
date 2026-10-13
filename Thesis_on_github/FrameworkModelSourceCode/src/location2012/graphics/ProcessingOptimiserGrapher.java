/*
 * For visualising a particular agents' optimisations; just clobbered together
 * avoiding the observer stuff.
 */
package location2012.graphics;

import processing.core.*;
import java.awt.BorderLayout;
import java.awt.Frame;
import java.awt.event.*;
import java.util.ArrayList;
import location2012.PersonAction;
import location2012.utils.gl;
import location2012.utils.p;

/**
 *
 * @author Olner Dan
 */
public class ProcessingOptimiserGrapher extends Frame {

    int xsize, ysize;
    MyPapplet processor;
    PersonAction pAct;
    //MovieMaker mm;
    //Camera camera1;
    //PeasyCam cam;
    //Array for drawables
    ArrayList<Drawable> drawables = new ArrayList<Drawable>();


    /*
     * Including an audible and a val for letting Processing know when to draw3D
     */
//    public ProcessingOptimiserGrapher(ArrayList<Drawable> drawables, int xsize, int ysize, Audible a, double val) {
//
//        //Add listener details to audible
//        giveShouterTo(a, val);
//
//        this.drawables = drawables;
//
//        this.xsize = xsize;
//        this.ysize = ysize;
//
//        setLayout(new BorderLayout());
//
//        //set up inner class where Processing will run
//        processor = new MyPapplet();
//
//        add(processor, BorderLayout.CENTER);
//        makeCloseable();
//        processor.init();
//
//        setLocation(750, 0);
//        setSize(xsize, ysize);
//
//        setVisible(true);
//
//    }
    public ProcessingOptimiserGrapher(PersonAction p, int xsize, int ysize) {

        pAct = p;
        this.xsize = xsize;
        this.ysize = ysize;

        setLayout(new BorderLayout());

        //set up inner class where Processing will run
        processor = new MyPapplet();

        add(processor, BorderLayout.CENTER);
        makeCloseable();
        processor.init();

        setLocation(750, 300);
        setSize(xsize, ysize);

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

            }
        });

    }

//    public void giveShouterTo(Audible a, double val) {
//
//        a.registerShouter(new Shouter(this, val));
//
//    }

    /*
     * Heard: so run through all registered drawables and draw3D them
     */
    public void heard() {

        processor.redraw();

    }

    public void setWeight(int weight) {
    }

    public int getWeight() {

        return 0;

    }

    //Inner class for running Processor
    public class MyPapplet extends PApplet {

        //PFont font;
        @Override
        public void setup() {

            //System.out.println("ping!");
            size(xsize, ysize);

//            camera1 = new Camera(this, 0, 0, 500);
//            camera1.arc(0.7f);
//            camera1.boom(60f);

//            mm = new MovieMaker(this, width, height, "drawing.mov",
//            30, MovieMaker.H263, MovieMaker.MEDIUM);

            colorMode(RGB);

            rectMode(CENTER);

//            font = loadFont("ArialMT12.vlw");
//            textFont(font, 24);

            noLoop();
            noFill();

            //font = createFont("Arial-BoldMT-48.vlw", 48);

        }

        @Override
        public void draw() {

            fill(0);
            stroke(255);

//            background(255);
            fill(255, 20);
            rect(0, 0, xsize*2, ysize*2);


            //stroke(0);


//            for (Drawable d : drawables) {
//
//                d.draw3D(this);
//
//            }
            pAct.draw3D(this);


        }//end draw3D

        public void keyPressed() {

//            if (key == CODED) {
//
//                if (keyCode == UP) {
//
//                    //gl.SPACECOST += 0.05;
//                    gl.LOOKATME++;
//
//                } else if (keyCode == DOWN) {
//
//                    if (gl.LOOKATME > 0) {
//                        gl.LOOKATME--;
//                    }
//
//                }
//
//            }

            //k: kill the model, write the data, shut it down. Shut it all down.
            if (key == 'k' || key == 'K') {
                gl.killSwitch = true;
            } else if (key == 'p' || key == 'P') {
                saveFrame("C://saveframes/KeyPrint_hillclimb-####.jpg");
            } else if (key == 'o' || key == 'O') {
                p.print = (p.print ? false : true);
            }

            //System.out.println("Spacecost: " + gl.SPACECOST);

        }
    }//end of inner class MyPapplet
}//end of class ProcessingTorus

