package com.newuniverse.nusmp.mana;

import com.newuniverse.nusmp.antimagic.AntiMagic;
import com.newuniverse.nusmp.balance.BalanceLaw;
import com.newuniverse.nusmp.book.GrimoireBook;
import com.newuniverse.nusmp.multiverse.MultiverseConfig;
import com.newuniverse.nusmp.multiverse.MultiverseProfile;
import com.newuniverse.nusmp.vfx.VfxShape;
import com.newuniverse.nusmp.vfx.VfxSpawn;
import io.github.manasmods.tensura.storage.TensuraStorages;
import io.github.manasmods.tensura.util.EnergyHelper;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.BiomeTags;
import net.minecraft.util.Mth;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.Biomes;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

/**
 * 1.5.0: the mana system of Black Clover as "The Weeb Commander" lays it out (see docs/mana_system.md), built so that no magic is a
 * bad draw:
 * <ul>
 *   <li><b>Mana is life force.</b> Spend it to nothing and you collapse (and, if the server allows it, can die).</li>
 *   <li><b>Mage Stages 9 to 0, Arcane and Saint.</b> Power comes from <i>training</i> (rank, mastery), never from which magic you drew.</li>
 *   <li><b>Affinity</b> (loved / ordinary / hated by mana) is a balanced trade you may change at will, not a luck tier.</li>
 *   <li><b>Mana Zone:</b> tap the mana around you; the denser the place (a Grand Magic Zone) the stronger it is.</li>
 *   <li><b>Negative Mana (Forbidden Magic):</b> a Magic Stone amplifies everything and corrupts you; only an Arcane mage survives it.</li>
 * </ul>
 * Everything is stored on the player (persisted data) and is server-side; the client only shows a summary.
 */
public final class ManaLaw {
    private ManaLaw() {}

    private static final String KEY = "nusmp_mana";
    public static final String SEALED = "nusmp_sealed_until";

    // ---------------------------------------------------------------- storage
    private static CompoundTag data(Player p) {
        CompoundTag pd = p.getPersistentData();
        if (!pd.contains(Player.PERSISTED_NBT_TAG, 10)) pd.put(Player.PERSISTED_NBT_TAG, new CompoundTag());
        CompoundTag root = pd.getCompound(Player.PERSISTED_NBT_TAG);
        if (!root.contains(KEY, 10)) root.put(KEY, new CompoundTag());
        return root.getCompound(KEY);
    }

    // ---------------------------------------------------------------- affinity: a balanced trade, never a luck tier
    public enum Affinity {
        LOVED("Loved by Mana", "A deep well and a slow stream: spells cost 10% less mana and recharge 10% slower.", 0.90, 1.10),
        NORMAL("Ordinary", "No lean either way.", 1.0, 1.0),
        HATED("Hated by Mana", "A shallow well and a quick stream: spells cost 10% more mana and recharge 10% faster.", 1.10, 0.90);

        public final String label, blurb;
        public final double cost, cooldown;

        Affinity(String label, String blurb, double cost, double cooldown) { this.label = label; this.blurb = blurb; this.cost = cost; this.cooldown = cooldown; }

        public static Affinity byName(String n) {
            for (Affinity a : values()) if (a.name().equalsIgnoreCase(n)) return a;
            return null;
        }
    }

    public static Affinity affinity(Player p) {
        Affinity a = Affinity.byName(data(p).getString("Affinity"));
        return a == null ? Affinity.NORMAL : a;
    }

    public static void setAffinity(Player p, Affinity a) { data(p).putString("Affinity", a.name()); }

    // ---------------------------------------------------------------- stages
    /** 9 (lowest) to 0 (highest) from rank and class, then the Arcane and Saint stages above them. */
    public record Stage(int number, boolean arcane, boolean saint) {
        public String label() { return saint ? "Saint Stage" : arcane ? "Arcane Stage" : "Stage " + number; }
        /** Spells cost less the higher you stand. */
        public double cost() { return saint ? 0.70 : arcane ? 0.75 : 1.15 - 0.039 * (9 - number); }
        /** ...and hit harder. */
        public double power() { return saint ? 1.35 : arcane ? 1.30 : 0.90 + 0.04 * (9 - number); }
    }

    public static Stage stage(Player p) {
        int step = MultiverseProfile.step(p);                                      // 0 (5th class junior) .. 16 (Wizard King)
        int number = 9 - Mth.clamp(Math.round(step * 9f / 16f), 0, 9);
        CompoundTag d = data(p);
        boolean arcane = d.getBoolean("Arcane") || d.getBoolean("Pact") || AntiMagic.isUser(p);   // magic that defies what magic can do
        return new Stage(number, arcane, d.getBoolean("Saint"));
    }

    public static void setArcane(Player p, boolean v) { data(p).putBoolean("Arcane", v); }

    public static void setSaint(Player p, boolean v) { data(p).putBoolean("Saint", v); }

    // ---------------------------------------------------------------- the numbers every spell passes through
    /** Multiplies a grimoire spell's mana cost. */
    public static double costMult(ServerPlayer p) {
        double c = stage(p).cost() * affinity(p).cost * Heritage.costMult(p);
        if (zoneActive(p)) c *= 0.8;
        if (negativeActive(p)) c *= 0.7;
        return c;
    }

    /** Multiplies a spell's cooldown. */
    public static double cooldownMult(ServerPlayer p) { return affinity(p).cooldown; }

    /** Multiplies the damage of a spell. */
    public static double powerMult(LivingEntity e) {
        if (!(e instanceof ServerPlayer p)) return 1.0;
        double m = stage(p).power() * Heritage.powerMult(p);
        if (zoneActive(p)) m *= 1.0 + 0.2 * Math.min(2.0, density(p.level(), p.blockPosition()));
        if (negativeActive(p)) m *= 1.6;
        return m;
    }

    // ---------------------------------------------------------------- mana density, Grand Magic Zones
    /** How thick the mana is here: 1.0 is ordinary; the deep sea, volcanic ground, mountain tops, the deep dark and the End are Grand Magic Zones. */
    public static double density(Level level, BlockPos pos) {
        var biome = level.getBiome(pos);
        double d = 1.0;
        if (biome.is(BiomeTags.IS_DEEP_OCEAN)) d += 1.0;                            // the Seabed Temple
        if (biome.is(BiomeTags.IS_NETHER)) d += 0.8;                                // the Ultime Volcano Trail
        if (biome.is(BiomeTags.IS_MOUNTAIN)) d += 0.4;
        if (biome.is(Biomes.DEEP_DARK)) d += 1.2;
        if (biome.is(BiomeTags.IS_END)) d += 1.0;
        if (pos.getY() > 190) d += 0.3;
        return Math.min(3.0, d);
    }

    public static boolean isGrandZone(Level level, BlockPos pos) { return density(level, pos) >= 1.8; }

    // ---------------------------------------------------------------- Mana Zone
    public static boolean zoneActive(Player p) { return p.level().getGameTime() < data(p).getLong("ZoneUntil"); }

    /** The Mana Zone page: tap all the mana around you for half a minute. Toggles. */
    public static boolean toggleZone(ServerPlayer p) {
        ServerLevel sl = p.serverLevel();
        CompoundTag d = data(p);
        if (zoneActive(p)) {
            d.putLong("ZoneUntil", 0);
            p.displayClientMessage(Component.literal("Your Mana Zone fades.").withStyle(ChatFormatting.GRAY), true);
            return true;
        }
        var ex = TensuraStorages.getExistenceFrom(p);
        if (ex != null && !p.isCreative() && ex.getMagicule() < EnergyHelper.getMaxMagicule(p) * 0.15) {
            p.displayClientMessage(Component.literal("Not enough mana to open a Mana Zone.").withStyle(ChatFormatting.RED), true);
            return false;
        }
        int ticks = (int) (600 * Mth.clamp(density(sl, p.blockPosition()), 1.0, 2.0));
        d.putLong("ZoneUntil", sl.getGameTime() + ticks);
        VfxSpawn.sendFollowing(sl, VfxShape.MAGIC_CIRCLE, p, p.position().add(0, 0.08, 0), 0xFF7A5CFF, Math.min(200, ticks), 2.4f);
        sl.playSound(null, p.blockPosition(), SoundEvents.BEACON_ACTIVATE, SoundSource.PLAYERS, 1.2f, 1.7f);
        String where = isGrandZone(sl, p.blockPosition()) ? " The mana here is thick: a Grand Magic Zone." : "";
        p.displayClientMessage(Component.literal("Mana Zone: the mana around you answers." + where).withStyle(ChatFormatting.LIGHT_PURPLE), true);
        return true;
    }

    // ---------------------------------------------------------------- Negative Mana (Forbidden Magic)
    public static boolean negativeActive(Player p) { return p.level().getGameTime() < data(p).getLong("NegUntil"); }

    public static float corruption(Player p) { return data(p).getFloat("Corruption"); }

    /** Attune to a Magic Stone. Only a mage whose magic already defies the ordinary can carry it; anyone else is torn by it. */
    public static boolean attuneNegative(ServerPlayer p) {
        ServerLevel sl = p.serverLevel();
        if (negativeActive(p)) { p.displayClientMessage(Component.literal("Negative mana already runs through you.").withStyle(ChatFormatting.DARK_PURPLE), true); return false; }
        if (!stage(p).arcane()) {
            addCorruption(p, 60f);
            p.addEffect(new MobEffectInstance(MobEffects.WITHER, 100, 1));
            p.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, 80, 0));
            p.displayClientMessage(Component.literal("The stone tears at you: only an Arcane Stage mage can hold negative mana.").withStyle(ChatFormatting.DARK_RED), true);
            return true;                                                          // the stone is spent
        }
        data(p).putLong("NegUntil", sl.getGameTime() + 1200);
        data(p).putBoolean("Pact", true);
        VfxSpawn.sendFollowing(sl, VfxShape.MAGIC_CIRCLE, p, p.position().add(0, 0.08, 0), 0xFF3A0A5A, 100, 2.8f);
        sl.playSound(null, p.blockPosition(), SoundEvents.WARDEN_HEARTBEAT, SoundSource.PLAYERS, 2f, 0.6f);
        p.displayClientMessage(Component.literal("Negative mana floods through you. Your magic is mighty, and it costs you a little of yourself with every cast.")
                .withStyle(ChatFormatting.DARK_PURPLE), true);
        return true;
    }

    public static void addCorruption(ServerPlayer p, float amount) {
        CompoundTag d = data(p);
        d.putFloat("Corruption", Mth.clamp(d.getFloat("Corruption") + amount, 0f, 150f));
    }

    // ---------------------------------------------------------------- the choice at the ceremony (no magic is a bad draw)
    public static void setOffer(Player p, String cover, String where) {
        CompoundTag d = data(p);
        d.putString("OfferCover", cover);
        d.putString("OfferWhere", where);
        d.putLong("OfferSentAt", p.level().getGameTime());
    }

    public static String offerCover(Player p) { return data(p).getString("OfferCover"); }

    public static String offerWhere(Player p) { return data(p).getString("OfferWhere"); }

    public static void clearOffer(Player p) { data(p).remove("OfferCover"); data(p).remove("OfferWhere"); }

    /** True when the offer was last shown more than 30 s ago (and marks it shown now). */
    public static boolean offerDue(Player p) {
        long now = p.level().getGameTime();
        if (now - data(p).getLong("OfferSentAt") < 600) return false;
        data(p).putLong("OfferSentAt", now);
        return true;
    }

    /** The panel's Mana block. */
    public static CompoundTag summary(Player p) {
        CompoundTag t = new CompoundTag();
        Stage s = stage(p);
        t.putString("Stage", s.label());
        t.putString("Affinity", affinity(p).label);
        t.putInt("Cost", (int) Math.round(s.cost() * affinity(p).cost * 100));
        t.putInt("Power", (int) Math.round(s.power() * 100));
        t.putInt("Corruption", (int) corruption(p));
        t.putBoolean("Zone", zoneActive(p));
        t.putBoolean("Negative", negativeActive(p));
        t.putString("Density", String.format("%.1f", density(p.level(), p.blockPosition())));
        t.putBoolean("Grand", isGrandZone(p.level(), p.blockPosition()));
        t.putString("Offer", offerCover(p));
        var k = Heritage.kingdom(p);
        var b = Heritage.blood(p);
        if (k != null) { var tr = Heritage.kingdomTrait(k); t.putString("Kingdom", k.displayName); t.putString("KingdomBuff", tr.buff()); t.putString("KingdomDebuff", tr.debuff()); }
        if (b != null) { var tr = Heritage.bloodTrait(b); t.putString("Blood", b.displayName); t.putString("BloodBuff", tr.buff()); t.putString("BloodDebuff", tr.debuff()); }
        return t;
    }

    // ---------------------------------------------------------------- hooks from the grimoire
    /** After a successful grimoire cast. */
    public static void onCast(ServerPlayer p) {
        if (negativeActive(p)) addCorruption(p, 4f);
    }

    /** Spending your mana to nothing: the shock, then the collapse. Mana is life force. */
    public static void exhaust(ServerPlayer p) {
        var ex = TensuraStorages.getExistenceFrom(p);
        if (ex == null || p.isCreative()) return;
        double frac = ex.getMagicule() / Math.max(1.0, EnergyHelper.getMaxMagicule(p));
        if (frac < 0.05) {
            p.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 100, 1));
            p.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 100, 1));
            p.addEffect(new MobEffectInstance(MobEffects.DIG_SLOWDOWN, 100, 1));
            p.displayClientMessage(Component.literal("Your mana is spent. Exhaustion takes you.").withStyle(ChatFormatting.GRAY), true);
        }
        if (frac < 0.005) collapse(p);
    }

    private static void collapse(ServerPlayer p) {
        long now = p.level().getGameTime();
        CompoundTag d = data(p);
        if (now - d.getLong("LastCollapse") < 200) return;                          // one faint at a time
        d.putLong("LastCollapse", now);
        p.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 160, 4));
        p.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, 120, 0));
        p.addEffect(new MobEffectInstance(MobEffects.CONFUSION, 160, 0));
        float shock = p.getMaxHealth() * 0.3f;
        boolean lethal = MultiverseConfig.get(MultiverseConfig.LETHAL_EXHAUSTION);
        float dmg = lethal ? shock : Math.min(shock, Math.max(0f, p.getHealth() - 1f));    // by default exhaustion alone never kills
        if (dmg > 0) p.hurt(p.damageSources().magic(), dmg);
        p.displayClientMessage(Component.literal("You black out. Mana is your life force, and you have burned it all.").withStyle(ChatFormatting.DARK_RED, ChatFormatting.BOLD), true);
    }

    // ---------------------------------------------------------------- once a second
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer p) || p.tickCount % 20 != 0) return;
        ServerLevel sl = p.serverLevel();
        CompoundTag d = data(p);
        long now = sl.getGameTime();
        // Mana Zone: it drinks a little mana a second; its owner flies light on the mana around them
        if (now < d.getLong("ZoneUntil")) {
            var ex = TensuraStorages.getExistenceFrom(p);
            if (ex != null && !p.isCreative()) {
                double max = EnergyHelper.getMaxMagicule(p);
                if (ex.getMagicule() < max * 0.05) {
                    d.putLong("ZoneUntil", 0);
                    p.displayClientMessage(Component.literal("The Mana Zone collapses: you have no mana left to hold it.").withStyle(ChatFormatting.GRAY), true);
                    collapse(p);
                } else {
                    ex.setMagicule(ex.getMagicule() - max * 0.012 / Math.max(1.0, density(sl, p.blockPosition())));
                    ex.markDirty();
                }
            }
            p.addEffect(new MobEffectInstance(MobEffects.SLOW_FALLING, 60, 0, false, false));
            p.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 60, 0, false, false));
        }
        // entering a Grand Magic Zone
        boolean grand = isGrandZone(sl, p.blockPosition());
        if (grand != d.getBoolean("InGrand")) {
            d.putBoolean("InGrand", grand);
            if (grand) p.displayClientMessage(Component.literal("The mana here is thick: a Grand Magic Zone.").withStyle(ChatFormatting.LIGHT_PURPLE), true);
        }
        // corruption: it eats, and it slowly lets go
        float c = d.getFloat("Corruption");
        if (c > 0) {
            if (!negativeActive(p)) c = Math.max(0, c - 0.5f);
            d.putFloat("Corruption", c);
            if (c >= 100) {
                p.getPersistentData().putLong(SEALED, now + 1200);                  // the grimoire shuts itself against you
                p.hurt(p.damageSources().magic(), 6f);
                d.putFloat("Corruption", 60f);
                p.displayClientMessage(Component.literal("Negative mana corrupts you. Your grimoire seals itself.").withStyle(ChatFormatting.DARK_RED, ChatFormatting.BOLD), true);
            } else if (c >= 70) {
                p.addEffect(new MobEffectInstance(MobEffects.DARKNESS, 60, 0, false, false));
                p.addEffect(new MobEffectInstance(MobEffects.WITHER, 40, 0, false, true));
            } else if (c >= 40) {
                p.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 60, 0, false, true));
            }
        }
    }

    // ---------------------------------------------------------------- the universal combat page
    /**
     * Mana Strike: every grimoire has it, whatever its magic. A bolt of raw mana 24 blocks long, tinted with the book's colour. Power
     * comes from the caster's stage, not from the book, so a healer's or a sealer's grimoire is never a dud.
     */
    public static boolean manaStrike(GrimoireBook book, ServerPlayer p) {
        ServerLevel sl = p.serverLevel();
        Vec3 eye = p.getEyePosition(), look = p.getViewVector(1f), end = eye.add(look.scale(24));
        int hits = 0;
        for (LivingEntity t : GrimoireBook.along(p, eye, end, 1.2)) {
            if (t == p || t.isAlliedTo(p)) continue;
            t.hurt(p.damageSources().indirectMagic(p, p), BalanceLaw.damage(p, t, 7f, 0.6));
            hits++;
            if (hits >= 3) { end = t.getBoundingBox().getCenter(); break; }
        }
        VfxSpawn.send(sl, VfxShape.LIGHT_BLADE, eye.add(0, -0.3, 0), end, book.color, 8, 1.0f);
        VfxSpawn.send(sl, VfxShape.MAGIC_CIRCLE_EXPLOSION, end, end, book.color, 10, 0.5f);
        sl.playSound(null, p.blockPosition(), SoundEvents.ILLUSIONER_CAST_SPELL, SoundSource.PLAYERS, 1.2f, 1.3f);
        return true;
    }
}
