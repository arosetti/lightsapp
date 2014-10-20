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
import com.lightsapp.camera.CameraController;
import com.lightsapp.camera.Frame;
import com.lightsapp.util.LinearFilter;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class GraphFragment extends Fragment {
    private static final String ARG_SECTION_NUMBER = "1";
    private static final String TAG = SendFragment.class.getSimpleName();

    private MainActivity mCtx;

    public Handler mHandler;

    private GraphView graphView_delay, graphView_lum;
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

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_graph, container, false);

        mCtx = (MainActivity) getActivity();

        graphView_delay = new LineGraphView(mCtx, "Delay");

        graphView_delay.setViewPort(2, 300);
        graphView_delay.setScrollable(true);
        graphView_delay.setScalable(true);
        /*graphView_delay.setShowLegend(true);
        graphView_delay.setLegendAlign(GraphView.LegendAlign.BOTTOM);
        graphView_delay.setLegendWidth(200);*/
        graphView_delay.getGraphViewStyle().setGridStyle(GraphViewStyle.GridStyle.HORIZONTAL);
        graphView_delay.getGraphViewStyle().setGridColor(Color.rgb(30,30,30));
        graphView_delay.getGraphViewStyle().setTextSize(10);
        graphView_delay.getGraphViewStyle().setNumHorizontalLabels(0);

        LinearLayout layout = (LinearLayout) v.findViewById(R.id.graph1);
        layout.addView(graphView_delay);

        graphView_lum = new LineGraphView(mCtx, "Luminance");

        graphView_lum.setViewPort(2, 300);
        graphView_lum.setScrollable(true);
        graphView_lum.setScalable(true);
        /*graphView_lum.setShowLegend(true);
        graphView_lum.setLegendAlign(GraphView.LegendAlign.BOTTOM);
        graphView_lum.setLegendWidth(200);*/
        graphView_lum.getGraphViewStyle().setGridStyle(GraphViewStyle.GridStyle.HORIZONTAL);
        graphView_lum.getGraphViewStyle().setGridColor(Color.rgb(30,30,30));
        graphView_lum.getGraphViewStyle().setTextSize(10);
        graphView_lum.getGraphViewStyle().setNumHorizontalLabels(0);

        layout = (LinearLayout) v.findViewById(R.id.graph2);
        layout.addView(graphView_lum);

        mHandler = new Handler() {
            public void handleMessage(Message msg) {
                super.handleMessage(msg);

                if (msg.getData().containsKey("update")) {
                    Log.v(TAG, "updating graph");
                    // TODO get frame by frame

                    List<Frame> lframes = mCtx.mCameraController.getFrames();

                    GraphView.GraphViewData data_delay[] = new GraphView.GraphViewData[lframes.size()];
                    GraphView.GraphViewData data_delay_f[] = new GraphView.GraphViewData[lframes.size()];
                    GraphView.GraphViewData data_lum[] = new GraphView.GraphViewData[lframes.size()];
                    GraphView.GraphViewData data_lum_f[] = new GraphView.GraphViewData[lframes.size()];
                    float fdata_delay[] = new float[lframes.size()];
                    float fdata_lum[] = new float[lframes.size()];

                    LinearFilter dataFilter = LinearFilter.get(LinearFilter.Filter.KERNEL_GAUSSIAN_11);

                    try {
                        for (int i = 0; i < lframes.size(); i++) {
                            fdata_delay[i] = (float) lframes.get(i).delta;
                            fdata_lum[i] = (float) lframes.get(i).luminance / 1000;
                        }
                        dataFilter.apply(fdata_lum);
                        dataFilter.apply(fdata_delay);

                        for (int i = 0; i < lframes.size(); i++) {
                            data_delay[i] = new GraphView.GraphViewData(i, lframes.get(i).delta);
                            data_lum[i] = new GraphView.GraphViewData(i, lframes.get(i).luminance / 1000);
                            data_delay_f[i] = new GraphView.GraphViewData(i, fdata_delay[i]);
                            data_lum_f[i] = new GraphView.GraphViewData(i, fdata_lum[i]);
                        }
                    }
                    catch (IndexOutOfBoundsException e) {}

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
                                new GraphViewSeries.GraphViewSeriesStyle(Color.rgb(70, 70, 70), 3),
                                data_lum);
                        graphView_lum.removeAllSeries();
                        graphView_lum.addSeries(series);

                        series = new GraphViewSeries("luminance_f",
                                new GraphViewSeries.GraphViewSeriesStyle(Color.rgb(200, 50, 00), 3),
                                data_lum_f);
                        graphView_lum.addSeries(series);
                    }
                    catch (NullPointerException e) {}

                    graphView_delay.scrollToEnd();
                    graphView_lum.scrollToEnd();
                }
            }
        };

        mCtx.mHandlerGraph = mHandler;

        return v;
    }
}