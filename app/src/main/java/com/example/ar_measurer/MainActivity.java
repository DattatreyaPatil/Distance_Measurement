package com.example.ar_measurer;

import androidx.appcompat.app.AppCompatActivity;

import android.app.ActionBar;
import android.app.Activity;
import android.app.ActivityManager;
import android.content.Context;
import android.os.Bundle;
import android.os.Debug;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.google.ar.core.Anchor;
import com.google.ar.core.Config;
import com.google.ar.core.Frame;
import com.google.ar.core.HitResult;
import com.google.ar.core.Plane;
import com.google.ar.core.Pose;
import com.google.ar.sceneform.AnchorNode;
import com.google.ar.sceneform.FrameTime;
import com.google.ar.sceneform.Node;
import com.google.ar.sceneform.Scene;
import com.google.ar.sceneform.math.Quaternion;
import com.google.ar.sceneform.math.Vector3;
import com.google.ar.sceneform.rendering.Color;
import com.google.ar.sceneform.rendering.MaterialFactory;
import com.google.ar.sceneform.rendering.ModelRenderable;
import com.google.ar.sceneform.rendering.ShapeFactory;
import com.google.ar.sceneform.ux.ArFragment;
import com.google.ar.sceneform.ux.TransformableNode;

import java.text.DecimalFormat;
import java.util.Objects;

public class MainActivity extends AppCompatActivity implements com.google.ar.sceneform.Scene.OnUpdateListener {

    private static final double MIN_OPENGL_VERSION = 3.0;
    private static final String TAG = MainActivity.class.getSimpleName();

    private ArFragment arFragment;
    private AnchorNode currentAnchorNode,currentAnchorNode_1;
    private TextView tvDistance;
    ModelRenderable cubeRenderable;
    private Anchor currentAnchor = null,currentAnchor_1=null;
    private static int countForAnchorsProduced=0;
    private static DecimalFormat df = new DecimalFormat("0.00");

    ModelRenderable lineRenderable;
    Anchor anchor ;
    AnchorNode anchorNode ;
    private String selectedMode="meters";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (!checkIsSupportedDeviceOrFinish(this)) {
            Toast.makeText(getApplicationContext(), "Device not supported", Toast.LENGTH_LONG).show();
        }

        setContentView(R.layout.activity_main);
        //initialise the member variables
        arFragment = (ArFragment) getSupportFragmentManager().findFragmentById(R.id.ux_fragment);
        tvDistance = findViewById(R.id.tvDistance);


        initModel();
/*        arFragment.getPlaneDiscoveryController().hide();
        arFragment.getPlaneDiscoveryController().setInstructionView(null);*/
        arFragment.setOnTapArPlaneListener((hitResult, plane, motionEvent) -> {
            if (cubeRenderable == null)
                return;

            countForAnchorsProduced++;
            //create the nodes  and anchors for visual representation
            if(countForAnchorsProduced==1)
            {
                Anchor anchor = hitResult.createAnchor();
                AnchorNode anchorNode = new AnchorNode(anchor);
                anchorNode.setParent(arFragment.getArSceneView().getScene());

                currentAnchor = anchor;
                currentAnchorNode = anchorNode;

                TransformableNode node = new TransformableNode(arFragment.getTransformationSystem());
                node.setRenderable(cubeRenderable);
                node.setParent(anchorNode);
                arFragment.getArSceneView().getScene().addOnUpdateListener(this);
                arFragment.getArSceneView().getScene().addChild(anchorNode);
                node.select();

            }
            if(countForAnchorsProduced==2)
            {
                // Creating Anchor.
                Anchor anchor_1 = hitResult.createAnchor();
                AnchorNode anchorNode_1 = new AnchorNode(anchor_1);
                anchorNode_1.setParent(arFragment.getArSceneView().getScene());

                currentAnchor_1=anchor_1;
                currentAnchorNode_1=anchorNode_1;


                TransformableNode node_1 = new TransformableNode(arFragment.getTransformationSystem());
                node_1.setRenderable(cubeRenderable);
                node_1.setParent(anchorNode_1);
                arFragment.getArSceneView().getScene().addOnUpdateListener(this);
                arFragment.getArSceneView().getScene().addChild(anchorNode_1);
                node_1.select();
                //lineBetweenPoints(currentAnchorNode.getWorldPosition(),currentAnchorNode_1.getWorldPosition());
                addLineBetweenHits(hitResult,plane,motionEvent);
            }
            //Clear the screen as soon as there are more than 2 nodes
            if(countForAnchorsProduced>2)
            {
                //set to default text
                tvDistance.setText("Click to start measuring!!");
                countForAnchorsProduced=0;
                clearAnchor();
            }
        });
        //Add a dropdown to let user select units in meters, cms or inches
        Spinner dropdown = (Spinner) findViewById(R.id.dropdown_list);
        //create a list of items for the spinner.
        String[] items = new String[]{"meters","cms","inches"};
        //fill the dropdown with arrayAdapter.
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, items);
        //set the spinners adapter to the previously created one.
        dropdown.setAdapter(adapter);
        dropdown.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view,
                                       int position, long id)
            {
                Log.v("item", (String) parent.getItemAtPosition(position));
                switch (position)
                {
                    case 0:
                        selectedMode="meters";
                        break;
                    case 1:
                        selectedMode="cms";
                        break;
                    case 2:
                        selectedMode="inches";
                        break;
                    default:
                        selectedMode="meters";
                        break;

                }
            }
            //Initiate the default selected mode
            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                // TODO Auto-generated method stub
                selectedMode="meters";
            }
        });
        dropdown.setSelection(0);
    }

    //Check if the device is supported or no
    public boolean checkIsSupportedDeviceOrFinish(final Activity activity) {

        String openGlVersionString =
                null;
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.KITKAT)
        {
            openGlVersionString = ((ActivityManager) Objects.requireNonNull(activity.getSystemService(Context.ACTIVITY_SERVICE)))
                    .getDeviceConfigurationInfo()
                    .getGlEsVersion();
        }
        if (Double.parseDouble(openGlVersionString) < MIN_OPENGL_VERSION)
        {
            Log.e(TAG, "Sceneform requires OpenGL ES 3.0 later");
            Toast.makeText(activity, "Sceneform requires OpenGL ES 3.0 or later", Toast.LENGTH_LONG)
                    .show();
            activity.finish();
            return false;
        }
        return true;
    }

    //create a sphere for visual representation of hwere the user clicked or selected the point
    private void initModel() {
        MaterialFactory.makeTransparentWithColor(this, new Color(android.graphics.Color.RED))
                .thenAccept(
                        material -> {
                            Vector3 vector3 = new Vector3(0.05f, 0.01f, 0.01f);
                            cubeRenderable = ShapeFactory.makeSphere(0.02f, Vector3.zero(), material);
                            cubeRenderable.setShadowCaster(false);
                            cubeRenderable.setShadowReceiver(false);
                        });
    }

    //Clear the nodes once distance is calculated
    private void clearAnchor() {
        currentAnchor = null;
        currentAnchor_1=null;

        //If they are still assignd and not cleared then remove nodes from scene manually
        if (currentAnchorNode != null && currentAnchorNode_1!=null)
        {
            arFragment.getArSceneView().getScene().removeChild(currentAnchorNode);
            currentAnchorNode.getAnchor().detach();
            currentAnchorNode.setParent(null);
            currentAnchorNode = null;

            arFragment.getArSceneView().getScene().removeChild(currentAnchorNode_1);
            currentAnchorNode_1.getAnchor().detach();
            currentAnchorNode_1.setParent(null);
            currentAnchorNode_1 = null;

            arFragment.getArSceneView().getScene().removeChild(anchorNode);
            anchorNode.getAnchor().detach();
            anchorNode.setParent(null);
            anchorNode = null;
        }

    }

    @Override
    public void onUpdate(FrameTime frameTime) {
        Frame frame = arFragment.getArSceneView().getArFrame();

        Log.d("API123", "onUpdateframe... current anchor node " + (currentAnchorNode == null));

        //Calculate the distance between the nodes generated in meters as it's the default
        if (currentAnchorNode != null && currentAnchorNode_1!=null) {
            Pose objectPose = currentAnchor.getPose();
            Pose objectPose_1= currentAnchor_1.getPose();

            float dx_1 = objectPose.tx() - objectPose_1.tx();
            float dy_1 = objectPose.ty() - objectPose_1.ty();
            float dz_1 = objectPose.tz() - objectPose_1.tz();
            ///Compute the straight-line distance.
            float distanceMeasured=(float) Math.sqrt(dx_1 * dx_1 + dy_1 * dy_1 + dz_1 * dz_1);
            if(selectedMode=="meters")
            {
                distanceMeasured=distanceMeasured;
            }
            else if(selectedMode=="cms")
            {
                distanceMeasured=distanceMeasured*100;
            }
            else if(selectedMode=="inches")
            {
                //standard conversion from meters to inches
                distanceMeasured=distanceMeasured*39.3701f;
            }
            String distanceMeters = df.format(distanceMeasured);
            tvDistance.setText("Distance measured: " + distanceMeters + selectedMode);

        }
    }

    //create a visual representation of a line between two points selected
    //Taje the hit result as a parameter to know exactly the target point was placed by user
    private void addLineBetweenHits(HitResult hitResult, Plane plane, MotionEvent motionEvent) {

        int val = motionEvent.getActionMasked();
        float axisVal = motionEvent.getAxisValue(MotionEvent.AXIS_X, motionEvent.getPointerId(motionEvent.getPointerCount() - 1));
        Log.e("Values:", String.valueOf(val) + String.valueOf(axisVal));
        anchor = hitResult.createAnchor();
        anchorNode = new AnchorNode(anchor);


        if (currentAnchorNode != null)
        {
            anchorNode.setParent(arFragment.getArSceneView().getScene());
            Vector3 point1, point2;
            point1 = currentAnchorNode.getWorldPosition();
            point2 = anchorNode.getWorldPosition();

            //calculate the difference vector i.e. from point A to B
            final Vector3 difference = Vector3.subtract(point1, point2);
            final Vector3 directionFromTopToBottom = difference.normalized();
            final Quaternion rotationFromAToB =
                    Quaternion.lookRotation(directionFromTopToBottom, Vector3.up());
            MaterialFactory.makeOpaqueWithColor(getApplicationContext(), new Color(0, 255, 244))
                    .thenAccept(
                            material -> {
                            /* Create a cube extending from one point to another point detected but limit it's length to the lenght of
                            difference vector because we don't want to extend more than required*/
                                ModelRenderable model = ShapeFactory.makeCube(
                                        new Vector3(.01f, .01f, difference.length()),
                                        Vector3.zero(), material);

                                /* Set the rotation to the cube */
                                Node node = new Node();
                                node.setParent(anchorNode);
                                node.setRenderable(model);
                                node.setWorldPosition(Vector3.add(point1, point2).scaled(.5f));
                                node.setWorldRotation(rotationFromAToB);
                            }
                    );

        }
    }


}
