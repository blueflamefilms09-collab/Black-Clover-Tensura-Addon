import com.newuniverse.nusmp.labyrinth.mega.system.SystemText;

import java.util.Map;

/**
 * Why: the English text of every key of the System lives in ONE map in the code (SystemText), so the lang file cannot drift from the code; this prints that map as
 * "key TAB text" lines for tools/system/gen_system_lang.py, which writes the fragment the lead merges into en_us.json. Nothing here is part of the mod.
 */
public final class LangDump {
    public static void main(String[] args) {
        for (Map.Entry<String, String> e : SystemText.all().entrySet()) System.out.println(e.getKey() + "\t" + e.getValue());
    }
}
