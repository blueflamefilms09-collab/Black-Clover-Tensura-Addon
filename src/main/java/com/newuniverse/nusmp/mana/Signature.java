package com.newuniverse.nusmp.mana;

import com.newuniverse.nusmp.blackclover.MagicType;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.saveddata.SavedData;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.UUID;

/**
 * 1.16.0: no two wizards' magic is the same, as in the show. Whoever receives a grimoire also receives a <b>signature</b>: their own
 * way of casting that attribute, which no other wizard on the server shares.
 * <ul>
 *   <li>a <b>name</b> ("Ashen Flame", "Sunless Gale of Blackmere"), unique on the server (a registry saved with the world checks it);</li>
 *   <li>two <b>traits</b>, small trades of power, mana cost and recharge (Overcharged, Quickened, Thrifty ...) that apply to every spell
 *       of the grimoire, so two Fire mages cast Fire a little differently;</li>
 *   <li>a <b>shape</b> for their own <b>Signature Spell</b>, the last family page of the grimoire (book.SignatureSpell): a lance, a nova, a
 *       wave, an orb, a rain or a spiral, built from the grimoire's attribute (and, for a fused grimoire, from both parents).</li>
 * </ul>
 * Stored in the player's persistent data (sub-compound "nusmp_signature"), so it survives death and is generated on first need for
 * grimoires that were granted before this existed.
 */
public final class Signature {
    private Signature() {}

    private static final String KEY = "nusmp_signature";

    public enum Shape { LANCE, NOVA, WAVE, ORB, RAIN, SPIRAL;
        public String label() { return name().charAt(0) + name().substring(1).toLowerCase(); } }

    /** A trait is a small trade: multipliers on spell power, mana cost and recharge. */
    public enum Trait {
        OVERCHARGED("Overcharged", 1.12, 1.10, 1.00), QUICKENED("Quickened", 0.95, 1.00, 0.88), THRIFTY("Thrifty", 0.95, 0.88, 1.00),
        HEAVY("Heavy", 1.10, 1.00, 1.12), FOCUSED("Focused", 1.06, 0.95, 1.06), WILD("Wild", 1.15, 1.00, 1.10),
        STEADY("Steady", 1.00, 0.92, 1.04), RADIANT("Radiant", 1.04, 1.04, 0.94);
        public final String label;
        public final double power, cost, cooldown;
        Trait(String label, double power, double cost, double cooldown) { this.label = label; this.power = power; this.cost = cost; this.cooldown = cooldown; }
    }

    private static final String[] EPITHETS = {"Ashen", "Gilded", "Hollow", "Sunless", "Verdant", "Crimson", "Silent", "Gleaming", "Wandering", "Gentle", "Fierce",
            "Veiled", "Restless", "Ancient", "Humble", "Radiant", "Burnished", "Sleeping", "Thorned", "Frozen", "Roaring", "Drifting", "Brazen", "Pale",
            "Starlit", "Wild", "Patient", "Hidden", "Reckless", "Steadfast", "Mirthful", "Somber", "Hungry", "Lonely", "Proud", "Dancing", "Singing",
            "Falling", "Rising", "Endless", "Fleeting", "Bitter", "Sweet", "Quiet", "Thunderous", "Misty", "Rusted", "Silvered", "Golden", "Ivory",
            "Obsidian", "Amber", "Cobalt", "Scarlet", "Violet", "Emerald", "Saffron", "Umber", "Sable", "Opal", "Lunar", "Solar", "Tidal", "Windborn"};
    private static final String[] PLACES = {"Ashvale", "Blackmere", "the Low Road", "Greywatch", "Emberfall", "Stillwater", "Thornhold", "Highmoor",
            "Saltmarsh", "the Pale Coast", "Redwood", "Dunmere", "Windhollow", "Stonebridge", "the Last Lantern", "Oldcrown", "Fenwick", "Harrowgate",
            "Lowtide", "Sunspire", "Wolfsbane", "Kestrel Keep", "Mossgate", "Hollowbrook"};

    // ---------------------------------------------------------------- the names taken (saved with the world)
    static final class Registry extends SavedData {
        final Map<String, UUID> taken = new HashMap<>();

        static Registry get(MinecraftServer server) {
            return server.overworld().getDataStorage().computeIfAbsent(new SavedData.Factory<>(Registry::new, Registry::load, null), "nusmp_signatures");
        }

        private static Registry load(CompoundTag tag, HolderLookup.Provider provider) {
            Registry r = new Registry();
            CompoundTag t = tag.getCompound("Taken");
            for (String k : t.getAllKeys()) if (t.hasUUID(k)) r.taken.put(k, t.getUUID(k));
            return r;
        }

        @Override
        public CompoundTag save(CompoundTag tag, HolderLookup.Provider provider) {
            CompoundTag t = new CompoundTag();
            for (var e : taken.entrySet()) t.putUUID(e.getKey(), e.getValue());
            tag.put("Taken", t);
            return tag;
        }
    }

    // ---------------------------------------------------------------- storage
    private static CompoundTag root(Player p) {
        CompoundTag pd = p.getPersistentData();
        if (!pd.contains(Player.PERSISTED_NBT_TAG, 10)) pd.put(Player.PERSISTED_NBT_TAG, new CompoundTag());
        return pd.getCompound(Player.PERSISTED_NBT_TAG);
    }

    private static CompoundTag data(Player p) {
        CompoundTag r = root(p);
        if (!r.contains(KEY, 10)) r.put(KEY, new CompoundTag());
        return r.getCompound(KEY);
    }

    public static boolean has(Player p) { return root(p).contains(KEY, 10) && !data(p).getString("Name").isEmpty(); }

    // ---------------------------------------------------------------- making one
    /** Gives this wizard a signature for this grimoire attribute (kept if it already belongs to the same attribute). Returns the name. */
    public static String assign(ServerPlayer p, MagicType base) { return assign(p, base, base.word, false); }

    /**
     * @param label the word the name ends in: the attribute's own ("Flame"), or the name of a magic born of two attributes ("Pyrotide")
     * @param force make a new signature even if the wizard already has one for this attribute (a new grimoire has been granted)
     */
    public static String assign(ServerPlayer p, MagicType base, String label, boolean force) {
        CompoundTag d = data(p);
        if (!force && !d.getString("Name").isEmpty() && base.name().equals(d.getString("Magic"))) return d.getString("Name");
        MinecraftServer server = p.getServer();
        Registry reg = server == null ? null : Registry.get(server);
        if (reg != null) reg.taken.values().removeIf(u -> u.equals(p.getUUID()));          // an old name of this wizard is free again
        String name = null;
        long salt = 0;
        Random rnd = null;
        for (int tries = 0; tries < 200; tries++, salt++) {
            rnd = new Random(p.getUUID().getMostSignificantBits() * 31 + p.getUUID().getLeastSignificantBits() + base.ordinal() * 0x9E3779B97F4A7C15L + salt * 0x632BE59BD9B4E019L);
            String candidate = EPITHETS[rnd.nextInt(EPITHETS.length)] + " " + label;
            if (tries >= 60) candidate += " of " + PLACES[rnd.nextInt(PLACES.length)];
            if (tries >= 120) candidate += " " + roman(1 + (tries - 120) / 6);
            if (reg == null || !reg.taken.containsKey(candidate)) { name = candidate; break; }
        }
        if (name == null) name = "Nameless " + label + " " + p.getUUID().toString().substring(0, 4);
        if (rnd == null) rnd = new Random(p.getUUID().getLeastSignificantBits());
        Trait[] all = Trait.values();
        int a = rnd.nextInt(all.length), b = rnd.nextInt(all.length - 1);
        if (b >= a) b++;
        d.putString("Name", name);
        d.putString("Magic", base.name());
        d.putInt("Shape", rnd.nextInt(Shape.values().length));
        d.putInt("A", a);
        d.putInt("B", b);
        if (reg != null) { reg.taken.put(name, p.getUUID()); reg.setDirty(); }
        return name;
    }

    private static String roman(int n) { return switch (Math.min(n, 5)) { case 1 -> "I"; case 2 -> "II"; case 3 -> "III"; case 4 -> "IV"; default -> "V"; }; }

    /** The wizard's signature, made now if they have a grimoire but no signature yet (grimoires granted before 1.16.0). */
    public static void ensure(ServerPlayer p, MagicType base) { if (!has(p)) assign(p, base); }

    // ---------------------------------------------------------------- reading it
    public static String name(Player p) { return has(p) ? data(p).getString("Name") : ""; }

    public static Shape shape(Player p) {
        Shape[] s = Shape.values();
        return has(p) ? s[Math.floorMod(data(p).getInt("Shape"), s.length)] : Shape.LANCE;
    }

    private static Trait trait(Player p, String key) { Trait[] t = Trait.values(); return t[Math.floorMod(data(p).getInt(key), t.length)]; }

    public static double power(Player p) { return has(p) ? trait(p, "A").power * trait(p, "B").power : 1.0; }
    public static double cost(Player p) { return has(p) ? trait(p, "A").cost * trait(p, "B").cost : 1.0; }
    public static double cooldown(Player p) { return has(p) ? trait(p, "A").cooldown * trait(p, "B").cooldown : 1.0; }

    /** "Ashen Flame: Quickened and Thrifty, a Lance". Empty without a signature. */
    public static String describe(Player p) {
        if (!has(p)) return "";
        return name(p) + ": " + trait(p, "A").label + " and " + trait(p, "B").label + ", a " + shape(p).label();
    }
}
