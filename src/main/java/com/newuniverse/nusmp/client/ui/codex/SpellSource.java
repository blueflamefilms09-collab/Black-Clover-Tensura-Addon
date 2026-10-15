package com.newuniverse.nusmp.client.ui.codex;

import com.newuniverse.nusmp.book.BookPage;
import com.newuniverse.nusmp.book.GrimoireBook;
import com.newuniverse.nusmp.client.multiverse.MultiverseStatusClient;
import com.newuniverse.nusmp.multiverse.MasteryPages;
import io.github.manasmods.manascore.skill.api.ManasSkillInstance;
import io.github.manasmods.manascore.skill.api.SkillAPI;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.entity.player.Player;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Why: reads what the player has actually learned and hands it to the pure {@link SpellModel}. No new packet: a learned grimoire is a ManasCore
 * skill whose instance (with its "Unlocked" page mask) the client already holds, because Tensura's own skill list draws the mode names from it.
 * The server's status summary ({@code Grimoire.PageList}, built by {@code MultiverseSync} for the Four Kingdoms menu) is the fallback when the
 * skills are not readable yet, so the tab never shows less than that menu does. Pages still sealed are only counted, never named (the discovery
 * rule). The description of a page is its written lang line {@code nusmp.page.<book>.<page>} when there is one, else a generated line of its cost
 * and cooldown.
 */
public final class SpellSource {
    private SpellSource() {}

    private record Read(List<SpellModel.Spell> spells, Map<String, Integer> sealed) {}

    /** The page's blocks: one per magic. */
    public static List<CodexBlock> blocks() {
        Read r = read();
        List<SpellModel.Group> groups = SpellModel.group(r.spells(), r.sealed());
        List<CodexBlock> out = new ArrayList<>();
        for (SpellModel.Group g : groups) {
            List<CodexRow> rows = new ArrayList<>();
            for (SpellModel.Spell s : g.spells()) {
                String text = s.descKey() != null && I18n.exists(s.descKey()) ? I18n.get(s.descKey()) : SpellModel.generatedLine(s);
                rows.add(new CodexRow(s.name(), text, "", null, SpellModel.statsLine(s)));
            }
            if (g.sealed() > 0)
                rows.add(new CodexRow("???", g.sealed() + (g.sealed() == 1 ? " page is" : " pages are") + " still sealed. Train this magic and mastery will open "
                        + (g.sealed() == 1 ? "it." : "them."), "", null, ""));
            out.add(new CodexBlock(g.magicName(), rows));
        }
        if (out.isEmpty())
            out.add(CodexBlock.single(CodexRow.of("No spells yet", "You have not learned any grimoire spells. Take a grimoire at a Grimoire Tower; its pages open through mastery, and each one is listed here once it does.")));
        return out;
    }

    private static Read read() {
        List<SpellModel.Spell> spells = new ArrayList<>();
        Map<String, Integer> sealed = new HashMap<>();
        Player p = Minecraft.getInstance().player;
        try {
            if (p != null) {
                var skills = SkillAPI.getSkillsFrom(p);
                if (skills != null) {
                    for (ManasSkillInstance inst : new ArrayList<>(skills.getLearnedSkills())) {
                        if (inst.getSkill() instanceof GrimoireBook book) fromBook(inst, book, spells, sealed);
                    }
                }
            }
        } catch (RuntimeException | LinkageError ignored) {
            // fall through to the status summary
        }
        if (spells.isEmpty()) fromStatus(spells, sealed);
        return new Read(spells, sealed);
    }

    private static void fromBook(ManasSkillInstance inst, GrimoireBook book, List<SpellModel.Spell> spells, Map<String, Integer> sealed) {
        String magicId = book.magicId(), label = book.magicLabel(), bookPath = book.getRegistryName().getPath();
        int n = book.familyCount();
        for (int m = 0; m < n; m++) {
            BookPage page = book.page(m);
            if (page == null) continue;
            if (!GrimoireBook.isUnlocked(inst, m)) { sealed.merge(magicId, 1, Integer::sum); continue; }
            int mastery = m == 0 ? 0 : (int) Math.round(MasteryPages.masteryCost(page, m, n) * 100.0);
            spells.add(new SpellModel.Spell(magicId, label, page.id(), page.name(), m, (int) Math.round(page.costPercent()), page.cooldown() / 20, mastery,
                    "nusmp.page." + bookPath + "." + page.id()));
        }
    }

    private static void fromStatus(List<SpellModel.Spell> spells, Map<String, Integer> sealed) {
        CompoundTag g = MultiverseStatusClient.status().getCompound("Grimoire");
        ListTag list = g.getList("PageList", Tag.TAG_COMPOUND);
        if (list.isEmpty()) return;
        String magicId = g.getString("MagicId").isEmpty() ? g.getString("Magic") : g.getString("MagicId");
        String label = g.getString("Magic");
        for (int i = 0; i < list.size(); i++) {
            CompoundTag e = list.getCompound(i);
            spells.add(new SpellModel.Spell(magicId, label, e.getString("Id"), e.getString("Name"), e.getInt("Mode"), e.getInt("Cost"), e.getInt("Cooldown"), -1, e.getString("Key")));
        }
        int closed = g.getInt("Pages") - g.getInt("Unlocked");
        if (closed > 0) sealed.put(magicId, closed);
    }
}
