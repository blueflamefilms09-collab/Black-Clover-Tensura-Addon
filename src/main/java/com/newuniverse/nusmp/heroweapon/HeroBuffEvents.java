package com.newuniverse.nusmp.heroweapon;

import com.newuniverse.nusmp.core.magic.profile.SafeCast;
import com.newuniverse.nusmp.heroweapon.bond.HeroBond;
import com.newuniverse.nusmp.heroweapon.bond.HeroBonds;
import com.newuniverse.nusmp.heroweapon.skill.HeroCtx;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;

/**
 * 3.55.0: the server-side buffs of the hero weapons, all on the damage event (no tick, no packet): a bound wielder with the weapon out deals
 * {@link HeroBuff#damageFactor} times the damage; a bound wielder takes less; the Legendary Shield throws a share of every blow back (one hurt per incoming
 * blow, guarded against loops). Everything is behind {@code [heroweapon.buff] enabled}.
 */
public final class HeroBuffEvents {
    private HeroBuffEvents() {}

    /** True while the Shield's own counter is being dealt, so it is neither multiplied nor reflected again. */
    private static boolean busy;

    public static void register(IEventBus bus) {
        bus.addListener(EventPriority.NORMAL, HeroBuffEvents::onDamage);
    }

    private static void onDamage(LivingIncomingDamageEvent e) {
        if (busy || e.getEntity().level().isClientSide || e.getAmount() <= 0f || !HeroConfig.enabled() || !HeroBuff.on()) return;
        SafeCast.tick("hero.buff", () -> {
            // the wielder deals damage with the weapon out
            if (e.getSource().getEntity() instanceof ServerPlayer dealer && HeroBonds.has(dealer)) {
                HeroCtx c = HeroCtx.of(dealer);
                if (c != null && !c.stack.isEmpty()) e.setAmount((float) (e.getAmount() * HeroBuff.damageFactor(c.kind, HeroBonds.of(dealer).level())));
            }
            // the wielder takes damage
            if (e.getEntity() instanceof ServerPlayer p && HeroBonds.has(p)) {
                HeroBond b = HeroBonds.of(p);
                if (!b.isBound()) return;
                float in = e.getAmount();
                if (b.kind() == HeroKind.SHIELD && e.getSource().getEntity() instanceof LivingEntity attacker && attacker != p) {
                    float back = HeroBuff.reflect(in);
                    if (back > 0f) {
                        busy = true;
                        try { attacker.hurt(p.damageSources().thorns(p), back); } finally { busy = false; }
                    }
                }
                e.setAmount(HeroBuff.taken(b.kind(), in));
            }
        });
    }
}
