package com.newuniverse.nusmp.labyrinth.mega.rift;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.newuniverse.nusmp.labyrinth.Labyrinth;
import com.newuniverse.nusmp.labyrinth.mega.MegaData;
import com.newuniverse.nusmp.labyrinth.mega.MegaDungeon;
import com.newuniverse.nusmp.labyrinth.mega.Sector;
import com.newuniverse.nusmp.labyrinth.mega.rift.pocket.ExclusivePockets;
import com.newuniverse.nusmp.labyrinth.mega.rift.pocket.MegaRifts;
import com.newuniverse.nusmp.labyrinth.mega.rift.pocket.PocketDef;
import com.newuniverse.nusmp.labyrinth.mega.rift.pocket.PocketDefs;
import com.newuniverse.nusmp.labyrinth.mega.rift.pocket.PocketRuntime;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.entity.BlockEntity;

/**
 * Why: the owner's hands on the rifts and the waypoint, attached under {@code /multiverse mega} (permission 2): {@code rift open <shortcut|pocket [boss|puzzle]|return>} opens one in front of the sender (the
 * way to look at the renderer and to test a destination), {@code rift list} lists the open ones with their age and life, {@code rift info} reads the nearest one (kind, floor it leads to, whether it is steady,
 * its pocket), {@code rift enter [player]} steps a player through the nearest one at once, {@code rift roll [shortcut|pocket]} rolls for a rift at the sender's feet (the spawner's own code, with the reason when
 * nothing opened), {@code rift why} says which of the spawner's conditions holds at the sender's feet, {@code rift closeall} closes them, {@code rift pocket list|collapse <slot>|clear <slot>|enter <slot>}
 * shows, ends, skips (opens the vault of) and visits the instanced realms, {@code rift waypoint show|save|clear|return [player]} reads and drives the waypoint (the first questions when "the crystal does
 * not take me back"), and {@code rift status} shows the switches. All text goes through lang keys {@code message.nusmp.mega.rift.cmd.*} with an English fallback.
 */
public final class RiftCommand {
    private RiftCommand() {}

    public static LiteralArgumentBuilder<CommandSourceStack> command() {
        return Commands.literal("rift").requires(s -> s.hasPermission(2))
                .then(Commands.literal("open")
                        .then(Commands.literal("shortcut").executes(c -> open(c.getSource(), RiftType.SHORTCUT, null)))
                        .then(Commands.literal("pocket").executes(c -> open(c.getSource(), RiftType.POCKET, null))
                                .then(Commands.literal("boss").executes(c -> open(c.getSource(), RiftType.POCKET, RiftRules.Variant.BOSS)))
                                .then(Commands.literal("puzzle").executes(c -> open(c.getSource(), RiftType.POCKET, RiftRules.Variant.PUZZLE)))
                                .then(Commands.literal("mega").executes(c -> openMega(c.getSource(), ""))
                                        .then(Commands.argument("id", StringArgumentType.word()).suggests((c, b) -> net.minecraft.commands.SharedSuggestionProvider.suggest(PocketDefs.ids(), b))
                                                .executes(c -> openMega(c.getSource(), StringArgumentType.getString(c, "id"))))))
                        .then(Commands.literal("return").executes(c -> open(c.getSource(), RiftType.RETURN, null))))
                .then(Commands.literal("list").executes(c -> list(c.getSource())))
                .then(Commands.literal("info").executes(c -> info(c.getSource())))
                .then(Commands.literal("enter").executes(c -> enter(c.getSource(), c.getSource().getPlayerOrException()))
                        .then(Commands.argument("player", EntityArgument.player()).executes(c -> enter(c.getSource(), EntityArgument.getPlayer(c, "player")))))
                .then(Commands.literal("roll").executes(c -> roll(c.getSource(), null))
                        .then(Commands.literal("shortcut").executes(c -> roll(c.getSource(), RiftType.SHORTCUT)))
                        .then(Commands.literal("pocket").executes(c -> roll(c.getSource(), RiftType.POCKET))))
                .then(Commands.literal("why").executes(c -> why(c.getSource())))
                .then(Commands.literal("closeall").executes(c -> closeAll(c.getSource())))
                .then(Commands.literal("status").executes(c -> status(c.getSource())))
                .then(Commands.literal("pocket")
                        .then(Commands.literal("list").executes(c -> pockets(c.getSource())))
                        .then(Commands.literal("collapse").then(Commands.argument("slot", IntegerArgumentType.integer(0, RiftRules.MAX_SLOTS - 1))
                                .executes(c -> collapse(c.getSource(), IntegerArgumentType.getInteger(c, "slot")))))
                        .then(Commands.literal("clear").then(Commands.argument("slot", IntegerArgumentType.integer(0, RiftRules.MAX_SLOTS - 1))
                                .executes(c -> clearPocket(c.getSource(), IntegerArgumentType.getInteger(c, "slot")))))
                        .then(Commands.literal("enter").then(Commands.argument("slot", IntegerArgumentType.integer(0, RiftRules.MAX_SLOTS - 1))
                                .executes(c -> visit(c.getSource(), IntegerArgumentType.getInteger(c, "slot")))))
                        .then(Commands.literal("types").executes(c -> types(c.getSource())))                                                      // 1.99.0: the exclusive tombs
                        .then(Commands.literal("make").then(Commands.argument("id", StringArgumentType.word()).suggests((c, b) -> net.minecraft.commands.SharedSuggestionProvider.suggest(PocketDefs.ids(), b))
                                .executes(c -> make(c.getSource(), StringArgumentType.getString(c, "id")))))
                        .then(Commands.literal("seal").then(Commands.argument("slot", IntegerArgumentType.integer(0, RiftRules.MAX_SLOTS - 1))
                                .executes(c -> sealOpen(c.getSource(), IntegerArgumentType.getInteger(c, "slot")))))
                        .then(Commands.literal("fall").then(Commands.argument("slot", IntegerArgumentType.integer(0, RiftRules.MAX_SLOTS - 1))
                                .executes(c -> fall(c.getSource(), IntegerArgumentType.getInteger(c, "slot"))))))
                .then(Commands.literal("tier").executes(c -> tier(c.getSource(), c.getSource().getPlayerOrException()))
                        .then(Commands.argument("player", EntityArgument.player()).executes(c -> tier(c.getSource(), EntityArgument.getPlayer(c, "player")))))
                .then(Commands.literal("pity").then(Commands.argument("player", EntityArgument.player())
                        .then(Commands.argument("count", IntegerArgumentType.integer(0, 100000)).executes(c -> pity(c.getSource(), EntityArgument.getPlayer(c, "player"), IntegerArgumentType.getInteger(c, "count"))))))
                .then(Commands.literal("waypoint")
                        .then(Commands.literal("show").executes(c -> show(c.getSource(), c.getSource().getPlayerOrException()))
                                .then(Commands.argument("player", EntityArgument.player()).executes(c -> show(c.getSource(), EntityArgument.getPlayer(c, "player")))))
                        .then(Commands.literal("save").executes(c -> save(c.getSource(), c.getSource().getPlayerOrException()))
                                .then(Commands.argument("player", EntityArgument.player()).executes(c -> save(c.getSource(), EntityArgument.getPlayer(c, "player")))))
                        .then(Commands.literal("clear").executes(c -> clear(c.getSource(), c.getSource().getPlayerOrException()))
                                .then(Commands.argument("player", EntityArgument.player()).executes(c -> clear(c.getSource(), EntityArgument.getPlayer(c, "player")))))
                        .then(Commands.literal("return").executes(c -> back(c.getSource(), c.getSource().getPlayerOrException()))
                                .then(Commands.argument("player", EntityArgument.player()).executes(c -> back(c.getSource(), EntityArgument.getPlayer(c, "player"))))))
                .then(Commands.literal("rooms").executes(c -> rooms(c.getSource(), -1))                                                               // 1.101.0: every room of a pocket, its doors, its switch
                        .then(Commands.argument("slot", IntegerArgumentType.integer(0, RiftRules.MAX_SLOTS - 1)).executes(c -> rooms(c.getSource(), IntegerArgumentType.getInteger(c, "slot")))))
                .then(Commands.literal("switch").executes(c -> switchInfo(c.getSource(), -1))                                                         // 1.101.0: where the release lever is, whether it works, and the way to test it
                        .then(Commands.argument("slot", IntegerArgumentType.integer(0, RiftRules.MAX_SLOTS - 1)).executes(c -> switchInfo(c.getSource(), IntegerArgumentType.getInteger(c, "slot"))))
                        .then(Commands.literal("open").executes(c -> switchOpen(c.getSource(), -1))
                                .then(Commands.argument("slot", IntegerArgumentType.integer(0, RiftRules.MAX_SLOTS - 1)).executes(c -> switchOpen(c.getSource(), IntegerArgumentType.getInteger(c, "slot")))))
                        .then(Commands.literal("tp").executes(c -> switchTp(c.getSource(), -1))
                                .then(Commands.argument("slot", IntegerArgumentType.integer(0, RiftRules.MAX_SLOTS - 1)).executes(c -> switchTp(c.getSource(), IntegerArgumentType.getInteger(c, "slot")))))
                        .then(Commands.literal("repair").executes(c -> repairPocket(c.getSource(), -1))
                                .then(Commands.argument("slot", IntegerArgumentType.integer(0, RiftRules.MAX_SLOTS - 1)).executes(c -> repairPocket(c.getSource(), IntegerArgumentType.getInteger(c, "slot"))))));
    }

    // ---------------------------------------------------------------- 1.101.0: rooms and the switch (docs/labyrinth/pocket_fix.md)
    /** The slot an admin means: the one given, else the pocket they stand in, else the only pocket alive; -1 when none of these is clear. */
    private static int slotOf(CommandSourceStack s, int given) {
        if (given >= 0) return given;
        if (s.getEntity() != null) {
            int here = RiftRules.slotAt(s.getEntity().getX(), s.getEntity().getZ());
            if (here >= 0 && PocketRealm.get(s.getServer()).at(here) != null) return here;
        }
        PocketRealm realm = PocketRealm.get(s.getServer());
        return realm.pockets().size() == 1 ? realm.pockets().keySet().iterator().next() : -1;
    }

    private static String where(int slot, int lx, int ly, int lz, boolean tomb) {
        BlockPos w = tomb ? com.newuniverse.nusmp.labyrinth.mega.rift.pocket.PocketCell.world(slot, lx, ly, lz) : PocketBuilder.world(slot, lx, ly, lz);
        return w.getX() + " " + w.getY() + " " + w.getZ();
    }

    /** Lists every room of a pocket with its place, its doors (and which is sealed or barred), the switch, and what the watchdog finds missing. */
    private static int rooms(CommandSourceStack s, int given) {
        int slot = slotOf(s, given);
        PocketRealm.Pocket pk = slot < 0 ? null : PocketRealm.get(s.getServer()).at(slot);
        if (pk == null) return fail(s, text("noslot", "Name a pocket slot (/multiverse mega rift pocket list), or stand in one."));
        ServerLevel mega = MegaDungeon.level(s.getServer());
        int n = 0;
        if (pk.exclusive()) {
            PocketRuntime rt = PocketRuntime.of(pk);
            if (rt == null) return fail(s, text("notombslot", "There is no tomb in slot %s.", slot));
            ok(s, text("roomsTomb", "Tomb %s in slot %s: %s rooms, %s doors (1 sealed), phase %s, build %s%%; entry room %s, key room %s, lair %s, treasury %s.", pk.pocketId, slot, rt.layout.rooms().size(), rt.layout.doors().size(), pk.phase,
                    rt.build == null ? 100 : rt.build.percent(), rt.layout.entryId(), rt.layout.keyId(), rt.layout.bossId(), rt.layout.treasuryId()));
            for (com.newuniverse.nusmp.labyrinth.mega.rift.pocket.PocketLayout.Room r : rt.layout.rooms()) {
                StringBuilder doors = new StringBuilder();
                for (com.newuniverse.nusmp.labyrinth.mega.rift.pocket.PocketLayout.Door d : rt.layout.doors())
                    if (d.a() == r.id() || d.b() == r.id()) doors.append(' ').append(d.a() == r.id() ? d.b() : d.a()).append(d.sealed() ? "(SEALED)" : "");
                ok(s, text("roomRow", "  room %s %s at %s, half width %s, doors to%s", r.id(), r.kind(), where(slot, r.cx(rt.layout.grid()), com.newuniverse.nusmp.labyrinth.mega.rift.pocket.PocketPlan.Y0, r.cz(rt.layout.grid()), true), r.half(), doors));
                n++;
            }
            ok(s, text("roomsSwitch", "  %s", PocketSwitch.describe(s.getServer(), pk)));
            if (rt.sigilChest != null) ok(s, text("roomsSigil", "  Sigil chest at %s; treasury exit rift at %s.", rt.sigilChest.toShortString(), rt.shortcutExit == null ? "none" : rt.shortcutExit.toShortString()));
            if (mega != null && pk.phase == 1) for (String gap : com.newuniverse.nusmp.labyrinth.mega.rift.pocket.PocketWatchdog.missing(mega, rt, pk)) ok(s, text("roomsGap", "  MISSING: %s", gap));
            return n;
        }
        RealmPlan plan = PocketBuilder.planOf(pk);
        ok(s, text("roomsRealm", "Realm %s in slot %s: %s rooms, %s doors, vault %s.", pk.variant, slot, plan.rooms().size(), plan.doors().size(), pk.bossDead || pk.solved ? "open" : "barred"));
        for (RealmPlan.Room r : plan.rooms()) {
            StringBuilder doors = new StringBuilder();
            for (RealmPlan.Door d : plan.doors()) if (d.a().equals(r.id()) || d.b().equals(r.id())) doors.append(' ').append(d.a().equals(r.id()) ? d.b() : d.a());
            if (r.id().equals(RealmPlan.HALL)) doors.append(" vault(BARS)");
            ok(s, text("roomRowRealm", "  %s at %s to %s, height %s, doors to%s", r.id(), where(slot, r.x0(), 0, r.z0(), false), where(slot, r.x1(), r.h() - 1, r.z1(), false), r.h(), doors));
            n++;
        }
        ok(s, text("roomsSwitch", "  %s", PocketSwitch.describe(s.getServer(), pk)));
        if (mega != null) for (String gap : PocketBuilder.verify(mega, pk)) ok(s, text("roomsGap", "  MISSING: %s", gap));
        return n;
    }

    private static int switchInfo(CommandSourceStack s, int given) {
        int slot = slotOf(s, given);
        PocketRealm.Pocket pk = slot < 0 ? null : PocketRealm.get(s.getServer()).at(slot);
        if (pk == null) return fail(s, text("noslot", "Name a pocket slot (/multiverse mega rift pocket list), or stand in one."));
        return ok(s, text("switchRow", "%s", PocketSwitch.describe(s.getServer(), pk)));
    }

    /** Pulls the lever as an admin: the door opens at once, whatever the delay says. */
    private static int switchOpen(CommandSourceStack s, int given) {
        int slot = slotOf(s, given);
        ServerLevel mega = MegaDungeon.level(s.getServer());
        PocketRealm realm = PocketRealm.get(s.getServer());
        PocketRealm.Pocket pk = slot < 0 ? null : realm.at(slot);
        if (pk == null || mega == null) return fail(s, text("noslot", "Name a pocket slot (/multiverse mega rift pocket list), or stand in one."));
        if (pk.exclusive()) {
            PocketRuntime rt = PocketRuntime.of(pk);
            if (rt == null) return fail(s, text("notombslot", "There is no tomb in slot %s.", slot));
            return ExclusivePockets.openSeal(mega, realm, pk, rt, null, "lever") ? ok(s, text("sealOpenedLever", "Pulled the seal lever of the tomb in slot %s: the seal is open.", slot)) : fail(s, text("sealWasOpen", "The seal of slot %s is open already.", slot));
        }
        if (!realm.clear(mega, pk, "admin")) return fail(s, text("alreadyOpen", "The vault of slot %s is open already.", slot));
        return ok(s, text("leverOpened", "Pulled the release lever of the realm in slot %s: the vault is open.", slot));
    }

    /** Teleports the sender to the cell in front of the pocket's lever (they come back out through the exit rift, to where they stood). */
    private static int switchTp(CommandSourceStack s, int given) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        ServerPlayer p = s.getPlayerOrException();
        int slot = slotOf(s, given);
        ServerLevel mega = MegaDungeon.level(s.getServer());
        PocketRealm.Pocket pk = slot < 0 ? null : PocketRealm.get(s.getServer()).at(slot);
        BlockPos lever = PocketSwitch.leverAt(pk);
        if (pk == null || mega == null || lever == null) return fail(s, text("noslot", "Name a pocket slot (/multiverse mega rift pocket list), or stand in one."));
        if (pk.exclusive() && pk.phase == 0) return fail(s, text("forming", "The tomb in slot %s is still forming.", slot));
        mega.getChunk(lever.getX() >> 4, lever.getZ() >> 4);
        net.minecraft.core.Direction face = mega.getBlockState(lever).hasProperty(net.minecraft.world.level.block.LeverBlock.FACING) ? mega.getBlockState(lever).getValue(net.minecraft.world.level.block.LeverBlock.FACING) : net.minecraft.core.Direction.SOUTH;
        BlockPos stand = lever.relative(face).below();
        if (RiftRules.slotAt(p.getX(), p.getZ()) != slot || p.level() != mega) {
            CompoundTag r = new CompoundTag();
            r.putString("Dim", p.level().dimension().location().toString());
            r.putDouble("X", p.getX());
            r.putDouble("Y", p.getY());
            r.putDouble("Z", p.getZ());
            r.putFloat("Yaw", p.getYRot());
            r.putFloat("Pitch", p.getXRot());
            r.putInt("Slot", slot);
            p.getPersistentData().put(PocketRealm.RETURN, r);
        }
        float yaw = switch (face) { case NORTH -> 180f; case EAST -> -90f; case WEST -> 90f; default -> 0f; };       // looking at the lever: against the way it faces
        PocketRealm.move(p, mega, stand.getX() + 0.5, stand.getY(), stand.getZ() + 0.5, yaw, 10f);
        return ok(s, text("switchTp", "Stepped to the release lever of slot %s at %s.", slot, lever.toShortString()));
    }

    /** Reads a pocket back and puts right what is missing. */
    private static int repairPocket(CommandSourceStack s, int given) {
        int slot = slotOf(s, given);
        ServerLevel mega = MegaDungeon.level(s.getServer());
        PocketRealm realm = PocketRealm.get(s.getServer());
        PocketRealm.Pocket pk = slot < 0 ? null : realm.at(slot);
        if (pk == null || mega == null) return fail(s, text("noslot", "Name a pocket slot (/multiverse mega rift pocket list), or stand in one."));
        if (pk.exclusive()) {
            PocketRuntime rt = PocketRuntime.of(pk);
            if (rt == null || pk.phase != 1) return fail(s, text("notRepairable", "The tomb in slot %s is not standing (phase %s).", slot, pk.phase));
            java.util.List<String> gaps = com.newuniverse.nusmp.labyrinth.mega.rift.pocket.PocketWatchdog.missing(mega, rt, pk);
            rt.repairJob = new com.newuniverse.nusmp.labyrinth.mega.rift.pocket.PocketBuild(rt, true, pk.sealOpen);
            return ok(s, text("repairTomb", "Slot %s: %s thing(s) missing; a repair pass is running.", slot, gaps.size()));
        }
        java.util.List<String> gaps = realm.keepWhole(mega, pk, com.newuniverse.nusmp.labyrinth.mega.RegionTheme.GRAVITY);
        return ok(s, text("repairRealm", "Slot %s: %s thing(s) were missing; %s", slot, gaps.size(), gaps.isEmpty() ? "nothing to repair." : "repaired."));
    }

    private static MutableComponent text(String key, String english, Object... args) {
        return Component.translatableWithFallback("message.nusmp.mega.rift.cmd." + key, english, args);
    }

    private static int ok(CommandSourceStack s, MutableComponent c) {
        s.sendSuccess(() -> c, false);
        return 1;
    }

    private static int fail(CommandSourceStack s, MutableComponent c) {
        s.sendFailure(c);
        return 0;
    }

    private static int open(CommandSourceStack s, RiftType type, RiftRules.Variant variant) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        ServerPlayer p = s.getPlayerOrException();
        ServerLevel level = p.serverLevel();
        BlockPos at = p.blockPosition().relative(p.getDirection(), 3);
        long seed = level.getRandom().nextLong();
        if (variant != null) {                                               // a pocket of the wanted kind: the kind is rolled from the seed and the config's boss share, so look for a seed that gives it
            double share = RiftConfig.get(RiftConfig.BOSS_SHARE);
            int tries = 0;
            while (RiftRules.variantFor(seed, share) != variant && tries++ < 256) seed = level.getRandom().nextLong();
            if (RiftRules.variantFor(seed, share) != variant) return fail(s, text("novariant", "The config's bossShare (%s) allows no %s pocket.", share, variant.id()));
        }
        DimensionalRiftBlockEntity be = RiftService.open(level, at, type, seed);
        if (be == null) return fail(s, text("nospace", "There is no room for a rift three blocks ahead."));
        if (type == RiftType.SHORTCUT) {
            Sector here = Labyrinth.inMega(level) ? MegaData.get(level.getServer()).sectorAt(p.blockPosition()) : null;      // 1.94.0: a sector is read only in the dungeon (elsewhere the coordinates mean nothing)
            if (here == null) {
                be.close();
                return fail(s, text("nofloor", "A shortcut needs a floor of the Mega-Dungeon under you."));
            }
            RiftService.aimShortcut(MegaData.get(level.getServer()), here, be);
        }
        RiftService.announce(level, at, type, null);
        return ok(s, text("opened", "Opened a %s rift at %s.", type.id() + (variant == null ? "" : " (" + variant.id() + ")"), at.toShortString()));
    }

    private static String describe(ServerLevel level, DimensionalRiftBlockEntity be) {
        return be.type().id() + " at " + be.getBlockPos().toShortString() + ", age " + be.age() / 20 + " s, " + be.left() / 20 + " s left" + (be.type() == RiftType.SHORTCUT
                ? ", floor chain " + be.fromChain() + " to " + be.targetChain() + (be.stable() ? " (steady)" : " (forming, waited " + be.waited() / 20 + " s)") : "")
                + (be.slot() >= 0 ? ", pocket slot " + be.slot() : "") + (be.isExclusive() ? ", MEGA-RARE tear to the tomb " + be.pocketId() : "");
    }

    private static int list(CommandSourceStack s) {
        var l = RiftService.openRifts();
        if (l.isEmpty()) return ok(s, text("none", "No rift is open."));
        int n = 0;
        for (ServerLevel level : s.getServer().getAllLevels()) {
            for (BlockPos p : RiftService.openRifts(level)) {
                if (level.isLoaded(p) && level.getBlockEntity(p) instanceof DimensionalRiftBlockEntity be) {
                    ok(s, text("row2", "%s in %s", describe(level, be), level.dimension().location().toString()));
                    n++;
                }
            }
        }
        return n;
    }

    private static int info(CommandSourceStack s) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        ServerPlayer p = s.getPlayerOrException();
        ServerLevel level = p.serverLevel();
        DimensionalRiftBlockEntity be = RiftService.nearest(level, p.blockPosition(), 24);
        if (be == null) return fail(s, text("noneNear", "No open rift within 24 blocks."));
        ok(s, text("info", "%s; seed %s.", describe(level, be), be.seed()));
        if (be.slot() >= 0) {
            PocketRealm.Pocket pk = PocketRealm.get(s.getServer()).at(be.slot());
            if (pk == null) ok(s, text("infoGone", "Its pocket (slot %s) does not exist (any more): the next step in builds a new one.", be.slot()));
            else if (pk.seed != be.seed()) ok(s, text("infoOther", "Slot %s belongs to another rift now: the next step in builds a new pocket.", be.slot()));
            else ok(s, text("infoPocket", "Its pocket: %s on floor %s, %s.", pk.variant, pk.floor, pk.bossDead || pk.solved ? "vault open" : "vault barred"));
        }
        return 1;
    }

    private static int enter(CommandSourceStack s, ServerPlayer who) {
        ServerLevel level = who.serverLevel();
        DimensionalRiftBlockEntity be = RiftService.nearest(level, who.blockPosition(), 16);
        if (be == null) return fail(s, text("noneNear16", "No open rift within 16 blocks of %s.", who.getName()));
        RiftService.forceEnter(level, who, be);
        return ok(s, text("entered", "Sent %s through the %s rift at %s.", who.getName(), be.type().id(), be.getBlockPos().toShortString()));
    }

    private static int roll(CommandSourceStack s, RiftType kind) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        ServerPlayer p = s.getPlayerOrException();
        ServerLevel mega = MegaDungeon.level(s.getServer());
        if (mega == null || p.level() != mega) return fail(s, text("notmega", "Stand in the Mega-Dungeon to roll for a rift."));
        String why = RiftService.rollNow(mega, p, kind);
        return why == null ? ok(s, text("rolled", "A rift opened near you.")) : fail(s, text("rollno", "No rift opened: %s. (/multiverse mega rift why lists the conditions.)", why));
    }

    private static int why(CommandSourceStack s) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        ServerPlayer p = s.getPlayerOrException();
        ServerLevel mega = MegaDungeon.level(s.getServer());
        if (mega == null) return fail(s, text("nodim2", "The dungeon dimension is not loaded."));
        if (p.level() != mega) return fail(s, text("notmega", "Stand in the Mega-Dungeon to roll for a rift."));
        MegaData md = MegaData.get(s.getServer());
        Sector sec = md.sectorAt(p.blockPosition());
        RiftRules.RollGate gate = RiftService.gateFor(mega, p, false);
        ok(s, text("whyGate", "Spawner gate at your feet: %s (enabled=%s, on ground=%s, floor=%s, built=%s, safe room=%s, rifts within %s blocks=%s of %s).", gate.name().toLowerCase(java.util.Locale.ROOT),
                RiftConfig.get(RiftConfig.ENABLED), p.onGround(), sec == null ? "none" : sec.isFloor(), sec == null ? "none" : com.newuniverse.nusmp.labyrinth.mega.gen.SectorGenerator.playable(sec),
                MegaDungeon.inSafeZone(mega, p.blockPosition()), RiftRules.HINT_RADIUS, RiftService.nearCount(mega, p.blockPosition(), RiftRules.HINT_RADIUS), RiftConfig.get(RiftConfig.MAX_NEAR)));
        long next = p.getPersistentData().getLong("nusmp_rift_next");
        ok(s, text("whyRoll", "Next natural roll for you in %s s; per roll: pocket %s, shortcut %s (never on a boss floor); a rift stays %s s.", Math.max(0, (next - mega.getGameTime()) / 20),
                RiftConfig.get(RiftConfig.POCKET_CHANCE), RiftConfig.get(RiftConfig.SHORTCUT_CHANCE), RiftConfig.get(RiftConfig.RIFT_SECONDS)));
        if (sec != null && sec.isBossFloor()) ok(s, text("whyBoss", "This is a boss floor: only pocket rifts open here."));
        return 1;
    }

    private static int closeAll(CommandSourceStack s) {
        int n = 0;
        for (ServerLevel level : s.getServer().getAllLevels())
            for (BlockPos p : RiftService.openRifts(level)) {
                if (!level.isLoaded(p)) continue;
                BlockEntity be = level.getBlockEntity(p);
                if (be instanceof DimensionalRiftBlockEntity r) {
                    r.close();
                    n++;
                }
            }
        return ok(s, text("closed", "Closed %s rifts.", n));
    }

    private static int status(CommandSourceStack s) {
        ServerLevel mega = MegaDungeon.level(s.getServer());
        PocketRealm realm = PocketRealm.get(s.getServer());
        return ok(s, text("status", "Rifts: enabled=%s pockets=%s waypoint=%s; open now %s; pockets alive %s; jump %s-%s floors; pocket %s min.", RiftConfig.get(RiftConfig.ENABLED),
                RiftConfig.get(RiftConfig.POCKETS), RiftConfig.get(RiftConfig.WAYPOINT), RiftService.openRifts().size(), realm.pockets().size(), RiftConfig.get(RiftConfig.JUMP_MIN),
                RiftConfig.get(RiftConfig.JUMP_MAX), RiftConfig.get(RiftConfig.POCKET_MINUTES)).append(mega == null ? text("nodim", " (the dungeon dimension is not loaded)") : Component.empty())
                .append(text("statusMega", " Mega-rare: enabled=%s one in %s, pity %s to %s, %s tombs loaded, %s of %s tombs standing.", RiftConfig.get(RiftConfig.MEGA_ENABLED), RiftConfig.get(RiftConfig.MEGA_ONE_IN),
                        RiftConfig.get(RiftConfig.PITY_START), RiftConfig.get(RiftConfig.PITY_HARD), PocketDefs.count(), realm.exclusiveCount(), RiftConfig.get(RiftConfig.EXCLUSIVE_MAX))));
    }

    private static int pockets(CommandSourceStack s) {
        PocketRealm realm = PocketRealm.get(s.getServer());
        if (realm.pockets().isEmpty()) return ok(s, text("nopockets", "No pocket realm is alive."));
        ServerLevel mega = MegaDungeon.level(s.getServer());
        for (PocketRealm.Pocket p : realm.pockets().values()) {
            if (p.exclusive()) {                                                                      // 1.99.0: an exclusive tomb
                ok(s, text("tomb", "slot %s: TOMB %s (floor %s), phase %s, %s inside, %s s left; boss %s, seal %s, relic %s", p.slot, p.pocketId, p.originFloor, p.phase, mega == null ? 0 : PocketRealm.playersIn(mega, p.slot).size(),
                        mega == null ? 0 : Math.max(0, (p.expiresAt - mega.getGameTime()) / 20), p.bossDead ? "dead" : "alive", p.sealOpen ? "open" : p.sealHp + " left", p.relicTaken ? "taken" : "on its pedestal"));
                continue;
            }
            ok(s, text("pocket", "slot %s: %s (floor %s), %s inside, %s s left%s", p.slot, p.variant, p.floor, mega == null ? 0 : PocketRealm.playersIn(mega, p.slot).size(),
                    mega == null ? 0 : Math.max(0, (p.expiresAt - mega.getGameTime()) / 20), p.bossDead || p.solved ? ", vault open" : ""));
        }
        return realm.pockets().size();
    }

    private static int collapse(CommandSourceStack s, int slot) {
        ServerLevel mega = MegaDungeon.level(s.getServer());
        if (mega == null || !PocketRealm.get(s.getServer()).collapseSlot(mega, slot)) return fail(s, text("nosuch", "There is no pocket in slot %s.", slot));
        return ok(s, text("collapsed", "Collapsed the pocket in slot %s.", slot));
    }

    /** Skips a pocket: its vault opens as if the boss had fallen or the plates had been pressed in order. */
    private static int clearPocket(CommandSourceStack s, int slot) {
        ServerLevel mega = MegaDungeon.level(s.getServer());
        PocketRealm realm = PocketRealm.get(s.getServer());
        PocketRealm.Pocket pk = realm.at(slot);
        if (mega == null || pk == null) return fail(s, text("nosuch", "There is no pocket in slot %s.", slot));
        if (!realm.clear(mega, pk, "admin")) return fail(s, text("alreadyOpen", "The vault of slot %s is open already.", slot));
        return ok(s, text("cleared", "Opened the vault of the pocket in slot %s.", slot));
    }

    /** Takes the sender into the pocket of a slot (they come back out through its exit rift to where they stood). */
    private static int visit(CommandSourceStack s, int slot) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        ServerPlayer p = s.getPlayerOrException();
        ServerLevel mega = MegaDungeon.level(s.getServer());
        PocketRealm.Pocket pk = PocketRealm.get(s.getServer()).at(slot);
        if (mega == null || pk == null) return fail(s, text("nosuch", "There is no pocket in slot %s.", slot));
        CompoundTag r = new CompoundTag();
        r.putString("Dim", p.level().dimension().location().toString());
        r.putDouble("X", p.getX());
        r.putDouble("Y", p.getY());
        r.putDouble("Z", p.getZ());
        r.putFloat("Yaw", p.getYRot());
        r.putFloat("Pitch", p.getXRot());
        r.putInt("Slot", slot);
        p.getPersistentData().put(PocketRealm.RETURN, r);
        mega.getChunk(pk.entry.getX() >> 4, pk.entry.getZ() >> 4);
        PocketRealm.move(p, mega, pk.entry.getX() + 0.5, pk.entry.getY(), pk.entry.getZ() + 0.5, -90f, 0f);
        return ok(s, text("visited", "Stepped into the pocket in slot %s.", slot));
    }

    // ---------------------------------------------------------------- 1.99.0: the exclusive tombs
    /** Opens a mega-rare tear three blocks ahead of the sender, to the tomb {@code id} (empty: a random one). */
    private static int openMega(CommandSourceStack s, String id) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        ServerPlayer p = s.getPlayerOrException();
        ServerLevel level = p.serverLevel();
        PocketDef def = MegaRifts.pickForced(id, level.getRandom());
        if (def == null) return fail(s, text("notomb", "There is no tomb '%s' (try /multiverse mega rift pocket types).", id));
        BlockPos at = p.blockPosition().relative(p.getDirection(), 3);
        DimensionalRiftBlockEntity be = RiftService.open(level, at, RiftType.POCKET, level.getRandom().nextLong());
        if (be == null) return fail(s, text("nospace", "There is no room for a rift three blocks ahead."));
        MegaRifts.aim(be, def);
        MegaRifts.announce(level, at, def, null);
        return ok(s, text("openedMega", "Opened a MEGA-RARE rift to %s at %s.", def.id(), at.toShortString()));
    }

    private static int types(CommandSourceStack s) {
        if (PocketDefs.count() == 0) return fail(s, text("notombs", "No exclusive tomb is loaded."));
        for (PocketDef d : PocketDefs.all())
            ok(s, text("tombRow", "%s: %s (%s), %s rooms, boss %s, relics %s, weight %s, from depth %s", d.id(), d.name(), d.series(), d.layout().rooms(), d.boss(), d.relics().all(), d.weight(), d.minDepth()));
        return PocketDefs.count();
    }

    /** Raises a fresh tomb of {@code id} and takes the sender into it. */
    private static int make(CommandSourceStack s, String id) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        ServerPlayer p = s.getPlayerOrException();
        if (PocketDefs.get(id) == null) return fail(s, text("notomb", "There is no tomb '%s' (try /multiverse mega rift pocket types).", id));
        return RiftAdmin.enter(p, id) ? ok(s, text("made", "Raised the tomb %s and stepped in.", id)) : fail(s, text("nomake", "The tomb %s could not be raised (you are in a pocket already, no cell is free, or the cap on tombs is reached).", id));
    }

    /** Opens the seal of the tomb in a slot as if it had been unravelled. */
    private static int sealOpen(CommandSourceStack s, int slot) {
        ServerLevel mega = MegaDungeon.level(s.getServer());
        PocketRealm realm = PocketRealm.get(s.getServer());
        PocketRealm.Pocket pk = realm.at(slot);
        PocketRuntime rt = pk == null ? null : PocketRuntime.of(pk);
        if (mega == null || rt == null) return fail(s, text("notombslot", "There is no tomb in slot %s.", slot));
        return ExclusivePockets.openSeal(mega, realm, pk, rt, null, "magic") ? ok(s, text("sealOpened", "Opened the seal of the tomb in slot %s.", slot)) : fail(s, text("sealWasOpen", "The seal of slot %s is open already.", slot));
    }

    /** Starts the collapse of the tomb in a slot as if its primary relic had been taken. */
    private static int fall(CommandSourceStack s, int slot) {
        ServerLevel mega = MegaDungeon.level(s.getServer());
        PocketRealm realm = PocketRealm.get(s.getServer());
        PocketRealm.Pocket pk = realm.at(slot);
        PocketRuntime rt = pk == null ? null : PocketRuntime.of(pk);
        if (mega == null || rt == null) return fail(s, text("notombslot", "There is no tomb in slot %s.", slot));
        return ExclusivePockets.startCollapse(mega, realm, pk, rt, mega.getGameTime()) ? ok(s, text("falling", "The tomb in slot %s begins to fall (%s s).", slot, rt.def.collapseSeconds())) : fail(s, text("fallingAlready", "The tomb in slot %s is already falling or gone.", slot));
    }

    /** What a player's next rift is worth: the chance that it is mega-rare and the pity behind it. */
    private static int tier(CommandSourceStack s, ServerPlayer p) {
        int floor = 1;
        if (Labyrinth.inMega(p.level())) {
            Sector here = MegaData.get(s.getServer()).sectorAt(p.blockPosition());
            if (here != null) floor = Math.abs(here.firstFloor());
        }
        double chance = MegaRifts.chance(p, floor);
        return ok(s, text("tier", "%s: pity %s (soft from %s, certain at %s); the next rift near them is mega-rare with chance %s (about one in %s) at floor %s.", p.getName(), MegaRifts.pity(p), RiftConfig.get(RiftConfig.PITY_START),
                RiftConfig.get(RiftConfig.PITY_HARD), String.format(java.util.Locale.ROOT, "%.4f", chance), chance <= 0 ? "never" : Math.round(1.0 / chance), floor));
    }

    private static int pity(CommandSourceStack s, ServerPlayer p, int count) {
        MegaRifts.setPity(p, count);
        return ok(s, text("pitySet", "Set the pity of %s to %s.", p.getName(), count));
    }

    private static int show(CommandSourceStack s, ServerPlayer p) {
        Waypoint w = WaypointService.get(p);
        if (w == null) return fail(s, text("wp.none", "%s has no waypoint.", p.getName()));
        return ok(s, text("wp.show", "%s: floor %s (level %s, sector %s) at %s %s %s in %s, saved at tick %s.", p.getName(), w.floor(), w.level(), w.sector(), Math.round(w.x()), Math.round(w.y()),
                Math.round(w.z()), w.dimension(), w.savedAt()));
    }

    private static int save(CommandSourceStack s, ServerPlayer p) {
        return WaypointService.save(p) ? ok(s, text("wp.saved", "Saved a waypoint for %s.", p.getName())) : fail(s, text("wp.nosave", "%s is not standing anywhere a waypoint is kept.", p.getName()));
    }

    private static int clear(CommandSourceStack s, ServerPlayer p) {
        WaypointService.clear(p);
        return ok(s, text("wp.cleared", "Cleared the waypoint of %s.", p.getName()));
    }

    private static int back(CommandSourceStack s, ServerPlayer p) {
        return WaypointService.returnToWaypoint(p) ? ok(s, text("wp.back", "Sent %s to their waypoint.", p.getName())) : fail(s, text("wp.noback", "%s could not be sent to a waypoint.", p.getName()));
    }
}
