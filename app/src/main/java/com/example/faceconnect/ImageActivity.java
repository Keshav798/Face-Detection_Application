package com.example.faceconnect;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ImageDecoder;
import android.graphics.Paint;
import android.graphics.Rect;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.View;
import android.widget.ImageView;
import android.widget.Toast;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.mlkit.vision.common.InputImage;
import com.google.mlkit.vision.face.Face;
import com.google.mlkit.vision.face.FaceDetection;
import com.google.mlkit.vision.face.FaceDetector;
import com.google.mlkit.vision.face.FaceDetectorOptions;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class ImageActivity extends AppCompatActivity {

    private final int REQUEST_CODE=1001; //request code for getting image from intent

    public Uri uri; //public variable for uri got from selected image
    public Bitmap image_bitmap; //variable for uri converted to bitmap

    ImageView imageView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_image);

        imageView=findViewById(R.id.imageView);
    }

    public void openImage(View v){
        getPermission(); // user defined function for getting permission for gallery(see function )
        Intent intent=new Intent(Intent.ACTION_GET_CONTENT);  //setting intent for getting content
        intent.setType("image/*"); //type =image
        startActivityForResult(intent,REQUEST_CODE); // starting activity for result for getting image uri

    }



    @Override  //overriding function to tell java what to do when startActivity intent(for getting image from gallery) got a result
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if(resultCode==RESULT_OK && requestCode==REQUEST_CODE){ //result is ok and if request code is our code given at time of intent
            uri=data.getData(); //getting uri
            image_bitmap=loadImageFromUri(uri); //user defined function for getting bitmap from uri
            imageView.setImageBitmap(image_bitmap); //setiing image

        }
    }

    public void detectFacesFunction(View view){
        if(image_bitmap==null){ //if no image selected
            Toast.makeText(this, "Select an Image first", Toast.LENGTH_SHORT).show();
        }
        else {
            detectFaces(image_bitmap);  //calling function
        }
    } //function made only for the functionality of "detect faces" button

    public void getPermission(){
        if(Build.VERSION.SDK_INT>=Build.VERSION_CODES.M){ //if version is above marshmallow(since below marshmallow permissions were given like that)
            if(checkSelfPermission(android.Manifest.permission.READ_MEDIA_IMAGES)!= PackageManager.PERMISSION_GRANTED){ //if permission not already given
                requestPermissions(new String[]{Manifest.permission.READ_MEDIA_IMAGES} ,1); //request permission
            }
        }
    }
    public Bitmap loadImageFromUri(Uri uri){ //takes uri and return corresponding image bitmap
        Bitmap bitmap=null;

        try {
            if(Build.VERSION.SDK_INT>Build.VERSION_CODES.O_MR1){ //this is because for version before 27 there is a different
                //way and after 27 theres a diff way to convert uri to bitmap
                ImageDecoder.Source source=ImageDecoder.createSource(getContentResolver(),uri); //contentResolver() gets actual source of image which uri(unique resource locator) is pointing to
                bitmap=ImageDecoder.decodeBitmap(source);
            }
            else{
                bitmap= MediaStore.Images.Media.getBitmap(getContentResolver(),uri); //contentResolver() gets actual source of image which uri(unique resource locator) is pointing to
            }

        }
        catch (IOException e){
            e.printStackTrace();
        }


        return bitmap;
    }

    public void detectFaces(Bitmap bitmap){
        //SO SOMEWHAT THIS IS MAIN PART OF CODE
        FaceDetector faceDetector;  //creating face detector object provided by google ml kit
        FaceDetectorOptions highAccOpts=new FaceDetectorOptions.Builder()  //setiing detector options(setting to performance mode accurate)
                .setPerformanceMode(FaceDetectorOptions.PERFORMANCE_MODE_ACCURATE)
                .enableTracking()
                .build();

        faceDetector= FaceDetection.getClient(highAccOpts);

        InputImage inputImage=InputImage.fromBitmap(bitmap,0); //getting input image
        faceDetector.process(inputImage) //passing input image for processing faces
                .addOnSuccessListener(new OnSuccessListener<List<Face>>() {
                    @Override  //overriding on success that is if everything went fine then what to do
                    public void onSuccess(List<Face> faces) { //we'll get a list of faces
                        if(faces.isEmpty()){ //if no face
                            Toast.makeText(ImageActivity.this, "No Faces Found", Toast.LENGTH_SHORT).show();
                        }
                        else{
                            List<BoxWithText> boxes=new ArrayList<>(); //creating BoxWithText array
                            for(Face face : faces){
                                //for each face we make an object of BoxWithText and set text parameter as tracking id and Rect parameter as bounding box
                                //bounding box is area covered by the face
                                BoxWithText boxWithText=new BoxWithText(face.getTrackingId()+"",face.getBoundingBox());
                                boxes.add(boxWithText); //add each object to arr
                            }
                            Bitmap output=drawDetectionResult(bitmap,boxes); //calling drawing function
                            imageView.setImageBitmap(output); //setting image
                        }
                    }
                }).addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Toast.makeText(ImageActivity.this, "Some error", Toast.LENGTH_SHORT); //on any error give this message
                        e.printStackTrace();
                    }
                });
    }

    /**
     * Draw bounding boxes around objects together with the object's name.
     */
    protected Bitmap drawDetectionResult(Bitmap bitmap, List<BoxWithText> detectionResults) { //passing image bitmap and list of BoxWithText class
        Bitmap outputBitmap = bitmap.copy(Bitmap.Config.ARGB_8888, true); //copying image and making it editable(since normal bitmap is not editable)
        Canvas canvas = new Canvas(outputBitmap); //class for editing images
        Paint pen = new Paint();                  //class for writing on images
        pen.setTextAlign(Paint.Align.LEFT);

        for (BoxWithText box : detectionResults) {
            // draw bounding box
            pen.setColor(Color.GREEN);   //setting pen settings for rectangular box
            pen.setStrokeWidth(10F);
            pen.setStyle(Paint.Style.STROKE);
            canvas.drawRect(box.rect, pen);   //drawing box

            Rect tagSize = new Rect(0, 0, 0, 0);

            // calculate the right font size
            pen.setStyle(Paint.Style.FILL_AND_STROKE);  //setting pen for text
            pen.setColor(Color.YELLOW);
            pen.setStrokeWidth(2F);

            pen.setTextSize(102F);
            pen.getTextBounds(box.text, 0, box.text.length(), tagSize); //getting the bounds of text
            float fontSize = pen.getTextSize() * box.rect.width() / tagSize.width();

            // adjust the font size so texts are inside the bounding box
            if (fontSize < pen.getTextSize()) {
                pen.setTextSize(fontSize);
            }

            float margin = (box.rect.width() - tagSize.width()) / 2.0F;
            if (margin < 0F) margin = 0F;
            canvas.drawText(  //writing text
                    box.text, box.rect.left + margin,
                    box.rect.top + tagSize.height(), pen
            );
        }
        return outputBitmap;
    }
}