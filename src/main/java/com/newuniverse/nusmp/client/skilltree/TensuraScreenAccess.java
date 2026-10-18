package com.newuniverse.nusmp.client.skilltree;

import com.mojang.logging.LogUtils;
import com.newuniverse.nusmp.client.skilltree.layout.MenuPlacement;
import io.github.manasmods.tensura.ability.skill.Skill;
import io.github.manasmods.tensura.client.screen.AbilityCategoriesScreen;
import io.github.manasmods.tensura.client.screen.AbilitySelectionScreen;
import io.github.manasmods.tensura.client.screen.templates.SimpleScreen;
import net.minecraft.client.gui.screens.Screen;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

/**
 * Why: Tensura's Ability Selection screen keeps the things this integration needs in PROTECTED members (which list it is showing, the search text, the template's frame) and offers no API for them. The
 * Tensura classes are API stubs here, so nothing about their layout can be assumed; each value is therefore read from the live screen by reflection, once-resolved and cached, and every read answers
 * "unknown" (null) instead of throwing when the member is missing or has another shape, so a Tensura update that renames a field turns a feature off (the panel then pins itself to the window edge, the
 * list is left alone) and never crashes the menu. The first failure of each kind is logged once. The frame is trusted only when the template's {@code scale} is 1: a template that scales its drawing would
 * make the raw values mean another coordinate space, and a panel docked by wrong numbers is worse than a panel pinned to the window edge. Client thread only.
 */
final class TensuraScreenAccess {
    private TensuraScreenAccess() {}

    private static final Field GUI_LEFT = field(SimpleScreen.class, "guiLeft"), GUI_TOP = field(SimpleScreen.class, "guiTop"), GUI_RIGHT = field(SimpleScreen.class, "guiRight"),
            GUI_BOTTOM = field(SimpleScreen.class, "guiBottom"), SCALE = field(SimpleScreen.class, "scale");
    private static final Field SKILL_FILTER = field(AbilitySelectionScreen.class, "skillFilter"), ABILITY_TYPE = field(AbilitySelectionScreen.class, "abilityType"),
            NAME_FILTER = field(AbilitySelectionScreen.class, "nameFilter");
    private static final Method UPDATE_FILTERED = method(AbilitySelectionScreen.class, "updateFilteredAbilities");
    private static boolean warnedFrame, warnedList;

    private static Field field(Class<?> c, String name) {
        try {
            Field f = c.getDeclaredField(name);
            f.setAccessible(true);
            return f;
        } catch (ReflectiveOperationException | RuntimeException e) {
            LogUtils.getLogger().warn("[nusmp] tensura menu: {}.{} could not be read, the matching feature is off", c.getSimpleName(), name);
            return null;
        }
    }

    private static Method method(Class<?> c, String name) {
        try {
            Method m = c.getDeclaredMethod(name);
            m.setAccessible(true);
            return m;
        } catch (ReflectiveOperationException | RuntimeException e) {
            LogUtils.getLogger().warn("[nusmp] tensura menu: {}.{}() could not be found, the matching feature is off", c.getSimpleName(), name);
            return null;
        }
    }

    /** Tensura's frame in GUI pixels, or null when it cannot be read or cannot be trusted. */
    static MenuPlacement.Frame frame(Screen s) {
        try {
            if (GUI_LEFT == null || GUI_TOP == null || GUI_RIGHT == null || GUI_BOTTOM == null) return null;
            if (SCALE != null) {
                float scale = SCALE.getFloat(s);
                if (Math.abs(scale - 1f) > 0.001f && scale != 0f) return null;
            }
            return new MenuPlacement.Frame(GUI_LEFT.getInt(null), GUI_TOP.getInt(null), GUI_RIGHT.getInt(null), GUI_BOTTOM.getInt(null));
        } catch (ReflectiveOperationException | RuntimeException e) {
            if (!warnedFrame) {
                warnedFrame = true;
                LogUtils.getLogger().warn("[nusmp] tensura menu: the frame of Tensura's screen could not be read; the panel is pinned to the window edge", e);
            }
            return null;
        }
    }

    /** Where Tensura's list ends, from the scroll-bar getters of the screen itself, or null. */
    static MenuPlacement.Inner inner(AbilitySelectionScreen s) {
        try {
            if (frame(s) == null) return null;
            int right = s.getScrollBarX() + Math.max(0, s.getScrollBarWidth());
            return new MenuPlacement.Inner(right, s.getScrollBarY(), s.getScrollBarTotalSpace());
        } catch (RuntimeException | LinkageError e) {
            return null;
        }
    }

    /** True when this is the list of Unique skills (or an unfiltered skill list): the only screens where the stand-ins belong. */
    static boolean showsUniqueSkills(AbilitySelectionScreen s) {
        try {
            if (ABILITY_TYPE == null || SKILL_FILTER == null) return false;
            if (ABILITY_TYPE.get(s) != AbilityCategoriesScreen.AbilityType.SKILL) return false;
            Object filter = SKILL_FILTER.get(s);
            return filter == null || filter == Skill.SkillType.UNIQUE;
        } catch (ReflectiveOperationException | RuntimeException e) {
            return false;
        }
    }

    /** The search text of the screen, or an empty string. */
    static String nameFilter(AbilitySelectionScreen s) {
        try {
            return NAME_FILTER == null ? "" : String.valueOf(NAME_FILTER.get(s));
        } catch (ReflectiveOperationException | RuntimeException e) {
            return "";
        }
    }

    /** Asks the screen to rebuild its filtered list from its full list (its own method, so its search and ordering rules apply). False when that could not be done. */
    static boolean refilter(AbilitySelectionScreen s) {
        try {
            if (UPDATE_FILTERED == null) return false;
            UPDATE_FILTERED.invoke(s);
            return true;
        } catch (ReflectiveOperationException | RuntimeException e) {
            if (!warnedList) {
                warnedList = true;
                LogUtils.getLogger().warn("[nusmp] tensura menu: the screen's list could not be refreshed", e);
            }
            return false;
        }
    }

    static boolean isAbilitySelection(Screen s) { return s instanceof AbilitySelectionScreen; }
}
