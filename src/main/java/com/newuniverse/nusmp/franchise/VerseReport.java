package com.newuniverse.nusmp.franchise;

import com.newuniverse.nusmp.NUCreativeTab;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.item.CreativeModeTab;
import net.neoforged.neoforge.event.RegisterCommandsEvent;

import java.util.ArrayList;
import java.util.List;

/**
 * 4.96.0: the verse report. {@code /multiverse verses} (operators, level 2) counts the distinct items of every franchise and verse tab and says which are empty, thin or heavy
 * ({@link FranchiseTabs#status}), plus how many items belong to no tab (so nothing is lost). It runs the creative fill once on the server (the same code the tabs use), reads
 * only, and is op-only, so it costs one fill per command and nothing per tick. The CI self-test {@code item.franchise_tabs} prints the same table to the log.
 */
public final class VerseReport {
    private VerseReport() {}

    /** One row of the report. */
    public record Row(String id, int items, int stacks) {}

    /** Rows in tab order (the seven franchise tabs, then the verse tabs) and the ids that are in no tab. */
    public record Report(List<Row> rows, List<String> none) {}

    public static Report build(MinecraftServer server) {
        var params = new CreativeModeTab.ItemDisplayParameters(server.getWorldData().enabledFeatures(), true, server.registryAccess());
        NUCreativeTab.Buckets b = NUCreativeTab.collect(params);
        List<Row> rows = new ArrayList<>();
        List<String> order = new ArrayList<>(FranchiseTabs.TABS);
        order.addAll(FranchiseTabs.VERSES);
        for (String id : order) rows.add(new Row(id, b.ids.getOrDefault(id, java.util.Set.of()).size(), b.stacks.getOrDefault(id, List.of()).size()));
        return new Report(rows, new ArrayList<>(b.none));
    }

    /** One line per verse for the log or chat. */
    public static List<String> lines(Report r) {
        List<String> out = new ArrayList<>();
        for (Row row : r.rows()) out.add(row.id() + ": " + row.items() + " items (" + row.stacks() + " stacks), " + FranchiseTabs.status(row.items()));
        out.add("in no tab: " + r.none().size() + (r.none().isEmpty() ? "" : " " + r.none().subList(0, Math.min(40, r.none().size()))));
        return out;
    }

    public static void register(RegisterCommandsEvent event) {
        event.getDispatcher().register(Commands.literal("multiverse").then(Commands.literal("verses").requires(s -> s.hasPermission(2)).executes(ctx -> {
            CommandSourceStack src = ctx.getSource();
            Report r = build(src.getServer());
            for (Row row : r.rows()) {
                ChatFormatting c = row.items() == 0 ? ChatFormatting.RED : row.items() < FranchiseTabs.THIN ? ChatFormatting.YELLOW : row.items() > FranchiseTabs.HEAVY ? ChatFormatting.GOLD : ChatFormatting.GREEN;
                String line = row.id() + ": " + row.items() + " items, " + FranchiseTabs.status(row.items());
                src.sendSuccess(() -> Component.literal(line).withStyle(c), false);
            }
            int none = r.none().size();
            src.sendSuccess(() -> Component.literal("in no verse tab: " + none).withStyle(ChatFormatting.GRAY), false);
            return 1;
        })));
    }
}
