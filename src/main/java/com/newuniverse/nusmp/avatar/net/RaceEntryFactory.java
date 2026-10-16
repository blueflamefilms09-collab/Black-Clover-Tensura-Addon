package com.newuniverse.nusmp.avatar.net;

import com.newuniverse.nusmp.avatar.appearance.DynamicAppearanceEngine;
import com.newuniverse.nusmp.avatar.race.AvatarRace;
import com.newuniverse.nusmp.avatar.race.AvatarRaces;
import com.newuniverse.nusmp.avatar.race.RaceConnectionChecker;
import net.minecraft.server.level.ServerPlayer;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Why: this is the single seam between AV1's race registry and the screen's race list: it reads a race and the player's connection check and writes the
 * plain {@link RaceEntry}. If the race record ever changes shape, only this file changes.
 */
public final class RaceEntryFactory {
    private RaceEntryFactory() {}

    public static List<RaceEntry> build(ServerPlayer player) {
        List<RaceEntry> out = new ArrayList<>();
        for (AvatarRace r : AvatarRaces.all()) {
            if (out.size() >= RaceEntry.MAX_RACES) break;
            RaceConnectionChecker.Result c = RaceConnectionChecker.check(player, r);
            List<String> mults = new ArrayList<>();
            if (r.statMultipliers() != null)
                for (Map.Entry<String, Double> e : r.statMultipliers().entrySet()) mults.add(e.getKey() + "=" + String.format(java.util.Locale.ROOT, "%.2f", e.getValue()));
            float sc = DynamicAppearanceEngine.scaleFor(r.defaultScale(), 0f, 99f);
            out.add(new RaceEntry(r.id(), r.displayName() == null ? r.id() : r.displayName(), sc, DynamicAppearanceEngine.minScale(sc), DynamicAppearanceEngine.maxScale(sc),
                    r.baseEp(), r.baseMagiculeCost(), r.evolvesToRaceId() == null ? "" : r.evolvesToRaceId(), r.requiresDndRoll(), c.ok(), c.failed() == null ? List.of() : c.failed(), mults));
        }
        return out;
    }
}
