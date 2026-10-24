package com.newuniverse.nusmp.star;

import com.newuniverse.nusmp.arc.ArcRoster;
import com.newuniverse.nusmp.arc.ArcSaga;
import com.newuniverse.nusmp.arc.story.StoryAttachments;
import com.newuniverse.nusmp.arc.world.SagaRewards;
import com.newuniverse.nusmp.blackclover.GrimoirePages;
import com.newuniverse.nusmp.blackclover.Kingdom;
import com.newuniverse.nusmp.blackclover.MagicType;
import com.newuniverse.nusmp.book.GrimoireBook;
import com.newuniverse.nusmp.prop.MagicPropEntity;
import com.newuniverse.nusmp.prop.MagicProps;
import com.newuniverse.nusmp.prop.PropKind;
import com.newuniverse.nusmp.skill.NUSkills;
import io.github.manasmods.manascore.skill.api.ManasSkillInstance;
import io.github.manasmods.manascore.skill.api.SkillAPI;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * Star Magic nodes: the server side (docs/vfx_remake/star_ingame.md). Holds the two pure registries ({@link StarNodes}, {@link StarPaneBook}), places and uses the stars, decides who is a Grinberryall heir, and
 * cleans up on logout, death, dimension change and server stop. Everything runs on the server thread; the client sends nothing new (a cast is the normal grimoire cast) and only draws the tracked prop entities.
 *
 * <p>Cost (rules 9 and 11): one star is one {@code MagicPropEntity} with a life counter and no per-tick scan; at most {@link StarConfig#serverCap()} (128) stars and {@link StarRules#PANES_SERVER} (8) panes exist
 * on the whole server; a placement is one ray (the aim), at most four block-collision tests and one entity spawn; looking at a player's stars is a loop over at most eight records plus one entity-by-UUID lookup
 * each (a record whose body is gone is dropped).
 */
public final class StarGame {
    private StarGame() {}

    public static final StarNodes NODES = new StarNodes();
    public static final StarPaneBook PANES = new StarPaneBook();
    /** Game tick of each mage's last placement (cleared on logout). */
    private static final Map<UUID, Long> LAST_SEED = new HashMap<>();

    public static String dimOf(Level l) { return l.dimension().location().toString(); }

    public static StarMath.P p(Vec3 v) { return new StarMath.P(v.x, v.y, v.z); }

    public static Vec3 v(StarMath.P p) { return new Vec3(p.x(), p.y(), p.z()); }

    // ------------------------------------------------------------------------------------------------ the heir
    /** The player's Star grimoire (their own skill instance), if they have one. */
    public static Optional<ManasSkillInstance> starGrimoire(ServerPlayer p) {
        return SkillAPI.getSkillsFrom(p).getSkill(NUSkills.grimoireSkillFor(MagicType.STAR).getRegistryName());
    }

    /** The Grinberryall heir mark: a Spade Star grimoire at the needed mastery AND the Spade Kingdom Raid cleared in the player's story. Computed, never stored. */
    public static boolean isHeir(ServerPlayer p) {
        try {
            Optional<ManasSkillInstance> inst = starGrimoire(p);
            if (inst.isEmpty() || !(inst.get().getSkill() instanceof GrimoireBook book)) return false;
            boolean spade = GrimoirePages.coverOf(inst.get()).kingdom == Kingdom.SPADE;
            if (!spade) return false;
            boolean cleared = SagaRewards.cleared(StoryAttachments.get(p), ArcSaga.SPADE_RAID, ArcRoster.all());
            return StarRules.isHeir(true, book.masteryFrac(inst.get()), StarConfig.heirMastery(), cleared);
        } catch (RuntimeException e) {
            return false;                       // a story that cannot be read is "not an heir", never a crash of a cast
        }
    }

    // ------------------------------------------------------------------------------------------------ the stars
    private static ServerLevel levelOf(MinecraftServer server, String dim) {
        ResourceLocation rl = ResourceLocation.tryParse(dim);
        return rl == null ? null : server.getLevel(ResourceKey.create(Registries.DIMENSION, rl));
    }

    private static void discardBody(MinecraftServer server, StarNodes.Node n, boolean pop) {
        ServerLevel lv = levelOf(server, n.dim());
        Entity e = lv == null ? null : lv.getEntity(n.entity());
        if (e instanceof MagicPropEntity m && !m.isRemoved()) { if (pop) m.expire(); else m.discard(); }
    }

    private static void discardAll(MinecraftServer server, List<StarNodes.Node> nodes) { for (StarNodes.Node n : nodes) discardBody(server, n, false); }

    /** The result of a placement: the star, or the reason it was refused. */
    public record Placed(StarNodes.Node node, String problem) {}

    /** The mage's live stars in their dimension, oldest first; expired or bodiless ones are dropped on the way. */
    public static List<StarNodes.Node> live(ServerPlayer p) {
        ServerLevel level = p.serverLevel();
        long now = level.getGameTime();
        List<StarNodes.Node> expired = new ArrayList<>();
        List<StarNodes.Node> all = NODES.live(p.getUUID(), dimOf(level), now, expired);
        discardAll(level.getServer(), expired);
        List<StarNodes.Node> out = new ArrayList<>(all.size());
        for (StarNodes.Node n : all) {
            Entity e = level.getEntity(n.entity());
            if (e instanceof MagicPropEntity m && !m.isRemoved()) out.add(n);
            else NODES.remove(p.getUUID(), n.entity());
        }
        return out;
    }

    /** Is there room for a star at this point (no solid block in a 0.5 box, chunk loaded)? */
    static boolean free(ServerLevel level, Vec3 at) {
        BlockPos bp = BlockPos.containing(at);
        return level.isLoaded(bp) && level.noCollision(new AABB(at.x - 0.25, at.y - 0.25, at.z - 0.25, at.x + 0.25, at.y + 0.25, at.z + 0.25));
    }

    /**
     * Hangs a star at {@code at} for {@code p}: validates (the mod switch, a living caster, range, the loaded chunk, a free spot), spawns the body, records it, applies both caps and discards what is evicted.
     * {@code checkCooldown} false is for the automatic quartet (it is part of one cast).
     */
    public static Placed placeAt(ServerPlayer p, Vec3 at, boolean checkCooldown) {
        if (!StarConfig.nodes()) return new Placed(null, "Star nodes are switched off on this server.");
        if (!p.isAlive() || p.isSpectator()) return new Placed(null, "You cannot hang a star now.");
        ServerLevel level = p.serverLevel();
        long now = level.getGameTime();
        if (checkCooldown) {
            Long last = LAST_SEED.get(p.getUUID());
            if (last != null && now >= last && now - last < StarConfig.seedCooldown()) return new Placed(null, "The stars are still settling.");
        }
        if (at.distanceTo(p.getEyePosition()) > StarConfig.placeRange() + 2.0) return new Placed(null, "That point is too far away.");
        if (!free(level, at)) return new Placed(null, "There is no room to hang a star there.");
        boolean heir = isHeir(p);
        int index = NODES.count(p.getUUID());
        Vec3 floor = groundBelow(p, at);
        int word = StarRules.packNode(index, floor == null ? 0 : at.y - floor.y);
        MagicPropEntity body = MagicProps.spawn(level, PropKind.STAR_NODE, at, 0f, 1f, StarConfig.lifeTicks(), word, p);
        if (body == null) return new Placed(null, "The star would not form.");
        StarNodes.Node node = new StarNodes.Node(body.getUUID(), dimOf(level), at.x, at.y, at.z, NODES.nextSeq(), now + StarConfig.lifeTicks());
        List<StarNodes.Node> out = NODES.add(p.getUUID(), node, StarConfig.perPlayer(heir), StarConfig.serverCap());
        discardAll(level.getServer(), out);
        LAST_SEED.put(p.getUUID(), now);
        return new Placed(node, null);
    }

    /** The star a seed cast hangs: above the aimed point (up to 3 blocks, lower when blocked), or at the end of the ray when it hit nothing. */
    public static Placed seed(ServerPlayer p) {
        double range = StarConfig.placeRange();
        HitResult hit = p.pick(range, 1f, false);
        Vec3 base = hit.getLocation();
        if (hit.getType() != HitResult.Type.BLOCK) return placeAt(p, base, true);
        for (int lift = 3; lift >= 0; lift--) {
            Vec3 at = base.add(0, lift + (lift == 0 ? 0.6 : 0.0), 0);
            if (free(p.serverLevel(), at)) return placeAt(p, at, true);
        }
        return new Placed(null, "There is no room to hang a star there.");
    }

    /**
     * The {@code n} newest stars within 48 blocks (a list of exactly {@code n}), placing the missing ones on a ring in front of the mage when {@code star.autoQuartet} allows it (only for four). Null when
     * there are not enough; {@code reason[0]} then holds the message.
     */
    public static List<StarNodes.Node> quartet(ServerPlayer p, int n, String[] reason) {
        List<StarNodes.Node> live = live(p);
        List<StarNodes.Node> near = new ArrayList<>();
        StarMath.P me = p(p.position());
        for (StarNodes.Node s : live) if (s.pos().dist(me) <= StarRules.QUARTET_RANGE) near.add(s);
        if (near.size() >= n) return new ArrayList<>(near.subList(near.size() - n, near.size()));
        if (!StarConfig.autoQuartet() || n != 4) {
            reason[0] = (n == 4 ? "Four" : "Five") + " stars are needed (Star Seed hangs them).";
            return null;
        }
        Vec3 look = p.getViewVector(1f);
        double yaw = Math.atan2(look.z, look.x);
        Vec3 centre = p.position().add(look.multiply(1, 0, 1).normalize().scale(4.0)).add(0, p.getBbHeight() * 1.2, 0);
        List<StarMath.P> ring = StarMath.ring(p(centre), yaw, 4, 3.0);
        int need = n - near.size();
        for (int i = 0; i < ring.size() && need > 0; i++) {
            Vec3 at = v(ring.get(i));
            if (!free(p.serverLevel(), at)) at = at.add(0, 1.0, 0);
            if (!free(p.serverLevel(), at)) continue;
            Placed placed = placeAt(p, at, false);
            if (placed.node() != null) { near.add(placed.node()); need--; }
        }
        if (near.size() < n) { reason[0] = "There is no room for four stars here."; return null; }
        return new ArrayList<>(near.subList(near.size() - n, near.size()));
    }

    /** Uses up these stars (Conjunction, Hasta): they leave the registry and their bodies pop. */
    public static void consume(ServerPlayer p, List<StarNodes.Node> stars) {
        MinecraftServer server = p.getServer();
        for (StarNodes.Node n : stars) {
            NODES.remove(p.getUUID(), n.entity());
            if (server != null) discardBody(server, n, true);
        }
    }

    /** A body ended by itself (its life ran out): forget its record. */
    public static void bodyGone(MagicPropEntity e) {
        UUID owner = e.ownerId();
        if (owner != null) NODES.remove(owner, e.getUUID());
    }

    // ------------------------------------------------------------------------------------------------ the panes
    /** Breaks (discards) the bodies of panes that left the book. */
    public static void discardPanes(MinecraftServer server, List<StarPaneBook.Pane> panes) {
        for (StarPaneBook.Pane pane : panes) {
            ServerLevel lv = levelOf(server, pane.dim);
            Entity e = lv == null ? null : lv.getEntity(pane.entity);
            if (e instanceof MagicPropEntity m && !m.isRemoved()) m.discard();
        }
    }

    // ------------------------------------------------------------------------------------------------ cleanup (logout, death, dimension, stop)
    /** Forgets and discards everything a player holds: stars, pane, cooldown. */
    public static void forget(ServerPlayer p) {
        UUID id = p.getUUID();
        MinecraftServer server = p.getServer();
        List<StarNodes.Node> gone = NODES.clear(id);
        List<StarPaneBook.Pane> panes = new ArrayList<>();
        PANES.clearOwner(id, panes);
        LAST_SEED.remove(id);
        if (server != null) { discardAll(server, gone); discardPanes(server, panes); }
    }

    public static void clearAll(MinecraftServer server) {
        discardAll(server, NODES.clearAll());
        discardPanes(server, PANES.clearAll());
        LAST_SEED.clear();
    }

    /** Ground under a star: a downward ray of at most 16 blocks; null when there is none (no floor within reach). */
    public static Vec3 groundBelow(ServerPlayer p, Vec3 star) {
        Vec3 end = star.add(0, -StarRules.DEST_SEARCH_DOWN, 0);
        HitResult hit = p.serverLevel().clip(new ClipContext(star, end, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, p));
        return hit.getType() == HitResult.Type.BLOCK ? hit.getLocation() : null;
    }
}
