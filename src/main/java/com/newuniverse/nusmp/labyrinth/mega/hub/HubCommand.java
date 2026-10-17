package com.newuniverse.nusmp.labyrinth.mega.hub;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.newuniverse.nusmp.labyrinth.mega.HubLayout;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;

/**
 * Why: the owner's hands on Floor 0, attached by the lead under {@code /multiverse mega} (permission 2): {@code hub tp [player]} puts someone on the
 * plaza without walking through the Gateway (the way to test the Hub, or to fetch a player who is stuck), {@code hub status} says whether the dimension is
 * there, whether and with which build the plaza was stamped, what the switches are and which volumes are protected (the first questions when "the Hub does
 * not work"), {@code hub plaza} re-stamps the plaza from the template on purpose (the one way it is ever rebuilt: nothing re-stamps by itself), and
 * {@code hub gates} lists the three gate centres and what {@link HubLayout#gateNear} makes of the place the sender stands on, which is how a gate that
 * does not answer is diagnosed. All text goes through lang keys {@code message.nusmp.mega.mhb.cmd.*} with an English fallback, so the command reads fine
 * before the lang fragment is merged.
 */
public final class HubCommand {
    private HubCommand() {}

    /** The {@code hub} node: {@code tp [player] | status | plaza | gates}. */
    public static LiteralArgumentBuilder<CommandSourceStack> command() {
        return Commands.literal("hub").requires(s -> s.hasPermission(2))
                .then(Commands.literal("tp")
                        .executes(ctx -> tp(ctx.getSource(), ctx.getSource().getPlayerOrException()))
                        .then(Commands.argument("player", EntityArgument.player()).executes(ctx -> tp(ctx.getSource(), EntityArgument.getPlayer(ctx, "player")))))
                .then(Commands.literal("status").executes(ctx -> status(ctx.getSource())))
                .then(Commands.literal("plaza").executes(ctx -> plaza(ctx.getSource())))
                .then(Commands.literal("gates").executes(ctx -> gates(ctx.getSource())));
    }

    private static MutableComponent text(String key, String english, Object... args) {
        return Component.translatableWithFallback("message.nusmp.mega.mhb.cmd." + key, english, args);
    }

    private static int ok(CommandSourceStack s, MutableComponent c) {
        s.sendSuccess(() -> c, false);
        return 1;
    }

    private static int fail(CommandSourceStack s, MutableComponent c) {
        s.sendFailure(c);
        return 0;
    }

    private static String at(int x, int y, int z) { return x + " " + y + " " + z; }

    private static int tp(CommandSourceStack src, ServerPlayer who) {
        if (HubTravel.level(src.getServer()) == null) return fail(src, text("nolevel", "The Sanctuary dimension (nusmp:sanctuary) is not loaded."));
        HubTravel.returnToPlaza(who);
        return ok(src, text("tp", "Sent %s to the plaza of Floor 0.", who.getDisplayName()));
    }

    private static int status(CommandSourceStack src) {
        ServerLevel hub = HubTravel.level(src.getServer());
        ok(src, text("status.head", "Floor 0, the Sanctuary Hub (hub.enabled = %s):", HubTravel.enabled()));
        if (hub == null) return fail(src, text("nolevel", "The Sanctuary dimension (nusmp:sanctuary) is not loaded."));
        ok(src, text("status.level", "Dimension nusmp:sanctuary is loaded, %s player(s) inside.", hub.players().size()));
        HubData data = HubData.get(hub);
        if (!data.built()) ok(src, text("status.plaza.no", "The plaza has not been stamped yet: it is built when the first player arrives, or by /multiverse mega hub plaza."));
        else ok(src, text("status.plaza", "The plaza was stamped from %s, build version %s (current %s).",
                data.fallback ? text("fallback", "the programmatic fallback") : text("template", "the template"), data.plazaVersion, HubPlazaLayout.BUILD_VERSION));
        ok(src, text("status.rules", "Rules: passiveOnly %s, buildEnabled %s, protectPlaza %s, gateCooldownTicks %s.", HubConfig.get(HubConfig.PASSIVE_ONLY),
                HubConfig.get(HubConfig.BUILD_ENABLED), HubConfig.get(HubConfig.PROTECT_PLAZA), HubConfig.get(HubConfig.GATE_COOLDOWN)));
        return ok(src, text("status.protected", "Protected: the plaza box x %s..%s, y %s..%s, z %s..%s, and a sphere of %s blocks round the Holy gate (%s) and the Abyssal gate (%s).",
                HubPlazaLayout.originX(), HubPlazaLayout.maxX(), HubPlazaLayout.originY(), HubPlazaLayout.maxY(), HubPlazaLayout.originZ(), HubPlazaLayout.maxZ(),
                HubProtect.GATE_SPHERE_RADIUS, at(HubLayout.HOLY_GATE_X, HubLayout.HOLY_GATE_Y, HubLayout.HOLY_GATE_Z),
                at(HubLayout.ABYSS_GATE_X, HubLayout.ABYSS_GATE_Y, HubLayout.ABYSS_GATE_Z)));
    }

    private static int plaza(CommandSourceStack src) {
        ServerLevel hub = HubTravel.level(src.getServer());
        if (hub == null) return fail(src, text("nolevel", "The Sanctuary dimension (nusmp:sanctuary) is not loaded."));
        HubPlaza.How how = HubPlaza.stamp(hub);
        return ok(src, text("plaza", "The plaza was stamped again from %s (build version %s).",
                how == HubPlaza.How.TEMPLATE ? text("template", "the template") : text("fallback", "the programmatic fallback"), HubPlazaLayout.BUILD_VERSION));
    }

    private static int gates(CommandSourceStack src) {
        ok(src, text("gates.home", "Home gate (the way back to the Overworld): %s", at(HubLayout.HOME_GATE_X, HubLayout.HOME_GATE_Y, HubLayout.HOME_GATE_Z)));
        ok(src, text("gates.holy", "Holy gate (the Spire, the road up): %s", at(HubLayout.HOLY_GATE_X, HubLayout.HOLY_GATE_Y, HubLayout.HOLY_GATE_Z)));
        ok(src, text("gates.abyssal", "Abyssal gate (the Chasm, the road down): %s", at(HubLayout.ABYSS_GATE_X, HubLayout.ABYSS_GATE_Y, HubLayout.ABYSS_GATE_Z)));
        BlockPos here = BlockPos.containing(src.getPosition());
        HubLayout.GateKind kind = HubLayout.gateNear(here.getX(), here.getY(), here.getZ());
        return ok(src, kind == null
                ? text("gates.none", "Where you stand (%s) no gate answers: the nearest one is more than %s blocks away.", at(here.getX(), here.getY(), here.getZ()), HubLayout.GATE_REACH)
                : text("gates.near", "Where you stand (%s) a gate block would be the %s gate.", at(here.getX(), here.getY(), here.getZ()), kind.name().toLowerCase(java.util.Locale.ROOT)));
    }
}
