package com.newuniverse.nusmp.charset;

import com.newuniverse.nusmp.book.AntiMagicBook;
import com.newuniverse.nusmp.book.BookPage;
import com.newuniverse.nusmp.book.CharacterPages;
import com.newuniverse.nusmp.book.GrimoireBook;
import com.newuniverse.nusmp.book.SpiritBond;
import com.newuniverse.nusmp.grimoire.CanonBook;
import com.newuniverse.nusmp.multiverse.SpiritLordSkill;
import io.github.manasmods.manascore.skill.api.ManasSkillInstance;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;

import java.util.EnumSet;
import java.util.Set;

/**
 * Why: the game side of the character spell sets (docs/magic/character_spells.md). {@link CharacterSetRegistry} holds the data and the pure decisions; this class reads the grimoire instance and
 * the player and hands the answers to the places that need them: {@code GrimoireBook.isUnlocked} (is the page hidden), {@code MasteryPages.tryUnlock} (does the character's rule open it now),
 * {@code CharacterSpells} (who a new page belongs to) and {@code GrimoireBook.castPage} (count the cast). Server and client both ask {@link #hidden}; only the server asks {@link #opens}.
 *
 * <p>Cost: {@link #hidden} is two map reads and a tag read; {@link #opens} runs per player every 2 seconds for at most the pages of one book; {@link #onCast} writes one small int per counted page
 * (only the pages some rule names are counted, at most a handful per character, so the grimoire's tag stays small).
 */
public final class CharacterSets {
    private CharacterSets() {}

    public static final String USES_TAG = "CsUses";
    private static final int USES_CAP = 9999;

    private static volatile CharacterSetRegistry registry;

    public static CharacterSetRegistry registry() {
        CharacterSetRegistry r = registry;
        if (r == null) {
            synchronized (CharacterSets.class) {
                if (registry == null) registry = CharacterSetRegistry.loadShipped();
                r = registry;
            }
        }
        return r;
    }

    public static boolean enabled() { return CharacterSetConfig.enabled(); }

    private static String canonOf(ManasSkillInstance i) {
        CompoundTag t = i.getTag();
        return t == null ? "" : t.getString("Canon");
    }

    private static String bookPath(GrimoireBook b) { return b.getRegistryName() == null ? "" : b.getRegistryName().getPath(); }

    /** Is this family page of this grimoire hidden by the character it is bound to (or by the basic book's exclusivity rule)? Cheap; safe on both sides. */
    public static boolean hidden(ManasSkillInstance i, int mode) {
        if (!enabled() || !(i.getSkill() instanceof GrimoireBook b) || mode < 0 || mode >= b.familyCount()) return false;
        BookPage pg = b.page(mode);
        return pg != null && registry().hidden(canonOf(i), bookPath(b), pg.id(), mode == 0, CharacterSetConfig.basicHidesSignatures(), com.newuniverse.nusmp.ultimate.UltimateRegistry.isUltimatePage(pg.id()), com.newuniverse.nusmp.ultimate.UltimateConfig.showInCharacterGrimoires());      // Ultimate Magic: a character grimoire shows the Ultimate of its own attribute(s) (ultimate.showInCharacterGrimoires)
    }

    /** How many family pages this grimoire can ever show (the pages of a character's set, or all). For the status panel's "x of y". */
    public static int visibleCount(ManasSkillInstance i, GrimoireBook b) {
        int n = 0;
        for (int m = 0; m < b.familyCount(); m++) if (!hidden(i, m)) n++;
        return n;
    }

    /** The set's verdict for opening this family page now: null = no set for this character (the mastery order applies), else open or not. Server only. */
    public static Boolean opens(ServerPlayer p, ManasSkillInstance i, GrimoireBook b, int mode) {
        if (!enabled()) return null;
        BookPage pg = b.page(mode);
        if (pg == null) return null;
        return registry().opens(canonOf(i), bookPath(b), pg.id(), new Facts(p, i, b), com.newuniverse.nusmp.ultimate.UltimateRegistry.isUltimatePage(pg.id()), com.newuniverse.nusmp.ultimate.UltimateConfig.showInCharacterGrimoires());
    }

    /** Does this character's set list the page (so the old "locked to another grimoire" line must not block it)? */
    public static boolean listed(String canon, GrimoireBook b, BookPage page) {
        return enabled() && canon != null && !canon.isEmpty() && registry().listed(canon, bookPath(b), page.id());
    }

    /**
     * The character a page that this feature ADDED belongs to (null for every older page, whose owners are the lock lines of CharacterSpells). It applies with the switch off too: a new page never
     * opens for another character's grimoire.
     */
    public static Set<CanonBook> addedPageOwners(GrimoireBook b, BookPage page) {
        String book = bookPath(b);
        if (!CharacterPages.isAdded(book, page.id()) && !com.newuniverse.nusmp.book.CharacterPagesGold.isAdded(book, page.id())) return null;      // builder gold: the pages of the Golden Dawn / Crimson Lion side
        String owner = registry().exclusiveOwner(book, page.id());
        CanonBook c = CanonBook.byId(owner);
        return c == null ? null : EnumSet.of(c);
    }

    /** A successful cast of a page: counts it if some rule of this grimoire's character names it. */
    public static void onCast(ManasSkillInstance i, String pageId) {
        if (!enabled()) return;
        String canon = canonOf(i);
        if (canon.isEmpty() || !registry().tracked(canon).contains(pageId)) return;
        CompoundTag uses = i.getOrCreateTag().getCompound(USES_TAG);
        int n = uses.getInt(pageId);
        if (n >= USES_CAP) return;
        uses.putInt(pageId, n + 1);
        i.getOrCreateTag().put(USES_TAG, uses);
        i.markDirty();
    }

    public static int usesOf(ManasSkillInstance i, String pageId) {
        CompoundTag t = i.getTag();
        return t == null ? 0 : t.getCompound(USES_TAG).getInt(pageId);
    }

    /** What the rules may ask, answered from the grimoire and the player. */
    static final class Facts implements RuleFacts {
        private final ServerPlayer p;
        private final ManasSkillInstance inst;
        private final GrimoireBook book;

        Facts(ServerPlayer p, ManasSkillInstance inst, GrimoireBook book) { this.p = p; this.inst = inst; this.book = book; }

        @Override public double mastery() { return book.masteryFrac(inst); }

        @Override public int uses(String pageId) { return usesOf(inst, pageId); }

        @Override public boolean pageOpen(String pageId) {
            for (int m = 0; m < book.pages().size(); m++) {
                BookPage pg = book.page(m);
                if (pg != null && pg.id().equals(pageId)) return m < book.familyCount() ? GrimoireBook.isUnlocked(inst, m) : book.usable(inst, p, m);
            }
            return false;
        }

        /** Is the player bonded to THIS book's spirit (Sylph for the Wind book ...)? A book without a spirit never counts. */
        private boolean bondedToThisSpirit() {
            String spirit = book.spiritName();
            return spirit != null && (SpiritBond.bonded(p) || SpiritLordSkill.instance(p).isPresent()) && spirit.equalsIgnoreCase(SpiritLordSkill.bondedType(p));
        }

        @Override public int trust() { return bondedToThisSpirit() && SpiritBond.bonded(p) ? SpiritBond.trust(p) : 0; }

        @Override public boolean flag(String name) {
            return switch (name) {
                case "spirit_bond" -> bondedToThisSpirit();
                case "saint_stage" -> trust() >= CharacterSetConfig.saintTrust() && usesOf(inst, "spirit_dive") >= CharacterSetConfig.saintDives();
                case "mana_zone" -> com.newuniverse.nusmp.mana.zone.ManaZoneManager.gateOpenFor(p);
                case "anti_magic_lord" -> AntiMagicBook.isLord(inst);
                default -> false;
            };
        }
    }
}
