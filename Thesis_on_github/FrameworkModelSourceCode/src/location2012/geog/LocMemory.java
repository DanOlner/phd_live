package location2012.geog;

import java.awt.geom.Point2D;
import java.util.ArrayList;
import location2012.Actor;

/**
 * HasLocations get LocMemories from the Torus when they ask e.g. who's in a certain radius
 * Location points are relative to the central location point the query is based on
 * So e.g. if I'm asking who's around me, I get back vectors relative to me.
 *
 * @author User
 */
public class LocMemory {

    public ArrayList<ActorNLocation> actors = new ArrayList<ActorNLocation>();
    //for data. Keeping here because easier to calculate once and store here
    public double meanDistanceOfPeopleInDensityCostRadius;

    public void addActor(Actor a, Point2D.Double p) {

        actors.add(new ActorNLocation(a, p));

    }

    /*
     * Return a reference to the actors list
     */
    public ArrayList<ActorNLocation> getActorNLocationArray() {

        return actors;

    }

    /*
     *  Inner class for storing pairs of actors and coordinates
     */
    public class ActorNLocation {

        public Actor actor;
        public Point2D.Double p;

        public ActorNLocation(Actor a, Point2D.Double p) {

            this.actor = a;
            this.p = p;

        }
    }
}
