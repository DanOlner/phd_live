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
public class FirstFirmPositionMouseChanger extends VarChanger implements MouseButtons {

    //which mouseButtons to be available to
    boolean left, right;
    String name;
    int id;
    ArrayList<Actor> firms = new ArrayList<Actor>();
    //Firm to cast to
    Firm f;

    public FirstFirmPositionMouseChanger(Audible a, double val, ArrayList<Actor> firms, String name, boolean left, boolean right) {
        super(a, val);

        //Keep some number

        this.firms = firms;
//        this.firms.add(firms.get(0));

        this.firms = firms;

        setLeft(left);
        setRight(right);
        setName(name);

    }

    @Override
    public void heard(ShoutEvent s) {


        
            f = (Firm) firms.get(0);
            f.setx((1-inVals[0])*gl.width);
//            f.deliverycost = (Math.pow(10, (inVals[1]) * 3)) - 1;
//            f.deliverycost = (f.deliverycost < 0 ? 0 : f.deliverycost);
            displayVal = f.getx();

        
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
