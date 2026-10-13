package location2012.io;

import java.util.ArrayList;
import location2012.Person;
import location2012.PersonAction;
import location2012.econs.Good;

/**
 * Bucket: average good cost (including space cost) for all People
 * @author Olner Dan
 */
public class AllPeopleMeanGoodCostIncSpace extends Bucket {

    ArrayList<Person> people;
    PersonAction p;

    public AllPeopleMeanGoodCostIncSpace(String str, ArrayList entities, int samplelength) {
        super(str, entities, samplelength); //- not passing these up here,

        //override to add a column in for mean
        //vals = new double[samplelength][entities.size()+1];

        people = entities;

    }

    @Override
    public void grabData() {

        //Now... what do I want to do here? Hmm. Good question.
        //Let's start with: how many people are contributing to me?


        for (int i = 0; i < people.size(); i++) {

            p = (PersonAction) people.get(i).actions.get(0);

            if (p.bestBundle.GoodsList.size() > 0) {

                //add up the cost of all goods
                for (Good g : p.bestBundle.GoodsList) {
                    //price in Good is per unit, and includes any delivery/space cost
                    vals[samplePoint][i] += g.price;
                }

                //find average price
                vals[samplePoint][i] /= p.bestBundle.GoodsList.size();

            } else {

                vals[samplePoint][i] = 0;

            }

        }

        samplePoint++;

    }


}
