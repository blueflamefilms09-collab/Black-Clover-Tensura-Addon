package com.newuniverse.nusmp.client.relics.worn;

/**
 * Draws one worn relic on a player, in the pose the player's model is in (the layer has already applied the entity transform, so the model's parts
 * are posed for this frame). Implementations push and pop their own pose changes. This is the registry's answer to Curios' {@code ICurioRenderer},
 * which cannot be implemented here because Curios is read reflectively (see {@code item/CuriosLink}).
 */
@FunctionalInterface
public interface WornRenderer {
    void render(WornCtx c);
}
