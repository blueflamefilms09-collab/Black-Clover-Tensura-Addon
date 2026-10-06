package com.newuniverse.nusmp.multiverse;

import com.newuniverse.nusmp.blackclover.GrimoirePages;
import com.newuniverse.nusmp.blackclover.Kingdom;
import net.minecraft.ChatFormatting;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundSetSubtitleTextPacket;
import net.minecraft.network.protocol.game.ClientboundSetTitleTextPacket;
import net.minecraft.network.protocol.game.ClientboundSetTitlesAnimationPacket;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.saveddata.SavedData;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.server.ServerStartedEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * The Convergence (0.39): the world starts as pure Tensura, and the Black Clover world slowly bleeds into it.
 *
 * <ul>
 *   <li><b>Origin roll:</b> the first time a player joins, they get one server-side roll, kept by account (leaving and rejoining or
 *       an alt cannot reroll it). About 5% (config) are <b>anomalies</b>, Black Clover-origin mages; everyone else is Tensura-origin.
 *       Pity rule: after {@code pityEvery} Tensura rolls in a row the next player is an anomaly. An anomaly's kingdom is a second
 *       weighted roll (50 Clover / 20 Diamond / 20 Heart / 10 Spade) and picks the covers their grimoire can have.</li>
 *   <li><b>Awakening:</b> an anomaly first notices [Unknown Magic Detected] after a few minutes of play, and then the world gives
 *       them their grimoire ("You have been chosen by another world's magic."), or a Grimoire Tower / altar does it sooner.
 *       Nobody else can be chosen: grimoires come only from the world or an admin.</li>
 *   <li><b>Stages</b> (server-wide): SIGNS (rumours, ruins that don't match the world) -> FIRST_GRIMOIRE (the first anomaly is
 *       chosen) -> CLOVER -> DIAMOND -> HEART -> SPADE. Each advances by itself after some real days (config) or by
 *       /multiverse convergence stage, and is announced anonymously.</li>
 * </ul>
 * Players who already had a grimoire before 0.39 become anomalies of their cover's kingdom. Everybody keeps their Tensura race and
 * skills; an anomaly simply also has a grimoire.
 */
public final class Convergence {
    /** Append only: stored by name, compared by order. */
    public enum Stage {
        SIGNS("The First Signs"), FIRST_GRIMOIRE("The First Grimoire"), CLOVER("The Clover Kingdom"), DIAMOND("The Diamond Kingdom"),
        HEART("The Heart Kingdom"), SPADE("The Spade Kingdom");

        public final String title;
        Stage(String title) { this.title = title; }

        public boolean atLeast(Stage s) { return ordinal() >= s.ordinal(); }

        public static Stage byName(String s) {
            for (Stage st : values()) if (st.name().equalsIgnoreCase(s)) return st;
            return SIGNS;
        }
    }

    public enum Origin { UNROLLED, TENSURA, ANOMALY }

    /** The current stage for code that runs off the server thread (world generation). */
    private static volatile Stage cached = Stage.SIGNS;

    private Convergence() {}

    public static boolean enabled() { return MultiverseConfig.get(MultiverseConfig.CONVERGENCE_ENABLED); }

    // ---------------------------------------------------------------- saved state
    static final class State extends SavedData {
        Stage stage = Stage.SIGNS;
        long stageSince = System.currentTimeMillis();
        int sinceAnomaly;                                   // Tensura rolls in a row (pity counter)
        final Map<UUID, CompoundTag> players = new HashMap<>();

        static State get(MinecraftServer server) {
            return server.overworld().getDataStorage().computeIfAbsent(new SavedData.Factory<>(State::new, State::load, null), "nusmp_convergence");
        }

        private static State load(CompoundTag tag, HolderLookup.Provider provider) {
            State s = new State();
            s.stage = Stage.byName(tag.getString("Stage"));
            s.stageSince = tag.contains("StageSince") ? tag.getLong("StageSince") : System.currentTimeMillis();
            s.sinceAnomaly = tag.getInt("SinceAnomaly");
            CompoundTag ps = tag.getCompound("Players");
            for (String k : ps.getAllKeys()) {
                try { s.players.put(UUID.fromString(k), ps.getCompound(k)); } catch (IllegalArgumentException ignored) {}
            }
            return s;
        }

        @Override
        public CompoundTag save(CompoundTag tag, HolderLookup.Provider provider) {
            tag.putString("Stage", stage.name());
            tag.putLong("StageSince", stageSince);
            tag.putInt("SinceAnomaly", sinceAnomaly);
            CompoundTag ps = new CompoundTag();
            players.forEach((u, t) -> ps.put(u.toString(), t));
            tag.put("Players", ps);
            return tag;
        }

        CompoundTag record(UUID id) { return players.computeIfAbsent(id, k -> new CompoundTag()); }
    }

    // ---------------------------------------------------------------- queries
    public static Stage stage(MinecraftServer server) { return State.get(server).stage; }

    /** Stage for world generation threads (the last value the server saw). */
    public static Stage cachedStage() { return cached; }

    public static Origin origin(ServerPlayer p) {
        CompoundTag r = State.get(p.getServer()).players.get(p.getUUID());
        if (r == null || !r.contains("Origin")) return Origin.UNROLLED;
        return "ANOMALY".equals(r.getString("Origin")) ? Origin.ANOMALY : Origin.TENSURA;
    }

    /** True when the Convergence is off (old rules) or this player is an anomaly. */
    public static boolean mayHaveGrimoire(ServerPlayer p) { return !enabled() || origin(p) == Origin.ANOMALY; }

    public static boolean isAnomaly(ServerPlayer p) { return enabled() && origin(p) == Origin.ANOMALY; }

    /** The anomaly's kingdom (picks their covers), or null when the Convergence is off or they are Tensura-origin. */
    public static Kingdom kingdomOf(ServerPlayer p) {
        if (!isAnomaly(p)) return null;
        CompoundTag r = State.get(p.getServer()).players.get(p.getUUID());
        try { return Kingdom.valueOf(r.getString("Kingdom")); } catch (IllegalArgumentException e) { return Kingdom.CLOVER; }
    }

    /** Has this anomaly felt [Unknown Magic Detected] yet? */
    public static boolean detected(ServerPlayer p) {
        CompoundTag r = State.get(p.getServer()).players.get(p.getUUID());
        return r != null && r.getBoolean("Detected");
    }

    /** Has the Black Clover side been revealed to this player? (anomalies and grimoire holders always; everyone else from CLOVER on) */
    public static boolean revealed(ServerPlayer p) {
        return !enabled() || isAnomaly(p) || stage(p.getServer()).atLeast(Stage.CLOVER) || GrimoirePages.grimoireOf(p).isPresent();
    }

    // ---------------------------------------------------------------- the roll
    /** First join: one roll per account, ever. Existing grimoire holders become anomalies of their cover's kingdom. */
    public static void roll(ServerPlayer p) {
        State s = State.get(p.getServer());
        CompoundTag r = s.record(p.getUUID());
        if (r.contains("Origin")) return;
        var held = GrimoirePages.grimoireOf(p);
        if (held.isPresent()) {                                             // grandfathered (from before 0.39, or an admin grant)
            r.putString("Origin", "ANOMALY");
            r.putString("Kingdom", GrimoirePages.coverOf(held.get()).kingdom.name());
            r.putBoolean("Detected", true);
            r.putBoolean("Awakened", true);
            s.setDirty();
            return;
        }
        RandomSource rand = p.getRandom();
        int pity = MultiverseConfig.get(MultiverseConfig.ANOMALY_PITY);
        boolean anomaly = rand.nextDouble() * 100.0 < MultiverseConfig.get(MultiverseConfig.ANOMALY_CHANCE)
                || (pity > 0 && s.sinceAnomaly >= pity);
        if (anomaly) {
            r.putString("Origin", "ANOMALY");
            r.putString("Kingdom", rollKingdom(rand).name());
            s.sinceAnomaly = 0;
        } else {
            r.putString("Origin", "TENSURA");
            s.sinceAnomaly++;
        }
        r.putLong("RolledAt", System.currentTimeMillis());
        s.setDirty();
        MultiverseSync.markDirty(p);
    }

    /** 50 Clover / 20 Diamond / 20 Heart / 10 Spade by default. */
    public static Kingdom rollKingdom(RandomSource r) {
        Kingdom[] k = {Kingdom.CLOVER, Kingdom.DIAMOND, Kingdom.HEART, Kingdom.SPADE};
        int[] w = {MultiverseConfig.get(MultiverseConfig.KINGDOM_CLOVER), MultiverseConfig.get(MultiverseConfig.KINGDOM_DIAMOND),
                MultiverseConfig.get(MultiverseConfig.KINGDOM_HEART), MultiverseConfig.get(MultiverseConfig.KINGDOM_SPADE)};
        int total = Math.max(1, w[0] + w[1] + w[2] + w[3]), x = r.nextInt(total);
        for (int i = 0; i < 4; i++) { if (x < w[i]) return k[i]; x -= w[i]; }
        return Kingdom.CLOVER;
    }

    /** Admin: set a player's origin (kingdom null = Tensura). */
    public static void setOrigin(ServerPlayer p, Kingdom kingdom) {
        State s = State.get(p.getServer());
        CompoundTag r = s.record(p.getUUID());
        r.putString("Origin", kingdom == null ? "TENSURA" : "ANOMALY");
        if (kingdom != null) r.putString("Kingdom", kingdom.name()); else r.remove("Kingdom");
        s.setDirty();
        MultiverseSync.markDirty(p);
    }

    /** Admin: forget the roll, so the player rolls again (now). */
    public static void reroll(ServerPlayer p) {
        State s = State.get(p.getServer());
        s.players.remove(p.getUUID());
        s.setDirty();
        roll(p);
    }

    // ---------------------------------------------------------------- stages
    public static void setStage(MinecraftServer server, Stage stage, boolean announce) {
        State s = State.get(server);
        if (s.stage == stage) return;
        s.stage = stage;
        s.stageSince = System.currentTimeMillis();
        s.setDirty();
        cached = stage;
        if (announce && MultiverseConfig.get(MultiverseConfig.CONVERGENCE_ANNOUNCE)) announceStage(server, stage);
        for (ServerPlayer p : server.getPlayerList().getPlayers()) MultiverseSync.markDirty(p);
    }

    private static void announceStage(MinecraftServer server, Stage stage) {
        List<Component> lines = switch (stage) {
            case SIGNS -> List.of(Component.literal("The air feels wrong tonight.").withStyle(ChatFormatting.DARK_GRAY, ChatFormatting.ITALIC));
            case FIRST_GRIMOIRE -> List.of(warn("The world has experienced an anomalous magical disturbance."));
            case CLOVER -> List.of(warn("A stable rift has opened. A settlement nobody has ever mapped stands where there was nothing."),
                    quote("\"...You mean this isn't the Clover Kingdom?\""));
            case DIAMOND -> List.of(warn("A second rupture tears the sky. Crystal-armoured researchers walk out of it."),
                    quote("\"What in the world is this energy? ...Magicules? Fascinating. Take samples.\""));
            case HEART -> List.of(warn("The bleed is changing the land. Forests shift, mana thickens, and spirits wake in the trees."),
                    quote("The Heart Kingdom is manifesting."));
            case SPADE -> List.of(warn("The final rupture opens. Devils walk through it first. Then soldiers. Then frost."),
                    Component.literal("❄ THE SPADE KINGDOM HAS ARRIVED. ❄").withStyle(ChatFormatting.AQUA, ChatFormatting.BOLD),
                    quote("This isn't an invasion. Their entire universe is bleeding into ours."));
        };
        for (Component c : lines) server.getPlayerList().broadcastSystemMessage(c, false);
        for (ServerPlayer p : server.getPlayerList().getPlayers())
            p.serverLevel().playSound(null, p.blockPosition(), stage == Stage.SPADE ? SoundEvents.WITHER_SPAWN : SoundEvents.ELDER_GUARDIAN_CURSE,
                    SoundSource.AMBIENT, 0.6f, 0.8f);
    }

    private static Component warn(String s) {
        return Component.literal("⚠ ").withStyle(ChatFormatting.GOLD).append(Component.literal(s).withStyle(ChatFormatting.YELLOW));
    }

    private static Component quote(String s) { return Component.literal(s).withStyle(ChatFormatting.GRAY, ChatFormatting.ITALIC); }

    /** The anonymous line everyone sees when an anomaly is chosen by a grimoire (instead of naming them). */
    public static void announceGrimoire(MinecraftServer server) {
        if (MultiverseConfig.get(MultiverseConfig.CONVERGENCE_ANNOUNCE))
            server.getPlayerList().broadcastSystemMessage(warn("The world has experienced an anomalous magical disturbance."), false);
    }

    // ---------------------------------------------------------------- events
    public static void onServerStarted(ServerStartedEvent e) { cached = State.get(e.getServer()).stage; }

    public static void onLogin(PlayerEvent.PlayerLoggedInEvent e) {
        if (enabled() && e.getEntity() instanceof ServerPlayer p) roll(p);
    }

    /** Every second: anomaly awakening, and the first-grimoire stage. */
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer p) || p.tickCount % 20 != 0 || !enabled()) return;
        State s = State.get(p.getServer());
        CompoundTag r = s.players.get(p.getUUID());
        if (r == null) { roll(p); return; }
        if (!"ANOMALY".equals(r.getString("Origin"))) return;
        boolean has = GrimoirePages.grimoireOf(p).isPresent();
        if (has && !r.getBoolean("Awakened")) { r.putBoolean("Awakened", true); r.putBoolean("Detected", true); s.setDirty(); }
        if (has && s.stage == Stage.SIGNS) setStage(p.getServer(), Stage.FIRST_GRIMOIRE, false);   // the grant itself announced it
        if (r.getBoolean("Awakened")) return;
        int play = r.getInt("Play") + 20;
        r.putInt("Play", play);
        s.setDirty();
        int minute = 60 * 20;
        if (!r.getBoolean("Detected") && play >= MultiverseConfig.get(MultiverseConfig.DETECT_MINUTES) * minute) {
            r.putBoolean("Detected", true);
            title(p, Component.literal("[Unknown Magic Detected]").withStyle(ChatFormatting.DARK_PURPLE),
                    Component.literal("Something inside you answers to a magic this world does not have.").withStyle(ChatFormatting.GRAY));
            p.sendSystemMessage(Component.literal("[Unknown Magic Detected]").withStyle(ChatFormatting.DARK_PURPLE, ChatFormatting.BOLD)
                    .append(Component.literal(" xxxxxxxx").withStyle(ChatFormatting.DARK_PURPLE, ChatFormatting.OBFUSCATED)));
            p.serverLevel().playSound(null, p.blockPosition(), SoundEvents.AMETHYST_BLOCK_RESONATE, SoundSource.PLAYERS, 1f, 0.6f);
            MultiverseSync.markDirty(p);
        } else if (r.getBoolean("Detected") && play >= MultiverseConfig.get(MultiverseConfig.AWAKEN_MINUTES) * minute) {
            awaken(p);
        }
    }

    /** The world gives an anomaly their grimoire (or an admin forces it). */
    public static boolean awaken(ServerPlayer p) {
        if (GrimoirePages.grimoireOf(p).isPresent()) return false;
        title(p, Component.literal("You have been chosen").withStyle(ChatFormatting.GOLD),
                Component.literal("by another world's magic.").withStyle(ChatFormatting.YELLOW));
        boolean ok = Ceremony.choose(p, "another world");
        State s = State.get(p.getServer());
        CompoundTag r = s.record(p.getUUID());
        r.putBoolean("Detected", true);
        r.putBoolean("Awakened", ok);
        s.setDirty();
        return ok;
    }

    private static void title(ServerPlayer p, Component title, Component sub) {
        p.connection.send(new ClientboundSetTitlesAnimationPacket(10, 70, 20));
        p.connection.send(new ClientboundSetSubtitleTextPacket(sub));
        p.connection.send(new ClientboundSetTitleTextPacket(title));
    }

    /** Every minute: auto-advance the stages, and the rumours of the first signs. */
    public static void onServerTick(ServerTickEvent.Post e) {
        MinecraftServer server = e.getServer();
        if (server.getTickCount() % 1200 != 0 || !enabled()) return;
        State s = State.get(server);
        if (MultiverseConfig.get(MultiverseConfig.CONVERGENCE_AUTO_ADVANCE) && s.stage.atLeast(Stage.FIRST_GRIMOIRE) && s.stage != Stage.SPADE) {
            List<? extends Integer> days = MultiverseConfig.get(MultiverseConfig.STAGE_DAYS);
            int i = s.stage.ordinal() - 1;                                  // FIRST_GRIMOIRE -> index 0
            int need = i < days.size() ? days.get(i) : 7;
            if (System.currentTimeMillis() - s.stageSince >= need * 86_400_000L) setStage(server, Stage.values()[s.stage.ordinal() + 1], true);
        }
        if (MultiverseConfig.get(MultiverseConfig.CONVERGENCE_RUMOURS)) {
            int every = Math.max(1, MultiverseConfig.get(MultiverseConfig.RUMOUR_MINUTES));
            for (ServerPlayer p : server.getPlayerList().getPlayers())
                if (p.getRandom().nextInt(every) == 0) p.sendSystemMessage(rumour(s.stage, p.getRandom()));
        }
    }

    private static final String[][] RUMOURS = {
            {"Someone nearby mutters about a \"Clover Kingdom\". Nobody has ever heard of it.",
                    "You glimpse a symbol carved into old stone: a leaf with three... no, four points?",
                    "A traveller swears a book flew past him last night. On its own.",
                    "The air hums with an energy that isn't quite magicules.",
                    "A merchant asks if you've seen a \"Magic Knight\". You don't know the word."},
            {"They say a human was seen with a book floating at their side.",
                    "\"Mana.\" A word you've never heard spoken aloud, until today.",
                    "Someone's skill analysis came back with a line it couldn't read."},
            {"A lost knight in a black robe asks you the way to the Royal Capital.",
                    "Kids are playing \"Wizard King\" in the street. Nobody taught them that game.",
                    "\"...You mean this isn't the Clover Kingdom?\""},
            {"Crystal-armoured strangers are taking soil samples. \"What IS this energy?\"",
                    "A Diamond researcher offers good money for anyone with a Unique Skill. Volunteers have not come back."},
            {"The forest breathes. The mana is so thick you can taste it.",
                    "A spirit guardian watches you from between the trees, then is gone."},
            {"Frost is spreading where no winter should be.",
                    "Something whispers from beneath the world. It knows your name.",
                    "Spade soldiers have been seen at the edge of the map. They are not lost."}};

    private static Component rumour(Stage stage, RandomSource r) {
        int tier = r.nextInt(stage.ordinal() + 1);                          // any rumour up to the current stage
        String[] pool = RUMOURS[Math.min(tier, RUMOURS.length - 1)];
        return Component.literal(pool[r.nextInt(pool.length)]).withStyle(ChatFormatting.DARK_GRAY, ChatFormatting.ITALIC);
    }

    // ---------------------------------------------------------------- status (for /multiverse convergence status and the panel)
    public static String describe(ServerPlayer p) {
        Origin o = origin(p);
        if (o != Origin.ANOMALY) return o == Origin.TENSURA ? "Tensura-origin" : "Not rolled";
        Kingdom k = kingdomOf(p);
        return "Anomaly (" + (k == null ? "?" : k.displayName) + ")";
    }

    public static int anomalies(MinecraftServer server) {
        int n = 0;
        for (CompoundTag t : State.get(server).players.values()) if ("ANOMALY".equals(t.getString("Origin"))) n++;
        return n;
    }

    public static int rolled(MinecraftServer server) { return State.get(server).players.size(); }

    public static long stageSince(MinecraftServer server) { return State.get(server).stageSince; }
}
