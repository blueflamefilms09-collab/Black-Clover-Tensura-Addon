package com.newuniverse.nusmp.fairytail.storm;

import com.newuniverse.nusmp.blackclover.MagicType;
import com.newuniverse.nusmp.item.WeaponEngravings;
import com.newuniverse.nusmp.labyrinth.mega.SelfTests;
import com.newuniverse.nusmp.labyrinth.mega.SelfTests.Result;
import com.newuniverse.nusmp.skill.NUSkills;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.item.ItemStack;

import java.util.HashSet;
import java.util.Set;

/**
 * Why: nothing here can be played by the author, so the CI server smoke test runs this in the real game ({@code /multiverse mega selftest magic.storm_staff}). It checks what only the
 * game can: the staff is registered and cycles its spells, the old Storm grimoire is still registered and still reachable by name but no longer rolled for a Wind soul (config
 * {@code storm.staffOnly}), a real cast by a stand-in player makes a storm through the whole pipeline (cooldown, caps, cost, packets), the per-player cap ends the oldest storm, and a
 * thunderhead run tick by tick over a zombie really hurts it through the balance law, within the per-creature caps, with no vanilla lightning entity spawned.
 */
public final class StormSelfTests {
    private StormSelfTests() {}

    public static void register() { SelfTests.register("magic.storm_staff", StormSelfTests::run); }

    static Result run(MinecraftServer server) {
        // the table
        Set<String> ids = new HashSet<>();
        for (StormSpell s : StormSpell.values()) {
            if (!ids.add(s.id)) return Result.fail(s.id + ": duplicate id");
            if (s.mask == 0 || s.durationTicks < 20 || s.radius <= 0 || s.cooldownTicks < 20) return Result.fail(s.id + ": a number is missing");
            if (s.has(StormComp.BOLTS) && (s.boltRaw <= 0 || s.maxBolts <= 0 || s.boltEvery <= 0)) return Result.fail(s.id + ": bolts without bolt numbers");
            if (s.has(StormComp.HAIL) && s.hailRaw <= 0) return Result.fail(s.id + ": hail without hail damage");
            if (s.isLine() && (s.aimRange <= 0 || s.tickRaw <= 0)) return Result.fail(s.id + ": a line spell without length or damage");
            if (StormMath.maskOf(StormMath.packSeed(12345L, s.mask, s.ordinal())) != s.mask || StormMath.spellOf(StormMath.packSeed(12345L, s.mask, s.ordinal())) != s.ordinal()) return Result.fail(s.id + ": the seed does not carry the spell");
        }
        // the staff
        if (StormBoot.STAFF.get() == null) return Result.fail("no staff item");
        ItemStack stack = new ItemStack(StormBoot.STAFF.get());
        StormSpell sel = StormStaffItem.selected(stack);
        for (int i = 0; i < StormSpell.count(); i++) {
            StormStaffItem.select(stack, StormStaffItem.selected(stack).next());
        }
        if (StormStaffItem.selected(stack) != sel) return Result.fail("the staff does not cycle back to its first spell after " + StormSpell.count() + " turns");
        // the old grimoire: still registered, no longer rolled
        if (!NUSkills.GRIMOIRES.containsKey(MagicType.STORM) || !NUSkills.BOOK_STORM.isBound()) return Result.fail("the old Storm grimoire is no longer registered");
        if (MagicType.lookup("STORM") != MagicType.STORM) return Result.fail("Storm Magic is no longer found by name");
        if (StormConfig.staffOnly() && MagicType.forSoul("WIND").contains(MagicType.STORM)) return Result.fail("staffOnly is on but a Wind soul can still roll Storm Magic");
        if (!MagicType.forSoul("WIND").contains(MagicType.WIND)) return Result.fail("the Wind soul lost Wind Magic");

        ServerLevel level = server.overworld();
        double y = level.getMaxBuildHeight() - 40;
        ServerPlayer fake = SelfTests.standIn(level, "storm_selftest", 0.5, y, 0.5);
        StormField.clearAll(false, server);
        long lightningBefore = level.getEntities(EntityType.LIGHTNING_BOLT, e -> true).size();

        // a real cast, the cooldown, and the per-player cap
        StormField.Cast first = StormField.cast(fake, StormSpell.GATHERING_CLOUDS);
        if (!first.done()) return Result.fail("a Gathering cast by a stand-in did not happen: " + first.message());
        if (StormField.liveOf(fake.getUUID()) != 1) return Result.fail("the cast made " + StormField.liveOf(fake.getUUID()) + " storms");
        if (StormField.cast(fake, StormSpell.GATHERING_CLOUDS).done()) return Result.fail("the cooldown did not hold a second Gathering");
        if (!StormField.cast(fake, StormSpell.DOWNPOUR).done() || !StormField.cast(fake, StormSpell.HAILSTORM).done()) return Result.fail("a second and third spell did not cast");
        int cap = StormConfig.perPlayer();
        if (StormField.liveOf(fake.getUUID()) > cap) return Result.fail("the player has " + StormField.liveOf(fake.getUUID()) + " storms, the cap is " + cap);
        StormField.clearAll(false, server);

        // a thunderhead run over a zombie
        fake.getCooldowns().removeCooldown(StormBoot.STAFF.get());
        if (!StormField.cast(fake, StormSpell.THUNDERHEAD).done()) return Result.fail("a Thunderhead cast did not happen");
        StormCell cell = StormField.newest();
        if (cell == null) return Result.fail("no storm after the cast");
        Zombie z = EntityType.ZOMBIE.create(level);
        if (z == null) return Result.fail("no zombie");
        z.setPos(cell.from[0] + 1.0, cell.from[1], cell.from[2]);
        z.setNoAi(true);
        z.setNoGravity(true);
        level.addFreshEntity(z);
        float before = z.getHealth();
        for (int age = 0; age < cell.duration; age++) StormField.tickAt(level, fake, cell, age, cell.start + age);
        float after = z.getHealth();
        int bolts = cell.hits.count(z.getId(), StormHits.Kind.BOLT);
        boolean hurt = after < before || !z.isAlive();
        z.discard();
        StormField.clearAll(false, server);
        if (!hurt) return Result.fail("a thunderhead run did not hurt the zombie (" + before + " -> " + after + ", bolts landed " + bolts + ")");
        if (bolts > StormMath.MAX_BOLTS_PER_TARGET) return Result.fail("the zombie took " + bolts + " bolts, the cap is " + StormMath.MAX_BOLTS_PER_TARGET);
        if (level.getEntities(EntityType.LIGHTNING_BOLT, e -> true).size() != lightningBefore) return Result.fail("a vanilla lightning bolt was spawned");

        // the engravings
        ItemStack engraved = new ItemStack(StormBoot.STAFF.get());
        WeaponEngravings.engrave(engraved, level.registryAccess());
        if (engraved.getEnchantments().isEmpty()) return Result.fail("the staff came out with no Tensura engravings");
        return Result.ok(StormSpell.count() + " spells, cast + caps + cooldown ok, thunderhead " + before + " -> " + after + " HP (" + bolts + " bolts), engravings " + engraved.getEnchantments().size());
    }
}
