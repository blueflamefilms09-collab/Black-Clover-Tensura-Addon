package com.newuniverse.nusmp.client.playermodel;

import com.mojang.logging.LogUtils;
import com.newuniverse.nusmp.arc.ArcBoss;
import com.newuniverse.nusmp.arc.ArcEntities;
import com.newuniverse.nusmp.arc.goku.GokuBoss;
import com.newuniverse.nusmp.arc.goku.GokuLadder;
import com.newuniverse.nusmp.arc.magi.SinbadBoss;
import com.newuniverse.nusmp.client.arc.ArcBossRenderer;
import com.newuniverse.nusmp.client.relics.djinn.DjinnBossLayer;
import com.newuniverse.nusmp.client.zogratis.ZogratisGlow;
import com.newuniverse.nusmp.entity.zogratis.Move;
import com.newuniverse.nusmp.entity.zogratis.ZogratisBoss;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.world.entity.EntityType;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import org.slf4j.Logger;

/**
 * Why: Goku and Sinbad on the redesigned player model (docs/boss_redesign_goku_sinbad.md), the same way the Black Clover bosses are (docs/bc_playermodels.md). Two {@link PlayerBody}s over the shared
 * {@link PlayerBodies.Arc} body (glow curve, cast poses, the fade of a dying body, the hitbox fit stay exactly the arcs'), each answering only which look the boss wears now:
 * <ul>
 *   <li>{@link Goku}: the look of his ladder step ({@link GsLooks#goku}: black, gold, gold with taller hair, the long mane, red, blue, silver, white), the glow tinted with the form's colour (the Kaio-ken
 *       step adds red over the blue), the power-up flash in the form's colour, and a form aura layer ({@link GsAuraLayer}) that grows with the step;</li>
 *   <li>{@link Sinbad}: the body of the djinn he wears (the synced equip number, written by the server: {@code SinbadBoss#equipIdx}) and, on top of it, the player's own Djinn Equip armour through the
 *       boss-safe adapter {@link DjinnBossLayer} (the same pieces, the same assemble and shatter animation, the same burst).</li>
 * </ul>
 * Both are chosen by {@link #register} only while the client option {@code bossLooks.bossLookClassic} is off; with it on the earlier geo-model renderer ({@code ArcBossRenderer}) draws them exactly as before.
 * Client only; nothing here is reached from common code. Cost per frame: one table lookup per boss plus the layers' own bounded drawing.
 */
public final class GsBodies {
    private GsBodies() {}

    private static final Logger LOG = LogUtils.getLogger();

    /** Registers the renderers of both bosses, after the kit's own registrations (the last registration of a type wins). A missing type is logged and skipped. */
    public static void register(EntityRenderersEvent.RegisterRenderers e) {
        for (ArcEntities.Entry b : ArcEntities.bosses()) {
            boolean goku = GsLooks.GOKU_BOSS.equals(b.id()), sinbad = GsLooks.SINBAD_BOSS.equals(b.id());
            if (!goku && !sinbad) continue;
            try {
                @SuppressWarnings("unchecked") EntityType<ArcBoss> type = (EntityType<ArcBoss>) b.type().get();
                e.registerEntityRenderer(type, ctx -> choose(ctx, b, goku));
            } catch (RuntimeException ex) {
                LOG.error("[anime_multiverse] the redesigned renderer of {} could not be registered: {}", b.id(), ex.toString());
            }
        }
    }

    private static PlayerModelRegistry.Split<ArcBoss> choose(EntityRendererProvider.Context ctx, ArcEntities.Entry b, boolean goku) {
        PlayerBody<ArcBoss> body = goku ? new Goku() : new Sinbad();
        return new PlayerModelRegistry.Split<ArcBoss>(ctx, x -> !PlayerModelOptions.bossLookClassic(), new PlayerModelRenderer<>(ctx, body), new ArcBossRenderer(ctx, b.look()));
    }

    private static PlayerLook lookOf(String id) { return PlayerAssets.look(PlayerAssets.hasLook(id) ? id : com.newuniverse.nusmp.client.playermodel.SkinPath.DEFAULT_ID); }

    // ================================================================================================ Goku
    public static final class Goku implements PlayerBody<ArcBoss> {
        private final PlayerBodies.Boss<ArcBoss> base = new PlayerBodies.Boss<ArcBoss>(false, false, -1) {
            @Override protected String first(ArcBoss e) { return GsLooks.goku(step(e)); }
        };

        static int step(ArcBoss e) { return e instanceof GokuBoss g ? g.formIdx() : 0; }

        @Override public PlayerLook look(ArcBoss e) { return lookOf(GsLooks.goku(step(e))); }

        @Override
        public void dynamic(ArcBoss e, float partial, Dynamic d) {
            base.dynamic(e, partial, d);
            int s = step(e);
            d.size = PlayerBodies.fit(e, look(e), 1f);
            int rgb = GokuLadder.rgb(s);
            d.glowRgb = s == GsLooks.GOKU_KAIOKEN_STEP ? 0xFF3A30 : rgb;
            if (s == GsLooks.GOKU_KAIOKEN_STEP) d.tint = 0xFFC0B8;                 // the Kaio-ken's red over the blue body
            if (d.flash > 0.02f) d.flashRgb = ZogratisGlow.mix(rgb, 0xFFFFFF, 0.5f);
        }

        @Override public void pose(ArcBoss e, float partial, PoseState p) { base.pose(e, partial, p); }

        @Override
        public void layers(PlayerModelRenderer.Body<ArcBoss> body, EntityRendererProvider.Context ctx) { body.addLayer(new GsAuraLayer(body)); }
    }

    // ================================================================================================ Sinbad
    public static final class Sinbad implements PlayerBody<ArcBoss> {
        private final PlayerBodies.Boss<ArcBoss> base = new PlayerBodies.Boss<ArcBoss>(false, false, -1) {
            @Override protected String first(ArcBoss e) { return GsLooks.sinbad(idx(e)); }
        };

        static int idx(ArcBoss e) { return e instanceof SinbadBoss s ? s.equipIdx() : 0; }

        @Override
        public PlayerLook look(ArcBoss e) {
            long gt = e.level().getGameTime();
            return lookOf(GsLooks.sinbad(DjinnBossLayer.bodyIdx(e.getId(), idx(e), gt)));
        }

        @Override
        public void dynamic(ArcBoss e, float partial, Dynamic d) {
            base.dynamic(e, partial, d);
            d.size = PlayerBodies.fit(e, look(e), 1f);
        }

        @Override public void pose(ArcBoss e, float partial, PoseState p) { base.pose(e, partial, p); }

        @Override
        public void layers(PlayerModelRenderer.Body<ArcBoss> body, EntityRendererProvider.Context ctx) {
            body.addLayer(new DjinnBossLayer<ArcBoss>(body, Sinbad::idx, e -> {
                Move m = e.state() == ZogratisBoss.STATE_CASTING ? e.castingMoveOrNull() : null;
                return m != null && m.ultimate();
            }));
        }
    }
}
