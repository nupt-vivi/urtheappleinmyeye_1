package com.ora;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;


  //这里定义的数据名字叫download.db,版本号为2，表名filedown,里面有三个字段，
  //一个id,主键，下载路径downpath，线程ID和一个下载长度position
public class DBOpenHelper extends SQLiteOpenHelper{
	private static final String DBNAME = "download.db";
	private static final int VERSION = 2;

	public DBOpenHelper(Context context) {
		super(context,DBNAME,null,VERSION);
	}

	@Override
	public void onCreate(SQLiteDatabase db) {
		db.execSQL("CREATE TABLE IF NOT EXISTS filedown(id integer primary key autoincrement,downpath varchar(100),threadid INTEGER,position INGEGER)");
	}

	@Override
	public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
		db.execSQL("DROP TABLE IF EXISTS filedown");
		onCreate(db);
	}
}
