package com.newuniverse.nusmp.dungeon.nexus.nav;

import com.newuniverse.nusmp.dungeon.DungeonTier;
import com.newuniverse.nusmp.dungeon.LivingDungeon;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import net.neoforged.neoforge.event.entity.living.LivingDropsEvent;

import java.util.Optional;

/**
 * The Anti-Bird Compass drops (docs/living_dungeon.md section 3): a rare drop of the avian scavengers of the Upper Catacombs. When a player
 * kills a creature of the entity-type tag {@code nusmp:dungeon/avian} on the upper floor, {@link AvianRules} gives it a 4% chance (Looting
 * adds). The Dungeon clears every drop of what dies in it (DungeonEcology.onDrops: it leaves a carcass instead), so this handler runs
 * <b>after</b> it (priority LOWEST): it is the one designed exception, and it adds the compass back to the list the Dungeon emptied.
 */
public final class AvianDrops {
    private AvianDrops() {}

    /** The birds and bats of the roster (every entry of the tag is optional). */
    public static final TagKey<EntityType<?>> AVIAN = TagKey.create(Registries.ENTITY_TYPE, ResourceLocation.fromNamespaceAndPath("nusmp", "dungeon/avian"));

    private static final ResourceKey<Enchantment> LOOTING = ResourceKey.create(Registries.ENCHANTMENT, ResourceLocation.withDefaultNamespace("looting"));

    /** Register at EventPriority.LOWEST on the game bus. */
    public static void onDrops(LivingDropsEvent e) {
        LivingEntity victim = e.getEntity();
        if (!(victim.level() instanceof ServerLevel level) || !LivingDungeon.in(level) || !victim.getType().is(AVIAN)) return;
        if (!(e.getSource().getEntity() instanceof Player killer)) return;
        if (DungeonTier.at(level, victim.blockPosition()) != DungeonTier.UPPER_CATACOMBS) return;
        if (!AvianRules.drops(looting(level, killer), level.getRandom().nextDouble())) return;
        ItemEntity drop = new ItemEntity(level, victim.getX(), victim.getY() + 0.3, victim.getZ(), new ItemStack(NavItems.ANTI_BIRD_COMPASS.get()));
        drop.setDefaultPickUpDelay();
        e.getDrops().add(drop);
    }

    /** The Looting level of the weapon in the killer's main hand (0 if it has none). */
    static int looting(ServerLevel level, Player killer) {
        Optional<? extends Holder<Enchantment>> looting = level.registryAccess().lookupOrThrow(Registries.ENCHANTMENT).get(LOOTING);
        return looting.isEmpty() ? 0 : Math.max(0, killer.getMainHandItem().getEnchantments().getLevel(looting.get()));
    }
}
