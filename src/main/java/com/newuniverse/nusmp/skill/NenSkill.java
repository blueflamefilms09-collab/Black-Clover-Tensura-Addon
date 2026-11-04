package com.newuniverse.nusmp.skill;

import com.newuniverse.nusmp.NUConfig;
import com.newuniverse.nusmp.skill.rules.NenRules;
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
import net.minecraft.world.entity.Mob;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import com.newuniverse.nusmp.vfx.VfxShape;

import java.util.List;

/**
 * Nen (Hunter x Hunter), an Ultimate with ten modes (3.46.0, new; see {@link NenRules} for the numbers and the reasoning). Ten wraps your aura round your body (a fifth of every hit goes), Zetsu shuts
 * it so that you cannot be seen or hunted until you strike, Ren pours it out (Strength II, Speed I), Gyo puts it in your eyes (Night Vision and the invisible shown), En spreads it as a sphere (everything
 * near glows), Ken is the stance (a hit does two fifths, but you are slow), Ko puts it all in one strike (your next hit lands a second heavy blow, and you take more while it waits), Hatsu Enhancement is
 * an empowered blow, Hatsu Emission an aura bolt that pierces three, and Vow and Limitation trades your defences (no Ten, Zetsu or Ken, a quarter more damage taken) for hatsu that hit three quarters harder.
 * All state is the game time each window ends at, kept in the skill tag of the one player who owns it: nothing ticks, nothing is cleaned up, nothing is shared. A press looks at no more than 24 creatures.
 * Switch: server setting {@code nenEnabled}. Obtain: {@code /tensura edit <player> ability grant nusmp:nen}.
 */
public class NenSkill extends NUSkill {
    static final SkillText TEXT = SkillText.of("nen", "Nen",
            "Ultimate. Hunter x Hunter's life energy in ten modes: Ten, Zetsu, Ren, Gyo, En, Ken, Ko, Hatsu Enhancement, Hatsu Emission and Vow and Limitation.");

    /** The cast colour of each mode (ARGB). */
    private static final int[] COLORS = {0xFFE8F4FF, 0xFF4A4A5C, 0xFFFFD060, 0xFF7AE0FF, 0xFF6AFFC0, 0xFF8AB8FF, 0xFFFF6A3A, 0xFFFF9A3A, 0xFF6AD8FF, 0xFFB060FF,
            0xFF7AFFD8, 0xFF303040, 0xFFFFFFFF, 0xFFFFE04A, 0xFFD8C890, 0xFFE060FF, 0xFFFF6AA0};

    public NenSkill() { super(SkillType.ULTIMATE, TEXT, true); }

    @Override public int getModes(ManasSkillInstance i) { return NenRules.MODES; }
    @Override public int nextMode(LivingEntity e, ManasSkillInstance i, int mode, boolean reverse) { return Math.floorMod(mode + (reverse ? -1 : 1), NenRules.MODES); }
    @Override public String getModeId(ManasSkillInstance i, int mode) { return "nen." + NenRules.IDS[Math.floorMod(mode, NenRules.MODES)]; }

    @Override
    public Component getModeName(ManasSkillInstance i, int mode) {
        return switch (Math.floorMod(mode, NenRules.MODES)) {
            case NenRules.TEN -> SkillMsg.mode("nen.ten", "Ten");
            case NenRules.ZETSU -> SkillMsg.mode("nen.zetsu", "Zetsu");
            case NenRules.REN -> SkillMsg.mode("nen.ren", "Ren");
            case NenRules.GYO -> SkillMsg.mode("nen.gyo", "Gyo");
            case NenRules.EN -> SkillMsg.mode("nen.en", "En");
            case NenRules.KEN -> SkillMsg.mode("nen.ken", "Ken");
            case NenRules.KO -> SkillMsg.mode("nen.ko", "Ko");
            case NenRules.ENHANCE -> SkillMsg.mode("nen.enhance", "Hatsu: Enhancement");
            case NenRules.EMIT -> SkillMsg.mode("nen.emit", "Hatsu: Emission");
            case NenRules.VOW -> SkillMsg.mode("nen.vow", "Vow and Limitation");
            case NenRules.SHU -> SkillMsg.mode("nen.shu", "Shu");
            case NenRules.IN -> SkillMsg.mode("nen.in", "In");
            case NenRules.RYU -> SkillMsg.mode("nen.ryu", "Ryu");
            case NenRules.TRANS -> SkillMsg.mode("nen.trans", "Hatsu: Transmutation");
            case NenRules.CONJ -> SkillMsg.mode("nen.conj", "Hatsu: Conjuration");
            case NenRules.MANIP -> SkillMsg.mode("nen.manip", "Hatsu: Manipulation");
            default -> SkillMsg.mode("nen.spec", "Hatsu: Specialization");
        };
    }

    private static boolean open(CompoundTag tag, int mode, long now) { return NenRules.active(tag.getLong(NenRules.untilKey(mode)), now); }

    // ---------------------------------------------------------------- the effects (3.48.5)
    private static final int[] AURA_KINDS = {NenRules.TEN, NenRules.ZETSU, NenRules.REN, NenRules.KEN, NenRules.KO, NenRules.VOW, NenRules.SHU, NenRules.RYU};
    /** Ticks an aura instance lives: resent every {@link #AURA_EVERY} ticks while its window is open, so a window ended early fades within about two and a half seconds. */
    private static final int AURA_LIFE = 50, AURA_EVERY = 40;

    private static final String IN_KEY = "nusmp_nen_in";
    private static boolean newFx() { return NUConfig.NEN_VFX.get(); }

    /** In hides the aura: while it is open none of this player's Nen effects are sent (the hatsu is still there, you just cannot see it). */
    private static boolean hidden(ServerPlayer p) { return p.getPersistentData().getLong(IN_KEY) > p.level().getGameTime(); }

    /** Sends the aura of one window kind (index in {@link #AURA_KINDS}) following the player. Cosmetic: nearby players only (VfxSpawn), never throws. */
    private static void auraFx(ServerPlayer p, int kind) {
        if (!hidden(p)) SkillFx.follow(p.serverLevel(), VfxShape.NEN_AURA, p, p.position(), 0, AURA_LIFE, kind);
    }

    private static void shapeFx(ServerPlayer p, VfxShape shape, Vec3 at, Vec3 to, int argb, int life, float power) {
        if (!hidden(p)) SkillFx.shape(p.serverLevel(), shape, at, to, argb, life, power);
    }

    /** Once every two seconds, for each window that is open: the aura (at most the six windows, in practice two or three). */
    @Override public boolean canTick(ManasSkillInstance instance, LivingEntity entity) { return true; }

    @Override
    public void onTick(ManasSkillInstance instance, LivingEntity living) {
        if (!(living instanceof ServerPlayer p) || p.level().isClientSide || p.tickCount % AURA_EVERY != 0 || !newFx() || !NUConfig.NEN_ENABLED.get()) return;
        CompoundTag tag = instance.getOrCreateTag();
        long now = p.level().getGameTime();
        for (int k = 0; k < AURA_KINDS.length; k++) if (open(tag, AURA_KINDS[k], now)) auraFx(p, k);
    }

    // ---------------------------------------------------------------- the press
    @Override
    public void onPressed(ManasSkillInstance instance, LivingEntity entity, int key, int mode) {
        if (!(entity instanceof ServerPlayer p) || !p.isAlive() || !NenRules.valid(mode)) return;
        if (!NUConfig.NEN_ENABLED.get()) { SkillUtil.fail(p, SkillMsg.t("nen.off", "Nen is switched off on this server.")); return; }
        CompoundTag tag = instance.getOrCreateTag();
        long now = p.level().getGameTime();
        if (NenRules.cancellable(mode) && open(tag, mode, now)) { end(instance, p, tag, mode); return; }   // a second press ends the window (its cooldown is already running)
        if (instance.onCoolDown(mode)) { SkillUtil.fail(p, SkillMsg.t("nen.cooldown", "Your aura needs a moment.")); return; }
        boolean vow = open(tag, NenRules.VOW, now);
        if (vow && NenRules.vowForbids(mode)) { SkillUtil.fail(p, SkillMsg.t("nen.vow_forbids", "Your vow forbids it.")); return; }
        boolean done = switch (mode) {
            case NenRules.TEN -> window(instance, p, tag, now, mode, SkillMsg.t("nen.ten", "Ten: your aura wraps your body."));
            case NenRules.ZETSU -> zetsu(instance, p, tag, now);
            case NenRules.REN -> ren(instance, p, tag, now);
            case NenRules.GYO -> gyo(p);
            case NenRules.EN -> en(p);
            case NenRules.KEN -> ken(instance, p, tag, now);
            case NenRules.KO -> window(instance, p, tag, now, mode, SkillMsg.t("nen.ko", "Ko: all your aura gathers in one strike."));
            case NenRules.ENHANCE -> enhance(instance, p, vow);
            case NenRules.EMIT -> emit(instance, p, vow);
            case NenRules.VOW -> vow(instance, p, tag, now);
            case NenRules.SHU, NenRules.IN, NenRules.RYU -> advanced(instance, p, tag, now, mode);
            case NenRules.TRANS -> trans(instance, p, tag, now, vow);
            case NenRules.CONJ -> conj(instance, p, tag, now, vow);
            case NenRules.MANIP -> manip(instance, p, tag, now, vow);
            default -> spec(instance, p, tag, now, vow);
        };
        if (!done) return;
        cooldown(instance, mode, DMUtil.cooldown(NenRules.cooldown(mode)));
        SkillFx.sound(p.serverLevel(), p.position(), SoundEvents.BEACON_ACTIVATE, 0.6F, 1.4F);
        SkillUtil.castVfx(p, COLORS[mode]);
        grow(instance, p, 1);
    }

    /** Pays and opens the window of a mode that only needs the window (Ten, Ko). */
    private boolean window(ManasSkillInstance instance, ServerPlayer p, CompoundTag tag, long now, int mode, Component message) {
        if (!SkillUtil.spendMagicules(p, DMUtil.cost(NenRules.cost(mode)))) return false;
        tag.putLong(NenRules.untilKey(mode), NenRules.endsAt(now, mode));
        if (newFx()) auraFx(p, mode == NenRules.KO ? 4 : 0); else SkillFx.aura(p.serverLevel(), p, false);
        SkillUtil.actionbar(p, message.copy().withStyle(ChatFormatting.AQUA));
        return true;
    }

    private boolean zetsu(ManasSkillInstance instance, ServerPlayer p, CompoundTag tag, long now) {
        if (!SkillUtil.spendMagicules(p, DMUtil.cost(NenRules.cost(NenRules.ZETSU)))) return false;
        tag.putLong(NenRules.untilKey(NenRules.ZETSU), NenRules.endsAt(now, NenRules.ZETSU));
        p.addEffect(new MobEffectInstance(MobEffects.INVISIBILITY, NenRules.duration(NenRules.ZETSU), 0, false, false, true));
        for (LivingEntity e : SkillTargets.foes(p, p.position(), NenRules.zetsuRadius())) {
            if (e instanceof Mob m && m.getTarget() == p) m.setTarget(null);   // what was hunting you forgets you
        }
        if (newFx()) auraFx(p, 1);
        SkillUtil.actionbar(p, SkillMsg.t("nen.zetsu", "Zetsu: your presence is gone. Striking breaks it.").withStyle(ChatFormatting.GRAY));
        return true;
    }

    private boolean ren(ManasSkillInstance instance, ServerPlayer p, CompoundTag tag, long now) {
        if (!SkillUtil.spendMagicules(p, DMUtil.cost(NenRules.cost(NenRules.REN)))) return false;
        int ticks = NenRules.duration(NenRules.REN);
        tag.putLong(NenRules.untilKey(NenRules.REN), NenRules.endsAt(now, NenRules.REN));
        p.addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, ticks, NenRules.renStrengthAmp()));
        p.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, ticks, NenRules.renSpeedAmp()));
        if (newFx()) { auraFx(p, 2); shapeFx(p, VfxShape.NEN_IMPACT, p.position().add(0, 1, 0), p.position().add(0, 1, 0), 0xFFFFE08A, 14, 1.4f); } else SkillFx.burst(p.serverLevel(), p.position().add(0, 1, 0), false, 18, 0.7);
        SkillUtil.actionbar(p, SkillMsg.t("nen.ren", "Ren: your aura pours out.").withStyle(ChatFormatting.GOLD));
        return true;
    }

    private boolean ken(ManasSkillInstance instance, ServerPlayer p, CompoundTag tag, long now) {
        if (!SkillUtil.spendMagicules(p, DMUtil.cost(NenRules.cost(NenRules.KEN)))) return false;
        int ticks = NenRules.duration(NenRules.KEN);
        tag.putLong(NenRules.untilKey(NenRules.KEN), NenRules.endsAt(now, NenRules.KEN));
        p.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, ticks, NenRules.kenSlowAmp()));
        if (newFx()) auraFx(p, 3); else SkillFx.aura(p.serverLevel(), p, false);
        SkillUtil.actionbar(p, SkillMsg.t("nen.ken", "Ken: you hold your ground.").withStyle(ChatFormatting.BLUE));
        return true;
    }

    /** Gyo: Night Vision for a minute, and every invisible creature within 24 blocks (at most 16) glows for eight seconds. */
    private boolean gyo(ServerPlayer p) {
        if (!SkillUtil.spendMagicules(p, DMUtil.cost(NenRules.cost(NenRules.GYO)))) return false;
        p.addEffect(new MobEffectInstance(MobEffects.NIGHT_VISION, NenRules.gyoNightTicks(), 0, false, false, true));
        double r = NenRules.gyoRadius();
        List<LivingEntity> hidden = p.serverLevel().getEntitiesOfClass(LivingEntity.class, new AABB(p.position(), p.position()).inflate(r),
                e -> e != p && e.isInvisible() && !e.isSpectator() && e.distanceToSqr(p) <= r * r);
        int shown = 0;
        for (LivingEntity e : hidden) {
            if (shown >= NenRules.gyoMaxRevealed()) break;
            e.addEffect(new MobEffectInstance(MobEffects.GLOWING, NenRules.gyoRevealTicks(), 0, false, false, false));
            shown++;
        }
        if (newFx() && !hidden(p)) SkillFx.follow(p.serverLevel(), VfxShape.NEN_GYO, p, p.position(), 0xFF7AE0FF, 20, 1f);
        SkillUtil.actionbar(p, SkillMsg.t("nen.gyo", "Gyo: the aura gathers in your eyes.").withStyle(ChatFormatting.AQUA));
        return true;
    }

    /** En: everything hostile within 16 blocks (at most 24) glows for ten seconds, and you are told how many there are. */
    private boolean en(ServerPlayer p) {
        if (!SkillUtil.spendMagicules(p, DMUtil.cost(NenRules.cost(NenRules.EN)))) return false;
        List<LivingEntity> foes = SkillTargets.foes(p, p.position(), NenRules.enRadius());
        for (LivingEntity e : foes) e.addEffect(new MobEffectInstance(MobEffects.GLOWING, NenRules.enGlowTicks(), 0, false, false, false));
        if (newFx()) shapeFx(p, VfxShape.NEN_EN, p.position(), p.position(), 0xFF6AFFC0, 40, (float) NenRules.enRadius());
        else SkillFx.ring(p.serverLevel(), p.position().add(0, 0.2, 0), 12.0, 0x6AFFC0);
        SkillUtil.actionbar(p, SkillMsg.t("nen.en", "En: %s presences within your aura.", foes.size()).withStyle(ChatFormatting.GREEN));
        return true;
    }

    private boolean enhance(ManasSkillInstance instance, ServerPlayer p, boolean vow) {
        LivingEntity t = SkillTargets.lookFoe(p, NenRules.enhanceReach());
        if (t == null) { SkillUtil.fail(p, SkillMsg.t("nen.no_target", "Nothing in reach.")); return false; }
        if (!SkillUtil.spendMagicules(p, DMUtil.cost(NenRules.cost(NenRules.ENHANCE)))) return false;
        SkillDamage.hit(p, t, NenRules.enhanceDamage() * hatsuMul(instance, p, NenRules.ENHANCE, vow), masteryFrac(instance));
        knock(p, t, NenRules.enhanceKnockback());
        Vec3 hit = t.position().add(0, t.getBbHeight() * 0.5, 0);
        if (newFx()) shapeFx(p, VfxShape.NEN_IMPACT, hit, hit, 0xFFFF9A3A, 14, 1f); else SkillFx.burst(p.serverLevel(), hit, false, 14, 0.4);
        return true;
    }

    private boolean emit(ManasSkillInstance instance, ServerPlayer p, boolean vow) {
        if (!SkillUtil.spendMagicules(p, DMUtil.cost(NenRules.cost(NenRules.EMIT)))) return false;
        Vec3 from = p.getEyePosition(), to = DMUtil.lookPoint(p, NenRules.emitRange());
        if (newFx()) shapeFx(p, VfxShape.NEN_BEAM, from, to, 0xFF6AD8FF, 14, 1f); else SkillFx.beam(p.serverLevel(), from, to, false);
        int hits = 0;
        for (LivingEntity t : SkillTargets.foesAlong(p, from, to, NenRules.emitWidth())) {
            if (hits >= NenRules.emitPierce()) break;
            if (SkillDamage.hit(p, t, NenRules.emitDamage() * hatsuMul(instance, p, NenRules.EMIT, vow), masteryFrac(instance), SkillDamage.Kind.MAGIC) > 0f) hits++;
        }
        return true;
    }

    private boolean vow(ManasSkillInstance instance, ServerPlayer p, CompoundTag tag, long now) {
        tag.putLong(NenRules.untilKey(NenRules.VOW), NenRules.endsAt(now, NenRules.VOW));
        if (newFx()) auraFx(p, 5); else SkillFx.aura(p.serverLevel(), p, true);
        SkillUtil.actionbar(p, SkillMsg.t("nen.vow", "You swear the vow: no Ten, Zetsu or Ken, and your hatsu hit harder.").withStyle(ChatFormatting.LIGHT_PURPLE));
        return true;
    }

    // ---------------------------------------------------------------- 3.49.0: Shu, In, Ryu and the other four hatsu types
    /** The type this player was given by the Water Divination (assigned from their UUID the first time it is asked, then kept in the skill tag). */
    private static int typeOf(ManasSkillInstance instance, ServerPlayer p) {
        CompoundTag tag = instance.getOrCreateTag();
        if (!tag.contains("NenType")) {
            tag.putInt("NenType", NenRules.divine(p.getUUID().getMostSignificantBits(), p.getUUID().getLeastSignificantBits()));
            instance.markDirty();
        }
        return Math.floorMod(tag.getInt("NenType"), 6);
    }

    /** The multiplier of a hatsu: the vow, In, and how well the player's type suits it. */
    private static double hatsuMul(ManasSkillInstance instance, ServerPlayer p, int mode, boolean vow) {
        double m = NenRules.hatsuMultiplier(vow) * NenRules.efficiency(typeOf(instance, p), NenRules.typeOfMode(mode));
        if (open(instance.getOrCreateTag(), NenRules.IN, p.level().getGameTime())) m *= NenRules.inMultiplier();
        return m;
    }

    /** Shu (the held item is wrapped in aura), In (the aura is hidden) and Ryu (the aura flows to where the blow lands): a window each. */
    private boolean advanced(ManasSkillInstance instance, ServerPlayer p, CompoundTag tag, long now, int mode) {
        if (!SkillUtil.spendMagicules(p, DMUtil.cost(NenRules.cost(mode)))) return false;
        tag.putLong(NenRules.untilKey(mode), NenRules.endsAt(now, mode));
        switch (mode) {
            case NenRules.SHU -> { if (newFx()) auraFx(p, 6); SkillUtil.actionbar(p, SkillMsg.t("nen.shu", "Shu: your aura wraps what you hold.").withStyle(ChatFormatting.AQUA)); }
            case NenRules.IN -> { p.getPersistentData().putLong(IN_KEY, NenRules.endsAt(now, mode)); SkillUtil.actionbar(p, SkillMsg.t("nen.in", "In: your aura cannot be seen.").withStyle(ChatFormatting.DARK_GRAY)); }
            default -> { if (newFx()) auraFx(p, 7); SkillUtil.actionbar(p, SkillMsg.t("nen.ryu", "Ryu: your aura flows to where it is needed.").withStyle(ChatFormatting.LIGHT_PURPLE)); }
        }
        return true;
    }

    /** Transmutation: the aura takes the nature of lightning and runs along a line, hitting up to four and slowing them. */
    private boolean trans(ManasSkillInstance instance, ServerPlayer p, CompoundTag tag, long now, boolean vow) {
        if (!SkillUtil.spendMagicules(p, DMUtil.cost(NenRules.cost(NenRules.TRANS)))) return false;
        Vec3 from = p.getEyePosition(), to = DMUtil.lookPoint(p, NenRules.transRange());
        shapeFx(p, VfxShape.NEN_BEAM, from, to, 0xFFFFE04A, 14, 1f);
        int hits = 0;
        for (LivingEntity t : SkillTargets.foesAlong(p, from, to, 1.5)) {
            if (hits >= NenRules.transMax()) break;
            if (SkillDamage.hit(p, t, NenRules.transDamage() * hatsuMul(instance, p, NenRules.TRANS, vow), masteryFrac(instance), SkillDamage.Kind.MAGIC) > 0f) {
                t.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, NenRules.transSlowTicks(), 0));
                hits++;
            }
        }
        return true;
    }

    /** Conjuration: chains of aura bind the creature you look at and hold it nearly still. */
    private boolean conj(ManasSkillInstance instance, ServerPlayer p, CompoundTag tag, long now, boolean vow) {
        LivingEntity t = SkillTargets.lookFoe(p, NenRules.conjRange());
        if (t == null) { SkillUtil.fail(p, SkillMsg.t("nen.no_target", "Nothing in reach.")); return false; }
        if (!SkillUtil.spendMagicules(p, DMUtil.cost(NenRules.cost(NenRules.CONJ)))) return false;
        SkillDamage.hit(p, t, NenRules.conjDamage() * hatsuMul(instance, p, NenRules.CONJ, vow), masteryFrac(instance), SkillDamage.Kind.MAGIC);
        t.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, NenRules.conjRootTicks(), 4));
        Vec3 at = t.position().add(0, t.getBbHeight() * 0.5, 0);
        shapeFx(p, VfxShape.NEN_IMPACT, at, at, 0xFFD8C890, 14, 1.3f);
        return true;
    }

    /** Manipulation: the creature you look at turns on the nearest other creature (a player is only weakened and slowed). */
    private boolean manip(ManasSkillInstance instance, ServerPlayer p, CompoundTag tag, long now, boolean vow) {
        LivingEntity t = SkillTargets.lookFoe(p, NenRules.manipRange());
        if (t == null) { SkillUtil.fail(p, SkillMsg.t("nen.no_target", "Nothing in reach.")); return false; }
        if (!SkillUtil.spendMagicules(p, DMUtil.cost(NenRules.cost(NenRules.MANIP)))) return false;
        t.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, NenRules.manipTicks(), 1));
        if (t instanceof Mob m) {
            for (LivingEntity other : SkillTargets.foes(p, t.position(), NenRules.manipSearch())) {
                if (other != t) { m.setTarget(other); break; }
            }
        } else {
            t.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, NenRules.manipTicks(), 1));
        }
        Vec3 at = t.position().add(0, t.getBbHeight() * 0.5, 0);
        shapeFx(p, VfxShape.NEN_IMPACT, at, at, 0xFFE060FF, 14, 1.1f);
        return true;
    }

    /** Specialization: a pull of aura draws the foes around you in and weakens them (at most eight). */
    private boolean spec(ManasSkillInstance instance, ServerPlayer p, CompoundTag tag, long now, boolean vow) {
        if (!SkillUtil.spendMagicules(p, DMUtil.cost(NenRules.cost(NenRules.SPEC)))) return false;
        int pulled = 0;
        for (LivingEntity t : SkillTargets.foes(p, p.position(), NenRules.specRadius())) {
            if (pulled >= NenRules.specMax()) break;
            Vec3 d = p.position().subtract(t.position());
            if (d.lengthSqr() < 1.0e-4) continue;
            Vec3 dir = d.normalize();
            t.setDeltaMovement(dir.x * 1.1, 0.3, dir.z * 1.1);
            t.hurtMarked = true;
            t.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 80, 0));
            SkillDamage.hit(p, t, NenRules.specDamage() * hatsuMul(instance, p, NenRules.SPEC, vow), masteryFrac(instance), SkillDamage.Kind.MAGIC);
            pulled++;
        }
        Vec3 c = p.position().add(0, 1, 0);
        shapeFx(p, VfxShape.NEN_IMPACT, c, c, 0xFFFF6AA0, 14, 2.4f);
        return true;
    }

    /** A second press (or the owner's choice): the window closes, with what it gave. */
    private void end(ManasSkillInstance instance, ServerPlayer p, CompoundTag tag, int mode) {
        tag.putLong(NenRules.untilKey(mode), 0L);
        if (mode == NenRules.ZETSU) p.removeEffect(MobEffects.INVISIBILITY);
        if (mode == NenRules.IN) p.getPersistentData().putLong(IN_KEY, 0L);
        if (mode == NenRules.REN) { dropOwn(p, MobEffects.DAMAGE_BOOST, NenRules.renStrengthAmp()); dropOwn(p, MobEffects.MOVEMENT_SPEED, NenRules.renSpeedAmp()); }
        if (mode == NenRules.KEN) dropOwn(p, MobEffects.MOVEMENT_SLOWDOWN, NenRules.kenSlowAmp());
        instance.markDirty();
        SkillUtil.actionbar(p, SkillMsg.t("nen.ended", "Your aura settles.").withStyle(ChatFormatting.GRAY));
    }

    private static void dropOwn(ServerPlayer p, net.minecraft.core.Holder<net.minecraft.world.effect.MobEffect> effect, int amp) {
        MobEffectInstance cur = p.getEffect(effect);
        if (cur != null && cur.getAmplifier() == amp) p.removeEffect(effect);
    }

    private static void knock(ServerPlayer p, LivingEntity t, double power) {
        Vec3 away = t.position().subtract(p.position());
        if (away.lengthSqr() < 1.0e-4) return;
        Vec3 dir = away.normalize();
        t.setDeltaMovement(dir.x * power, 0.35, dir.z * power);
        t.hurtMarked = true;
    }

    // ---------------------------------------------------------------- the damage hooks
    /** Ten, Ken, Ko and the vow change what a hit does to you (see {@link NenRules#takenMultiplier}); what bypasses invulnerability (the void, kill) is never touched. */
    @Override
    public boolean onTakenDamage(ManasSkillInstance instance, LivingEntity owner, DamageSource source, Changeable<Float> amount) {
        if (amount.get() <= 0 || !NUConfig.NEN_ENABLED.get() || source.is(DamageTypeTags.BYPASSES_INVULNERABILITY)) return true;
        CompoundTag tag = instance.getOrCreateTag();
        long now = owner.level().getGameTime();
        double m = NenRules.takenMultiplier(open(tag, NenRules.TEN, now), open(tag, NenRules.KEN, now), open(tag, NenRules.KO, now), open(tag, NenRules.VOW, now),
                open(tag, NenRules.ZETSU, now), open(tag, NenRules.RYU, now));
        if (m != 1.0) amount.set((float) (amount.get() * m));
        return true;
    }

    /** Striking breaks Zetsu; the first blow while Ko waits is followed by Ko's heavy strike (through the balance law, so a player is never one-shot). */
    @Override
    public boolean onDamageEntity(ManasSkillInstance instance, LivingEntity owner, LivingEntity target, DamageSource source, Changeable<Float> amount) {
        if (!(owner instanceof ServerPlayer p) || target == owner || amount.get() <= 0 || !NUConfig.NEN_ENABLED.get()) return true;
        CompoundTag tag = instance.getOrCreateTag();
        long now = p.level().getGameTime();
        if (open(tag, NenRules.SHU, now) && !p.getMainHandItem().isEmpty()) amount.set((float) (amount.get() + NenRules.shuBonus()));      // the aura on the weapon
        if (open(tag, NenRules.RYU, now)) amount.set((float) (amount.get() * NenRules.ryuDealtMultiplier()));
        if (open(tag, NenRules.ZETSU, now)) {
            tag.putLong(NenRules.untilKey(NenRules.ZETSU), 0L);
            p.removeEffect(MobEffects.INVISIBILITY);
            instance.markDirty();
        }
        if (open(tag, NenRules.KO, now) && SkillTargets.isAimable(p, target)) {
            tag.putLong(NenRules.untilKey(NenRules.KO), 0L);   // closed first: the strike below is itself a hit and must not start another
            instance.markDirty();
            double mul = NenRules.hatsuMultiplier(open(tag, NenRules.VOW, now));
            SkillDamage.hit(p, target, NenRules.koDamage() * mul, masteryFrac(instance));
            knock(p, target, NenRules.enhanceKnockback() * 1.5);
            if (p.level() instanceof ServerLevel level) {
                Vec3 koAt = target.position().add(0, target.getBbHeight() * 0.5, 0);
                if (newFx()) shapeFx(p, VfxShape.NEN_IMPACT, koAt, koAt, 0xFFFF6A3A, 14, 1.6f); else SkillFx.burst(level, koAt, false, 20, 0.5);
                SkillFx.sound(level, target.position(), SoundEvents.ANVIL_LAND, 0.5F, 1.6F);
            }
        }
        return true;
    }
}
