/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package location2012.control;

import java.util.ArrayList;
import location2012.Actor;
import location2012.SingleActorPerson;
import location2012.observe.Audible;
import location2012.observe.ShoutEvent;
import location2012.utils.gl;

/**
 *
 * @author Dan
 */
public class SingleActorsDelCostMouseChanger extends VarChanger implements MouseButtons {

    //which mouseButtons to be available to
    boolean left, right;
    String name;
    int id;
    ArrayList<Actor> actors = new ArrayList<Actor>();
    //Firm to cast to
    SingleActorPerson sap;
    double lastInVals = 0;

    public SingleActorsDelCostMouseChanger(Audible a, double val, ArrayList<Actor> actors, String name, boolean left, boolean right) {
        super(a, val);

        //Keep some number

        this.actors = actors;
//        this.firms.add(firms.get(0));

        this.actors = actors;

        setLeft(left);
        setRight(right);
        setName(name);

        displayVal = gl.deliveryCost;

    }

    @Override
    public void heard(ShoutEvent s) {


//            System.out.println("lastInVals: " + lastInVals + ", inVals: " + inVals[1]);

            gl.deliveryCost += s.heardValue;
            gl.deliveryCost = (gl.deliveryCost < 0 ? 0 : gl.deliveryCost);
            displayVal = gl.deliveryCost;

            for (Actor a : actors) {
                sap = (SingleActorPerson) a;
                sap.deliverycost = gl.deliveryCost;
//            sap.deliverycost = (Math.pow(10, (inVals[1]) * 3)) - 1;
//            sap.deliverycost = (sap.deliverycost < 0 ? 0 : sap.deliverycost);

            }
//        }

//        lastInVals = inVals[1];

    }//end method heard

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
