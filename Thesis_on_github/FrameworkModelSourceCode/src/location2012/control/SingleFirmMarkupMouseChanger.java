/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package location2012.control;

import java.util.ArrayList;
import location2012.Actor;
import location2012.Firm;
import location2012.SingleActorPerson;
import location2012.observe.Audible;
import location2012.observe.ShoutEvent;
import location2012.utils.gl;

/**
 *
 * @author Dan
 */
public class SingleFirmMarkupMouseChanger extends VarChanger implements MouseButtons {

    //which mouseButtons to be available to
    boolean left, right;
    String name;
    int id;
//    ArrayList<Actor> firms = new ArrayList<Actor>();
    //Firm to cast to
    Firm f;
    int numPeople;

    public SingleFirmMarkupMouseChanger(Audible a, double val, String name, boolean left, boolean right) {
        super(a, val);

        numPeople = gl.people.size();

        setLeft(left);
        setRight(right);
        setName(name);
        
        f = (Firm) gl.firms.get(gl.LOOKATFIRM);
        displayVal = f.markUp;

    }

    @Override
    public void heard(ShoutEvent s) {

        f = (Firm) gl.firms.get(gl.LOOKATFIRM);
        f.markUp += s.heardValue;
        displayVal = f.markUp;

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
