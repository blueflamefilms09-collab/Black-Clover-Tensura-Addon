package com.newuniverse.nusmp.book;

import com.newuniverse.nusmp.aura.Aura;
import com.newuniverse.nusmp.aura.PlayerAuras;
import com.newuniverse.nusmp.blackclover.MagicType;
import com.newuniverse.nusmp.labyrinth.mega.SelfTests;
import com.newuniverse.nusmp.labyrinth.mega.SelfTests.Result;
import com.newuniverse.nusmp.skill.NUSkills;
import io.github.manasmods.manascore.skill.api.ManasSkillInstance;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

import java.util.ArrayList;
import java.util.List;

/**
 * Why: nothing here can be played by its author, so the real game proves what can be proved without a person (the CI server smoke test runs it; key prefix {@code magic}).
 * {@code magic.armour_policy}: the shipped policy table loads without a complaint and knows every magic; no more than 40 percent of the magics have an armour; every armour page the
 * table names is a page of the magic's real book (ids that exist, none invented); for every book, every page that is not an armour or form page answers "no armour instruction";
 * and with a stand-in player: casting a bolt of an armoured magic neither equips nor resets an armour that is already on, the armour page of a worn armour only takes it off, the
 * armour page puts it on, a magic without armour gets no render layer from any of its pages, and death clears the state. Registered from {@code NUSMP}.
 */
public final class ArmourPolicySelfTests {
    private ArmourPolicySelfTests() {}

    private static boolean registered;

    public static void register() {
        if (registered) return;
        registered = true;
        SelfTests.register("magic.armour_policy", ArmourPolicySelfTests::policy);
    }

    private static Result policy(MinecraftServer server) {
        List<String> bad = new ArrayList<>();
        ArmourPolicy pol = ArmourPolicy.get();
        bad.addAll(pol.problems());

        // ---- every magic has an entry; the armoured share is small
        for (MagicType m : MagicType.values()) if (pol.entry(m.name()) == null) bad.add(m.name() + ": no entry in the armour policy");
        int total = MagicType.values().length, armoured = pol.count(ArmourPolicy.Kind.TOGGLE) + pol.count(ArmourPolicy.Kind.FORM);
        if (armoured * 10 > total * 4) bad.add(armoured + " of " + total + " magics have an armour (more than 40 percent)");

        // ---- the pages the table names are real pages; every other page answers NONE
        int books = 0, pagesChecked = 0;
        for (MagicType m : MagicType.values()) {
            GrimoireBook book = NUSkills.bookFor(m);
            ArmourPolicy.Entry e = pol.entry(m.name());
            if (book == null || e == null) continue;
            books++;
            List<String> ids = new ArrayList<>();
            for (BookPage pg : book.pages()) ids.add(pg.id());
            for (String id : e.armourPages().keySet()) if (!ids.contains(id)) bad.add(m.name() + ": armour page " + id + " is not a page of its book");
            for (String id : e.formPages()) if (!ids.contains(id)) bad.add(m.name() + ": form page " + id + " is not a page of its book");
            for (String id : e.externalPages()) if (!ids.contains(id)) bad.add(m.name() + ": form page " + id + " is not a page of its book");
            for (String id : ids) {
                pagesChecked++;
                boolean armourPage = e.armourPages().containsKey(id), formPage = e.formPages().contains(id);
                ArmourPolicy.Instruction want = armourPage ? ArmourPolicy.Instruction.ARMOUR : formPage ? ArmourPolicy.Instruction.FORM : ArmourPolicy.Instruction.NONE;
                if (pol.forCast(m.name(), id) != want) bad.add(m.name() + ": page " + id + " answers " + pol.forCast(m.name(), id) + ", expected " + want);
                if (e.kind() == ArmourPolicy.Kind.NONE && want != ArmourPolicy.Instruction.NONE) bad.add(m.name() + ": a NONE magic has an armour instruction on " + id);
            }
        }
        if (books == 0) bad.add("no grimoire book resolved: the registry is empty");
        if (!bad.isEmpty()) return Result.fail(String.join("; ", bad.subList(0, Math.min(8, bad.size()))));

        // ---- with a stand-in player
        ServerPlayer fp = SelfTests.player(server.overworld(), "armour_policy", 0.5, 100, 0.5);
        GrimoireBook flame = NUSkills.bookFor(MagicType.FLAME), air = NUSkills.bookFor(MagicType.AIR);
        if (flame == null || air == null) return Result.ok(books + " books, " + pagesChecked + " pages answered; the stand-in checks were skipped (no Flame or Air book)");
        ArmourState.takeOff(fp);
        ManasSkillInstance fi = flame.createDefaultInstance(), ai = air.createDefaultInstance();
        try {
            // a cast of an ordinary page of an armoured magic neither equips nor touches a worn armour
            ArmourCast.afterCast(fp, flame, fi, "exploding_fireball", 0);
            if (ArmourState.held(fp) != null) bad.add("a Flame bolt equipped an armour");
            ArmourPolicy.Entry fe = pol.entry("FLAME");
            ArmourState.putOn(fp, fe, 0, false, 1f);
            long since = fp.getPersistentData().getCompound(ArmourState.KEY).getLong("T");
            ArmourCast.afterCast(fp, flame, fi, "sol_linea", 1);
            ArmourRules.Held h = ArmourState.held(fp);
            if (h == null || !h.magic().equals("FLAME") || h.form()) bad.add("a Flame bolt changed the worn armour");
            if (fp.getPersistentData().getCompound(ArmourState.KEY).getLong("T") != since) bad.add("a Flame bolt reset the armour's timer");
            // the armour page of a worn armour only takes it off
            if (!ArmourCast.beforeCast(fp, flame, fi, "flame_armour")) bad.add("the Flame Armour page did not take the armour off");
            if (ArmourState.held(fp) != null) bad.add("the armour is still on after the toggle");
            // the armour page puts it on
            ArmourCast.afterCast(fp, flame, fi, "flame_armour", 9);
            if (!ArmourState.isOn(fp, MagicType.FLAME)) bad.add("the Flame Armour page did not put the armour on");
            if (!PlayerAuras.active(fp, Aura.FLAME)) bad.add("the Flame armour layer is not running on the server");
            // death clears it
            ArmourState.end(fp, ArmourRules.Drop.DEATH);
            if (ArmourState.held(fp) != null) bad.add("death did not clear the armour");
            // a magic without armour gets no layer from any page
            for (BookPage pg : air.pages()) {
                ArmourCast.afterCast(fp, air, ai, pg.id(), 0);
                if (ArmourState.held(fp) != null) bad.add("Air page " + pg.id() + " equipped an armour");
            }
            if (PlayerAuras.active(fp, Aura.AIR)) bad.add("an Air page started the Air armour layer");
        } finally {
            ArmourState.end(fp, ArmourRules.Drop.DEATH);
        }
        if (!bad.isEmpty()) return Result.fail(String.join("; ", bad.subList(0, Math.min(8, bad.size()))));
        return Result.ok(pol.count(ArmourPolicy.Kind.NONE) + " NONE, " + pol.count(ArmourPolicy.Kind.TOGGLE) + " TOGGLE, " + pol.count(ArmourPolicy.Kind.FORM) + " FORM of " + total
                + " magics; " + books + " books and " + pagesChecked + " pages answered; the stand-in toggled, kept and cleared its armour");
    }
}
