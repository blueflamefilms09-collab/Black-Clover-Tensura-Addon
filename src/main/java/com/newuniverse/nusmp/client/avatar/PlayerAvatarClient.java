package com.newuniverse.nusmp.client.avatar;

import com.newuniverse.nusmp.avatar.AvatarEntities;
import com.newuniverse.nusmp.avatar.AvatarEntity;
import com.newuniverse.nusmp.avatar.menu.AvatarMenus;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.world.entity.Entity;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.ModelEvent;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;
import net.neoforged.neoforge.common.NeoForge;
import org.joml.Vector3f;

/**
 * Why: the one client entry point of the Avatar creation system (the Devil Avatar boss keeps its own {@code AvatarClient}; a second {@code register}
 * overload there would make its existing method reference ambiguous). Registers the renderer for the player-made Avatar, the creation screens (the
 * classic one and the Soul Forge's), drops cached textures on a resource reload and on logout, and runs the small client tick that spawns the frenzy's dark particles and the aura's motes.
 * Call {@code PlayerAvatarClient.register(modEventBus)} from the client block of NUSMP.
 */
public final class PlayerAvatarClient {
    private PlayerAvatarClient() {}

    public static void register(IEventBus modBus) {
        modBus.addListener((EntityRenderersEvent.RegisterRenderers e) -> e.registerEntityRenderer(AvatarEntities.AVATAR.get(), PlayerAvatarRenderer::new));
        modBus.addListener((RegisterMenuScreensEvent e) -> {
            e.register(AvatarMenus.BUILDER.get(), AvatarBuilderScreen::new);
            e.register(AvatarMenus.SOUL_FORGE.get(), SoulForgeScreen::new);
        });
        modBus.addListener((ModelEvent.BakingCompleted e) -> { AvatarSkinCache.clear(); AvatarLooks.clear(); ForgeIcons.clear(); });
        NeoForge.EVENT_BUS.addListener((ClientPlayerNetworkEvent.LoggingOut e) -> { AvatarSkinCache.clear(); AvatarLooks.clear(); AvatarClientData.clear(); SoulForgeClientData.clear(); ForgeIcons.clear(); });
        NeoForge.EVENT_BUS.addListener((ClientTickEvent.Post e) -> tick());
    }

    private static int phase;

    /** Frenzy: dark red smoke every tick; otherwise a faint mote of the aura colour now and then. Only near the camera, only a few. */
    private static void tick() {
        Minecraft mc = Minecraft.getInstance();
        ClientLevel level = mc.level;
        if (level == null || mc.player == null || mc.isPaused()) return;
        phase++;
        for (Entity en : level.entitiesForRendering()) {
            if (!(en instanceof AvatarEntity a) || a.isInvisible() || a.distanceToSqr(mc.player) > 48 * 48) continue;
            var r = level.random;
            float s = a.getScale();
            if (a.frenzied()) {
                level.addParticle(ParticleTypes.LARGE_SMOKE, a.getX() + (r.nextDouble() - 0.5) * 0.8 * s, a.getY() + r.nextDouble() * 1.8 * s, a.getZ() + (r.nextDouble() - 0.5) * 0.8 * s, 0, 0.04, 0);
                if ((phase & 1) == 0)
                    level.addParticle(new DustParticleOptions(new Vector3f(0.45f, 0.02f, 0.06f), 1.3f), a.getX() + (r.nextDouble() - 0.5) * s, a.getY() + r.nextDouble() * 1.9 * s,
                            a.getZ() + (r.nextDouble() - 0.5) * s, 0, 0.06, 0);
            } else if ((phase + a.getId()) % 8 == 0) {
                int c = AvatarLooks.of(a).auraRgb();
                level.addParticle(new DustParticleOptions(new Vector3f(((c >> 16) & 255) / 255f, ((c >> 8) & 255) / 255f, (c & 255) / 255f), 0.7f),
                        a.getX() + (r.nextDouble() - 0.5) * 1.2 * s, a.getY() + 0.1, a.getZ() + (r.nextDouble() - 0.5) * 1.2 * s, 0, 0.03, 0);
            }
        }
    }
}
