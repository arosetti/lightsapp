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

        graphView_delay.setViewPort(1, 100);
        graphView_delay.setScrollable(true);
        graphView_delay.setScalable(true);
        graphView_delay.getGraphViewStyle().setGridStyle(GraphViewStyle.GridStyle.HORIZONTAL);
        graphView_delay.getGraphViewStyle().setGridColor(Color.rgb(30,30,30));

        LinearLayout layout = (LinearLayout) v.findViewById(R.id.graph1);
        layout.addView(graphView_delay);

        graphView_lum = new LineGraphView(mCtx, "Luminance");

        graphView_lum.setViewPort(1, 100);
        graphView_lum.setScrollable(true);
        graphView_lum.setScalable(true);
        graphView_lum.getGraphViewStyle().setGridStyle(GraphViewStyle.GridStyle.HORIZONTAL);
        graphView_lum.getGraphViewStyle().setGridColor(Color.rgb(30,30,30));

        layout = (LinearLayout) v.findViewById(R.id.graph2);
        layout.addView(graphView_lum);

        mHandler = new Handler() {
            public void handleMessage(Message msg) {
                super.handleMessage(msg);

                if (msg.getData().containsKey("update")) {
                    Log.v(TAG, "updating graph");
                    // TODO get frame by frame

                    List<Frame> lframes = mCtx.mCameraController.getFrames();

                    GraphView.GraphViewData data1[] = new GraphView.GraphViewData[lframes.size()];
                    GraphView.GraphViewData data2[] = new GraphView.GraphViewData[lframes.size()];

                    try {
                        for (int i = 0; i < lframes.size(); i++) {
                            data1[i] = new GraphView.GraphViewData(i, lframes.get(i).delta);
                            data2[i] = new GraphView.GraphViewData(i, lframes.get(i).luminance);
                        }
                    }
                    catch (IndexOutOfBoundsException e) {}
                    
                    GraphViewSeries series = new GraphViewSeries(data1);
                    graphView_delay.removeAllSeries();
                    graphView_delay.addSeries(series);

                    series = new GraphViewSeries(data2);
                    graphView_lum.removeAllSeries();
                    graphView_lum.addSeries(series);

                }
            }
        };

        mCtx.mHandlerGraph = mHandler;

        return v;
    }
}