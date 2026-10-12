package com.newuniverse.nusmp.dungeon;

import com.mojang.brigadier.arguments.FloatArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.phys.AABB;

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
        root.then(Commands.literal("spawn").requires(s -> s.hasPermission(2))
                .then(Commands.literal("slime").executes(ctx -> spawn(ctx.getSource(), DungeonEntities.SLIME.get())))
                .then(Commands.literal("armor").executes(ctx -> spawn(ctx.getSource(), DungeonEntities.LIVING_ARMOR.get())))
                .then(Commands.literal("direwolf").executes(ctx -> spawn(ctx.getSource(), DungeonEntities.DIREWOLF.get())))
                .then(Commands.literal("basilisk").executes(ctx -> spawn(ctx.getSource(), DungeonEntities.BASILISK.get()))));
        root.then(Commands.literal("census").requires(s -> s.hasPermission(2)).executes(ctx -> {
            ServerPlayer p = ctx.getSource().getPlayerOrException();
            int[] n = new int[4];
            float mass = 0;
            for (DungeonCreature m : p.serverLevel().getEntitiesOfClass(DungeonCreature.class, new AABB(p.blockPosition()).inflate(96))) { n[m.tier()]++; mass += m.weight(); }
            return ok(ctx.getSource(), "Within 96 blocks: " + n[1] + " slimes, " + n[2] + " Living Armors, " + n[3] + " apex predators; biomass " + Math.round(mass) + ".");
        }));
        return root;
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
