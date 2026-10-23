package com.newuniverse.nusmp.spiritblades;

import net.minecraft.ChatFormatting;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.SwordItem;
import net.minecraft.world.item.Tiers;
import net.minecraft.world.item.TooltipFlag;
import org.joml.Vector3f;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import com.newuniverse.nusmp.particle.fx.Fx;
import com.newuniverse.nusmp.particle.fx.FxOld;
import com.newuniverse.nusmp.particle.fx.FxThemes;

/**
 * Why: Spirit of Zephyr, the long glowing green sword of the Wind Spirit Dive, as a creative-display item. It is an honest sword: netherite damage and speed (7 damage, 1.6 swings a second),
 * nothing else; no key, no packet, no special magic. A hit throws a few wind-green sparkles at the target, drawn from the server with ordinary particle packets to the players near the
 * spot ({@link SpiritBladesRules}: {@value SpiritBladesRules#HIT_PARTICLES} particles, one burst per attacker every {@value SpiritBladesRules#HIT_GAP_TICKS} ticks, four bursts a tick over the
 * server). Its engravings (severance, barrier piercing, holy weapon) are applied once by {@code item/WeaponEngravings}. In hand it is a 3D model drawn by {@code client/WeaponRenderer}.
 */
public class ZephyrSwordItem extends SwordItem {
    private static final DustParticleOptions MINT = new DustParticleOptions(new Vector3f(0.62f, 1.0f, 0.84f), 0.9f);
    /** The game tick of each attacker's last burst (server thread only, bounded, cleared on logout). */
    private static final Map<UUID, Long> LAST_BURST = new HashMap<>();
    private static long budgetTick = Long.MIN_VALUE;
    private static int bursts;

    public ZephyrSwordItem() {
        super(Tiers.NETHERITE, new Item.Properties().rarity(Rarity.EPIC).fireResistant()
                .attributes(SwordItem.createAttributes(Tiers.NETHERITE, SpiritBladesRules.SWORD_BASE_DAMAGE, SpiritBladesRules.SWORD_SPEED)));
    }

    @Override
    public boolean hurtEnemy(ItemStack stack, LivingEntity target, LivingEntity attacker) {
        boolean r = super.hurtEnemy(stack, target, attacker);
        if (attacker.level() instanceof ServerLevel level) burst(level, target, attacker);
        return r;
    }

    /** The hit sparkles: at most one burst per attacker per few ticks and a few per server tick; ordinary particle packets to the players near the target. */
    static void burst(ServerLevel level, LivingEntity target, LivingEntity attacker) {
        long now = level.getGameTime();
        if (!SpiritBladesRules.hitBurstAllowed(LAST_BURST, attacker.getUUID(), now)) return;
        if (budgetTick != now) {
            budgetTick = now;
            bursts = 0;
        }
        if (bursts >= SpiritBladesRules.HIT_BURSTS_PER_TICK) return;
        bursts++;
        double x = target.getX(), y = target.getY() + target.getBbHeight() * 0.55, z = target.getZ();
        level.sendParticles(MINT, x, y, z, SpiritBladesRules.HIT_PARTICLES - 2, 0.35, 0.35, 0.35, 0.02);
        Fx.spray(level, FxThemes.WORLD, FxOld.END_ROD, x, y, z, 2, 0.25, 0.25, 0.25, 0.04);
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
        tip.add(Component.translatableWithFallback("item.nusmp.spirit_of_zephyr.lore", "The long green blade of the Wind Spirit Zephyr.").withStyle(ChatFormatting.GREEN));
    }
}
