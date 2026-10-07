package examples;

import com.newuniverse.nusmp.aura.Aura;
import com.newuniverse.nusmp.client.aura.AuraRender;
import com.newuniverse.nusmp.client.aura.PlayerAuraClient;

/** Self-test of the preview's checks: an aura painter that pushes a pose and never pops it (registered under Aura.COPPER). */
public final class BadAura {
    private BadAura() {}

    public static void register() {
        PlayerAuraClient.register(Aura.COPPER, c -> {
            c.pose().pushPose();
            AuraRender.model(c, AuraRender.cutout(AuraRender.tex("aura/preview_overlay")), 1.02f, 1.02f, 1.02f, 0xFFFFFFFF);
        });
    }
}
