package com.newuniverse.nusmp.entity.veldanava.skills;

import com.newuniverse.nusmp.book.BookPage;
import com.newuniverse.nusmp.book.GrimoireBook;
import com.newuniverse.nusmp.core.magic.profile.SafeCast;
import io.github.manasmods.manascore.skill.api.ManasSkill;
import io.github.manasmods.manascore.skill.api.ManasSkillInstance;
import io.github.manasmods.manascore.skill.api.SkillAPI;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayDeque;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;

/**
 * Why: Grand Spell Copy and Spell Chain need to know which grimoire spells were cast lately. This is the one place that remembers them: a log of the last {@link SolomonRules#LOG_SIZE} grimoire casts of the
 * whole server (who, which book, which page, where, when) and the last {@link SolomonRules#OWN_LOG} casts of each caster. {@code GrimoireBook.castPage} adds one line to it (one small record, two deque
 * operations, nothing else) after every finished cast. A spell is kept as {@code "<book id>|<page>"}: the page of the book is the spell, so a replay needs no stored object.
 *
 * <p>A replay is the page's own {@code Cast} run for the replaying player inside {@link SafeCast} (an exception in one spell never breaks the tick), with the player's own instance of the book when the player
 * has learned it and a fresh default instance when not; the cost, the cooldown and the chant are not paid (the replay is the free cast). A replay is never logged (the {@link #replaying} flag), so a chain cannot
 * feed itself, and a page that needs a held weapon is not replayed. The copy a player holds is stored in the player's own data and survives a restart; everything else is in memory, bounded, and dropped at logout.
 * Server thread only.
 */
public final class SolomonCasts {
    private SolomonCasts() {}

    public static final String COPY_TAG = "nusmp_solomon_copy";

    /** One logged cast. */
    public record Entry(UUID caster, String spell, ResourceKey<Level> dimension, Vec3 at, long time) {}

    private static final ArrayDeque<Entry> LOG = new ArrayDeque<>();
    private static final Map<UUID, ArrayDeque<String>> OWN = new HashMap<>();
    private static boolean replaying;

    public static boolean replaying() { return replaying; }

    public static int logSize() { return LOG.size(); }

    public static int ownersTracked() { return OWN.size(); }

    public static String spell(String bookId, int mode) { return bookId + "|" + mode; }

    /** Splits {@code "<book id>|<page>"}; null for text that is not one. */
    public static Object[] split(String spell) {
        if (spell == null) return null;
        int bar = spell.lastIndexOf('|');
        if (bar <= 0 || bar == spell.length() - 1) return null;
        try {
            return new Object[]{spell.substring(0, bar), Integer.parseInt(spell.substring(bar + 1))};
        } catch (NumberFormatException bad) {
            return null;
        }
    }

    // ---------------------------------------------------------------- the log
    /** A grimoire spell was cast (called once from {@code GrimoireBook.castPage}). A replay is not logged. */
    public static void record(ServerPlayer caster, GrimoireBook book, int mode) {
        if (replaying || caster.level().isClientSide) return;
        ResourceLocation id = book.getRegistryName();
        if (id == null) return;
        add(caster.getUUID(), spell(id.toString(), mode), caster.level().dimension(), caster.position(), caster.level().getGameTime());
    }

    /** The log operation without any game object (a unit test, the self-test). */
    static void add(UUID caster, String spell, ResourceKey<Level> dimension, Vec3 at, long time) {
        LOG.addLast(new Entry(caster, spell, dimension, at, time));
        while (LOG.size() > SolomonRules.LOG_SIZE) LOG.removeFirst();
        ArrayDeque<String> own = OWN.computeIfAbsent(caster, k -> new ArrayDeque<>());
        own.addLast(spell);
        while (own.size() > SolomonRules.OWN_LOG) own.removeFirst();
    }

    /** The newest logged cast that somebody else made, in this dimension, within the copy range and the copy window of the player; null when there is none. */
    public static Entry findToCopy(ServerPlayer p) {
        long now = p.level().getGameTime();
        double r2 = SolomonRules.COPY_RANGE * SolomonRules.COPY_RANGE;
        Iterator<Entry> it = LOG.descendingIterator();
        while (it.hasNext()) {
            Entry e = it.next();
            if (now - e.time() > SolomonRules.COPY_WINDOW_TICKS) return null;
            if (e.caster().equals(p.getUUID()) || !e.dimension().equals(p.level().dimension()) || e.at().distanceToSqr(p.position()) > r2) continue;
            if (replayable(e.spell())) return e;
        }
        return null;
    }

    /** The last spells of a caster, oldest first (a copy: the chain fires them in the order they were cast). */
    public static java.util.List<String> lastOf(UUID caster) {
        ArrayDeque<String> own = OWN.get(caster);
        return own == null ? java.util.List.of() : java.util.List.copyOf(own);
    }

    // ---------------------------------------------------------------- the stored copy
    public static boolean hasCopy(ServerPlayer p) { return p.getPersistentData().contains(COPY_TAG, 8) && !p.getPersistentData().getString(COPY_TAG).isEmpty(); }

    public static String copy(ServerPlayer p) { return p.getPersistentData().getString(COPY_TAG); }

    public static void storeCopy(ServerPlayer p, String spell) { p.getPersistentData().putString(COPY_TAG, spell); }

    public static void dropCopy(ServerPlayer p) { p.getPersistentData().remove(COPY_TAG); }

    // ---------------------------------------------------------------- replay
    /** The book and page of a stored spell, or null when the book is gone or the page does not exist. */
    private static GrimoireBook bookOf(String spell) {
        Object[] parts = split(spell);
        if (parts == null) return null;
        ResourceLocation rl = ResourceLocation.tryParse((String) parts[0]);
        if (rl == null) return null;
        ManasSkill skill = SkillAPI.getSkillRegistry().get(rl);
        return skill instanceof GrimoireBook b ? b : null;
    }

    /** True when a stored spell can be replayed: the book exists, the page exists, and the page does not need a held weapon. */
    public static boolean replayable(String spell) {
        GrimoireBook book = bookOf(spell);
        Object[] parts = split(spell);
        if (book == null || parts == null) return false;
        BookPage page = book.page((Integer) parts[1]);
        return page != null && page.weaponTag() == null;
    }

    /** The name of a stored spell as the player would read it ("" when unknown). */
    public static String nameOf(String spell) {
        GrimoireBook book = bookOf(spell);
        Object[] parts = split(spell);
        if (book == null || parts == null) return "";
        BookPage page = book.page((Integer) parts[1]);
        return page == null ? "" : page.name();
    }

    /**
     * Casts a stored spell for {@code p} without its cost, chant or cooldown. Returns true when the page's cast reported that it did something. A fault inside the spell is caught by {@link SafeCast}
     * (the cast then counts as spent, as every grimoire cast does).
     */
    public static boolean replay(ServerPlayer p, String spell) {
        GrimoireBook book = bookOf(spell);
        Object[] parts = split(spell);
        if (!JudgmentConfig.solomonReplay() || book == null || parts == null || !replayable(spell)) return false;
        int mode = (Integer) parts[1];
        BookPage page = book.page(mode);
        ManasSkillInstance inst = SkillAPI.getSkillsFrom(p).getSkill(book.getRegistryName()).orElseGet(book::createDefaultInstance);
        boolean before = replaying;
        replaying = true;
        try {
            return SafeCast.run(book.magicId(), p.level().getGameTime(), p, () -> page.cast().cast(book, inst, p, mode));
        } finally {
            replaying = before;
        }
    }

    // ---------------------------------------------------------------- cleaning
    /** Death, logout, forgetting: the caster's own list goes (the server log keeps the lines until they are old). */
    public static void leave(UUID player) { OWN.remove(player); }

    static void clearAll() {
        LOG.clear();
        OWN.clear();
        replaying = false;
    }

    /** Test helper: the tag of a copy as written. */
    static CompoundTag tagOf(ServerPlayer p) { return p.getPersistentData(); }
}
