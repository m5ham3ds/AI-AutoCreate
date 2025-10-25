package com.ai_autocreate.utils;

import android.os.Environment;

public class StoragePaths {
    private static final String APP_ROOT = "/storage/emulated/0/AIAutoCreate";

    public static String getAppRootDir() {
        return APP_ROOT;
    }

    public static String getProjectsDir() {
        return APP_ROOT + "/projects";
    }

    public static String getTempDir() {
        return APP_ROOT + "/temp";
    }

    public static String getAgentResultsDir() {
        return APP_ROOT + "/agent_results";
    }

    public static String getConfigDir() {
        return APP_ROOT + "/config";
    }

    public static String getFontsDir() {
        return APP_ROOT + "/fonts";
    }

    public static String getImagesDir() {
        return APP_ROOT + "/images";
    }

    public static String getAudioDir() {
        return APP_ROOT + "/audio";
    }

    public static String getVideosDir() {
        return APP_ROOT + "/videos";
    }

    public static String getScriptsDir() {
        return APP_ROOT + "/scripts";
    }

    public static String getProjectDir(String projectId) {
        return getProjectsDir() + "/" + projectId;
    }

    public static String getProjectFramesDir(String projectId) {
        return getProjectDir(projectId) + "/frames";
    }

    public static String getProjectAudioDir(String projectId) {
        return getProjectDir(projectId) + "/audio";
    }

    public static String getProjectCheckpointsDir(String projectId) {
        return getProjectDir(projectId) + "/checkpoints";
    }

    public static boolean isExternalStorageAvailable() {
        String state = Environment.getExternalStorageState();
        return Environment.MEDIA_MOUNTED.equals(state);
    }

    public static boolean isExternalStorageReadOnly() {
        String state = Environment.getExternalStorageState();
        return Environment.MEDIA_MOUNTED_READ_ONLY.equals(state);
    }
}
