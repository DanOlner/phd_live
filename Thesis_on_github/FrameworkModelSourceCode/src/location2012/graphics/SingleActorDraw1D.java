/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package location2012.graphics;

import java.awt.Color;
import java.util.ArrayList;
import location2012.Actor;
import location2012.SingleActorPerson;
import location2012.io.DataStore;
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
public class SingleActorDraw1D implements Drawable {

    MinMaxValues minMax;
    SingleActorPerson pe;
    Color c1, highc, lowc;
    VariableStore vs_contribTime, vs_contribTimeToMyself, vs_goodCost, vs_stock, vsx, vsy;
    Double[] contribTime, timeContrib2myself, x, y, goodCost, stock;

    public SingleActorDraw1D() {
        //Set up calculation of min-max vals for utility
        minMax = new MinMaxValues(gl.time, 1, 2, gl.people, "currentContributedTime");

        lowc = new Color(180, 209, 219);
//        highc = new Color(214,255,216);
//        highc = new Color(112, 204, 55);
        highc = new Color(255, 100, 50);

        //get local reference to data
        vs_contribTime = gl.mainDataStore.getVariableStoreByVariableLabel("currentContributedTime");
        vs_contribTimeToMyself = gl.mainDataStore.getVariableStoreByVariableLabel("timeContributedToMyself");
//        vsg = gl.mainDataStore.getVariableStoreByVariableLabel("goodCost");
        vs_goodCost = gl.mainDataStore.getVariableStoreByVariableLabel("goodCost");
        vs_stock = gl.mainDataStore.getVariableStoreByVariableLabel("stock");
        vsx = gl.mainDataStore.getVariableStoreByVariableLabel("people_x");
        vsy = gl.mainDataStore.getVariableStoreByVariableLabel("people_y");

    }

    public void draw2D(PGraphics p) {

        contribTime = vs_contribTime.getLatestArrayListOfData();
        timeContrib2myself = vs_contribTimeToMyself.getLatestArrayListOfData();
        x = vsx.getLatestArrayListOfData();
        y = vsy.getLatestArrayListOfData();
        goodCost = vs_goodCost.getLatestArrayListOfData();
        stock = vs_stock.getLatestArrayListOfData();

        p.stroke(0);
        p.strokeWeight(Map.mapToVizX(0.004));

        xAxisForLocation(p);

    }

    private void xAxisForLocation(PGraphics p) {

        p.rectMode(p.CORNERS);

//        for (Actor a : gl.people) {
//        for (Double d : data) {
//        for (Double d : data) {
        for (int i = 0; i < contribTime.length; i++) {

//              pe = (SingleActorPerson) a;

            try {

                c1 = Map.mapLowHighToColour_DoublesColorsInColorOut(
                        contribTime[i], minMax.lowVal,
                        minMax.highVal, lowc, highc);

                p.fill(c1.getRGB());

                //width of bar, fit to number
                float barWidth = (float) gl.vizWidth / (float) gl.people.size();
                //split in two for each side
                barWidth /= 3;

                p.rect(Map.mapToVizX(x[i]) - barWidth,
                        gl.vizWidth - 50 - Map.mapToVizY(contribTime[i] * 0.25),
                        //                    gl.vizWidth - Map.mapToVizY(Math.log(data.get(i)*10) * 0.1),
                        Map.mapToVizX(x[i]) + barWidth, gl.vizWidth - 50);
//            p.rect(Map.mapToVizX(a.getx()) - barWidth,
//                    gl.vizWidth - Map.mapToVizY(pe.currentContributedTime * 0.5),
//                    Map.mapToVizX(a.getx()) + barWidth, gl.vizWidth);

                p.fill(100);
                //rectangle for the amount of time I'm adding to myself
                p.rect(Map.mapToVizX(x[i]) - barWidth,
                        gl.vizWidth - 50 - Map.mapToVizY(timeContrib2myself[i] * 0.25),
                        //                    gl.vizWidth - Map.mapToVizY(Math.log(data.get(i)*10) * 0.1),
                        Map.mapToVizX(x[i]) + barWidth, gl.vizWidth - 50);


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
                        Map.mapToVizX(x[i]), gl.vizWidth / 2);

                p.text(Double.toString(MathsBits.roundToDecimals(contribTime[i], 2)),
                        Map.mapToVizX(x[i]), (gl.vizWidth / 2) + 20);

                p.text(Double.toString(MathsBits.roundToDecimals(stock[i], 2)),
                        Map.mapToVizX(x[i]), (gl.vizWidth / 2) + 40);

            } catch (Throwable e) {

                System.err.println("SingleActorDraw1D. day: " + gl.day + ", data: " + contribTime.length + ", x: "
                        + x.length + ", y: " + y.length + ", " + e.toString());

            }

        }

    }

    private void peopleSquares(PGraphics p) {

        p.noSmooth();

        for (Actor a : gl.people) {

            pe = (SingleActorPerson) a;

            float rectSize = Map.mapLowToHigh_DoublesInFloatOut(
                    pe.maxUtility, minMax.lowVal,
                    minMax.highVal, 0.02, 0.04);

            c1 = Map.mapLowHighToColour_DoublesColorsInColorOut(
                    pe.maxUtility, minMax.lowVal,
                    minMax.highVal, Color.green, Color.red);

            p.fill(0, 0);
            p.fill(c1.getRGB());

//            p.strokeJoin(p.MITER);
            p.stroke(0);
            p.strokeWeight(Map.mapToVizX(0.004));
//            p.rect(Map.mapToVizX(a.getx()), Map.mapToVizY(a.gety()), 
//                    rectSizeTest, rectSizeTest);
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