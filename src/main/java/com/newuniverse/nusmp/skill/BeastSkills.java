package com.newuniverse.nusmp.skill;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.newuniverse.nusmp.NUConfig;
import com.newuniverse.nusmp.skill.rules.BeastSkillRules;
import com.newuniverse.nusmp.skill.rules.BeastSkillRules.Spec;
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
 * 3.58.0: the 27 Beast Will skill pairs (a Unique that evolves into an Ultimate, {@link BeastSkill}): registration, the evolution check, the grant when a will reaches full mastery and the admin command. The
 * evolution is the same rule as {@link SkillEvolution} (a MASTERED Unique plus the server's minimum EP), run from the same 5-second player check right after {@link DbSkills#evolve}; the table is kept apart so
 * the older evolution table stays exactly as it was. Obtain: {@code /multiverse beastskill give <player> <id> [unique|ultimate]} (permission 2), {@code /tensura edit <player> ability grant nusmp:bw_<key>_unique},
 * or master the Beast Will itself (the Unique of that will is granted once, setting {@code beastSkillGrantOnMastery}).
 */
public final class BeastSkills {
    private BeastSkills() {}

    public static final Map<String, DeferredHolder<ManasSkill, BeastSkill>> UNIQUE = new LinkedHashMap<>();
    public static final Map<String, DeferredHolder<ManasSkill, BeastSkill>> ULTIMATE = new LinkedHashMap<>();

    /** The persistent-data key prefix of "this will already granted its Unique skill once". */
    static final String GRANTED = "nusmp_bws_g_";

    /** Registers the 54 skills; called once from {@link NUSkills}. Returns how many. */
    static int registerAll(DeferredRegister<ManasSkill> reg) {
        for (Spec s : BeastSkillRules.SPECS) {
            UNIQUE.put(s.key(), reg.register(s.uniqueId(), () -> new BeastSkill(s, false)));
            ULTIMATE.put(s.key(), reg.register(s.id(), () -> new BeastSkill(s, true)));
        }
        return UNIQUE.size() + ULTIMATE.size();
    }

    /** The evolution check of one player (the caller runs it every 5 seconds); at most one evolution per call. */
    static void evolve(ServerPlayer player, Skills skills) {
        if (!NUConfig.BEAST_SKILLS_ENABLED.get()) return;
        for (Spec s : BeastSkillRules.SPECS) {
            ManasSkill base = UNIQUE.get(s.key()).get(), ult = ULTIMATE.get(s.key()).get();
            Optional<ManasSkillInstance> owned = skills.getSkill(base.getRegistryName());
            if (owned.isEmpty() || !owned.get().isMastered(player) || skills.getSkill(ult.getRegistryName()).isPresent()) continue;
            double need = NUConfig.EVOLUTION_MIN_EP.get();
            IExistence ex = TensuraStorages.getExistenceFrom(player);
            if (need > 0 && (ex == null || ex.getEP() < need)) continue;
            SkillEvolution.evolve(player, skills, owned.get(), ult);
            return;
        }
    }

    /**
     * The hook of the Beast Will manager: the will {@code willId} has reached full mastery. Grants the Unique skill of that beast once per player (a stored flag; nothing if the player already has the
     * Unique or the Ultimate). Cheap: a flag read after the first time. Does nothing when the skills or the grant setting are off.
     */
    public static void grantOnMastery(ServerPlayer p, String willId) {
        if (p == null || willId == null || !NUConfig.BEAST_SKILLS_ENABLED.get() || !NUConfig.BEAST_SKILL_GRANT_ON_MASTERY.get()) return;
        Spec s = BeastSkillRules.spec(willId);
        if (s == null || !UNIQUE.containsKey(s.key())) return;
        String flag = GRANTED + s.key();
        if (p.getPersistentData().getBoolean(flag)) return;
        p.getPersistentData().putBoolean(flag, true);
        Skills skills = SkillAPI.getSkillsFrom(p);
        ManasSkill unique = UNIQUE.get(s.key()).get();
        if (skills.getSkill(unique.getRegistryName()).isPresent() || skills.getSkill(ULTIMATE.get(s.key()).get().getRegistryName()).isPresent()) return;
        skills.learnSkill(unique.createDefaultInstance(), Component.literal("Mastering the will grants you " + unique.getName().getString() + ".").withStyle(ChatFormatting.GOLD));
    }

    public static LiteralArgumentBuilder<CommandSourceStack> node() {
        return Commands.literal("beastskill").requires(c -> c.hasPermission(2))
                .then(Commands.literal("give").then(Commands.argument("player", EntityArgument.player())
                        .then(Commands.argument("id", StringArgumentType.word())
                                .suggests((c, b) -> SharedSuggestionProvider.suggest(BeastSkillRules.SPECS.stream().map(Spec::id), b))
                                .executes(c -> give(c.getSource(), EntityArgument.getPlayer(c, "player"), StringArgumentType.getString(c, "id"), false))
                                .then(Commands.literal("unique").executes(c -> give(c.getSource(), EntityArgument.getPlayer(c, "player"), StringArgumentType.getString(c, "id"), false)))
                                .then(Commands.literal("ultimate").executes(c -> give(c.getSource(), EntityArgument.getPlayer(c, "player"), StringArgumentType.getString(c, "id"), true))))))
                .then(Commands.literal("list").executes(c -> {
                    c.getSource().sendSuccess(() -> Component.literal(String.join(", ", BeastSkillRules.SPECS.stream().map(Spec::id).toList())), false);
                    return BeastSkillRules.SPECS.size();
                }));
    }

    private static int give(CommandSourceStack src, ServerPlayer p, String id, boolean ultimate) {
        Spec s = BeastSkillRules.spec(id);
        if (s == null || !UNIQUE.containsKey(s.key())) { src.sendFailure(Component.literal("Unknown Beast Will skill '" + id + "'. Use /multiverse beastskill list.")); return 0; }
        DeferredHolder<ManasSkill, BeastSkill> h = (ultimate ? ULTIMATE : UNIQUE).get(s.key());
        Skills skills = SkillAPI.getSkillsFrom(p);
        if (skills.getSkill(h.getId()).isPresent()) { src.sendFailure(Component.literal(p.getName().getString() + " already has it.")); return 0; }
        boolean ok = skills.learnSkill(h.get().createDefaultInstance(), Component.literal("You are granted " + h.get().getName().getString() + ".").withStyle(ChatFormatting.GOLD));
        if (!ok) { src.sendFailure(Component.literal("Could not give it.")); return 0; }
        src.sendSuccess(() -> Component.literal("Gave " + h.get().getName().getString() + " to " + p.getName().getString() + "."), true);
        return 1;
    }
}
