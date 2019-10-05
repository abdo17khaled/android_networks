package com.example.hindiapporach;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.IntentFilter;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.net.wifi.p2p.WifiP2pConfig;
import android.net.wifi.p2p.WifiP2pDevice;
import android.net.wifi.p2p.WifiP2pDeviceList;
import android.net.wifi.p2p.WifiP2pInfo;
import android.net.wifi.p2p.WifiP2pManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;
import android.os.Build;
import java.util.concurrent.TimeUnit;

import androidx.appcompat.app.AppCompatActivity;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {
    boolean deviceDiscovered;
    int selectedInt;
    Button btnSend;
    ListView listView;
    ListView readMsg;
    EditText writeMsg;

    WifiManager wifiManager;
    WifiP2pManager mManager;
    WifiP2pManager.Channel mChannel;

    BroadcastReceiver mReceiver;
    IntentFilter mIntentFilter;

    WifiP2pDevice currentSelectedDevice;
    List<WifiP2pDevice> peers = new ArrayList<WifiP2pDevice>();
    List<String> read_msg_box = new ArrayList<String>();
    String myDeviceName="[Phone] galaxya70";
//    String myDeviceName="oppoabdo";
    String[] deviceNameArray = {"oppoabdo","[Phone] galaxya70"};
    String selectedDeviceName = "";
    static final int MESSAGE_READ = 1;
    boolean connectionStatus = false;
    boolean relayPoint = false;
    ServerClass serverClass;
    ClientClass clientClass;
    SendReceive sendReceive;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        initialWork();
        exqListener();
    }
    // receiving messages: gets called from SendReceive
    //
    Handler handler = new Handler(new Handler.Callback() {
        @Override
        public boolean handleMessage(Message msg) {
            switch (msg.what) {
                case MESSAGE_READ:
                    byte[] readBuff = (byte[]) msg.obj;
                    String tempMsg = new String(readBuff, 0, msg.arg1);
                    System.out.println(tempMsg+":::messageRecievedbatee5");
                    //clears arraylist of messages after 10 messages
                    if (read_msg_box.size() > 10) {
                        read_msg_box.clear();
                    }
                    System.out.println(tempMsg);
                    // TO-DO
                    /*
                    1. decode message to see if we need to send it to another device or not
                    note: symbol to split on will be namedevice$&%*Message
                    2. if namedevice not the same as current device, search for device  in device list
                    3. if device found, send to device
                    4. if device not found, do nothing
                     */
                    if(tempMsg != null || tempMsg != "") {
                        String[] tmp = tempMsg.trim().split("\\$&%");
                        String tmpSelectedDeviceName = tmp[1];

                        if (tmpSelectedDeviceName.equals(myDeviceName)) {
                            // arraylist of messages
                            System.out.println(tempMsg+":::batee5a 2");
                            read_msg_box.add(tmp[0]+": "+tmp[2]);
                            // fills new messages into listview readMsg
                            fillMessages();
                            return true;
                        } else {
                            Disconnect();
                            currentSelectedDevice = peers.get(0);
                            for (int i = 0; i < peers.size(); i++) {
                                if (peers.get(i).deviceName.equals(tmpSelectedDeviceName)) {
                                    currentSelectedDevice = peers.get(i);
                                }
                            }

                            connectToCurrentDevice();
                            sendReceive.write(readBuff);
                            Disconnect();
                            return true;
                        }
                    }

                    return true;
            }
            return true;
        }
    });

    private void exqListener() {
        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                System.out.println("peersSize:::"+peers.size());
                if(!peers.isEmpty() && peers.size()>=1) {
                    deviceDiscovered = true;
                    selectedDeviceName = deviceNameArray[i];
                    if (currentSelectedDevice != null) {
                        if (!currentSelectedDevice.deviceName.equals(deviceNameArray[i])) {
                            currentSelectedDevice = peers.get(0);
                            for (int j = 0; j < peers.size(); j++) {
                                if (deviceNameArray[i].equals(peers.get(j).deviceName)) {
                                    currentSelectedDevice = peers.get(j);
                                    relayPoint = false;
                                    break;
                                }
                                relayPoint = true;
                            }
                            connectToCurrentDevice();
                        }
//                        else {
////                            currentSelectedDevice = null;
////                            selectedDeviceName = "";
////                            listView.setItemChecked(i,false);
//                        }
                    }
                    else {
                        currentSelectedDevice = peers.get(0);
                        for (int j = 0; j < peers.size(); j++) {
                            if (deviceNameArray[i].equals(peers.get(j).deviceName)) {
                                currentSelectedDevice = peers.get(j);
                                relayPoint = false;
                                break;
                            }
                            relayPoint = true;
                        }
                        connectToCurrentDevice();
                    }
                }
                else {
                    deviceDiscovered = false;
                    currentSelectedDevice = null;
                }
            }
        });

        btnSend.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String msg = writeMsg.getText().toString();
                msg =  myDeviceName + "$&%" + selectedDeviceName + "$&%" + msg;
                if (deviceDiscovered && msg != "" && msg != null) {
                    // TO-DO
                    /*
                    1. connect to device with connectToCurrentDevice() and wait 1 second     Done
                    2. add current device name to the beginning of the message and put a splitter token namedevice$&%*Message   Done
                    3. disconnect from device using code found in TO-DO
                     */

                    sendReceive.write(msg.getBytes());
//                    try {
//                        TimeUnit.SECONDS.sleep(1);
//                    } catch (InterruptedException e) {
//                        System.err.format("IOException: %s%n", e);
//                    }
                    if(relayPoint){
                        Disconnect();
                    }
                } else if (!deviceDiscovered) {
                    Toast.makeText(getApplicationContext(), "no devices around you", Toast.LENGTH_SHORT).show();
                }
            }
        });
    }
    private void discoverDevices(){
        mManager.discoverPeers(mChannel, new WifiP2pManager.ActionListener() {
            @Override
            public void onSuccess() {
                Toast.makeText(getApplicationContext(),"Discovery Started",Toast.LENGTH_SHORT).show();
            }
            @Override
            public void onFailure(int i) {
                Toast.makeText(getApplicationContext(),"Discovery Starting Failed",Toast.LENGTH_SHORT).show();
            }
        });
    }
    private void connectToCurrentDevice(){

        WifiP2pConfig config=new WifiP2pConfig();
        config.deviceAddress=currentSelectedDevice.deviceAddress;
        mManager.connect(mChannel, config, new WifiP2pManager.ActionListener() {
            @Override
            public void onSuccess() {
                connectionStatus = true;
                System.out.println("Connected batee5 device yes yes");
                Toast.makeText(getApplicationContext(),"Connected to "+currentSelectedDevice.deviceName,Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onFailure(int i) {
                Toast.makeText(getApplicationContext(),"Not Connected",Toast.LENGTH_SHORT).show();
            }
        });
        System.out.println("after series, Connected batee5 device yes yes");

    }
    //TO-DO
    /*
    1. create fuction to disconnect device
            mManager.removeGroup(mChannel, new WifiP2pManager.ActionListener() {
            @Override
            public void onSuccess() {
            }
            @Override
            public void onFailure(int reason) {
            }
        });
    2. stop socket in send receive then set sendReceive to null
     */

    private void Disconnect()
    {
        sendReceive.socket = null;
        initialWork();
    }

    private void initialWork() {
        deviceDiscovered = false;
        selectedInt = -1;
        btnSend = (Button) findViewById(R.id.sendButton);
        listView = (ListView) findViewById(R.id.peerListView);
        readMsg = (ListView) findViewById(R.id.readMsg);
        writeMsg = (EditText) findViewById(R.id.writeMsg);
        fillList();
        wifiManager = (WifiManager) getApplicationContext().getSystemService(Context.WIFI_SERVICE);


        mManager = (WifiP2pManager) getSystemService(Context.WIFI_P2P_SERVICE);

        mChannel = mManager.initialize(this, getMainLooper(), null);

        mReceiver = new WiFiDirectBroadcastReceiver(mManager, mChannel, this);

        mIntentFilter = new IntentFilter();
        mIntentFilter.addAction(WifiP2pManager.WIFI_P2P_STATE_CHANGED_ACTION);
        mIntentFilter.addAction(WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION);
        mIntentFilter.addAction(WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION);
        mIntentFilter.addAction(WifiP2pManager.WIFI_P2P_THIS_DEVICE_CHANGED_ACTION);
        discoverDevices();
    }
    private void fillList(){

        ArrayAdapter<String> adapter = new ArrayAdapter<String>(getApplicationContext(), android.R.layout.simple_list_item_1, deviceNameArray);
        listView.setAdapter(adapter);
    }
    private void fillMessages(){
        ArrayAdapter<String> adapter = new ArrayAdapter<String>(getApplicationContext(), android.R.layout.simple_list_item_1, read_msg_box);
        readMsg.setAdapter(adapter);
    }
    WifiP2pManager.PeerListListener listpeer = new WifiP2pManager.PeerListListener() {
        @Override
        public void onPeersAvailable(WifiP2pDeviceList peerList) {
            System.out.println("WIFIDIRECT:::");
            if (!peerList.getDeviceList().equals(peers)) {
                peers.clear();
                peers.addAll(peerList.getDeviceList());
                for(WifiP2pDevice peer:peers)
                    System.out.println("wifiDevices:::"+peer.deviceName);
                if (peers.size() == 0) {
                    deviceDiscovered = false;
                    return;
                } else {
                    deviceDiscovered = true;
                }
            }

        }
    };

    WifiP2pManager.ConnectionInfoListener connectionInfoListener = new WifiP2pManager.ConnectionInfoListener() {
        @Override
        public void onConnectionInfoAvailable(WifiP2pInfo wifiP2pInfo) {
            final InetAddress groupOwnerAddress = wifiP2pInfo.groupOwnerAddress;

            if (wifiP2pInfo.groupFormed && wifiP2pInfo.isGroupOwner) {
                connectionStatus = true;
                System.out.println("group formed server");
                serverClass = new ServerClass();
                serverClass.start();
            } else if (wifiP2pInfo.groupFormed) {
                connectionStatus = true;
                System.out.println("group formed client");
                clientClass = new ClientClass(groupOwnerAddress);
                clientClass.start();
            }
            System.out.println("group not formed");
        }
    };

    @Override
    protected void onResume() {
        super.onResume();
        registerReceiver(mReceiver, mIntentFilter);
    }

    @Override
    protected void onPause() {
        super.onPause();
        unregisterReceiver(mReceiver);
    }

    public class ServerClass extends Thread {
        Socket socket;
        ServerSocket serverSocket;

        @Override
        public void run() {
            try {
                serverSocket = new ServerSocket(); // <-- create an unbound socket first
                serverSocket.setReuseAddress(true);
                serverSocket.bind(new InetSocketAddress(8888)); // <-- now bind it
                socket = serverSocket.accept();
                sendReceive = new SendReceive(socket);
                System.out.println(sendReceive);
                sendReceive.start();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private class SendReceive extends Thread {
        private Socket socket;
        private InputStream inputStream;
        private OutputStream outputStream;

        public SendReceive(Socket skt) {
            socket = skt;
            try {
                inputStream = socket.getInputStream();
                outputStream = socket.getOutputStream();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        @Override
        public void run() {
            byte[] buffer = new byte[1024];
            int bytes;

            while (socket != null) {
                try {
                    bytes = inputStream.read(buffer);
                    if (bytes > 0) {
                        handler.obtainMessage(MESSAGE_READ, bytes, -1, buffer).sendToTarget();
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            System.out.println("thread terminated");
        }

        public void write(final byte[] bytes) {
            new Thread(new Runnable() {

                @Override
                public void run() {
                    System.out.println("batee5 bytes");
                    System.out.println(bytes);
                    try {
                        outputStream.write(bytes);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }).start();
        }
    }

    public class ClientClass extends Thread {
        Socket socket;
        String hostAdd;

        public ClientClass(InetAddress hostAddress) {
            hostAdd = hostAddress.getHostAddress();
            socket = new Socket();
        }

        @Override
        public void run() {
            try {
                socket.connect(new InetSocketAddress(hostAdd, 8888));
                sendReceive = new SendReceive(socket);
                sendReceive.start();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}
