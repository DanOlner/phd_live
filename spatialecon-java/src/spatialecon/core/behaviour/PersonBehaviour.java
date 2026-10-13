package spatialecon.core.behaviour;

import spatialecon.core.engine.Person;
import spatialecon.core.engine.World;

/** What a Person does when their turn comes. */
public interface PersonBehaviour {

    void act(Person person, World world);

    /** Short description for logs and figure captions. */
    String describe();

    /** Does nothing. Lets a scenario hold People fixed as pure sources of demand. */
    PersonBehaviour NONE = new PersonBehaviour() {
        @Override public void act(Person person, World world) { }
        @Override public String describe() { return "inactive"; }
    };
}
