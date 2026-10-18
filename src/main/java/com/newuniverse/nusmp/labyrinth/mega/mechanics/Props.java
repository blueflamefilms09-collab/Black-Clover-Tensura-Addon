package com.newuniverse.nusmp.labyrinth.mega.mechanics;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.FaceAttachedHorizontalDirectionalBlock;
import net.minecraft.world.level.block.LeverBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.AttachFace;

import java.util.ArrayList;
import java.util.List;

/**
 * Why: a counter ("pull the lever", "stand on the plate", "use the pedestals in order", "kill the keystone warden") needs something in the world to be done TO. This places it when
 * the mechanic activates, from the catalogue's description of what the counter needs: wardens (monsters), pedestals, plates or a lever, and one altar when the trigger is
 * {@code interact}. Props are vanilla blocks (coloured concrete pedestals so a sequence can be told apart, an iron pressure plate, a lever) placed in free floor cells round the
 * anchor through the instance's prop journal, so they vanish with the mechanic. If the room is too cramped to hold what the counter needs, the counter is marked broken and the
 * mechanic simply cannot be beaten, rather than hanging the room in a state nobody can resolve.
 */
final class Props {
    private Props() {}

    static final String[] COLORS = {"red", "blue", "green", "yellow", "purple", "orange", "white", "cyan"};

    static void setup(Instance in) {
        String ck = in.def.counter().kind();
        OpCatalog cat = MechanicLibrary.catalog();
        OpCatalog.KindSpec ks = cat == null ? null : cat.counter(ck);
        String need = ks == null ? "none" : ks.props();
        Counters.prepare(in);
        switch (need) {
            case "mobs" -> wardens(in);
            case "pedestal" -> {
                int n = switch (ck) {
                    case "sequence", "mirror", "pillars_sync" -> (int) Counters.num(in, "steps");
                    case "carry" -> 2;
                    default -> 1;
                };
                place(in, "pedestal", n);
            }
            case "plate" -> place(in, "plate", ck.equals("split_plates") ? (int) Counters.num(in, "n") : ck.equals("all_present") ? 2 : 1);
            case "lever" -> place(in, "lever", 1);
            default -> { }
        }
        if (in.def.trigger().kind().equals("interact") && in.props.isEmpty()) place(in, "pedestal", 1);
        if (!need.equals("none") && (need.equals("mobs") ? in.mobs.isEmpty() : in.props.isEmpty())) in.ctr.broken = true;
    }

    /** Before the blocks are put back: a reward chest nobody opened gives up its unrolled loot table, so removing it spills nothing (the party has left; the loot is not a gift to the floor). */
    static void sealChests(Instance in) {
        for (Prop p : in.props) {
            if (!p.kind().equals("chest") && !p.kind().equals("cursed_chest") && !p.kind().equals("mimic")) continue;
            if (in.level.hasChunkAt(p.pos()) && in.level.getBlockEntity(p.pos()) instanceof net.minecraft.world.level.block.entity.RandomizableContainerBlockEntity c) c.setLootTable(null);
        }
    }

    /** The chest was a mimic: the lid snaps shut on the player's hand and a monster of the dungeon climbs out. */
    static void mimic(Instance in, ServerPlayer p, Prop prop) {
        in.props.remove(prop);
        com.newuniverse.nusmp.dungeon.DungeonGuard.internal(() -> in.level.setBlock(prop.pos(), net.minecraft.world.level.block.Blocks.AIR.defaultBlockState(), 3));
        Mob m = Mobs.one(in, "beast", prop.pos(), 1.8, false, true);
        if (m == null) m = Mobs.oneId(in, "minecraft:silverfish", prop.pos(), 3.0, false, true);
        if (m != null) {
            m.setCustomName(Messages.text("mimic", "Mimic"));
            m.setGlowingTag(true);
        }
        PrimUtil.hurt(in.level, p, 3.0 * in.difficulty, "generic");
        in.level.playSound(null, prop.pos(), net.minecraft.sounds.SoundEvents.CHEST_CLOSE, net.minecraft.sounds.SoundSource.BLOCKS, 1.0f, 0.5f);
        Rukh.at(in, prop.pos(), "dark", "burst", 10);
        p.displayClientMessage(Messages.text("mimic_reveal", "It bites!"), true);
    }

    /** A cursed chest: the loot is real, and so is the price. */
    static void curse(Instance in, ServerPlayer p) {
        PrimUtil.addEffect(p, "minecraft:weakness", 400, 1);
        PrimUtil.addEffect(p, "minecraft:wither", 100, 0);
        Rukh.at(in, p.blockPosition(), "dark", "aura", 8);
        p.displayClientMessage(Messages.text("cursed", "The chest was cursed."), true);
    }

    private static void wardens(Instance in) {
        String group = Counters.str(in, "group");
        int n = ck(in).equals("kill_marked") ? 1 : Math.max(1, (int) Counters.num(in, "count"));
        List<Mob> made = Mobs.band(in, group, n, 4, ck(in).equals("kill_marked") ? 2.5 : 1.3, false, true);
        if (!made.isEmpty()) {
            in.leader = made.get(0).getUUID();
            if (ck(in).equals("kill_marked")) {
                made.get(0).setGlowingTag(true);
                made.get(0).setCustomName(Messages.text("keystone", "Keystone"));
            }
        }
        in.ctr.spawned = !made.isEmpty();
    }

    private static String ck(Instance in) { return in.def.counter().kind(); }

    /** Places {@code n} props of a kind in free cells round the anchor, indexed 0..n-1. */
    static void place(Instance in, String kind, int n) {
        List<BlockPos> cells = cells(in, n);
        int idx = 0;
        for (BlockPos c : cells) {
            BlockState st = state(kind, idx, in);
            if (in.propJournal.placeInAir(c, st)) in.props.add(new Prop(c, kind, idx++));
        }
    }

    /** The block of a prop. */
    static BlockState state(String kind, int index, Instance in) {
        switch (kind) {
            case "plate": return Blocks.HEAVY_WEIGHTED_PRESSURE_PLATE.defaultBlockState();
            case "lever": return Blocks.LEVER.defaultBlockState().setValue(FaceAttachedHorizontalDirectionalBlock.FACE, AttachFace.FLOOR).setValue(LeverBlock.POWERED, false);
            case "lectern": return Blocks.LECTERN.defaultBlockState();
            case "button": return Blocks.POLISHED_BLACKSTONE_BUTTON.defaultBlockState().setValue(FaceAttachedHorizontalDirectionalBlock.FACE, AttachFace.FLOOR);
            case "chest": return Blocks.CHEST.defaultBlockState();
            default: {
                Block b = BuiltInRegistries.BLOCK.get(ResourceLocation.fromNamespaceAndPath("minecraft", COLORS[Math.floorMod(index, COLORS.length)] + "_concrete"));
                return b.defaultBlockState();
            }
        }
    }

    /** Free floor cells round the anchor, at least two apart and not on the anchor itself, shuffled by the instance's rng. */
    static List<BlockPos> cells(Instance in, int n) {
        List<BlockPos> cand = new ArrayList<>();
        for (int r = 2; r <= 7; r++) {
            for (int a = 0; a < 16; a++) {
                double ang = a * Math.PI / 8.0 + r * 0.31;
                BlockPos p = in.anchor.offset((int) Math.round(Math.cos(ang) * r), 0, (int) Math.round(Math.sin(ang) * r));
                if (Mobs.fit(in, p) && inside(in, p)) cand.add(p);
            }
        }
        for (int i = cand.size() - 1; i > 0; i--) {
            int j = in.rng.nextInt(i + 1);
            BlockPos t = cand.get(i);
            cand.set(i, cand.get(j));
            cand.set(j, t);
        }
        List<BlockPos> out = new ArrayList<>();
        for (BlockPos p : cand) {
            if (out.size() >= n) break;
            boolean ok = true;
            for (BlockPos o : out) if (o.distSqr(p) < 4) { ok = false; break; }
            for (Prop q : in.props) if (q.pos().distSqr(p) < 4) { ok = false; break; }
            if (ok) out.add(p);
        }
        return out;
    }

    /** Stay two blocks clear of a room's wall layers. */
    private static boolean inside(Instance in, BlockPos p) {
        if (in.room == null) return true;
        return p.getX() >= in.room.box.minX() + 3 && p.getX() <= in.room.box.maxX() - 3 && p.getZ() >= in.room.box.minZ() + 3 && p.getZ() <= in.room.box.maxZ() - 3;
    }
}
