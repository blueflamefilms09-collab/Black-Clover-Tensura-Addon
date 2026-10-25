package com.newuniverse.nusmp.corpse;

import com.newuniverse.nusmp.blackclover.GrimoireCover;
import com.newuniverse.nusmp.blackclover.GrimoireItem;
import com.newuniverse.nusmp.blackclover.GrimoirePages;
import com.newuniverse.nusmp.blackclover.GrimoireSlot;
import com.newuniverse.nusmp.blackclover.Kingdom;
import com.newuniverse.nusmp.blackclover.MagicType;
import com.newuniverse.nusmp.book.BookPage;
import com.newuniverse.nusmp.book.CharacterSpells;
import com.newuniverse.nusmp.book.GrimoireBook;
import com.newuniverse.nusmp.book.GrimoireSummon;
import com.newuniverse.nusmp.grimoire.CanonBook;
import com.newuniverse.nusmp.labyrinth.mega.SelfTests;
import com.newuniverse.nusmp.labyrinth.mega.SelfTests.Result;
import com.newuniverse.nusmp.multiverse.MasteryPages;
import com.newuniverse.nusmp.multiverse.MultiverseConfig;
import com.newuniverse.nusmp.skill.NUSkills;
import io.github.manasmods.manascore.skill.api.ManasSkillInstance;
import io.github.manasmods.manascore.skill.api.SkillAPI;
import io.github.manasmods.tensura.storage.TensuraStorages;
import io.github.manasmods.tensura.util.EnergyHelper;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;

import java.util.List;
import java.util.UUID;

/**
 * Why (owner report 2.1.0, "none of the new Soul Corpse summon spells work", docs/magic/soul_corpse_fix.md): the older self-test {@code magic.rades_undead} summons straight through
 * {@link SoulZombies} and {@link Corpses}, which is why it stayed green while no player could ever choose the pages (they were sealed by mastery, rank and the slot limit). This one goes
 * through the doors a player uses, on a stand-in player, on a dedicated server (CI key {@code magic.soul_corpse_cast}):
 * <ol>
 *   <li>the page table: the book's pages are {@link SoulCorpseAccess#ORDER} in that order, every cost is sane, the generic summons are not character-locked and exactly Rades's six are;</li>
 *   <li>the page unlock: a stand-in that learns the Soul Corpse grimoire gets Reanimation and the Marionette at once ({@code Corpses.openStarterPage}), and with full mastery the real
 *       {@code MasteryPages.tryUnlock} opens Necropolis and Legion without any rank, while Rades's pages stay shut; bound to Rades's grimoire the same loop opens all six, David and Michael
 *       Caesal included (the slot limit used to starve them);</li>
 *   <li>the casts: with the grimoire summoned, the real cast path ({@code GrimoireBook.castFromStyle}: checks, mana, SafeCast, the spirit and fire wrappers, cooldown, mastery) is run for the
 *       Marionette, Reanimation, Necropolis and Legion and a zombie must stand each time: owned by the caster, tagged, a {@link SoulZombieEntity}, never above {@code corpse.zombieCap} and the
 *       server cap; then for Jimmy, David, Alfredo, Carl and Michael Caesal, which must appear for Rades only, within {@link NamedCorpses#MAX_AT_ONCE};</li>
 *   <li>the cleanup: every body, the grimoire, the skills and the mana are put back.</li>
 * </ol>
 * What it does not cover: the look of anything, the client, a real key press and hold (the stand-in has no connection, so the release timing of {@code onRelease} is not run), a second player
 * at the same time, the dimension change (the sweep rule is shared with {@code magic.rades_undead}), and the 40-tick scheduling of the starter and the mastery tick (called directly).
 */
public final class SoulCorpseCastSelfTests {
    private SoulCorpseCastSelfTests() {}

    private static boolean registered;

    public static void register() {
        if (registered) return;
        registered = true;
        SelfTests.register("magic.soul_corpse_cast", SoulCorpseCastSelfTests::run);
    }

    private static Result run(MinecraftServer server) {
        ServerLevel level = server.overworld();
        Vec3 at = Vec3.atCenterOf(level.getSharedSpawnPos()).add(0, 2, 0);
        ServerPlayer p = SelfTests.standIn(level, "scf_soul_corpse_cast", at.x, at.y, at.z);
        UUID id = p.getUUID();
        StringBuilder sb = new StringBuilder();
        try {
            var holder = NUSkills.WIKI_ATTRIBUTE_BOOKS.get(MagicType.SOUL_CORPSE);
            if (holder == null) return Result.fail("there is no Soul Corpse book");
            GrimoireBook book = holder.get();
            String why = table(book);
            if (why != null) return Result.fail(why);
            sb.append("table ").append(book.familyCount()).append(" pages; ");

            // ---- the stand-in learns the grimoire like a player who was handed it
            reset(server, p);
            GrimoireCover cover = Kingdom.CLOVER.coverFor(3);
            ManasSkillInstance fresh = book.createDefaultInstance();
            fresh.getOrCreateTag().putInt("Leaves", cover.tier);
            fresh.getOrCreateTag().putString("Cover", cover.name());
            fresh.getOrCreateTag().putString("Magic", MagicType.SOUL_CORPSE.name());
            SkillAPI.getSkillsFrom(p).learnSkill(fresh, Component.literal("self-test"));
            ManasSkillInstance inst = SkillAPI.getSkillsFrom(p).getSkill(holder.getId()).orElse(null);
            if (inst == null) return Result.fail("the stand-in could not learn the Soul Corpse grimoire");
            ItemStack stack = GrimoireItem.create(p, cover, MagicType.SOUL_CORPSE, null);
            GrimoireSlot.set(p, stack);
            if (GrimoirePages.grimoireOf(p).isEmpty()) return Result.fail("the grimoire of the stand-in is not found by GrimoirePages");
            GrimoireSummon.summon(p, book);
            if (!GrimoireSummon.isFloating(p, MagicType.SOUL_CORPSE)) return Result.fail("the grimoire did not float (summon grimoire)");

            // ---- 2. unlocking, as a player without a character
            Corpses.openStarterPage(p);
            for (String s : SoulCorpseAccess.STARTER) if (!book.usable(inst, p, mode(book, s))) return Result.fail(s + " is not open for a new holder of the grimoire (starter page)");
            sb.append("starters open; ");
            inst.setMastery(inst.getMaxMastery());
            unlockAll(p, inst, book);
            for (String s : SoulCorpseAccess.GENERIC_SUMMONS) if (!book.usable(inst, p, mode(book, s))) return Result.fail(s + " stays sealed at full mastery for a holder of any grimoire");
            if (!CorpseConfig.numberedForAll()) for (String s : SoulCorpseAccess.RADES_ONLY) if (book.usable(inst, p, mode(book, s))) return Result.fail(s + " opened for a caster who is not Rades");
            sb.append("generic open / Rades shut; ");

            // ---- 3. the generic casts through the real pipeline
            int cap = CorpseConfig.zombieCap();
            String note = "";
            int[][] expected = {{mode(book, SoulCorpseAccess.MARIONETTE), 1}, {mode(book, SoulCorpseAccess.NECROPOLIS), Math.min(2, cap)}, {mode(book, SoulCorpseAccess.LEGION), Math.min(4, cap)},
                    {mode(book, SoulCorpseAccess.REANIMATE), CorpseConfig.conjure() ? 1 : 0}};
            for (int[] e : expected) {
                BookPage page = book.page(e[0]);
                Corpses.crumbleAllOf(server, id);
                String problem = cast(book, inst, p, e[0], e[1], server);
                if (problem != null) return Result.fail(page.id() + ": " + problem);
                int made = SoulZombies.count(id);
                for (UUID zid : Corpses.ZOMBIES.bodies(id)) {
                    Entity z = Corpses.find(server, zid);
                    if (!(z instanceof SoulZombieEntity sz) || !sz.isAlive()) return Result.fail(page.id() + ": a listed zombie is not a live soul_zombie");
                    if (!id.equals(sz.getOwnerUUID()) || !id.equals(Corpses.ownerOf(sz)) || !Corpses.isCorpse(sz)) return Result.fail(page.id() + ": a zombie is not owned and tagged");
                    if (sz.burnsInDaylight()) return Result.fail(page.id() + ": a zombie would burn in daylight");
                }
                if (made > cap || Corpses.ZOMBIES.total() > CorpseConfig.zombieGlobalCap()) return Result.fail(page.id() + ": the caps were passed (" + made + "/" + cap + ")");
                sb.append(page.id()).append("=").append(made).append(" ");
            }
            sb.append("; ");
            // casting Legion again and again never passes the caps
            for (int k = 0; k < 3; k++) {
                int m = mode(book, SoulCorpseAccess.LEGION);
                String problem = cast(book, inst, p, m, 1, server);
                if (problem != null) return Result.fail("legion again: " + problem);
                if (SoulZombies.count(id) > cap) return Result.fail("legion again passed the cap: " + SoulZombies.count(id));
            }
            sb.append("cap holds; ");

            // ---- 4. Rades's pages: shut for everyone else, a cast of a sealed page does nothing, open and working with his grimoire
            Corpses.crumbleAllOf(server, id);
            int david = mode(book, SoulCorpseAccess.DAVID);
            book.castFromStyle(inst, p, david);
            if (Corpses.liveNamed(p, NamedCorpses.DAVID) != null) return Result.fail("David answered a caster who is not Rades");
            ItemStack rades = GrimoireSlot.get(p).copy();
            GrimoireItem.setCanon(rades, CanonBook.RADES.id());
            GrimoireSlot.set(p, rades);
            if (CharacterSpells.characterOf(p) != CanonBook.RADES) return Result.fail("the stand-in is not Rades after binding his canon book");
            unlockAll(p, inst, book);
            for (String s : SoulCorpseAccess.RADES_ONLY) if (!book.usable(inst, p, mode(book, s))) return Result.fail(s + " stays sealed at full mastery for Rades (page slots?)");
            sb.append("Rades open; ");
            String[][] named = {{SoulCorpseAccess.JIMMY, "JIMMY"}, {SoulCorpseAccess.DAVID, "DAVID"}, {SoulCorpseAccess.ALFREDO, "ALFREDO"}, {SoulCorpseAccess.CARL, "CARL"},
                    {SoulCorpseAccess.MICHAEL, "MICHAEL_CAESAL"}};
            for (String[] n : named) {
                NamedCorpses kind = NamedCorpses.valueOf(n[1]);
                int m = mode(book, n[0]);
                inst.setCoolDown(0, m);
                refill(p);
                try {
                    book.castFromStyle(inst, p, m);
                } catch (RuntimeException afterwards) {
                    if (Corpses.liveNamed(p, kind) == null) return Result.fail(n[0] + ": the cast threw " + afterwards);
                    note = " (a hook after the cast threw " + afterwards.getClass().getSimpleName() + " for the stand-in)";
                }
                if (Corpses.liveNamed(p, kind) == null) return Result.fail(n[0] + ": the cast did not raise " + kind.title());
                if (Corpses.NAMED.count(id) > NamedCorpses.MAX_AT_ONCE) return Result.fail(n[0] + ": more than " + NamedCorpses.MAX_AT_ONCE + " numbered undead stand");
            }
            if (Corpses.NAMED.total() > CorpseConfig.namedGlobalCap()) return Result.fail("the server-wide cap of numbered undead was passed");
            sb.append("named ").append(Corpses.NAMED.count(id)).append("/").append(NamedCorpses.MAX_AT_ONCE).append(note);

            // ---- 5. (3.7.1, owner report "Rades's Number Summons are just magic circles") each numbered page, cast through the real page path, leaves a real, visible, owned body that stays
            Corpses.crumbleAllOf(server, id);
            String standing = numberedStand(server, level, book, inst, p, sb);
            if (standing != null) return Result.fail(standing);
            String sun = sunRule(server, level, p, sb);
            if (sun != null) return Result.fail(sun);
            return Result.ok(sb.toString());
        } finally {
            reset(server, p);
        }
    }

    // ---------------------------------------------------------------- the numbered undead really stand (3.7.1)
    /** The five numbered pages in the order Rades's list shows them: page id and the body it must leave. */
    private static final Object[][] NUMBERED = {{SoulCorpseAccess.JIMMY, NamedCorpses.JIMMY}, {SoulCorpseAccess.DAVID, NamedCorpses.DAVID}, {SoulCorpseAccess.ALFREDO, NamedCorpses.ALFREDO},
            {SoulCorpseAccess.CARL, NamedCorpses.CARL}, {SoulCorpseAccess.MICHAEL, NamedCorpses.MICHAEL_CAESAL}};

    /** Ticks a body is driven after its cast (two seconds): the entity's own tick forty times and the owner sweep every ten. */
    private static final int STAND_TICKS = 40;

    /**
     * For each of the five numbered pages: one cast through the real page path ({@code castFromStyle}: checks, mana, SafeCast, the wrappers, cooldown, mastery) as the stand-in, then the body must be in
     * the level's entity list under its id, alive, of the right type and row, owned by the caster (field, ledger and the tag), not invisible, named with its title and the name shown, carrying the decay
     * marker, listed in the roster, and still all of that after {@value #STAND_TICKS} ticks of its own tick and four owner sweeps (the sweep gets the stand-in as the online owner: it is not in the
     * player list, which is why a plain {@code Corpses.tick} would dismiss it at once). The elite is cast twice: the second cast must refuse and leave the same body. Null when all hold.
     */
    private static String numberedStand(MinecraftServer server, ServerLevel level, GrimoireBook book, ManasSkillInstance inst, ServerPlayer p, StringBuilder sb) {
        UUID id = p.getUUID();
        java.util.function.Function<UUID, ServerPlayer> online = who -> id.equals(who) ? p : null;
        for (Object[] row : NUMBERED) {
            String pageId = (String) row[0];
            NamedCorpses kind = (NamedCorpses) row[1];
            Corpses.crumbleAllOf(server, id);
            int m = mode(book, pageId);
            inst.setCoolDown(0, m);
            refill(p);
            try {
                book.castFromStyle(inst, p, m);
            } catch (RuntimeException afterwards) {
                if (Corpses.liveNamed(p, kind) == null) return pageId + ": the real page cast threw " + afterwards;
            }
            NamedCorpseEntity n = Corpses.liveNamed(p, kind);
            if (n == null) return pageId + ": the real page cast left no " + kind.title() + " (the server log has a 'numbered undead' line with the reason)";
            UUID uid = n.getUUID();
            String bad = standing(level, n, kind, id);
            if (bad != null) return pageId + ": " + bad + " right after the cast";
            if (kind.elite()) {                                                                        // the elite stands once: a second cast refuses and leaves him as he is
                inst.setCoolDown(0, m);
                refill(p);
                try { book.castFromStyle(inst, p, m); } catch (RuntimeException ignored) { /* a refusal may still run the hooks after it */ }
                NamedCorpseEntity again = Corpses.liveNamed(p, kind);
                if (again == null || !again.getUUID().equals(uid)) return pageId + ": a second cast replaced or removed the elite instead of refusing";
            }
            for (int t = 1; t <= STAND_TICKS; t++) {
                n.tick();
                if (t % 10 == 0) Corpses.sweepAll(server, online);
            }
            bad = standing(level, n, kind, id);
            if (bad != null) return pageId + ": " + bad + " " + STAND_TICKS + " ticks after the cast";
            sb.append(kind.number).append(" stands; ");
        }
        Corpses.crumbleAllOf(server, id);
        return null;
    }

    /** Everything that must be true of a numbered body in the world; null when it is, else the first thing that is not. */
    private static String standing(ServerLevel level, NamedCorpseEntity n, NamedCorpses kind, UUID owner) {
        UUID uid = n.getUUID();
        if (!n.isAlive() || n.isRemoved() || n.getHealth() <= 0f) return kind.title() + " is not alive (health " + n.getHealth() + ", removed " + n.isRemoved() + ")";
        if (level.getEntity(uid) != n) return kind.title() + " is not in the level's entity list";
        if (n.getType() != CorpseEntities.type(kind) || n.kind() != kind) return kind.title() + " has the wrong type or row (" + n.getType() + ", " + n.kind() + ")";
        if (!owner.equals(n.getOwnerUUID()) || !owner.equals(Corpses.ownerOf(n)) || !Corpses.isCorpse(n)) return kind.title() + " is not owned and tagged by the caster";
        if (n.isInvisible()) return kind.title() + " is invisible";
        if (n.getCustomName() == null || !n.isCustomNameVisible() || !kind.title().equals(n.getCustomName().getString())) return kind.title() + " has lost its name tag";
        if (n.getEffect(CorpseEffects.holder()) == null) return kind.title() + " carries no decay marker (the client picks its skin from it)";
        if (!Corpses.NAMED.bodies(owner).contains(uid)) return kind.title() + " is not in the roster";
        if (n.burnsInDaylight() || n.isSunBurnTick()) return kind.title() + " would burn in daylight";
        return null;
    }

    /**
     * The daylight rule (3.7.1, "zombies raised by Soul Corpse Magic do not burn in daylight"): at noon, a Soul Zombie and each numbered undead, driven twenty ticks, never catch fire and answer no to both
     * daylight questions. The time of day is put back. The rule itself is the two overrides ({@code SoulZombieEntity}, {@code NamedCorpseEntity}), which a source scan pins too.
     */
    private static String sunRule(MinecraftServer server, ServerLevel level, ServerPlayer p, StringBuilder sb) {
        long was = level.getDayTime();
        UUID id = p.getUUID();
        try {
            level.setDayTime(6000L);
            Corpses.crumbleAllOf(server, id);
            List<SoulZombieEntity> zs = SoulZombies.summon(p, 1, 400, 1.0, null);
            if (zs.isEmpty()) return "no soul zombie could be summoned for the daylight check";
            SoulZombieEntity z = zs.get(0);
            for (int t = 0; t < 20; t++) z.tick();
            if (z.burnsInDaylight() || z.isSunBurnTick() || z.isOnFire()) return "a soul zombie burns in daylight (burnsInDaylight " + z.burnsInDaylight() + ", on fire " + z.isOnFire() + ")";
            for (NamedCorpses k : NamedCorpses.values()) {
                NamedCorpseEntity n = Corpses.summonNamed(p, k);
                if (n == null) return k.title() + " could not be summoned for the daylight check";
                for (int t = 0; t < 20; t++) n.tick();
                if (n.burnsInDaylight() || n.isSunBurnTick() || n.isOnFire()) return k.title() + " burns in daylight (on fire " + n.isOnFire() + ")";
            }
            sb.append("no sun burn at noon; ");
            return null;
        } finally {
            level.setDayTime(was);
            Corpses.crumbleAllOf(server, id);
        }
    }

    // ---------------------------------------------------------------- the table
    private static String table(GrimoireBook book) {
        List<BookPage> pages = book.pages();
        if (book.familyCount() < SoulCorpseAccess.ORDER.size()) return "the book has " + book.familyCount() + " family pages, expected at least " + SoulCorpseAccess.ORDER.size();
        for (int k = 0; k < SoulCorpseAccess.ORDER.size(); k++)
            if (!pages.get(k).id().equals(SoulCorpseAccess.ORDER.get(k))) return "page " + k + " is " + pages.get(k).id() + ", the table says " + SoulCorpseAccess.ORDER.get(k);
        for (BookPage pg : pages.subList(0, book.familyCount())) {
            if (pg.cast() == null) return pg.id() + " has no handler";
            if (pg.costPercent() <= 0 || pg.costPercent() > 35 || pg.costFloor() <= 0 || pg.cooldown() < 0 || pg.cooldown() > 20 * 60 * 20) return pg.id() + " has an odd cost or cooldown";
            boolean locked = CharacterSpells.owners(book, pg) != null;      // with corpse.numberedForAll (default) Rades's pages are open to every holder, else locked to him
            boolean rades = SoulCorpseAccess.isRadesOnly(pg.id());
            if (locked != (rades && !CorpseConfig.numberedForAll())) return pg.id() + (rades ? " lost its Rades lock" : " is character-locked but should be open to every holder");
            if (rades && locked && !CharacterSpells.owners(book, pg).equals(java.util.EnumSet.of(CanonBook.RADES))) return pg.id() + " is locked to someone besides Rades";
        }
        double scale = MultiverseConfig.get(MultiverseConfig.PAGE_MASTERY_SCALE);
        for (String s : SoulCorpseAccess.MASTERY_CAP.keySet()) {
            int m = mode(book, s);
            double need = MasteryPages.needed(book, book.page(m), m, book.familyCount());
            if (need > Math.min(1.0, SoulCorpseAccess.MASTERY_CAP.get(s) * scale) + 1e-9) return s + " asks " + need + " of the mastery bar, above its cap";
            String key = SoulCorpseAccess.BOOK + ":" + s;
            boolean configured = false;
            for (String line : MultiverseConfig.get(MultiverseConfig.PAGE_GATES)) if (line.trim().toLowerCase(java.util.Locale.ROOT).startsWith(key)) configured = true;
            if (!configured && MasteryPages.gate(book, book.page(m)).minStep() != 0) return s + " still asks a rank";
        }
        return null;
    }

    private static int mode(GrimoireBook book, String pageId) {
        for (int k = 0; k < book.familyCount(); k++) if (book.page(k).id().equals(pageId)) return k;
        throw new IllegalStateException("no page " + pageId);
    }

    // ---------------------------------------------------------------- the pieces
    /** Runs the unlock tick until nothing more opens (each call opens at most one page, like the 2 second tick). */
    private static void unlockAll(ServerPlayer p, ManasSkillInstance inst, GrimoireBook book) {
        for (int k = 0; k < 2 * book.familyCount() && MasteryPages.tryUnlock(p, inst, book) >= 0; k++) { /* one per call */ }
    }

    /** One cast of a page through the real cast path; null when a zombie count of at least 'min' stands afterwards. */
    private static String cast(GrimoireBook book, ManasSkillInstance inst, ServerPlayer p, int mode, int min, MinecraftServer server) {
        inst.setCoolDown(0, mode);
        refill(p);
        int before = SoulZombies.count(p.getUUID());
        try {
            book.castFromStyle(inst, p, mode);
        } catch (RuntimeException e) {
            if (SoulZombies.count(p.getUUID()) < Math.max(min, 1)) return "the cast threw " + e;
        }
        int after = SoulZombies.count(p.getUUID());
        if (after < min) return "only " + after + " zombies stand after the cast (before " + before + ", wanted " + min + "): the cast did nothing";
        return null;
    }

    /** Plenty of magicule, so the cost never decides the test (the cost rule is not what is checked here). */
    private static void refill(ServerPlayer p) {
        EnergyHelper.setMaxMagicule(p, 1.0E9);
        var ex = TensuraStorages.getExistenceFrom(p);
        if (ex != null) { ex.setMagicule(1.0E9); ex.markDirty(); }
    }

    private static void reset(MinecraftServer server, ServerPlayer p) {
        try {
            Corpses.crumbleAllOf(server, p.getUUID());
            GrimoireSummon.dismiss(p, false);
            var skills = SkillAPI.getSkillsFrom(p);
            for (var sid : NUSkills.allGrimoireSkillIds()) skills.forgetSkill(sid);
            GrimoireSlot.set(p, ItemStack.EMPTY);
            p.getInventory().clearContent();
        } catch (RuntimeException ignored) {
            // the cleanup of a stand-in must never hide the result of the test
        }
    }
}
