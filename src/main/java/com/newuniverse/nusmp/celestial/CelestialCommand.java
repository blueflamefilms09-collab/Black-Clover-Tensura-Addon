package com.newuniverse.nusmp.celestial;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;

/**
 * Why: {@code /multiverse celestial ...}: the admin side of the keys (give, bind, reset, a test king) and the two lines a player needs (the choice after the Gate opens, the way
 * back out of the Spirit World, a look at their own collection).
 * <pre>
 *   celestial give  &lt;player&gt; &lt;key|all|gold|silver|sceptre&gt;      give the item(s)
 *   celestial bind  &lt;player&gt; &lt;key|all&gt;                          seal the contract in the player's record (and in their keys' items)
 *   celestial reset &lt;player&gt;                                    forget everything (collection, contracts, bonds, the King's record)
 *   celestial king  [player]                                    summon a test king beside you / the player (admin)
 *   celestial gate  challenge|recognition|open                   the choice after the Gate (anyone); open = use the gate key
 *   celestial leave                                             leave the Spirit World
 *   celestial info  [player]                                    the collection
 * </pre>
 */
public final class CelestialCommand {
    private CelestialCommand() {}

    private static boolean admin(CommandSourceStack s) { return s.hasPermission(2); }

    private static final SuggestionProvider<CommandSourceStack> KEYS = (ctx, b) -> {
        List<String> ids = new ArrayList<>();
        for (CelestialKey k : CelestialKey.values()) ids.add(k.id);
        ids.addAll(List.of("all", "gold", "silver", "sceptre"));
        return SharedSuggestionProvider.suggest(ids, b);
    };

    public static LiteralArgumentBuilder<CommandSourceStack> node() {
        return Commands.literal("celestial")
                .then(Commands.literal("give").requires(CelestialCommand::admin).then(Commands.argument("player", EntityArgument.player())
                        .then(Commands.argument("what", StringArgumentType.word()).suggests(KEYS).executes(ctx -> give(ctx, StringArgumentType.getString(ctx, "what"))))))
                .then(Commands.literal("bind").requires(CelestialCommand::admin).then(Commands.argument("player", EntityArgument.player())
                        .then(Commands.argument("what", StringArgumentType.word()).suggests(KEYS).executes(ctx -> bind(ctx, StringArgumentType.getString(ctx, "what"))))))
                .then(Commands.literal("reset").requires(CelestialCommand::admin).then(Commands.argument("player", EntityArgument.player()).executes(CelestialCommand::reset)))
                .then(Commands.literal("king").requires(CelestialCommand::admin).executes(ctx -> king(ctx, ctx.getSource().getPlayerOrException()))
                        .then(Commands.argument("player", EntityArgument.player()).executes(ctx -> king(ctx, EntityArgument.getPlayer(ctx, "player")))))
                .then(Commands.literal("gate")
                        .then(Commands.literal("challenge").executes(ctx -> { GateRitual.choose(ctx.getSource().getPlayerOrException(), KingRules.Choice.CHALLENGE); return 1; }))
                        .then(Commands.literal("recognition").executes(ctx -> { GateRitual.choose(ctx.getSource().getPlayerOrException(), KingRules.Choice.RECOGNITION); return 1; }))
                        .then(Commands.literal("open").executes(ctx -> {
                            ServerPlayer p = ctx.getSource().getPlayerOrException();
                            if (CelestialAttachments.of(p).complete() && !CelestialRules.has(CelestialLedger.heldMask(p), CelestialKey.SPIRIT_KING_GATE)) {   // a lost gate key is made again for the keeper of all twelve
                                ItemStack k = CelestialItems.stack(CelestialKey.SPIRIT_KING_GATE);
                                if (!p.getInventory().add(k)) p.drop(k, false);
                            }
                            return GateRitual.open(p) ? 1 : 0;
                        })))
                .then(Commands.literal("leave").executes(ctx -> {
                    ServerPlayer p = ctx.getSource().getPlayerOrException();
                    if (!SpiritWorld.in(p.level())) { ctx.getSource().sendFailure(Component.literal("You are not in the Spirit World.")); return 0; }
                    SpiritWorld.leave(p);
                    return 1;
                }))
                .then(Commands.literal("info").executes(ctx -> info(ctx, ctx.getSource().getPlayerOrException()))
                        .then(Commands.argument("player", EntityArgument.player()).requires(CelestialCommand::admin).executes(ctx -> info(ctx, EntityArgument.getPlayer(ctx, "player")))));
    }

    private static List<CelestialKey> pick(String what) {
        List<CelestialKey> out = new ArrayList<>();
        switch (what) {
            case "all" -> { for (CelestialKey k : CelestialKey.values()) out.add(k); }
            case "gold" -> out.addAll(CelestialKey.gold());
            case "silver" -> out.addAll(CelestialKey.silver());
            default -> { CelestialKey k = CelestialKey.byId(what); if (k != null) out.add(k); }
        }
        return out;
    }

    private static int give(CommandContext<CommandSourceStack> ctx, String what) throws CommandSyntaxException {
        ServerPlayer p = EntityArgument.getPlayer(ctx, "player");
        List<ItemStack> stacks = new ArrayList<>();
        if (what.equals("sceptre")) stacks.add(new ItemStack(CelestialItems.SCEPTRE.get()));
        else for (CelestialKey k : pick(what)) stacks.add(CelestialItems.stack(k));
        if (stacks.isEmpty()) { ctx.getSource().sendFailure(Component.literal("No such key: " + what)); return 0; }
        for (ItemStack s : stacks) if (!p.getInventory().add(s)) p.drop(s, false);
        int n = stacks.size();
        ctx.getSource().sendSuccess(() -> Component.literal("Gave " + n + " item(s) to " + p.getGameProfile().getName() + "."), true);
        return n;
    }

    private static int bind(CommandContext<CommandSourceStack> ctx, String what) throws CommandSyntaxException {
        ServerPlayer p = EntityArgument.getPlayer(ctx, "player");
        CelestialData d = CelestialAttachments.of(p);
        List<CelestialKey> keys = pick(what);
        int n = 0;
        for (CelestialKey k : keys) {
            if (!k.summons()) continue;
            d.bind(k);
            ItemStack s = CelestialLedger.find(p, k);
            if (!s.isEmpty()) CelestialLedger.seal(p, s);
            n++;
        }
        CelestialNet.sync(p);
        int count = n;
        ctx.getSource().sendSuccess(() -> Component.literal("Bound " + count + " contract(s) for " + p.getGameProfile().getName() + "."), true);
        return n;
    }

    private static int reset(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        ServerPlayer p = EntityArgument.getPlayer(ctx, "player");
        CelestialDress.clear(p);
        CelestialAttachments.of(p).reset();
        for (ItemStack s : CelestialLedger.keyStacks(p)) CelestialLedger.release(p, s);
        KingRewards.refresh(p);
        CelestialNet.sync(p);
        ctx.getSource().sendSuccess(() -> Component.literal("Celestial record of " + p.getGameProfile().getName() + " reset."), true);
        return 1;
    }

    private static int king(CommandContext<CommandSourceStack> ctx, ServerPlayer p) {
        var level = p.serverLevel();
        SpiritKingEntity k = CelestialEntities.SPIRIT_KING.get().create(level);
        if (k == null) return 0;
        Vec3 at = p.position().add(p.getViewVector(1f).multiply(1, 0, 1).normalize().scale(8));
        k.moveTo(at.x, at.y, at.z, p.getYRot() + 180f, 0f);
        k.configure(com.newuniverse.nusmp.balance.BalanceLaw.epScale(p), 1, p.position(), -1);
        k.finalizeSpawn(level, level.getCurrentDifficultyAt(k.blockPosition()), net.minecraft.world.entity.MobSpawnType.COMMAND, null);
        level.addFreshEntity(k);
        ctx.getSource().sendSuccess(() -> Component.literal("A test Celestial Spirit King stands beside " + p.getGameProfile().getName() + "."), true);
        return 1;
    }

    private static int info(CommandContext<CommandSourceStack> ctx, ServerPlayer p) {
        CelestialData d = CelestialAttachments.of(p);
        StringBuilder sb = new StringBuilder("Gold keys " + CelestialRules.goldCount(d.owned()) + "/12:");
        for (CelestialKey k : CelestialKey.values()) {
            if (!k.summons()) continue;
            sb.append(' ').append(k.display).append(d.has(k) ? (d.isBound(k) ? "[bound L" + d.bondLevel(k) + "]" : "[held]") : "[-]");
        }
        sb.append(d.won() ? " | King defeated" : d.recognised() ? " | King recognised" : "");
        String text = sb.toString();
        ctx.getSource().sendSuccess(() -> Component.literal(text), false);
        return 1;
    }
}
