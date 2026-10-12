package com.newuniverse.nusmp.item;

import com.newuniverse.nusmp.antimagic.Nullification;
import com.newuniverse.nusmp.balance.BalanceLaw;
import com.newuniverse.nusmp.book.GrimoireBook;
import com.newuniverse.nusmp.book.SpellRuntime;
import com.newuniverse.nusmp.vfx.VfxShape;
import com.newuniverse.nusmp.vfx.VfxSpawn;
import io.github.manasmods.tensura.damage.TensuraDamageTypes;
import net.minecraft.ChatFormatting;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.EntityTypeTags;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.OwnableEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * 1.2.0: the Weapon of Resurgence (the paladin's longsword wound with a holy chain and a cross-guard). Reached from
 * {@link ReferenceWeapons} and {@link MagicWeaponItem}.
 * <ul>
 *   <li><b>Lay on Hands</b> (the passive): the blade keeps a pool of 40 points. Every hit mends you and the most wounded ally within
 *       8 blocks, one point each; every hit also returns a point (three on undead and demons) and the pool refills by itself.</li>
 *   <li><b>Resurgent Smite</b> (right-click): a radiant burst 7 blocks wide. It spends up to 15 points of the pool to heal you and your
 *       allies and cleanse one affliction each; undead and demons are burned, everyone else is dazzled.</li>
 *   <li><b>Chains of Sanctity</b> (sneak + right-click): holy chains bind the foe you aim at for 3 s. The shared hard-control lock and
 *       the boss / player caps of the balance law apply; the wicked are also silenced and scorched while bound.</li>
 * </ul>
 * Damage goes through the balance law (Tensura's holy damage type against undead and demons).
 */
final class ResurgenceArts {
    private ResurgenceArts() {}

    static final int POOL_MAX = 40, REFILL_TICKS = 24, BURST_SPEND = 15;
    static final int GOLD = 0xFFFFD86B, HOLY = 0xFFFFF2B0, CHAIN_GOLD = 0xFFFFC870;
    private static final String K_POOL = "LayPool", K_AT = "LayAt";

    static final WeaponArts.Alt ALT = new WeaponArts.Alt("Chains of Sanctity",
            "Holy chains bind the foe you aim at (or the nearest within 12 blocks) for 3 s: it cannot move, and the wicked are silenced and scorched while they hold. Bosses are held only for a moment.", 800);

    // ---------------------------------------------------------------- the Lay on Hands pool (kept in the stack, refilled lazily)
    static int pool(ItemStack s, long now) {
        CustomData d = s.get(DataComponents.CUSTOM_DATA);
        if (d == null) return POOL_MAX;
        CompoundTag t = d.copyTag();
        if (!t.contains(K_POOL)) return POOL_MAX;
        int p = t.getInt(K_POOL);
        long at = t.getLong(K_AT);
        if (now > at) p += (int) ((now - at) / REFILL_TICKS);
        return Mth.clamp(p, 0, POOL_MAX);
    }

    static void setPool(ItemStack s, long now, int v) {
        int val = Mth.clamp(v, 0, POOL_MAX);
        CustomData.update(DataComponents.CUSTOM_DATA, s, t -> { t.putInt(K_POOL, val); t.putLong(K_AT, now); });
    }

    // ---------------------------------------------------------------- who is who
    /** Undead, demons and devil-tainted mages feel the weapon as holy fire. */
    static boolean evil(LivingEntity t) {
        if (t.getType().is(EntityTypeTags.UNDEAD) || GrimoireBook.inDevilState(t)) return true;
        var id = BuiltInRegistries.ENTITY_TYPE.getKey(t.getType());
        return id != null && (id.getPath().contains("demon") || id.getPath().contains("devil"));
    }

    private static boolean friend(ServerPlayer p, LivingEntity x) {
        if (x == p) return true;
        if (x.isAlliedTo(p)) return true;
        return x instanceof OwnableEntity o && o.getOwner() == p;
    }

    /** A holy blow: Tensura's holy damage on the wicked (x1.6, and they burn), light damage on the rest. */
    private static void smite(ServerPlayer p, LivingEntity t, float raw) {
        if (friend(p, t)) return;
        boolean evil = evil(t);
        DamageSource src;
        try {
            src = TensuraDamageTypes.getEntityDamageSource(p.serverLevel(), evil ? TensuraDamageTypes.HOLY_DAMAGE : TensuraDamageTypes.LIGHT_ELEMENTAL, p);
        } catch (RuntimeException noType) {
            src = p.damageSources().playerAttack(p);
        }
        t.hurt(src, BalanceLaw.damage(p, t, evil ? raw * 1.6f : raw, 0.6));
        if (evil) t.igniteForSeconds(4);
    }

    private static void cleanse(LivingEntity a) {
        for (MobEffectInstance e : new ArrayList<>(a.getActiveEffects())) {
            if (!e.getEffect().value().isBeneficial()) { a.removeEffect(e.getEffect()); return; }
        }
    }

    // ---------------------------------------------------------------- the passive: Lay on Hands
    /** The most wounded friend (not the wielder) within r blocks, or null. */
    private static LivingEntity woundedFriend(ServerPlayer p, double r) {
        LivingEntity best = null;
        float worst = 0.999f;
        for (LivingEntity x : p.serverLevel().getEntitiesOfClass(LivingEntity.class, p.getBoundingBox().inflate(r), e -> e != p && e.isAlive() && !e.isSpectator() && friend(p, e))) {
            float frac = x.getHealth() / Math.max(1f, x.getMaxHealth());
            if (frac < worst) { worst = frac; best = x; }
        }
        return best;
    }

    private static float mendAmount(LivingEntity x) { return Mth.clamp(x.getMaxHealth() * 0.04f, 1.5f, 10f); }

    static void onHit(ServerPlayer p, ItemStack stack, LivingEntity t) {
        ServerLevel sl = p.serverLevel();
        long now = sl.getGameTime();
        int pool = pool(stack, now);
        boolean evil = evil(t);
        if (pool >= 1 && p.getHealth() < p.getMaxHealth()) {
            p.heal(mendAmount(p));
            pool--;
            VfxSpawn.send(sl, VfxShape.WATER_RING, p.position().add(0, 0.1, 0), p.position().add(0, 1, 0), GOLD, 10, 0.55f);
        }
        LivingEntity ally = pool >= 1 ? woundedFriend(p, 8) : null;
        if (ally != null) {
            ally.heal(mendAmount(ally));
            pool--;
            VfxSpawn.send(sl, VfxShape.SEAL_WOUND, p.getEyePosition().add(0, -0.5, 0), ally.getBoundingBox().getCenter(), GOLD, 20, 1f);
        }
        setPool(stack, now, pool + (evil ? 3 : 1));
        if (evil) {
            smite(p, t, 2f);
            VfxSpawn.send(sl, VfxShape.LIGHT_FLARE, t.getBoundingBox().getCenter(), t.getBoundingBox().getCenter(), GOLD, 8, 0.6f);
        }
    }

    // ---------------------------------------------------------------- right-click: Resurgent Smite
    static boolean technique(ServerPlayer p, ItemStack stack) {
        ServerLevel sl = p.serverLevel();
        long now = sl.getGameTime();
        int pool = pool(stack, now), spent = Math.min(pool, BURST_SPEND);
        setPool(stack, now, pool - spent);
        float heal = 4f + spent * 0.5f;                                           // 4 at an empty pool, 11.5 with 15 points behind it
        double r = 7;
        Vec3 c = p.position();
        for (LivingEntity a : sl.getEntitiesOfClass(LivingEntity.class, p.getBoundingBox().inflate(r), x -> x.isAlive() && !x.isSpectator() && friend(p, x) && x.distanceToSqr(p) <= r * r)) {
            BalanceLaw.heal(a, heal);
            cleanse(a);
            VfxSpawn.send(sl, VfxShape.LIGHT_FLARE, a.position().add(0, 1.3, 0), a.position().add(0, 1.3, 0), HOLY, 12, 0.7f);
        }
        for (LivingEntity t : GrimoireBook.around(p, c, r)) {
            boolean evil = evil(t);
            smite(p, t, evil ? 9f : 3f);
            if (evil) {
                Vec3 away = t.position().subtract(c).multiply(1, 0, 1);
                if (away.lengthSqr() > 1e-4 && !BalanceLaw.isBoss(t)) t.knockback(0.9, -away.x, -away.z);
                t.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 100, 0));
            } else {
                t.addEffect(new MobEffectInstance(MobEffects.GLOWING, 100, 0));
            }
        }
        VfxSpawn.send(sl, VfxShape.MAGIC_CIRCLE, c.add(0, 0.12, 0), c.add(0, 1.12, 0), GOLD, 34, 4.3f);
        VfxSpawn.send(sl, VfxShape.LIGHT_FLARE, c.add(0, 1.4, 0), c.add(0, 1.4, 0), HOLY, 20, 3.2f);
        VfxSpawn.send(sl, VfxShape.WIND_RING, c.add(0, 0.2, 0), c.add(0, 1, 0), GOLD, 22, 3.0f);
        sl.playSound(null, p.blockPosition(), SoundEvents.BELL_BLOCK, SoundSource.PLAYERS, 2.2f, 0.8f);
        sl.playSound(null, p.blockPosition(), SoundEvents.BEACON_ACTIVATE, SoundSource.PLAYERS, 1.6f, 1.5f);
        sl.playSound(null, p.blockPosition(), SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.PLAYERS, 2f, 1.2f);
        return true;
    }

    // ---------------------------------------------------------------- sneak + right-click: Chains of Sanctity
    static boolean cast(ServerPlayer p) {
        ServerLevel sl = p.serverLevel();
        LivingEntity t = GrimoireBook.target(p, 18);
        if (t == null || friend(p, t)) {
            List<LivingEntity> near = new ArrayList<>(GrimoireBook.around(p, p.position(), 12));
            near.sort(Comparator.comparingDouble(x -> x.distanceToSqr(p)));
            t = near.isEmpty() ? null : near.get(0);
        }
        if (t == null) { GrimoireBook.fail(p, "No foe for the chains to find."); return false; }
        if (!GrimoireBook.control(p)) return false;                               // the shared 12 s hard-control lock
        final LivingEntity foe = t;
        int ticks = BalanceLaw.controlTicks(foe, 60);
        boolean evil = evil(foe), boss = BalanceLaw.isBoss(foe);
        foe.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, ticks, 6));
        foe.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, ticks, 1));
        smite(p, foe, 6f);
        if (!boss) Nullification.interfere(foe, Math.max(1, ticks / 20), evil ? 0.8f : 0.4f, evil ? 0.4f : 0.15f);
        Vec3 mid = foe.getBoundingBox().getCenter();
        VfxSpawn.send(sl, VfxShape.SEAL_CHAINS, foe.position(), foe.position(), CHAIN_GOLD, ticks + 10, 1.5f);
        VfxSpawn.send(sl, VfxShape.CHAIN_FX1, p.getEyePosition().add(0, -0.4, 0), mid, CHAIN_GOLD, 14, 1.0f);
        VfxSpawn.send(sl, VfxShape.LIGHT_FLARE, mid, mid, HOLY, 14, 1.2f);
        sl.playSound(null, foe.blockPosition(), SoundEvents.CHAIN_PLACE, SoundSource.PLAYERS, 2f, 0.7f);
        sl.playSound(null, foe.blockPosition(), SoundEvents.BELL_BLOCK, SoundSource.PLAYERS, 1.4f, 1.3f);
        SpellRuntime.zone(sl, ticks, 2, age -> {
            if (!foe.isAlive() || !p.isAlive()) return;
            Vec3 m = foe.getDeltaMovement();
            foe.setDeltaMovement(0, Math.min(m.y, 0), 0);                         // held fast by the chains
            foe.hurtMarked = true;
            if (evil && age % 10 == 0) smite(p, foe, 1.5f);                       // the wicked burn in them
        });
        SpellRuntime.later(sl, ticks, () -> {
            if (!foe.isAlive()) return;
            Vec3 e = foe.getBoundingBox().getCenter();
            VfxSpawn.send(sl, VfxShape.CHAIN_FX3, e, e, CHAIN_GOLD, 24, 0.9f);
            sl.playSound(null, foe.blockPosition(), SoundEvents.CHAIN_BREAK, SoundSource.PLAYERS, 1.6f, 0.9f);
        });
        return true;
    }

    // ---------------------------------------------------------------- the full swing
    /** Sanctified Cleave: a full swing lands a holy blow and mends the wielder. */
    static void fullSwing(ServerPlayer p, LivingEntity t) {
        ServerLevel sl = p.serverLevel();
        Vec3 mid = t.getBoundingBox().getCenter(), look = p.getViewVector(1f);
        smite(p, t, 5f);
        if (evil(t)) t.knockback(0.8, p.getX() - t.getX(), p.getZ() - t.getZ());
        BalanceLaw.heal(p, 2f);
        VfxSpawn.send(sl, VfxShape.LIGHT_BLADE, mid.subtract(look.scale(1.4)).add(0, 0.8, 0), mid.add(look.scale(1.4)).add(0, -0.4, 0), GOLD, 10, 1.1f);
        VfxSpawn.send(sl, VfxShape.LIGHT_FLARE, mid, mid, HOLY, 10, 1.0f);
        sl.playSound(null, t.blockPosition(), SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.PLAYERS, 1.4f, 1.4f);
    }

    // ---------------------------------------------------------------- tooltip
    static void tooltip(ItemStack stack, Item.TooltipContext ctx, List<Component> tip) {
        tip.add(Component.literal("Lay on Hands pool: " + pool(stack, 0) + "/" + POOL_MAX).withStyle(ChatFormatting.GOLD));
        tip.add(Component.literal("  Every hit mends you and the most wounded ally within 8 blocks (1 point each) and returns a point, 3 on undead and demons. The pool refills by itself.").withStyle(ChatFormatting.YELLOW));
        tip.add(Component.literal("  Resurgent Smite spends up to 15 points: the more behind it, the more it mends.").withStyle(ChatFormatting.YELLOW));
        tip.add(Component.literal("A +1 weapon that forms a bond with a paladin.").withStyle(ChatFormatting.DARK_GRAY, ChatFormatting.ITALIC));
    }
}
