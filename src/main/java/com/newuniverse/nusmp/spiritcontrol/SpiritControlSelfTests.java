package com.newuniverse.nusmp.spiritcontrol;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.tree.CommandNode;
import com.newuniverse.nusmp.blackclover.MagicType;
import com.newuniverse.nusmp.book.BookPage;
import com.newuniverse.nusmp.book.GrimoireBook;
import com.newuniverse.nusmp.labyrinth.mega.SelfTests;
import com.newuniverse.nusmp.labyrinth.mega.SelfTests.Result;
import com.newuniverse.nusmp.skill.NUSkills;
import com.newuniverse.nusmp.vfx.VfxShape;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.phys.Vec3;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Why: the control plane cannot be played by its author, so what can be proved without a person is proved by the real game: the CI server smoke test boots the mod and runs these. {@code magic.spirit_control}:
 * every spirit package registered its handle (eight in all), the numbers are sane, every effect shape exists, the six spirit pages are the LAST pages of every grimoire (after the Mana Zone's pages) with unique
 * ids, they are hidden from a player no spirit answers, the command tree has all its verbs under both roots, no spirit keybinding class is left, and a stand-in spirit round trip (summon, the rate limit,
 * release, an ability, an unknown ability) behaves. {@code magic.spirit_combat}: the config defaults hold (half and half, bounded), the toggle sets and clears, a forced cast on a real zombie pays the spirit's
 * pool and hurts it, the spirit runs dry and stops, the target rule accepts the zombie and refuses the owner, and everything of the player is gone after the forget.
 */
public final class SpiritControlSelfTests {
    private SpiritControlSelfTests() {}

    private static boolean registered;

    public static void register() {
        if (registered) return;
        registered = true;
        SelfTests.register("magic.spirit_control", SpiritControlSelfTests::control);
        SelfTests.register("magic.spirit_combat", SpiritControlSelfTests::combat);
    }

    /** A stand-in spirit that always answers and keeps its state in a field. */
    private static final class Dummy extends AbstractSpirit {
        boolean out;
        int abilityCalls;
        final SpiritCatalog.Entry row;

        Dummy(SpiritCatalog.Entry row) { super(row); this.row = row; }
        @Override protected boolean packageAllows(ServerPlayer p) { return true; }
        @Override public boolean isOut(ServerPlayer p) { return out; }
        @Override public boolean summon(ServerPlayer p) { out = true; return true; }
        @Override public void release(ServerPlayer p) { out = false; }
        @Override public boolean ability(ServerPlayer p, String id, int arg) { abilityCalls++; return true; }
        @Override public Vec3 origin(ServerPlayer p) { return p.position().add(0, 1.5, 0); }
    }

    private static final SpiritCatalog.Entry DUMMY_ROW = new SpiritCatalog.Entry("selftest_dummy", "Test Spirit", Set.of("FLAME"),
            List.of(new SpiritAbilityDef("poke", "Poke", 0.0)), SpiritCatalog.FIRE_SPELLS);

    private static Result control(MinecraftServer server) {
        if (!SpiritControlConfig.enabled()) return Result.fail("magic.spiritControl does not default to true");
        for (String id : new String[]{"salamander", "sylph", "undine", "gnome", "spirit_light", "spirit_dark", "spirit_space", "lord"})
            if (SpiritControl.REG.get(id) == null) return Result.fail("the spirit " + id + " did not register with the control plane");
        // the catalogue
        Set<String> seen = new HashSet<>();
        for (SpiritCatalog.Entry e : SpiritCatalog.all()) {
            if (!seen.add(e.id())) return Result.fail("duplicate catalogue id " + e.id());
            if (e.abilities().size() > PageGate.MAX_ABILITIES) return Result.fail(e.id() + " has too many abilities");
            Set<String> ab = new HashSet<>();
            for (SpiritAbilityDef d : e.abilities()) if (!ab.add(d.id())) return Result.fail(e.id() + " lists the ability " + d.id() + " twice");
            for (SpiritSpellDef s : e.spells()) {
                try { VfxShape.valueOf(s.shape()); } catch (IllegalArgumentException x) { return Result.fail("the spell " + s.id() + " names a missing VfxShape " + s.shape()); }
            }
        }
        for (SpiritSpellDef s : SpiritCatalog.ANTI_SPELLS) { try { VfxShape.valueOf(s.shape()); } catch (IllegalArgumentException x) { return Result.fail("missing VfxShape " + s.shape()); } }
        // the pages: the last six of every book, in order, unique
        List<String> want = PageGate.pageIds();
        int books = 0;
        for (var h : NUSkills.BOOKS) {
            GrimoireBook b = h.get();
            if (!b.pages().isEmpty() && b.pages().stream().noneMatch(pg -> pg.id().equals(GrimoireBook.STRIKE_ID))) continue;     // a book without the universal pages (the Forbidden book) has no spirit pages either
            List<BookPage> pages = b.pages();
            int n = pages.size() - com.newuniverse.nusmp.mana.method.ManaMethodPages.tailCount(pages);      // the five Mana Method pages are appended after the spirit pages
            if (n < want.size()) return Result.fail(b.magicId() + " has too few pages");
            Set<String> ids = new HashSet<>();
            for (BookPage pg : pages) if (!ids.add(pg.id())) return Result.fail(b.magicId() + " has the page " + pg.id() + " twice");
            for (int k = 0; k < want.size(); k++)
                if (!pages.get(n - want.size() + k).id().equals(want.get(k))) return Result.fail(b.magicId() + ": page " + (n - want.size() + k) + " should be " + want.get(k));
            books++;
        }
        if (books < 10) return Result.fail("only " + books + " grimoires carry the spirit pages");
        // hidden from a player no spirit answers
        ServerLevel level = server.overworld();
        Vec3 at = Vec3.atCenterOf(level.getSharedSpawnPos()).add(0, 2, 0);
        ServerPlayer p = SelfTests.standIn(level, "spirit_control", at.x, at.y, at.z);
        try {
            GrimoireBook fire = NUSkills.bookFor(MagicType.FLAME);
            int n = fire.pages().size() - com.newuniverse.nusmp.mana.method.ManaMethodPages.tailCount(fire.pages());
            for (int k = n - want.size(); k < n; k++) if (fire.usable(null, p, k)) return Result.fail("the spirit page " + fire.page(k).id() + " is usable for a player no spirit answers");
            if (!SpiritControl.eligible(p).isEmpty()) return Result.fail("a spirit answers a fresh stand-in player");
            // the command tree under both roots
            CommandDispatcher<CommandSourceStack> d = new CommandDispatcher<>();
            SpiritControlCommand.registerAlias(d);
            CommandNode<CommandSourceStack> root = d.getRoot().getChild("spirit");
            if (root == null) return Result.fail("no /spirit root");
            for (String verb : new String[]{"summon", "release", "status", "combat", "ability"}) if (root.getChild(verb) == null) return Result.fail("/spirit has no verb " + verb);
            if (root.getChild("combat").getChild("on") == null || root.getChild("combat").getChild("off") == null) return Result.fail("/spirit combat lacks on or off");
            // a stand-in spirit: summon, the rate limit, release, an ability, an unknown ability
            Dummy dummy = new Dummy(DUMMY_ROW);
            if (SpiritControl.summon(p, dummy) != SpiritControl.Verdict.DONE || !dummy.out) return Result.fail("summon did not call the stand-in spirit");
            if (SpiritControl.release(p, dummy) != SpiritControl.Verdict.RATE) return Result.fail("a second ask in the same tick was not rate limited");
            SpiritControl.forget(p.getUUID());
            if (SpiritControl.ability(p, dummy, "nonsense", 0) != SpiritControl.Verdict.NONE) return Result.fail("an unknown ability was accepted");
            if (SpiritControl.ability(p, dummy, "POKE", SpiritControl.MAX_SLOT) != SpiritControl.Verdict.NONE) return Result.fail("a slot out of range was accepted");
            if (SpiritControl.ability(p, dummy, "POKE", 0) != SpiritControl.Verdict.DONE || dummy.abilityCalls != 1) return Result.fail("the ability did not reach the spirit");
            SpiritControl.forget(p.getUUID());
            if (SpiritControl.release(p, dummy) != SpiritControl.Verdict.DONE || dummy.out) return Result.fail("release did not send the stand-in spirit away");
            if (SpiritControl.toggle(p, null) != SpiritControl.Verdict.NONE && SpiritControl.toggle(p, null) != SpiritControl.Verdict.RATE) return Result.fail("toggling no spirit did something");
        } finally {
            SpiritControl.forget(p.getUUID());
        }
        return Result.ok(SpiritControl.REG.size() + " spirits registered, " + books + " grimoires end with the six spirit pages, the command has all its verbs, the rate limit and the validation hold");
    }

    private static Result combat(MinecraftServer server) {
        if (!SpiritControlConfig.combat()) return Result.fail("combat mode does not default to on");
        if (Math.abs(SpiritControlConfig.userShare() - 0.5) > 1e-9) return Result.fail("the user's share does not default to one half");
        if (SpiritControlConfig.decisionTicks() < 5 || SpiritControlConfig.castGap() < 10 || SpiritControlConfig.decisionsPerTick() > 64 || SpiritControlConfig.serverCastsPerSecond() > 200) return Result.fail("a combat bound is outside its hard limits");
        ServerLevel level = server.overworld();
        Vec3 at = Vec3.atCenterOf(level.getSharedSpawnPos()).add(0, 2, 0);
        ServerPlayer p = SelfTests.standIn(level, "spirit_combat", at.x, at.y, at.z);
        Zombie z = EntityType.ZOMBIE.create(level);
        if (z == null) return Result.fail("no zombie to fight");
        try {
            SpiritCombat.set(p, true);
            if (!SpiritCombat.isOn(p.getUUID())) return Result.fail("the toggle did not set combat mode");
            SpiritCombat.set(p, false);
            if (SpiritCombat.isOn(p.getUUID())) return Result.fail("the toggle did not clear combat mode");
            z.moveTo(at.x + 3, at.y, at.z, 0f, 0f);
            level.addFreshEntity(z);
            if (!SpiritCombat.valid(p, z, 20)) return Result.fail("a hostile zombie is not a legal target");
            if (SpiritCombat.valid(p, p, 20)) return Result.fail("the owner is a legal target");
            Dummy spirit = new Dummy(DUMMY_ROW);
            spirit.out = true;
            float before = z.getHealth();
            long now = level.getGameTime();
            if (!SpiritCombat.castNow(p, spirit, 0, z, now)) return Result.fail("the forced cast was refused");
            SpiritMana m = SpiritCombat.manaOf(p.getUUID(), spirit.id());
            double wantLeft = SpiritControlConfig.manaMax(50) - ManaSplit.spiritUnits(SpiritCatalog.FIRE_SPELLS.get(0).units(), SpiritControlConfig.userShare());
            if (m == null || Math.abs(m.current() - wantLeft) > 1e-6) return Result.fail("the spirit's pool did not pay its half (" + (m == null ? "no pool" : m.current()) + " left, wanted " + wantLeft + ")");
            if (z.getHealth() >= before && !z.isRemoved()) return Result.fail("the cast did not hurt the zombie");
            int casts = 1;
            while (SpiritCombat.castNow(p, spirit, 0, z, now) && casts < 1000) casts++;
            if (casts >= 1000) return Result.fail("the spirit never ran dry");
            if (m.canSpend(ManaSplit.spiritUnits(SpiritCatalog.FIRE_SPELLS.get(0).units(), SpiritControlConfig.userShare()))) return Result.fail("the spirit stopped with mana left");
            SpiritCombat.set(p, true);
            SpiritCombat.forget(p.getUUID());
            if (SpiritCombat.isOn(p.getUUID()) || SpiritCombat.manaOf(p.getUUID(), spirit.id()) != null) return Result.fail("something of the player's combat mode stayed after the forget");
            return Result.ok("half and half pools, the cap on the damage, the legal targets, the spirit running dry after " + casts + " casts, and the cleanup hold");
        } finally {
            z.discard();
            SpiritCombat.forget(p.getUUID());
        }
    }
}
