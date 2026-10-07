package com.newuniverse.nusmp.book;

import com.newuniverse.nusmp.blackclover.MagicType;
import com.newuniverse.nusmp.vfx.VfxShape;
import io.github.manasmods.tensura.damage.TensuraDamageTypes;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.damagesource.DamageType;

import java.util.List;

/** 0.54 Demon Light Magic: black light that blinds, burns the underworld, strikes the spirit and cuts through barriers. */
public class DemonLightBook extends GrimoireBook {
    static final int COLOR = 0xFFFF3AD0;

    private final List<BookPage> pages = List.of(
            BookPage.starter("demon_light_strike", "Demon Light Strike", ElementBook.bolt(8f, 1.6, 0.5, 14, true, 0, VfxShape.DEMON_LIGHT_FX1, null, ElementBook.NONE)),
            // 0.54: the real book (wiki spells and the pages that suit black light), appended after the first page
            BookPage.starter("light_sword_of_judgment", "Demon Light Magic: Light Sword of Judgment", DemonLightArts::swordOfJudgment).withAnim("out"),
            BookPage.starter("blinding_flash", "Demon Light Magic: Blinding Flash", ElementBook.nova(3, 7, false, VfxShape.DEMON_LIGHT_FX3,
                    DemonLightArts.blinding(100))),
            BookPage.starter("bright_judgment_whip", "Demon Light Magic: Bright Judgment Whip", DemonLightArts::judgmentWhip).withAnim("sweep"),
            BookPage.mid("prism_breaker", "Demon Light Magic: Prism Breaker", DemonLightArts::prismBreaker),
            BookPage.mid("purging_light", "Demon Light Magic: Purging Light", DemonLightArts::purgingLight),
            BookPage.mid("light_prison", "Demon Light Magic: Light Prison", DemonLightArts::lightPrison),
            BookPage.mid("light_step", "Demon Light Magic: Light Step", DemonLightArts.lightStep()).withAnim("out"),
            BookPage.mid("veil_of_black_light", "Demon Light Magic: Veil of Black Light", DemonLightArts::veil),
            BookPage.zone("eclipse_domain", "Demon Light Magic: Eclipse Domain", DemonLightArts::eclipseDomain),
            BookPage.signature("light_shaft_of_divine_punishment", "Demon Light Magic: Light Shaft of Divine Punishment", DemonLightArts::divineShaft).withAnim("signature"),
            BookPage.daily("demon_dawn", "Demon Light Magic: Dawn of the Demon Light", DemonLightArts::demonDawn).withCooldown(12000));

    public DemonLightBook() { super(MagicType.DEMON_LIGHT, COLOR); }
    @Override protected List<BookPage> familyPages() { return pages; }
    @Override public ResourceKey<DamageType> damageType() { return TensuraDamageTypes.MAGIC_GENERIC; }
}
