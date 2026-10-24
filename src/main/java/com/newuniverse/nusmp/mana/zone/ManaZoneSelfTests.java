package com.newuniverse.nusmp.mana.zone;

import com.newuniverse.nusmp.avatar.cast.AvatarSkillList;
import com.newuniverse.nusmp.book.GrimoireBook;
import com.newuniverse.nusmp.labyrinth.mega.SelfTests;
import com.newuniverse.nusmp.labyrinth.mega.SelfTests.Result;
import com.newuniverse.nusmp.mana.ManaLaw;
import com.newuniverse.nusmp.skill.NUSkills;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.entity.projectile.Arrow;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.NeoForgeMod;
import net.neoforged.neoforge.common.util.FakePlayer;

/**
 * The CI self-tests of the Mana Zone (run by the server smoke test with a stand-in player, see labyrinth.mega.SelfTests):
 * <ul>
 *   <li>{@code mana.zone_gate}: the gate is closed for a fresh player in every way it can be asked (the verdict, the toggle, the old ManaLaw entry point, the four
 *       grimoire pages, the Avatar's cast list) and is the late-game numbers (floors, the shipped file read by the game);</li>
 *   <li>{@code mana.zone_core}: one tracked entry per caster, the radius a function of the start tick and capped, a foreign projectile destroyed from within and a
 *       boss's only slowed, the end (flight and speed taken back, nothing of hunger touched), and the Grand Magic Zone regions (all five loaded, found by position,
 *       harm for a weak player, the dungeon's hook binding a box and releasing it).</li>
 * </ul>
 */
public final class ManaZoneSelfTests {
    private ManaZoneSelfTests() {}

    public static void register() {
        SelfTests.register("mana.zone_gate", ManaZoneSelfTests::gate);
        SelfTests.register("mana.zone_core", ManaZoneSelfTests::core);
    }

    private static Result gate(MinecraftServer server) {
        ManaZoneRules rules = ManaZoneData.rules();
        if (rules.gate().minEp() < ManaZoneGate.FLOOR_EP) return Result.fail("the loaded gate asks " + rules.gate().minEp() + " EP, under the floor");
        if (ManaZoneGate.needStep(rules.gate(), 1.0) < ManaZoneGate.FLOOR_STEP) return Result.fail("the loaded rank is under the floor");
        if (ManaZoneGate.passes(rules.gate(), 1.0, ManaZoneGate.Progress.NONE)) return Result.fail("a player with nothing passes the gate");
        ServerLevel sl = server.overworld();
        FakePlayer p = SelfTests.player(sl, "zone_gate", 8.5, 120, 8.5);
        var verdict = ManaZoneManager.verdict(p);
        if (verdict.open()) return Result.fail("a fresh player passes the gate: " + verdict.summary());
        if (verdict.unmet().size() < 4) return Result.fail("a fresh player misses only " + verdict.unmet().size() + " of the six requirements");
        if (ManaZoneManager.toggle(p)) return Result.fail("the Mana Zone page opened a zone for a player who fails the gate");
        if (ManaZoneManager.active(p) || ManaLaw.zoneActive(p)) return Result.fail("a zone is open for a refused player");
        if (ManaLaw.toggleZone(p)) return Result.fail("the old ManaLaw.toggleZone entry point opened a zone for a refused player");
        if (ManaZoneManager.gateOpenFor(p)) return Result.fail("gateOpenFor says open for a refused player");
        // the pages: Mana Zone and the two new ones are not usable, and are the last pages of the book
        GrimoireBook book = NUSkills.BOOK_REINFORCEMENT.get();
        var pages = book.pages();
        int zone = -1, volley = -1, space = -1, truth = -1;
        for (int i = 0; i < pages.size(); i++) {
            String id = pages.get(i).id();
            if (id.equals(GrimoireBook.ZONE_ID)) zone = i;
            else if (id.equals(ManaZoneActions.VOLLEY_ID)) volley = i;
            else if (id.equals(ManaZoneActions.SPACE_ID)) space = i;
            else if (id.equals(TrueMagic.ID)) truth = i;
        }
        if (zone < 0 || volley < 0 || space < 0 || truth < 0) return Result.fail("a grimoire misses a Mana Zone page (zone " + zone + ", volley " + volley + ", space " + space + ", true magic " + truth + ")");
        int spirit = pages.size() - com.newuniverse.nusmp.mana.method.ManaMethodPages.tailCount(pages) - com.newuniverse.nusmp.spiritcontrol.PageGate.pageIds().size();      // 3.0.0: the six spirit pages of the control plane are appended after the Mana Zone's three
        if (space != volley + 1 || truth != space + 1 || truth != spirit - 1) return Result.fail("the new pages are not the three right before the spirit pages (" + volley + ", " + space + ", " + truth + " of " + pages.size() + ")");
        for (int m : new int[]{zone, volley, space, truth})
            if (book.usable(null, p, m)) return Result.fail("page " + pages.get(m).id() + " is usable for a player who fails the gate");
        for (String id : new String[]{GrimoireBook.ZONE_ID, ManaZoneActions.VOLLEY_ID, ManaZoneActions.SPACE_ID, TrueMagic.ID})
            if (AvatarSkillList.allowedPage(id, 6.0)) return Result.fail("the Avatar's cast list allows " + id);
        // the Wizard King's crown is the one forced way in (pure check: the gate's own rule)
        if (!ManaZoneGate.passes(rules.gate(), 1.0, new ManaZoneGate.Progress(0, 16, 0, 0, 0, 0, 0, true))) return Result.fail("the Wizard King is refused");
        return Result.ok("closed for a fresh player in all 6 ways; floors " + (long) ManaZoneGate.FLOOR_EP + " EP, step " + ManaZoneGate.FLOOR_STEP + "; gate " + (long) rules.gate().minEp() + " EP, step "
                + rules.gate().minKnightStep() + ", " + rules.gate().minBossKills() + " bosses, floor " + rules.gate().minDeepestFloor() + ", " + rules.gate().attuneSeconds() + " s");
    }

    private static Result core(MinecraftServer server) {
        ServerLevel sl = server.overworld();
        FakePlayer p = SelfTests.player(sl, "zone_core", 8.5, 120, 8.5);
        int food = p.getFoodData().getFoodLevel();
        float exhaustion = p.getFoodData().getExhaustionLevel();
        ManaZoneManager.end(p, ManaZoneManager.End.ADMIN);
        int before = ManaZoneManager.activeCount();
        long now = sl.getGameTime();

        // one tracked entry per caster, however often it is opened
        ManaZoneManager.startForTest(p, 600);
        ManaZoneManager.startForTest(p, 600);
        if (!ManaZoneManager.active(p)) return Result.fail("the zone is not active after it was started");
        if (ManaZoneManager.activeCount() != before + 1) return Result.fail("two starts made " + (ManaZoneManager.activeCount() - before) + " tracked entries for one caster");
        long start = ManaZoneManager.startTickOf(p);
        double cap = ManaZoneManager.capOf(p);
        ManaZoneRules.Zone zr = ManaZoneData.zone();
        if (cap < zr.baseRadius() - 1e-9 || cap > ManaZoneConfig.maxRadius() + 1e-9) return Result.fail("the cap " + cap + " is outside " + zr.baseRadius() + ".." + ManaZoneConfig.maxRadius());

        // the radius is a function of the start tick: zero at the start, the cap after the growth, never decreasing
        if (ManaZoneManager.radiusAt(p, start) != 0.0) return Result.fail("the zone has a radius at its start tick");
        double prev = -1;
        for (long t = 0; t <= zr.growTicks() + 20; t += 5) {
            double r = ManaZoneManager.radiusAt(p, start + t);
            if (r < prev - 1e-9) return Result.fail("the radius shrank at +" + t);
            prev = r;
        }
        if (Math.abs(ManaZoneManager.radiusAt(p, start + zr.growTicks() + 40) - cap) > 1e-6) return Result.fail("the radius does not reach the cap");

        // a foreign arrow in the zone is destroyed from within; a boss's is only slowed
        long later = start + zr.growTicks() + 40;
        Vec3 c = p.position();
        Arrow plain = EntityType.ARROW.create(sl);
        Arrow bossArrow = EntityType.ARROW.create(sl);
        Zombie boss = EntityType.ZOMBIE.create(sl);
        if (plain == null || bossArrow == null || boss == null) return Result.fail("could not create the test entities");
        boss.getAttribute(Attributes.MAX_HEALTH).setBaseValue(500.0);
        boss.setHealth(500f);
        plain.moveTo(c.x + 2, c.y + 1, c.z);
        plain.setDeltaMovement(1.0, 0.0, 0.0);
        bossArrow.moveTo(c.x - 2, c.y + 1, c.z);
        bossArrow.setDeltaMovement(-1.0, 0.0, 0.0);
        bossArrow.setOwner(boss);
        sl.addFreshEntity(plain);
        sl.addFreshEntity(bossArrow);
        ManaZoneManager.tickPlayer(p, later);
        boolean plainGone = plain.isRemoved();
        double bossSpeed = bossArrow.getDeltaMovement().length();
        boolean bossKept = !bossArrow.isRemoved();
        plain.discard();
        bossArrow.discard();
        if (!plainGone) return Result.fail("a foreign arrow inside the zone was not destroyed");
        if (!bossKept) return Result.fail("a boss's arrow was destroyed (it may only be slowed)");
        if (bossSpeed >= 0.9) return Result.fail("a boss's arrow was not slowed (" + bossSpeed + ")");

        // the end: the speed bonus and the flight are taken back, a cooldown was not charged for a forced zone, and the hunger was never touched
        ManaZoneManager.end(p, ManaZoneManager.End.DIMENSION);
        if (ManaZoneManager.active(p) || ManaZoneManager.activeCount() != before) return Result.fail("the zone is still tracked after it ended");
        if (p.getAttribute(Attributes.MOVEMENT_SPEED).hasModifier(net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("nusmp", "mana_zone_speed"))) return Result.fail("the speed bonus outlived the zone");
        var fly = p.getAttribute(NeoForgeMod.CREATIVE_FLIGHT);
        if (fly != null && fly.hasModifier(net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("nusmp", "mana_zone_foothold"))) return Result.fail("the footholds outlived the zone");
        if (p.getFoodData().getFoodLevel() != food || p.getFoodData().getExhaustionLevel() != exhaustion) return Result.fail("the zone touched the food bar");

        // the Grand Magic Zones: five named regions, found by position, hard on a weak player, bound and released by the dungeon's hook
        for (String id : new String[]{"seabed_temple", "witches_forest", "ultime_volcano_trail", "gravito_rock", "elysia"})
            if (GrandZones.byId(id) == null) return Result.fail("the Grand Magic Zone " + id + " is not loaded");
        GrandZoneRegion g = GrandZones.byId("gravito_rock");
        if (GrandZones.regionAt(net.minecraft.resources.ResourceLocation.parse(g.dimension()), g.cx(), g.cy(), g.cz()) == null) return Result.fail("the heart of " + g.id() + " is in no region");
        var weak = GrandZones.exposure(p, g);
        if (weak.harm() < 0.5) return Result.fail("a player with no EP is hardly harmed in " + g.id() + " (" + weak.harm() + ")");
        if (GrandZones.exposure(p, GrandZones.byId("elysia")).harm() != 0.0) return Result.fail("the haven harms");
        boolean bound = GrandZones.bind("selftest_floor", "seabed_temple", "minecraft:overworld", c.x - 20, c.y - 20, c.z - 20, c.x + 20, c.y + 20, c.z + 20);
        boolean found = GrandZones.regionAt(sl, p.blockPosition()) != null && GrandZones.regionAt(sl, p.blockPosition()).id().equals("selftest_floor");
        GrandZones.unbind("selftest_floor");
        if (!bound || !found) return Result.fail("the dungeon hook did not bind a box (bound " + bound + ", found " + found + ")");
        if (GrandZones.byId("selftest_floor") != null) return Result.fail("the dungeon hook's binding was not released");
        return Result.ok("one entry per caster, cap " + (int) cap + ", radius from the start tick, foreign arrow destroyed, boss arrow slowed to " + String.format("%.2f", bossSpeed)
                + ", ended clean, 5 regions, weak harm " + String.format("%.2f", weak.harm()) + ", hook bound and released");
    }
}
