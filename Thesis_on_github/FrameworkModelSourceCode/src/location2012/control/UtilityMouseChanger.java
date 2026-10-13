    /*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package location2012.control;

import location2012.econs.CESOneTypeUtility;
import location2012.econs.UtilityShell;
import location2012.observe.Audible;
import location2012.observe.ShoutEvent;
import location2012.utils.gl;

/**
 *
 * @author Dan
 */
public class UtilityMouseChanger extends VarChanger implements MouseButtons {

    //which mouseButtons to be available to
    boolean left, right;
    String name;
    int id;
    //for holding previous values to test for movement
    double lastInVals = 0;
    CESOneTypeUtility util;

    public UtilityMouseChanger(Audible a, double val, String name, boolean left, boolean right) {
        super(a, val);

        setLeft(left);
        setRight(right);
        setName(name);

    }

    @Override
    public void heard(ShoutEvent s) {

//        if (lastInVals != inVals[1]) {
        
//        System.out.println("made it!");

        //assuming CES...
        util = (CESOneTypeUtility) UtilityShell.u;

//        util.rho += s.heardValue;
        gl.rho += s.heardValue;
        
        gl.rho = (gl.rho < 0.00001 ? 0.00001 : gl.rho);
        gl.rho = (gl.rho > 0.99 ? 0.99 : gl.rho);
        
        util.rho = gl.rho;
        
//        util.rho += inVals[1];
        //hacking! VariableStore currently looks at gl to find value
//        gl.rho += inVals[1];
        displayVal = util.rho;

//        }

//        lastInVals = inVals[1];

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
