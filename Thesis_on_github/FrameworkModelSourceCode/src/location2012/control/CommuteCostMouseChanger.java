/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package location2012.control;

import java.util.ArrayList;
import location2012.Actor;
import location2012.Firm;
import location2012.observe.Audible;
import location2012.observe.ShoutEvent;
import location2012.utils.gl;

/**
 *
 * @author Dan
 */
public class CommuteCostMouseChanger extends VarChanger implements MouseButtons {

    //which mouseButtons to be available to
    boolean left, right;
    String name;
    int id;
    ArrayList<Actor> firms = new ArrayList<Actor>();
    Firm f;
    //for holding previous values to test for movement
    double[] pVals = new double[2];

    public CommuteCostMouseChanger(Audible a, double val, ArrayList<Actor> firms, String name, boolean left, boolean right) {
        super(a, val);

        setLeft(left);
        setRight(right);
        setName(name);

        this.firms = firms;

        displayVal = gl.commuteCost;

    }

    @Override
    public void heard(ShoutEvent s) {

//        gl.TECH = inVals[1];
//        gl.TECH = (Math.pow(10, (inVals[1]) * 3)) - 1;;
//        gl.TECH = (gl.TECH < 0 ? 0 : gl.TECH);
//        displayVal = gl.TECH;

//        gl.COMMUTECOST += s.heardValue;
//
//        gl.COMMUTECOST = (gl.COMMUTECOST < 0 ? 0 : gl.COMMUTECOST);
//
//        displayVal = gl.COMMUTECOST;
//        
        gl.commuteCost += s.heardValue;
//            gl.COMMUTECOST += ((Math.pow(10, (s.heardValue) * 3))/2)-1;
        gl.commuteCost = (gl.commuteCost < 0 ? 0 : gl.commuteCost);
        displayVal = gl.commuteCost;


        for (Actor a : firms) {
            f = (Firm) a;

            if (gl.commuteRange) {
                f.commuteCost = gl.commuteCost * (f.ID - gl.people.size() + 1);
            } else {
                f.commuteCost = gl.commuteCost;
            }

        }

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
