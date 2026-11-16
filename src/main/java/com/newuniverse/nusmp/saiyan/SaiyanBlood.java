package com.newuniverse.nusmp.saiyan;

import com.mojang.logging.LogUtils;
import io.github.manasmods.manascore.race.api.ManasRace;
import io.github.manasmods.manascore.race.api.ManasRaceInstance;
import io.github.manasmods.manascore.race.api.RaceAPI;
import io.github.manasmods.tensura.race.template.EvolutionRequirement;
import io.github.manasmods.tensura.storage.TensuraStorages;
import io.github.manasmods.tensura.storage.ep.IExistence;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import org.slf4j.Logger;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Why: the Saiyan-blooded evolution (docs/saiyan_prodigy.md): in Tensura's evolution screen a Human can evolve into a Saiyan once three deeds are done: existence points, worthy hostile creatures beaten, and
 * surviving at the edge of death. The numbers are {@link SaiyanBloodRules}, the switches {@link SaiyanProdigyConfig} ({@code blood.bloodEvolution} = false restores the old way: the Saiyan is only made by command or
 * the race menu). Two small pieces:
 * <ul>
 * <li>{@link #addEvolution} (called from {@code HumanRaceEvolutionMixin}) adds the Saiyan race to the Human's list of next evolutions; Tensura's own packet then checks the progress on the server.</li>
 * <li>the two deed counters live in the Human race instance's tag (saved and synced with the race, so the evolution screen shows them): a kill by the Human and a blow survived at the edge.</li>
 * </ul>
 * The evolution itself is Tensura's: it sets the race, ManasCore fires {@code SET_RACE}, and {@link SaiyanRaceLink} turns that into the Saiyan flag (and {@link SaiyanProdigyHub#onSaiyan} gives the Prodigy).
 * Cost: a death or a damage event does two cheap checks (is the actor a player, is the switch on) before it looks at the race; a counter write per deed (capped at 1000).
 */
public final class SaiyanBlood {
    private SaiyanBlood() {}

    private static final Logger LOG = LogUtils.getLogger();
    private static final ResourceLocation HUMAN = ResourceLocation.fromNamespaceAndPath("tensura", "human");

    public static void register(IEventBus bus) {
        bus.addListener(SaiyanBlood::onDeath);
        bus.addListener(SaiyanBlood::onDamaged);
    }

    // ---------------------------------------------------------------------------------------------------- the evolution list
    /** Adds the Saiyan to a Human's next evolutions (idempotent; a no-op while {@code bloodEvolution} is off). The list is the one Tensura just built, so it is changed in place. */
    public static void addEvolution(List<ManasRace> list) {
        if (list == null || !SaiyanProdigyConfig.bloodEvolution()) return;
        try {
            ManasRace saiyan = SaiyanRaces.SAIYAN.get();
            if (!list.contains(saiyan)) list.add(saiyan);
        } catch (RuntimeException | LinkageError e) {
            LOG.warn("Saiyan blood: could not add the Saiyan to the Human's evolutions ({})", e.toString());
        }
    }

    /** The requirements of the evolution with their weights (they add up to 100): what {@code SaiyanTensuraRace.getEvolutionRequirements} answers. */
    public static Map<EvolutionRequirement, Float> requirements() {
        double ep = SaiyanProdigyConfig.bloodEp();
        int kills = SaiyanProdigyConfig.bloodKills(), near = SaiyanProdigyConfig.bloodNearDeath();
        double[] w = SaiyanBloodRules.weights(ep, kills, near);
        Map<EvolutionRequirement, Float> out = new LinkedHashMap<>();
        if (w[0] > 0) out.put(new EvolutionRequirement.EPRequirement(ep), (float) w[0]);
        if (w[1] > 0) out.put(new Deed(SaiyanBloodRules.TAG_KILLS, kills, "anime_multiverse.saiyan.blood.kills", "Worthy foes beaten: %s / %s"), (float) w[1]);
        if (w[2] > 0) out.put(new Deed(SaiyanBloodRules.TAG_NEAR, near, "anime_multiverse.saiyan.blood.near", "Edge-of-death survivals: %s / %s"), (float) w[2]);
        if (out.isEmpty()) out.put(new EvolutionRequirement.EPRequirement(0.0), 100.0F);      // every deed off: nothing stands in the way
        return out;
    }

    /** One counted deed: progress is the counter in the Human race instance's tag over the need. */
    private static final class Deed extends EvolutionRequirement {
        private final String key, lang, fallback;
        private final int need;

        Deed(String key, int need, String lang, String fallback) {
            this.key = key;
            this.need = need;
            this.lang = lang;
            this.fallback = fallback;
        }

        private int have(ManasRaceInstance inst) {
            CompoundTag t = inst == null ? null : inst.getTag();
            return t == null ? 0 : t.getInt(key);
        }

        @Override
        public float getProgress(ManasRaceInstance inst, LivingEntity entity) { return (float) SaiyanBloodRules.share(have(inst), need); }

        @Override
        public Component getRequirementComponent(ManasRaceInstance inst, LivingEntity entity) { return Component.translatableWithFallback(lang, fallback, have(inst), need); }
    }

    // ---------------------------------------------------------------------------------------------------- the deeds
    static void onDeath(LivingDeathEvent e) {
        if (!(e.getSource().getEntity() instanceof ServerPlayer p) || !SaiyanProdigyConfig.bloodEvolution() || SaiyanProdigyConfig.bloodKills() <= 0) return;
        LivingEntity v = e.getEntity();
        if (v == p || !SaiyanBloodRules.worthyKill(v instanceof Enemy, v.getMaxHealth(), v instanceof Player)) return;
        count(p, SaiyanBloodRules.TAG_KILLS, -1L, 0L);
    }

    static void onDamaged(LivingDamageEvent.Post e) {
        if (!(e.getEntity() instanceof ServerPlayer p) || !SaiyanProdigyConfig.bloodEvolution() || SaiyanProdigyConfig.bloodNearDeath() <= 0) return;
        if (!SaiyanBloodRules.nearDeath(p.getHealth(), p.getMaxHealth())) return;
        count(p, SaiyanBloodRules.TAG_NEAR, p.level().getGameTime(), SaiyanBloodRules.NEAR_GAP_TICKS);
    }

    /** Adds one to a deed counter of a Human (any other race does not count towards this evolution); {@code now >= 0} also enforces the gap between two counted survivals. */
    private static void count(ServerPlayer p, String key, long now, long gap) {
        try {
            Optional<ManasRaceInstance> race = RaceAPI.getRaceFrom(p).getRace();
            if (race.isEmpty() || !HUMAN.equals(race.get().getRaceId())) return;
            ManasRaceInstance inst = race.get();
            CompoundTag t = inst.getOrCreateTag();
            if (now >= 0) {
                long last = t.contains(SaiyanBloodRules.TAG_NEAR_AT) ? t.getLong(SaiyanBloodRules.TAG_NEAR_AT) : -1L;
                if (!SaiyanBloodRules.nearGapOver(now, last)) return;
                t.putLong(SaiyanBloodRules.TAG_NEAR_AT, now);
            }
            int have = t.getInt(key), bumped = SaiyanBloodRules.bump(have);
            if (bumped == have) return;
            t.putInt(key, bumped);
            inst.markDirty();
        } catch (RuntimeException | LinkageError ex) {
            LOG.warn("Saiyan blood: a deed of {} was not counted ({})", p.getGameProfile().getName(), ex.toString());
        }
    }

    // ---------------------------------------------------------------------------------------------------- reading
    /** The Human's progress 0..100 towards the evolution, or -1 when the player is not a Human (for {@code /saiyan blood}). */
    public static double progress(ServerPlayer p) {
        try {
            Optional<ManasRaceInstance> race = RaceAPI.getRaceFrom(p).getRace();
            if (race.isEmpty() || !HUMAN.equals(race.get().getRaceId())) return -1.0;
            ManasRaceInstance inst = race.get();
            CompoundTag t = inst.getTag();
            IExistence ex = TensuraStorages.getExistenceFrom(p);
            return SaiyanBloodRules.progress(ex == null ? 0.0 : ex.getEP(), SaiyanProdigyConfig.bloodEp(), t == null ? 0 : t.getInt(SaiyanBloodRules.TAG_KILLS), SaiyanProdigyConfig.bloodKills(),
                    t == null ? 0 : t.getInt(SaiyanBloodRules.TAG_NEAR), SaiyanProdigyConfig.bloodNearDeath());
        } catch (RuntimeException | LinkageError ex) {
            return -1.0;
        }
    }

    /** The three counters (kills, near-death survivals) of a Human, for the command; {0, 0} otherwise. */
    public static int[] counters(ServerPlayer p) {
        try {
            Optional<ManasRaceInstance> race = RaceAPI.getRaceFrom(p).getRace();
            if (race.isEmpty() || !HUMAN.equals(race.get().getRaceId())) return new int[] {0, 0};
            CompoundTag t = race.get().getTag();
            return t == null ? new int[] {0, 0} : new int[] {t.getInt(SaiyanBloodRules.TAG_KILLS), t.getInt(SaiyanBloodRules.TAG_NEAR)};
        } catch (RuntimeException | LinkageError ex) {
            return new int[] {0, 0};
        }
    }

    /** Test helper: a fresh mutable list of evolutions with the Saiyan added (what the mixin does to Tensura's list). */
    static List<ManasRace> withSaiyan(List<ManasRace> base) {
        List<ManasRace> copy = new ArrayList<>(base);
        addEvolution(copy);
        return copy;
    }
}
