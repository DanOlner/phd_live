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
public class TransmissionDraw1D implements Drawable {

    MinMaxValues minMaxTime, minMaxUtil;
    Person pe;
    Color c1, highc, lowc;
    VariableStore vs_contribTime, vs_goodCost, vs_productionLevel,
            vs_stock, vs_peopleX, vs_peopleY, vs_firmX, vs_firmY;
    Double[] contribTime, px, py, fx, fy, goodCost, stock, productionLevel;
    //hackeroos
    boolean firmDrawSwitch = true;

    public TransmissionDraw1D() {
        //Set up calculation of min-max vals for utility
        minMaxTime = new MinMaxValues(gl.time, 1, 2, gl.firms, "currentContributedTime");
        minMaxUtil = new MinMaxValues(gl.time, 1, 2, gl.people, "maxUtility");

        lowc = new Color(180, 209, 219);
//        highc = new Color(214,255,216);
//        highc = new Color(112, 204, 55);
        highc = new Color(255, 100, 50);

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

        p.stroke(0);
        p.strokeWeight(Map.mapToVizX(0.004));

        if (gl.space == gl.SpaceType.TwoRegion) {
            xAxisForLocationTwoRegion(p);
            peopleInRegions(p);
        } else {
            xAxisForLocation(p);
            peopleSquares(p);
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

                p.rect(Map.mapToVizX(0.3 + adjust + (fx[i] / 2)) - barWidth,
//                        gl.vizWidth - 50 - Map.mapToVizY(contribTime[i] * 0.2),
                        gl.vizWidth - 50 - Map.mapToVizY(productionLevel[i] * 0.2),
                        Map.mapToVizX(0.3 + adjust + (fx[i] / 2)) + barWidth, gl.vizWidth - 50);



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
                p.text("good: " + Double.toString(MathsBits.roundToDecimals(goodCost[i], 2)),
                        Map.mapToVizX(0.3 + adjust + (fx[i] / 2)), gl.vizWidth / 2);

                p.text("time: " + Double.toString(MathsBits.roundToDecimals(contribTime[i], 2)),
                        Map.mapToVizX(0.3 + adjust +(fx[i] / 2)), (gl.vizWidth / 2) + 20);

                p.text("stock: " + Double.toString(MathsBits.roundToDecimals(stock[i], 2)),
                        Map.mapToVizX(0.3 + adjust + (fx[i] / 2)), (gl.vizWidth / 2) + 40);


            } catch (Throwable e) {

                System.err.println("TransmissionDraw1D. day: " + gl.day + ", data: " + contribTime.length + ", x: "
                        + fx.length + ", y: " + fy.length + ", " + e.toString());

            }

        }

    }

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