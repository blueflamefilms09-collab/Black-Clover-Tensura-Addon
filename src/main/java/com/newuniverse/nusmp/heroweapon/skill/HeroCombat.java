package com.newuniverse.nusmp.heroweapon.skill;

import com.newuniverse.nusmp.core.magic.profile.SafeCast;
import com.newuniverse.nusmp.heroweapon.HeroConfig;
import com.newuniverse.nusmp.heroweapon.HeroStyle;
import com.newuniverse.nusmp.heroweapon.HeroWeaponItem;
import com.newuniverse.nusmp.heroweapon.bond.HeroBond;
import com.newuniverse.nusmp.heroweapon.bond.HeroBonds;
import com.newuniverse.nusmp.heroweapon.forms.FormMastery;
import com.newuniverse.nusmp.heroweapon.forms.HeroForms;
import com.newuniverse.nusmp.heroweapon.rules.HeroXp;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.OwnableEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.entity.living.LivingShieldBlockEvent;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Why: the weapon's logic wants to hear about the fights of its wielder (a blow it landed, damage it is about to take, a blow its raised guard met, a kill),
 * and the experience and mastery of the weapon grow from the same moments, but the logic must not have to know which game event carries which. This class is
 * that translation, in one place: the item tells it about a swing and a landed melee blow (a technique's own damage is NOT a blow, because it arrives from a
 * different path), the game events tell it about damage taken, a guard that met a blow, and a kill. Each handler is guarded so a fault in a weapon's logic
 * is logged once and never reaches the game. A blow gives 1 experience (3 on a full swing), a kill the victim's health share, a block that stopped something one
 * mastery point; the amounts are the config's and {@code HeroXp}'s.
 */
public final class HeroCombat {
    private HeroCombat() {}

    /** The attack strength (0..1) when the swing began, per player: read when the blow lands, because the strength is reset before the item hears of the hit. */
    private static final Map<UUID, Float> SWING = new HashMap<>();

    /** The last damage a player dealt directly: to whom (entity id), how much, at which game time; read by the item in the same tick. */
    private record Dealt(int target, float amount, long tick) {}

    private static final Map<UUID, Dealt> DEALT = new HashMap<>();

    /** Registers the listeners on the game bus. */
    public static void register(IEventBus bus) {
        bus.addListener(EventPriority.LOWEST, HeroCombat::onDamageDone);
        bus.addListener(HeroCombat::onIncoming);
        bus.addListener(HeroCombat::onShieldBlock);
        bus.addListener(EventPriority.LOW, HeroCombat::onKill);
    }

    // ================================================================ the wielder's blows (called by HeroWeaponItem)
    /** The wielder began a swing at an entity: remember how charged the attack was. */
    public static void onSwing(Player player) {
        if (!player.level().isClientSide) SWING.put(player.getUUID(), player.getAttackStrengthScale(0.5f));
    }

    /** A melee blow of the summoned weapon landed on {@code target}: the logic's {@code onHit}, a mastery point, and the experience. */
    public static void onMelee(ServerPlayer p, LivingEntity target) {
        SafeCast.tick("hero.melee", () -> {
            if (!HeroConfig.enabled()) return;
            HeroCtx c = HeroCtx.of(p);
            if (c == null || c.stack.isEmpty()) return;
            Float strength = SWING.remove(p.getUUID());
            boolean full = strength != null && strength >= 0.9f;
            float dealt = dealtTo(p, target);
            SafeCast.tick("hero.melee.logic", () -> HeroLogics.of(c.kind).onHit(c, target, dealt, full));
            SafeCast.tick("hero.melee.mastery", () -> FormMastery.hit(p));
            HeroXp.gain(p, full ? 3 : 1);
        });
    }

    private static float dealtTo(ServerPlayer p, LivingEntity target) {
        Dealt d = DEALT.get(p.getUUID());
        if (d != null && d.target() == target.getId() && d.tick() == p.level().getGameTime()) return d.amount();
        return (float) p.getAttributeValue(Attributes.ATTACK_DAMAGE);
    }

    /** Records the damage a player dealt directly (so the blow the item hears of next can say how much it was). */
    private static void onDamageDone(LivingDamageEvent.Post e) {
        if (e.getEntity().level().isClientSide) return;
        Entity attacker = e.getSource().getEntity();
        if (attacker instanceof ServerPlayer p && e.getSource().getDirectEntity() == p)
            DEALT.put(p.getUUID(), new Dealt(e.getEntity().getId(), e.getNewDamage(), p.level().getGameTime()));
    }

    // ================================================================ damage taken
    /** The wielder is about to take damage: the weapon's logic may cut it, whether or not the weapon is out. */
    private static void onIncoming(LivingIncomingDamageEvent e) {
        if (!(e.getEntity() instanceof ServerPlayer p) || e.getAmount() <= 0f || !HeroConfig.enabled()) return;
        if (!HeroBonds.has(p)) return;
        SafeCast.tick("hero.damaged", () -> {
            HeroCtx c = HeroCtx.of(p);
            if (c == null) return;
            float in = e.getAmount();
            float out = HeroLogics.of(c.kind).onDamaged(c, e.getSource(), in);
            if (Float.isNaN(out) || out < 0f) out = 0f;
            if (out != in) e.setAmount(out);
        });
    }

    /**
     * A raised guard met a blow: the weapon's {@code onBlocked} says what gets through, and the event is told to block the rest. The guard takes no wear
     * (a hero weapon does not break). A block that stopped something counts one mastery point.
     */
    private static void onShieldBlock(LivingShieldBlockEvent e) {
        if (!(e.getEntity() instanceof ServerPlayer p) || !e.getBlocked() || !HeroConfig.enabled()) return;
        ItemStack use = p.getUseItem();
        if (!(use.getItem() instanceof HeroWeaponItem w) || w.kind().style() != HeroStyle.BLOCK) return;
        HeroBond b = HeroBonds.of(p);
        if (!b.isBound() || b.kind() != w.kind()) return;
        SafeCast.tick("hero.block", () -> {
            HeroCtx c = new HeroCtx(p, b, HeroForms.active(b), use);
            float original = e.getOriginalBlockedDamage();
            float through = HeroLogics.of(c.kind).onBlocked(c, e.getDamageSource(), original);
            if (Float.isNaN(through)) through = original;
            through = Math.max(0f, Math.min(original, through));
            e.setBlockedDamage(original - through);
            e.setShieldDamage(0f);
            if (through < original) FormMastery.block(p);
        });
    }

    // ================================================================ kills
    /** The wielder killed something (by any means, the weapon out or not): the logic hears of it; the experience follows when the weapon is out. */
    private static void onKill(LivingDeathEvent e) {
        LivingEntity victim = e.getEntity();
        if (victim.level().isClientSide || !HeroConfig.enabled()) return;
        if (!(e.getSource().getEntity() instanceof ServerPlayer p) || p == victim || !HeroBonds.has(p)) return;
        if (victim.isAlliedTo(p) || victim instanceof OwnableEntity pet && p.getUUID().equals(pet.getOwnerUUID())) return;
        SafeCast.tick("hero.kill", () -> {
            HeroCtx c = HeroCtx.of(p);
            if (c == null) return;
            SafeCast.tick("hero.kill.logic", () -> HeroLogics.of(c.kind).onKill(c, victim));
            if (c.bond.summoned() || p.isCreative()) HeroXp.gainKill(p, victim);
        });
    }

    // ================================================================ housekeeping
    /** Forgets one player's swing (they left). */
    public static void forget(UUID player) {
        SWING.remove(player);
        DEALT.remove(player);
    }

    /** Forgets everything (the server stopped). */
    public static void clear() {
        SWING.clear();
        DEALT.clear();
    }
}
