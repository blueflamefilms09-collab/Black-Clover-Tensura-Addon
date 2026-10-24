package examples;

import com.newuniverse.nusmp.aura.Aura;
import com.newuniverse.nusmp.blackclover.ModeArmor;
import com.newuniverse.nusmp.book.ModeArmorValkyrieRules.Form;
import com.newuniverse.nusmp.book.ModeArmorValkyrieRules.LanceMode;
import com.newuniverse.nusmp.book.ModeArmorValkyrieRules.Look;
import com.newuniverse.nusmp.client.aura.PlayerAuraClient;
import com.newuniverse.nusmp.client.mode.ModeArmorClient;
import com.newuniverse.nusmp.client.mode.ModeArmorLayer;
import com.newuniverse.nusmp.client.mode.ModeArmorValkyrieV2;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.resources.ResourceLocation;

/**
 * Preview helper of the Valkyrie armour v2 (Mode Armour, Noelle's Valkyrie Dress): runs the REAL {@link ModeArmorLayer} for the mode VALKYRIE as a painter of the carrier aura MUCUS, so it draws in the same
 * pose, with the same buffers and the same {@link ModeArmorValkyrieV2} (meshes, plan, pool, spin, wings, lance) as the game. {@code v2} false = the option CLASSIC: exactly the old overlay.
 */
final class Vg2Common {
    private Vg2Common() {}

    static void install(boolean v2, Form form, LanceMode lance) {
        ModeArmorClient.MODE = ModeArmor.Mode.VALKYRIE;
        ModeArmorValkyrieV2.setLook(v2 ? Look.V2 : Look.CLASSIC);
        ModeArmorValkyrieV2.setForm(form);
        ModeArmorValkyrieV2.setLance(lance);
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
