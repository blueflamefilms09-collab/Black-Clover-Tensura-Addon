package com.newuniverse.nusmp.dungeon;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.FallingBlockEntity;
import net.minecraft.world.entity.item.PrimedTnt;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseFireBlock;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.common.ItemAbilities;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.neoforge.event.entity.EntityMobGriefingEvent;
import net.neoforged.neoforge.event.entity.living.LivingDestroyBlockEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.level.BlockEvent;
import net.neoforged.neoforge.event.level.ExplosionEvent;
import net.neoforged.neoforge.event.level.PistonEvent;

import java.util.function.Supplier;

/**
 * Every block of the Dungeon is unbreakable by any means (the {@code unbreakable} switch of the labyrinth config, on by default): not mined
 * (creative too), not blown up, not eaten or carried off by mobs, not stripped, tilled or pushed, not burned, not removed by any spell that
 * goes through the world's block writers. The Dungeon is every Labyrinth dimension (the Mega-Dungeon, and the Spire's sky in an old world) and,
 * always, the arena of the Final Hour. The rule itself is {@link DungeonGuardRules}. The events below stop the ordinary routes early and
 * cleanly (the crack never starts, no drops, the action bar says why); {@code mixin.LevelGuardMixin} (and the chunk mixin beside it) is the net
 * under them for every mod that calls the block writers directly. The mod's own builders (the sector generator, a door that opens) run inside
 * {@link #internal(Runnable)}, which the guard lets through. (Until 1.58.0 it also held the Living Dungeon, and told that dungeon's immune
 * system about every blow at a wall; both went with the Living Dungeon.)
 */
public final class DungeonGuard {
    private DungeonGuard() {}

    private static final String LINE = "The Dungeon's stone does not yield.", STAMP = "nusmp_dungeon_guard_msg";
    /** Vanilla gravity blocks and primed TNT would be copied by a refused removal (the block stays, the entity still appears): they are not let in. */
    private static final boolean FREEZE_GRAVITY = true, FREEZE_TNT = true;
    private static final ThreadLocal<DungeonGuardRules.Depth> DEPTH = ThreadLocal.withInitial(DungeonGuardRules.Depth::new);

    // ---------------------------------------------------------------- the switch
    /** Is the guard holding this world? Server side only, in the Dungeon only, and only while the config switch is on. */
    public static boolean on(Level level) {
        if (level == null || level.isClientSide()) return false;
        if (com.newuniverse.nusmp.arena.ZogratisArena.in(level)) return true;                                       // 1.46.0: the arena of the Final Hour is held by the same guard, always (the fight's furniture must not be dug up)
        if (com.newuniverse.nusmp.arc.arena.ArcArena.in(level)) return !com.newuniverse.nusmp.arc.arena.ArcArena.editing();   // 1.61.0: the story arenas of the arcs, the same way; an operator's edit switch (off at every start) lets a creative operator change one
        if (com.newuniverse.nusmp.labyrinth.Labyrinth.in(level)) return com.newuniverse.nusmp.labyrinth.LabyrinthConfig.get(com.newuniverse.nusmp.labyrinth.LabyrinthConfig.UNBREAKABLE);   // 1.35.0: the Labyrinth is held by the same guard (nobody digs round a sealed door)
        return false;
    }

    /**
     * Does a client know the Dungeon is unbreakable? Only when it can read the synced switch (if it cannot, it predicts nothing and the
     * server still refuses). Used so the crack animation never starts.
     */
    public static boolean predictsClient(Level level) {
        if (level == null || !level.isClientSide()) return false;
        if (com.newuniverse.nusmp.arena.ZogratisArena.in(level)) return true;                                       // 1.46.0: always unbreakable, so the crack never starts
        if (com.newuniverse.nusmp.arc.arena.ArcArena.in(level)) return true;                                       // 1.61.0: the story arenas too (an operator's edit switch lives on the server; a client that predicts a refusal only loses a crack animation)
        if (!com.newuniverse.nusmp.labyrinth.Labyrinth.in(level)) return false;                                   // 1.35.0: the Labyrinth's own switch
        try { return com.newuniverse.nusmp.labyrinth.LabyrinthConfig.UNBREAKABLE.get(); } catch (RuntimeException notSynced) { return false; }
    }

    private static boolean watches(Level level) { return level != null && (level.isClientSide() ? predictsClient(level) : on(level)); }

    // ---------------------------------------------------------------- the mod's own builders
    /** Runs the mod's own world building: nothing it changes is refused. Nests, and always closes. */
    public static void internal(Runnable work) {
        begin();
        try { work.run(); } finally { end(); }
    }

    /** Like {@link #internal(Runnable)}, for work that returns something (a different name so a lambda is never ambiguous). */
    public static <T> T internalGet(Supplier<T> work) {
        begin();
        try { return work.get(); } finally { end(); }
    }

    /** Opens an internal span on this thread; every call needs an {@link #end()} (use a try/finally). */
    public static void begin() { DEPTH.get().begin(); }

    public static void end() { DEPTH.get().end(); }

    public static boolean inInternal() { return DEPTH.get().get() > 0; }

    // ---------------------------------------------------------------- what is held
    /** Water, lava and the like (a bubble column too): a fluid with no block in it. */
    public static boolean isFluidOnly(BlockState s) { return s.getBlock() instanceof LiquidBlock || s.is(Blocks.BUBBLE_COLUMN); }

    private static boolean isFire(BlockState s) { return s.getBlock() instanceof BaseFireBlock; }

    /** Is this block state one the guard holds (everything but air, fluids and fire)? */
    public static boolean isProtected(BlockState s) {
        return DungeonGuardRules.isProtected(0, s.isAir(), isFluidOnly(s), isFire(s));
    }

    // ---------------------------------------------------------------- the block writers (called from the mixins)
    /** Would {@code Level.setBlock} take a held block away (to air, a fluid or fire)? Cheap until it must look at the old state. */
    public static boolean refusesSet(Level level, BlockPos pos, BlockState to) {
        if (!on(level)) return false;
        boolean air = to.isAir(), fluid = isFluidOnly(to), fire = isFire(to);
        if (!DungeonGuardRules.takesAway(air, fluid, fire)) return false;
        int depth = DEPTH.get().get();
        if (depth > 0) return false;
        BlockState from = level.getBlockState(pos);
        return DungeonGuardRules.refusesChange(depth, from.isAir(), isFluidOnly(from), isFire(from), air, fluid, fire);
    }

    /** Would {@code Level.removeBlock} or {@code Level.destroyBlock} take the block here away? */
    public static boolean refusesRemoval(Level level, BlockPos pos) {
        if (!on(level) || inInternal()) return false;
        return isProtected(level.getBlockState(pos));
    }

    /** A player who tried is told, at most once every few seconds; anything else is ignored. */
    public static void tell(Entity who) {
        if (!(who instanceof Player p)) return;
        long now = p.level().getGameTime();
        CompoundTag data = p.getPersistentData();
        if (!DungeonGuardRules.messageDue(now, data.getLong(STAMP), DungeonGuardRules.MESSAGE_INTERVAL)) return;
        data.putLong(STAMP, now);
        p.displayClientMessage(Component.literal(LINE).withStyle(ChatFormatting.GRAY), true);
    }

    // ---------------------------------------------------------------- mining: no crack, no break, no drops
    public static void onBreak(BlockEvent.BreakEvent e) {
        if (!(e.getLevel() instanceof Level level) || !on(level) || !isProtected(e.getState())) return;
        e.setCanceled(true);
        tell(e.getPlayer());
    }

    public static void onBreakSpeed(PlayerEvent.BreakSpeed e) {
        if (!watches(e.getEntity().level()) || !isProtected(e.getState())) return;
        e.setNewSpeed(0f);
        e.setCanceled(true);
    }

    public static void onLeftClick(PlayerInteractEvent.LeftClickBlock e) {
        Level level = e.getLevel();
        if (!watches(level) || !isProtected(level.getBlockState(e.getPos()))) return;
        e.setCanceled(true);
        tell(e.getEntity());
    }

    // ---------------------------------------------------------------- explosions, mobs, pistons, tools
    /** The blast still hurts whoever is in it; it takes no block. */
    public static void onExplosion(ExplosionEvent.Detonate e) {
        if (!on(e.getLevel())) return;
        e.getAffectedBlocks().clear();
    }

    public static void onLivingDestroy(LivingDestroyBlockEvent e) {
        if (on(e.getEntity().level()) && isProtected(e.getState())) e.setCanceled(true);
    }

    /** Nothing in the Dungeon may grief: endermen, ravagers, creepers, withers, dragons, other mods' beasts. Registered last so it has the last word. */
    public static void onMobGriefing(EntityMobGriefingEvent e) {
        Entity who = e.getEntity();
        if (who != null && on(who.level())) e.setCanGrief(false);
    }

    public static void onPiston(PistonEvent.Pre e) {
        if (e.getLevel() instanceof Level level && on(level)) e.setCanceled(true);
    }

    /** No stripping, scraping, flattening or tilling (dousing a campfire is only a lit flag, and stays). */
    public static void onToolUse(BlockEvent.BlockToolModificationEvent e) {
        if (!(e.getLevel() instanceof Level level) || !on(level) || e.getItemAbility() == ItemAbilities.SHOVEL_DOUSE || !isProtected(e.getState())) return;
        e.setCanceled(true);
    }

    public static void onFarmlandTrample(BlockEvent.FarmlandTrampleEvent e) {
        if (e.getLevel() instanceof Level level && on(level)) e.setCanceled(true);
    }

    /** A gravity block or a lit TNT block that cannot be removed must not also spawn its entity (the block would be copied). Entities loaded from disk are left alone. */
    public static void onEntityJoin(EntityJoinLevelEvent e) {
        if (e.loadedFromDisk()) return;
        Class<?> type = e.getEntity().getClass();
        if (((FREEZE_GRAVITY && type == FallingBlockEntity.class) || (FREEZE_TNT && type == PrimedTnt.class)) && on(e.getLevel()) && !inInternal()) e.setCanceled(true);          // the mod's own builders spawn falling blocks inside the guard's internal scope
    }
}
