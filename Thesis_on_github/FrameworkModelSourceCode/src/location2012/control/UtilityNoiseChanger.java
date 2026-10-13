    /*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package location2012.control;

import location2012.econs.CESOneTypeUtility;
import location2012.observe.Audible;
import location2012.observe.ShoutEvent;
import location2012.utils.gl;

/**
 *
 * @author Dan
 */
public class UtilityNoiseChanger extends VarChanger implements MouseButtons {

    //which mouseButtons to be available to
    boolean left, right;
    String name;
    int id;
    //for holding previous values to test for movement
    double lastInVals = 0;
    CESOneTypeUtility util;

    public UtilityNoiseChanger(Audible a, double val, String name, boolean left, boolean right) {
        super(a, val);

        setLeft(left);
        setRight(right);
        setName(name);

    }

    @Override
    public void heard(ShoutEvent s) {

        //assuming CES...
//        util = (CESOneTypeUtility) UtilityShell.u;

        gl.noiseUtil += s.heardValue;
        displayVal = gl.noiseUtil;

        
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
