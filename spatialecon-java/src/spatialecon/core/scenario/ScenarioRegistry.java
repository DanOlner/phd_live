package spatialecon.core.scenario;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

import spatialecon.core.scenario.models.A1_CesExplorer;
import spatialecon.core.scenario.models.A2_CobbDouglasIndependence;
import spatialecon.core.scenario.models.A3_ProductionFunctions;
import spatialecon.core.scenario.models.A4_OptimiserComparison;
import spatialecon.core.scenario.models.B1_LoVMeetsDistance;
import spatialecon.core.scenario.models.B2_DoubleDip;
import spatialecon.core.scenario.models.B3_BlackHole;
import spatialecon.core.scenario.models.C1_MonocentricEquilibrium;
import spatialecon.core.scenario.models.C2_DensityInternals;
import spatialecon.core.scenario.models.C6_VonThunen;
import spatialecon.core.scenario.models.C8_SpatialNotUtilityEquilibrium;
import spatialecon.core.scenario.models.D1_TwoFirmsOnALine;
import spatialecon.core.scenario.models.D2_DmrGame;
import spatialecon.core.scenario.models.E1_Damping;
import spatialecon.core.scenario.models.E3_RegimeTransition;
import spatialecon.core.scenario.models.E5_TwoRegions;
import spatialecon.core.scenario.models.E7_LineOfAgents;
import spatialecon.core.scenario.models.E8_BeltPlusDensity;
import spatialecon.core.scenario.models.Smoke;

/**
 * Every available model, by id.
 *
 * <p>One place to look to see what exists — the thing the original most conspicuously
 * lacked, where the answer was spread across {@code listOfModels.txt}, a folder of jars and
 * the commented-out state of three source files.
 *
 * <p>Suppliers rather than instances, so each run gets a fresh scenario object and stale
 * state cannot leak between runs.
 */
public final class ScenarioRegistry {

    private static final Map<String, Supplier<Scenario>> REGISTRY = new LinkedHashMap<>();

    static {
        register(Smoke::new);

        // Group A — foundations. Pure function explorers; each day is a sweep step.
        register(A1_CesExplorer::new);
        register(A2_CobbDouglasIndependence::new);
        register(A3_ProductionFunctions::new);
        register(A4_OptimiserComparison::new);

        // Group B — space enters. Single mechanisms, still mostly analytic.
        register(B1_LoVMeetsDistance::new);
        register(B2_DoubleDip::new);
        register(B2_DoubleDip::withFirmAtZero);
        register(B3_BlackHole::new);

        // Group C — mobile People and the density cost. The visual centrepiece.
        register(C1_MonocentricEquilibrium::new);
        register(C2_DensityInternals::new);
        register(C6_VonThunen::new);
        register(C6_VonThunen::varyingCommute);
        register(C8_SpatialNotUtilityEquilibrium::new);

        // Group D — mobile Firms, and why a Firm misreads its own revenue.
        register(D1_TwoFirmsOnALine::highVarietyNearMarket);
        register(D1_TwoFirmsOnALine::substitutesNearMarket);
        register(D1_TwoFirmsOnALine::highVarietyFarMarket);
        register(D1_TwoFirmsOnALine::substitutesFarMarket);
        register(D2_DmrGame::clean);
        register(D2_DmrGame::someNoise);
        register(D2_DmrGame::muchNoise);

        // Group E — the transmission belt: supply and demand actually linked.
        register(E1_Damping::damped);
        register(E1_Damping::undamped);
        register(E1_Damping::manyAgents);
        register(E3_RegimeTransition::new);
        register(E5_TwoRegions::oneFirmEach);
        register(E5_TwoRegions::twoFirmsEach);
        register(E7_LineOfAgents::new);
        register(E8_BeltPlusDensity::new);
        // Stage 5: C3, C5, C7, E2, E4 (sweeps and markup)
        // Stage 6: C4, F1-F4 (demonstrations)
    }

    private ScenarioRegistry() { }

    private static void register(Supplier<Scenario> supplier) {
        String id = supplier.get().id();
        if (REGISTRY.putIfAbsent(id, supplier) != null) {
            throw new IllegalStateException("duplicate scenario id: " + id);
        }
    }

    /** A fresh instance of the scenario with this id. */
    public static Scenario get(String id) {
        Supplier<Scenario> s = REGISTRY.get(id.toLowerCase());
        if (s == null) {
            throw new IllegalArgumentException(
                    "unknown scenario '" + id + "'. Available: " + String.join(", ", ids()));
        }
        return s.get();
    }

    public static boolean has(String id) { return REGISTRY.containsKey(id.toLowerCase()); }

    public static List<String> ids() { return new ArrayList<>(REGISTRY.keySet()); }

    /** Fresh instances of everything registered, for listing. */
    public static List<Scenario> all() {
        List<Scenario> out = new ArrayList<>();
        for (Supplier<Scenario> s : REGISTRY.values()) out.add(s.get());
        return out;
    }
}
