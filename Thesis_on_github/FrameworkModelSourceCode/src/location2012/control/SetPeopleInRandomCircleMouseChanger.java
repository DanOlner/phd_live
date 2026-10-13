/*
 * For shifting values in the SpaceTools class
 */
package location2012.control;

import java.util.ArrayList;
import location2012.Actor;
import location2012.geog.SpaceTools;
import location2012.observe.Audible;
import location2012.observe.ShoutEvent;

/**
 *
 * @author Olner Dan
 */
public class SetPeopleInRandomCircleMouseChanger extends VarChanger implements MouseButtons {

    //which mouseButtons to be available to
    boolean left, right;
    String name;
    int id;
    ArrayList<Actor> actors;
    double lastV;

    public SetPeopleInRandomCircleMouseChanger(Audible time, double val, ArrayList<Actor> actors, String name, boolean left, boolean right) {

        super(time, val);
        this.name = name;
        this.left = left;
        this.right = right;

        this.actors = actors;

    }

    public int getWeight() {

        return 0;

    }

    public void giveShouterTo(Audible a, double val) {
    }

    public void heard(ShoutEvent sh) {

        System.out.println("in actor changer thing. Val: " + inVals[1]);

        if (lastV != inVals[1]) {

            lastV = inVals[1];

            //Max is always as wide as world
            SpaceTools.putActorsInsideCircleAreaRandomly(actors, inVals[1] * (SpaceTools.width / 2));

        }

    }

    public void setWeight(int weight) {
    }

    public int getID() {
        return id;
    }

    public String getName() {
        return name;
    }

    public void setID(int id) {
        this.id = id;
    }

    public void setLeft(boolean l) {
        this.left = left;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setRight(boolean r) {
        this.right = right;
    }

    public boolean useLeft() {
        return left;
    }

    public boolean useRight() {
        return right;
    }
}
