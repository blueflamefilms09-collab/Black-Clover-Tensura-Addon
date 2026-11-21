package com.newuniverse.nusmp.stones;

import com.newuniverse.nusmp.block.NUBlocks;
import com.newuniverse.nusmp.labyrinth.mega.SelfTests.Result;
import com.newuniverse.nusmp.relics.Act;
import com.newuniverse.nusmp.relics.RelicCatalog;
import com.newuniverse.nusmp.relics.RelicDef;
import com.newuniverse.nusmp.relics.Relics;
import net.minecraft.world.item.ItemStack;

import java.util.List;
import java.util.Locale;
import java.util.UUID;

/**
 * Why: the CI server smoke test proves what a server with no player can for Phase B (keys {@code stones.carrier.*}, covered by the existing smoke prefix {@code stones}): {@code rules} (the circuit, the surge,
 * the Snap's fates, the caps), {@code defs} (the Nano Gauntlet and the scepter exist, the old Infinity Gauntlet kept its old acts and gained the carrier acts, nothing controls a mind), {@code data} (a real
 * stack takes a Mind Stone in its container component, reads it back, refuses a stone the carrier may not hold, and gives it back), {@code state} (lifts and links are capped and emptied), {@code snap}
 * (refusals, mode cycling, Erase off by default) and {@code altar} (the block and its item are registered). Server safe: no client class.
 */
final class CarrierSelfTests {
    private CarrierSelfTests() {}

    static Result rules() {
        InfinityStone[] c = CarrierRules.CIRCUIT;
        for (int i = 0; i < c.length; i++) if (CarrierRules.predecessor(c[(i + 1) % c.length]) != c[i]) return Result.fail("the circuit is not Mind > Power > Space > Time > Reality > Soul > Mind");
        if (CarrierRules.predecessor(InfinityStone.MIND) != InfinityStone.SOUL) return Result.fail("Mind does not follow Soul");
        int mask = CarrierRules.with(CarrierRules.with(0, InfinityStone.MIND), InfinityStone.POWER);
        if (!CarrierRules.boosted(mask, InfinityStone.POWER) || CarrierRules.boosted(mask, InfinityStone.MIND) || CarrierRules.boosted(mask, InfinityStone.SPACE)) return Result.fail("the circuit boosts the wrong stone");
        if (CarrierRules.effectFactor(mask, InfinityStone.POWER, true) != 1.25 || CarrierRules.cooldownFactor(mask, InfinityStone.POWER, true) != 0.85 || CarrierRules.effectFactor(mask, InfinityStone.POWER, false) != 1.0) return Result.fail("the circuit numbers are wrong");
        if (CarrierRules.surgeFraction(1.0e9, 1.0e9, false) != 0.10 || CarrierRules.surgeFraction(1.0e9, 1.0e9, true) != 0.20) return Result.fail("the surge at the gate is not 10% / 20%");
        if (CarrierRules.surgeFraction(1.0e10, 1.0e9, false) >= CarrierRules.surgeFraction(1.0e9, 1.0e9, false)) return Result.fail("the surge is not inverse to strength");
        if (CarrierRules.surgeFraction(1.0, 1.0e12, true) > 0.30 || StoneRules.recoil(100f, 2f, CarrierRules.surgeFraction(1.0e9, 1.0e9, true)) > 1f) return Result.fail("the surge can kill");
        if (CarrierRules.fate(false, true, false, 10f) != CarrierRules.Fate.NONE || CarrierRules.fate(false, false, true, 10f) != CarrierRules.Fate.NONE) return Result.fail("a player or a spared mob can be dusted");
        if (CarrierRules.fate(true, false, false, 1000f) != CarrierRules.Fate.TENTH || CarrierRules.eraseDamage(CarrierRules.Fate.TENTH, 1000f, 1000f) > 100.001f) return Result.fail("a boss loses more than a tenth");
        if (CarrierRules.fate(false, false, false, 20f) != CarrierRules.Fate.DUST || CarrierRules.fate(false, false, false, 500f) != CarrierRules.Fate.HALF) return Result.fail("the dust threshold is wrong");
        if (CarrierRules.liftable(true, false, true) || CarrierRules.liftable(false, true, true) || !CarrierRules.liftable(false, false, true)) return Result.fail("telekinesis may lift a player or a boss");
        if (CarrierRules.linkAmplifier(9) != CarrierRules.LINK_MAX_AMP || CarrierRules.linkTicks(100000) != CarrierRules.LINK_EFFECT_TICKS) return Result.fail("a Mind Link effect is not capped");
        if (CarrierRules.allowedMask(CarrierRules.SCEPTER_ID) != CarrierRules.bit(InfinityStone.MIND) || CarrierRules.allowedMask(CarrierRules.NANO_ID) != CarrierRules.ALL || CarrierRules.allowedMask("stone") != 0) return Result.fail("a carrier holds the wrong stones");
        return Result.ok("circuit, surge, fates, caps, carrier masks");
    }

    static Result defs() {
        RelicDef inf = Relics.byId(CarrierRules.INFINITY_ID), nano = Relics.byId(CarrierRules.NANO_ID), sc = Relics.byId(CarrierRules.SCEPTER_ID);
        if (inf == null || nano == null || sc == null) return Result.fail("a carrier is missing from the catalog");
        if (inf.actByKey("snap") == null || inf.actByKey("greater_wish") == null || inf.actByKey("stone_space") == null) return Result.fail("the old Infinity Gauntlet lost an old act");
        for (RelicDef d : List.of(inf, nano)) {
            for (String k : new String[]{"cs_socket", "cs_unsocket", "cs_combo_mend", "cs_combo_rift", "cs_snap_mode", "cs_snap"}) if (d.actByKey(k) == null) return Result.fail(d.id() + " lacks " + k);
            for (InfinityStone s : InfinityStone.values()) for (int n = 1; n <= CarrierRules.slotActs(s); n++) if (d.actByKey(CarrierRules.carrierKey(s, n)) == null) return Result.fail(d.id() + " lacks " + CarrierRules.carrierKey(s, n));
            if (d.actByKey("cs_snap").holdTicks() != 100) return Result.fail(d.id() + " Snap is not a five second hold");
        }
        for (InfinityStone s : InfinityStone.values()) {
            if (s == InfinityStone.MIND) continue;
            if (sc.actByKey(CarrierRules.carrierKey(s, 1)) != null) return Result.fail("the scepter holds more than the Mind Stone");
        }
        for (int n = 1; n <= 4; n++) if (sc.actByKey(CarrierRules.carrierKey(InfinityStone.MIND, n)) == null) return Result.fail("the scepter lacks Mind act " + n);
        if (sc.actByKey("sc_blast") == null || sc.actByKey("sc_barrier") == null || sc.engravings().isEmpty() || sc.meleeDamage() <= 1.0) return Result.fail("the scepter is not a weapon with engravings");
        for (RelicDef d : List.of(inf, nano, sc)) {
            for (Act a : d.acts()) {
                String n = a.displayName().toLowerCase(Locale.ROOT);
                if (n.contains("dominion") || n.contains("dominate") || n.contains("charm") || n.contains("control") || n.contains("enthrall")) return Result.fail(d.id() + " has a controlling act: " + a.displayName());
            }
        }
        return Result.ok("two gauntlets and the scepter, old acts kept, carrier acts added, no controlling act");
    }

    static Result data() {
        RelicDef nano = Relics.byId(CarrierRules.NANO_ID), sc = Relics.byId(CarrierRules.SCEPTER_ID), mind = Relics.byId(InfinityStone.MIND.relicId()), space = Relics.byId(InfinityStone.SPACE.relicId());
        if (nano == null || sc == null || mind == null || space == null) return Result.fail("a def is missing");
        ItemStack gauntlet = RelicCatalog.newStack(nano), scepter = RelicCatalog.newStack(sc), mindStone = RelicCatalog.newStack(mind), spaceStone = RelicCatalog.newStack(space);
        if (gauntlet.isEmpty() || scepter.isEmpty() || mindStone.isEmpty() || spaceStone.isEmpty()) return Result.fail("a stack could not be made");
        if (CarrierData.mask(gauntlet) != 0) return Result.fail("a new gauntlet already holds a stone");
        if (!CarrierData.put(gauntlet, InfinityStone.MIND, mindStone) || CarrierData.mask(gauntlet) != CarrierRules.bit(InfinityStone.MIND)) return Result.fail("the gauntlet did not take the Mind Stone");
        if (CarrierData.put(gauntlet, InfinityStone.MIND, mindStone)) return Result.fail("a taken socket took a second stone");
        if (CarrierData.put(gauntlet, InfinityStone.SPACE, mindStone)) return Result.fail("the Space socket took a Mind Stone");
        if (CarrierData.put(scepter, InfinityStone.SPACE, spaceStone) || CarrierData.mask(scepter) != 0) return Result.fail("the scepter took a Space Stone");
        if (!CarrierData.put(scepter, InfinityStone.MIND, mindStone) || CarrierData.mask(scepter) != CarrierRules.bit(InfinityStone.MIND)) return Result.fail("the scepter did not take the Mind Stone");
        List<ItemStack> back = CarrierData.takeAll(gauntlet);
        if (back.size() != 1 || StoneSlot.stoneOf(back.get(0)) != InfinityStone.MIND || CarrierData.mask(gauntlet) != 0) return Result.fail("unsocketing did not give the stone back");
        CarrierData.setMode(gauntlet, CarrierRules.SnapMode.PURGE);
        if (CarrierData.mode(gauntlet) != CarrierRules.SnapMode.PURGE) return Result.fail("the Snap mode did not read back");
        return Result.ok("sockets take, refuse and give back; the Snap mode is kept");
    }

    static Result state() {
        CarrierState.clear();
        int cap = 3;
        for (int i = 0; i < cap + 5; i++) CarrierState.openLift(UUID.randomUUID(), new CarrierState.Lift(UUID.randomUUID(), "minecraft:overworld", 100), cap);
        if (CarrierState.lifts().size() != cap) return Result.fail("the lift table is not capped");
        for (int i = 0; i < cap + 5; i++) CarrierState.openLink(UUID.randomUUID(), new CarrierState.Link(List.of(), "minecraft:overworld", 100, 40), cap);
        if (CarrierState.links().size() != cap) return Result.fail("the link table is not capped");
        UUID id = UUID.randomUUID();
        CarrierState.openLift(id, new CarrierState.Lift(UUID.randomUUID(), "minecraft:overworld", 100), 100);
        CarrierState.masteryAwarded(id, 5);
        CarrierState.setMask(id, 3);
        CarrierState.forgetAll(id);
        if (CarrierState.lift(id) != null || CarrierState.link(id) != null || CarrierState.lastMastery(id) != 0 || CarrierState.mask(id) != 0) return Result.fail("forgetting a player left their tables");
        CarrierState.setBoost(5.0);
        if (CarrierState.boost() > 2.0) return Result.fail("the circuit boost is not capped");
        CarrierState.clear();
        if (CarrierState.boost() != 1.0 || !CarrierState.lifts().isEmpty() || !CarrierState.links().isEmpty()) return Result.fail("clear left something");
        return Result.ok("lifts and links capped, a player's tables are emptied");
    }

    static Result snap() {
        int all = CarrierRules.ALL;
        long now = 1000, later = now + CarrierRules.DAY;
        if (CarrierRules.snapRefusal(all, true, true, false, now, 0) != null) return Result.fail("a ready Snap is refused");
        if (CarrierRules.snapRefusal(all - 1, true, true, false, now, 0) == null) return Result.fail("a Snap with five stones is allowed");
        if (CarrierRules.snapRefusal(all, true, false, false, now, 0) == null || CarrierRules.snapRefusal(all, true, true, true, now, 0) == null) return Result.fail("the gate or a held hand does not stop the Snap");
        if (CarrierRules.snapRefusal(all, false, true, false, now, 0) == null || CarrierRules.snapRefusal(all, true, true, false, now, later) == null) return Result.fail("the setting or the cooldown does not stop the Snap");
        if (CarrierRules.snapReadyAt(now, 7) != now + 7 * CarrierRules.DAY) return Result.fail("the Snap cooldown is not in game days");
        if (CarrierRules.nextMode(CarrierRules.SnapMode.RESTORE, false) != CarrierRules.SnapMode.PURGE || CarrierRules.nextMode(CarrierRules.SnapMode.RESTORE, true) != CarrierRules.SnapMode.ERASE) return Result.fail("the Snap modes do not cycle");
        if (CarrierRules.nextMode(CarrierRules.SnapMode.GRANT, false) != CarrierRules.SnapMode.RESTORE) return Result.fail("the Snap modes do not wrap");
        if (StoneConfig.snapErase()) return Result.fail("the Erase Snap is on by default");
        return Result.ok("refusals, cooldown, mode cycling, Erase off by default");
    }

    static Result altar() {
        if (!net.minecraft.core.registries.BuiltInRegistries.BLOCK.containsKey(net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("anime_multiverse", "vormir_altar"))) return Result.fail("the altar block is not registered");
        if (NUBlocks.VORMIR_ALTAR_ITEM.get() == null) return Result.fail("the altar has no item");
        if (CarrierRules.altarOffer("ep") || CarrierRules.altarOffer("player") || !CarrierRules.altarOffer("skill") || !CarrierRules.altarOffer("pet")) return Result.fail("the altar asks for the wrong thing");
        if (StoneConfig.altarRequired()) return Result.fail("the altar is required by default");
        return Result.ok("the altar block and item are registered; it asks for a skill or a pet only");
    }
}
