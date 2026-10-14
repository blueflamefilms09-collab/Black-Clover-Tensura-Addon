package com.newuniverse.nusmp.relics.wave;

import com.newuniverse.nusmp.relics.ActCtx;
import com.newuniverse.nusmp.relics.RelicDef;
import com.newuniverse.nusmp.relics.Relics;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;

import java.util.Optional;
import java.util.function.Consumer;

/**
 * Wave 17: the small glue the notebooks, pouches and charms share: the live stack of a relic, its data, per-player persistent data, a safe landing
 * spot, coloured dust and effects by id. Statics only; all server side.
 */
public final class Wave17Kit {
    private Wave17Kit() {}

    private static final String ROOT = "nusmp_w17";

    // ---------------------------------------------------------------- the stack
    /** The live stack of the relic the act came from: the worn one if it is worn (so writes stick), else the one the act was fired with. */
    public static ItemStack live(ActCtx c) {
        ItemStack s = find(c.player(), c.def().id());
        return s.isEmpty() ? c.stack() : s;
    }

    /** The worn (equipped) stack of a relic id, or EMPTY. */
    public static ItemStack find(LivingEntity e, String relicId) {
        for (ItemStack s : Relics.equipped(e)) {
            RelicDef d = Relics.defOf(s);
            if (d != null && relicId.equals(d.id())) return s;
        }
        return ItemStack.EMPTY;
    }

    public static CompoundTag tag(ItemStack s) { return s.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag(); }

    public static void edit(ItemStack s, Consumer<CompoundTag> change) { CustomData.update(DataComponents.CUSTOM_DATA, s, change); }

    // ---------------------------------------------------------------- the player's persistent data
    /** The player's own data compound of this wave (kept through death and relog). */
    public static CompoundTag data(Player p) {
        CompoundTag pd = p.getPersistentData();
        if (!pd.contains(Player.PERSISTED_NBT_TAG, Tag.TAG_COMPOUND)) pd.put(Player.PERSISTED_NBT_TAG, new CompoundTag());
        CompoundTag root = pd.getCompound(Player.PERSISTED_NBT_TAG);
        if (!root.contains(ROOT, Tag.TAG_COMPOUND)) root.put(ROOT, new CompoundTag());
        return root.getCompound(ROOT);
    }

    /** Reads a long from the player's data without creating anything (0 if absent): for the once-a-second passes that touch every player. */
    public static long peekLong(Player p, String key) {
        CompoundTag pd = p.getPersistentData();
        if (!pd.contains(Player.PERSISTED_NBT_TAG, Tag.TAG_COMPOUND)) return 0L;
        CompoundTag root = pd.getCompound(Player.PERSISTED_NBT_TAG);
        return root.contains(ROOT, Tag.TAG_COMPOUND) ? root.getCompound(ROOT).getLong(key) : 0L;
    }

    // ---------------------------------------------------------------- time
    /** The Overworld's game clock: the same for everyone, not moved by sleeping. */
    public static long clock(ServerLevel any) { return any.getServer().overworld().getGameTime(); }

    /** The day number dawn refills count by: the Overworld's, so sleeping through the night is a new dawn everywhere. */
    public static long today(ServerLevel any) { return any.getServer().overworld().getDayTime() / 24000L; }

    // ---------------------------------------------------------------- effects by id
    public static Optional<Holder<MobEffect>> effect(String id) {
        ResourceLocation rl = ResourceLocation.tryParse(id);
        return rl == null ? Optional.empty() : BuiltInRegistries.MOB_EFFECT.getHolder(rl).map(h -> (Holder<MobEffect>) h);
    }

    /** Gives an effect by id to the living thing (visible particles), skipped when the effect does not exist (an absent mod). */
    public static void give(LivingEntity e, String id, int ticks, int amp) {
        effect(id).ifPresent(h -> e.addEffect(new MobEffectInstance(h, ticks, amp)));
    }

    // ---------------------------------------------------------------- words
    /** A short line over the hotbar. */
    public static void hint(ServerPlayer p, String text, ChatFormatting colour) { p.displayClientMessage(Component.literal(text).withStyle(colour), true); }

    // ---------------------------------------------------------------- particles
    public static DustParticleOptions dust(int rgb, float size) {
        return new DustParticleOptions(new Vector3f((rgb >> 16 & 255) / 255f, (rgb >> 8 & 255) / 255f, (rgb & 255) / 255f), size);
    }

    // ---------------------------------------------------------------- a safe place to land
    /**
     * A standing spot for {@code e} within two blocks (sideways) and three (up or down) of {@code around}: room for the body, something solid to
     * stand on, no lava or fire, inside the world border and the height limits. Null if there is none.
     */
    public static Vec3 safeSpot(ServerLevel level, Entity e, BlockPos around) {
        int[] ds = {0, 1, -1, 2, -2};
        int[] dys = {0, 1, -1, 2, -2, 3, -3};
        for (int dy : dys) {
            for (int dx : ds) {
                for (int dz : ds) {
                    BlockPos feet = around.offset(dx, dy, dz);
                    if (!level.isInWorldBounds(feet) || !level.getWorldBorder().isWithinBounds(feet)) continue;
                    double x = feet.getX() + 0.5, y = feet.getY(), z = feet.getZ() + 0.5;
                    AABB box = e.getBoundingBox().move(x - e.getX(), y - e.getY(), z - e.getZ());
                    if (!level.noCollision(e, box)) continue;
                    if (level.getFluidState(feet).is(FluidTags.LAVA)) continue;
                    boolean ground = false, bad = false;
                    for (int i = 1; i <= 2 && !ground && !bad; i++) {
                        BlockPos under = feet.below(i);
                        BlockState st = level.getBlockState(under);
                        if (st.is(Blocks.LAVA) || st.is(Blocks.FIRE) || st.is(Blocks.MAGMA_BLOCK) || st.is(Blocks.CAMPFIRE)) bad = true;
                        else ground = !st.getCollisionShape(level, under).isEmpty();
                    }
                    if (ground && !bad) return new Vec3(x, y, z);
                }
            }
        }
        return null;
    }
}
