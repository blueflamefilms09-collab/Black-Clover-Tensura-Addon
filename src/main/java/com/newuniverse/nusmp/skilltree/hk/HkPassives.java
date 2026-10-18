package com.newuniverse.nusmp.skilltree.hk;

import com.mojang.logging.LogUtils;
import com.newuniverse.nusmp.skilltree.SkillTreeApi;
import com.newuniverse.nusmp.skilltree.data.RouteRegistry;
import net.minecraft.core.Holder;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.living.LivingDropsEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import org.slf4j.Logger;

import java.util.ArrayList;
import java.util.List;

/**
 * Why: nine nodes of the Heavy Knight routes are not buttons but things that are simply true of the player while the route is EQUIPPED, and the owner's rule is exact: "if the active route is not
 * this route, return" - an unequipped route gives zero benefit. So every passive starts with ONE question, {@link #level}, which is the tree's own cast gate ({@code SkillTreeApi.canCast}: the
 * engine is on, the route is unlocked, the node is bought AND the route's class is the player's active class), and a node of an unequipped route answers 0, so its passive is not applied at all and
 * nothing has to be cleaned up when the player switches class. The passives live where the game decides the thing they change: the damage event decides what a blow does (the Cat's Paw cancels it, the
 * Bulwark and the Unyielding shape what the wearer takes, the Lucky Strike and the Riposte shape what the wielder deals), the drops event decides what a kill leaves (the Treasure Nose), and the
 * attribute modifiers (armor, knockback resistance, sword damage, Luck) are TRANSIENT modifiers kept in step with the gate by a light tick every half second: transient so they are never saved (a world
 * saved while the route was equipped carries nothing to leak into a world where it is not), and removed on the first beat after the route is unequipped. All numbers come from the route files by node
 * level, with the shipped level-1 values as fallback. Every handler is wrapped so an exception never reaches the server loop; the per-player rests are game ticks kept in the player's own persistent data (no
 * static state). The listeners are registered once from the route register; the logic is in public static methods so the in-game self-test runs exactly what the events run.
 */
public final class HkPassives {
    private HkPassives() {}

    private static final Logger LOG = LogUtils.getLogger();
    static final String REST_PAW = "nusmp_hk_paw_rest", REST_LAST = "nusmp_hk_last_rest", HURT_AT = "nusmp_hk_hurt_at";
    /** The beat of the attribute tick: every this many ticks (a player's own count modulo, so a server's players do not all update on one tick). */
    public static final int BEAT = 10;

    private static boolean wired;

    public static synchronized void init() {
        if (wired) return;
        wired = true;
        NeoForge.EVENT_BUS.addListener(HkPassives::onIncomingDamage);
        NeoForge.EVENT_BUS.addListener(HkPassives::onDrops);
        NeoForge.EVENT_BUS.addListener(HkPassives::onPlayerTick);
    }

    /** The level of a node when it counts for this player right now, else 0 (not unlocked, not bought, or the route is not the equipped one). */
    public static int level(ServerPlayer p, String route, String node) {
        return SkillTreeApi.canCast(p, route, node) ? Math.max(1, SkillTreeApi.nodeLevel(p, route, node)) : 0;
    }

    private static double num(String route, String node, String key, int lv) { return RouteRegistry.value(route, node, key, lv, HkRoute.fallback(node, key)); }

    // ================================================================ attribute modifiers
    private record Mod(Holder<Attribute> attr, String route, String node, String key, ResourceLocation id) {}

    private static ResourceLocation id(String path) { return ResourceLocation.fromNamespaceAndPath("nusmp", "hk/" + path); }

    private static final List<Mod> MODS = List.of(
            new Mod(Attributes.ARMOR, HkRoute.OATH, HkRoute.IRON_HIDE, "armor", id("iron_hide_armor")),
            new Mod(Attributes.KNOCKBACK_RESISTANCE, HkRoute.OATH, HkRoute.IRON_HIDE, "knockback_resist", id("iron_hide_knockback")),
            new Mod(Attributes.ATTACK_DAMAGE, HkRoute.SWORD, HkRoute.SWORD_MASTERY, "damage_bonus", id("sword_mastery")),
            new Mod(Attributes.LUCK, HkRoute.LUCK, HkRoute.FORTUNES_FAVOR, "luck", id("fortunes_favor")));

    /** The ids of the transient modifiers of the Heavy Knight passives (the tests check that none stays on a player whose route is not equipped). */
    public static List<ResourceLocation> modifierIds() {
        List<ResourceLocation> out = new ArrayList<>();
        for (Mod m : MODS) out.add(m.id());
        return out;
    }

    /** Brings the player's modifiers in step with the gate: applied for a node that counts, removed for one that does not. Returns how many are applied. */
    public static int sync(ServerPlayer p) {
        int applied = 0;
        for (Mod m : MODS) {
            AttributeInstance inst = p.getAttribute(m.attr());
            if (inst == null) continue;
            int lv = level(p, m.route(), m.node());
            boolean wanted = lv > 0;
            if (wanted && m.node().equals(HkRoute.SWORD_MASTERY)) wanted = p.getMainHandItem().is(ItemTags.SWORDS);     // the mastery is of the sword in hand
            if (!wanted) {
                if (inst.hasModifier(m.id())) inst.removeModifier(m.id());
                continue;
            }
            double amount = num(m.route(), m.node(), m.key(), lv);
            AttributeModifier have = inst.getModifier(m.id());
            if (have == null || have.amount() != amount) inst.addOrUpdateTransientModifier(new AttributeModifier(m.id(), amount, AttributeModifier.Operation.ADD_VALUE));
            applied++;
        }
        return applied;
    }

    private static void onPlayerTick(PlayerTickEvent.Post e) {
        try {
            if (e.getEntity() instanceof ServerPlayer p && p.tickCount % BEAT == 5) sync(p);
        } catch (Throwable t) {
            LOG.warn("[nusmp] Heavy Knight passive failed on tick: {}", t.toString());
        }
    }

    // ================================================================ damage
    private static void onIncomingDamage(LivingIncomingDamageEvent e) {
        try {
            if (e.getEntity() instanceof ServerPlayer victim) e.setAmount(taken(victim, e.getSource(), e.getAmount(), () -> e.setCanceled(true)));
            if (e.getSource().getEntity() instanceof ServerPlayer attacker && attacker != e.getEntity() && !e.isCanceled()) e.setAmount(dealt(attacker, e.getAmount()));
        } catch (Throwable t) {
            LOG.warn("[nusmp] Heavy Knight passive failed on damage: {}", t.toString());
        }
    }

    /**
     * A player is about to take {@code amount}: the Cat's Paw may bat it away ({@code cancel} runs and the result is 0), the Bulwark cuts it while a shield is raised, the Unyielding answers a blow that
     * would leave the player under a third of their health, and the Riposte notes that they were hit. Returns the amount to use.
     */
    public static float taken(ServerPlayer p, DamageSource src, float amount, Runnable cancel) {
        if (amount <= 0f || src.is(DamageTypeTags.BYPASSES_INVULNERABILITY)) return amount;
        long now = p.serverLevel().getGameTime();
        CompoundTag d = p.getPersistentData();

        int paw = level(p, HkRoute.LUCK, HkRoute.CATS_PAW);
        if (paw > 0 && now >= d.getLong(REST_PAW)) {
            double chance = num(HkRoute.LUCK, HkRoute.CATS_PAW, "chance", paw) + num(HkRoute.LUCK, HkRoute.CATS_PAW, "luck_bonus", paw) * Math.max(0.0, p.getAttributeValue(Attributes.LUCK));
            if (p.getRandom().nextDouble() < Math.min(0.75, chance)) {
                d.putLong(REST_PAW, now + (long) num(HkRoute.LUCK, HkRoute.CATS_PAW, "rest", paw));
                p.serverLevel().playSound(null, p.blockPosition(), SoundEvents.CAT_HISS, net.minecraft.sounds.SoundSource.PLAYERS, 0.8f, 1.4f);
                p.displayClientMessage(net.minecraft.network.chat.Component.literal("The cat bats the blow aside."), true);
                cancel.run();
                return 0f;
            }
        }

        int wall = level(p, HkRoute.OATH, HkRoute.BULWARK);
        if (wall > 0 && p.isBlocking()) amount *= (float) (1.0 - Math.min(0.9, num(HkRoute.OATH, HkRoute.BULWARK, "reduction", wall)));

        int stand = level(p, HkRoute.OATH, HkRoute.UNYIELDING);
        if (stand > 0 && now >= d.getLong(REST_LAST) && p.getHealth() - amount < p.getMaxHealth() / 3f) {
            d.putLong(REST_LAST, now + (long) num(HkRoute.OATH, HkRoute.UNYIELDING, "rest", stand));
            int ticks = Math.max(20, (int) num(HkRoute.OATH, HkRoute.UNYIELDING, "seconds", stand) * 20);
            int absorbAmp = Math.max(0, (int) Math.round(num(HkRoute.OATH, HkRoute.UNYIELDING, "absorb_hearts", stand) / 2.0) - 1);
            p.addEffect(new MobEffectInstance(MobEffects.ABSORPTION, ticks, absorbAmp, true, false, true));
            p.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, ticks, 1, true, false, true));
        }

        if (level(p, HkRoute.SWORD, HkRoute.RIPOSTE) > 0) d.putLong(HURT_AT, now);
        return amount;
    }

    /** A player deals {@code amount}: the Lucky Strike may crit it and a Riposte after a recent blow raises it. Returns the amount to use. */
    public static float dealt(ServerPlayer a, float amount) {
        if (amount <= 0f) return amount;
        long now = a.serverLevel().getGameTime();
        CompoundTag d = a.getPersistentData();
        int rip = level(a, HkRoute.SWORD, HkRoute.RIPOSTE);
        if (rip > 0 && d.contains(HURT_AT) && now - d.getLong(HURT_AT) <= (long) num(HkRoute.SWORD, HkRoute.RIPOSTE, "window", rip)) {
            d.remove(HURT_AT);
            amount *= (float) (1.0 + num(HkRoute.SWORD, HkRoute.RIPOSTE, "bonus", rip));
        }
        int lucky = level(a, HkRoute.LUCK, HkRoute.LUCKY_STRIKE);
        if (lucky > 0) {
            double chance = num(HkRoute.LUCK, HkRoute.LUCKY_STRIKE, "chance", lucky) + num(HkRoute.LUCK, HkRoute.LUCKY_STRIKE, "luck_bonus", lucky) * Math.max(0.0, a.getAttributeValue(Attributes.LUCK));
            if (a.getRandom().nextDouble() < Math.min(0.6, chance)) {
                amount *= 1.5f;
                a.serverLevel().playSound(null, a.blockPosition(), SoundEvents.PLAYER_ATTACK_CRIT, net.minecraft.sounds.SoundSource.PLAYERS, 0.9f, 1.0f);
            }
        }
        return amount;
    }

    // ================================================================ drops
    private static void onDrops(LivingDropsEvent e) {
        try {
            if (e.getSource().getEntity() instanceof ServerPlayer p) doubled(p, e);
        } catch (Throwable t) {
            LOG.warn("[nusmp] Heavy Knight passive failed on drops: {}", t.toString());
        }
    }

    /** The Treasure Nose: with the node's chance one stack of what the kill dropped is dropped twice. Returns true when it was. */
    static boolean doubled(ServerPlayer p, LivingDropsEvent e) {
        int lv = level(p, HkRoute.LUCK, HkRoute.TREASURE_NOSE);
        if (lv <= 0 || e.getDrops().isEmpty() || p.getRandom().nextDouble() >= num(HkRoute.LUCK, HkRoute.TREASURE_NOSE, "chance", lv)) return false;
        List<ItemEntity> drops = new ArrayList<>(e.getDrops());
        ItemEntity pick = drops.get(p.getRandom().nextInt(drops.size()));
        ItemStack copy = pick.getItem().copy();
        if (copy.isEmpty()) return false;
        e.getDrops().add(new ItemEntity(pick.level(), pick.getX(), pick.getY(), pick.getZ(), copy));
        return true;
    }
}
