package com.newuniverse.nusmp.elite.records;

import com.newuniverse.nusmp.elite.records.rule.Bonuses;
import com.newuniverse.nusmp.elite.records.rule.Bonuses.Attr;
import com.newuniverse.nusmp.elite.records.rule.Bonuses.Effect;
import com.newuniverse.nusmp.elite.records.rule.Catalog;
import com.newuniverse.nusmp.elite.records.rule.Defs.SynergyDef;
import com.newuniverse.nusmp.elite.records.rule.Defs.TitleDef;
import com.newuniverse.nusmp.elite.records.rule.RecordData;
import com.newuniverse.nusmp.elite.records.rule.SynergyMatcher;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Why: this is where a title's bonuses actually reach the player. Every beat the server computes the DESIRED bonus set (the active title's bonuses plus the bonuses of every synergy the held titles
 * match, times the config's {@code bonusScale}) and makes the player look like that: each attribute amount is one transient modifier (never saved, so a crash or a removed mod cannot leave a stuck
 * +10 Max Health in a player file) with a stable id per attribute and operation, an effect is a short ambient effect that the next beat renews, and anything applied before that is no longer
 * desired is removed (the old title's modifiers disappear the beat after the player picks another one, or at once when the player picks it, because the pick calls {@link #apply} directly).
 * Custom stats are not applied to anything; the totals are kept so {@code RecordEvents.customStat} and the three stats this system implements can answer. Server thread only. Unknown attribute or effect
 * ids (a title naming a mod that is not installed) are skipped silently here; the self-test lists them.
 */
final class TitleApplier {
    private TitleApplier() {}

    /** What was applied to one player last time: the modifier ids with their attribute, the effects, and the totals. */
    private record Applied(Map<ResourceLocation, Holder<Attribute>> attrs, Set<Holder<MobEffect>> effects, Bonuses totals) {}

    private static final Map<UUID, Applied> APPLIED = new HashMap<>();

    // ------------------------------------------------------------------ what is wanted
    /** The synergies the player's HELD titles match (empty when synergies are off). */
    static List<SynergyDef> matchedSynergies(Catalog cat, RecordData d) {
        if (!RecordsConfig.synergies()) return List.of();
        return SynergyMatcher.match(cat.synergies(), d.heldTitles(), cat::tagsOf);
    }

    /** The bonus set the player should have right now (scaled; NONE when title bonuses are off). */
    static Bonuses desired(Catalog cat, RecordData d) {
        if (!RecordsConfig.enabled() || !RecordsConfig.titleBonuses()) return Bonuses.NONE;
        List<Bonuses> parts = new ArrayList<>();
        TitleDef active = d.active().isEmpty() ? null : cat.title(d.active());
        if (active != null && d.holds(active.id())) parts.add(active.bonuses());
        for (SynergyDef s : matchedSynergies(cat, d)) parts.add(s.bonuses());
        return Bonuses.merge(parts).scaled(RecordsConfig.bonusScale());
    }

    // ------------------------------------------------------------------ applying
    static ResourceLocation modifierId(Attr a) {
        String raw = a.attribute().replace(':', '_').replace('.', '_').toLowerCase();
        return ResourceLocation.fromNamespaceAndPath("nusmp", "elite_title/" + raw + "_" + a.op().name().toLowerCase());
    }

    static AttributeModifier.Operation operation(Bonuses.Op op) {
        return switch (op) {
            case ADD_VALUE -> AttributeModifier.Operation.ADD_VALUE;
            case ADD_MULTIPLIED_BASE -> AttributeModifier.Operation.ADD_MULTIPLIED_BASE;
            case ADD_MULTIPLIED_TOTAL -> AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL;
        };
    }

    static Holder<Attribute> attribute(String id) {
        ResourceLocation rl = ResourceLocation.tryParse(id);
        return rl == null ? null : BuiltInRegistries.ATTRIBUTE.getHolder(rl).map(h -> (Holder<Attribute>) h).orElse(null);
    }

    static Holder<MobEffect> effect(String id) {
        ResourceLocation rl = ResourceLocation.tryParse(id);
        return rl == null ? null : BuiltInRegistries.MOB_EFFECT.getHolder(rl).map(h -> (Holder<MobEffect>) h).orElse(null);
    }

    /** Makes the player carry exactly {@code want}: adds or updates what is wanted, removes what was applied before and is not wanted now. */
    static void apply(ServerPlayer p, Bonuses want) {
        Applied old = APPLIED.get(p.getUUID());
        Map<ResourceLocation, Holder<Attribute>> now = new HashMap<>();
        for (Attr a : want.attributes()) {
            Holder<Attribute> h = attribute(a.attribute());
            if (h == null) continue;
            AttributeInstance inst = p.getAttribute(h);
            if (inst == null) continue;
            ResourceLocation id = modifierId(a);
            AttributeModifier.Operation op = operation(a.op());
            now.put(id, h);
            AttributeModifier cur = inst.getModifier(id);
            if (cur != null && cur.operation() == op && Math.abs(cur.amount() - a.amount()) < 1e-9) continue;
            if (cur != null) inst.removeModifier(id);
            inst.addTransientModifier(new AttributeModifier(id, a.amount(), op));
        }
        if (old != null) {
            for (var e : old.attrs().entrySet()) {
                if (now.containsKey(e.getKey())) continue;
                AttributeInstance inst = p.getAttribute(e.getValue());
                if (inst != null) inst.removeModifier(e.getKey());
            }
        }
        int duration = Math.max(260, RecordsConfig.beatTicks() * 6);
        Set<Holder<MobEffect>> effects = new HashSet<>();
        for (Effect e : want.effects()) {
            Holder<MobEffect> h = effect(e.effect());
            if (h == null) continue;
            effects.add(h);
            p.addEffect(new MobEffectInstance(h, duration, e.amplifier(), true, false, false));
        }
        if (old != null) {
            for (Holder<MobEffect> h : old.effects()) {
                if (effects.contains(h)) continue;
                MobEffectInstance cur = p.getEffect(h);
                if (cur != null && cur.isAmbient() && !cur.isVisible() && cur.getDuration() <= duration + 20) p.removeEffect(h);
            }
        }
        if (now.isEmpty() && effects.isEmpty() && want.stats().isEmpty()) APPLIED.remove(p.getUUID());
        else APPLIED.put(p.getUUID(), new Applied(now, effects, want));
    }

    /** Removes everything this class applied to the player and forgets them (logout, system switched off). */
    static void clear(ServerPlayer p) {
        apply(p, Bonuses.NONE);
        APPLIED.remove(p.getUUID());
    }

    static void forget(UUID id) { APPLIED.remove(id); }

    /** The totals applied to an online player last beat (NONE if nothing). */
    static Bonuses totals(UUID id) {
        Applied a = APPLIED.get(id);
        return a == null ? Bonuses.NONE : a.totals();
    }

    /** One custom stat of the bonuses applied to the player; 0 when none. */
    static double stat(UUID id, String name) { return totals(id).stat(name); }

    /** The ids of the modifiers currently applied to the player (self-test, debugging). */
    static Set<ResourceLocation> appliedModifierIds(UUID id) {
        Applied a = APPLIED.get(id);
        return a == null ? Set.of() : new HashSet<>(a.attrs().keySet());
    }
}
