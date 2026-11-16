package com.newuniverse.nusmp.skill;

import com.newuniverse.nusmp.NUConfig;
import com.newuniverse.nusmp.balance.BalanceLaw;
import com.newuniverse.nusmp.skill.rules.AuraRules;
import com.newuniverse.nusmp.skill.rules.FireForceRules;
import com.newuniverse.nusmp.skill.rules.ShinraRules;
import com.newuniverse.nusmp.skill.rules.Tier;
import com.newuniverse.nusmp.vfx.VfxShape;
import io.github.manasmods.manascore.network.api.util.Changeable;
import io.github.manasmods.manascore.skill.api.ManasSkillInstance;
import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;

import java.util.List;

/**
 * Canon pass (setting {@code shinraCanon}, docs/shinra_rework.md, from the Fire Force wiki): Rapid (a focused flame jet: faster for less fire; the Rapid-Man Kick is a kick while it burns), Hellfire (Corna: a
 * demon's head of dark flame that bites and explodes), Adolla Link (senses hostile minds, and is the door to Adolla Grace), Adolla Grace (the Ultimate only: a short Fourth Generation boost, then the
 * Ignition Limit) and Hysterical Strength (under a third of health: blue flames, more speed and strength). The Devil's Grin is only a look; the Devil's Footprints are the trail of the Ignition Dash.
 * Unlock order: BASE has the Ignition Dash, Devil's Kick, Rapid, the Rapid-Man Kick and the Adolla Link; MASTERED adds Hellfire and Hysterical Strength; the Ultimate adds Light Speed, the Adolla Burst blast and Adolla Grace.
 * <p>
 * Shinra Kusakabe (Fire Force), id {@code hell_walker} (docs/shinra_rework.md). The skill is the main character: a wheel of actives and one ON/OFF switch for the passives.
 * <ul>
 *   <li>Wheel: Ignition Dash and Devil's Kick (the old Devil's Footprints, folded in: {@link DevilsFootprintsSkill#dash}/{@link DevilsFootprintsSkill#kick}) and the Rapid-Man Kick (a short ignition dash
 *       that kicks and ignites everything on its line). Its Ultimate, {@link AdollaBurstSkill}, swaps the dash for Light Speed and adds the Adolla Burst blast.</li>
 *   <li>Always on: your own flames cannot burn you (fire damage is cancelled), as Devil's Footprints had it.</li>
 *   <li>Switch on (Hell Mode, 60 magicules a second, the old Hell Walker): the lower your health the harder you hit; under a third of health the Devil's Grin (Speed I); a mastered
 *       skill under a quarter of health is in Adolla Link (Speed II, Strength I, moves recharge 40 % faster).</li>
 * </ul>
 * Hell Conqueror (the old Ultimate of this skill) extends this class and keeps its own hellfire aura. The SERVER setting {@code shinraRework=false} restores the old behaviour: no wheel, no fire
 * immunity, the plain toggle. Numbers: {@link AuraRules} (the damage curve), {@link FireForceRules} (dash, kick, burst), {@link ShinraRules}. No state is kept per player outside the skill instance,
 * so nothing has to be cleared on logout.
 */
public class HellWalkerSkill extends ToggleAuraSkill {
    static final SkillText TEXT = SkillText.of("hell_walker", "Shinra Kusakabe",
            "Unique. Shinra Kusakabe of Fire Force, a Third Generation pyrokinetic who ignites his feet. Wheel: Ignition Dash (the Devil's Footprints trail), Devil's Kick, Rapid-Man Kick, Rapid, Adolla Link, and once mastered Hellfire. Your own flames cannot burn you. Toggle Hell Mode: the closer you are to death the harder you hit, and under a third of health Hysterical Strength burns blue.");

    public HellWalkerSkill() { this(SkillType.UNIQUE, TEXT, false); }

    protected HellWalkerSkill(SkillType type, SkillText text, boolean ultimate) { super(type, text, ultimate); }

    /** True for Adolla Burst: Light Speed, Devil's Kick, Adolla Burst, Rapid-Man Kick. Everything else (this skill, Hell Conqueror) has the three-move wheel. */
    protected boolean fullWheel() { return false; }

    /** True when the Shinra wheel and fire immunity are on: the SERVER setting {@code shinraRework}. Adolla Burst overrides it to stay on (its wheel is the old one with the setting off). */
    protected boolean wheelOn() { return NUConfig.shinraRework(); }

    /** True while the Fire Force canon pass is on (the SERVER setting {@code shinraCanon}, which needs {@code shinraRework}). */
    protected final boolean canon() { return wheelOn() && NUConfig.shinraCanon(); }

    static final String RAPID_UNTIL = "shinraRapidUntil", LINK_UNTIL = "shinraLinkUntil", GRACE_UNTIL = "shinraGraceUntil";

    /** The blue of Hysterical Strength (the wiki: the flames turn blue and take a claw shape); the normal ignition is orange. */
    static final int HYSTERICAL_BLUE = 0xFF4AA8FF, IGNITION_ORANGE = 0xFFFF5A1E;

    private static boolean hysteric(ManasSkillInstance i, ServerPlayer p, Tier t) { return NUConfig.shinraCanon() && i.isToggled() && ShinraRules.hysteria(p.getHealth(), p.getMaxHealth(), t); }
    private static int hystColor(ManasSkillInstance i, ServerPlayer p, Tier t) { return hysteric(i, p, t) ? HYSTERICAL_BLUE : IGNITION_ORANGE; }

    private static long now(ServerPlayer p) { return p.level().getGameTime(); }
    /** Rapid is burning (a timestamp in the skill instance, so nothing is kept per player elsewhere). */
    protected static boolean rapidOn(ManasSkillInstance i, ServerPlayer p) { return ShinraRules.rapidActive(now(p), i.getOrCreateTag().getLong(RAPID_UNTIL)); }
    protected static boolean graceOn(ManasSkillInstance i, ServerPlayer p) { return ShinraRules.graceActive(now(p), i.getOrCreateTag().getLong(GRACE_UNTIL)); }

    @Override protected int interval() { return AuraRules.HELL_INTERVAL; }
    @Override protected double drainPerPulse(Tier tier) { return AuraRules.HELL_DRAIN; }

    // ------------------------------------------------------------------ the wheel
    @Override public int getModes(ManasSkillInstance i) { return wheelOn() ? ShinraRules.wheel(fullWheel(), canon()).length : super.getModes(i); }

    @Override
    public int nextMode(LivingEntity e, ManasSkillInstance i, int mode, boolean reverse) {
        if (!wheelOn()) return super.nextMode(e, i, mode, reverse);
        int count = getModes(i);
        int next = mode;
        for (int k = 0; k < count; k++) {                      // skip the moves this tier has not learned yet
            next = Math.floorMod(next + (reverse ? -1 : 1), count);
            if (!canon() || ShinraRules.unlocked(ShinraRules.move(fullWheel(), true, next), tier(i, e))) return next;
        }
        return mode;
    }

    @Override
    public String getModeId(ManasSkillInstance i, int mode) {
        if (!wheelOn()) return super.getModeId(i, mode);
        return switch (ShinraRules.move(fullWheel(), canon(), mode)) {
            case ShinraRules.DASH -> fullWheel() ? "fireforce.light_speed" : "fireforce.dash";
            case ShinraRules.KICK -> "fireforce.kick";
            case ShinraRules.BURST -> "fireforce.burst";
            case ShinraRules.RAPID_JET -> "shinra.rapid_jet";
            case ShinraRules.HELLFIRE -> "shinra.hellfire";
            case ShinraRules.LINK -> "shinra.link";
            case ShinraRules.GRACE -> "shinra.grace";
            default -> "shinra.rapid";
        };
    }

    @Override
    public Component getModeName(ManasSkillInstance i, int mode) {
        if (!wheelOn()) return super.getModeName(i, mode);
        return switch (ShinraRules.move(fullWheel(), canon(), mode)) {
            case ShinraRules.DASH -> fullWheel() ? SkillMsg.mode("fireforce.light_speed", "Light Speed") : SkillMsg.mode("fireforce.dash", "Ignition Dash");
            case ShinraRules.KICK -> SkillMsg.mode("fireforce.kick", "Devil's Kick");
            case ShinraRules.BURST -> SkillMsg.mode("fireforce.burst", "Adolla Burst");
            case ShinraRules.RAPID_JET -> SkillMsg.mode("shinra.rapid_jet", "Rapid");
            case ShinraRules.HELLFIRE -> SkillMsg.mode("shinra.hellfire", "Hellfire");
            case ShinraRules.LINK -> SkillMsg.mode("shinra.link", "Adolla Link");
            case ShinraRules.GRACE -> SkillMsg.mode("shinra.grace", "Adolla Grace");
            default -> SkillMsg.mode("shinra.rapid", "Rapid-Man Kick");
        };
    }

    /** The cooldown of a move, shortened while Adolla Link burns (only with Hell Mode on). */
    protected final void moveCooldown(ManasSkillInstance instance, ServerPlayer player, int mode, int baseTicks) {
        Tier tier = tier(instance, player);
        double f = canon() ? (graceOn(instance, player) ? ShinraRules.graceCooldownFactor() : 1.0)
                : instance.isToggled() ? ShinraRules.cooldownFactor(player.getHealth(), player.getMaxHealth(), tier) : 1.0;
        cooldown(instance, mode, (int) Math.round(DMUtil.cooldown(baseTicks) * f));
    }

    @Override
    public void onPressed(ManasSkillInstance instance, LivingEntity entity, int key, int mode) {
        if (!wheelOn() || !(entity instanceof ServerPlayer player) || !player.isAlive()) return;
        int move = ShinraRules.move(fullWheel(), canon(), mode);
        int slot = Math.floorMod(mode, ShinraRules.wheel(fullWheel(), canon()).length);
        if (instance.onCoolDown(slot)) { SkillUtil.fail(player, SkillMsg.t("fireforce.cooldown", "Your flames need a moment.")); return; }
        Tier tier = tier(instance, player);
        if (canon() && !ShinraRules.unlocked(move, tier)) { SkillUtil.fail(player, SkillMsg.t("shinra.locked", "You have not learned this yet: master the skill first.")); return; }
        boolean rapid = canon() && rapidOn(instance, player);                      // "less of his fire" while Rapid burns
        double fire = rapid ? ShinraRules.rapidFireFactor() : 1.0;

        if (move == ShinraRules.DASH) {
            if (!pay(instance, player, slot, DMUtil.cost(FireForceRules.dashCost(tier)) * fire)) return;
            DevilsFootprintsSkill.dash(player, tier);
            if (rapid) player.setDeltaMovement(player.getDeltaMovement().scale(ShinraRules.rapidDashFactor()));
            moveCooldown(instance, player, slot, FireForceRules.dashCooldown(tier));
        } else if (move == ShinraRules.KICK) {
            if (!pay(instance, player, slot, DMUtil.cost(FireForceRules.kickCost(tier)))) return;
            DevilsFootprintsSkill.kick(instance, player, tier);
            if (hysteric(instance, player, tier)) SkillAnimeFx2.devilKick(player, FireForceRules.kickRange(tier), HYSTERICAL_BLUE);      // the claw flames turn blue
            moveCooldown(instance, player, slot, FireForceRules.kickCooldown(tier));
        } else if (move == ShinraRules.RAPID) {
            if (!pay(instance, player, slot, DMUtil.cost(ShinraRules.rapidCost(tier)) * fire)) return;
            rapid(instance, player, tier, rapid, hystColor(instance, player, tier));
            moveCooldown(instance, player, slot, ShinraRules.rapidCooldown(tier));
        } else if (move == ShinraRules.RAPID_JET) {
            if (!pay(instance, player, slot, DMUtil.cost(ShinraRules.rapidJetCost(tier)))) return;
            rapidJet(instance, player, tier);
            moveCooldown(instance, player, slot, ShinraRules.rapidJetCooldown(tier));
        } else if (move == ShinraRules.HELLFIRE) {
            if (!pay(instance, player, slot, DMUtil.cost(ShinraRules.hellfireCost(tier)))) return;
            corna(instance, player, tier);
            moveCooldown(instance, player, slot, ShinraRules.hellfireCooldown(tier));
        } else if (move == ShinraRules.LINK) {
            if (!pay(instance, player, slot, DMUtil.cost(ShinraRules.linkCost(tier)))) return;
            adollaLink(instance, player, tier, false);
            moveCooldown(instance, player, slot, ShinraRules.linkCooldown());
        } else if (move == ShinraRules.GRACE) {
            if (!ShinraRules.linkOpen(now(player), instance.getOrCreateTag().getLong(LINK_UNTIL))) {
                SkillUtil.fail(player, SkillMsg.t("shinra.grace.nolink", "Open an Adolla Link first: the Grace comes through the Link."));
                return;
            }
            if (!pay(instance, player, slot, DMUtil.cost(ShinraRules.graceCost()))) return;
            moveCooldown(instance, player, slot, ShinraRules.graceCooldown());
            adollaGrace(instance, player, tier);
        } else {
            return;
        }
        SkillUtil.castVfx(player, 0xFFFF5A1E);
        grow(instance, player, 1);
    }

    /** Rapid (the Tora Hishigi seal): the flame becomes one jet. A burst of speed for a few seconds, and while it burns the dash and the Rapid-Man Kick cost half and reach farther. One potion effect. */
    private static void rapidJet(ManasSkillInstance instance, ServerPlayer player, Tier tier) {
        int ticks = ShinraRules.rapidJetTicks(tier);
        instance.getOrCreateTag().putLong(RAPID_UNTIL, now(player) + ticks);
        player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, ticks, ShinraRules.rapidJetSpeedAmp(tier), true, false));
        ServerLevel level = player.serverLevel();
        Vec3 from = player.position().add(0, 0.2, 0);
        SkillFx.shape(level, VfxShape.FLAME_TRAIL, from, from.add(player.getViewVector(1.0F).scale(-4.0)), hystColor(instance, player, tier), 14, 0.5f);     // one narrow jet, not the usual spread
        SkillFx.sound(level, player.position(), SoundEvents.FIRECHARGE_USE, 1.0F, 1.6F);
        SkillUtil.actionbar(player, SkillMsg.t("shinra.rapid_on", "Rapid: one jet of flame.").withStyle(ChatFormatting.GOLD));
    }

    /** Hellfire (Corna): a demon's head of dark flame bites the line in front (jaws), then explodes at its end. At most {@link ShinraRules#maxTargets()} foes bitten and as many in the blast: two queries. */
    private static void corna(ManasSkillInstance instance, ServerPlayer player, Tier tier) {
        ServerLevel level = player.serverLevel();
        Vec3 look = player.getViewVector(1.0F);
        Vec3 a = player.position().add(0, 1, 0);
        Vec3 b = a.add(look.scale(ShinraRules.hellfireRange(tier)));
        int hit = 0;
        for (LivingEntity t : SkillTargets.foesAlong(player, a, b, ShinraRules.hellfireWidth())) {
            if (hit >= ShinraRules.maxTargets()) break;
            if (SkillDamage.hit(player, t, ShinraRules.hellfireDamage(tier), masteryFrac(instance), instance) > 0f) {
                t.igniteForSeconds(ShinraRules.hellfireBurnSeconds(tier));
                hit++;
            }
        }
        int blast = 0;
        for (LivingEntity t : SkillTargets.foes(player, b, ShinraRules.hellfireBlastRadius(tier))) {
            if (blast >= ShinraRules.maxTargets()) break;
            if (SkillDamage.hit(player, t, ShinraRules.hellfireBlastDamage(tier), masteryFrac(instance), SkillDamage.Kind.MAGIC, instance) > 0f) {
                t.knockback(1.2, b.x - t.getX(), b.z - t.getZ());
                blast++;
            }
        }
        SkillFx.shape(level, VfxShape.FLAME_TRAIL, a, b, 0xFF4A0A0A, 16, 1.6f);
        SkillFx.shape(level, VfxShape.FIRE_WILD, b, b.add(0, 1, 0), 0xFF8A0A0A, 24, (float) ShinraRules.hellfireBlastRadius(tier));
        SkillAnimeFx.sigil(level, SkillAnimeFx.HELL, b, ShinraRules.hellfireBlastRadius(tier), 0xFF2A0A0A, 24);
        SkillFx.sound(level, b, SoundEvents.GENERIC_EXPLODE.value(), 1.0F, 0.8F);
    }

    /** The Adolla Link: you sense the hostile minds around you (they glow for a few seconds; players are not marked) and the Link stays open, which Adolla Grace needs. One entity query, at most {@link ShinraRules#linkMaxSensed()} marks. */
    protected static void adollaLink(ManasSkillInstance instance, ServerPlayer player, Tier tier, boolean passive) {
        int glow = passive ? ShinraRules.passiveLinkGlowTicks() : ShinraRules.linkGlowTicks();
        int sensed = 0;
        for (LivingEntity t : SkillTargets.foes(player, player.position(), ShinraRules.linkRadius(tier))) {
            if (sensed >= ShinraRules.linkMaxSensed()) break;
            if (t instanceof ServerPlayer) continue;
            t.addEffect(new MobEffectInstance(MobEffects.GLOWING, glow, 0, true, false));
            sensed++;
        }
        instance.getOrCreateTag().putLong(LINK_UNTIL, now(player) + (passive ? ShinraRules.passiveLinkWindowTicks() : ShinraRules.linkWindowTicks()));
        if (!passive) SkillUtil.actionbar(player, SkillMsg.t("shinra.link.sensed", "Adolla Link: you sense %s hostile minds.", sensed).withStyle(ChatFormatting.GOLD));
        SkillFx.spray(player.serverLevel(), true, player.position().add(0, 1, 0), 6, 0.4, 0.6, 0.4, 0.02);
    }

    /** Adolla Grace (Fourth Generation), only through an open Link: a short boost of speed, strength and resistance, every move recharging faster, Light Speed hitting harder. When it ends the Ignition Limit hits (see {@link #onTick}). */
    private static void adollaGrace(ManasSkillInstance instance, ServerPlayer player, Tier tier) {
        int ticks = ShinraRules.graceTicks(tier);
        instance.getOrCreateTag().putLong(GRACE_UNTIL, now(player) + ticks);
        player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, ticks, ShinraRules.graceSpeedAmp(), true, false));
        player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, ticks, ShinraRules.graceStrengthAmp(), true, false));
        player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, ticks, ShinraRules.graceResistanceAmp(), true, false));
        ServerLevel level = player.serverLevel();
        SkillFx.sound(level, player.position(), SoundEvents.BEACON_ACTIVATE, 1.0F, 1.4F);
        SkillAnimeFx.sigil(level, SkillAnimeFx.FIRE_FORCE, player.position(), 3.5, 0xFF6AD8FF, 30);
        SkillUtil.actionbar(player, SkillMsg.t("shinra.grace.on", "Adolla Grace: the Fourth Generation burns, for a moment.").withStyle(ChatFormatting.AQUA));
    }

    /** The Grace ended: the Ignition Limit. Overheat leaves Shinra weak, slow and drained for a few seconds. One long read per tick; the mark is cleared at once. */
    @Override
    public void onTick(ManasSkillInstance instance, LivingEntity living) {
        super.onTick(instance, living);
        if (!(living instanceof ServerPlayer player) || !canon()) return;
        CompoundTag tag = instance.getOrCreateTag();
        long until = tag.getLong(GRACE_UNTIL);
        long t = now(player);
        if (!ShinraRules.graceEnded(t, until)) return;
        tag.remove(GRACE_UNTIL);
        instance.markDirty();
        if (!ShinraRules.overheatDue(t, until) || !player.isAlive()) return;
        int ticks = ShinraRules.overheatTicks();
        player.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, ticks, 1, true, false));
        player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, ticks, 0, true, false));
        player.addEffect(new MobEffectInstance(MobEffects.HUNGER, ticks, 1, true, false));
        SkillUtil.actionbar(player, SkillMsg.t("shinra.overheat", "Ignition Limit: you overheat.").withStyle(ChatFormatting.RED));
    }

    /** The Rapid-Man Kick (the wiki: a kick while using Rapid; the rest is design): rocket forward along your sight line, kicking and igniting every foe on the way, harder and farther while Rapid burns (at most {@link ShinraRules#maxTargets()}, one query). */
    protected static void rapid(ManasSkillInstance instance, ServerPlayer player, Tier tier, boolean whileRapid, int argb) {
        ServerLevel level = player.serverLevel();
        Vec3 look = player.getViewVector(1.0F);
        Vec3 a = player.position().add(0, 1, 0);
        double range = ShinraRules.rapidRange(tier) * (whileRapid ? ShinraRules.rapidKickRangeFactor() : 1.0);
        double damage = ShinraRules.rapidDamage(tier) * (whileRapid ? ShinraRules.rapidKickDamageFactor() : 1.0);   // the kick of a Rapid-boosted speed hits harder
        Vec3 b = a.add(look.scale(range));
        int hit = 0;
        for (LivingEntity t : SkillTargets.foesAlong(player, a, b, ShinraRules.rapidWidth())) {
            if (hit >= ShinraRules.maxTargets()) break;
            if (SkillDamage.hit(player, t, damage, masteryFrac(instance), instance) > 0f) {
                t.igniteForSeconds(ShinraRules.rapidBurnSeconds(tier));
                t.knockback(0.8, -look.x, -look.z);
                hit++;
            }
        }
        double p = ShinraRules.rapidPower(tier);
        player.setDeltaMovement(look.x * p, Math.max(0.2, look.y * p), look.z * p);
        player.hurtMarked = true;
        player.fallDistance = 0;
        SkillFx.shape(level, VfxShape.FLAME_TRAIL, a, a.add(look.scale(-6.0)), 0xFFFF5A1E, 16, 1.0f);
        SkillFx.spray(level, false, player.position().add(0, 0.2, 0), 10, 0.2, 0.1, 0.2, 0.08);
        SkillFx.sound(level, player.position(), SoundEvents.FIRECHARGE_USE, 1.0F, 1.2F);
        SkillAnimeFx2.devilKick(player, range, argb);
    }

    /** Shinra's own flames do not burn him (the old Devil's Footprints passive); the setting off keeps the old Hell Walker, which did not have it. */
    @Override
    public boolean onTakenDamage(ManasSkillInstance instance, LivingEntity owner, DamageSource source, Changeable<Float> amount) {
        if (wheelOn() && FireForceRules.fireImmune(true) && source.is(DamageTypeTags.IS_FIRE)) amount.set(0.0F);
        return true;
    }

    // ------------------------------------------------------------------ Hell Mode (the toggle)
    @Override
    protected void pulse(ManasSkillInstance instance, ServerPlayer player, Tier tier) {
        double hp = player.getHealth(), max = player.getMaxHealth();
        if (hp < max * 0.5F) {
            SkillFx.spray(player.serverLevel(), true, player.position().add(0, 0.2, 0), 6, 0.3, 0.1, 0.3, 0.01);
        }
        if (!NUConfig.shinraRework()) return;
        if (canon()) {
            int sp = ShinraRules.hysteriaSpeedAmp(hp, max, tier);                 // Hysterical Strength: the Pressure of Death, 100 % of the ignition, blue flames
            if (sp >= 0) {
                player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, ShinraRules.EFFECT_TICKS, sp, true, false));
                Vec3 at = player.position();
                SkillFx.shape(player.serverLevel(), VfxShape.FIRE_BURST, at, at.add(0, 1, 0), HYSTERICAL_BLUE, 14, 0.9f);
            }
            int st = ShinraRules.hysteriaStrengthAmp(hp, max, tier);
            if (st >= 0) player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, ShinraRules.EFFECT_TICKS, st, true, false));
            if (ShinraRules.involuntaryLink(hp, max)) adollaLink(instance, player, tier, true);   // strong emotion opens the Link by itself
            return;
        }
        int speed = ShinraRules.speedAmp(hp, max, tier);          // the Devil's Grin / Adolla Link
        if (speed >= 0) {
            player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, ShinraRules.EFFECT_TICKS, speed, true, false));
            SkillFx.spray(player.serverLevel(), false, player.position().add(0, 0.1, 0), 4, 0.3, 0.05, 0.3, 0.05);
        }
        int strength = ShinraRules.strengthAmp(hp, max, tier);
        if (strength >= 0) player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, ShinraRules.EFFECT_TICKS, strength, true, false));
    }

    @Override
    protected void turnedOn(ManasSkillInstance instance, ServerPlayer player) {
        SkillFx.sound(player.serverLevel(), player.position(), SoundEvents.WITHER_AMBIENT, 0.6F, 0.6F);
        SkillFx.spray(player.serverLevel(), true, player.position().add(0, 1, 0), 12, 0.4, 0.6, 0.4, 0.02);
        SkillAnimeFx.sigil(player.serverLevel(), SkillAnimeFx.HELL, player.position(), 2.5, 0xFF8A0A0A, 30);      // VFX wave 3: the ground cracks into hell
    }

    @Override
    public boolean onDamageEntity(ManasSkillInstance instance, LivingEntity owner, LivingEntity target, DamageSource source, Changeable<Float> amount) {
        if (!instance.isToggled() || owner.getMaxHealth() <= 0 || amount.get() <= 0) return true;
        Tier tier = tier(instance, owner);
        float dealt = (float) (amount.get() * AuraRules.hellMultiplier(owner.getHealth(), owner.getMaxHealth(), tier));
        amount.set(dealt);
        double steal = AuraRules.hellLifesteal(owner.getHealth(), owner.getMaxHealth(), tier);
        if (steal > 0) BalanceLaw.heal(owner, (float) (dealt * steal));
        return true;
    }

    /** Adolla Burst uses it: the hellfire aura and the berserk of the old Hell Conqueror, now part of its Adolla Link. At most one target query a second. */
    protected static void hellfire(ManasSkillInstance instance, ServerPlayer player, Tier tier) {
        double hp = player.getHealth(), max = player.getMaxHealth();
        if (AuraRules.hellfire(hp, max, tier)) {
            List<LivingEntity> foes = SkillTargets.foes(player, player.position(), AuraRules.hellfireRadius());
            for (LivingEntity foe : foes) {
                SkillDamage.hit(player, foe, AuraRules.hellfireDamage(), masteryFrac(instance), SkillDamage.Kind.MAGIC, instance);
                foe.igniteForSeconds(3);
            }
            Vec3 at = player.position();
            SkillFx.shape(player.serverLevel(), VfxShape.FIRE_WILD, at, at.add(0, 1, 0), 0xFFFF2A1E, 20, (float) AuraRules.hellfireRadius());
        }
        if (AuraRules.berserk(hp, max, tier)) {
            player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 60, 1, true, false));
            player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, 60, 1, true, false));
        }
    }
}
