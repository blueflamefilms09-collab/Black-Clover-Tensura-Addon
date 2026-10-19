package com.newuniverse.nusmp.book;

import com.newuniverse.nusmp.antimagic.AntiMagic;
import com.newuniverse.nusmp.antimagic.AntiMagicAura;
import com.newuniverse.nusmp.antimagic.AntiMagicCosts;
import com.newuniverse.nusmp.labyrinth.mega.SelfTests;
import com.newuniverse.nusmp.labyrinth.mega.SelfTests.Result;
import io.github.manasmods.manascore.skill.api.ManasSkillInstance;
import io.github.manasmods.tensura.storage.TensuraStorages;
import io.github.manasmods.tensura.util.EnergyHelper;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

import java.util.ArrayList;
import java.util.List;

/**
 * Why: nothing here can be played by its author, so the real game proves what can be proved without a person (the CI server smoke test runs
 * it; key prefix {@code antimagic}). {@code antimagic.aura}: with a stand-in player that has Tensura existence data, casting an Anti-Magic page
 * lowers the Aura and not the Anti-Magic Power (AMP); with no Aura (and no magicules) the page fails with "Not enough aura to ..." and does
 * not run; a page that does nothing gives its price back; the Black Form toggle is drained from the Aura and switches off cleanly when the
 * Aura is gone; stored AMP and the Lord state survive the migration. Registered from {@code NUSMP}.
 */
public final class AntiMagicSelfTests {
    private AntiMagicSelfTests() {}

    private static boolean registered;

    public static void register() {
        if (registered) return;
        registered = true;
        SelfTests.register("antimagic.aura", AntiMagicSelfTests::aura);
    }

    private static Result aura(MinecraftServer server) {
        List<String> bad = new ArrayList<>();
        AntiMagicBook book = new AntiMagicBook();

        // ---- the price table covers every page of the book and nothing else
        for (int k = 0; k < book.familyCount(); k++) {
            String id = book.page(k).id();
            if (!AntiMagicCosts.PAGES.containsKey(id)) bad.add("page " + id + " has no Aura price");
            if (book.page(k).costPercent() > 0 || book.page(k).costFloor() > 0) bad.add("page " + id + " still has a magicule price");
        }
        for (String id : AntiMagicCosts.PAGES.keySet()) {
            boolean found = false;
            for (int k = 0; k < book.familyCount(); k++) if (book.page(k).id().equals(id)) found = true;
            if (!found) bad.add("the price table has " + id + " but the book has no such page");
        }

        ServerPlayer fp = SelfTests.player(server.overworld(), "antimagic_aura", 0.5, 100, 0.5);
        var ex = TensuraStorages.getExistenceFrom(fp);
        if (ex == null) return bad.isEmpty() ? Result.ok("the stand-in has no Tensura existence data, so only the price table was checked") : Result.fail(String.join("; ", bad));
        double auraBefore = ex.getAura(), mpBefore = ex.getMagicule();
        try {
            AntiMagicAura.forget(fp.getUUID());
            double max = Math.max(EnergyHelper.getMaxAura(fp), 1000);
            ManasSkillInstance inst = book.createDefaultInstance();
            AntiMagic.setAmp(inst, 500);
            int[] ran = {0};
            BookPage.Cast probe = (b, i, p, m) -> { ran[0]++; return true; };

            // ---- a page costs Aura, not AMP
            ex.setAura(max);
            ex.setMagicule(0);
            ex.markDirty();
            if (ex.getAura() < 100) return bad.isEmpty() ? Result.ok("the stand-in cannot hold Aura (max " + EnergyHelper.getMaxAura(fp) + "), so only the price table was checked") : Result.fail(String.join("; ", bad));
            double a0 = ex.getAura();
            if (!AntiMagicBook.paid("black_divider", "swing the Black Divider", probe).cast(book, inst, fp, 1)) bad.add("a page with full Aura did not cast");
            if (ran[0] != 1) bad.add("the paid page ran " + ran[0] + " times");
            double spent = a0 - ex.getAura();
            double want = AntiMagicCosts.page("black_divider").amount(EnergyHelper.getMaxAura(fp));
            if (spent < AntiMagicCosts.page("black_divider").floor() - 1e-6) bad.add("a page took only " + spent + " Aura");
            if (Math.abs(spent - want) > Math.max(1e-6, want * 0.001) && EnergyHelper.getMaxAura(fp) > 0) bad.add("a page took " + spent + " Aura, the table says " + want);
            if (AntiMagic.amp(inst) != 500) bad.add("casting changed the AMP: " + AntiMagic.amp(inst));
            if (ex.getMagicule() != 0) bad.add("a page paid in Aura also took magicules");

            // ---- a page that does nothing gives its price back
            double a1 = ex.getAura();
            if (AntiMagicBook.paid("bull_thrust", "charge with Bull Thrust", (b, i, p, m) -> false).cast(book, inst, fp, 5)) bad.add("a page that did nothing reported success");
            if (ex.getAura() < a1 - 1e-6) bad.add("a page that did nothing kept its price: " + a1 + " became " + ex.getAura());

            // ---- no Aura (and no magicules): the page fails with the message and does not run
            ex.setAura(0);
            ex.setMagicule(0);
            ex.markDirty();
            AntiMagicAura.forget(fp.getUUID());
            if (AntiMagicBook.paid("infinite_slash", "cut the Infinite Slash", probe).cast(book, inst, fp, 6)) bad.add("a page cast with no Aura");
            if (ran[0] != 1) bad.add("a page with no Aura still ran");
            String msg = AntiMagicAura.lastMessage(fp.getUUID());
            if (msg == null || !msg.startsWith("Not enough aura to ")) bad.add("the failure message was '" + msg + "'");
            if (ex.getAura() != 0) bad.add("a failed page changed the Aura to " + ex.getAura());
            if (AntiMagic.amp(inst) != 500) bad.add("a failed page changed the AMP: " + AntiMagic.amp(inst));

            // ---- Black Form: drained from the Aura each second, AMP untouched
            ex.setAura(max);
            ex.markDirty();
            double a2 = ex.getAura();
            if (!AntiMagicAura.drain(fp, AntiMagicCosts.FORM_DRAIN)) bad.add("a second of Black Form could not be paid with full Aura");
            if (a2 - ex.getAura() < AntiMagicCosts.FORM_DRAIN.floor() - 1e-6) bad.add("a second of Black Form took only " + (a2 - ex.getAura()));
            if (AntiMagic.amp(inst) != 500) bad.add("the drain changed the AMP: " + AntiMagic.amp(inst));

            // ---- ... and switches off cleanly at zero Aura
            inst.getOrCreateTag().putBoolean(AntiMagicBook.K_LORD, true);
            inst.getOrCreateTag().putBoolean(AntiMagicBook.K_FORM, true);
            ex.setAura(0);
            ex.setMagicule(0);
            ex.markDirty();
            boolean held = AntiMagicBook.upkeepForm(inst, fp, true);
            if (held) bad.add("Black Form was still held at zero Aura");
            if (AntiMagicBook.inBlackForm(inst)) bad.add("Black Form did not switch off at zero Aura");
            if (!AntiMagicBook.isLord(inst)) bad.add("running out of Aura took the Lord state away");
            if (AntiMagic.amp(inst) != 500) bad.add("the form ending changed the AMP: " + AntiMagic.amp(inst));
            fp.removeAllEffects();

            // ---- migration: stored AMP and the Lord state are kept
            ManasSkillInstance old = book.createDefaultInstance();
            old.getOrCreateTag().putBoolean(AntiMagicBook.K_LORD, true);
            old.getOrCreateTag().putInt("AMP", 700);
            AntiMagicBook.migrate(old, false);
            if (!AntiMagicBook.isLord(old) || AntiMagic.amp(old) != 700) bad.add("migration lost the Lord state or the AMP: lord=" + AntiMagicBook.isLord(old) + " amp=" + AntiMagic.amp(old));
            if (old.getOrCreateTag().getInt(AntiMagicBook.K_VERSION) != AntiMagicCosts.DATA_VERSION) bad.add("migration did not stamp the data version");
        } finally {
            ex.setAura(auraBefore);
            ex.setMagicule(mpBefore);
            ex.markDirty();
            AntiMagicAura.forget(fp.getUUID());
        }
        return bad.isEmpty() ? Result.ok("pages " + AntiMagicCosts.PAGES.size() + ": Aura spent, AMP untouched, no Aura = 'Not enough aura to ...', Black Form ends at zero Aura, AMP and Lord migrate")
                : Result.fail(String.join("; ", bad));
    }
}
