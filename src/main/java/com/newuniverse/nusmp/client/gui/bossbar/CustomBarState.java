package com.newuniverse.nusmp.client.gui.bossbar;

import com.newuniverse.nusmp.boss.bars.BossBarState;
import com.newuniverse.nusmp.boss.bars.DpsMeter;
import com.newuniverse.nusmp.boss.bars.GhostBar;
import net.minecraft.client.gui.Font;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;

import java.util.Locale;

/**
 * Why: what the client keeps for one custom boss bar: the last {@link BossBarState} the server sent, the animation clocks (entrance, phase flash, death), the damage-lag ghost and the optional
 * damage ticker, and the cached text of every label (a label is rebuilt only when what it says changes, never per frame). Client thread only.
 */
final class CustomBarState {
    /** A cached label: the component, its ready-to-draw sequence and its width; rebuilt only when {@code key} changes. */
    static final class Txt {
        Component c = Component.empty();
        FormattedCharSequence seq = FormattedCharSequence.EMPTY;
        int w;
        private Object ref;
        private long num = Long.MIN_VALUE;

        /** Sets the label when {@code ref} is not the object it was built from (reference test: a state's strings are new objects per packet). */
        void set(Font f, Component c, Object ref) {
            if (this.ref == ref && this.ref != null) return;
            put(f, c);
            this.ref = ref;
            this.num = Long.MIN_VALUE;
        }

        boolean differs(long n) { return num != n; }

        boolean isSet() { return ref != null; }

        void setNum(Font f, Component c, long n) {
            put(f, c);
            this.num = n;
            this.ref = null;
        }

        private void put(Font f, Component c) {
            this.c = c;
            this.seq = c.getVisualOrderText();
            this.w = f.width(this.seq);
        }
    }

    final java.util.UUID id;
    BossBarState s;
    BarStyles.Loaded style;
    String styleId = "";
    final GhostBar ghost = new GhostBar();
    final DpsMeter dps = new DpsMeter();
    double enterTime = Double.NaN, deathTime = Double.NaN, phaseFlashTime = Double.NEGATIVE_INFINITY, shakeStart = Double.NEGATIVE_INFINITY;
    long arrivedSeen;
    long arrivedAt;                                   // the game time the state arrived (the lair countdown runs from it)
    boolean dying, preview;
    long previewUntil;
    float previewHp = 1f;
    float lastHp = 1f;
    Component lastEventName = Component.empty();      // the vanilla event's name, kept for the death animation
    float lastProgress = 1f;
    LivingEntity portrait;
    long portraitCheck = Long.MIN_VALUE;
    int lastTick;

    final Txt name = new Txt(), title = new Txt(), phaseLine = new Txt(), pct = new Txt(), roman = new Txt(), adds = new Txt(), fighters = new Txt(), cast = new Txt(), lair = new Txt(),
            hp = new Txt(), dpsText = new Txt(), invulnerable = new Txt(), enraged = new Txt();

    CustomBarState(java.util.UUID id, BossBarState s, long now) {
        this.id = id;
        this.arrivedAt = now;
        this.s = s;
    }

    void apply(BossBarState next, long now) {
        BossBarState old = this.s;
        if (old != null && next.phase() > old.phase()) phaseFlashTime = Double.NaN;           // the painter stamps the time (it knows the frame time)
        this.s = next;
        this.arrivedAt = now;
        if (next.dead()) dying = true;
    }

    /** The entity of the boss if it is loaded and alive (re-checked about twice a second), else null. */
    LivingEntity entity(net.minecraft.client.multiplayer.ClientLevel level, long now) {
        if (s == null || s.entityId() < 0) return null;
        if (portrait != null && !portrait.isRemoved() && now - portraitCheck < 10) return portrait;
        portraitCheck = now;
        Entity e = level.getEntity(s.entityId());
        portrait = e instanceof LivingEntity le && !le.isRemoved() ? le : null;
        return portrait;
    }

    static Component resolve(String text) {
        if (text.isEmpty()) return Component.empty();
        if (text.charAt(0) != '@') return Component.literal(text);
        String key = text.substring(1);
        return Component.translatableWithFallback(key, humanize(key.substring(key.lastIndexOf('.') + 1)));
    }

    static String humanize(String id) {
        StringBuilder sb = new StringBuilder(id.length());
        boolean up = true;
        for (int i = 0; i < id.length(); i++) {
            char c = id.charAt(i);
            if (c == '_') {
                sb.append(' ');
                up = true;
            } else {
                sb.append(up ? Character.toUpperCase(c) : c);
                up = false;
            }
        }
        return sb.toString().toLowerCase(Locale.ROOT).isEmpty() ? id : sb.toString();
    }
}
