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
public class AllFirmsWageMouseChanger extends VarChanger implements MouseButtons {

    //which mouseButtons to be available to
    boolean left, right;
    String name;
    int id;
    ArrayList<Actor> firms;
    //Firm to cast to
    Firm f;

    public AllFirmsWageMouseChanger(Audible a, double val, ArrayList<Actor> firms, String name, boolean left, boolean right) {
        super(a, val);

        this.firms = firms;

        setLeft(left);
        setRight(right);
        setName(name);

    }

    @Override
    public void heard(ShoutEvent s) {

//        for (Actor a : firms) {
//            f = (Firm) a;
//            f.wage = inVals[1] * 10;
//            f.wage = (f.wage < 0 ? 0 : f.wage);
//        }
//        displayVal = f.wage;

        gl.wage += s.heardValue;
//            gl.wage += ((Math.pow(10, (s.heardValue) * 3))/2)-1;
        gl.wage = (gl.wage < 0 ? 0 : gl.wage);
        displayVal = gl.wage;


        for (Actor a : firms) {
            f = (Firm) a;

            if (gl.wageRange) {
                f.wage = gl.wage * (f.ID - gl.people.size() + 1);
            } else {
                f.wage = gl.wage;
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
