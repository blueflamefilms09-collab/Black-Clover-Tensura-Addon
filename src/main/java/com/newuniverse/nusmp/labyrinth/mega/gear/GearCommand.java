package com.newuniverse.nusmp.labyrinth.mega.gear;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.newuniverse.nusmp.dungeon.sentient.Dice;
import com.newuniverse.nusmp.labyrinth.mega.FloorMap;
import com.newuniverse.nusmp.labyrinth.mega.MegaDungeon;
import com.newuniverse.nusmp.quests.NusmpEvents;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Random;

/**
 * Why: the owner cannot watch a hundred kills to find out whether the drop rates feel right, and cannot wait for a Rare piece to test one, so this is the
 * admin's hands on the standard gear, attached by the lead under {@code /multiverse mega} (permission 2). {@code gear give <player> <grade> [slot]} makes
 * a piece with the real factory and hands it over (the way to look at a Rare chestplate, to test the magic resistance on yourself, to check the tooltip);
 * {@code gear roll [count]} runs the real roller {@code count} times at YOUR floor and prints the grade and slot histogram, which is how the table is
 * tuned (it is the same code the monsters use, with the config's rates and the data file's curves, not a copy); {@code gear status} prints the chance table
 * at your floor: the configured bases, what the depth curves and Looting make of them, the total, the caps and the switches. All text goes through lang
 * keys {@code message.nusmp.mega.mgr.cmd.*} with an English fallback, so the command reads fine before the lang fragment is merged. Outside a sector (or from
 * the console) the floor is 1.
 */
public final class GearCommand {
    private GearCommand() {}

    private static final int DEFAULT_ROLLS = 1000, MAX_ROLLS = 200_000;

    /** The {@code gear} node: {@code give <player> <grade> [slot] | roll [count] | roll chest [count] | roll treasure [count] | status}. */
    public static LiteralArgumentBuilder<CommandSourceStack> command() {
        return Commands.literal("gear").requires(s -> s.hasPermission(2))
                .then(Commands.literal("give").then(Commands.argument("player", EntityArgument.player())
                        .then(Commands.argument("grade", StringArgumentType.word())
                                .suggests((ctx, b) -> SharedSuggestionProvider.suggest(grades(), b))
                                .executes(ctx -> give(ctx.getSource(), EntityArgument.getPlayer(ctx, "player"), StringArgumentType.getString(ctx, "grade"), null))
                                .then(Commands.argument("slot", StringArgumentType.word())
                                        .suggests((ctx, b) -> SharedSuggestionProvider.suggest(slots(), b))
                                        .executes(ctx -> give(ctx.getSource(), EntityArgument.getPlayer(ctx, "player"), StringArgumentType.getString(ctx, "grade"),
                                                StringArgumentType.getString(ctx, "slot")))))))
                .then(Commands.literal("roll")
                        .executes(ctx -> roll(ctx.getSource(), DEFAULT_ROLLS))
                        .then(Commands.argument("count", IntegerArgumentType.integer(1, MAX_ROLLS))
                                .executes(ctx -> roll(ctx.getSource(), IntegerArgumentType.getInteger(ctx, "count"))))
                        .then(Commands.literal("chest")
                                .executes(ctx -> rollChests(ctx.getSource(), DEFAULT_ROLLS, ChestRules.Kind.STANDARD))
                                .then(Commands.argument("count", IntegerArgumentType.integer(1, MAX_ROLLS))
                                        .executes(ctx -> rollChests(ctx.getSource(), IntegerArgumentType.getInteger(ctx, "count"), ChestRules.Kind.STANDARD))))
                        .then(Commands.literal("treasure")
                                .executes(ctx -> rollChests(ctx.getSource(), DEFAULT_ROLLS, ChestRules.Kind.TREASURE))
                                .then(Commands.argument("count", IntegerArgumentType.integer(1, MAX_ROLLS))
                                        .executes(ctx -> rollChests(ctx.getSource(), IntegerArgumentType.getInteger(ctx, "count"), ChestRules.Kind.TREASURE)))))
                .then(Commands.literal("status").executes(ctx -> status(ctx.getSource())));
    }

    private static List<String> grades() {
        List<String> out = new ArrayList<>();
        for (GearGrade g : GearGrade.values()) out.add(g.id());
        return out;
    }

    private static List<String> slots() {
        List<String> out = new ArrayList<>();
        for (GearSlot s : GearSlot.values()) out.add(s.id());
        return out;
    }

    private static MutableComponent text(String key, String english, Object... args) {
        return Component.translatableWithFallback("message.nusmp.mega.mgr.cmd." + key, english, args);
    }

    private static int ok(CommandSourceStack s, MutableComponent c) {
        s.sendSuccess(() -> c, false);
        return 1;
    }

    private static int fail(CommandSourceStack s, MutableComponent c) {
        s.sendFailure(c);
        return 0;
    }

    private static String pct(double p) { return String.format(Locale.ROOT, "%.2f%%", p * 100.0); }

    /** The floor of the sender: the sector floor under a player, 1 for the console or a spot outside every sector. */
    private static int floorOf(CommandSourceStack src) {
        ServerPlayer p = src.getPlayer();
        int floor = p == null ? 0 : MegaDungeon.floorAt(p.level(), p.blockPosition());
        return floor == 0 ? 1 : floor;
    }

    // ---------------------------------------------------------------- give
    private static int give(CommandSourceStack src, ServerPlayer target, String gradeArg, String slotArg) {
        GearGrade grade = GearGrade.fromId(gradeArg);
        if (grade == null) return fail(src, text("give.grade", "'%s' is not a grade. Use normal, special or rare.", gradeArg));
        GearSlot slot = null;
        if (slotArg != null) {
            slot = GearSlot.fromId(slotArg);
            if (slot == null) return fail(src, text("give.slot", "'%s' is not a slot. Use %s.", slotArg, String.join(", ", slots())));
        }
        Dice dice = new Random()::nextDouble;
        Optional<GearSpec> spec = GearRoller.make(dice, grade, slot, GearTablesReload.tables());
        if (spec.isEmpty()) return fail(src, text("give.none", "The tables offer no %s piece%s (see the log for the gear tables).", grade.id(), slot == null ? "" : " for " + slot.id()));
        ItemStack stack = GearFactory.create(spec.get(), src.registryAccess());
        if (stack.isEmpty()) return fail(src, text("give.item", "The base item %s does not exist in this game.", spec.get().item()));
        ItemStack shown = stack.copy();
        if (!target.getInventory().add(stack)) target.drop(stack, false);
        NusmpEvents.fireOnce(target, GearEvents.QUEST_KEY, grade.id());
        return ok(src, text("give.ok", "Gave %s to %s.", shown.getDisplayName(), target.getDisplayName()));
    }

    // ---------------------------------------------------------------- roll
    private static int roll(CommandSourceStack src, int count) {
        GearTables tables = GearTablesReload.tables();
        if (!tables.isUsable()) return fail(src, text("roll.empty", "The gear tables are empty or unusable (see the log)."));
        GearRates rates = GearConfig.rates();
        int floor = floorOf(src);
        Dice dice = new Random()::nextDouble;
        int[] byGrade = new int[GearGrade.values().length];
        Map<GearSlot, Integer> bySlot = new EnumMap<>(GearSlot.class);
        int pieces = 0, resistant = 0;
        long resistSum = 0;
        for (int i = 0; i < count; i++) {
            Optional<GearSpec> s = GearRoller.roll(dice, floor, 0, tables, rates);
            if (s.isEmpty()) continue;
            pieces++;
            byGrade[s.get().grade().ordinal()]++;
            bySlot.merge(s.get().slot(), 1, Integer::sum);
            if (s.get().resistPercent() > 0) {
                resistant++;
                resistSum += s.get().resistPercent();
            }
        }
        ok(src, text("roll.head", "%s kills at floor %s (threat %s, no Looting): %s pieces (%s of kills).", count, FloorMap.label(floor), FloorMap.threat(floor), pieces, pct(pieces / (double) count)));
        for (GearGrade g : GearGrade.values()) {
            int n = byGrade[g.ordinal()];
            ok(src, text("roll.grade", "%s: %s (%s of kills)", g.id(), n, pct(n / (double) count)));
        }
        StringBuilder slotLine = new StringBuilder();
        for (GearSlot s : GearSlot.values()) {
            if (slotLine.length() > 0) slotLine.append(", ");
            slotLine.append(s.id()).append(' ').append(bySlot.getOrDefault(s, 0));
        }
        ok(src, text("roll.slots", "By slot: %s", slotLine.toString()));
        if (resistant > 0) ok(src, text("roll.resist", "%s pieces resist magic, on average %s percent each.", resistant, String.format(Locale.ROOT, "%.1f", resistSum / (double) resistant)));
        return pieces;
    }

    /**
     * {@code roll chest|treasure [count]}: the same histogram for {@code count} chests of the kind at YOUR floor, with the chest chances of the config (and the
     * treasure factor), no Luck. The code path is the chest modifier's own decision ({@link ChestGear#decide}), not a copy.
     */
    private static int rollChests(CommandSourceStack src, int count, ChestRules.Kind kind) {
        GearTables tables = GearTablesReload.tables();
        if (!tables.isUsable()) return fail(src, text("roll.empty", "The gear tables are empty or unusable (see the log)."));
        GearRates rates = GearConfig.chestRates();
        double factor = GearConfig.get(GearConfig.CHEST_TREASURE_FACTOR);
        int floor = floorOf(src);
        Dice dice = new Random()::nextDouble;
        int[] byGrade = new int[GearGrade.values().length];
        Map<GearSlot, Integer> bySlot = new EnumMap<>(GearSlot.class);
        int pieces = 0;
        for (int i = 0; i < count; i++) {
            Optional<GearSpec> s = ChestGear.decide(dice, kind, rates, factor, tables, floor, 0);
            if (s.isEmpty()) continue;
            pieces++;
            byGrade[s.get().grade().ordinal()]++;
            bySlot.merge(s.get().slot(), 1, Integer::sum);
        }
        ok(src, text("roll.chest.head", "%s %s chests at floor %s (threat %s, no Luck): %s pieces (%s of chests).", count, kind == ChestRules.Kind.TREASURE ? "treasure" : "standard",
                FloorMap.label(floor), FloorMap.threat(floor), pieces, pct(pieces / (double) count)));
        for (GearGrade g : GearGrade.values()) {
            int n = byGrade[g.ordinal()];
            ok(src, text("roll.chest.grade", "%s: %s (%s of chests)", g.id(), n, pct(n / (double) count)));
        }
        StringBuilder slotLine = new StringBuilder();
        for (GearSlot s : GearSlot.values()) {
            if (slotLine.length() > 0) slotLine.append(", ");
            slotLine.append(s.id()).append(' ').append(bySlot.getOrDefault(s, 0));
        }
        ok(src, text("roll.slots", "By slot: %s", slotLine.toString()));
        return pieces;
    }

    // ---------------------------------------------------------------- status
    private static int status(CommandSourceStack src) {
        GearTables tables = GearTablesReload.tables();
        GearRates rates = GearConfig.rates();
        int floor = floorOf(src);
        ok(src, text("status.head", "Standard gear: drops %s, bosses' guaranteed Rare %s, engravings %s, magic resistance ceiling %s percent.",
                GearConfig.get(GearConfig.ENABLED) ? "on" : "off", GearConfig.get(GearConfig.BOSS_GUARANTEED) ? "on" : "off",
                GearConfig.get(GearConfig.ENGRAVINGS) ? "on" : "off", GearConfig.get(GearConfig.MAX_RESIST)));
        ok(src, text("status.floor", "Floor %s (threat %s): depth progress %s of the curves. Tables: %s", FloorMap.label(floor), FloorMap.threat(floor),
                String.format(Locale.ROOT, "%.2f", GearRoller.progress(rates, floor)), GearTablesParser.summary(tables)));
        double[] none = GearRoller.chances(rates, tables, floor, 0), three = GearRoller.chances(rates, tables, floor, 3);
        for (GearGrade g : GearGrade.values()) {
            int i = g.ordinal();
            ok(src, text("status.grade", "%s: base %s, here %s, with Looting III %s (cap %s)", g.id(), pct(rates.base(g)), pct(none[i]), pct(three[i]), pct(tables.maxChance(g))));
        }
        ok(src, text("status.total", "Total per kill: %s, with Looting III %s (cap %s). At most one piece per kill.", pct(GearRoller.total(none)), pct(GearRoller.total(three)), pct(tables.totalCap())));
        GearRates chest = GearConfig.chestRates();
        double factor = GearConfig.get(GearConfig.CHEST_TREASURE_FACTOR);
        double[] std = ChestGear.chances(ChestRules.Kind.STANDARD, chest, factor, tables, floor, 0), trs = ChestGear.chances(ChestRules.Kind.TREASURE, chest, factor, tables, floor, 0);
        ok(src, text("status.chest", "Chests (the loot modifier nusmp:gear_chests, one piece at most): standard %s normal / %s special / %s rare = %s; treasure (x%s on special and rare) %s / %s / %s = %s.",
                pct(std[0]), pct(std[1]), pct(std[2]), pct(GearRoller.total(std)), String.format(Locale.ROOT, "%.1f", factor),
                pct(trs[0]), pct(trs[1]), pct(trs[2]), pct(GearRoller.total(trs))));
        int problems = GearTablesReload.problems().size();
        if (problems > 0) ok(src, text("status.problems", "The tables file has %s problem(s); the log lists them.", problems));
        return 1;
    }
}
