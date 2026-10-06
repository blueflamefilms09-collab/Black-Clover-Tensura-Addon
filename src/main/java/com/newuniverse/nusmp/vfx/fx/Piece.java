package com.newuniverse.nusmp.vfx.fx;

import java.util.List;

/**
 * One piece of an effect: a white mesh built once, anchored at 'pos' in the effect's local frame
 * (x = from->to, y = up, z = side), pointing along 'dir'. All motion comes from its animations.
 * 'group' ties repeated pieces together (for the jitter / stagger / 3D-burst checks).
 */
public record Piece(String group, Mesh mesh, int segments, String texture, Vec pos, Vec dir, double length, double width,
                    int color, int start, int end, String phase, double shimmer, double pulse, List<Vec> path, List<Anim> anims,
                    boolean burst) {
    public enum Mesh { SPRITE, DECAL, CROSS, SHARD, RIBBON, DOME }

    /** Vertices this piece costs (whole quads only). */
    public int vertices() {
        return switch (mesh) {
            case SPRITE, DECAL -> 4;
            case CROSS, SHARD -> 8;
            case RIBBON -> 8 * segments;
            case DOME -> 4 * segments;
        };
    }

    public boolean aliveAt(double t) { return t >= start && t < end; }
}
