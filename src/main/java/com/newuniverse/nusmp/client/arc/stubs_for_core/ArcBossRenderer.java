// STUB for the arc core builder ONLY, so that client/arc/ArcClient type-checks before the art kit (ARC-ART) is merged.
// The real class is client/arc/ArcBossRenderer.java from ARC-ART (same package, same constructor signature).
// THE LEAD DELETES THE WHOLE FOLDER client/arc/stubs_for_core/ WHEN ARC-ART IS MERGED (two classes of the same name would not compile).
package com.newuniverse.nusmp.client.arc;

import com.newuniverse.nusmp.arc.ArcBoss;
import com.newuniverse.nusmp.arc.ArcLook;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;

/** Why: a signature-only stand-in (see the header). It draws nothing. */
public class ArcBossRenderer extends EntityRenderer<ArcBoss> {
    public ArcBossRenderer(EntityRendererProvider.Context context, ArcLook look) {
        super(context);
    }

    @Override
    public ResourceLocation getTextureLocation(ArcBoss entity) {
        return ResourceLocation.withDefaultNamespace("textures/misc/white.png");
    }
}
