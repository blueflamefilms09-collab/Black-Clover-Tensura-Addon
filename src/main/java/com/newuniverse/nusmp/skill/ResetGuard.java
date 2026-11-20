package com.newuniverse.nusmp.skill;

import com.newuniverse.nusmp.multiverse.MultiverseConfig;
import dev.architectury.event.EventResult;
import io.github.manasmods.manascore.network.api.util.Changeable;
import io.github.manasmods.manascore.skill.api.ManasSkillInstance;
import io.github.manasmods.manascore.skill.api.SkillEvents;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Why (3.42.0, owner request: "none of my skills can be rerolled with the basic Tensura skill [reset] and full reset"): Tensura's Reset Scrolls (skill reset and full reset) forget the skills a player holds,
 * and that used to include every skill of this mod. Now a skill of this mod (namespace {@code anime_multiverse}: grimoire, Shishou, the Veldanava-family, Evolution and Unique skills, all of them) cannot be removed by
 * a Reset Scroll. Two layers: (1) ManasCore's cancellable {@code REMOVE_SKILL} event is refused when the removal comes out of Tensura's {@code ResetScrollItem} (found on the call stack, which is only walked when
 * one of our skills is being removed, a rare event); (2) best effort, our ids are added to {@code ResetScrollItem.NO_RESET} when that list can be changed. Everything else still works: this mod's own removals
 * (a grimoire swap, {@code /multiverse ... take}, revoking Shishou) are not scroll calls, and the admin command {@code /tensura reset} is not blocked. Server side only; no state but a 64-entry message throttle;
 * server setting {@code guard.resetScrollGuard} (default on) restores the old behaviour.
 */
public final class ResetGuard {
    private static final String SCROLL_CLASS = ResetRules.SCROLL_CLASS, NAMESPACE = ResetRules.NAMESPACE;

    private static final org.slf4j.Logger LOGGER = com.mojang.logging.LogUtils.getLogger();
    /** Who was told when: at most 64 players, oldest dropped (a reset forgets dozens of skills in one tick; the message is sent once). */
    private static final Map<UUID, Long> TOLD = new LinkedHashMap<>(16, 0.75f, false) {
        @Override
        protected boolean removeEldestEntry(Map.Entry<UUID, Long> e) { return size() > 64; }
    };

    private ResetGuard() {}

    public static boolean enabled() { return MultiverseConfig.get(MultiverseConfig.RESET_SCROLL_GUARD); }

    /** 4.67.0: true (default) = only soul-tied skills survive a scroll (and are switched off); false = every skill of ours survives, as in 3.42.0. */
    public static boolean soulOnly() { return MultiverseConfig.get(MultiverseConfig.RESET_SOUL_ONLY); }

    /** Set once the remove-skill event took the hook (the self-test reads it). */
    private static volatile boolean hooked;

    public static boolean hooked() { return hooked; }

    // ---------------------------------------------------------------- the event
    /** Hooks ManasCore's remove-skill event. A missing or changed event cannot break loading. */
    public static void register() {
        try {
            SkillEvents.REMOVE_SKILL.register(ResetGuard::onRemove);
            hooked = true;
            LOGGER.info("Reset guard: the skills of {} are hooked against Tensura's Reset Scrolls.", NAMESPACE);
        } catch (Throwable t) {
            LOGGER.warn("Reset guard: the remove-skill event is not hooked ({}).", t.toString());
        }
    }

    static EventResult onRemove(ManasSkillInstance inst, LivingEntity who, Changeable<MutableComponent> message) {
        try {
            if (!(who instanceof ServerPlayer p) || inst == null || !ResetRules.isOurs(inst.getSkillId().getNamespace()) || !enabled()) return EventResult.pass();
            boolean soulOnly = soulOnly();
            String ns = inst.getSkillId().getNamespace(), path = inst.getSkillId().getPath();
            if (soulOnly && !ResetRules.soulTied(path)) return EventResult.pass();                 // 4.67.0: a plain skill of ours is reset like any other, no stack walk
            boolean scroll = StackWalker.getInstance().walk(s -> s.anyMatch(f -> ResetRules.isScrollClass(f.getClassName())));
            if (!ResetRules.refuses(true, soulOnly, ns, path, scroll)) return EventResult.pass();
            if (soulOnly) switchOff(inst, p);
            tell(p, soulOnly);
            return EventResult.interruptFalse();
        } catch (RuntimeException ignored) {
            return EventResult.pass();
        }
    }

    /** 4.67.0: the soul-tied skill stays but is switched off (its off-hook runs, so a floating grimoire, a form or an aura ends). */
    static void switchOff(ManasSkillInstance inst, ServerPlayer p) {
        try {
            if (!inst.isToggled()) return;
            inst.setToggled(false);
            inst.onToggleOff(p);
        } catch (RuntimeException e) {
            LOGGER.warn("Reset guard: could not switch {} off ({}).", inst.getSkillId(), e.toString());
        }
    }

    private static void tell(ServerPlayer p, boolean soulOnly) {
        long now = p.serverLevel().getGameTime();
        Long last = TOLD.get(p.getUUID());
        if (last != null && now - last < 40L) return;
        TOLD.put(p.getUUID(), now);
        p.displayClientMessage(Component.literal(soulOnly ? "What is bound to your soul stays, but it falls silent." : "The skills of this world cannot be reset by a scroll.").withStyle(ChatFormatting.DARK_PURPLE), true);
    }

    // ---------------------------------------------------------------- layer two: Tensura's own list
    /** Best effort, once the registries are filled: adds the ids of our skills to {@code ResetScrollItem.NO_RESET}. A list that cannot be changed is only logged; layer one still holds. */
    @SuppressWarnings("unchecked")
    public static void protectScrollLists() {
        try {
            Object list = Class.forName(SCROLL_CLASS).getField("NO_RESET").get(null);
            if (!(list instanceof List<?> raw)) return;
            List<ResourceLocation> no = (List<ResourceLocation>) raw;
            int added = 0;
            boolean soulOnly = soulOnly();
            for (var h : NUSkills.SKILLS.getEntries()) {
                if (soulOnly && !ResetRules.soulTied(h.getId().getPath())) continue;               // 4.67.0: only the soul-tied ones are on Tensura's no-reset list
                if (!no.contains(h.getId())) {
                    no.add(h.getId());
                    added++;
                }
            }
            LOGGER.info("Reset guard: {} skill id(s) added to Tensura's NO_RESET list.", added);
        } catch (UnsupportedOperationException fixed) {
            LOGGER.info("Reset guard: Tensura's NO_RESET list cannot be changed; the remove-skill hook protects the skills.");
        } catch (Throwable t) {
            LOGGER.warn("Reset guard: Tensura's NO_RESET list not touched ({}).", t.toString());
        }
    }
}
