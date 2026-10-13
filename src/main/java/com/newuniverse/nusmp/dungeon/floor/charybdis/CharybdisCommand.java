package com.newuniverse.nusmp.dungeon.floor.charybdis;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.newuniverse.nusmp.dungeon.LivingDungeon;
import com.newuniverse.nusmp.dungeon.nexus.NexusInfo;
import com.newuniverse.nusmp.dungeon.nexus.NexusZone;
import com.newuniverse.nusmp.dungeon.nexus.NexusZones;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.phys.Vec3;

/**
 * {@code /multiverse boss charybdis} (permission 2): spawns one Charybdis Spawn. Inside a Charybdis Basin it appears at the basin's hover point, over
 * the dais, with the basin as its arena (so its fall collapses that basin); anywhere else it appears at the executor with the arena there (a test
 * boss: it floats in the air, steers the same way and collapses nothing). The multiverse command calls {@link #addTo} with its "boss" node.
 */
public final class CharybdisCommand {
    private CharybdisCommand() {}

    public static void addTo(LiteralArgumentBuilder<CommandSourceStack> bossNode) {
        bossNode.then(Commands.literal("charybdis").requires(s -> s.hasPermission(2)).executes(CharybdisCommand::spawn));
    }

    private static int spawn(CommandContext<CommandSourceStack> ctx) {
        ServerLevel level = ctx.getSource().getLevel();
        Vec3 at = ctx.getSource().getPosition();
        BlockPos here = BlockPos.containing(at);
        NexusInfo basin = LivingDungeon.in(level) ? NexusZones.containing(level, here) : null;
        CharybdisSpawnBoss boss;
        String message;
        if (basin != null && basin.zone == NexusZone.CHARYBDIS_BASIN) {
            boss = CharybdisSurface.spawn(level, basin.centre(), false);
            message = "The Charybdis Spawn rises over the dais of this basin.";
        } else {
            boss = CharybdisEntities.CHARYBDIS_SPAWN.get().create(level);
            if (boss != null) {
                boss.moveTo(at.x, at.y, at.z, 0f, 0f);
                boss.setArena(here);
                level.addFreshEntity(boss);
            }
            message = "The Charybdis Spawn appears. The arena is here (outside a basin it floats, and its fall collapses nothing).";
        }
        if (boss == null) {
            ctx.getSource().sendFailure(Component.literal("The Charybdis Spawn could not be summoned here."));
            return 0;
        }
        ctx.getSource().sendSuccess(() -> Component.literal(message), true);
        return 1;
    }
}
