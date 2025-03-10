package com.kdt;

import android.content.Context;
import android.graphics.Typeface;
import android.os.Build;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.ToggleButton;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.constraintlayout.widget.ConstraintLayout;

import net.kdt.pojavlaunch.Logger;
import net.kdt.pojavlaunch.R;
import net.kdt.pojavlaunch.Tools;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Base64;
import java.util.zip.GZIPInputStream;

/**
 * A class able to display logs to the user.
 * It has support for the Logger class
 */
public class LoggerView extends ConstraintLayout {
    private Logger.eventLogListener mLogListener;
    private ToggleButton mToggleButton;
    private ScrollView mScrollView;
    private TextView mLogTextView;


    public LoggerView(@NonNull Context context) {
        this(context, null);
    }

    public LoggerView(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    @Override
    public void setVisibility(int visibility) {
        super.setVisibility(visibility);
        // Triggers the log view shown state by default when viewing it
        mToggleButton.setChecked(visibility == VISIBLE);
    }

    /**
     * Inflate the layout, and add component behaviors
     */
    private void init(){
        inflate(getContext(), R.layout.view_logger, this);
        mLogTextView = findViewById(R.id.content_log_view);
        mLogTextView.setTypeface(Typeface.MONOSPACE);
        //TODO clamp the max text so it doesn't go oob
        mLogTextView.setMaxLines(Integer.MAX_VALUE);
        mLogTextView.setEllipsize(null);
        mLogTextView.setVisibility(GONE);

        // Toggle log visibility
        mToggleButton = findViewById(R.id.content_log_toggle_log);
        mToggleButton.setOnCheckedChangeListener(
                (compoundButton, isChecked) -> {
                    mLogTextView.setVisibility(isChecked ? VISIBLE : GONE);
                    if(isChecked) {
                        Logger.setLogListener(mLogListener);
                    }else{
                        mLogTextView.setText("");
                        //Logger.setLogListener(null); // Makes the JNI code be able to skip expensive logger callbacks
                        // NOTE: was tested by rapidly smashing the log on/off button, no sync issues found :)
                    }
                });
        mToggleButton.setChecked(false);

        // Remove the loggerView from the user View
        ImageButton cancelButton = findViewById(R.id.log_view_cancel);
        cancelButton.setOnClickListener(view -> LoggerView.this.setVisibility(GONE));

        // Set the scroll view
        mScrollView = findViewById(R.id.content_log_scroll);

        // Listen to logs
        mLogListener = text -> {
            if(text.startsWith("pDAT")){
                try {
                    // 1. Extract the base64 string after the "pDAT " prefix
                    String base64Data = text.substring("pDAT ".length());

                    // 2. Decode Base64 to get compressed bytes
                    byte[] compressedBytes = null;
                    try{
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                            compressedBytes = Base64.getDecoder().decode(base64Data);
                        }
                    } catch (Exception e){
                        Log.e("jrelog-logcat", "Failed to decode player_data save.");
                    }

                    if (compressedBytes == null) return;

                    // 3. Decompress GZIP to get the original serialized data
                    byte[] decompressedBytes = decompressGzip(compressedBytes);

                    // 4. Write the decompressed bytes to a file (on Android storage)
                    File outputFile = new File(Tools.DIR_DATA + "/cache/players/", "android_data.dat");
                    try (FileOutputStream fos = new FileOutputStream(outputFile)) {
                        fos.write(decompressedBytes);
                    }

                    System.out.println("Successfully wrote decompressed data to: " + outputFile.getAbsolutePath());
                    try{
                        Logger.begin(text);
                        Log.i("jrelog-logcat", "Log Flushed");
                    } catch (Exception e) {
                        Log.e("jrelog-logcat", "Failed to flush the log");
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            } else if(text.startsWith("pCACHE")){
                try {
                    // 1. Extract the base64 string after the "pDAT " prefix
                    String base64Data = text.substring("pCACHE ".length());

                    // 2. Decode Base64 to get compressed bytes
                    byte[] compressedBytes = null;
                    try{
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                            compressedBytes = Base64.getDecoder().decode(base64Data);
                        }
                    } catch (Exception e){
                        Log.e("jrelog-logcat", "Failed to decode player_cache save");
                    }

                    if (compressedBytes == null) return;

                    // 3. Decompress GZIP to get the original serialized data
                    byte[] decompressedBytes = decompressGzip(compressedBytes);

                    // 4. Write the decompressed bytes to a file (on Android storage)
                    File outputFile = new File(Tools.DIR_DATA + "/cache/players/", "android_cache.dat");
                    try (FileOutputStream fos = new FileOutputStream(outputFile)) {
                        fos.write(decompressedBytes);
                    }

                    System.out.println("Successfully wrote decompressed data to: " + outputFile.getAbsolutePath());
                    try{
                        Logger.begin(text);
                        Log.i("jrelog-logcat", "Log Flushed");
                    } catch (Exception e) {
                        Log.e("jrelog-logcat", "Failed to flush the log");
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            Log.i("jrelog-logcat",text);
            if(mLogTextView.getVisibility() != VISIBLE) return;
            post(() -> {
                mLogTextView.append(text + '\n');
                mScrollView.fullScroll(View.FOCUS_DOWN);
            });

        };
    }

    private byte[] decompressGzip(byte[] compressedData) throws IOException {
        if (compressedData == null) return null;
        try (ByteArrayInputStream byteStream = new ByteArrayInputStream(compressedData);
             GZIPInputStream gzipStream = new GZIPInputStream(byteStream);
             ByteArrayOutputStream outStream = new ByteArrayOutputStream()) {

            byte[] buffer = new byte[4096];
            int len;
            while ((len = gzipStream.read(buffer)) != -1) {
                outStream.write(buffer, 0, len);
            }
            return outStream.toByteArray();
        }
    }


}
