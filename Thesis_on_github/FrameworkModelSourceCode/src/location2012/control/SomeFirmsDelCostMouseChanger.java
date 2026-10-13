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

/**
 *
 * @author Dan
 */
public class SomeFirmsDelCostMouseChanger extends VarChanger implements MouseButtons {

    //which mouseButtons to be available to
    boolean left, right;
    String name;
    int id;
    ArrayList<Actor> firms = new ArrayList<Actor>();
    //Firm to cast to
    Firm f;

    public SomeFirmsDelCostMouseChanger(Audible a, double val, ArrayList<Actor> firms, String name, boolean left, boolean right) {
        super(a, val);

        //Keep some number

        for (int i = 0; i < firms.size() / 2; i++) {

            this.firms.add(firms.get(i));

        }
//        this.firms.add(firms.get(0));

        System.out.println("Number of firms in SomeFirmsDelCostMouseChanger: " + this.firms.size());

//        this.firms = firms;

        setLeft(left);
        setRight(right);
        setName(name);

    }

    @Override
    public void heard(ShoutEvent s) {


        for (Actor a : firms) {
            f = (Firm) a;
            f.deliverycost = (Math.pow(10, (inVals[1]) * 3)) - 1;
            f.deliverycost = (f.deliverycost < 0 ? 0 : f.deliverycost);
            displayVal = f.deliverycost;

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
