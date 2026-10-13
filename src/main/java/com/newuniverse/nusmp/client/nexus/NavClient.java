package com.newuniverse.nusmp.client.nexus;

import com.newuniverse.nusmp.NUSMP;
import com.newuniverse.nusmp.dungeon.LivingDungeon;
import com.newuniverse.nusmp.dungeon.nexus.SpatialDistortion;
import com.newuniverse.nusmp.dungeon.nexus.nav.AntiBirdCompassItem;
import com.newuniverse.nusmp.dungeon.nexus.nav.CompassMath;
import com.newuniverse.nusmp.dungeon.nexus.nav.FlameMode;
import com.newuniverse.nusmp.dungeon.nexus.nav.LanternItem;
import com.newuniverse.nusmp.dungeon.nexus.nav.NavItems;
import com.newuniverse.nusmp.dungeon.nexus.nav.NeedleWobble;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.item.ItemProperties;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;

/**
 * The client side of the Dungeon Delving Gear (docs/living_dungeon.md section 3): the two item properties the models switch on.
 * <ul>
 *   <li>{@code nusmp:flame} of the Ki-Resonance Lantern: 0 out, 0.34 amber, 0.67 blue, 1.0 red; the flame the server keeps in the stack,
 *       shown only while the lantern is in its holder's off-hand;</li>
 *   <li>{@code nusmp:angle} of the Anti-Bird Compass: the turn of the needle round the holder, 0 to 1, the way vanilla's compass does it
 *       (the local player's own needle swings and settles). It spins wildly while the holder stands in a pocket of warped space
 *       ({@link SpatialDistortion}), points at the target the server wrote into the stack, and idles slowly when there is none (above
 *       ground, or no life near).</li>
 * </ul>
 * Register from the client section of NUSMP: {@code modEventBus.addListener(NavClient::registerProperties)}. The two helpers at the end are for
 * WeaponRenderer, which draws these items' inventory icon and in-hand mesh itself.
 */
public final class NavClient {
    private NavClient() {}

    private static final NeedleWobble WOBBLE = new NeedleWobble();

    public static void registerProperties(FMLClientSetupEvent e) {
        e.enqueueWork(() -> {
            ItemProperties.register(NavItems.KI_RESONANCE_LANTERN.get(), ResourceLocation.fromNamespaceAndPath(NUSMP.MODID, "flame"),
                    (stack, level, entity, seed) -> flame(stack, entity));
            ItemProperties.register(NavItems.ANTI_BIRD_COMPASS.get(), ResourceLocation.fromNamespaceAndPath(NUSMP.MODID, "angle"),
                    (stack, level, entity, seed) -> angle(stack, level, entity, seed));
        });
    }

    /** The lantern's flame: only the stack in its holder's off-hand burns (anywhere else the server has put it out, and a dropped or stored one shows no flame). */
    static float flame(ItemStack stack, LivingEntity holder) {
        if (holder == null || !ItemStack.isSameItemSameComponents(holder.getOffhandItem(), stack)) return 0f;
        return LanternItem.flameValue(stack);
    }

    /** The compass needle in turns, 0 to 1. */
    static float angle(ItemStack stack, ClientLevel level, LivingEntity entity, int seed) {
        Entity holder = entity != null ? entity : stack.getEntityRepresentation();
        if (holder == null) return 0f;
        ClientLevel world = level != null ? level : holder.level() instanceof ClientLevel c ? c : null;
        if (world == null) return 0f;
        long gameTime = world.getGameTime();
        double time = gameTime + Minecraft.getInstance().getTimer().getGameTimeDeltaPartialTick(false);
        if (!LivingDungeon.in(world)) return (float) CompassMath.idle(time, seed);                                    // above ground it has nothing to point at
        if (SpatialDistortion.at(holder.getBlockX(), holder.getBlockY(), holder.getBlockZ())) return (float) CompassMath.spin(time, seed);   // warped space: it spins wildly
        BlockPos target = AntiBirdCompassItem.targetOf(stack);
        if (target == null) return (float) CompassMath.idle(time, seed);
        double bearing = CompassMath.bearing(holder.getX(), holder.getZ(), target.getX() + 0.5, target.getZ() + 0.5);
        boolean mine = holder == Minecraft.getInstance().player;                                                       // only the local player's own needle is smoothed
        return (float) CompassMath.needle(holder.getVisualRotationYInDegrees(), bearing, mine ? WOBBLE : null, gameTime);
    }

    // ---------------------------------------------------------------- for WeaponRenderer
    /**
     * An item's inventory icon model with its overrides applied. WeaponRenderer renders the {@code _icon} model directly, which skips the
     * override lookup, so for the lantern (flame) and the compass (angle) it must pass the icon through here first.
     */
    public static BakedModel resolveIcon(BakedModel icon, ItemStack stack) {
        Minecraft mc = Minecraft.getInstance();
        BakedModel resolved = icon.getOverrides().resolve(icon, stack, mc.level, mc.player, 0);
        return resolved == null ? icon : resolved;
    }

    /**
     * The mesh id of a stack for the context it is drawn in. A lantern's follows its flame (ki_resonance_lantern, _off, _blue, _red) while it
     * is held; lying on the ground or hung in a frame it burns no more, whatever its stack still says. Everything else is just {@code id}.
     */
    public static String meshId(ItemStack stack, String id, ItemDisplayContext ctx) {
        if (stack.getItem() != NavItems.KI_RESONANCE_LANTERN.get()) return id;
        boolean held = ctx.firstPerson() || ctx == ItemDisplayContext.THIRD_PERSON_LEFT_HAND || ctx == ItemDisplayContext.THIRD_PERSON_RIGHT_HAND;
        return (held ? FlameMode.ofProperty(LanternItem.flameValue(stack)) : FlameMode.OFF).meshId();
    }
}
