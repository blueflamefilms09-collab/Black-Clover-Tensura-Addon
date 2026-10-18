package com.newuniverse.nusmp.elite.forge;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.newuniverse.nusmp.elite.forge.block.ForgeStationBlock;
import com.newuniverse.nusmp.elite.forge.block.ForgeStationBlockEntity;
import com.newuniverse.nusmp.elite.forge.craft.SkillProbe;
import com.newuniverse.nusmp.elite.forge.data.ForgeRecipes;
import com.newuniverse.nusmp.elite.forge.net.ForgeServer;
import com.newuniverse.nusmp.elite.forge.rules.ForgeProgress;
import com.newuniverse.nusmp.elite.forge.rules.Quality;
import com.newuniverse.nusmp.elite.forge.rules.Specialization;
import com.newuniverse.nusmp.elite.forge.rules.StationTier;
import com.newuniverse.nusmp.elite.forge.store.ForgeStore;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Why: the operator's door to the forge, under the shared {@code /multiverse elite forge} branch (Brigadier merges the {@code multiverse} and {@code elite} nodes the
 * other elite systems register, so this adds only its own {@code forge} sub-tree). Operators (permission 2): {@code give station <tier> [player]} and {@code give kit
 * <tier> [player]} hand out a station block or an upgrade kit; {@code spec set <player> <specialization>} and {@code spec clear <player>} set a specialization without
 * the cost; {@code info [player]} shows a player's progress, skill bonus and, when looking at a station, the station's own numbers (anyone may read their own info);
 * {@code reload} says how many recipes are loaded and which are unavailable. Works while the master switch is off, so an operator can still tend to a world.
 */
public final class ForgeCommand {
    private ForgeCommand() {}

    public static void register(CommandDispatcher<CommandSourceStack> d) {
        d.register(Commands.literal("multiverse").then(Commands.literal("elite").then(Commands.literal("forge")
                .then(Commands.literal("give").requires(s -> s.hasPermission(2))
                        .then(Commands.literal("station")
                                .then(Commands.argument("tier", StringArgumentType.word()).suggests((c, b) -> SharedSuggestionProvider.suggest(tiers(false), b))
                                        .executes(c -> giveStation(c, c.getSource().getPlayerOrException()))
                                        .then(Commands.argument("player", EntityArgument.player()).executes(c -> giveStation(c, EntityArgument.getPlayer(c, "player"))))))
                        .then(Commands.literal("kit")
                                .then(Commands.argument("tier", StringArgumentType.word()).suggests((c, b) -> SharedSuggestionProvider.suggest(tiers(true), b))
                                        .executes(c -> giveKit(c, c.getSource().getPlayerOrException()))
                                        .then(Commands.argument("player", EntityArgument.player()).executes(c -> giveKit(c, EntityArgument.getPlayer(c, "player")))))))
                .then(Commands.literal("spec").requires(s -> s.hasPermission(2))
                        .then(Commands.literal("set").then(Commands.argument("player", EntityArgument.player())
                                .then(Commands.argument("specialization", StringArgumentType.word()).suggests((c, b) -> SharedSuggestionProvider.suggest(specs(), b))
                                        .executes(ForgeCommand::setSpec))))
                        .then(Commands.literal("clear").then(Commands.argument("player", EntityArgument.player()).executes(ForgeCommand::clearSpec))))
                .then(Commands.literal("info")
                        .executes(c -> info(c, c.getSource().getPlayerOrException()))
                        .then(Commands.argument("player", EntityArgument.player()).requires(s -> s.hasPermission(2)).executes(c -> info(c, EntityArgument.getPlayer(c, "player")))))
                .then(Commands.literal("recipes").requires(s -> s.hasPermission(2)).executes(ForgeCommand::recipes)))));
    }

    private static List<String> tiers(boolean kits) {
        List<String> out = new ArrayList<>();
        for (StationTier t : StationTier.values()) if (!kits || t.prev() != null) out.add(t.id());
        return out;
    }

    private static List<String> specs() {
        List<String> out = new ArrayList<>();
        for (Specialization s : Specialization.values()) out.add(s.id());
        return out;
    }

    private static int fail(CommandContext<CommandSourceStack> c, String text) {
        c.getSource().sendFailure(Component.literal(text));
        return 0;
    }

    private static int ok(CommandContext<CommandSourceStack> c, String text) {
        c.getSource().sendSuccess(() -> Component.literal(text), true);
        return 1;
    }

    private static int giveStation(CommandContext<CommandSourceStack> c, ServerPlayer target) {
        StationTier t = StationTier.byId(StringArgumentType.getString(c, "tier"));
        if (t == null) return fail(c, "Unknown tier. Use basic, magisteel, mythril or dragonforge.");
        target.getInventory().placeItemBackInInventory(new ItemStack(ForgeRegistry.stationItem(t).get()));
        return ok(c, "Gave " + target.getName().getString() + " a " + t.id() + " forge station.");
    }

    private static int giveKit(CommandContext<CommandSourceStack> c, ServerPlayer target) {
        StationTier t = StationTier.byId(StringArgumentType.getString(c, "tier"));
        if (t == null || ForgeRegistry.kit(t) == null) return fail(c, "Unknown tier. Kits exist for magisteel, mythril and dragonforge.");
        target.getInventory().placeItemBackInInventory(new ItemStack(ForgeRegistry.kit(t).get()));
        return ok(c, "Gave " + target.getName().getString() + " a " + t.id() + " station upgrade kit.");
    }

    private static int setSpec(CommandContext<CommandSourceStack> c) throws CommandSyntaxException {
        ServerPlayer p = EntityArgument.getPlayer(c, "player");
        Specialization s = Specialization.byId(StringArgumentType.getString(c, "specialization"));
        if (s == null) return fail(c, "Unknown specialization. Use " + String.join(", ", specs()) + ".");
        ForgeStore.of(p).setSpecialization(s);
        ForgeStore.mirror(p);
        ForgeServer.sendSync(p);
        return ok(c, p.getName().getString() + " is now a " + s.id() + ".");
    }

    private static int clearSpec(CommandContext<CommandSourceStack> c) throws CommandSyntaxException {
        ServerPlayer p = EntityArgument.getPlayer(c, "player");
        ForgeStore.of(p).clearSpecialization();
        ForgeStore.mirror(p);
        ForgeServer.sendSync(p);
        return ok(c, p.getName().getString() + " has no specialization now.");
    }

    private static int info(CommandContext<CommandSourceStack> c, ServerPlayer p) {
        ForgeProgress pr = ForgeStore.of(p);
        String name = p.getName().getString();
        Quality best = Quality.ofLevel(pr.best());
        StringBuilder sb = new StringBuilder();
        sb.append(name).append(": ").append(ForgeConfig.enabled() ? "forge on" : "forge OFF (config)").append("; forged ").append(pr.crafted()).append(" (");
        for (StationTier t : StationTier.values()) sb.append(t.id()).append(' ').append(pr.craftedAt(t)).append(t.next() == null ? "" : ", ");
        sb.append("); best ").append(best == null ? "none" : best.id()).append("; specialization ")
                .append(pr.specialization() == null ? "none" : pr.specialization().id()).append(" (changed ").append(pr.specChanges()).append("x); recipes known ")
                .append(pr.known().size()).append("; top station ").append(StationTier.byIndex(pr.topStation()).id());
        c.getSource().sendSuccess(() -> Component.literal(sb.toString()), false);
        Map<String, Integer> why = SkillProbe.explain(p);
        int bonus = SkillProbe.bonus(p);
        String skills = why.isEmpty() ? "none" : why.toString();
        c.getSource().sendSuccess(() -> Component.literal("Skill bonus: +" + bonus + " quality points (" + skills + ", cap " + ForgeRecipes.skillTable().cap() + ")"), false);
        HitResult hit = p.pick(8.0, 0.0f, false);
        if (hit instanceof BlockHitResult bh && ForgeStationBlock.tierAt(p.level(), bh.getBlockPos()) != null) {
            StationTier t = ForgeStationBlock.tierAt(p.level(), bh.getBlockPos());
            String extra = p.level().getBlockEntity(bh.getBlockPos()) instanceof ForgeStationBlockEntity be
                    ? ", " + be.forged() + " forged here, best " + (Quality.ofLevel(be.best()) == null ? "none" : Quality.ofLevel(be.best()).id()) + ", upgraded " + be.upgrades() + "x" : "";
            c.getSource().sendSuccess(() -> Component.literal("Station: " + t.id() + " (+" + t.qualityBonus() + " points, best quality " + t.ceiling().id() + ")" + extra), false);
        }
        return 1;
    }

    private static int recipes(CommandContext<CommandSourceStack> c) {
        int n = 0, bad = 0;
        StringBuilder missing = new StringBuilder();
        for (ForgeRecipes.Loaded l : ForgeRecipes.all().values()) {
            n++;
            if (!l.available()) {
                bad++;
                if (missing.length() < 300) missing.append(l.def().id()).append(" (").append(l.missing()).append(") ");
            }
        }
        final String line = n + " forge recipes loaded, " + bad + " unavailable" + (bad > 0 ? ": " + missing : "") + ".";
        return ok(c, line);
    }
}
