package com.vivi;

import java.io.File;


import com.changchen.downloader.R;
//import com.vivi.R
import android.app.Activity;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

public class MulThreadDownloader extends Activity {
	private EditText pathText;
	private ProgressBar progressBar;
	private TextView resultView;
	//handler的作用是向绑定对象的线程中发送消息队列
	private Handler handler = new Handler(){
		public void handleMessage(Message msg) {
			//如果线程没有被中断
			if (!Thread.currentThread().isInterrupted()) {
				switch (msg.what) {
				case 1:
					int size = msg.getData().getInt("size");
					progressBar.setProgress(size);
					//更新进度条的显示值
					int result = (int) (((float) size / (float) progressBar
							.getMax()) * 100);
					resultView.setText(result + "%");
					if (size == progressBar.getMax()) {
						Toast.makeText(MulThreadDownloader.this, "文件下载完成",
								Toast.LENGTH_LONG).show();
					}
					break;
				case -1:
					String error = msg.getData().getString("error");
					Toast.makeText(MulThreadDownloader.this, error,
							Toast.LENGTH_LONG).show();
					break;
				}
			}
			super.handleMessage(msg);
		}
	};

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);
        pathText = (EditText)findViewById(R.id.path);
        progressBar = (ProgressBar)findViewById(R.id.downloadbar);
        resultView = (TextView)findViewById(R.id.resultView);
        Button button = (Button)findViewById(R.id.button);
        button.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				String path = pathText.getText().toString().trim();
				//Environment.MEDIA_MOUNTED 判断SD卡是否存在
				//Environment.getExternalStorageDirectory()获取根目录
				if(Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED)){
					download(path,Environment.getExternalStorageDirectory());
				}
				else{
					Toast.makeText(MulThreadDownloader.this, R.string.sdcarderror, Toast.LENGTH_LONG).show();
				}
			}			
		});
        
    }
    //因为不能用主线程处理一个需要很长时间的事件
    //所以要开启子进程进行处理。但是控件画面的重绘需要由主线程来控制，所以需要用到handler
    private void download(final String path,final File saveDir) {
		new Thread(new Runnable() {
			@Override
			public void run() {
				FileDownloader downer = new FileDownloader(MulThreadDownloader.this,path,saveDir,3);
				progressBar.setMax(downer.getFileSize());
				try{
					downer.download(new DownloadProgressListener(){
						public void onDownloadSize(int size){
							Message msg = new Message();
							msg.what = 1;
							msg.getData().putInt("size", size);
							handler.sendMessage(msg);
						}
					});
				}catch(Exception e){
					Message msg = new Message();
					msg.what = -1;
					msg.getData().putString("error", "下载失败");
					handler.sendMessage(msg);
				}
			}
		}).start();
	}
}