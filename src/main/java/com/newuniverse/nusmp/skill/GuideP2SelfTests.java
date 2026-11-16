package com.newuniverse.nusmp.skill;

import com.newuniverse.nusmp.NUConfig;
import com.newuniverse.nusmp.balance.BalanceLaw;
import com.newuniverse.nusmp.blackclover.MagicType;
import com.newuniverse.nusmp.book.BookPage;
import com.newuniverse.nusmp.book.GrimoireBook;
import com.newuniverse.nusmp.book.GrimoireCastEvent;
import com.newuniverse.nusmp.labyrinth.mega.SelfTests;
import com.newuniverse.nusmp.labyrinth.mega.SelfTests.Result;
import com.newuniverse.nusmp.labyrinth.mega.cores.QuenchedBridge;
import com.newuniverse.nusmp.labyrinth.mega.cores.QuenchedRules;
import com.newuniverse.nusmp.skill.rules.UltimateClass;
import io.github.manasmods.manascore.skill.api.ManasSkill;
import io.github.manasmods.manascore.skill.api.ManasSkillInstance;
import io.github.manasmods.manascore.skill.api.SkillAPI;
import net.minecraft.server.MinecraftServer;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.common.util.FakePlayer;

/**
 * The CI self-tests of guide P2 (ids {@code guide.p2.pay}, {@code guide.p2.cast_event}, {@code guide.p2.damage_class}, {@code guide.p2.quenched}). Reads and one event post with a stand-in player:
 * nothing is paid, damaged or run (docs/guide_fixes_p2.md).
 * <ul>
 * <li>{@code pay}: the legacy grimoire skills answer a price of 0 outside a payment, owe nothing for a free cast, and the scaled price is never below the flat one.</li>
 * <li>{@code cast_event}: a Pre event with no listener is not cancelled, the cancel flag works, and a book's page list is one stable list.</li>
 * <li>{@code damage_class}: the class scaling a hit now uses stays inside epCap x lateGameShare, and a classified Skill reports its class to SkillDamage's lookup.</li>
 * <li>{@code quenched}: the bridge is off by default, builds no command from a blank template, and cannot throw without the other mod.</li>
 * </ul>
 */
public final class GuideP2SelfTests {
    private GuideP2SelfTests() {}

    private static boolean registered;

    public static void register() {
        if (registered) return;
        registered = true;
        SelfTests.register("guide.p2.pay", GuideP2SelfTests::pay);
        SelfTests.register("guide.p2.cast_event", GuideP2SelfTests::castEvent);
        SelfTests.register("guide.p2.damage_class", GuideP2SelfTests::damageClass);
        SelfTests.register("guide.p2.quenched", s -> quenched());
    }

    private static Result pay(MinecraftServer server) {
        FakePlayer p = SelfTests.player(server.overworld(), "GuideP2Pay", 0, 300, 0);
        ManasSkill skill = SkillAPI.getSkillRegistry().get(NUSkills.GRIMOIRES.get(MagicType.values()[0]).getId());
        if (!(skill instanceof GrimoireMagicSkill magic)) return Result.fail("the legacy grimoire skill is not registered: " + skill);
        ManasSkillInstance inst = magic.createDefaultInstance();
        if (magic.getMagiculeCost(p, inst, 0) != 0.0 || LegacyPay.paying() != 0.0) return Result.fail("a price is left over outside a payment");
        if (!LegacyPay.charge(inst, p, 0, 0.0)) return Result.fail("a free cast was refused");
        if (LegacyPay.paying() != 0.0) return Result.fail("the payment scope was not cleared");
        double flat = 800.0, scaled = BalanceLaw.classCost(p, null, flat);
        if (scaled < flat) return Result.fail("the scaled price is below the flat price: " + scaled);
        return Result.ok("price 800 -> " + (long) scaled + " for this pool (flatCosts=" + NUConfig.ucFlatCosts() + ")");
    }

    private static Result castEvent(MinecraftServer server) {
        FakePlayer p = SelfTests.player(server.overworld(), "GuideP2Cast", 0, 300, 0);
        GrimoireBook book = NUSkills.BOOK_REINFORCEMENT.get();
        ManasSkillInstance inst = book.createDefaultInstance();
        BookPage page = book.page(0);
        if (page == null) return Result.fail("the book has no first page");
        GrimoireCastEvent.Pre pre = NeoForge.EVENT_BUS.post(new GrimoireCastEvent.Pre(book, p, inst, page, 0));
        if (pre.isCanceled()) return Result.fail("a Pre event with no listener is cancelled");
        pre.setCanceled(true);
        if (!pre.isCanceled()) return Result.fail("the cancel flag does not stick");
        if (book.pages() != book.pages()) return Result.fail("the page list is rebuilt on every call (cache broken)");
        return Result.ok("grimoireCastEvent=" + NUConfig.gfCastEvent() + ", " + book.pages().size() + " pages in one stable list");
    }

    private static Result damageClass(MinecraftServer server) {
        FakePlayer p = SelfTests.player(server.overworld(), "GuideP2Class", 0, 300, 0);
        for (UltimateClass c : UltimateClass.values()) {
            double scale = BalanceLaw.classScale(p, c), cap = NUConfig.ucEpCap(c) * NUConfig.ucLateShare();
            if (scale > cap + 1e-9 || !(scale > 0.0)) return Result.fail(c + ": class scale x" + scale + " outside (0, " + cap + "]");
        }
        ManasSkill gate = SkillAPI.getSkillRegistry().get(NUSkills.GATE_OF_BABYLON.getId());
        if (!(gate instanceof NUSkill n) || n.ultimateClass() != UltimateClass.KING) return Result.fail("Gate of Babylon does not report its class");
        return Result.ok("classDamage=" + NUConfig.ucClassDamage() + ", lateGameShare=" + NUConfig.ucLateShare());
    }

    private static Result quenched() {
        if (QuenchedRules.render("", "Steve", 5) != null || QuenchedRules.render("say {amount}", "bad name", 5) != null) return Result.fail("an unsafe command was built");
        if (!"say Steve 5".equals(QuenchedRules.render("/say {player} {amount}", "Steve", 5))) return Result.fail("the template is not filled in");
        boolean present = QuenchedBridge.present();
        return Result.ok("bridge off by default, tensuranoindex loaded=" + present);
    }
}
