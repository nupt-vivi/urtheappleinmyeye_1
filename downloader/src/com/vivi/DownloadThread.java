package com.vivi;

import java.io.InputStream;
import java.io.RandomAccessFile;
import java.net.HttpURLConnection;
import java.net.URL;

import android.util.Log;

public class DownloadThread extends Thread {
	private static final String TAG = "DownloadThread";
	private RandomAccessFile saveFile;
	private URL downUrl;
	private int block;
	private int threadId=-1;
	private int startPos;
	private int downLength;
	private boolean finish = false;
	private FileDownloader downloader;
	

	public DownloadThread(FileDownloader downloader, URL downUrl,
			RandomAccessFile saveFile, int block, int startPos, int threadId) {
		this.downUrl = downUrl;
		this.saveFile = saveFile;
		this.block = block;
		this.startPos = startPos;
		this.downloader = downloader;
		this.threadId = threadId;
		this.downLength = startPos-(block*(threadId-1));
	}
	
	public void run(){
		if(downLength<block){
			try{
				HttpURLConnection conn = (HttpURLConnection)downUrl.openConnection();
				conn.setConnectTimeout(6*1000);
				conn.setRequestMethod("GET");
				conn.setRequestProperty("Accept", "image/gif,image/jpeg,image/pjpeg,application/X-shockwave-flash," +
						"application/xaml+xml,application/vnd.ms-xpsdocument,application/X-ms-xbap," +
						"application/X-ms-application,application/vnd.ms-excel,application/vnd.ms-powerpiont,application/msword,*/*");
				conn.setRequestProperty("Accept-Language", "zh-CN");
				conn.setRequestProperty("Referer", downUrl.toString());
				conn.setRequestProperty("Charset", "UTF-8");
				//int startPos = block * (threadId - 1) + downLength;// 开始位置
                //int endPos = block * threadId - 1;// 结束位置
				conn.setRequestProperty("Range", "bytes="+this.startPos+"-");
				conn.setRequestProperty("User-Agent", "Mozilla/4.0(compatible;MSIE 8.0;Windows NT5.2;Trident/4.0;.NET CLR 1.1.4322;.Net CLR 2.0.50727;" +
						".NET CLR 3.0.04506.30;.NET CLR 3.0.4506.2152;.NET CLR 3.5.30729）");
				conn.setRequestProperty("Connection", "Keep-Alive");
				InputStream inStream = conn.getInputStream();
				int max = block>1024?1024:(block>10?10:1);
				byte[]buffer = new byte[max];
				int offset = 0;
				print("线程"+this.threadId+"从位置"+this.startPos+"开始下载");
				while(downLength<block&&(offset=inStream.read(buffer,0,max))!=-1){
					saveFile.write(buffer, 0, offset);
					downLength +=offset;
					downloader.update(this.threadId,block*(threadId-1)+downLength);
					downloader.saveLogFile();
					downloader.append(offset);
					int spare = block-downLength;
					if(spare<max)max = (int)spare;
				}
				saveFile.close();
				inStream.close();
				print("线程"+this.threadId+"完成下载");
				this.finish = true;
				this.interrupt();
			}catch(Exception e){
				this.downLength = -1;
				print("线程"+this.threadId+":"+e);
			}
		}
	}
	private static void print(String string){
		Log.i(TAG, string);
	}
	public boolean isFinish() {
		return finish;
	}

	public int getDownLength() {
		return downLength;
	}

}
