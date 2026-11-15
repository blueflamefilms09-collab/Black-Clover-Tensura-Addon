package com.newuniverse.nusmp.skill;

import com.newuniverse.nusmp.greed.GreedService;
import io.github.manasmods.manascore.skill.api.ManasSkillInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;

/**
 * Lord of Gluttonous Greed (Ultimate skill, Berserk of Gluttony): the skill of the Black Sword Greed's wielder, given when the sword binds to them and taken when it leaves them ({@code greed.GreedGrantService}). Three modes
 * switched with the skill system's mode key: the Secret Technique of the form in hand (it pays a share of the devoured stats), Half-Starvation (double devouring, hunger 2.5 times faster) and the Offer (a tenth of the
 * magicule becomes stored EP and satiation). All the work is {@code greed.GreedService}, on the server; this class is the registration and the names.
 */
public class LordOfGluttonousGreedSkill extends NUSkill {
    static final SkillText TEXT = SkillText.of("lord_of_gluttonous_greed", "Lord of Gluttonous Greed",
            "Ultimate. The skill of the Black Sword Greed: devour what you kill (stats, satiation, skills into the Gluttony Vault). Modes: Secret Technique (pays a share of the devoured stats for the technique of the form in hand), Half-Starvation (double devouring, faster hunger), Offer (a tenth of your magicule for stored EP and satiation).");

    private static final String[] IDS = {"secret_technique", "half_starvation", "offer"};
    private static final String[] NAMES = {"Secret Technique", "Half-Starvation", "Offer"};

    public LordOfGluttonousGreedSkill() { super(SkillType.ULTIMATE, TEXT, true); }

    @Override public int getModes(ManasSkillInstance instance) { return IDS.length; }

    @Override
    public int nextMode(LivingEntity entity, ManasSkillInstance instance, int mode, boolean reverse) {
        return Math.floorMod(mode + (reverse ? -1 : 1), IDS.length);
    }

    @Override
    public String getModeId(ManasSkillInstance instance, int mode) { return "greed." + IDS[Math.floorMod(mode, IDS.length)]; }

    @Override
    public Component getModeName(ManasSkillInstance instance, int mode) {
        int m = Math.floorMod(mode, IDS.length);
        return SkillMsg.mode("greed." + IDS[m], NAMES[m]);
    }

    @Override
    public void onPressed(ManasSkillInstance instance, LivingEntity entity, int key, int mode) {
        if (!(entity instanceof ServerPlayer p) || !p.isAlive()) return;
        switch (Math.floorMod(mode, IDS.length)) {
            case 0 -> GreedService.secretTechnique(p);
            case 1 -> GreedService.toggleHalf(p);
            default -> GreedService.offer(p);
        }
    }
}
