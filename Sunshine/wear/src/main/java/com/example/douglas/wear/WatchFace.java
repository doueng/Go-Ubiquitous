package com.example.douglas.wear;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Point;
import android.graphics.Rect;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.wearable.view.BoxInsetLayout;
import android.support.wearable.watchface.CanvasWatchFaceService;
import android.util.Log;
import android.view.Display;
import android.view.LayoutInflater;
import android.view.SurfaceHolder;
import android.view.View;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.TextClock;
import android.widget.TextView;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.wearable.Asset;
import com.google.android.gms.wearable.DataApi;
import com.google.android.gms.wearable.DataEvent;
import com.google.android.gms.wearable.DataEventBuffer;
import com.google.android.gms.wearable.DataItem;
import com.google.android.gms.wearable.DataMap;
import com.google.android.gms.wearable.DataMapItem;
import com.google.android.gms.wearable.Wearable;

import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;
import java.util.concurrent.TimeUnit;

/**
 * Created by douglas on 10/07/2016.
 */
public class WatchFace extends CanvasWatchFaceService {

    private static final String TAG = WatchFace.class.getSimpleName();

    private static final SimpleDateFormat DATE_FORMAT =
            new SimpleDateFormat("EEE-dd-MM-yyyy", Locale.US);

    private BoxInsetLayout containerView;
    private TextView dateView, highTemperature, minTemperature;
    private ImageView weatherIcon;
    private TextClock textClock;
    private GoogleApiClient googleApiClient;
    private View rootView;
    private int specW, specH;
    private final Point displaySize = new Point();

    @Override
    public Engine onCreateEngine() {
        /* provide your watch face implementation */
        return new Engine();
    }

    /* implement service callback methods */
    private class Engine extends CanvasWatchFaceService.Engine implements
            DataApi.DataListener, GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener{

        @Override
        public void onCreate(SurfaceHolder holder) {
            super.onCreate(holder);
            /* initialize your watch face */

            LayoutInflater inflater =
                    (LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            rootView = inflater.inflate(R.layout.watch_face, null);

            Display display = ((WindowManager) getSystemService(Context.WINDOW_SERVICE))
                    .getDefaultDisplay();
            display.getSize(displaySize);

            specW = View.MeasureSpec.makeMeasureSpec(displaySize.x,
                    View.MeasureSpec.EXACTLY);
            specH = View.MeasureSpec.makeMeasureSpec(displaySize.y,
                    View.MeasureSpec.EXACTLY);

            String date = DATE_FORMAT.format(new Date());
            String[] parts = date.split("-");
            date = parts[0] + ", " + parts[1] + " " + parts[2] + " " + parts[3];

            dateView = (TextView) rootView.findViewById(R.id.date_view);
            dateView.setText(date);
            containerView = (BoxInsetLayout) rootView.findViewById(R.id.container);
            weatherIcon = (ImageView) rootView.findViewById(R.id.weather_icon);
            highTemperature = (TextView) rootView.findViewById(R.id.high_temperature);
            minTemperature = (TextView) rootView.findViewById(R.id.min_temperature);
            textClock = (TextClock) rootView.findViewById(R.id.text_clock);



            googleApiClient = new GoogleApiClient.Builder(WatchFace.this)
                    .addConnectionCallbacks(new GoogleApiClient.ConnectionCallbacks() {
                        @Override
                        public void onConnected(Bundle connectionHint) {
                            Log.d(TAG, "onConnected: " + connectionHint);
                            // Now you can use the Data Layer API
                        }

                        @Override
                        public void onConnectionSuspended(int cause) {
                            Log.d(TAG, "onConnectionSuspended: " + cause);
                        }
                    })
                    .addOnConnectionFailedListener(new GoogleApiClient.OnConnectionFailedListener() {
                        @Override
                        public void onConnectionFailed(ConnectionResult result) {
                            Log.d(TAG, "onConnectionFailed: " + result);
                        }
                    })
                    // Request access only to the Wearable API
                    .addApi(Wearable.API)
                    .build();
        }

        @Override
        public void onPropertiesChanged(Bundle properties) {
            super.onPropertiesChanged(properties);
            /* get device features (burn-in, low-bit ambient) */
        }

        @Override
        public void onTimeTick() {
            super.onTimeTick();
            /* the time changed */
            invalidate();
        }

        @Override
        public void onAmbientModeChanged(boolean inAmbientMode) {
            super.onAmbientModeChanged(inAmbientMode);
            /* the wearable switched between modes */
            if (inAmbientMode) {
                containerView.setBackground(null);
            } else {
                containerView.setBackgroundColor(getResources().getColor(R.color.background));
            }
        }

        @Override
        public void onDraw(Canvas canvas, Rect bounds) {
            /* draw your watch face */
            textClock.setTimeZone(TimeZone.getDefault().toString());
            rootView.measure(specW, specH);
            rootView.layout(0, 0, rootView.getMeasuredWidth(), rootView.getMeasuredHeight());

            canvas.drawColor(Color.BLACK);
            rootView.draw(canvas);
        }

        @Override
        public void onVisibilityChanged(boolean visible) {
            super.onVisibilityChanged(visible);
            /* the watch face became visible or invisible */

            if (visible) {
                googleApiClient.connect();

            } else {
                if (googleApiClient != null && googleApiClient.isConnected()) {
                    Wearable.DataApi.removeListener(googleApiClient, this);
                    googleApiClient.disconnect();
                }
            }
        }

        public Bitmap loadBitmapFromAsset(Asset asset) {
            if (asset == null) {
                throw new IllegalArgumentException("Asset must be non-null");
            }
            ConnectionResult result =
                    googleApiClient.blockingConnect(40000, TimeUnit.MILLISECONDS);
            if (!result.isSuccess()) {
                return null;
            }
            // convert asset into a file descriptor and block until it's ready
            InputStream assetInputStream = Wearable.DataApi.getFdForAsset(
                    googleApiClient, asset).await().getInputStream();
            googleApiClient.disconnect();

            if (assetInputStream == null) {
                Log.w(TAG, "Requested an unknown Asset.");
                return null;
            }
            // decode the stream into a bitmap
            return BitmapFactory.decodeStream(assetInputStream);
        }

        @Override
        public void onConnected(@Nullable Bundle bundle) {
            Wearable.DataApi.addListener(googleApiClient, this);
        }

        @Override
        public void onConnectionSuspended(int i) {

        }

        @Override
        public void onDataChanged(DataEventBuffer dataEventBuffer) {

            for (DataEvent event : dataEventBuffer) {
                if (event.getType() == DataEvent.TYPE_CHANGED) {
                    DataItem item = event.getDataItem();
                    if (item.getUri().getPath().equals("/temperature")) {
                        DataMap dataMap = DataMapItem.fromDataItem(item).getDataMap();
                        highTemperature.setText(dataMap.getString("highTemperature"));
                        minTemperature.setText(dataMap.getString("minTemperature"));
                    }

                    if (item.getUri().getPath().equals("/image")) {
                        DataMapItem dataMapItem = DataMapItem.fromDataItem(event.getDataItem());
                        Asset profileAsset = dataMapItem.getDataMap().getAsset("weatherImage");
                        Bitmap bitmap = loadBitmapFromAsset(profileAsset);
                        weatherIcon.setImageBitmap(bitmap);
                    }
                    invalidate();
                }
            }
        }

        @Override
        public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {

        }
    }
}
