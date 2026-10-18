package com.newuniverse.nusmp.labyrinth.mega;

import com.mojang.authlib.GameProfile;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.util.FakePlayer;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Why: NeoForge's {@code FakePlayer} is made for machines that act in the world without ever standing anywhere, so it answers {@code position()} with the origin and
 * {@code blockPosition()} with 0, 0, 0 whatever {@code setPos} said, and vanilla builds the bounding box from {@code position()}. A self-test that moves a stand-in to a
 * place and then drives a technique, a shop or a circle of allies around it asks the stand-in "where are you" through those methods and is told "at the origin", so it tests
 * a player who is a hundred blocks from the scene. This subclass answers with the coordinates the entity really has (the same thing {@code Entity.position()} does for every
 * other creature) and changes nothing else: it is still a {@code FakePlayer}, with the same no-op network handler and the same invulnerability, and it is still not part of the
 * level's entity list or player list, so code that looks a caster up in the level must include the caster itself (the techniques of the skill tree do). One stand-in is kept per profile
 * and level, like {@code FakePlayerFactory} does; the older self-tests keep using {@link SelfTests#player} and are untouched.
 */
public final class StandIn extends FakePlayer {
    private static final Map<UUID, StandIn> CACHE = new HashMap<>();

    private StandIn(ServerLevel level, GameProfile profile) { super(level, profile); }

    /** The stand-in of this profile in this level: the one made before when it is still for this level, else a new one. */
    public static StandIn get(ServerLevel level, GameProfile profile) {
        StandIn kept = CACHE.get(profile.getId());
        if (kept != null && kept.level() == level) return kept;
        StandIn made = new StandIn(level, profile);
        CACHE.put(profile.getId(), made);
        return made;
    }

    /** Forgets every stand-in (the server stopped: they hold a level). */
    public static void clear() { CACHE.clear(); }

    @Override
    public Vec3 position() { return new Vec3(getX(), getY(), getZ()); }

    @Override
    public BlockPos blockPosition() { return BlockPos.containing(getX(), getY(), getZ()); }
}
