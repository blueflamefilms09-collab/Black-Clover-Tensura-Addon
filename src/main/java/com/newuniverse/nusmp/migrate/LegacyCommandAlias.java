package com.newuniverse.nusmp.migrate;

import com.newuniverse.nusmp.NUSMP;
import com.mojang.brigadier.tree.CommandNode;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterCommandsEvent;

/** 3.84.0: the old command root still works, as an alias of the new one (scripts and command blocks keep running). Runs last, after every command has been added. */
@EventBusSubscriber(modid = NUSMP.MODID)
public final class LegacyCommandAlias {
    private LegacyCommandAlias() {}

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onRegister(RegisterCommandsEvent event) {
        CommandNode<CommandSourceStack> root = event.getDispatcher().getRoot().getChild(LegacyId.NEW);
        if (root == null) return;
        event.getDispatcher().register(Commands.literal(LegacyId.OLD).requires(root.getRequirement()).redirect(root));
    }
}
