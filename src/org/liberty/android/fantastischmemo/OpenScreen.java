/*
Copyright (C) 2010 Haowen Ning

This program is free software; you can redistribute it and/or
modify it under the terms of the GNU General Public License
as published by the Free Software Foundation; either version 2
of the License, or (at your option) any later version.

This program is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.

See the GNU General Public License for more details.

You should have received a copy of the GNU General Public License
along with this program; if not, write to the Free Software
Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.

*/
package org.liberty.android.fantastischmemo;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.ListIterator;
import java.util.List;
import java.util.Map;
import java.util.ArrayList;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.LayoutInflater;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.AdapterView;
import android.widget.TextView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.SimpleAdapter;
import android.widget.AdapterView.OnItemClickListener;
import android.view.View.OnClickListener;
import android.content.Context;

public class OpenScreen extends Activity implements OnItemClickListener, OnClickListener {

	private String dbName;
	private String dbPath;
	private int returnValue;
    private Context mContext;
    private RecentOpenList mRecentOpenList;
    private ListView recentListView;
    private ArrayList<RecentItem> recentItemList;
    private Button openButton;
    private Button importButton;
    private ProgressDialog mProgressDialog;
    private Handler mHandler;

    /* This part is added for threading data sharing */
    private Intent tmpIntent = null;
    private AlertDialog.Builder mAlert;


    private final int ACTIVITY_DB = 1;
    private final int ACTIVITY_XML = 2;
    private final int ACTIVITY_EXPORT_XML = 3;


	private List<HashMap<String, String>> recentList;
    public final static String TAG = "org.liberty.android.fantastischmemo.OpenScreen";

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
        setContentView(R.layout.open_screen);
        mContext = this;
        mRecentOpenList = new RecentOpenList(this);
        openButton = (Button)findViewById(R.id.open_screen_open_exist);
        importButton = (Button)findViewById(R.id.open_screen_import);
        openButton.setOnClickListener(this);
        importButton.setOnClickListener(this);
        mHandler = new Handler();

	}

    @Override
    public void onClick(View v){
        Intent myIntent = new Intent();

        if(v == openButton){
            myIntent.setClass(this, FileBrowser.class);
            myIntent.putExtra("default_root", dbPath);
            myIntent.putExtra("file_extension", ".db");
            startActivityForResult(myIntent, ACTIVITY_DB);
        }

        if(v == importButton){
            myIntent.setClass(this, FileBrowser.class);
            myIntent.putExtra("default_root", dbPath);
            myIntent.putExtra("file_extension", ".xml");
            startActivityForResult(myIntent, ACTIVITY_XML);
        }

    }

    @Override
	public void onItemClick(AdapterView<?> parentView, View childView, int position, long id) {
		
    		Intent myIntent = new Intent();
    		myIntent.setClass(this, MemoScreen.class);
    		this.dbPath = recentList.get(position).get("recentdbpath");
    		this.dbName = recentList.get(position).get("recentdbname");
    		myIntent.putExtra("dbname", dbName);
    		myIntent.putExtra("dbpath", dbPath);
    		mRecentOpenList.writeNewList(dbPath, dbName);
    		startActivity(myIntent);
		
	}
	
	
    public void onResume(){
    	super.onResume();
        if(returnValue == ACTIVITY_XML){
            return;
        }
    	if(returnValue == ACTIVITY_DB){
    		SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(this);
    		//SharedPreferences settings = getSharedPreferences(PREFS_NAME, 0);
    		SharedPreferences.Editor editor = settings.edit();
    		editor.putString("dbname", this.dbName);
    		editor.putString("dbpath", this.dbPath);
    		editor.commit();
    		
    		mRecentOpenList.writeNewList(this.dbPath, this.dbName);
    		
    		
    		Intent myIntent = new Intent();
    		myIntent.setClass(this, MemoScreen.class);
    		myIntent.putExtra("dbname", dbName);
    		myIntent.putExtra("dbpath", dbPath);
    		startActivity(myIntent);
    		returnValue = 0;
            return;
    	}
            mProgressDialog = ProgressDialog.show(this, getString(R.string.loading_please_wait), getString(R.string.loading_recent_list), true);

            recentListView = (ListView)findViewById(R.id.recent_open_list);
            recentListView.setOnItemClickListener(this);

            Thread loadingThread = new Thread(){
                public void run(){

    	
            recentItemList = new ArrayList<RecentItem>();
            
            // Fill the recent open list from the pref
            mRecentOpenList = new RecentOpenList(mContext);
            recentList = mRecentOpenList.getList();
            
            for(HashMap<String, String> hm : recentList){
                recentItemList.add(new RecentItem(hm.get("recentdbname"), mContext.getString(R.string.stat_total) + hm.get("recentdbtotal") + " " + mContext.getString(R.string.stat_new) + hm.get("recentnew") + " " + mContext.getString(R.string.stat_scheduled) + hm.get("recentscheduled")));
            }


            mHandler.post(new Runnable(){
                public void run(){
            
            recentListView.setAdapter(new RecentListAdapter(mContext, R.layout.open_screen_recent_item, recentItemList));
            mProgressDialog.dismiss();

                }
            });
                }
            };
            loadingThread.start();
		
    	
    }
    public void onActivityResult(int requestCode, int resultCode, Intent data){
    	super.onActivityResult(requestCode, resultCode, data);
    	switch(requestCode){
    	case ACTIVITY_DB:
    		if(resultCode == Activity.RESULT_OK){
    			dbName = data.getStringExtra("org.liberty.android.fantastischmemo.dbName");
    			dbPath = data.getStringExtra("org.liberty.android.fantastischmemo.dbPath");
    			dbPath += "/";
    			returnValue = ACTIVITY_DB;
    			
    		}
    		break;
    	
    	case ACTIVITY_XML:
    		if(resultCode == Activity.RESULT_OK){
                returnValue = ACTIVITY_XML;

                mProgressDialog = ProgressDialog.show(this, getString(R.string.loading_please_wait), getString(R.string.loading_import), true);
                tmpIntent = data;
                Thread convertThread = new Thread(){
                    @Override
                    public void run(){
                        XMLConverter conv = null;
                        mAlert = new AlertDialog.Builder(mContext);
                        mAlert.setPositiveButton( "OK", null );
                        String xmlPath = tmpIntent.getStringExtra("org.liberty.android.fantastischmemo.dbPath");
                        String xmlName = tmpIntent.getStringExtra("org.liberty.android.fantastischmemo.dbName");
                        try{
                            conv = new XMLConverter(mContext, xmlPath, xmlName);
                            conv.outputDB();
                            mAlert.setTitle(getString(R.string.success));
                            mAlert.setMessage(getString(R.string.success_import)+ " " + xmlPath + "/" + xmlName.replaceAll(".xml", ".db"));
                            
                            //conv.outputTabFile();
                        }
                        catch(Exception e){
                            Log.e("XMLError",e.toString());
                            mAlert.setTitle(getString(R.string.fail));
                            mAlert.setMessage(getString(R.string.fail_import) + " " + xmlPath + "/" + xmlName + " Exception: " + e.toString());
                        }
                        mHandler.post(new Runnable(){
                            @Override
                            public void run(){
                                mProgressDialog.dismiss();
                                mAlert.show();
                            }
                        });
                    }
                };
                convertThread.start();

    		}
    		break;
    	case ACTIVITY_EXPORT_XML:
    		if(resultCode == Activity.RESULT_OK){
                returnValue = ACTIVITY_XML;

                mProgressDialog = ProgressDialog.show(this, getString(R.string.loading_please_wait), getString(R.string.loading_export), true);
                tmpIntent = data;
                Thread convertThread = new Thread(){
                    @Override
                    public void run(){
                        String dbPath = tmpIntent.getStringExtra("org.liberty.android.fantastischmemo.dbPath");
                        String dbName = tmpIntent.getStringExtra("org.liberty.android.fantastischmemo.dbName");
                        mAlert = new AlertDialog.Builder(mContext);
                        mAlert.setPositiveButton( "OK", null );
                        try{
                            DBExporter exporter = new DBExporter(mContext, dbPath, dbName);
                            exporter.writeXML();
                            mAlert.setTitle(getString(R.string.success));
                            mAlert.setMessage(getString(R.string.success_export)+ " " + dbPath + "/" + dbName.replaceAll(".db", ".xml"));
                        }
                        catch(Exception e){
                            Log.e(TAG, "XML export error", e);
                            mAlert.setTitle(getString(R.string.fail));
                            mAlert.setMessage(getString(R.string.fail_export) + " " + dbPath + "/" + dbName + " Exception: " + e.toString());
                        }

                        mHandler.post(new Runnable(){
                            @Override
                            public void run(){
                                mProgressDialog.dismiss();
                                mAlert.show();
                            }
                        });
                    }
                };
                convertThread.start();

            }

    		
    		
    	}
    }
    
	public boolean onCreateOptionsMenu(Menu menu){
		MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.open_screen_menu, menu);
		return true;
	}
	
	public boolean onOptionsItemSelected(MenuItem item) {
	    switch (item.getItemId()) {
	    case R.id.openmenu_clear:
	    	SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(this);
	    	SharedPreferences.Editor editor = settings.edit();
			for(int i = 0; i < RecentOpenList.MAX_LIST_NUMBER; i++){
	    		//SharedPreferences settings = getSharedPreferences(PREFS_NAME, 0);
	    		editor.putString("recentdbname" + i, null);
	    		editor.putString("recentdbpath" + i, null);
			}
			editor.commit();
			Intent refresh = new Intent(this, OpenScreen.class);
			startActivity(refresh);
			this.finish();
			return true;
        
        case R.id.openmenu_export_xml:
            Intent myIntent = new Intent();
            myIntent.setClass(this, FileBrowser.class);
            myIntent.putExtra("default_root", dbPath);
            myIntent.putExtra("file_extension", ".db");
            startActivityForResult(myIntent, ACTIVITY_EXPORT_XML);

	    }
	    return false;
	}

    
    private class RecentItem{
        String mFileName;
        String mInfo;

        public RecentItem(){
        }

        public RecentItem(String name, String info){
            mFileName = name;
            mInfo = info;
        }

        public String getFileName(){
            return mFileName;
        }

        public String getInfo(){
            return mInfo;
        }

        public void setFilename(String name){
            mFileName = name;
        }

        public void setInfo(String info){
            mInfo = info;
        }

    }

    private class RecentListAdapter extends ArrayAdapter<RecentItem>{
        private ArrayList<RecentItem> mItems;

        public RecentListAdapter(Context context, int textViewResourceId, ArrayList<RecentItem> items){
            super(context, textViewResourceId, items);
            mItems = items;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent){
            View v = convertView;
            if(v == null){
                LayoutInflater li = (LayoutInflater)getSystemService(Context.LAYOUT_INFLATER_SERVICE);
                v = li.inflate(R.layout.open_screen_recent_item, null);
            }
            RecentItem recentItem = mItems.get(position);
            if(recentItem != null){
                TextView filenameView = (TextView)v.findViewById(R.id.recent_item_filename);
                TextView infoView = (TextView)v.findViewById(R.id.recent_item_info);
                filenameView.setText(recentItem.getFileName());
                infoView.setText(recentItem.getInfo());
            }
            return v;
        }

    }

    private class RecentOpenList {
        public static final int MAX_LIST_NUMBER = 5;
        final Context mContext;
        List<HashMap<String, String>> recentList;
        Integer i = Integer.valueOf(1);;
        
        public RecentOpenList(Context context){
            recentList = new LinkedList<HashMap<String, String>>();
            mContext = context; 
            fetchListFromPref();
        }
        
        public List<HashMap<String, String>> getList(){
            ListIterator<HashMap<String, String>> it = recentList.listIterator();
            while(it.hasNext()){
                HashMap<String,  String> hm = (HashMap<String, String>)it.next();
                String dbname = hm.get("recentdbname");
                String dbpath = hm.get("recentdbpath");
                
                DatabaseHelper dbHelper = new DatabaseHelper(mContext, dbpath, dbname, 1);
                if(dbHelper.checkDatabase() == false){
                    it.remove();
                }
                else{
                    dbHelper.openDatabase();
                    hm.put("recentdbtotal", "" + dbHelper.getTotalCount());
                    hm.put("recentscheduled", "" + dbHelper.getScheduledCount());
                    hm.put("recentnew", "" + dbHelper.getNewCount());
                    dbHelper.close();
                    it.set(hm);
                }
                
            }
            return recentList;
        }
        
        private void fetchListFromPref(){
            SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(mContext);
            for(int i = 0; i < MAX_LIST_NUMBER; i++){
                String dbName = settings.getString("recentdbname" + i, null);
                String dbPath = settings.getString("recentdbpath" + i, null);
                if(dbName == null || dbPath == null){ 
                    break;
                }
                HashMap<String, String> hm = new HashMap<String, String>();
                hm.put("recentdbname", dbName);
                hm.put("recentdbpath", dbPath);
                recentList.add(hm);
            }
        }
        
        public void writeNewList(String dbpath, String dbname){
            HashMap<String, String> hm = new HashMap<String, String>();
            hm.put("recentdbname", dbname);
            hm.put("recentdbpath", dbpath);
            ListIterator<HashMap<String, String>> it = recentList.listIterator();
            while(it.hasNext()){
                HashMap<String, String> h = it.next();
                
                if(h.get("recentdbname").equals(dbname)){
                    it.remove();
                }
            }
            if(recentList.size() < MAX_LIST_NUMBER){
                recentList.add(0, hm);
            }
            else{
                recentList.remove(recentList.size() - 1);
                recentList.add(0, hm);
            }
            int i = 0;
            for(HashMap<String, String> h : recentList){
                SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(mContext);
                //SharedPreferences settings = getSharedPreferences(PREFS_NAME, 0);
                SharedPreferences.Editor editor = settings.edit();
                editor.putString("recentdbname" + i, h.get("recentdbname"));
                editor.putString("recentdbpath" + i, h.get("recentdbpath"));
                editor.commit();
                i += 1;
                
            }
            
        }
        


    }

            
}
