package com.ai_autocreate.utils;

import android.content.Context;
import android.os.AsyncTask;

import com.ai_autocreate.R;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class FFmpegRunner {
    private Context context;
    private JSONLogger logger;
    private boolean isFFmpegAvailable;

    public FFmpegRunner(Context context) {
        this.context = context;
        this.logger = new JSONLogger(context);
        this.isFFmpegAvailable = checkFFmpegAvailability();
    }

    public FFmpegResult execute(String command) {
        if (!isFFmpegAvailable) {
            logger.log("FFmpegRunner", "FFmpeg is not available");
            return null;
        }

        try {
            // Parse command
            List<String> commandParts = parseCommand(command);

            // Create process
            ProcessBuilder processBuilder = new ProcessBuilder(commandParts);
            processBuilder.redirectErrorStream(true);

            // Start process
            Process process = processBuilder.start();

            // Read output
            StringBuilder output = new StringBuilder();
            BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
            String line;

            while ((line = reader.readLine()) != null) {
                output.append(line).append("\n");
            }

            // Wait for process to complete
            boolean finished = process.waitFor(30, TimeUnit.SECONDS);

            if (!finished) {
                process.destroyForcibly();
                logger.log("FFmpegRunner", "FFmpeg command timed out: " + command);
                return null;
            }

            int exitCode = process.exitValue();

            return new FFmpegResult(exitCode, output.toString());

        } catch (Exception e) {
            logger.log("FFmpegRunner", "Error executing FFmpeg command: " + e.getMessage());
            return null;
        }
    }

    public void executeAsync(String command, FFmpegCallback callback) {
        new ExecuteFFmpegTask(command, callback).execute();
    }

    private List<String> parseCommand(String command) {
        List<String> parts = new ArrayList<>();
        boolean inQuotes = false;
        StringBuilder currentPart = new StringBuilder();

        for (int i = 0; i < command.length(); i++) {
            char c = command.charAt(i);

            if (c == '"' || c == '\'') {
                inQuotes = !inQuotes;
            } else if (c == ' ' && !inQuotes) {
                if (currentPart.length() > 0) {
                    parts.add(currentPart.toString());
                    currentPart = new StringBuilder();
                }
            } else {
                currentPart.append(c);
            }
        }

        if (currentPart.length() > 0) {
            parts.add(currentPart.toString());
        }

        // Add ffmpeg binary path
        parts.add(0, getFFmpegBinaryPath());

        return parts;
    }

    private String getFFmpegBinaryPath() {
        // Check if FFmpeg binary exists in app directory
        File ffmpegBinary = new File(context.getFilesDir(), "ffmpeg");
        if (ffmpegBinary.exists()) {
            return ffmpegBinary.getAbsolutePath();
        }

        // Check if FFmpeg binary exists in external storage
        ffmpegBinary = new File("/storage/emulated/0/AIAutoCreate/ffmpeg");
        if (ffmpegBinary.exists()) {
            return ffmpegBinary.getAbsolutePath();
        }

        // Default to system ffmpeg (if available)
        return "ffmpeg";
    }

    private boolean checkFFmpegAvailability() {
        try {
            // Try to run ffmpeg -version to check if it's available
            ProcessBuilder processBuilder = new ProcessBuilder(getFFmpegBinaryPath(), "-version");
            Process process = processBuilder.start();

            boolean finished = process.waitFor(5, TimeUnit.SECONDS);

            if (!finished) {
                process.destroyForcibly();
                return false;
            }

            return process.exitValue() == 0;

        } catch (Exception e) {
            logger.log("FFmpegRunner", "Error checking FFmpeg availability: " + e.getMessage());

            // Try to check if FFmpegKit is available
            try {
                Class.forName("com.arthenica.ffmpegkit.FFmpegKit");
                return true;
            } catch (ClassNotFoundException ex) {
                return false;
            }
        }
    }

    public boolean isFFmpegAvailable() {
        return isFFmpegAvailable;
    }

    public String getFFmpegVersion() {
        if (!isFFmpegAvailable) {
            return "Not available";
        }

        try {
            ProcessBuilder processBuilder = new ProcessBuilder(getFFmpegBinaryPath(), "-version");
            Process process = processBuilder.start();

            BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
            String line = reader.readLine();

            if (line != null && line.contains("ffmpeg version")) {
                return line;
            } else {
                return "Unknown version";
            }

        } catch (Exception e) {
            logger.log("FFmpegRunner", "Error getting FFmpeg version: " + e.getMessage());
            return "Error getting version";
        }
    }

    public static class FFmpegResult {
        private int exitCode;
        private String output;

        public FFmpegResult(int exitCode, String output) {
            this.exitCode = exitCode;
            this.output = output;
        }

        public int getExitCode() {
            return exitCode;
        }

        public String getOutput() {
            return output;
        }

        public boolean isSuccess() {
            return exitCode == 0;
        }
    }

    public interface FFmpegCallback {
        void onSuccess(FFmpegResult result);
        void onError(String errorMessage);
    }

    private class ExecuteFFmpegTask extends AsyncTask<Void, String, FFmpegResult> {
        private String command;
        private FFmpegCallback callback;
        private String errorMessage;

        public ExecuteFFmpegTask(String command, FFmpegCallback callback) {
            this.command = command;
            this.callback = callback;
        }
     
        @Override
        protected FFmpegResult doInBackground(Void... params) {
            try {
                return FFmpegRunner.this.execute(command); // حدد اسم الكلاس لتفادي لبس execute()
            } catch (Exception e) {
                errorMessage = e.getMessage();
                return null;
            }
        }

        @Override
        protected void onPostExecute(FFmpegResult result) {
            if (result != null && callback != null) {
                callback.onSuccess(result);
            } else if (callback != null) {
                callback.onError(errorMessage != null ? errorMessage : "Unknown error");
            }
        }
    }
}
