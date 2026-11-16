package com.newuniverse.nusmp.skill;

import com.mojang.logging.LogUtils;
import com.newuniverse.nusmp.NUConfig;
import com.newuniverse.nusmp.skill.rules.ArgonautMergeRules;
import io.github.manasmods.manascore.skill.api.ManasSkillInstance;
import io.github.manasmods.manascore.skill.api.SkillAPI;
import io.github.manasmods.manascore.skill.api.Skills;
import net.minecraft.ChatFormatting;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.common.util.FakePlayer;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import org.slf4j.Logger;

import java.util.Optional;

/**
 * Why: Firebolt was folded into Argonaut (docs/argonaut_rework.md). This holds the SERVER setting that turns the merge off ({@code danmachi.argonautFireboltMerge}) and the
 * one-off migration of a saved Firebolt holder: at login and respawn, a player who holds {@code firebolt} gets {@code argonaut} (unless they already hold it or its Ultimate) and
 * the standalone Firebolt is forgotten; the Argonaut / Hero's Argonaut instance is stamped {@code FireboltMigrated} so that, with the setting off again, the next login gives
 * the standalone Firebolt back. No static per-player state, no tick work: two registry reads per login. The Firebolt skill itself stays registered and castable (old saves, commands).
 */
public final class ArgonautMerge {
    private ArgonautMerge() {}

    private static final Logger LOG = LogUtils.getLogger();
    /** Tag on the Argonaut / Hero's Argonaut instance: its holder's Firebolt was moved here. */
    static final String MIGRATED = "FireboltMigrated";
    private static boolean done;

    /** The SERVER setting; true (the default) before the config is loaded, e.g. on a client menu. */
    public static boolean enabled() {
        try { return NUConfig.ARGONAUT_FIREBOLT_MERGE.get(); } catch (IllegalStateException notLoaded) { return NUConfig.ARGONAUT_FIREBOLT_MERGE.getDefault(); }
    }

    /** The SERVER setting argonautWikiCanon (the wiki-faithful Argonaut), only with the merge on; true before the config is loaded. */
    public static boolean canon() {
        if (!enabled()) return false;
        try { return NUConfig.ARGONAUT_WIKI_CANON.get(); } catch (IllegalStateException notLoaded) { return NUConfig.ARGONAUT_WIKI_CANON.getDefault(); }
    }

    /** Hooks login and respawn on the game bus. Called once from the mod constructor. */
    public static synchronized void register() {
        if (done) return;
        done = true;
        NeoForge.EVENT_BUS.addListener((PlayerEvent.PlayerLoggedInEvent e) -> { if (e.getEntity() instanceof ServerPlayer p) onJoin(p); });
        NeoForge.EVENT_BUS.addListener((PlayerEvent.PlayerRespawnEvent e) -> { if (e.getEntity() instanceof ServerPlayer p) onJoin(p); });
    }

    /** Login and respawn: migrate (or restore) this player's Firebolt. Never throws: a login must not fail. */
    public static void onJoin(ServerPlayer p) {
        try {
            if (p == null || p instanceof FakePlayer) return;
            Skills skills = SkillAPI.getSkillsFrom(p);
            if (skills != null) migrate(skills, enabled());
        } catch (RuntimeException | LinkageError e) {
            LOG.warn("[anime_multiverse] could not move Firebolt into Argonaut for {}", p == null ? "?" : p.getGameProfile().getName(), e);
        }
    }

    /** Applies the merge (or its undo) to one player's skills. Returns true when anything changed. */
    static boolean migrate(Skills skills, boolean merged) {
        Optional<ManasSkillInstance> firebolt = skills.getSkill(NUSkills.FIREBOLT.getId());
        Optional<ManasSkillInstance> argonaut = skills.getSkill(NUSkills.ARGONAUT.getId());
        Optional<ManasSkillInstance> hero = skills.getSkill(NUSkills.HEROS_ARGONAUT.getId());
        Optional<ManasSkillInstance> holder = hero.isPresent() ? hero : argonaut;

        if (ArgonautMergeRules.migrates(merged, firebolt.isPresent())) {
            if (holder.isPresent()) {
                stamp(holder.get());
            } else if (ArgonautMergeRules.grantsArgonaut(argonaut.isPresent(), hero.isPresent())) {
                ManasSkillInstance fresh = NUSkills.ARGONAUT.get().createDefaultInstance();
                fresh.getOrCreateTag().putBoolean(MIGRATED, true);
                if (!skills.learnSkill(fresh, SkillMsg.t("argonaut.migrated", "Firebolt is now a mode of Argonaut.").withStyle(ChatFormatting.GOLD))) return false;
            }
            skills.forgetSkill(firebolt.get().getSkillId());
            return true;
        }
        boolean was = holder.isPresent() && holder.get().getOrCreateTag().getBoolean(MIGRATED);
        if (ArgonautMergeRules.restores(merged, was, firebolt.isPresent())) {
            ManasSkillInstance back = NUSkills.FIREBOLT.get().createDefaultInstance();
            if (!skills.learnSkill(back, SkillMsg.t("argonaut.restored", "Firebolt is its own skill again.").withStyle(ChatFormatting.GOLD))) return false;
            holder.get().getOrCreateTag().remove(MIGRATED);
            holder.get().markDirty();
            return true;
        }
        return false;
    }

    private static void stamp(ManasSkillInstance holder) {
        holder.getOrCreateTag().putBoolean(MIGRATED, true);
        holder.markDirty();
    }
}
