/*
 * Testing Output for datastore in Backyard1
 */
package location2012.io;

import location2012.Person;
import java.util.ArrayList;
import location2012.Actor;

/**
 *
 * @author Olner Dan
 */
public class ActorProductivityBucket extends Bucket {

    ArrayList<Actor> actors;
    
    Actor a;

    double prod;

    public ActorProductivityBucket(String str, ArrayList entities, int samplelength) {
        super(str, entities, samplelength);

        actors = entities;

    }

    @Override
    public void grabData() {

        for(int i = 0; i < actors.size(); i++) {

            a = actors.get(i);

//            prod = (Productivity.giveOutput(a.myContribTime.to.getContributedTime())
//                    * (a.myContribTime.getTime() / a.myContribTime.to.getContributedTime()));
            //prod = a.productivity;

            vals[samplePoint][i] = prod;

        }

        samplePoint++;

    }





}
