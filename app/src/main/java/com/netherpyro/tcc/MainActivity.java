package com.netherpyro.tcc;

import android.os.AsyncTask;
import android.os.Bundle;

import com.netherpyro.tcc.chart.ChartData;
import com.netherpyro.tcc.chart.ChartView;
import com.netherpyro.tcc.util.AssetsFileReader;
import com.netherpyro.tcc.util.JsonParser;

import org.json.JSONException;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import androidx.appcompat.app.AppCompatActivity;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        new LoadJsonTask().execute();
    }

    private class LoadJsonTask extends AsyncTask<Void, Void, List<ChartData>> {

        @Override
        protected List<ChartData> doInBackground(Void... voids) {
            List<ChartData> data = new ArrayList<>();
            try {
                data = new JsonParser().parse(
                        new AssetsFileReader().parse(MainActivity.this, "chart_data.json")
                );
            } catch (JSONException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }

            return data;
        }

        @Override
        protected void onPostExecute(List<ChartData> chartData) {
            super.onPostExecute(chartData);
            ChartView chartView = findViewById(R.id.chartView);
            chartView.setChartName("Followers");
            chartView.setData(chartData);
        }
    }
}
