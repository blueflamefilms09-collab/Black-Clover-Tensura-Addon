package com.newuniverse.nusmp.book;

import com.newuniverse.nusmp.blackclover.MagicType;
import com.newuniverse.nusmp.book.NoelleFormRules.Form;
import com.newuniverse.nusmp.grimoire.CanonBook;
import com.newuniverse.nusmp.labyrinth.mega.SelfTests;
import com.newuniverse.nusmp.labyrinth.mega.SelfTests.Result;
import com.newuniverse.nusmp.skill.NUSkills;
import io.github.manasmods.manascore.skill.api.ManasSkillInstance;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * Why: nothing about Noelle's forms can be looked at by its author, so the real game proves what can be proved (the CI server smoke test runs it; key {@code magic.noelle_forms}):
 * the Water book really holds the three appended pages after the old ones (in order, with a cast), the armour policy answers ARMOUR for them with the styles 2, 3 and 4 and raises the style by the
 * character bit for the character tier only, the two dragon pages are locked to Noelle's grimoire and the Mermaid Form is not, every model, texture and glow map the painter lists is in the
 * mod's resources, the level-of-detail and quad budget rules hold, and with a stand-in player: the Mermaid page puts the form on (style 2) and a second cast takes it off, a wearer of the Sea Dragon
 * God style is seen as that form, and the Black Dragon exhaustion refuses a new Black Dragon Form until it is cleared. Nothing is cast at anyone. Registered from {@code NUSMP}.
 */
public final class NoelleFormsSelfTests {
    private NoelleFormsSelfTests() {}

    private static boolean registered;

    public static void register() {
        if (registered) return;
        registered = true;
        SelfTests.register("magic.noelle_forms", NoelleFormsSelfTests::check);
    }

    private static boolean has(String path) { return NoelleFormsSelfTests.class.getResource("/assets/nusmp/" + path) != null; }

    private static Result check(MinecraftServer server) {
        List<String> bad = new ArrayList<>();
        GrimoireBook water = NUSkills.bookFor(MagicType.WATER);
        if (water == null) return Result.fail("no registered Water book");

        // ---- the pages: appended after the old ones, with a cast; the locks
        List<String> ids = new ArrayList<>();
        for (BookPage p : water.pages()) ids.add(p.id());
        int at = ids.indexOf("torrent");
        String[] added = {NoelleFormRules.PAGE_MERMAID, NoelleFormRules.PAGE_DRAGON, NoelleFormRules.PAGE_BLACK};
        for (int i = 0; i < added.length; i++) {
            if (at < 0 || at + 1 + i >= ids.size() || !added[i].equals(ids.get(at + 1 + i))) bad.add("page " + added[i] + " is not appended after torrent");
            else if (water.pages().get(at + 1 + i).cast() == null) bad.add("page " + added[i] + " has no cast");
        }
        if (ids.indexOf("valkyrie_dress") != 5) bad.add("the Valkyrie Dress page moved");
        BookPage dragon = at >= 0 ? water.page(at + 2) : null, black = at >= 0 ? water.page(at + 3) : null, mermaid = at >= 0 ? water.page(at + 1) : null;
        if (dragon == null || CharacterSpells.owners(water, dragon) == null || !CharacterSpells.owners(water, dragon).equals(Set.of(CanonBook.NOELLE))) bad.add("the Sea Dragon God page is not locked to Noelle");
        if (black == null || CharacterSpells.owners(water, black) == null || !CharacterSpells.owners(water, black).equals(Set.of(CanonBook.NOELLE))) bad.add("the Black Dragon page is not locked to Noelle");
        if (mermaid == null || CharacterSpells.owners(water, mermaid) != null) bad.add("the Mermaid page is locked");

        // ---- the policy: the pages put the armour on with the right styles; the character bit only for the character tier
        ArmourPolicy pol = ArmourPolicy.get();
        for (int i = 0; i < added.length; i++)
            if (pol.forCast("WATER", added[i]) != ArmourPolicy.Instruction.ARMOUR) bad.add(added[i] + " is not an armour page in the policy");
        for (Form f : new Form[]{Form.VALKYRIE, Form.MERMAID, Form.SEA_DRAGON, Form.BLACK_DRAGON}) {
            String page = f == Form.VALKYRIE ? "valkyrie_dress" : f == Form.MERMAID ? added[0] : f == Form.SEA_DRAGON ? added[1] : added[2];
            int plain = pol.styleFor("WATER", page, false, false, false), tier = pol.styleFor("WATER", page, false, false, true);
            if (plain != NoelleFormRules.style(f, false)) bad.add(page + ": style " + plain + " for a plain mage, expected " + NoelleFormRules.style(f, false));
            if (tier != NoelleFormRules.style(f, true)) bad.add(page + ": style " + tier + " for the character tier, expected " + NoelleFormRules.style(f, true));
        }
        if (pol.styleFor("WATER", "scale_mail", false, false, true) != 0) bad.add("Sea Dragon's Scale Mail was raised to the character tier");

        // ---- the assets the painter draws
        int models = 0;
        for (Form f : new Form[]{Form.VALKYRIE, Form.MERMAID, Form.SEA_DRAGON, Form.BLACK_DRAGON}) {
            List<String> all = new ArrayList<>(List.of(NoelleFormRules.standardPieces(f)));
            all.addAll(List.of(NoelleFormRules.characterPieces(f)));
            for (String n : all) {
                String id = NoelleFormRules.key(f) + "_arm_" + n;
                models++;
                if (!has("geo/entity/" + id + ".geo.json")) bad.add("missing model " + id);
                if (!has("textures/entity/" + id + ".png")) bad.add("missing texture " + id);
                if (!has("textures/entity/" + id + "_glow.png")) bad.add("missing glow map " + id);
            }
        }
        for (String s : NoelleFormRules.SPRITES) if (!has("textures/aura/" + s + ".png")) bad.add("missing sprite " + s);

        // ---- the budgets
        for (Form f : new Form[]{Form.VALKYRIE, Form.MERMAID, Form.SEA_DRAGON, Form.BLACK_DRAGON})
            for (NoelleFormRules.Lod l : NoelleFormRules.Lod.values())
                if (NoelleFormRules.quads(NoelleFormRules.recipe(f, l)) > NoelleFormRules.QUAD_CAP) bad.add(f + " at " + l + " draws more than " + NoelleFormRules.QUAD_CAP + " sprite quads");
        if (NoelleFormRules.lod(100.0 * 100.0, true) != NoelleFormRules.Lod.NONE || NoelleFormRules.lod(10.0 * 10.0, false) != NoelleFormRules.Lod.STANDARD) bad.add("the level of detail by distance is wrong");
        if (!bad.isEmpty()) return Result.fail(String.join("; ", bad.subList(0, Math.min(8, bad.size()))));

        // ---- with a stand-in player: the form pages through the real armour engine
        ServerPlayer fp = SelfTests.player(server.overworld(), "noelle_forms", 0.5, 100, 0.5);
        ManasSkillInstance wi = water.createDefaultInstance();
        ArmourState.takeOff(fp);
        fp.getPersistentData().remove(NoelleFormState.AFTER);
        try {
            ArmourCast.afterCast(fp, water, wi, NoelleFormRules.PAGE_MERMAID, 0);
            ArmourRules.Held h = ArmourState.held(fp);
            if (h == null || !h.magic().equals("WATER") || h.style() != NoelleFormRules.style(Form.MERMAID, false)) bad.add("the Mermaid page did not put the form on");
            if (NoelleFormState.worn(fp) != Form.MERMAID) bad.add("a wearer of the Mermaid style is not seen as the Mermaid Form");
            if (!ArmourCast.beforeCast(fp, water, wi, NoelleFormRules.PAGE_MERMAID)) bad.add("a second cast of the Mermaid page did not take the form off");
            if (ArmourState.held(fp) != null) bad.add("the Mermaid Form is still on after the toggle");
            ArmourState.putOn(fp, pol.entry("WATER"), NoelleFormRules.style(Form.SEA_DRAGON, true), false, 1f);
            if (NoelleFormState.worn(fp) != Form.SEA_DRAGON) bad.add("a wearer of the Sea Dragon God style is not seen as that form");
            ArmourState.takeOff(fp);
            ArmourState.putOn(fp, pol.entry("WATER"), NoelleFormRules.style(Form.VALKYRIE, true), false, 1f);
            if (NoelleFormState.worn(fp) != null) bad.add("the Valkyrie Dress was taken for one of the three limited forms");
            ArmourState.takeOff(fp);
            // the Black Dragon exhaustion keeps the form out until it is cleared
            NoelleFormState.exhaust(fp, fp.level().getGameTime());
            if (!NoelleFormState.refuseBlackDragon(fp)) bad.add("the Black Dragon exhaustion did not refuse a new form");
            fp.getPersistentData().remove(NoelleFormState.AFTER);
            fp.removeAllEffects();
        } finally {
            ArmourState.end(fp, ArmourRules.Drop.DEATH);
            fp.getPersistentData().remove(NoelleFormState.AFTER);
            fp.removeAllEffects();
        }
        if (!bad.isEmpty()) return Result.fail(String.join("; ", bad.subList(0, Math.min(8, bad.size()))));
        return Result.ok(models + " models, " + NoelleFormRules.SPRITES.length + " sprites, 3 pages appended and styled, locks set; the stand-in put on, kept and took off its forms");
    }
}
