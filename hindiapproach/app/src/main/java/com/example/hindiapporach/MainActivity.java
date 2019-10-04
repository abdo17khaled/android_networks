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
    boolean selectedItem;
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

    WifiP2pDevice currentDevice;
    List<WifiP2pDevice> peers = new ArrayList<WifiP2pDevice>();
    List<String> read_msg_box = new ArrayList<String>();

    String[] deviceNameArray = {"oppoabdo"};

    static final int MESSAGE_READ = 1;

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

    Handler handler = new Handler(new Handler.Callback() {
        @Override
        public boolean handleMessage(Message msg) {
            switch (msg.what) {
                case MESSAGE_READ:
                    byte[] readBuff = (byte[]) msg.obj;
                    String tempMsg = new String(readBuff, 0, msg.arg1);
                    if (read_msg_box.size() > 10) {
                        read_msg_box.clear();
                    }
                    read_msg_box.add(tempMsg);
                    fillMessages();
                    break;
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
                    selectedItem = true;
                    if (currentDevice != null) {
                        if (!currentDevice.deviceName.equals(peers.get(i).deviceName)) {
                            currentDevice = peers.get(0);
                            for (int j = 0; j < peers.size(); j++) {
                                if (deviceNameArray[i].equals(peers.get(j).deviceName)) {
                                    currentDevice = peers.get(j);
                                }
                            }

                        }
                        else {
                            currentDevice = null;
                            listView.setItemChecked(i,false);
                            selectedItem = false;
                        }
                    }
                    else {
                        currentDevice = peers.get(0);
                        for (int j = 0; j < peers.size(); j++) {
                            if (deviceNameArray[i].equals(peers.get(j).deviceName)) {
                                currentDevice = peers.get(j);
                            }
                        }
                    }
                    connectToCurrentDevice();
                }
                else {
                    currentDevice = null;
                }

            }
        });

        btnSend.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String msg = writeMsg.getText().toString();
                if (deviceDiscovered && selectedItem && msg != "" && msg != null && currentDevice!=null) {
                        sendReceive.write(msg.getBytes());
                } else if (!deviceDiscovered) {
                    Toast.makeText(getApplicationContext(), "no devices around you", Toast.LENGTH_SHORT).show();
                } else if (!selectedItem) {
                    Toast.makeText(getApplicationContext(), "no devices selected", Toast.LENGTH_SHORT).show();
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
        config.deviceAddress=currentDevice.deviceAddress;
        mManager.connect(mChannel, config, new WifiP2pManager.ActionListener() {
            @Override
            public void onSuccess() {
                Toast.makeText(getApplicationContext(),"Connected to "+currentDevice.deviceName,Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onFailure(int i) {
                Toast.makeText(getApplicationContext(),"Not Connected",Toast.LENGTH_SHORT).show();
            }
        });
    }
    private void initialWork() {
        deviceDiscovered = false;
        selectedItem = false;
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
                System.out.println("group formed server");
                serverClass = new ServerClass();
                serverClass.start();
            } else if (wifiP2pInfo.groupFormed) {
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
                System.out.println("accessed");
                serverSocket = new ServerSocket(); // <-- create an unbound socket first
                serverSocket.setReuseAddress(true);
                serverSocket.bind(new InetSocketAddress(8888)); // <-- now bind it
                System.out.println("accessed1");
                socket = serverSocket.accept();
                System.out.println("accessed2");
                sendReceive = new SendReceive(socket);
                System.out.println("sendReceiveserver");
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
                socket.connect(new InetSocketAddress(hostAdd, 8888), 500);
                sendReceive = new SendReceive(socket);
                sendReceive.start();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}
