package com.newuniverse.nusmp.client.relics.djinn.gui;

import com.newuniverse.nusmp.client.relics.djinn.DjinnClientState.Snap;
import com.newuniverse.nusmp.relics.djinn.Djinn;
import com.newuniverse.nusmp.relics.djinn.DjinnPhaseRules;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.resources.language.I18n;

import java.util.EnumMap;
import java.util.Map;

/**
 * The game side of the active Djinn panel: turns the client's Djinn state into a {@link GuiState}, picks the cached {@link GuiTheme} of the Djinn
 * (every Djinn of the table gets one from its own row, so a Djinn added later needs no code here), tracks when the panel was first shown (the summon
 * intro) and hands everything to the pure {@link DjinnGuiPainter}. Called from {@code DjinnScreenFx.render} when the config style is NEW.
 * Nothing is allocated per frame: the state, the canvas and the painter are reused.
 */
public final class DjinnGui {
    private DjinnGui() {}

    private static final GuiState STATE = new GuiState();
    private static final DjinnGuiPainter PAINTER = new DjinnGuiPainter();
    private static final GfxCanvas CANVAS = new GfxCanvas();
    private static final Map<Djinn, GuiTheme[]> THEMES = new EnumMap<>(Djinn.class);

    private static boolean visible;
    private static Djinn shownDjinn;
    private static float introStart;
    private static String labelLang;

    /** The panel is not on screen this frame: the next time it shows, the summon intro plays again. */
    public static void hide() { visible = false; }

    /** Logging out: forget everything about the screen. */
    public static void clear() {
        visible = false;
        shownDjinn = null;
        THEMES.clear();
        labelLang = null;
    }

    public static GuiTheme theme(Djinn d, boolean dark) {
        GuiTheme[] pair = THEMES.computeIfAbsent(d, k -> new GuiTheme[2]);
        int i = dark ? 1 : 0;
        if (pair[i] == null) pair[i] = GuiTheme.of(d, dark);
        return pair[i];
    }

    private static void refreshLabels(Minecraft mc) {
        String lang = mc.getLanguageManager().getSelected();
        if (lang.equals(labelLang)) return;
        labelLang = lang;
        PAINTER.setLabels(GuiLabels.from((key, fallback) -> I18n.exists(key) ? I18n.get(key) : fallback));
    }

    /**
     * @param gt      the game time of this frame, {@code partial} the partial tick
     */
    public static void render(GuiGraphics g, Minecraft mc, Snap s, Djinn d, long gt, float partial) {
        float now = gt + partial;
        if (!visible || shownDjinn != d) {
            visible = true;
            shownDjinn = d;
            introStart = now;
        }
        refreshLabels(mc);
        GuiState st = STATE;
        st.phase = s.phase();
        st.prev = s.prev();
        st.phaseAge = now - s.since();
        float age = now - s.at();
        st.timeLeft = Math.max(0f, s.left() - age);
        st.timeMax = s.phase() >= 4 ? DjinnPhaseRules.EXTREME_CHARGE : s.phase() == 1 ? DjinnPhaseRules.LESSER_TICKS : DjinnPhaseRules.FULL_MAX_TICKS;
        st.magoi = s.magoi();
        st.assim = s.assim();
        st.trust = s.trust();
        st.lockLeft = Math.max(0f, s.lock() - age);
        st.lockMax = DjinnPhaseRules.LOCKOUT_MAX;
        st.coolLeft = Math.max(0f, s.cooldown() - age);
        st.coolMax = DjinnPhaseRules.EXTREME_COOLDOWN;
        st.dark = s.dark();
        st.pulse = s.pulse(gt, partial);
        st.introAge = now - introStart;
        st.time = (gt % 1_000_000L) + partial;
        GuiLayout.Anchor anchor = DjinnGuiConfig.anchor();
        CANVAS.set(g, mc.font);
        PAINTER.paint(CANVAS, theme(d, st.dark), st, GuiLayout.originX(anchor, g.guiWidth()), GuiLayout.originY(anchor, g.guiHeight()));
    }
}
