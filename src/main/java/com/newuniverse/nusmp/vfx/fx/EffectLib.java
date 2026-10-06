package com.newuniverse.nusmp.vfx.fx;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SplittableRandom;

/**
 * Effect builders. Each one starts from what the power physically does, uses only its family's
 * vocabulary and palette, and lays its pieces out on a fixed tick timeline. Pure Java: the same
 * specs are rendered in game and validated headlessly by EffectChecks.
 */
public final class EffectLib {
    private EffectLib() {}

    public enum Detail { FULL, REDUCED }

    /** Each family's visual vocabulary: the only textures its effects may use. */
    public static final Map<String, Set<String>> VOCABULARY = Map.of(
            "lightning", Set.of("fx_arc", "fx_flash", "fx_spark", "fx_scorch"),
            "fire", Set.of("fx_flame", "fx_flash", "fx_ember", "fx_heat", "fx_scorch"),
            "water", Set.of("fx_water", "fx_droplet", "fx_foam", "fx_mist", "fx_flash"),
            "wind", Set.of("fx_streak", "fx_mote"),
            "earth", Set.of("fx_crack", "fx_rock", "fx_dust"));

    /** Textures drawn additively (glows); everything else alpha-blends. */
    public static final Set<String> ADDITIVE = Set.of("fx_arc", "fx_flash", "fx_spark", "fx_flame", "fx_ember", "fx_heat", "fx_water", "fx_droplet", "fx_streak", "fx_mote");

    public static final List<String> IDS = List.of("lightning_arc", "fire_eruption", "water_crash", "wind_gust", "earth_spikes",
            "spirit_aura", "spirit_overdrive", "spirit_nova", "spirit_cataclysm");

    /** Spirit effects come in four elements (payload variant): 0 fire, 1 water, 2 wind, 3 earth. */
    public static final int SPIRIT_VARIANTS = 4;

    public static EffectSpec build(String id, Detail d, long seed, double length, double power, int variant) {
        if (id.startsWith("spirit_")) return spirit(id.substring(7), Math.floorMod(variant, SPIRIT_VARIANTS), d, seed, length, power);
        return build(id, d, seed, length, power);
    }

    public static EffectSpec build(String id, Detail d, long seed, double length, double power) {
        return switch (id) {
            case "lightning_arc" -> lightningArc(d, seed, length, power);
            case "fire_eruption" -> fireEruption(d, seed, power);
            case "water_crash" -> waterCrash(d, seed, power);
            case "wind_gust" -> windGust(d, seed, length, power);
            case "earth_spikes" -> earthSpikes(d, seed, length, power);
            default -> throw new IllegalArgumentException("Unknown effect " + id);
        };
    }

    // ------------------------------------------------------------------ helpers
    private static final class B {
        final SplittableRandom r;
        final List<Piece> pieces = new ArrayList<>();
        B(long seed) { r = new SplittableRandom(seed); }
        double jit(double v, double frac) { return v * (1 + (r.nextDouble() * 2 - 1) * frac); }
        int stagger(int max) { return max <= 0 ? 0 : r.nextInt(max + 1); }
        /** A direction over the sphere, with y kept inside [minY, maxY] (3D bursts, never flat discs). */
        Vec sphere(double minY, double maxY) {
            double y = minY + r.nextDouble() * (maxY - minY), a = r.nextDouble() * Math.PI * 2, h = Math.sqrt(Math.max(0, 1 - y * y));
            return new Vec(Math.cos(a) * h, y, Math.sin(a) * h);
        }
        void add(Piece p) { pieces.add(p); }
        /** n size factors spread evenly over +/-frac, shuffled: repeated pieces are never alike. */
        double[] ladder(int n, double frac) {
            double[] f = new double[n];
            for (int i = 0; i < n; i++) f[i] = n == 1 ? 1 : 1 - frac + 2 * frac * i / (n - 1) + (r.nextDouble() - 0.5) * 0.04;
            shuffle(f);
            return f;
        }
        /** n start offsets in 0..max, using at least two distinct ticks, shuffled. */
        int[] staggers(int n, int max) {
            int[] o = new int[n];
            for (int i = 0; i < n; i++) o[i] = max <= 0 ? 0 : i % (max + 1);
            for (int i = n - 1; i > 0; i--) { int j = r.nextInt(i + 1); int t = o[i]; o[i] = o[j]; o[j] = t; }
            return o;
        }
        /** The k-th of n directions over the sphere, stratified in height so a burst is always 3D. */
        Vec sphereK(int k, int n, double minY, double maxY) {
            double y = minY + (k + r.nextDouble()) / n * (maxY - minY), a = r.nextDouble() * Math.PI * 2, h = Math.sqrt(Math.max(0, 1 - y * y));
            return new Vec(Math.cos(a) * h, y, Math.sin(a) * h);
        }
        private void shuffle(double[] f) { for (int i = f.length - 1; i > 0; i--) { int j = r.nextInt(i + 1); double t = f[i]; f[i] = f[j]; f[j] = t; } }
    }

    /** Standard soft entrance (within 2 ticks) and eased exit. */
    private static List<Anim> life(int s, int e, int fadeOutTicks, Anim... more) {
        List<Anim> l = new ArrayList<>();
        l.add(Anim.fadeIn(s, s + 1, Ease.OUT_QUAD));
        l.add(Anim.fadeOut(Math.max(s + 1, e - fadeOutTicks), e, Ease.IN_QUAD));
        l.addAll(List.of(more));
        return l;
    }

    private static Piece piece(String group, Piece.Mesh mesh, int segs, String tex, Vec pos, Vec dir, double len, double w,
                               int color, int s, int e, String phase, double shimmer, double pulse, List<Vec> path, List<Anim> anims, boolean burst) {
        return new Piece(group, mesh, segs, tex, pos, dir, len, w, color, s, e, phase, shimmer, pulse, path, anims, burst);
    }

    /** A jagged path from a to b with 'segs' segments; interior points jitter sideways by 'jag' blocks. */
    private static List<Vec> jagged(B b, Vec a, Vec end, int segs, double jag) {
        List<Vec> pts = new ArrayList<>();
        Vec d = end.sub(a);
        for (int i = 0; i <= segs; i++) {
            Vec p = a.add(d.mul((double) i / segs));
            if (i > 0 && i < segs) p = p.add(new Vec(0, (b.r.nextDouble() * 2 - 1) * jag, (b.r.nextDouble() * 2 - 1) * jag));
            pts.add(p);
        }
        return pts;
    }

    // ------------------------------------------------------------------ LIGHTNING: Chain Lightning jump
    /**
     * Lightning magic: a bolt is a branching discharge, not a beam. Static crackles gather at the caster,
     * the bolt snaps across with forks, the target flashes and throws sparks, the path glows out.
     * 13 ticks: crackle 0-3, bolt 2-6, impact 4-10, afterglow 5-13.
     */
    static EffectSpec lightningArc(Detail d, long seed, double length, double power) {
        B b = new B(seed);
        boolean full = d == Detail.FULL;
        double L = Math.max(1.0, length), w = 0.22 * clampPower(power);
        var phases = List.of(new EffectSpec.Phase("windup", 0, 3), new EffectSpec.Phase("release", 2, 6),
                new EffectSpec.Phase("impact", 4, 10), new EffectSpec.Phase("linger", 5, 13));
        // wind-up: short crackling arcs around the caster's hand
        int nc = full ? 4 : 2;
        double[] cs = b.ladder(nc, 0.25); int[] cst = b.staggers(nc, 1);
        for (int i = 0; i < nc; i++) {
            Vec dir = b.sphereK(i, nc, -0.6, 0.9);
            int s = cst[i];
            b.add(piece("crackle", Piece.Mesh.RIBBON, 3, "fx_arc", Vec.ZERO, dir, 0.5 * cs[i], w * 0.5 * cs[i], 1, s, 3, "windup", 0, 0.6,
                    jagged(b, Vec.ZERO, dir.mul(0.5 * cs[i]), 3, 0.08), life(s, 3, 1, Anim.flicker(s, 3, 0.3, 6)), false));
        }
        // release: the main bolt, plus forks
        List<Vec> bolt = jagged(b, Vec.ZERO, new Vec(L, 0, 0), full ? 8 : 5, L * 0.08);
        b.add(piece("bolt", Piece.Mesh.RIBBON, full ? 8 : 5, "fx_arc", Vec.ZERO, Vec.UP, 0, w, 0, 2, 6, "release", 0, 0.4, bolt,
                life(2, 6, 1, Anim.flash(2, 4, Ease.OUT_QUAD, 1.6)), false));
        int nf = full ? 3 : 1;
        double[] fs = b.ladder(nf, 0.25); int[] fst = b.staggers(nf, 1);
        for (int i = 0; i < nf; i++) {
            Vec from = bolt.get(1 + b.r.nextInt(bolt.size() - 2));
            Vec to = from.add(b.sphereK(i, nf, -0.7, 0.7).add(new Vec(0.4, 0, 0)).mul(L * 0.18 * fs[i]));
            int s = 3 + fst[i];
            b.add(piece("fork", Piece.Mesh.RIBBON, 3, "fx_arc", Vec.ZERO, Vec.UP, L * 0.18 * fs[i], w * 0.6 * fs[i], 2, s, 6, "release", 0, 0.4,
                    jagged(b, from, to, 3, 0.1), life(s, 6, 1), false));
        }
        // impact: flash and a 3D spray of sparks at the target
        Vec hit = new Vec(L, 0, 0);
        b.add(piece("flash", Piece.Mesh.SPRITE, 1, "fx_flash", hit, Vec.UP, 0, 1.4 * clampPower(power), 0, 4, 8, "impact", 0, 0, List.of(),
                List.of(Anim.flash(4, 6, Ease.OUT_QUAD, 1.8), Anim.fadeOut(6, 8, Ease.IN_QUAD)), false));
        int ns = full ? 6 : 3;
        double[] ss = b.ladder(ns, 0.25); int[] sst = b.staggers(ns, 2);
        for (int i = 0; i < ns; i++) {
            int s = 4 + sst[i];
            Vec dir = b.sphereK(i, ns, -0.5, 0.95);
            b.add(piece("sparks", Piece.Mesh.SHARD, 1, "fx_spark", hit, dir, 0.25 * ss[i], 0.08 * ss[i], i % 2 == 0 ? 2 : 3, s, 10, "impact", 0, 0, List.of(),
                    life(s, 10, 2, Anim.drift(s, 10, Ease.OUT_CUBIC, dir.mul(ss[i]))), true));
        }
        // linger: afterglow along the same bolt path (cross-fades from the bolt), and a scorch mark
        b.add(piece("afterglow", Piece.Mesh.RIBBON, full ? 8 : 5, "fx_arc", Vec.ZERO, Vec.UP, 0, w * 1.4, 3, 5, 13, "linger", 0.3, 0, bolt,
                life(5, 13, 6), false));
        b.add(piece("scorch", Piece.Mesh.DECAL, 1, "fx_scorch", hit.add(new Vec(0, -0.9, 0)), Vec.UP, 1.0, 1.0, 4, 6, 13, "linger", 0, 0, List.of(),
                life(6, 13, 5), false));
        int[] palette = {0xFFFFFFFF, 0xFFFFF6A0, 0xFFFFE14A, 0xFFFFB02E, 0xFF5A3A10};
        var cues = List.of(new EffectSpec.Cue("release", 2, "minecraft:block.amethyst_block.resonate", 0.6f, 1.8f, 0),
                new EffectSpec.Cue("impact", 4, "minecraft:entity.lightning_bolt.impact", 0.5f, 1.6f, 0.2));
        return new EffectSpec("lightning_arc", "lightning", palette, 13, phases, b.pieces, cues, "");
    }

    // ------------------------------------------------------------------ FIRE: Calderos pillar eruption
    /**
     * Fire magic: a pillar of flame erupts from the ground. A white-hot burst at the vent, flame tongues
     * shooting up and out (cooling from yellow to red), embers rising on the heat, a scorched patch.
     * 24 ticks: burst 0-4, tongues 1-14, embers 4-20, scorch 8-24.
     */
    static EffectSpec fireEruption(Detail d, long seed, double power) {
        B b = new B(seed);
        boolean full = d == Detail.FULL;
        double p = clampPower(power);
        var phases = List.of(new EffectSpec.Phase("impact", 0, 4), new EffectSpec.Phase("bloom", 1, 14),
                new EffectSpec.Phase("embers", 4, 20), new EffectSpec.Phase("linger", 8, 24));
        b.add(piece("flash", Piece.Mesh.SPRITE, 1, "fx_flash", new Vec(0, 0.4, 0), Vec.UP, 0, 1.6 * p, 0, 0, 4, "impact", 0, 0, List.of(),
                List.of(Anim.flash(0, 2, Ease.OUT_QUAD, 1.7), Anim.fadeOut(2, 4, Ease.IN_QUAD)), false));
        int nt = full ? 10 : 6;
        double[] ts = b.ladder(nt, 0.25); int[] tst = b.staggers(nt, 3);
        for (int i = 0; i < nt; i++) {
            int s = 1 + tst[i], e = Math.min(14, s + 9 + b.stagger(2));
            Vec dir = b.sphereK(i, nt, 0.35, 1.0);
            b.add(piece("tongues", Piece.Mesh.CROSS, 1, "fx_flame", Vec.ZERO, dir, 1.4 * p * ts[i], 0.5 * p * ts[i], 1, s, e, "bloom", 0, 0.5, List.of(),
                    life(s, e, 3, Anim.grow(s, s + 3, Ease.OUT_BACK, 0.3, 1.0), Anim.drift(s, e, Ease.OUT_CUBIC, dir.mul(b.jit(1.2 * p, 0.25))),
                            Anim.tint(s, e, Ease.IN_OUT_SINE, 1, 3)), true));
        }
        b.add(piece("heat", Piece.Mesh.DOME, full ? 24 : 12, "fx_heat", Vec.ZERO, Vec.UP, 2.2 * p, 1.8 * p, 0, 2, 14, "bloom", 0.8, 0, List.of(),
                life(2, 14, 5, Anim.grow(2, 6, Ease.OUT_CUBIC, 0.5, 1.0)), false));
        int ne = full ? 12 : 6;
        double[] es = b.ladder(ne, 0.25); int[] est = b.staggers(ne, 4);
        for (int i = 0; i < ne; i++) {
            int s = 4 + est[i], e = Math.min(20, s + 10 + b.stagger(2));
            Vec rise = new Vec((b.r.nextDouble() - 0.5) * 0.8, b.jit(2.0 * p, 0.25), (b.r.nextDouble() - 0.5) * 0.8);
            b.add(piece("embers", Piece.Mesh.SPRITE, 1, "fx_ember", new Vec((b.r.nextDouble() - 0.5) * 1.2, 0.3, (b.r.nextDouble() - 0.5) * 1.2), Vec.UP, 0,
                    0.12 * es[i], i % 2 == 0 ? 1 : 2, s, e, "embers", 0, 0, List.of(),
                    life(s, e, 3, Anim.drift(s, e, Ease.OUT_QUAD, rise), Anim.sway(s, e, 0.08, 1.5), Anim.flicker(s, e, 0.3, 4)), true));
        }
        b.add(piece("scorch", Piece.Mesh.DECAL, 1, "fx_scorch", new Vec(0, 0.02, 0), Vec.UP, 2.4 * p, 2.4 * p, 4, 8, 24, "linger", 0, 0, List.of(),
                life(8, 24, 6), false));
        int[] palette = {0xFFFFF4C2, 0xFFFFC23A, 0xFFFF6A1E, 0xFFC2261A, 0xFF3A1A12};
        var cues = List.of(new EffectSpec.Cue("impact", 0, "minecraft:item.firecharge.use", 0.8f, 0.8f, 0.3));
        return new EffectSpec("fire_eruption", "fire", palette, 24, phases, b.pieces, cues, "");
    }

    // ------------------------------------------------------------------ WATER: Sea Dragon's Roar impact
    /**
     * Water magic: the sea dragon crashes into its target. A bright crash, a crown of water thrown up and
     * out, droplets raining over a sphere, foam spreading on the ground, then a fading mist.
     * 22 ticks: crash 0-3, crown 1-12, droplets 3-18, foam & mist 6-22.
     */
    static EffectSpec waterCrash(Detail d, long seed, double power) {
        B b = new B(seed);
        boolean full = d == Detail.FULL;
        double p = clampPower(power);
        var phases = List.of(new EffectSpec.Phase("impact", 0, 3), new EffectSpec.Phase("crown", 1, 12),
                new EffectSpec.Phase("droplets", 3, 18), new EffectSpec.Phase("linger", 6, 22));
        b.add(piece("flash", Piece.Mesh.SPRITE, 1, "fx_flash", new Vec(0, 0.6, 0), Vec.UP, 0, 1.8 * p, 0, 0, 3, "impact", 0, 0, List.of(),
                List.of(Anim.flash(0, 2, Ease.OUT_QUAD, 1.5), Anim.fadeOut(1, 3, Ease.IN_QUAD)), false));
        int nw = full ? 12 : 7;
        double[] ws = b.ladder(nw, 0.25); int[] wst = b.staggers(nw, 2);
        for (int i = 0; i < nw; i++) {
            int s = 1 + wst[i], e = Math.min(12, s + 8 + b.stagger(1));
            Vec dir = b.sphereK(i, nw, 0.45, 0.95);
            b.add(piece("crown", Piece.Mesh.CROSS, 1, "fx_water", Vec.ZERO, dir, 1.6 * p * ws[i], 0.45 * p * ws[i], 1, s, e, "crown", 0.4, 0, List.of(),
                    life(s, e, 3, Anim.grow(s, s + 3, Ease.OUT_BACK, 0.2, 1.0), Anim.drift(s, e, Ease.OUT_CUBIC, dir.mul(b.jit(1.0 * p, 0.25))),
                            Anim.tint(s, e, Ease.IN_OUT_SINE, 1, 2)), true));
        }
        int nd = full ? 16 : 8;
        double[] ds = b.ladder(nd, 0.25); int[] dst = b.staggers(nd, 4);
        for (int i = 0; i < nd; i++) {
            int s = 3 + dst[i], e = Math.min(18, s + 9 + b.stagger(1));
            Vec dir = b.sphereK(i, nd, -0.2, 1.0);
            b.add(piece("droplets", Piece.Mesh.SPRITE, 1, "fx_droplet", new Vec(0, 0.8, 0), Vec.UP, 0, 0.14 * ds[i], i % 2, s, e, "droplets", 0, 0, List.of(),
                    life(s, e, 3, Anim.drift(s, e, Ease.OUT_QUAD, dir.mul(b.jit(2.4 * p, 0.25)).add(new Vec(0, -0.8, 0)))), true));
        }
        b.add(piece("foam", Piece.Mesh.DECAL, 1, "fx_foam", new Vec(0, 0.03, 0), Vec.UP, 2.4 * p, 2.4 * p, 0, 6, 22, "linger", 0.2, 0, List.of(),
                life(6, 22, 6, Anim.grow(6, 22, Ease.OUT_CUBIC, 0.6, 1.4)), false));
        b.add(piece("mist", Piece.Mesh.DOME, full ? 24 : 12, "fx_mist", Vec.ZERO, Vec.UP, 1.6 * p, 2.0 * p, 1, 8, 22, "linger", 0.3, 0, List.of(),
                life(8, 22, 7, Anim.grow(8, 22, Ease.OUT_CUBIC, 0.8, 1.3)), false));
        int[] palette = {0xFFEAF8FF, 0xFF9FDCFF, 0xFF4FA8FF, 0xFF1E5FB0, 0xFF0B2A55};
        var cues = List.of(new EffectSpec.Cue("impact", 0, "minecraft:entity.generic.splash", 1.0f, 0.7f, 0.5));
        return new EffectSpec("water_crash", "water", palette, 22, phases, b.pieces, cues, "");
    }

    // ------------------------------------------------------------------ WIND: Gust Lane
    /**
     * Wind magic: air is drawn in around the caster, then released as a lane of fast streaks that race down
     * the lane in 3D, with dust motes carried along. 18 ticks: draw-in 0-4, streaks 3-14, motes 6-18.
     */
    static EffectSpec windGust(Detail d, long seed, double length, double power) {
        B b = new B(seed);
        boolean full = d == Detail.FULL;
        double L = Math.max(2, length), p = clampPower(power);
        var phases = List.of(new EffectSpec.Phase("windup", 0, 4), new EffectSpec.Phase("release", 3, 14), new EffectSpec.Phase("linger", 6, 18));
        int nn = full ? 5 : 3;
        double[] ns2 = b.ladder(nn, 0.25); int[] nst = b.staggers(nn, 1);
        for (int i = 0; i < nn; i++) {
            int s = nst[i];
            double ang = Math.PI * 2 * i / 5 + b.r.nextDouble() * 0.6;
            Vec at = new Vec(Math.cos(ang) * 1.2, 0.6 + b.r.nextDouble() * 0.8, Math.sin(ang) * 1.2);
            Vec tangent = new Vec(-Math.sin(ang), 0, Math.cos(ang)).mul(0.5);
            b.add(piece("drawin", Piece.Mesh.RIBBON, 2, "fx_streak", at, Vec.UP, 0.5 * ns2[i], 0.12 * ns2[i], 1, s, 4, "windup", 0, 0, List.of(Vec.ZERO, tangent.mul(0.5 * ns2[i]), tangent.mul(ns2[i])),
                    life(s, 4, 1, Anim.spiralIn(s, 4, Ease.IN_QUAD, ns2[i], 0.4)), false));
        }
        int nk = full ? 8 : 4;
        double[] ks = b.ladder(nk, 0.25); int[] kst = b.staggers(nk, 4);
        for (int i = 0; i < nk; i++) {
            int s = 3 + kst[i], e = Math.min(14, s + 6 + b.stagger(3));
            double len = L * 0.45 * ks[i];
            Vec at = new Vec(b.r.nextDouble() * L * 0.3, 0.3 + b.r.nextDouble() * 1.5, (b.r.nextDouble() - 0.5) * 2.4 * p);
            List<Vec> path = new ArrayList<>();
            for (int k = 0; k <= 4; k++) path.add(new Vec(len * k / 4, Math.sin(k * 0.8 + i) * 0.08, Math.cos(k * 0.7 + i) * 0.08));
            b.add(piece("streaks", Piece.Mesh.RIBBON, 4, "fx_streak", at, Vec.UP, len, 0.1 * ks[i], 1 + b.r.nextInt(3), s, e, "release", 0, 0, path,
                    life(s, e, 2, Anim.drift(s, e, Ease.OUT_CUBIC, new Vec(L * 0.5, b.jit(0.2, 0.3), 0))), false));
        }
        int nm = full ? 10 : 5;
        double[] ms = b.ladder(nm, 0.25); int[] mst = b.staggers(nm, 4);
        for (int i = 0; i < nm; i++) {
            int s = 6 + mst[i], e = Math.min(18, s + 7 + b.stagger(3));
            Vec at = new Vec(b.r.nextDouble() * L, 0.2 + b.r.nextDouble() * 1.2, (b.r.nextDouble() - 0.5) * 2.0);
            b.add(piece("motes", Piece.Mesh.SPRITE, 1, "fx_mote", at, Vec.UP, 0, 0.1 * ms[i], 0, s, e, "linger", 0, 0, List.of(),
                    life(s, e, 3, Anim.drift(s, e, Ease.OUT_QUAD, new Vec(b.jit(1.5, 0.3), b.jit(0.3, 0.3), 0)), Anim.sway(s, e, 0.1, 1.2)), false));
        }
        int[] palette = {0xFFFFFFFF, 0xFFE6FFF2, 0xFFB8F5D6, 0xFF8CFFC2, 0xFF4E9C7A};
        var cues = List.of(new EffectSpec.Cue("release", 3, "minecraft:entity.breeze.wind_burst", 0.7f, 1.2f, 0));
        return new EffectSpec("wind_gust", "wind", palette, 18, phases, b.pieces, cues, "");
    }

    // ------------------------------------------------------------------ EARTH: Earth Spikes
    /**
     * Earth magic: the ground cracks along the line first, then stone spikes burst up one after another,
     * kicking up dust, and sink back. 26 ticks: cracks 0-6, spikes 4-22, dust 6-26.
     */
    static EffectSpec earthSpikes(Detail d, long seed, double length, double power) {
        B b = new B(seed);
        boolean full = d == Detail.FULL;
        double L = Math.max(2, length), p = clampPower(power);
        int n = full ? 8 : 5;
        double[] crk = b.ladder(n, 0.25), spk = b.ladder(n, 0.25), dsz = b.ladder(n * (full ? 2 : 1), 0.25);
        var phases = List.of(new EffectSpec.Phase("windup", 0, 6), new EffectSpec.Phase("release", 4, 22), new EffectSpec.Phase("linger", 6, 26));
        for (int i = 0; i < n; i++) {
            double x = L * (i + 0.5) / n + (b.r.nextDouble() - 0.5) * 0.3;
            Vec base = new Vec(x, 0.02, (b.r.nextDouble() - 0.5) * 0.5);
            int cs = Math.min(3, i / 2);
            b.add(piece("cracks", Piece.Mesh.DECAL, 1, "fx_crack", base, Vec.UP, L / n * 1.3 * crk[i], 0.7 * crk[i], 3, cs, 6, "windup", 0, 0, List.of(),
                    life(cs, 6, 2, Anim.grow(cs, cs + 3, Ease.OUT_CUBIC, 0.2, 1.0)), false));
            int s = Math.min(11, 4 + i + b.stagger(1)), e = Math.min(22, s + 10);
            Vec dir = new Vec((b.r.nextDouble() - 0.5) * 0.5, 1, (b.r.nextDouble() - 0.5) * 0.5).norm();
            b.add(piece("spikes", Piece.Mesh.SHARD, 1, "fx_rock", base, dir, 1.4 * p * spk[i], 0.7 * p * spk[i], i % 2 == 0 ? 1 : 2, s, e, "release", 0, 0, List.of(),
                    List.of(Anim.grow(s, s + 3, Ease.OUT_BACK, 0.0, 1.0), Anim.grow(e - 3, e, Ease.IN_CUBIC, 1.0, 0.0), Anim.tint(s, e, Ease.IN_OUT_SINE, 1, 2)), true));
            for (int k = 0; k < (full ? 2 : 1); k++) {
                int ds = Math.min(14, s + 2 + k), de = Math.min(26, ds + 10 + b.stagger(2));
                Vec puff = b.sphereK((i + k) % 3, 3, 0.2, 0.9).mul(b.jit(0.7, 0.3));
                b.add(piece("dust", Piece.Mesh.SPRITE, 1, "fx_dust", base.add(new Vec(0, 0.3, 0)), Vec.UP, 0, 0.6 * dsz[i * (full ? 2 : 1) + k], 0, ds, de, "linger", 0, 0, List.of(),
                        life(ds, de, 4, Anim.drift(ds, de, Ease.OUT_CUBIC, puff), Anim.grow(ds, de, Ease.OUT_CUBIC, 0.5, 1.3)), true));
            }
        }
        int[] palette = {0xFFE8D3A8, 0xFFB08850, 0xFF7A5A36, 0xFF4A3624, 0xFF2A1E14};
        var cues = List.of(new EffectSpec.Cue("windup", 0, "minecraft:block.rooted_dirt.break", 0.8f, 0.6f, 0),
                new EffectSpec.Cue("release", 4, "minecraft:block.pointed_dripstone.break", 0.9f, 0.7f, 0.3));
        return new EffectSpec("earth_spikes", "earth", palette, 26, phases, b.pieces, cues, "");
    }

    // ------------------------------------------------------------------ SPIRIT CHANNELING (fire / water / wind / earth)
    /** Per-element vocabulary for the spirit effects: body pieces, motes, ground marks, glow. */
    private record SpiritKit(String family, int[] palette, Piece.Mesh bodyMesh, String body, String mote, String ground, String glow) {}

    private static final SpiritKit[] KITS = {
            // Fire spirit (Salamander): flame sheets licking upward, embers, heat, scorch
            new SpiritKit("fire", new int[]{0xFFFFE666, 0xFFFFA61A, 0xFFFF590D, 0xFFE62605}, Piece.Mesh.CROSS, "fx_flame", "fx_ember", "fx_scorch", "fx_flash"),
            // Water spirit (Undine): fluid sheets, droplet arcs, foam
            new SpiritKit("water", new int[]{0xFFD9F2FF, 0xFF66CCFF, 0xFF2673FF, 0xFF0D338C}, Piece.Mesh.CROSS, "fx_water", "fx_droplet", "fx_foam", "fx_flash"),
            // Wind spirit (Sylph): shear streaks and motes only - wind has no glow and leaves no mark
            new SpiritKit("wind", new int[]{0xFFE6FFF2, 0xFFB3FFD9, 0xFF8CE6FF, 0xFF66BFA6}, Piece.Mesh.RIBBON, "fx_streak", "fx_mote", null, null),
            // Earth spirit (Gnome): stone shards, dust plumes, cracks - heavy and settling
            new SpiritKit("earth", new int[]{0xFFE6CC73, 0xFFBF9940, 0xFF8C662E, 0xFF4D401F}, Piece.Mesh.SHARD, "fx_rock", "fx_dust", "fx_crack", null)};

    private static Piece body(B b, SpiritKit k, String group, Vec pos, Vec dir, double size, int color, int s, int e, String phase, List<Anim> anims) {
        if (k.bodyMesh() == Piece.Mesh.RIBBON) {   // wind: a short curved streak along 'dir'
            Vec side = dir.cross(Vec.UP).norm().mul(0.15 * size);
            List<Vec> path = List.of(Vec.ZERO, dir.mul(size * 0.5).add(side), dir.mul(size));
            return piece(group, Piece.Mesh.RIBBON, 2, k.body(), pos, dir, size, 0.14 * size, color, s, e, phase, 0, 0, path, anims, true);
        }
        return piece(group, k.bodyMesh(), 1, k.body(), pos, dir, size, 0.4 * size, color, s, e, phase, k.family().equals("water") ? 0.4 : 0, k.family().equals("fire") ? 0.5 : 0, List.of(), anims, true);
    }

    static EffectSpec spirit(String kind, int element, Detail d, long seed, double length, double power) {
        SpiritKit k = KITS[element];
        B b = new B(seed + element * 7919L);
        boolean full = d == Detail.FULL;
        double p = clampPower(power);
        List<EffectSpec.Phase> phases;
        List<EffectSpec.Cue> cues;
        int life;
        switch (kind) {
            case "aura" -> {
                // Channeling: raw spirit energy is pulled into you (motes spiral in), while the element stirs around you.
                life = 14;
                phases = List.of(new EffectSpec.Phase("gather", 0, 14), new EffectSpec.Phase("stir", 2, 14));
                int n = full ? 8 : 4;
                double[] sz = b.ladder(n, 0.25); int[] st = b.staggers(n, 3);
                for (int i = 0; i < n; i++) {
                    Vec at = b.sphereK(i, n, -0.3, 0.8).mul(2.2 * p).add(new Vec(0, 1, 0));
                    int s = st[i];
                    b.add(piece("motes", Piece.Mesh.SPRITE, 1, k.mote(), at, Vec.UP, 0, 0.16 * sz[i], i % 2, s, 14, "gather", 0, 0, List.of(),
                            life(s, 14, 3, Anim.spiralIn(s, 14, Ease.IN_QUAD, 2.0 * p * sz[i], 0.3)), false));
                }
                int m = full ? 4 : 2;
                double[] bs = b.ladder(m, 0.25); int[] bst = b.staggers(m, 2);
                for (int i = 0; i < m; i++) {
                    double ang = Math.PI * 2 * i / m + b.r.nextDouble() * 0.5;
                    Vec at = new Vec(Math.cos(ang) * 0.9, 0.1, Math.sin(ang) * 0.9);
                    Vec dir = new Vec(Math.cos(ang) * 0.3, 1, Math.sin(ang) * 0.3).norm();
                    int s = 2 + bst[i];
                    b.add(body(b, k, "stir", at, dir, 0.9 * p * bs[i], 1 + i % 2, s, 14, "stir",
                            life(s, 14, 4, Anim.grow(s, s + 3, Ease.OUT_CUBIC, 0.4, 1.0), Anim.sway(s, 14, 0.06, 1.0))));
                }
                cues = List.of();
            }
            case "overdrive" -> {
                // Elemental Overdrive: you dash cloaked in the element; it tears off you along the path and settles.
                life = 16;
                double L = Math.max(2, length);
                phases = List.of(new EffectSpec.Phase("release", 0, 8), new EffectSpec.Phase("linger", 4, 16));
                int n = full ? 10 : 5;
                double[] sz = b.ladder(n, 0.25); int[] st = b.staggers(n, 3);
                for (int i = 0; i < n; i++) {
                    double x = L * (i + b.r.nextDouble()) / n;
                    Vec at = new Vec(x, 0.3 + b.r.nextDouble() * 1.2, (b.r.nextDouble() - 0.5) * 0.8);
                    Vec dir = new Vec(-0.6, 0.8, (b.r.nextDouble() - 0.5) * 0.6).norm();
                    int s = st[i], e = Math.min(8, s + 4 + b.stagger(1));
                    b.add(body(b, k, "trail", at, dir, 0.9 * p * sz[i], i % 3, s, e, "release",
                            life(s, e, 2, Anim.drift(s, e, Ease.OUT_CUBIC, new Vec(-0.8, 0.4, 0).mul(sz[i])))));
                }
                int m = full ? 10 : 5;
                double[] ms = b.ladder(m, 0.25); int[] mst = b.staggers(m, 4);
                for (int i = 0; i < m; i++) {
                    Vec at = new Vec(b.r.nextDouble() * L, 0.4 + b.r.nextDouble(), (b.r.nextDouble() - 0.5) * 1.2);
                    int s = 4 + mst[i], e = Math.min(16, s + 7 + b.stagger(2));
                    Vec drift = element == 3 ? new Vec(0, -0.4, 0) : element == 0 ? new Vec(0, 0.8, 0) : new Vec(-0.6, 0.2, 0);
                    b.add(piece("motes", Piece.Mesh.SPRITE, 1, k.mote(), at, Vec.UP, 0, (element == 3 ? 0.5 : 0.14) * ms[i], i % 2, s, e, "linger", 0, 0, List.of(),
                            life(s, e, 3, Anim.drift(s, e, Ease.OUT_QUAD, drift.mul(ms[i]))), false));
                }
                if (k.ground() != null)
                    b.add(piece("mark", Piece.Mesh.DECAL, 1, k.ground(), new Vec(L / 2, 0.03, 0), Vec.UP, L, 0.9, 3, 5, 16, "linger", 0, 0, List.of(), life(5, 16, 5), false));
                cues = List.of(new EffectSpec.Cue("release", 0, element == 0 ? "minecraft:item.firecharge.use" : element == 1 ? "minecraft:entity.player.splash"
                        : element == 2 ? "minecraft:entity.breeze.wind_burst" : "minecraft:block.gravel.break", 0.7f, 1.3f, 0));
            }
            case "nova" -> {
                // Spirit Nova: the gathered energy bursts out over a sphere 8 blocks wide.
                life = 22;
                phases = List.of(new EffectSpec.Phase("impact", 0, 3), new EffectSpec.Phase("bloom", 1, 14), new EffectSpec.Phase("linger", 8, 22));
                if (k.glow() != null)
                    b.add(piece("flash", Piece.Mesh.SPRITE, 1, k.glow(), new Vec(0, 1, 0), Vec.UP, 0, 2.4 * p, 0, 0, 3, "impact", 0, 0, List.of(),
                            List.of(Anim.flash(0, 2, Ease.OUT_QUAD, 1.7), Anim.fadeOut(1, 3, Ease.IN_QUAD)), false));
                else {   // wind / earth have no glow: the impact is a first ring of short bodies bursting out
                    int n0 = full ? 4 : 2; double[] s0 = b.ladder(n0, 0.25); int[] t0 = b.staggers(n0, 1);
                    for (int i = 0; i < n0; i++) {
                        Vec dir = b.sphereK(i, n0, 0.1, 0.8);
                        b.add(body(b, k, "kick", new Vec(0, 0.4, 0), dir, 0.7 * p * s0[i], 0, t0[i], 3, "impact",
                                List.of(Anim.grow(t0[i], t0[i] + 1, Ease.OUT_EXPO, 0.0, 1.0), Anim.fadeOut(t0[i] + 1, 3, Ease.IN_QUAD))));
                    }
                }
                int n = full ? 14 : 7;
                double[] sz = b.ladder(n, 0.25); int[] st = b.staggers(n, 3);
                for (int i = 0; i < n; i++) {
                    Vec dir = b.sphereK(i, n, -0.3, 0.95);
                    int s = 1 + st[i], e = Math.min(14, s + 8 + b.stagger(2));
                    b.add(body(b, k, "burst", new Vec(0, 1, 0), dir, 1.1 * p * sz[i], 1 + i % 3, s, e, "bloom",
                            life(s, e, 3, Anim.grow(s, s + 2, Ease.OUT_BACK, 0.3, 1.0), Anim.drift(s, e, Ease.OUT_CUBIC, dir.mul(5.0 * p * sz[i])))));
                }
                int m = full ? 10 : 5;
                double[] ms = b.ladder(m, 0.25); int[] mst = b.staggers(m, 4);
                for (int i = 0; i < m; i++) {
                    Vec dir = b.sphereK(i, m, 0.0, 0.9);
                    int s = 8 + mst[i], e = Math.min(22, s + 9 + b.stagger(2));
                    b.add(piece("motes", Piece.Mesh.SPRITE, 1, k.mote(), dir.mul(4.0 * p), Vec.UP, 0, (element == 3 ? 0.6 : 0.16) * ms[i], i % 2, s, e, "linger", 0, 0, List.of(),
                            life(s, e, 4, Anim.drift(s, e, Ease.OUT_QUAD, element == 3 ? new Vec(0, -0.5, 0) : new Vec(0, 0.8, 0))), false));
                }
                if (k.ground() != null)
                    b.add(piece("mark", Piece.Mesh.DECAL, 1, k.ground(), new Vec(0, 0.03, 0), Vec.UP, 5.0 * p, 5.0 * p, 3, 8, 22, "linger", 0, 0, List.of(), life(8, 22, 6), false));
                cues = List.of(new EffectSpec.Cue("impact", 0, "minecraft:entity.generic.explode", 0.7f, element == 2 ? 1.6f : 1.1f, 0.5));
            }
            default -> {
                // Cataclysm: for 6 s you are a living conduit - the element wells up around you in waves, then lets go.
                life = 120;
                phases = List.of(new EffectSpec.Phase("ignite", 0, 8), new EffectSpec.Phase("sustain", 6, 108), new EffectSpec.Phase("release", 100, 120));
                boolean ribbon = k.bodyMesh() == Piece.Mesh.RIBBON;   // wind streaks cost twice a sheet: fewer of them
                int n0 = full ? (ribbon ? 4 : 6) : (ribbon ? 2 : 3); double[] s0 = b.ladder(n0, 0.25); int[] t0 = b.staggers(n0, 2);
                for (int i = 0; i < n0; i++) {
                    Vec dir = b.sphereK(i, n0, 0.2, 0.95);
                    int s = t0[i];
                    b.add(body(b, k, "ignite", new Vec(0, 0.2, 0), dir, 1.3 * p * s0[i], i % 2, s, 8, "ignite",
                            life(s, 8, 3, Anim.grow(s, s + 2, Ease.OUT_BACK, 0.3, 1.0), Anim.drift(s, 8, Ease.OUT_CUBIC, dir.mul(1.2 * s0[i])))));
                }
                int waves = full ? (ribbon ? 4 : 5) : 3, per = full ? (ribbon ? 3 : 5) : (ribbon ? 2 : 3);
                for (int w = 0; w < waves; w++) {
                    double[] ws = b.ladder(per, 0.25); int[] wt = b.staggers(per, 3);
                    int base = 6 + w * (96 / waves);
                    for (int i = 0; i < per; i++) {
                        double ang = Math.PI * 2 * (i + b.r.nextDouble() * 0.6) / per;
                        Vec at = new Vec(Math.cos(ang) * 1.2 * p, 0.1, Math.sin(ang) * 1.2 * p);
                        Vec dir = new Vec(Math.cos(ang) * 0.35, 1, Math.sin(ang) * 0.35).norm();
                        int s = base + wt[i], e = Math.min(108, s + 14 + b.stagger(3));
                        b.add(body(b, k, "wave" + w, at, dir, 1.2 * p * ws[i], 1 + (i + w) % 3, s, e, "sustain",
                                life(s, e, 5, Anim.grow(s, s + 3, Ease.OUT_CUBIC, 0.3, 1.0), Anim.drift(s, e, Ease.OUT_QUAD, new Vec(0, 0.6, 0)), Anim.sway(s, e, 0.05, 0.8))));
                    }
                }
                int m = full ? 8 : 4; double[] ms = b.ladder(m, 0.25); int[] mt = b.staggers(m, 6);
                for (int i = 0; i < m; i++) {
                    Vec dir = b.sphereK(i, m, 0.2, 0.9);
                    int s = 100 + mt[i];
                    b.add(piece("letgo", Piece.Mesh.SPRITE, 1, k.mote(), new Vec(0, 1, 0), Vec.UP, 0, (element == 3 ? 0.6 : 0.18) * ms[i], i % 2, s, 120, "release", 0, 0, List.of(),
                            life(s, 120, 6, Anim.drift(s, 120, Ease.OUT_CUBIC, dir.mul(2.0 * ms[i]))), false));
                }
                if (k.ground() != null)
                    b.add(piece("mark", Piece.Mesh.DECAL, 1, k.ground(), new Vec(0, 0.03, 0), Vec.UP, 3.0 * p, 3.0 * p, 3, 100, 120, "release", 0, 0, List.of(), life(100, 120, 8), false));
                cues = List.of(new EffectSpec.Cue("ignite", 0, "minecraft:entity.evoker.prepare_summon", 0.8f, element == 2 ? 1.4f : 0.9f, 0.4),
                        new EffectSpec.Cue("release", 100, "minecraft:block.beacon.deactivate", 0.6f, 1.2f, 0));
            }
        }
        return new EffectSpec("spirit_" + kind, k.family(), k.palette(), life, phases, b.pieces, cues, "");
    }

    private static double clampPower(double p) { return Math.max(0.6, Math.min(1.6, p <= 0 ? 1 : p)); }
}
