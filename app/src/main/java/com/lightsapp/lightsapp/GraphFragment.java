package com.lightsapp.lightsapp;

import android.app.Fragment;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.SurfaceView;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.jjoe64.graphview.GraphView;
import com.jjoe64.graphview.GraphViewSeries;
import com.jjoe64.graphview.GraphViewStyle;
import com.jjoe64.graphview.LineGraphView;
import com.lightsapp.core.lightanalyzer.Frame;
import com.lightsapp.utils.LinearFilter;

import java.util.List;

public class GraphFragment extends Fragment {
    private static final String ARG_SECTION_NUMBER = "2";
    private static final String TAG = SendFragment.class.getSimpleName();

    private static final int GRAPH_SIZE = 300;
    private static final int MAX_LUM = 250;
    private static float CAMERA_RATIO = 4/3;
    private static int scale = 2;

    private MainActivity mCtx;

    public Handler mHandler;

    FrameLayout mPreview;
    private TextView mTextViewMessageStatus;
    private GraphView graphView_delay, graphView_lum, graphView_dlum;
    private GraphViewSeries series;

    public static GraphFragment newInstance(int sectionNumber) {
        GraphFragment fragment = new GraphFragment();
        Bundle args = new Bundle();
        args.putInt(ARG_SECTION_NUMBER, sectionNumber);
        fragment.setArguments(args);
        return fragment;
    }

    public GraphFragment() {
        mCtx = (MainActivity) getActivity();
    }

    /*
    public void onResume() {
        super.onResume();
        Log.v(TAG, "RESUME_GRAPH");
        if (mCtx.mCameraController != null) {
            mPreview.removeAllViews();
            mPreview.addView(mCtx.mCameraController);
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        Log.v(TAG, "PAUSE_GRAPH");
        mPreview.removeAllViews();
    }
    */

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_graph, container, false);

        mCtx = (MainActivity) getActivity();

        mPreview = (FrameLayout) v.findViewById(R.id.camera_preview);
        mPreview.addView(new SurfaceView(getActivity()), 0);   // BLACK MAGIC avoids black flashing.

        mTextViewMessageStatus = (TextView) v.findViewById(R.id.textViewStatus);
        mTextViewMessageStatus.setText("idle");

        graphView_delay = new LineGraphView(mCtx, "Delay");

        graphView_delay.setViewPort(2, GRAPH_SIZE);
        graphView_delay.setScrollable(true);
        graphView_delay.setScalable(false);
        /*graphView_delay.setShowLegend(true);
        graphView_delay.setLegendAlign(GraphView.LegendAlign.BOTTOM);
        graphView_delay.setLegendWidth(200);*/
        graphView_delay.getGraphViewStyle().setGridStyle(GraphViewStyle.GridStyle.HORIZONTAL);
        graphView_delay.getGraphViewStyle().setGridColor(Color.rgb(30, 30, 30));
        graphView_delay.getGraphViewStyle().setTextSize(10);
        graphView_delay.getGraphViewStyle().setNumHorizontalLabels(0);

        LinearLayout layout = (LinearLayout) v.findViewById(R.id.graph3);
        layout.addView(graphView_delay);

        graphView_lum = new LineGraphView(mCtx, "Luminance");

        graphView_lum.setViewPort(2, GRAPH_SIZE);
        graphView_lum.setScrollable(true);
        graphView_lum.setScalable(false);
        graphView_lum.setManualYAxisBounds(MAX_LUM, 0);
        graphView_lum.getGraphViewStyle().setGridStyle(GraphViewStyle.GridStyle.HORIZONTAL);
        graphView_lum.getGraphViewStyle().setGridColor(Color.rgb(30, 30, 30));
        graphView_lum.getGraphViewStyle().setTextSize(10);
        graphView_lum.getGraphViewStyle().setNumHorizontalLabels(0);

        layout = (LinearLayout) v.findViewById(R.id.graph1);
        layout.addView(graphView_lum);


        graphView_dlum = new LineGraphView(mCtx, "Luminance first derivative");

        graphView_dlum.setViewPort(2, GRAPH_SIZE);
        graphView_dlum.setScrollable(true);
        graphView_dlum.setScalable(false);
        graphView_dlum.setManualYAxisBounds(MAX_LUM, -MAX_LUM);
        graphView_dlum.getGraphViewStyle().setGridStyle(GraphViewStyle.GridStyle.HORIZONTAL);
        graphView_dlum.getGraphViewStyle().setGridColor(Color.rgb(30, 30, 30));
        graphView_dlum.getGraphViewStyle().setTextSize(10);
        graphView_dlum.getGraphViewStyle().setNumHorizontalLabels(0);

        layout = (LinearLayout) v.findViewById(R.id.graph2);
        layout.addView(graphView_dlum);

        mHandler = new Handler() {
            public void handleMessage(Message msg) {
                super.handleMessage(msg);

                if (mTextViewMessageStatus != null && msg.getData().containsKey("info_message")) {
                    mTextViewMessageStatus.setText((String) msg.getData().get("info_message"));
                }

                if (msg.getData().containsKey("setup_done")) {
                    mPreview.removeAllViews();
                    mPreview.addView(mCtx.mCameraController);

                    ViewGroup.LayoutParams l = mPreview.getLayoutParams();
                    l.height = (int) (CAMERA_RATIO * mPreview.getWidth() / scale);
                    l.width = mPreview.getWidth() / scale;
                    scale = 1;
                    Log.v(TAG, "init camera preview done");
                }

                if (msg.getData().containsKey("update")) {
                    List<Frame> lframes;

                    try {
                        lframes = mCtx.mLightA.getFrames();
                    } catch (Exception e) {
                        return;
                    }

                    if (lframes.size() < 2)
                        return;

                    int size, first = 0, last = lframes.size() - 1;

                    for (; last >= 0 ; last--) {
                        if (lframes.get(last).luminance >= 0)
                            break;
                    }

                    if (last < GRAPH_SIZE)
                        size = last;
                    else {
                        size = GRAPH_SIZE;
                        first = last - GRAPH_SIZE;
                    }

                    GraphView.GraphViewData data_delay[];
                    GraphView.GraphViewData data_delay_f[];
                    GraphView.GraphViewData data_lum[];
                    GraphView.GraphViewData data_lum_f[];
                    GraphView.GraphViewData data_lum_d[];
                    float fdata_delay[];
                    float fdata_lum[];

                    try {
                        data_delay = new GraphView.GraphViewData[size];
                        data_delay_f = new GraphView.GraphViewData[size];
                        data_lum = new GraphView.GraphViewData[size];
                        data_lum_f = new GraphView.GraphViewData[size];
                        data_lum_d = new GraphView.GraphViewData[size - 1];
                        fdata_delay = new float[size];
                        fdata_lum = new float[size];
                    }
                    catch (Exception e) {
                        return;
                    }

                    try {
                        LinearFilter dataFilter = LinearFilter.get(LinearFilter.Filter.KERNEL_GAUSSIAN_11);
                        for (int i = first, j = 0; i < last; i++ , j++) {
                            fdata_delay[j] = (float) lframes.get(i).delta;
                            //fdata_lum[j] = (float) lframes.get(i).luminance;
                        }
                        //dataFilter.apply(fdata_lum);
                        dataFilter.apply(fdata_delay);

                        for (int i = first, j = 0; i < last; i++, j++) {
                            data_delay[j] = new GraphView.GraphViewData(i, lframes.get(i).delta);
                            data_lum[j] = new GraphView.GraphViewData(i, lframes.get(i).luminance);
                            data_delay_f[j] = new GraphView.GraphViewData(i, fdata_delay[j]);
                            data_lum_f[j] = new GraphView.GraphViewData(i, fdata_lum[j]);
                        }

                        for (int i = first + 1, j = 1; i < last; i++, j++) {
                            data_lum_d[j - 1] = new GraphView.GraphViewData(i, (lframes.get(i).luminance - lframes.get(i - 1).luminance));
                        }
                    }
                    catch (IndexOutOfBoundsException e) {
                        Log.e(TAG, "out of bounds: " + e.getMessage());
                    }
                    catch (Exception e) {
                        Log.e(TAG, "error while generating graph data: " + e.getMessage());
                    }

                    GraphViewSeries series;
                    try {
                        series = new GraphViewSeries("delay_raw",
                                new GraphViewSeries.GraphViewSeriesStyle(Color.rgb(70, 70, 70), 3),
                                data_delay);
                        graphView_delay.removeAllSeries();
                        graphView_delay.addSeries(series);

                        series = new GraphViewSeries("delay_f",
                                new GraphViewSeries.GraphViewSeriesStyle(Color.rgb(20, 200, 0), 3),
                                data_delay_f);
                        graphView_delay.addSeries(series);

                        series = new GraphViewSeries("luminance_raw",
                                new GraphViewSeries.GraphViewSeriesStyle(Color.rgb(200, 50, 0), 3),
                                data_lum);
                        graphView_lum.removeAllSeries();
                        graphView_lum.addSeries(series);

                        series = new GraphViewSeries("luminance_d",
                                new GraphViewSeries.GraphViewSeriesStyle(Color.rgb(170, 80, 255), 3),
                                data_lum_d);
                        graphView_dlum.removeAllSeries();
                        graphView_dlum.addSeries(series);
                    }
                    catch (Exception e) {
                        Log.d(TAG, "" + e.getMessage());
                    }

                    graphView_delay.scrollToEnd();
                    graphView_lum.scrollToEnd();
                    graphView_dlum.scrollToEnd();
                }
            }
        };

        mCtx.mHandlerGraph = mHandler;

        return v;
    }
}