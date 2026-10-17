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
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.event.server.ServerStartedEvent;

/**
 * Why: the flow rewards and punishes a CAST, so it has to hear every cast, and this mod has more than one way to cast. The central door is the
 * grimoire: every one of the 105 magic families (and every merged magic) casts through {@code GrimoireBook.castPage}, the one place that runs a page
 * (it is private, no subclass replaces it, and {@code QuestHooks.onCast}, which reports {@code spell.cast}, is called from the same spot). Two lines
 * in {@code castPage}, given to the lead, feed this class. {@link #beforeCast} goes just before the page's spell runs and tells the tracker "this
 * tick's damage comes from a cast of this alignment" (a spell that hurts instantly deals its damage INSIDE the call that casts it, before anything
 * can report it finished; see {@link FlowTracker}); it opens no window and rejects nothing, so a page that fails costs nothing. {@link #onCast} goes
 * right after {@code QuestHooks.onCast}, which means the page was unlocked, off cooldown, paid for and did something: exactly when a rejection should
 * cost something. Both lines pass {@code magicId()} (the family's enum name, or {@code merged:flame+water} for a merged book), NOT the dominant
 * parent that {@code QuestHooks.onCast} receives, so a merged magic is classified part by part and a merge that contains a Dark parent is Dark. The
 * second door is any other ManasCore skill, which includes the Tensura skills and magics that relics grant: ManasCore announces every skill activation,
 * and {@link #registerSkillHook} listens to it (the pattern {@code BossRulesHooks} already uses) for skills that are NOT grimoires (a grimoire's key
 * press is not yet a cast; its cast arrives through the first door), reading the skill's registry id through the same table. That listener is added
 * once, when the server has started, so it runs after the listeners that veto an activation (the spell seal, the boss rules) and never counts a skill
 * that was refused. That door cannot know whether the activation will succeed, so it skips a skill that is on cooldown; the rejection's own cooldown
 * covers the rest. Whatever the door, the result is the same: the cast is classified (the page first, then its family, see
 * {@link MagicAlignments#classify}), remembered for the damage that follows (a grimoire cast of any alignment is remembered, so a plain spell ends the
 * window of an earlier Light one; the second door remembers only Light and Dark skills, because a utility skill or a toggle is not a spell) and, if it
 * opposes the flow of the place the caster stands in, rejected. Casts outside the Mega-Dungeon are not remembered at all. Relic actives and Djinn moves
 * are not casts of either kind and do not reach this class; {@link #onCastIds} is the public door for any code that wants to report one.
 */
public final class FlowCasts {
    private FlowCasts() {}

    private static boolean skillHooked;

    /**
     * A grimoire page was cast successfully (called from {@code GrimoireBook.castPage} right after {@code QuestHooks.onCast}: {@code magic} is the book's
     * {@code magicId()}, {@code pageId} the page's id). Never throws.
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
            if (!(player.level() instanceof ServerLevel level) || !MegaDungeon.in(level)) return;      // before classifying: a cast elsewhere costs one comparison
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
    /**
     * Arranges to listen to ManasCore's skill activations: the listener is added when the server has started (once per game session, whatever number of
     * worlds are opened), not while the mod is still being constructed, because ManasCore calls its listeners in the order they were added and the ones
     * that refuse an activation are added during construction.
     */
    static void registerSkillHook(IEventBus gameBus) {
        gameBus.addListener((ServerStartedEvent e) -> hookSkills());
    }

    private static synchronized void hookSkills() {
        if (skillHooked) return;
        skillHooked = true;
        SkillEvents.ACTIVATE_SKILL.register((skill, user, key, mode) -> {
            onSkill(skill.get(), user, mode);
            return EventResult.pass();
        });
    }

    private static void onSkill(ManasSkillInstance instance, LivingEntity user, int mode) {
        try {
            if (!(user instanceof ServerPlayer p) || instance == null) return;
            if (!(p.level() instanceof ServerLevel level) || !MegaDungeon.in(level)) return;       // every other dimension: one comparison, nothing else
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
