package com.stradtman.newsreader;

import androidx.appcompat.app.AppCompatActivity;

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteStatement;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.widget.ArrayAdapter;
import android.widget.ListView;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;

public class MainActivity extends AppCompatActivity {
    private static final String TAG = "MainActivity";
    ArrayList<String> titles = new ArrayList<String>();
    ArrayList<String> content = new ArrayList<>();
    ArrayAdapter arrayAdapter;
    SQLiteDatabase articleDB;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        ListView listView = (ListView) findViewById(R.id.listView);
        arrayAdapter = new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1, titles);
        listView.setAdapter(arrayAdapter);
        articleDB = this.openOrCreateDatabase("Articles", MODE_PRIVATE, null);
        articleDB.execSQL("CREATE TABLE IF NOT EXISTS articles (id INTEGER PRIMARY KEY, articleId INTEGER, title VARCHAR, content VARCHAR)");
        DownloadTask task = new DownloadTask();
        try {
            task.execute("https://hacker-news.firebaseio.com/v0/topstories.json?print=pretty");
        } catch (Exception e) {
            e.printStackTrace();
        }
        updateListView();
    }

    public void updateListView() {
        Cursor cursor = articleDB.rawQuery("SELECT * FROM articles", null);
        int contentIndex = cursor.getColumnIndex("content");
        int titleIndex = cursor.getColumnIndex("title");
        if(cursor.moveToFirst()) {
            titles.clear();
            content.clear();
            do {
                titles.add(cursor.getString(titleIndex));
                content.add(cursor.getString(contentIndex));
            } while(cursor.moveToNext());
        }
    }

    public class DownloadTask extends AsyncTask<String, Void, String> {
        @Override
        protected String doInBackground(String... strings) {
            String result = "";
            URL url;
            HttpURLConnection urlConnection = null;
            try {
                url = new URL(strings[0]);
                urlConnection = (HttpURLConnection) url.openConnection();
                InputStream in = urlConnection.getInputStream();
                InputStreamReader reader = new InputStreamReader(in);
                int data = reader.read();
                while(data != -1) {
                    char current = (char) data;
                    result += current;
                    data = reader.read();
                }
                Log.i(TAG, "doInBackground: URLContent: " + result);
                JSONArray jsonArray = new JSONArray(result);
                int numberOfItems = 20;
                if(jsonArray.length() < 20) {
                    numberOfItems = jsonArray.length();
                }
                for(int i = 0; i < jsonArray.length(); i++) {
                    String articleId = jsonArray.getString(i);
                    url = new URL("https://hacker-news.firebaseio.com/v0/item/" + articleId + ".json?print=pretty");
                    urlConnection = (HttpURLConnection) url.openConnection();
                    in = urlConnection.getInputStream();
                    reader = new InputStreamReader(in);
                    data = reader.read();
                    String articleContent = "";
                    String articleTitle = "";
                    while(data != -1) {
                        char current = (char) data;
                        articleContent += current;
                        data = reader.read();
                    }
                    String sql = "INSERT INTO articles (articleId, title, content) VALUES (?, ?, ?)";
                    SQLiteStatement statement = articleDB.compileStatement(sql);
                    statement.bindString(1, articleId);
                    statement.bindString(2, articleTitle);
                    statement.bindString(3, articleContent);
                    statement.execute();
                }
            } catch (MalformedURLException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            } catch (JSONException e) {
                e.printStackTrace();
            }
            return null;
        }

        @Override
        protected void onPostExecute(String s) {
            super.onPostExecute(s);
            updateListView();
        }
    }
}
