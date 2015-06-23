package com.shizhongkeji.musicplayer;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.ListView;

import com.shizhongkeji.GlobalApplication;
import com.shizhongkeji.adapter.SystemMusicAdapter;
import com.shizhongkeji.info.AppConstant;
import com.shizhongkeji.info.Mp3Info;
import com.shizhongkeji.service.PlayerService;
import com.shizhongkeji.sqlutils.DBManager;
import com.shizhongkeji.utils.MediaUtil;

public class SystemSongActivity extends Activity implements OnClickListener {
	private ListView mListMusic;
	private SystemMusicAdapter mAdapter;
	private List<Mp3Info> mp3Infos_application;
	private List<Mp3Info> mp3Infos_db;
	private Button cancel;
	private Button confirm;
	private HashMap<Integer, Boolean> checkMusic;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_editsong);
		cancel = (Button) findViewById(R.id.cancel);
		confirm = (Button) findViewById(R.id.confirm);
		cancel.setOnClickListener(this);
		confirm.setOnClickListener(this);
		mp3Infos_db = new ArrayList<Mp3Info>();
		mp3Infos_application = MediaUtil.getMp3Infos(this);
		mListMusic = (ListView) findViewById(R.id.song_list);
		mAdapter = new SystemMusicAdapter(mp3Infos_application, this);
		mListMusic.setAdapter(mAdapter);
	}

	@Override
	public void onClick(View v) {
		switch (v.getId()) {
		case R.id.cancel:
			finish();
			break;
		case R.id.confirm:
			checkMusic = GlobalApplication.isSelecte;
			Iterator<Entry<Integer, Boolean>> iterator = checkMusic.entrySet().iterator();
			while (iterator.hasNext()) {
				Map.Entry<Integer, Boolean> entry = iterator.next();
				boolean isCheck = entry.getValue();
				if (isCheck) {
					int index = entry.getKey();
					Mp3Info mp3info = mp3Infos_application.get(index);
					mp3Infos_db.add(mp3info);
					DBManager.getInstance(this).insertMusic(mp3Infos_db);
					Intent intent = new Intent(getApplicationContext(), PlayerService.class);
					intent.putExtra("MSG", AppConstant.PlayerMsg.ADD_MUSIC);
					startService(intent);
				}
			}
			finish();
			break;
		default:
			break;
		}
	}

}
