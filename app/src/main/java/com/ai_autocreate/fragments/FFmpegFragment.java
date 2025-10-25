package com.ai_autocreate.fragments;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.text.method.ScrollingMovementMethod;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.ai_autocreate.R;
import com.ai_autocreate.utils.FFmpegRunner;
import com.ai_autocreate.utils.JSONLogger;

import org.json.JSONException;
import org.json.JSONObject;

public class FFmpegFragment extends Fragment {
    private EditText commandEditText;
    private Button runCommandButton;
    private Button presetButton;
    private TextView outputTextView;
    private ProgressBar progressBar;
    private FFmpegRunner ffmpegRunner;
    private JSONLogger logger;

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_ffmpeg, container, false);

        // Initialize views
        commandEditText = view.findViewById(R.id.command_edit_text);
        runCommandButton = view.findViewById(R.id.run_command_button);
        presetButton = view.findViewById(R.id.preset_button);
        outputTextView = view.findViewById(R.id.output_text_view);
        progressBar = view.findViewById(R.id.progress_bar);

        // Initialize components
        ffmpegRunner = new FFmpegRunner(getActivity());
        logger = new JSONLogger(getActivity());

        // Setup output text view
        outputTextView.setMovementMethod(new ScrollingMovementMethod());

        // Setup listeners
        setupListeners();

        return view;
    }

    private void setupListeners() {
        runCommandButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    String command = commandEditText.getText().toString().trim();
                    if (command.isEmpty()) {
                        Toast.makeText(getActivity(), R.string.please_enter_command, Toast.LENGTH_SHORT).show();
                        return;
                    }

                    new RunFFmpegTask().execute(command);
                }
            });

        presetButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    showPresetsDialog();
                }
            });
    }

    private void showPresetsDialog() {
        String[] presets = {
            "Video Conversion",
            "Audio Extraction",
            "Image Sequence to Video",
            "Video to Images",
            "Add Subtitles",
            "Trim Video",
            "Change Resolution",
            "Change Frame Rate"
        };

        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setTitle(R.string.select_preset)
            .setItems(presets, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    String command = getPresetCommand(which);
                    commandEditText.setText(command);
                }
            })
            .show();
    }

    private String getPresetCommand(int presetIndex) {
        switch (presetIndex) {
            case 0: // Video Conversion
                return "-i input.mp4 -c:v libx264 -c:a aac output.mp4";
            case 1: // Audio Extraction
                return "-i input.mp4 -vn -acodec copy output.aac";
            case 2: // Image Sequence to Video
                return "-framerate 24 -i frame_%04d.jpg -c:v libx264 -pix_fmt yuv420p output.mp4";
            case 3: // Video to Images
                return "-i input.mp4 -vf fps=24 frame_%04d.jpg";
            case 4: // Add Subtitles
                return "-i input.mp4 -i subtitles.srt -c copy -c:s mov_text output.mp4";
            case 5: // Trim Video
                return "-i input.mp4 -ss 00:00:10 -t 00:00:30 -c copy output.mp4";
            case 6: // Change Resolution
                return "-i input.mp4 -vf scale=1280:720 -c:a copy output.mp4";
            case 7: // Change Frame Rate
                return "-i input.mp4 -r 30 output.mp4";
            default:
                return "";
        }
    }

    private void appendOutput(String text) {
        outputTextView.append(text + "\n");

        // Auto-scroll to bottom
        final int scrollAmount = outputTextView.getLayout().getLineTop(outputTextView.getLineCount()) - outputTextView.getHeight();
        if (scrollAmount > 0) {
            outputTextView.scrollTo(0, scrollAmount);
        }
    }

    private class RunFFmpegTask extends AsyncTask<String, String, Integer> {
        private String command;
        private String output;

        @Override
        protected void onPreExecute() {
            progressBar.setVisibility(View.VISIBLE);
            runCommandButton.setEnabled(false);
            outputTextView.setText("");
            appendOutput("Running command: " + commandEditText.getText().toString());
            appendOutput("--------------------------------------------------");
        }

        @Override
        protected Integer doInBackground(String... params) {
            command = params[0];

            try {
                // Run FFmpeg command
                FFmpegRunner.FFmpegResult result = ffmpegRunner.execute(command);

                if (result != null) {
                    output = result.getOutput();
                    publishProgress(output);

                    return result.getExitCode();
                } else {
                    publishProgress("Error: FFmpeg execution failed");
                    return -1;
                }
            } catch (Exception e) {
                publishProgress("Error: " + e.getMessage());
                return -1;
            }
        }

        @Override
        protected void onProgressUpdate(String... values) {
            for (String line : values) {
                appendOutput(line);
            }
        }

        @Override
        protected void onPostExecute(Integer exitCode) {
            progressBar.setVisibility(View.GONE);
            runCommandButton.setEnabled(true);

            appendOutput("--------------------------------------------------");

            if (exitCode == 0) {
                appendOutput("Command completed successfully");
                Toast.makeText(getActivity(), R.string.command_completed_successfully, Toast.LENGTH_SHORT).show();
            } else {
                appendOutput("Command failed with exit code: " + exitCode);
                Toast.makeText(getActivity(), R.string.command_failed, Toast.LENGTH_SHORT).show();
            }

            // Log result
            logFFmpegResult(command, exitCode, output);
        }
    }

    private void logFFmpegResult(String command, int exitCode, String output) {
        try {
            JSONObject logEntry = new JSONObject();
            logEntry.put("command", command);
            logEntry.put("exit_code", exitCode);
            logEntry.put("output", output);
            logEntry.put("timestamp", System.currentTimeMillis());

            logger.log("FFmpegFragment", logEntry);
        } catch (JSONException e) {
            logger.log("FFmpegFragment", "Error logging FFmpeg result: " + e.getMessage());
        }
    }
}
