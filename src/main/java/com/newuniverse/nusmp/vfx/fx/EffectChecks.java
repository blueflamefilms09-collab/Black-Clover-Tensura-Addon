package com.newuniverse.nusmp.vfx.fx;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Headless rule checks for every effect spec. Each returned string is one broken rule.
 * Run by the test suite (gradle build runs tests, so a failing effect blocks the jar).
 */
public final class EffectChecks {
    private EffectChecks() {}

    public static final int BUDGET_FULL = 400, BUDGET_REDUCED = 200, MAX_DRAWS = 64;
    public static final double MAX_FLASH = 1.8, MAX_FLICKER = 0.35, MAX_SHAKE = 0.6;

    public static List<String> check(EffectSpec s, EffectLib.Detail detail) {
        List<String> bad = new ArrayList<>();
        String id = s.id() + "[" + detail + "]";

        // palette: 3-6 colours, every piece / tint inside it
        if (s.palette().length < 3 || s.palette().length > 6) bad.add(id + ": palette has " + s.palette().length + " colours (need 3-6)");
        // vocabulary
        Set<String> vocab = EffectLib.VOCABULARY.get(s.family());
        if (vocab == null) bad.add(id + ": unknown family " + s.family());

        // budget
        int budget = detail == EffectLib.Detail.FULL ? BUDGET_FULL : BUDGET_REDUCED;
        if (s.vertices() > budget) bad.add(id + ": " + s.vertices() + " vertices > budget " + budget);

        // phases: inside the life, each one used
        for (EffectSpec.Phase ph : s.phases()) {
            if (ph.start() < 0 || ph.end() > s.life() || ph.end() <= ph.start()) bad.add(id + ": phase " + ph.name() + " outside life");
            if (s.pieces().stream().noneMatch(p -> p.phase().equals(ph.name()))) bad.add(id + ": phase " + ph.name() + " has no pieces (phases may thin, never drop)");
        }

        Map<String, List<Piece>> groups = new HashMap<>();
        for (Piece p : s.pieces()) {
            String pid = id + "/" + p.group();
            if (p.vertices() % 4 != 0) bad.add(pid + ": not whole quads");
            if (p.start() < 0 || p.end() > s.life() || p.end() <= p.start()) bad.add(pid + ": lives " + p.start() + "-" + p.end() + " outside 0-" + s.life());
            EffectSpec.Phase ph = s.phase(p.phase());
            if (ph == null) bad.add(pid + ": unknown phase " + p.phase());
            else if (p.start() < ph.start() || p.end() > ph.end()) bad.add(pid + ": lives " + p.start() + "-" + p.end() + " outside its phase " + ph.name() + " " + ph.start() + "-" + ph.end());
            if (p.color() < 0 || p.color() >= s.palette().length) bad.add(pid + ": colour " + p.color() + " not in palette");
            if (vocab != null && !vocab.contains(p.texture())) bad.add(pid + ": texture " + p.texture() + " is not " + s.family() + " vocabulary");
            if (p.mesh() == Piece.Mesh.RIBBON && p.path().size() != p.segments() + 1) bad.add(pid + ": ribbon path has " + p.path().size() + " points for " + p.segments() + " segments");
            for (Anim a : p.anims()) {
                if (a.ease() == null) bad.add(pid + ": animation without a named easing curve");
                if (a.start() < p.start() || a.end() > p.end()) bad.add(pid + ": " + a.type() + " runs outside the piece's life");
                if (a.type() == Anim.Type.FLASH && a.a() > MAX_FLASH) bad.add(pid + ": flash peak " + a.a() + " > " + MAX_FLASH);
                if (a.type() == Anim.Type.FLICKER && a.a() > MAX_FLICKER) bad.add(pid + ": flicker " + a.a() + " > " + MAX_FLICKER);
                if (a.type() == Anim.Type.TINT && (a.a() < 0 || a.a() >= s.palette().length || a.b() < 0 || a.b() >= s.palette().length)) bad.add(pid + ": tint leaves the palette");
            }
            if (!entersSoftly(p)) bad.add(pid + ": pops in (needs a fade-in within 2 ticks, a grow from 0, or a deliberate flash)");
            if (!exitsSoftly(p)) bad.add(pid + ": pops out (needs a fade-out or shrink to 0 ending at its end)");
            groups.computeIfAbsent(p.group(), k -> new ArrayList<>()).add(p);
        }

        // jitter + stagger for repeated pieces; 3D bursts
        for (var en : groups.entrySet()) {
            List<Piece> g = en.getValue();
            if (g.size() < 3) continue;
            String gid = id + "/" + en.getKey();
            double min = Double.MAX_VALUE, max = 0;
            for (Piece p : g) { double sz = Math.max(p.width(), p.length()); min = Math.min(min, sz); max = Math.max(max, sz); }
            if (max > 0 && (max - min) / max < 0.15) bad.add(gid + ": repeated pieces are not jittered (size spread " + Math.round((max - min) / max * 100) + "% < 15%)");
            if (g.stream().mapToInt(Piece::start).distinct().count() < 2) bad.add(gid + ": repeated pieces all start on the same tick (stagger them)");
            if (g.get(0).burst()) {
                double sum = 0, sum2 = 0; int n = 0;
                for (Piece p : g) for (Anim a : p.anims()) if (a.type() == Anim.Type.DRIFT || a.type() == Anim.Type.GROW) {
                    Vec dir = a.type() == Anim.Type.DRIFT ? new Vec(a.a(), a.b(), a.c()).norm() : p.dir().norm();
                    sum += dir.y(); sum2 += dir.y() * dir.y(); n++;
                    break;
                }
                double var = n == 0 ? 0 : sum2 / n - (sum / n) * (sum / n);
                double spreadXZ = g.stream().mapToDouble(p -> Math.hypot(p.dir().x(), p.dir().z())).max().orElse(0);
                if (n > 0 && var < 0.005 && spreadXZ > 0.9) bad.add(gid + ": burst is a flat disc (vary directions over a sphere)");
            }
        }

        // draw calls on the busiest tick
        for (int t = 0; t < s.life(); t++) {
            final int tt = t;
            long alive = s.pieces().stream().filter(p -> p.aliveAt(tt)).count();
            if (alive > MAX_DRAWS) bad.add(id + ": " + alive + " pieces alive at tick " + t + " > " + MAX_DRAWS);
        }

        // cues fire on the first frame of their phase; shake stays light
        for (EffectSpec.Cue c : s.cues()) {
            EffectSpec.Phase ph = s.phase(c.phase());
            if (ph == null || ph.start() != c.tick()) bad.add(id + ": cue at tick " + c.tick() + " is not on the first frame of phase " + c.phase());
            if (c.shake() > MAX_SHAKE) bad.add(id + ": shake " + c.shake() + " > " + MAX_SHAKE);
        }
        return bad;
    }

    static boolean entersSoftly(Piece p) {
        for (Anim a : p.anims()) {
            if (a.start() != p.start()) continue;
            if (a.type() == Anim.Type.FADE_IN && a.end() - a.start() <= 2) return true;
            if (a.type() == Anim.Type.GROW && a.a() == 0) return true;
            if (a.type() == Anim.Type.FLASH) return true;
        }
        return false;
    }

    static boolean exitsSoftly(Piece p) {
        for (Anim a : p.anims()) {
            if (a.end() != p.end()) continue;
            if (a.type() == Anim.Type.FADE_OUT) return true;
            if (a.type() == Anim.Type.GROW && a.b() == 0) return true;
        }
        return false;
    }
}
