package com.newuniverse.nusmp.entity.riven;

import com.mojang.logging.LogUtils;
import com.newuniverse.nusmp.antimagic.AntiMagic;
import com.newuniverse.nusmp.blackclover.GrimoirePages;
import io.github.manasmods.manascore.skill.api.SkillAPI;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.AxeItem;
import net.minecraft.world.item.SwordItem;
import net.minecraft.world.item.TridentItem;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * What Riven knows about one target, built from server-side state only and kept in memory (nothing is ever sent off-server). {@link #traits}
 * are what the target IS (the "counters" of a codex skill match against them); {@link #resists} are what blunts it (matched against "resisted_by").
 * Rebuilt every replan; the last 8 damage outcomes persist across scans in {@link #recent}.
 */
public final class ThreatScan {
    private static final org.slf4j.Logger LOG = LogUtils.getLogger();
    private static List<ResourceLocation> tensuraIds;

    public final Set<String> traits = new HashSet<>();
    public final Set<String> resists = new HashSet<>();
    public final ArrayDeque<String> recent = new ArrayDeque<>();       // "<damage kind>:hit" / "<damage kind>:blocked", newest last
    public float health, maxHealth, absorption, armor;
    public double distance, vertical;
    public boolean lineOfSight, antiMagic, barrierUp;

    /** Records how a hit went; a kind that was blocked twice in the last 8 outcomes is treated as resisted. */
    public void record(String kind, boolean landed) {
        recent.addLast(kind + (landed ? ":hit" : ":blocked"));
        while (recent.size() > 8) recent.removeFirst();
    }

    public boolean blockedRecently(String kind) {
        int n = 0;
        for (String s : recent) if (s.equals(kind + ":blocked")) n++;
        return n >= 2;
    }

    /** Fresh scan of 'target' as seen by 'boss'; keeps the hit history of 'previous' when it was the same target. */
    public static ThreatScan of(RivenBossEntity boss, LivingEntity target, ThreatScan previous) {
        ThreatScan s = new ThreatScan();
        if (previous != null) s.recent.addAll(previous.recent);
        s.health = target.getHealth();
        s.maxHealth = target.getMaxHealth();
        s.absorption = target.getAbsorptionAmount();
        s.armor = (float) target.getAttributeValue(Attributes.ARMOR);
        s.distance = Math.sqrt(boss.distanceToSqr(target));
        s.vertical = target.getY() - boss.getY();
        s.lineOfSight = boss.hasLineOfSight(target);
        s.barrierUp = s.absorption >= 4f;
        if (s.barrierUp) s.traits.add("barrier");
        if (s.armor >= 12) s.traits.add("armored");
        if (s.armor >= 15 || s.maxHealth >= 60) s.traits.add("tank");
        if (target.hasEffect(MobEffects.REGENERATION) || target.hasEffect(MobEffects.HEAL)) s.traits.add("healer");
        if (target.isFallFlying() || target.getVehicle() != null || (target instanceof Player p && p.getAbilities().flying)) s.traits.add("flyer");
        if (s.distance > 8) s.traits.add("kiter");
        if (s.distance < 5) s.traits.add("rusher");
        if (target.isOnFire() || target.fireImmune()) s.resists.add("fire_immune");
        if (target instanceof Player p) {
            var held = p.getMainHandItem().getItem();
            if (held instanceof SwordItem || held instanceof AxeItem || held instanceof TridentItem) s.traits.add("melee");
            if (GrimoirePages.grimoireOf(p).isPresent()) s.traits.add("caster");
            if (AntiMagic.lord(p).isPresent()) { s.antiMagic = true; s.resists.add("anti_magic"); s.traits.add("anti_magic"); }
            scanTensura(p, s);
        }
        for (String k : List.of("physical", "magic")) if (s.blockedRecently(k)) s.resists.add(k.equals("physical") ? "physical_null" : "magic_null");
        return s;
    }

    /** Tensura via ManasCore: soft. An unknown or changed skill id is simply not seen. */
    private static void scanTensura(Player p, ThreatScan s) {
        try {
            if (tensuraIds == null) {
                tensuraIds = new ArrayList<>();
                for (ResourceLocation id : SkillAPI.getSkillRegistry().getIds()) {
                    String path = id.getPath();
                    if ("tensura".equals(id.getNamespace()) && (path.contains("barrier") || path.contains("nullification") || path.contains("immun") || path.contains("resist")))
                        tensuraIds.add(id);
                }
            }
            var skills = SkillAPI.getSkillsFrom(p);
            for (ResourceLocation id : tensuraIds) {
                if (skills.getSkill(id).isEmpty()) continue;
                String path = id.getPath();
                s.traits.add("tensura");
                if (path.contains("barrier")) { s.traits.add("barrier"); s.barrierUp = true; }
                if (path.contains("physical") && (path.contains("nullification") || path.contains("immun"))) s.resists.add("physical_null");
                if (path.contains("magic") && (path.contains("nullification") || path.contains("immun"))) s.resists.add("magic_null");
            }
        } catch (Throwable t) {
            LOG.debug("[nusmp] Riven could not read Tensura skills of {}: {}", p.getName().getString(), t.toString());
            tensuraIds = tensuraIds == null ? new ArrayList<>() : tensuraIds;
        }
    }
}
