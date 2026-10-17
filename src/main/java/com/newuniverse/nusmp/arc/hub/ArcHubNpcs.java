package com.newuniverse.nusmp.arc.hub;

import com.mojang.logging.LogUtils;
import com.newuniverse.nusmp.arc.ArcCharacter;
import com.newuniverse.nusmp.arc.ArcConfig;
import com.newuniverse.nusmp.arc.ArcNpcEntity;
import com.newuniverse.nusmp.arc.ArcRoster;
import com.newuniverse.nusmp.arc.ArcSpawner;
import com.newuniverse.nusmp.arc.story.ArcStory;
import com.newuniverse.nusmp.arc.world.ArcWorldConfig;
import com.newuniverse.nusmp.labyrinth.Labyrinth;
import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.entity.EntityTypeTest;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import org.slf4j.Logger;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Why: the friendly characters of the arcs stand in the Hub, on the posts of the Promenade, once the story has brought them there, and the pass that keeps them
 * standing runs every five seconds on a world that is saved, restarted and half unloaded at any time. The rules of what stands where are pure and tested
 * ({@link HubPosts} gives every character a post that never moves, {@link NpcPresence} turns the wish and what the pass saw into exactly one act); this class is
 * the glue: one scan of the Hub's loaded NPCs per pass, the count of online players whose story opens each character, and the carrying-out of the acts. Three
 * promises hold. A character is never made twice: the pass counts every Hub-placed NPC of that character in the loaded part of the level, not only the ones
 * near the post, and it makes none unless the whole ground round the post is loaded (so it never acts on what it cannot see). It never touches an NPC an
 * operator placed by hand (the ones it placed carry a mark of their own). And it never loads a chunk. The posts are the Promenade's, so nothing is placed while
 * {@code arc.chronicle} is off or before the site is laid; {@code arc.hubNpcs} off stops the pass (what stands stays, an operator clears it with a command).
 */
public final class ArcHubNpcs {
    private ArcHubNpcs() {}

    private static final Logger LOG = LogUtils.getLogger();
    /** The persistent mark of an NPC this pass placed. */
    public static final String MARK = "nusmp_arc_hub_npc";
    public static final int EVERY = 100;

    private static boolean registered;

    public static synchronized void register(IEventBus bus) {
        if (registered) return;
        registered = true;
        bus.addListener(ArcHubNpcs::onTick);
    }

    private static void onTick(ServerTickEvent.Post e) {
        MinecraftServer server = e.getServer();
        if (server.getTickCount() % EVERY != 0 || !ArcWorldConfig.hubNpcs()) return;
        try {
            ServerLevel hub = server.getLevel(Labyrinth.SANCTUARY_DIMENSION);
            if (hub == null || hub.players().isEmpty() || ChronicleSave.get(hub).version() < ChronicleBuilder.BUILD_VERSION) return;
            pass(hub, server);
        } catch (RuntimeException ex) {
            LOG.error("[nusmp] the Hub NPC pass failed: {}", ex.toString());
        }
    }

    /** What one pass did (for the command and the self-test). */
    public record Report(int spawned, int removed, int deduped, int kept, int unseen, List<String> unplaced) {}

    /** One ensure pass. Public so a command can run it and the self-test can drive it; the caller has checked the switches. */
    public static Report pass(ServerLevel hub, MinecraftServer server) {
        List<ArcCharacter> npcs = ArcRoster.npcs();
        List<String> ids = new ArrayList<>();
        for (ArcCharacter c : npcs) ids.add(c.id());
        HubPosts.Assignment posts = HubPosts.assign(ids, ArcWorldConfig.hubGuide(), ChronicleSite.posts().size());
        Map<String, List<ArcNpcEntity>> found = standing(hub);
        List<ServerPlayer> online = server.getPlayerList().getPlayers();
        int spawned = 0, removed = 0, deduped = 0, kept = 0, unseen = 0;
        for (ArcCharacter c : npcs) {
            Integer index = posts.posts().get(c.id());
            List<ArcNpcEntity> there = found.getOrDefault(c.id(), List.of());
            int unlockedFor = 0;
            if (!ArcWorldConfig.hubNpcsAlways() && !c.prerequisites().isEmpty()) for (ServerPlayer p : online) if (ArcStory.unlocked(p, c)) unlockedFor++;
            boolean wanted = index != null && NpcPresence.wanted(ArcWorldConfig.hubNpcsAlways(), c.prerequisites(), unlockedFor);
            ChronicleSite.Post post = index == null ? null : ChronicleSite.post(index);
            boolean seeable = post == null || loaded(hub, post);
            NpcPresence.Act act = NpcPresence.decide(wanted && post != null, seeable, there.size());
            switch (act) {
                case SPAWN -> {
                    ArcNpcEntity npc = ArcSpawner.spawnNpc(hub, c, Vec3.atBottomCenterOf(new BlockPos(post.x(), post.y(), post.z())), post.yaw());
                    if (npc != null) {
                        npc.getPersistentData().putBoolean(MARK, true);
                        spawned++;
                    }
                }
                case DEDUPE -> {
                    ArcNpcEntity keep = nearest(there, post);
                    for (ArcNpcEntity n : there) if (n != keep) {
                        n.discard();
                        deduped++;
                    }
                }
                case REMOVE -> {
                    for (ArcNpcEntity n : there) n.discard();
                    removed += there.size();
                }
                case KEEP -> {
                    if (!seeable) unseen++;
                    else kept++;
                }
            }
        }
        return new Report(spawned, removed, deduped, kept, unseen, posts.unplaced());
    }

    /** Every Hub-placed NPC in the loaded part of the level, by character id. One scan per pass, however many characters there are. */
    public static Map<String, List<ArcNpcEntity>> standing(ServerLevel hub) {
        Map<String, List<ArcNpcEntity>> out = new LinkedHashMap<>();
        for (ArcNpcEntity n : hub.getEntities(EntityTypeTest.<Entity, ArcNpcEntity>forClass(ArcNpcEntity.class), x -> x.isAlive() && x.getPersistentData().getBoolean(MARK))) {
            out.computeIfAbsent(n.characterId(), k -> new ArrayList<>()).add(n);
        }
        return out;
    }

    /** Is the ground round the post, as far as the character wanders, loaded (so a count of zero means zero)? */
    static boolean loaded(ServerLevel hub, ChronicleSite.Post post) {
        int r = ArcConfig.npcWander() + 2;
        for (int cx = (post.x() - r) >> 4; cx <= (post.x() + r) >> 4; cx++) for (int cz = (post.z() - r) >> 4; cz <= (post.z() + r) >> 4; cz++) if (!hub.hasChunk(cx, cz)) return false;
        return true;
    }

    private static ArcNpcEntity nearest(List<ArcNpcEntity> list, ChronicleSite.Post post) {
        ArcNpcEntity best = list.get(0);
        if (post == null) return best;
        double bd = Double.MAX_VALUE;
        for (ArcNpcEntity n : list) {
            double d = n.distanceToSqr(post.x() + 0.5, post.y(), post.z() + 0.5);
            if (d < bd) {
                bd = d;
                best = n;
            }
        }
        return best;
    }

    /** Removes every NPC this pass placed (the operator's clean-up command). Returns how many. */
    public static int clear(ServerLevel hub) {
        int n = 0;
        for (List<ArcNpcEntity> list : standing(hub).values()) for (ArcNpcEntity npc : list) {
            npc.discard();
            n++;
        }
        return n;
    }
}
