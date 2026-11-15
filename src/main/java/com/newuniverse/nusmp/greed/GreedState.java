package com.newuniverse.nusmp.greed;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Why: what the wielder of the Black Sword Greed has devoured, saved with the player ({@link GreedStore}): the permanent bonus stats, the stored EP that opens the tiers, the satiation, Half-Starvation and the Gluttony
 * Skill Vault (stolen skill ids, each favourited or not). Hand-written, clamped tag format so a damaged save cannot give a number outside {@link GreedRules}. Server thread only.
 */
public final class GreedState {
    public double bonusHp, bonusAtk, bonusArmor, storedEp;
    public float satiation = 100.0f;
    public boolean half;
    private final Map<String, Boolean> vault = new LinkedHashMap<>();

    public boolean isEmpty() { return storedEp <= 0 && bonusHp <= 0 && bonusAtk <= 0 && bonusArmor <= 0 && vault.isEmpty() && !half && satiation >= 100.0f; }

    public int tier() { return GreedRules.tierFor(storedEp); }

    public void add(double hp, double atk, double armor, double ep) {
        bonusHp = GreedRules.clamp(bonusHp + hp, 0.0, GreedRules.MAX_HP);
        bonusAtk = GreedRules.clamp(bonusAtk + atk, 0.0, GreedRules.MAX_ATK);
        bonusArmor = GreedRules.clamp(bonusArmor + armor, 0.0, GreedRules.MAX_ARMOR);
        storedEp = Math.max(0.0, storedEp + ep);
    }

    /** Pays {@code share} (0..1) of the devoured stats; returns the power paid (the technique's strength). The stored EP grows by what the sacrificed power is worth. */
    public double sacrifice(double share) {
        double r = GreedRules.clamp(share, 0.0, 1.0);
        double hp = bonusHp * r, atk = bonusAtk * r, ar = bonusArmor * r;
        bonusHp -= hp;
        bonusAtk -= atk;
        bonusArmor -= ar;
        double power = GreedRules.power(hp, atk, ar);
        storedEp += GreedRules.epOfPower(power);
        return power;
    }

    public void setSatiation(float s) { satiation = Math.max(0.0f, Math.min(100.0f, s)); }

    public void reset() {
        bonusHp = bonusAtk = bonusArmor = storedEp = 0.0;
        satiation = 100.0f;
        half = false;
        vault.clear();
    }

    // ---------------------------------------------------------------- the vault
    public List<String> vaultIds() { return new ArrayList<>(vault.keySet()); }
    public int vaultSize() { return vault.size(); }
    public boolean favourited(String id) { return vault.getOrDefault(id, false); }
    public boolean has(String id) { return vault.containsKey(id); }

    public boolean addSkill(String id) {
        if (id == null || id.isEmpty() || vault.containsKey(id) || vault.size() >= GreedRules.VAULT_MAX) return false;
        vault.put(id, false);
        return true;
    }

    public boolean toggleFavourite(String id) {
        if (!vault.containsKey(id)) return false;
        vault.put(id, !vault.get(id));
        return true;
    }

    public boolean removeSkill(String id) { return vault.remove(id) != null; }

    // ---------------------------------------------------------------- saving
    public CompoundTag toTag() {
        CompoundTag t = new CompoundTag();
        t.putDouble("hp", bonusHp);
        t.putDouble("atk", bonusAtk);
        t.putDouble("armor", bonusArmor);
        t.putDouble("ep", storedEp);
        t.putFloat("sat", satiation);
        t.putBoolean("half", half);
        ListTag l = new ListTag();
        for (Map.Entry<String, Boolean> e : vault.entrySet()) {
            CompoundTag c = new CompoundTag();
            c.putString("id", e.getKey());
            c.putBoolean("fav", e.getValue());
            l.add(c);
        }
        t.put("vault", l);
        return t;
    }

    public static GreedState fromTag(CompoundTag t) {
        GreedState s = new GreedState();
        s.bonusHp = GreedRules.clamp(t.getDouble("hp"), 0.0, GreedRules.MAX_HP);
        s.bonusAtk = GreedRules.clamp(t.getDouble("atk"), 0.0, GreedRules.MAX_ATK);
        s.bonusArmor = GreedRules.clamp(t.getDouble("armor"), 0.0, GreedRules.MAX_ARMOR);
        s.storedEp = Math.max(0.0, t.getDouble("ep"));
        s.satiation = t.contains("sat") ? Math.max(0.0f, Math.min(100.0f, t.getFloat("sat"))) : 100.0f;
        s.half = t.getBoolean("half");
        ListTag l = t.getList("vault", Tag.TAG_COMPOUND);
        for (int i = 0; i < l.size() && s.vault.size() < GreedRules.VAULT_MAX; i++) {
            CompoundTag c = l.getCompound(i);
            String id = c.getString("id");
            if (!id.isEmpty()) s.vault.put(id, c.getBoolean("fav"));
        }
        return s;
    }

}
