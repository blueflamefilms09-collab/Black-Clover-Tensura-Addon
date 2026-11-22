package com.newuniverse.nusmp.arc.magi;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;

/**
 * Why (4.105.0): the one write of Sinbad's maximum health ({@code sinbad_boss.healthFactor}). It is not damage and not a heal: the permanent modifier changes the maximum and the share of health he
 * had is kept. It lives here, not in {@link SinbadBoss}, because the source scan keeps every damage and heal of that class on the boss damage law. Called once per boss (idempotent: the same
 * factor twice changes nothing). Server thread only.
 */
final class SinbadHealth {
    private SinbadHealth() {}

    private static final ResourceLocation HEALTH_ID = ResourceLocation.fromNamespaceAndPath("anime_multiverse", "sinbad_health");

    static void apply(SinbadBoss b, double factor) {
        AttributeInstance max = b.getAttribute(Attributes.MAX_HEALTH);
        if (max == null || factor <= 1.0 + 1.0e-9) return;
        AttributeModifier have = max.getModifier(HEALTH_ID);
        if (have != null && Math.abs(have.amount() - (factor - 1.0)) < 1.0e-6) return;
        float before = b.getMaxHealth(), hp = b.getHealth();
        max.addOrReplacePermanentModifier(new AttributeModifier(HEALTH_ID, factor - 1.0, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL));
        float share = before <= 0.0F ? 1.0F : Math.min(1.0F, hp / before);
        float to = Math.max(1.0F, b.getMaxHealth() * share);
        b.dpsBudget().bypass(() -> com.newuniverse.nusmp.boss.rules.BossHealGuard.allow(() -> b.setHealth(to)));
    }
}
