package com.example.luther.reportabusers;

import android.Manifest;
import android.app.Activity;
import android.app.DownloadManager;
import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.Image;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.support.annotation.IntegerRes;
import android.support.annotation.NonNull;
import android.support.design.widget.TextInputLayout;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.util.Base64;
import android.util.Base64OutputStream;
import android.util.Log;
import android.util.SparseArray;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.vision.Frame;
import com.google.android.gms.vision.text.Text;
import com.google.android.gms.vision.text.TextBlock;
import com.google.android.gms.vision.text.TextRecognizer;

import org.apache.commons.io.IOUtils;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.Date;

public class MainActivity extends AppCompatActivity {
    private static final String LOG_TAG = "Text API";
    private static final int PHOTO_REQUEST = 10;
    AlertDialog imageAlert;
    private static int REQUEST_CAMERA = 0;
    private static int RESULT_LOAD_IMG = 1;
    private static int PICKFILE_REQUEST_CODE = 2;
    private TextView scanResults;
    private Uri imageUri;
    ByteArrayOutputStream alternateBao=null;
    private TextRecognizer detector;
    private static final int REQUEST_WRITE_PERMISSION = 20;
    private static final String SAVED_INSTANCE_URI = "uri";
    private static final String SAVED_INSTANCE_RESULT = "result";
    String imageDataToStoreLoacal="";
    ByteArrayOutputStream baos;
    Uri pickedImage=null;
    public TextInputLayout nameView,currentOrganisationView,positionView,educationalBackView,emailidView,phoneNoView,pastExpVIew;
    public EditText nameText,currentOrganisationText,positionText,educationalText,emailIdText,phoneNoText,pastExpText;
    public int maximumHeight,maximumWidth;
    String imagePath=null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Button button = (Button) findViewById(R.id.button);
        Button submitButton = (Button) findViewById(R.id.submit);
        scanResults = (TextView) findViewById(R.id.results);
        nameView = (TextInputLayout) findViewById(R.id.nameText);
        currentOrganisationView = (TextInputLayout) findViewById(R.id.currentOrganizationText);
        positionView = (TextInputLayout) findViewById(R.id.positionText);
        educationalBackView = (TextInputLayout) findViewById(R.id.educationBackgroundText);
        emailidView = (TextInputLayout) findViewById(R.id.emailText);
        phoneNoView = (TextInputLayout) findViewById(R.id.phoneNoText);
        pastExpVIew = (TextInputLayout) findViewById(R.id.pastExperienceText);
        nameText = (EditText)findViewById(R.id.name);
        currentOrganisationText = (EditText)findViewById(R.id.currentOrganisation);
        positionText = (EditText)findViewById(R.id.position);
        educationalText = (EditText)findViewById(R.id.educationBackground);
        emailIdText = (EditText)findViewById(R.id.emailId);
        phoneNoText = (EditText)findViewById(R.id.phoneNo);
        pastExpText = (EditText)findViewById(R.id.pastExperience);



        if (savedInstanceState != null) {
            imageUri = Uri.parse(savedInstanceState.getString(SAVED_INSTANCE_URI));
            scanResults.setText(savedInstanceState.getString(SAVED_INSTANCE_RESULT));
        }
        detector = new TextRecognizer.Builder(getApplicationContext()).build();
        Bundle extras = getIntent().getExtras();
        if (extras != null) {
            try{
                if (extras.containsKey("dataURI")) {
                    String imageData = extras.getString("dataURI");
                    imageDataToStoreLoacal = imageData;
                    imageUri = Uri.parse(imageData);
                    launchMediaScanIntent();
                    textAnalysis();
                }
            }catch(Exception e)
            {
            }
        }
//        try {
//            if(pickedImage!=null) {
//                String[] filePath = {MediaStore.Images.Media.DATA};
//                Bitmap image = BitmapFactory.decodeStream(getContentResolver().openInputStream(pickedImage), null, null);
//                maximumHeight = image.getHeight();
//                maximumWidth = image.getWidth();
//                Cursor cursorforimage = getContentResolver().query(pickedImage, filePath, null, null, null);
//                if(cursorforimage!=null) {
//                    cursorforimage.moveToFirst();
//                    imagePath = cursorforimage.getString(cursorforimage.getColumnIndex(filePath[0]));
//                    if(imagePath!=null)
//                        getImageData(imagePath);
//                    cursorforimage.close();
//                }
//
//            }
//        } catch (Exception e) {
//            // TODO Auto-generated catch block
//            e.printStackTrace();
//        }
        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                ActivityCompat.requestPermissions(MainActivity.this, new
                        String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, REQUEST_WRITE_PERMISSION);
            }
        });
        submitButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                System.out.println(nameText);
                CreateNewResume createNewResume = new CreateNewResume();
                createNewResume.execute();
            }
        });
    }


    public void getImageData(String imagePath)
    {
        try {
            File audio = new File(imagePath);
            FileInputStream fileInputStream = new FileInputStream(audio);
            baos = new ByteArrayOutputStream();
            OutputStream out = new Base64OutputStream(baos, Base64.DEFAULT);
            IOUtils.copy(fileInputStream, out);
            fileInputStream.close();
            out.close();
            int size = (baos.size())/1024;
            if(maximumWidth > 2048 || maximumHeight > 2048) {
                File toBeCompressed = new File(imagePath);
                File compressedFile = saveBitmapToFile(toBeCompressed);
                FileInputStream fileInputStrea = new FileInputStream(compressedFile);
                alternateBao = new ByteArrayOutputStream();
                OutputStream outPutStream = new Base64OutputStream(alternateBao, Base64.DEFAULT);
                IOUtils.copy(fileInputStrea, outPutStream);
                fileInputStrea.close();
                int size1 = (alternateBao.size())/1024;
                outPutStream.close();
            }
        }catch (Exception e) {
        }
        catch (OutOfMemoryError e) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    Toast.makeText(getBaseContext(),"Image too heavy to upload",Toast.LENGTH_SHORT).show();
                }
            });

        }
    }

    public File saveBitmapToFile(File file){
        try {
            BitmapFactory.Options optionsForDeprecatingImage = new BitmapFactory.Options();
            optionsForDeprecatingImage.inJustDecodeBounds = true;
            FileInputStream inputStream = new FileInputStream(file);
            BitmapFactory.decodeStream(inputStream, null, optionsForDeprecatingImage);
            inputStream.close();
            final int REQUIRED_VALUE = 2048;
            int scale = 4;

            while(optionsForDeprecatingImage.outWidth / scale / 2 >= REQUIRED_VALUE &&
                    optionsForDeprecatingImage.outHeight / scale / 2 >= REQUIRED_VALUE) {
                scale *= 2;
            }

            BitmapFactory.Options optionForSendingDeprecatedImage = new BitmapFactory.Options();
            optionForSendingDeprecatedImage.inSampleSize = scale;
            inputStream = new FileInputStream(file);

            Bitmap selectedBitmap = BitmapFactory.decodeStream(inputStream, null, optionForSendingDeprecatedImage);
            inputStream.close();
            String fileName = java.util.UUID.randomUUID().toString();
            File storageDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES);
            File imageFile= File.createTempFile(
                    fileName,  /* prefix */
                    ".png",         /* suffix */
                    storageDir      /* directory */
            );

            FileOutputStream outputStream = new FileOutputStream(imageFile);
            selectedBitmap.compress(Bitmap.CompressFormat.JPEG, 100 , outputStream);
            return imageFile;
        } catch (Exception e) {
            return null;
        }
    }
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        switch (requestCode) {
            case REQUEST_WRITE_PERMISSION:
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    takePicture();
                } else {
                    Toast.makeText(MainActivity.this, "Permission Denied!", Toast.LENGTH_SHORT).show();
                }
        }
    }

//    @Override
//    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
//        if (requestCode == PHOTO_REQUEST && resultCode == RESULT_OK) {
//            launchMediaScanIntent();
//            try {
////                Bitmap bitmap = decodeBitmapUri(this, imageUri);
//                Bitmap bitmap = MediaStore.Images.Media.getBitmap(this.getContentResolver(),imageUri);
//                System.out.println(bitmap);
//                System.out.println(detector.isOperational());
//                if (detector.isOperational() && bitmap != null) {
//                    Frame frame = new Frame.Builder().setBitmap(bitmap).build();
//                    SparseArray<TextBlock> textBlocks = detector.detect(frame);
//                    String blocks = "";
//                    String lines = "";
//                    String words = "";
//                    for (int index = 0; index < textBlocks.size(); index++) {
//                        //extract scanned text blocks here
//                        TextBlock tBlock = textBlocks.valueAt(index);
//                        blocks = blocks + tBlock.getValue() + "\n" + "\n";
//                        for (Text line : tBlock.getComponents()) {
//                            //extract scanned text lines here
//                            lines = lines + line.getValue() + "\n";
//                            for (Text element : line.getComponents()) {
//                                //extract scanned text words here
//                                words = words + element.getValue() + ", ";
//                            }
//                        }
//                    }
//                    if (textBlocks.size() == 0) {
//                        scanResults.setText("Scan Failed: Found nothing to scan");
//                    } else {
//                        scanResults.setText(scanResults.getText() + "Blocks: " + "\n");
//                        scanResults.setText(scanResults.getText() + blocks + "\n");
//                        scanResults.setText(scanResults.getText() + "---------" + "\n");
//                        scanResults.setText(scanResults.getText() + "Lines: " + "\n");
//                        scanResults.setText(scanResults.getText() + lines + "\n");
//                        scanResults.setText(scanResults.getText() + "---------" + "\n");
//                        scanResults.setText(scanResults.getText() + "Words: " + "\n");
//                        scanResults.setText(scanResults.getText() + words + "\n");
//                        scanResults.setText(scanResults.getText() + "---------" + "\n");
//                    }
//                } else {
//                    scanResults.setText("Could not set up the detector!");
//                }
//            } catch (Exception e) {
//                Toast.makeText(this, "Failed to load Image", Toast.LENGTH_SHORT)
//                        .show();
//                Log.e(LOG_TAG, e.toString());
//            }
//        }
//    }

    private void textAnalysis(){
        try {
//            Bitmap bitmap = decodeBitmapUri(this, pickedImage);
            Bitmap bitmap = MediaStore.Images.Media.getBitmap(this.getContentResolver(),imageUri);
            if (detector.isOperational() && bitmap != null) {
                Frame frame = new Frame.Builder().setBitmap(bitmap).build();
                SparseArray<TextBlock> textBlocks = detector.detect(frame);
                String blocks = "";
                String lines = "";
                String words = "";
                for (int index = 0; index < textBlocks.size(); index++) {
                    //extract scanned text blocks here
                    TextBlock tBlock = textBlocks.valueAt(index);
                    blocks = blocks + tBlock.getValue() + "\n" + "\n";
                    for (Text line : tBlock.getComponents()) {
                        //extract scanned text lines here
                        lines = lines + line.getValue() + "\n";
                        for (Text element : line.getComponents()) {
                            //extract scanned text words here
                            words = words + element.getValue() + ", ";
                        }
                    }
                }
                if (textBlocks.size() == 0) {
                    scanResults.setText("Scan Failed: Found nothing to scan");
                } else {
                    scanResults.setText(scanResults.getText() + lines + "\n");
                }
            } else {
                scanResults.setText("Could not set up the detector!");
            }
        } catch (Exception e) {
            Toast.makeText(this, "Failed to load Image", Toast.LENGTH_SHORT)
                    .show();
            Log.e(LOG_TAG, e.toString());
        }
    }


    private void takePicture() {
        AlertDialog.Builder Builder =  new  AlertDialog.Builder (MainActivity.this);
        Builder.setTitle ( "Change Photo" )
                .setItems(R.array.image_options, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface Dialog, int Which) {
                        Intent imageIntent;

                        switch (Which) {
                            case 0:
                                imageIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
                                startActivityForResult(imageIntent, REQUEST_CAMERA);
                                break;
                            case 1:
//                                startActivity(new Intent(DownloadManager.ACTION_VIEW_DOWNLOADS));
//                                Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
//                                intent.setType("*/*");
//                                startActivityForResult(intent, PICKFILE_REQUEST_CODE);
                                imageIntent = new Intent(Intent.ACTION_PICK, android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
                                startActivityForResult(imageIntent, RESULT_LOAD_IMG);
                                break;
                        }
                    }
                });
        imageAlert = Builder.create();
        imageAlert.show();
//        Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
//        File photo = new File(Environment.getExternalStorageDirectory(), "picture.jpg");
//        imageUri = Uri.fromFile(photo);
//        intent.putExtra(MediaStore.EXTRA_OUTPUT, imageUri);
//        startActivityForResult(intent, PHOTO_REQUEST);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == RESULT_LOAD_IMG && resultCode== Activity.RESULT_OK && data != null) {
            Intent intent=new Intent(getBaseContext(),MainActivity.class);

            intent.putExtra("dataURI",data.getData().toString());
            startActivity(intent);
        }

        if (requestCode == REQUEST_CAMERA && resultCode==Activity.RESULT_OK && data != null) {
            try
            {
                String mediaStoreUri;
                Bitmap thumbnail=null;
                try
                {
                    thumbnail = (Bitmap) (data.getExtras().get("data"));
                }catch (OutOfMemoryError e)
                {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            Toast.makeText(getBaseContext(),"Image too heavy to upload",Toast.LENGTH_SHORT).show();
                        }
                    });
                }
                OutputStream fOut ;
                String strDirectory = Environment.getExternalStorageDirectory().toString();
                File capturedImageFile = new File(strDirectory, System.currentTimeMillis()+"");
                try
                {
                    if(thumbnail!=null)
                    {
                        fOut = new FileOutputStream(capturedImageFile);
                        thumbnail.compress(Bitmap.CompressFormat.PNG, 100, fOut);
                        fOut.flush();
                        fOut.close();
                        mediaStoreUri=MediaStore.Images.Media.insertImage(getContentResolver(),
                                capturedImageFile.getAbsolutePath(), capturedImageFile.getName(), capturedImageFile.getName());
                        File file = new File(capturedImageFile.getAbsolutePath());
                        file.delete();
                        Intent intent=new Intent(getBaseContext(),MainActivity.class);
                        intent.putExtra("dataURI", mediaStoreUri);
                        thumbnail.recycle();
                        startActivity(intent);
                        finish();
                    }
                    else
                    {
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                Toast.makeText(getBaseContext(),"Image not found",Toast.LENGTH_SHORT).show();
                            }
                        });
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
            catch(Exception e)
            {
                Log.v("Camera issue",e.toString());
            }
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        if (imageUri != null) {
            outState.putString(SAVED_INSTANCE_URI, imageUri.toString());
            outState.putString(SAVED_INSTANCE_RESULT, scanResults.getText().toString());
        }
        super.onSaveInstanceState(outState);
    }

    private void launchMediaScanIntent() {
        Intent mediaScanIntent = new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE);
        mediaScanIntent.setData(imageUri);
        this.sendBroadcast(mediaScanIntent);
    }

    private Bitmap decodeBitmapUri(Context ctx, Uri uri) throws FileNotFoundException {
        int targetW = 600;
        int targetH = 600;
        BitmapFactory.Options bmOptions = new BitmapFactory.Options();
        bmOptions.inJustDecodeBounds = true;
        BitmapFactory.decodeStream(ctx.getContentResolver().openInputStream(uri), null, bmOptions);
        int photoW = bmOptions.outWidth;
        int photoH = bmOptions.outHeight;

        int scaleFactor = Math.min(photoW / targetW, photoH / targetH);
        bmOptions.inJustDecodeBounds = false;
        bmOptions.inSampleSize = scaleFactor;

        return BitmapFactory.decodeStream(ctx.getContentResolver()
                .openInputStream(uri), null, bmOptions);
    }


    public class CreateNewResume extends AsyncTask<String, Void, Void> {
        BufferedReader reader;
        HttpURLConnection urlConnection;
        Boolean postexec = false;
        String valueError="";
        Boolean uniquePhone = true;
        Boolean uniqueEmail = true;
        Boolean errorValue = false;
        public String name,organisation,position,education,emailId,phoneNo,pastExp;

        @Override
        protected void onPreExecute() {
            name = nameText.getText().toString();
            organisation = currentOrganisationText.getText().toString();
            position = positionText.getText().toString();
            education = educationalText.getText().toString();
            emailId = emailIdText.getText().toString();
            phoneNo = phoneNoText.getText().toString();
            pastExp = pastExpText.getText().toString();

        }

        @Override
        protected Void doInBackground(String... params) {
            try {

                URL url = new URL("http://127.0.0.1:8000/candidates/");
                urlConnection = (HttpURLConnection) url.openConnection();
                urlConnection.setRequestMethod("POST");
                urlConnection.setRequestProperty("Content-Type", "application/json");
//                urlConnection.setRequestProperty("Accept", "application/json");
                urlConnection.setDoInput(true);
                urlConnection.setDoOutput(true);
                urlConnection.connect();
                OutputStreamWriter wr = new OutputStreamWriter(urlConnection.getOutputStream());
                JSONObject data = new JSONObject();
                data.put("name", name);
                data.put("current_organization", organisation);
                data.put("position", position);
                data.put("educational_background", education);
                data.put("email_id", emailId);
                data.put("phone_no", Integer.parseInt(phoneNo));
                data.put("past_experience", pastExp);
                wr.write(data.toString());
                wr.flush();
                wr.close();

                StringBuilder sb = new StringBuilder();
                int responseCode = urlConnection.getResponseCode();
                InputStream _is;
                if (urlConnection.getResponseCode() < HttpURLConnection.HTTP_BAD_REQUEST) {
                    reader = new BufferedReader(new InputStreamReader(urlConnection.getInputStream()));
                } else {
                    reader = new BufferedReader(new InputStreamReader(urlConnection.getErrorStream()));
                }
                if(responseCode == 200 || responseCode == 201 || responseCode == 500) {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        sb.append(line);
                    }
                    reader.close();
                    valueError = sb.toString();

                    return null;
                }
            } catch (Exception e) {
                Log.v("Error er", e.toString());
            }

            return null;
        }


        @Override
        protected void onPostExecute(Void result) {



        }

    }
}