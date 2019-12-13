package com.example.fridgetag;

import android.Manifest;
import android.content.ContentResolver;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.storage.StorageManager;
import android.os.storage.StorageVolume;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v4.provider.DocumentFile;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

public class MainActivity extends AppCompatActivity {

    private final int REQUEST_TREE_PARSE = 101;
    private final int PARSE_TREE_ERROR = 467;
    private final int REQUEST_PERMISSION = 206;
    private final int SUCCESS = 200;
    private final int CANCEL = 406;
    private final int OBJECT = 300;

    String fullFilepath = "";
    Intent returnIntent;
    Intent copyIntent;
    StorageVolume fridgeTag;
    Button connect;
    Button download;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        returnIntent = getIntent();
        String fileName = returnIntent.getStringExtra("fileName");
        String destinationFile = returnIntent.getStringExtra("destinationFile");
        fullFilepath = destinationFile + fileName;

        TextView textView = findViewById(R.id.textView2);
        textView.setText(fullFilepath);

        connect = findViewById(R.id.locateButton);
        download = findViewById(R.id.copyButton);

        requestBasicPermissions();
    }

    // Used to make sure that the application has the basic requirements to
    // write and read from external storage.
    private void requestBasicPermissions() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED) {
            // Permission is not granted, so ask for it
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},1);
        }
    }

    public void locateFilesystem(View view) {
        try {
            // Locate the filesystem of the FridgeTag
            StorageManager sm = getSystemService(StorageManager.class);
            boolean found = false;
            List<StorageVolume> volumes = sm.getStorageVolumes();
            for (StorageVolume v : volumes) {
                if (v.isRemovable()) {
                    // This should be the fridge tag
                    found = true;
                    fridgeTag = v;
                    requestTagPermission(view);
                    download.setEnabled(true);
                    connect.setEnabled(false);
                    Toast.makeText(this, "The FridgeTag has been located!", Toast.LENGTH_LONG).show();
                }
            }
            if (!found) {
                // There was no removable storage (which means there was no tag)
                Toast.makeText(this, "The FridgeTag could not be located.", Toast.LENGTH_LONG).show();
            }
        } catch (Exception e) {
            Toast.makeText(this, "Error getting access to filesystem.", Toast.LENGTH_LONG).show();
        }
    }

    public void copyFileFromTag(View view) {
        if (fridgeTag == null) {
            Toast.makeText(this, "Please locate the fridge tag before attempting to copy.", Toast.LENGTH_LONG).show();
            return;
        } else if (copyIntent == null) {
            Toast.makeText(this, "Please request for permission before attempting to copy.", Toast.LENGTH_LONG).show();
            return;
        }
        startActivityForResult(copyIntent, REQUEST_TREE_PARSE);
    }

    public void requestTagPermission(View view) {
        try {
            if (fridgeTag != null) {
                copyIntent = fridgeTag.createAccessIntent(null);
                copyIntent.addFlags(Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION);
                startActivityForResult(copyIntent, REQUEST_PERMISSION);
            }
        } catch (Exception e) {
            Toast.makeText(this, e.getLocalizedMessage(), Toast.LENGTH_LONG).show();
        }
    }

    public void cancel(View view) {
        onBackPressed();
    }

    @Override
    public void onBackPressed() {
        setResult(CANCEL);
        finish();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_PERMISSION) {
            ContentResolver cr = getContentResolver();
            cr.takePersistableUriPermission(data.getData(), Intent.FLAG_GRANT_READ_URI_PERMISSION);
            cr.takePersistableUriPermission(data.getData(), Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
            return;
        }
        if (requestCode == REQUEST_TREE_PARSE) {
            Uri temp = data.getData();
            // Convert the given Uri into a DocumentFile
            DocumentFile tf = DocumentFile.fromTreeUri(this, temp);
            for (DocumentFile doc : tf.listFiles()) {
                if (doc != null) {
                    if (doc.getName().endsWith(".txt")) {
                        try {
                            InputStream s = getContentResolver().openInputStream(doc.getUri());
                            // The name of the new file has been hardcoded
                            // The location as well. Both of these can be changed

                            // Default tester string "/storage/emulated/0/opendatakit/test.txt"
                            File newF = new File(fullFilepath);
                            copyInputStreamToFile(s, newF);
                            newF.createNewFile();

                            if (getParent() == null) {
                                setResult(SUCCESS);
                            }
                            finish();
                        } catch (Exception e) {
                            Toast.makeText(this, "Error creating the filesystem.", Toast.LENGTH_LONG).show();
                        }
                    }
                }
            }
        }
    }

    // Convert the information in an InputStream into a file
    private static void copyInputStreamToFile(InputStream inputStream, File file)
            throws IOException {
        try (FileOutputStream outputStream = new FileOutputStream(file)) {
            int read;
            byte[] bytes = new byte[1024];

            while ((read = inputStream.read(bytes)) != -1) {
                outputStream.write(bytes, 0, read);
            }
        }
    }

    // Read a .txt file from external storage
    public void readFile(View view) {
        //Toast.makeText(this, "-1", Toast.LENGTH_LONG).show();
        try {
            FTObject res = new FTObject();

            //Toast.makeText(this, "0", Toast.LENGTH_LONG).show();
            File temp = new File("/storage/emulated/0/opendatakit/test.txt");
            Scanner fileScanner = new Scanner(temp);
            boolean beginParse = false;
            int dates = 1;

            // Parse each string
            while (fileScanner.hasNextLine()) {
                // Parse each token of the string
                String p = fileScanner.nextLine();
                if (p.contains("Hist:")) {
                    beginParse = true;
                } else if (beginParse && dates < 31) {
                    if (p.trim().equals(dates + ":")) {
                        // This should technically be the first day. Begin parsing
                        String t = fileScanner.nextLine(); // date

                        String date = t.split(":")[1].trim();

                        t = fileScanner.nextLine(); // min temp
                        float minTemp = Float.parseFloat(t.split(",")[0].split(":")[1].substring(2));

                        t = fileScanner.nextLine(); // max temp
                        float maxTemp = Float.parseFloat(t.split(",")[0].split(":")[1].substring(2));

                        t = fileScanner.nextLine(); // average temp
                        float avgTemp = Float.parseFloat(t.split(":")[1].substring(2));
                        fileScanner.nextLine(); // Alarms
                        fileScanner.nextLine();

                        t = fileScanner.nextLine(); // lower alarm status
                        boolean lower = t.split(":")[1].trim().equals("0");
                        double timeOut = 0;

                        if (!lower) {
                            // The lower alarm went off, so calculate the time out of range
                            timeOut += Double.parseDouble(t.split(":")[1].split(",")[0].substring(1));
                        }
                        fileScanner.nextLine();
                        t = fileScanner.nextLine(); // upper alarm status
                        boolean upper = t.split(":")[1].trim().equals("0");
                        if (!upper) {
                            // The upper alarm went off, so calculate the time out of range
                            timeOut += Double.parseDouble(t.split(":")[1].split(",")[0].substring(1));
                        }
                        fileScanner.nextLine();
                        fileScanner.nextLine();
                        fileScanner.nextLine();
                        dates++;
                        res.add(date,avgTemp,lower,timeOut,"",upper,maxTemp, minTemp);
                    }
                }
            }
            // finally done with parsing the file, return it through the intent
            returnIntent.putExtra("dates", res);
            setResult(OBJECT, returnIntent);
            finish();
        } catch (Exception e) {
            Toast.makeText(this, e.getLocalizedMessage(), Toast.LENGTH_LONG).show();
        }
    }
}
