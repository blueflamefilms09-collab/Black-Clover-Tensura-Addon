package com.newuniverse.nusmp.elite.races;

import com.mojang.logging.LogUtils;
import com.newuniverse.nusmp.NUSMP;
import com.newuniverse.nusmp.elite.races.cmd.RaceCommands;
import com.newuniverse.nusmp.elite.races.data.RaceTemplateLoader;
import com.newuniverse.nusmp.elite.races.data.RaceTemplates;
import com.newuniverse.nusmp.elite.races.net.RaceNet;
import com.newuniverse.nusmp.elite.races.race.DivineSaiyan;
import com.newuniverse.nusmp.elite.races.race.HighSaiyan;
import com.newuniverse.nusmp.elite.races.race.LesserSaiyan;
import com.newuniverse.nusmp.elite.races.race.MediumSaiyan;
import com.newuniverse.nusmp.elite.races.race.PrimordialRace;
import com.newuniverse.nusmp.elite.races.runtime.RaceRuntime;
import io.github.manasmods.manascore.race.api.ManasRace;
import io.github.manasmods.manascore.race.api.RaceAPI;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.AddReloadListenerEvent;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import org.slf4j.Logger;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Why: the ONE registry class of the elite races (docs/elite/plan.md), and the answer to "how does a mod add a Tensura race". A race of Tensura Reincarnated is a
 * {@code ManasRace} in ManasCore's race registry, and a mod adds one exactly as this mod already adds skills to ManasCore's skill registry ({@code NUSkills}): a NeoForge
 * {@code DeferredRegister} created with {@code RaceAPI.getRaceRegistryKey()} and registered on the mod bus. So the five races (Lesser, Medium, High and Divine Saiyan, and the
 * Primordial) are REAL Tensura races, listed by Tensura's own race command and evolution screen, not a buff laid over another race (which is what {@code multiverse/RaceBuffs} does for
 * the Tensura families, and why that mechanism is kept untouched beside this one).
 *
 * <p>{@link #init} does everything, in the one order that works: read the bundled race files first (a race bakes its stats when it is built, at registration, long before any
 * datapack), then the config, then the register, the payloads, the listeners, the self-tests, and on the client the key and the HUD line. It is idempotent. The wiring line in
 * {@code EliteSystem} is the only change outside this package.
 */
public final class EliteRaces {
    private EliteRaces() {}

    private static final Logger LOG = LogUtils.getLogger();
    private static boolean done;

    public static final String PRIMORDIAL_ID = PrimordialRace.ID;

    public static final DeferredRegister<ManasRace> RACES = DeferredRegister.create(RaceAPI.getRaceRegistryKey(), NUSMP.MODID);

    // The template files must be read before these suppliers run (they run at registration): init() does that first.
    public static final DeferredHolder<ManasRace, LesserSaiyan> LESSER_SAIYAN = RACES.register(LesserSaiyan.ID, LesserSaiyan::new);
    public static final DeferredHolder<ManasRace, MediumSaiyan> MEDIUM_SAIYAN = RACES.register(MediumSaiyan.ID, MediumSaiyan::new);
    public static final DeferredHolder<ManasRace, HighSaiyan> HIGH_SAIYAN = RACES.register(HighSaiyan.ID, HighSaiyan::new);
    public static final DeferredHolder<ManasRace, DivineSaiyan> DIVINE_SAIYAN = RACES.register(DivineSaiyan.ID, DivineSaiyan::new);
    public static final DeferredHolder<ManasRace, PrimordialRace> PRIMORDIAL = RACES.register(PrimordialRace.ID, PrimordialRace::new);

    private static final Map<String, DeferredHolder<ManasRace, ? extends ManasRace>> BY_ID = new LinkedHashMap<>();

    static {
        BY_ID.put(LesserSaiyan.ID, LESSER_SAIYAN);
        BY_ID.put(MediumSaiyan.ID, MEDIUM_SAIYAN);
        BY_ID.put(HighSaiyan.ID, HIGH_SAIYAN);
        BY_ID.put(DivineSaiyan.ID, DIVINE_SAIYAN);
        BY_ID.put(PrimordialRace.ID, PRIMORDIAL);
    }

    /** The registered race for a template id, or null while it is not bound (before registration) or when the id is not one of ours. */
    public static ManasRace race(String templateId) {
        DeferredHolder<ManasRace, ? extends ManasRace> h = BY_ID.get(templateId);
        if (h == null) return null;
        try {
            return h.get();
        } catch (RuntimeException notBound) {
            return null;
        }
    }

    public static List<String> ids() { return List.copyOf(BY_ID.keySet()); }

    /** The single entry point, called from {@code EliteSystem.init} on both sides. Safe to call twice. */
    public static synchronized void init(IEventBus modBus, ModContainer container) {
        if (done) return;
        done = true;
        List<String> problems = RaceTemplates.loadBundled();
        for (String p : problems) LOG.error("[nusmp] elite races: {}", p);
        for (String p : RaceTemplates.checkLines(RaceTemplates.all())) LOG.error("[nusmp] elite races: line problem: {}", p);
        container.registerConfig(ModConfig.Type.SERVER, EliteRacesConfig.SPEC, "nusmp-elite-races-server.toml");
        RACES.register(modBus);
        modBus.addListener(RaceNet::register);
        RaceRuntime.register(NeoForge.EVENT_BUS);
        NeoForge.EVENT_BUS.addListener((AddReloadListenerEvent e) -> e.addListener(new RaceTemplateLoader()));
        NeoForge.EVENT_BUS.addListener(RaceCommands::onRegister);
        RaceSelfTests.register();                          // keys elite.races.*, expected by the server smoke test
        if (FMLEnvironment.dist == Dist.CLIENT) com.newuniverse.nusmp.elite.races.client.RaceClient.init(modBus);
        LOG.info("[nusmp] elite races: {} races registered, {} template files read", BY_ID.size(), RaceTemplates.all().size());
    }
}
