package com.newuniverse.nusmp.song;

import com.newuniverse.nusmp.blackclover.MagicType;
import com.newuniverse.nusmp.book.GrimoireBook;
import com.newuniverse.nusmp.skill.NUSkills;
import io.github.manasmods.manascore.skill.api.ManasSkillInstance;
import io.github.manasmods.manascore.skill.api.SkillAPI;
import net.minecraft.server.level.ServerPlayer;

import java.util.Optional;

/**
 * Why: Song Magic is the generic wiki-attribute book for {@link MagicType#SONG}; the passive song, the pages and the HUD all need to
 * find that one skill instance on a player and read or write the chosen mode on its tag, and they should do it the same way.
 */
public final class SongSkill {
    /** Tag key on the skill instance: the chosen {@link SongMode#id}. */
    public static final String TAG_MODE = "SongMode";
    /** Persistent data of the player: a cached copy of the mode so the per-tick code does not search the skill list. */
    public static final String CACHE_MODE = "nusmp_song_mode";

    private SongSkill() {}

    public static Optional<ManasSkillInstance> of(ServerPlayer p) {
        var holder = NUSkills.WIKI_ATTRIBUTE_BOOKS.get(MagicType.SONG);
        if (holder == null) return Optional.empty();
        return SkillAPI.getSkillsFrom(p).getSkill(holder.getId());
    }

    public static SongMode mode(ManasSkillInstance i) { return SongMode.byId(i.getOrCreateTag().getInt(TAG_MODE)); }

    public static void setMode(ServerPlayer p, ManasSkillInstance i, SongMode m) {
        i.getOrCreateTag().putInt(TAG_MODE, m.id);
        i.markDirty();
        p.getPersistentData().putInt(CACHE_MODE, m.id);
    }

    /** The mode from the player's cache (0 when none). */
    public static SongMode cachedMode(ServerPlayer p) { return SongMode.byId(p.getPersistentData().getInt(CACHE_MODE)); }

    /** The grimoire behind an instance, for its damage helper. */
    public static GrimoireBook book(ManasSkillInstance i) { return i.getSkill() instanceof GrimoireBook b ? b : null; }
}
