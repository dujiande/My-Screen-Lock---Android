package com.eva.me.myscreenlock.sensor;

import java.util.Calendar;

import android.R.integer;
import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.util.Log;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.eva.me.myscreenlock.R;
import com.eva.me.myscreenlock.psdlock.LocusPassWordView;
import com.eva.me.myscreenlock.psdlock.LoginActivity;
import com.eva.me.myscreenlock.util.StringUtil;


public class SensorMainActivity extends Activity implements SensorEventListener {

	private static final String TAG = SensorMainActivity.class.getSimpleName();
	public static final int OP_VALIDATE_PSD = 3;
	private SensorManager mSensorManager;
	private Sensor mSensor;
	private TextView textviewX;
	private TextView textviewY;
	private TextView textviewZ;
	private TextView textviewF;
	
	private TextView btn_switch;
	
	private Intent staSerIntent =null;
	
	private boolean wantedExit = false;

	public static boolean validateSuccess = false;
	public static boolean isOn = false;
	public static boolean initialTime = true;
	
	private int mX, mY, mZ;
	private long lasttimestamp = 0;
	Calendar mCalendar;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		Log.e(TAG+"SYS", "====== > OnCreate()");
		Log.e(TAG+"SYS", "====== > OnCreate() : initialTime: "+initialTime);
		Log.e(TAG+"SYS", "====== > OnCreate() : isOn: "+isOn);
		setContentView(R.layout.activity_sensor_main);
		init();
	}
	
	
	private void init() {
		staSerIntent = new Intent("com.eva.me.myscreenlock.sensor.AlarmPlayService");
		startService(staSerIntent);
		
		textviewX = (TextView) findViewById(R.id.textView1);
		textviewY = (TextView) findViewById(R.id.textView2);
		textviewZ = (TextView) findViewById(R.id.textView3);
		textviewF = (TextView) findViewById(R.id.textView4);

		initialText();
		initStaticFieldDefult();
		
		mSensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
		mSensor = mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);// TYPE_GRAVITY
		if (null == mSensorManager) {
			Log.d(TAG, "deveice not support SensorManager");
		}
		
//		// 参数三，检测的精准度
//		mSensorManager.registerListener(this, mSensor, SensorManager.SENSOR_DELAY_NORMAL);// SENSOR_DELAY_GAME
		
		btn_switch =(TextView) findViewById(R.id.tvSwitch);
		btn_switch.setOnClickListener(new OnClickListener() {
			
			@Override
			public void onClick(View v) {
				if (!isOn) {
					//是关闭状态的时候
					if (StringUtil.isEmpty(getPassword())) {
						//空密码 需要重设密码才能使用这项功能
						Toast.makeText(SensorMainActivity.this, "你需要先设置密码才能使用这项功能", Toast.LENGTH_SHORT).show();
						return;
					}
					
					btn_switch.setText("关闭防盗");
					Toast.makeText(SensorMainActivity.this, "防盗功能开启~", Toast.LENGTH_SHORT).show();
					SensorMainActivity.isOn = true;
					SensorMainActivity.initialTime = true;
					
					startAlarmPlayService(-1);
					
					// 参数三，检测的精准度
					mSensorManager.registerListener(SensorMainActivity.this, mSensor, SensorManager.SENSOR_DELAY_NORMAL);// SENSOR_DELAY_GAME
					
				} else {
					//是开启的状态的时候
					if(SensorMainActivity.validateSuccess == false) {
						Toast.makeText(SensorMainActivity.this, "请先验证密码再进行关闭", Toast.LENGTH_SHORT).show();
						Intent staLogin = new Intent();
						staLogin.putExtra("operation", SensorMainActivity.OP_VALIDATE_PSD);
						staLogin.setClass(SensorMainActivity.this, LoginActivity.class);
						startActivity(staLogin);
						
					}else if (SensorMainActivity.validateSuccess == true) {
						//接下里关闭这项防盗功能
						SensorMainActivity.validateSuccess = false;
						btn_switch.setText("开启防盗");
						Toast.makeText(SensorMainActivity.this, "防盗功能关闭，需要时请重新打开", Toast.LENGTH_SHORT).show();
						SensorMainActivity.isOn = false;
						SensorMainActivity.initialTime = false;
						initialText();
						
						startAlarmPlayService(3);
						
						mSensorManager.unregisterListener(SensorMainActivity.this, mSensor);
					}
					
				}
			}

		});
	}

	private void startAlarmPlayService(int op) {
		//启动播放类的服务
		if(staSerIntent == null) {
			staSerIntent = new Intent("com.eva.me.myscreenlock.sensor.AlarmPlayService");
		}
		staSerIntent.putExtra("op", op);
		startService(staSerIntent);
	}

	private void initialText() {
		textviewX.setText("0");
		textviewY.setText("0");
		textviewZ.setText("0");
		textviewF.setText("手机处于安静状态");
	}

	@Override
	public void onAccuracyChanged(Sensor sensor, int accuracy) {

	}

	@Override
	public void onSensorChanged(SensorEvent event) {
		if (event.sensor == null) {
			return;
		}

		if (event.sensor.getType() == Sensor.TYPE_ACCELEROMETER) {
			int x = (int) event.values[0];
			int y = (int) event.values[1];
			int z = (int) event.values[2];
			
			if (initialTime) {
				mX = x;
				mY = y;
				mZ = z;
				initialTime = false;
			}
			
			mCalendar = Calendar.getInstance();
			long stamp = mCalendar.getTimeInMillis() / 1000l;// 1393844912
			Log.e(TAG, "Stamp: "+stamp);
			
			textviewX.setText(String.valueOf(x));
			textviewY.setText(String.valueOf(y));
			textviewZ.setText(String.valueOf(z));

			int second = mCalendar.get(Calendar.SECOND);// 53
			Log.e(TAG, "Second: "+second);
			
			
			int px = Math.abs(mX - x);
			int py = Math.abs(mY - y);
			int pz = Math.abs(mZ - z);
			Log.d(TAG, "pX:" + px + "  pY:" + py + "  pZ:" + pz + "    stamp:"
					+ stamp + "  second:" + second+" Lasttimestamp: "+lasttimestamp);
			int maxvalue = getMaxValue(px, py, pz);
			if (maxvalue > 2 && (stamp - lasttimestamp) > 10) {
				lasttimestamp = stamp;
				Log.e(TAG, " sensor isMoveorchanged....");
				textviewF.setText("检测手机在移动..");
				
				startAlarmPlayService(1);
			}
			
			mX = x;
			mY = y;
			mZ = z;

		}
	}

	/**
	 * 获取一个最大值
	 * 
	 * @param px
	 * @param py
	 * @param pz
	 * @return
	 */
	public int getMaxValue(int px, int py, int pz) {
		int max = 0;
		if (px > py && px > pz) {
			max = px;
		} else if (py > px && py > pz) {
			max = py;
		} else if (pz > px && pz > py) {
			max = pz;
		}

		return max;
	}
	
	/**
	 * 取得密码
	 * 
	 * @return
	 */
	private String getPassword() {
		SharedPreferences settings = this.getSharedPreferences(
				LocusPassWordView.name, 0);
		Log.e(TAG, "NAME : "+LocusPassWordView.name);
		return settings.getString("password", ""); // , "0,1,2,3,4,5,6,7,8"
	}
	
	/**
	 * 设置密码
	 * 
	 * @param password
	 */
	public void resetPassWord(String password) {
		SharedPreferences settings = this.getSharedPreferences(
				LocusPassWordView.name, 0);
		Editor editor = settings.edit();
		editor.putString("password", password);
		editor.commit();
	}
	
	@Override
	protected void onResume() {
		super.onResume();
		
		Log.e(TAG+"SYS", "===== > onResume()");
		Log.e(TAG+"SYS", "===== > onResume() + SensorMainActivity.validateSuccess :"+SensorMainActivity.validateSuccess );
		
		if (SensorMainActivity.validateSuccess == true) {
			if (wantedExit) {
				wantedExit =false;
				Toast.makeText(SensorMainActivity.this, "验证通过，关闭防盗功能", Toast.LENGTH_SHORT).show();
				finish();
				return;
			}
			
			//接下里关闭这项防盗功能
			SensorMainActivity.validateSuccess = false;
			btn_switch.setText("开启防盗");
			Toast.makeText(SensorMainActivity.this, "防盗功能关闭，需要时请重新打开", Toast.LENGTH_SHORT).show();
			SensorMainActivity.isOn = false;
			SensorMainActivity.initialTime = false;
			initialText();
			
			startAlarmPlayService(3);
			
			mSensorManager.unregisterListener(SensorMainActivity.this, mSensor);
		}
		
	}
	
	@Override
	protected void onStart() {
		Log.e(TAG+"SYS", "======= > onStart() ");
		super.onStart();
	}
	
	@Override
	protected void onStop() {
		Log.e(TAG+"SYS", "==== > onStop()");
		super.onStop();
	}
	
	@Override
	protected void onDestroy() {
		Log.e(TAG+"SYS", "==== > onDestroy()");
		//进行垃圾回收机制，终于理解了为什么十分的重要
		initStaticFieldDefult();
		
		if (staSerIntent != null) {
			stopService(staSerIntent);
			staSerIntent = null;
			Log.e(TAG+"SYS", "stop service and staSerIntent is not null");
		}
		
		if (mSensor != null) {
			mSensorManager.unregisterListener(SensorMainActivity.this, mSensor);
			Log.e(TAG+"SYS", "unregisterListener and mSensor is not null");				
		}
		
		super.onDestroy();
	}
	
	private void initStaticFieldDefult() {
		SensorMainActivity.initialTime = true;
		SensorMainActivity.isOn = false;
		SensorMainActivity.validateSuccess = false;
	}


	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.sensor_main, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		// Handle action bar item clicks here. The action bar will
		// automatically handle clicks on the Home/Up button, so long
		// as you specify a parent activity in AndroidManifest.xml.
		int id = item.getItemId();
		if (id == R.id.action_settings) {
			return true;
		}
		return super.onOptionsItemSelected(item);
	}
	
	@Override
	public boolean onKeyDown(int keyCode, KeyEvent event) {
		if (keyCode == KeyEvent.KEYCODE_BACK) {
			Log.e(TAG+"SYS", "按动返回键");
			if(!SensorMainActivity.isOn) 
				finish();
			else {
				wantedExit = true;
				initOnBackKeyPressed();
			}
			
			return true;
		}
		return super.onKeyDown(keyCode, event);
	}

	private void initOnBackKeyPressed() {
		if(SensorMainActivity.validateSuccess == false) {
			Toast.makeText(SensorMainActivity.this, "您真的想要关闭这项功能吗？请验证密码", Toast.LENGTH_SHORT).show();
			Intent staLogin = new Intent();
			staLogin.putExtra("operation", SensorMainActivity.OP_VALIDATE_PSD);
			staLogin.setClass(SensorMainActivity.this, LoginActivity.class);
			startActivity(staLogin);
			
		}else if (SensorMainActivity.validateSuccess == true) {
			//接下里关闭这项防盗功能
			//下面貌似永远进不了
			Toast.makeText(SensorMainActivity.this, "防盗功能关闭，需要时请重新打开", Toast.LENGTH_SHORT).show();
			SensorMainActivity.this.finish();
		}
	}
	
	
}
