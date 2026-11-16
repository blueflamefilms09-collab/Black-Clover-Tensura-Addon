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
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;

/**
 * Argonaut (Bell Cranel): HOLD the skill key to charge: a bell chimes every second (up to five) and each chime costs magicules; RELEASE to
 * unleash a heroic strike on everything in the cone in front of you. Damage grows with the charge (and 1.5x when mastered). Letting go
 * early still strikes with what you have; a strike that hits nothing teaches nothing. Hero's Argonaut (the Ultimate) extends this with ten
 * bells and a piercing beam. Wiki canon (docs/argonaut_rework.md section 3, server setting danmachi.argonautWikiCanon): the cap is 3 : 4 : 5 bells by mastery, a bell costs stamina too,
 * a hit breaks the charge, charging while walking and Dual Charge (Argo Vesta) are learned with mastery, the weapon takes recoil. Firebolt is folded in (docs/argonaut_rework.md, server setting danmachi.argonautFireboltMerge): a second mode casts Firebolt
 * the moment the key is pressed (own price and cooldown), and a release of two or more bells sets everything it strikes alight. Numbers: {@link ChargeRules} and the config (argonautDamagePerCharge, argonautMagiculePerCharge, argonautCooldown).
 */
public class ArgonautSkill extends NUSkill {
    private static final String PAID = "Paid", BLOCKED = "Blocked", HP = "Hp", PX = "Px", PZ = "Pz";

    /** Argo Vesta's crimson (wiki: the blade gives off a crimson light as the charge grows). */
    static final int VESTA_CRIMSON = 0xFFD8202E;

    static final SkillText TEXT = SkillText.of("argonaut", "Argonaut",
            "Unique. The Hero Wish. Hold to charge with the chime of a small bell (three, then four, then five as you master it); stamina and magicules are spent, a hit breaks the charge. Release for the Hero's Strike. Other modes: instant Firebolt, Firebolt carrying the charge, and Argo Vesta.");

    public ArgonautSkill() { this(SkillType.UNIQUE, TEXT, false); }

    protected ArgonautSkill(SkillType type, SkillText text, boolean ultimate) { super(type, text, ultimate); }

    // ------------------------------------------------------------------ modes (Strike 0, Hero's Beam 1, Firebolt appended: ArgonautMergeRules)
    protected ChargeRules.Strike kind(int mode) { return ArgonautMergeRules.kind(mode, ultimate, ArgonautMerge.enabled(), ArgonautMerge.canon()); }

    @Override public int getModes(ManasSkillInstance instance) { return ArgonautMergeRules.modes(ultimate, ArgonautMerge.enabled(), ArgonautMerge.canon()); }

    /** Bells this wearer can charge: 3 : 4 : 5 by mastery in canon (the Ultimate ten), else the old 5 / 10. */
    protected int cap(ManasSkillInstance instance, LivingEntity entity) {
        Tier tier = tier(instance, entity);
        return ArgonautMerge.canon() ? ChargeRules.canonMaxCharge(tier, masteryFrac(instance)) : ChargeRules.maxCharge(tier);
    }

    @Override
    public int nextMode(LivingEntity entity, ManasSkillInstance instance, int mode, boolean reverse) {
        return ArgonautMergeRules.nextMode(mode, reverse, ultimate, ArgonautMerge.enabled(), ArgonautMerge.canon());
    }

    @Override
    public String getModeId(ManasSkillInstance instance, int mode) {
        return switch (kind(mode)) {
            case FIREBOLT -> "argonaut.firebolt";
            case CHARGED_FIREBOLT -> "argonaut.charged_firebolt";
            case ARGO_VESTA -> "argonaut.argo_vesta";
            case BEAM -> "argonaut.beam";
            case STRIKE -> ultimate || ArgonautMerge.enabled() ? "argonaut.strike" : super.getModeId(instance, mode);
        };
    }

    @Override
    public Component getModeName(ManasSkillInstance instance, int mode) {
        return switch (kind(mode)) {
            case FIREBOLT -> SkillMsg.mode("argonaut.firebolt", "Firebolt");
            case CHARGED_FIREBOLT -> SkillMsg.mode("argonaut.charged_firebolt", "Charged Firebolt");
            case ARGO_VESTA -> SkillMsg.mode("argonaut.argo_vesta", "Argo Vesta");
            case BEAM -> SkillMsg.mode("argonaut.beam", "Pseudo-Afterglow");
            case STRIKE -> ultimate ? SkillMsg.mode("argonaut.strike", "Grand Bell")
                    : ArgonautMerge.enabled() ? SkillMsg.mode("argonaut.charge", "Hero's Strike") : super.getModeName(instance, mode);
        };
    }

    @Override
    public int getMaxHeldTime(ManasSkillInstance instance, LivingEntity entity) {
        return ChargeRules.maxHeldTicks(cap(instance, entity));
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
        boolean locked = !ArgonautMergeRules.unlocked(kind(mode), ArgonautMerge.canon(), ultimate, masteryFrac(instance));
        tag.putBoolean(BLOCKED, cooling || locked);
        if (cooling) SkillUtil.fail(player, SkillMsg.t("argonaut.cooldown", "Argonaut is recovering."));
        else if (locked) SkillUtil.fail(player, SkillMsg.t("argonaut.locked", "Argo Vesta (Dual Charge) is learned with more Argonaut mastery."));
        tag.putFloat(HP, player.getHealth());                        // canon: a hit while charging breaks the charge
        tag.putDouble(PX, player.getX());                            // canon: charging while walking is learned with mastery
        tag.putDouble(PZ, player.getZ());
        instance.markDirty();
    }

    @Override
    public boolean onHeld(ManasSkillInstance instance, LivingEntity living, int heldTicks, int mode) {
        if (!(living instanceof ServerPlayer player)) return true;
        var tag = instance.getOrCreateTag();
        if (tag.getBoolean(BLOCKED)) return false;
        boolean canon = ArgonautMerge.canon();
        if (canon) {                                                  // wiki: the charge is lost when Bell is hit; early on he cannot charge while moving
            float hp = player.getHealth();
            if (ArgonautMergeRules.brokenByHit(tag.getFloat(HP), hp)) return breakCharge(instance, player, "argonaut.broken", "Your charge broke: you were hit.");
            if (hp > tag.getFloat(HP)) tag.putFloat(HP, hp);
            if (!ArgonautMergeRules.canChargeMoving(true, ultimate, masteryFrac(instance))
                    && ArgonautMergeRules.movedTooFar(player.getX() - tag.getDouble(PX), player.getZ() - tag.getDouble(PZ)))
                return breakCharge(instance, player, "argonaut.moving", "You cannot charge while moving yet: keep still, or master Argonaut.");
        }
        int cap = cap(instance, player);
        if (!ChargeRules.chimeNow(heldTicks, cap)) return true;

        int paid = tag.getInt(PAID);
        if (paid >= cap) return true;
        if (!pay(instance, player, mode, NUConfig.ARGONAUT_MAGICULE_PER_CHARGE.get())) return false;
        paid++;
        tag.putInt(PAID, paid);
        instance.markDirty();

        ServerLevel level = player.serverLevel();
        if (ArgonautMerge.canon() && ultimate) SkillFx.sound(level, player.position(), SoundEvents.BELL_RESONATE, 0.7F, 0.6F + paid * 0.05F);   // the Ultimate rings the grand bell (wiki: Grand Bell), the Unique a small chime
        else SkillFx.sound(level, player.position(), SoundEvents.BELL_BLOCK, 1.0F, 0.8F + paid * 0.12F);
        if (kind(mode) == ChargeRules.Strike.ARGO_VESTA && ArgonautMerge.canon())                // Argo Vesta: the blade glows crimson and brighter with every bell (one packet per bell)
            SkillFx.shape(level, VfxShape.LIGHT_FLARE, player.getEyePosition().add(player.getViewVector(1.0F).scale(0.9)).add(0, -0.45, 0), player.getEyePosition(), VESTA_CRIMSON, 14, 0.5f + 0.12f * paid);
        SkillFx.spray(level, false, player.position().add(0, 1, 0), 4 + paid, 0.4, 0.6, 0.4, 0.02);
        SkillAnimeFx.chime(level, player, paid);                                           // VFX wave 3: the bell of light (one per charge step)
        if (paid >= cap) {
            SkillUtil.actionbar(player, SkillMsg.t("argonaut.full", "Fully charged: release!").withStyle(ChatFormatting.GOLD));
        } else {
            SkillUtil.actionbar(player, SkillMsg.t("argonaut.charge", "Charge: %s/%s", paid, cap).withStyle(ChatFormatting.WHITE));
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
        ChargeRules.Strike k = kind(mode);
        if (ArgonautMerge.canon() && (k == ChargeRules.Strike.STRIKE || k == ChargeRules.Strike.ARGO_VESTA)) {   // wiki: recoil wears whatever was charged
            int wear = ArgonautMergeRules.recoilWear(charge, ultimate);
            var hand = player.getMainHandItem();
            if (wear > 0 && hand.isDamageableItem()) hand.hurtAndBreak(wear, player, EquipmentSlot.MAINHAND);
        }
        cooldown(instance, mode, NUConfig.ARGONAUT_COOLDOWN.get());
        SkillUtil.castVfx(player, 0xFFFFFFFF);
        grow(instance, player, ChargeRules.masteryGain(charge, hits));
    }

    /** The charge is lost (nothing refunded, as in the wiki); the hold stops. */
    private boolean breakCharge(ManasSkillInstance instance, ServerPlayer player, String key, String fallback) {
        var tag = instance.getOrCreateTag();
        tag.putInt(PAID, 0);
        tag.putBoolean(BLOCKED, true);
        instance.markDirty();
        SkillUtil.fail(player, SkillMsg.t(key, fallback));
        return false;
    }

    /** The release; returns how many creatures it hit. Hero's Argonaut overrides this for its beam. */
    protected int release(ManasSkillInstance instance, ServerPlayer player, int charge, int mode) {
        switch (kind(mode)) {
            case CHARGED_FIREBOLT:                       // the wiki's "Argonaut charged Firebolt": the bells ride on one bolt (Firebolt's own price at release)
                if (!pay(instance, player, mode, NUConfig.FIREBOLT_MAGICULE_COST.get())) return 0;
                return FireboltSkill.fire(this, instance, player, charge) ? 1 : 0;
            case ARGO_VESTA: {                           // Firebolt cast onto the blade, both charged at once: a burning slash with a shockwave
                boolean infused = pay(instance, player, mode, NUConfig.FIREBOLT_MAGICULE_COST.get());
                return strike(instance, player, charge, infused ? ChargeRules.Strike.ARGO_VESTA : ChargeRules.Strike.STRIKE);
            }
            default:
                return strike(instance, player, charge, ChargeRules.Strike.STRIKE);
        }
    }

    /** Seconds a charged release of this kind burns what it hits: 0 with the merge off (the old plain strike), else per {@link ArgonautMergeRules#burns}. */
    protected static int fireSeconds(int charge, ChargeRules.Strike kind) {
        return ArgonautMerge.enabled() && ArgonautMergeRules.burns(kind, ArgonautMerge.canon()) ? ArgonautMergeRules.chargedIgniteSeconds(charge) : 0;
    }

    /**
     * The Firebolt-augmented look of a charged release (the anime's Argonaut-charged Firebolt): one orange fire-lightning bolt along the strike and, on a full charge,
     * one visual-only lightning strike on the first creature hit. Cost: one effect packet and at most one visual entity per release (a release is at most one per cooldown).
     */
    protected static void fireFlavour(ServerLevel level, ServerPlayer player, Vec3 tip, LivingEntity first, int charge, Tier tier) {
        Vec3 hand = player.getEyePosition().add(player.getViewVector(1.0F).scale(0.8)).add(0, -0.25, 0);
        SkillAnimeFx.firebolt(level, hand, tip, 0xFFFF8A2A, 0.6f + 0.08f * charge);
        if (first != null && charge >= ChargeRules.maxCharge(tier)) FireboltSkill.strikeVisual(level, first.position());
    }

    /**
     * Argo Vesta's release as the wiki describes it: a crimson blade slash, a flash of light, a heatwave (fire burst) and a shockwave (a projected crescent plus a boom).
     * Cost: four effect packets and two sounds per release (a release is at most one per cooldown); all go through SkillFx's nearby-players-only budget.
     */
    private static void vestaLook(ServerLevel level, Vec3 eye, Vec3 tip, int charge) {
        Vec3 from = eye.add(0, -0.3, 0);
        SkillFx.shape(level, VfxShape.LIGHT_BLADE, from, tip, VESTA_CRIMSON, 12, 1.0f + charge * 0.14f);                     // the crimson blade
        SkillFx.shape(level, VfxShape.LIGHT_FLARE, tip, tip, 0xFFFFFFFF, 10, 1.5f + charge * 0.1f);                           // the flash of light
        SkillFx.shape(level, VfxShape.FIRE_BURST, tip, tip, 0xFFFF5A2A, 14, 1.0f + charge * 0.1f);                            // the heatwave
        SkillFx.shape(level, VfxShape.SLASH_WAVE, from, tip, 0xFFFFD0C0, 12, 1.2f + charge * 0.15f);                          // the shockwave
        SkillFx.sound(level, tip, SoundEvents.WARDEN_SONIC_BOOM, 0.5F, 1.3F);
    }

    /** The cone strike: everything in front of the caster takes the charge's damage and is thrown back. */
    protected final int strike(ManasSkillInstance instance, ServerPlayer player, int charge, ChargeRules.Strike kind) {
        ServerLevel level = player.serverLevel();
        Tier tier = tier(instance, player);
        boolean vesta = kind == ChargeRules.Strike.ARGO_VESTA;
        double damage = ChargeRules.damage(NUConfig.ARGONAUT_DAMAGE_PER_CHARGE.get(), charge, tier, instance.isMastered(player))
                * (vesta ? ArgonautMergeRules.vestaFactor() : 1.0);
        Vec3 eye = player.getEyePosition();
        Vec3 look = player.getViewVector(1.0F);
        int hits = 0;
        int burn = fireSeconds(charge, kind);
        LivingEntity first = null;
        for (LivingEntity t : SkillTargets.foesInCone(player, ChargeRules.strikeReach(tier), ChargeRules.strikeCone(tier))) {
            if (SkillDamage.hit(player, t, damage, masteryFrac(instance), instance) > 0f) {
                t.knockback(ChargeRules.knockback(charge) * (vesta ? ArgonautMergeRules.vestaKnockback() : 1.0), -look.x, -look.z);
                if (burn > 0) t.igniteForSeconds(burn);
                if (first == null) first = t;
                hits++;
            }
        }
        Vec3 tip = eye.add(look.scale(ChargeRules.strikeReach(tier)));
        if (burn > 0) fireFlavour(level, player, tip, first, charge, tier);
        if (vesta) vestaLook(level, eye, tip, charge);
        else SkillFx.shape(level, VfxShape.LIGHT_BLADE, eye.add(look.scale(0.8)).add(0, -0.3, 0), tip, 0xFFFFF2B0, 10, 1.0f + charge * 0.12f);
        SkillAnimeFx.argoStrike(level, eye, tip, charge);                                  // VFX wave 3: the white crescent
        SkillFx.spray(level, false, eye.add(look.scale(2.0)).add(0, -0.3, 0), 6 + charge, 1.0, 0.3, 1.0, 0.0);
        SkillFx.sound(level, player.position(), SoundEvents.PLAYER_ATTACK_SWEEP, 1.5F, 0.6F);
        return hits;
    }
}
