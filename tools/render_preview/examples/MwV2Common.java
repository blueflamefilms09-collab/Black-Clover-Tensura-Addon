package examples;

import com.newuniverse.nusmp.aura.Aura;
import com.newuniverse.nusmp.blackclover.ModeArmor;
import com.newuniverse.nusmp.book.FuegoleonArmRules;
import com.newuniverse.nusmp.book.ModeArmorWindV2Rules.Look;
import com.newuniverse.nusmp.book.SpiritDiveDjinnPieces;
import com.newuniverse.nusmp.book.SpiritDiveWindRules.WindLook;
import com.newuniverse.nusmp.client.aura.PlayerAuraClient;
import com.newuniverse.nusmp.client.aura.SpiritDiveLook;
import com.newuniverse.nusmp.client.mode.ModeArmorClient;
import com.newuniverse.nusmp.client.mode.ModeArmorLayer;
import com.newuniverse.nusmp.client.mode.ModeArmorWindV2;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.resources.ResourceLocation;

/**
 * Preview helper of the Wind Spirit Dive overlay v2 (Mode Armour, Yuno): runs the REAL {@link ModeArmorLayer} for the mode a scene asks for, as a painter of the carrier aura MUCUS, so it draws in the
 * same pose, with the same buffers, as the Spirit Dive aura painters of the same scene. {@code yuno} true = the mode WIND_SPIRIT_DIVE_YUNO (the overlay v2 when {@code v2} is on, the classic cards when
 * it is off), false = the plain WIND_SPIRIT_DIVE (what every other Wind user wears).
 */
final class MwV2Common {
    private MwV2Common() {}

    static void install(boolean yuno, boolean v2) {
        SpiritDiveLook.set(SpiritDiveDjinnPieces.Look.REEL, true, true, FuegoleonArmRules.View.ALWAYS);
        SpiritDiveLook.setWindLook(WindLook.A_ANIME);
        SpiritDiveLook.setCalm(true);
        ModeArmorClient.MODE = yuno ? ModeArmor.Mode.WIND_SPIRIT_DIVE_YUNO : ModeArmor.Mode.WIND_SPIRIT_DIVE;
        ModeArmorWindV2.setLook(v2 ? Look.V2 : Look.CLASSIC);
        PlayerAuraClient.register(Aura.MUCUS, ctx -> {
            PlayerModel<AbstractClientPlayer> model = ctx.model();
            ModeArmorLayer layer = new ModeArmorLayer(new RenderLayerParent<AbstractClientPlayer, PlayerModel<AbstractClientPlayer>>() {
                @Override public PlayerModel<AbstractClientPlayer> getModel() { return model; }
                @Override public ResourceLocation getTextureLocation(AbstractClientPlayer e) { return null; }
            });
            layer.render(ctx.pose(), ctx.buffers(), ctx.light(), ctx.player(), ctx.limbSwing(), ctx.limbSwingAmount(), ctx.partial(), ctx.age(), ctx.netHeadYaw(), ctx.headPitch());
        });
    }
}
