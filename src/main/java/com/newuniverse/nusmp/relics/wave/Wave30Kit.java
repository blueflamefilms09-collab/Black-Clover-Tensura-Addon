package com.newuniverse.nusmp.relics.wave;

import com.newuniverse.nusmp.balance.BalanceLaw;
import com.newuniverse.nusmp.boss.rules.BossIdentity;
import com.newuniverse.nusmp.item.CuriosLink;
import com.newuniverse.nusmp.relics.Fx;
import com.newuniverse.nusmp.relics.RelicDef;
import com.newuniverse.nusmp.relics.Relics;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * Why: Vecna, the Deck, the Stones, the Paradox and the Key all need the same fairness guards; one copy keeps them identical.
 * <p>
 * Wave 30: the glue the classes of the special wave share: who counts as a boss, the target in front of the bearer, a safe hit (a player is never taken
 * below a heart), cleansing, giving items, transient attribute modifiers, what the bearer carries, and the timed hostile summons of the Deck and of
 * Vecna's Hunger. Statics only; everything runs on the server.
 */
final class Wave30Kit {
    private Wave30Kit() {}

    /** The persistent-data key {@code RelicEngine} reads as "a relic's ally of this owner": the owner is never its target and AoEs skip it. */
    static final String ALLY_OWNER = "nusmp_relic_owner";
    /** The entity tags of a timed hostile summon of this wave: its owner and the game time it goes. */
    static final String FOE_OWNER = "nusmp_w30_foe", FOE_UNTIL = "nusmp_w30_foe_until";

    // ---------------------------------------------------------------- who
    static boolean isBoss(LivingEntity e) {
        try {
            return e != null && (BalanceLaw.isBoss(e) || BossIdentity.isBoss(e));
        } catch (RuntimeException | LinkageError unreadable) {
            return false;
        }
    }

    /** A living thing the bearer may target with a mind-bending act: a mob that is not a boss, a player or an ally of a relic. */
    static boolean controllable(ServerPlayer p, LivingEntity e) {
        return e instanceof Mob m && m.isAlive() && !isBoss(m) && !m.getPersistentData().hasUUID(ALLY_OWNER) && !m.getPersistentData().hasUUID(FOE_OWNER) && !m.isAlliedTo(p);
    }

    /** A player the bearer may harm or affect against their will: not creative, not spectator, and one the bearer may fight (PvP rules). */
    static boolean harmable(ServerPlayer p, Player o) {
        return o != p && o.isAlive() && !o.isCreative() && !o.isSpectator() && p.canHarmPlayer(o);
    }

    // ---------------------------------------------------------------- aiming
    /** The living thing the bearer looks at within {@code reach} (blocks stop the look), or null. Creative and spectator players are never found. */
    static LivingEntity target(ServerPlayer p, double reach) {
        Vec3 eye = p.getEyePosition(), look = p.getLookAngle();
        double dist = reach;
        HitResult blocks = p.pick(reach, 1.0f, false);
        if (blocks.getType() == HitResult.Type.BLOCK) dist = Math.sqrt(blocks.getLocation().distanceToSqr(eye));
        Vec3 end = eye.add(look.scale(dist));
        AABB box = p.getBoundingBox().expandTowards(look.scale(dist)).inflate(1.0);
        EntityHitResult r = ProjectileUtil.getEntityHitResult(p, eye, end, box,
                e -> e instanceof LivingEntity le && le.isAlive() && !e.isSpectator() && e != p && !(e instanceof Player o && o.isCreative()), dist * dist);
        return r != null && r.getEntity() instanceof LivingEntity le ? le : null;
    }

    /** Living things in a cone in front of the bearer (nearest first, at most 24): {@code cos} is the cosine of the half angle. Never creative or spectator players. */
    static List<LivingEntity> cone(ServerPlayer p, double range, double cos) {
        List<LivingEntity> out = new ArrayList<>();
        Vec3 eye = p.getEyePosition(), look = p.getLookAngle();
        for (LivingEntity e : Fx.foesAround(p, range)) {
            Vec3 to = e.getEyePosition().subtract(eye);
            double len = to.length();
            if (len < 1.0e-3 || to.scale(1.0 / len).dot(look) >= cos) out.add(e);
        }
        return out;
    }

    // ---------------------------------------------------------------- harm
    /**
     * A hit on a living thing with the Fx rules for mobs; a player is taken through {@link Wave30Rules#playerHit}: at most 30% (45% when {@code heavy}) of
     * their maximum health and never so much that less than a heart is left. Creative and spectator players and those the bearer may not fight are skipped.
     */
    static void hit(ServerPlayer p, LivingEntity t, float amount, String damageType, boolean heavy) {
        if (t == null || !t.isAlive() || amount <= 0f || t == p) return;
        if (t instanceof Player o) {
            if (!harmable(p, o)) return;
            float a = Wave30Rules.playerHit(amount, o.getMaxHealth(), o.getHealth(), heavy ? 0.45 : 0.30, Wave30Tune.LEAVE_HEALTH);
            if (a > 0f) o.hurt(p.damageSources().indirectMagic(p, p), a);
        } else if (heavy) {
            Fx.damageHeavy(p, t, amount, damageType);
        } else {
            Fx.damage(p, t, amount, damageType);
        }
    }

    /** Takes a plain amount off a mob's health through a hurt (a share of its health: the jelly and the Snap), as a player's blow. */
    static void hurtShare(ServerPlayer p, LivingEntity t, float amount) {
        if (t == null || !t.isAlive() || amount <= 0f) return;
        t.hurt(p.damageSources().indirectMagic(p, p), amount);
    }

    // ---------------------------------------------------------------- effects
    /** Removes every harmful potion effect (poison, wither, slowness, weakness, the Tensura ones that are harmful too). */
    static int cleanse(LivingEntity e) {
        List<Holder<MobEffect>> bad = new ArrayList<>();
        for (MobEffectInstance i : e.getActiveEffects()) if (i.getEffect().value().getCategory() == MobEffectCategory.HARMFUL) bad.add(i.getEffect());
        for (Holder<MobEffect> h : bad) e.removeEffect(h);
        return bad.size();
    }

    static Optional<Holder<MobEffect>> effect(String id) {
        ResourceLocation rl = ResourceLocation.tryParse(id);
        return rl == null ? Optional.empty() : BuiltInRegistries.MOB_EFFECT.getHolder(rl).map(h -> (Holder<MobEffect>) h);
    }

    static void lift(LivingEntity e, String id) { effect(id).ifPresent(e::removeEffect); }

    static boolean has(LivingEntity e, String id) { return effect(id).map(e::hasEffect).orElse(false); }

    static void give(LivingEntity e, String id, int ticks, int amp) { Fx.addEffect(e, id, ticks, amp); }

    // ---------------------------------------------------------------- attributes
    static Optional<Holder<Attribute>> attribute(String id) {
        ResourceLocation rl = ResourceLocation.tryParse(id);
        return rl == null ? Optional.empty() : BuiltInRegistries.ATTRIBUTE.getHolder(rl).map(h -> (Holder<Attribute>) h);
    }

    /** Switches a transient modifier of this wave on or off (idempotent). It never reaches the save file; a tick re-applies it. */
    static void setMod(LivingEntity e, String attributeId, String name, double amount, AttributeModifier.Operation op, boolean on) {
        ResourceLocation id = ResourceLocation.fromNamespaceAndPath("nusmp", "w30_" + name);
        attribute(attributeId).ifPresent(h -> {
            AttributeInstance inst = e.getAttribute(h);
            if (inst == null) return;
            AttributeModifier cur = inst.getModifier(id);
            if (on) {
                if (cur != null && Math.abs(cur.amount() - amount) < 1.0e-9) return;
                if (cur != null) inst.removeModifier(id);
                inst.addTransientModifier(new AttributeModifier(id, amount, op));
            } else if (cur != null) {
                inst.removeModifier(id);
            }
        });
    }

    /** Sets a permanent modifier (it is saved with the player; the wave re-applies it from its own counter after a death, which does not copy modifiers). */
    static void setPermanent(LivingEntity e, String attributeId, String name, double amount, AttributeModifier.Operation op) {
        ResourceLocation id = ResourceLocation.fromNamespaceAndPath("nusmp", "w30_" + name);
        attribute(attributeId).ifPresent(h -> {
            AttributeInstance inst = e.getAttribute(h);
            if (inst == null) return;
            AttributeModifier cur = inst.getModifier(id);
            if (cur != null && Math.abs(cur.amount() - amount) < 1.0e-9) return;
            if (cur != null) inst.removeModifier(id);
            if (amount != 0.0) inst.addPermanentModifier(new AttributeModifier(id, amount, op));
        });
    }

    // ---------------------------------------------------------------- items
    /** Hands the stack to the player, dropping what does not fit at their feet. */
    static void give(ServerPlayer p, ItemStack s) {
        if (s == null || s.isEmpty()) return;
        if (!p.getInventory().add(s) && !s.isEmpty()) p.drop(s, false);
    }

    /** Counts a plain (non-relic) item in the player's inventory. */
    static int count(ServerPlayer p, net.minecraft.world.item.Item item) {
        int n = 0;
        Inventory inv = p.getInventory();
        for (int i = 0; i < inv.getContainerSize(); i++) {
            ItemStack s = inv.getItem(i);
            if (s.is(item) && Relics.defOf(s) == null) n += s.getCount();
        }
        return n;
    }

    /** Removes up to {@code max} of a plain item from the inventory; returns how many went. Never a relic. */
    static int take(ServerPlayer p, net.minecraft.world.item.Item item, int max) {
        int left = max;
        Inventory inv = p.getInventory();
        for (int i = 0; i < inv.getContainerSize() && left > 0; i++) {
            ItemStack s = inv.getItem(i);
            if (!s.is(item) || Relics.defOf(s) != null) continue;
            int n = Math.min(left, s.getCount());
            s.shrink(n);
            left -= n;
        }
        return max - left;
    }

    /** The ids of every relic the player has on them: the whole inventory (hands and armour included) and the Curios slots. */
    static Set<String> carriedIds(ServerPlayer p) {
        Set<String> out = new HashSet<>();
        Inventory inv = p.getInventory();
        for (int i = 0; i < inv.getContainerSize(); i++) {
            RelicDef d = Relics.defOf(inv.getItem(i));
            if (d != null) out.add(d.id());
        }
        for (ItemStack s : CuriosLink.equipped(p)) {
            RelicDef d = Relics.defOf(s);
            if (d != null) out.add(d.id());
        }
        return out;
    }

    // ---------------------------------------------------------------- timed hostile summons
    /**
     * A hostile summon of the Deck or of Vecna's Hunger: the entity type by id (skipped silently if absent), set on {@code p} alone, with the given health
     * and attack, named, gone after {@code life} ticks, dropping nothing. Its blows never take a player below a heart (see the damage handler).
     * Returns it, or null.
     */
    static Mob foe(ServerPlayer p, String typeId, int life, String name, double health, double attack) {
        ResourceLocation rl = ResourceLocation.tryParse(typeId);
        if (rl == null || !p.isAlive()) return null;
        Optional<EntityType<?>> type = BuiltInRegistries.ENTITY_TYPE.getOptional(rl);
        if (type.isEmpty()) return null;
        ServerLevel level = p.serverLevel();
        try {
            if (!(type.get().create(level) instanceof Mob mob)) return null;
            Vec3 look = p.getLookAngle();
            mob.moveTo(p.getX() + look.x * 3, p.getY() + 0.5, p.getZ() + look.z * 3, p.getYRot() + 180f, 0f);
            mob.finalizeSpawn(level, level.getCurrentDifficultyAt(p.blockPosition()), MobSpawnType.MOB_SUMMONED, null);
            AttributeInstance hp = mob.getAttribute(Attributes.MAX_HEALTH);
            if (hp != null) hp.setBaseValue(Math.max(1.0, health));
            mob.setHealth((float) Math.max(1.0, health));
            AttributeInstance atk = mob.getAttribute(Attributes.ATTACK_DAMAGE);
            if (atk != null) atk.setBaseValue(Math.max(0.5, attack));
            mob.setCustomName(net.minecraft.network.chat.Component.literal(name));
            mob.setCustomNameVisible(true);
            mob.setPersistenceRequired();
            mob.getPersistentData().putUUID(FOE_OWNER, p.getUUID());
            mob.getPersistentData().putLong(FOE_UNTIL, level.getGameTime() + Math.max(20, life));
            mob.setTarget(p);
            if (!level.addFreshEntity(mob)) return null;
            Wave30Events.trackFoe(mob);
            return mob;
        } catch (RuntimeException | LinkageError failed) {
            return null;
        }
    }

    static void say(ServerPlayer p, String text) { Fx.say(p, text); }

    static void hint(ServerPlayer p, String text) {
        p.displayClientMessage(net.minecraft.network.chat.Component.literal(text).withStyle(net.minecraft.ChatFormatting.GOLD), true);
    }

    /** A loud chat line in a colour (Vecna's whispers, the Deck's cards). */
    static void shout(ServerPlayer p, String text, net.minecraft.ChatFormatting colour) {
        p.sendSystemMessage(net.minecraft.network.chat.Component.literal(text).withStyle(colour));
    }
}
