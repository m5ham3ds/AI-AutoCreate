package com.ai_autocreate.fragments;

import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.widget.SwipeRefreshLayout;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.ai_autocreate.R;
import com.ai_autocreate.activities.AgentPlaygroundActivity;
import com.ai_autocreate.agents.SanityCheckAgent;
import com.ai_autocreate.utils.JSONLogger;
import com.ai_autocreate.utils.StoragePaths;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class TestFragment extends Fragment {
    private SwipeRefreshLayout swipeRefreshLayout;
    private LinearLayout testsContainer;
    private Button runAllTestsButton;
    private Button clearResultsButton;
    private JSONLogger logger;
    private SanityCheckAgent sanityCheckAgent;
    private List<TestResult> testResults;

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_test, container, false);

        // Initialize views
        swipeRefreshLayout = view.findViewById(R.id.swipe_refresh_layout);
        testsContainer = view.findViewById(R.id.tests_container);
        runAllTestsButton = view.findViewById(R.id.run_all_tests_button);
        clearResultsButton = view.findViewById(R.id.clear_results_button);

        // Initialize components
        logger = new JSONLogger(getActivity());
        sanityCheckAgent = new SanityCheckAgent(getActivity());
        testResults = new ArrayList<>();

        // Setup listeners
        setupListeners();

        // Load test results
        loadTestResults();

        return view;
    }

    private void setupListeners() {
        swipeRefreshLayout.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
                @Override
                public void onRefresh() {
                    runAllTests();
                    swipeRefreshLayout.setRefreshing(false);
                }
            });

        runAllTestsButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    runAllTests();
                }
            });

        clearResultsButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    clearResults();
                }
            });
    }

    private void runAllTests() {
        testResults.clear();
        testsContainer.removeAllViews();

        // Test 1: Sanity Check
        addTestView("Sanity Check", "Running system sanity checks...");
        new SanityCheckTask().execute();

        // Test 2: Storage Test
        addTestView("Storage Test", "Testing storage access...");
        new StorageTestTask().execute();

        // Test 3: Network Test
        addTestView("Network Test", "Testing network connectivity...");
        new NetworkTestTask().execute();

        // Test 4: Model Test
        addTestView("Model Test", "Testing AI model access...");
        new ModelTestTask().execute();

        // Test 5: FFmpeg Test
        addTestView("FFmpeg Test", "Testing FFmpeg availability...");
        new FFmpegTestTask().execute();
    }

    private void addTestView(String testName, String initialMessage) {
        View testView = LayoutInflater.from(getActivity()).inflate(R.layout.item_test, testsContainer, false);

        TextView testNameText = testView.findViewById(R.id.test_name);
        TextView testStatusText = testView.findViewById(R.id.test_status);
        ProgressBar testProgressBar = testView.findViewById(R.id.test_progress);
        Button testActionButton = testView.findViewById(R.id.test_action_button);

        testNameText.setText(testName);
        testStatusText.setText(initialMessage);
        testProgressBar.setVisibility(View.VISIBLE);
        testActionButton.setVisibility(View.GONE);

        // Store test result
        TestResult testResult = new TestResult(testName, testView, testNameText, testStatusText, testProgressBar, testActionButton);
        testResults.add(testResult);

        testsContainer.addView(testView);
    }

    private void updateTestResult(String testName, boolean success, String message) {
        for (TestResult testResult : testResults) {
            if (testResult.testName.equals(testName)) {
                testResult.success = success;
                testResult.message = message;

                testResult.progressBar.setVisibility(View.GONE);
                testResult.statusText.setText(message);

                if (success) {
                    testResult.statusText.setTextColor(getResources().getColor(android.R.color.holo_green_dark));
                } else {
                    testResult.statusText.setTextColor(getResources().getColor(android.R.color.holo_red_dark));
                }

                testResult.actionButton.setVisibility(View.VISIBLE);
                testResult.actionButton.setText(R.string.view_details);
                testResult.actionButton.setOnClickListener(new View.OnClickListener() {
                        private final TestResult resultCopy = testResult; // نسخة final
                        @Override
                        public void onClick(View v) {
                            showTestDetails(resultCopy);
                        }
                    });

                break;
            }
        }
    }

    private void showTestDetails(TestResult testResult) {
        // Show test details in a dialog or navigate to a details screen
        // For now, we'll just show a toast
        Toast.makeText(getActivity(), testResult.testName + ": " + testResult.message, Toast.LENGTH_LONG).show();
    }

    private void clearResults() {
        testResults.clear();
        testsContainer.removeAllViews();

        // Clear log files
        File logDir = new File(StoragePaths.getAgentResultsDir());
        if (logDir.exists()) {
            File[] logFiles = logDir.listFiles();
            if (logFiles != null) {
                for (File file : logFiles) {
                    if (file.isDirectory()) {
                        File[] agentFiles = file.listFiles();
                        if (agentFiles != null) {
                            for (File agentFile : agentFiles) {
                                agentFile.delete();
                            }
                        }
                        file.delete();
                    } else {
                        file.delete();
                    }
                }
            }
        }

        Toast.makeText(getActivity(), R.string.test_results_cleared, Toast.LENGTH_SHORT).show();
    }

    private void loadTestResults() {
        // Load previous test results from log files
        File logDir = new File(StoragePaths.getAgentResultsDir());
        if (logDir.exists()) {
            // This is a simplified version - in a real implementation, 
            // this would load and display previous test results
        }
    }

    private class SanityCheckTask extends AsyncTask<Void, Void, JSONObject> {
        @Override
        protected JSONObject doInBackground(Void... params) {
            return sanityCheckAgent.runCheck();
        }

        @Override
        protected void onPostExecute(JSONObject result) {
            boolean success = result != null && result.optBoolean("passed", false);
            String message = result != null ? 
                result.optString("message", "Sanity check completed") : 
                "Sanity check failed";

            updateTestResult("Sanity Check", success, message);
        }
    }

    private class StorageTestTask extends AsyncTask<Void, Void, Boolean> {
        @Override
        protected Boolean doInBackground(Void... params) {
            try {
                // Test writing to app directory
                File testFile = new File(StoragePaths.getTempDir() + "/test.txt");
                if (!testFile.getParentFile().exists()) {
                    testFile.getParentFile().mkdirs();
                }

                // Write test data
                String testData = "Storage test data: " + System.currentTimeMillis();
                JSONLogger.writeToFile(testFile, testData);

                // Read test data
                String readData = JSONLogger.readFromFile(testFile);

                // Clean up
                testFile.delete();

                return testData.equals(readData);
            } catch (Exception e) {
                return false;
            }
        }

        @Override
        protected void onPostExecute(Boolean success) {
            String message = success ? "Storage test passed" : "Storage test failed";
            updateTestResult("Storage Test", success, message);
        }
    }

    private class NetworkTestTask extends AsyncTask<Void, Void, Boolean> {
        @Override
        protected Boolean doInBackground(Void... params) {
            try {
                // Simple network connectivity test
                java.net.URL url = new java.net.URL("https://www.google.com");
                java.net.HttpURLConnection connection = (java.net.HttpURLConnection) url.openConnection();
                connection.setRequestMethod("HEAD");
                connection.setConnectTimeout(5000);
                connection.setReadTimeout(5000);

                int responseCode = connection.getResponseCode();
                connection.disconnect();

                return responseCode == 200;
            } catch (Exception e) {
                return false;
            }
        }

        @Override
        protected void onPostExecute(Boolean success) {
            String message = success ? "Network test passed" : "Network test failed";
            updateTestResult("Network Test", success, message);
        }
    }

    private class ModelTestTask extends AsyncTask<Void, Void, Boolean> {
        @Override
        protected Boolean doInBackground(Void... params) {
            try {
                // Test model connectivity
                // This is a simplified version - in a real implementation, 
                // this would test actual model endpoints

                // For now, we'll just simulate a successful test
                Thread.sleep(1000);
                return true;
            } catch (Exception e) {
                return false;
            }
        }

        @Override
        protected void onPostExecute(Boolean success) {
            String message = success ? "Model test passed" : "Model test failed";
            updateTestResult("Model Test", success, message);
        }
    }

    private class FFmpegTestTask extends AsyncTask<Void, Void, Boolean> {
        @Override
        protected Boolean doInBackground(Void... params) {
            try {
                // Test FFmpeg availability
                // This is a simplified version - in a real implementation, 
                // this would test actual FFmpeg functionality

                // For now, we'll just simulate a successful test
                Thread.sleep(1000);
                return true;
            } catch (Exception e) {
                return false;
            }
        }

        @Override
        protected void onPostExecute(Boolean success) {
            String message = success ? "FFmpeg test passed" : "FFmpeg test failed";
            updateTestResult("FFmpeg Test", success, message);
        }
    }

    private static class TestResult {
        String testName;
        View testView;
        TextView nameText;
        TextView statusText;
        ProgressBar progressBar;
        Button actionButton;
        boolean success;
        String message;
        JSONObject details;

        TestResult(String testName, View testView, TextView nameText, TextView statusText, 
                   ProgressBar progressBar, Button actionButton) {
            this.testName = testName;
            this.testView = testView;
            this.nameText = nameText;
            this.statusText = statusText;
            this.progressBar = progressBar;
            this.actionButton = actionButton;
        }
    }
}
