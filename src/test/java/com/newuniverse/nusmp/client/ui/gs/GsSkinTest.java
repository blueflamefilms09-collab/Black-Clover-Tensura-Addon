package com.newuniverse.nusmp.client.ui.gs;

import com.newuniverse.nusmp.client.ui.theme.UiThemeMode;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Why: the skin of the Great Spirit menus follows the user (Parchment and Ink for normal users, Slime Crest for devil users) unless the player forces one,
 * and the new theme value must parse without disturbing the two old ones.
 */
class GsSkinTest {
    @Test
    void autoFollowsTheUser() {
        assertEquals(GsSkin.B, GsSkin.choose(GsSkin.Choice.AUTO, false));
        assertEquals(GsSkin.C, GsSkin.choose(GsSkin.Choice.AUTO, true));
    }

    @Test
    void aForcedChoiceWinsOverTheUser() {
        assertEquals(GsSkin.B, GsSkin.choose(GsSkin.Choice.B, true));
        assertEquals(GsSkin.C, GsSkin.choose(GsSkin.Choice.C, false));
    }

    @Test
    void theChoiceParsesAnyCaseAndFallsBackToAuto() {
        assertEquals(GsSkin.Choice.AUTO, GsSkin.Choice.parse("auto"));
        assertEquals(GsSkin.Choice.B, GsSkin.Choice.parse(" b "));
        assertEquals(GsSkin.Choice.C, GsSkin.Choice.parse("C"));
        assertEquals(GsSkin.Choice.AUTO, GsSkin.Choice.parse("A"));
        assertEquals(GsSkin.Choice.AUTO, GsSkin.Choice.parse(null));
        assertEquals(GsSkin.Choice.AUTO, GsSkin.Choice.parse(""));
    }

    @Test
    void theNewThemeValueIsAppendedAndTheOldOnesStillParse() {
        assertEquals(UiThemeMode.GREAT_SPIRIT, UiThemeMode.parse("great_spirit"));
        assertEquals(UiThemeMode.GREAT_SPIRIT, UiThemeMode.parse(" Great_Spirit "));
        assertEquals(UiThemeMode.CLASSIC, UiThemeMode.parse("classic"));
        assertEquals(UiThemeMode.ANIME, UiThemeMode.parse("anime"));
        assertEquals(UiThemeMode.ANIME, UiThemeMode.parse("great spirit"), "an unknown text is still the default");
        assertEquals(UiThemeMode.ANIME, UiThemeMode.parse(null));
        UiThemeMode[] all = UiThemeMode.values();
        assertEquals(UiThemeMode.ANIME, all[0], "enum entries are only appended");
        assertEquals(UiThemeMode.CLASSIC, all[1]);
        assertEquals(UiThemeMode.GREAT_SPIRIT, all[2]);
    }
}
