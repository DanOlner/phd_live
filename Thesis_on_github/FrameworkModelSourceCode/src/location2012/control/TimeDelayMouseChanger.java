/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package location2012.control;

import location2012.econs.OptimiseHillClimb;
import location2012.observe.Audible;
import location2012.observe.ShoutEvent;
import location2012.utils.gl;

/**
 *
 * @author Dan
 */
public class TimeDelayMouseChanger extends VarChanger implements MouseButtons {

    //which mouseButtons to be available to
    boolean left, right;
    String name;
    int id;

    public TimeDelayMouseChanger(Audible a, double val, String name, boolean left, boolean right) {
        super(a, val);


        setLeft(left);
        setRight(right);
        setName(name);

    }

    @Override
    public void heard(ShoutEvent s) {

//        OptimiseHillClimb.iterations = (int) (Math.pow(10, (inVals[1]) * 3)) - 1;
//        gl.EoS = Math.pow(10, 0.5 + (inVals[1]) * 2) - 1;
        gl.timeDelay += s.heardValue;
        
        gl.timeDelay = (gl.timeDelay < 0 ? 0 : gl.timeDelay);
                
        displayVal = gl.timeDelay;

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
