package com.newuniverse.nusmp.relics.wave;

import com.newuniverse.nusmp.vfx.VfxPayload;
import com.newuniverse.nusmp.vfx.VfxShape;
import com.newuniverse.nusmp.vfx.VfxSpawn;
import com.newuniverse.nusmp.vfx.deck.DeckSummonGeometry;
import com.newuniverse.nusmp.vfx.deck.DeckSummonTimeline;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;

/**
 * Why: the client plays a short card summon when a card of the Deck of Many Things is drawn (vfx.client.layer.DeckSummonLayer). The server only says which card (its number in the table,
 * its alignment, the zone of the disk it lands on) in the payload seed of one effect packet; it goes out through {@link VfxSpawn}, so it is counted against the per-tick effect packet budget and
 * reaches the players near the drawer only. It decides nothing and never throws into the card's own effect.
 */
final class Wave30Summon {
    private Wave30Summon() {}

    static void send(ServerPlayer p, ServerLevel level, Wave30DeckMath.Card card, int indexInDraw) {
        try {
            int mood = card.mood == Wave30DeckMath.Mood.GOOD ? 0 : card.mood == Wave30DeckMath.Mood.NEUTRAL ? 1 : 2;
            long seed = DeckSummonGeometry.pack(card.ordinal(), Math.floorMod(indexInDraw, DeckSummonGeometry.ZONES), mood);
            int colour = 0xFF000000 | DeckSummonGeometry.alignmentColour(mood);
            VfxSpawn.send(level, new VfxPayload(VfxShape.DECK_SUMMON.ordinal(), p.position(), p.position(), colour, DeckSummonTimeline.TOTAL, 1.0f, p.getId(), seed));
        } catch (RuntimeException | LinkageError ignored) {
            // an animation must never break a card
        }
    }
}
