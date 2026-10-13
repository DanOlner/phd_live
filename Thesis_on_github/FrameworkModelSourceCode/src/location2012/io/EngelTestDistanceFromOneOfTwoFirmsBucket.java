/*
 * Specifically for a record of distance for actors from one of two firms
 */
package location2012.io;

import location2012.Person;
import java.util.ArrayList;
import location2012.Actor;
import location2012.Firm;
import location2012.econs.GoodSeller;
import location2012.utils.gl;

/**
 *
 * @author Olner Dan
 */
public class EngelTestDistanceFromOneOfTwoFirmsBucket extends Bucket {

    ArrayList<Person> actors;
//    PersonAction pa;
    Actor a;
    Firm f;
    GoodSeller g;
    double luxmean, luxvariance, luxsd, necmean, necvariance, necsd;
    double luxdisttot, necdisttot;

    public EngelTestDistanceFromOneOfTwoFirmsBucket(String str, ArrayList entities, int samplelength) {
        super(str, entities, samplelength); //- not passing these up here,

        //override to add a column in for mean
        //two for mean, sd of distance from each, one for whatever var we happen to be changing
        vals = new double[samplelength][5];

        actors = entities;

        colnames = new String[5];

        colnames[0] = "val";
        colnames[1] = "nec_mean_d";
        colnames[2] = "lux_mean_d";
        colnames[3] = "nec_sd_d";
        colnames[4] = "lux_sd_d";


    }

    @Override
    public void grabData() {


        //reset vars
        luxdisttot = 0;
        necdisttot = 0;
        luxvariance = 0;
        necvariance = 0;

        f = (Firm) gl.firms.get(0);
//        vals[samplePoint][0] = f.wageoffer;
        vals[samplePoint][0] = f.deliverycost;

        for
         (int i = 0; i < actors.size(); i++) {

            a = actors.get(i);

//            f = (Firm) gl.firms.get(0);
            g = (GoodSeller) gl.firms.get(0);

            //get distance totals for both firms
//            System.out.println("type for firm 0: " + g.getGoodType());

            necdisttot += gl.firms.get(1).getPoint().distance(a.getPoint());
            luxdisttot += gl.firms.get(0).getPoint().distance(a.getPoint());


        }

        //add mean nearest neigbour value in last slot
        necmean = necdisttot / gl.people.size();
        luxmean = luxdisttot / gl.people.size();

        vals[samplePoint][1] = necmean;
        vals[samplePoint][2] = luxmean;


        //now we have means, work out variance
        for (int i = 0; i < actors.size(); i++) {

            a = actors.get(i);

            necvariance += Math.pow((necmean - gl.firms.get(1).getPoint().distance(a.getPoint())), 2);
            luxvariance += Math.pow((luxmean - gl.firms.get(0).getPoint().distance(a.getPoint())), 2);


        }


        //find final variance by averaging total squared differences
        necvariance /= gl.people.size();
        luxvariance /= gl.people.size();


        //find SD
        necsd = Math.sqrt(necvariance);
        luxsd = Math.sqrt(luxvariance);

        //add to data
        vals[samplePoint][3] = necsd;
        vals[samplePoint][4] = luxsd;

        samplePoint++;

    }
}
