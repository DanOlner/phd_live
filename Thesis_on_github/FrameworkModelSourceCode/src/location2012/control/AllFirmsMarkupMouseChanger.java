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
public class AllFirmsMarkupMouseChanger extends VarChanger implements MouseButtons {

    //which mouseButtons to be available to
    boolean left, right;
    String name;
    int id;
    ArrayList<Actor> firms = new ArrayList<Actor>();
    //Firm to cast to
    Firm f;
    double lastInVals = 0;

    public AllFirmsMarkupMouseChanger(Audible a, double val, ArrayList<Actor> firms, String name, boolean left, boolean right) {
        super(a, val);

        //Keep some number

//        this.firms = firms;
//        this.firms.add(firms.get(0));

        this.firms = firms;

        setLeft(left);
        setRight(right);
        setName(name);

        displayVal = gl.markUp;

    }

    @Override
    public void heard(ShoutEvent s) {

        gl.markUp += s.heardValue;
//        gl.markUp = (gl.markUp < 0 ? 0 : gl.markUp);
        displayVal = gl.markUp;


        for (Actor a : firms) {
            f = (Firm) a;
            f.markUp = gl.markUp;
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
