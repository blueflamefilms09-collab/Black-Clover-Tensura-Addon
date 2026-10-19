package com.newuniverse.nusmp.skill;

import com.newuniverse.nusmp.celestial.CelestialAttachments;
import com.newuniverse.nusmp.celestial.CelestialData;
import com.newuniverse.nusmp.celestial.CelestialDress;
import com.newuniverse.nusmp.celestial.CelestialKey;
import com.newuniverse.nusmp.celestial.CelestialLedger;
import com.newuniverse.nusmp.celestial.CelestialNet;
import com.newuniverse.nusmp.celestial.CelestialProps;
import com.newuniverse.nusmp.celestial.CelestialRules;
import com.newuniverse.nusmp.celestial.KingRules;
import com.newuniverse.nusmp.prop.MagicPropEntity;
import com.newuniverse.nusmp.prop.MagicProps;
import com.newuniverse.nusmp.prop.PropKind;
import io.github.manasmods.manascore.skill.api.ManasSkillInstance;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/**
 * Celestial Spirit Magic (Lucy Heartfilia): a Tensura UNIQUE skill, learned from its own book ({@code celestial.CelestialSkillBookItem}). The skill has no spell list of its
 * own: it opens the gates of the stars. Its modes are the spirits of the keys the player has collected (the twelve Gold Zodiac spirits, then the silver ones), a Dismiss, the
 * Star Dress forms (a spirit's power worn instead of called; each opens with the mastery of the skill and the bond with its spirit), and two modes the Spirit King leaves
 * behind (his Blessing, then the summon of the king himself). A spirit answers only while its key is carried (in the pack or the Celestial Key slot of Curios), only if a
 * contract is sealed with it (hold the key's use button), and costs a share of the magicules, a cooldown and a place among the few spirits the mastery allows at once.
 * The numbers are {@link CelestialKey} and {@link CelestialRules}; what a spirit does is {@link CelestialArts}.
 */
public class CelestialSpiritMagicSkill extends NUSkill {
    static final SkillText TEXT = SkillText.of("celestial_spirit_magic", "Celestial Spirit Magic",
            "Unique. Call the spirits of the stars with their golden and silver keys, wear their power as Star Dress forms, and open the Celestial Gate when all twelve Gold Zodiac keys are yours.");

    /** Mode indexes (append-only): 0..15 the spirits in the order of {@link CelestialKey}, then these. */
    public static final int SPIRITS = 16, DISMISS = 16, DRESS0 = 17, BLESSING = 29, KING = 30, MODES = 31;

    public CelestialSpiritMagicSkill() { super(SkillType.UNIQUE, TEXT, false); }

    @Override public int getModes(ManasSkillInstance instance) { return MODES; }

    /** The next mode the player can actually use: the wheel skips spirits without a key, closed dresses and king modes not yet earned. */
    @Override
    public int nextMode(LivingEntity entity, ManasSkillInstance instance, int mode, boolean reverse) {
        for (int step = 1; step <= MODES; step++) {
            int m = Math.floorMod(reverse ? mode - step : mode + step, MODES);
            if (available(entity, instance, m)) return m;
        }
        return mode;
    }

    /** Whether the wheel shows this mode to this player. */
    public static boolean available(LivingEntity entity, ManasSkillInstance instance, int mode) {
        CelestialNet.View v = CelestialNet.view(entity);
        if (mode < SPIRITS) return v.has(CelestialKey.byOrdinal(mode));
        if (mode == DISMISS) return true;
        if (mode >= DRESS0 && mode < DRESS0 + 12) {
            CelestialKey k = CelestialKey.byOrdinal(mode - DRESS0);
            return CelestialRules.starDressUnlocked(k, v.isBound(k), v.bondLevel(k), masteryFrac(instance));
        }
        if (mode == BLESSING) return v.blessing();
        if (mode == KING) return v.summon();
        return false;
    }

    @Override
    public String getModeId(ManasSkillInstance instance, int mode) {
        return "celestial_spirit_magic." + modeKey(mode);
    }

    private static String modeKey(int mode) {
        if (mode < SPIRITS) return CelestialKey.byOrdinal(mode).id;
        if (mode == DISMISS) return "dismiss";
        if (mode >= DRESS0 && mode < DRESS0 + 12) return "dress_" + CelestialKey.byOrdinal(mode - DRESS0).id;
        return mode == BLESSING ? "blessing" : "king";
    }

    @Override
    public Component getModeName(ManasSkillInstance instance, int mode) {
        if (mode < SPIRITS) {
            CelestialKey k = CelestialKey.byOrdinal(mode);
            return Component.translatableWithFallback("nusmp.skill.mode.celestial." + k.id, k.display + ": " + k.ability);
        }
        if (mode == DISMISS) return SkillMsg.mode("celestial.dismiss", "Send the Spirits Home");
        if (mode >= DRESS0 && mode < DRESS0 + 12) {
            CelestialKey k = CelestialKey.byOrdinal(mode - DRESS0);
            return Component.translatableWithFallback("nusmp.skill.mode.celestial.dress_" + k.id, "Star Dress: " + k.dressName);
        }
        return mode == BLESSING ? SkillMsg.mode("celestial.blessing", "The Spirit King's Blessing") : SkillMsg.mode("celestial.king", "Summon the Spirit King");
    }

    // ================================================================ use
    @Override
    public void onPressed(ManasSkillInstance instance, LivingEntity entity, int slot, int mode) {
        if (!(entity instanceof ServerPlayer p) || !p.isAlive()) return;
        if (mode < SPIRITS) summon(instance, p, CelestialKey.byOrdinal(mode), mode);
        else if (mode == DISMISS) dismiss(p);
        else if (mode >= DRESS0 && mode < DRESS0 + 12) dress(instance, p, CelestialKey.byOrdinal(mode - DRESS0), mode);
        else if (mode == BLESSING) blessing(instance, p, mode);
        else if (mode == KING) kingSummon(instance, p, mode);
    }

    private static int spiritsOut(ServerPlayer p) {
        return p.serverLevel().getEntitiesOfClass(MagicPropEntity.class, new AABB(p.position(), p.position()).inflate(96),
                e -> e.kind() == PropKind.CELESTIAL_1 && e.ownerId() != null && e.ownerId().equals(p.getUUID()) && e.isAlive()).size();
    }

    private void summon(ManasSkillInstance instance, ServerPlayer p, CelestialKey key, int mode) {
        CelestialData data = CelestialAttachments.of(p);
        ItemStack keyStack = CelestialLedger.find(p, key);
        double max = CelestialLedger.maxMagicules(p);
        int bond = data.bondLevel(key);
        double cost = CelestialRules.cost(key, max, bond);
        double ms = masteryFrac(instance);
        CelestialRules.Refusal why = CelestialRules.check(key, !keyStack.isEmpty(), data.isBound(key), CelestialLedger.ownerOf(keyStack), p.getUUID().toString(),
                instance.onCoolDown(mode) ? 1 : 0, p.isCreative() ? 1.0e12 : CelestialLedger.magicules(p), cost, spiritsOut(p), CelestialRules.spiritCap(ms));
        if (why != CelestialRules.Refusal.NONE) {
            SkillUtil.fail(p, refusal(why, key));
            return;
        }
        if (!CelestialLedger.spend(p, cost)) {
            SkillUtil.fail(p, refusal(CelestialRules.Refusal.NOT_ENOUGH_MAGICULES, key));
            return;
        }
        ServerLevel sl = p.serverLevel();
        Vec3 look = p.getViewVector(1f).multiply(1, 0, 1);
        look = look.lengthSqr() < 1e-4 ? new Vec3(0, 0, 1) : look.normalize();
        Vec3 at = CelestialProps.standingPlace(sl, p.position().add(look.scale(2.8)), p.getY());
        int life = key.stay + CelestialArts.actionTicks(key);
        MagicPropEntity spirit = MagicProps.spawn(sl, PropKind.CELESTIAL_1, at, p.getYRot() + 180f, CelestialProps.scaleOf(key), life, key.ordinal(), p);
        if (spirit == null) {
            CelestialLedger.refund(p, cost);
            SkillUtil.fail(p, SkillMsg.t("celestial.failed", "The gate would not open."));
            return;
        }
        CelestialArts.stamp(spirit, key, bond, ms, com.newuniverse.nusmp.balance.BalanceLaw.beginControl(p));
        cooldown(instance, mode, CelestialRules.cooldown(key, bond));
        if (data.addBond(key, 1)) p.displayClientMessage(SkillMsg.t("celestial.bond_up", "Your bond with %s deepens.", key.spirit).withStyle(ChatFormatting.AQUA), true);
        SkillUtil.castVfx(p, 0xFF000000 | key.rgb);
        grow(instance, p, 1.0 + (key.isGold() ? 0.5 : 0.0));
        CelestialNet.sync(p);
    }

    private static Component refusal(CelestialRules.Refusal why, CelestialKey key) {
        return switch (why) {
            case NO_KEY -> SkillMsg.t("celestial.no_key", "You do not carry the %s key.", key.display);
            case NOT_BOUND -> SkillMsg.t("celestial.not_bound", "No contract with %s yet: hold the key's use button to seal one.", key.spirit);
            case SOMEONE_ELSES -> SkillMsg.t("celestial.not_yours", "That key keeps a contract with someone else.");
            case COOLDOWN -> SkillMsg.t("celestial.cooldown", "%s is resting.", key.spirit);
            case TOO_MANY_SPIRITS -> SkillMsg.t("celestial.too_many", "Your mastery lets you keep fewer spirits at once.");
            case NOT_ENOUGH_MAGICULES -> SkillMsg.t("no_magicules", "Not enough magicules (need %s).", "more");
            default -> SkillMsg.t("celestial.no_spirit", "That key calls no spirit.");
        };
    }

    private void dismiss(ServerPlayer p) {
        int n = 0;
        for (MagicPropEntity e : p.serverLevel().getEntitiesOfClass(MagicPropEntity.class, new AABB(p.position(), p.position()).inflate(96),
                x -> (x.kind() == PropKind.CELESTIAL_1 || x.kind() == PropKind.CELESTIAL_2) && x.ownerId() != null && x.ownerId().equals(p.getUUID()))) {
            e.expire();
            n++;
        }
        SkillUtil.actionbar(p, SkillMsg.t("celestial.dismissed", "%s spirits return to the stars.", n));
        SkillFx.sound(p.serverLevel(), p.position(), SoundEvents.AMETHYST_BLOCK_CHIME, 1.0f, 0.8f);
    }

    // ================================================================ Star Dress
    private void dress(ManasSkillInstance instance, ServerPlayer p, CelestialKey key, int mode) {
        CelestialData data = CelestialAttachments.of(p);
        if (!CelestialRules.starDressUnlocked(key, data.isBound(key), data.bondLevel(key), masteryFrac(instance))) {
            SkillUtil.fail(p, SkillMsg.t("celestial.dress_closed", "The %s is not open to you yet.", key.dressName));
            return;
        }
        if (instance.onCoolDown(mode)) {
            SkillUtil.fail(p, SkillMsg.t("celestial.cooldown", "%s is resting.", key.spirit));
            return;
        }
        if (!CelestialLedger.spend(p, CelestialRules.dressCost(key, CelestialLedger.maxMagicules(p)))) {
            SkillUtil.fail(p, refusal(CelestialRules.Refusal.NOT_ENOUGH_MAGICULES, key));
            return;
        }
        long until = p.level().getGameTime() + CelestialRules.DRESS_TICKS;
        CelestialDress.apply(p, key, until);
        cooldown(instance, mode, CelestialRules.DRESS_COOLDOWN);
        ServerLevel sl = p.serverLevel();
        SkillFx.follow(sl, com.newuniverse.nusmp.vfx.VfxShape.STAR_MAGIC, p, p.position().add(0, 1, 0), 0xFF000000 | key.rgb, 40, 1.4f);
        SkillFx.follow(sl, com.newuniverse.nusmp.vfx.VfxShape.DIVINE_HALO, p, p.position().add(0, 1, 0), 0xFF000000 | key.rgb, 40, 1.0f);
        SkillFx.ring(sl, p.position(), 3.0, key.rgb);
        SkillFx.sound(sl, p.position(), SoundEvents.BEACON_ACTIVATE, 1.2f, 1.4f);
        SkillUtil.actionbar(p, SkillMsg.t("celestial.dress_on", "Star Dress: %s.", key.dressName).copy().withStyle(ChatFormatting.GOLD));
        data.addBond(key, 2);
        grow(instance, p, 2.0);
        CelestialNet.sync(p);
    }

    // ================================================================ the king's gifts
    private void blessing(ManasSkillInstance instance, ServerPlayer p, int mode) {
        CelestialData data = CelestialAttachments.of(p);
        if (!data.blessingMode()) { SkillUtil.fail(p, SkillMsg.t("celestial.king_closed", "The Spirit King has not given you this.")); return; }
        if (instance.onCoolDown(mode)) { SkillUtil.fail(p, SkillMsg.t("celestial.cooldown", "%s is resting.", "The King")); return; }
        if (!CelestialLedger.spend(p, Math.max(120, CelestialLedger.maxMagicules(p) * 0.15))) { SkillUtil.fail(p, refusal(CelestialRules.Refusal.NOT_ENOUGH_MAGICULES, CelestialKey.LEO)); return; }
        ServerLevel sl = p.serverLevel();
        for (LivingEntity a : CelestialArts.alliesNear(p, p.position(), 20)) {
            CelestialArts.cleanse(a);
            a.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 600, 1));
            a.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 600, 0));
            a.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 600, 0));
            a.addEffect(new MobEffectInstance(MobEffects.ABSORPTION, 600, 2));
            SkillFx.shape(sl, com.newuniverse.nusmp.vfx.VfxShape.DIVINE_PILLAR, a.position(), a.position().add(0, 1, 0), 0xFFFFF3B0, 40, 1.0f);
        }
        SkillFx.ring(sl, p.position(), 8.0, 0xFFF3B0);
        SkillFx.sound(sl, p.position(), SoundEvents.BEACON_POWER_SELECT, 1.4f, 1.0f);
        cooldown(instance, mode, 6000);
        grow(instance, p, 3.0);
    }

    private void kingSummon(ManasSkillInstance instance, ServerPlayer p, int mode) {
        CelestialData data = CelestialAttachments.of(p);
        if (!data.summonMode()) { SkillUtil.fail(p, SkillMsg.t("celestial.king_closed", "The Spirit King has not given you this.")); return; }
        if (instance.onCoolDown(mode)) { SkillUtil.fail(p, SkillMsg.t("celestial.cooldown", "%s is resting.", "The King")); return; }
        double cost = Math.max(400, CelestialLedger.maxMagicules(p) * KingRules.SUMMON_COST_PERMILLE / 1000.0);
        if (!CelestialLedger.spend(p, cost)) { SkillUtil.fail(p, refusal(CelestialRules.Refusal.NOT_ENOUGH_MAGICULES, CelestialKey.LEO)); return; }
        ServerLevel sl = p.serverLevel();
        Vec3 look = p.getViewVector(1f).multiply(1, 0, 1);
        look = look.lengthSqr() < 1e-4 ? new Vec3(0, 0, 1) : look.normalize();
        Vec3 at = CelestialProps.standingPlace(sl, p.position().add(look.scale(5)), p.getY());
        MagicPropEntity king = MagicProps.spawn(sl, PropKind.CELESTIAL_2, at, p.getYRot() + 180f, 2.6f, KingRules.SUMMON_STAY, 1, p);
        if (king == null) { CelestialLedger.refund(p, cost); SkillUtil.fail(p, SkillMsg.t("celestial.failed", "The gate would not open.")); return; }
        CelestialArts.stamp(king, CelestialKey.SPIRIT_KING_GATE, 5, masteryFrac(instance), com.newuniverse.nusmp.balance.BalanceLaw.beginControl(p));
        cooldown(instance, mode, KingRules.SUMMON_COOLDOWN);
        SkillUtil.castVfx(p, 0xFFFFF3B0);
        grow(instance, p, 6.0);
    }
}
