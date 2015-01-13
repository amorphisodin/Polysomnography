package com.gomtel.polysomnography;

import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.util.Log;

import org.achartengine.ChartFactory;
import org.achartengine.model.CategorySeries;
import org.achartengine.renderer.DefaultRenderer;
import org.achartengine.renderer.SimpleSeriesRenderer;

/**
 * Created by lixiang on 14-12-17.
 */
public class SleepPieChart implements SleepChart  {
    private static final String TAG = "SleepPieChart";
    private Polysomnography mContext;
    private static final int[] margins = {20,50,10,20};

    @Override
    public Intent getIntents(Context context) {
        mContext = (Polysomnography) context;
        return ChartFactory.getPieChartIntent(context, getDataSet(), getPieRenderer(), "睡眠质量检测");
    }

    private DefaultRenderer getPieRenderer() {
        DefaultRenderer renderer = new DefaultRenderer();
//        renderer.setZoomButtonsVisible(true);
//        renderer.setZoomEnabled(true);

        renderer.setChartTitleTextSize(20);
        SimpleSeriesRenderer yellowRenderer = new SimpleSeriesRenderer();
        yellowRenderer.setColor(Color.YELLOW);
        SimpleSeriesRenderer blueRenderer = new SimpleSeriesRenderer();
        blueRenderer.setColor(Color.BLUE);
        SimpleSeriesRenderer redRenderer = new SimpleSeriesRenderer();
        redRenderer.setColor(Color.RED);
        renderer.addSeriesRenderer(yellowRenderer);
        renderer.addSeriesRenderer(blueRenderer);
        renderer.addSeriesRenderer(redRenderer);
// 设置饼图文字字体大小和饼图标签字体大小
        renderer.setDisplayValues(true);
        renderer.setShowLabels(true);
        renderer.setLabelsTextSize(15);
        renderer.setLegendTextSize(18);
        renderer.setPanEnabled(false);
//        renderer.setFitLegend(true);
        renderer.setLegendHeight(0);
//        for(int i = 0;i< 4;i++)
//        Log.e(TAG,"lixiang---renderer= "+renderer.getMargins()[i]);
        renderer.setMargins(margins);
        for(int i = 0;i< 4;i++)
            Log.e(TAG,"lixiang---renderer= "+renderer.getMargins()[i]);

// 设置背景颜色
        renderer.setApplyBackgroundColor(true);
        renderer.setBackgroundColor(Color.BLACK);
        renderer.setAxesColor(Color.WHITE);

        return renderer;
    }

    private CategorySeries getDataSet() {

        CategorySeries pieSeries = new CategorySeries("睡眠质量检测");
//        long wakeTime = (long) (mContext.getWakeTimePercent()*100);
        long lightTime = (long) (mContext.getLightTimePercent()*100);
        long deepTime = (long) (mContext.getDeepTimePercent()*100);
        Log.e("lixiang","lixiang---wakeTime= "+mContext.getWakeTimePercent()+"  deepTime= "+mContext.getDeepTimePercent());

        pieSeries.add("浅睡眠", lightTime);
        pieSeries.add("深睡眠", deepTime);
        pieSeries.add("未睡眠", (100-lightTime-deepTime));
        return pieSeries;
    }
}
