package com.newuniverse.nusmp.book;

import com.newuniverse.nusmp.blackclover.MagicType;
import com.newuniverse.nusmp.labyrinth.mega.SelfTests;
import com.newuniverse.nusmp.labyrinth.mega.SelfTests.Result;
import com.newuniverse.nusmp.prop.MagicProps;
import com.newuniverse.nusmp.prop.MagicPropEntity;
import com.newuniverse.nusmp.prop.PropKind;
import com.newuniverse.nusmp.skill.NUSkills;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Why: nothing of the 1.93.0 rebuild of Song, Sound, Soul, Soul Corpse and Memory Magic can be played by its author, so what can be proved without a person is proved by the
 * real game: the CI server smoke test boots the mod and runs these (key prefix {@code magic}). One checks every rebuilt book in the real skill registry (the right class, the
 * five generic page ids in their old order, the exclusive pages of Kahono, Lucius and Rades still at their old places, unique ids, at most 32 family pages); the other spawns
 * every prop kind of the five magics in the real world, ticks it once and ends it, and fails if a spawn is refused, a behaviour throws or a prop stays.
 */
public final class SongRebuildSelfTests {
    private SongRebuildSelfTests() {}

    private static boolean registered;

    public static void register() {
        if (registered) return;
        registered = true;
        SelfTests.register("magic.song_rebuild_books", SongRebuildSelfTests::books);
        SelfTests.register("magic.song_rebuild_props", SongRebuildSelfTests::props);
    }

    private static final String[] GENERIC = {"attribute_bolt", "attribute_cut", "attribute_field", "attribute_domain", "attribute_guard"};

    private static Result books(MinecraftServer server) {
        StringBuilder sb = new StringBuilder();
        for (MagicType m : new MagicType[]{MagicType.SONG, MagicType.SOUND, MagicType.SOUL, MagicType.SOUL_CORPSE, MagicType.MEMORY}) {
            var holder = NUSkills.WIKI_ATTRIBUTE_BOOKS.get(m);
            if (holder == null) return Result.fail(m + ": no wiki attribute book is registered");
            GrimoireBook b = holder.get();
            Class<?> want = switch (m) {
                case SONG -> SongBook.class;
                case SOUND -> SoundBook.class;
                case SOUL -> SoulBook.class;
                case SOUL_CORPSE -> SoulCorpseBook.class;
                default -> MemoryBook.class;
            };
            if (b.getClass() != want) return Result.fail(m + ": the registered book is " + b.getClass().getSimpleName() + ", not " + want.getSimpleName());
            List<BookPage> pages = b.pages();
            for (int k = 0; k < GENERIC.length; k++)
                if (!pages.get(k).id().equals(GENERIC[k])) return Result.fail(m + ": page " + k + " is " + pages.get(k).id() + ", expected " + GENERIC[k]);
            Set<String> ids = new HashSet<>();
            for (BookPage p : pages) if (!ids.add(p.id())) return Result.fail(m + ": the page id " + p.id() + " is used twice");
            if (b.familyCount() > 32) return Result.fail(m + ": " + b.familyCount() + " family pages (the budget is 32)");
            List<String> old = switch (m) {
                case SONG -> List.of("song_mode_heal", "song_mode_lullaby", "song_mode_barrier", "song_mode_requiem", "song_crescendo");
                case SOUL -> List.of("paladin_conversion", "paladin_recall");
                case SOUL_CORPSE -> List.of("corpse_reanimate", "corpse_jimmy", "corpse_alfredo", "corpse_carl", "true_soul_corpse");
                default -> List.<String>of();
            };
            for (int k = 0; k < old.size(); k++)
                if (!pages.get(GENERIC.length + k).id().equals(old.get(k))) return Result.fail(m + ": the exclusive page " + old.get(k) + " moved from index " + (GENERIC.length + k));
            sb.append(m.name().toLowerCase()).append(' ').append(b.familyCount()).append(" pages; ");
        }
        return Result.ok(sb.toString().trim());
    }

    private static Result props(MinecraftServer server) {
        ServerLevel level = server.overworld();
        Vec3 at = Vec3.atCenterOf(level.getSharedSpawnPos()).add(0, 3, 0);
        List<MagicPropEntity> made = new ArrayList<>();
        int[][] params = {{0}, {1}};
        for (PropKind k : new PropKind[]{PropKind.SONG_1, PropKind.SONG_2, PropKind.SOUND_1, PropKind.SOUND_2, PropKind.SOUL_1, PropKind.SOUL_2, PropKind.SOUL_CORPSE_1,
                PropKind.SOUL_CORPSE_2, PropKind.MEMORY_1, PropKind.MEMORY_2}) {
            for (int[] p : params) {
                if ((k == PropKind.SONG_2 || k == PropKind.SOUND_2 || k == PropKind.SOUL_1 || k == PropKind.SOUL_2 || k == PropKind.SOUL_CORPSE_2 || k == PropKind.MEMORY_1 || k == PropKind.MEMORY_2) && p[0] == 1) continue;
                MagicPropEntity e = MagicProps.spawn(level, k, at, 0f, 1f, 40, p[0], null);
                if (e == null) return Result.fail(k + " param " + p[0] + ": the spawn was refused");
                made.add(e);
            }
        }
        for (MagicPropEntity e : made) e.tick();                       // an ownerless prop ends itself at its first tick
        int stuck = 0;
        for (MagicPropEntity e : made) { if (!e.isRemoved()) { e.expire(); if (!e.isRemoved()) stuck++; } }
        if (stuck > 0) return Result.fail(stuck + " of " + made.size() + " props did not end");
        return Result.ok(made.size() + " props of the five magics spawned, ticked once and ended");
    }
}
