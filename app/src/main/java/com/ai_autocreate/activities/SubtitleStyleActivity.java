package com.ai_autocreate.activities;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.graphics.Color;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.SeekBar;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.ai_autocreate.R;
import com.ai_autocreate.utils.JSONLogger;
import com.ai_autocreate.utils.StoragePaths;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class SubtitleStyleActivity extends AppCompatActivity {
    private TextView previewTextView;
    private EditText fontNameEditText;
    private SeekBar fontSizeSeekBar;
    private TextView fontSizeValueTextView;
    private SeekBar alphaSeekBar;
    private TextView alphaValueTextView;
    private Spinner fontColorSpinner;
    private Spinner positionSpinner;
    private Spinner alignmentSpinner;
    private CheckBox shadowCheckBox;
    private SeekBar shadowRadiusSeekBar;
    private SeekBar shadowDxSeekBar;
    private SeekBar shadowDySeekBar;
    private Spinner shadowColorSpinner;
    private CheckBox outlineCheckBox;
    private SeekBar outlineWidthSeekBar;
    private Spinner outlineColorSpinner;
    private CheckBox backgroundCheckBox;
    private SeekBar backgroundPaddingSeekBar;
    private Spinner backgroundColorSpinner;
    private CheckBox applyGloballyCheckBox;
    private Button saveButton;
    private Button resetButton;
    private Button previewButton;
    private ProgressBar progressBar;

    private List<SubtitleStyle> stylePresets;
    private ArrayAdapter<String> presetAdapter;
    private SubtitleStyle currentStyle;
    private JSONLogger logger;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_subtitle_style);

        // Initialize views
        previewTextView = findViewById(R.id.preview_text_view);
        fontNameEditText = findViewById(R.id.font_name_edit_text);
        fontSizeSeekBar = findViewById(R.id.font_size_seek_bar);
        fontSizeValueTextView = findViewById(R.id.font_size_value_text_view);
        alphaSeekBar = findViewById(R.id.alpha_seek_bar);
        alphaValueTextView = findViewById(R.id.alpha_value_text_view);
        fontColorSpinner = findViewById(R.id.font_color_spinner);
        positionSpinner = findViewById(R.id.position_spinner);
        alignmentSpinner = findViewById(R.id.alignment_spinner);
        shadowCheckBox = findViewById(R.id.shadow_check_box);
        shadowRadiusSeekBar = findViewById(R.id.shadow_radius_seek_bar);
        shadowDxSeekBar = findViewById(R.id.shadow_dx_seek_bar);
        shadowDySeekBar = findViewById(R.id.shadow_dy_seek_bar);
        shadowColorSpinner = findViewById(R.id.shadow_color_spinner);
        outlineCheckBox = findViewById(R.id.outline_check_box);
        outlineWidthSeekBar = findViewById(R.id.outline_width_seek_bar);
        outlineColorSpinner = findViewById(R.id.outline_color_spinner);
        backgroundCheckBox = findViewById(R.id.background_check_box);
        backgroundPaddingSeekBar = findViewById(R.id.background_padding_seek_bar);
        backgroundColorSpinner = findViewById(R.id.background_color_spinner);
        applyGloballyCheckBox = findViewById(R.id.apply_globally_check_box);
        saveButton = findViewById(R.id.save_button);
        resetButton = findViewById(R.id.reset_button);
        previewButton = findViewById(R.id.preview_button);
        progressBar = findViewById(R.id.progress_bar);

        // Initialize components
        logger = new JSONLogger(this);
        stylePresets = new ArrayList<>();
        currentStyle = new SubtitleStyle();

        // Setup views
        setupSpinners();
        setupSeekBars();

        // Setup listeners
        setupListeners();

        // Load presets
        loadPresets();

        // Load current style
        loadCurrentStyle();

        // Update preview
        updatePreview();
    }

    private void setupSpinners() {
        // Font color spinner
        String[] fontColors = {"White", "Black", "Red", "Green", "Blue", "Yellow", "Cyan", "Magenta", "Custom"};
        ArrayAdapter<String> fontColorAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, fontColors);
        fontColorAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        fontColorSpinner.setAdapter(fontColorAdapter);

        // Position spinner
        String[] positions = {"Top", "Middle", "Bottom"};
        ArrayAdapter<String> positionAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, positions);
        positionAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        positionSpinner.setAdapter(positionAdapter);

        // Alignment spinner
        String[] alignments = {"Left", "Center", "Right"};
        ArrayAdapter<String> alignmentAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, alignments);
        alignmentAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        alignmentSpinner.setAdapter(alignmentAdapter);

        // Shadow color spinner
        String[] shadowColors = {"Black", "White", "Gray", "Red", "Green", "Blue", "Custom"};
        ArrayAdapter<String> shadowColorAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, shadowColors);
        shadowColorAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        shadowColorSpinner.setAdapter(shadowColorAdapter);

        // Outline color spinner
        String[] outlineColors = {"Black", "White", "Gray", "Red", "Green", "Blue", "Custom"};
        ArrayAdapter<String> outlineColorAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, outlineColors);
        outlineColorAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        outlineColorSpinner.setAdapter(outlineColorAdapter);

        // Background color spinner
        String[] backgroundColors = {"Black", "White", "Gray", "Transparent", "Custom"};
        ArrayAdapter<String> backgroundColorAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, backgroundColors);
        backgroundColorAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        backgroundColorSpinner.setAdapter(backgroundColorAdapter);
    }

    private void setupSeekBars() {
        // Font size seek bar
        fontSizeSeekBar.setMax(72 - 12); // 12-72
        fontSizeSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
                @Override
                public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                    int fontSize = 12 + progress;
                    fontSizeValueTextView.setText(fontSize + "sp");
                    if (fromUser) {
                        updatePreview();
                    }
                }

                @Override
                public void onStartTrackingTouch(SeekBar seekBar) {}

                @Override
                public void onStopTrackingTouch(SeekBar seekBar) {}
            });

        // Alpha seek bar
        alphaSeekBar.setMax(100);
        alphaSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
                @Override
                public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                    alphaValueTextView.setText(progress + "%");
                    if (fromUser) {
                        updatePreview();
                    }
                }

                @Override
                public void onStartTrackingTouch(SeekBar seekBar) {}

                @Override
                public void onStopTrackingTouch(SeekBar seekBar) {}
            });

        // Shadow radius seek bar
        shadowRadiusSeekBar.setMax(20);
        shadowRadiusSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
                @Override
                public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                    if (fromUser) {
                        updatePreview();
                    }
                }

                @Override
                public void onStartTrackingTouch(SeekBar seekBar) {}

                @Override
                public void onStopTrackingTouch(SeekBar seekBar) {}
            });

        // Shadow DX seek bar
        shadowDxSeekBar.setMax(20);
        shadowDxSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
                @Override
                public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                    if (fromUser) {
                        updatePreview();
                    }
                }

                @Override
                public void onStartTrackingTouch(SeekBar seekBar) {}

                @Override
                public void onStopTrackingTouch(SeekBar seekBar) {}
            });

        // Shadow DY seek bar
        shadowDySeekBar.setMax(20);
        shadowDySeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
                @Override
                public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                    if (fromUser) {
                        updatePreview();
                    }
                }

                @Override
                public void onStartTrackingTouch(SeekBar seekBar) {}

                @Override
                public void onStopTrackingTouch(SeekBar seekBar) {}
            });

        // Outline width seek bar
        outlineWidthSeekBar.setMax(10);
        outlineWidthSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
                @Override
                public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                    if (fromUser) {
                        updatePreview();
                    }
                }

                @Override
                public void onStartTrackingTouch(SeekBar seekBar) {}

                @Override
                public void onStopTrackingTouch(SeekBar seekBar) {}
            });

        // Background padding seek bar
        backgroundPaddingSeekBar.setMax(20);
        backgroundPaddingSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
                @Override
                public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                    if (fromUser) {
                        updatePreview();
                    }
                }

                @Override
                public void onStartTrackingTouch(SeekBar seekBar) {}

                @Override
                public void onStopTrackingTouch(SeekBar seekBar) {}
            });
    }

    private void setupListeners() {
        fontNameEditText.setOnFocusChangeListener(new View.OnFocusChangeListener() {
                @Override
                public void onFocusChange(View v, boolean hasFocus) {
                    if (!hasFocus) {
                        updatePreview();
                    }
                }
            });

        fontColorSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                @Override
                public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                    updatePreview();
                }

                @Override
                public void onNothingSelected(AdapterView<?> parent) {}
            });

        positionSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                @Override
                public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                    updatePreview();
                }

                @Override
                public void onNothingSelected(AdapterView<?> parent) {}
            });

        alignmentSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                @Override
                public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                    updatePreview();
                }

                @Override
                public void onNothingSelected(AdapterView<?> parent) {}
            });

        shadowCheckBox.setOnCheckedChangeListener(new android.widget.CompoundButton.OnCheckedChangeListener() {
                @Override
                public void onCheckedChanged(android.widget.CompoundButton buttonView, boolean isChecked) {
                    updatePreview();
                }
            });

        shadowColorSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                @Override
                public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                    updatePreview();
                }

                @Override
                public void onNothingSelected(AdapterView<?> parent) {}
            });

        outlineCheckBox.setOnCheckedChangeListener(new android.widget.CompoundButton.OnCheckedChangeListener() {
                @Override
                public void onCheckedChanged(android.widget.CompoundButton buttonView, boolean isChecked) {
                    updatePreview();
                }
            });

        outlineColorSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                @Override
                public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                    updatePreview();
                }

                @Override
                public void onNothingSelected(AdapterView<?> parent) {}
            });

        backgroundCheckBox.setOnCheckedChangeListener(new android.widget.CompoundButton.OnCheckedChangeListener() {
                @Override
                public void onCheckedChanged(android.widget.CompoundButton buttonView, boolean isChecked) {
                    updatePreview();
                }
            });

        backgroundColorSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                @Override
                public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                    updatePreview();
                }

                @Override
                public void onNothingSelected(AdapterView<?> parent) {}
            });

        saveButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    saveStyle();
                }
            });

        resetButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    resetStyle();
                }
            });

        previewButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    updatePreview();
                }
            });
    }

    private void loadPresets() {
        new LoadPresetsTask().execute();
    }

    private void loadCurrentStyle() {
        new LoadCurrentStyleTask().execute();
    }

    private void updatePreview() {
        // Collect current settings
        currentStyle.fontName = fontNameEditText.getText().toString();
        currentStyle.fontSize = 12 + fontSizeSeekBar.getProgress();
        currentStyle.alpha = alphaSeekBar.getProgress() / 100.0f;
        currentStyle.fontColor = getSelectedColor(fontColorSpinner.getSelectedItem().toString());
        currentStyle.position = positionSpinner.getSelectedItem().toString();
        currentStyle.alignment = alignmentSpinner.getSelectedItem().toString();
        currentStyle.shadowEnabled = shadowCheckBox.isChecked();
        currentStyle.shadowRadius = shadowRadiusSeekBar.getProgress();
        currentStyle.shadowDx = shadowDxSeekBar.getProgress() - 10; // -10 to 10
        currentStyle.shadowDy = shadowDySeekBar.getProgress() - 10; // -10 to 10
        currentStyle.shadowColor = getSelectedColor(shadowColorSpinner.getSelectedItem().toString());
        currentStyle.outlineEnabled = outlineCheckBox.isChecked();
        currentStyle.outlineWidth = outlineWidthSeekBar.getProgress();
        currentStyle.outlineColor = getSelectedColor(outlineColorSpinner.getSelectedItem().toString());
        currentStyle.backgroundEnabled = backgroundCheckBox.isChecked();
        currentStyle.backgroundPadding = backgroundPaddingSeekBar.getProgress();
        currentStyle.backgroundColor = getSelectedColor(backgroundColorSpinner.getSelectedItem().toString());
        currentStyle.applyGlobally = applyGloballyCheckBox.isChecked();

        // Apply style to preview
        applyStyleToPreview(currentStyle);
    }

    private String getSelectedColor(String colorName) {
        switch (colorName) {
            case "White": return "#FFFFFF";
            case "Black": return "#000000";
            case "Red": return "#FF0000";
            case "Green": return "#00FF00";
            case "Blue": return "#0000FF";
            case "Yellow": return "#FFFF00";
            case "Cyan": return "#00FFFF";
            case "Magenta": return "#FF00FF";
            case "Gray": return "#808080";
            case "Transparent": return "#00000000";
            default: return "#FFFFFF";
        }
    }

    private void applyStyleToPreview(SubtitleStyle style) {
        previewTextView.setText("Sample subtitle text\nThis is how your subtitles will look");
        previewTextView.setTextSize(style.fontSize);
        previewTextView.setTextColor(parseColor(style.fontColor, style.alpha));

        // Set text alignment
        switch (style.alignment) {
            case "Left":
                previewTextView.setTextAlignment(View.TEXT_ALIGNMENT_TEXT_START);
                break;
            case "Center":
                previewTextView.setTextAlignment(View.TEXT_ALIGNMENT_CENTER);
                break;
            case "Right":
                previewTextView.setTextAlignment(View.TEXT_ALIGNMENT_TEXT_END);
                break;
        }

        // Apply shadow
        if (style.shadowEnabled) {
            previewTextView.setShadowLayer(style.shadowRadius, style.shadowDx, style.shadowDy, parseColor(style.shadowColor, 1.0f));
        } else {
            previewTextView.setShadowLayer(0, 0, 0, Color.TRANSPARENT);
        }

        // Apply background
        if (style.backgroundEnabled) {
            previewTextView.setBackgroundColor(parseColor(style.backgroundColor, 0.8f));
            int padding = style.backgroundPadding * 2; // Convert dp to pixels (simplified)
            previewTextView.setPadding(padding, padding, padding, padding);
        } else {
            previewTextView.setBackgroundColor(Color.TRANSPARENT);
            previewTextView.setPadding(8, 8, 8, 8);
        }
    }

    private int parseColor(String colorString, float alpha) {
        try {
            int color = Color.parseColor(colorString);
            int alphaInt = (int) (alpha * 255);
            return Color.argb(alphaInt, Color.red(color), Color.green(color), Color.blue(color));
        } catch (Exception e) {
            return Color.WHITE;
        }
    }

    private void saveStyle() {
        new SaveStyleTask().execute();
    }

    private void resetStyle() {
        // Reset to default values
        fontNameEditText.setText("Default");
        fontSizeSeekBar.setProgress(16 - 12); // 16sp
        alphaSeekBar.setProgress(100); // 100%
        fontColorSpinner.setSelection(0); // White
        positionSpinner.setSelection(2); // Bottom
        alignmentSpinner.setSelection(1); // Center
        shadowCheckBox.setChecked(false);
        shadowRadiusSeekBar.setProgress(3);
        shadowDxSeekBar.setProgress(10); // 0
        shadowDySeekBar.setProgress(12); // 2
        shadowColorSpinner.setSelection(0); // Black
        outlineCheckBox.setChecked(false);
        outlineWidthSeekBar.setProgress(2);
        outlineColorSpinner.setSelection(0); // Black
        backgroundCheckBox.setChecked(true);
        backgroundPaddingSeekBar.setProgress(4);
        backgroundColorSpinner.setSelection(0); // Black
        applyGloballyCheckBox.setChecked(false);

        updatePreview();
    }

    private class LoadPresetsTask extends AsyncTask<Void, Void, Boolean> {
        @Override
        protected Boolean doInBackground(Void... params) {
            try {
                File presetsFile = new File(StoragePaths.getConfigDir() + "/subtitle_presets.json");

                if (presetsFile.exists()) {
                    String jsonContent = JSONLogger.readFromFile(presetsFile);
                    JSONArray presetsArray = new JSONArray(jsonContent);

                    for (int i = 0; i < presetsArray.length(); i++) {
                        JSONObject presetObj = presetsArray.getJSONObject(i);
                        SubtitleStyle preset = new SubtitleStyle();
                        preset.fromJSON(presetObj);
                        stylePresets.add(preset);
                    }
                } else {
                    // Create default presets
                    createDefaultPresets();
                }

                return true;
            } catch (Exception e) {
                logger.log("SubtitleStyle", "Error loading presets: " + e.getMessage());
                return false;
            }
        }

        @Override
        protected void onPostExecute(Boolean success) {
            if (!success) {
                Toast.makeText(SubtitleStyleActivity.this, R.string.error_loading_presets, Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void createDefaultPresets() {
        // Default style
        SubtitleStyle defaultStyle = new SubtitleStyle();
        defaultStyle.name = "Default";
        defaultStyle.fontName = "Default";
        defaultStyle.fontSize = 16;
        defaultStyle.alpha = 1.0f;
        defaultStyle.fontColor = "#FFFFFF";
        defaultStyle.position = "Bottom";
        defaultStyle.alignment = "Center";
        defaultStyle.shadowEnabled = true;
        defaultStyle.shadowRadius = 3;
        defaultStyle.shadowDx = 0;
        defaultStyle.shadowDy = 2;
        defaultStyle.shadowColor = "#000000";
        defaultStyle.outlineEnabled = false;
        defaultStyle.backgroundEnabled = true;
        defaultStyle.backgroundPadding = 4;
        defaultStyle.backgroundColor = "#000000";
        stylePresets.add(defaultStyle);

        // Clean style
        SubtitleStyle cleanStyle = new SubtitleStyle();
        cleanStyle.name = "Clean";
        cleanStyle.fontName = "Default";
        cleanStyle.fontSize = 18;
        cleanStyle.alpha = 1.0f;
        cleanStyle.fontColor = "#FFFFFF";
        cleanStyle.position = "Bottom";
        cleanStyle.alignment = "Center";
        cleanStyle.shadowEnabled = false;
        cleanStyle.outlineEnabled = false;
        cleanStyle.backgroundEnabled = false;
        stylePresets.add(cleanStyle);

        // Bold style
        SubtitleStyle boldStyle = new SubtitleStyle();
        boldStyle.name = "Bold";
        boldStyle.fontName = "Default";
        boldStyle.fontSize = 20;
        boldStyle.alpha = 1.0f;
        boldStyle.fontColor = "#FFFF00";
        boldStyle.position = "Bottom";
        boldStyle.alignment = "Center";
        boldStyle.shadowEnabled = true;
        boldStyle.shadowRadius = 5;
        boldStyle.shadowDx = 0;
        boldStyle.shadowDy = 3;
        boldStyle.shadowColor = "#000000";
        boldStyle.outlineEnabled = true;
        boldStyle.outlineWidth = 2;
        boldStyle.outlineColor = "#000000";
        boldStyle.backgroundEnabled = false;
        stylePresets.add(boldStyle);
    }

    private class LoadCurrentStyleTask extends AsyncTask<Void, Void, Boolean> {
        @Override
        protected Boolean doInBackground(Void... params) {
            try {
                File styleFile = new File(StoragePaths.getConfigDir() + "/subtitle_style_default.json");

                if (styleFile.exists()) {
                    String jsonContent = JSONLogger.readFromFile(styleFile);
                    JSONObject styleObj = new JSONObject(jsonContent);
                    currentStyle.fromJSON(styleObj);
                    return true;
                } else {
                    // Use first preset as default
                    if (!stylePresets.isEmpty()) {
                        currentStyle = stylePresets.get(0);
                        return true;
                    }
                }

                return false;
            } catch (Exception e) {
                logger.log("SubtitleStyle", "Error loading current style: " + e.getMessage());
                return false;
            }
        }

        @Override
        protected void onPostExecute(Boolean success) {
            if (success) {
                // Update UI with current style
                updateUIFromStyle(currentStyle);
                updatePreview();
            }
        }
    }

    private void updateUIFromStyle(SubtitleStyle style) {
        fontNameEditText.setText(style.fontName);
        fontSizeSeekBar.setProgress(style.fontSize - 12);
        fontSizeValueTextView.setText(style.fontSize + "sp");
        alphaSeekBar.setProgress((int) (style.alpha * 100));
        alphaValueTextView.setText((int) (style.alpha * 100) + "%");

        // Set spinners (simplified)
        fontColorSpinner.setSelection(0); // Default to first item
        positionSpinner.setSelection(2); // Default to Bottom
        alignmentSpinner.setSelection(1); // Default to Center

        shadowCheckBox.setChecked(style.shadowEnabled);
        shadowRadiusSeekBar.setProgress(style.shadowRadius);
        shadowDxSeekBar.setProgress(style.shadowDx + 10);
        shadowDySeekBar.setProgress(style.shadowDy + 10);

        outlineCheckBox.setChecked(style.outlineEnabled);
        outlineWidthSeekBar.setProgress(style.outlineWidth);

        backgroundCheckBox.setChecked(style.backgroundEnabled);
        backgroundPaddingSeekBar.setProgress(style.backgroundPadding);

        applyGloballyCheckBox.setChecked(style.applyGlobally);
    }

    private class SaveStyleTask extends AsyncTask<Void, Void, Boolean> {
        @Override
        protected void onPreExecute() {
            progressBar.setVisibility(View.VISIBLE);
            saveButton.setEnabled(false);
        }

        @Override
        protected Boolean doInBackground(Void... params) {
            try {
                // Save current style
                File styleFile = new File(StoragePaths.getConfigDir() + "/subtitle_style_default.json");
                JSONLogger.writeToFile(styleFile, currentStyle.toJSON().toString());

                // Save presets
                JSONArray presetsArray = new JSONArray();
                for (SubtitleStyle preset : stylePresets) {
                    presetsArray.put(preset.toJSON());
                }

                File presetsFile = new File(StoragePaths.getConfigDir() + "/subtitle_presets.json");
                JSONLogger.writeToFile(presetsFile, presetsArray.toString());

                return true;
            } catch (Exception e) {
                logger.log("SubtitleStyle", "Error saving style: " + e.getMessage());
                return false;
            }
        }

        @Override
        protected void onPostExecute(Boolean success) {
            progressBar.setVisibility(View.GONE);
            saveButton.setEnabled(true);

            if (success) {
                Toast.makeText(SubtitleStyleActivity.this, R.string.style_saved, Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(SubtitleStyleActivity.this, R.string.error_saving_style, Toast.LENGTH_SHORT).show();
            }
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.subtitle_style, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();

        if (id == android.R.id.home) {
            finish();
            return true;
        } else if (id == R.id.action_save_preset) {
            showSavePresetDialog();
            return true;
        } else if (id == R.id.action_load_preset) {
            showLoadPresetDialog();
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    private void showSavePresetDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_save_preset, null);

        final EditText nameEdit = dialogView.findViewById(R.id.preset_name_edit_text);

        builder.setTitle(R.string.save_preset)
            .setView(dialogView)
            .setPositiveButton(R.string.save, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    String name = nameEdit.getText().toString().trim();
                    if (!name.isEmpty()) {
                        savePreset(name);
                    }
                }
            })
            .setNegativeButton(R.string.cancel, null)
            .show();
    }

    private void showLoadPresetDialog() {
        String[] presetNames = new String[stylePresets.size()];
        for (int i = 0; i < stylePresets.size(); i++) {
            presetNames[i] = stylePresets.get(i).name;
        }

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(R.string.load_preset)
            .setItems(presetNames, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    loadPreset(which);
                }
            })
            .setNegativeButton(R.string.cancel, null)
            .show();
    }

    private void savePreset(String name) {
        SubtitleStyle newPreset = new SubtitleStyle();
        newPreset.name = name;
        newPreset.copyFrom(currentStyle);

        stylePresets.add(newPreset);
        new SaveStyleTask().execute();
        Toast.makeText(this, R.string.preset_saved, Toast.LENGTH_SHORT).show();
    }

    private void loadPreset(int index) {
        currentStyle.copyFrom(stylePresets.get(index));
        updateUIFromStyle(currentStyle);
        updatePreview();
        Toast.makeText(this, R.string.preset_loaded, Toast.LENGTH_SHORT).show();
    }

    public static class SubtitleStyle {
        public String name = "Default";
        public String fontName = "Default";
        public int fontSize = 16;
        public float alpha = 1.0f;
        public String fontColor = "#FFFFFF";
        public String position = "Bottom";
        public String alignment = "Center";
        public boolean shadowEnabled = true;
        public int shadowRadius = 3;
        public int shadowDx = 0;
        public int shadowDy = 2;
        public String shadowColor = "#000000";
        public boolean outlineEnabled = false;
        public int outlineWidth = 2;
        public String outlineColor = "#000000";
        public boolean backgroundEnabled = true;
        public int backgroundPadding = 4;
        public String backgroundColor = "#000000";
        public boolean applyGlobally = false;

        public void copyFrom(SubtitleStyle other) {
            this.name = other.name;
            this.fontName = other.fontName;
            this.fontSize = other.fontSize;
            this.alpha = other.alpha;
            this.fontColor = other.fontColor;
            this.position = other.position;
            this.alignment = other.alignment;
            this.shadowEnabled = other.shadowEnabled;
            this.shadowRadius = other.shadowRadius;
            this.shadowDx = other.shadowDx;
            this.shadowDy = other.shadowDy;
            this.shadowColor = other.shadowColor;
            this.outlineEnabled = other.outlineEnabled;
            this.outlineWidth = other.outlineWidth;
            this.outlineColor = other.outlineColor;
            this.backgroundEnabled = other.backgroundEnabled;
            this.backgroundPadding = other.backgroundPadding;
            this.backgroundColor = other.backgroundColor;
            this.applyGlobally = other.applyGlobally;
        }

        public JSONObject toJSON() throws JSONException {
            JSONObject json = new JSONObject();
            json.put("name", name);
            json.put("font_name", fontName);
            json.put("font_size", fontSize);
            json.put("alpha", alpha);
            json.put("font_color", fontColor);
            json.put("position", position);
            json.put("alignment", alignment);
            json.put("shadow_enabled", shadowEnabled);
            json.put("shadow_radius", shadowRadius);
            json.put("shadow_dx", shadowDx);
            json.put("shadow_dy", shadowDy);
            json.put("shadow_color", shadowColor);
            json.put("outline_enabled", outlineEnabled);
            json.put("outline_width", outlineWidth);
            json.put("outline_color", outlineColor);
            json.put("background_enabled", backgroundEnabled);
            json.put("background_padding", backgroundPadding);
            json.put("background_color", backgroundColor);
            json.put("apply_globally", applyGlobally);
            return json;
        }

        public void fromJSON(JSONObject json) throws JSONException {
            name = json.optString("name", "Default");
            fontName = json.optString("font_name", "Default");
            fontSize = json.optInt("font_size", 16);
            alpha = (float) json.optDouble("alpha", 1.0);
            fontColor = json.optString("font_color", "#FFFFFF");
            position = json.optString("position", "Bottom");
            alignment = json.optString("alignment", "Center");
            shadowEnabled = json.optBoolean("shadow_enabled", true);
            shadowRadius = json.optInt("shadow_radius", 3);
            shadowDx = json.optInt("shadow_dx", 0);
            shadowDy = json.optInt("shadow_dy", 2);
            shadowColor = json.optString("shadow_color", "#000000");
            outlineEnabled = json.optBoolean("outline_enabled", false);
            outlineWidth = json.optInt("outline_width", 2);
            outlineColor = json.optString("outline_color", "#000000");
            backgroundEnabled = json.optBoolean("background_enabled", true);
            backgroundPadding = json.optInt("background_padding", 4);
            backgroundColor = json.optString("background_color", "#000000");
            applyGlobally = json.optBoolean("apply_globally", false);
        }
    }
}
