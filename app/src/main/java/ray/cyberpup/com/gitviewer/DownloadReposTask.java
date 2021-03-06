package ray.cyberpup.com.gitviewer;

import android.app.Activity;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.util.Log;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

/**
 * Headless Fragment that wraps an AsyncTask named DownloadReposAsyncTask
 *
 * Responsible for downloading repository information of a single user.
 *
 * @author Raymond Tong
 */
public class DownloadReposTask extends Fragment {

    private static final String LOG_TAG = DownloadReposTask.class.getSimpleName();

    TaskListener mListener;
    DownloadReposAsyncTask mTask;
    private String mQuery;
    interface TaskListener{

        public void onPreExecute();
        public void onProgressUpdate(Integer... progress);
        public void onPostExecute(String results);
        public void onCancelled();
    }

    public static DownloadReposTask getInstance(String query){

        DownloadReposTask taskFragment = new DownloadReposTask();
        Bundle args = new Bundle();
        args.putString("query",query);

        taskFragment.setArguments(args);
        return taskFragment;

    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setRetainInstance(true);
        System.out.println("onCreate");
    }

    public void beginTask(){




    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        mTask = new DownloadReposAsyncTask();
        mQuery = getArguments().getString("query");
        mTask.execute(mQuery);
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);

        try {
            mListener = (TaskListener) activity;
        } catch(ClassCastException e){
            throw new ClassCastException(activity.toString() +
                    " must implement TaskListener");
        }

    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);

    }

    @Override
    public void onDetach() {
        super.onDetach();
        mListener = null;
    }


    void cancel(boolean mayInterruptIfRunning) {
        mTask.cancel(mayInterruptIfRunning);
    }

    int mInput;
    private class DownloadReposAsyncTask extends AsyncTask<String, Integer, String> {


        @Override
        protected void onPreExecute() {

            if(mListener!=null)
                mListener.onPreExecute();
        }

        @Override
        protected void onPostExecute(String results) {

            if(mListener!=null)
                mListener.onPostExecute(results);

        }

        @Override
        protected void onProgressUpdate(Integer... progress) {

            int percentCompleted = (int)(progress[0]*100f/mInput);
            if(mListener!=null)
                mListener.onProgressUpdate(percentCompleted);

        }

        @Override
        protected String doInBackground(String... query) {

            if(query[0].length() == 0)
                return null;

            String result = downloadJSON(query[0]);
            if(isCancelled())
                return null;

            return result;


        }

        private String downloadJSON(String queryUrl){

            // These two need to be declared outside the try/catch
            // so that they can be closed in the finally block.
            HttpURLConnection urlCon = null;
            BufferedReader reader = null;

            // raw JSON response
            String jsonStr=null;

            try {
                // Construct URL
                URL url = new URL(queryUrl);

                // Create the request to OpenWeatherMap, and open the connection
                urlCon = (HttpURLConnection) url.openConnection();
                urlCon.setRequestMethod("GET");
                urlCon.setConnectTimeout(5000);
                urlCon.setRequestProperty("Accept", "application/json");
                //urlCon.setRequestProperty("Content-type", "application/json");
                //urlCon.setRequestProperty("X-CZ-Authorization", AUTH_TOKEN);
                urlCon.connect();

                if(urlCon.getResponseCode() == 200) {

                    // Connection successful
                    InputStream inputStream = urlCon.getInputStream();

                    if (inputStream == null) {
                        return null;
                    }

                    // Synchronized mutable sequence of characters
                    StringBuffer stringBuffer = new StringBuffer();

                    // byte to character bridge
                    // read from resulting character-input stream
                    // using 8192 characters buffer size
                    reader = new BufferedReader(new InputStreamReader(inputStream));

                    // Download Data
                    // Read JSON into a single string
                    String line;
                    while ((line = reader.readLine()) != null) {
                        stringBuffer.append(line + "\n"); // newline added for debugging only
                    }

                    if (stringBuffer.length() == 0) {
                        return null;
                    } else {
                        jsonStr = stringBuffer.toString();
                        // DEBUG
                        //Log.d(LOG_TAG, jsonStr);

                    }
                    return jsonStr;
                }

            } catch (IOException e) {
                // If the code didn't successfully get data
                Log.e(LOG_TAG, "Error ", e);

            } finally {
                if (urlCon != null) {
                    urlCon.disconnect();
                }
                if (reader != null) {
                    try {
                        reader.close();
                    } catch (final IOException e) {
                        Log.e(LOG_TAG, "Error closing stream", e);
                    }
                }
            }

            // Happens if error in parsing or getting data
            return null;
        }



        @Override
        protected void onCancelled() {
            if(mListener!=null)
                mListener.onCancelled();

        }


    }
}
