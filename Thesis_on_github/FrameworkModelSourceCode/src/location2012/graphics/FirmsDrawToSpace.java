/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package location2012.graphics;

import java.awt.Color;
import location2012.Actor;
import location2012.Firm;
import location2012.utils.Map;
import location2012.utils.gl;
import processing.core.PApplet;
import processing.core.PGraphics;

/**
 * An element to be drawn by Visualiser. Responsible for actually drawing to the
 * PApplet
 *
 * @author geodo
 */
public class FirmsDrawToSpace implements Drawable {

//    MinMaxBetweenDays utilityMinMax;
    Firm f;
    Color col1;
    //for e.g. size of pentagon
    float size;
    //pentagon coords
    //http://mathworld.wolfram.com/Pentagon.html
    float s1 = 0.9510565162951535f;
    float s2 = 0.5877852522924731f;
    float c1 = 0.30901699437494745f;
    float c2 = 0.8090169943749475f;

    public FirmsDrawToSpace() {
        //Set up calculation of min-max vals for utility
//        utilityMinMax = new MinMaxBetweenDays(gl.time, 1, 1, gl.people, "maxUtility");
    }

    public void draw2D(PGraphics p) {

        firmShapes(p);
//        firmSquares(p);    

    }

    private void firmSquares(PGraphics p) {

        p.smooth();

        for (Actor a : gl.firms) {

            f = (Firm) a;

//            float weightTest = Map.mapLowToHigh_DoublesInFloatOut(
//                    pe.maxUtility, utilityMinMax.lowestVal,
//                    utilityMinMax.highestVal, 1, 5);
//
//            float rectSizeTest = Map.mapLowToHigh_DoublesInFloatOut(
//                    pe.maxUtility, utilityMinMax.lowestVal,
//                    utilityMinMax.highestVal, 0.02, 0.04);
//
//            c1 = Map.mapLowHighToColour_DoublesColorsInColorOut(
//                    pe.maxUtility, utilityMinMax.lowestVal,
//                    utilityMinMax.highestVal, Color.green, Color.red);

            p.fill(0, 0);
//            p.fill(c1.getRGB());

//            p.strokeJoin(p.MITER);
            p.stroke(0);
            p.strokeWeight(Map.mapToVizX(0.004));

            p.fill(f.colour.getRGB());
            //Using white for CEUS paper
//            p.fill(255);

            p.rect(Map.mapToVizX(a.getx()), Map.mapToVizY(a.gety()),
                    Map.mapToVizX(0.05),
                    Map.mapToVizY(0.05));

            p.stroke(f.colour.getRGB(), 200);
            p.noFill();
//            p.fill(f.colour.getRGB(),20);

//            p.ellipse(Map.mapToVizX(a.getx()), Map.mapToVizY(a.gety()),
//                    Map.mapToVizX(f.yesterdayRevenue * 0.004), 
//                    Map.mapToVizY(f.yesterdayRevenue * 0.004));

        }

    }//end method firmSquares

    private void firmShapes(PGraphics p) {

        p.smooth();

        for (Actor a : gl.firms) {

            f = (Firm) a;

//            float weightTest = Map.mapLowToHigh_DoublesInFloatOut(
//                    pe.maxUtility, utilityMinMax.lowestVal,
//                    utilityMinMax.highestVal, 1, 5);
//
//            float rectSizeTest = Map.mapLowToHigh_DoublesInFloatOut(
//                    pe.maxUtility, utilityMinMax.lowestVal,
//                    utilityMinMax.highestVal, 0.02, 0.04);
//
//            c1 = Map.mapLowHighToColour_DoublesColorsInColorOut(
//                    pe.maxUtility, utilityMinMax.lowestVal,
//                    utilityMinMax.highestVal, Color.green, Color.red);

            p.fill(0, 0);
//            p.fill(c1.getRGB());

//            p.strokeJoin(p.MITER);
            p.stroke(0);
            p.strokeWeight(Map.mapToVizX(0.005));

//            p.fill(f.colour.getRGB());
            //white fill for CEUS paper
            p.fill(255);

            size = Map.mapToVizX(0.025f);
            
            //bit of hackery
//            size = Map.mapToVizX(f.commuteCost * 0.01);
//            size = Map.mapToVizX(f.goodCost * 0.1);
//            size = Map.mapToVizX(f.deliverycost * 0.01);

            p.translate(Map.mapToVizX(a.getx()), Map.mapToVizY(a.gety()));

            //see pentagon def in var defs above
//            p.beginShape();
//            p.vertex(0, -1f * size);
//            p.vertex(-s1 * size, -c1 * size);
//            p.vertex(-s2 * size, c2 * size);
//            p.vertex(s2 * size, c2 * size);
//            p.vertex(s1 * size, -c1 * size);
//            p.endShape(p.CLOSE);

            //cross
            p.beginShape();
            p.vertex(0.75f * size, -0.75f * size);
            p.vertex(2 * size, -0.75f * size);
            p.vertex(2 * size, 0.75f * size);
            p.vertex(0.75f * size, 0.75f * size);
            p.vertex(0.75f * size, 2 * size);
            p.vertex(-0.75f * size, 2 * size);
            p.vertex(-0.75f * size, 0.75f * size);
            p.vertex(-2 * size, 0.75f * size);
            p.vertex(-2 * size, -0.75f * size);
            p.vertex(-0.75f * size, -0.75f * size);
            p.vertex(-0.75f * size, -2 * size);
            p.vertex(0.75f * size, -2 * size);
            p.endShape(p.CLOSE);

            p.translate(-Map.mapToVizX(a.getx()), -Map.mapToVizY(a.gety()));

            p.stroke(f.colour.getRGB(), 200);
            p.noFill();
//            p.fill(f.colour.getRGB(),20);

//            p.ellipse(Map.mapToVizX(a.getx()), Map.mapToVizY(a.gety()),
//                    Map.mapToVizX(f.yesterdayRevenue * 0.004), 
//                    Map.mapToVizY(f.yesterdayRevenue * 0.004));

        }

    }//end method firmPentagons

    public void draw3D(PApplet p) {
    }
}


/*
 * Cuttingz
 * 
 * System.out.println("colValTest. val, low, high, out: " + pe.maxUtility + ","
                    + utilityMinMax.lowestVal + ","
                    + utilityMinMax.highestVal + ","
                    + colValTest);

 */