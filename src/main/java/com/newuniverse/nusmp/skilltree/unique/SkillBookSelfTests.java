package com.newuniverse.nusmp.skilltree.unique;

import com.newuniverse.nusmp.labyrinth.mega.SelfTests;
import com.newuniverse.nusmp.skilltree.data.RouteRegistry;
import com.newuniverse.nusmp.skilltree.data.SkillNode;
import com.newuniverse.nusmp.skilltree.data.SkillRoute;
import com.newuniverse.nusmp.skilltree.item.BookUse;
import com.newuniverse.nusmp.skilltree.item.SkillBookItem;
import com.newuniverse.nusmp.skilltree.line.LinePassives;
import io.github.manasmods.manascore.skill.api.ManasSkill;
import io.github.manasmods.manascore.skill.api.ManasSkillInstance;
import io.github.manasmods.manascore.skill.api.SkillAPI;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.common.util.FakePlayer;

import java.util.ArrayList;
import java.util.List;

/**
 * Why: the unique skills are wiring (a registry that must hold one skill per book and the inert stubs of the removed magics, a data file per book whose arts must be the skill's modes, an item that must teach,
 * refuse and keep itself for a creative reader) and wiring is what a unit test cannot see. {@code skillbook.unique}, run by the CI server smoke test with a stand-in player, drives the real things. (1) Registry:
 * every book of the shipped table has a registered {@link LineUniqueSkill}, and every removed id still has its {@link LegacySkill}. (2) Data: every book has a loaded route whose book id and arts (names and order)
 * are the shipped table's, so the client and the server agree on the modes. (3) The book on a stand-in player: reading teaches the skill switched ON and uses the book up; the passives of that skill count while
 * it is on and stop when it is switched off; a second copy is refused and kept; a creative reader keeps the book; an old book of a removed magic is swapped for a book of another line. The player's skills and
 * inventory are restored. Where the world lacks something (no routes loaded, a stand-in with no skill list) the answer says so and checks what it can.
 */
public final class SkillBookSelfTests {
    private SkillBookSelfTests() {}

    public static final String KEY = "skillbook.unique";

    public static void register() { SelfTests.register(KEY, SkillBookSelfTests::run); }

    private static SelfTests.Result run(MinecraftServer server) {
        List<String> bad = new ArrayList<>();
        List<LineIndex.Line> lines = UniqueSkills.lines();
        if (lines.isEmpty()) return SelfTests.Result.fail("the shipped table of books is empty: " + LineIndex.problem());
        StringBuilder said = new StringBuilder();

        // 1. the registry
        for (LineIndex.Line l : lines) {
            ManasSkill s = SkillAPI.getSkillRegistry().get(UniqueSkills.skillId(l.bare()));
            if (!(s instanceof LineUniqueSkill)) bad.add(l.bare() + ": no unique skill is registered under " + UniqueSkills.skillId(l.bare()));
        }
        int legacy = 0;
        for (String path : LegacyIds.REMOVED_TECHNIQUES) legacy += isLegacy(path) ? 1 : 0;
        for (String bare : LegacyIds.REMOVED_LINES) legacy += isLegacy(LineIndex.skillPath(bare)) ? 1 : 0;
        int wantLegacy = LegacyIds.REMOVED_TECHNIQUES.size() + LegacyIds.REMOVED_LINES.size();
        if (legacy != wantLegacy) bad.add("only " + legacy + " of " + wantLegacy + " removed skill ids still have their inert stub");
        said.append(lines.size()).append(" books have a skill, ").append(legacy).append(" inert stubs; ");

        // 2. the data
        int checked = 0;
        if (RouteRegistry.all().isEmpty()) {
            said.append("no route data loaded (data checks skipped); ");
        } else {
            for (LineIndex.Line l : lines) {
                SkillRoute route = RouteRegistry.byId(l.routeId());
                if (route == null) {
                    bad.add(l.bare() + ": no route data is loaded for it");
                    continue;
                }
                checked++;
                if (!l.book().equals(route.book())) bad.add(l.bare() + ": the table says book " + l.book() + ", the data says " + route.book());
                List<String> arts = new ArrayList<>();
                for (SkillNode n : UniqueBooks.arts(route)) arts.add(n.name());
                if (!arts.equals(l.arts())) bad.add(l.bare() + ": the modes " + l.arts() + " differ from the data's arts " + arts);
            }
            said.append(checked).append(" routes agree with the table; ");
        }
        if (!bad.isEmpty()) return fail(bad);

        // 3. the book on a stand-in
        ServerLevel level = server.overworld();
        FakePlayer fp = SelfTests.standIn(level, "skillbook_tester", 0.5, 100, 0.5);
        if (SkillAPI.getSkillsFrom(fp) == null || RouteRegistry.all().isEmpty()) {
            said.append("the book reading was not driven (the stand-in has no skill list, or no data is loaded)");
            return SelfTests.Result.ok(said.toString());
        }
        LineIndex.Line first = null, second = null;
        for (LineIndex.Line l : lines) {
            SkillRoute r = RouteRegistry.byId(l.routeId());
            if (r == null || !r.isLine() || LinePassives.toggleNodesOf(r).isEmpty()) continue;
            if (first == null) first = l;
            else if (second == null) {
                second = l;
                break;
            }
        }
        if (first == null || second == null) {
            said.append("the book reading was not driven (fewer than two lines with passives)");
            return SelfTests.Result.ok(said.toString());
        }
        boolean creative = fp.getAbilities().instabuild;
        try {
            fp.getAbilities().instabuild = false;
            UniqueBooks.forget(fp, first.bare());
            UniqueBooks.forget(fp, second.bare());
            ItemStack book = SkillBookItem.stackFor(first);
            book.setCount(2);
            if (BookUse.readSkillBook(fp, book) != BookUse.Result.LEARNED) bad.add("reading " + first.bare() + " did not teach it");
            if (!UniqueBooks.owns(fp, first.bare())) bad.add("after reading, the player does not own " + first.bare());
            if (book.getCount() != 1) bad.add("reading used up " + (2 - book.getCount()) + " books, not one");
            ManasSkillInstance inst = UniqueBooks.held(fp, first.bare());
            if (inst == null || !inst.isToggled()) bad.add("the new skill is not switched on");
            if (inst != null) {
                if (LinePassives.active(fp).isEmpty()) bad.add("the skill is on but no passive counts");
                inst.setToggled(false);
                if (!LinePassives.active(fp).isEmpty()) bad.add("the skill is off but a passive still counts");
                inst.setToggled(true);
            }
            fp.getCooldowns().removeCooldown(book.getItem());
            if (BookUse.readSkillBook(fp, book) != BookUse.Result.ALREADY_OWNED) bad.add("a second copy of an owned skill was not refused");
            if (book.getCount() != 1) bad.add("a refused book was used up");
            fp.getAbilities().instabuild = true;
            ItemStack other = SkillBookItem.stackFor(second);
            fp.getCooldowns().removeCooldown(other.getItem());
            if (BookUse.readSkillBook(fp, other) != BookUse.Result.LEARNED) bad.add("a creative reader could not learn " + second.bare());
            if (other.getCount() != 1) bad.add("a creative reader lost the book");
            fp.getAbilities().instabuild = false;
            ItemStack old = SkillBookItem.stack(LegacyIds.bookId(LegacyIds.REMOVED_LINES.get(0)));
            fp.getCooldowns().removeCooldown(old.getItem());
            if (BookUse.readSkillBook(fp, old) != BookUse.Result.REPLACED) bad.add("an old book of a removed magic was not swapped");
            boolean gotBook = false;
            for (ItemStack s : fp.getInventory().items) if (s.getItem() instanceof SkillBookItem && !s.isEmpty()) gotBook = true;
            if (!gotBook) bad.add("the swapped book is not in the inventory");
            said.append("read/refuse/creative/swap ok on ").append(first.bare()).append(" and ").append(second.bare());
        } finally {
            fp.getAbilities().instabuild = creative;
            UniqueBooks.forget(fp, first.bare());
            UniqueBooks.forget(fp, second.bare());
            fp.getInventory().clearContent();
        }
        return bad.isEmpty() ? SelfTests.Result.ok(said.toString()) : fail(bad);
    }

    private static boolean isLegacy(String path) {
        try {
            return SkillAPI.getSkillRegistry().get(ResourceLocation.fromNamespaceAndPath("nusmp", path)) instanceof LegacySkill;
        } catch (RuntimeException e) {
            return false;
        }
    }

    private static SelfTests.Result fail(List<String> bad) {
        return SelfTests.Result.fail(bad.size() + " problem(s): " + String.join(" | ", bad.subList(0, Math.min(6, bad.size()))));
    }
}
