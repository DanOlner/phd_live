/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package location2012.graphics;

import java.awt.Color;
import java.util.ConcurrentModificationException;
import location2012.Actor;
import location2012.Firm;
import location2012.Person;
import location2012.PersonAction;
import location2012.econs.Bundle;
import location2012.econs.Good;
import location2012.geog.LocMemory.ActorNLocation;
import location2012.utils.Map;
import location2012.utils.MathsBits;
import location2012.utils.gl;
import location2012.variables.MinMaxValues;
import location2012.variables.VariableStore;
import location2012.variables.VariableStoreMeanSD;
import processing.core.PApplet;
import processing.core.PGraphics;

/**
 * An element to be drawn by Visualiser. Responsible for actually drawing to the
 * PApplet
 *
 * @author geodo
 */
public class PeopleDrawToSpace implements Drawable {

    PGraphics p;
    MinMaxValues minMax1, minMax2, minMaxBundle;
    Person pe, pe2;
    PersonAction pa;//for getting access to Bundles
    Color col1;
    VariableStore vs1, vs2;
    VariableStoreMeanSD meanSD, meanSD2;
    //highest agent y, for positioning text
    double low, high, highestAgentY;
    //String to remember name of variable being displayed
    String var;
    //pentagon coords
    //http://mathworld.wolfram.com/Pentagon.html
    float s1 = 0.9510565162951535f;
    float s2 = 0.5877852522924731f;
    float c1 = 0.30901699437494745f;
    float c2 = 0.8090169943749475f;
    float size;
    //
//    vonThunenVarPair vonThunenVarPair;

    public PeopleDrawToSpace() {

        //Set up calculation of min-max vals for utility
        minMax1 = new MinMaxValues(gl.time, 1, 2, gl.people, "maxUtility");
        minMax2 = new MinMaxValues(gl.time, 1, 2, gl.people, "chosenDensityCost");
        //
        pa = (PersonAction) gl.people.get(gl.LOOKATME).actions.get(0);
        minMaxBundle = new MinMaxValues(gl.time, 1, 2,
                pa.bundlesToOptimise, "maxUtility");


        //get local reference to data
        vs1 = gl.mainDataStore.getVariableStoreByVariableName("maxUtility");
        vs2 = gl.mainDataStore.getVariableStoreByVariableName("chosenDensityCost");
        meanSD = (VariableStoreMeanSD) gl.mainDataStore.getVariableStoreByVariableLabel("utility mean");
        meanSD2 = (VariableStoreMeanSD) gl.mainDataStore.getVariableStoreByVariableLabel("density cost mean");


        //constructor sets name and base cost vars
        new vonThunenVarPair();

    }

    public void draw2D(PGraphics p) {

        if (this.p == null) {
            this.p = p;
        }


        densityCostCircles(p);

        //hackeroo
        if (gl.vonThunenMode && !gl.viewPeopleVar3) {
            peopleSquaresVonThunen();
        } else {
            if (gl.viewPeopleVar2) {
                peopleSquares(p);
            }
        }

//        commuteCostEdge(p);

        if (gl.viewPeopleVar2) {
        }


        //draw one Person's bundles, marking the best one.
//        drawBundles();
//        drawLookAtMe(p);


    }

    private void densityCostCircles(PGraphics p) {

        p.smooth();
        p.strokeWeight(Map.mapToVizX(0.004));
        p.stroke(0, 100);


        for (Actor a : gl.people) {

            pe = (Person) a;

//            c1 = Map.mapLowHighToColour_DoublesColorsInColorOut(
//                    pe.maxUtility, utilityMinMax.lowVal,
//                    utilityMinMax.highVal, Color.green, Color.red);

            //b&w version
            col1 = Map.mapLowHighToColour_DoublesColorsInColorOut(
                    pe.maxUtility, minMax1.lowVal,
                    minMax1.highVal, Color.white, Color.black);

//            p.fill(c1.getRGB(), 40);
//            p.fill(0, 8);

//            p.stroke(c1.getRGB(), 100);
            p.stroke(0, 80);

//            p.ellipse(Map.mapToPGraphicsX(a.getx(),p.width), Map.mapToPGraphicsY(a.gety(),p.height),
//                    Map.mapToPGraphicsX(gl.DENSITYCOSTRADIUS * 2,p.width), 
//                    Map.mapToPGraphicsY(gl.DENSITYCOSTRADIUS * 2,p.height));
            p.ellipse(Map.mapToVizX(a.getx()), Map.mapToVizY(a.gety()),
                    Map.mapToVizX(gl.DENSITYCOSTRADIUS * 2), Map.mapToVizY(gl.DENSITYCOSTRADIUS * 2));

        }

    }//end method densityCostCircles

    private void peopleSquares(PGraphics p) {

        highestAgentY = -1;

        p.noSmooth();
        p.rectMode(p.CENTER);

        float rectSize;

        for (Actor a : gl.people) {

            highestAgentY = (a.gety() > highestAgentY ? a.gety() : highestAgentY);

            pe = (Person) a;

            //relative value
//            if (false) {
            if (gl.viewPeopleVar) {
                rectSize = Map.mapLowToHigh_DoublesInFloatOut(
                        pe.maxUtility, minMax1.lowVal,
                        minMax1.highVal, 0.005, 0.05);
                //for use below
                low = minMax1.lowVal;
                high = minMax1.highVal;
                var = "utility";

            } else {
                rectSize = Map.mapLowToHigh_DoublesInFloatOut(
                        pe.chosenDensityCost, minMax2.lowVal,
                        minMax2.highVal, 0.005, 0.05);
                //for use below
                low = minMax2.lowVal;
                high = minMax2.highVal;
                var = "density cost";
            }
            //absolute value
//            float rectSize = Map.mapLowToHigh_DoublesInFloatOut(
//                    pe.maxUtility, 0, 2.0, 0.005, 0.05);

            //What the hell am I using vs for here? Very confused!
//            c1 = Map.mapLowHighToColour_DoublesColorsInColorOut(
//                    pe.maxUtility, utilityMinMax.lowVal,
//                    utilityMinMax.highVal, Color.green, Color.red);
////                    vs.variableData.get(vs.variableData.size())[a.ID], Color.green, Color.red);

            //b&w version
            if (gl.viewPeopleVar) {
                col1 = Map.mapLowHighToColour_DoublesColorsInColorOut(
                        pe.maxUtility, minMax1.lowVal,
                        minMax1.highVal, Color.black, Color.white);
            } else {
                col1 = Map.mapLowHighToColour_DoublesColorsInColorOut(
                        pe.chosenDensityCost, minMax2.lowVal,
                        minMax2.highVal, Color.black, Color.white);
            }
//            c1 = Map.mapLowHighToColour_DoublesColorsInColorOut(
//                    pe.maxUtility, utilityMinMax.lowVal,
//                    utilityMinMax.highVal, Color.white, Color.black);
//                    vs.variableData.get(vs.variableData.size())[a.ID], Color.green, Color.red);

            p.fill(0, 0);
            p.fill(col1.getRGB());

//            p.strokeJoin(p.MITER);
            p.stroke(0);
            p.strokeWeight(Map.mapToVizX(0.004));
//            p.rect(Map.mapToVizX(a.getx()), Map.mapToVizY(a.gety()), 
//                    rectSizeTest, rectSizeTest);

            //check for zero utility
            if (pe.maxUtility == 0) {
//                System.out.println("zero!");

                p.fill(255);
                p.strokeWeight(Map.mapToVizX(0.005));
                p.smooth();
                p.ellipse(Map.mapToVizX(a.getx()), Map.mapToVizY(a.gety()),
                        Map.mapToVizX(0.03), Map.mapToVizY(0.03));

            } else {
                p.noSmooth();
                p.rect(Map.mapToVizX(a.getx()), Map.mapToVizY(a.gety()),
                        Map.mapToVizX(rectSize), Map.mapToVizY(rectSize));
            }



        }//end for each


        p.fill(0);
        double start = 0.28;
        double gap = 0.27;

        p.textSize(Map.mapToVizX(0.04));

        //also need to add key for the values used above. Hacking a bit now...
//        p.text("rho: " + MathsBits.roundToDecimals(gl.rho, 2), Map.mapToVizX(start),
//                Map.mapToVizY(highestAgentY + 0.11));
        if (gl.viewPeopleVar) {
            p.text(var + " mean: " + MathsBits.roundToDecimals(meanSD.mean, 4), Map.mapToVizX(start + gap),
                    Map.mapToVizY(highestAgentY + 0.11));
            p.text("standard dev: " + MathsBits.roundToDecimals(meanSD.sd, 4), Map.mapToVizX(start + (gap * 3)),
                    Map.mapToVizY(highestAgentY + 0.11));
        } else {
            p.text(var + " mean: " + MathsBits.roundToDecimals(meanSD2.mean, 4), Map.mapToVizX(start + gap),
                    Map.mapToVizY(highestAgentY + 0.11));
            p.text("standard dev: " + MathsBits.roundToDecimals(meanSD2.sd, 4), Map.mapToVizX(start + (gap * 3)),
                    Map.mapToVizY(highestAgentY + 0.11));
        }


        //        p.text(var + " low: " + MathsBits.roundToDecimals(low, 4), Map.mapToVizX(start + 0.27),
//                Map.mapToVizY(highestAgentY + 0.11));
//        p.text("high: " + MathsBits.roundToDecimals(high, 4), Map.mapToVizX(start + 0.69),
//                Map.mapToVizY(highestAgentY + 0.11));


    }//end method peopleSquares

    /**
     * Draw People with specific shapes to mark their type. Just for four shapes
     * for now.
     *
     * @param p
     */
    private void peopleSquaresVonThunen() {

        highestAgentY = -1;

        p.noSmooth();
        p.rectMode(p.CENTER);

        float shapeSize;

        for (Actor a : gl.people) {

            highestAgentY = (a.gety() > highestAgentY ? a.gety() : highestAgentY);

            pe = (Person) a;

            //relative value
            if (gl.viewPeopleVar) {
                shapeSize = Map.mapLowToHigh_DoublesInFloatOut(
                        pe.maxUtility, minMax1.lowVal,
                        minMax1.highVal, 0.005, 0.05);
                //for use below
                low = minMax1.lowVal;
                high = minMax1.highVal;
                var = "utility";

            } else {
                shapeSize = Map.mapLowToHigh_DoublesInFloatOut(
                        pe.chosenDensityCost, minMax2.lowVal,
                        minMax2.highVal, 0.005, 0.05);
                //for use below
                low = minMax2.lowVal;
                high = minMax2.highVal;
                var = "density cost";
            }

            shapeSize = 0.05f;

            //absolute value
//            float rectSize = Map.mapLowToHigh_DoublesInFloatOut(
//                    pe.maxUtility, 0, 2.0, 0.005, 0.05);

            //What the hell am I using vs for here? Very confused!
//            c1 = Map.mapLowHighToColour_DoublesColorsInColorOut(
//                    pe.maxUtility, utilityMinMax.lowVal,
//                    utilityMinMax.highVal, Color.green, Color.red);
////                    vs.variableData.get(vs.variableData.size())[a.ID], Color.green, Color.red);

            //b&w version
            if (gl.viewPeopleVar) {
                col1 = Map.mapLowHighToColour_DoublesColorsInColorOut(
                        pe.maxUtility, minMax1.lowVal,
                        minMax1.highVal, Color.black, Color.white);
            } else {
                col1 = Map.mapLowHighToColour_DoublesColorsInColorOut(
                        pe.chosenDensityCost, minMax2.lowVal,
                        minMax2.highVal, Color.black, Color.white);
            }
//            c1 = Map.mapLowHighToColour_DoublesColorsInColorOut(
//                    pe.maxUtility, utilityMinMax.lowVal,
//                    utilityMinMax.highVal, Color.white, Color.black);
//                    vs.variableData.get(vs.variableData.size())[a.ID], Color.green, Color.red);

            col1 = pe.vonThunenFirm.colour;

            p.fill(0, 0);
            p.fill(col1.getRGB());

//            p.strokeJoin(p.MITER);
            p.stroke(0);
            p.strokeWeight(Map.mapToVizX(0.004));
//            p.rect(Map.mapToVizX(a.getx()), Map.mapToVizY(a.gety()), 
//                    rectSizeTest, rectSizeTest);

            //check for zero utility
            if (pe.maxUtility == 0) {
//                System.out.println("zero!");

                p.fill(255);
                p.strokeWeight(Map.mapToVizX(0.005));
                p.smooth();
                p.ellipse(Map.mapToVizX(a.getx()), Map.mapToVizY(a.gety()),
                        Map.mapToVizX(0.03), Map.mapToVizY(0.03));

            } else {
                p.noSmooth();

                //shape based on von Thunen Mode, four options. Test with colour?
                Person pe = (Person) a;

                p.stroke(pe.vonThunenFirm.colour.getRGB());
                p.stroke(0);

                //hacking for only four firms, a shape each
                switch (pe.vonThunenFirmID - gl.people.size()) {

                    case 0:

                        p.noSmooth();

                        p.rect(Map.mapToVizX(a.getx()), Map.mapToVizY(a.gety()),
                                Map.mapToVizX(shapeSize), Map.mapToVizY(shapeSize));

                        break;

                    case 1:

                        p.smooth();
                        p.ellipse(Map.mapToVizX(a.getx()), Map.mapToVizY(a.gety()),
                                Map.mapToVizX(shapeSize * 1.2), Map.mapToVizY(shapeSize * 1.2));

                        break;


                    case 2:

                        triangle((float) a.getx(), (float) a.gety(), shapeSize);

                        break;

                    case 3:

                        pentagon((float) a.getx(), (float) a.gety(), shapeSize);

                }//end switch



            }//end else



        }//end for each

        shapeSize = 0.05f;
        //also need to add key for the values used above. Hacking a bit now...
//        p.text(var + " low: " + MathsBits.roundToDecimals(low, 4), Map.mapToVizX(0.55), Map.mapToVizY(highestAgentY + 0.11));
//        p.text("high: " + MathsBits.roundToDecimals(high, 4), Map.mapToVizX(1.15), Map.mapToVizY(highestAgentY + 0.11));

        //von Thunen key needs to be the shapes above
        //In order: 0,l,2,3. Which will be base cost, then *2,*3,*4
        float start = 0.74f;
        float spacing = 0.18f;

        //which von Thunen var
        //See, this is the kind of thing OOP was invented to avoid!
        p.fill(0);
        p.textAlign(p.RIGHT);
        p.text(vonThunenVarPair.name + ":", Map.mapToVizX(start - 0.03), Map.mapToVizY(highestAgentY + 0.125));

        p.textAlign(p.LEFT);
        p.noSmooth();
        p.fill(gl.firms.get(0).colour.getRGB());
        p.rect(Map.mapToVizX(start), Map.mapToVizY(highestAgentY + 0.11),
                Map.mapToVizX(shapeSize), Map.mapToVizY(shapeSize));
        p.fill(0);
        p.text("=" + Double.toString(vonThunenVarPair.cost[0]),
                Map.mapToVizX(start + (shapeSize * 0.7)), Map.mapToVizY(highestAgentY + 0.125));

        p.smooth();
        p.fill(gl.firms.get(1).colour.getRGB());
        p.ellipse(Map.mapToVizX(start + spacing), Map.mapToVizY(highestAgentY + 0.11),
                Map.mapToVizX(shapeSize), Map.mapToVizY(shapeSize));
        p.fill(0);
        p.text("=" + Double.toString(vonThunenVarPair.cost[1]),
                Map.mapToVizX(start + spacing + (shapeSize * 0.7)), Map.mapToVizY(highestAgentY + 0.125));

        p.fill(gl.firms.get(2).colour.getRGB());
        triangle(start + (spacing * 2), (float) highestAgentY + 0.11f, shapeSize);
        p.fill(0);
        p.text("=" + Double.toString(vonThunenVarPair.cost[2]),
                Map.mapToVizX(start + (spacing * 2) + (shapeSize * 0.7)), Map.mapToVizY(highestAgentY + 0.125));

        p.fill(gl.firms.get(3).colour.getRGB());
        pentagon(start + (spacing * 3), (float) highestAgentY + 0.11f, shapeSize);
        p.fill(0);
        p.text("=" + Double.toString(vonThunenVarPair.cost[3]),
                Map.mapToVizX(start + (spacing * 3) + (shapeSize * 0.7)), Map.mapToVizY(highestAgentY + 0.125));




    }//end method peopleSquaresVonThunen

    private void triangle(float x, float y, float shapeSize) {

        p.smooth();
        size = Map.mapToVizX(shapeSize * 0.8);

        //equilateral triangle. Coordinates for two bottom angles: x = sqrt(3)/2. y = 1/2. Decimal versions used.
        p.translate(Map.mapToVizX(x), Map.mapToVizY(y));
        //see pentagon def in var defs above
        p.beginShape();
        p.vertex(0, -1f * size);
        p.vertex(0.8660254037844386f * size, 0.5f * size);
        p.vertex(-0.8660254037844386f * size, 0.5f * size);
        p.endShape(p.CLOSE);

        p.translate(-Map.mapToVizX(x), -Map.mapToVizY(y));

    }

    private void pentagon(float x, float y, float shapeSize) {

        p.smooth();
        size = Map.mapToVizX(shapeSize * 0.7);

        p.translate(Map.mapToVizX(x), Map.mapToVizY(y));
        //see pentagon def in var defs above
        p.beginShape();
        p.vertex(0, -1f * size);
        p.vertex(-s1 * size, -c1 * size);
        p.vertex(-s2 * size, c2 * size);
        p.vertex(s2 * size, c2 * size);
        p.vertex(s1 * size, -c1 * size);
        p.endShape(p.CLOSE);

        p.translate(-Map.mapToVizX(x), -Map.mapToVizY(y));

    }//end method pentagon

    private void commuteCostEdge(PGraphics p) {

        p.noFill();
        p.stroke(100);
        p.smooth();

        p.ellipse(Map.mapToVizX(1), Map.mapToVizY(1), 2 * Map.mapToVizX(1 / gl.commuteCost), 2 * Map.mapToVizY(1 / gl.commuteCost));

    }

    /**
     * For drawing bits for Person who has the 'lookatme' focus
     *
     * @param p
     */
    private void drawLookAtMe(PGraphics p) {

        p.smooth();

        for (Actor a : gl.people) {

            pe = (Person) a;

//            if (true) {
            if (a.ID % 30 == gl.LOOKATME) {
//            if (a.ID == gl.LOOKATME) {

                //mark my density cost radius
                p.noFill();
                p.stroke(0);
                p.strokeWeight(Map.mapToVizX(0.008));

                p.stroke(255);
                p.strokeWeight(Map.mapToVizX(0.019));
                p.ellipse(Map.mapToVizX(a.getx()), Map.mapToVizY(a.gety()),
                        Map.mapToVizX(gl.DENSITYCOSTRADIUS * 2), Map.mapToVizY(gl.DENSITYCOSTRADIUS * 2));
//                p.stroke(0);
//                p.strokeWeight(Map.mapToVizX(0.005));
//                p.ellipse(Map.mapToVizX(a.getx()), Map.mapToVizY(a.gety()),
//                        Map.mapToVizX(gl.DENSITYCOSTRADIUS * 2), Map.mapToVizY(gl.DENSITYCOSTRADIUS * 2));



            }

        }//for actor a 
        for (Actor a : gl.people) {

            pe = (Person) a;

//            if (true) {
            if (a.ID % 30 == gl.LOOKATME) {
//            if (a.ID == gl.LOOKATME) {

                //mark my density cost radius
                p.noFill();
                p.stroke(0);
                p.strokeWeight(Map.mapToVizX(0.008));

//                p.stroke(255);
//                p.strokeWeight(Map.mapToVizX(0.016));
//                p.ellipse(Map.mapToVizX(a.getx()), Map.mapToVizY(a.gety()),
//                        Map.mapToVizX(gl.DENSITYCOSTRADIUS * 2), Map.mapToVizY(gl.DENSITYCOSTRADIUS * 2));
                p.stroke(0);
                p.strokeWeight(Map.mapToVizX(0.01));
                p.ellipse(Map.mapToVizX(a.getx()), Map.mapToVizY(a.gety()),
                        Map.mapToVizX(gl.DENSITYCOSTRADIUS * 2), Map.mapToVizY(gl.DENSITYCOSTRADIUS * 2));



            }

        }//for actor a 

        //do this twice so people markers are always above black circles
        for (Actor a : gl.people) {

            pe = (Person) a;

//            if (true) {
            if (a.ID % 30 == gl.LOOKATME) {
//            if (a.ID == 99999) {
//            if (a.ID == gl.LOOKATME) {

                p.fill(255);
                p.stroke(0);
                p.strokeWeight(Map.mapToVizX(0.005));

                //draw bestBundle actor positions
                for (ActorNLocation b : pe.bestBundle.densityCostActors.actors) {

                    pe2 = (Person) b.actor;

                    p.ellipse(Map.mapToVizX(pe2.getx()), Map.mapToVizY(pe2.gety()), Map.mapToVizX(0.025), Map.mapToVizX(0.025));

                }

            }

        }//for actor a         

    }//

    /**
     * Draw a single Person's Bundles, highlighting the best one.
     */
    public void draw3D(PApplet p) {
    }

    //inner class for vonThunen variable name and base cost pair
    static class vonThunenVarPair {

        static String name;
        //stored in order of Firms
        static double[] cost;
        static Firm f;

        public vonThunenVarPair() {

            cost = new double[gl.firms.size()];

            if (gl.vonThunenCommute || gl.commuteRange) {
//            return "commute";
                name = "commute";

                for (int i = 0; i < gl.firms.size(); i++) {
                    f = (Firm) gl.firms.get(i);
                    cost[i] = f.commuteCost;
                }

            } else if (gl.vonThunenWage || gl.wageRange) {
                name = "wage";
                for (int i = 0; i < gl.firms.size(); i++) {
                    f = (Firm) gl.firms.get(i);
                    cost[i] = f.wage;
                }

//            return "wage";
            } else if (gl.vonThunenGoods || gl.goodsRange) {
                name = "good";
                for (int i = 0; i < gl.firms.size(); i++) {
                    f = (Firm) gl.firms.get(i);
                    cost[i] = f.goodCost;
                }

//            return "good";
            } else if (gl.vonThunenDelivery || gl.deliveryRange) {
                name = "delivery";
                for (int i = 0; i < gl.firms.size(); i++) {
                    f = (Firm) gl.firms.get(i);
                    cost[i] = f.deliverycost;
                }

//            return "delivery";
            }
        }
////        public vonThunenVarPair(String name, double baseCost) {
////            this.name = name;
////            this.baseCost = baseCost;
////        }
//        private String[] getVonThunenVarString() {
//
//            String[] str = new String[2];
//
//            if (gl.vonThunenCommute || gl.commuteRange) {
////            return "commute";
//                str[0] = "commute";
//                str[1] = "commute";
//            } else if (gl.vonThunenWage || gl.wageRange) {
//                str[0] = "wage";
//                str[1] = "wage";
////            return "wage";
//            } else if (gl.vonThunenGoods || gl.goodsRange) {
//                str[0] = "good";
//                str[1] = "good";
////            return "good";
//            } else if (gl.vonThunenDelivery || gl.deliveryRange) {
//                str[0] = "delivery";
//                str[1] = "good";
////            return "delivery";
//            }
//
//            return str;
//        }
    }

    private void drawBundles() {

        pe = (Person) gl.people.get(gl.LOOKATME);
        pa = (PersonAction) gl.people.get(gl.LOOKATME).actions.get(0);
        double maxBundle = -1;
        Bundle bestBundle = new Bundle();

        Firm f;
        p.smooth();

        //draw lines first so Bundle boxes appear over top.
        //For optimal Bundle,
        //line between Bundle centre and goods / firm
        for (Bundle b : pa.initialBundles) {

            if (b.maxUtility > maxBundle) {
                maxBundle = b.maxUtility;
                bestBundle = b;
            }

        }

        for (Good g : bestBundle.GoodsList) {

            f = (Firm) g.gs;

            p.strokeWeight(Map.mapToVizX(g.optimalChosenAmount * 0.45));
            p.stroke(255);
            p.line(Map.mapToVizX(bestBundle.myAbsoluteLocation.x + bestBundle.here.x),
                    Map.mapToVizY(bestBundle.myAbsoluteLocation.y + bestBundle.here.y),
                    Map.mapToVizX(f.getx()),
                    Map.mapToVizY(f.gety()));
            p.strokeWeight(Map.mapToVizX(g.optimalChosenAmount * 0.3));
            p.stroke(0);
            p.line(Map.mapToVizX(bestBundle.myAbsoluteLocation.x + bestBundle.here.x),
                    Map.mapToVizY(bestBundle.myAbsoluteLocation.y + bestBundle.here.y),
                    Map.mapToVizX(f.getx()),
                    Map.mapToVizY(f.gety()));


        }

        //Bundle boxes
        //avoid concurrent wotsits
        try {

            for (Bundle b : pa.initialBundles) {

                if (b.maxUtility > maxBundle) {
                    maxBundle = b.maxUtility;
                    bestBundle = b;
                }

                float rectSize = Map.mapLowToHigh_DoublesInFloatOut(
                        b.maxUtility, minMaxBundle.lowVal,
                        minMaxBundle.highVal, 0.005, 0.05);

                col1 = Map.mapLowHighToColour_DoublesColorsInColorOut(
                        b.maxUtility, minMaxBundle.lowVal,
                        minMaxBundle.highVal, Color.black, Color.white);

//            c1 = Map.mapLowHighToColour_DoublesColorsInColorOut(
//                    pe.maxUtility, utilityMinMax.lowVal,
//                    utilityMinMax.highVal, Color.white, Color.black);
//                    vs.variableData.get(vs.variableData.size())[a.ID], Color.green, Color.red);

//                p.fill(0, 0);
                p.fill(col1.getRGB());

//            p.strokeJoin(p.MITER);
                p.stroke(0);
                p.strokeWeight(Map.mapToVizX(0.004));

                p.noSmooth();
                p.rect(Map.mapToVizX(b.myAbsoluteLocation.x + b.here.x), Map.mapToVizY(b.myAbsoluteLocation.y + b.here.y),
                        Map.mapToVizX(rectSize), Map.mapToVizY(rectSize));


            }

        } catch (ConcurrentModificationException e) {
            System.out.println("ping!");
        }



        //My old location (Lookatme method takes care of 'current' one (i.e. has been changed
        //since bundle created because of chosen optimum)
//                p.stroke(255, 0, 0);
        p.smooth();

        p.fill(255);
        p.stroke(255);
        p.strokeWeight(Map.mapToVizX(0.02f));
        triangle((float) bestBundle.myAbsoluteLocation.x, (float) bestBundle.myAbsoluteLocation.y, 0.05f);
        p.stroke(0);
        p.strokeWeight(Map.mapToVizX(0.01f));
        triangle((float) bestBundle.myAbsoluteLocation.x, (float) bestBundle.myAbsoluteLocation.y, 0.05f);

        //"current location" is actually last location, so there's no density cost radius marker. Just need to add one as standin.
        p.smooth();
        p.strokeWeight(Map.mapToVizX(0.004));
        p.stroke(0, 100);
        p.noFill();
        p.ellipse(Map.mapToVizX(bestBundle.myAbsoluteLocation.x), Map.mapToVizY(bestBundle.myAbsoluteLocation.y),
                Map.mapToVizX(gl.DENSITYCOSTRADIUS * 2), Map.mapToVizY(gl.DENSITYCOSTRADIUS * 2));




    }//end method drawBundles
}


/*
 * Cuttingz
 * 
 * System.out.println("colValTest. val, low, high, out: " + pe.maxUtility + ","
                    + utilityMinMax.lowVal + ","
                    + utilityMinMax.highVal + ","
                    + colValTest);

 */