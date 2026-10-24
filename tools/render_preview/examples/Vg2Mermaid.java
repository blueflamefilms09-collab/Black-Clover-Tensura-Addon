package examples;

import com.newuniverse.nusmp.book.ModeArmorValkyrieRules.Form;
import com.newuniverse.nusmp.book.ModeArmorValkyrieRules.LanceMode;

/** Valkyrie armour v2, the MERMAID look (scaled tail, shell top, horn), the lance beside the tail. */
public final class Vg2Mermaid {
    private Vg2Mermaid() {}

    public static void register() { Vg2Common.install(true, Form.MERMAID, LanceMode.ATTACHED); }
}
