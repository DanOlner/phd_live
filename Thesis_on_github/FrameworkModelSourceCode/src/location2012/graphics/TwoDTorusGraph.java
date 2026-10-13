/*
 * For drawing Torus details in 2D.
 */
package location2012.graphics;

import location2012.Person;
import location2012.utils.gl;
import java.util.ArrayList;
import processing.core.PApplet;
import processing.core.PGraphics;

/**
 *
 * @author Olner Dan
 */
public class TwoDTorusGraph implements Drawable {

    ArrayList<Person> people;
    //for setting colours
    float xcol, ycol;
    //factor for multiplying size of circle
    int m = 30;
    //we need to know width of world
    int width;

    public TwoDTorusGraph(ArrayList<Person> people) {

        this.people = people;

        //get ref to torus from random actor. Schmooth! (er... hack!!)
        this.width = people.get(0).getSpace().width;

    }

    public void draw3D(PApplet pa) {

        pa.stroke(0, 20);
        pa.fill(0, 40, 40, 20);

        for (Person i : people) {

            pa.rect((float) (i.getPoint().x * (pa.width / width)),
                    (float) (i.getPoint().y * (pa.height / width)), (float) 5, (float) 5);

        }



    }

    public void draw2D(PApplet pa) {
    }

    public void draw2D(PGraphics p) {
    }

   
    
    
}
