package com.newuniverse.nusmp.skilltree.line;

import com.newuniverse.nusmp.skilltree.ability.AbilityKit;
import com.newuniverse.nusmp.skilltree.ft.sky.SkyAllies;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.food.FoodData;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.event.entity.living.LivingDropsEvent;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Why: this is the whole of the 25 passive kinds of the effect catalogue, one small description per KIND, and nothing in it decides WHETHER a passive acts: by the time a method here is called the
 * dispatcher ({@link LinePassives}) has already asked the gate ({@link LineGate}) and found the line equipped, the node bought and its switch on, so an executor only says WHAT happens. A passive is
 * a bundle of hooks, because the kinds fall into five shapes: it reshapes a blow the player DEALS (a critical hit, a bonus against undead, a combo), reshapes a blow the player TAKES (a dodge, a
 * resistance, a cut while wounded), reacts to a blow once it landed (a lifesteal, a shove, an effect on the foe), does something every half second (an aura, a regeneration, a hunger saver, an effect
 * while a condition holds), or reacts to a KILL, its experience or its drops. A kind fills the hooks it needs and leaves the rest empty. Every number comes from the node's tables at its level through
 * {@link PassiveCtx}; every "rest" (a dodge cannot fire twice in a row) lives in the player's own data under the passive's own key, never in a static map. Healing a passive does itself (a lifesteal, a regeneration)
 * goes straight to {@code heal} and is capped per event (a tenth of the maximum health for a hit, a quarter for a kill), because the balance law's fade after a heal is meant for casts and would turn every lifesteal
 * into a 40 % lifesteal. The registry is filled by the {@code reg} calls; a test reads this file and the catalogue and fails when a kind has no line here or a line here has no kind.
 */
public final class PassiveExecutors {
    private PassiveExecutors() {}

    @FunctionalInterface public interface Dealt { float apply(PassiveCtx c, LivingEntity target, DamageSource src, float amount); }

    @FunctionalInterface public interface DealtPost { void apply(PassiveCtx c, LivingEntity target, float dealt); }

    @FunctionalInterface public interface Taken { float apply(PassiveCtx c, DamageSource src, float amount); }

    @FunctionalInterface public interface Beat { void apply(PassiveCtx c); }

    @FunctionalInterface public interface Kill { void apply(PassiveCtx c, LivingEntity victim); }

    @FunctionalInterface public interface Drops { void apply(PassiveCtx c, LivingDropsEvent e); }

    @FunctionalInterface public interface Xp { int apply(PassiveCtx c, int xp); }

    /** The hooks of one kind; a hook that is null is not used by that kind. */
    public static final class Passive {
        Dealt dealt;
        DealtPost post;
        Taken taken;
        Beat beat;
        Kill kill;
        Drops drops;
        Xp xp;

        Passive dealt(Dealt h) { dealt = h; return this; }

        Passive post(DealtPost h) { post = h; return this; }

        Passive taken(Taken h) { taken = h; return this; }

        Passive beat(Beat h) { beat = h; return this; }

        Passive kill(Kill h) { kill = h; return this; }

        Passive drops(Drops h) { drops = h; return this; }

        Passive xp(Xp h) { xp = h; return this; }

        public Dealt dealt() { return dealt; }

        public DealtPost post() { return post; }

        public Taken taken() { return taken; }

        public Beat beat() { return beat; }

        public Kill kill() { return kill; }

        public Drops drops() { return drops; }

        public Xp xp() { return xp; }
    }

    private static final Map<String, Passive> KINDS = new LinkedHashMap<>();
    /** The beat is every 10 ticks, half a second. */
    public static final int BEAT_TICKS = 10;

    private static Passive on() { return new Passive(); }

    private static void reg(String kind, Passive p) { KINDS.put(kind, p); }

    static {
        reg("on_hit_effect", on().post((c, t, dealt) -> {
            if (c.roll(c.num("chance"))) LineLookups.give(t, c.str("effect", ""), Math.max(20, c.whole("seconds") * 20), c.whole("amp"), true);
        }));
        reg("on_hurt_retaliate", on().taken((c, src, amount) -> {
            LivingEntity attacker = src.getEntity() instanceof LivingEntity a && a != c.p ? a : null;
            if (attacker != null && !attacker.isAlliedTo(c.p) && c.roll(c.num("chance")) && c.rested("", c.whole("rest"))) {
                LineLookups.give(attacker, c.str("effect", ""), Math.max(20, c.whole("seconds") * 20), c.whole("amp"), true);
            }
            return amount;
        }));
        reg("self_effect_when", on().beat(c -> {
            if (LineLookups.condition(c.str("condition", ""), c.p)) LineLookups.give(c.p, c.str("effect", ""), BEAT_TICKS * 3, c.whole("amp"), false);
        }));
        reg("lifesteal", on().post((c, t, dealt) -> {
            if (dealt > 0f) c.p.heal(Math.min((float) (dealt * c.num("pct")), c.p.getMaxHealth() * 0.1f));
        }));
        reg("thorns", on().taken((c, src, amount) -> {
            LivingEntity attacker = src.getEntity() instanceof LivingEntity a && a != c.p ? a : null;
            if (attacker != null && amount > 0f && !attacker.isAlliedTo(c.p) && c.rested("", c.whole("rest"))) {
                LineEvents.reflect(c.p, attacker, (float) (amount * c.num("pct")));
            }
            return amount;
        }));
        reg("crit", on().dealt((c, t, src, amount) -> {
            if (!c.roll(c.num("chance"))) return amount;
            c.p.serverLevel().playSound(null, c.p.blockPosition(), SoundEvents.PLAYER_ATTACK_CRIT, SoundSource.PLAYERS, 0.8f, 1.0f);
            return (float) (amount * c.num("mult"));
        }));
        reg("dmg_vs", on().dealt((c, t, src, amount) -> LineLookups.category(c.str("category", ""), t) ? (float) (amount * (1.0 + c.num("bonus"))) : amount));
        reg("dmg_when", on().dealt((c, t, src, amount) -> LineLookups.condition(c.str("condition", ""), c.p) ? (float) (amount * (1.0 + c.num("bonus"))) : amount));
        reg("reduce_when", on().taken((c, src, amount) -> LineLookups.condition(c.str("condition", ""), c.p) ? (float) (amount * (1.0 - Math.min(0.9, c.num("pct")))) : amount));
        reg("regen_when", on().beat(c -> {
            if (c.p.getHealth() < c.p.getMaxHealth() && LineLookups.condition(c.str("condition", ""), c.p)) c.p.heal((float) (c.num("per_second") * BEAT_TICKS / 20.0));
        }));
        reg("kill_reward", on().kill((c, victim) -> {
            if (c.num("heal") > 0) c.p.heal(Math.min((float) c.num("heal"), c.p.getMaxHealth() * 0.25f));
            if (c.num("seconds") > 0) LineLookups.give(c.p, c.str("effect", "none"), c.whole("seconds") * 20, c.whole("amp"), true);
        }));
        reg("dodge", on().taken((c, src, amount) -> {
            if (amount <= 0f || src.is(DamageTypeTags.BYPASSES_INVULNERABILITY)) return amount;
            if (!c.roll(c.num("chance")) || !c.rested("", c.whole("rest"))) return amount;
            c.p.serverLevel().playSound(null, c.p.blockPosition(), SoundEvents.PLAYER_ATTACK_NODAMAGE, SoundSource.PLAYERS, 0.9f, 1.4f);
            LineFx.rukh(c.p.serverLevel(), c.route, c.p, 4);
            return 0f;
        }));
        reg("resist", on().taken((c, src, amount) -> LineLookups.damageKind(c.str("damage_kind", ""), src) ? (float) (amount * (1.0 - Math.min(0.9, c.num("pct")))) : amount));
        reg("xp_bonus", on().xp((c, xp) -> (int) Math.round(xp * (1.0 + c.num("pct")))));
        reg("drop_bonus", on().drops((c, e) -> doubled(c, e)));
        reg("aura", on().beat(c -> aura(c)));
        reg("hunger_saver", on().beat(c -> {
            FoodData food = c.p.getFoodData();
            double last = LineState.getDouble(c.p, LineState.EXHAUST + "." + c.key()), cur = food.getExhaustionLevel();
            if (cur > last && last > 0.0) {                                  // exhaustion rose since the last beat: give a share of the rise back
                float back = (float) ((cur - last) * Math.min(0.9, c.num("pct")));
                food.setExhaustion(Math.max(0f, (float) cur - back));
                cur -= back;
            }
            LineState.putDouble(c.p, LineState.EXHAUST + "." + c.key(), cur);
        }));
        reg("combo", on().dealt((c, t, src, amount) -> {
            String k = "combo." + c.key();
            int n = LineState.getInt(c.p, k);
            if (c.now - LineState.getLong(c.p, k + ".t") > 60) n = 0;               // a pause of three seconds ends the chain
            n++;
            LineState.putLong(c.p, k + ".t", c.now);
            if (n >= Math.max(2, c.whole("hits"))) {
                LineState.putInt(c.p, k, 0);
                c.p.serverLevel().playSound(null, c.p.blockPosition(), SoundEvents.PLAYER_ATTACK_STRONG, SoundSource.PLAYERS, 0.9f, 0.8f);
                return (float) (amount * (1.0 + c.num("bonus")));
            }
            LineState.putInt(c.p, k, n);
            return amount;
        }));
        reg("last_stand", on().taken((c, src, amount) -> {
            if (amount <= 0f || src.is(DamageTypeTags.BYPASSES_INVULNERABILITY)) return amount;
            if (c.p.getHealth() - amount > c.p.getMaxHealth() * c.num("threshold")) return amount;
            if (!c.rested("", c.whole("rest"))) return amount;
            int ticks = Math.max(20, c.whole("seconds") * 20);
            LineLookups.give(c.p, "absorption", ticks, 1, true);
            LineLookups.give(c.p, "resistance", ticks, 1, true);
            LineFx.rukh(c.p.serverLevel(), c.route, c.p, 8);
            return amount;
        }));
        reg("momentum", on().dealt((c, t, src, amount) -> {
            String k = "momentum." + c.key();
            int stacks = LineState.getInt(c.p, k);
            if (c.now - LineState.getLong(c.p, k + ".t") > 60) stacks = 0;          // the stacks fade after three seconds without a blow
            float out = (float) (amount * (1.0 + c.num("per_stack") * stacks));
            LineState.putInt(c.p, k, Math.min(c.whole("max_stacks"), stacks + 1));
            LineState.putLong(c.p, k + ".t", c.now);
            return out;
        }));
        reg("sprint_bonus", on().dealt((c, t, src, amount) -> c.p.isSprinting() ? (float) (amount * (1.0 + c.num("bonus"))) : amount));
        reg("fall_guard", on().taken((c, src, amount) -> src.is(DamageTypeTags.IS_FALL) ? (float) (amount * (1.0 - Math.min(0.9, c.num("pct")))) : amount));
        reg("knockback_on_hit", on().post((c, t, dealt) -> {
            if (dealt > 0f && t.isAlive() && c.roll(c.num("chance"))) LineGeo.shove(c.p, t, c.num("strength"));
        }));
        reg("execute_threshold", on().dealt((c, t, src, amount) -> t.getHealth() <= t.getMaxHealth() * c.num("threshold") ? (float) (amount * (1.0 + c.num("bonus"))) : amount));
        reg("mending", on().beat(c -> {
            String k = "mend." + c.key();
            if (c.now < LineState.getLong(c.p, k)) return;
            LineState.putLong(c.p, k, c.now + Math.max(20, c.whole("seconds") * 20L));
            if (c.p.getHealth() < c.p.getMaxHealth()) c.p.heal((float) c.num("amount"));
        }));
    }

    /** The hooks of a kind, or null. */
    public static Passive get(String kind) { return kind == null ? null : KINDS.get(kind); }

    /** Every kind that has an executor (the test compares it with the catalogue). */
    public static Set<String> kinds() { return Collections.unmodifiableSet(KINDS.keySet()); }

    // ================================================================== the longer ones
    private static void doubled(PassiveCtx c, LivingDropsEvent e) {
        if (e.getDrops().isEmpty() || !c.roll(c.num("chance"))) return;
        List<ItemEntity> drops = new ArrayList<>(e.getDrops());
        ItemEntity pick = drops.get(c.p.getRandom().nextInt(drops.size()));
        ItemStack copy = pick.getItem().copy();
        if (copy.isEmpty()) return;
        e.getDrops().add(new ItemEntity(pick.level(), pick.getX(), pick.getY(), pick.getZ(), copy));
    }

    private static void aura(PassiveCtx c) {
        double radius = c.num("radius");
        String effect = c.str("effect", "");
        int ticks = BEAT_TICKS * 3, amp = c.whole("amp"), n = 0;
        if ("enemies".equals(c.str("target", "allies"))) {
            for (LivingEntity e : AbilityKit.around(c.p, c.p.position(), radius)) {
                if (n++ >= 12) break;
                LineLookups.give(e, effect, ticks, amp, false);
            }
        } else {
            for (LivingEntity e : SkyAllies.around(c.p, c.p.position(), radius)) {
                if (n++ >= 12) break;
                LineLookups.give(e, effect, ticks, amp, false);
            }
        }
        if (c.p.tickCount % 40 < BEAT_TICKS) LineFx.rukh(c.p.serverLevel(), c.route, c.p, 2);
    }
}
