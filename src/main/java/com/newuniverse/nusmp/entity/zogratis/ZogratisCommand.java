package com.newuniverse.nusmp.entity.zogratis;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.registries.DeferredHolder;

/**
 * {@code /multiverse boss dante | vanica | zenon | lucius | triad} (permission 2): spawns the boss at the executor with its arena
 * there. {@code triad} places Dante, Vanica and Zenon in a ring of 8 blocks around the executor, facing them. The multiverse
 * command calls {@link #addTo} with its "boss" node.
 */
public final class ZogratisCommand {
    private ZogratisCommand() {}

    public static void addTo(LiteralArgumentBuilder<CommandSourceStack> bossNode) {
        bossNode.then(single("dante", ZogratisEntities.DANTE, "Dante Zogratis descends, bearing Lucifero's gravity. The arena is here."))
                .then(single("vanica", ZogratisEntities.VANICA, "Vanica Zogratis descends, bearing Megicula's curse. The arena is here."))
                .then(single("zenon", ZogratisEntities.ZENON, "Zenon Zogratis descends, bearing Beelzebub's space. The arena is here."))
                .then(single("lucius", ZogratisEntities.LUCIUS, "Lucius Zogratis descends, the Soul Magic and the Supreme Time Devil with him. The arena is here."))
                .then(Commands.literal("triad").requires(s -> s.hasPermission(2)).executes(ZogratisCommand::triad));
    }

    private static <T extends ZogratisBoss> LiteralArgumentBuilder<CommandSourceStack> single(String name, DeferredHolder<EntityType<?>, EntityType<T>> type, String message) {
        return Commands.literal(name).requires(s -> s.hasPermission(2)).executes(ctx -> {
            ServerLevel level = ctx.getSource().getLevel();
            if ("lucius".equals(name) && com.newuniverse.nusmp.arena.ArenaConfig.arenaOnly() && !com.newuniverse.nusmp.arena.ZogratisArena.in(level))
                return fail(ctx, "Lucius will only answer in his arena: use the Hourglass of the Final Hour (or /zogratis_arena enter), then /multiverse boss lucius there.");      // 1.46.0
            T boss = ZogratisBoss.spawnAt(level, type.get(), ctx.getSource().getPosition(), 0f);
            if (boss == null) return fail(ctx, name + " could not be summoned here.");
            ctx.getSource().sendSuccess(() -> Component.literal(message), true);
            return 1;
        });
    }

    private static int triad(CommandContext<CommandSourceStack> ctx) {
        ServerLevel level = ctx.getSource().getLevel();
        Vec3 c = ctx.getSource().getPosition();
        int made = (member(level, ZogratisEntities.DANTE.get(), c, 0) ? 1 : 0) + (member(level, ZogratisEntities.VANICA.get(), c, 1) ? 1 : 0)
                + (member(level, ZogratisEntities.ZENON.get(), c, 2) ? 1 : 0);
        if (made == 0) return fail(ctx, "The Dark Triad could not be summoned here.");
        ctx.getSource().sendSuccess(() -> Component.literal("The Dark Triad descends: Dante, Vanica and Zenon Zogratis, rulers of the Spade Kingdom. The arena is here."), true);
        return made;
    }

    /** Member 'i' of three on a ring of 8 blocks round 'c', facing the middle; the arena is the middle. */
    private static <T extends ZogratisBoss> boolean member(ServerLevel level, EntityType<T> type, Vec3 c, int i) {
        double a = Math.PI * 2 * i / 3 + Math.PI / 2;
        Vec3 at = ZogratisBoss.ground(level, c.add(Math.cos(a) * 8, 0, Math.sin(a) * 8));
        float yaw = (float) Math.toDegrees(Math.atan2(at.x - c.x, c.z - at.z));
        T boss = ZogratisBoss.spawnAt(level, type, at, yaw);
        if (boss == null) return false;
        boss.setArena(BlockPos.containing(c));
        return true;
    }

    private static int fail(CommandContext<CommandSourceStack> ctx, String msg) {
        ctx.getSource().sendFailure(Component.literal(msg));
        return 0;
    }
}
