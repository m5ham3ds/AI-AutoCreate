package com.ai_autocreate.agents;

import android.content.Context;
import android.os.Environment;
import android.os.StatFs;

import com.ai_autocreate.utils.JSONLogger;
import com.ai_autocreate.utils.StoragePaths;

import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONArray; // CHANGED: add missing import

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class SanityCheckAgent {
    private Context context;
    private JSONLogger logger;

    public SanityCheckAgent(Context context) {
        this.context = context;
        this.logger = new JSONLogger(context);
    }

    public JSONObject runCheck() {
        JSONObject result = new JSONObject();
        List<String> errors = new ArrayList<>();
        List<String> warnings = new ArrayList<>();

        try {
            result.put("agent", "SanityCheckAgent");
            result.put("timestamp", System.currentTimeMillis());

            // Check 1: Storage space
            JSONObject storageCheck = checkStorageSpace();
            result.put("storage_check", storageCheck);

            if (!storageCheck.optBoolean("passed", false)) {
                errors.add(storageCheck.optString("message", "Storage check failed"));
            }

            // Check 2: Permissions
            JSONObject permissionsCheck = checkPermissions();
            result.put("permissions_check", permissionsCheck);

            if (!permissionsCheck.optBoolean("passed", false)) {
                errors.add(permissionsCheck.optString("message", "Permissions check failed"));
            }

            // Check 3: Memory
            JSONObject memoryCheck = checkMemory();
            result.put("memory_check", memoryCheck);

            if (!memoryCheck.optBoolean("passed", false)) {
                warnings.add(memoryCheck.optString("message", "Memory check warning"));
            }

            // Check 4: FFmpeg availability
            JSONObject ffmpegCheck = checkFFmpeg();
            result.put("ffmpeg_check", ffmpegCheck);

            if (!ffmpegCheck.optBoolean("passed", false)) {
                warnings.add(ffmpegCheck.optString("message", "FFmpeg check warning"));
            }

            // Check 5: Directory structure
            JSONObject directoriesCheck = checkDirectories();
            result.put("directories_check", directoriesCheck);

            if (!directoriesCheck.optBoolean("passed", false)) {
                errors.add(directoriesCheck.optString("message", "Directories check failed"));
            }

            // Overall result
            boolean passed = errors.isEmpty();
            result.put("passed", passed);
            result.put("errors", new JSONArray(errors));
            result.put("warnings", new JSONArray(warnings));

            if (passed) {
                result.put("message", "All sanity checks passed");
            } else {
                result.put("message", "Sanity check failed with " + errors.size() + " error(s)");
            }

            // Log result
            logResult(result);

            return result;

        } catch (JSONException e) {
            logger.log("SanityCheckAgent", "Error running sanity check: " + e.getMessage());

            try {
                result.put("passed", false);
                result.put("message", "Error running sanity check: " + e.getMessage());
                return result;
            } catch (JSONException ex) {
                return null;
            }
        }
    }

    private JSONObject checkStorageSpace() {
        JSONObject result = new JSONObject();

        try {
            result.put("check", "storage_space");

            // Get available storage space
            File storageDir = Environment.getExternalStorageDirectory();
            StatFs stat = new StatFs(storageDir.getPath());
            long availableBytes = stat.getAvailableBlocks() * stat.getBlockSize();
            long availableMB = availableBytes / (1024 * 1024);

            result.put("available_mb", availableMB);

            // Check if we have at least 500MB
            boolean passed = availableMB >= 500;
            result.put("passed", passed);

            if (passed) {
                result.put("message", "Storage space check passed (" + availableMB + "MB available)");
            } else {
                result.put("message", "Insufficient storage space (" + availableMB + "MB available, 500MB required)");
            }

        } catch (JSONException e) {
            try {
                result.put("check", "storage_space");
                result.put("passed", false);
                result.put("message", "Error checking storage space: " + e.getMessage());
            } catch (JSONException ex) {
                // Ignore
            }
        }

        return result;
    }

    private JSONObject checkPermissions() {
        JSONObject result = new JSONObject();

        try {
            result.put("check", "permissions");

            // Check if we have write permission to external storage
            boolean canWrite = Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED);
            result.put("can_write_external", canWrite);

            // Check if we can create directories
            File testDir = new File(StoragePaths.getAppRootDir() + "/test");
            boolean canCreateDirs = testDir.mkdirs() || testDir.exists();
            result.put("can_create_directories", canCreateDirs);

            // Clean up test directory
            if (testDir.exists()) {
                testDir.delete();
            }

            boolean passed = canWrite && canCreateDirs;
            result.put("passed", passed);

            if (passed) {
                result.put("message", "Permissions check passed");
            } else {
                result.put("message", "Missing required permissions");
            }

        } catch (JSONException e) {
            try {
                result.put("check", "permissions");
                result.put("passed", false);
                result.put("message", "Error checking permissions: " + e.getMessage());
            } catch (JSONException ex) {
                // Ignore
            }
        }

        return result;
    }

    private JSONObject checkMemory() {
        JSONObject result = new JSONObject();

        try {
            result.put("check", "memory");

            // Get available memory
            Runtime runtime = Runtime.getRuntime();
            long maxMemory = runtime.maxMemory();
            long totalMemory = runtime.totalMemory();
            long freeMemory = runtime.freeMemory();
            long availableMemory = maxMemory - totalMemory + freeMemory;
            long availableMB = availableMemory / (1024 * 1024);

            result.put("available_mb", availableMB);
            result.put("max_mb", maxMemory / (1024 * 1024));

            // Check if we have at least 200MB
            boolean passed = availableMB >= 200;
            result.put("passed", passed);

            if (passed) {
                result.put("message", "Memory check passed (" + availableMB + "MB available)");
            } else {
                result.put("message", "Low memory (" + availableMB + "MB available, 200MB recommended)");
            }

        } catch (JSONException e) {
            try {
                result.put("check", "memory");
                result.put("passed", false);
                result.put("message", "Error checking memory: " + e.getMessage());
            } catch (JSONException ex) {
                // Ignore
            }
        }

        return result;
    }

    private JSONObject checkFFmpeg() {
        JSONObject result = new JSONObject();

        try {
            result.put("check", "ffmpeg");

            // Check if FFmpeg binary exists
            File ffmpegBinary = new File("/data/data/com.ai_autocreate/files/ffmpeg");
            boolean ffmpegExists = ffmpegBinary.exists();
            result.put("ffmpeg_exists", ffmpegExists);

            // Check if FFmpegKit is available
            boolean ffmpegKitAvailable = false;
            try {
                Class.forName("com.arthenica.ffmpegkit.FFmpegKit");
                ffmpegKitAvailable = true;
            } catch (ClassNotFoundException e) {
                ffmpegKitAvailable = false;
            }
            result.put("ffmpeg_kit_available", ffmpegKitAvailable);

            // Check if libass is available
            boolean libassAvailable = false;
            try {
                Class.forName("com.arthenica.ffmpegkit.FFmpegKitConfig");
                libassAvailable = true;
            } catch (ClassNotFoundException e) {
                libassAvailable = false;
            }
            result.put("libass_available", libassAvailable);

            boolean passed = ffmpegExists || ffmpegKitAvailable;
            result.put("passed", passed);

            if (passed) {
                result.put("message", "FFmpeg check passed");
            } else {
                result.put("message", "FFmpeg not available");
            }

        } catch (JSONException e) {
            try {
                result.put("check", "ffmpeg");
                result.put("passed", false);
                result.put("message", "Error checking FFmpeg: " + e.getMessage());
            } catch (JSONException ex) {
                // Ignore
            }
        }

        return result;
    }

    private JSONObject checkDirectories() {
        JSONObject result = new JSONObject();

        try {
            result.put("check", "directories");

            // Check if all required directories exist or can be created
            String[] requiredDirs = {
                StoragePaths.getAppRootDir(),
                StoragePaths.getProjectsDir(),
                StoragePaths.getTempDir(),
                StoragePaths.getAgentResultsDir(),
                StoragePaths.getConfigDir(),
                StoragePaths.getFontsDir()
            };

            boolean allDirsExist = true;
            for (String dirPath : requiredDirs) {
                File dir = new File(dirPath);
                if (!dir.exists() && !dir.mkdirs()) {
                    allDirsExist = false;
                    break;
                }
            }

            result.put("all_directories_exist", allDirsExist);
            result.put("passed", allDirsExist);

            if (allDirsExist) {
                result.put("message", "Directory structure check passed");
            } else {
                result.put("message", "Failed to create required directories");
            }

        } catch (JSONException e) {
            try {
                result.put("check", "directories");
                result.put("passed", false);
                result.put("message", "Error checking directories: " + e.getMessage());
            } catch (JSONException ex) {
                // Ignore
            }
        }

        return result;
    }

    private void logResult(JSONObject result) {
        try {
            File logFile = new File(StoragePaths.getAgentResultsDir() + "/SanityCheckAgent/sanity_check.json");
            JSONLogger.writeToFile(logFile, result.toString());
        } catch (Exception e) {
            logger.log("SanityCheckAgent", "Error logging result: " + e.getMessage());
        }
    }
}
