package me.m0dii.safecreative.utils;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.ChatColor;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;

public class Utils {
    public static String format(String msg) {
        return ChatColor.translateAlternateColorCodes('&', msg);
    }

    public static String stripColor(Component component) {
        return ChatColor.stripColor(PlainTextComponentSerializer.plainText().serializeOr(component, ""));
    }

    public static void copy(InputStream in, File file) {
        if (in == null) {
            return;
        }

        try {
            OutputStream out = new FileOutputStream(file);

            byte[] buf = new byte[1024];

            int len;

            while((len = in.read(buf)) > 0)
                out.write(buf, 0, len);

            out.close();
            in.close();
        }
        catch(Exception ignored) { }
    }
}
