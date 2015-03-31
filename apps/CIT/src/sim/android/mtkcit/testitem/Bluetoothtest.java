package sim.android.mtkcit.testitem;

import java.util.List;
import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.os.SystemClock;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import sim.android.mtkcit.R;
import sim.android.mtkcit.CITActivity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;

public class Bluetoothtest extends TestBase {
	/** Called when the activity is first created. */
	private TextView allNetWork;
	private Button btn_success;
	private Button btn_fail;
	// 扫描结果列表
	private StringBuffer sb = new StringBuffer();
	private BluetoothAdapter mAdapter;
	private BluetoothDevice mDevice;
	private int num;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.test_bt);
		
		allNetWork = (TextView) findViewById(R.id.allNetWork);
		btn_success = (Button) findViewById(R.id.btn_success);
		btn_success.setOnClickListener(this);
		btn_fail = (Button) findViewById(R.id.btn_fail);
		btn_fail.setOnClickListener(this);
		ct.initButton(btn_success);
		
		// 设置广播信息过滤
		IntentFilter intentFilter = new IntentFilter();   
		intentFilter.addAction(BluetoothDevice.ACTION_FOUND); 
		intentFilter.addAction(BluetoothAdapter.ACTION_LOCAL_NAME_CHANGED); 
//		intentFilter.addAction(BluetoothAdapter.ACTION_DISCOVERY_STARTED); 
		intentFilter.addAction(BluetoothAdapter.ACTION_DISCOVERY_FINISHED); 
		
		// 注册广播接收器，接收并处理搜索结果
		this.registerReceiver(mReceiver, intentFilter); 
		
		// open bt
		mAdapter = BluetoothAdapter.getDefaultAdapter();
		if (!mAdapter.isEnabled())
			mAdapter.enable();
		
		if (mAdapter.getState() != mAdapter.STATE_ON)
			mAdapter.enable();
		
		// 延迟2s，否则BT扫描不到设备，why???
		SystemClock.sleep(3000L);
		allNetWork.setText("Searching...");
		getAllNetWorkList();
	}

	// 搜索周围的蓝牙设备
	BroadcastReceiver mReceiver = new BroadcastReceiver() {
		public void onReceive(Context context, Intent intent) {
			String action = intent.getAction();
			
			Log.i("Bluetoothtest000","11111111111111111111111");
		    //找到设备
			if (action.equals(BluetoothDevice.ACTION_FOUND) ||
					action.equals(BluetoothAdapter.ACTION_LOCAL_NAME_CHANGED)) {
				BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
				
				num++;	
				btn_success.setEnabled(true);
				
				Log.i("Bluetoothtest000","num=" + num);
				
				//执行更新列表的代码
				if(device != null){
									sb = sb.append("[" + device.getName() + "]Address: ")
							.append(device.getAddress() + "\n\n");
							allNetWork.setText("Scanning to Bluetooth device: \n" + sb.toString());
					}
			}
			//搜索完成
			else if (action.equals(BluetoothAdapter.ACTION_DISCOVERY_FINISHED)) {
				if (num == 0) {
					Toast.makeText(context, "Search completed,There is no device", Toast.LENGTH_SHORT).show();
				} else {
					Toast.makeText(context, "Search completed", Toast.LENGTH_SHORT).show();
				}
			}
		}
	}; 
	
	public void getAllNetWorkList() {
		mAdapter = BluetoothAdapter.getDefaultAdapter();
		
		// 每次点击扫描之前清空上一次的扫描结果
		if (sb != null) {
			sb = new StringBuffer();
		}
		
		// close first
//		mAdapter.disable();
		
		// open bt
		if (!mAdapter.isEnabled())
			mAdapter.enable();
		
		if (mAdapter.getState() != mAdapter.STATE_ON)
			mAdapter.enable();
			
		// 寻找蓝牙设备，android会将查找到的设备以广播形式发出去 
		if (!mAdapter.isDiscovering()) {
			mAdapter.startDiscovery();
        }
		
	}
	
	public void onClick(View v) {
		// TODO Auto-generated method stub
		int id = v.getId();
		Bundle b = new Bundle();
		Intent intent = new Intent();

		if (id == R.id.btn_success) {
			b.putInt("test_result", 1);
		} else {
			b.putInt("test_result", 0);
		}
		
		// close 
		mAdapter.disable();
		
		intent.putExtras(b);
		setResult(RESULT_OK, intent);
		finish();
	}
	
	@Override
    protected void onDestroy() {
    	// TODO Auto-generated method stub
    	super.onDestroy();
    	unregisterReceiver(mReceiver);
    }
    
	@Override
	protected void onStop() {
		Log.i("Bluetoothtest000", "onStop");
		
		// close 
		mAdapter.disable();
		
		super.onStop();
	}
}
