package com.newuniverse.nusmp.book;

import com.newuniverse.nusmp.NUConfig;
import com.newuniverse.nusmp.blackclover.GrimoireCover;
import com.newuniverse.nusmp.blackclover.GrimoireItem;
import com.newuniverse.nusmp.blackclover.GrimoirePages;
import com.newuniverse.nusmp.blackclover.Kingdom;
import com.newuniverse.nusmp.blackclover.MagicType;
import com.newuniverse.nusmp.skill.NUSkills;
import io.github.manasmods.manascore.skill.api.ManasSkillInstance;
import io.github.manasmods.manascore.skill.api.SkillAPI;
import net.minecraft.ChatFormatting;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundSetSubtitleTextPacket;
import net.minecraft.network.protocol.game.ClientboundSetTitleTextPacket;
import net.minecraft.network.protocol.game.ClientboundSetTitlesAnimationPacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

import java.util.ArrayDeque;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/** Devils, devil contracts, prices, whispers and the position history forbidden pages need. */
public final class ForbiddenMagic {
    private ForbiddenMagic() {}

    // ---------------------------------------------------------------- devils (original names)
    public enum PactDevil {
        IGNIVAR("Ignivar", "the fire devil", 0xFFFF3A10),
        VERBAEL("Verbael", "the word devil", 0xFFB06AFF),
        GRAVEMAW("Gravemaw", "the gravity-tainted devil", 0xFF6A2AA0),
        KAIROVORE("Kairovore", "the time-eater", 0xFFF5D76E);

        public final String displayName, title;
        public final int color;
        PactDevil(String n, String t, int c) { displayName = n; title = t; color = c; }

        public static PactDevil forMagic(MagicType m) {
            return switch (m) {
                case FLAME, EXPLOSION, MAGMA -> IGNIVAR;
                case TIME -> KAIROVORE;
                case GRAVITY, SPATIAL, EARTH, SAND -> GRAVEMAW;
                default -> VERBAEL;
            };
        }
        public static PactDevil byName(String s) { try { return valueOf(s); } catch (Exception e) { return null; } }
    }

    /** SavedData: one devil per player, server-wide. */
    public static final class Pacts extends SavedData {
        final Map<UUID, String> pacts = new HashMap<>();
        public static Pacts get(MinecraftServer s) {
            return s.overworld().getDataStorage().computeIfAbsent(new SavedData.Factory<>(Pacts::new, Pacts::load, null), "nusmp_devil_pacts");
        }
        static Pacts load(CompoundTag t, HolderLookup.Provider p) { Pacts x = new Pacts(); for (String k : t.getAllKeys()) x.pacts.put(UUID.fromString(k), t.getString(k)); return x; }
        @Override public CompoundTag save(CompoundTag t, HolderLookup.Provider p) { pacts.forEach((k, v) -> t.putString(k.toString(), v)); return t; }
    }

    public static PactDevil devilOf(ServerPlayer p) { return PactDevil.byName(Pacts.get(p.getServer()).pacts.getOrDefault(p.getUUID(), "")); }

    // ---------------------------------------------------------------- persisted player data
    public static CompoundTag data(Player p) {
        CompoundTag root = p.getPersistentData();
        if (!root.contains(Player.PERSISTED_NBT_TAG)) root.put(Player.PERSISTED_NBT_TAG, new CompoundTag());
        return root.getCompound(Player.PERSISTED_NBT_TAG);
    }
    private static void save(Player p, CompoundTag t) { p.getPersistentData().put(Player.PERSISTED_NBT_TAG, t); }

    public static double manaTax(Player p) { return data(p).getDouble("nusmp_mana_tax"); }

    // ---------------------------------------------------------------- contract
    /** Price: "maxhp" (-2 hearts permanently), "seal" (a random page sealed for good), or "tax" (+15% cost on all grimoire magic). */
    public static boolean bind(ServerPlayer p, String price) {
        var g = GrimoirePages.grimoireOf(p);
        if (g.isEmpty()) { GrimoireBook.fail(p, "You need a grimoire to bargain with."); return false; }
        if (devilOf(p) != null) { GrimoireBook.fail(p, "A devil already lives in your grimoire."); return false; }
        ManasSkillInstance inst = g.get();
        CompoundTag d = data(p);
        switch (price) {
            case "maxhp" -> {
                int n = d.getInt("nusmp_devil_hp") + 1;
                d.putInt("nusmp_devil_hp", n);
                applyMaxHpPrice(p, n * NUConfig.FORBIDDEN_MAXHP_PRICE.get());
            }
            case "seal" -> {
                if (inst.getSkill() instanceof GrimoireBook b) {
                    for (int m = b.familyCount() - 1; m >= 1; m--) {
                        if (GrimoireBook.isUnlocked(inst, m)) {
                            inst.getOrCreateTag().putInt("Unlocked", inst.getOrCreateTag().getInt("Unlocked") & ~(1 << m));
                            p.displayClientMessage(Component.literal("The devil seals " + b.page(m).name() + " forever.").withStyle(ChatFormatting.DARK_RED), false);
                            break;
                        }
                    }
                }
            }
            default -> d.putDouble("nusmp_mana_tax", d.getDouble("nusmp_mana_tax") + NUConfig.FORBIDDEN_MANA_TAX.get());
        }
        save(p, d);
        MagicType magic = GrimoirePages.magicOf(inst);
        PactDevil devil = PactDevil.forMagic(magic);
        Pacts pacts = Pacts.get(p.getServer());
        pacts.pacts.put(p.getUUID(), devil.name());
        pacts.setDirty();
        // The cover darkens: Spade -> triple spade, everyone else -> five-leaf.
        GrimoireCover dark = GrimoirePages.coverOf(inst).kingdom == Kingdom.SPADE ? GrimoireCover.TRIPLE_SPADE : GrimoireCover.FIVE_LEAF;
        inst.getOrCreateTag().putString("Cover", dark.name());
        inst.getOrCreateTag().putInt("Leaves", 5);
        inst.markDirty();
        var invent = p.getInventory();
        com.newuniverse.nusmp.blackclover.GrimoireSlot.replaceOwned(p, old -> GrimoireItem.create(p, dark, magic, null));
        for (int s = 0; s < invent.getContainerSize(); s++) {
            if (GrimoireItem.isOwnedBy(invent.getItem(s), p.getUUID())) invent.setItem(s, GrimoireItem.create(p, dark, magic, null));
        }
        // The Forbidden grimoire skill: Devil Dive and Five-Leaf Whisper are written at once.
        ManasSkillInstance fb = NUSkills.BOOK_FORBIDDEN.get().createDefaultInstance();
        GrimoireBook.unlock(fb, 1);
        SkillAPI.getSkillsFrom(p).learnSkill(fb, Component.literal("Forbidden pages bleed into your grimoire.").withStyle(ChatFormatting.DARK_RED));
        subtitle(p, Component.literal(p.getName().getString() + " has bound " + devil.displayName + ", " + devil.title + "."));
        return true;
    }

    private static final ResourceLocation MAXHP_ID = ResourceLocation.fromNamespaceAndPath("nusmp", "devil_price_hp");
    private static final ResourceLocation REWIND_ID = ResourceLocation.fromNamespaceAndPath("nusmp", "rewind_price_hp");

    static void applyMaxHpPrice(ServerPlayer p, double hp) {
        var a = p.getAttribute(Attributes.MAX_HEALTH);
        if (a == null) return;
        a.removeModifier(MAXHP_ID);
        a.addPermanentModifier(new AttributeModifier(MAXHP_ID, -hp, AttributeModifier.Operation.ADD_VALUE));
    }

    /** Time Reversal on the Living: a small permanent max-HP sliver per use. */
    static void payRewindSliver(ServerPlayer p) {
        CompoundTag d = data(p);
        int n = d.getInt("nusmp_rewind_hp") + 1;
        d.putInt("nusmp_rewind_hp", n);
        save(p, d);
        var a = p.getAttribute(Attributes.MAX_HEALTH);
        if (a == null) return;
        a.removeModifier(REWIND_ID);
        a.addPermanentModifier(new AttributeModifier(REWIND_ID, -n, AttributeModifier.Operation.ADD_VALUE));
    }

    // ---------------------------------------------------------------- price per cast (stacks, decays)
    /** Pays the HP price of a forbidden cast. Returns false (and pays nothing) if it would kill you. */
    public static boolean payPrice(ServerPlayer p) {
        CompoundTag d = data(p);
        long now = p.level().getGameTime();
        int debt = Math.max(0, d.getInt("nusmp_devil_debt") - (int) ((now - d.getLong("nusmp_devil_debt_at")) / 6000));
        float price = (float) (NUConfig.FORBIDDEN_HP_PRICE.get() * (1 + 0.25 * debt) * com.newuniverse.nusmp.item.MagicGear.selfDamageMult(p));
        if (p.getHealth() <= price + 1) { GrimoireBook.fail(p, "The price would kill you."); return false; }
        p.hurt(p.damageSources().magic(), price);
        d.putInt("nusmp_devil_debt", debt + 1);
        d.putLong("nusmp_devil_debt_at", now);
        save(p, d);
        p.getPersistentData().putLong("nusmp_devil_paid", now);
        return true;
    }

    /** Forbidden spells always announce in a dark subtitle. */
    public static void subtitle(ServerPlayer p, Component text) {
        for (ServerPlayer o : p.serverLevel().players()) {
            if (o.distanceToSqr(p) > 48 * 48) continue;
            o.connection.send(new ClientboundSetTitlesAnimationPacket(5, 50, 15));
            o.connection.send(new ClientboundSetTitleTextPacket(Component.empty()));
            o.connection.send(new ClientboundSetSubtitleTextPacket(text.copy().withStyle(ChatFormatting.DARK_RED, ChatFormatting.ITALIC)));
        }
    }

    // ---------------------------------------------------------------- whispers on kill streaks
    private static final String[][] WHISPERS = {
            {"More. Burn them all.", "Can you smell it? Ash. Lovely ash.", "Your fire is so small. Let me in."},
            {"Say the word and they kneel.", "Words are chains. You hold the key.", "Speak, and the world obeys."},
            {"Heavier. Make them heavier.", "Everything falls, in the end.", "Let them sink."},
            {"Their seconds taste so sweet.", "Give me their time. All of it.", "Tick. Tock. Tick."}};

    public static void onKill(LivingDeathEvent e) {
        if (!(e.getSource().getEntity() instanceof ServerPlayer p)) return;
        PactDevil devil = devilOf(p);
        if (devil == null) return;
        var t = p.getPersistentData();
        long now = p.level().getGameTime();
        int streak = now - t.getLong("nusmp_streak_at") < 300 ? t.getInt("nusmp_streak") + 1 : 1;
        t.putInt("nusmp_streak", streak);
        t.putLong("nusmp_streak_at", now);
        if (streak == 3 || streak == 5 || streak % 10 == 0) {
            String[] lines = WHISPERS[devil.ordinal()];
            p.displayClientMessage(Component.literal(devil.displayName + " whispers: " + lines[p.getRandom().nextInt(lines.length)])
                    .withStyle(ChatFormatting.DARK_PURPLE, ChatFormatting.ITALIC), false);
        }
    }

    // ---------------------------------------------------------------- position history (4 s rewinds)
    private static final Map<UUID, ArrayDeque<Vec3>> HISTORY = new HashMap<>();

    public static void onServerTick(ServerTickEvent.Post e) {
        if (e.getServer().getTickCount() % 20 != 0) return;
        for (ServerPlayer p : e.getServer().getPlayerList().getPlayers()) {
            ArrayDeque<Vec3> q = HISTORY.computeIfAbsent(p.getUUID(), k -> new ArrayDeque<>());
            q.addLast(p.position());
            while (q.size() > 5) q.removeFirst();
        }
    }

    /** Where this player stood ~4 seconds ago. */
    public static Vec3 fourSecondsAgo(ServerPlayer p) {
        ArrayDeque<Vec3> q = HISTORY.get(p.getUUID());
        return q == null || q.isEmpty() ? p.position() : q.peekFirst();
    }

    public static boolean consents(ServerPlayer target) {
        return target.getMainHandItem().is(com.newuniverse.nusmp.item.NUItems.WRITTEN_CONSENT.get())
                || target.getOffhandItem().is(com.newuniverse.nusmp.item.NUItems.WRITTEN_CONSENT.get());
    }

    static List<ServerPlayer> playersNear(ServerPlayer p, double r) {
        return p.serverLevel().getEntitiesOfClass(ServerPlayer.class, p.getBoundingBox().inflate(r), o -> o != p);
    }
}
