package com.newuniverse.nusmp.client.relics;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.newuniverse.nusmp.client.relics.fx.RelicFxOptions;
import com.newuniverse.nusmp.relics.RelicShowPayload;
import com.newuniverse.nusmp.relics.Relics;
import com.newuniverse.nusmp.relics.WornOnce;
import com.newuniverse.nusmp.relics.WornSync;
import com.newuniverse.nusmp.relics.WornSyncRules;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.LivingEntity;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RegisterClientCommandsEvent;
import net.neoforged.neoforge.event.entity.EntityLeaveLevelEvent;

/**
 * The client half of "relics are worn and shown all the time unless the player switches them off" (relics_wear), registered from {@link RelicClient#init}:
 * <ul>
 *   <li><b>The all-relics switch</b> {@code relicfx.showWorn} (default on; the contract is {@code WornVisibility}): the command {@code /multiverse relic show <on|off|toggle>} (a SERVER command that only
 *       tells the player's own client what to do, see {@link #apply}; a client command with the root word {@code multiverse} would swallow the server's subcommands), {@code /nusmp relic show ...} (the same as a
 *       client command, no round trip), and the key "Show or hide all worn relics" (unbound by default, {@link RelicActKeys}). It comes on top of the Curios eye icon of one slot and
 *       {@code relicfx.hideArmourRelics}.</li>
 *   <li><b>A fresh look while a screen is open</b>: the cached look at the local player lives {@link WornSyncRules#FAST_TTL} ticks while any screen is open and for
 *       {@link WornSyncRules#FAST_HOLD} ticks after it closes, so a relic put on in the inventory or the Curios screen is drawn on the preview and the paper doll at once.</li>
 *   <li><b>No stale server list</b>: when a living entity leaves this client's level, the server's list for it is dropped ({@link WornSync#forgetClient}); the server sends it again when the entity is tracked again.</li>
 * </ul>
 * Cost: one comparison and one volatile write per client tick; one map removal per living entity that leaves the level.
 */
public final class RelicWearClient {
    private RelicWearClient() {}

    private static int hold;

    /** Game bus, client tick. */
    public static void onClientTick(ClientTickEvent.Post e) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null) {
            hold = 0;
            Relics.setFastLook(null);
            return;
        }
        hold = WornSyncRules.nextFastHold(mc.screen != null, hold);
        Relics.setFastLook(hold > 0 ? mc.player.getUUID() : null);
    }

    /** Game bus: an entity left the client's level. */
    public static void onLeave(EntityLeaveLevelEvent e) {
        if (e.getLevel().isClientSide() && e.getEntity() instanceof LivingEntity le) WornSync.forgetClient(le.getUUID());
    }

    /** Leaving a world: the causes already logged may be logged again in the next one. */
    public static void onLogout() {
        Relics.setFastLook(null);
        hold = 0;
        WornOnce.clear();
    }

    /** The action-bar / chat line after the switch changed. */
    public static Component stateLine(boolean on) {
        return on ? Component.translatableWithFallback("message.nusmp.relic_wear.show_on", "Worn relics are shown on every player.").withStyle(ChatFormatting.GREEN)
                : Component.translatableWithFallback("message.nusmp.relic_wear.show_off", "Worn relics are hidden on every player (turn them back on with /multiverse relic show on).").withStyle(ChatFormatting.GRAY);
    }

    // ---------------------------------------------------------------- the switch
    /** Applies a {@link RelicShowPayload} (from {@code /multiverse relic show}): sets, flips or reports {@code relicfx.showWorn} and says the state in the chat. */
    public static void apply(int mode) {
        boolean on = switch (mode) {
            case RelicShowPayload.OFF -> RelicFxOptions.setShowWorn(false);
            case RelicShowPayload.ON -> RelicFxOptions.setShowWorn(true);
            case RelicShowPayload.TOGGLE -> RelicFxOptions.toggleShowWorn();
            default -> RelicFxOptions.showWorn();
        };
        say(on);
    }

    private static void say(boolean on) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.gui != null) mc.gui.getChat().addMessage(stateLine(on));
    }

    private static LiteralArgumentBuilder<CommandSourceStack> show() {
        return Commands.literal("show")
                .executes(c -> { say(RelicFxOptions.showWorn()); return 1; })
                .then(Commands.literal("on").executes(c -> { say(RelicFxOptions.setShowWorn(true)); return 1; }))
                .then(Commands.literal("off").executes(c -> { say(RelicFxOptions.setShowWorn(false)); return 1; }))
                .then(Commands.literal("toggle").executes(c -> { say(RelicFxOptions.toggleShowWorn()); return 1; }));
    }

    /**
     * Game bus: {@code RegisterClientCommandsEvent}. {@code /nusmp relic show <on|off|toggle>} under the mod's own client root (the same switch without a server round trip; the root word
     * {@code multiverse} belongs to the server's command tree and is not used by a client command).
     */
    public static void registerCommands(RegisterClientCommandsEvent e) {
        e.getDispatcher().register(Commands.literal("nusmp").then(Commands.literal("relic").then(show())));
    }
}
