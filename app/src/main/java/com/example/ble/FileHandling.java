package com.example.ble;

import static com.example.ble.S3Uploader.BUCKET_NAME;

import android.graphics.drawable.Drawable;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;

import com.github.mikephil.charting.data.Entry;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;


public class FileHandling {
    List<EmojiEntry> emojiEntries = new ArrayList<>();
    List<String> lable = new ArrayList<>();
    Handler mainhandler = new Handler(Looper.getMainLooper());


        boolean isExternalStorageWritable(){
            String state = Environment.getExternalStorageState();
            return Environment.MEDIA_MOUNTED.equals(state);
        }
        File getExternalStorageDir(String dirname){
            File file = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS),dirname);
            if(!file.exists()){
                file.mkdirs();
            }
            return file;
        }
        void writetoexternalfile(String dirname, String filename, String data){
            if(isExternalStorageWritable()){
                File dir = getExternalStorageDir(dirname);
                File file = new File(dir,filename);
                try{

                    FileWriter writer = new FileWriter(file,true);

                    writer.append(data).append("\n");
                    writer.close();
                }catch(IOException e){
                    e.printStackTrace();
                }
            }
        }



    List<Entry>[] dailyusgae(String dirname){

            List<Entry> data1 = new ArrayList<>();
            List<Entry> data2 = new ArrayList<>();
            List<String> lable = new ArrayList<>();
            float mXValue=0;
            File dir = getExternalStorageDir(dirname);
            if(dir.exists()&& dir.isDirectory()){
                File[] files = dir.listFiles();
                mainhandler.post(new Runnable() {
                    @Override
                    public void run() {
                        System.out.println(files);
                    }
                });

               if(files != null){
                   for (File file : files) {
                       if (file.isFile() && file.getName().endsWith(".csv")) {
                           try {
                               // Parse CSV file
                               BufferedReader reader = new BufferedReader(new FileReader(file));
                               String nextLine;
                               double calcval1 =0;
                               double calcval2 =0;

                               double sumValue1 = 0;
                               double sumValue2 = 0;
                               while ((nextLine = reader.readLine()) != null) {
                                   // Assuming the CSV structure is: Date, Value1, Value2
                                   String[] parts = nextLine.split(",");
                                   sumValue1 += Double.parseDouble(parts[1]);
                                   sumValue2 += Double.parseDouble(parts[2]);
                               }
                               reader.close();

                               // Extract date from filename

                               String dateparts[] =  file.getName().split("\\.");
                               String  date = dateparts[0];
                               String dates[] = date.split("-");

                               String dateafter = dates[0]+"/"+dates[1];
                               System.out.println(dateafter);

                               System.out.println(date);
                               calcval1 = sumValue1/60;
                               calcval2 = sumValue2/60;
                               Entry entry1 = new Entry(mXValue, (float) calcval1);
                               Entry entry2 = new Entry(mXValue, (float) calcval2);
                               data1.add(entry1);
                               data2.add(entry2);
                               lable.add(dateafter);

                           } catch (IOException | NumberFormatException e) {
                               e.printStackTrace();
                           }
                       }
                   }

               }
               else{
                   System.out.println("files is null");
               }
            }

            return new List[]{data1, data2};

        }
        List<String>  labels(){
            System.out.println(lable);
            return this.lable;
        }
        int no_of_files(String dirname){
            File dir = getExternalStorageDir(dirname);
            int size = 0;
            if(dir.exists()&& dir.isDirectory()){
                File[] files = dir.listFiles();
                System.out.println(files);
                size=files.length;
            }
            return  size;
        }
         Float[] todaysum(String filename,String dirname) throws FileNotFoundException {
            float leftsum = 0.0f;
            float rightsum = 0.0f;
            File dir = getExternalStorageDir(dirname);
            File file = new File(dir,filename);
            try (BufferedReader br = new BufferedReader(new FileReader(file))){
                String line;
                while ((line = br.readLine())!= null){
                    String[] parts = line.split(",");
                    if(parts.length >= 3){
                        float leftvalue = Float.parseFloat(parts[1]);
                        float rightvalue = Float.parseFloat(parts[2]);
                        leftsum += leftvalue;
                        rightsum += rightvalue;
                    }
                }
            } catch (IOException e){
                e.printStackTrace();
            }

            return new Float[]{leftsum,rightsum};
        }

    }

