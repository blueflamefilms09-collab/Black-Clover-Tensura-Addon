package com.newuniverse.nusmp.skill;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.newuniverse.nusmp.NUConfig;
import com.newuniverse.nusmp.skill.rules.DbRules;
import com.newuniverse.nusmp.skill.rules.DbRules.Spec;
import io.github.manasmods.manascore.skill.api.ManasSkill;
import io.github.manasmods.manascore.skill.api.ManasSkillInstance;
import io.github.manasmods.manascore.skill.api.SkillAPI;
import io.github.manasmods.manascore.skill.api.Skills;
import io.github.manasmods.tensura.storage.TensuraStorages;
import io.github.manasmods.tensura.storage.ep.IExistence;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

/**
 * 3.56.0: the 15 Dragon Ball skill pairs (a Unique that evolves into an Ultimate, {@link DbSkill}): registration, the evolution check and the admin command. The evolution is the same rule as
 * {@link SkillEvolution} (a MASTERED Unique plus the server's minimum EP), run from the same 5-second player check; the table is kept apart so the older evolution table stays exactly as it was.
 * Obtain: {@code /multiverse dragonball give <player> <id> [unique|ultimate]} (permission 2), or {@code /tensura edit <player> ability grant nusmp:<id>_unique}.
 */
public final class DbSkills {
    private DbSkills() {}

    public static final Map<String, DeferredHolder<ManasSkill, DbSkill>> UNIQUE = new LinkedHashMap<>();
    public static final Map<String, DeferredHolder<ManasSkill, DbSkill>> ULTIMATE = new LinkedHashMap<>();

    /** Registers the 30 skills; called once from {@link NUSkills}. Returns how many. */
    static int registerAll(DeferredRegister<ManasSkill> reg) {
        for (Spec s : DbRules.SPECS) {
            UNIQUE.put(s.id(), reg.register(s.uniqueId(), () -> new DbSkill(s, false)));
            ULTIMATE.put(s.id(), reg.register(s.id(), () -> new DbSkill(s, true)));
        }
        return UNIQUE.size() + ULTIMATE.size();
    }

    /** The evolution check of one player (the caller runs it every 5 seconds); at most one evolution per call. */
    static void evolve(ServerPlayer player, Skills skills) {
        if (!NUConfig.DB_ENABLED.get()) return;
        for (Spec s : DbRules.SPECS) {
            ManasSkill base = UNIQUE.get(s.id()).get(), ult = ULTIMATE.get(s.id()).get();
            Optional<ManasSkillInstance> owned = skills.getSkill(base.getRegistryName());
            if (owned.isEmpty() || !owned.get().isMastered(player) || skills.getSkill(ult.getRegistryName()).isPresent()) continue;
            double need = NUConfig.EVOLUTION_MIN_EP.get();
            IExistence ex = TensuraStorages.getExistenceFrom(player);
            if (need > 0 && (ex == null || ex.getEP() < need)) continue;
            SkillEvolution.evolve(player, skills, owned.get(), ult);
            return;
        }
    }

    public static LiteralArgumentBuilder<CommandSourceStack> node() {
        return Commands.literal("dragonball").requires(c -> c.hasPermission(2))
                .then(Commands.literal("give").then(Commands.argument("player", EntityArgument.player())
                        .then(Commands.argument("id", StringArgumentType.word())
                                .suggests((c, b) -> SharedSuggestionProvider.suggest(DbRules.SPECS.stream().map(Spec::id), b))
                                .executes(c -> give(c.getSource(), EntityArgument.getPlayer(c, "player"), StringArgumentType.getString(c, "id"), false))
                                .then(Commands.literal("unique").executes(c -> give(c.getSource(), EntityArgument.getPlayer(c, "player"), StringArgumentType.getString(c, "id"), false)))
                                .then(Commands.literal("ultimate").executes(c -> give(c.getSource(), EntityArgument.getPlayer(c, "player"), StringArgumentType.getString(c, "id"), true))))))
                .then(Commands.literal("list").executes(c -> {
                    c.getSource().sendSuccess(() -> Component.literal(String.join(", ", DbRules.SPECS.stream().map(Spec::id).toList())), false);
                    return DbRules.SPECS.size();
                }));
    }

    private static int give(CommandSourceStack src, ServerPlayer p, String id, boolean ultimate) {
        Spec s = DbRules.spec(id);
        if (s == null || !UNIQUE.containsKey(s.id())) { src.sendFailure(Component.literal("Unknown Dragon Ball skill '" + id + "'. Use /multiverse dragonball list.")); return 0; }
        DeferredHolder<ManasSkill, DbSkill> h = (ultimate ? ULTIMATE : UNIQUE).get(s.id());
        Skills skills = SkillAPI.getSkillsFrom(p);
        if (skills.getSkill(h.getId()).isPresent()) { src.sendFailure(Component.literal(p.getName().getString() + " already has it.")); return 0; }
        boolean ok = skills.learnSkill(h.get().createDefaultInstance(), Component.literal("You are granted " + h.get().getName().getString() + ".").withStyle(ChatFormatting.GOLD));
        if (!ok) { src.sendFailure(Component.literal("Could not give it.")); return 0; }
        src.sendSuccess(() -> Component.literal("Gave " + h.get().getName().getString() + " to " + p.getName().getString() + "."), true);
        return 1;
    }
}
