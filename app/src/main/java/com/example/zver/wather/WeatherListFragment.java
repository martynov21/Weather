package com.example.zver.wather;

import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceManager;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.concurrent.ExecutionException;


public class WeatherListFragment extends Fragment {

    public WeatherListFragment() {
    }

    ArrayList<String> arrayForecast = new ArrayList<String>();
    ArrayAdapter<String> adapter;
    ListView listView;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.fragment_menu, menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onStart() {
        super.onStart();
        updateWeather();
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.main_list_fragment, container, false);
        listView = (ListView) view.findViewById(R.id.listView);

        adapter = new ArrayAdapter<String>(getActivity(),
                android.R.layout.simple_list_item_1, new ArrayList<String>());
        listView.setAdapter(adapter);

        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                String forecast = adapter.getItem(position);
                Intent intent = new Intent(getActivity(), DetailActivity.class).putExtra(Intent.EXTRA_TEXT, forecast);
                startActivity(intent);
            }
        });
        return view;
    }

    private void updateWeather() {
        DownloadWeather downloadWeather = new DownloadWeather();
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(getActivity());
        String location = preferences.getString(getString(R.string.pref_location_key),
                getString(R.string.pref_location_default));
        downloadWeather.execute(location);
    }


    public class DownloadWeather extends AsyncTask<String, Void, ArrayList<String>> {

        private int colDays(){
            SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(getActivity());
            String colDays = pref.getString(
                    getString(R.string.pref_col_days_key),
                    getString(R.string.pref_col_days_default));
            Log.e("ColDays", "" + colDays);

            int i = Integer.valueOf(colDays);

            return i;
        }

        private String formatHighLows(double high, double low) {
            SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(getActivity());
            String temperatureUnits = preferences.getString(
                    getString(R.string.pref_temperature_key),
                    getString(R.string.pref_temperature_metric));


            if (temperatureUnits.equals(getString(R.string.pref_temperature_imperial))){
                high =1.8 * high +32;
                low  = 1.8 * low +32;
            }

            else if(!temperatureUnits.equals(getString(R.string.pref_temperature_metric))){
                    Log.e("Error","Type not found");
            }

            long roundedHigh = Math.round(high);
            long roundedLow = Math.round(low);

            String highLowStr = roundedHigh + "-" + roundedLow;
            return highLowStr;
        }

        private ArrayList<String> getWeatherDataFromJson(String forecastJsonStr, int numDays)
                throws JSONException {

            final String OWM_LIST = "list";
            final String OWM_WEATHER = "weather";
            final String OWM_TEMPERATURE = "temp";
            final String OWM_MAX = "max";
            final String OWM_MIN = "min";
            final String OWM_DESCRIPTION = "main";

            JSONObject forecastJson = new JSONObject(forecastJsonStr);
            JSONArray weatherArray = forecastJson.getJSONArray(OWM_LIST);


            String[] resultStrs = new String[numDays];
            ArrayList<String> resultArray = new ArrayList<String>();
            for (int i = 0; i < weatherArray.length(); i++) {
                String description;
                String highAndLow;

                // Get the JSON object representing the day
                JSONObject dayForecast = weatherArray.getJSONObject(i);

                ///currentDate + i
                Date currentDate = new Date();
                currentDate = new Date();
                Long time = currentDate.getTime();
                long anotherDate = i;
                time = time + (60 * 60 * 24 * 1000 * anotherDate);
                currentDate = new Date(time);

                String dayOfWeek = new SimpleDateFormat("EEE MMM d").format(currentDate);


                JSONObject weatherObject = dayForecast.getJSONArray(OWM_WEATHER).getJSONObject(0);
                description = weatherObject.getString(OWM_DESCRIPTION);

                JSONObject temperatureObject = dayForecast.getJSONObject(OWM_TEMPERATURE);
                double high = temperatureObject.getDouble(OWM_MAX);
                double low = temperatureObject.getDouble(OWM_MIN);

                highAndLow = formatHighLows(high, low);
                resultArray.add(dayOfWeek + " - " + description + " - " + highAndLow);
            }
            return resultArray;

        }

        @Override
        protected ArrayList<String> doInBackground(String... params) {

            if (params.length == 0) {
                return null;
            }

            HttpURLConnection urlConnection = null;
            BufferedReader reader = null;
            String forecastJsonStr = null;

            String mode = "json";
            String units = "metric";
            String apiKey = "8f77431b8a6e4242cd75e831c7ce4b80";
            int numDays = colDays();;

            try {

                final String FORECAST_BASIC_URL = "http://api.openweathermap.org/data/2.5/forecast/daily?";
                final String QUERY_PARAM = "q";
                final String MODE_PARAM = "mode";
                final String UNITS_PARAM = "units";
                final String DAYS_PARAM = "cnt";
                final String KEY_PARAM = "appid";

                Uri builtUri = Uri.parse(FORECAST_BASIC_URL).buildUpon()
                        .appendQueryParameter(QUERY_PARAM, params[0])
                        .appendQueryParameter(MODE_PARAM, mode)
                        .appendQueryParameter(UNITS_PARAM, units)
                        .appendQueryParameter(DAYS_PARAM, Integer.toString(numDays))
                        .appendQueryParameter(KEY_PARAM, apiKey)
                        .build();


                URL url = new URL(builtUri.toString());
                Log.v("URL", "URL = " + builtUri.toString());
                urlConnection = (HttpURLConnection) url.openConnection();
                urlConnection.setRequestMethod("GET");
                urlConnection.connect();

                InputStream inputStream = urlConnection.getInputStream();
                StringBuffer buffer = new StringBuffer();
                if (inputStream == null) {
                    return null;
                }
                reader = new BufferedReader(new InputStreamReader(inputStream));

                String line;
                while ((line = reader.readLine()) != null) {
                    buffer.append(line + "\n");
                }

                if (buffer.length() == 0) {
                    return null;
                }
                forecastJsonStr = buffer.toString();

            } catch (IOException e) {

                Log.e("DownloadWeather", "Error ", e);
                return null;

            } finally {

                if (urlConnection != null) {
                    urlConnection.disconnect();
                }
                if (reader != null) {
                    try {
                        reader.close();
                    } catch (final IOException e) {
                        Log.e("DownloadWeather", "Error closing stream", e);
                    }
                }
            }

            try {
                return getWeatherDataFromJson(forecastJsonStr, numDays);
            } catch (JSONException e) {
                Log.e("Error", e + "");
            }

            return null;
        }

        @Override
        protected void onPostExecute(ArrayList<String> strings) {
            if (strings != null) {
                adapter.clear();
                for (String dayForecas : strings) {
                    adapter.add(dayForecas);
                }
            }
        }
    }
}

