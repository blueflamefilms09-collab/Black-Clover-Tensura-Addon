package com.newuniverse.nusmp.power;

import com.newuniverse.nusmp.NUGameRules;
import com.newuniverse.nusmp.beastwill.BeastWillState;
import com.newuniverse.nusmp.beastwill.BeastWillStore;
import com.newuniverse.nusmp.core.magic.profile.SafeCast;
import com.newuniverse.nusmp.heroweapon.HeroKind;
import com.newuniverse.nusmp.heroweapon.bond.HeroBonds;
import com.newuniverse.nusmp.heroweapon.skill.HeroCtx;
import com.newuniverse.nusmp.relics.djinn.DjinnAttachments;
import com.newuniverse.nusmp.relics.djinn.DjinnPhaseRules;
import com.newuniverse.nusmp.relics.djinn.DjinnState;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;

/**
 * Why: the 3.74.0 power raise on the server, on the damage event only (no tick, no packet, no state): a player whose Djinn is out, who holds a Beast Will phase, or who has a hero weapon
 * out (Seven Star / Vassal, and since 3.78.1 the four Legendary Weapons with a larger factor so they stay on top) deals {@link PowerRules#factor} times the damage, held at the PvP hit cap against players; a Djinn bearer or Beast Will holder takes {@code damageTakenFactor} of what hits them.
 * LOW priority: after the hero weapon buff and the relic riders have set their amounts, before the LOWEST listeners (boss caps, shields that read the final blow). Cost: for a player with none of
 * the three, two attachment lookups; nothing per tick.
 */
public final class PowerEvents {
    private PowerEvents() {}

    public static void register(IEventBus bus) { bus.addListener(EventPriority.LOW, PowerEvents::onDamage); }

    private static void onDamage(LivingIncomingDamageEvent e) {
        if (e.getEntity().level().isClientSide || e.getAmount() <= 0f || !PowerConfig.enabled()) return;
        SafeCast.tick("power.boost", () -> {
            LivingEntity target = e.getEntity();
            if (e.getSource().getEntity() instanceof ServerPlayer dealer && dealer != target) {
                double f = Math.max(PowerRules.factor(djinnOut(dealer) ? PowerConfig.djinn() : 1.0, beastHeld(dealer) ? PowerConfig.beastWill() : 1.0, 1.0), heroFactor(dealer));      // the hero factor is taken as it is (a Legendary one may pass the 1000 clamp of the others)
                if (f > 1.0) e.setAmount(PowerRules.dealt(e.getAmount(), f, target instanceof Player, target.getMaxHealth(), NUGameRules.pvpHitCap(target.level())));
            }
            if (target instanceof ServerPlayer p && (djinnOut(p) || beastHeld(p))) e.setAmount(PowerRules.taken(e.getAmount(), PowerConfig.taken()));
        });
    }

    /** The Djinn is out: any phase from the Lesser Summon up. Never creates the attachment. */
    static boolean djinnOut(ServerPlayer p) {
        if (!p.hasData(DjinnAttachments.STATE)) return false;
        DjinnState s = DjinnState.of(p);
        return s.djinn() != null && s.phase() >= DjinnPhaseRules.LESSER;
    }

    /** A Beast Will phase is held (Acquire or Integrate). */
    static boolean beastHeld(ServerPlayer p) {
        BeastWillState s = BeastWillStore.peek(p);
        return s != null && !s.active.isEmpty() && s.phase > 0;
    }

    /** The factor of the hero weapon in the hand: the Vassal factor for a Seven Star or Vassal Weapon, the larger Legendary factor for one of the four Holy Weapons, 1 with none out. */
    static double heroFactor(ServerPlayer p) {
        if (!HeroBonds.has(p)) return 1.0;
        HeroKind k = HeroBonds.kindOf(p);
        if (k == null) return 1.0;
        HeroCtx c = HeroCtx.of(p);
        if (c == null || c.stack.isEmpty()) return 1.0;
        return k.isHoly() ? PowerConfig.legendary() : PowerConfig.vassal();
    }
}
