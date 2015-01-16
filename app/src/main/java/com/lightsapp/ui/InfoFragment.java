package com.lightsapp.ui;

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

import com.jjoe64.graphview.GraphView;
import com.jjoe64.graphview.GraphViewSeries;
import com.jjoe64.graphview.GraphViewStyle;
import com.jjoe64.graphview.LineGraphView;
import com.lightsapp.core.analyzer.light.Frame;
import com.lightsapp.lightsapp.R;
import com.lightsapp.utils.math.LinearFilter;

import java.util.List;

import static com.lightsapp.utils.HandlerUtils.signalStr;
import static com.lightsapp.utils.math.DFT.*;
import static com.lightsapp.utils.math.DFT.RECTANGULAR;


public class InfoFragment extends Fragment {
    private static final String ARG_SECTION_NUMBER = "2";
    private static final String TAG = InfoFragment.class.getSimpleName();

    private static final int GRAPH_SIZE = 500;
    private static final int MAX_LUM = 250;
    private static float CAMERA_RATIO = 9f/11f;
    private static float scale = 1.3f;

    private MainActivity mContext;

    public Handler mHandler;

    FrameLayout mPreview;

    public static InfoFragment newInstance(int sectionNumber) {
        InfoFragment fragment = new InfoFragment();
        Bundle args = new Bundle();
        args.putInt(ARG_SECTION_NUMBER, sectionNumber);
        fragment.setArguments(args);
        return fragment;
    }

    public InfoFragment() {
        mContext = (MainActivity) getActivity();
    }

    private GraphView newGraphView(String name, int size) {
        GraphView gv = new LineGraphView(mContext, name);

        gv.setViewPort(2, size);
        gv.setScrollable(true);
        gv.setScalable(false);
        gv.getGraphViewStyle().setGridStyle(GraphViewStyle.GridStyle.HORIZONTAL);
        gv.getGraphViewStyle().setGridColor(Color.rgb(30, 30, 30));
        gv.getGraphViewStyle().setTextSize(10);
        gv.getGraphViewStyle().setNumHorizontalLabels(0);
        /*
        gw.setShowLegend(true);
        gw.setLegendAlign(GraphView.LegendAlign.BOTTOM);
        gw.setLegendWidth(200);
        */
        return gv;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_info, container, false);

        mContext = (MainActivity) getActivity();

        mPreview = (FrameLayout) v.findViewById(R.id.camera_preview);
        mPreview.addView(new SurfaceView(getActivity()), 0);   // BLACK MAGIC avoids black flashing.

        mContext.graphView_delay = newGraphView("Delay", GRAPH_SIZE);
        LinearLayout layout = (LinearLayout) v.findViewById(R.id.graph3);
        layout.addView(mContext.graphView_delay);

        mContext.graphView_lum = newGraphView("Luminance", GRAPH_SIZE);
        mContext.graphView_lum.setManualYAxisBounds(MAX_LUM, 0);
        layout = (LinearLayout) v.findViewById(R.id.graph1);
        layout.addView(mContext.graphView_lum);

        mContext.graphView_lum2 = newGraphView("Luminance", GRAPH_SIZE);
        mContext.graphView_lum2.setManualYAxisBounds(MAX_LUM, 0);

        mContext.graphView_dlum = newGraphView("Luminance first derivative", GRAPH_SIZE);
        mContext.graphView_dlum.setManualYAxisBounds(MAX_LUM, -MAX_LUM);
        layout = (LinearLayout) v.findViewById(R.id.graph2);
        layout.addView(mContext.graphView_dlum);

        mContext.graphView_snd = newGraphView("Sound", 512);
        mContext.graphView_snd.setManualYAxisBounds(1000000, -1000000);

        return v;
    }

    public void onResume() {
        super.onResume();
        Log.v(TAG, "onResume info fragment");

        mHandler = new Handler() {
            public void handleMessage(Message msg) {
                super.handleMessage(msg);

                if (msg.getData().containsKey("setup_done")) {
                    Log.d(TAG, "setup_done received!");
                    try {
                        mPreview.removeAllViews();
                        if (mContext.mCameraController != null)
                            mPreview.addView(mContext.mCameraController);
                        ViewGroup.LayoutParams l = mPreview.getLayoutParams();
                        l.height = (int) (CAMERA_RATIO * mPreview.getWidth() / scale);
                        l.width = (int) (mPreview.getWidth() / scale);
                        scale = 1;
                        Log.d(TAG, "preview should be running...");
                    }
                    catch (Exception e) {
                        Log.e(TAG, "error setting camera preview");
                        e.printStackTrace();
                    }
                }

                if (msg.getData().containsKey("update")) {
                    /* Update camera preview */
                    if (mContext.mCameraController != null)
                        mContext.mCameraController.update();

                    /* Light graphs */
                    List<Frame> lframes;
                    try {
                        lframes = mContext.mLightA.getFrames();
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
                            fdata_delay[j] = 1000 / (float) lframes.get(i).delta;
                        }
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
                        Log.e(TAG, "error while generating light graph of light data: " + e.getMessage());
                        e.printStackTrace();
                    }

                    /* Sound graphs */
                    GraphView.GraphViewData data_snd[] = null, data_snd_diff[] = null;
                    try {
                        double[] sframes = mContext.mSoundA.getFrames();
                        if (sframes != null) {
                            sframes = window(sframes, RECTANGULAR);
                            data_snd = new GraphView.GraphViewData[sframes.length];

                            for (int i = 0; i < sframes.length; i++) {
                                data_snd[i] = new GraphView.GraphViewData(i, sframes[i]); //10 * Math.log10(sframes[i]));
                            }
                        }

                        double[] sframes_diff = mContext.mSoundA.getDiffFrames();
                        if (sframes_diff != null) {
                            sframes_diff = window(sframes_diff, RECTANGULAR);
                            data_snd_diff = new GraphView.GraphViewData[sframes_diff.length];

                            for (int i = 0; i < sframes_diff.length; i++) {
                                data_snd_diff[i] = new GraphView.GraphViewData(i, sframes_diff[i]); //10 * Math.log10(sframes[i]));
                            }
                        }
                    }
                    catch (Exception e) {
                        Log.e(TAG, "error while generating graph of sound data: " + e.getMessage());
                        e.printStackTrace();
                    }

                    GraphViewSeries series;
                    try {
                        series = new GraphViewSeries("fps",
                                new GraphViewSeries.GraphViewSeriesStyle(Color.rgb(70, 70, 70), 3),
                                data_delay);
                        mContext.graphView_delay.removeAllSeries();
                        mContext.graphView_delay.addSeries(series);

                        series = new GraphViewSeries("fps_avg",
                                new GraphViewSeries.GraphViewSeriesStyle(Color.rgb(20, 200, 0), 3),
                                data_delay_f);
                        mContext.graphView_delay.addSeries(series);

                        series = new GraphViewSeries("luminance_raw",
                                new GraphViewSeries.GraphViewSeriesStyle(Color.rgb(200, 50, 0), 3),
                                data_lum);
                        mContext.graphView_lum.removeAllSeries();
                        mContext.graphView_lum.addSeries(series);
                        mContext.graphView_lum2.removeAllSeries();
                        mContext.graphView_lum2.addSeries(series);

                        series = new GraphViewSeries("luminance_d",
                                new GraphViewSeries.GraphViewSeriesStyle(Color.rgb(170, 80, 255), 3),
                                data_lum_d);
                        mContext.graphView_dlum.removeAllSeries();
                        mContext.graphView_dlum.addSeries(series);

                        if (data_snd != null && data_snd_diff != null) {
                            mContext.graphView_snd.removeAllSeries();
                            series = new GraphViewSeries("sound",
                                    new GraphViewSeries.GraphViewSeriesStyle(Color.rgb(255, 80, 0), 3),
                                    data_snd);
                            mContext.graphView_snd.addSeries(series);
                            series = new GraphViewSeries("sound_diff",
                                    new GraphViewSeries.GraphViewSeriesStyle(Color.rgb(0, 170, 255), 3),
                                    data_snd_diff);
                            mContext.graphView_snd.addSeries(series);

                        }
                    }
                    catch (Exception e) {
                        Log.d(TAG, "error in graphview: " + e.getMessage());
                        e.printStackTrace();
                    }

                    mContext.graphView_delay.scrollToEnd();
                    mContext.graphView_lum.scrollToEnd();
                    mContext.graphView_lum2.scrollToEnd();
                    mContext.graphView_dlum.scrollToEnd();
                }
            }
        };

        mContext.mHandlerInfo = mHandler;

        boolean done = false;
        do {
            if (mContext.mHandlerRecv != null) {
                Log.d(TAG, "sending graph_setup_done");
                signalStr(mContext.mHandlerRecv, "graph_setup_done", "");
                done = true;
            }
            try {
                Thread.sleep(100);
            }
            catch (InterruptedException e) {
            }
        } while (!done);
    }

    @Override
    public void onPause() {
        super.onPause();
        Log.v(TAG, "onPause info fragment");
        mPreview.removeAllViews();
        mPreview.addView(new SurfaceView(getActivity()), 0);
        mContext.mHandlerInfo = null;
    }

}