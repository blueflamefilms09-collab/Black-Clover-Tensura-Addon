package com.newuniverse.nusmp.elite.races.runtime;

import com.newuniverse.nusmp.elite.races.EliteRacesConfig;
import com.newuniverse.nusmp.elite.races.data.RaceTemplate;
import com.newuniverse.nusmp.elite.races.state.TransformSpec;
import com.newuniverse.nusmp.elite.races.state.TransformState;

import java.util.EnumMap;
import java.util.Map;

/**
 * Why: the states of a template as THIS server runs them: the Great Ape removed when the config forbids it, and duration, rest and upkeep scaled by the server's dials. The
 * machine and the commands always read the states through here, so a dial changed in the config applies on the next tick with no restart.
 */
public final class RaceSpecs {
    private RaceSpecs() {}

    public static Map<TransformState, TransformSpec> effective(RaceTemplate t) {
        Map<TransformState, TransformSpec> out = new EnumMap<>(TransformState.class);
        if (t == null) return out;
        double dur = EliteRacesConfig.durationScale(), cd = EliteRacesConfig.cooldownScale(), drain = EliteRacesConfig.drainScale();
        for (Map.Entry<TransformState, TransformSpec> e : t.transformations().entrySet()) {
            if (e.getKey() == TransformState.GREAT_APE && !EliteRacesConfig.greatApe()) continue;
            out.put(e.getKey(), e.getValue().scaled(dur, cd, drain));
        }
        return out;
    }
}
