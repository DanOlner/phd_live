/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package location2012.control;

import location2012.graphics.ProcessingSpaceDrawer;
import location2012.observe.Audible;
import location2012.observe.ShoutEvent;

/**
 *
 * @author Dan
 */
public class ModelViewMouseChanger extends VarChanger implements MouseButtons {

    //which mouseButtons to be available to
    boolean left, right;
    String name;
    int id;
    //for holding previous values to test for movement
    double[] pVals = new double[2];
    double[] compareVals = new double[2];
    boolean first = true;

    public ModelViewMouseChanger(Audible a, double val, String name, boolean left, boolean right) {
        super(a, val);

        setLeft(left);
        setRight(right);
        setName(name);

    }

    @Override
    public void heard(ShoutEvent s) {



        //if the values changed from last time we updated the 'previous' values
//        if (pVals[0] - vals[0] == 0 && pVals[1] - vals[1] == 0) {
//
//            System.out.println("mouse resting");
//            first = true;
//
//        } else {
//
//            if (first) {
//                compareVals[0] = vals[0];
//                compareVals[1] = vals[1];
//                first = false;
//
//            } else {

        ProcessingSpaceDrawer.hRotateVal += (float) ((inVals[0] - pVals[0]) * Math.PI*1.5);
        ProcessingSpaceDrawer.vRotateVal += (float) ((inVals[1] - pVals[1]) * Math.PI/1.9);

        pVals[0] = inVals[0];
        pVals[1] = inVals[1];

    }

    public boolean useLeft() {
        return left;
    }

    public boolean useRight() {
        return right;
    }

    public void setLeft(boolean l) {
        left = l;
    }

    public void setRight(boolean r) {
        right = r;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public int getID() {
        return id;
    }

    public void setID(int id) {
        this.id = id;
    }
}
