package com.newuniverse.nusmp.item;

import com.newuniverse.nusmp.antimagic.Nullification;
import com.newuniverse.nusmp.balance.BalanceLaw;
import com.newuniverse.nusmp.NUGameRules;
import com.newuniverse.nusmp.book.EnergyBridge;
import com.newuniverse.nusmp.book.GrimoireBook;
import com.newuniverse.nusmp.book.SpellRuntime;
import com.newuniverse.nusmp.item.MagicWeaponItem.Kind;
import com.newuniverse.nusmp.vfx.VfxShape;
import com.newuniverse.nusmp.vfx.VfxSpawn;
import io.github.manasmods.tensura.storage.TensuraStorages;
import io.github.manasmods.tensura.util.EnergyHelper;
import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * 1.2.0: the Magebane Shield ("Parry this you filthy casual"): the turquoise heater shield made in the Spellplague days, held in
 * either hand. The logic lives here and is reached from {@link ReferenceWeapons} and {@link MagicWeaponItem}.
 * <ul>
 *   <li><b>A real shield</b> (1.15.0): hold right-click to block like a vanilla shield (the item reports SHIELD_BLOCK, UseAnim.BLOCK and a
 *       72000-tick use, so blocking, durability loss and an axe knocking it down are all vanilla). The two abilities are extras on top.</li>
 *   <li><b>Passive</b> (either hand, or worn in a Curios slot): magic damage taken is cut by a third (by a half with the Magebane Longsword
 *       in a hand).</li>
 *   <li><b>Spellbreak Parry</b> (raising the shield, when it is ready): for 2 s (3 s with the pair) while the shield stays up projectiles
 *       are turned back at their caster, spell bolts are erased and returned, and magic or missile damage is cancelled and thrown back at
 *       whoever dealt it. It recharges on its own timer, so the shield itself is never locked out.</li>
 *   <li><b>Absorb Surge</b> (sneak + right-click, when it is ready and a caster is near; otherwise sneaking just blocks): strips a buff (two with the pair) and a share of magicules from the nearest caster
 *       and heals you with what was taken.</li>
 *   <li><b>Pair bonus</b>: the Magebane Longsword's Dispel Surge also raises a free Spellbreak Parry.</li>
 * </ul>
 * Damage goes through the balance law; the Parry's reflected damage is capped like any other addon hit.
 */
public final class MagebaneShieldArts {
    private MagebaneShieldArts() {}

    static final String K_PARRY = "nusmp_magebane_parry_until", K_PARRY_FREE = "nusmp_magebane_parry_free", K_PARRY_READY = "nusmp_magebane_parry_ready";
    static final int PARRY_TICKS = 40, PARRY_TICKS_PAIR = 60;
    static final int TURQUOISE = 0xFF2FD6C8, PALE = 0xFFB8FFF4;

    static final WeaponArts.Alt ALT = new WeaponArts.Alt("Absorb Surge",
            "Strips a buff from the nearest caster within 14 blocks, drinks some of their magicules and heals you with what was taken. With the Magebane Longsword in the other hand it strips two buffs and heals more.", 700);

    /** Guards against two Magebane Shields reflecting the same blow back and forth. */
    private static boolean reflecting;

    // ---------------------------------------------------------------- who holds what
    static boolean isShield(ItemStack s) { return s.getItem() instanceof MagicWeaponItem w && w.kind == Kind.MAGEBANE_SHIELD; }

    static boolean isLongsword(ItemStack s) { return s.getItem() instanceof MagicWeaponItem w && w.kind == Kind.MAGEBANE_LONGSWORD; }

    /** The shield is in the main hand, the off hand or a Curios slot. */
    static boolean carrying(Player p) { return isShield(p.getMainHandItem()) || isShield(p.getOffhandItem()) || CuriosLink.carries(p, Kind.MAGEBANE_SHIELD); }

    /** The shield is up right now (right-click held). */
    static boolean raisedNow(Player p) { return p.isUsingItem() && isShield(p.getUseItem()); }

    /** Shield in one hand, Magebane Longsword in the other. */
    static boolean pair(Player p) {
        ItemStack a = p.getMainHandItem(), b = p.getOffhandItem();
        return (isShield(a) && isLongsword(b)) || (isLongsword(a) && isShield(b));
    }

    /** The parry window is open and the shield is still up (a parry raised for free by the Magebane Longsword's surge needs no shield up). */
    static boolean parrying(Player p) {
        CompoundTag d = p.getPersistentData();
        return d.getLong(K_PARRY) > p.level().getGameTime() && (d.getBoolean(K_PARRY_FREE) || raisedNow(p));
    }

    /** Is this player inside the Spellbreak Parry window right now? The public face of {@link #parrying}, for other systems (the Charybdis Spawn's scales). */
    public static boolean parryOpen(Player p) { return p != null && parrying(p); }

    // ---------------------------------------------------------------- damage classes
    /** Spell damage: vanilla magic, or any Tensura damage type that is not physical. */
    static boolean isMagic(DamageSource s) {
        if (s.is(DamageTypes.MAGIC) || s.is(DamageTypes.INDIRECT_MAGIC)) return true;
        var key = s.typeHolder().unwrapKey();
        return key.isPresent() && "tensura".equals(key.get().location().getNamespace())
                && !s.is(io.github.manasmods.tensura.data.TensuraTags.DamageTypes.IS_PHYSICAL);
    }

    // ---------------------------------------------------------------- raising the shield: Spellbreak Parry
    /** Called when right-click starts the block: if the parry has gathered again it opens now; otherwise the shield is just a shield. */
    static void raised(ServerPlayer p) {
        long now = p.level().getGameTime();
        CompoundTag d = p.getPersistentData();
        if (now < d.getLong(K_PARRY_READY)) return;
        int cd = (int) Math.round(Kind.MAGEBANE_SHIELD.cooldown * NUGameRules.spellCooldown(p.level()));
        d.putLong(K_PARRY_READY, now + (p.isCreative() ? cd / 4 : cd));
        boolean pair = pair(p);
        raise(p, pair ? PARRY_TICKS_PAIR : PARRY_TICKS, pair, false);
    }

    /** The old one-press parry (kept: ReferenceWeapons still routes the shield's technique here). */
    static boolean technique(ServerPlayer p, ItemStack stack) {
        boolean pair = pair(p);
        raise(p, pair ? PARRY_TICKS_PAIR : PARRY_TICKS, pair, false);
        return true;
    }

    /** The Magebane Longsword's Dispel Surge, with the shield in the other hand, raises a free parry (no new cooldown on the shield). */
    static void pairedSurge(ServerPlayer p) {
        if (pair(p) && !parrying(p)) raise(p, PARRY_TICKS, true, true);
    }

    static void raise(ServerPlayer p, int ticks, boolean pair, boolean free) {
        ServerLevel sl = p.serverLevel();
        p.getPersistentData().putLong(K_PARRY, sl.getGameTime() + ticks);
        p.getPersistentData().putBoolean(K_PARRY_FREE, free);
        Vec3 c = p.position();
        VfxSpawn.sendFollowing(sl, VfxShape.BARRIER_FX2, p, c, TURQUOISE, ticks, pair ? 2.8f : 2.3f);
        Vec3 front = p.getEyePosition().add(p.getViewVector(1f).scale(0.9));
        VfxSpawn.send(sl, VfxShape.ANTI_MAGIC_SLASH, front.add(0, -1.1, 0), front.add(0, 0.7, 0), TURQUOISE, 12, 0.9f);
        sl.playSound(null, p.blockPosition(), SoundEvents.SHIELD_BLOCK, SoundSource.PLAYERS, 1.4f, 0.6f);
        sl.playSound(null, p.blockPosition(), SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.PLAYERS, 1.6f, 1.5f);
        SpellRuntime.zone(sl, ticks, 1, age -> parryTick(p, sl));
    }

    /** Every tick of the parry: turn missiles around, erase spell bolts at the shield and send them back. */
    private static void parryTick(ServerPlayer p, ServerLevel sl) {
        if (!p.isAlive() || p.serverLevel() != sl || !parrying(p)) return;
        Vec3 c = p.position().add(0, 1.0, 0), face = p.getViewVector(1f);
        for (Projectile pr : sl.getEntitiesOfClass(Projectile.class, p.getBoundingBox().inflate(4.5))) {
            Entity owner = pr.getOwner();
            if (owner == p || (owner instanceof LivingEntity o && o.isAlliedTo(p))) continue;
            Vec3 rel = pr.position().subtract(c);
            if (rel.lengthSqr() > 1e-6 && rel.normalize().dot(face) < -0.35) continue;      // behind you: the shield does not cover it
            Vec3 vel = pr.getDeltaMovement();
            Vec3 back = owner != null ? owner.getBoundingBox().getCenter().subtract(pr.position()) : vel.scale(-1);
            if (back.lengthSqr() < 1e-6) back = face;
            pr.setDeltaMovement(back.normalize().scale(Math.max(0.9, vel.length()) * 1.15));
            pr.setOwner(p);
            pr.hurtMarked = true;
            VfxSpawn.send(sl, VfxShape.BARRIER_FX3, pr.position(), pr.position().add(back.normalize()), TURQUOISE, 12, 0.5f);
            sl.playSound(null, pr.blockPosition(), SoundEvents.SHIELD_BLOCK, SoundSource.PLAYERS, 1.2f, 1.3f);
        }
        int erased = SpellRuntime.dissolveBolts(sl, c.add(face.scale(1.6)), 3.4, p);
        if (erased > 0) returnSpell(p, sl, c, face, erased);
    }

    /** A spell bolt was erased at the shield: the blow goes back to the nearest foe in front of you (the caster, most likely). */
    private static void returnSpell(ServerPlayer p, ServerLevel sl, Vec3 c, Vec3 face, int n) {
        LivingEntity best = null;
        double bd = Double.MAX_VALUE;
        for (LivingEntity t : GrimoireBook.around(p, p.position(), 32)) {
            Vec3 d = t.getBoundingBox().getCenter().subtract(c);
            if (d.lengthSqr() < 1e-6 || d.normalize().dot(face) < 0.5) continue;
            if (d.lengthSqr() < bd) { bd = d.lengthSqr(); best = t; }
        }
        VfxSpawn.send(sl, VfxShape.KOTO_SHATTER, c.add(face.scale(1.6)), c.add(face.scale(1.6)), TURQUOISE, 14, 1.0f);
        sl.playSound(null, p.blockPosition(), SoundEvents.SHIELD_BLOCK, SoundSource.PLAYERS, 1.4f, 0.9f);
        if (best == null) return;
        best.hurt(p.damageSources().indirectMagic(p, p), BalanceLaw.damage(p, best, 6f * Math.min(3, n), 0.6));
        VfxSpawn.send(sl, VfxShape.THREAD_LINE, c.add(face.scale(1.4)), best.getBoundingBox().getCenter(), PALE, 8, 1.2f);
    }

    // ---------------------------------------------------------------- the passive and the parry's damage side
    /** Registered on the game bus (NUSMP). The parry throws magic and missile damage back; the shield in hand cuts magic damage. */
    public static void onIncomingDamage(LivingIncomingDamageEvent e) {
        if (!(e.getEntity() instanceof ServerPlayer p) || e.getAmount() <= 0 || !carrying(p)) return;
        DamageSource src = e.getSource();
        boolean magic = isMagic(src), missile = src.is(DamageTypeTags.IS_PROJECTILE);
        if (parrying(p) && (magic || missile)) {
            float blocked = e.getAmount();
            e.setCanceled(true);
            Entity from = src.getEntity();
            ServerLevel sl = p.serverLevel();
            Vec3 at = p.position().add(0, 1.2, 0).add(p.getViewVector(1f).scale(0.9));
            VfxSpawn.send(sl, VfxShape.BARRIER_FX3, at, at.add(p.getViewVector(1f)), TURQUOISE, 12, 0.7f);
            sl.playSound(null, p.blockPosition(), SoundEvents.SHIELD_BLOCK, SoundSource.PLAYERS, 1.5f, 1.1f);
            if (!reflecting && from instanceof LivingEntity att && att != p && !att.isAlliedTo(p) && att.isAlive()) {
                reflecting = true;
                try {
                    att.hurt(p.damageSources().indirectMagic(p, p), BalanceLaw.cap(att, blocked * 0.6f));
                } finally { reflecting = false; }
                VfxSpawn.send(sl, VfxShape.THREAD_LINE, at, att.getBoundingBox().getCenter(), PALE, 8, 1.2f);
            }
            return;
        }
        if (magic) e.setAmount(e.getAmount() * (pair(p) ? 0.5f : 2f / 3f));
    }

    // ---------------------------------------------------------------- sneak + right-click: Absorb Surge
    private static boolean hasBuff(LivingEntity t) {
        for (MobEffectInstance i : t.getActiveEffects()) if (i.getEffect().value().isBeneficial()) return true;
        return false;
    }

    /** A caster: a player, anything with a buff on it, or a being with a pool of magicules. */
    private static boolean caster(LivingEntity t) {
        if (t instanceof Player || hasBuff(t)) return true;
        try { return TensuraStorages.getExistenceFrom(t) != null && EnergyHelper.getMaxMagicule(t) > 100; } catch (Throwable noPool) { return false; }
    }

    /** The nearest caster within 14 blocks, or null. */
    private static LivingEntity nearestCaster(ServerPlayer p) {
        List<LivingEntity> near = new ArrayList<>(GrimoireBook.around(p, p.position(), 14));
        near.sort(Comparator.comparingDouble(t -> t.distanceToSqr(p)));
        for (LivingEntity t : near) if (caster(t)) return t;
        return null;
    }

    /** Sneak + raising the shield: Absorb Surge if it is ready and there is a caster to drink from; false means "just block". */
    static boolean surge(ServerPlayer p) {
        long now = p.level().getGameTime();
        String key = WeaponArts.K_ALT + Kind.MAGEBANE_SHIELD.name();
        if (now < p.getPersistentData().getLong(key) || nearestCaster(p) == null) return false;
        WeaponArts.tryAlt(p, Kind.MAGEBANE_SHIELD);
        return p.getPersistentData().getLong(key) > now;
    }

    static boolean cast(ServerPlayer p) {
        ServerLevel sl = p.serverLevel();
        LivingEntity src = nearestCaster(p);
        if (src == null) { GrimoireBook.fail(p, "No caster within 14 blocks to drink from."); return false; }
        boolean pair = pair(p);
        int strips = 0;
        for (int i = 0; i < (pair ? 2 : 1); i++) {
            if (!hasBuff(src)) break;
            MagicWeaponItem.stripOne(src);
            strips++;
        }
        double drained = 0;
        try { drained = EnergyBridge.drain(src, p, pair ? 0.08 : 0.05); } catch (Throwable noPool) { /* no magicules to drink */ }
        if (strips == 0 && drained <= 0) { GrimoireBook.fail(p, "There is nothing to absorb from that caster."); return false; }
        Nullification.interfere(src, 3, pair ? 0.7f : 0.5f, 0.2f);
        src.hurt(p.damageSources().indirectMagic(p, p), BalanceLaw.damage(p, src, 3f, 0.6));
        BalanceLaw.heal(p, 3f + 2.5f * strips + (drained > 0 ? 2f : 0f) + (pair ? 2f : 0f));
        p.addEffect(new MobEffectInstance(MobEffects.ABSORPTION, 200, pair ? 1 : 0));
        Vec3 from = src.getBoundingBox().getCenter(), to = p.getBoundingBox().getCenter();
        VfxSpawn.send(sl, VfxShape.KOTO_SHATTER, from, to, TURQUOISE, 22, 1.5f);
        VfxSpawn.send(sl, VfxShape.THREAD_LINE, from, to, PALE, 14, 1.3f);
        VfxSpawn.send(sl, VfxShape.BARRIER_FX3, p.position().add(0, 1, 0), p.position().add(0, 2, 0), TURQUOISE, 14, 0.9f);
        sl.playSound(null, p.blockPosition(), SoundEvents.ENCHANTMENT_TABLE_USE, SoundSource.PLAYERS, 1.6f, 1.6f);
        sl.playSound(null, p.blockPosition(), SoundEvents.BEACON_POWER_SELECT, SoundSource.PLAYERS, 1.2f, 1.8f);
        return true;
    }

    // ---------------------------------------------------------------- on hit and the full swing
    /** The shield's bash: a short shove and a jam of the target's skills. */
    static void onHit(ServerPlayer p, LivingEntity t) {
        t.knockback(0.5, p.getX() - t.getX(), p.getZ() - t.getZ());
        Nullification.interfere(t, 2, 0.3f, 0.1f);
        if (pair(p)) MagicWeaponItem.stripOne(t);
    }

    /** Aegis Slam: a full swing drives the shield home, hurling the target away with a ring of force round it. */
    static void fullSwing(ServerPlayer p, LivingEntity t) {
        ServerLevel sl = p.serverLevel();
        Vec3 mid = t.getBoundingBox().getCenter();
        t.hurt(p.damageSources().playerAttack(p), BalanceLaw.damage(p, t, 4, 0.6));
        t.knockback(1.1, p.getX() - t.getX(), p.getZ() - t.getZ());
        t.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 30, 1));
        for (LivingEntity o : GrimoireBook.around(p, t.position(), 2.5)) {
            if (o == t) continue;
            o.hurt(p.damageSources().playerAttack(p), BalanceLaw.damage(p, o, 2, 0.6));
            o.knockback(0.6, p.getX() - o.getX(), p.getZ() - o.getZ());
        }
        VfxSpawn.send(sl, VfxShape.BARRIER_FX3, mid, mid.add(p.getViewVector(1f)), TURQUOISE, 12, 0.8f);
        sl.playSound(null, t.blockPosition(), SoundEvents.SHIELD_BLOCK, SoundSource.PLAYERS, 1.6f, 0.7f);
    }

    // ---------------------------------------------------------------- tooltip
    static void tooltip(List<Component> tip) {
        tip.add(Component.literal("Held in a hand or worn in a Curios slot: magic damage taken is cut by a third").withStyle(ChatFormatting.DARK_AQUA));
        tip.add(Component.literal("  Parry lasts 2 s while the shield is up: it turns projectiles and spells back at their caster").withStyle(ChatFormatting.AQUA));
        tip.add(Component.literal("  With the Magebane Longsword in the other hand: the cut becomes a half, the parry lasts 3 s, Dispel Surge raises a parry too, Absorb Surge strips two buffs").withStyle(ChatFormatting.AQUA));
        tip.add(Component.literal("\"Parry this, you filthy casual.\"").withStyle(ChatFormatting.DARK_GRAY, ChatFormatting.ITALIC));
    }
}
