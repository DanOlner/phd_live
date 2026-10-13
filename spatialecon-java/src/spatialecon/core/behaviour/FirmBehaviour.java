package spatialecon.core.behaviour;

import spatialecon.core.engine.Firm;
import spatialecon.core.engine.World;

/**
 * What a Firm does when its turn comes.
 *
 * <p>Note that the transmission belt's stock targeting is <em>not</em> expressed here. It
 * fires on every purchase rather than once a turn — a reflex, not a scheduled action — so
 * it hangs off the sale itself. This interface covers only turn-scheduled behaviour, which
 * in practice means the location crawl of §5.5.
 */
public interface FirmBehaviour {

    /** Scheduled action, once per turn. */
    void act(Firm firm, World world);

    /**
     * Reflex: called the moment a Person buys, out of turn.
     *
     * <p>The third decision type of §4.5.2, and the one the transmission belt runs on. A Firm
     * adjusting price here responds to every individual sale rather than waiting for its own
     * turn — with 300 People that is 300 adjustments a day, not one. The speed is the point:
     * the thesis contrasts it with the sluggish, error-prone feedback of {@link #act}, and
     * calls it closer to the cybernetic ideal of intrinsic control.
     *
     * @param units quantity of good sold
     * @param input labour the Firm received for it, which goes straight into production
     */
    default void onSale(Firm firm, double units, double input, World world) { }

    /** Short description for logs and figure captions. */
    String describe();

    /** Does nothing. Firms are then fixed points of supply, as in most partial models. */
    FirmBehaviour NONE = new FirmBehaviour() {
        @Override public void act(Firm firm, World world) { }
        @Override public String describe() { return "inactive"; }
    };
}
