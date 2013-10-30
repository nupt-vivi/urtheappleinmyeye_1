package com.ora;

import java.util.HashMap;
import java.util.Map;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

public class FileService {
	private DBOpenHelper openHelper;

	public FileService(Context context) {
		openHelper = new DBOpenHelper(context);
	}
    //获取每条线程已经下载的长度
	public Map<Integer, Integer> getData(String path) {
		SQLiteDatabase  db = openHelper.getReadableDatabase();
		Cursor cursor = db.rawQuery("select threadid,position from filedown where downpath=?", new String[]{path});
		Map<Integer,Integer> data = new HashMap<Integer, Integer>();
		while(cursor.moveToNext()){
			data.put(cursor.getInt(0), cursor.getInt(1));
		}
		cursor.close();
		db.close();
		return data;
	}
      //保存每条线程已经下载的长度
	public void save(String path,Map<Integer, Integer> map) {
		SQLiteDatabase db = openHelper.getWritableDatabase();
		db.beginTransaction();
		try{
			for(Map.Entry<Integer, Integer>entry:map.entrySet()){
				db.execSQL("insert into filedown(downpath,threadid,position)values(?,?,?)", new Object[]{path,entry.getKey(),entry.getValue()});
			}
			db.setTransactionSuccessful();
		}finally{
			db.endTransaction();
		}
		db.close();
	}
    //实时更新每条线程下载的长度
	public void update(String path,Map<Integer, Integer> map) {
		SQLiteDatabase db = openHelper.getWritableDatabase();
		db.beginTransaction();
		try{
			for(Map.Entry<Integer, Integer>entry:map.entrySet()){
				db.execSQL("update filedown set position=? where downpath=? and threadid=?", 
						new Object[]{entry.getValue(),path,entry.getKey()} );
			}
			db.setTransactionSuccessful();
		}finally{
			db.endTransaction();
		}
		db.close();
	}
     //当文件下载完成后，删除相应的下载记录
	public void delete(String path) {
		SQLiteDatabase db = openHelper.getWritableDatabase();
		db.execSQL("delete from filedown where downpath=?", new Object[]{path});
		db.close();
	}
}
