package com.ai_autocreate.activities;

import android.app.AlertDialog; // keep or change to support.v7 AlertDialog if you prefer
import android.content.DialogInterface;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.ai_autocreate.R;
import com.ai_autocreate.agents.OrchestratorAgent;
import com.ai_autocreate.agents.SanityCheckAgent;
import com.ai_autocreate.agents.VideoAnalyzerAgent;
import com.ai_autocreate.agents.ScriptGeneratorAgent;
import com.ai_autocreate.agents.ImageInterpreterAgent;
import com.ai_autocreate.agents.AudioContextAgent;
import com.ai_autocreate.agents.ModelValidatorAgent;
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

public class AgentPlaygroundActivity extends AppCompatActivity {
    private Spinner agentSpinner;
    private EditText inputEditText;
    private Button testButton;
    private Button testAllButton;
    private Button clearButton;
    private ProgressBar progressBar;
    private TextView outputTextView;
    private TextView statusTextView;

    private JSONLogger logger;
    private List<AgentWrapper> agents;
    private ArrayAdapter<String> agentAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_agent_playground);

        // Initialize views (CHANGED: casts added for compatibility)
        agentSpinner = (Spinner) findViewById(R.id.agent_spinner); // CHANGED
        inputEditText = (EditText) findViewById(R.id.input_edit_text); // CHANGED
        testButton = (Button) findViewById(R.id.test_button); // CHANGED
        testAllButton = (Button) findViewById(R.id.test_all_button); // CHANGED
        clearButton = (Button) findViewById(R.id.clear_button); // CHANGED
        progressBar = (ProgressBar) findViewById(R.id.progress_bar); // CHANGED
        outputTextView = (TextView) findViewById(R.id.output_text_view); // CHANGED
        statusTextView = (TextView) findViewById(R.id.status_text_view); // CHANGED

        // Initialize components
        logger = new JSONLogger(this);
        initializeAgents();

        // Setup spinner (CHANGED: explicit generic type)
        agentAdapter = new ArrayAdapter<String>(this, android.R.layout.simple_spinner_item); // CHANGED
        agentAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);

        for (AgentWrapper agent : agents) {
            agentAdapter.add(agent.name);
        }

        agentSpinner.setAdapter(agentAdapter);

        // Setup listeners
        setupListeners();
    }

    private void initializeAgents() {
        agents = new ArrayList<AgentWrapper>();
        agents.add(new AgentWrapper("Sanity Check Agent", new SanityCheckAgent(this)));
        agents.add(new AgentWrapper("Video Analyzer Agent", new VideoAnalyzerAgent(this)));
        agents.add(new AgentWrapper("Script Generator Agent", new ScriptGeneratorAgent(this)));
        agents.add(new AgentWrapper("Image Interpreter Agent", new ImageInterpreterAgent(this)));
        agents.add(new AgentWrapper("Audio Context Agent", new AudioContextAgent(this)));
        agents.add(new AgentWrapper("Model Validator Agent", new ModelValidatorAgent(this)));
        agents.add(new AgentWrapper("Orchestrator Agent", new OrchestratorAgent(this)));
    }

    private void setupListeners() {
        agentSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                @Override
                public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                    AgentWrapper selectedAgent = agents.get(position);
                    inputEditText.setHint("Enter input for " + selectedAgent.name);
                }

                @Override
                public void onNothingSelected(AdapterView<?> parent) {
                    // Do nothing
                }
            });

        testButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    testSelectedAgent();
                }
            });

        testAllButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    testAllAgents();
                }
            });

        clearButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    clearOutput();
                }
            });
    }

    private void testSelectedAgent() {
        String input = inputEditText.getText().toString().trim();
        if (input.isEmpty()) {
            Toast.makeText(this, R.string.please_enter_input, Toast.LENGTH_SHORT).show();
            return;
        }

        int selectedPosition = agentSpinner.getSelectedItemPosition();
        AgentWrapper selectedAgent = agents.get(selectedPosition);

        new TestAgentTask(selectedAgent, input).execute();
    }

    private void testAllAgents() {
        String input = inputEditText.getText().toString().trim();
        if (input.isEmpty()) {
            Toast.makeText(this, R.string.please_enter_input, Toast.LENGTH_SHORT).show();
            return;
        }

        new TestAllAgentsTask(input).execute();
    }

    private void clearOutput() {
        outputTextView.setText("");
        statusTextView.setText("");
    }

    private void appendOutput(String text) {
        outputTextView.append(text + "\n");

        // Auto-scroll to bottom (CHANGED: guard against null layout)
        if (outputTextView.getLayout() != null) {
            final int scrollAmount = outputTextView.getLayout().getLineTop(outputTextView.getLineCount()) - outputTextView.getHeight();
            if (scrollAmount > 0) {
                outputTextView.scrollTo(0, scrollAmount);
            }
        }
    }

    private void updateStatus(String status) {
        statusTextView.setText(status);
    }

    private class TestAgentTask extends AsyncTask<Void, String, JSONObject> {
        private AgentWrapper agent;
        private String input;
        private long startTime;

        public TestAgentTask(AgentWrapper agent, String input) {
            this.agent = agent;
            this.input = input;
        }

        @Override
        protected void onPreExecute() {
            progressBar.setVisibility(View.VISIBLE);
            testButton.setEnabled(false);
            testAllButton.setEnabled(false);
            clearButton.setEnabled(false);

            appendOutput("=== Testing " + agent.name + " ===");
            appendOutput("Input: " + input);
            appendOutput("Starting test...");
            updateStatus("Testing " + agent.name);

            startTime = System.currentTimeMillis();
        }

        @Override
        protected JSONObject doInBackground(Void... params) {
            try {
                publishProgress("Running " + agent.name + "...");

                JSONObject result = null;

                if (agent.agent instanceof SanityCheckAgent) {
                    result = ((SanityCheckAgent) agent.agent).runCheck();
                } else if (agent.agent instanceof VideoAnalyzerAgent) {
                    result = ((VideoAnalyzerAgent) agent.agent).analyzeVideo(input);
                } else if (agent.agent instanceof ScriptGeneratorAgent) {
                    result = ((ScriptGeneratorAgent) agent.agent).generateScript(input);
                } else if (agent.agent instanceof ImageInterpreterAgent) {
                    result = ((ImageInterpreterAgent) agent.agent).analyzeImage(input);
                } else if (agent.agent instanceof AudioContextAgent) {
                    result = ((AudioContextAgent) agent.agent).analyzeAudio(input);
                } else if (agent.agent instanceof ModelValidatorAgent) {
                    JSONObject model = new JSONObject();
                    model.put("id", "test-model");
                    model.put("endpoint", "https://api-inference.huggingface.co/models/");
                    result = new JSONObject();
                    result.put("success", ((ModelValidatorAgent) agent.agent).validateModel(model));
                } else if (agent.agent instanceof OrchestratorAgent) {
                    JSONObject orchestratorInput = new JSONObject();
                    orchestratorInput.put("project_id", "test-project");
                    orchestratorInput.put("prompt", input);
                    result = ((OrchestratorAgent) agent.agent).process(orchestratorInput);
                }

                return result;

            } catch (Exception e) {
                publishProgress("Error: " + e.getMessage());
                return null;
            }
        }

        @Override
        protected void onProgressUpdate(String... values) {
            for (String value : values) {
                appendOutput(value);
            }
        }

        @Override
        protected void onPostExecute(JSONObject result) {
            long endTime = System.currentTimeMillis();
            long duration = endTime - startTime;

            progressBar.setVisibility(View.GONE);
            testButton.setEnabled(true);
            testAllButton.setEnabled(true);
            clearButton.setEnabled(true);

            if (result != null) {
                appendOutput("Result: " + result.toString());
                appendOutput("Duration: " + duration + "ms");
                appendOutput("Status: " + (result.optBoolean("success", false) ? "SUCCESS" : "FAILED"));
                updateStatus(agent.name + " test completed");
            } else {
                appendOutput("Result: null");
                appendOutput("Duration: " + duration + "ms");
                appendOutput("Status: FAILED");
                updateStatus(agent.name + " test failed");
            }

            appendOutput("=== End Test ===\n");

            // Save test result
            saveTestResult(agent.name, input, result, duration);
        }
    }

    private class TestAllAgentsTask extends AsyncTask<Void, String, List<AgentTestResult>> {
        private String input;
        private long startTime;

        public TestAllAgentsTask(String input) {
            this.input = input;
        }

        @Override
        protected void onPreExecute() {
            progressBar.setVisibility(View.VISIBLE);
            testButton.setEnabled(false);
            testAllButton.setEnabled(false);
            clearButton.setEnabled(false);

            appendOutput("=== Testing All Agents ===");
            appendOutput("Input: " + input);
            appendOutput("Starting tests...");
            updateStatus("Testing all agents");

            startTime = System.currentTimeMillis();
        }

        @Override
        protected List<AgentTestResult> doInBackground(Void... params) {
            List<AgentTestResult> results = new ArrayList<AgentTestResult>();

            for (AgentWrapper agent : agents) {
                try {
                    publishProgress("Testing " + agent.name + "...");

                    long agentStartTime = System.currentTimeMillis();
                    JSONObject result = null;

                    if (agent.agent instanceof SanityCheckAgent) {
                        result = ((SanityCheckAgent) agent.agent).runCheck();
                    } else if (agent.agent instanceof VideoAnalyzerAgent) {
                        result = ((VideoAnalyzerAgent) agent.agent).analyzeVideo(input);
                    } else if (agent.agent instanceof ScriptGeneratorAgent) {
                        result = ((ScriptGeneratorAgent) agent.agent).generateScript(input);
                    } else if (agent.agent instanceof ImageInterpreterAgent) {
                        result = ((ImageInterpreterAgent) agent.agent).analyzeImage(input);
                    } else if (agent.agent instanceof AudioContextAgent) {
                        result = ((AudioContextAgent) agent.agent).analyzeAudio(input);
                    } else if (agent.agent instanceof ModelValidatorAgent) {
                        JSONObject model = new JSONObject();
                        model.put("id", "test-model");
                        model.put("endpoint", "https://api-inference.huggingface.co/models/");
                        result = new JSONObject();
                        result.put("success", ((ModelValidatorAgent) agent.agent).validateModel(model));
                    } else if (agent.agent instanceof OrchestratorAgent) {
                        JSONObject orchestratorInput = new JSONObject();
                        orchestratorInput.put("project_id", "test-project");
                        orchestratorInput.put("prompt", input);
                        result = ((OrchestratorAgent) agent.agent).process(orchestratorInput);
                    }

                    long agentEndTime = System.currentTimeMillis();
                    long duration = agentEndTime - agentStartTime;

                    results.add(new AgentTestResult(agent.name, result, duration));

                } catch (Exception e) {
                    publishProgress("Error testing " + agent.name + ": " + e.getMessage());
                    results.add(new AgentTestResult(agent.name, null, 0));
                }
            }

            return results;
        }

        @Override
        protected void onProgressUpdate(String... values) {
            for (String value : values) {
                appendOutput(value);
            }
        }

        @Override
        protected void onPostExecute(List<AgentTestResult> results) {
            long endTime = System.currentTimeMillis();
            long totalDuration = endTime - startTime;

            progressBar.setVisibility(View.GONE);
            testButton.setEnabled(true);
            testAllButton.setEnabled(true);
            clearButton.setEnabled(true);

            appendOutput("\n=== Test Results ===");

            int successCount = 0;
            for (AgentTestResult result : results) {
                boolean success = result.result != null && result.result.optBoolean("success", false);
                if (success) {
                    successCount++;
                }

                appendOutput(result.agentName + ": " + (success ? "SUCCESS" : "FAILED") +
                             " (" + result.duration + "ms)");
            }

            appendOutput("\nSummary:");
            appendOutput("Total: " + results.size() + " agents");
            appendOutput("Success: " + successCount + " agents");
            appendOutput("Failed: " + (results.size() - successCount) + " agents");
            appendOutput("Total Duration: " + totalDuration + "ms");
            appendOutput("=== End Test ===\n");

            updateStatus("All agents test completed");

            // Save test results
            saveAllTestResults(input, results, totalDuration);
        }
    }

    private void saveTestResult(String agentName, String input, JSONObject result, long duration) {
        try {
            JSONObject testResult = new JSONObject();
            testResult.put("agent_name", agentName);
            testResult.put("input", input);
            testResult.put("result", result);
            testResult.put("duration_ms", duration);
            testResult.put("timestamp", System.currentTimeMillis());
            testResult.put("date", new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(new Date()));

            File testDir = new File(StoragePaths.getAgentResultsDir() + "/AgentPlayground");
            if (!testDir.exists()) {
                testDir.mkdirs();
            }

            String fileName = "test_" + agentName.replace(" ", "_").toLowerCase() + "_" + System.currentTimeMillis() + ".json";
            File testFile = new File(testDir, fileName);

            JSONLogger.writeToFile(testFile, testResult.toString());

        } catch (JSONException e) {
            logger.log("AgentPlayground", "Error saving test result: " + e.getMessage());
        }
    }

    private void saveAllTestResults(String input, List<AgentTestResult> results, long totalDuration) {
        try {
            JSONObject allResults = new JSONObject();
            allResults.put("input", input);
            allResults.put("total_duration_ms", totalDuration);
            allResults.put("timestamp", System.currentTimeMillis());
            allResults.put("date", new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(new Date()));

            JSONArray resultsArray = new JSONArray(); // CHANGED: JSONArray import used
            for (AgentTestResult result : results) {
                JSONObject agentResult = new JSONObject();
                agentResult.put("agent_name", result.agentName);
                agentResult.put("result", result.result);
                agentResult.put("duration_ms", result.duration);
                resultsArray.put(agentResult);
            }
            allResults.put("results", resultsArray);

            File testDir = new File(StoragePaths.getAgentResultsDir() + "/AgentPlayground");
            if (!testDir.exists()) {
                testDir.mkdirs();
            }

            String fileName = "test_all_" + System.currentTimeMillis() + ".json";
            File testFile = new File(testDir, fileName);

            JSONLogger.writeToFile(testFile, allResults.toString());

        } catch (JSONException e) {
            logger.log("AgentPlayground", "Error saving all test results: " + e.getMessage());
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.agent_playground, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();

        if (id == android.R.id.home) {
            finish();
            return true;
        } else if (id == R.id.action_export_results) {
            exportResults();
            return true;
        } else if (id == R.id.action_clear_history) {
            clearHistory();
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    private void exportResults() {
        // Export test results functionality
        Toast.makeText(this, R.string.export_results_feature, Toast.LENGTH_SHORT).show();
    }

    private void clearHistory() {
        new AlertDialog.Builder(this)
            .setTitle(R.string.clear_history)
            .setMessage(R.string.clear_history_confirmation)
            .setPositiveButton(R.string.clear, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    // Clear test history
                    File testDir = new File(StoragePaths.getAgentResultsDir() + "/AgentPlayground");
                    if (testDir.exists()) {
                        File[] testFiles = testDir.listFiles();
                        if (testFiles != null) {
                            for (File file : testFiles) {
                                file.delete();
                            }
                        }
                    }

                    clearOutput();
                    Toast.makeText(AgentPlaygroundActivity.this, R.string.history_cleared, Toast.LENGTH_SHORT).show();
                }
            })
            .setNegativeButton(android.R.string.cancel, null)
            .show();
    }

    private static class AgentWrapper {
        String name;
        Object agent;

        AgentWrapper(String name, Object agent) {
            this.name = name;
            this.agent = agent;
        }
    }

    private static class AgentTestResult {
        String agentName;
        JSONObject result;
        long duration;

        AgentTestResult(String agentName, JSONObject result, long duration) {
            this.agentName = agentName;
            this.result = result;
            this.duration = duration;
        }
    }
}
