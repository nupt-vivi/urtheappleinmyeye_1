package com.vivi;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import android.content.Context;
import android.util.Log;

import com.ora.FileService;

public class FileDownloader {
	//private Context context;
	private FileService fileService;
	private static final String TAG = "fildownloader";
	private int downloadSize = 0;
	private int fileSize = 0;
	private DownloadThread[] threads;
	private URL url;
	private File saveFile;
	private ConcurrentHashMap<Integer, Integer> data = new ConcurrentHashMap<Integer, Integer>();
	private int block;
	private String downloadUrl;
	
	//获取线程数
	public int getThreadSize(){
		return threads.length;
	}	
	//获取文件大小
	public int getFileSize() {
		return fileSize;
	}
	//累计下载文件的大小
	protected synchronized void append(int size){
		downloadSize += size;
	}
	/*
	 *	更新指定线程最后下载位置
	 */
	protected void update(int threadId,int pos){
		this.data.put(threadId, pos);
	}
	
	//保存下载的数据
	protected synchronized void saveLogFile(){
		this.fileService.update(this.downloadUrl,this.data);
	}
	
	//开始构造下载文件的构造器
	public FileDownloader(Context context, String downloadUrl,
			File fileSaveDir, int threadNum) {
		//this.context = context;
		this.downloadUrl = downloadUrl;
		fileService = new FileService(context);
		try {
			this.url = new URL(downloadUrl);
			//如果下载目录存在的话
		    if(!fileSaveDir.exists()){
			fileSaveDir.mkdirs();
		}
		this.threads = new DownloadThread[threadNum];
		HttpURLConnection conn = (HttpURLConnection)url.openConnection();
		conn.setConnectTimeout(6*1000);
		//Android使用get方式提交参数给网络
		conn.setRequestMethod("GET");
		//定义各种属性，比如编码方式，代理服务，支持语言等等。
		conn.setRequestProperty("Accept", "image/gif,image/jpeg,image/pjpeg,application/X-shockwave-flash," +
				"application/xaml+xml,application/vnd.ms-xpsdocument,application/X-ms-xbap," +
				"application/X-ms-application,application/vnd.ms-excel,application/vnd.ms-powerpiont,application/msword,*/*");
		conn.setRequestProperty("Accept-Language", "zh-CN");
		conn.setRequestProperty("Referer", downloadUrl);
		conn.setRequestProperty("Charset", "UTF-8");
		conn.setRequestProperty("User-Agent", "Mozilla/4.0(compatible;MSIE 8.0;Windows NT5.2;Trident/4.0;.NET CLR 1.1.4322;.Net CLR 2.0.50727;" +
				".NET CLR 3.0.04506.30;.NET CLR 3.0.4506.2152;.NET CLR 3.5.30729）");
		conn.setRequestProperty("Connection", "Keep-Alive");
		//开启网络连接
		conn.connect();
		//见后面的打印Http头文件
		printResponseHeader(conn);
		if(conn.getResponseCode()==200){
			this.fileSize = conn.getContentLength();//根据响应获取文件大小
			if(this.fileSize<=0)throw new RuntimeException("无法获知文件大小");
			String filename = getFileName(conn);
			this.saveFile = new File(fileSaveDir,filename);//保存文件
			Map<Integer,Integer> logdata = fileService.getData(downloadUrl);
			if(logdata.size()>0){
				data.putAll(logdata);
			}
			this.block = this.fileSize/this.threads.length+1;
			if(this.data.size()==this.threads.length){
				for(int i=0;i<this.threads.length;i++){
					this.downloadSize +=this.data.get(i+1)-(this.block*i);
				}
				print("已经下载的长度"+this.downloadSize);
			}
		}else{
			throw new RuntimeException("服务器响应错误");
		}
		} catch (MalformedURLException e) {
			e.printStackTrace();
		} catch (IOException e) {
			print(e.toString());
			e.printStackTrace();
		}
	}
	
	private static void print(String string) {
		Log.d(TAG, string);
	}
	private String getFileName(HttpURLConnection conn) {
		String filename = this.url.toString().substring(this.url.toString().lastIndexOf('/')+1);
		if(filename==null||"".equals(filename.trim())){
			for(int i=0;;i++){
				String mine = conn.getHeaderField(i);
				if(mine==null)break;
				if("content-disposition".equals(conn.getHeaderFieldKey(i).toLowerCase())){
					Matcher m = Pattern.compile(".*filename=(.*)").matcher(mine.toLowerCase());
					if(m.find())return m.group(1);
				}
			}
			filename = UUID.randomUUID()+".tmp";//默认取一个文件名
		}
		return filename;
	}
	
	public int download(DownloadProgressListener listener)throws Exception {
		try{
		if(this.data.size()!=this.threads.length){
			this.data.clear();
			for(int i=0;i<this.threads.length;i++){
				this.data.put(i+1, this.block*i);
			}
		}
		for(int i=0;i<this.threads.length;i++){
			int downLength = this.data.get(i+1)-(this.block*i);
			if(downLength<this.block&&this.data.get(i+1)<this.fileSize){
				RandomAccessFile randOut = new RandomAccessFile(this.saveFile,"rwd");
				if(this.fileSize>0)   randOut.setLength(this.fileSize);
				this.threads[i] = new DownloadThread(this,this.url,randOut,this.block,this.data.get(i+1),i+1);
				this.threads[i].setPriority(7);
				this.threads[i].start();
			}else{
				this.threads[i]=null;
			}
		}
		this.fileService.save(this.downloadUrl,this.data);
		boolean notFinish = true;
		while(notFinish){
			Thread.sleep(900);
			notFinish = false;
			for(int i=0;i<this.threads.length;i++){
				if(this.threads[i]!=null&&!this.threads[i].isFinish()){
					notFinish = true;//下载没有完成
					if(this.threads[i].getDownLength()==-1){
						//如果失败重新下载
						RandomAccessFile randOut = new RandomAccessFile(this.saveFile, "rw");
						randOut.seek(this.data.get(i+1));
						this.threads[i] = new DownloadThread(this, this.url, randOut, this.block, this.data.get(i+1), i+1);
						this.threads[i].setPriority(7);
						this.threads[i].start();
					}
				}
			}
			if(listener!=null)listener.onDownloadSize(this.downloadSize);
		}
		fileService.delete(this.downloadUrl);
		}catch(Exception e){
			print(e.toString());
			throw new Exception("下载失败");
		}
		return this.downloadSize;
	}	
	//响应Http头文件
	public static Map<String,String> getHttpResponseHeader(HttpURLConnection http){
		Map<String,String>header = new LinkedHashMap<String,String>();
		for(int i=0;;i++){
			String mine = http.getHeaderField(i);
			if(mine==null)break;
			header.put(http.getHeaderFieldKey(i), mine);
		}
		return header;
	}
	//打印Http头文件
	public static void printResponseHeader(HttpURLConnection http) {
		Map<String,String>header = getHttpResponseHeader(http);
		for(Map.Entry<String, String> entry:header.entrySet()){
			String key = entry.getKey()!=null?entry.getKey()+":":"";
			print(key+entry.getValue());
		}
	}

}
