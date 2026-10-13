/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package location2012.graphics;

import java.awt.Color;
import java.awt.Point;
import location2012.Actor;
import location2012.Person;
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
public class TransmissionDraw1DPresentationForManyAgents implements Drawable {

    MinMaxValues minMaxTime, minMaxUtil;
    Person pe;
    Color c1, highc, lowc;
    VariableStore vs_contribTime, vs_goodCost, vs_productionLevel,
            vs_stock, vs_peopleX, vs_peopleY, vs_firmX, vs_firmY, vs_utility;
    //specifically for assigning quantity being shown in height of bars
    VariableStore vs_y_axis_Firms, vs_y_axis_People;
    Double[] contribTime, px, py, fx, fy, goodCost, stock,
            productionLevel, y_axis_Firms, y_axis_People, utility;
    //hackeroos
    boolean firmDrawSwitch = true;
    //Controls width of overall graph normalised to window width
    float widthOfGraph = 0.95f;
    //I'll do these so I'm only changing adjustment values once not four times
    float widthOfLines, widthOfBaseLine, numberPos, widthOfAgents, labelPos;
    //for drawing line where average utility is. 
    float utilityMean;

    public TransmissionDraw1DPresentationForManyAgents() {

        //set relative position bits as multipliers of widthOfGraph
        widthOfLines = widthOfGraph * 0.95f;
        widthOfBaseLine = widthOfLines * 0.98f;
        numberPos = widthOfGraph * widthOfLines * 1.1f;
        widthOfAgents = widthOfGraph * widthOfLines * 0.88f;
        labelPos = 1 - (widthOfGraph * widthOfLines * 1.1f);

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
//        vs_utility = gl.mainDataStore.getVariableStoreByVariableName("maxUtility");
        //        vsg = gl.mainDataStore.getVariableStoreByVariableLabel("goodCost");
        vs_goodCost = gl.mainDataStore.getVariableStoreByVariableLabel("good cost");
        vs_stock = gl.mainDataStore.getVariableStoreByVariableName("stock");
        vs_peopleX = gl.mainDataStore.getVariableStoreByVariableLabel("people_x");
        vs_peopleY = gl.mainDataStore.getVariableStoreByVariableLabel("people_y");
        vs_firmX = gl.mainDataStore.getVariableStoreByVariableLabel("firm_x");
        vs_firmY = gl.mainDataStore.getVariableStoreByVariableLabel("firm_y");

        //set y axis value
        vs_y_axis_Firms = gl.mainDataStore.getVariableStoreByVariableName("productionLevel");
        vs_y_axis_People = gl.mainDataStore.getVariableStoreByVariableName("maxUtility");

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
        y_axis_Firms = vs_y_axis_Firms.getLatestArrayListOfData();
        y_axis_People = vs_y_axis_People.getLatestArrayListOfData();

        //find utility mean
        try {
            utilityMean = 0;
            for (Double d : y_axis_People) {
                utilityMean += d;
            }
        } catch (Exception e) {
        }

        utilityMean /= y_axis_People.length;

        p.stroke(0);
        p.strokeWeight(Map.mapToVizX(0.004));

        if (gl.space == gl.SpaceType.TwoRegion) {
//            xAxisForLocationTwoRegion(p);
//            xAxisForLocationTwoRegionFourFirms(p);            
//            peopleInRegions(p);
        } else {
            xAxisForLocationLineFirms(p);
            xAxisForLocationLinePeople(p);
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
        for (float i = 0; i < gl.width / yAxisMultiplier; i += (gl.width / yAxisMultiplier / (float) 10)) {

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
        p.text(vs_y_axis_Firms.varNameLabel, 0, 0);
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
                        Map.mapToVizY(base - y_axis_Firms[i] * yAxisMultiplier),
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

    private void xAxisForLocationLineFirms(PGraphics p) {

        p.strokeCap(p.SQUARE);
        p.stroke(0);
        p.strokeWeight(Map.mapToVizX(0.004));

        //set position vars
        //Scaler for multiplying the value that's on the y axis
        float yAxisMultiplier = 0.5f;
//        float yAxisMultiplier = 0.13f;
//        float yAxisMultiplier = 0.8f;

        //location of base of model drawing where bottom of bars go (and y axis zero)
        //In terms of model width; put through mapper late
        float base = 0.45f;

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
        for (float i = 0; i < gl.width / yAxisMultiplier; i += 0.1) {

//            System.out.println("i = " + i);

            //to keep from drawing too many lines. You'd think I could get the for loop to do this...!
            if (Map.mapToVizY(base - (i * yAxisMultiplier)) > (0)) {
//            if (Map.mapToVizY(base - (i * yAxisMultiplier)) > (gl.vizWidth * 0.1)) {

                p.stroke(150);
                p.strokeWeight(Map.mapToVizX(0.003));

                //factor width using widthofGraph leaving room for numbers and labels
                p.line(Map.mapToVizX(((float) gl.width * (1 - (widthOfLines)))),
                        Map.mapToVizY(base - (i * yAxisMultiplier)),
                        Map.mapToVizX(((float) gl.width * (widthOfLines))),
                        Map.mapToVizY(base - (i * yAxisMultiplier)));
//                p.line(Map.mapToVizX(((float) gl.width / (float) 7)),
//                        Map.mapToVizY(base - (i * yAxisMultiplier)),
//                        Map.mapToVizX(((float) gl.width * ((float) 6 / (float) 7))),
//                        Map.mapToVizY(base - (i * yAxisMultiplier)));

                p.stroke(0);
                p.textSize(Map.mapToVizX(0.035));

                //text markers on right hand side
                if (i == 0) {
                    p.text(0,
                            Map.mapToVizX(((float) gl.width * numberPos)),
                            Map.mapToVizY(base - (i * yAxisMultiplier) + 0.01));
                } else {
                    p.text("" + MathsBits.roundToDecimals((double) i, 1, true),
                            Map.mapToVizX(((float) gl.width * numberPos)),
                            Map.mapToVizY(base - (i * yAxisMultiplier) + 0.01));
                }

            }//if gl.visWidth

        }//for float i

        //HACKING IN SOME EXTRA LINES TO SHOW WHERE THE NEXT GRAPH SCALE IS
//        for (float i = 0; i < gl.width / yAxisMultiplier; i += (gl.width / yAxisMultiplier / (float) 10)) {
        if (false) {
            for (float i = 0; i < 0.5; i += 0.1) {

//            System.out.println("i = " + i);

                //to keep from drawing too many lines. You'd think I could get the for loop to do this...!
//            if (Map.mapToVizY(base - (i * yAxisMultiplier)) > (0)) {
                if (true) {
//            if (Map.mapToVizY(base - (i * yAxisMultiplier)) > (gl.vizWidth * 0.1)) {

//                    p.stroke(150);
//                    p.strokeWeight(Map.mapToVizX(0.00125));

                    //factor width using widthofGraph leaving room for numbers and labels
                    p.line(Map.mapToVizX(((float) gl.width * (1 - (widthOfLines)))),
                            Map.mapToVizY(base - (i * yAxisMultiplier)),
                            Map.mapToVizX(((float) gl.width * (widthOfLines))),
                            Map.mapToVizY(base - (i * yAxisMultiplier)));
//                p.line(Map.mapToVizX(((float) gl.width / (float) 7)),
//                        Map.mapToVizY(base - (i * yAxisMultiplier)),
//                        Map.mapToVizX(((float) gl.width * ((float) 6 / (float) 7))),
//                        Map.mapToVizY(base - (i * yAxisMultiplier)));


                    p.stroke(150);
                    p.textSize(Map.mapToVizX(0.03));

                    //text markers on right hand side
                    if (i > 0.31) {
                        p.text("" + MathsBits.roundToDecimals((double) i, 1, true),
                                Map.mapToVizX(((float) gl.width * numberPos)),
                                Map.mapToVizY(base - (i * yAxisMultiplier) + 0.005));
                    }

                }//if gl.visWidth

            }//for float i
        }//if boolean


        //Draw one base line to mark edge of graph
        p.stroke(0);
        p.strokeWeight(Map.mapToVizX(0.005));
        float lineWidth = 0.35f;
        float baseDistance = 1.02f;
        //Draw a line to mark base
        p.line(Map.mapToVizX(gl.width * (1 - widthOfBaseLine)), Map.mapToVizY(base * baseDistance),
                Map.mapToVizX(gl.width * widthOfBaseLine), Map.mapToVizY(base * baseDistance));

//        p.line(Map.mapToVizX(((float) gl.width * ((float) 2 / (float) 3)) - lineWidth),
//                Map.mapToVizY(base * baseDistance),
//                Map.mapToVizX(((float) gl.width * ((float) 2 / (float) 3)) + lineWidth),
//                Map.mapToVizY(base * baseDistance));


        //write label for distance
        p.textSize(Map.mapToVizX(0.035));

        p.text("distance",
                Map.mapToVizX((float) gl.width / 2),
                Map.mapToVizY(base * baseDistance * 1.11));

        //rho and distance costs
//        p.text("rho: " + gl.rho,
//                Map.mapToVizX((float) gl.width / 3), Map.mapToVizY(base * baseDistance * 1.15));
//        p.text("distance cost: " + gl.deliveryCost,
//                (float) gl.width * ((float) 2 / (float) 3), Map.mapToVizY(base * baseDistance * 1.15));


        //y axis label
        //nabbed from http://forum.processing.org/topic/vertical-text
        p.pushMatrix();
        p.translate(Map.mapToVizX(gl.width * labelPos), Map.mapToVizY(0.25f * gl.width));
        p.rotate(-p.HALF_PI);
        p.text("Firms: " + vs_y_axis_Firms.varNameLabel, 0, 0);
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
                barWidth /= 4;

//                System.out.println("drawing firms. X loc for " + i + ": " + fx[i]);

                //little hack to space two firms in one region a little distance apart
//                firmDrawSwitch = (firmDrawSwitch ? false : true);

//                double adjust = (firmDrawSwitch ? -0.055 : 0.055);
                //Used to expand or contract size/spacing of line
                double adjust = widthOfAgents;//cos I can't be bothered to change them!

                p.strokeWeight(Map.mapToVizX(0.003));

                //Starting at (1 - adjust)/2. Then make sure to finish in the right place! 
                p.rect(Map.mapToVizX(((1 - adjust) / 2) + (fx[i] * adjust)) - barWidth,
                        //                        gl.vizWidth - 50 - Map.mapToVizY(contribTime[i] * 0.2),
                        Map.mapToVizY(base - y_axis_Firms[i] * yAxisMultiplier),
                        Map.mapToVizX(((1 - adjust) / 2) + (fx[i] * adjust)) + barWidth,
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

    private void xAxisForLocationLinePeople(PGraphics p) {

        p.strokeCap(p.SQUARE);
        p.stroke(0);
        p.strokeWeight(Map.mapToVizX(0.004));

        //set position vars
        //Scaler for multiplying the value that's on the y axis
        float yAxisMultiplier = 0.2f;

        //location of base of model drawing where bottom of bars go (and y axis zero)
        //In terms of model width; put through mapper late
        float base = 0.55f;

        p.rectMode(p.CORNERS);
        //rectMode(CENTER) interprets the first two parameters of rect() 
        //as the shape's center point, while the third and fourth parameters are its width and height.
//        p.rectMode(p.CENTER);
        p.textAlign(p.CENTER);

        //draw axis. First, so in background
        p.textFont(ProcessingSpaceDrawer.presentFont);
        p.textMode(p.MODEL);

        float linegap = 0.003f;
        p.stroke(50);
        p.strokeWeight(Map.mapToVizX(0.003));
        //draw mean line first, want in background
        p.line(Map.mapToVizX(((float) gl.width * (1 - widthOfLines))),
                Map.mapToVizY(base + (utilityMean * yAxisMultiplier) - linegap),
                Map.mapToVizX(((float) gl.width * widthOfLines)),
                Map.mapToVizY(base + (utilityMean * yAxisMultiplier) - linegap));

        p.line(Map.mapToVizX(((float) gl.width * (1 - widthOfLines))),
                Map.mapToVizY(linegap + base + (utilityMean * yAxisMultiplier)),
                Map.mapToVizX(((float) gl.width * widthOfLines)),
                Map.mapToVizY(linegap + base + (utilityMean * yAxisMultiplier)));

        //draw horizontal lines for tick points with text by their side
        //gl.width / yAxisMultiplier: the full range of the value that's going to be displayed
        //(gl.width / yAxisMultiplier / (float) 10): breaking that down into 10 ticks.
        //x/y/z == x/yz. But easier to see in code this way.
//        for (float i = 0; i < gl.width / yAxisMultiplier; i += (gl.width / yAxisMultiplier / (float) 10)) {
        for (float i = 0; i < gl.width / yAxisMultiplier; i += (yAxisMultiplier * 2)) {

//            System.out.println("i = " + i);

            //to keep from drawing too many lines. You'd think I could get the for loop to do this...!
            if (Map.mapToVizY(base + (i * yAxisMultiplier)) > (0)) {
//            if (Map.mapToVizY(base - (i * yAxisMultiplier)) > (gl.vizWidth * 0.1)) {

                p.stroke(150);
                p.strokeWeight(Map.mapToVizX(0.003));

                p.line(Map.mapToVizX(((float) gl.width * (1 - widthOfLines))),
                        Map.mapToVizY(base + (i * yAxisMultiplier)),
                        Map.mapToVizX(((float) gl.width * widthOfLines)),
                        Map.mapToVizY(base + (i * yAxisMultiplier)));

                p.stroke(0);
                p.textSize(Map.mapToVizX(0.035));

                //text markers on right hand side
                if (i < 10 || i % 1 == 0) {

                    if (i == 0) {
                        p.text(0,
                                Map.mapToVizX(((float) gl.width * numberPos)),
                                Map.mapToVizY(base + (i * yAxisMultiplier) + 0.01));
                    } else {
//                        p.text((int) i,
                        p.text("" + MathsBits.roundToDecimals((double) i, 1, true),
                                Map.mapToVizX(((float) gl.width * numberPos)),
                                Map.mapToVizY(base + (i * yAxisMultiplier) + 0.01));
                    }

                } else {
                    p.text("" + (int) i,
                            Map.mapToVizX(((float) gl.width * numberPos)),
                            Map.mapToVizY(base + (i * yAxisMultiplier) + 0.01));
                }

            }//if gl.visWidth

        }//for float i


        //Draw one base line to mark edge of graph
        p.stroke(0);
        p.strokeWeight(Map.mapToVizX(0.005));
        float lineWidth = 0.35f;
        float baseDistance = 0.98f;
        //Draw one line for each region, symmetrically around the quarter points
        p.line(Map.mapToVizX(gl.width * (1 - widthOfBaseLine)), Map.mapToVizY(base * baseDistance),
                Map.mapToVizX(gl.width * widthOfBaseLine), Map.mapToVizY(base * baseDistance));
//        p.line(Map.mapToVizX(((float) gl.width / 2) - lineWidth), Map.mapToVizY(base * baseDistance),
//                Map.mapToVizX(((float) gl.width / 2) + lineWidth), Map.mapToVizY(base * baseDistance));



        //write label for distance
        p.textSize(Map.mapToVizX(0.035));

//        p.text("distance",
//                Map.mapToVizX((float) gl.width / 2),
//                Map.mapToVizY(base * baseDistance * 1.1));

        //rho and distance costs
//        p.text("rho: " + gl.rho,
//                Map.mapToVizX((float) gl.width / 3), Map.mapToVizY(base * baseDistance * 1.15));
//        p.text("distance cost: " + gl.deliveryCost,
//                (float) gl.width * ((float) 2 / (float) 3), Map.mapToVizY(base * baseDistance * 1.15));


        //y axis label
        //nabbed from http://forum.processing.org/topic/vertical-text
        p.pushMatrix();
        p.translate(Map.mapToVizX(gl.width * labelPos), Map.mapToVizY(0.75f * gl.width));
        p.rotate(-p.HALF_PI);
        p.text("People: " + vs_y_axis_People.varNameLabel, 0, 0);
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
                        y_axis_People[i], minMaxUtil.lowVal,
                        minMaxUtil.highVal, lowc, highc);

                p.fill(c1.getRGB());

                //width of bar, fit to number
                float barWidth = (float) gl.vizWidth / (float) gl.firms.size();
                //split in two for each side
                barWidth /= 6;

//                System.out.println("drawing firms. X loc for " + i + ": " + fx[i]);

                //little hack to space two firms in one region a little distance apart
//                firmDrawSwitch = (firmDrawSwitch ? false : true);

//                double adjust = (firmDrawSwitch ? -0.055 : 0.055);
                //Used to expand or contract size/spacing of line
                double adjust = widthOfAgents;//cos I can't be bothered to change them!

                p.strokeWeight(Map.mapToVizX(0.0015));

                //Starting at (1 - adjust)/2. Then make sure to finish in the right place! 
                p.rect(Map.mapToVizX(((1 - adjust) / 2) + (px[i] * adjust)) - barWidth,
                        //                        gl.vizWidth - 50 - Map.mapToVizY(contribTime[i] * 0.2),
                        Map.mapToVizY(base + y_axis_People[i] * yAxisMultiplier),
                        Map.mapToVizX(((1 - adjust) / 2) + (px[i] * adjust)) + barWidth,
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

    private void dashedLine(PGraphics p) {
        //from http://processing.org/discourse/beta/num_1202486379.html
    }//method dashedLine
    /**
     *
     * From: http://processing.org/discourse/beta/num_1202486379.html
     *
     * Draw a dashed line with given set of dashes and gap lengths. x0 starting
     * x-coordinate of line. y0 starting y-coordinate of line. x1 ending
     * x-coordinate of line. y1 ending y-coordinate of line. spacing array
     * giving lengths of dashes and gaps in pixels; an array with values {5, 3,
     * 9, 4} will draw a line with a 5-pixel dash, 3-pixel gap, 9-pixel dash,
     * and 4-pixel gap. if the array has an odd number of entries, the values
     * are recycled, so an array of {5, 3, 2} will draw a line with a 5-pixel
     * dash, 3-pixel gap, 2-pixel dash, 5-pixel gap, 3-pixel dash, and 2-pixel
     * gap, then repeat.
     *
     */
//void dashline(float x0, float y0, float x1, float y1, float[ ] spacing, PGraphics p) 
//{ 
////  float distance = (float) Point.distance((double) x0, (double) y0, (double) x1, (double) y1); 
//  float distance = (float) Point.distance(x0, y0, x1, y1); 
//  float [ ] xSpacing = new float[spacing.length]; 
//  float [ ] ySpacing = new float[spacing.length]; 
//  float drawn = 0.0f;  // amount of distance drawn 
// 
//  if (distance > 0) 
//  { 
//    int i; 
//    boolean drawLine = true; // alternate between dashes and gaps 
// 
//    /* 
//      Figure out x and y distances for each of the spacing values 
//      I decided to trade memory for time; I'd rather allocate 
//      a few dozen bytes than have to do a calculation every time 
//      I draw. 
//    */ 
//    for (i = 0; i < spacing.length; i++) 
//    { 
//      xSpacing[i] = p.lerp(0, (x1 - x0), spacing[i] / distance); 
//      ySpacing[i] = p.lerp(0, (y1 - y0), spacing[i] / distance);       
//    } 
// 
//    i = 0; 
//    while (drawn < distance) 
//    { 
//      if (drawLine) 
//      { 
//        line(x0, y0, x0 + xSpacing[i], y0 + ySpacing[i]); 
//      } 
//      x0 += xSpacing[i]; 
//      y0 += ySpacing[i]; 
//      /* Add distance "drawn" by this line or gap */ 
//      drawn = drawn + mag(xSpacing[i], ySpacing[i]); 
//      i = (i + 1) % spacing.length;  // cycle through array 
//      drawLine = !drawLine;  // switch between dash and gap 
//    } 
//  } 
//} 
}


/*
 * Cuttingz
 * 
 * System.out.println("colValTest. val, low, high, out: " + pe.maxUtility + ","
                    + minMax.lowVal + ","
                    + minMax.highVal + ","
                    + colValTest);

 */