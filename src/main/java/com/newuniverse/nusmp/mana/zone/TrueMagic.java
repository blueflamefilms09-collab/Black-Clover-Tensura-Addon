package com.newuniverse.nusmp.mana.zone;

import com.newuniverse.nusmp.balance.BalanceLaw;
import com.newuniverse.nusmp.blackclover.GrimoirePages;
import com.newuniverse.nusmp.blackclover.MagicType;
import com.newuniverse.nusmp.book.BookPage;
import com.newuniverse.nusmp.book.GrimoireBook;
import com.newuniverse.nusmp.book.SpellRuntime;
import com.newuniverse.nusmp.entity.DaemonMaster;
import com.newuniverse.nusmp.heroweapon.weapon.kit.Foes;
import com.newuniverse.nusmp.mana.ManaLaw;
import com.newuniverse.nusmp.multiverse.MultiverseProfile;
import com.newuniverse.nusmp.multiverse.Race;
import com.newuniverse.nusmp.quests.NusmpEvents;
import com.newuniverse.nusmp.vfx.VfxShape;
import io.github.manasmods.manascore.skill.api.ManasSkillInstance;
import io.github.manasmods.tensura.util.EnergyHelper;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LightningBolt;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;

/**
 * The True Magic page (Shin Maho): Mana Method taken to its end. An INTERPRETATION of the canon (docs/mana/mana_zone_redesign.md): the caster draws a rune array
 * in the air above the point they aim at; after a moment the array fires the REAL element (a column of true fire, a deluge, a downburst, a fall of stone, a bolt
 * from the sky, a surge of thorns) instead of the imitation; most of the spell's cost is paid by the environment's dense natural mana (a share is refunded, more in
 * thick mana); it is far stronger than any ordinary page; devils are highly susceptible. It needs the same late-game gate as the Mana Zone, a NATURAL attribute
 * (fire, water, wind, earth, lightning, plant) and Stage 3 control or better. It is a page of its own: a Mana Zone never makes true elements.
 *
 * <p>No world edits: the lightning is visual only, nothing burns, nothing is placed; the damage goes through the balance law (EP scaling, the per-hit caps on players
 * and the boss caps); the strike is one query round one point.
 */
public final class TrueMagic {
    private TrueMagic() {}

    public static final String ID = "true_magic_rune_array";

    public static BookPage page() { return new BookPage(ID, "True Magic", "True Magic", TrueMagicRules.COST_PERCENT, 800, TrueMagicRules.COOLDOWN_TICKS, TrueMagic::cast); }

    public static boolean cast(GrimoireBook b, ManasSkillInstance i, ServerPlayer p, int mode) {
        MagicType magic = GrimoirePages.magicOf(i);
        if (!TrueMagicRules.isNatural(magic.name())) {
            GrimoireBook.fail(p, "True Magic follows the shapes of nature: fire, water, wind, earth, lightning and plants. Your magic is too abstract to be drawn as a rune array.");
            return false;
        }
        var stage = ManaLaw.stage(p);
        if (!TrueMagicRules.controlEnough(stage.number(), stage.arcane() || stage.saint())) {
            GrimoireBook.fail(p, "True Magic needs the control of Stage 3 or higher to hold the runes.");
            return false;
        }
        ServerLevel sl = p.serverLevel();
        Vec3 at = GrimoireBook.aim(p, TrueMagicRules.REACH);
        double density = ManaLaw.density(sl, net.minecraft.core.BlockPos.containing(at));
        // the rune array draws itself in the air above the point, facing down at it
        b.vfx(p, VfxShape.MAGIC_CIRCLE, at.add(0, 8, 0), at, TrueMagicRules.DELAY_TICKS + 12, 3.2f);
        sl.playSound(null, p.blockPosition(), SoundEvents.ENCHANTMENT_TABLE_USE, SoundSource.PLAYERS, 1.4f, 0.7f);
        // the environment pays most of the cost: it comes back as the caster's mana
        try {
            double cost = b.getMagiculeCost(p, i, mode);
            EnergyHelper.gainMagicule(p, cost * TrueMagicRules.fuelShare(density), EnergyHelper.GainType.NORMAL);
        } catch (Throwable ignored) { /* no Tensura data: nothing to refund */ }
        SpellRuntime.later(sl, TrueMagicRules.DELAY_TICKS, () -> { if (p.isAlive()) strike(b, i, p, mode, magic, at, density); });
        NusmpEvents.fire(p, "true_magic.cast", magic.name().toLowerCase(java.util.Locale.ROOT));
        return true;
    }

    private static void strike(GrimoireBook b, ManasSkillInstance i, ServerPlayer p, int mode, MagicType magic, Vec3 at, double density) {
        ServerLevel sl = p.serverLevel();
        List<LivingEntity> hit = new ArrayList<>();
        for (LivingEntity t : GrimoireBook.around(p, at, TrueMagicRules.RADIUS)) {
            if (hit.size() >= TrueMagicRules.MAX_TARGETS) break;
            if (Foes.isFoe(p, t)) hit.add(t);
        }
        float r = (float) TrueMagicRules.RADIUS;
        switch (magic) {
            case FLAME -> b.vfx(p, VfxShape.FIRE_PILLAR, at, at, 40, r);
            case WATER -> b.vfx(p, VfxShape.WATER_NEST, at, at, 40, r);
            case WIND -> b.vfx(p, VfxShape.WIND_TORNADO, at, at, 40, r);
            case EARTH -> b.vfx(p, VfxShape.EARTH_FISSURE, at.add(-r, 0, 0), at.add(r, 0, 0), 30, r);
            case LIGHTNING -> b.vfx(p, VfxShape.LIGHTNING_SPEAR, at.add(0, 40, 0), at, 14, 2.2f);
            default -> b.vfx(p, VfxShape.PLANT_FX2, at, at, 40, r);
        }
        if (magic == MagicType.LIGHTNING) sky(sl, at);
        sl.playSound(null, net.minecraft.core.BlockPos.containing(at), magic == MagicType.LIGHTNING ? SoundEvents.LIGHTNING_BOLT_THUNDER : SoundEvents.GENERIC_EXPLODE.value(), SoundSource.PLAYERS, 1.6f, 1.0f);
        for (LivingEntity t : hit) {
            boolean boss = BalanceLaw.isBoss(t);
            b.hurt(i, p, t, mode, (float) TrueMagicRules.damage(density, isDevil(t), boss));
            element(magic, t, boss);
        }
    }

    /** What the real element does to something it strikes, short and capped (control goes through the balance law's caps). */
    private static void element(MagicType magic, LivingEntity t, boolean boss) {
        switch (magic) {
            case FLAME -> t.setRemainingFireTicks(Math.max(t.getRemainingFireTicks(), boss ? 40 : 100));
            case WATER -> t.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, BalanceLaw.controlTicks(t, 60), 1, false, true));
            case WIND -> { t.setDeltaMovement(t.getDeltaMovement().add(0, boss ? 0.3 : 0.9, 0)); t.hurtMarked = true; }
            case EARTH -> t.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, BalanceLaw.controlTicks(t, 80), 2, false, true));
            case LIGHTNING -> t.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, BalanceLaw.controlTicks(t, 60), 0, false, true));
            default -> t.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, BalanceLaw.controlTicks(t, 100), 3, false, true));
        }
    }

    /** The bolt from the sky: light and thunder only (no fire, no transformed mobs, nothing persists). */
    private static void sky(ServerLevel sl, Vec3 at) {
        LightningBolt bolt = EntityType.LIGHTNING_BOLT.create(sl);
        if (bolt == null) return;
        bolt.moveTo(at);
        bolt.setVisualOnly(true);
        sl.addFreshEntity(bolt);
    }

    /** Is this a devil (the Devil Arc's kind, Zagred's, a player who is one)? */
    public static boolean isDevil(LivingEntity t) {
        if (t instanceof DaemonMaster || GrimoireBook.inDevilState(t)) return true;
        if (t instanceof ServerPlayer sp) return MultiverseProfile.race(sp) == Race.DEVIL;
        return TrueMagicRules.devilPath(BuiltInRegistries.ENTITY_TYPE.getKey(t.getType()).getPath());
    }
}
