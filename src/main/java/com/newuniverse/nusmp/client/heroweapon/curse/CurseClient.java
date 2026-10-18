package com.newuniverse.nusmp.client.heroweapon.curse;

import com.mojang.blaze3d.platform.InputConstants;
import com.newuniverse.nusmp.heroweapon.HeroWeaponSystem;
import com.newuniverse.nusmp.heroweapon.Sin;
import com.newuniverse.nusmp.heroweapon.curse.CurseActPayload;
import com.newuniverse.nusmp.heroweapon.curse.CurseSyncPayload;
import com.newuniverse.nusmp.heroweapon.curse.CurseTuning;
import com.newuniverse.nusmp.heroweapon.curse.CurseText;
import com.newuniverse.nusmp.heroweapon.curse.CurseView;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RegisterGuiLayersEvent;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.network.PacketDistributor;

/**
 * Why: the client side of the Curse Series. The server decides everything and sends a small packet; this class keeps the last one ({@link CurseView}), draws it
 * ({@link CurseOverlay}: the tint at the screen's edges in the sin's colour, the corruption bar, the whisper lines), plays a heartbeat at high corruption, and
 * carries the one deliberate act the owner asked for, accepting the weapon's offer: HOLD Sneak and the Embrace key for three seconds while an offer is open, and
 * the client sends one {@link CurseActPayload} that the server re-checks. A key of its own (default O, listed under the mod's Controls category) is used instead of
 * the hero technique key because holding a technique key would also cast its technique at the first tick and the curse cannot see the core's key state. Every part
 * can be switched off or moved in {@code nusmp-heroweapon-curse-client.toml}. Client only: reached from {@code HeroClient.init} and, by name inside a lambda, from
 * the packet registration, so a dedicated server never loads it.
 */
public final class CurseClient {
    private CurseClient() {}

    /** The mod's Controls category (the same string the other keys of the mod use). */
    public static final String CATEGORY = "key.categories.nusmp";

    /** Hold Sneak and this key for three seconds while the weapon's offer is open to accept it (default O). */
    public static final KeyMapping EMBRACE = new KeyMapping(CurseText.KEY_EMBRACE, InputConstants.Type.KEYSYM, org.lwjgl.glfw.GLFW.GLFW_KEY_O, CATEGORY);

    /** Ticks the key must be held (three seconds). */
    public static final int HOLD_TICKS = 60;
    private static final int COOLDOWN_TICKS = 40;

    private static int held, cooldown, beat;
    private static Sin lastSin = Sin.WRATH;

    /** Called once from {@code HeroClient.init}: the config file, the key, the overlay layer and the tick. */
    public static void init(IEventBus modBus) {
        ModContainer container = HeroWeaponSystem.modContainer();
        if (container != null) container.registerConfig(ModConfig.Type.CLIENT, CurseClientConfig.SPEC, "nusmp-heroweapon-curse-client.toml");
        modBus.addListener((RegisterKeyMappingsEvent e) -> e.register(EMBRACE));
        modBus.addListener((RegisterGuiLayersEvent e) -> e.registerAboveAll(ResourceLocation.fromNamespaceAndPath("nusmp", "hero_curse_overlay"), CurseOverlay::render));
        NeoForge.EVENT_BUS.addListener((ClientTickEvent.Post e) -> tick());
        NeoForge.EVENT_BUS.addListener((ClientPlayerNetworkEvent.LoggingOut e) -> reset());
    }

    /** The server's picture of the curse (called on the client thread by the packet handler). */
    public static void receive(CurseSyncPayload p) {
        Minecraft mc = Minecraft.getInstance();
        CurseView.apply(p.despair(), p.corruption(), p.offered(), p.active(), p.overrunLeft(), p.offerLeft(), p.series(), mc.level == null ? 0L : mc.level.getGameTime());
        if (CurseView.active() != null) lastSin = CurseView.active();
        else if (CurseView.offered() != null) lastSin = CurseView.offered();
    }

    /** The sin whose colour the overlay uses now: the active curse form, else the open offer, else the last one seen. */
    static Sin tint() {
        Sin s = CurseView.active();
        if (s != null) return s;
        s = CurseView.offered();
        return s != null ? s : lastSin;
    }

    /** Ticks the Embrace key has been held (for the progress gauge). */
    static int held() { return held; }

    static void reset() {
        held = 0;
        cooldown = 0;
        beat = 0;
        CurseView.reset();
    }

    private static void tick() {
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer p = mc.player;
        if (p == null || mc.level == null) {
            held = 0;
            return;
        }
        long now = mc.level.getGameTime();
        if (cooldown > 0) cooldown--;
        boolean open = CurseView.offerOpen(now) && !CurseView.locked(now);
        if (open && cooldown == 0 && mc.screen == null && !mc.isPaused() && EMBRACE.isDown() && p.isShiftKeyDown()) {
            held++;
            if (held >= HOLD_TICKS) {
                held = 0;
                cooldown = COOLDOWN_TICKS;
                PacketDistributor.sendToServer(new CurseActPayload(CurseActPayload.EMBRACE));
            }
        } else {
            held = 0;
        }
        heartbeat(mc, p, now);
    }

    /** A heartbeat that quickens with the corruption, from 600 on (and throughout an Overrun). */
    private static void heartbeat(Minecraft mc, LocalPlayer p, long now) {
        if (!CurseClientConfig.heartbeat() || mc.isPaused()) return;
        int c = CurseView.locked(now) ? CurseTuning.MAX : CurseView.corruption();
        int period = VignetteMath.heartbeatPeriodTicks(c);
        if (period <= 0) {
            beat = 0;
            return;
        }
        if (++beat < period) return;
        beat = 0;
        float volume = 0.35f + 0.55f * Math.max(0f, Math.min(1f, (c - 600) / 400f));
        mc.level.playLocalSound(p.getX(), p.getY(), p.getZ(), SoundEvents.WARDEN_HEARTBEAT, SoundSource.PLAYERS, volume, 1.0f, false);
    }
}
