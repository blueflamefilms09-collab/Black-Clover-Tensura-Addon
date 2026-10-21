package com.newuniverse.nusmp.client.music;

import com.newuniverse.nusmp.audio.music.MusicCatalog;
import com.newuniverse.nusmp.audio.music.MusicDirector;
import com.newuniverse.nusmp.audio.music.MusicSounds;
import com.newuniverse.nusmp.audio.music.MusicState;
import com.newuniverse.nusmp.labyrinth.Labyrinth;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.SelectMusicEvent;
import net.neoforged.neoforge.common.NeoForge;

import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * Why: the client half of the themed dungeon music: it plays a loop that fits the floor you are on and cross-fades when the situation changes. Once per client tick (not while the
 * game is paused) it asks the last state the server sent ({@link MusicClientState}) which track is wanted, hands that to the pure {@link MusicDirector} (which owns every timing
 * decision: the fade in and out, the cooldown, the hold after a fight, the faster switch UP to a fight or a boss), and mirrors the director's voices onto sound instances
 * ({@link ThemedMusicInstance}, MUSIC category, looping, streamed from {@code sounds.json}) whose volume follows the voice's gain times the {@code themedVolume} option. While a voice
 * exists the vanilla music is kept off (the {@code SelectMusicEvent} answer is "no music", and a vanilla track that was already playing is stopped), because two soundtracks at
 * once is the one thing worse than none. The Music slider of the sound settings is honoured twice: the instances use the MUSIC category, and at slider 0 nothing is started at all
 * (no streaming of a file nobody can hear). Outside the Mega-Dungeon and the Hub, with the switch off, or after a logout, every voice fades out or is dropped and vanilla music is
 * back. A loop that the sound engine dropped (a resource reload, F3+S) is started again.
 * <p>
 * The tracks are plain files ({@code assets/nusmp/sounds/music/<track>.ogg}), so a resource pack replaces them with real music without any change here; see docs/audio/themed_music.md.
 * Client only; started from the client block of the mod class.
 */
public final class ThemedMusicEngine {
    private ThemedMusicEngine() {}

    private static final MusicDirector DIRECTOR = new MusicDirector(MusicDirector.Params.defaults());
    private static final Map<Long, ThemedMusicInstance> PLAYING = new HashMap<>();
    private static long now;
    private static double lastFade = -1, lastCooldown = -1;

    /** Mod bus: the client config; game bus: the tick, the vanilla-music veto and the logout clean-up. */
    public static void init(IEventBus modBus, ModContainer container) {
        container.registerConfig(ModConfig.Type.CLIENT, ThemedMusicConfig.SPEC, "nusmp-music-client.toml");
        NeoForge.EVENT_BUS.addListener(ThemedMusicEngine::onTick);
        NeoForge.EVENT_BUS.addListener(ThemedMusicEngine::onSelectMusic);
        NeoForge.EVENT_BUS.addListener((ClientPlayerNetworkEvent.LoggingOut e) -> shutdown());
    }

    private static void onSelectMusic(SelectMusicEvent e) {
        if (DIRECTOR.isActive() && ThemedMusicConfig.stopVanilla()) e.setMusic(null);
    }

    private static boolean dimensionOk(ClientLevel level) { return Labyrinth.inMega(level) || Labyrinth.inSanctuary(level); }

    private static void onTick(ClientTickEvent.Post e) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || mc.player == null) {
            shutdown();
            return;
        }
        if (mc.isPaused()) return;
        now++;
        double fade = ThemedMusicConfig.fadeSeconds(), cool = ThemedMusicConfig.cooldownSeconds();
        if (fade != lastFade || cool != lastCooldown) {
            DIRECTOR.setParams(MusicDirector.Params.ofSeconds(fade, cool));
            lastFade = fade;
            lastCooldown = cool;
        }
        String wanted = null;
        int priority = 0;
        if (ThemedMusicConfig.themed() && mc.options.getSoundSourceVolume(SoundSource.MUSIC) > 0.0f && dimensionOk(mc.level)) {
            MusicState s = MusicClientState.fresh(now);
            wanted = s.track();
            priority = s.layer().priority();
        }
        boolean started = DIRECTOR.tick(now, wanted, priority);
        sync(mc);
        if (DIRECTOR.isActive() && ThemedMusicConfig.stopVanilla()) mc.getMusicManager().stopPlaying();
        if (started && ThemedMusicConfig.showTitle() && wanted != null) {
            mc.gui.setOverlayMessage(Component.translatableWithFallback(MusicCatalog.titleKey(wanted), wanted.replace('_', ' ')), false);
        }
    }

    /** Mirrors the director's voices onto sound instances: starts the missing ones, sets the volumes, stops the ones that are gone or were dropped by the sound engine. */
    private static void sync(Minecraft mc) {
        float user = ThemedMusicConfig.volume();
        List<MusicDirector.Voice> voices = DIRECTOR.voices();
        for (int i = 0; i < voices.size(); i++) {
            MusicDirector.Voice v = voices.get(i);
            ThemedMusicInstance inst = PLAYING.get(v.id());
            if (inst != null && now - inst.startedAt() > 60 && !mc.getSoundManager().isActive(inst)) {
                inst.finish();
                PLAYING.remove(v.id());
                inst = null;                                       // the engine dropped it (a reload): start it again below
            }
            if (inst == null) {
                SoundEvent ev = MusicSounds.event(v.track());
                if (ev == null) continue;
                inst = new ThemedMusicInstance(ev, now);
                PLAYING.put(v.id(), inst);
                mc.getSoundManager().play(inst);
            }
            inst.setLevel(v.level() * user);
        }
        for (Iterator<Map.Entry<Long, ThemedMusicInstance>> it = PLAYING.entrySet().iterator(); it.hasNext(); ) {
            Map.Entry<Long, ThemedMusicInstance> en = it.next();
            if (DIRECTOR.voice(en.getKey()) == null) {
                en.getValue().finish();
                mc.getSoundManager().stop(en.getValue());
                it.remove();
            }
        }
    }

    /** Logout, no world: every voice stops at once and the state is forgotten. */
    private static void shutdown() {
        if (PLAYING.isEmpty() && !DIRECTOR.isActive()) {
            MusicClientState.clear();
            return;
        }
        Minecraft mc = Minecraft.getInstance();
        for (ThemedMusicInstance inst : PLAYING.values()) {
            inst.finish();
            if (mc != null && mc.getSoundManager() != null) mc.getSoundManager().stop(inst);
        }
        PLAYING.clear();
        DIRECTOR.reset();
        MusicClientState.clear();
    }
}
