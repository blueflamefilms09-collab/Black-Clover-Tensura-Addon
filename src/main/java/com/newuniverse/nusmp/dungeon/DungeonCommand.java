package com.newuniverse.nusmp.dungeon;

import com.mojang.brigadier.arguments.FloatArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import com.newuniverse.nusmp.dungeon.wild.WildSpawn;
import com.newuniverse.nusmp.dungeon.wild.Wildlife;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.ResourceLocationArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.phys.AABB;

import java.util.Locale;
import java.util.concurrent.CompletableFuture;

/** /multiverse dungeon ...: the way in and out for admins, and a look at the ecosystem under your feet for everyone. */
public final class DungeonCommand {
    private DungeonCommand() {}

    private static int ok(CommandSourceStack s, String msg) { s.sendSuccess(() -> Component.literal(msg), false); return 1; }

    private static int fail(CommandSourceStack s, String msg) { s.sendFailure(Component.literal(msg)); return 0; }

    public static LiteralArgumentBuilder<CommandSourceStack> build() {
        LiteralArgumentBuilder<CommandSourceStack> root = Commands.literal("dungeon");
        root.then(Commands.literal("enter").requires(s -> s.hasPermission(2)).executes(ctx -> {
            ServerPlayer p = ctx.getSource().getPlayerOrException();
            return LivingDungeon.enter(p, p.blockPosition()) ? 1 : fail(ctx.getSource(), "The Dungeon is not there.");
        }));
        root.then(Commands.literal("exit").executes(ctx -> {
            ServerPlayer p = ctx.getSource().getPlayerOrException();
            if (!LivingDungeon.in(p)) return fail(ctx.getSource(), "You are not in the Dungeon.");
            LivingDungeon.leave(p);
            return 1;
        }));
        root.then(Commands.literal("maw").requires(s -> s.hasPermission(2))
                .then(Commands.literal("open").executes(ctx -> {
                    ServerPlayer p = ctx.getSource().getPlayerOrException();
                    return p.level() instanceof ServerLevel sl && DungeonMaw.open(sl, p.blockPosition()) ? 1 : fail(ctx.getSource(), "No ground to tear open here.");
                }))
                .then(Commands.literal("close").executes(ctx -> ok(ctx.getSource(), DungeonMaw.closeAll(ctx.getSource().getServer()) + " Maw(s) closed."))));
        root.then(Commands.literal("status").executes(ctx -> {
            ServerPlayer p = ctx.getSource().getPlayerOrException();
            if (!LivingDungeon.in(p)) {
                DungeonSave s = DungeonSave.get(ctx.getSource().getServer());
                return ok(ctx.getSource(), "The Dungeon lies below. " + s.maws.size() + " Maw(s) open. Golden City seal: " + (s.cityBreached ? "broken" : "intact") + ".");
            }
            ServerLevel level = p.serverLevel();
            DungeonTier tier = DungeonTier.at(level, p.blockPosition());
            EcosystemData e = LivingDungeon.eco(level, p.blockPosition());
            return ok(ctx.getSource(), tier.label + (e == null ? "" : String.format(": magicule %.0f, biomass %.0f, oxygen %.0f%s", e.magicule, e.biomass, e.oxygen, e.thinAir() ? " (thin air)" : "")));
        }));
        root.then(Commands.literal("ecosystem").requires(s -> s.hasPermission(2))
                .then(Commands.literal("magicule").then(Commands.argument("value", FloatArgumentType.floatArg(0, EcosystemData.MAGICULE_MAX)).executes(ctx -> set(ctx.getSource(), 0, FloatArgumentType.getFloat(ctx, "value")))))
                .then(Commands.literal("biomass").then(Commands.argument("value", FloatArgumentType.floatArg(0, EcosystemData.BIOMASS_MAX)).executes(ctx -> set(ctx.getSource(), 1, FloatArgumentType.getFloat(ctx, "value")))))
                .then(Commands.literal("oxygen").then(Commands.argument("value", FloatArgumentType.floatArg(0, EcosystemData.OXYGEN_MAX)).executes(ctx -> set(ctx.getSource(), 2, FloatArgumentType.getFloat(ctx, "value"))))));
        // 1.19.0: the Dungeon's creatures are the real mobs of Tensura; the 1.18.0 custom ones stay under "custom"
        root.then(Commands.literal("spawn").requires(s -> s.hasPermission(2))
                .then(Commands.literal("slime").executes(ctx -> spawnRoster(ctx.getSource(), "tensura:slime")))
                .then(Commands.literal("armor").executes(ctx -> spawnRoster(ctx.getSource(), "tensura:armorsaurus")))
                .then(Commands.literal("direwolf").executes(ctx -> spawnRoster(ctx.getSource(), "tensura:direwolf")))
                .then(Commands.literal("basilisk").executes(ctx -> spawnRoster(ctx.getSource(), "tensura:basilisk")))
                .then(Commands.literal("mob").then(Commands.argument("entity_type", ResourceLocationArgument.id())
                        .suggests((c, b) -> suggestRoster(b))
                        .executes(ctx -> spawnRoster(ctx.getSource(), ResourceLocationArgument.getId(ctx, "entity_type").toString()))))
                .then(Commands.literal("custom")
                        .then(Commands.literal("slime").executes(ctx -> spawn(ctx.getSource(), DungeonEntities.SLIME.get())))
                        .then(Commands.literal("armor").executes(ctx -> spawn(ctx.getSource(), DungeonEntities.LIVING_ARMOR.get())))
                        .then(Commands.literal("direwolf").executes(ctx -> spawn(ctx.getSource(), DungeonEntities.DIREWOLF.get())))
                        .then(Commands.literal("basilisk").executes(ctx -> spawn(ctx.getSource(), DungeonEntities.BASILISK.get())))));
        root.then(Commands.literal("census").requires(s -> s.hasPermission(2)).executes(ctx -> {
            ServerPlayer p = ctx.getSource().getPlayerOrException();
            int[] n = new int[4];
            int legends = 0;
            float mass = 0;
            for (Mob m : p.serverLevel().getEntitiesOfClass(Mob.class, new AABB(p.blockPosition()).inflate(96), x -> Wildlife.tierOf(x) > 0)) {
                n[Wildlife.tierOf(m)]++;
                mass += Wildlife.massOf(m);
                if (Wildlife.isLegendary(m)) legends++;
            }
            return ok(ctx.getSource(), "Within 96 blocks: " + n[1] + " tier 1 (slimes, scavengers, grazers), " + n[2] + " tier 2 (armoured, mid predators), " + n[3] + " apex predators"
                    + (legends > 0 ? " (" + legends + " legendary)" : "") + "; biomass " + Math.round(mass) + ".");
        }));
        return root;
    }

    /** Suggests the entity types of the roster tags (nusmp:dungeon/tier1, tier2, tier3, legendary). */
    private static CompletableFuture<Suggestions> suggestRoster(SuggestionsBuilder b) {
        String typed = b.getRemaining().toLowerCase(Locale.ROOT);
        for (String id : Wildlife.rosterIds()) if (typed.isEmpty() || id.startsWith(typed) || id.substring(id.indexOf(':') + 1).startsWith(typed)) b.suggest(id);
        return b.buildFuture();
    }

    /** Spawns a creature of the Dungeon's roster (a real mob of Tensura, or of Ice and Fire) at the executor, with a clear message if its mod is missing. */
    private static int spawnRoster(CommandSourceStack src, String id) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        ServerPlayer p = src.getPlayerOrException();
        EntityType<?> type = Wildlife.typeOf(id);
        if (type == null) {
            String mod = id.contains(":") ? id.substring(0, id.indexOf(':')) : "minecraft";
            return fail(src, id + " is not registered" + (mod.equals("minecraft") ? "." : ": is the mod \"" + mod + "\" installed?"));
        }
        Mob m = WildSpawn.spawnType(p.serverLevel(), type, p.blockPosition(), p.getRandom(), MobSpawnType.COMMAND, false);
        if (m == null) return fail(src, "Could not create " + id + " (it is not a mob, or its own setup failed: see the log).");
        int tier = Wildlife.tierOf(m);
        return ok(src, "Spawned " + id + (tier > 0 ? " (tier " + tier + (Wildlife.isLegendary(m) ? ", legendary" : "") + ")." : " (not in the roster tags)."));
    }

    private static int spawn(CommandSourceStack src, EntityType<? extends DungeonCreature> type) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        ServerPlayer p = src.getPlayerOrException();
        DungeonCreature m = type.create(p.serverLevel());
        if (m == null) return fail(src, "Could not create it.");
        m.moveTo(p.getX(), p.getY(), p.getZ(), p.getYRot(), 0f);
        m.finalizeSpawn(p.serverLevel(), p.serverLevel().getCurrentDifficultyAt(p.blockPosition()), net.minecraft.world.entity.MobSpawnType.COMMAND, null);
        p.serverLevel().addFreshEntity(m);
        return ok(src, "Spawned.");
    }

    private static int set(CommandSourceStack src, int which, float v) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        ServerPlayer p = src.getPlayerOrException();
        if (!LivingDungeon.in(p)) return fail(src, "Only inside the Dungeon.");
        EcosystemData e = LivingDungeon.eco(p.serverLevel(), p.blockPosition());
        if (e == null) return fail(src, "This chunk is not loaded.");
        switch (which) { case 0 -> e.magicule = v; case 1 -> e.biomass = v; default -> e.oxygen = v; }
        e.clamp();
        LivingDungeon.touch(p.serverLevel(), p.blockPosition());
        return ok(src, "Set.");
    }
}
