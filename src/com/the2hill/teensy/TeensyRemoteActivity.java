package com.the2hill.teensy;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.net.Socket;
import java.util.Set;
import java.util.UUID;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ToggleButton;

import com.MobileAnarchy.Android.Widgets.Joystick.JoystickMovedListener;
import com.MobileAnarchy.Android.Widgets.Joystick.JoystickView;
import com.MobileAnarchy.Android.Widgets.Joystick.DualJoystickView;

public class TeensyRemoteActivity extends Activity {
	private static final boolean D = true;
	private static final String TAG = "TeensyRemote";

	private BlueSmirfSPP mSPP;
	private Thread cThread;
	private BluetoothDevice mmDevice;
	private BluetoothSocket mmSocket;

	// Intent request codes
	private static final int REQUEST_CONNECT_DEVICE = 1;
	private static final int REQUEST_ENABLE_BT = 2;

	private BluetoothAdapter mBluetoothAdapter = null;

	private static final UUID MY_UUID = UUID
			.fromString("00001101-0000-1000-8000-00805F9B34FB");

	public static int SERVERPORT = 5000;
	private static int FORWARD = 3;
	private static int BACKWARD = 2;
	private static int RIGHT = 5;
	private static int LEFT = 4;

	private EditText serverIp;
	private ToggleButton isConnected;
	private String serverIpAddress = "";
	private boolean connected = false;
	Bundle what;
	int current = 0;

	// handle messages from server responses
	Handler handler = new Handler();

	public Handler myThreadHandler;

	TextView txtX, txtY;
	TextView txtX2, txtY2;
	DualJoystickView joystick;
	private Bundle mArrayListBluetoothAddress;
	private String mBluetoothAddress;
	private OutputStream mOutputStream;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.dualjoystick);
		// isConnected = (ToggleButton) findViewById(R.id.toggleButton1);
		// isConnected.setOnClickListener(connectListener);
		// serverIp = (EditText) findViewById(R.id.server_ip);

		joystick = (DualJoystickView) findViewById(R.id.dualjoystickView);
		joystick.setOnJostickMovedListener(_listenerLeft, _listenerRight);

		mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
		if (mBluetoothAdapter == null) {
			Toast.makeText(this, "Bluetooth is not available",
					Toast.LENGTH_LONG).show();
			finish();
			return;
		}
	}

	@Override
	protected void onDestroy() {
		super.onDestroy();
	}

	public class ClientThread implements Runnable {
		protected Socket socket;
		private String mAddy;

		public void run() {
			try {
				Log.d("ClientActivity", "C: Connecting...");
				try {
					mmSocket = mmDevice
							.createRfcommSocketToServiceRecord(MY_UUID);
					mBluetoothAdapter.cancelDiscovery();
					Log.d(TAG, "Address: " + mBluetoothAddress);

					mmSocket.connect();
					mOutputStream = mmSocket.getOutputStream();

				} catch (IOException e) {
					Log.e(TAG, "create() connection failed", e);
				}
				if (mOutputStream != null) {
					Log.d("ClientActivity",
							"Socket outSTream not null,  connection to device");
					connected = true;
				} else {
					Log.d("ClientActivity",
							"Socket outSTream null, no connection to device");
					connected = false;
				}
				Looper.prepare();
				myThreadHandler = new Handler() {
					public void handleMessage(Message msg) {
						what = msg.getData();
						while (connected) {
							try {
								Log.d("ClientActivity", "C: Sending command.");
								PrintWriter out = new PrintWriter(
										new BufferedWriter(
												new OutputStreamWriter(
														mOutputStream)), true);
								out.println(what.get("what").toString());
								Log.d("ClientActivity",
										"C: Sent."
												+ what.get("what").toString());
								break;
							} catch (Exception e) {
								Log.e("ClientActivity", "S: Error", e);
							}
						}
					}
				};
				Looper.loop();

				Log.e("Closing connection/ending transaction...", mAddy);
				mmSocket.close();
				connected = false;
				Log.d("ClientActivity", "C: Closed.");
			} catch (Exception e) {
				if (e instanceof InterruptedException) {
					Log.w("Thread was interupted, ignoring...", e);
					return;
				}
				Log.e("ClientActivity", "C: Error", e);
				connected = false;
			}
		}
	}

	private void processMovement(int x, int y, int pad) {
		if (connected) {
			if (pad == 1 && y == 0 && x == 0) {
				current = 0;
				sendData("A0&");
//				sendData(BACKWARD, 0);
			} else if (pad == 0 && x == 0 && y == 0) {
				sendData("B90&");
//				sendData(RIGHT, 0);
			} else if (pad == 1) {
				if (y == -2) {
					sendData("A30&");
//					sendData(BACKWARD, 0);
				}
				if (y == 2) {
					sendData("A134&");
//					sendData(FORWARD, 0);
				}
			} else if (pad == 0) {
				if (x <= -2) {
					sendData("B0&");
//					sendData(LEFT, 0);
				}
				if (x >= 2) {
					sendData("B180&");
//					sendData(RIGHT, 0);
				}
			}
		}
	}

	private void sendData(String connect) {
		Message m = Message.obtain();
		Bundle b = new Bundle();
		b.putString("what", connect);
		m.setData(b);
		myThreadHandler.sendMessage(m);
	}

	private void sendData(int pin, int dir) {
		Message m = Message.obtain();
		Bundle b = new Bundle();
		b.putString("what", String.valueOf(pin) + String.valueOf(dir));
		m.setData(b);
		myThreadHandler.sendMessage(m);
	}

	public void onActivityResult(int requestCode, int resultCode, Intent data) {
		if (D)
			Log.d(TAG, "onActivityResult " + resultCode);
		switch (requestCode) {
		case REQUEST_CONNECT_DEVICE:
			// When DeviceListActivity returns with a device to connect
			if (resultCode == Activity.RESULT_OK) {
				// Get the device MAC address
				mBluetoothAddress = data.getExtras().getString(
						DeviceListActivity.EXTRA_DEVICE_ADDRESS);
				// Get the BLuetoothDevice object
				BluetoothDevice device = mBluetoothAdapter
						.getRemoteDevice(mBluetoothAddress.toUpperCase());
				// Attempt to connect to the device
				mmDevice = device;
				if (connected) {
					connected = false;
					cThread.interrupt();
				}
				if (mBluetoothAddress != null) {
					cThread = new Thread(new ClientThread());
					cThread.start();
				} else {
					Log.d(TAG, "Could not connect to device...");
					Toast.makeText(this, R.string.bt_not_connected,
							Toast.LENGTH_SHORT).show();
				}
			}
			break;
		case REQUEST_ENABLE_BT:
			// When the request to enable Bluetooth returns
			if (resultCode == Activity.RESULT_OK) {
				// Bluetooth is now enabled,
				cThread = new Thread(new ClientThread());
				cThread.start();
			} else {
				// User did not enable Bluetooth or an error occured
				Log.d(TAG, "BT not enabled");
				Toast.makeText(this, R.string.bt_not_enabled_leaving,
						Toast.LENGTH_SHORT).show();
				finish();
			}
		}
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.option_menu, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case R.id.scan:
			if (!mBluetoothAdapter.isEnabled()) {
				Intent enableBluetooth = new Intent(
						BluetoothAdapter.ACTION_REQUEST_ENABLE);
				startActivityForResult(enableBluetooth, 0);
			}
			// Launch the DeviceListActivity to see devices and do scan
			Intent serverIntent = new Intent(this, DeviceListActivity.class);
			startActivityForResult(serverIntent, REQUEST_CONNECT_DEVICE);
			return true;
		case R.id.discoverable:
			// Ensure this device is discoverable by others
			// ensureDiscoverable();
			return true;
		}
		return false;
	}

	private OnClickListener connectListener = new OnClickListener() {
		Thread cThread;

		public void onClick(View v) {
			if (isConnected.isChecked() && !connected) {
				serverIpAddress = serverIp.getText().toString();
				if (!serverIpAddress.equals("")) {
					cThread = new Thread(new ClientThread());
					cThread.start();
				}
			} else if (connected) {
				sendData("q");
				connected = false;
				cThread.interrupt();
			}
		}
	};

	private JoystickMovedListener _listenerLeft = new JoystickMovedListener() {

		public void OnMoved(int pan, int tilt) {
			Log.d("JoyStickMovedListener", "PanX: " + Integer.toString(pan));
			Log.d("JoyStickMovedListener", "TiltY: " + Integer.toString(tilt));
			// txtX.setText(Integer.toString(pan));
			// txtY.setText(Integer.toString(tilt));
			processMovement(pan, tilt, 1);
		}

		public void OnReleased() {
			Log.d("JoyStickMovedListener", "Released...");
			// txtX.setText("released");
			// txtY.setText("released");
			processMovement(0, 0, 1);
		}

		public void OnReturnedToCenter() {
			Log.d("JoyStickMovedListener", "Stopped...");
			// txtX.setText("stopped");
			// txtY.setText("stopped");
			processMovement(0, 0, 1);
		};
	};

	private JoystickMovedListener _listenerRight = new JoystickMovedListener() {

		public void OnMoved(int pan, int tilt) {
			Log.d("JoyStickMovedListener", "PanX: " + Integer.toString(pan));
			Log.d("JoyStickMovedListener", "TiltY: " + Integer.toString(tilt));
			// txtX2.setText(Integer.toString(pan));
			// txtY2.setText(Integer.toString(tilt));
			processMovement(pan, tilt, 0);
		}

		public void OnReleased() {
			Log.d("JoyStickMovedListener", "Released...");
			// txtX2.setText("released");
			// txtY2.setText("released");
			processMovement(0, 0, 0);
		}

		public void OnReturnedToCenter() {
			Log.d("JoyStickMovedListener", "Stopped...");
			// txtX2.setText("stopped");
			// txtY2.setText("stopped");
			processMovement(0, 0, 0);
		};
	};
}