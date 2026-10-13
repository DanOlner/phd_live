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
public class AllFirmsPriceMouseChanger extends VarChanger implements MouseButtons {

    //which mouseButtons to be available to
    boolean left, right;
    String name;
    int id;
    ArrayList<Actor> firms;
    //Firm to cast to
    Firm f;
    double lastInVals = 0;

    public AllFirmsPriceMouseChanger(Audible a, double val, ArrayList<Actor> firms, String name, boolean left, boolean right) {
        super(a, val);

        this.firms = firms;

        setLeft(left);
        setRight(right);
        setName(name);

        f = (Firm) firms.get(0);

        displayVal = f.goodCost;

    }

    @Override
    public void heard(ShoutEvent s) {

//        if (lastInVals != inVals[1]) {

        gl.good = s.heardValue;
        gl.good = (gl.good < 0 ? 0 : gl.good);

        for (Actor a : firms) {
            f = (Firm) a;
            
            if (gl.goodsRange) {
                f.goodCost = gl.good * (f.ID - gl.people.size() + 1);
            } else {
                f.goodCost = gl.good;
            }
            
        }
        displayVal = f.goodCost;

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
