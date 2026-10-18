package com.newuniverse.nusmp.heroweapon.bond;

import com.newuniverse.nusmp.heroweapon.HeroKind;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

/**
 * Why: a hero weapon belongs to a PERSON, not to an item stack: it cannot be dropped, traded or lost (the owner's "permanently bound to the user's body"), it
 * keeps what it has absorbed and which forms it has learned through death, and the stack in the hand is only a projection of it. This is that record: which
 * weapon (if any), its experience, the active form, the forms learned, the mastery of each, what the gem has absorbed, whether the weapon is summoned, and an
 * open {@link #extra} area per system (the loan of the item belongs to the core, the curse to the curse system) so no system has to change this class to
 * save something. It is stored as a NeoForge attachment ({@link HeroBonds}) in NBT, so an old save, a save without a field or a hand-edited save still loads
 * (every key is optional). Server thread only; the client gets a copy by the core's sync payload.
 */
public final class HeroBond {
    private HeroKind kind;
    private long boundAt;
    private long exp;
    private String form = "base";
    private boolean summoned;
    private final Set<String> unlocked = new LinkedHashSet<>();
    private final Set<String> bonusGranted = new LinkedHashSet<>();
    private final Map<String, Integer> mastery = new LinkedHashMap<>();
    private final Map<String, Integer> absorbed = new LinkedHashMap<>();
    private CompoundTag extra = new CompoundTag();

    // ------------------------------------------------------------------ the weapon
    public boolean isBound() { return kind != null; }

    /** The bound weapon, or null. */
    public HeroKind kind() { return kind; }

    public long boundAt() { return boundAt; }

    /** Binds a weapon: forgets everything of the previous bond (the caller decides whether a rebind is allowed). */
    public void bind(HeroKind k, long gameTime) {
        clear();
        kind = k;
        boundAt = gameTime;
        unlocked.add("base");
    }

    /** Releases the weapon and forgets all its progress. */
    public void clear() {
        kind = null;
        boundAt = 0;
        exp = 0;
        form = "base";
        summoned = false;
        unlocked.clear();
        bonusGranted.clear();
        mastery.clear();
        absorbed.clear();
        extra = new CompoundTag();
    }

    // ------------------------------------------------------------------ level
    public long exp() { return exp; }

    public void setExp(long v) { exp = Math.max(0, v); }

    public void addExp(long v) { exp = Math.max(0, exp + v); }

    public int level() { return HeroLevels.level(exp); }

    // ------------------------------------------------------------------ summon
    public boolean summoned() { return summoned; }

    public void setSummoned(boolean v) { summoned = v && kind != null; }

    // ------------------------------------------------------------------ forms
    /** The id of the active form ({@code base} until another is chosen). */
    public String form() { return form; }

    public void setForm(String id) { form = id == null || id.isEmpty() ? "base" : id; }

    public Set<String> unlocked() { return Collections.unmodifiableSet(unlocked); }

    public boolean isUnlocked(String formId) { return "base".equals(formId) || unlocked.contains(formId); }

    /** True when the form was newly learned. */
    public boolean unlock(String formId) { return formId != null && !formId.isEmpty() && unlocked.add(formId); }

    public int mastery(String formId) { return mastery.getOrDefault(formId, 0); }

    public void addMastery(String formId, int points) {
        if (formId == null || formId.isEmpty() || points == 0) return;
        mastery.merge(formId, points, Integer::sum);
        if (mastery.get(formId) < 0) mastery.put(formId, 0);
    }

    public Map<String, Integer> masteryMap() { return Collections.unmodifiableMap(mastery); }

    /** True when the form's permanent ability bonus was already handed out. */
    public boolean bonusGranted(String formId) { return bonusGranted.contains(formId); }

    public boolean grantBonus(String formId) { return formId != null && bonusGranted.add(formId); }

    public Set<String> bonusesGranted() { return Collections.unmodifiableSet(bonusGranted); }

    // ------------------------------------------------------------------ absorption
    /** How many of an item (registry id, e.g. {@code minecraft:leather}) the gem has absorbed. */
    public int absorbedCount(String itemId) { return absorbed.getOrDefault(itemId, 0); }

    public void absorb(String itemId, int count) {
        if (itemId == null || itemId.isEmpty() || count <= 0) return;
        absorbed.merge(itemId, count, Integer::sum);
    }

    public Map<String, Integer> absorbedMap() { return Collections.unmodifiableMap(absorbed); }

    // ------------------------------------------------------------------ per-system area
    /**
     * A live sub-compound for one system to keep its own state in (keys by system: {@code loan}, {@code curse}, {@code book}, {@code gem} ...); created empty on
     * first use. Changing it changes the saved bond. Cleared with the bond.
     */
    public CompoundTag extra(String system) {
        if (!extra.contains(system, Tag.TAG_COMPOUND)) extra.put(system, new CompoundTag());
        return extra.getCompound(system);
    }

    public boolean hasExtra(String system) { return extra.contains(system, Tag.TAG_COMPOUND); }

    // ------------------------------------------------------------------ save
    public CompoundTag toTag() {
        CompoundTag t = new CompoundTag();
        if (kind != null) t.putString("kind", kind.itemId());
        t.putLong("boundAt", boundAt);
        t.putLong("exp", exp);
        t.putString("form", form);
        t.putBoolean("summoned", summoned);
        t.put("unlocked", strings(unlocked));
        t.put("bonus", strings(bonusGranted));
        t.put("mastery", ints(mastery));
        t.put("absorbed", ints(absorbed));
        t.put("extra", extra.copy());
        return t;
    }

    public static HeroBond fromTag(CompoundTag t) {
        HeroBond b = new HeroBond();
        if (t == null) return b;
        b.kind = t.contains("kind", Tag.TAG_STRING) ? HeroKind.byItemId(t.getString("kind")) : null;
        if (b.kind == null) return b;
        b.boundAt = t.getLong("boundAt");
        b.exp = Math.max(0, t.getLong("exp"));
        b.form = t.contains("form", Tag.TAG_STRING) && !t.getString("form").isEmpty() ? t.getString("form") : "base";
        b.summoned = t.getBoolean("summoned");
        readStrings(t, "unlocked", b.unlocked);
        readStrings(t, "bonus", b.bonusGranted);
        readInts(t, "mastery", b.mastery);
        readInts(t, "absorbed", b.absorbed);
        if (t.contains("extra", Tag.TAG_COMPOUND)) b.extra = t.getCompound("extra").copy();
        b.unlocked.add("base");
        return b;
    }

    /** Copies everything from another bond (the new body after a death). */
    public void copyFrom(HeroBond o) {
        HeroBond c = fromTag(o.toTag());
        kind = c.kind;
        boundAt = c.boundAt;
        exp = c.exp;
        form = c.form;
        summoned = false;                       // a new body starts with the weapon stowed
        unlocked.clear();
        unlocked.addAll(c.unlocked);
        bonusGranted.clear();
        bonusGranted.addAll(c.bonusGranted);
        mastery.clear();
        mastery.putAll(c.mastery);
        absorbed.clear();
        absorbed.putAll(c.absorbed);
        extra = c.extra;
    }

    public boolean isEmpty() { return kind == null; }

    private static ListTag strings(Set<String> s) {
        ListTag l = new ListTag();
        for (String v : s) l.add(StringTag.valueOf(v));
        return l;
    }

    private static CompoundTag ints(Map<String, Integer> m) {
        CompoundTag c = new CompoundTag();
        for (Map.Entry<String, Integer> e : m.entrySet()) c.putInt(e.getKey(), e.getValue());
        return c;
    }

    private static void readStrings(CompoundTag t, String key, Set<String> into) {
        if (!t.contains(key, Tag.TAG_LIST)) return;
        ListTag l = t.getList(key, Tag.TAG_STRING);
        for (int i = 0; i < l.size(); i++) into.add(l.getString(i));
    }

    private static void readInts(CompoundTag t, String key, Map<String, Integer> into) {
        if (!t.contains(key, Tag.TAG_COMPOUND)) return;
        CompoundTag c = t.getCompound(key);
        for (String k : c.getAllKeys()) into.put(k, c.getInt(k));
    }
}
