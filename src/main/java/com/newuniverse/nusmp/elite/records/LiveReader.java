package com.newuniverse.nusmp.elite.records;

import com.newuniverse.nusmp.elite.api.EliteHooks;
import com.newuniverse.nusmp.elite.records.rule.Live;
import com.newuniverse.nusmp.multiverse.RaceBuffs;
import io.github.manasmods.manascore.skill.api.ManasSkillInstance;
import io.github.manasmods.manascore.skill.api.SkillAPI;
import io.github.manasmods.tensura.ability.skill.Skill;
import io.github.manasmods.tensura.storage.TensuraStorages;
import io.github.manasmods.tensura.storage.ep.IExistence;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.stats.Stats;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;

/**
 * Why: the one place that reads the live game for the records system and turns it into the plain {@link Live} the pure engine understands. Every source is read in its own try block and a source that
 * fails simply stays "not answered" (Tensura's storage missing gives {@code tensuraOk = false}, so the EP and status records are UNAVAILABLE instead of a false zero; no FTB Chunks gives a null claim
 * count). Tensura EP and the Demon Lord / Hero statuses come straight from Tensura's own existence storage ({@code isTrueDemonLord}, {@code isDemonLordSeed}, {@code isTrueHero}, {@code isHeroEgg}); this
 * mod does not keep a second copy. "Higher being" is this mod's own reading of Tensura: 1 for a spiritual form, 2 for a divine or god race (race path starting with {@code divine} or {@code god}).
 * Distances and deaths are vanilla statistics (centimetres, converted to metres). Server thread only; it reads, it never writes.
 */
final class LiveReader {
    private LiveReader() {}

    static Live read(ServerPlayer p) {
        Live l = new Live();
        readTensura(p, l);
        l.ftbChunks = FtbChunksBridge.claimed(p);
        try {
            l.deaths = p.getStats().getValue(Stats.CUSTOM.get(Stats.DEATHS));
        } catch (RuntimeException ignored) { /* stats not loaded yet */ }
        readDistances(p, l);
        try {
            l.hookForgeCount = Math.max(0, EliteHooks.forgeCraftedCount.applyAsInt(p.getUUID()));
            l.hookForgeBest = Math.max(0, EliteHooks.forgeBestQuality.applyAsInt(p.getUUID()));
        } catch (RuntimeException ignored) { /* a hook owner threw: treat as nothing */ }
        readSkills(p, l);
        return l;
    }

    private static void readTensura(ServerPlayer p, Live l) {
        try {
            IExistence ex = TensuraStorages.getExistenceFrom(p);
            if (ex == null) return;
            l.tensuraOk = true;
            l.ep = ex.getEP();
            l.demon = ex.isTrueDemonLord() ? 2 : ex.isDemonLordSeed() ? 1 : 0;
            l.hero = ex.isTrueHero() ? 2 : ex.isHeroEgg() ? 1 : 0;
            int higher = ex.isSpiritualForm() ? 1 : 0;
            var race = RaceBuffs.tensuraRace(p);
            if (race.isPresent()) {
                String path = race.get().getRaceId().getPath();
                if (path.startsWith("divine") || path.startsWith("god")) higher = 2;
            }
            l.higher = higher;
        } catch (RuntimeException | LinkageError e) {
            l.tensuraOk = false;
        }
    }

    private static double m(ServerPlayer p, ResourceLocation stat) {
        return p.getStats().getValue(Stats.CUSTOM.get(stat)) / 100.0;
    }

    private static void readDistances(ServerPlayer p, Live l) {
        try {
            double walk = m(p, Stats.WALK_ONE_CM) + m(p, Stats.CROUCH_ONE_CM), sprint = m(p, Stats.SPRINT_ONE_CM);
            double swim = m(p, Stats.SWIM_ONE_CM) + m(p, Stats.WALK_UNDER_WATER_ONE_CM) + m(p, Stats.WALK_ON_WATER_ONE_CM);
            double fly = m(p, Stats.FLY_ONE_CM), elytra = m(p, Stats.AVIATE_ONE_CM), boat = m(p, Stats.BOAT_ONE_CM);
            double mount = m(p, Stats.HORSE_ONE_CM) + m(p, Stats.PIG_ONE_CM) + m(p, Stats.STRIDER_ONE_CM);
            double other = m(p, Stats.MINECART_ONE_CM) + m(p, Stats.CLIMB_ONE_CM);
            l.distance.put("walk", walk);
            l.distance.put("sprint", sprint);
            l.distance.put("swim", swim);
            l.distance.put("fly", fly);
            l.distance.put("elytra", elytra);
            l.distance.put("boat", boat);
            l.distance.put("mount", mount);
            l.distance.put("all", walk + sprint + swim + fly + elytra + boat + mount + other);
        } catch (RuntimeException ignored) { /* stats not loaded yet */ }
    }

    private static void readSkills(ServerPlayer p, Live l) {
        try {
            var skills = SkillAPI.getSkillsFrom(p);
            if (skills == null) return;
            Set<String> all = new HashSet<>();
            for (ManasSkillInstance i : new ArrayList<>(skills.getLearnedSkills())) {
                String id = i.getSkill().getRegistryName().toString();
                all.add(id);
                try {
                    if (i.getSkill() instanceof Skill s) l.skillsByType.computeIfAbsent(s.getType().getSerializedName(), k -> new HashSet<>()).add(id);
                } catch (RuntimeException | LinkageError ignored) { /* a skill of another mod: counted without a type */ }
            }
            l.skills = all;
        } catch (RuntimeException | LinkageError e) {
            l.skills = null;
        }
    }
}
