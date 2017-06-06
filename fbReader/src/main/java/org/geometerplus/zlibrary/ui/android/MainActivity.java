package org.geometerplus.zlibrary.ui.android;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.View;
import android.widget.Button;

import org.geometerplus.android.fbreader.FBReader;
import org.geometerplus.android.fbreader.libraryService.BookCollectionShadow;
import org.geometerplus.fbreader.book.Book;

import java.io.File;

/**
 * Created by stevefat on 2016/12/20.
 */

public class MainActivity extends Activity {
    public Button bthtml;
    public Button bteup;
    public Button btn;
    private BookCollectionShadow bs;


    private Context context;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activty_main);
        context = MainActivity.this;

        bs =new BookCollectionShadow();
        bs.bindToService(context, null);
        bthtml = (Button) findViewById(R.id.bthtml);
        bteup = (Button) findViewById(R.id.bteup);
        btn= (Button) findViewById(R.id.btn);

        bthtml.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                File file = new File(Environment.getExternalStorageDirectory().getPath() + "/demo/44.html");
                if (file.exists()) {
                    openFile("html", file + "");
                }

            }
        });

        bteup.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                File file = new File(Environment.getExternalStorageDirectory().getPath() + "/DownLoad/王.epub");
                if (file.exists()) {
                    openFile("epub", file + "");
                }
            }
        });


        btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(MainActivity.this,FBReader.class));
            }
        });

    }

    public void openFile(String type, String filePath) {
        Log.i("GOV", "图书类型-----" + type);
        Book book = bs.getBookByFile(filePath);

        if (type.equalsIgnoreCase("epub")) {
            System.out.println("本地图书路径：" + filePath);
            FBReader.openBookActivity(context, book, null);
        }
        if (type.equalsIgnoreCase("html")) {
            System.out.println("本地图书路径：" + filePath);
            FBReader.openBookActivity(context, book, null);
        }
    }
}

