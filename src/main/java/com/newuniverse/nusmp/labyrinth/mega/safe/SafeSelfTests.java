package com.newuniverse.nusmp.labyrinth.mega.safe;

import com.newuniverse.nusmp.labyrinth.mega.Marker;
import com.newuniverse.nusmp.labyrinth.mega.MarkerKind;
import com.newuniverse.nusmp.labyrinth.mega.MegaDungeon;
import com.newuniverse.nusmp.labyrinth.mega.RegionContext;
import com.newuniverse.nusmp.labyrinth.mega.Room;
import com.newuniverse.nusmp.labyrinth.mega.RoomKind;
import com.newuniverse.nusmp.labyrinth.mega.Sector;
import com.newuniverse.nusmp.labyrinth.mega.SelfTests;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.AbstractFurnaceBlock;
import net.minecraft.world.level.block.BedBlock;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CampfireBlock;
import net.minecraft.world.level.block.LecternBlock;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.StairBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BedPart;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureProcessor;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureProcessorList;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureProcessorType;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import io.github.manasmods.tensura.registry.attribute.TensuraAttributes;

import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Why: nothing here can be played by the author, so the Safe-Zone builder registers SELF-TESTS that the CI server smoke test runs in the real game
 * ({@code /multiverse mega selftest}), with a fake player standing in for a person on the real, generated sector 0. Each test drives the system through
 * the SAME public entry points the event handlers call ({@link RestEvents#interact}, {@link RestEvents#advance}, {@link RestEvents#interruptByDamage},
 * {@link RegenThrottle#scaledHeal}, {@code Player.heal} through the real heal event, {@link ModCompatProcessor#processBlock}), advancing the game time
 * by passing it in, so a ten-second rest takes microseconds, and asserts what must hold: the rest heals and costs a short rest, a long rest refills and
 * starts the cool-down, a rest outside a safe zone is refused, an ambush-free rest is broken by damage, by walking away and by a monster, natural
 * regeneration is throttled outside the safe zones and normal inside them, the compat processor swaps a block when the mod and the block exist and
 * leaves it alone when they do not (a table that points at vanilla blocks stands in for the absent furniture mods, and the real table is put back), and
 * the zone query answers right at the Landing and at a corridor. Every test restores what it changed (the fake players' state and health, the compat
 * table, mobs it added). Keys: {@code safe.zone safe.rest_short safe.rest_long safe.rest_interrupt safe.no_rest_outside safe.regen_throttle
 * safe.processor safe.processor_list safe.compat_data}.
 */
final class SafeSelfTests {
    private SafeSelfTests() {}

    static void register() {
        SelfTests.register("safe.zone", SafeSelfTests::zone);
        SelfTests.register("safe.rest_short", SafeSelfTests::restShort);
        SelfTests.register("safe.rest_long", SafeSelfTests::restLong);
        SelfTests.register("safe.rest_interrupt", SafeSelfTests::restInterrupt);
        SelfTests.register("safe.no_rest_outside", SafeSelfTests::noRestOutside);
        SelfTests.register("safe.regen_throttle", SafeSelfTests::regenThrottle);
        SelfTests.register("safe.processor", SafeSelfTests::processor);
        SelfTests.register("safe.processor_list", SafeSelfTests::processorList);
        SelfTests.register("safe.compat_data", SafeSelfTests::compatData);
    }

    // ---------------------------------------------------------------- shared setup
    private record Ctx(ServerLevel level, Sector sector) {}

    private static Ctx ctx(MinecraftServer server) {
        ServerLevel level = SelfTests.mega(server);
        if (level == null) throw new IllegalStateException("no mega dimension");
        Sector s = MegaDungeon.data(server).sector(0);
        if (s == null) throw new IllegalStateException("sector 0 does not exist (the smoke test creates it with /multiverse mega expand first)");
        return new Ctx(level, s);
    }

    /** The first marker of this kind that stands in a Landing or Safe Zone of the sector (the templates guarantee one of each kind). */
    private static Marker anchorIn(Sector s, MarkerKind kind) {
        for (Marker m : s.markers(kind)) {
            Room r = s.roomAt(m.pos());
            if (r != null && r.kind.isSafe()) return m;
        }
        throw new IllegalStateException("sector 0 has no " + kind.id() + " marker in a safe room (" + s.markers(kind).size() + " in the whole sector)");
    }

    private static Room firstRoom(Sector s, boolean safe) {
        for (Room r : s.rooms) if (r.kind.isSafe() == safe && r.kind != RoomKind.CAP) return r;
        return null;
    }

    private static ServerPlayer playerAt(ServerLevel level, String name, BlockPos feet) {
        return SelfTests.player(level, name, feet.getX() + 0.5, feet.getY(), feet.getZ() + 0.5);
    }

    private static RestState fresh(ServerPlayer p) {
        RestState st = p.getData(SafeRegistry.REST_STATE);
        st.refill();
        st.shortRestsUsed = 0;
        st.lastRest = -1L;
        p.removeAllEffects();
        p.setHealth(p.getMaxHealth());
        return st;
    }

    private static int clearHostiles(ServerLevel level, ServerPlayer p, double radius) {
        int n = 0;
        for (Mob m : level.getEntitiesOfClass(Mob.class, p.getBoundingBox().inflate(radius), mob -> RestEvents.isHostile(mob, p))) {
            m.discard();
            n++;
        }
        return n;
    }

    /** Clicks {@code clicked} at {@code t0} and, when a rest starts, advances it in steps of the check interval until it ends; returns the last step and the end time through {@code endTime[0]}. */
    private static RestEvents.Step restThrough(ServerPlayer p, BlockPos clicked, long t0, RestKind expected, long[] endTime) {
        RestEvents.Click click = RestEvents.interact(p, clicked, ItemStack.EMPTY, t0);
        if (click.action() != RestEvents.Click.Action.STARTED) {
            throw new IllegalStateException("the click at " + clicked.toShortString() + " was " + click.action() + " (" + click.refusal() + "), not STARTED");
        }
        RestState st = p.getData(SafeRegistry.REST_STATE);
        if (st.channel == null || st.channel.kind != expected) throw new IllegalStateException("the rest that started is " + (st.channel == null ? "missing" : st.channel.kind));
        int total = st.channel.total;
        RestEvents.Step step = null;
        long t = t0;
        for (int i = 0; i < total / RestRules.CHECK_INTERVAL + 3; i++) {
            t += RestRules.CHECK_INTERVAL;
            step = RestEvents.advance(p, t);
            if (step.phase() != RestEvents.Step.Phase.RESTING) break;
        }
        endTime[0] = t;
        return step;
    }

    private static String pos(BlockPos p) { return p.getX() + " " + p.getY() + " " + p.getZ(); }

    // ---------------------------------------------------------------- safe.zone
    private static SelfTests.Result zone(MinecraftServer server) {
        Ctx c = ctx(server);
        Room landing = null;
        for (Room r : c.sector.rooms) if (r.kind == RoomKind.LANDING || r.kind == RoomKind.STAIRWELL) { landing = r; break; }
        if (landing == null) return SelfTests.Result.fail("sector 0 has no Landing room (" + c.sector.rooms.size() + " rooms)");
        if (!MegaDungeon.inSafeZone(c.level, landing.center())) return SelfTests.Result.fail("the Landing centre " + pos(landing.center()) + " is not a safe zone");
        int safe = 0;
        for (Room r : c.sector.rooms) {
            if (r.kind == RoomKind.SAFE) {
                safe++;
                if (!MegaDungeon.inSafeZone(c.level, r.center())) return SelfTests.Result.fail("the safe room " + r.template + " is not a safe zone at its centre");
            }
        }
        Room other = firstRoom(c.sector, false);
        if (other == null) return SelfTests.Result.fail("sector 0 has no room that is not safe, nothing to compare with");
        if (MegaDungeon.inSafeZone(c.level, other.center())) return SelfTests.Result.fail("the centre of " + other.template + " (" + other.kind + ") is wrongly a safe zone");
        BlockPos far = outsideEverything(c);
        if (MegaDungeon.inSafeZone(c.level, far)) return SelfTests.Result.fail("a position outside every sector is wrongly a safe zone");
        return SelfTests.Result.ok("landing " + landing.template + " safe, " + safe + " safe room(s) safe, " + other.template + " (" + other.kind + ") and the void outside not");
    }

    /** A position inside the dimension that no sector region holds. */
    private static BlockPos outsideEverything(Ctx c) {
        int x = c.sector.region.minX() - 40;
        for (int i = 0; i < 20; i++, x -= 40) {
            BlockPos p = new BlockPos(x, 65, c.sector.region.minZ() - 40);
            if (MegaDungeon.sectorAt(c.level, p) == null) return p;
        }
        return new BlockPos(c.sector.region.minX() - 2000, 65, c.sector.region.minZ() - 2000);
    }

    // ---------------------------------------------------------------- safe.rest_short
    private static SelfTests.Result restShort(MinecraftServer server) {
        Ctx c = ctx(server);
        Marker seat = anchorIn(c.sector, MarkerKind.SEAT);
        BlockPos clicked = seat.pos().below();
        ServerPlayer p = playerAt(c.level, "safe_rest_short", seat.pos());
        RestState st = fresh(p);
        try {
            int cleared = clearHostiles(c.level, p, 24);
            float max = p.getMaxHealth();
            p.setHealth(4f);
            boolean mana = RestMana.setMagiculeShare(p, 0.1);
            long[] end = new long[1];
            long t0 = c.level.getGameTime();
            RestEvents.Step step = restThrough(p, clicked, t0, RestKind.SHORT, end);
            if (step == null || step.phase() != RestEvents.Step.Phase.DONE) return SelfTests.Result.fail("the short rest did not finish: " + step);
            float expected = Math.min(max, 4f + RestRules.healAmount(4f, max, RestRules.healthShare(RestKind.SHORT, SafeConfig.get(SafeConfig.SHORT_REST_HEALTH))));
            if (Math.abs(p.getHealth() - expected) > 0.05f) return SelfTests.Result.fail("health after the short rest is " + p.getHealth() + ", expected " + expected);
            if (st.shortRestsUsed != 1 || st.channel != null || st.lastRest != end[0]) {
                return SelfTests.Result.fail("state after the short rest: used=" + st.shortRestsUsed + " channel=" + st.channel + " lastRest=" + st.lastRest);
            }
            String manaNote = "mana not tracked for a fake player";
            if (mana) {
                double share = RestMana.magiculeShare(p);
                double want = 0.1 + RestRules.manaShare(RestKind.SHORT, SafeConfig.get(SafeConfig.SHORT_REST_MANA));
                if (share + 0.03 < Math.min(1.0, want)) return SelfTests.Result.fail("mana share after the short rest is " + share + ", expected about " + Math.min(1.0, want));
                manaNote = "mana 10% -> " + Math.round(share * 100) + "%";
            }
            int uses = SafeConfig.get(SafeConfig.SHORT_REST_USES);
            long t = end[0] + 10;
            for (int i = 1; i < uses; i++) {                                           // spend the rest of the short rests
                step = restThrough(p, clicked, t, RestKind.SHORT, end);
                if (step == null || step.phase() != RestEvents.Step.Phase.DONE) return SelfTests.Result.fail("short rest number " + (i + 1) + " did not finish: " + step);
                t = end[0] + 10;
            }
            RestEvents.Click again = RestEvents.interact(p, clicked, ItemStack.EMPTY, t);
            if (again.action() != RestEvents.Click.Action.REFUSED || again.refusal() != RestRules.Refusal.NO_SHORT_RESTS) {
                return SelfTests.Result.fail("after " + uses + " short rests the next click was " + again.action() + " / " + again.refusal() + ", expected REFUSED / NO_SHORT_RESTS");
            }
            return SelfTests.Result.ok("seat at " + pos(seat.pos()) + ": health 4 -> " + expected + " of " + max + ", " + manaNote + ", " + uses + " rests then NO_SHORT_RESTS"
                    + (cleared > 0 ? " (cleared " + cleared + " hostile mob(s) first)" : ""));
        } finally {
            RestMana.setMagiculeShare(p, 1.0);
            p.setHealth(p.getMaxHealth());
            st.refill();
        }
    }

    // ---------------------------------------------------------------- safe.rest_long
    private static SelfTests.Result restLong(MinecraftServer server) {
        Ctx c = ctx(server);
        Marker bed = anchorIn(c.sector, MarkerKind.BED);
        BlockPos foot = bed.pos().below();
        ServerPlayer p = playerAt(c.level, "safe_rest_long", bed.pos());
        RestState st = fresh(p);
        try {
            int cleared = clearHostiles(c.level, p, 24);
            float max = p.getMaxHealth();
            p.setHealth(2f);
            st.shortRestsUsed = RestRules.DEFAULT_SHORT_USES;
            boolean mana = RestMana.setMagiculeShare(p, 0.05);
            p.getPersistentData().remove(RestEvents.LANDING_KEY);
            long[] end = new long[1];
            long t0 = c.level.getGameTime();
            RestEvents.Step step = restThrough(p, foot, t0, RestKind.LONG, end);
            if (step == null || step.phase() != RestEvents.Step.Phase.DONE) return SelfTests.Result.fail("the long rest did not finish: " + step);
            if (p.getHealth() < max - 0.05f) return SelfTests.Result.fail("health after the long rest is " + p.getHealth() + " of " + max);
            if (st.shortRestsUsed != 0) return SelfTests.Result.fail("the long rest did not refill the short rests (used=" + st.shortRestsUsed + ")");
            if (st.lastLongRest != end[0]) return SelfTests.Result.fail("lastLongRest=" + st.lastLongRest + ", expected " + end[0]);
            if (!p.getPersistentData().contains(RestEvents.LANDING_KEY) || p.getPersistentData().getInt(RestEvents.LANDING_KEY) < c.sector.id) {
                return SelfTests.Result.fail("the dungeon respawn Landing " + RestEvents.LANDING_KEY + " was not recorded");
            }
            String manaNote = "mana not tracked for a fake player";
            if (mana) {
                double share = RestMana.magiculeShare(p);
                if (share < 0.99) return SelfTests.Result.fail("mana share after the long rest is " + share + ", expected full");
                manaNote = "mana 5% -> 100%";
            }
            int cooldown = SafeConfig.get(SafeConfig.LONG_REST_COOLDOWN_TICKS);
            if (cooldown > 0) {
                RestEvents.Click soon = RestEvents.interact(p, foot, ItemStack.EMPTY, end[0] + 10);
                if (soon.action() != RestEvents.Click.Action.REFUSED || soon.refusal() != RestRules.Refusal.LONG_COOLDOWN) {
                    return SelfTests.Result.fail("a second long rest at once was " + soon.action() + " / " + soon.refusal() + ", expected REFUSED / LONG_COOLDOWN");
                }
            }
            // after the cool-down, starting from the OTHER half of the bed (the head) must work too
            BlockState bedState = c.level.getBlockState(foot);
            String headNote = "head click skipped (not a vanilla bed)";
            if (bedState.getBlock() instanceof BedBlock && bedState.hasProperty(BedBlock.FACING)) {
                BlockPos head = foot.relative(bedState.getValue(BedBlock.FACING));
                RestEvents.Click later = RestEvents.interact(p, head, ItemStack.EMPTY, end[0] + cooldown + 1);
                if (later.action() != RestEvents.Click.Action.STARTED || later.kind() != RestKind.LONG) {
                    return SelfTests.Result.fail("after the cool-down a click on the bed's head at " + head.toShortString() + " was " + later.action() + " / " + later.refusal());
                }
                p.getData(SafeRegistry.REST_STATE).channel = null;
                headNote = "head click starts a rest after the cool-down";
            }
            return SelfTests.Result.ok("bed at " + pos(foot) + ": health 2 -> " + max + ", " + manaNote + ", short rests refilled, cool-down " + cooldown + " ticks enforced, " + headNote
                    + (cleared > 0 ? " (cleared " + cleared + " hostile mob(s) first)" : ""));
        } finally {
            RestMana.setMagiculeShare(p, 1.0);
            p.setHealth(p.getMaxHealth());
            p.getPersistentData().remove(RestEvents.LANDING_KEY);
            st.refill();
        }
    }

    // ---------------------------------------------------------------- safe.rest_interrupt
    private static SelfTests.Result restInterrupt(MinecraftServer server) {
        Ctx c = ctx(server);
        Marker seat = anchorIn(c.sector, MarkerKind.SEAT);
        BlockPos clicked = seat.pos().below();
        ServerPlayer p = playerAt(c.level, "safe_rest_interrupt", seat.pos());
        RestState st = fresh(p);
        Zombie zombie = null;
        try {
            clearHostiles(c.level, p, 24);
            p.setHealth(4f);
            long t0 = c.level.getGameTime();
            // 1. damage
            if (RestEvents.interact(p, clicked, ItemStack.EMPTY, t0).action() != RestEvents.Click.Action.STARTED) return SelfTests.Result.fail("could not start a rest to interrupt");
            if (!RestEvents.interruptByDamage(p) || st.channel != null) return SelfTests.Result.fail("damage did not break the rest");
            if (p.getHealth() != 4f) return SelfTests.Result.fail("a broken rest healed anyway (" + p.getHealth() + ")");
            if (st.shortRestsUsed != 0) return SelfTests.Result.fail("a broken rest cost a short rest");
            // 2. walking away
            if (RestEvents.interact(p, clicked, ItemStack.EMPTY, t0).action() != RestEvents.Click.Action.STARTED) return SelfTests.Result.fail("could not start the second rest");
            p.setPos(p.getX() + 3.0, p.getY(), p.getZ());
            RestEvents.Step moved = RestEvents.advance(p, t0 + RestRules.CHECK_INTERVAL);
            if (moved.phase() != RestEvents.Step.Phase.BROKEN || moved.interrupt() != RestRules.Interrupt.MOVED) return SelfTests.Result.fail("walking away gave " + moved);
            p.setPos(seat.pos().getX() + 0.5, seat.pos().getY(), seat.pos().getZ() + 0.5);
            // 3. a monster arrives while the player rests
            String monster = "monster step skipped (no mob could be added)";
            if (RestEvents.interact(p, clicked, ItemStack.EMPTY, t0).action() != RestEvents.Click.Action.STARTED) return SelfTests.Result.fail("could not start the third rest");
            zombie = EntityType.ZOMBIE.create(c.level);
            if (zombie != null) {
                zombie.moveTo(seat.pos().getX() + 0.5, seat.pos().getY() - 1.0, seat.pos().getZ() + 5.5);
                boolean added = c.level.addFreshEntity(zombie);
                if (added && RestEvents.nearestHostile(c.level, p, 16.0) <= 16.0) {
                    RestEvents.Step hostile = RestEvents.advance(p, t0 + RestRules.CHECK_INTERVAL);
                    if (hostile.phase() != RestEvents.Step.Phase.BROKEN || hostile.interrupt() != RestRules.Interrupt.HOSTILE) return SelfTests.Result.fail("an arriving zombie gave " + hostile);
                    RestEvents.Click refused = RestEvents.interact(p, clicked, ItemStack.EMPTY, t0 + 20);
                    if (refused.action() != RestEvents.Click.Action.REFUSED || refused.refusal() != RestRules.Refusal.HOSTILE_NEAR) {
                        return SelfTests.Result.fail("a rest next to a zombie was " + refused.action() + " / " + refused.refusal() + ", expected REFUSED / HOSTILE_NEAR");
                    }
                    monster = "a zombie five blocks away breaks a rest and refuses the next one";
                } else {
                    st.channel = null;
                }
            } else {
                st.channel = null;
            }
            return SelfTests.Result.ok("damage and walking away break a rest and cost nothing; " + monster);
        } finally {
            if (zombie != null) zombie.discard();
            p.setHealth(p.getMaxHealth());
            st.refill();
        }
    }

    // ---------------------------------------------------------------- safe.no_rest_outside
    private static SelfTests.Result noRestOutside(MinecraftServer server) {
        Ctx c = ctx(server);
        Marker seat = anchorIn(c.sector, MarkerKind.SEAT);
        BlockPos clicked = seat.pos().below();
        long now = c.level.getGameTime();
        StringBuilder detail = new StringBuilder();
        BlockPos out = outsideEverything(c);
        ServerPlayer p = playerAt(c.level, "safe_rest_outside", out);
        RestState st = fresh(p);
        try {
            RestEvents.Click far = RestEvents.interact(p, clicked, ItemStack.EMPTY, now);
            if (far.action() != RestEvents.Click.Action.REFUSED || far.refusal() != RestRules.Refusal.NOT_SAFE_ZONE || st.channel != null) {
                return SelfTests.Result.fail("a rest attempt from outside every sector was " + far.action() + " / " + far.refusal() + " (channel " + st.channel + ")");
            }
            detail.append("from the void outside: NOT_SAFE_ZONE");
            Room hall = firstRoom(c.sector, false);
            if (hall != null) {
                p.setPos(hall.center().getX() + 0.5, hall.center().getY(), hall.center().getZ() + 0.5);
                RestEvents.Click in = RestEvents.interact(p, clicked, ItemStack.EMPTY, now);
                if (in.action() != RestEvents.Click.Action.REFUSED || in.refusal() != RestRules.Refusal.NOT_SAFE_ZONE || st.channel != null) {
                    return SelfTests.Result.fail("a rest attempt from the middle of " + hall.template + " was " + in.action() + " / " + in.refusal());
                }
                detail.append("; from the middle of ").append(hall.template).append(" (").append(hall.kind).append("): NOT_SAFE_ZONE");
                for (MarkerKind k : new MarkerKind[] {MarkerKind.SEAT, MarkerKind.HEARTH, MarkerKind.BED}) {
                    for (Marker m : c.sector.markers(k)) {
                        Room r = c.sector.roomAt(m.pos());
                        if (r == null || r.kind.isSafe()) continue;
                        p.setPos(m.pos().getX() + 0.5, m.pos().getY(), m.pos().getZ() + 0.5);
                        p.setShiftKeyDown(c.level.getBlockState(m.pos().below()).getBlock() instanceof AbstractFurnaceBlock);   // a furnace-type hearth rests only on a sneaking click
                        RestEvents.Click here = RestEvents.interact(p, m.pos().below(), ItemStack.EMPTY, now);
                        p.setShiftKeyDown(false);
                        if (here.action() != RestEvents.Click.Action.REFUSED || here.refusal() != RestRules.Refusal.NOT_SAFE_ZONE || st.channel != null) {
                            return SelfTests.Result.fail("a " + k.id() + " in " + r.template + " (" + r.kind + ") started or mis-refused a rest: " + here.action() + " / " + here.refusal());
                        }
                        detail.append("; the ").append(k.id()).append(" in ").append(r.template).append(": NOT_SAFE_ZONE");
                        break;
                    }
                }
            }
            return SelfTests.Result.ok(detail.toString());
        } finally {
            st.refill();
        }
    }

    // ---------------------------------------------------------------- safe.regen_throttle
    private static SelfTests.Result regenThrottle(MinecraftServer server) {
        Ctx c = ctx(server);
        Room landing = null;
        for (Room r : c.sector.rooms) if (r.kind == RoomKind.LANDING || r.kind == RoomKind.STAIRWELL) { landing = r; break; }
        if (landing == null) return SelfTests.Result.fail("sector 0 has no Landing room");
        double scale = SafeConfig.get(SafeConfig.PASSIVE_REGEN_SCALE);
        BlockPos out = outsideEverything(c);
        ServerPlayer p = playerAt(c.level, "safe_regen", out);
        fresh(p);
        try {
            float expectedOut = RegenMath.scaleHeal(1.0f, scale, false);
            p.setHealth(5f);
            p.heal(1.0f);                                                                 // the real heal event, as vanilla's natural regeneration calls it
            float gainOut = p.getHealth() - 5f;
            if (Math.abs(gainOut - expectedOut) > 0.001f) return SelfTests.Result.fail("outside a safe zone a natural heal of 1.0 gave " + gainOut + ", expected " + expectedOut + " (scale " + scale + ")");
            if (Math.abs(RegenThrottle.scaledHeal(p, 1.0f) - expectedOut) > 0.001f) return SelfTests.Result.fail("scaledHeal disagrees with the heal event");
            p.setHealth(5f);
            p.heal(4.0f);
            if (Math.abs(p.getHealth() - 9f) > 0.001f) return SelfTests.Result.fail("a big heal (a potion) was throttled: 5 + 4 gave " + p.getHealth());
            p.setHealth(5f);
            p.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 100, 0));
            p.heal(1.0f);
            if (Math.abs(p.getHealth() - 6f) > 0.001f) return SelfTests.Result.fail("the Regeneration effect was throttled: 5 + 1 gave " + p.getHealth());
            p.removeAllEffects();
            // the attribute modifiers
            RegenThrottle.refresh(p);
            boolean onOut = RegenThrottle.throttled(p);
            if (scale < 1.0 && !onOut && p.getAttribute(TensuraAttributes.MAGICULE_REGENERATION_MULTIPLIER) != null) {
                return SelfTests.Result.fail("the magicule regeneration modifier is not on outside a safe zone");
            }
            // inside the Landing everything is normal
            BlockPos in = landing.center();
            p.setPos(in.getX() + 0.5, in.getY(), in.getZ() + 0.5);
            p.setHealth(5f);
            p.heal(1.0f);
            if (Math.abs(p.getHealth() - 6f) > 0.001f) return SelfTests.Result.fail("inside the Landing a natural heal of 1.0 gave " + (p.getHealth() - 5f) + ", expected 1.0");
            RegenThrottle.refresh(p);
            if (RegenThrottle.throttled(p)) return SelfTests.Result.fail("the regeneration modifier stayed on inside the Landing");
            return SelfTests.Result.ok("scale " + scale + ": outside +" + gainOut + " per natural heal (potions and Regeneration untouched, modifier " + (onOut ? "on" : "off")
                    + "), inside the Landing +1.0 and the modifier off");
        } finally {
            p.setPos(out.getX() + 0.5, out.getY(), out.getZ() + 0.5);
            RegenThrottle.refresh(p);
            p.setHealth(p.getMaxHealth());
            p.removeAllEffects();
        }
    }

    // ---------------------------------------------------------------- safe.processor
    private static StructureTemplate.StructureBlockInfo run(ServerLevel level, Sector sector, Map<BlockPos, Marker> markers, StructureTemplate.StructureBlockInfo info,
                                                            StructurePlaceSettings settings) {
        ModCompatProcessor proc = new ModCompatProcessor();
        if (markers == null) return proc.processBlock(level, info.pos(), info.pos(), info, info, settings);
        return RegionContext.call(sector, markers, () -> proc.processBlock(level, info.pos(), info.pos(), info, info, settings));
    }

    private static CompatTable table(String mod, Map<String, List<String>> swaps, Set<String> flip) {
        return CompatTable.of(List.of(new CompatTable.Source(mod, swaps, flip, null)));
    }

    private static SelfTests.Result processor(MinecraftServer server) {
        Ctx c = ctx(server);
        ServerLevel level = c.level;
        CompatTable previous = CompatLoader.table();
        try {
            BlockPos at = new BlockPos(7, 70, 7);
            StructurePlaceSettings none = new StructurePlaceSettings();
            BlockState campfire = Blocks.CAMPFIRE.defaultBlockState().setValue(CampfireBlock.FACING, Direction.EAST);
            StructureTemplate.StructureBlockInfo fire = new StructureTemplate.StructureBlockInfo(at, campfire, null);
            Marker hearthMarker = new Marker(MarkerKind.HEARTH, at.above(), Map.of("as", "stove"));
            Map<BlockPos, Marker> withHearth = Map.of(at.above(), hearthMarker);

            // 1. the mod "is loaded" (it is this mod) and the second candidate exists: the first (a missing block) is skipped, the campfire becomes a smoker
            CompatLoader.install(table("nusmp", Map.of("stove", List.of("nusmp:no_such_block_selftest", "minecraft:smoker")), Set.of()));
            BlockState swapped = run(level, c.sector, withHearth, fire, none).state();
            if (!swapped.is(Blocks.SMOKER)) return SelfTests.Result.fail("a campfire with a hearth marker became " + swapped.getBlock() + ", expected the smoker");
            if (swapped.getValue(AbstractFurnaceBlock.FACING) != Direction.EAST) return SelfTests.Result.fail("the swap lost the facing: " + swapped);
            // 2. the mod is absent: the block stays
            CompatLoader.install(table("selftest_absent_mod", Map.of("stove", List.of("minecraft:smoker")), Set.of()));
            if (run(level, c.sector, withHearth, fire, none).state() != campfire) return SelfTests.Result.fail("a swap happened although its mod is not loaded");
            // 3. the mod is there but the block does not exist: the block stays
            CompatLoader.install(table("nusmp", Map.of("stove", List.of("nusmp:no_such_block_selftest")), Set.of()));
            if (run(level, c.sector, withHearth, fire, none).state() != campfire) return SelfTests.Result.fail("a swap happened for a block that does not exist");
            // 4. no marker above, and 5. no generator context: the block stays
            CompatLoader.install(table("nusmp", Map.of("stove", List.of("minecraft:smoker")), Set.of()));
            if (run(level, c.sector, Map.of(), fire, none).state() != campfire) return SelfTests.Result.fail("a swap happened with no marker");
            if (run(level, c.sector, null, fire, none).state() != campfire) return SelfTests.Result.fail("a swap happened outside the generator (no region context)");
            // 6. a stair seat flips its facing: oak stairs facing north + seat;as=chair -> a lectern facing south
            CompatLoader.install(table("nusmp", Map.of("chair", List.of("minecraft:lectern")), Set.of("chair")));
            BlockState stair = Blocks.OAK_STAIRS.defaultBlockState().setValue(StairBlock.FACING, Direction.NORTH);
            Marker seatMarker = new Marker(MarkerKind.SEAT, at.above(), Map.of("as", "chair", "facing", "south"));
            BlockState chair = run(level, c.sector, Map.of(at.above(), seatMarker), new StructureTemplate.StructureBlockInfo(at, stair, null), none).state();
            if (!chair.is(Blocks.LECTERN) || chair.getValue(LecternBlock.FACING) != Direction.SOUTH) {
                return SelfTests.Result.fail("a stair seat facing north became " + chair + ", expected a lectern facing south");
            }
            // 7. a bed: both halves swap together under a quarter turn (the marker is above the FOOT; the head finds it across the bed), the head alone does not
            CompatLoader.install(table("nusmp", Map.of("bed", List.of("minecraft:blue_bed")), Set.of()));
            StructurePlaceSettings turned = new StructurePlaceSettings().setRotation(Rotation.CLOCKWISE_90);
            BlockState footState = Blocks.RED_BED.defaultBlockState().setValue(BedBlock.FACING, Direction.SOUTH).setValue(BedBlock.PART, BedPart.FOOT);
            BlockState headState = footState.setValue(BedBlock.PART, BedPart.HEAD);
            BlockPos foot = at;
            BlockPos head = foot.relative(Direction.WEST);                                // south turned a quarter clockwise is west
            Marker bedMarker = new Marker(MarkerKind.BED, foot.above(), Map.of("as", "bed"));
            Map<BlockPos, Marker> withBed = Map.of(foot.above(), bedMarker);
            BlockState newFoot = run(level, c.sector, withBed, new StructureTemplate.StructureBlockInfo(foot, footState, null), turned).state();
            BlockState newHead = run(level, c.sector, withBed, new StructureTemplate.StructureBlockInfo(head, headState, null), turned).state();
            if (!newFoot.is(Blocks.BLUE_BED) || newFoot.getValue(BedBlock.PART) != BedPart.FOOT || newFoot.getValue(BedBlock.FACING) != Direction.SOUTH) return SelfTests.Result.fail("the bed foot became " + newFoot);
            if (!newHead.is(Blocks.BLUE_BED) || newHead.getValue(BedBlock.PART) != BedPart.HEAD || newHead.getValue(BedBlock.FACING) != Direction.SOUTH) return SelfTests.Result.fail("the bed head became " + newHead);
            BlockState lonelyHead = run(level, c.sector, Map.of(), new StructureTemplate.StructureBlockInfo(head, headState, null), turned).state();
            if (lonelyHead != headState) return SelfTests.Result.fail("a bed head with no marker over its foot was swapped");
            return SelfTests.Result.ok("campfire -> smoker (first candidate missing, second used, facing kept); absent mod, missing block, no marker and no context leave it alone; "
                    + "stair seat -> lectern with the facing flipped; a turned bed swaps foot and head together, a lone head stays");
        } finally {
            CompatLoader.install(previous);
        }
    }

    // ---------------------------------------------------------------- safe.processor_list
    private static SelfTests.Result processorList(MinecraftServer server) {
        ResourceLocation typeId = ResourceLocation.fromNamespaceAndPath("nusmp", "mod_compat");
        if (BuiltInRegistries.STRUCTURE_PROCESSOR.getOptional(typeId).isEmpty()) return SelfTests.Result.fail("the processor type nusmp:mod_compat is not registered");
        if (SafeRegistry.MOD_COMPAT.get().codec() != ModCompatProcessor.CODEC) return SelfTests.Result.fail("the registered type does not use ModCompatProcessor.CODEC");
        Tag encoded = StructureProcessorType.SINGLE_CODEC.encodeStart(NbtOps.INSTANCE, new ModCompatProcessor()).getOrThrow();
        StructureProcessor back = StructureProcessorType.SINGLE_CODEC.parse(NbtOps.INSTANCE, encoded).getOrThrow();
        if (!(back instanceof ModCompatProcessor)) return SelfTests.Result.fail("the processor does not survive its own codec: " + encoded);
        ServerLevel level = SelfTests.mega(server);
        if (level == null) return SelfTests.Result.fail("no mega dimension");
        Registry<StructureProcessorList> lists = level.registryAccess().registryOrThrow(Registries.PROCESSOR_LIST);
        ResourceLocation listId = ResourceLocation.fromNamespaceAndPath("nusmp", "mega_interior");
        StructureProcessorList list = lists.getOptional(listId).orElse(null);
        if (list == null) return SelfTests.Result.fail("the processor list " + listId + " was not loaded (a missing processor type fails the whole file)");
        boolean has = false;
        for (StructureProcessor p : list.list()) if (p instanceof ModCompatProcessor) has = true;
        if (!has) return SelfTests.Result.fail("the processor list " + listId + " does not contain nusmp:mod_compat (" + list.list().size() + " processors)");
        return SelfTests.Result.ok("nusmp:mod_compat is registered, round-trips through its codec, and sits in " + listId + " (" + list.list().size() + " processors)");
    }

    // ---------------------------------------------------------------- safe.compat_data
    private static SelfTests.Result compatData(MinecraftServer server) {
        int seen = CompatLoader.filesSeen();
        if (seen < 2) return SelfTests.Result.fail("the data reload found " + seen + " compat file(s) under data/<ns>/mega/compat, expected at least the two that ship");
        CompatTable t = CompatLoader.table();
        return SelfTests.Result.ok(seen + " compat files found, " + CompatLoader.filesUsed() + " used (their mod is loaded); live hints: " + (t.isEmpty() ? "none (no compat mod installed)" : t.hints()));
    }
}
