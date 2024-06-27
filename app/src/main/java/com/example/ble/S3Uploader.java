package com.example.ble;

import static java.util.Arrays.sort;

import android.content.Context;
import android.content.DialogInterface;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.os.Environment;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;

import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.mobileconnectors.s3.transferutility.TransferListener;
import com.amazonaws.mobileconnectors.s3.transferutility.TransferNetworkLossHandler;
import com.amazonaws.mobileconnectors.s3.transferutility.TransferObserver;
import com.amazonaws.mobileconnectors.s3.transferutility.TransferState;
import com.amazonaws.mobileconnectors.s3.transferutility.TransferUtility;
import com.amazonaws.regions.Region;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.s3.AmazonS3Client;
import com.amazonaws.services.s3.model.GetObjectRequest;
import com.amazonaws.services.s3.model.ListObjectsV2Request;
import com.amazonaws.services.s3.model.ListObjectsV2Result;
import com.amazonaws.services.s3.model.S3Object;
import com.amazonaws.services.s3.model.S3ObjectInputStream;
import com.amazonaws.services.s3.model.S3ObjectSummary;
import com.github.mikephil.charting.data.Entry;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

public class S3Uploader {
    private static final String ACCESS_KEY = "AKIA6GBMH3ERSCGEPAFD";
    private static final String SECRET_KEY = "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";
    static final String BUCKET_NAME = "clinicianappbucket";
    static BasicAWSCredentials awsCreds = new BasicAWSCredentials(ACCESS_KEY, SECRET_KEY);
    static AmazonS3Client s3Client = new AmazonS3Client(awsCreds, Region.getRegion(Regions.EU_NORTH_1));

    static File getExternalStorageDir(String dirname) {
        File file = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS), dirname);
        if (!file.exists()) {
            file.mkdirs();
        }
        return file;
    }

    static File getLatestCsvFile() {
        // Specify the folder where CSV files are stored
        File folder = getExternalStorageDir("imuble");

        // List CSV files in the folder
        File[] files = folder.listFiles(new FilenameFilter() {
            @Override
            public boolean accept(File dir, String name) {
                return name.toLowerCase().endsWith(".csv");
            }
        });

        // Sort files by last modified time (latest first)
        if (files != null && files.length > 0) {
            Arrays.sort(files, new Comparator<File>() {
                @Override
                public int compare(File f1, File f2) {
                    return Long.compare(f2.lastModified(), f1.lastModified());
                }
            });

            // Return the latest CSV file
            return files[0];
        } else {
            // No CSV file found
            return null;
        }
    }

    public static void upload(Context context,  File file) {
        TransferNetworkLossHandler transferNetworkLossHandler = TransferNetworkLossHandler.getInstance(context);
        TransferUtility transferUtility = TransferUtility.builder()
                .context(context.getApplicationContext())
                .defaultBucket(BUCKET_NAME)
                .s3Client(s3Client)
                .build();

        TransferObserver observer = transferUtility.upload(BUCKET_NAME, "gokul1/" + file.getName(), file);
        observer.setTransferListener(new TransferListener() {
            @Override
            public void onStateChanged(int id, TransferState state) {
                if (state == TransferState.COMPLETED) {
                    Log.d("S3Upload", "Comment upload completed!");

                } else if (state == TransferState.FAILED) {
                    Log.e("S3Upload", "Comment upload failed!");
                }
            }

            @Override
            public void onProgressChanged(int id, long bytesCurrent, long bytesTotal) {
                float percentDone = ((float) bytesCurrent / bytesTotal) * 100;
                Log.d("S3Upload", "Progress: " + percentDone + "%");
            }

            @Override
            public void onError(int id, Exception ex) {
                ex.printStackTrace();
            }
        });
    }
    public static List<String[]> fetchComments() throws IOException {
        List<String[]> comments = new ArrayList<>();

        S3Object object = s3Client.getObject(new GetObjectRequest(BUCKET_NAME, "gokul1/Comment.csv"));
        S3ObjectInputStream inputStream = object.getObjectContent();
        BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
        String line;
        while ((line = reader.readLine()) != null) {
            String[] values = line.split(",");
            comments.add(values);
        }
        reader.close();


        return comments;
    }
    public static void showCommentsDialog(Context context, List<String[]> comments) {
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setTitle("Comments");

        // Create an ArrayAdapter to display the comments
        ArrayAdapter<String> adapter = new ArrayAdapter<String>(context, android.R.layout.simple_list_item_1) {
            @Override
            public View getView(int position, View convertView, ViewGroup parent) {
                View view = super.getView(position, convertView, parent);
                TextView textView = (TextView) view.findViewById(android.R.id.text1);
                // Get the comment and set the text with read status
                String[] comment = comments.get(position);
                String commentText = comment[0]; // Assuming the 4th element is the comment text
                textView.setText(commentText);
               return view;
            }
        };

        // Add comments to the adapter
        for (String[] comment : comments) {
            adapter.add(comment[0]); // Assuming the 4th element is the comment text
        }

        builder.setAdapter(adapter, null);
        builder.setPositiveButton("Make As Read", (dialogInterface, i) -> {

        });
        builder.show();
        Executor executor = Executors.newSingleThreadExecutor();
        executor.execute(() -> {
            try {
                updateReadStatusAndUpload(comments,context);
            } catch (IOException e) {
                System.out.println(e.getMessage());
            }

        });

    }
    public static List<EmojiEntry> readDataFromCSV(String fileName) {
        List<EmojiEntry> entries = new ArrayList<>();
            boolean fileexist = s3Client.doesObjectExist(BUCKET_NAME, fileName);


            if (fileexist) {

                try {
                    S3Object object = s3Client.getObject(new GetObjectRequest(BUCKET_NAME, fileName));
                    S3ObjectInputStream inputStream = object.getObjectContent();
                    BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
                    String line;
                    while ((line = reader.readLine()) != null) {
                        String[] tokens = line.split(",");
                        float x = Float.parseFloat(tokens[0]);
                        float y = Float.parseFloat(tokens[1]);
                        String emoji = tokens.length > 2 ? tokens[2] : "";
                        System.out.println(emoji);
                        entries.add(new EmojiEntry(x, y, emoji));
                    }
                    reader.close();
                    inputStream.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }


        return entries;
    }

    public static void updateReadStatusAndUpload(List<String[]> comments, Context context) throws IOException {
        // Update read status of all comments
        for (String[] comment : comments) {
            comment[1] = "true"; // Assuming the 6th element is the read status
        }

        // Write updated comments to a temporary CSV file
        String tem = "Comment.csv";

        File tempFile = new File(context.getFilesDir(), tem);

        BufferedWriter writer = new BufferedWriter(new FileWriter(tempFile));
        for (String[] comment : comments) {
            writer.write(String.join(",", comment));
            writer.newLine();
        }
        writer.close();

        // Upload the updated CSV file back to S3
        upload(context,tempFile);

    }

}
