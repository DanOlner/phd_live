/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package location2012.graphics;

import java.awt.Color;
import java.util.ArrayList;
import location2012.Actor;
import location2012.Person;
import location2012.PersonAction;
import location2012.econs.Bundle;
import location2012.utils.Map;
import location2012.utils.MathsBits;
import location2012.utils.gl;
import location2012.variables.MinMaxValues;
import location2012.variables.VariableStore;
import processing.core.PApplet;
import processing.core.PGraphics;

/**
 * An element to be drawn by Visualiser. Responsible for actually drawing to the
 * PApplet
 *
 * @author geodo
 */
public class TransmissionDraw1DPresentation implements Drawable {

    MinMaxValues minMaxTime, minMaxUtil;
    Person pe;
    Color c1, highc, lowc;
    VariableStore vs_contribTime, vs_goodCost, vs_productionLevel,
            vs_stock, vs_peopleX, vs_peopleY, vs_firmX, vs_firmY;
    //specifically for assigning quantity being shown in height of bars
    VariableStore y_axis;
    Double[] contribTime, px, py, fx, fy, goodCost, stock, productionLevel, y_axis_vals;
    //hackeroos
    boolean firmDrawSwitch = true;

    public TransmissionDraw1DPresentation() {
        //Set up calculation of min-max vals for utility
        minMaxTime = new MinMaxValues(gl.time, 1, 2, gl.firms, "currentContributedTime");
        minMaxUtil = new MinMaxValues(gl.time, 1, 2, gl.people, "maxUtility");

//        lowc = new Color(180, 209, 219);
//        highc = new Color(255, 100, 50);

//        highc = new Color(214,255,216);
//        highc = new Color(112, 204, 55);

        //B&W version
        highc = new Color(255, 255, 255);
        lowc = new Color(40, 40, 40);
//        lowc = new Color(255,255,255);
//        highc = new Color(40,40,40);


        //get local reference to data
        vs_contribTime = gl.mainDataStore.getVariableStoreByVariableName("currentContributedTime");
        vs_productionLevel = gl.mainDataStore.getVariableStoreByVariableName("productionLevel");
        //        vsg = gl.mainDataStore.getVariableStoreByVariableLabel("goodCost");
        vs_goodCost = gl.mainDataStore.getVariableStoreByVariableLabel("good cost");
        vs_stock = gl.mainDataStore.getVariableStoreByVariableName("stock");
        vs_peopleX = gl.mainDataStore.getVariableStoreByVariableLabel("people_x");
        vs_peopleY = gl.mainDataStore.getVariableStoreByVariableLabel("people_y");
        vs_firmX = gl.mainDataStore.getVariableStoreByVariableLabel("firm_x");
        vs_firmY = gl.mainDataStore.getVariableStoreByVariableLabel("firm_y");

        //set y axis value
        y_axis = gl.mainDataStore.getVariableStoreByVariableName("productionLevel");

    }

    public void draw2D(PGraphics p) {

        contribTime = vs_contribTime.getLatestArrayListOfData();
        px = vs_peopleX.getLatestArrayListOfData();
        py = vs_peopleY.getLatestArrayListOfData();
        fx = vs_firmX.getLatestArrayListOfData();
        fy = vs_firmY.getLatestArrayListOfData();
        goodCost = vs_goodCost.getLatestArrayListOfData();
        stock = vs_stock.getLatestArrayListOfData();
        productionLevel = vs_productionLevel.getLatestArrayListOfData();
        y_axis_vals = y_axis.getLatestArrayListOfData();

        p.stroke(0);
        p.strokeWeight(Map.mapToVizX(0.004));

        if (gl.space == gl.SpaceType.TwoRegion) {
//            xAxisForLocationTwoRegion(p);
            xAxisForLocationTwoRegionFourFirms(p);
//            peopleInRegions(p);
        } else {
            xAxisForLocationTwoRegionFourFirms(p);
//            xAxisForLocation(p);
//            peopleSquares(p);
//            peopleInRegions(p);
        }

    }//end method draw2D

    private void peopleInRegions(PGraphics p) {


        int reg0 = 0, reg1 = 0;
        //Just counting the number in each region for now
        for (Actor a : gl.people) {

            if (a.getx() < gl.width / 2) {
                reg0++;
            } else {
                reg1++;
            }

        }//for actor a

//        System.out.println("people in 0:" + reg0 + ", 1: " + reg1);
        p.text(Integer.toString(reg0), Map.mapToVizX(0.3), gl.vizWidth / 4);
        p.text(Integer.toString(reg1), Map.mapToVizX(0.7), gl.vizWidth / 4);

//        //show bundle optima for a single Person
//        PersonAction pa = (PersonAction) gl.people.get(gl.LOOKATME).actions.get(0);
//
////        ArrayList<Bundle> initialBundles = (ArrayList<Bundle>) pa.initialBundles.clone();
//        ArrayList<Bundle> initialBundles = (ArrayList<Bundle>) pa.bundlesToOptimise.clone();
//
//        for (Bundle bwp : initialBundles) {
//            p.text(Double.toString(bwp.maxUtility),
//                    Map.mapToVizX(0.3 + ((bwp.myAbsoluteLocation.x + bwp.here.x) * 0.5)), gl.vizWidth / 3);
//        }



    }

    private void xAxisForLocation(PGraphics p) {

        p.rectMode(p.CORNERS);

//        for (Actor a : gl.people) {
//        for (Double d : data) {
//        for (Double d : data) {
        for (int i = 0; i < fx.length; i++) {

//              pe = (SingleActorPerson) a;

            try {

                c1 = Map.mapLowHighToColour_DoublesColorsInColorOut(
                        contribTime[i], minMaxTime.lowVal,
                        minMaxTime.highVal, lowc, highc);

                p.fill(c1.getRGB());

                //width of bar, fit to number
                float barWidth = (float) gl.vizWidth / (float) gl.firms.size();
                //split in two for each side
                barWidth /= 3;

                p.rect(Map.mapToVizX(fx[i]) - barWidth,
                        //                        gl.vizWidth - 50 - Map.mapToVizY(productionLevel[i] * 0.1),
                        gl.vizWidth - 50 - Map.mapToVizY(contribTime[i] * 0.1),
                        Map.mapToVizX(fx[i]) + barWidth, gl.vizWidth - 50);
//            p.rect(Map.mapToVizX(a.getx()) - barWidth,
//                    gl.vizWidth - Map.mapToVizY(pe.currentContributedTime * 0.5),
//                    Map.mapToVizX(a.getx()) + barWidth, gl.vizWidth);


                //write current cost of 1 unit of output
                p.textSize(14);
                p.textAlign(p.CENTER);
                p.fill(0);

                float textx, texty;
//            textx = (x[i] > 

                //same height as bars
//            p.text(Double.toString(MathsBits.roundToDecimals(goodCost[i],2)), 
//                    Map.mapToVizX(x[i]),gl.vizWidth - Map.mapToVizY(data[i] * 0.25));

//                vertically centred
                p.text(Double.toString(MathsBits.roundToDecimals(goodCost[i], 2)),
                        Map.mapToVizX(fx[i]), gl.vizWidth / 2);

                p.text(Double.toString(MathsBits.roundToDecimals(contribTime[i], 2)),
                        Map.mapToVizX(fx[i]), (gl.vizWidth / 2) + 20);

                p.text(Double.toString(MathsBits.roundToDecimals(stock[i], 2)),
                        Map.mapToVizX(fx[i]), (gl.vizWidth / 2) + 40);

                p.text(Double.toString(MathsBits.roundToDecimals(productionLevel[i], 2)),
                        Map.mapToVizX(fx[i]), (gl.vizWidth / 2) + 60);

            } catch (Throwable e) {

                System.err.println("TransmissionDraw1D. day: " + gl.day + ", data: " + contribTime.length + ", x: "
                        + fx.length + ", y: " + fy.length + ", " + e.toString());

            }

        }

    }

    private void xAxisForLocationTwoRegion(PGraphics p) {

        p.rectMode(p.CORNERS);

//        for (Actor a : gl.people) {
//        for (Double d : data) {
//        for (Double d : data) {
        for (int i = 0; i < fx.length; i++) {

//              pe = (SingleActorPerson) a;

            try {

                c1 = Map.mapLowHighToColour_DoublesColorsInColorOut(
                        contribTime[i], minMaxTime.lowVal,
                        minMaxTime.highVal, lowc, highc);

                p.fill(c1.getRGB());

                //width of bar, fit to number
                float barWidth = (float) gl.vizWidth / (float) gl.firms.size();
                //split in two for each side
                barWidth /= 3;

//                System.out.println("drawing firms. X loc for " + i + ": " + fx[i]);

                //little hack to space two firms in one region a little distance apart
                firmDrawSwitch = (firmDrawSwitch ? false : true);

                double adjust = (firmDrawSwitch ? -0.085 : 0.085);

                p.rect(Map.mapToVizX(0.25 + adjust + (fx[i] / 2)) - barWidth,
                        //                        gl.vizWidth - 50 - Map.mapToVizY(contribTime[i] * 0.2),
                        gl.vizWidth - 50 - Map.mapToVizY(productionLevel[i] * 0.2),
                        Map.mapToVizX(0.25 + adjust + (fx[i] / 2)) + barWidth, gl.vizWidth - 50);


                //write current cost of 1 unit of output
                p.textSize(14);
                p.textAlign(p.CENTER);
                p.fill(0);

            } catch (Throwable e) {

                System.err.println("TransmissionDraw1D. day: " + gl.day + ", data: " + contribTime.length + ", x: "
                        + fx.length + ", y: " + fy.length + ", " + e.toString());

            }

        }

    }//method xAxisForLocationTwoRegion

    private void xAxisForLocationTwoRegionFourFirms(PGraphics p) {

        p.strokeCap(p.SQUARE);
        p.stroke(0);
        p.strokeWeight(Map.mapToVizX(0.004));

        //set position vars
        //Scaler for multiplying the value that's on the y axis
        float yAxisMultiplier = 0.3f;

        //location of base of model drawing where bottom of bars go (and y axis zero)
        //In terms of model width; put through mapper late
        float base = 0.8f;

        p.rectMode(p.CORNERS);
        //rectMode(CENTER) interprets the first two parameters of rect() 
        //as the shape's center point, while the third and fourth parameters are its width and height.
//        p.rectMode(p.CENTER);
        p.textAlign(p.CENTER);

        //draw axis. First, so in background
        p.textFont(ProcessingSpaceDrawer.presentFont);
        p.textMode(p.MODEL);

        //draw horizontal lines for tick points with text by their side
        //gl.width / yAxisMultiplier: the full range of the value that's going to be displayed
        //(gl.width / yAxisMultiplier / (float) 10): breaking that down into 10 ticks.
        //x/y/z == x/yz. But easier to see in code this way.
//        for (float i = 0; i < gl.width / yAxisMultiplier; i += (gl.width / yAxisMultiplier / (float) 10)) {
        for (float i = 0; i < gl.width / yAxisMultiplier; i += 0.3) {

//            System.out.println("i = " + i);

            //to keep from drawing too many lines. You'd think I could get the for loop to do this...!
            if (Map.mapToVizY(base - (i * yAxisMultiplier)) > (gl.vizWidth * 0.1)) {

                p.stroke(150);
                p.strokeWeight(Map.mapToVizX(0.003));

                p.line(Map.mapToVizX(((float) gl.width / (float) 7)),
                        Map.mapToVizY(base - (i * yAxisMultiplier)),
                        Map.mapToVizX(((float) gl.width * ((float) 6 / (float) 7))),
                        Map.mapToVizY(base - (i * yAxisMultiplier)));

                p.stroke(0);
                p.textSize(Map.mapToVizX(0.04));

                //text markers on right hand side
                p.text("" + MathsBits.roundToDecimals((double) i, 1, true),
                        Map.mapToVizX(((float) gl.width * (float) 8 / 9)),
                        Map.mapToVizY(base - (i * yAxisMultiplier) + 0.01));

            }//if gl.visWidth

        }//for float i


        //Draw two base lines to mark separate regions
        p.stroke(0);
        p.strokeWeight(Map.mapToVizX(0.005));
        float lineWidth = 0.15f;
        float baseDistance = 1.02f;
        //Draw one line for each region, symmetrically around the quarter points
        p.line(Map.mapToVizX(((float) gl.width / 3) - lineWidth), Map.mapToVizY(base * baseDistance),
                Map.mapToVizX(((float) gl.width / 3) + lineWidth), Map.mapToVizY(base * baseDistance));

        p.line(Map.mapToVizX(((float) gl.width * ((float) 2 / (float) 3)) - lineWidth),
                Map.mapToVizY(base * baseDistance),
                Map.mapToVizX(((float) gl.width * ((float) 2 / (float) 3)) + lineWidth),
                Map.mapToVizY(base * baseDistance));


        //write region labels in the same place
        p.textSize(Map.mapToVizX(0.06));

        p.text("region 1",
                Map.mapToVizX((float) gl.width / 3),
                Map.mapToVizY(base * baseDistance * 1.1));
        p.text("region 2",
                Map.mapToVizX((float) gl.width * ((float) 2 / (float) 3)),
                Map.mapToVizY(base * baseDistance * 1.1));


        //rho and distance costs
//        p.text("rho: " + gl.rho,
//                Map.mapToVizX((float) gl.width / 3), Map.mapToVizY(base * baseDistance * 1.15));
//        p.text("distance cost: " + gl.deliveryCost,
//                (float) gl.width * ((float) 2 / (float) 3), Map.mapToVizY(base * baseDistance * 1.15));


        //y axis label
        //nabbed from http://forum.processing.org/topic/vertical-text
        p.pushMatrix();
        p.translate(Map.mapToVizX(0.1), Map.mapToVizY(0.5f * gl.width));
        p.rotate(-p.HALF_PI);
        p.text(y_axis.varNameLabel, 0, 0);
        p.popMatrix();
//            rotate(HALF_PI);
//            translate(-x, -y);

//        for (Actor a : gl.people) {
//        for (Double d : data) {
//        for (Double d : data) {
        for (int i = 0; i < fx.length; i++) {

//              pe = (SingleActorPerson) a;

            try {

                c1 = Map.mapLowHighToColour_DoublesColorsInColorOut(
                        contribTime[i], minMaxTime.lowVal,
                        minMaxTime.highVal, lowc, highc);

                p.fill(c1.getRGB());

                //width of bar, fit to number
                float barWidth = (float) gl.vizWidth / (float) gl.firms.size();
                //split in two for each side
                barWidth /= 6;

//                System.out.println("drawing firms. X loc for " + i + ": " + fx[i]);

                //little hack to space two firms in one region a little distance apart
                firmDrawSwitch = (firmDrawSwitch ? false : true);

                double adjust = (firmDrawSwitch ? -0.055 : 0.055);

                p.strokeWeight(Map.mapToVizX(0.003));

                p.rect(Map.mapToVizX(0.33333 + adjust + (fx[i] / 3)) - barWidth,
                        //                        gl.vizWidth - 50 - Map.mapToVizY(contribTime[i] * 0.2),
                        Map.mapToVizY(base - y_axis_vals[i] * yAxisMultiplier),
                        Map.mapToVizX(0.33333 + adjust + (fx[i] / 3)) + barWidth,
                        Map.mapToVizY(base));


                //write current cost of 1 unit of output
                p.textSize(14);
                p.textAlign(p.CENTER);
                p.fill(0);

//                p.text(Double.toString(MathsBits.roundToDecimals(productionLevel[i], 2)),
//                        Map.mapToVizX(0.33333 + adjust + (fx[i] / 3)), (gl.vizWidth / 2) + 60);

            } catch (Throwable e) {

                System.err.println("TransmissionDraw1D. day: " + gl.day + ", data: " + contribTime.length + ", x: "
                        + fx.length + ", y: " + fy.length + ", " + e.toString());

            }

        }

        p.textMode(p.SCREEN);
        p.textAlign(p.RIGHT);


    }//method xAxisForLocationTwoRegionFourFirms

    private void peopleSquares(PGraphics p) {

        p.rectMode(p.CORNER);

        p.noSmooth();

        for (Actor a : gl.people) {

            pe = (Person) a;

            float rectSize = Map.mapLowToHigh_DoublesInFloatOut(
                    pe.maxUtility, minMaxUtil.lowVal,
                    minMaxUtil.highVal, 0.02, 0.04);

            c1 = Map.mapLowHighToColour_DoublesColorsInColorOut(
                    pe.maxUtility, minMaxUtil.lowVal,
                    minMaxUtil.highVal, Color.green, Color.red);

            p.fill(0, 0);
            p.fill(c1.getRGB());

//            p.strokeJoin(p.MITER);
            p.stroke(0);
            p.strokeWeight(Map.mapToVizX(0.004));
//            p.rect(Map.mapToVizX(a.getx()), Map.mapToVizY(a.gety()), 
//                    rectSizeTest, rectSizeTest);

//            p.rect(Map.mapToVizX(a.getx()), Map.mapToVizY(a.gety()),
//                    5,5);
            p.rect(Map.mapToVizX(a.getx()), Map.mapToVizY(a.gety()),
                    Map.mapToVizX(rectSize), Map.mapToVizY(rectSize));



        }//end for each

    }//end method peopleSquares

    public void draw3D(PApplet p) {
    }
}


/*
 * Cuttingz
 * 
 * System.out.println("colValTest. val, low, high, out: " + pe.maxUtility + ","
                    + minMax.lowVal + ","
                    + minMax.highVal + ","
                    + colValTest);

 */