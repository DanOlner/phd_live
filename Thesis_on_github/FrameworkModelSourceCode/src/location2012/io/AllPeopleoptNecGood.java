/*
 * Bucket for playing about with networks connections - who's collaborating with who?
 */
package location2012.io;

import java.util.ArrayList;
import location2012.Person;
import location2012.PersonAction;
import location2012.utils.gl;

/**
 *
 * @author Olner Dan
 */
public class AllPeopleoptNecGood extends Bucket {

    ArrayList<Person> people;
    PersonAction p;

    public AllPeopleoptNecGood(String str, ArrayList entities, int samplelength) {
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

//            System.out.println("ping!");

            p = (PersonAction) people.get(i).actions.get(0);

            if (p.bestBundle.GoodsList.size() > 0) {

//                System.out.println("goods!");

                if (p.bestBundle.GoodsList.get(0).gs.getGoodType() == gl.GoodType.Necessity) {

                    vals[samplePoint][i] = p.bestBundle.GoodsList.get(0).optimalChosenAmount;

                } else {

                    vals[samplePoint][i] = p.bestBundle.GoodsList.get(1).optimalChosenAmount;

                }

            } else {

                vals[samplePoint][i] = 0;

            }

        }

//        //add mean nearest neigbour value in last slot
//
        samplePoint++;

    }
}
