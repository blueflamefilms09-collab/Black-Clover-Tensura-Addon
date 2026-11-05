package com.newuniverse.nusmp.skill;

import com.newuniverse.nusmp.NUConfig;
import com.newuniverse.nusmp.skill.rules.NenRules;
import io.github.manasmods.manascore.network.api.util.Changeable;
import io.github.manasmods.manascore.skill.api.ManasSkillInstance;
import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.List;

/**
 * Nen (Hunter x Hunter), an Ultimate with seventeen modes (3.46.0, new; 3.49.0 the last seven; reworked in 3.57.0, see {@link NenRules} for the numbers and docs/nen_rework.md for the reasoning).
 * Basics: Ten wraps your aura round your body (a fifth of every hit goes and you mend a little), Zetsu shuts it so that you cannot be seen or hunted and you rest, and the blow that ends it is
 * an ambush; Ren pours it out (Strength II, Speed II and a burst that throws back what is near); Gyo puts it in your eyes (Night Vision, the invisible shown, and the creature you look at read:
 * your blows do more to it); En opens a sphere for twelve seconds (everything hostile in it glows and the aura feels the first blow every six seconds coming); Ken is the stance (a hit does two
 * fifths and a third of it is thrown back at who struck); Ko gathers all the aura in one fist, the longer you wait the harder the strike, and Enhancement can spend it instead; Shu, In and Ryu
 * wrap the weapon, hide the aura and move it. The six Hatsu are signature moves ({@link NenHatsu}): Enhancement a rushing fist that wears armour down, Emission a homing volley, Transmutation
 * electricity, gum or rubber by stance, Conjuration chains, Manipulation a needle, Specialization a field that steals buffs; Vow and Limitation trades your defences (no Ten, Zetsu or Ken, a quarter
 * more damage taken) for hatsu that hit three quarters harder. Windows are the game time they end at, kept in the skill tag of the one player who owns them; what runs after a press is bounded and
 * expires by game time ({@link NenHatsu}). A press looks at no more than 24 creatures. Switch: server setting {@code nenEnabled}; looks: {@code nenVfx} and {@code nenVfxV2}.
 * Obtain: {@code /tensura edit <player> ability grant nusmp:nen}.
 */
public class NenSkill extends NUSkill {
    static final SkillText TEXT = SkillText.of("nen", "Nen",
            "Ultimate. Hunter x Hunter's life energy in seventeen modes: Ten, Zetsu, Ren, Gyo, En, Ken, Ko, Shu, In, Ryu, the six Hatsu types (Enhancement, Transmutation, Emission, Conjuration, Manipulation, Specialization) and Vow and Limitation.");

    /** The cast colour of each mode (ARGB), used by the old cast flourish (nenVfxV2 = false). */
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

    static boolean open(CompoundTag tag, int mode, long now) { return NenRules.active(tag.getLong(NenRules.untilKey(mode)), now); }

    /** The index of a mode in the visual layer's aura kinds, or -1 for a mode that shows none. */
    static int kindOf(int mode) {
        for (int k = 0; k < NenRules.AURA_MODES.length; k++) if (NenRules.AURA_MODES[k] == mode) return k;
        return -1;
    }

    /** Once every two seconds, for each window that is open: the aura (at most {@link NenRules#MAX_AURAS_SENT}). */
    private static final int AURA_EVERY = 40;
    private static final String EN_DODGE = "NenEnDodge", KEN_HIT = "NenKenHit";

    private static void auras(ManasSkillInstance instance, ServerPlayer p, CompoundTag tag, long now) {
        int type = typeOf(instance, p), sent = 0;
        for (int k = 0; k < NenRules.AURA_MODES.length && sent < NenRules.MAX_AURAS_SENT; k++) {
            int mode = NenRules.AURA_MODES[k];
            if (!open(tag, mode, now)) continue;
            NenFx.aura(p, k, type, mode == NenRules.KO ? NenRules.koCharge(tag.getLong(NenRules.untilKey(mode)), now) : 0.0);
            sent++;
        }
        if (open(tag, NenRules.EN, now)) NenFx.sense(p, NenRules.enRadius(), NenRules.typeColor(type), true);
    }

    @Override public boolean canTick(ManasSkillInstance instance, LivingEntity entity) { return true; }

    /** Every tick: what the last hatsu left running (one tag read when nothing is). Once a second: the small gifts of Ten and Zetsu and En's look. Once every two seconds: the auras. */
    @Override
    public void onTick(ManasSkillInstance instance, LivingEntity living) {
        if (!(living instanceof ServerPlayer p) || p.level().isClientSide || !NUConfig.NEN_ENABLED.get()) return;
        CompoundTag tag = instance.getOrCreateTag();
        long now = p.level().getGameTime();
        NenHatsu.tick(instance, p, tag, now);
        if (p.tickCount % NenRules.enPulseEvery() != 0) return;
        if (p.getHealth() < p.getMaxHealth()) {
            if (open(tag, NenRules.TEN, now) && p.tickCount % NenRules.tenHealEvery() == 0) p.heal((float) NenRules.tenHeal());
            if (open(tag, NenRules.ZETSU, now)) p.heal((float) NenRules.zetsuHeal());
        }
        if (open(tag, NenRules.EN, now)) enPulse(p, NenRules.enGlowPulseTicks());
        if (p.tickCount % AURA_EVERY == 0 && NenFx.on()) auras(instance, p, tag, now);
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
            case NenRules.REN -> ren(instance, p, tag, now, vow);
            case NenRules.GYO -> gyo(instance, p);
            case NenRules.EN -> en(instance, p, tag, now);
            case NenRules.KEN -> ken(instance, p, tag, now);
            case NenRules.KO -> window(instance, p, tag, now, mode, SkillMsg.t("nen.ko", "Ko: all your aura gathers in one fist. Strike now, or wait: the longer, the harder."));
            case NenRules.ENHANCE -> NenHatsu.enhance(instance, p, tag, now, vow);
            case NenRules.EMIT -> NenHatsu.emit(instance, p, tag, now, vow);
            case NenRules.VOW -> vow(instance, p, tag, now);
            case NenRules.SHU, NenRules.IN, NenRules.RYU -> advanced(instance, p, tag, now, mode);
            case NenRules.TRANS -> trans(instance, p, tag, now, vow);
            case NenRules.CONJ -> NenHatsu.conj(instance, p, tag, now, vow);
            case NenRules.MANIP -> NenHatsu.manip(instance, p, tag, now, vow);
            default -> NenHatsu.spec(instance, p, tag, now, vow);
        };
        if (!done) return;
        cooldown(instance, mode, DMUtil.cooldown(NenRules.cooldown(mode)));
        SkillFx.sound(p.serverLevel(), p.position(), SoundEvents.BEACON_ACTIVATE, 0.6F, 1.4F);
        NenFx.cast(p, COLORS[mode]);
        grow(instance, p, 1);
    }

    /** Pays and opens the window of a mode that only needs the window (Ten, Ko). */
    private boolean window(ManasSkillInstance instance, ServerPlayer p, CompoundTag tag, long now, int mode, Component message) {
        if (!SkillUtil.spendMagicules(p, DMUtil.cost(NenRules.cost(mode)))) return false;
        tag.putLong(NenRules.untilKey(mode), NenRules.endsAt(now, mode));
        NenFx.aura(p, kindOf(mode), typeOf(instance, p), 0.0);
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
        NenFx.aura(p, kindOf(NenRules.ZETSU), typeOf(instance, p), 0.0);
        SkillUtil.actionbar(p, SkillMsg.t("nen.zetsu", "Zetsu: your presence is gone and you rest. The blow that breaks it is an ambush.").withStyle(ChatFormatting.GRAY));
        return true;
    }

    /** Ren: Strength II and Speed II, and the aura bursts out: what stands within five blocks (at most 12) is thrown back and hurt a little. */
    private boolean ren(ManasSkillInstance instance, ServerPlayer p, CompoundTag tag, long now, boolean vow) {
        if (!SkillUtil.spendMagicules(p, DMUtil.cost(NenRules.cost(NenRules.REN)))) return false;
        int ticks = NenRules.duration(NenRules.REN);
        tag.putLong(NenRules.untilKey(NenRules.REN), NenRules.endsAt(now, NenRules.REN));
        p.addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, ticks, NenRules.renStrengthAmp()));
        p.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, ticks, NenRules.renSpeedAmp()));
        int n = 0;
        for (LivingEntity e : SkillTargets.foes(p, p.position(), NenRules.renBurstRadius())) {
            if (n++ >= NenRules.renBurstMax()) break;
            knock(p, e, NenRules.renBurstKnockback());
            SkillDamage.hit(p, e, NenRules.renBurstDamage() * NenRules.hatsuMultiplier(vow), masteryFrac(instance), SkillDamage.Kind.MAGIC);
        }
        int type = typeOf(instance, p);
        NenFx.aura(p, kindOf(NenRules.REN), type, 0.0);
        NenFx.burst(p, p.position().add(0, 1, 0), NenRules.typeColor(type), 2.0f, true);
        SkillUtil.actionbar(p, SkillMsg.t("nen.ren", "Ren: your aura pours out.").withStyle(ChatFormatting.GOLD));
        return true;
    }

    private boolean ken(ManasSkillInstance instance, ServerPlayer p, CompoundTag tag, long now) {
        if (!SkillUtil.spendMagicules(p, DMUtil.cost(NenRules.cost(NenRules.KEN)))) return false;
        int ticks = NenRules.duration(NenRules.KEN);
        tag.putLong(NenRules.untilKey(NenRules.KEN), NenRules.endsAt(now, NenRules.KEN));
        p.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, ticks, NenRules.kenSlowAmp()));
        NenFx.aura(p, kindOf(NenRules.KEN), typeOf(instance, p), 0.0);
        SkillUtil.actionbar(p, SkillMsg.t("nen.ken", "Ken: you hold your ground, and a third of every blow goes back at who struck.").withStyle(ChatFormatting.BLUE));
        return true;
    }

    /** Gyo: Night Vision for a minute, every invisible creature within 24 blocks (at most 16) glows for eight seconds, and the creature you look at is read: for eight seconds your blows do 20 % more to it. */
    private boolean gyo(ManasSkillInstance instance, ServerPlayer p) {
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
        LivingEntity read = SkillTargets.lookFoe(p, NenRules.gyoMarkRange());
        if (read != null) {
            NenHatsu.mark(read, p, NenRules.gyoMarkTicks(), NenRules.gyoMarkMultiplier());
            read.addEffect(new MobEffectInstance(MobEffects.GLOWING, NenRules.gyoMarkTicks(), 0, false, false, false));
            NenFx.burst(p, read.position().add(0, read.getBbHeight() * 0.5, 0), NenRules.typeColor(typeOf(instance, p)), 0.9f, false);
        }
        NenFx.eyes(p, NenRules.typeColor(typeOf(instance, p)));
        SkillUtil.actionbar(p, SkillMsg.t("nen.gyo", "Gyo: the aura gathers in your eyes.").withStyle(ChatFormatting.AQUA));
        return true;
    }

    /** En: a sphere of aura round you for twelve seconds. Everything hostile in it (at most 24) glows, refreshed once a second, and the first blow every six seconds is felt coming and halved. */
    private boolean en(ManasSkillInstance instance, ServerPlayer p, CompoundTag tag, long now) {
        if (!SkillUtil.spendMagicules(p, DMUtil.cost(NenRules.cost(NenRules.EN)))) return false;
        tag.putLong(NenRules.untilKey(NenRules.EN), NenRules.endsAt(now, NenRules.EN));
        int n = enPulse(p, NenRules.enGlowPulseTicks());
        NenFx.sense(p, NenRules.enRadius(), NenRules.typeColor(typeOf(instance, p)), false);
        SkillUtil.actionbar(p, SkillMsg.t("nen.en", "En: %s presences within your aura.", n).withStyle(ChatFormatting.GREEN));
        return true;
    }

    /** One look of En: the foes in the sphere glow. Returns how many (at most 24). */
    private static int enPulse(ServerPlayer p, int glowTicks) {
        List<LivingEntity> foes = SkillTargets.foes(p, p.position(), NenRules.enRadius());
        for (LivingEntity e : foes) e.addEffect(new MobEffectInstance(MobEffects.GLOWING, glowTicks, 0, false, false, false));
        return foes.size();
    }

    private boolean vow(ManasSkillInstance instance, ServerPlayer p, CompoundTag tag, long now) {
        tag.putLong(NenRules.untilKey(NenRules.VOW), NenRules.endsAt(now, NenRules.VOW));
        NenFx.aura(p, kindOf(NenRules.VOW), typeOf(instance, p), 0.0);
        SkillUtil.actionbar(p, SkillMsg.t("nen.vow", "You swear the vow: no Ten, Zetsu or Ken, and your hatsu hit harder.").withStyle(ChatFormatting.LIGHT_PURPLE));
        return true;
    }

    // ---------------------------------------------------------------- Shu, In, Ryu and the Hatsu types
    /** The type this player was given by the Water Divination (assigned from their UUID the first time it is asked, then kept in the skill tag). */
    static int typeOf(ManasSkillInstance instance, ServerPlayer p) {
        CompoundTag tag = instance.getOrCreateTag();
        if (!tag.contains("NenType")) {
            tag.putInt("NenType", NenRules.divine(p.getUUID().getMostSignificantBits(), p.getUUID().getLeastSignificantBits()));
            instance.markDirty();
        }
        return Math.floorMod(tag.getInt("NenType"), 6);
    }

    /** The multiplier of a hatsu: the vow, In, and how well the player's type suits it. */
    static double hatsuMul(ManasSkillInstance instance, ServerPlayer p, int mode, boolean vow) {
        double m = NenRules.hatsuMultiplier(vow) * NenRules.efficiency(typeOf(instance, p), NenRules.typeOfMode(mode));
        if (open(instance.getOrCreateTag(), NenRules.IN, p.level().getGameTime())) m *= NenRules.inMultiplier();
        return m;
    }

    /** Shu (the held item is wrapped in aura), In (the aura is hidden) and Ryu (the aura flows to where the blow lands): a window each. */
    private boolean advanced(ManasSkillInstance instance, ServerPlayer p, CompoundTag tag, long now, int mode) {
        if (!SkillUtil.spendMagicules(p, DMUtil.cost(NenRules.cost(mode)))) return false;
        tag.putLong(NenRules.untilKey(mode), NenRules.endsAt(now, mode));
        switch (mode) {
            case NenRules.SHU -> { NenFx.aura(p, kindOf(mode), typeOf(instance, p), 0.0); SkillUtil.actionbar(p, SkillMsg.t("nen.shu", "Shu: your aura wraps what you hold.").withStyle(ChatFormatting.AQUA)); }
            case NenRules.IN -> { p.getPersistentData().putLong(NenFx.IN_KEY, NenRules.endsAt(now, mode)); SkillUtil.actionbar(p, SkillMsg.t("nen.in", "In: your aura cannot be seen.").withStyle(ChatFormatting.DARK_GRAY)); }
            default -> { NenFx.aura(p, kindOf(mode), typeOf(instance, p), 0.0); SkillUtil.actionbar(p, SkillMsg.t("nen.ryu", "Ryu: your aura flows to where it is needed.").withStyle(ChatFormatting.LIGHT_PURPLE)); }
        }
        return true;
    }

    /** Transmutation: the nature of the aura follows your stance (standing electricity, sneaking gum, in the air rubber); the message says which. */
    private boolean trans(ManasSkillInstance instance, ServerPlayer p, CompoundTag tag, long now, boolean vow) {
        int property = NenRules.transProperty(p.isShiftKeyDown(), !p.onGround());
        if (!NenHatsu.trans(instance, p, tag, now, vow)) return false;
        SkillUtil.actionbar(p, switch (property) {
            case NenRules.GUM -> SkillMsg.t("nen.trans.gum", "Transmutation: your aura turns to gum.").withStyle(ChatFormatting.LIGHT_PURPLE);
            case NenRules.RUBBER -> SkillMsg.t("nen.trans.rubber", "Transmutation: your aura turns to rubber.").withStyle(ChatFormatting.YELLOW);
            default -> SkillMsg.t("nen.trans.electric", "Transmutation: your aura turns to lightning.").withStyle(ChatFormatting.BLUE);
        });
        return true;
    }

    /** A second press (or the owner's choice): the window closes, with what it gave. */
    private void end(ManasSkillInstance instance, ServerPlayer p, CompoundTag tag, int mode) {
        tag.putLong(NenRules.untilKey(mode), 0L);
        if (mode == NenRules.ZETSU) p.removeEffect(MobEffects.INVISIBILITY);
        if (mode == NenRules.IN) p.getPersistentData().putLong(NenFx.IN_KEY, 0L);
        if (mode == NenRules.REN) { dropOwn(p, MobEffects.DAMAGE_BOOST, NenRules.renStrengthAmp()); dropOwn(p, MobEffects.MOVEMENT_SPEED, NenRules.renSpeedAmp()); }
        if (mode == NenRules.KEN) dropOwn(p, MobEffects.MOVEMENT_SLOWDOWN, NenRules.kenSlowAmp());
        instance.markDirty();
        SkillUtil.actionbar(p, SkillMsg.t("nen.ended", "Your aura settles.").withStyle(ChatFormatting.GRAY));
    }

    private static void dropOwn(ServerPlayer p, net.minecraft.core.Holder<net.minecraft.world.effect.MobEffect> effect, int amp) {
        MobEffectInstance cur = p.getEffect(effect);
        if (cur != null && cur.getAmplifier() == amp) p.removeEffect(effect);
    }

    static void knock(ServerPlayer p, LivingEntity t, double power) {
        Vec3 away = t.position().subtract(p.position());
        if (away.lengthSqr() < 1.0e-4) return;
        Vec3 dir = away.normalize();
        t.setDeltaMovement(dir.x * power, 0.35, dir.z * power);
        t.hurtMarked = true;
    }

    // ---------------------------------------------------------------- the damage hooks
    /**
     * Ten, Ken, Ko, Zetsu, Ryu and the vow change what a hit does to you (see {@link NenRules#takenMultiplier}); what bypasses invulnerability (the void, kill) is never touched. En's aura feels
     * the first blow every six seconds coming (half), and Ken throws a third of what it was struck with back at the attacker (at most 12, once every ten ticks, through the balance law).
     */
    @Override
    public boolean onTakenDamage(ManasSkillInstance instance, LivingEntity owner, DamageSource source, Changeable<Float> amount) {
        if (amount.get() <= 0 || !NUConfig.NEN_ENABLED.get() || source.is(DamageTypeTags.BYPASSES_INVULNERABILITY)) return true;
        CompoundTag tag = instance.getOrCreateTag();
        long now = owner.level().getGameTime();
        double m = NenRules.takenMultiplier(open(tag, NenRules.TEN, now), open(tag, NenRules.KEN, now), open(tag, NenRules.KO, now), open(tag, NenRules.VOW, now),
                open(tag, NenRules.ZETSU, now), open(tag, NenRules.RYU, now));
        if (open(tag, NenRules.EN, now) && NenRules.enDodgeReady(tag.getLong(EN_DODGE), now)) {
            m *= NenRules.enDodgeMultiplier();
            tag.putLong(EN_DODGE, now);
        }
        if (owner instanceof ServerPlayer p && open(tag, NenRules.KEN, now) && source.getEntity() instanceof LivingEntity attacker && attacker != owner
                && now - tag.getLong(KEN_HIT) >= NenRules.kenReflectGap()) {
            tag.putLong(KEN_HIT, now);
            SkillDamage.hit(p, attacker, Math.min(NenRules.kenReflectMax(), amount.get() * NenRules.kenReflect()), masteryFrac(instance), SkillDamage.Kind.MAGIC);
        }
        if (m != 1.0) amount.set((float) (amount.get() * m));
        return true;
    }

    /**
     * What your blows do: Shu adds to a weapon blow, Ryu and a mark (Gyo, the fist, the needle) multiply it; striking breaks Zetsu, and the blow that breaks it is an ambush (x1.75); the first
     * blow while Ko waits is followed by Ko's strike (22 at once, 44 after a full second of waiting, through the balance law so a player is never one-shot). The hatsu's own magic hits
     * ({@code INDIRECT_MAGIC}) get the mark but not the weapon bonuses and never start Ko's strike, so a volley of bullets is not six weapon blows.
     */
    @Override
    public boolean onDamageEntity(ManasSkillInstance instance, LivingEntity owner, LivingEntity target, DamageSource source, Changeable<Float> amount) {
        if (!(owner instanceof ServerPlayer p) || target == owner || amount.get() <= 0 || !NUConfig.NEN_ENABLED.get()) return true;
        CompoundTag tag = instance.getOrCreateTag();
        long now = p.level().getGameTime();
        boolean blow = !source.is(DamageTypes.INDIRECT_MAGIC);
        float a = amount.get();
        if (blow && open(tag, NenRules.SHU, now) && !p.getMainHandItem().isEmpty()) a += (float) NenRules.shuBonus();      // the aura on the weapon
        if (blow && open(tag, NenRules.RYU, now)) a *= (float) NenRules.ryuDealtMultiplier();
        a *= NenHatsu.markMul(target, p, now);
        if (open(tag, NenRules.ZETSU, now)) {
            if (blow) a *= (float) NenRules.zetsuAmbush();
            tag.putLong(NenRules.untilKey(NenRules.ZETSU), 0L);
            p.removeEffect(MobEffects.INVISIBILITY);
            instance.markDirty();
        }
        if (a != amount.get()) amount.set(a);
        if (blow && open(tag, NenRules.KO, now) && SkillTargets.isAimable(p, target)) {
            double charge = NenRules.koCharge(tag.getLong(NenRules.untilKey(NenRules.KO)), now);
            tag.putLong(NenRules.untilKey(NenRules.KO), 0L);   // closed first: the strike below is itself a hit and must not start another
            instance.markDirty();
            double mul = NenRules.hatsuMultiplier(open(tag, NenRules.VOW, now));
            SkillDamage.hit(p, target, NenRules.koStrike(charge) * mul, masteryFrac(instance));
            knock(p, target, NenRules.enhanceKnockback() * (1.0 + charge));
            Vec3 koAt = target.position().add(0, target.getBbHeight() * 0.5, 0);
            NenFx.burst(p, koAt, 0xFFFF8A4A, (float) (1.0 + charge), false);
            SkillFx.sound(p.serverLevel(), target.position(), SoundEvents.ANVIL_LAND, 0.5F, 1.6F);
        }
        return true;
    }

    /** Dying ends everything a press left running (the chains let go, the volley stops, the field closes). */
    @Override
    public boolean onDeath(ManasSkillInstance instance, LivingEntity owner, DamageSource source) {
        NenHatsu.clearAll(instance.getOrCreateTag());
        return true;
    }
}
