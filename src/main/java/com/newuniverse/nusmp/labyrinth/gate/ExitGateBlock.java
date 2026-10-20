package com.newuniverse.nusmp.labyrinth.gate;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import com.newuniverse.nusmp.particle.fx.Fx;
import com.newuniverse.nusmp.particle.fx.FxOld;
import com.newuniverse.nusmp.particle.fx.FxThemes;

/**
 * Why: the Exit Gate ({@code nusmp:labyrinth_exit}) stands on the Spire's arrival pad (the base piece puts it three blocks east of the pad centre,
 * see the contract) and is the one honest way out that is not death: walking into it returns the player to the exact spot they stepped in
 * from, in whatever dimension that was. It is a {@link GateBlock} with the direction turned round so both share the no-collision, bedrock-hard,
 * translucent set-up and the cooldown; it is placed by the structure template, never crafted, and in a legacy-engine world (where nobody is
 * sent to the Labyrinth) it still works for an admin who teleported in. The particles rise instead of sinking, in the gate's golden-white.
 */
public class ExitGateBlock extends GateBlock {
    public ExitGateBlock(BlockBehaviour.Properties props) { super(props); }

    @Override
    protected void travel(ServerPlayer p, BlockPos pos) { LabyrinthTravel.leaveAny(p); }

    /** Motes rise out of the gate; a chime now and then. */
    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource rnd) {
        double t = (level.getGameTime() % 80) / 80.0 * Math.PI * 2.0;
        for (int i = 0; i < 3; i++) {
            double a = -t + i * 2.0944 + rnd.nextDouble() * 0.3, r = 0.3 + i * 0.12;
            double x = pos.getX() + 0.5 + Math.cos(a) * r, z = pos.getZ() + 0.5 + Math.sin(a) * r;
            double y = pos.getY() + 0.05 + rnd.nextDouble() * 0.5;
            Fx.add(level, FxThemes.DUNGEON_GATE, FxOld.REVERSE_PORTAL, x, y, z, Math.cos(a) * 0.04, 0.08, Math.sin(a) * 0.04);
        }
        if (rnd.nextInt(3) == 0) Fx.add(level, FxThemes.DUNGEON_GATE, FxOld.END_ROD, pos.getX() + 0.2 + rnd.nextDouble() * 0.6, pos.getY() + 0.2 + rnd.nextDouble() * 0.6, pos.getZ() + 0.2 + rnd.nextDouble() * 0.6, 0.0, 0.03, 0.0);
        if (rnd.nextInt(120) == 0) level.playLocalSound(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.BLOCKS, 0.4f, 0.8f + rnd.nextFloat() * 0.4f, false);
    }
}
