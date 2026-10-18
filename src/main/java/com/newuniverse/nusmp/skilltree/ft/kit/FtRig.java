package com.newuniverse.nusmp.skilltree.ft.kit;

import com.newuniverse.nusmp.labyrinth.mega.SelfTests;
import com.newuniverse.nusmp.skilltree.SkillTreeApi;
import com.newuniverse.nusmp.skilltree.data.SkillNode;
import com.newuniverse.nusmp.skilltree.data.SkillRoute;
import com.newuniverse.nusmp.skilltree.state.IPlayerSkillTree;
import com.newuniverse.nusmp.skilltree.state.PlayerSkillTree;
import io.github.manasmods.tensura.storage.TensuraStorages;
import io.github.manasmods.tensura.storage.ep.IExistence;
import io.github.manasmods.tensura.util.EnergyHelper;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.Zombie;
import net.neoforged.neoforge.common.util.FakePlayer;

import java.util.ArrayList;
import java.util.List;

/**
 * Why: the in-game self-tests of the Fairy Tail routes all need the same stage: a stand-in player (a {@code FakePlayer}, the way the other systems of the mod test without a person),
 * a dummy creature to hit, a route granted or taken away, a full pool of magicules, and a way to let time pass so that a technique that finishes later has finished. Building that stage
 * once keeps each route's test a list of questions and not a page of set-up, and it makes the one risky decision (where to stand) in one place: the actors are put high above the
 * spawn column, where there is only air, so no tree, hill or cave wall can sit between the caster and the dummy and make a correct technique look broken; the column is still loaded
 * (the spawn chunk is), so the creatures there are real. The rig remembers the player's tree before it changes it and puts it back on {@link #close}, discards every creature it
 * made, and clears the player's effects, so a test never leaves anything behind in the throw-away CI world or in a world someone plays on. {@link #flush} plays time forward through
 * {@link FtClock#drain}, which is exactly what the server tick does, so delayed effects are tested for real and not mocked.
 */
public final class FtRig implements AutoCloseable {
    public final ServerLevel level;
    public final FakePlayer player;
    private final CompoundTag savedTree;
    private final List<Entity> spawned = new ArrayList<>();

    private FtRig(ServerLevel level, FakePlayer player) {
        this.level = level;
        this.player = player;
        this.savedTree = SkillTreeApi.of(player).toTag();
    }

    /** Opens a stage for the stand-in player called {@code name}, or null when the spawn column is not loaded (the caller then reports the casts as skipped). */
    public static FtRig open(MinecraftServer server, String name) {
        ServerLevel level = server.overworld();
        BlockPos spawn = level.getSharedSpawnPos();
        if (!level.hasChunkAt(spawn)) return null;
        double y = Math.min(280.0, level.getMaxBuildHeight() - 30.0);
        FakePlayer p = SelfTests.player(level, name, spawn.getX() + 0.5, y, spawn.getZ() + 0.5);
        p.setYRot(0f);
        p.setXRot(0f);
        FtRig rig = new FtRig(level, p);
        rig.reset();
        return rig;
    }

    /** A motionless, silent Zombie {@code dz} blocks in front of the player (south, the way the stand-in faces), {@code dx} to the side, at the player's height. */
    public Zombie dummy(double dx, double dz) {
        Zombie z = EntityType.ZOMBIE.create(level);
        if (z == null) throw new IllegalStateException("a Zombie cannot be created");
        z.setNoAi(true);
        z.setPersistenceRequired();
        z.setSilent(true);
        z.moveTo(player.getX() + dx, player.getY(), player.getZ() + dz, 0f, 0f);
        level.addFreshEntity(z);
        spawned.add(z);
        return z;
    }

    /** Adds an entity the test made (an arrow, a second creature) so that {@link #close} removes it. */
    public <T extends Entity> T track(T e) {
        spawned.add(e);
        return e;
    }

    /** Gives the player the route with every node at level 1, or at its maximum level, and makes it their active class (what the cast gate asks for). */
    public void grant(SkillRoute route, boolean maxLevels) {
        IPlayerSkillTree t = SkillTreeApi.of(player);
        t.addRoute(route.id());
        t.setActiveClass(route.classId());
        for (SkillNode n : route.nodes()) t.setNodeLevel(route.id(), n.id(), maxLevels ? n.maxLevel() : 1);
    }

    /** Takes the route away again (the "never bought it" state). */
    public void revoke(SkillRoute route) {
        IPlayerSkillTree t = SkillTreeApi.of(player);
        t.clearRoute(route.id());
        t.removeRoute(route.id());
        t.setActiveClass("");
    }

    /** Full health, no effects, no fire or frost, a full pool of magicules; every creature of the rig healed and cleansed too. */
    public void reset() {
        clean(player);
        player.setAbsorptionAmount(0f);
        topUp();
        for (Entity e : spawned) if (e instanceof LivingEntity le && le.isAlive()) clean(le);
    }

    private static void clean(LivingEntity e) {
        e.removeAllEffects();
        e.setHealth(e.getMaxHealth());
        e.clearFire();
        e.setTicksFrozen(0);
    }

    /** Fills the player's magicule pool and returns it (NaN when the player has no Tensura record). */
    public double topUp() {
        IExistence ex = TensuraStorages.getExistenceFrom(player);
        if (ex == null) return Double.NaN;
        ex.setMagicule(EnergyHelper.getMaxMagicule(player));
        ex.markDirty();
        return ex.getMagicule();
    }

    /** The player's magicules now (NaN when the player has no Tensura record). */
    public double pool() {
        IExistence ex = TensuraStorages.getExistenceFrom(player);
        return ex == null ? Double.NaN : ex.getMagicule();
    }

    /** Lets up to {@code ticks} game ticks pass for the delayed effects, stopping early when nothing is waiting any more. */
    public void flush(int ticks) {
        long t0 = level.getGameTime();
        for (int i = 0; i <= ticks && FtClock.pending() > 0; i++) FtClock.drain(t0 + i);
    }

    @Override
    public void close() {
        for (Entity e : spawned) if (!e.isRemoved()) e.discard();
        spawned.clear();
        flush(400);
        IPlayerSkillTree t = SkillTreeApi.of(player);
        if (t instanceof PlayerSkillTree own) own.copyFrom(PlayerSkillTree.fromTag(savedTree));
        clean(player);
        player.setAbsorptionAmount(0f);
    }
}
