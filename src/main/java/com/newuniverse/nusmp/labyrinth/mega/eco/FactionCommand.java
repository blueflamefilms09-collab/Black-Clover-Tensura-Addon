package com.newuniverse.nusmp.labyrinth.mega.eco;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.newuniverse.nusmp.labyrinth.mega.MegaData;
import com.newuniverse.nusmp.labyrinth.mega.MegaDungeon;
import com.newuniverse.nusmp.labyrinth.mega.Room;
import com.newuniverse.nusmp.labyrinth.mega.Sector;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.Vec3;

import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.TreeSet;

/**
 * Why: the owner's hands on the faction layer, attached by the lead under {@code /multiverse mega} (permission 2): {@code faction list} shows every
 * faction with its rivals, allies and how many creature types its tag lists in this game (the quick check that a datapack or an optional mod did
 * what was meant), {@code faction of} names the faction of the creature you are looking at and whether the turf rules apply to it (a boss or a
 * servant sits out, and the command says which), {@code faction room} names the native faction of the room under your feet, and
 * {@code faction set <faction|none>} changes it, because the only way to test a quarrel is to stand two factions in one hall. All text goes through
 * lang keys {@code message.nusmp.mega.mec.cmd.*} with an English fallback, so the command reads fine even before the lang fragment is merged.
 */
public final class FactionCommand {
    private FactionCommand() {}

    private static final double LOOK_DISTANCE = 32.0;

    /** The {@code faction} node: {@code list | of | room | set <faction|none>}. */
    public static LiteralArgumentBuilder<CommandSourceStack> command() {
        return Commands.literal("faction").requires(s -> s.hasPermission(2))
                .then(Commands.literal("list").executes(ctx -> list(ctx.getSource())))
                .then(Commands.literal("of").executes(ctx -> of(ctx.getSource())))
                .then(Commands.literal("room").executes(ctx -> room(ctx.getSource())))
                .then(Commands.literal("set").then(Commands.argument("faction", StringArgumentType.word())
                        .suggests((ctx, builder) -> SharedSuggestionProvider.suggest(withNone(), builder))
                        .executes(ctx -> set(ctx.getSource(), StringArgumentType.getString(ctx, "faction")))));
    }

    private static List<String> withNone() {
        List<String> ids = FactionRegistry.graph().ids();
        ids.add("none");
        return ids;
    }

    private static MutableComponent text(String key, String english, Object... args) {
        return Component.translatableWithFallback("message.nusmp.mega.mec.cmd." + key, english, args);
    }

    private static int ok(CommandSourceStack s, MutableComponent c) {
        s.sendSuccess(() -> c, false);
        return 1;
    }

    private static int fail(CommandSourceStack s, MutableComponent c) {
        s.sendFailure(c);
        return 0;
    }

    private static MutableComponent names(Set<String> ids) {
        if (ids.isEmpty()) return text("nobody", "nobody");
        MutableComponent out = Component.empty();
        boolean first = true;
        for (String id : new TreeSet<>(ids)) {
            if (!first) out.append(", ");
            out.append(FactionRegistry.name(id));
            first = false;
        }
        return out;
    }

    private static int list(CommandSourceStack src) {
        FactionGraph g = FactionRegistry.graph();
        if (g.size() == 0) return fail(src, text("list.empty", "No faction is loaded (data/<namespace>/mega/factions/*.json)."));
        ok(src, text("list.head", "%s Mega-Dungeon factions (turf wars %s):", g.size(), EcoConfig.get(EcoConfig.TURF_WARS) ? "on" : "off"));
        for (String id : g.ids()) {
            FactionDef d = g.def(id);
            ok(src, text("list.line", "%s: rivals %s; allies %s; %s creature types in #%s%s",
                    FactionRegistry.name(id), names(g.rivalsOf(id)), names(g.alliesOf(id)), FactionRegistry.memberCount(id), d.tag(), d.territorial() ? " (territorial)" : ""));
        }
        return g.size();
    }

    private static int of(CommandSourceStack src) throws CommandSyntaxException {
        ServerPlayer p = src.getPlayerOrException();
        Vec3 from = p.getEyePosition(), to = from.add(p.getLookAngle().scale(LOOK_DISTANCE));
        AABB box = p.getBoundingBox().expandTowards(p.getLookAngle().scale(LOOK_DISTANCE)).inflate(1.0);
        EntityHitResult hit = ProjectileUtil.getEntityHitResult(p, from, to, box, e -> !e.isSpectator() && e.isPickable() && e != p, LOOK_DISTANCE * LOOK_DISTANCE);
        if (hit == null) return fail(src, text("of.nothing", "No creature in your line of sight within %s blocks.", (int) LOOK_DISTANCE));
        Entity target = hit.getEntity();
        String id = BuiltInRegistries.ENTITY_TYPE.getKey(target.getType()).toString();
        String faction = FactionRegistry.factionOf(target);
        if (faction == null) return ok(src, text("of.none", "%s (%s) belongs to no faction.", target.getDisplayName(), id));
        ok(src, text("of.faction", "%s (%s) is of the %s.", target.getDisplayName(), id, FactionRegistry.name(faction)));
        if (target instanceof Mob mob) {
            String why = TurfEligibility.why(mob);
            ok(src, why == null ? text("of.takes", "The turf rules apply to it.") : text("of.sits", "The turf rules skip it: it is a %s.", why));
            ok(src, TurfWars.hasGoal(mob) ? text("of.goal.yes", "It carries the turf-war goal.") : text("of.goal.no", "It carries no turf-war goal (it joined before the wars were on, its goal selector cannot be read, or it is a boss or a servant)."));
        }
        return 1;
    }

    private static int room(CommandSourceStack src) throws CommandSyntaxException {
        ServerPlayer p = src.getPlayerOrException();
        ServerLevel level = p.serverLevel();
        BlockPos pos = p.blockPosition();
        Sector sector = MegaDungeon.sectorAt(level, pos);
        Room room = sector == null ? null : sector.roomAt(pos);
        if (room == null) return fail(src, text("room.none", "You are not inside a room of a Mega-Dungeon sector."));
        if (room.faction == null) return ok(src, text("room.unowned", "Sector %s, room #%s (%s, %s): no native faction (safe rooms and corridors have none).", sector.id, room.id, room.template, room.kind));
        return ok(src, text("room.line", "Sector %s, room #%s (%s, %s): native to the %s.", sector.id, room.id, room.template, room.kind, FactionRegistry.name(room.faction)));
    }

    private static int set(CommandSourceStack src, String arg) throws CommandSyntaxException {
        ServerPlayer p = src.getPlayerOrException();
        ServerLevel level = p.serverLevel();
        BlockPos pos = p.blockPosition();
        Sector sector = MegaDungeon.sectorAt(level, pos);
        Room room = sector == null ? null : sector.roomAt(pos);
        if (room == null) return fail(src, text("room.none", "You are not inside a room of a Mega-Dungeon sector."));
        String id = arg.trim().toLowerCase(Locale.ROOT);
        if (id.equals("none")) {
            room.faction = null;
            MegaData.get(src.getServer()).setDirty();
            return ok(src, text("set.cleared", "Room #%s has no native faction now.", room.id));
        }
        if (!FactionRegistry.graph().knows(id)) {
            return fail(src, text("set.unknown", "'%s' is not a faction. Known: %s", arg, String.join(", ", FactionRegistry.graph().ids())));
        }
        room.faction = id;
        MegaData.get(src.getServer()).setDirty();
        return ok(src, text("set.ok", "Room #%s is now native to the %s.", room.id, FactionRegistry.name(id)));
    }
}
