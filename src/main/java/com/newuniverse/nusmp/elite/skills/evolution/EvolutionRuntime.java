package com.newuniverse.nusmp.elite.skills.evolution;

import com.mojang.logging.LogUtils;
import com.newuniverse.nusmp.NUConfig;
import com.newuniverse.nusmp.NUGameRules;
import com.newuniverse.nusmp.elite.skills.EliteSkillsConfig;
import com.newuniverse.nusmp.skill.EliteBridge;
import com.newuniverse.nusmp.skill.SkillMsg;
import io.github.manasmods.manascore.skill.api.ManasSkill;
import io.github.manasmods.manascore.skill.api.ManasSkillInstance;
import io.github.manasmods.manascore.skill.api.SkillAPI;
import io.github.manasmods.manascore.skill.api.Skills;
import net.minecraft.ChatFormatting;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.ResourceManagerReloadListener;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.event.AddReloadListenerEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import org.slf4j.Logger;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * Why: the Minecraft side of the data-driven evolution engine. It loads {@code data/nusmp/elite/skills/evolutions.json} (every data pack's copy, lowest priority first, later packs override per
 * path) on each data reload into an {@link EvolutionTable}; every few seconds it looks at each player's skills and, for each one that has paths in the table, asks {@link EvolutionEngine} whether
 * ALL the requirements are met; when they are it pays the offerings, teaches the player the target skill (carrying the old skill's data over, like the original {@code SkillEvolution}), forgets the old one if
 * {@code evolution.replacesBase} says so, and announces it. This is an ADDITIONAL path: the original mastery-based Unique to Ultimate evolution ({@code SkillEvolution}) is untouched and keeps
 * running, so no old evolution changes. Nothing happens when the master switch or {@code evolution.enabled} is off, or the {@code skillEvolution} game rule is false.
 */
public final class EvolutionRuntime {
    private EvolutionRuntime() {}

    private static final Logger LOG = LogUtils.getLogger();
    public static final ResourceLocation FILE = ResourceLocation.fromNamespaceAndPath("nusmp", "elite/skills/evolutions.json");
    private static volatile EvolutionTable table = new EvolutionTable();

    public static EvolutionTable table() { return table; }

    /** The skill ids that have at least one path (kill counters are kept for these). */
    public static Set<String> baseSkills() {
        Set<String> out = new LinkedHashSet<>();
        for (EvolutionTable.Path p : table.paths()) out.add(p.from());
        return out;
    }

    public static void register(IEventBus game) {
        game.addListener((AddReloadListenerEvent e) -> e.addListener(new Loader()));
        game.addListener(EvolutionRuntime::onPlayerTick);
    }

    /** Loads and installs the table from a resource manager (also used by the self-test). */
    public static final class Loader implements ResourceManagerReloadListener {
        @Override
        public void onResourceManagerReload(ResourceManager manager) {
            EvolutionTable t = new EvolutionTable();
            try {
                for (Resource res : manager.getResourceStack(FILE)) {                   // lowest priority pack first
                    try (InputStream in = res.open()) {
                        t.add(new String(in.readAllBytes(), StandardCharsets.UTF_8), res.sourcePackId());
                    } catch (IOException | RuntimeException e) {
                        LOG.warn("[nusmp] Elite evolutions file of {} could not be read: {}", res.sourcePackId(), e.toString());
                    }
                }
            } catch (RuntimeException e) {
                LOG.warn("[nusmp] Elite evolutions could not be loaded", e);
            }
            for (String err : t.errors()) LOG.warn("[nusmp] Elite evolutions: {}", err);
            table = t;
            LOG.info("[nusmp] Elite evolutions: {} paths loaded", t.size());
        }
    }

    // ================================================================== the check
    static void onPlayerTick(PlayerTickEvent.Post e) {
        if (!(e.getEntity() instanceof ServerPlayer p) || p.level().isClientSide) return;
        if (!EliteSkillsConfig.enabled() || !EliteSkillsConfig.get(EliteSkillsConfig.EVOLUTION_ENABLED)) return;
        int every = Math.max(20, EliteSkillsConfig.get(EliteSkillsConfig.EVOLUTION_CHECK_SECONDS) * 20);
        if (p.tickCount % every != every / 2 || table.size() == 0) return;
        try {
            if (NUGameRules.skillEvolution(p.level())) check(p);
        } catch (RuntimeException | LinkageError broken) {
            LOG.warn("[nusmp] Elite evolution check failed for {}", p.getName().getString(), broken);
        }
    }

    /** One look at the player's skills; at most one evolution per call. Returns true if one happened. */
    public static boolean check(ServerPlayer player) {
        Skills skills = SkillAPI.getSkillsFrom(player);
        PlayerEvolutionContext ctx = new PlayerEvolutionContext(player);
        for (ManasSkillInstance inst : new ArrayList<>(skills.getLearnedSkills())) {
            String from = inst.getSkillId().toString();
            for (EvolutionTable.Path path : table.from(from)) {
                ResourceLocation toId = ResourceLocation.tryParse(path.to());
                ManasSkill target = toId == null ? null : SkillAPI.getSkillRegistry().get(toId);
                if (target == null || skills.getSkill(toId).isPresent()) continue;
                EvolutionEngine.Outcome o = EvolutionEngine.check(path, ctx);
                if (o.ok()) {
                    if (evolve(player, skills, inst, target, path, ctx)) return true;
                } else if (player.tickCount % 6000 < 200 && ctx.ownsMastered(from)) {
                    // the skill is ready by itself: remind the player now and then of what is still missing
                    player.displayClientMessage(SkillMsg.t("elite.evolution.needs", "Your mastered %s can evolve into %s. Still needed: %s.", inst.getDisplayName(), target.getName(),
                            String.join("; ", o.unmet())).withStyle(ChatFormatting.GRAY), false);
                }
            }
        }
        return false;
    }

    /** Pays the offerings and swaps the skill. Returns false (nothing changed) if it could not. */
    public static boolean evolve(ServerPlayer player, Skills skills, ManasSkillInstance old, ManasSkill target, EvolutionTable.Path path, EvolutionContext ctx) {
        if (!EvolutionEngine.commit(path, ctx)) return false;
        ManasSkillInstance evolved = target.createDefaultInstance();
        evolved.getOrCreateTag().merge(old.getOrCreateTag().copy());
        boolean learned = skills.learnSkill(evolved, SkillMsg.t("evolution.learned", "Your skill has evolved!").withStyle(ChatFormatting.GOLD));
        if (!learned) return false;
        if (NUConfig.EVOLUTION_REPLACES_BASE.get()) skills.forgetSkill(old.getSkillId());
        player.serverLevel().playSound(null, player.blockPosition(), SoundEvents.UI_TOAST_CHALLENGE_COMPLETE, SoundSource.PLAYERS, 1.0F, 0.8F);
        var server = player.getServer();
        if (server != null && EliteSkillsConfig.get(EliteSkillsConfig.EVOLUTION_BROADCAST)) {
            server.getPlayerList().broadcastSystemMessage(
                    SkillMsg.t("voice", "[Voice of the World] ").withStyle(ChatFormatting.GOLD)
                            .append(SkillMsg.t("elite.evolution.confirmed", "Confirmed. %s's skill %s has evolved into %s.", player.getName().copy().withStyle(ChatFormatting.WHITE),
                                    old.getDisplayName().copy().withStyle(ChatFormatting.AQUA), target.getName().copy().withStyle(ChatFormatting.LIGHT_PURPLE, ChatFormatting.BOLD))
                                    .withStyle(ChatFormatting.WHITE)), false);
        }
        EliteBridge.burst(player.serverLevel(), player.position().add(0, 1, 0), false, 18, 0.8);
        return true;
    }

    /** All the requirement lines of the paths from a skill (for tooltips and the self-test). */
    public static List<String> describe(String from) {
        List<String> out = new ArrayList<>();
        for (EvolutionTable.Path p : table.from(from)) {
            for (EvolutionTable.Req r : p.reqs()) out.add(p.to() + ": " + r.trigger().describe(r.params(), from));
        }
        return out;
    }
}
