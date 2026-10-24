package com.newuniverse.nusmp.valkyrielance;

import net.minecraft.ChatFormatting;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.SwordItem;
import net.minecraft.world.item.Tiers;
import net.minecraft.world.item.TooltipFlag;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Why: the Valkyrie Drill Lance, the spiral lance of water of Noelle's Valkyrie Dress, as a creative-display item. It is an honest spear-class weapon with a trident's numbers ({@link ValkyrieLanceRules}),
 * nothing else; no key, no packet, no throw. A hit throws a few water drops at the target, drawn from the server with ordinary particle packets to the players near the spot, capped (one burst per attacker every
 * {@value ValkyrieLanceRules#HIT_GAP_TICKS} ticks, {@value ValkyrieLanceRules#HIT_BURSTS_PER_TICK} bursts a tick over the server). Its engravings (barrier piercing, elemental boost, crushing, magic weapon) are
 * applied once by {@code item/WeaponEngravings}. In hand it is a 3D model drawn by {@code client/WeaponRenderer}; the drill part of the model turns on the client, by the game clock (no state, no packet).
 */
public class ValkyrieDrillLanceItem extends SwordItem {
    /** The game tick of each attacker's last burst (server thread only, bounded, cleared on logout). */
    private static final Map<UUID, Long> LAST_BURST = new HashMap<>();
    private static long budgetTick = Long.MIN_VALUE;
    private static int bursts;

    public ValkyrieDrillLanceItem() {
        super(Tiers.IRON, new Item.Properties().rarity(Rarity.EPIC).fireResistant()
                .attributes(SwordItem.createAttributes(Tiers.IRON, ValkyrieLanceRules.BASE_DAMAGE, ValkyrieLanceRules.SPEED)));
    }

    @Override
    public boolean hurtEnemy(ItemStack stack, LivingEntity target, LivingEntity attacker) {
        boolean r = super.hurtEnemy(stack, target, attacker);
        if (attacker.level() instanceof ServerLevel level) burst(level, target, attacker);
        return r;
    }

    /** The hit drops: at most one burst per attacker per few ticks and a few per server tick; ordinary particle packets to the players near the target. */
    static void burst(ServerLevel level, LivingEntity target, LivingEntity attacker) {
        long now = level.getGameTime();
        if (!ValkyrieLanceRules.hitBurstAllowed(LAST_BURST, attacker.getUUID(), now)) return;
        if (budgetTick != now) {
            budgetTick = now;
            bursts = 0;
        }
        if (bursts >= ValkyrieLanceRules.HIT_BURSTS_PER_TICK) return;
        bursts++;
        double x = target.getX(), y = target.getY() + target.getBbHeight() * 0.55, z = target.getZ();
        level.sendParticles(ParticleTypes.SPLASH, x, y, z, ValkyrieLanceRules.HIT_PARTICLES, 0.35, 0.3, 0.35, 0.05);
    }

    /** Forgets an attacker (logout). */
    public static void forget(UUID id) { LAST_BURST.remove(id); }

    /** Forgets everyone (server stop). */
    public static void clearAll() {
        LAST_BURST.clear();
        budgetTick = Long.MIN_VALUE;
        bursts = 0;
    }

    /** How many attackers are remembered (for the self-test). */
    public static int remembered() { return LAST_BURST.size(); }

    @Override
    public void appendHoverText(ItemStack stack, Item.TooltipContext ctx, List<Component> tip, TooltipFlag flag) {
        tip.add(Component.translatableWithFallback("item.nusmp.valkyrie_drill_lance.lore", "The spiral lance of water of the Valkyrie Dress: its drill turns, and a hit scatters drops.").withStyle(ChatFormatting.AQUA));
    }
}
