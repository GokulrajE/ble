package com.example.ble;


import static androidx.constraintlayout.helper.widget.MotionEffect.TAG;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.mobileconnectors.s3.transferutility.TransferListener;
import com.amazonaws.mobileconnectors.s3.transferutility.TransferState;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.PutObjectRequest;

import java.io.File;

public class uploadCSVWorker extends Worker {

    public uploadCSVWorker(@NonNull Context context, @NonNull WorkerParameters workerParams) {
        super(context, workerParams);
    }

    @NonNull
    @Override
    public Result doWork() {
        try {
            // Step 1: Check network availability
            File file = S3Uploader. getLatestCsvFile();
            if (!isNetworkAvailable()) {
                Log.e("network","connect to network");
                return Result.retry(); // Retry the job later when the network is available

            }
            if((file == null || !file.exists())){
                Log.e("file","no latest file");
                return Result.failure();
            }

            // Step 2: Update or generate CSV data
            // Replace this with your code to update or generate the CSV file

            // Step 3: Upload to S3 bucket

           S3Uploader.upload(getApplicationContext(),file);

            return Result.success();
        } catch (Exception e) {
            Log.e(TAG, "Error uploading CSV to S3", e);
            return Result.failure();
        }

    }
    private boolean isNetworkAvailable() {
        ConnectivityManager connectivityManager = (ConnectivityManager) getApplicationContext().getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeNetworkInfo = connectivityManager.getActiveNetworkInfo();
        return activeNetworkInfo != null && activeNetworkInfo.isConnectedOrConnecting();
    }


}
