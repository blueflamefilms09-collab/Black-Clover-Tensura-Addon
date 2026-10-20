package com.newuniverse.nusmp.labyrinth.mega.enc;

import com.newuniverse.nusmp.labyrinth.squad.SquadHook;
import com.newuniverse.nusmp.labyrinth.squad.SquadPlan;
import net.minecraft.server.level.ServerLevel;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

/**
 * Why: the Dungeon Master decides WHICH monsters an encounter has and WHERE they stand; a squad decides how MANY (5 to 8, by band, depth and party), and what each does. This
 * is the one place where the two meet, so the Director gains three calls and no more: {@link #shape} turns the planned groups into a squad-sized headcount (a plan of three
 * goblins becomes a squad of six, a plan of twelve becomes eight) and begins the squad; the spawner stamps each monster; the Director then hands the placed monsters to
 * {@code SquadHook.end}. Where a group has to GROW, the new members are drawn from the faction's own pool by the same toughness window the encounter used ({@code MobPicker}), not
 * cloned, so the squad has a heavier body for its tank and a lighter one for its healer; without a faction pool the first kinds are repeated. An entry that holds its place
 * ({@code hunt: 0}, a turret) is not a squad, and with {@code squads.enabled} false nothing here changes the plan: the old path runs exactly as it did.
 */
final class SquadBridge {
    private SquadBridge() {}

    static List<EncounterRoll.Planned> shape(ServerLevel level, List<EncounterRoll.Planned> plan, EncounterTable.Entry entry, Band band, EncounterDirector.View v, String theme,
                                             String nativeFaction, EncounterTable table, Map<String, String> factionOf, Map<String, Double> costOf, Random rnd) {
        if (!SquadHook.on() || entry.hunt() == 0 || plan.isEmpty() || band == Band.NOTHING) return plan;
        int depth = v.floor() != 0 ? Math.abs(v.floor()) : v.threat();
        String faction = factionFor(plan, factionOf, entry, nativeFaction, table);
        SquadPlan.Plan sp = SquadHook.begin(level, rnd.nextLong(), depth, v.size(), Math.max(0, band.ordinal() - 1), faction, theme);
        int[] counts = new int[plan.size()];
        int total = 0;
        for (int i = 0; i < counts.length; i++) {
            counts[i] = plan.get(i).count();
            total += counts[i];
        }
        if (total == sp.size()) return plan;
        int[] fitted = SquadPlan.fit(counts, sp.size());
        Map<String, Integer> out = new LinkedHashMap<>();
        List<MobPicker.Candidate> pool = faction == null ? List.of() : Bestiary.members(faction);
        int fresh = 0;
        for (int i = 0; i < fitted.length; i++) {
            String id = plan.get(i).entity();
            int keep = Math.min(fitted[i], counts[i]);
            if (keep > 0) out.merge(id, keep, Integer::sum);
            for (int k = counts[i]; k < fitted[i]; k++) {
                MobPicker.Candidate c = pool.isEmpty() || fresh >= 6 ? null : MobPicker.pick(pool, v.threat(), rnd);
                if (c == null) {
                    out.merge(id, 1, Integer::sum);
                    continue;
                }
                fresh++;
                out.merge(c.id(), 1, Integer::sum);
                factionOf.putIfAbsent(c.id(), faction);
                costOf.putIfAbsent(c.id(), EncounterRoll.cost(c.toughness()));
            }
        }
        List<EncounterRoll.Planned> shaped = new ArrayList<>(out.size());
        for (Map.Entry<String, Integer> e : out.entrySet()) shaped.add(new EncounterRoll.Planned(e.getKey(), e.getValue()));
        return shaped;
    }

    /** The faction that tilts the squad's mix: the faction of the first planned monster, else the entry's, else the room's native one, else the table's default. */
    private static String factionFor(List<EncounterRoll.Planned> plan, Map<String, String> factionOf, EncounterTable.Entry entry, String nativeFaction, EncounterTable table) {
        for (EncounterRoll.Planned p : plan) {
            String f = factionOf.get(p.entity());
            if (f != null && !f.isEmpty()) return f;
        }
        if (!entry.faction().isEmpty()) return entry.faction();
        if (nativeFaction != null) return nativeFaction;
        return table.defaultFaction();
    }
}
