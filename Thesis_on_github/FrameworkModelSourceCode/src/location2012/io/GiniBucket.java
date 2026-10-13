/*
 * Output gini coefficient
 */
package location2012.io;

import location2012.Person;
import location2012.utils.p;
import java.util.ArrayList;

/**
 *
 * @author Olner Dan
 */
public class GiniBucket extends Bucket {

    ArrayList<Person> actors;
    Person a;
    //for bubblesort
    double[] productivities;
    double ca, cb;
    //for bits of the gini calc, breaking it down
    double top, bottom, g;

    public GiniBucket(String str, ArrayList entities, int samplelength) {
        super(str, entities, samplelength);

        //Overwrite: just need one for gini
        vals = new double[samplelength][1];

        actors = entities;

        productivities = new double[actors.size()];

    }

    @Override
    public void grabData() {

        top = 0;
        bottom = 0;
        ca = 0; cb = 0;

        //sort everyone in ascending order of productivity - which is currently wealth
        sort();


        for (int i = 0; i < actors.size(); i++) {

            //test sort
            //p.p("Test sort:" + productivities[i]);

            top += (i * productivities[i]);
            bottom += productivities[i];



        }

        //top is multiplied by 2, the bottom by n
        top *= 2;
        bottom *= actors.size();

        //the final answer for G
        g = (top / bottom) - ((actors.size() + 1) / actors.size());

        vals[samplePoint][0] = g;

        samplePoint++;

    }


    /*
     * Private method for sorting actor array into a copy array in ascending
     * order of productivity
     * adapted from http://www.daniweb.com/code/snippet18.html
     */
    private void sort() {

        //dump all actor productivity values into productivities
        for (int i = 0; i < actors.size(); i++) {
            //productivities[i] = actors.get(i).productivity;
        }


        for (int a = 0; a < actors.size(); a++) {

            for (int b = 0; b < actors.size() - 1; b++) {

                ca = productivities[b];
                cb = productivities[b+1];

                if (ca > cb) {

                    //temp = array[b];
                    //array[b] = array[b + 1];
                    //array[b + 1] = temp;
                    productivities[b] = cb;

                    productivities[b + 1] = ca;

                }//end if
            }//end for b
        }//end for a

    }
}
