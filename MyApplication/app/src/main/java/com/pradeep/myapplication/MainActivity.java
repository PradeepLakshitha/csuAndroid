package com.pradeep.myapplication;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.provider.MediaStore;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.util.SparseArray;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.vision.Frame;
import com.google.android.gms.vision.text.TextBlock;
import com.google.android.gms.vision.text.TextRecognizer;
import com.google.gson.Gson;
import com.kosalgeek.android.photoutil.GalleryPhoto;
import com.kosalgeek.android.photoutil.ImageLoader;
import com.microsoft.projectoxford.vision.VisionServiceClient;
import com.microsoft.projectoxford.vision.VisionServiceRestClient;
import com.microsoft.projectoxford.vision.contract.AnalysisResult;
import com.microsoft.projectoxford.vision.contract.Caption;
import com.theartofdev.edmodo.cropper.CropImage;
import com.theartofdev.edmodo.cropper.CropImageView;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;

public class MainActivity extends AppCompatActivity {

    TextView textView;
    ImageView imageView;
    Button button, videoButton, analysisButton;
    ImageButton camera, gallery, cropButton;
    TextRecognizer textRecognizer;
    GalleryPhoto gallerPhoto;
    final int CAMERA_REQUEST = 12121;
    final int GALLERY_REQUEST = 11111;
    Bitmap bitmap;
    Uri imageUri;
    public VisionServiceClient vision = new VisionServiceRestClient("18740c67fab0456f9e8f55f0b0b2bba3");
    ProgressDialog dialog;

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putParcelable("bitmap", bitmap);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        dialog = new ProgressDialog(MainActivity.this);
        dialog.setMessage("Processing...");
        gallerPhoto = new GalleryPhoto(getApplicationContext());
        textView = (TextView) findViewById(R.id.textView1);
        imageView = (ImageView) findViewById(R.id.imageView1);
        button = (Button) findViewById(R.id.button1);
        videoButton = (Button) findViewById(R.id.button4);
        analysisButton = (Button) findViewById(R.id.button5);
        camera = (ImageButton) findViewById(R.id.button2);
        gallery = (ImageButton) findViewById(R.id.button3);
        cropButton = (ImageButton) findViewById(R.id.cropButton);
        cropButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (bitmap != null) {
                    imageUri = getImageUri(MainActivity.this, bitmap);
                    startCropImageActivity(imageUri);
                } else {
                    Toast.makeText(MainActivity.this, "Select Image", Toast.LENGTH_SHORT).show();
                }
            }
        });
        if (savedInstanceState != null) {
            bitmap = savedInstanceState.getParcelable("bitmap");
            imageView.setImageBitmap(bitmap);
        } else {
            Log.d("SavedInstanceState", "null");
        }

        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (bitmap != null) {
                    textRecognizer = new TextRecognizer.Builder(getApplicationContext()).build();
                    if (!textRecognizer.isOperational()) {
                        Log.w("MainActivity", "-----------------");
                    } else {
                        Frame frame = new Frame.Builder().setBitmap(bitmap).build();
                        SparseArray<TextBlock> items = textRecognizer.detect(frame);
                        StringBuilder stringBuilder = new StringBuilder();
                        for (int i = 0; i < items.size(); i++) {
                            TextBlock item = items.valueAt(i);
                            stringBuilder.append(item.getValue());
                            stringBuilder.append("\n");
                        }
                        textView.setText(stringBuilder.toString());

                    }
                } else {
                    Toast.makeText(MainActivity.this, "Select Image", Toast.LENGTH_SHORT).show();
                }
            }
        });
        camera.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                try {
                    Intent cameraIntent = new Intent(android.provider.MediaStore.ACTION_IMAGE_CAPTURE);
                    startActivityForResult(cameraIntent, CAMERA_REQUEST);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });
        gallery.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                try {
                    Intent in = gallerPhoto.openGalleryIntent();
                    startActivityForResult(in, GALLERY_REQUEST);

                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });

        videoButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(MainActivity.this, VideoProcesingActivity.class));
            }
        });

        analysisButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (bitmap != null) {
                    if (NetworkUtil.getConnectivity(MainActivity.this)) {
                        ByteArrayOutputStream bao = new ByteArrayOutputStream();
                        bitmap.compress(Bitmap.CompressFormat.JPEG, 100, bao);
                        final ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(bao.toByteArray());

                        AsyncTask<InputStream, String, String> visionTask = new AsyncTask<InputStream, String, String>() {
                            @Override
                            protected void onPreExecute() {
                                dialog.show();
                            }

                            @Override
                            protected String doInBackground(InputStream... params) {

                                try {
                                    String[] features = {"Description"};
                                    String[] details = {};
                                    AnalysisResult analysisResult = vision.analyzeImage(params[0], features, details);
                                    String s = new Gson().toJson(analysisResult);
                                    return s;
                                } catch (Exception e) {
                                    return null;
                                }

                            }

                            @Override
                            protected void onPostExecute(String s) {
                                AnalysisResult result = new Gson().fromJson(s, AnalysisResult.class);
                                StringBuilder stringBuilder = new StringBuilder();
                                for (Caption caption : result.description.captions) {
                                    stringBuilder.append(caption.text);
                                }
                                textView.setText(stringBuilder);
                                dialog.dismiss();
                            }
                        };
                        visionTask.execute(byteArrayInputStream);
                    } else {
                        Toast.makeText(MainActivity.this, "Internet Connection Error...", Toast.LENGTH_SHORT).show();
                    }
                } else {
                    Toast.makeText(MainActivity.this, "Select Image", Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    public Uri getImageUri(Context inContext, Bitmap inImage) {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        inImage.compress(Bitmap.CompressFormat.PNG, 100, bytes);
        String seletedCropImagePath = MediaStore.Images.Media.insertImage(inContext.getContentResolver(), inImage, "Title", null);
        return Uri.parse(seletedCropImagePath);
    }

    private void startCropImageActivity(Uri imageUri) {
        CropImage.activity(imageUri)
                .setGuidelines(CropImageView.Guidelines.ON)
                .setMultiTouchEnabled(true)
                .start(this);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (resultCode == RESULT_OK) {
            if (requestCode == CAMERA_REQUEST) {

                try {
                    bitmap = (Bitmap) data.getExtras().get("data");
                    imageView.setImageBitmap(bitmap);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            } else if (requestCode == GALLERY_REQUEST) {
                gallerPhoto.setPhotoUri(data.getData());
                String photoPath = gallerPhoto.getPath();
                try {
                    bitmap = ImageLoader.init().from(photoPath).requestSize(512, 512).getBitmap();
                    imageView.setImageBitmap(bitmap);
                } catch (FileNotFoundException e) {
                    e.printStackTrace();
                }
            }
        }
        if (requestCode == CropImage.CROP_IMAGE_ACTIVITY_REQUEST_CODE) {
            CropImage.ActivityResult result = CropImage.getActivityResult(data);
            if (resultCode == RESULT_OK) {
                try {
                    imageUri = result.getUri();
                    bitmap = MediaStore.Images.Media.getBitmap(this.getContentResolver(), imageUri);
                    imageView.setImageBitmap(bitmap);
//                    try {
//                        if (seletedCropImage != null) {
//                            File file = new File(seletedCropImage.getPath());
//                            file.delete();
//                            if(file.exists()){
//                                file.getCanonicalFile().delete();
//                                if(file.exists()){
//                                    getApplicationContext().deleteFile(file.getName());
//                                }
//                            }
//                        }
//                    } catch (Exception ee) {
//                        ee.printStackTrace();
//                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (bitmap != null) {
            imageView.setImageBitmap(bitmap);
        }

    }

}
