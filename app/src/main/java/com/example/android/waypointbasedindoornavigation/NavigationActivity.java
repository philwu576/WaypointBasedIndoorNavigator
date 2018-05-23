package com.example.android.waypointbasedindoornavigation;

/*--

Module Name:

    NavigationActivity.java

Abstract:

    This module works as follow:

    1. Provides a UI for navigational guidance

    2. Calculate a navigation path

    3. Background listening to Lbeacon signals

Author:

    Phil Wu 01-Feb-2018

--*/

import android.Manifest;
import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothManager;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Paint.Style;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.RemoteException;
import android.speech.tts.TextToSpeech;
import android.support.annotation.RequiresApi;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.LinearLayout.LayoutParams;
import android.widget.PopupWindow;
import android.widget.TextView;

import org.altbeacon.beacon.Beacon;
import org.altbeacon.beacon.BeaconConsumer;
import org.altbeacon.beacon.BeaconManager;
import org.altbeacon.beacon.BeaconParser;
import org.altbeacon.beacon.RangeNotifier;
import org.altbeacon.beacon.Region;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Map.Entry;
import java.util.PriorityQueue;
import java.util.Queue;

import static com.example.android.waypointbasedindoornavigation.R.id.imageView;
import static com.example.android.waypointbasedindoornavigation.Setting.getMobilityValue;


public class NavigationActivity extends AppCompatActivity implements BeaconConsumer {

    private static final int NORMAL_WAYPOINT = 0;
    private static final int ELEVATOR_WAYPOINT = 1;
    private static final int STAIRWELL_WAYPOINT = 2;
    private static final int CONNECTPOINT = 3;
    private static final int ARRIVED_NOTIFIER = 0;
    private static final int WRONGWAY_NOTIFIER = 1;
    private static final int MAKETURN_NOTIFIER = 2;


    private static final int ONE_SECOND = 1000;
    private static final int RSSI_THRESHOLD = -65;


    private static final String FRONT = "front";
    private static final String LEFT = "left";
    private static final String FRONT_LEFT = "frontLeft";
    private static final String REAR_LEFT = "rearLeft";
    private static final String RIGHT = "right";
    private static final String FRONT_RIGHT = "frontRight";
    private static final String REAR_RIGHT = "rearRight";
    private static final String ELEVATOR = "elevator";
    private static final String STAIR = "stair";
    private static final String ARRIVED = "arrived";
    private static final String WRONG = "wrong";



    private static final String GO_STRAIGHT_ABOUT = "直走約";
    private static final String THEN_GO_STRAIGHT = "然後直走";
    private static final String THEN_TURN_LEFT = "然後向左轉";
    private static final String THEN_TURN_RIGHT = "然後向右轉";
    private static final String THEN_TURN_FRONT_LEFT = "然後向左前方轉";
    private static final String THEN_TURN__FRONT_RIGHT = "然後向右前方轉";
    private static final String THEN_TURN_REAR_LEFT = "然後向左後方轉";
    private static final String THEN_TURN__REAR_RIGHT = "然後向右後方轉";
    private static final String THEN_TAKE_ELEVATOR = "然後搭電梯";
    private static final String THEN_WALK_UP_STAIR = "然後走樓梯";
    private static final String WAIT_FOR_ELEVATOR = "電梯中請稍候";
    private static final String WALKING_UP_STAIR = "爬樓梯中";



    private static final String YOU_HAVE_ARRIVE = "抵達目的地";
    private static final String GET_LOST = "糟糕，你走錯路了";
    private static final String METERS = "公尺";
    private static final String PLEASE_GO_STRAIGHT = "請直走";
    private static final String PLEASE_TURN_LEFT = "請左轉";
    private static final String PLEASE_TURN_RIGHT = "請右轉";
    private static final String PLEASE_TURN_FRONT_LEFT = "請向左前方轉";
    private static final String PLEASE_TURN__FRONT_RIGHT = "請向右前方轉";
    private static final String PLEASE_TURN_REAR_LEFT = "請向左後方轉";
    private static final String PLEASE_TURN__REAR_RIGHT = "請向右後方轉";
    private static final String PLEASE_TAKE_ELEVATOR = "請搭電梯";
    private static final String PLEASE_WALK_UP_STAIR = "請走樓梯";


    private BluetoothManager bluetoothManager;
    private BluetoothAdapter mBluetoothAdapter;
    private static String[] PERMISSIONS_STORAGE = {
            Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.WRITE_EXTERNAL_STORAGE,
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION};

    // start ---------- Variables used to record important values ------------

    // IDs and Regions of source and destination input by user on home screen
    String sourceID, destinationID, sourceRegion, destinationRegion;

    // integer to record how many waypoints have been traveled
    int walkedWaypoint = 0;

    // ---------- variables used to record important values ------------ end




    // start ---------- variables used to store routing data ----------

    // a list of NavigationSubgraph object representing a Navigation Graph
    List<NavigationSubgraph> navigationGraph = new ArrayList<>();

    // a list of Region object storing the information of regions that will be traveled through
    List<com.example.android.waypointbasedindoornavigation.Region> regionPath = new ArrayList<>();

    // hashmap for storing region data
    RegionGraph regionGraph = new RegionGraph();
    //HashMap<String, com.example.android.waypointbasedindoornavigation.Region> regionData = new HashMap<>();

    // a list of Vertex object representing a navigation path
    List<Vertex> navigationPath = new ArrayList<Vertex>();
    Queue<String> tmp_path = new LinkedList<>();

    HashMap<String, String> mappingOfRegionNameAndID = new HashMap<>();

    // ---------- variables used to store routing data ---------- end



    // start ---------- objects used to provide voice and text navigational guidance ----------

    // reminder for destination and current location
    TextView destinationReminder, currentLocationReminder;

    // textual navigational instruction
    TextView firstMovement, howFarToMove, nextTurnMovement;

    // graphical navigational indicator
    ImageView imageTurnIndicator;

    // Indicator for popupwindow notifying user to make a turn at each waypoint
    String turnNotificationForPopup = null;

    // vocice engine for vocal navigational instrucation
    private TextToSpeech tts;

    // object for creating popupWindow
    private PopupWindow popupWindow;
    private LinearLayout positionOfPopup;

    // ---------- objects used to provide voice and text navigational guidance ---------- end



    // start ----------  manager and handlers to Lbeacon signals ----------

    //Beacon manager for ranging Lbeaon signal
    private BeaconManager beaconManager;
    private Region region;

    // thread for handling Lbeacon ID while in a navigation tour
    Thread threadForHandleLbeaconID;

    // handlers for "threadToHandleLbeaconID", receive message from the thread
    static Handler instructionHandler, currentPositiontHandler, walkedPointHandler;

    // synchronization between Lbeacon receiver and handler thread
    final Object sync = new Object();

    // string for storing currently received Lbeacon ID
    String currentLBeaconID = "EmptyString";

    // ----------  manager and handlers to Lbeacon signals ---------- end


    // start ---------- draw panel to display navigation progress bar ----------

    //Paint and drawpanel
    Paint paint = new Paint();
    Bitmap myBitmap;
    Bitmap workingBitmap;
    Bitmap mutableBitmap;
    Canvas canvas;
    ImageView drawPanel;
    int base = 0;
    int paddingLeft = 0;
    int paddingBottom = 0;

    // ---------- draw panel to display navigation progress bar ---------- end



    // variables created for demo purpose
    EditText waypointIDInput;
    Button waypointIDInputButton;
    int whichWaypointOnProgressBar = 0;
    private Find_Loc LBD = new Find_Loc();


    @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN_MR2)


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_navigation);

        // find UI objects by IDs
        firstMovement = (TextView) findViewById(R.id.instruction1);
        howFarToMove = (TextView) findViewById(R.id.instruction2);
        nextTurnMovement = (TextView) findViewById(R.id.insturction3);
        destinationReminder = (TextView) findViewById(R.id.to);
        currentLocationReminder = (TextView) findViewById(R.id.nowAt);
        imageTurnIndicator = (ImageView) findViewById(imageView);
        positionOfPopup = (LinearLayout) findViewById(R.id.navigationLayout);
        waypointIDInput = (EditText) findViewById(R.id.inputID);
        waypointIDInputButton = (Button) findViewById(R.id.inputButton);
        drawPanel = (ImageView) findViewById(R.id.drawpanel);

        // draw panel setup
        myBitmap = BitmapFactory.decodeResource(getResources(), R.drawable.img_drawpanel);
        workingBitmap = Bitmap.createBitmap(myBitmap);
        mutableBitmap = workingBitmap.copy(Bitmap.Config.ARGB_8888, true);
        canvas = new Canvas(mutableBitmap);


        // voice engine setup
        tts=new TextToSpeech(getApplicationContext(), new TextToSpeech.OnInitListener() {
            @Override
            public void onInit(int status) {

                if(status != TextToSpeech.ERROR) {
                    tts.setLanguage(Locale.CHINESE);
                }
            }
        });

        // Lbeacon Manager setup
        beaconManagerSetup();

        // receive value passed from MainActivity,
        //including IDs and Regions of source and destination
        Bundle bundle = getIntent().getExtras();
        if (bundle != null) {
            sourceID = bundle.getString("sourceID");
            destinationID = bundle.getString("destinationID");
            sourceRegion = bundle.getString("sourceRegion");
            destinationRegion = bundle.getString("destinationRegion");
        }

        // load all routing data
        loadWaypointsData();

        //start navigation
        startNavigation();

        //set text of destination
        destinationReminder.setText("目的地 : " + navigationPath.get(navigationPath.size()-1)._name);


        // create a thread to handle the Lbeacon signal
        threadForHandleLbeaconID = new Thread(new NavigationTread());
        threadForHandleLbeaconID.start();

        instructionHandler =  new Handler() {

            @Override
            public void handleMessage(Message msg) {

                // distance to the next waypoint
                int distance = 0;

                // if there are two or more waypoints to go
                if(navigationPath.size()>=2)
                    distance = (int) GeoCalulation.getDistance(navigationPath.get(0), navigationPath.get(1));

                // receive a turn direction message from threadForHandleLbeaconID
                String turnDirection = (String) msg.obj;

                switch(turnDirection){

                    case LEFT:
                        if(turnNotificationForPopup !=null)
                            showPopupWindow(MAKETURN_NOTIFIER);
                        turnNotificationForPopup = LEFT;
                        firstMovement.setText(GO_STRAIGHT_ABOUT);
                        howFarToMove.setText(""+distance +" "+METERS);
                        if(navigationPath.get(1)._nodeType == 1)
                            nextTurnMovement.setText(THEN_TAKE_ELEVATOR);
                        else if(navigationPath.get(1)._nodeType == 2)
                            nextTurnMovement.setText(THEN_WALK_UP_STAIR);
                        else
                            nextTurnMovement.setText(THEN_TURN_LEFT);
                        imageTurnIndicator.setImageResource(R.drawable.left_arrow);
                        break;

                    case FRONT_LEFT:
                        if(turnNotificationForPopup !=null)
                            showPopupWindow(MAKETURN_NOTIFIER);
                        turnNotificationForPopup = FRONT_LEFT;
                        firstMovement.setText(GO_STRAIGHT_ABOUT);
                        howFarToMove.setText(""+distance +" "+METERS);
                        if(navigationPath.get(1)._nodeType == 1)
                            nextTurnMovement.setText(THEN_TAKE_ELEVATOR);
                        else if(navigationPath.get(1)._nodeType == 2)
                            nextTurnMovement.setText(THEN_WALK_UP_STAIR);
                        else
                            nextTurnMovement.setText(THEN_TURN_FRONT_LEFT);
                        imageTurnIndicator.setImageResource(R.drawable.frontleft_arrow);
                        break;
                    case REAR_LEFT:
                        if(turnNotificationForPopup !=null)
                            showPopupWindow(MAKETURN_NOTIFIER);
                        turnNotificationForPopup = REAR_LEFT;
                        firstMovement.setText(GO_STRAIGHT_ABOUT);
                        howFarToMove.setText(""+distance +" "+METERS);
                        if(navigationPath.get(1)._nodeType == 1)
                            nextTurnMovement.setText(THEN_TAKE_ELEVATOR);
                        else if(navigationPath.get(1)._nodeType == 2)
                            nextTurnMovement.setText(THEN_WALK_UP_STAIR);
                        else
                            nextTurnMovement.setText(THEN_TURN_REAR_LEFT);
                        imageTurnIndicator.setImageResource(R.drawable.rearleft_arrow);
                        break;

                    case RIGHT:
                        if(turnNotificationForPopup !=null)
                            showPopupWindow(MAKETURN_NOTIFIER);
                        turnNotificationForPopup = RIGHT;
                        firstMovement.setText(GO_STRAIGHT_ABOUT);
                        howFarToMove.setText(""+distance +" "+METERS);
                        if(navigationPath.get(1)._nodeType == 1)
                            nextTurnMovement.setText(THEN_TAKE_ELEVATOR);
                        else if(navigationPath.get(1)._nodeType == 2)
                            nextTurnMovement.setText(THEN_WALK_UP_STAIR);
                        else
                            nextTurnMovement.setText(THEN_TURN_RIGHT);
                        imageTurnIndicator.setImageResource(R.drawable.right_arrow);
                        break;

                    case FRONT_RIGHT:
                        if(turnNotificationForPopup !=null)
                            showPopupWindow(MAKETURN_NOTIFIER);
                        turnNotificationForPopup = FRONT_RIGHT;
                        firstMovement.setText(GO_STRAIGHT_ABOUT);
                        howFarToMove.setText(""+distance +" "+METERS);
                        if(navigationPath.get(1)._nodeType == 1)
                            nextTurnMovement.setText(THEN_TAKE_ELEVATOR);
                        else if(navigationPath.get(1)._nodeType == 2)
                            nextTurnMovement.setText(THEN_WALK_UP_STAIR);
                        else
                            nextTurnMovement.setText(THEN_TURN__FRONT_RIGHT);
                        imageTurnIndicator.setImageResource(R.drawable.frontright_arrow);
                        break;

                    case REAR_RIGHT:
                        if(turnNotificationForPopup !=null)
                            showPopupWindow(MAKETURN_NOTIFIER);
                        turnNotificationForPopup = REAR_RIGHT;
                        firstMovement.setText(GO_STRAIGHT_ABOUT);
                        howFarToMove.setText(""+distance +" "+METERS);
                        if(navigationPath.get(1)._nodeType == 1)
                            nextTurnMovement.setText(THEN_TAKE_ELEVATOR);
                        else if(navigationPath.get(1)._nodeType == 2)
                            nextTurnMovement.setText(THEN_WALK_UP_STAIR);
                        else
                            nextTurnMovement.setText(THEN_TURN__REAR_RIGHT);
                        imageTurnIndicator.setImageResource(R.drawable.rearright_arrow);
                        break;

                    case FRONT:
                        if(turnNotificationForPopup !=null)
                            showPopupWindow(MAKETURN_NOTIFIER);
                        turnNotificationForPopup = FRONT;
                        firstMovement.setText(GO_STRAIGHT_ABOUT);
                        howFarToMove.setText(""+distance +" "+METERS);
                        if(navigationPath.get(1)._nodeType == 1)
                            nextTurnMovement.setText(THEN_TAKE_ELEVATOR);
                        else if(navigationPath.get(1)._nodeType == 2)
                            nextTurnMovement.setText(THEN_WALK_UP_STAIR);
                        else
                            nextTurnMovement.setText(THEN_GO_STRAIGHT);
                        imageTurnIndicator.setImageResource(R.drawable.up_arrow);
                        break;

                    case STAIR:
                        turnNotificationForPopup = STAIR;
                        if(turnNotificationForPopup !=null)
                            showPopupWindow(MAKETURN_NOTIFIER);
                        firstMovement.setText(WALKING_UP_STAIR);
                        howFarToMove.setText("");
                        nextTurnMovement.setText("");
                        imageTurnIndicator.setImageResource(R.drawable.stair);
                        walkedWaypoint = 0;
                        sourceID = navigationPath.get(1)._id;
                        break;

                    case ELEVATOR:
                        turnNotificationForPopup = ELEVATOR;
                        if(turnNotificationForPopup !=null)
                            showPopupWindow(MAKETURN_NOTIFIER);
                        firstMovement.setText(WAIT_FOR_ELEVATOR);
                        howFarToMove.setText("");
                        nextTurnMovement.setText("");
                        imageTurnIndicator.setImageResource(R.drawable.elevator);
                        walkedWaypoint = 0;
                        sourceID = navigationPath.get(1)._id;
                        break;

                    case ARRIVED:
                        showPopupWindow(ARRIVED_NOTIFIER);
                        walkedWaypoint = 0;
                        break;

                    case WRONG:
                        turnNotificationForPopup = null;
                        canvas.drawColor(Color.WHITE);
                        showPopupWindow(WRONGWAY_NOTIFIER);
                        walkedWaypoint = 0;
                        sourceRegion = navigationPath.get(0)._region;
                        break;
                }

                //After the navigational instruction for current waypoint is properly given,
                //the waypoint is removed from the top of the navigationPath
                navigationPath.remove(0);
            }
        };

        //Handler for setting current location on UI
        currentPositiontHandler = new Handler() {

            @Override
            public void handleMessage(Message msg) {
                String currentLocation = (String) msg.obj;
                currentLocationReminder.setText("目前位置 : " + currentLocation);
            }
        };

        //Handler for number of waypoint traveled in this navigation route
        walkedPointHandler = new Handler(){

            @Override
            public void handleMessage(Message msg) {
                int numberOfWaypointTraveled = (int) msg.obj;

                //If it is the first waypoint of travel of a region, meaning that
                //heading correction is needed
                if(numberOfWaypointTraveled==1 && (navigationPath.size()>=2)){
                    turnNotificationForPopup = null;

                    //Start CompassActivity for heading correction
                    Intent intent = new Intent(NavigationActivity.this,
                            CompassActivity.class);
                    intent.putExtra("degree",
                            GeoCalulation.getBearingOfTwoPoints(navigationPath.get(0),
                            navigationPath.get(1)));
                    startActivity(intent);
                }

            }
        };

    }


    // set up Lbeacon manager
    private void beaconManagerSetup(){

        //Beacon manager setup
        beaconManager =  BeaconManager.getInstanceForApplication(this);
        beaconManager.bind(this);
        beaconManager.getBeaconParsers().add(new BeaconParser().
                setBeaconLayout("m:2-3=0215,i:4-15,i:16-19,i:20-23,p:24-24"));

        //setBeaconLayout("m:2-3=0215,i:4-19,i:20-23,i:24-27,p:28-28"));
        // Detect the Eddystone main identifier (UID) frame:
        beaconManager.getBeaconParsers().add(new BeaconParser().
                setBeaconLayout("s:0-1=feaa,m:2-2=00,p:3-3:-41,i:4-13,i:14-19"));

        // Detect the Eddystone telemetry (TLM) frame:
        beaconManager.getBeaconParsers().add(new BeaconParser().
                setBeaconLayout("x,s:0-1=feaa,m:2-2=20,d:3-3,d:4-5,d:6-7,d:8-11,d:12-15"));

        // Detect the Eddystone URL frame:
        beaconManager.getBeaconParsers().add(new BeaconParser().
                setBeaconLayout("s:0-1=feaa,m:2-2=10,p:3-3:-41,i:4-20"));

        //beaconManager.setForegroundScanPeriod(ONE_SECOND);
        //beaconManager.setForegroundBetweenScanPeriod(2*ONE_SECOND);


        beaconManager.setForegroundScanPeriod(200);
        beaconManager.setForegroundBetweenScanPeriod(0);
        beaconManager.removeAllMonitorNotifiers();
        beaconManager.removeAllRangeNotifiers();

        // Get the details for all the beacons we encounter.
        region = new Region("justGiveMeEverything", null, null, null);
        bluetoothManager = (BluetoothManager)
                getSystemService(Context.BLUETOOTH_SERVICE);
        ActivityCompat.requestPermissions(this, PERMISSIONS_STORAGE, 1001);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        beaconManager.unbind(this);
    }

    @Override
    public void onBeaconServiceConnect() {

        //Start scanning for Lbeacon signal
        beaconManager.addRangeNotifier(new RangeNotifier() {
            @Override
            public void didRangeBeaconsInRegion(Collection<Beacon> beacons,Region region) {
                Log.i("beacon","Beacon Size:"+beacons.size());
                if (beacons.size() > 0) {
                    Iterator<Beacon> beaconIterator = beacons.iterator();
                    while (beaconIterator.hasNext()) {
                        Beacon beacon = beaconIterator.next();
                        logBeaconData(LBD.Find_Loc(beacon,true));
                    }
                }
            }

        });

        try {
            beaconManager.startRangingBeaconsInRegion(new Region("myRangingUniqueId",
                    null, null, null));
        } catch (RemoteException e) {
            e.printStackTrace();
        }

    }


    // load beacon ID
    private void logBeaconData(List<String> beacon) {

        if (beacon.size()>2) {
            LBD.wrtieFileOnInternalStorage("Log.txt","NAP1:"+beacon.toString());

            String receivebeacon = beacon.get(1);

            // block the Lbeacon ID the navigator just received
            if (!currentLBeaconID.equals(receivebeacon)) {

                // draw a navigation progress bar
                paint.setAntiAlias(true);
                paint.setColor(Color.RED);
                paint.setStyle(Style.FILL_AND_STROKE);
                paint.setStrokeWidth(10);

                canvas.drawCircle(200 + base * whichWaypointOnProgressBar, 250, 15, paint);
                canvas.drawLine(200 + base * whichWaypointOnProgressBar, 250,
                        200 + base * (whichWaypointOnProgressBar + 1), 250, paint);

                whichWaypointOnProgressBar += 1;

                // update the currentLbeaconID and go to handlerForLbeacon
                currentLBeaconID = receivebeacon;
//                currentLBeaconID = CConvX.concat(CConvY);
                synchronized (sync) {
                    sync.notify();
                }
            }
        }
    }



    // load waypoint data
    public void loadWaypointsData(){

        // load region data from region graph
        regionGraph = DataParser.getRegionDataFromRegionGraph(this);

        mappingOfRegionNameAndID = DataParser.waypointNameAndIDMappings(this,
                regionGraph.getAllRegionNames());

        // regionPath for storing Region objects represent the regions
        //that the user passes by from source to destination
        regionPath = regionGraph.getRegionPath(sourceRegion, destinationRegion);
        //regionPath = getRegionPath(sourceRegion, destinationRegion);

        // a list of String of region name in regionPath
        List<String> regionPathID = new ArrayList<>();

        for(int i=0; i<regionPath.size(); i++)
            regionPathID.add(regionPath.get(i)._name);


        //Load waypoint data from the navigation subgraphs according to the regionPathID
        navigationGraph = DataParser.getWaypointDataFromNavigationGraph(this, regionPathID);

    }


    public void startNavigation() {

        // get the two Vertex objects that represent starting point and destination
        Vertex startVertex = navigationGraph.get(0).verticesInSubgraph.get(sourceID);
        Vertex endVertex = navigationGraph.get(navigationGraph.size()-1).verticesInSubgraph.get(destinationID);

        // temporary variable to record connectPointID
        int connectPointID;

        // navigation in the same region
        if(navigationGraph.size()==1)

            // preform typical dijkstra's algorithm with two given Vertex objects
            navigationPath = computeDijkstraShortestPath(startVertex, endVertex);

        // navigation between several regions
        else{

            // compute N-1 navigation paths for each region,
            // where N is the number of region to travel

            for(int i = 0; i< navigationGraph.size()-1; i++){

                // a destination vertex for each region
                Vertex destinationOfARegion = null;

                // the source vertex becomes a normal waypoint
                navigationGraph.get(i).verticesInSubgraph.get(sourceID)._nodeType = NORMAL_WAYPOINT;

                //If the elevation of the next region to travel is same as the current region
                if(regionPath.get(i)._elevation == regionPath.get(i+1)._elevation){

                    // compute a path to a transfer point of current region
                    // return the transfer point
                    destinationOfARegion = computePathToTraversePoint(
                            navigationGraph.get(i).verticesInSubgraph.get(sourceID),true, i+1);

                    // sourceID is updated with the ID of transfer node for the next computation
                    // since the transfer node has the same ID in the same elevation
                    sourceID = destinationOfARegion.getID();

                }
                //If the elevation of the next region to travel is different from the current region
                else if(regionPath.get(i)._elevation != regionPath.get(i+1)._elevation){

                    // compute a path to a transfer point(elevator or stairwell) of current region
                    // return the transfer point
                    destinationOfARegion = computePathToTraversePoint(
                            navigationGraph.get(i).verticesInSubgraph.get(sourceID),false, i+1);

                    // get the connectPointID of the transfer node
                    connectPointID = destinationOfARegion._connectPointID;

                    // find the transfer node with the same connectPointID in the next region
                    // where elevation is different from the current region
                    for(Entry<String, Vertex> entry : navigationGraph.get(i+1).verticesInSubgraph.entrySet()){

                        Vertex v = entry.getValue();

                        if(v._connectPointID == connectPointID){

                            sourceID = v.getID();
                            break;
                        }
                    }
                }

                // add up all the navigation paths into one
                navigationPath.addAll(getShortestPathToDestination(destinationOfARegion));

            }

            Log.i("bb", "sourceID: " + sourceID);
            //Compute navigation path in the last region
            List<Vertex> pathInLastRegion = computeDijkstraShortestPath(
                    navigationGraph.get(navigationGraph.size()-1).verticesInSubgraph.get(sourceID),
                    endVertex);

            // complete the navigation path
            navigationPath.addAll(pathInLastRegion);

            // remove duplicated waypoints which are used as connecting points in the same elevation
            for(int i = 1; i<navigationPath.size(); i++){
                if(navigationPath.get(i)._id.equals(navigationPath.get(i-1)._id))
                     navigationPath.remove(i);
            }
        }


        for (int i = 0; i<navigationPath.size(); i++)
            tmp_path.offer(navigationPath.get(i).getID());
        LBD.setpath(tmp_path);
//        Log.i("NAP",tmp_path.toString());

        //Draw a navigation progress bar based on navigation path
        drawProgressBar(navigationPath);
    }


    // compute a shortest path with given starting point and destination
    public List<Vertex> computeDijkstraShortestPath(Vertex source, Vertex destination) {
        source.minDistance = 0.;
        PriorityQueue<Vertex> vertexQueue = new PriorityQueue<Vertex>();
        vertexQueue.add(source);
        while (!vertexQueue.isEmpty()) {
            Vertex v = vertexQueue.poll();
            //Stop searching when reach the destination node
            if (v._id.equals(destination._id))
                break;
            // Visit each edge that is adjacent to v
            for (Edge e : v.adjacencies) {
                Vertex a = e.target;
                double weight = e.weight;
                double distanceThroughU = v.minDistance + weight;
                if (distanceThroughU < a.minDistance) {
                    vertexQueue.remove(a);
                    a.minDistance = distanceThroughU;
                    a.previous = v;
                    vertexQueue.add(a);
                }
            }
        }
        return getShortestPathToDestination(destination);
    }

    // compute a shortest path from a given starting point to a transfer node (e.g., elevator, stairwell)
    public Vertex computePathToTraversePoint(Vertex source, Boolean sameElevation, int indexOfNextRegion) {

        source.minDistance = 0.;
        PriorityQueue<Vertex> vertexQueue = new PriorityQueue<Vertex>();
        vertexQueue.add(source);

        while (!vertexQueue.isEmpty()) {

            Vertex u = vertexQueue.poll();

            // Visit each edge exiting u
            for (Edge e : u.adjacencies) {
                Vertex v = e.target;
                double weight = e.weight;
                double distanceThroughU = u.minDistance + weight;
                if (distanceThroughU < v.minDistance) {
                    vertexQueue.remove(v);

                    v.minDistance = distanceThroughU;
                    v.previous = u;
                    vertexQueue.add(v);
                }

                // if the elevation of the next region to travel is same as current region
                // find the nearest connect point and check if it is legal
                if(sameElevation == true && v._nodeType == CONNECTPOINT){

                    // return v, only if the connect point is in the next region
                    if(navigationGraph.get(indexOfNextRegion).verticesInSubgraph.get(v._id) != null)
                            return v;

                }

                // if the elevation of the next region to travel is different from current region
                // find the nearest elevator or stairwell based on user's preference
                else if(sameElevation == false && v._nodeType == getMobilityValue())
                            return v;

            }
        }
        return source;
    }


    // get shortest path by traversing previous waypoint back to the source
    public List<Vertex> getShortestPathToDestination(Vertex destination) {
        List<Vertex> path = new ArrayList<Vertex>();
        for (Vertex vertex = destination; vertex != null; vertex = vertex.previous)
            path.add(vertex);

        // reverse path to get correct order
        Collections.reverse(path);
        return path;
    }


    // popup window for turn direction notification
    public void showPopupWindow(final int instruction){

        LayoutInflater inflater = (LayoutInflater) getBaseContext().getSystemService(LAYOUT_INFLATER_SERVICE);
        View customView = inflater.inflate(R.layout.popup, null);

        popupWindow = new PopupWindow(customView, LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);

        final Button popupButton = (Button) customView.findViewById(R.id.popupButton);
        TextView popupText = (TextView) customView.findViewById(R.id.popupText);

        if(instruction== ARRIVED_NOTIFIER){
            popupText.setText(YOU_HAVE_ARRIVE);
            tts.speak(popupText.getText().toString(), TextToSpeech.QUEUE_FLUSH, null);
            popupButton.setText("OK");
        }
        else if(instruction== WRONGWAY_NOTIFIER){
            popupText.setText(GET_LOST);
            popupButton.setText("重新導航");
            tts.speak(popupText.getText().toString(), TextToSpeech.QUEUE_FLUSH, null);
        }
        else if(instruction== MAKETURN_NOTIFIER){

            switch(turnNotificationForPopup){

                case RIGHT:
                    popupText.setText(PLEASE_TURN_RIGHT);
                    break;
                case LEFT:
                    popupText.setText(PLEASE_TURN_LEFT);
                    break;
                case FRONT_RIGHT:
                    popupText.setText(PLEASE_TURN__FRONT_RIGHT);
                    break;
                case FRONT_LEFT:
                    popupText.setText(PLEASE_TURN_FRONT_LEFT);
                    break;
                case REAR_RIGHT:
                    popupText.setText(PLEASE_TURN__REAR_RIGHT);
                    break;
                case REAR_LEFT:
                    popupText.setText(PLEASE_TURN_REAR_LEFT);
                    break;
                case FRONT:
                    popupText.setText(PLEASE_GO_STRAIGHT);
                    break;
                case ELEVATOR:
                    popupText.setText(PLEASE_TAKE_ELEVATOR);
                    break;
                case STAIR:
                    popupText.setText(PLEASE_WALK_UP_STAIR);
                    break;
                case ARRIVED:
                    popupText.setText(YOU_HAVE_ARRIVE);
                    break;

            }
            popupButton.setText("OK");

            tts.speak(popupText.getText().toString(), TextToSpeech.QUEUE_FLUSH, null);
        }

        popupButton.setOnClickListener(new View.OnClickListener(){

            @Override

            public void onClick(View v){

                //Navigation tour finished, back to home screen
                if(instruction== ARRIVED_NOTIFIER){
                    Intent i = new Intent(v.getContext(), MainActivity.class);;
                    v.getContext().startActivity(i);
                    ((Activity)v.getContext()).finish();
                    popupWindow.dismiss();
                }//User get wrong way, reload waypoint data and restart navigation
                else if(instruction== WRONGWAY_NOTIFIER){
                    navigationPath.clear();
                    sourceID = currentLBeaconID;
                    loadWaypointsData();
                    startNavigation();
                    whichWaypointOnProgressBar = 0;
                    popupWindow.dismiss();

                    /*
                    // back to main screen
                    Intent i = new Intent(v.getContext(), MainActivity.class);;
                    v.getContext().startActivity(i);
                    ((Activity)v.getContext()).finish();
                    popupWindow.dismiss();*/
                }//Check button for every turn instruction
                else if(instruction== MAKETURN_NOTIFIER){
                    tts.speak(firstMovement.getText().toString() + howFarToMove.getText().toString() + nextTurnMovement.getText().toString(), TextToSpeech.QUEUE_FLUSH, null);
                    popupWindow.dismiss();
                }
            }
        });

        popupWindow.showAtLocation(positionOfPopup, Gravity.CENTER, 0, 0);
    }



    //Create a thread to handle the currently received Lbeacon ID
    class NavigationTread implements Runnable {

        @Override
        public void run() {

            // while the navigation path is not finished yet
            while (!navigationPath.isEmpty()) {

                // the thread waits for beacon manager to notify it when a new Lbeacon ID is received
                synchronized (sync) {
                    try {
                        sync.wait();
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }

                // if the received ID matches the ID of the next waypoint in the navigation path
                if (navigationPath.get(0)._id.equals(currentLBeaconID)) {

                    // three message objects send messages to corresponding handlers
                    Message messageFromInstructionHandler = instructionHandler.obtainMessage();
                    Message messageFromCurrentPositionHandler = currentPositiontHandler.obtainMessage();
                    Message messageFromWalkedPointHandler = walkedPointHandler.obtainMessage();

                    // CurrentPositionHandler get the message of currently matched waypoint name
                    messageFromCurrentPositionHandler.obj = navigationPath.get(0)._name;

                    // if the navigation path has more than three waypoints to travel
                    if (navigationPath.size() >= 3) {

                        // if the next two waypoints are in the same region as the current waypoint
                        // get the turn direction at the next waypoint
                        if (navigationPath.get(0)._region.equals(navigationPath.get(1)._region) &&
                                navigationPath.get(1)._region.equals(navigationPath.get(2)._region)){
                            messageFromInstructionHandler.obj =
                                    GeoCalulation.getDirectionFromBearing(navigationPath.get(0),
                                            navigationPath.get(1), navigationPath.get(2));
                        }

                        // if the next two waypoints are not in the same region
                        // means that the next waypoint is the last waypoint of the region to travel
                        else if (!(navigationPath.get(1)._region.equals(navigationPath.get(2)._region))) {
                            messageFromInstructionHandler.obj = FRONT;
                        }

                        // if the current waypoint and the next waypoint are not in the same region
                        // transfer through elevator or stairwell
                        else if (!(navigationPath.get(0)._region.equals(navigationPath.get(1)._region))) {
                            if (navigationPath.get(0)._nodeType == ELEVATOR_WAYPOINT)
                                messageFromInstructionHandler.obj = ELEVATOR;
                            else if(navigationPath.get(0)._nodeType == STAIRWELL_WAYPOINT)
                                messageFromInstructionHandler.obj = STAIR;
                            else if((navigationPath.get(0)._nodeType == CONNECTPOINT))
                                messageFromInstructionHandler.obj =
                                        GeoCalulation.getDirectionFromBearing(navigationPath.get(0),
                                                navigationPath.get(1), navigationPath.get(2));

                        }
                    }
                    // if there are two waypoints left in the navigation path
                    else if (navigationPath.size() == 2) {

                        // if the current waypoint and the next waypoint are not in the same region
                        if (!(navigationPath.get(0)._region.equals(navigationPath.get(1)._region))) {

                            if (navigationPath.get(0)._nodeType == ELEVATOR_WAYPOINT)
                                messageFromInstructionHandler.obj = ELEVATOR;
                            else if (navigationPath.get(0)._nodeType == STAIRWELL_WAYPOINT)
                                messageFromInstructionHandler.obj = STAIR;
                        }
                        // else go straight to the final waypoint
                        else
                            messageFromInstructionHandler.obj = FRONT;

                    }
                    // if there is only one waypoint left, the user has arrived
                    else if (navigationPath.size() == 1)
                        messageFromInstructionHandler.obj = ARRIVED;


                    // every time the received ID is matched,
                    // the user is considered to travel one more waypoint
                    walkedWaypoint++;

                    // WalkedPointHandler get the message of number
                    //of waypoint has been traveled in a region
                    messageFromWalkedPointHandler.obj = walkedWaypoint;

                    // send the newly updated message to three handlers
                    walkedPointHandler.sendMessage(messageFromWalkedPointHandler);
                    instructionHandler.sendMessage(messageFromInstructionHandler);
                    currentPositiontHandler.sendMessage(messageFromCurrentPositionHandler);
                }
                // if the received ID does not match the ID of waypoint in the navigation path
                else if (!(navigationPath.get(0)._id.equals(currentLBeaconID))) {

                    // send a "wrong" message to the handler
                    Message messageFromInstructionHandler = instructionHandler.obtainMessage();
                    messageFromInstructionHandler.obj = WRONG;
                    instructionHandler.sendMessage(messageFromInstructionHandler);
                }
            }
        }
    }


    // draw navigation progress bar
    public void drawProgressBar(List<Vertex> navigationPath){



        DisplayMetrics displayMetrics = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(displayMetrics);

        int width = displayMetrics.widthPixels;
        int height = displayMetrics.heightPixels;

        Log.i("bb", "Width : " + width);
        Log.i("bb", "Height : " + height);

        base = (int) ((width*0.8)/navigationPath.size());
        paddingLeft = (int) (width*0.1);
        paddingBottom = (int) (height*0.1);



        //Set drawing style
        paint.setAntiAlias(true);
        paint.setColor(Color.parseColor("#1A237E"));
        paint.setStrokeWidth(10);
        paint.setTextSize(40);
        paint.setStyle(Style.FILL_AND_STROKE);


        // draw waypoints
        for(int i = 0; i < navigationPath.size(); i++)
            canvas.drawCircle(paddingLeft+base*i,paddingBottom, 15,paint);



        // draw progress bar
        canvas.drawLine(paddingLeft, paddingBottom,
                paddingLeft+base*(navigationPath.size()-1), 250, paint);

        //Set drawing style
        paint.setAntiAlias(true);
        paint.setColor(Color.parseColor("#1A237E"));
        paint.setStrokeWidth(4);
        paint.setTextSize(50);
        paint.setStyle(Style.FILL_AND_STROKE);


        // show all the waypoint name above the progress bar (for demo)
        for(int i = 0; i < navigationPath.size(); i++)
            canvas.drawText(navigationPath.get(i)._name, (paddingBottom-70)+base*i , paddingBottom-100, paint);

        /*// draw the name of starting point and destination above progress bar
        canvas.drawText(navigationPath.get(0)._name, 50+base , 150, paint);
        canvas.drawText(navigationPath.get(navigationPath.size()-1)._name,
         50+base*(navigationPath.size()) , 150, paint);*/

        // set up drawPanel
        drawPanel.setAdjustViewBounds(true);
        drawPanel.setImageBitmap(mutableBitmap);

    }


    // enter a waypoint ID to emulate the navigator receiving the corresponding Lbeacon ID (for demo)
    public void enterWaypointID(View view){


        paint.setAntiAlias(true);
        paint.setColor(Color.RED);
        paint.setStyle(Style.FILL_AND_STROKE);
        paint.setStrokeWidth(10);

        canvas.drawCircle(paddingLeft+base*whichWaypointOnProgressBar,paddingBottom, 15,paint);

        if(navigationPath.size()>1)
            canvas.drawLine(paddingLeft+base*whichWaypointOnProgressBar, paddingBottom,
                    paddingLeft+base*(whichWaypointOnProgressBar+1),paddingBottom, paint);


        whichWaypointOnProgressBar += 1;

        // Input waypoint name for debug mode
        String nameOFWaypoint = waypointIDInput.getText().toString();

        currentLBeaconID = mappingOfRegionNameAndID.get(nameOFWaypoint);


        synchronized (sync) {
            sync.notify();
        }
    }

    public void clearEditText(View v){

        waypointIDInput.getText().clear();
    }

}