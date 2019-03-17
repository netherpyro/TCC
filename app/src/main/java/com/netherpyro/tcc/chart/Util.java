package com.netherpyro.tcc.chart;

import android.content.res.Resources;
import android.util.DisplayMetrics;
import android.util.TypedValue;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import androidx.annotation.NonNull;

/**
 * @author mmikhailov on 17/03/2019.
 */
class Util {

    static int dpToPx(int dp) {
        DisplayMetrics metrics = Resources.getSystem().getDisplayMetrics();
        return (int) (dp * metrics.density);
    }

    static int spToPx(int sp) {
        DisplayMetrics metrics = Resources.getSystem().getDisplayMetrics();
        return (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, sp, metrics);
    }

    static List<String> convertTimestampsToLabels(@NonNull List<Long> timestampList) {
        List<String> result = new ArrayList<>(timestampList.size());
        SimpleDateFormat dateFormat = new SimpleDateFormat("MMM d", Locale.getDefault());

        for (Long timestamp : timestampList) {
            result.add(dateFormat.format(new Date(timestamp)));
        }

        return result;
    }
}
