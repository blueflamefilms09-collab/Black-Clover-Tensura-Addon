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
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * 3.56.0: the 15 Dragon Ball skill pairs (a Unique that evolves into an Ultimate, {@link DbSkill}): registration, the evolution check and the admin command. The evolution is the same rule as
 * {@link SkillEvolution} (a MASTERED Unique plus the server's minimum EP), run from the same 5-second player check; the table is kept apart so the older evolution table stays exactly as it was.
 * Obtain: {@code /multiverse dragonball give <player> <id> [unique|ultimate]} (permission 2), or {@code /tensura edit <player> ability grant anime_multiverse:<id>_unique}.
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

    /** The 30 holders (Uniques first, then Ultimates), for the Saiyan Prodigy's migration and arts (a copy, read only). */
    public static List<DeferredHolder<ManasSkill, DbSkill>> allHolders() {
        List<DeferredHolder<ManasSkill, DbSkill>> all = new java.util.ArrayList<>(UNIQUE.values());
        all.addAll(ULTIMATE.values());
        return all;
    }

    /** Dragon Ball rework phase 1: the old skills are removed unless the server sets {@code dragonBallLegacySkills = true}. Removed = not offered, not evolved, not usable, taken from players at login. */
    public static boolean legacy() { return NUConfig.DB_LEGACY_SKILLS.get(); }

    /** The old skills may be used: not removed ({@link #legacy()}) and not switched off ({@code dragonBallEnabled}). */
    public static boolean active() { return NUConfig.DB_ENABLED.get() && legacy(); }

    /** Login, once per player: while the skills are removed, any of the 30 ids the player still owns is forgotten (30 map lookups, no tick work, nothing sent unless something was taken). */
    public static void onLogin(PlayerEvent.PlayerLoggedInEvent e) {
        if (!(e.getEntity() instanceof ServerPlayer p) || legacy()) return;
        if (com.newuniverse.nusmp.saiyan.SaiyanProdigyHub.enabled()) return;      // the Saiyan Prodigy hub keeps what a player owns (rule 2): the old ids stay owned, inert, and feed the Prodigy's arts
        Skills skills = SkillAPI.getSkillsFrom(p);
        if (skills == null) return;
        int taken = 0;
        for (Map<String, DeferredHolder<ManasSkill, DbSkill>> table : List.of(UNIQUE, ULTIMATE))
            for (DeferredHolder<ManasSkill, DbSkill> h : table.values()) {
                try {
                    if (skills.getSkill(h.getId()).isEmpty()) continue;
                    skills.forgetSkill(h.getId());
                    taken++;
                } catch (RuntimeException ignored) { /* a skill that cannot be forgotten now is refused by active() anyway */ }
            }
        if (taken > 0) p.displayClientMessage(Component.literal("The old Dragon Ball skills were removed (" + taken + ").").withStyle(ChatFormatting.GRAY), false);
    }

    /** The evolution check of one player (the caller runs it every 5 seconds); at most one evolution per call. */
    static void evolve(ServerPlayer player, Skills skills) {
        if (!active()) return;
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
        return Commands.literal("dragonball").requires(c -> c.hasPermission(2) && legacy())   // hidden while removed; read lazily, the server config is not loaded when commands are built
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
        if (!legacy()) { src.sendFailure(Component.literal("The old Dragon Ball skills are removed on this server (dragonBallLegacySkills = false).")); return 0; }
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
