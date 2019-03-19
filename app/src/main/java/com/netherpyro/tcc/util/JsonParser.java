package com.netherpyro.tcc.util;

import android.graphics.Color;

import com.netherpyro.tcc.chart.ChartData;
import com.netherpyro.tcc.chart.GraphLineModel;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import androidx.collection.ArraySet;

/**
 * @author mmikhailov on 17/03/2019.
 */
public class JsonParser {

    private final String X_COLUMN = "x";

    public List<ChartData> parse(String json) throws JSONException {
        final List<ChartData> chartDataList = new ArrayList<>();
        final JSONArray rootJsonChartArray = new JSONArray(json);

        for (int rootJsonChartIndex = 0; rootJsonChartIndex < rootJsonChartArray.length(); rootJsonChartIndex++) {
            final JSONObject chartJson = rootJsonChartArray.getJSONObject(rootJsonChartIndex);

            final HashMap<String, String> columnsTypes = new HashMap<>();
            final HashMap<String, String> columnsNames = new HashMap<>();
            final HashMap<String, Integer> columnsColors = new HashMap<>();
            final HashMap<String, List<Float>> columnsValues = new HashMap<>();

            final JSONObject typesJson = chartJson.getJSONObject("types");
            final Iterator<String> typeKeys = typesJson.keys();
            while (typeKeys.hasNext()) {
                final String type = typeKeys.next();
                columnsTypes.put(type, typesJson.getString(type));
            }

            final JSONObject namesJson = chartJson.getJSONObject("names");
            final Iterator<String> nameKeys = namesJson.keys();
            while (nameKeys.hasNext()) {
                final String type = nameKeys.next();
                columnsNames.put(type, namesJson.getString(type));
            }

            final JSONObject colorsJson = chartJson.getJSONObject("colors");
            final Iterator<String> colorKeys = colorsJson.keys();
            while (colorKeys.hasNext()) {
                final String type = colorKeys.next();
                columnsColors.put(type, Color.parseColor(colorsJson.getString(type)));
            }

            final JSONArray columnsJson = chartJson.getJSONArray("columns");
            final List<Long> abscissa = new LinkedList<>();
            for (int i = 0; i < columnsJson.length(); i++) {
                final JSONArray columnValues = columnsJson.getJSONArray(i);
                final String key = columnValues.getString(0);

                if (key.equals(X_COLUMN)) {
                    for (int columnValueIndex = 1; columnValueIndex < columnValues.length(); columnValueIndex++) {
                        abscissa.add(columnValues.getLong(columnValueIndex));
                    }
                } else {
                    final List<Float> values = new LinkedList<>();

                    for (int columnValueIndex = 1; columnValueIndex < columnValues.length(); columnValueIndex++) {
                        values.add((float) columnValues.getDouble(columnValueIndex));
                    }

                    columnsValues.put(key, values);
                }
            }

            final Set<GraphLineModel> ordinates = new ArraySet<>();

            for (Map.Entry<String, String> entry : columnsTypes.entrySet()) {
                final String column = entry.getKey();

                if (column.equals(X_COLUMN)) continue;

                final GraphLineModel line = new GraphLineModel(
                        column,
                        columnsNames.get(column),
                        columnsValues.get(column),
                        columnsColors.containsKey(column) ? columnsColors.get(column) : 0
                );

                ordinates.add(line);
            }

            chartDataList.add(new ChartData(abscissa, ordinates));
        }

        return chartDataList;
    }

}