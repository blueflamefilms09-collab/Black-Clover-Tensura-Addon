package com.newuniverse.nusmp.multiverse;

import com.newuniverse.nusmp.balance.BalanceLaw;
import com.newuniverse.nusmp.blackclover.GrimoirePages;
import com.newuniverse.nusmp.blackclover.Kingdom;
import com.newuniverse.nusmp.book.GrimoireBook;
import com.newuniverse.nusmp.book.GrimoireSummon;
import com.newuniverse.nusmp.book.SpiritBond;
import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.BiomeTags;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.monster.Enemy;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

import java.util.ArrayList;
import java.util.List;

/**
 * Secret side quests for anomaly players (0.39). Nobody else ever sees them.
 *
 * <p>Every quest is tracked here, so they work on any server: a new quest is whispered in chat when it opens ("✦ Secret quest"),
 * completion pays its reward, and the open / done list is in the Multiverse status panel (the Tensura menu's custom area). Each
 * quest also has a hidden advancement {@code nusmp:convergence/<id>}; with FTB Quests installed the mod writes a hidden
 * "The Convergence" chapter whose quests are completed by those advancements, so the quest book shows them only to anomalies
 * (see {@link FtbQuestsChapter}).
 */
public final class SecretQuests {
    /** Append only: the id is stored and used for the advancement. */
    public enum Quest {
        UNKNOWN_MAGIC("unknown_magic", "Unknown Magic Detected", "Something inside you answers to a magic this world does not have.",
                Convergence.Stage.SIGNS, null, 0, 0),
        CHOSEN("chosen", "Chosen by Another World", "Receive your grimoire. The world will find you, or a Grimoire Tower will.",
                Convergence.Stage.SIGNS, null, 1, 0),
        FIRST_SPELL("first_spell", "Words of Another World", "Summon your grimoire and cast your first spell.",
                Convergence.Stage.SIGNS, null, 1, 5),
        RUINS("ruins", "Ruins That Don't Belong", "Find library ruins that match no history of this world (/multiverse locate ruins).",
                Convergence.Stage.SIGNS, null, 1, 0),
        PRACTICE("practice", "A Mage Must Practise", "Cast 100 spells.", Convergence.Stage.FIRST_GRIMOIRE, null, 2, 20),
        SURVIVOR("survivor", "Survive the Other World", "Defeat 50 monsters while your grimoire floats at your side.",
                Convergence.Stage.FIRST_GRIMOIRE, null, 2, 10),
        TOWER("tower", "A Tower Without a Kingdom", "Stand inside a Grimoire Tower.", Convergence.Stage.CLOVER, null, 1, 0),
        BOSS("boss", "Proof of Strength", "Defeat a boss of this world.", Convergence.Stage.CLOVER, null, 3, 15),
        MAGICULES("magicules", "What Is This Energy?", "Reach 20,000 maximum magicules. The Diamond researchers would kill to study you.",
                Convergence.Stage.DIAMOND, null, 2, 10),
        SPIRIT("spirit", "The Forest Breathes", "Form a bond with a spirit.", Convergence.Stage.HEART, null, 2, 10),
        NIGHT("night", "Whispers from Below", "Defeat 30 monsters at night after the Spade Kingdom arrives.",
                Convergence.Stage.SPADE, null, 3, 15),
        HOME_CLOVER("home_clover", "A Knight Without a Squad", "Earn 3 gold stars, as a Magic Knight of the Clover Kingdom would.",
                Convergence.Stage.FIRST_GRIMOIRE, Kingdom.CLOVER, 2, 10),
        HOME_DIAMOND("home_diamond", "Study the Anomaly", "Reach 25% mastery of your grimoire: you are your own best experiment.",
                Convergence.Stage.FIRST_GRIMOIRE, Kingdom.DIAMOND, 2, 0),
        HOME_HEART("home_heart", "Mana in the Leaves", "Cast 20 spells in a forest.", Convergence.Stage.FIRST_GRIMOIRE, Kingdom.HEART, 2, 10),
        HOME_SPADE("home_spade", "Cold Ambition", "Defeat 25 monsters at night.", Convergence.Stage.FIRST_GRIMOIRE, Kingdom.SPADE, 2, 10);

        public final String id, title, hint;
        public final Convergence.Stage stage;
        public final Kingdom kingdom;
        public final int goldStars, mastery;

        Quest(String id, String title, String hint, Convergence.Stage stage, Kingdom kingdom, int goldStars, int mastery) {
            this.id = id; this.title = title; this.hint = hint; this.stage = stage; this.kingdom = kingdom;
            this.goldStars = goldStars; this.mastery = mastery;
        }

        public ResourceLocation advancement() { return ResourceLocation.fromNamespaceAndPath("nusmp", "convergence/" + id); }
    }

    private static final String KEY = "nusmp_secret_quests";

    private SecretQuests() {}

    public static boolean enabled() { return Convergence.enabled() && MultiverseConfig.get(MultiverseConfig.QUESTS_ENABLED); }

    // ---------------------------------------------------------------- state (player NBT, survives death)
    private static CompoundTag data(Player p) {
        CompoundTag root = p.getPersistentData();
        if (!root.contains(Player.PERSISTED_NBT_TAG)) root.put(Player.PERSISTED_NBT_TAG, new CompoundTag());
        return root.getCompound(Player.PERSISTED_NBT_TAG).getCompound(KEY);
    }

    private static void save(Player p, CompoundTag t) {
        CompoundTag root = p.getPersistentData().getCompound(Player.PERSISTED_NBT_TAG);
        root.put(KEY, t);
        p.getPersistentData().put(Player.PERSISTED_NBT_TAG, root);
    }

    private static boolean in(CompoundTag t, String list, Quest q) {
        for (Tag s : t.getList(list, Tag.TAG_STRING)) if (s.getAsString().equals(q.id)) return true;
        return false;
    }

    private static void add(CompoundTag t, String list, Quest q) {
        ListTag l = t.getList(list, Tag.TAG_STRING);
        l.add(StringTag.valueOf(q.id));
        t.put(list, l);
    }

    public static boolean done(ServerPlayer p, Quest q) { return in(data(p), "Done", q); }

    /** Open to this player now: an anomaly, the stage has come, their kingdom (if the quest has one), and Unknown Magic first. */
    public static boolean open(ServerPlayer p, Quest q) {
        if (!enabled() || !Convergence.isAnomaly(p)) return false;
        if (!Convergence.stage(p.getServer()).atLeast(q.stage)) return false;
        if (q.kingdom != null && q.kingdom != Convergence.kingdomOf(p)) return false;
        return q == Quest.UNKNOWN_MAGIC || done(p, Quest.UNKNOWN_MAGIC);
    }

    // ---------------------------------------------------------------- progress
    private static void count(ServerPlayer p, String key, int by) {
        CompoundTag t = data(p);
        t.putInt(key, t.getInt(key) + by);
        save(p, t);
    }

    private static int counter(ServerPlayer p, String key) { return data(p).getInt(key); }

    /** Called by GrimoireBook after a spell is cast. */
    public static void onCast(ServerPlayer p) {
        if (!enabled() || !Convergence.isAnomaly(p)) return;
        count(p, "Casts", 1);
        if (p.level().getBiome(p.blockPosition()).is(BiomeTags.IS_FOREST)) count(p, "ForestCasts", 1);
        check(p);
    }

    public static void onKill(LivingDeathEvent e) {
        if (!(e.getSource().getEntity() instanceof ServerPlayer p) || !enabled() || !Convergence.isAnomaly(p)) return;
        var dead = e.getEntity();
        if (BalanceLaw.isBoss(dead)) count(p, "Bosses", 1);
        if (dead instanceof Enemy) {
            if (GrimoireSummon.isFloating(p)) count(p, "SummonKills", 1);
            if (p.level().isNight()) count(p, "NightKills", 1);
            if (p.level().isNight() && Convergence.stage(p.getServer()).atLeast(Convergence.Stage.SPADE)) count(p, "SpadeNightKills", 1);
        }
        check(p);
    }

    /** Every 2 s: location and stat quests, newly opened quests, and the hidden stage / kingdom markers. */
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer p) || p.tickCount % 40 != 13 || !enabled() || !Convergence.isAnomaly(p)) return;
        check(p);
    }

    private static boolean met(ServerPlayer p, Quest q) {
        return switch (q) {
            case UNKNOWN_MAGIC -> Convergence.detected(p);
            case CHOSEN -> GrimoirePages.grimoireOf(p).isPresent();
            case FIRST_SPELL -> counter(p, "Casts") >= 1;
            case PRACTICE -> counter(p, "Casts") >= 100;
            case RUINS -> near(p, WorldSites.Kind.RUINS, 20);
            case TOWER -> near(p, WorldSites.Kind.TOWER, 10);
            case SURVIVOR -> counter(p, "SummonKills") >= 50;
            case BOSS -> counter(p, "Bosses") >= 1;
            case MAGICULES -> io.github.manasmods.tensura.util.EnergyHelper.getMaxMagicule(p) >= 20000;
            case SPIRIT -> SpiritBond.bonded(p);
            case NIGHT -> counter(p, "SpadeNightKills") >= 30;
            case HOME_CLOVER -> MultiverseProfile.goldStars(p) >= 3;
            case HOME_DIAMOND -> GrimoirePages.grimoireOf(p).map(i -> i.getSkill() instanceof GrimoireBook b && b.masteryFrac(i) >= 0.25).orElse(false);
            case HOME_HEART -> counter(p, "ForestCasts") >= 20;
            case HOME_SPADE -> counter(p, "NightKills") >= 25;
        };
    }

    private static boolean near(ServerPlayer p, WorldSites.Kind kind, int radius) {
        var site = WorldSites.get(p.getServer()).nearest(kind, p.level().dimension(), p.blockPosition());
        return site.isPresent() && site.get().pos().distSqr(p.blockPosition()) <= (double) radius * radius;
    }

    /** Opens, completes and rewards whatever has changed. */
    public static void check(ServerPlayer p) {
        CompoundTag t = data(p);
        boolean changed = false;
        for (Quest q : Quest.values()) {
            if (in(t, "Done", q) || !open(p, q)) continue;
            if (!in(t, "Opened", q)) {
                add(t, "Opened", q);
                save(p, t);
                changed = true;
                if (q != Quest.UNKNOWN_MAGIC)
                    p.sendSystemMessage(Component.literal("✦ Secret quest: ").withStyle(ChatFormatting.DARK_PURPLE)
                            .append(Component.literal(q.title).withStyle(ChatFormatting.LIGHT_PURPLE, ChatFormatting.BOLD))
                            .append(Component.literal(" - " + q.hint).withStyle(ChatFormatting.GRAY, ChatFormatting.ITALIC)));
            }
            if (met(p, q)) {
                t = data(p);
                add(t, "Done", q);
                save(p, t);
                complete(p, q);
                changed = true;
            }
            t = data(p);
        }
        // hidden markers for the FTB Quests chapter: the stages reached and the anomaly's kingdom
        Convergence.Stage now = Convergence.stage(p.getServer());
        for (Convergence.Stage s : Convergence.Stage.values()) if (now.atLeast(s)) award(p, "convergence/stage_" + s.name().toLowerCase());
        Kingdom k = Convergence.kingdomOf(p);
        if (k != null) award(p, "convergence/kingdom_" + k.name().toLowerCase());
        if (changed) MultiverseSync.markDirty(p);
    }

    private static void complete(ServerPlayer p, Quest q) {
        award(p, "convergence/" + q.id);
        if (q.goldStars > 0) MultiverseProfile.addStars(p, q.goldStars);
        if (q.mastery > 0) GrimoirePages.grimoireOf(p).ifPresent(i -> { i.getSkill().addMasteryPoint(i, p, q.mastery); i.markDirty(); });
        p.sendSystemMessage(Component.literal("✔ Secret quest complete: ").withStyle(ChatFormatting.DARK_PURPLE)
                .append(Component.literal(q.title).withStyle(ChatFormatting.LIGHT_PURPLE, ChatFormatting.BOLD))
                .append(Component.literal(reward(q)).withStyle(ChatFormatting.GOLD)));
        p.serverLevel().playSound(null, p.blockPosition(), SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.PLAYERS, 1f, 1.2f);
    }

    private static String reward(Quest q) {
        List<String> r = new ArrayList<>();
        if (q.goldStars > 0) r.add("+" + q.goldStars + " gold star" + (q.goldStars > 1 ? "s" : ""));
        if (q.mastery > 0) r.add("+" + q.mastery + " grimoire mastery");
        return r.isEmpty() ? "" : "  (" + String.join(", ", r) + ")";
    }

    private static void award(ServerPlayer p, String path) {
        var holder = p.getServer().getAdvancements().get(ResourceLocation.fromNamespaceAndPath("nusmp", path));
        if (holder != null && !p.getAdvancements().getOrStartProgress(holder).isDone()) p.getAdvancements().award(holder, "done");
    }

    // ---------------------------------------------------------------- admin and panel
    public static void forceComplete(ServerPlayer p, Quest q) {
        CompoundTag t = data(p);
        if (in(t, "Done", q)) return;
        add(t, "Done", q);
        save(p, t);
        complete(p, q);
        MultiverseSync.markDirty(p);
    }

    public static void reset(ServerPlayer p) {
        save(p, new CompoundTag());
        MultiverseSync.markDirty(p);
    }

    /** For the status panel: "Open" and "Done" quest titles (the hints are in chat and /multiverse quests). */
    public static CompoundTag summary(ServerPlayer p) {
        CompoundTag out = new CompoundTag();
        if (!enabled() || !Convergence.isAnomaly(p)) return out;
        ListTag open = new ListTag(), done = new ListTag();
        for (Quest q : Quest.values()) {
            if (done(p, q)) done.add(StringTag.valueOf(q.title));
            else if (open(p, q)) open.add(StringTag.valueOf("✦ " + q.title));
        }
        out.put("Open", open);
        out.put("Done", done);
        return out;
    }

    public static Quest byId(String id) {
        for (Quest q : Quest.values()) if (q.id.equalsIgnoreCase(id) || q.name().equalsIgnoreCase(id)) return q;
        return null;
    }
}
