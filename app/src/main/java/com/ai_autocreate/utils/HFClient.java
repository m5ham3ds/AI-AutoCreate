package com.ai_autocreate.utils;

import android.content.Context;
import android.os.AsyncTask;

import com.ai_autocreate.R;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.concurrent.TimeUnit;

public class HFClient {
    private static final int CONNECT_TIMEOUT = 10000; // 10 seconds
    private static final int READ_TIMEOUT = 30000; // 30 seconds
    private static final int MAX_RETRIES = 3;
    private static final int[] RETRY_DELAYS = {1000, 2000, 4000}; // 1s, 2s, 4s

    private Context context;
    private JSONLogger logger;

    public HFClient(Context context) {
        this.context = context;
        this.logger = new JSONLogger(context);
    }

    public JSONObject requestModel(String endpoint, String apiKey, JSONObject payload) {
        for (int attempt = 0; attempt < MAX_RETRIES; attempt++) {
            try {
                JSONObject response = makeRequest(endpoint, apiKey, payload);
                if (response != null) {
                    return response;
                }

                // If this is not the last attempt, wait before retrying
                if (attempt < MAX_RETRIES - 1) {
                    Thread.sleep(RETRY_DELAYS[attempt]);
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return null;
            } catch (Exception e) {
                logger.log("HFClient", "Error in request attempt " + (attempt + 1) + ": " + e.getMessage());

                // If this is not the last attempt, wait before retrying
                if (attempt < MAX_RETRIES - 1) {
                    try {
                        Thread.sleep(RETRY_DELAYS[attempt]);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        return null;
                    }
                }
            }
        }

        logger.log("HFClient", "All request attempts failed for endpoint: " + endpoint);
        return null;
    }

    private JSONObject makeRequest(String endpoint, String apiKey, JSONObject payload) {
        HttpURLConnection connection = null;
        BufferedReader reader = null;

        try {
            URL url = new URL(endpoint);
            connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("POST");
            connection.setConnectTimeout(CONNECT_TIMEOUT);
            connection.setReadTimeout(READ_TIMEOUT);
            connection.setRequestProperty("Content-Type", "application/json");
            connection.setRequestProperty("Accept", "application/json");

            // Add API key if provided
            if (apiKey != null && !apiKey.isEmpty()) {
                connection.setRequestProperty("Authorization", "Bearer " + apiKey);
            }

            // Send request
            connection.setDoOutput(true);
            DataOutputStream outputStream = new DataOutputStream(connection.getOutputStream());
            outputStream.writeBytes(payload.toString());
            outputStream.flush();
            outputStream.close();

            // Get response
            int responseCode = connection.getResponseCode();

            if (responseCode == HttpURLConnection.HTTP_OK) {
                reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
                StringBuilder response = new StringBuilder();
                String line;

                while ((line = reader.readLine()) != null) {
                    response.append(line);
                }

                // Parse JSON response
                return new JSONObject(response.toString());
            } else {
                logger.log("HFClient", "HTTP error: " + responseCode + " for endpoint: " + endpoint);
                return null;
            }

        } catch (Exception e) {
            logger.log("HFClient", "Error making request: " + e.getMessage());
            return null;
        } finally {
            // Clean up
            if (reader != null) {
                try {
                    reader.close();
                } catch (IOException e) {
                    // Ignore
                }
            }

            if (connection != null) {
                connection.disconnect();
            }
        }
    }

    public JSONObject uploadFile(String endpoint, String apiKey, File file, String fieldName) {
        for (int attempt = 0; attempt < MAX_RETRIES; attempt++) {
            try {
                JSONObject response = makeFileUploadRequest(endpoint, apiKey, file, fieldName);
                if (response != null) {
                    return response;
                }

                // If this is not the last attempt, wait before retrying
                if (attempt < MAX_RETRIES - 1) {
                    Thread.sleep(RETRY_DELAYS[attempt]);
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return null;
            } catch (Exception e) {
                logger.log("HFClient", "Error in file upload attempt " + (attempt + 1) + ": " + e.getMessage());

                // If this is not the last attempt, wait before retrying
                if (attempt < MAX_RETRIES - 1) {
                    try {
                        Thread.sleep(RETRY_DELAYS[attempt]);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        return null;
                    }
                }
            }
        }

        logger.log("HFClient", "All file upload attempts failed for endpoint: " + endpoint);
        return null;
    }

    private JSONObject makeFileUploadRequest(String endpoint, String apiKey, File file, String fieldName) {
        HttpURLConnection connection = null;
        BufferedReader reader = null;
        DataOutputStream outputStream = null;

        try {
            String boundary = "----WebKitFormBoundary" + System.currentTimeMillis();
            URL url = new URL(endpoint);
            connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("POST");
            connection.setConnectTimeout(CONNECT_TIMEOUT);
            connection.setReadTimeout(READ_TIMEOUT);
            connection.setRequestProperty("Content-Type", "multipart/form-data; boundary=" + boundary);
            connection.setRequestProperty("Accept", "application/json");

            // Add API key if provided
            if (apiKey != null && !apiKey.isEmpty()) {
                connection.setRequestProperty("Authorization", "Bearer " + apiKey);
            }

            // Send request
            connection.setDoOutput(true);
            outputStream = new DataOutputStream(connection.getOutputStream());

            // Add file part
            outputStream.writeBytes("--" + boundary + "\r\n");
            outputStream.writeBytes("Content-Disposition: form-data; name=\"" + fieldName + "\"; filename=\"" + file.getName() + "\"\r\n");
            outputStream.writeBytes("Content-Type: application/octet-stream\r\n\r\n");

            // Write file content
            FileInputStream fileInputStream = new FileInputStream(file);
            byte[] buffer = new byte[4096];
            int bytesRead;

            while ((bytesRead = fileInputStream.read(buffer)) != -1) {
                outputStream.write(buffer, 0, bytesRead);
            }

            fileInputStream.close();
            outputStream.writeBytes("\r\n");
            outputStream.writeBytes("--" + boundary + "--\r\n");
            outputStream.flush();

            // Get response
            int responseCode = connection.getResponseCode();

            if (responseCode == HttpURLConnection.HTTP_OK) {
                reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
                StringBuilder response = new StringBuilder();
                String line;

                while ((line = reader.readLine()) != null) {
                    response.append(line);
                }

                // Parse JSON response
                return new JSONObject(response.toString());
            } else {
                logger.log("HFClient", "HTTP error: " + responseCode + " for file upload to endpoint: " + endpoint);
                return null;
            }

        } catch (Exception e) {
            logger.log("HFClient", "Error making file upload request: " + e.getMessage());
            return null;
        } finally {
            // Clean up
            if (outputStream != null) {
                try {
                    outputStream.close();
                } catch (IOException e) {
                    // Ignore
                }
            }

            if (reader != null) {
                try {
                    reader.close();
                } catch (IOException e) {
                    // Ignore
                }
            }

            if (connection != null) {
                connection.disconnect();
            }
        }
    }

    public void requestModelAsync(String endpoint, String apiKey, JSONObject payload, RequestCallback callback) {
        new RequestTask(endpoint, apiKey, payload, callback).execute();
    }

    public void uploadFileAsync(String endpoint, String apiKey, File file, String fieldName, RequestCallback callback) {
        new FileUploadTask(endpoint, apiKey, file, fieldName, callback).execute();
    }

    public interface RequestCallback {
        void onSuccess(JSONObject response);
        void onError(String errorMessage);
    }

    private class RequestTask extends AsyncTask<Void, Void, JSONObject> {
        private String endpoint;
        private String apiKey;
        private JSONObject payload;
        private RequestCallback callback;
        private String errorMessage;

        public RequestTask(String endpoint, String apiKey, JSONObject payload, RequestCallback callback) {
            this.endpoint = endpoint;
            this.apiKey = apiKey;
            this.payload = payload;
            this.callback = callback;
        }

        @Override
        protected JSONObject doInBackground(Void... params) {
            try {
                return requestModel(endpoint, apiKey, payload);
            } catch (Exception e) {
                errorMessage = e.getMessage();
                return null;
            }
        }

        @Override
        protected void onPostExecute(JSONObject result) {
            if (result != null && callback != null) {
                callback.onSuccess(result);
            } else if (callback != null) {
                callback.onError(errorMessage != null ? errorMessage : "Unknown error");
            }
        }
    }

    private class FileUploadTask extends AsyncTask<Void, Void, JSONObject> {
        private String endpoint;
        private String apiKey;
        private File file;
        private String fieldName;
        private RequestCallback callback;
        private String errorMessage;

        public FileUploadTask(String endpoint, String apiKey, File file, String fieldName, RequestCallback callback) {
            this.endpoint = endpoint;
            this.apiKey = apiKey;
            this.file = file;
            this.fieldName = fieldName;
            this.callback = callback;
        }

        @Override
        protected JSONObject doInBackground(Void... params) {
            try {
                return uploadFile(endpoint, apiKey, file, fieldName);
            } catch (Exception e) {
                errorMessage = e.getMessage();
                return null;
            }
        }

        @Override
        protected void onPostExecute(JSONObject result) {
            if (result != null && callback != null) {
                callback.onSuccess(result);
            } else if (callback != null) {
                callback.onError(errorMessage != null ? errorMessage : "Unknown error");
            }
        }
    }
}
