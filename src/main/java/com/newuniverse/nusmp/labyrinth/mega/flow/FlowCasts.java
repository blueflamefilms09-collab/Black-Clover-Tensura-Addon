package com.newuniverse.nusmp.labyrinth.mega.flow;

import com.newuniverse.nusmp.book.GrimoireBook;
import com.newuniverse.nusmp.labyrinth.mega.MegaDungeon;
import dev.architectury.event.EventResult;
import io.github.manasmods.manascore.skill.api.ManasSkill;
import io.github.manasmods.manascore.skill.api.ManasSkillInstance;
import io.github.manasmods.manascore.skill.api.SkillEvents;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;

/**
 * Why: the flow rewards and punishes a CAST, so it has to hear every cast, and this mod has more than one way to cast. The central door is the
 * grimoire: every one of the 105 magic families (and every merged magic) casts through {@code GrimoireBook.castPage}, which reports the finished cast to
 * {@code QuestHooks.onCast(player, magic, pageId, costPercent)}. {@link #onCast} has the same arguments and is hooked right there (two added lines in
 * {@code castPage}, given to the lead), so a cast is heard as FINISHED only when it SUCCEEDED (the page was unlocked, off cooldown and paid for), which is
 * exactly when a rejection should cost something. A spell that hurts instantly deals its damage before it is reported as finished, so {@link #beforeCast} is
 * hooked just before the spell runs and tells the tracker "this tick's damage comes from a cast of this alignment" (see {@link FlowTracker}); it opens no window and
 * rejects nothing, so a page that fails costs nothing. The second door is any other ManasCore skill, which includes the Tensura skills and magics that relics grant: ManasCore announces every
 * skill activation, and {@link #registerSkillHook} listens to it (the pattern {@code BossRulesHooks} already uses) for skills that are NOT grimoires (a grimoire's
 * key press is not yet a cast; its cast arrives through the first door), reading the skill's registry id through the same table. That door cannot know whether
 * the activation will succeed, so it skips a skill that is on cooldown; the rejection's own cooldown covers the rest. Whatever the door, the result is the same:
 * the cast is classified (the page first, then its family, see {@link MagicAlignments#classify}), remembered for the damage that follows (a grimoire cast of
 * any alignment is remembered, so a plain spell ends the window of an earlier Light one; the second door remembers only Light and Dark skills, because a utility
 * skill or a toggle is not a spell), and, if it opposes the flow of the place the caster stands in, rejected. Casts outside the Mega-Dungeon are not remembered at all. Relic actives and Djinn moves are not casts of
 * either kind and do not reach this class; {@link #onCastIds} is the public door for any code that wants to report one.
 */
public final class FlowCasts {
    private FlowCasts() {}

    /**
     * A grimoire page was cast successfully (called from {@code QuestHooks.onCast} with its own arguments: {@code magic} is the family's enum name such as
     * {@code FLAME}, {@code pageId} the page's id). Never throws.
     */
    public static void onCast(ServerPlayer player, String magic, String pageId, double costPercent) {
        onCastIds(player, pageId, magic);
    }

    /**
     * A grimoire page is about to run (called from {@code GrimoireBook.castPage} just before the spell is executed, with the book's {@code magicId()} and the
     * page's id). Marks this tick's damage as coming from a cast of the page's alignment; changes nothing else. Never throws.
     */
    public static void beforeCast(ServerPlayer player, String magic, String pageId) {
        try {
            if (player == null || !FlowConfig.get(FlowConfig.ENABLED)) return;
            if (!(player.level() instanceof ServerLevel level) || !MegaDungeon.in(level)) return;
            FlowTracker.shared().beginCast(player.getUUID(), AlignmentRegistry.classify(pageId, magic), level.getGameTime());      // a neutral page counts too: its instant damage is not the last Light cast's
        } catch (RuntimeException ex) {
            FlowEvents.warn(ex);
        }
    }

    /** Reports a cast named by these ids, most specific first (a page, then a family, or a skill id). Never throws. */
    public static void onCastIds(ServerPlayer player, String... ids) {
        try {
            if (player == null || !FlowConfig.get(FlowConfig.ENABLED)) return;
            cast(player, AlignmentRegistry.classify(ids));
        } catch (RuntimeException ex) {
            FlowEvents.warn(ex);
        }
    }

    /** Remembers the cast and rejects it if the place opposes it. */
    static void cast(ServerPlayer p, Alignment a) {
        if (!(p.level() instanceof ServerLevel level) || !MegaDungeon.in(level)) return;
        FlowTracker.shared().recordCast(p.getUUID(), a, level.getGameTime());
        if (a == Alignment.NEUTRAL) return;
        Alignment flow = FlowEvents.flowAt(level, FlowEvents.posOf(p));
        if (FlowRules.outcome(flow, a) == FlowRules.Outcome.REJECT) Rejection.apply(p, a, flow);
    }

    private static void castSafely(ServerPlayer p, Alignment a) {
        try {
            cast(p, a);
        } catch (RuntimeException ex) {
            FlowEvents.warn(ex);
        }
    }

    // ---------------------------------------------------------------- the second door: other skills
    /** Listens to ManasCore's skill activations (once, from the game-bus registration). */
    static void registerSkillHook() {
        SkillEvents.ACTIVATE_SKILL.register((skill, user, key, mode) -> {
            onSkill(skill.get(), user, mode);
            return EventResult.pass();
        });
    }

    private static void onSkill(ManasSkillInstance instance, LivingEntity user, int mode) {
        try {
            if (!(user instanceof ServerPlayer p) || instance == null) return;
            if (!FlowConfig.get(FlowConfig.ENABLED) || !FlowConfig.get(FlowConfig.SKILL_HOOK)) return;
            ManasSkill skill = instance.getSkill();
            if (skill == null || skill instanceof GrimoireBook || instance.onCoolDown(mode)) return;
            ResourceLocation id = skill.getRegistryName();
            if (id == null) return;
            Alignment a = AlignmentRegistry.classify(id.toString());
            if (a == Alignment.NEUTRAL) return;                                              // a utility skill or a toggle is not a spell: it must not end the window of a real one
            MinecraftServer server = p.getServer();
            if (server != null && !server.isSameThread()) server.execute(() -> castSafely(p, a));   // the world is touched on the server thread only
            else cast(p, a);
        } catch (RuntimeException ex) {
            FlowEvents.warn(ex);
        }
    }
}
