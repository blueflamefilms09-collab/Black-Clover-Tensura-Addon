package com.newuniverse.nusmp.devil.mastery;

import com.newuniverse.nusmp.devil.DevilConfig;
import com.newuniverse.nusmp.devil.DevilContract;
import com.newuniverse.nusmp.devil.DevilContracts;
import com.newuniverse.nusmp.devil.DevilRules;
import com.newuniverse.nusmp.devil.DevilStore;
import com.newuniverse.nusmp.devil.WorldDevil;
import com.newuniverse.nusmp.devil.phase2.DevilLedger;
import com.newuniverse.nusmp.devil.phase2.Phase2;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;

import java.util.List;

/**
 * Why: the server half of the Devil Awakening skill (docs/devil_mastery.md): the mastery of a contract, how it grows, what its tiers open, the timed awakening (the key and the skill's first mode begin and end it,
 * {@code DevilContracts.onToggleAsk}), the kit's passives and the double unions. Every decision is {@link MasteryRules} and {@link DevilKit}; this class only reads and writes the player. Everything is the server's,
 * keyed by the player's own contract (one attachment), nothing is kept in a static map.
 *
 * <p>Cost: one hasData lookup per damage event for players without a contract. A holder pays, once a second, a few additions, a ledger map lookup (only inside the Devil Union with a kit that has fusions) and at
 * most six attribute writes (each only when its value changed); per blow dealt, one counted use a second (an effect on the target). No entity is spawned, no world is scanned, no packet is sent from here (the
 * mastery rides on the existing once-a-change {@code DevilNet.Sync}).
 */
public final class Mastery {
    private Mastery() {}

    public static void register(IEventBus bus) {
        bus.addListener(Mastery::onDamage);
        bus.addListener(Mastery::onDeath);
    }

    private static ResourceLocation rl(String p) { return ResourceLocation.fromNamespaceAndPath("anime_multiverse", p); }

    private static final ResourceLocation[] STAT_IDS = {rl("devil_kit_attack"), rl("devil_kit_speed"), rl("devil_kit_armor"), rl("devil_kit_health"), rl("devil_kit_knockback"), rl("devil_kit_reach")};
    private static final String[] STAT_NAMES = {"attack", "speed", "armor", "health", "knockback", "reach"};
    private static final double[] STAT_CAP = {12.0, 0.4, 8.0, 12.0, 0.8, 2.0};
    private static final double MAX_RESIST = 0.6, MAX_REGEN = 1.0;

    // ---------------------------------------------------------------- the mastery
    public static boolean on() { return DevilConfig.enabled() && DevilConfig.masteryOn(); }

    /** The mastery in percent (the server's start value until the first gain). */
    public static float of(DevilContract c) { return MasteryRules.clamp(c.mastery < 0f ? DevilConfig.masteryStart() : c.mastery); }

    public static int tier(DevilContract c) { return MasteryRules.tier(of(c)); }

    public static int tenths(DevilContract c) { return MasteryRules.tenths(of(c)); }

    /** What the HUD packet carries: the tenths, or 0 (no mastery panel) when the system is off or the legacy assimilation is on. */
    public static int hudTenths(DevilContract c) { return on() && !DevilConfig.legacyAssimilation() ? tenths(c) : 0; }

    /** The highest assimilation level the mastery lets through now (100 with the system off or the legacy setting on). */
    public static float ceiling(DevilContract c) { return !on() ? 100f : MasteryRules.ceiling(Math.min(of(c), DevilConfig.masteryMax()), DevilConfig.legacyAssimilation()); }

    public static boolean awake(DevilContract c) { return c != null && c.hasContract() && (c.assimilating || c.inUnion()) && c.assimilation >= 1f; }

    private static boolean timed() { return on() && !DevilConfig.legacyAssimilation(); }

    /** Counts a use: {@code eff} effective seconds go through the daily cap and the curve. {@code acted}: the use was a deed (a blow, an art, a kill), which also resets the anti-AFK clock. */
    public static void used(ServerPlayer p, DevilContract c, double eff, boolean acted) {
        if (!on() || c == null || !c.hasContract()) return;
        long now = DevilContracts.gameTime(p);
        if (acted) c.lastAct = now;
        add(p, c, eff);
    }

    private static void add(ServerPlayer p, DevilContract c, double eff) {
        if (!(eff > 0)) return;
        int day = (int) (System.currentTimeMillis() / 86_400_000L);
        if (c.masteryDay != day) { c.masteryDay = day; c.masteryUsed = 0f; }
        double e = Math.min(eff, MasteryRules.capLeft(c.masteryUsed, DevilConfig.masteryDailyMinutes()));
        if (e <= 0) return;
        c.masteryUsed += (float) e;
        float before = of(c), after = MasteryRules.gain(before, e, DevilConfig.masteryRate(), DevilConfig.masteryMax());
        if (after <= before) return;
        c.mastery = after;
        int t0 = MasteryRules.tier(before), t1 = MasteryRules.tier(after);
        if (t1 > t0) tierUp(p, c, t1);
    }

    private static void tierUp(ServerPlayer p, DevilContract c, int tier) {
        say(p, "tier_up", "Mastery %s%%: the devil's power opens another step.", ChatFormatting.GOLD, Math.round(MasteryRules.TIER_AT[tier]));
        p.serverLevel().playSound(null, p.blockPosition(), SoundEvents.BEACON_POWER_SELECT, SoundSource.PLAYERS, 0.8f, 0.6f);
        DevilContracts.syncNow(p, c);
    }

    /** A contract was signed after a trial fight: the start is a little higher for a devil that was beaten or impressed. {@code result}: {@code Phase2Rules.TRIAL_*}. */
    public static void joined(DevilContract c, WorldDevil d, int result) {
        if (!on() || result <= 0) return;
        float bonus = (float) (result >= 2 ? MasteryRules.TRIAL_WON : MasteryRules.TRIAL_IMPRESSED);
        if (d.id().equals(c.devil)) c.mastery = MasteryRules.clamp(of(c) + bonus);
        else c.masteryStash.put(d.id(), MasteryRules.clamp(DevilConfig.masteryStart() + bonus));
    }

    /** A different devil becomes the active one: the mastery of the old one is kept for later, the new one's (or the start value) is taken. */
    public static void swap(DevilContract c, String from, String to) {
        if (from != null && !from.isEmpty() && c.mastery >= 0f) c.masteryStash.put(from, c.mastery);
        Float back = c.masteryStash.remove(to);
        c.mastery = back == null ? -1f : back;
    }

    /** The contract with this devil ended: its mastery goes with it. */
    public static void forget(DevilContract c, String devil) {
        c.mastery = -1f;
        c.awakenUntil = 0;
        if (devil != null) c.masteryStash.remove(devil);
    }

    // ---------------------------------------------------------------- the timed awakening
    /** True (with a message) when the devil is still resting and may not be let in yet. */
    public static boolean refuseBegin(ServerPlayer p, DevilContract c, long now) {
        if (!timed() || c.awakenReadyAt <= now) return false;
        say(p, "resting", "The devil rests: %s s before it answers again.", ChatFormatting.GRAY, DevilRules.secondsLeft(c.awakenReadyAt, now));
        return true;
    }

    /** The devil was let in: the clock of this awakening starts. */
    public static void began(ServerPlayer p, DevilContract c, long now) {
        if (!timed()) { c.awakenUntil = 0; return; }
        c.awakenUntil = now + MasteryRules.durationSeconds(of(c)) * 20L;
        c.lastAct = now;
        c.lastX = p.getX();
        c.lastZ = p.getZ();
    }

    public static boolean expired(DevilContract c, long now) { return timed() && c.awakenUntil > 0 && now >= c.awakenUntil; }

    /** The awakening ended ({@code why}: withdraw by hand, time, dry): the rest begins, and an awakening that ran out by itself leaves a fatigue. Call after {@code assimilating} was cleared, before the level falls. */
    public static void ended(ServerPlayer p, DevilContract c, long now, String why) {
        if (!timed()) return;
        boolean hand = "withdraw".equals(why);
        int rest = hand ? MasteryRules.withdrawCooldownSeconds(of(c)) : MasteryRules.cooldownSeconds(of(c));
        c.awakenReadyAt = Math.max(c.awakenReadyAt, now + rest * 20L);
        c.awakenUntil = 0;
        int after = hand ? 0 : MasteryRules.aftermathSeconds(c.assimilation, of(c));
        if (after > 0 && !p.isCreative()) {
            p.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, after * 20, 0, false, false, true));
            p.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, after * 20, 0, false, false, true));
            say(p, "aftermath", "The awakening ends. Your body aches for %s s.", ChatFormatting.DARK_RED, after);
        }
    }

    // ---------------------------------------------------------------- once a second
    /** Called from {@code DevilContracts.second} after the level moved: counts the awakened second, notices the stage change and heals. */
    public static void second(ServerPlayer p, DevilContract c, long now) {
        if (!on()) return;
        double dx = p.getX() - c.lastX, dz = p.getZ() - c.lastZ;
        if (dx * dx + dz * dz > 2.25) c.movedAt = now;
        c.lastX = p.getX();
        c.lastZ = p.getZ();
        if (awake(c)) {
            double f = MasteryRules.afkFactor(now - c.lastAct, now - c.movedAt <= 400);
            if (f > 0) add(p, c, f);
        }
        int stage = DevilRules.stage(c.assimilation);
        if (stage > c.lastStage && awake(c)) onStage(p, c, stage);
        c.lastStage = stage;
        double heal = regen(p, c);
        if (heal > 0 && p.getHealth() < p.getMaxHealth() && !p.isDeadOrDying()) p.heal((float) heal);
    }

    private static void onStage(ServerPlayer p, DevilContract c, int stage) {
        DevilKit.Kit k = DevilKit.of(c.devil);
        String name = k != null && k.stages().size() == 3 ? k.stages().get(stage - 1) : "";
        if (stage == 3 && k != null && !k.unionName().isEmpty()) name = k.unionName();
        if (name.isEmpty()) return;
        say(p, "stage", "%s", ChatFormatting.DARK_PURPLE, name);
        ServerLevel level = p.serverLevel();
        com.newuniverse.nusmp.relics.Fx.burst(level, p.position(), 0x5A0F18);                                       // one burst per stage change (a few times an awakening at most), through the Rukh particles
    }

    // ---------------------------------------------------------------- the kit's passives
    private static boolean usable(DevilKit.Passive p, DevilContract c, int tier) {
        return tier >= p.tier() && (p.when().equals("awake") || (c.inUnion() && (p.when().equals("union") || p.when().equals("fusion"))));
    }

    /** The passives that work for this player now: the kit's (by tier, awake) and, in the Devil Union, the union's and the fusions' of a second held devil. Empty when none. */
    public static List<DevilKit.Passive> active(ServerPlayer p, DevilContract c) {
        if (!on() || !DevilConfig.kitPassives() || !awake(c)) return List.of();
        DevilKit.Kit k = DevilKit.of(c.devil);
        if (k == null) return List.of();
        int tier = tier(c);
        List<DevilKit.Passive> out = new java.util.ArrayList<>(6);
        for (DevilKit.Passive ps : k.passives()) if (usable(ps, c, tier)) out.add(ps);
        if (c.inUnion() && DevilConfig.kitFusions() && p.getServer() != null) {
            DevilLedger ledger = null;
            for (DevilKit.Fusion f : DevilKit.table().fusions()) {
                String partner = f.a().equals(c.devil) ? f.b() : f.b().equals(c.devil) ? f.a() : null;
                if (partner == null) continue;
                if (ledger == null) ledger = DevilLedger.get(p.getServer());
                if (ledger.pact(p.getUUID(), partner) == null) continue;
                for (DevilKit.Passive ps : f.passives()) if (usable(ps, c, tier)) out.add(ps);
            }
        }
        return out;
    }

    /** Sets the six kit attribute modifiers to what the active passives add (0 removes). Called from {@code DevilContracts.applyStats}; each write only when the value changed. */
    public static void applyPassives(ServerPlayer p, DevilContract c) {
        double[] sum = new double[STAT_NAMES.length];
        List<DevilKit.Passive> act = active(p, c);
        if (!act.isEmpty()) {
            double scale = MasteryRules.passiveScale(of(c)) * Phase2.power(p, c);
            for (DevilKit.Passive ps : act) if (ps.kind().equals("stat")) {
                int i = java.util.Arrays.asList(STAT_NAMES).indexOf(ps.type());
                if (i >= 0) sum[i] += ps.value() * scale;
            }
        }
        boolean hadHealth = false;
        for (int i = 0; i < sum.length; i++) {
            double v = Math.min(sum[i], STAT_CAP[i]);
            switch (i) {
                case 0 -> DevilContracts.setModifier(p, Attributes.ATTACK_DAMAGE, STAT_IDS[i], v, AttributeModifier.Operation.ADD_VALUE, false);
                case 1 -> DevilContracts.setModifier(p, Attributes.MOVEMENT_SPEED, STAT_IDS[i], v, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL, false);
                case 2 -> DevilContracts.setModifier(p, Attributes.ARMOR, STAT_IDS[i], v, AttributeModifier.Operation.ADD_VALUE, false);
                case 3 -> { DevilContracts.setModifier(p, Attributes.MAX_HEALTH, STAT_IDS[i], v, AttributeModifier.Operation.ADD_VALUE, false); hadHealth = true; }
                case 4 -> DevilContracts.setModifier(p, Attributes.KNOCKBACK_RESISTANCE, STAT_IDS[i], v, AttributeModifier.Operation.ADD_VALUE, false);
                default -> DevilContracts.setModifier(p, Attributes.ENTITY_INTERACTION_RANGE, STAT_IDS[i], v, AttributeModifier.Operation.ADD_VALUE, false);
            }
        }
        if (hadHealth && p.getHealth() > p.getMaxHealth()) p.setHealth(p.getMaxHealth());
    }

    private static double regen(ServerPlayer p, DevilContract c) {
        double sum = 0;
        List<DevilKit.Passive> act = active(p, c);
        if (act.isEmpty()) return 0;
        for (DevilKit.Passive ps : act) if (ps.kind().equals("regen")) sum += ps.value();
        return Math.min(MAX_REGEN, sum * MasteryRules.passiveScale(of(c)) * Phase2.power(p, c));
    }

    // ---------------------------------------------------------------- blows
    private static String category(DamageSource s) {
        if (s.is(DamageTypeTags.IS_FIRE)) return "fire";
        if (s.is(DamageTypeTags.IS_FALL)) return "fall";
        if (s.is(DamageTypeTags.IS_EXPLOSION)) return "explosion";
        if (s.is(DamageTypeTags.IS_PROJECTILE)) return "projectile";
        if (s.is(DamageTypes.MAGIC) || s.is(DamageTypes.INDIRECT_MAGIC) || s.is(DamageTypes.WITHER) || s.is(com.newuniverse.nusmp.devil.dual.DevilMagic.DAMAGE_TAG)) return "magic";
        return s.getDirectEntity() instanceof LivingEntity ? "physical" : "";
    }

    private static void onDamage(LivingIncomingDamageEvent e) {
        if (e.getEntity().level().isClientSide || e.getAmount() <= 0f || !on()) return;
        LivingEntity target = e.getEntity();
        DamageSource src = e.getSource();
        if (target instanceof ServerPlayer victim) {
            DevilContract vc = DevilStore.peek(victim);
            if (vc != null && awake(vc) && DevilConfig.kitPassives()) {
                String cat = category(src);
                if (!cat.isEmpty()) {
                    double r = 0;
                    for (DevilKit.Passive ps : active(victim, vc)) if (ps.kind().equals("resist") && ps.type().equals(cat)) r += ps.value();
                    r = Math.min(MAX_RESIST, r * MasteryRules.passiveScale(of(vc)) * Phase2.power(victim, vc));
                    if (r > 0) e.setAmount((float) (e.getAmount() * (1.0 - r)));
                }
            }
        }
        if (src.getEntity() instanceof ServerPlayer p && src.getDirectEntity() == p && p != target) {
            DevilContract c = DevilStore.peek(p);
            if (c == null || !awake(c)) return;
            long now = DevilContracts.gameTime(p);
            c.lastAct = now;
            if (now - c.lastPassiveHit < 20 && now >= c.lastPassiveHit) return;                // one counted blow and one on-hit effect a second
            c.lastPassiveHit = now;
            add(p, c, MasteryRules.HIT);
            if (!DevilConfig.kitPassives()) return;
            for (DevilKit.Passive ps : active(p, c)) if (ps.kind().equals("onhit") && ps.fx() != null)
                com.newuniverse.nusmp.relics.Fx.addEffect(target, ps.fx().id(), ps.fx().ticks(), ps.fx().amp());
        }
    }

    private static void onDeath(LivingDeathEvent e) {
        if (e.getEntity().level().isClientSide || !on() || e.getEntity() instanceof Player || !(e.getSource().getEntity() instanceof ServerPlayer p)) return;
        DevilContract c = DevilStore.peek(p);
        if (c == null || !awake(c)) return;
        LivingEntity dead = e.getEntity();
        long now = DevilContracts.gameTime(p);
        if (dead.getMaxHealth() < MasteryRules.KILL_MIN_HEALTH || (now - c.lastKillCredit < MasteryRules.KILL_GAP && now >= c.lastKillCredit)) return;
        c.lastKillCredit = now;
        used(p, c, MasteryRules.KILL * Math.min(1.0, dead.getMaxHealth() / 20.0), true);
    }

    // ---------------------------------------------------------------- text
    private static void say(ServerPlayer p, String key, String fallback, ChatFormatting color, Object... args) {
        p.displayClientMessage(Component.translatableWithFallback("anime_multiverse.devil.mastery." + key, fallback, args).withStyle(color), true);
    }

    /** The line the Read mode of the skill prints: mastery, tier, the next opening and the awakening's numbers. */
    public static Component readout(DevilContract c, long now) {
        float m = of(c);
        float next = MasteryRules.nextAt(m);
        int rest = DevilRules.secondsLeft(c.awakenReadyAt, now);
        return Component.translatableWithFallback("anime_multiverse.devil.mastery.readout",
                "Mastery %s%% (step %s of %s). %s Awakening: %s s, then rest %s s%s.",
                String.format(java.util.Locale.ROOT, "%.1f", m), MasteryRules.tier(m) + 1, MasteryRules.TIERS,
                next < 0 ? "Everything is open." : "Next opening at " + Math.round(next) + "%.",
                MasteryRules.durationSeconds(m), MasteryRules.cooldownSeconds(m), rest > 0 ? " (resting " + rest + " s)" : "");
    }
}
