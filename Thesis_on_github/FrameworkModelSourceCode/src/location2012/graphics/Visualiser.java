/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package location2012.graphics;

import java.util.ArrayList;
import location2012.*;
import location2012.econs.Good;
import location2012.utils.gl;
import processing.core.PApplet;
import processing.core.PGraphics;

/**
 *
 * @author geodo
 */
public class Visualiser implements Drawable {

    /*
     * Drawing vars
     *
     */
    //Used for finding upper, lower bound of people's utility for colouring the range
    //These two found "this turn"
    double findHighUtil, findLowUtil;
    //These two used "this turn", set when the other two above are found
    //after running through them all.
    double useHighUtil, useLowUtil;
    //same for good amounts
    double findHighAmount, findLowAmount, useHighAmount, useLowAmount;
    PApplet pa;
    //Array of Drawables the Visualiser will draw
    ArrayList<Drawable> drawables = new ArrayList<Drawable>();

    /**
     * Visualiser's constructor is the place to compose the visualisation
     * elements Can use global reference to objects via gl's static variable
     * list
     */
    public Visualiser() {

        if (gl.singleActor) {
            drawables.add(new SingleActorDraw1D());
        } else if (gl.transmissionFirms) {
//            drawables.add(new CESTest1DPresentation());
//            drawables.add(new TransmissionDraw1DPresentationForManyAgents());            
//            drawables.add(new TransmissionDraw1DPresentation());            
//            drawables.add(new TransmissionDraw1D());            
            drawables.add(new PeopleDrawToSpace());
            drawables.add(new FirmsDrawToSpace());
            
        } else {
//            drawables.add(new CESTest1DPresentation());
//            drawables.add(new Hotelling1DPresentation());
//            drawables.add(new TransmissionDraw1DPresentation());            
//            drawables.add(new TransmissionDraw1D());            
            drawables.add(new PeopleDrawToSpace());
            drawables.add(new FirmsDrawToSpace());
//            drawables.add(new PeopleBuyLines());
        }

    }

    public void draw2D(PGraphics pa) {
//    public void draw2D(PApplet pa) {

//        draw3D(pa);

        for (Drawable d : drawables) {

//            pa.beginDraw();
            d.draw2D(pa);
//            pa.endDraw();

        }

    }

    public void draw3D(PApplet pa) {

        //reset vars
        findLowUtil = 9999;
        findHighUtil = -1;
        findLowAmount = 9999;
        findHighAmount = -1;

        Person p;

        //so other internal methods can use it easily
        this.pa = pa;

        pa.noFill();

        if (gl.space == gl.SpaceType.Point) {
            //if point-space, hive drawing off to a different method
//            drawPointModel();
            return;
        } else if (gl.space == gl.SpaceType.Line) {
            //if drawing the racetrack, move the line down to the centre
            pa.translate(0, (float) pa.width / 2, 0);
        }



        for (Actor h : gl.allActors) {

            Actor i = (Actor) h;
            pa.strokeWeight(2);



            if (i.getClass().getSimpleName().equals("Firm")) {

                Firm fm = (Firm) i;

//                if (fm.goodType == gl.GoodType.Luxury) {
//                    pa.stroke(255, 0, 0);
//
//
//                } else {
//                    pa.stroke(0, 0, 255);
//
//                }
                pa.stroke(i.colour.getRGB());


                //square in proportion to time input

                //pa.translate(0f, 0f, (float) fm.currentContributedTime * 3);

                pa.rect((float) (i.getx() * (pa.width / gl.width)),
                        (float) (i.gety() * (pa.height / gl.width)), (float) 5, (float) 5);

                //circle, radius 1
                if (gl.drawFirmOneDayCircles) {
                    pa.stroke(255, 20);
                    pa.ellipse((float) (i.getx() * (pa.width / gl.width)),
                            (float) (i.gety() * (pa.height / gl.width)),
                            1f * (pa.width / (float) gl.width),
                            1f * (pa.height / (float) gl.width));
                }


                //pa.translate(0f, 0f, -(float) fm.currentContributedTime * 3);

                //stick a line between them
                //pa.stroke(255, 0, 0);
//
//                pa.line((float) (i.getx() * (pa.width / gl.width)),
//                        (float) (i.gety() * (pa.height / gl.width)), 0,
//                        (float) (i.getx() * (pa.width / gl.width)),
//                        (float) (i.gety() * (pa.height / gl.width)), (float) fm.currentContributedTime * 3);





            } else if (i.getClass().getSimpleName().equals("Person")
                    || i.getClass().getSimpleName().equals("SingleActorPerson")) {
                pa.strokeWeight(1);

                PersonAction pac = (PersonAction) i.actions.get(0);




                //draw densitycost circle
                pa.stroke(100, 100);
                pa.ellipse((float) (i.getx() * (pa.width / gl.width)),
                        (float) (i.gety() * (pa.height / gl.width)),
                        (float) (gl.DENSITYCOSTRADIUS * 2 * (pa.width / gl.width)),
                        (float) (gl.DENSITYCOSTRADIUS * 2 * (pa.width / gl.width)));

                if (pac.bestBundle.employer == null) {
                    pa.stroke(255);
                } else {
                    pa.stroke(0, 255, 0, 100);
                }
                //ground square; don't draw3D if viewing from above
//                if (!gl.topView) {
                pa.rect((float) (i.getx() * (pa.width / gl.width)),
                        (float) (i.gety() * (pa.height / gl.width)), (float) 5, (float) 5);
//                }

                //If you don't use this, the initialBundles sometimes get thrown away between translates...

                double maxU = pac.bestBundle.maxUtility;

                if (gl.singleActor) {
                    SingleActorAction sl = (SingleActorAction) i.actions.get(0);
//                    maxU = sl.bestBundle.maxUtility;
                    maxU = sl.me.currentContributedTime;
                }

//                System.out.println("bestBundle utility in space: " + maxU);

                //record highest, lowest utility, use next time
                if (maxU > findHighUtil) {
                    findHighUtil = maxU;
                }
                if (maxU < findLowUtil) {
                    findLowUtil = maxU;
                }



                //translate highest, lowest utility values to 0-255 range
                if (gl.visUtilityRangeOn) {
//                    float colVal = (float) (((maxU - useLowUtil) / (useHighUtil - useLowUtil)) * 255f);
                    float colVal = (float) (((maxU - useLowUtil) / (useHighUtil - useLowUtil)) * 255f);
                    //System.out.println(colVal);
                    pa.strokeWeight(1);
                    pa.stroke(colVal, 255 - colVal, 0, 200);
                } else {
                    pa.stroke(0, 255, 0, 200);
                }

                //if von Thunen style model connecting to single Firm, use Firm's colour instead
                if (gl.colourByFirm && (gl.vonThunenGoods || gl.vonThunenWage)) {

                    p = (Person) i;
                    pa.stroke(p.vonThunenFirm.colour.getRGB(), 200);

                }

                //hacking colour test
//                if (gl.singleActor) {
//                    SingleActorAction sl = (SingleActorAction) i.actions.get(0);
//                    pa.stroke((int) (sl.me.deliverycost * 620), 0, 0, 150);
//                }


                //factor to multiply utility by - will need to change depending on vis
                float utilMult = gl.visFactorUtility;

                //if using topView, don't raise their height.

                if (!gl.topView) {
                    pa.translate(0f, 0f, (float) (maxU) * utilMult);
//                    pa.translate(0f, 0f, (float) (pac.bestBundle.maxUtility) * utilMult);
                }
                //System.out.println("pac.bestBundle.maxUtility" + pac.bestBundle.maxUtility + "pac.bestBundle.maxUtility  * 3f: " + pac.bestBundle.maxUtility * 3f);

                pa.rect((float) (i.getx() * (pa.width / gl.width)),
                        (float) (i.gety() * (pa.height / gl.width)), (float) (maxU) * (utilMult / 5), (float) (maxU) * (utilMult / 5));
//
                if (!gl.topView) {
                    pa.translate(0f, 0f, -(float) (maxU) * utilMult);
                }
                //System.out.println("Maxutility in draw3D: " + pac.bestBundle.maxUtility);

                //draw line
                if (!gl.topView) {
                    pa.stroke(200, 100);
                    //pa.strokeWeight(4);
//
                    pa.line((float) (i.getx() * (pa.width / gl.width)),
                            (float) (i.gety() * (pa.height / gl.width)), 0,
                            (float) (i.getx() * (pa.width / gl.width)),
                            (float) (i.gety() * (pa.height / gl.width)), (float) (maxU) * utilMult);

                }


                //should be able to use bestBundle to draw3D lines between me and firms I bought from
                //indicating quantities

                //System.out.println("size of bestB goodslist: " + pac.bestBundle.GoodsList.size());



                //drawing only one person's trade lines
                //if (gl.LOOKATME == pac.me.ID) {

//                if (pac.bestBundle.GoodsList.size() > 0) {
//                    System.out.println("size of goods list: " + pac.bestBundle.GoodsList.size());
//                }




                for (Good g : pac.bestBundle.GoodsList) {

                    Actor f = (Actor) g.gs;

                    //Make line more solid the higher the quantity... in a minute, let's just get lines working firs
                    //Note: this won't work with buyers across the space. Will have to write a specific method to deal
                    //with that.

//                        System.out.println("optimch: " + g.optimalChosenAmount);
                    //Start line at me, draw3D to firm
                    if (g.optimalChosenAmount > 0) {

                        //System.out.println("g.opt  = " + g.optimalChosenAmount);

                        //record highest, lowest goodAmount, use next time
//                        if (g.chosenAmount > findHighAmount) {
//                            findHighAmount = g.chosenAmount;
//                        }
//                        if (g.chosenAmount < findLowAmount) {
//                            findLowAmount = g.chosenAmount;
//                        }
//
//                        //double chmMult = 600;
//                        colVal = (float) ((useHighAmount - useLowAmount - g.chosenAmount) / (useHighAmount - useLowAmount) * 255f);
//
//                        System.out.println("useHighAmount: " + useHighAmount + ", useLowAm: " + useLowAmount + ", g.chosenAmount: " + g.chosenAmount);
//
//                        if (colVal < 0) {
//                            colVal = 0;
//                        }

                        //they're mostly high so try halving it. Really, I could do with a standard deviation, but not right now!
                        //colVal -= 125;

                        float chmMult = gl.visFactorBuyLines;

                        //System.out.println(colVal);

//                            if (g.f.goodType == gl.GoodType.Luxury) {
//                                pa.stroke(255, 50, 50, 100 + (float) (g.chosenAmount * 80));
//                            } else {
//                                pa.stroke(50, 50, 255, 100 + (float) (g.chosenAmount * 80));
//                            }

                        //pa.strokeWeight(colVal / 100);
                        pa.strokeWeight((float) (g.optimalChosenAmount * (chmMult) / 50));


//                        if (g.gs.getGoodType() == gl.GoodType.Luxury) {
//                            //pa.stroke(255, 50, 50, (float) (g.chosenAmount * chmMult));
//                            //pa.stroke((float) 255, 50, 50, (float) (g.chosenAmount * chmMult));
////                            pa.stroke((float) (g.optimalChosenAmount * chmMult), 50, 50, (float) (g.optimalChosenAmount * chmMult));
//                            //pa.stroke(colVal, 50, 50, colVal);
//                        } else {
//                            //pa.stroke(50, 50, 255, (float) (g.chosenAmount * chmMult));
//                            //pa.stroke(50, 50, 255, (float) (g.chosenAmount * chmMult));
////                            pa.stroke(50, 50, (float) (g.optimalChosenAmount * chmMult), (float) (g.optimalChosenAmount * chmMult));
//                            //pa.stroke(50, 50, colVal, colVal);
//
//                        }
                        float alpha = (float) (g.optimalChosenAmount * chmMult > 255 ? 255 : g.optimalChosenAmount * chmMult);

                        if (!gl.singleActor) {
                            Firm fi = (Firm) g.gs;
                            //note: this is, I think, where all the crazy random colours come in, so may want to keep when I come back
                            //to doing the arty stuff!
                            //float alpha = (float) (g.optimalChosenAmount * chmMult > 255 ? 255 : g.optimalChosenAmount * chmMult);
                            pa.stroke(fi.colour.getRGB(), alpha);
                        } else {
                            pa.stroke(255, alpha);
                        }


//                        curve if on line
                        if (gl.space == gl.SpaceType.Torus) {

                            pa.line((float) (i.getx() * (pa.width / gl.width)),
                                    (float) (i.gety() * (pa.height / gl.width)), 0,
                                    (float) (f.getx() * (pa.width / gl.width)),
                                    (float) (f.gety() * (pa.height / gl.width)), 0);

                            if (gl.reliefLines) {

                                float midx = ((float) Math.abs((f.getx() - i.getx())) / 2);
                                float midy = ((float) Math.abs((f.gety() - i.gety())) / 2);

                                pa.curve((float) ((i.getx() + midx) * (pa.width / gl.width)),
                                        (float) ((i.gety() + midy) * (pa.width / gl.width)),
                                        -(float) (g.optimalChosenAmount * chmMult) * 3,
                                        (float) (i.getx() * (pa.width / gl.width)),
                                        (float) (i.gety() * (pa.height / gl.width)), 0,
                                        (float) (f.getx() * (pa.width / gl.width)),
                                        (float) (f.gety() * (pa.height / gl.width)), 0,
                                        (float) ((i.getx() + midx) * (pa.width / gl.width)),
                                        (float) ((i.gety() + midy) * (pa.width / gl.width)),
                                        -(float) (g.optimalChosenAmount * chmMult) * 3);

                            }


                        } else if (gl.space == gl.SpaceType.Line) {

                            float midPoint = 0;
                            midPoint = ((float) Math.abs((f.getx() - i.getx())) / 2);

                            if (f.getx() > i.getx()) {



                                //put on z axis if side view
                                if (gl.topView) {
                                    pa.curve((float) ((i.getx() + midPoint) * (pa.width / gl.width)),
                                            (float) (g.optimalChosenAmount * chmMult) * 3,
                                            (float) (i.getx() * (pa.width / gl.width)),
                                            (float) (i.gety() * (pa.height / gl.width)),
                                            (float) (f.getx() * (pa.width / gl.width)),
                                            (float) (f.gety() * (pa.height / gl.width)),
                                            (float) ((i.getx() + midPoint) * (pa.width / gl.width)),
                                            (float) (g.optimalChosenAmount * chmMult * 3));
                                } else {
                                    pa.curve((float) ((i.getx() + midPoint) * (pa.width / gl.width)),
                                            -(float) (g.optimalChosenAmount * chmMult) * 3, 0,
                                            (float) (i.getx() * (pa.width / gl.width)),
                                            (float) (i.gety() * (pa.height / gl.width)), 0,
                                            (float) (f.getx() * (pa.width / gl.width)),
                                            (float) (f.gety() * (pa.height / gl.width)), 0,
                                            (float) ((i.getx() + midPoint) * (pa.width / gl.width)),
                                            -(float) (g.optimalChosenAmount * chmMult * 3), 0);
//                                pa.curve((float) (i.getx() + midPoint) * (pa.width / gl.width), 0, -(float) (g.optimalChosenAmount * chmMult) * 3,
//                                        (float) (i.getx() * (pa.width / gl.width)), (float) (i.gety() * (pa.height / gl.width)), 0,
//                                        (float) (f.getx() * (pa.width / gl.width)), (float) (f.gety() * (pa.height / gl.width)), 0,
//                                        (float) (i.getx() + midPoint) * (pa.width / gl.width), 0, -(float) (g.optimalChosenAmount * chmMult * 3));

                                }
//
                            } else {

                                if (gl.topView) {
                                    pa.curve((float) ((f.getx() + midPoint) * (pa.width / gl.width)),
                                            (float) (g.optimalChosenAmount * chmMult) * 3,
                                            (float) (i.getx() * (pa.width / gl.width)),
                                            (float) (i.gety() * (pa.height / gl.width)),
                                            (float) (f.getx() * (pa.width / gl.width)),
                                            (float) (f.gety() * (pa.height / gl.width)),
                                            (float) ((f.getx() + midPoint) * (pa.width / gl.width)),
                                            (float) (g.optimalChosenAmount * chmMult * 3));
                                } else {

//                               
                                    pa.curve((float) ((f.getx() + midPoint) * (pa.width / gl.width)), 0,
                                            -(float) (g.optimalChosenAmount * chmMult) * 3,
                                            (float) (i.getx() * (pa.width / gl.width)),
                                            (float) (i.gety() * (pa.height / gl.width)), 0,
                                            (float) (f.getx() * (pa.width / gl.width)),
                                            (float) (f.gety() * (pa.height / gl.width)), 0,
                                            (float) ((f.getx() + midPoint) * (pa.width / gl.width)), 0,
                                            -(float) (g.optimalChosenAmount * chmMult * 3));
//                                pa.curve((float) (f.getx() + midPoint) * (pa.width / gl.width), 0, -(float) (g.optimalChosenAmount * chmMult) * 3,
//                                        (float) (i.getx() * (pa.width / gl.width)),(float) (i.gety() * (pa.height / gl.width)), 0,
//                                        (float) (f.getx() * (pa.width / gl.width)),(float) (f.gety() * (pa.height / gl.width)), 0,
//                                        (float) (f.getx() + midPoint) * (pa.width / gl.width), 0, -(float) (g.optimalChosenAmount * chmMult * 3));
                                }

                            }
                            //System.out.println("Firm: " + g.f.getx() + ", person: " + i.getx() + ", Midpoint:" + midPoint);



                        }

                    }


                }//end for good...

                //}//end if LOOKATME

                pa.strokeWeight(1);



            }//end actor-type if


            pa.stroke(0);

            /*
             * LOOKATME CIRCLE
             */
            //1 day's radius
            //or, if I'm testing changing space costs... oh yes, still 1 day's radius!
            //But might change
//            pa.ellipse(((float) isInSpace.get(gl.LOOKATME).getx()) * (pa.width / gl.width),
//                    ((float) isInSpace.get(gl.LOOKATME).gety()) * (pa.height / gl.width),
//                    (2 * (float) gl.SPACECOST) * (pa.width / gl.width),
//                    (2 * (float) gl.SPACECOST) * (pa.height / gl.width));
//
//            //and mark the actor it's supposed to relate to...
//            pa.strokeWeight(5);
//
//            pa.translate(0f, 0f, 10f);
//            pa.rect((float) (isInSpace.get(gl.LOOKATME).getx() * (pa.width / gl.width)),
//                    (float) (isInSpace.get(gl.LOOKATME).gety() * (pa.height / gl.width)), (float) 5, (float) 5);
//            pa.translate(0f, 0f, -10f);

            //p.a(" -> " + ((float) isInTorus.get(lookatme).h.getx() - 1) + ", " + ((float) isInTorus.get(lookatme).h.gety() - 1));



        }//for HasLocation isInSpace

//        drawMeanCentrePoint();

        //draw an agent's detected actors to check location...
        pa.strokeWeight(3);

        //note: if this picks up a firm, or some actor that doesn't have a populated LocMemory
        //you'll get a nullpointerexception

////        so let's make sure I'm getting a person
//        Actor dr = (Actor) isInSpace.get(gl.LOOKATME);
////
//        for (ActorNLocation anl : dr.lm.actors) {
//
////            if (anl.actor.getClass().getSimpleName().equals("Person")) {
////
////                pa.stroke(100, 0, 110);
////
////
////
////            } else {
//            pa.stroke(255, 30, 255);
//
//
//            //} //Draw the found objects, relative to the location of me
//            pa.rect((float) ((dr.getx() + anl.p.x) * (pa.width / gl.width)),
//                    (float) ((dr.gety() + anl.p.y) * (pa.height / gl.width)), (float) 10, (float) 10);
//
//            //System.out.println("Loc of drawn actor: " + dr.getx() + "," + dr.gety());
//
//        }//end for ActorNLocation

        //set col vals for next time
        if (findLowUtil > 0) {
            useHighUtil = findHighUtil;
            useLowUtil = findLowUtil;
        }
//        System.out.println("low high utils found: " + useLowUtil + "," + useHighUtil);
        useHighAmount = findHighAmount;
        useLowAmount = findLowAmount;

    }
}
