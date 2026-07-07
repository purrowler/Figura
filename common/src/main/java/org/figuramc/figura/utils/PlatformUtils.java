package org.figuramc.figura.utils;

import dev.architectury.injectables.annotations.ExpectPlatform;

import java.io.FileNotFoundException;
import java.io.InputStream;
import java.nio.file.Path;

public class PlatformUtils {

    @ExpectPlatform
    public static Path getGameDir() {
        try {
            return (Path) Class.forName("org.figuramc.figura.utils.fabric.PlatformUtilsImpl")
                .getMethod("getGameDir").invoke(null);
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException(e);
        }
    }

    @ExpectPlatform
    public static String getFiguraModVersionString(){
        try {
            return (String) Class.forName("org.figuramc.figura.utils.fabric.PlatformUtilsImpl")
                .getMethod("getFiguraModVersionString").invoke(null);
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException(e);
        }
    }

    @ExpectPlatform
    public static Path getConfigDir() {
        try {
            return (Path) Class.forName("org.figuramc.figura.utils.fabric.PlatformUtilsImpl")
                .getMethod("getConfigDir").invoke(null);
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException(e);
        }
    }

    @ExpectPlatform
    public static boolean isModLoaded(String modId) {
        try {
            return (boolean) Class.forName("org.figuramc.figura.utils.fabric.PlatformUtilsImpl")
                .getMethod("isModLoaded", String.class).invoke(null, modId);
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException(e);
        }
    }

    @ExpectPlatform
    public static String getModVersion(String modId) {
        try {
            return (String) Class.forName("org.figuramc.figura.utils.fabric.PlatformUtilsImpl")
                .getMethod("getModVersion", String.class).invoke(null, modId);
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException(e);
        }
    }

    public static int compareVersionTo(String v1, String v2) {
        if(v1 == null)
            return 1;
        String[] v1Parts = v1.split("[+,_]")[0].split("\\.");
        String[] v2Parts = v2.split("[+,_]")[0].split("\\.");
        int length = Math.max(v1Parts.length, v2Parts.length);
        for(int i = 0; i < length; i++) {
            int v1Part = i < v1Parts.length ?
                    Integer.parseInt(v1Parts[i]) : 0;
            int v2Part = i < v2Parts.length ?
                    Integer.parseInt(v2Parts[i]) : 0;
            if(v1Part < v2Part)
                return -1;
            if(v1Part > v2Part)
                return 1;
        }
        return 0;
    }

    public enum ModLoader {
        FORGE,
        FABRIC
    }

    @ExpectPlatform
    public static ModLoader getModLoader(){
        try {
            return (ModLoader) Class.forName("org.figuramc.figura.utils.fabric.PlatformUtilsImpl")
                .getMethod("getModLoader").invoke(null);
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException(e);
        }
    }

    @ExpectPlatform
    public static InputStream loadFileFromRoot(String file) throws FileNotFoundException {
        try {
            return (InputStream) Class.forName("org.figuramc.figura.utils.fabric.PlatformUtilsImpl")
                .getMethod("loadFileFromRoot", String.class).invoke(null, file);
        } catch (java.lang.reflect.InvocationTargetException e) {
            if (e.getCause() instanceof FileNotFoundException fnfe) throw fnfe;
            throw new RuntimeException(e);
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException(e);
        }
    }
}
