package net.minecraft.client.player;

import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;

/**
 * Preview stub of AbstractClientPlayer. Crouching, invisibility and spectator are scene fields; the game reads them from the player
 * (isCrouching(), isInvisible(), isSpectator()).
 */
public class AbstractClientPlayer extends Player {
    private boolean crouching, invisible, spectator, slim;

    public AbstractClientPlayer(EntityType<?> type, Level level) { super((EntityType) type, level); }

    @Override public boolean isCrouching() { return crouching; }
    @Override public boolean isInvisible() { return invisible; }
    @Override public boolean isSpectator() { return spectator; }

    public net.minecraft.client.resources.PlayerSkin getSkin() { return new net.minecraft.client.resources.PlayerSkin(slim ? net.minecraft.client.resources.PlayerSkin.Model.SLIM : net.minecraft.client.resources.PlayerSkin.Model.WIDE); }

    /** Preview helpers (not in the game): slim arms. */
    public void previewSlim(boolean slim) { this.slim = slim; }

    /** Preview helpers (not in the game). */
    public void previewSet(boolean crouching, boolean invisible, boolean spectator) { this.crouching = crouching; this.invisible = invisible; this.spectator = spectator; }
}
