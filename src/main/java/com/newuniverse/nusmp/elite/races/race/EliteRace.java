package com.newuniverse.nusmp.elite.races.race;

import com.newuniverse.nusmp.NUSMP;
import com.newuniverse.nusmp.elite.races.EliteRaces;
import com.newuniverse.nusmp.elite.races.EliteRacesConfig;
import com.newuniverse.nusmp.elite.races.data.RaceTemplate;
import com.newuniverse.nusmp.elite.races.data.RaceTemplates;
import com.newuniverse.nusmp.elite.races.rule.RaceEvolutionRule;
import com.newuniverse.nusmp.elite.races.rule.RaceRuleSet;
import com.newuniverse.nusmp.elite.races.runtime.RaceAttributes;
import com.newuniverse.nusmp.elite.races.runtime.RaceEvolution;
import com.newuniverse.nusmp.elite.races.runtime.RaceSkills;
import io.github.manasmods.manascore.race.api.ManasRace;
import io.github.manasmods.manascore.race.api.ManasRaceInstance;
import io.github.manasmods.manascore.skill.api.ManasSkill;
import io.github.manasmods.tensura.config.race.RaceConfig;
import io.github.manasmods.tensura.race.template.DefaultRace;
import io.github.manasmods.tensura.race.template.EvolutionRequirement;
import io.github.manasmods.tensura.storage.Alignment;
import net.minecraft.core.Holder;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attribute;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Why: this is how a race of this mod becomes a REAL Tensura race. It extends Tensura's own {@code DefaultRace} (the same base the Tensura races and the other add-ons use), is
 * registered in ManasCore's race registry (see {@link EliteRaces}), and takes everything from its template file (data/nusmp/elite/races/<id>.json): the default-race stats block
 * through {@link TemplateStats}, extra attributes, intrinsic and resistance skills, the next race and the evolution requirements. So Tensura's own evolution screen, race command,
 * stat reset and skill learning all work on it with no special case. The behaviour that is NOT Tensura's (the transformation states, Zenkai, kill counting) lives in
 * {@code runtime/} and keys off this class. If the master switch is off the race keeps its stats and skills but offers no evolution.
 *
 * <p>The template id is assigned after {@code super(...)} and the stats are read only after it is set, which is the order the other add-ons' races use (the base constructor does
 * not read the config).
 */
public abstract class EliteRace extends DefaultRace {
    private final String templateId;

    protected EliteRace(String templateId) {
        super(difficultyOf(templateId));
        this.templateId = templateId;
        applyDefaultAttributeModifiers();
        applyExtraAttributes();
    }

    private static Difficulty difficultyOf(String id) {
        RaceTemplate t = RaceTemplates.get(id);
        if (t == null) throw new IllegalStateException("elite race template '" + id + "' is not loaded (RaceTemplates.loadBundled must run before the races are built)");
        return Difficulty.valueOf(t.difficulty().toUpperCase(Locale.ROOT));
    }

    public String templateId() { return templateId; }

    /** The live template (a datapack may have replaced it since the race was built; the stats are baked, the rest follows). */
    public RaceTemplate template() { return RaceTemplates.get(templateId); }

    private void applyExtraAttributes() {
        for (RaceTemplate.Extra e : RaceTemplates.get(templateId).attributes()) {
            Holder<Attribute> h = RaceAttributes.get(e.attribute());
            if (h == null) continue;
            addAttributeModifier(h, ResourceLocation.fromNamespaceAndPath(NUSMP.MODID, "race_" + templateId + "_" + e.attribute().replace(':', '_')),
                    e.amount(), RaceAttributes.operation(e.op()));
        }
    }

    // ---------------------------------------------------------------- Tensura's default-race block
    @Override
    public RaceConfig.Default getDefaultConfig() { return TemplateStats.of(templateId); }

    @Override
    public Alignment getAlignment() {
        RaceTemplate t = template();
        String a = t == null ? "default" : t.alignment();
        return switch (a) {
            case "majin" -> Alignment.MAJIN;
            case "holy" -> Alignment.HOLY;
            case "chaos" -> Alignment.CHAOS;
            default -> Alignment.DEFAULT;
        };
    }

    // ---------------------------------------------------------------- look and words
    @Override
    public ResourceLocation getRaceIcon() { return ResourceLocation.fromNamespaceAndPath(NUSMP.MODID, "textures/race/" + templateId + ".png"); }

    @Override
    public MutableComponent getName() { return Component.translatableWithFallback("nusmp.race." + templateId, RaceText.title(templateId)); }

    @Override
    public MutableComponent getRaceDescription() { return Component.translatableWithFallback("nusmp.race." + templateId + ".description", RaceText.describe(template())); }

    // ---------------------------------------------------------------- skills
    @Override
    public List<ManasSkill> getIntrinsicSkills(ManasRaceInstance instance, LivingEntity entity) {
        List<ManasSkill> out = new ArrayList<>(super.getIntrinsicSkills(instance, entity));
        RaceTemplate t = template();
        if (t != null) {
            for (ManasSkill s : RaceSkills.resolve(t.intrinsic())) if (!out.contains(s)) out.add(s);
            for (ManasSkill s : RaceSkills.resolve(t.resistances())) if (!out.contains(s)) out.add(s);
        }
        return out;
    }

    // ---------------------------------------------------------------- evolution
    private ManasRace next() {
        RaceTemplate t = template();
        return t == null || t.evolvesTo().isEmpty() ? null : EliteRaces.race(t.evolvesTo());
    }

    @Override
    public ManasRace getDefaultEvolution(ManasRaceInstance instance, LivingEntity entity) { return EliteRacesConfig.enabled() ? next() : null; }

    @Override
    public List<ManasRace> getNextEvolutions(ManasRaceInstance instance, LivingEntity entity) {
        ManasRace n = EliteRacesConfig.enabled() ? next() : null;
        return n == null ? List.of() : List.of(n);
    }

    @Override
    public List<ManasRace> getPreviousEvolutions(ManasRaceInstance instance, LivingEntity entity) {
        List<ManasRace> out = new ArrayList<>();
        for (RaceTemplate t : RaceTemplates.all().values()) {
            if (t.evolvesTo().equals(templateId)) {
                ManasRace r = EliteRaces.race(t.id());
                if (r != null) out.add(r);
            }
        }
        return out;
    }

    /** One requirement per rule, weighted by the rule's share of the bar (the shares add up to 100). */
    @Override
    public Map<EvolutionRequirement, Float> getEvolutionRequirements(ManasRaceInstance instance, LivingEntity entity) {
        RaceTemplate t = template();
        if (t == null || t.evolution().isEmpty() || !EliteRacesConfig.enabled()) return Map.of();
        RaceRuleSet set = t.evolution();
        Map<EvolutionRequirement, Float> out = new LinkedHashMap<>();
        for (RaceEvolutionRule rule : set.rules()) out.put(new RuleRequirement(rule), (float) set.sharePercent(rule));
        return out;
    }

    /** Whatever path evolved the player (Tensura's screen, a command), the items of the rules are taken once. */
    @Override
    public void onRaceEvolution(ManasRaceInstance self, LivingEntity entity, ManasRaceInstance next) {
        super.onRaceEvolution(self, entity, next);
        if (entity instanceof ServerPlayer p && EliteRacesConfig.enabled()) RaceEvolution.onEvolved(p, this);
    }
}
