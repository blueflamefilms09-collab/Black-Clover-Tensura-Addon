package com.newuniverse.nusmp.skill;

import com.newuniverse.nusmp.NUConfig;
import com.newuniverse.nusmp.skill.rules.ControlRules;
import com.newuniverse.nusmp.skill.rules.KiRules;
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
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

import java.util.List;

/**
 * Ki (Dragon Ball), an Ultimate with ten modes (3.47.0, new; see {@link KiRules} for the numbers and the reasoning). Ki Blast is the quick shot and the Kamehameha a beam through six; Instant
 * Transmission is a blink of up to 32 blocks, Solar Flare blinds what faces you, Afterimage lets the next blow pass through you (it does a fifth); Super Saiyan (Strength II, Speed I, a hit does
 * 0.85), Kaioken (Strength III, Speed II, paid for with 15% of your maximum health and a quarter more damage taken) and Ultra Instinct (one blow a second dodged outright, the rest do half) are the forms;
 * Spirit Bomb grows with the living allies lending it energy and Ki Sense shows every hostile presence near.
 * All state is the game time each window ends at, kept in the skill tag of the one player who owns it: nothing ticks, nothing is cleaned up, nothing is shared. A press looks at no more than 24
 * creatures, hits at most 24 (Spirit Bomb, 90-second cooldown) and draws one beam or one ring.
 * Switch: server setting {@code kiEnabled}. Obtain: {@code /tensura edit <player> ability grant nusmp:ki}.
 */
public class KiSkill extends NUSkill {
    static final SkillText TEXT = SkillText.of("ki", "Ki",
            "Ultimate. Dragon Ball's ki in ten modes: Ki Blast, Kamehameha, Instant Transmission, Solar Flare, Afterimage, Super Saiyan, Spirit Bomb, Kaioken, Ultra Instinct and Ki Sense.");

    /** The cast colour of each mode (ARGB). */
    private static final int[] COLORS = {0xFF8AD8FF, 0xFF40B8FF, 0xFFE8F4FF, 0xFFFFF8A0, 0xFFB0C8FF, 0xFFFFD020, 0xFF60E0FF, 0xFFFF3030, 0xFFD8E8FF, 0xFF8AFFC0};

    public KiSkill() { super(SkillType.ULTIMATE, TEXT, true); }

    @Override public int getModes(ManasSkillInstance i) { return KiRules.MODES; }
    @Override public int nextMode(LivingEntity e, ManasSkillInstance i, int mode, boolean reverse) { return Math.floorMod(mode + (reverse ? -1 : 1), KiRules.MODES); }
    @Override public String getModeId(ManasSkillInstance i, int mode) { return "ki." + KiRules.IDS[Math.floorMod(mode, KiRules.MODES)]; }

    @Override
    public Component getModeName(ManasSkillInstance i, int mode) {
        return switch (Math.floorMod(mode, KiRules.MODES)) {
            case KiRules.BLAST -> SkillMsg.mode("ki.blast", "Ki Blast");
            case KiRules.KAME -> SkillMsg.mode("ki.kame", "Kamehameha");
            case KiRules.TELEPORT -> SkillMsg.mode("ki.teleport", "Instant Transmission");
            case KiRules.FLARE -> SkillMsg.mode("ki.flare", "Solar Flare");
            case KiRules.AFTERIMAGE -> SkillMsg.mode("ki.afterimage", "Afterimage");
            case KiRules.SSJ -> SkillMsg.mode("ki.ssj", "Super Saiyan");
            case KiRules.SPIRIT -> SkillMsg.mode("ki.spirit", "Spirit Bomb");
            case KiRules.KAIOKEN -> SkillMsg.mode("ki.kaioken", "Kaioken");
            case KiRules.UI -> SkillMsg.mode("ki.ui", "Ultra Instinct");
            default -> SkillMsg.mode("ki.sense", "Ki Sense");
        };
    }

    private static boolean open(CompoundTag tag, int mode, long now) { return KiRules.active(tag.getLong(KiRules.untilKey(mode)), now); }

    // ---------------------------------------------------------------- the press
    @Override
    public void onPressed(ManasSkillInstance instance, LivingEntity entity, int key, int mode) {
        if (!(entity instanceof ServerPlayer p) || !p.isAlive() || !KiRules.valid(mode)) return;
        if (!NUConfig.KI_ENABLED.get()) { SkillUtil.fail(p, SkillMsg.t("ki.off", "Ki is switched off on this server.")); return; }
        CompoundTag tag = instance.getOrCreateTag();
        long now = p.level().getGameTime();
        if (KiRules.cancellable(mode) && open(tag, mode, now)) { end(instance, p, tag, mode); return; }   // a second press ends the form (its cooldown is already running)
        if (instance.onCoolDown(mode)) { SkillUtil.fail(p, SkillMsg.t("ki.cooldown", "Your ki needs a moment.")); return; }
        boolean done = switch (mode) {
            case KiRules.BLAST -> blast(instance, p);
            case KiRules.KAME -> kame(instance, p);
            case KiRules.TELEPORT -> teleport(p);
            case KiRules.FLARE -> flare(p);
            case KiRules.AFTERIMAGE -> afterimage(p, tag, now);
            case KiRules.SSJ -> ssj(p, tag, now);
            case KiRules.SPIRIT -> spirit(instance, p);
            case KiRules.KAIOKEN -> kaioken(p, tag, now);
            case KiRules.UI -> ultraInstinct(p, tag, now);
            default -> sense(p);
        };
        if (!done) return;
        cooldown(instance, mode, DMUtil.cooldown(KiRules.cooldown(mode)));
        SkillFx.sound(p.serverLevel(), p.position(), SoundEvents.BEACON_ACTIVATE, 0.6F, 1.6F);
        SkillUtil.castVfx(p, COLORS[mode]);
        grow(instance, p, 1);
    }

    /** Ki Blast: a quick ball along your look; the first creature in its path is hit. */
    private boolean blast(ManasSkillInstance instance, ServerPlayer p) {
        if (!SkillUtil.spendMagicules(p, DMUtil.cost(KiRules.cost(KiRules.BLAST)))) return false;
        Vec3 from = p.getEyePosition(), to = DMUtil.lookPoint(p, KiRules.blastRange());
        for (LivingEntity t : SkillTargets.foesAlong(p, from, to, KiRules.blastWidth())) {
            if (SkillDamage.hit(p, t, KiRules.blastDamage(), masteryFrac(instance), SkillDamage.Kind.MAGIC) > 0f) { to = t.position().add(0, t.getBbHeight() * 0.5, 0); break; }
        }
        SkillFx.beam(p.serverLevel(), from, to, false);
        SkillFx.burst(p.serverLevel(), to, false, 8, 0.3);
        return true;
    }

    /** Kamehameha: a beam 28 blocks long through up to six creatures. */
    private boolean kame(ManasSkillInstance instance, ServerPlayer p) {
        if (!SkillUtil.spendMagicules(p, DMUtil.cost(KiRules.cost(KiRules.KAME)))) return false;
        ServerLevel level = p.serverLevel();
        Vec3 from = p.getEyePosition(), to = DMUtil.lookPoint(p, KiRules.kameRange());
        SkillFx.beam(level, from, to, false);
        SkillFx.shape(level, com.newuniverse.nusmp.vfx.VfxShape.MAGIC_CIRCLE_EXPLOSION, from, to, 0xFF40B8FF, 20, 0.9f);
        int hits = 0;
        for (LivingEntity t : SkillTargets.foesAlong(p, from, to, KiRules.kameWidth())) {
            if (hits >= KiRules.kamePierce()) break;
            if (SkillDamage.hit(p, t, KiRules.kameDamage(), masteryFrac(instance), SkillDamage.Kind.MAGIC) > 0f) hits++;
        }
        return true;
    }

    /** Instant Transmission: a blink along your look to the nearest safe spot (the shared helper checks room, ground and lava); a spot closer than a block and a half is no blink and costs nothing. */
    private boolean teleport(ServerPlayer p) {
        double cost = DMUtil.cost(KiRules.cost(KiRules.TELEPORT));
        if (!SkillUtil.spendMagicules(p, cost)) return false;
        Vec3 from = p.position();
        boolean moved = com.newuniverse.nusmp.relics.Fx.teleportLook(p, KiRules.teleportRange());
        if (!moved || !KiRules.teleportOk(from.distanceTo(p.position()))) {
            if (moved) p.teleportTo(from.x, from.y, from.z);
            SkillUtil.refund(p, cost);
            SkillUtil.fail(p, SkillMsg.t("ki.no_room", "No room to appear."));
            return false;
        }
        ServerLevel level = p.serverLevel();
        SkillFx.burst(level, from.add(0, 1, 0), false, 10, 0.4);
        SkillFx.burst(level, p.position().add(0, 1, 0), false, 10, 0.4);
        return true;
    }

    /** Solar Flare: every foe in the cone in front of you is blinded (a boss for two fifths as long, a player for half as long) and slowed. */
    private boolean flare(ServerPlayer p) {
        if (!SkillUtil.spendMagicules(p, DMUtil.cost(KiRules.cost(KiRules.FLARE)))) return false;
        for (LivingEntity t : SkillTargets.foesInCone(p, KiRules.flareRange(), KiRules.flareCone())) {
            boolean boss = GodTier.isBoss(t), player = t instanceof Player;
            t.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, ControlRules.controlTicks(KiRules.flareBlindTicks(), boss, player), 0));
            t.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, ControlRules.controlTicks(KiRules.flareSlowTicks(), boss, player), 1));
        }
        SkillFx.burst(p.serverLevel(), p.getEyePosition().add(p.getViewVector(1f).scale(1.5)), false, 20, 0.8);
        SkillUtil.actionbar(p, SkillMsg.t("ki.flare", "Solar Flare!").withStyle(ChatFormatting.YELLOW));
        return true;
    }

    private boolean afterimage(ServerPlayer p, CompoundTag tag, long now) {
        if (!SkillUtil.spendMagicules(p, DMUtil.cost(KiRules.cost(KiRules.AFTERIMAGE)))) return false;
        tag.putLong(KiRules.untilKey(KiRules.AFTERIMAGE), KiRules.endsAt(now, KiRules.AFTERIMAGE));
        SkillFx.burst(p.serverLevel(), p.position().add(0, 1, 0), false, 12, 0.5);
        SkillUtil.actionbar(p, SkillMsg.t("ki.afterimage", "Afterimage: the next blow will find only an image.").withStyle(ChatFormatting.AQUA));
        return true;
    }

    private boolean ssj(ServerPlayer p, CompoundTag tag, long now) {
        if (!SkillUtil.spendMagicules(p, DMUtil.cost(KiRules.cost(KiRules.SSJ)))) return false;
        int ticks = KiRules.duration(KiRules.SSJ);
        tag.putLong(KiRules.untilKey(KiRules.SSJ), KiRules.endsAt(now, KiRules.SSJ));
        p.addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, ticks, KiRules.ssjStrengthAmp()));
        p.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, ticks, KiRules.ssjSpeedAmp()));
        SkillFx.aura(p.serverLevel(), p, false);
        SkillUtil.actionbar(p, SkillMsg.t("ki.ssj", "Your hair turns gold: Super Saiyan!").withStyle(ChatFormatting.GOLD));
        return true;
    }

    /** Spirit Bomb: grows with the living allies around you (at most eight) lending their energy, and falls where you look. */
    private boolean spirit(ManasSkillInstance instance, ServerPlayer p) {
        if (!SkillUtil.spendMagicules(p, DMUtil.cost(KiRules.cost(KiRules.SPIRIT)))) return false;
        int lenders = 0;
        for (LivingEntity a : SkillTargets.alliesAround(p, p.position(), KiRules.spiritLendRadius())) {
            if (a != p && (a.isAlliedTo(p) || !(a instanceof Player o) || !p.canHarmPlayer(o))) lenders++;
        }
        double damage = KiRules.spiritDamage(lenders);
        ServerLevel level = p.serverLevel();
        Vec3 centre = DMUtil.lookPoint(p, KiRules.spiritReach());
        SkillFx.ring(level, centre.add(0, 0.2, 0), KiRules.spiritRadius(), 0x60E0FF);
        SkillFx.shape(level, com.newuniverse.nusmp.vfx.VfxShape.MAGIC_CIRCLE_EXPLOSION, centre.add(0, 1, 0), centre, 0xFF60E0FF, 24, 1.1f);
        for (LivingEntity t : SkillTargets.foes(p, centre, KiRules.spiritRadius())) {
            SkillDamage.hit(p, t, damage, masteryFrac(instance), SkillDamage.Kind.MAGIC);
        }
        SkillUtil.actionbar(p, SkillMsg.t("ki.spirit", "Spirit Bomb: %s lent their energy.", lenders).withStyle(ChatFormatting.AQUA));
        return true;
    }

    /** Kaioken: 15% of your maximum health (never to the last heart) buys Strength III and Speed II for ten seconds, and every hit does a quarter more. */
    private boolean kaioken(ServerPlayer p, CompoundTag tag, long now) {
        if (!KiRules.kaiokenAllowed(p.getHealth())) { SkillUtil.fail(p, SkillMsg.t("ki.too_weak", "You are too hurt for Kaioken.")); return false; }
        if (!SkillUtil.spendMagicules(p, DMUtil.cost(KiRules.cost(KiRules.KAIOKEN)))) return false;
        p.setHealth(p.getHealth() - KiRules.kaiokenHealthCost(p.getHealth(), p.getMaxHealth()));
        int ticks = KiRules.duration(KiRules.KAIOKEN);
        tag.putLong(KiRules.untilKey(KiRules.KAIOKEN), KiRules.endsAt(now, KiRules.KAIOKEN));
        p.addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, ticks, KiRules.kaiokenStrengthAmp()));
        p.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, ticks, KiRules.kaiokenSpeedAmp()));
        SkillFx.aura(p.serverLevel(), p, true);
        SkillUtil.actionbar(p, SkillMsg.t("ki.kaioken", "Kaioken!").withStyle(ChatFormatting.RED));
        return true;
    }

    private boolean ultraInstinct(ServerPlayer p, CompoundTag tag, long now) {
        if (!SkillUtil.spendMagicules(p, DMUtil.cost(KiRules.cost(KiRules.UI)))) return false;
        tag.putLong(KiRules.untilKey(KiRules.UI), KiRules.endsAt(now, KiRules.UI));
        tag.putLong(KiRules.UI_DODGE_KEY, 0L);
        SkillFx.aura(p.serverLevel(), p, false);
        SkillUtil.actionbar(p, SkillMsg.t("ki.ui", "Ultra Instinct: your body moves on its own.").withStyle(ChatFormatting.WHITE));
        return true;
    }

    /** Ki Sense: every hostile presence within 32 blocks (at most 24) glows for ten seconds, and you are told how many there are. */
    private boolean sense(ServerPlayer p) {
        if (!SkillUtil.spendMagicules(p, DMUtil.cost(KiRules.cost(KiRules.SENSE)))) return false;
        List<LivingEntity> foes = SkillTargets.foes(p, p.position(), KiRules.senseRadius());
        for (LivingEntity e : foes) e.addEffect(new MobEffectInstance(MobEffects.GLOWING, KiRules.senseGlowTicks(), 0, false, false, false));
        SkillFx.ring(p.serverLevel(), p.position().add(0, 0.2, 0), 12.0, 0x8AFFC0);
        SkillUtil.actionbar(p, SkillMsg.t("ki.sense", "Ki Sense: %s presences near.", foes.size()).withStyle(ChatFormatting.GREEN));
        return true;
    }

    /** A second press (or the owner's choice): the form ends, and what it gave goes with it. */
    private void end(ManasSkillInstance instance, ServerPlayer p, CompoundTag tag, int mode) {
        tag.putLong(KiRules.untilKey(mode), 0L);
        if (mode == KiRules.SSJ) { dropOwn(p, MobEffects.DAMAGE_BOOST, KiRules.ssjStrengthAmp()); dropOwn(p, MobEffects.MOVEMENT_SPEED, KiRules.ssjSpeedAmp()); }
        if (mode == KiRules.KAIOKEN) { dropOwn(p, MobEffects.DAMAGE_BOOST, KiRules.kaiokenStrengthAmp()); dropOwn(p, MobEffects.MOVEMENT_SPEED, KiRules.kaiokenSpeedAmp()); }
        instance.markDirty();
        SkillUtil.actionbar(p, SkillMsg.t("ki.ended", "Your ki settles.").withStyle(ChatFormatting.GRAY));
    }

    private static void dropOwn(ServerPlayer p, net.minecraft.core.Holder<net.minecraft.world.effect.MobEffect> effect, int amp) {
        MobEffectInstance cur = p.getEffect(effect);
        if (cur != null && cur.getAmplifier() == amp) p.removeEffect(effect);
    }

    // ---------------------------------------------------------------- the damage hook
    /**
     * Afterimage turns the next blow to a fifth and is spent; Ultra Instinct dodges one blow a second outright and halves the others; Super Saiyan and Kaioken change what a hit does (see
     * {@link KiRules#takenMultiplier}). What bypasses invulnerability (the void, kill) is never touched.
     */
    @Override
    public boolean onTakenDamage(ManasSkillInstance instance, LivingEntity owner, DamageSource source, Changeable<Float> amount) {
        if (amount.get() <= 0 || !NUConfig.KI_ENABLED.get() || source.is(DamageTypeTags.BYPASSES_INVULNERABILITY)) return true;
        CompoundTag tag = instance.getOrCreateTag();
        long now = owner.level().getGameTime();
        float a = amount.get();
        if (open(tag, KiRules.AFTERIMAGE, now)) {
            tag.putLong(KiRules.untilKey(KiRules.AFTERIMAGE), 0L);   // spent on this blow
            a *= (float) KiRules.afterimageMultiplier();
            instance.markDirty();
            if (owner.level() instanceof ServerLevel level) SkillFx.burst(level, owner.position().add(0, 1, 0), false, 10, 0.5);
        }
        boolean ui = open(tag, KiRules.UI, now);
        if (ui && KiRules.uiDodges(tag.getLong(KiRules.UI_DODGE_KEY), now)) {
            tag.putLong(KiRules.UI_DODGE_KEY, now);
            instance.markDirty();
            amount.set(0.0F);
            if (owner.level() instanceof ServerLevel level) SkillFx.burst(level, owner.position().add(0, 1, 0), false, 8, 0.5);
            return true;
        }
        double m = KiRules.takenMultiplier(open(tag, KiRules.SSJ, now), open(tag, KiRules.KAIOKEN, now), ui);
        float out = (float) (a * m);
        if (out != amount.get()) amount.set(out);
        return true;
    }
}
