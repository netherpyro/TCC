package com.netherpyro.tcc;

import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.widget.LinearLayout;

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

            final LinearLayout parent = findViewById(R.id.parent);

            for (final ChartData data : chartData) {
                LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT
                );

                final ChartView chartView = new ChartView(MainActivity.this);
                chartView.setLayoutParams(params);
                parent.addView(chartView);

                if (Build.VERSION.SDK_INT > Build.VERSION_CODES.JELLY_BEAN) {
                    parent.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
                        @Override
                        public void onGlobalLayout() {
                            parent.getViewTreeObserver().removeOnGlobalLayoutListener(this);
                            chartView.setChartName("Followers");
                            chartView.setData(data);
                        }
                    });
                } else {
                    parent.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
                        @Override
                        public void onGlobalLayout() {
                            parent.getViewTreeObserver().removeGlobalOnLayoutListener(this);
                            chartView.setChartName("Followers");
                            chartView.setData(data);
                        }
                    });
                }
            }
        }
    }
}
