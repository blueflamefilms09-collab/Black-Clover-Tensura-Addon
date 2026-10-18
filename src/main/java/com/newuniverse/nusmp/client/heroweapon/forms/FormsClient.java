package com.newuniverse.nusmp.client.heroweapon.forms;

import com.mojang.blaze3d.platform.InputConstants;
import com.mojang.logging.LogUtils;
import com.newuniverse.nusmp.heroweapon.forms.FormsNet;
import com.newuniverse.nusmp.heroweapon.forms.HeroForms;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.network.PacketDistributor;
import org.lwjgl.glfw.GLFW;
import org.slf4j.Logger;

/**
 * Why: the client side of the forms system (the Weapon Book screen, its key, the answer to the server's state packet). The key asks the SERVER to open the book, and
 * the screen opens when the answer arrives, so a player with no weapon gets the server's explanation on the action bar instead of an empty screen, and the book
 * always opens on facts the server has just sent. Default key {@code M}: it is not a vanilla default and no key of this mod uses it (the keys taken by the mod are V, R,
 * G, J, K, H, Y, U, the arrows, Home and End; the core's hero keys are Z, X, C, B and N); a clash with another mod's key is shown by the controls screen and cured by
 * rebinding. The same screen is opened by {@code /multiverse hero form book}. Owned by the forms builder.
 */
public final class FormsClient {
    private FormsClient() {}

    private static final Logger LOG = LogUtils.getLogger();

    /** The Weapon Book key ({@code key.nusmp.hero_book}), default M. */
    public static final KeyMapping KEY = new KeyMapping("key.nusmp.hero_book", InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_M, "key.categories.nusmp");

    public static void init(IEventBus modBus) {
        modBus.addListener((RegisterKeyMappingsEvent e) -> e.register(KEY));
        NeoForge.EVENT_BUS.addListener((ClientTickEvent.Post e) -> tick());
        NeoForge.EVENT_BUS.addListener((ClientPlayerNetworkEvent.LoggingOut e) -> {
            ClientBook.clear();
            HeroForms.forgetSynced();
        });
    }

    private static void tick() {
        Minecraft mc = Minecraft.getInstance();
        while (KEY.consumeClick()) {
            if (mc.player == null || mc.screen != null) continue;
            try {
                PacketDistributor.sendToServer(FormsNet.openRequest());
            } catch (RuntimeException e) {
                LOG.warn("[nusmp] could not ask the server for the Weapon Book", e);
            }
        }
    }

    /** The server's state packet (client thread): remember it, refresh an open book, or open the book when this is the answer to a request. */
    public static void onState(FormsNet.State state) {
        ClientBook.set(state);
        Minecraft mc = Minecraft.getInstance();
        if (mc.screen instanceof WeaponBookScreen book) book.refresh(state);
        else if (state.open()) mc.setScreen(new WeaponBookScreen());
    }
}
