/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package location2012.control;

import location2012.observe.Audible;
import location2012.observe.ShoutEvent;
import location2012.utils.gl;

/**
 *
 * @author Dan
 */
public class LineVisMouseChanger extends VarChanger implements MouseButtons {

    //which mouseButtons to be available to
    boolean left, right;
    String name;
    int id;
    

    public LineVisMouseChanger(Audible a, double val, String name, boolean left, boolean right) {
        super(a, val);

        setLeft(left);
        setRight(right);
        setName(name);

    }

    @Override
    public void heard(ShoutEvent s) {

       gl.visFactorBuyLines = (float) inVals[1] * 10000;
        gl.visFactorBuyLines = (gl.visFactorBuyLines < 0 ? 0 : gl.visFactorBuyLines);
        displayVal = gl.visFactorBuyLines;

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
