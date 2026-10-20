package com.newuniverse.nusmp.labyrinth.mega.rift;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.WallSignBlock;
import net.minecraft.world.level.block.entity.SignBlockEntity;
import net.minecraft.world.level.block.entity.SignText;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Why: every sealed door of a hidden realm or a tomb needs a sentence next to it that says what opens it and where the way is (1.101.0, docs/labyrinth/pocket_fix.md): a glowing, waxed wall sign
 * (nobody can rewrite it) whose four lines are lang keys {@code sign.nusmp.pocket.<key>.<n>} with an English fallback, so it reads in any language the lang file has. The texts live here, in one table, so the
 * builders of the realm and of the tombs and the tests agree on them. Server side only.
 */
public final class PocketSigns {
    private PocketSigns() {}

    /** The four lines of a sign, by key (the words are in {@link PocketSignText}, which a unit test can reach). */
    public static String[] lines(String key) { return PocketSignText.lines(key); }

    /** The component of one line (a lang key with its English fallback). */
    public static Component line(String key, int n) {
        String[] l = lines(key);
        String text = n < l.length ? l[n] : "";
        return text.isEmpty() ? Component.empty() : Component.translatableWithFallback("sign.nusmp.pocket." + key + "." + n, text);
    }

    /** The direction a letter N, E, S, W names (the way a wall sign or lever FACES, out of its wall). */
    public static Direction dir(char c) {
        return switch (c) {
            case 'N' -> Direction.NORTH;
            case 'E' -> Direction.EAST;
            case 'W' -> Direction.WEST;
            default -> Direction.SOUTH;
        };
    }

    /** The letter of a direction. */
    public static char letter(Direction d) {
        return switch (d) {
            case NORTH -> 'N';
            case EAST -> 'E';
            case WEST -> 'W';
            default -> 'S';
        };
    }

    /** Puts a glowing, waxed wall sign at {@code at} facing {@code facing} with the text of {@code key}. Returns false when the sign could not be written. */
    public static boolean place(ServerLevel level, BlockPos at, Direction facing, String key) {
        BlockState st = Blocks.CRIMSON_WALL_SIGN.defaultBlockState().setValue(WallSignBlock.FACING, facing);
        level.setBlock(at, st, 2 | 16);
        if (!(level.getBlockEntity(at) instanceof SignBlockEntity sign)) return false;
        SignText t = new SignText();
        for (int i = 0; i < 4; i++) t = t.setMessage(i, line(key, i));
        t = t.setColor(DyeColor.WHITE).setHasGlowingText(true);
        sign.setText(t, true);
        sign.setWaxed(true);
        sign.setChanged();
        level.sendBlockUpdated(at, st, st, 3);
        return true;
    }
}
