package com.newuniverse.nusmp.labyrinth.mega.foundry;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.newuniverse.nusmp.labyrinth.mega.MegaDungeon;
import com.newuniverse.nusmp.labyrinth.mega.Polarity;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;

import java.util.List;

/**
 * Why: 400 mobs that nobody can playtest by waiting need the admin's hands on them, in the style of the hub and rift commands: {@code /multiverse mega foundry list [holy|abyssal
 * [tier]]} shows the roster by road and tier (and, for one tier, every name with its faction, archetype and source), {@code spawn <id>} makes one in front of you (with the Dungeon
 * Master's tag, on the floor you stand on, or the middle of its tier outside the dungeon), {@code count} shows the roster and the live mobs, {@code verify} runs the same promise
 * check as the self-test and the JUnit test (200 + 200, 40 per tier, sources, abilities). Permission 2; every text goes through a lang key with an English fallback.
 */
public final class FoundryCommand {
    private FoundryCommand() {}

    public static LiteralArgumentBuilder<CommandSourceStack> command() {
        return Commands.literal("foundry").requires(s -> s.hasPermission(2))
                .then(Commands.literal("list").executes(c -> list(c.getSource(), null, 0))
                        .then(road("holy", Polarity.HOLY)).then(road("abyssal", Polarity.ABYSSAL)))
                .then(Commands.literal("spawn").then(Commands.argument("id", StringArgumentType.word())
                        .suggests((c, b) -> SharedSuggestionProvider.suggest(FoundryData.roster().all().stream().map(FoundryDef::id).toList(), b))
                        .executes(c -> spawn(c.getSource(), StringArgumentType.getString(c, "id")))))
                .then(Commands.literal("count").executes(c -> count(c.getSource())))
                .then(Commands.literal("verify").executes(c -> verify(c.getSource())));
    }

    private static LiteralArgumentBuilder<CommandSourceStack> road(String name, Polarity p) {
        return Commands.literal(name).executes(c -> list(c.getSource(), p, 0))
                .then(Commands.argument("tier", IntegerArgumentType.integer(1, FoundryScaling.TIERS)).executes(c -> list(c.getSource(), p, IntegerArgumentType.getInteger(c, "tier"))));
    }

    private static MutableComponent text(String key, String english, Object... args) {
        return Component.translatableWithFallback("message.nusmp.mega.foundry.cmd." + key, english, args);
    }

    private static int ok(CommandSourceStack s, MutableComponent c) {
        s.sendSuccess(() -> c, false);
        return 1;
    }

    private static int fail(CommandSourceStack s, MutableComponent c) {
        s.sendFailure(c);
        return 0;
    }

    private static int list(CommandSourceStack s, Polarity road, int tier) {
        FoundryRoster r = FoundryData.roster();
        if (r.size() == 0) return fail(s, text("empty", "The foundry roster is empty (no data loaded)."));
        if (road == null) {
            for (Polarity p : Polarity.values()) {
                StringBuilder row = new StringBuilder();
                for (int t = 1; t <= FoundryScaling.TIERS; t++) row.append(" t").append(t).append('=').append(r.of(p, t).size());
                ok(s, text("road", "%s: %s mobs,%s", p.id(), r.count(p), row.toString()));
            }
            return r.size();
        }
        int from = tier == 0 ? 1 : tier, to = tier == 0 ? FoundryScaling.TIERS : tier;
        int n = 0;
        for (int t = from; t <= to; t++) {
            List<FoundryDef> l = r.of(road, t);
            if (tier == 0) {
                ok(s, text("tier", "%s tier %s: %s mobs", road.id(), t, l.size()));
                n += l.size();
                continue;
            }
            for (FoundryDef d : l) {
                ok(s, text("row", "%s (%s): %s, %s, %s [%s]", d.name(), d.id(), d.faction(), d.archetype(), String.join("+", d.abilities()), d.source()));
                n++;
            }
        }
        return n;
    }

    private static int spawn(CommandSourceStack s, String id) throws CommandSyntaxException {
        FoundryDef d = FoundryData.roster().byId(id);
        if (d == null) return fail(s, text("nosuch", "There is no foundry mob '%s'.", id));
        ServerPlayer p = s.getPlayerOrException();
        ServerLevel level = p.serverLevel();
        FoundryMobEntity e = FoundryRegistry.MOB.get().create(level);
        if (e == null) return fail(s, text("nocreate", "The mob could not be created."));
        BlockPos at = p.blockPosition().relative(p.getDirection(), 3);
        int floor = MegaDungeon.in(level) ? MegaDungeon.floorAt(level, p.blockPosition()) : 0;
        e.moveTo(at.getX() + 0.5, at.getY(), at.getZ() + 0.5, p.getYRot() + 180f, 0f);
        e.setFoundry(d, floor);
        e.makeDungeonMonster();
        level.addFreshEntity((Entity) e);
        return ok(s, text("spawned", "Spawned %s (tier %s, %s road) at %s.", d.name(), d.tier(), d.road().id(), at.toShortString()));
    }

    private static int count(CommandSourceStack s) {
        FoundryRoster r = FoundryData.roster();
        FoundryLibrary lib = FoundryData.library();
        int live = 0;
        for (ServerLevel level : s.getServer().getAllLevels()) for (Entity e : level.getAllEntities()) if (e instanceof FoundryMobEntity) live++;
        return ok(s, text("count", "Foundry: %s mobs (%s holy, %s abyssal), %s sources, %s abilities, %s archetypes; %s alive now; weight %s%%, enabled %s.", r.size(), r.count(Polarity.HOLY),
                r.count(Polarity.ABYSSAL), FoundryVerify.sourceCount(r), lib.abilities().size(), lib.archetypes().size(), live, FoundryConfig.get(FoundryConfig.WEIGHT),
                FoundryConfig.get(FoundryConfig.ENABLED)));
    }

    private static int verify(CommandSourceStack s) {
        List<String> bad = FoundryVerify.check(FoundryData.roster(), FoundryData.library(), true);
        if (bad.isEmpty()) return ok(s, text("verify.ok", "The foundry roster keeps its promises: 200 + 200 mobs, 40 per tier, sources and abilities all there."));
        for (String b : bad.subList(0, Math.min(20, bad.size()))) fail(s, text("verify.bad", "%s", b));
        return fail(s, text("verify.count", "%s problems (the first %s shown).", bad.size(), Math.min(20, bad.size())));
    }
}
