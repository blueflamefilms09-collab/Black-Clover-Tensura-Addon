package com.newuniverse.nusmp.avatar;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.newuniverse.nusmp.avatar.appearance.AppearanceData;
import com.newuniverse.nusmp.avatar.appearance.DynamicAppearanceEngine;
import com.newuniverse.nusmp.avatar.dnd.StatRoller;
import com.newuniverse.nusmp.avatar.item.AvatarItems;
import com.newuniverse.nusmp.avatar.luck.LuckPoller;
import com.newuniverse.nusmp.avatar.outcome.AvatarOutcomes;
import com.newuniverse.nusmp.avatar.outcome.Outcome;
import com.newuniverse.nusmp.avatar.race.AvatarRace;
import com.newuniverse.nusmp.avatar.race.AvatarRaces;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;

import java.util.Comparator;
import java.util.List;
import java.util.Random;

/**
 * Why: testing and running the Avatar system needs admin handles: spawn an Avatar of any race without a Genesis Core, see what exists, read one's state,
 * force an outcome to see betrayal or awakening without waiting three days, and hand out the core. Plain Brigadier under {@code /nusmp avatar}, operators only
 * (permission level 2).
 * <pre>
 *   /nusmp avatar spawn &lt;race&gt; [player]        make an Avatar of that race for the player (default: you); no core, no cost, no limit
 *   /nusmp avatar list                          every Avatar the world knows (creator, state, dimension)
 *   /nusmp avatar info                          the Avatar nearest to you
 *   /nusmp avatar kill                          kill the Avatar nearest to you
 *   /nusmp avatar outcome now|reset             decide the nearest Avatar's fate now / start a fresh timer
 *   /nusmp avatar outcome force betrayal|awaken|ordinary
 *   /nusmp avatar give genesis_core [player]    hand out a Genesis Core
 * </pre>
 */
public final class AvatarCommand {
    private AvatarCommand() {}

    public static void register(CommandDispatcher<CommandSourceStack> d) {
        d.register(Commands.literal("nusmp").requires(s -> s.hasPermission(2)).then(Commands.literal("avatar").requires(s -> s.hasPermission(2))
                .then(Commands.literal("spawn").then(Commands.argument("race", StringArgumentType.word())
                        .suggests((c, b) -> SharedSuggestionProvider.suggest(AvatarRaces.all().stream().map(AvatarRace::id).toList(), b))
                        .executes(c -> spawn(c, c.getSource().getPlayerOrException()))
                        .then(Commands.argument("player", EntityArgument.player()).executes(c -> spawn(c, EntityArgument.getPlayer(c, "player"))))))
                .then(Commands.literal("list").executes(AvatarCommand::list))
                .then(Commands.literal("info").executes(AvatarCommand::info))
                .then(Commands.literal("kill").executes(AvatarCommand::kill))
                .then(Commands.literal("outcome")
                        .then(Commands.literal("now").executes(c -> outcome(c, null, true)))
                        .then(Commands.literal("reset").executes(c -> outcome(c, Outcome.PENDING, false)))
                        .then(Commands.literal("force")
                                .then(Commands.literal("betrayal").executes(c -> outcome(c, Outcome.BETRAYAL, false)))
                                .then(Commands.literal("awaken").executes(c -> outcome(c, Outcome.SOUL_AWAKENING, false)))
                                .then(Commands.literal("ordinary").executes(c -> outcome(c, Outcome.ORDINARY, false)))))
                .then(Commands.literal("give").then(Commands.literal("genesis_core")
                        .executes(c -> give(c, c.getSource().getPlayerOrException()))
                        .then(Commands.argument("player", EntityArgument.player()).executes(c -> give(c, EntityArgument.getPlayer(c, "player"))))))));
    }

    private static int spawn(CommandContext<CommandSourceStack> c, ServerPlayer owner) {
        AvatarRace race = AvatarRaces.get(StringArgumentType.getString(c, "race"));
        if (race == null) { c.getSource().sendFailure(Component.literal("Unknown race.")); return 0; }
        if (!(owner.level() instanceof ServerLevel level)) return 0;
        Random rng = new Random(owner.getRandom().nextLong());
        double luck = LuckPoller.total(owner);
        int[] stats = race.requiresDndRoll() ? StatRoller.roll(rng, StatRoller.usesFiveDice(luck, AvatarConfig.luckBonusThreshold())) : new int[]{12, 12, 12, 12, 12, 12};
        AvatarPersonality first = AvatarPersonality.values()[rng.nextInt(AvatarPersonality.values().length)];
        AppearanceData look = DynamicAppearanceEngine.sanitize(AppearanceData.defaults(race.id(), race.defaultScale(), rng.nextLong()), race);
        AvatarEntity a = AvatarCreation.spawn(owner, level, race, stats, luck, first, null, look, "");
        if (a == null) { c.getSource().sendFailure(Component.literal("No room to spawn.")); return 0; }
        c.getSource().sendSuccess(() -> Component.literal("Spawned a " + race.displayName() + " Avatar (" + first.displayName + ") for " + owner.getGameProfile().getName() + "."), true);
        return 1;
    }

    private static int list(CommandContext<CommandSourceStack> c) {
        List<AvatarManager.Row> rows = AvatarManager.get(c.getSource().getServer()).all();
        if (rows.isEmpty()) { c.getSource().sendSuccess(() -> Component.literal("No Avatars."), false); return 0; }
        for (AvatarManager.Row r : rows) {
            String creator = c.getSource().getServer().getProfileCache() == null ? r.creator.toString()
                    : c.getSource().getServer().getProfileCache().get(r.creator).map(p -> p.getName()).orElse(r.creator.toString());
            String line = (r.name.isEmpty() ? "(unnamed)" : r.name) + " of " + creator + ": " + r.state + " in " + r.dim + " at " + Math.round(r.x) + " " + Math.round(r.y) + " " + Math.round(r.z);
            c.getSource().sendSuccess(() -> Component.literal(line), false);
        }
        return rows.size();
    }

    private static AvatarEntity nearest(CommandContext<CommandSourceStack> c) {
        CommandSourceStack s = c.getSource();
        List<AvatarEntity> near = s.getLevel().getEntitiesOfClass(AvatarEntity.class, new net.minecraft.world.phys.AABB(s.getPosition(), s.getPosition()).inflate(64));
        return near.stream().min(Comparator.comparingDouble(a -> a.distanceToSqr(s.getPosition()))).orElse(null);
    }

    private static int info(CommandContext<CommandSourceStack> c) {
        AvatarEntity a = nearest(c);
        if (a == null) { c.getSource().sendFailure(Component.literal("No Avatar within 64 blocks.")); return 0; }
        for (String line : a.infoLines()) c.getSource().sendSuccess(() -> Component.literal(line), false);
        return 1;
    }

    private static int kill(CommandContext<CommandSourceStack> c) {
        AvatarEntity a = nearest(c);
        if (a == null) { c.getSource().sendFailure(Component.literal("No Avatar within 64 blocks.")); return 0; }
        a.kill();
        c.getSource().sendSuccess(() -> Component.literal("Killed " + a.avatarName() + "."), true);
        return 1;
    }

    private static int outcome(CommandContext<CommandSourceStack> c, Outcome forced, boolean decide) {
        AvatarEntity a = nearest(c);
        if (a == null) { c.getSource().sendFailure(Component.literal("No Avatar within 64 blocks.")); return 0; }
        if (decide) AvatarOutcomes.resolveTimer(a);
        else AvatarOutcomes.apply(a, forced);
        c.getSource().sendSuccess(() -> Component.literal(a.avatarName() + " is now " + a.outcomeState().name().toLowerCase(java.util.Locale.ROOT) + "."), true);
        return 1;
    }

    private static int give(CommandContext<CommandSourceStack> c, ServerPlayer target) {
        ItemStack core = AvatarItems.coreStack();
        if (!target.getInventory().add(core)) target.drop(core, false);
        c.getSource().sendSuccess(() -> Component.literal("Gave a Genesis Core to " + target.getGameProfile().getName() + "."), true);
        return 1;
    }
}
