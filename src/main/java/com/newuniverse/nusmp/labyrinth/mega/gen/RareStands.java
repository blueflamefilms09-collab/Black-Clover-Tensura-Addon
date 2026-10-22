package com.newuniverse.nusmp.labyrinth.mega.gen;

import com.newuniverse.nusmp.labyrinth.props.ArmourStandRarity;
import com.newuniverse.nusmp.labyrinth.props.PropsConfig;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Why: the armour stand of the dungeon is a RARE prop ({@link ArmourStandRarity}), decided once per floor from the plan: which pieces' rooms hold the one stand. This is the glue between the
 * plan and that pure decision (the templates each piece is really built from, their footprints, the plan's seed, the two config numbers). It runs once when the placer is made, costs one pass over
 * the pieces (a floor has at most a few hundred), and nothing here touches the world.
 */
final class RareStands {
    private RareStands() {}

    /** The seed of the stand roll of a plan: its own stream, so the stand rolls never disturb and are never disturbed by the other choices made from the plan's seed. */
    static long seedOf(Result r) { return Rng.mix(r.seed, 0x5441534E4453L); }

    /** The indices of the pieces whose rooms hold the rare stand, for explicit numbers (the self-test and the unit tests use this one). */
    static Set<Integer> plan(Result r, List<String> templates, double percent, int floorCap) {
        List<ArmourStandRarity.Piece> pieces = new ArrayList<>(r.pieces.size());
        for (Placed p : r.pieces) {
            String t = p.index < templates.size() ? templates.get(p.index) : p.shape.id;
            pieces.add(new ArmourStandRarity.Piece(p.index, t, p.box.minX(), p.box.minZ(), p.box.maxX(), p.box.maxZ()));
        }
        return new HashSet<>(ArmourStandRarity.plan(pieces, seedOf(r), percent, floorCap));
    }

    /** The same with the configured numbers. A failure to read them is an empty plan, never an exception: no stand is better than a sector that is never finished. */
    static Set<Integer> plan(Result r, List<String> templates) {
        try {
            return plan(r, templates, PropsConfig.armourStandRarity(), PropsConfig.armourStandFloorCap());
        } catch (RuntimeException e) {
            return Set.of();
        }
    }
}
