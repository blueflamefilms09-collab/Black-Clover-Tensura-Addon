package com.newuniverse.nusmp.skill;

import com.newuniverse.nusmp.NUConfig;
import com.newuniverse.nusmp.skill.rules.ArgonautMergeRules;
import com.newuniverse.nusmp.skill.rules.ChargeRules;
import com.newuniverse.nusmp.skill.rules.Tier;
import com.newuniverse.nusmp.vfx.VfxShape;
import io.github.manasmods.manascore.skill.api.ManasSkillInstance;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;

/**
 * Argonaut (Bell Cranel): HOLD the skill key to charge: a bell chimes every second (up to five) and each chime costs magicules; RELEASE to
 * unleash a heroic strike on everything in the cone in front of you. Damage grows with the charge (and 1.5x when mastered). Letting go
 * early still strikes with what you have; a strike that hits nothing teaches nothing. Hero's Argonaut (the Ultimate) extends this with ten
 * bells and a piercing beam. Firebolt is folded in (docs/argonaut_rework.md, server setting danmachi.argonautFireboltMerge): a second mode casts Firebolt
 * the moment the key is pressed (own price and cooldown), and a release of two or more bells sets everything it strikes alight. Numbers: {@link ChargeRules} and the config (argonautDamagePerCharge, argonautMagiculePerCharge, argonautCooldown).
 */
public class ArgonautSkill extends NUSkill {
    private static final String PAID = "Paid", BLOCKED = "Blocked";

    static final SkillText TEXT = SkillText.of("argonaut", "Argonaut",
            "Unique. The wish to become a hero. Hold to charge with the chime of bells (up to five) and release to strike everything in front of you, setting it alight. A second mode fires Firebolt: the instant you press, a bolt of lightning-fire.");

    public ArgonautSkill() { this(SkillType.UNIQUE, TEXT, false); }

    protected ArgonautSkill(SkillType type, SkillText text, boolean ultimate) { super(type, text, ultimate); }

    // ------------------------------------------------------------------ modes (Strike 0, Hero's Beam 1, Firebolt appended: ArgonautMergeRules)
    protected ChargeRules.Strike kind(int mode) { return ArgonautMergeRules.kind(mode, ultimate, ArgonautMerge.enabled()); }

    @Override public int getModes(ManasSkillInstance instance) { return ArgonautMergeRules.modes(ultimate, ArgonautMerge.enabled()); }

    @Override
    public int nextMode(LivingEntity entity, ManasSkillInstance instance, int mode, boolean reverse) {
        return ArgonautMergeRules.nextMode(mode, reverse, ultimate, ArgonautMerge.enabled());
    }

    @Override
    public String getModeId(ManasSkillInstance instance, int mode) {
        return switch (kind(mode)) {
            case FIREBOLT -> "argonaut.firebolt";
            case BEAM -> "argonaut.beam";
            case STRIKE -> ultimate || ArgonautMerge.enabled() ? "argonaut.strike" : super.getModeId(instance, mode);
        };
    }

    @Override
    public Component getModeName(ManasSkillInstance instance, int mode) {
        return switch (kind(mode)) {
            case FIREBOLT -> SkillMsg.mode("argonaut.firebolt", "Firebolt");
            case BEAM -> SkillMsg.mode("argonaut.beam", "Hero's Beam");
            case STRIKE -> ultimate ? SkillMsg.mode("argonaut.strike", "Hero's Strike")
                    : ArgonautMerge.enabled() ? SkillMsg.mode("argonaut.charge", "Argonaut") : super.getModeName(instance, mode);
        };
    }

    @Override
    public int getMaxHeldTime(ManasSkillInstance instance, LivingEntity entity) {
        return ChargeRules.maxHeldTicks(ultimate ? Tier.ULTIMATE : Tier.BASE);
    }

    /** A charge that ran out of magicules still strikes with what was paid for. */
    @Override
    public boolean shouldTriggerReleaseOnHeldInterrupt(ManasSkillInstance instance, LivingEntity entity, int heldTicks, int mode) {
        return true;
    }

    @Override
    public void onPressed(ManasSkillInstance instance, LivingEntity entity, int keyNumber, int mode) {
        if (!(entity instanceof ServerPlayer player)) return;
        var tag = instance.getOrCreateTag();
        if (kind(mode) == ChargeRules.Strike.FIREBOLT) {            // Firebolt: chantless, the bolt leaves the moment the key is pressed
            tag.putInt(PAID, 0);
            tag.putBoolean(BLOCKED, true);                           // nothing to charge: onHeld stops, onRelease strikes nothing
            instance.markDirty();
            FireboltSkill.cast(this, instance, player, mode, ArgonautMergeRules.FIREBOLT_MASTERY_PER_HIT);
            return;
        }
        tag.putInt(PAID, 0);
        boolean cooling = instance.onCoolDown(mode);
        tag.putBoolean(BLOCKED, cooling);
        if (cooling) SkillUtil.fail(player, SkillMsg.t("argonaut.cooldown", "Argonaut is recovering."));
        instance.markDirty();
    }

    @Override
    public boolean onHeld(ManasSkillInstance instance, LivingEntity living, int heldTicks, int mode) {
        if (!(living instanceof ServerPlayer player)) return true;
        var tag = instance.getOrCreateTag();
        if (tag.getBoolean(BLOCKED)) return false;
        Tier tier = tier(instance, player);
        if (!ChargeRules.chimeNow(heldTicks, tier)) return true;

        int paid = tag.getInt(PAID);
        if (paid >= ChargeRules.maxCharge(tier)) return true;
        if (!pay(instance, player, mode, NUConfig.ARGONAUT_MAGICULE_PER_CHARGE.get())) return false;
        paid++;
        tag.putInt(PAID, paid);
        instance.markDirty();

        ServerLevel level = player.serverLevel();
        SkillFx.sound(level, player.position(), SoundEvents.BELL_BLOCK, 1.0F, 0.8F + paid * 0.12F);
        SkillFx.spray(level, false, player.position().add(0, 1, 0), 4 + paid, 0.4, 0.6, 0.4, 0.02);
        SkillAnimeFx.chime(level, player, paid);                                           // VFX wave 3: the bell of light (one per charge step)
        if (paid >= ChargeRules.maxCharge(tier)) {
            SkillUtil.actionbar(player, SkillMsg.t("argonaut.full", "Fully charged: release!").withStyle(ChatFormatting.GOLD));
        } else {
            SkillUtil.actionbar(player, SkillMsg.t("argonaut.charge", "Charge: %s/%s", paid, ChargeRules.maxCharge(tier)).withStyle(ChatFormatting.WHITE));
        }
        return true;
    }

    @Override
    public void onRelease(ManasSkillInstance instance, LivingEntity entity, int heldTicks, int keyNumber, int mode) {
        if (!(entity instanceof ServerPlayer player)) return;
        var tag = instance.getOrCreateTag();
        int charge = tag.getInt(PAID);
        boolean blocked = tag.getBoolean(BLOCKED);
        tag.putInt(PAID, 0);
        tag.putBoolean(BLOCKED, false);
        instance.markDirty();
        if (blocked || charge <= 0 || !player.isAlive() || kind(mode) == ChargeRules.Strike.FIREBOLT) return;

        int hits = release(instance, player, charge, mode);
        cooldown(instance, mode, NUConfig.ARGONAUT_COOLDOWN.get());
        SkillUtil.castVfx(player, 0xFFFFFFFF);
        grow(instance, player, ChargeRules.masteryGain(charge, hits));
    }

    /** The strike itself; returns how many creatures it hit. Hero's Argonaut overrides this for its beam. */
    protected int release(ManasSkillInstance instance, ServerPlayer player, int charge, int mode) {
        return strike(instance, player, charge);
    }

    /** Seconds the charged release burns what it hits: 0 with the merge off (the old plain strike). */
    protected static int fireSeconds(int charge) { return ArgonautMerge.enabled() ? ArgonautMergeRules.chargedIgniteSeconds(charge) : 0; }

    /**
     * The Firebolt-augmented look of a charged release (the anime's Argonaut-charged Firebolt): one orange fire-lightning bolt along the strike and, on a full charge,
     * one visual-only lightning strike on the first creature hit. Cost: one effect packet and at most one visual entity per release (a release is at most one per cooldown).
     */
    protected static void fireFlavour(ServerLevel level, ServerPlayer player, Vec3 tip, LivingEntity first, int charge, Tier tier) {
        Vec3 hand = player.getEyePosition().add(player.getViewVector(1.0F).scale(0.8)).add(0, -0.25, 0);
        SkillAnimeFx.firebolt(level, hand, tip, 0xFFFF8A2A, 0.6f + 0.08f * charge);
        if (first != null && charge >= ChargeRules.maxCharge(tier)) FireboltSkill.strikeVisual(level, first.position());
    }

    /** The cone strike: everything in front of the caster takes the charge's damage and is thrown back. */
    protected final int strike(ManasSkillInstance instance, ServerPlayer player, int charge) {
        ServerLevel level = player.serverLevel();
        Tier tier = tier(instance, player);
        double damage = ChargeRules.damage(NUConfig.ARGONAUT_DAMAGE_PER_CHARGE.get(), charge, tier, instance.isMastered(player));
        Vec3 eye = player.getEyePosition();
        Vec3 look = player.getViewVector(1.0F);
        int hits = 0;
        int burn = fireSeconds(charge);
        LivingEntity first = null;
        for (LivingEntity t : SkillTargets.foesInCone(player, ChargeRules.strikeReach(tier), ChargeRules.strikeCone(tier))) {
            if (SkillDamage.hit(player, t, damage, masteryFrac(instance), instance) > 0f) {
                t.knockback(ChargeRules.knockback(charge), -look.x, -look.z);
                if (burn > 0) t.igniteForSeconds(burn);
                if (first == null) first = t;
                hits++;
            }
        }
        Vec3 tip = eye.add(look.scale(ChargeRules.strikeReach(tier)));
        if (burn > 0) fireFlavour(level, player, tip, first, charge, tier);
        SkillFx.shape(level, VfxShape.LIGHT_BLADE, eye.add(look.scale(0.8)).add(0, -0.3, 0), tip, 0xFFFFF2B0, 10, 1.0f + charge * 0.12f);
        SkillAnimeFx.argoStrike(level, eye, tip, charge);                                  // VFX wave 3: the white crescent
        SkillFx.spray(level, false, eye.add(look.scale(2.0)).add(0, -0.3, 0), 6 + charge, 1.0, 0.3, 1.0, 0.0);
        SkillFx.sound(level, player.position(), SoundEvents.PLAYER_ATTACK_SWEEP, 1.5F, 0.6F);
        return hits;
    }
}
