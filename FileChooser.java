package com.example.swt369.filechooserdemo;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Paint;
import android.os.Build;
import android.os.Environment;
import android.support.annotation.NonNull;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.ScrollView;
import android.widget.TextView;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

public class FileChooser extends AppCompatActivity {

    //自定义号码，用于申请权限
    private final int CODE_PERMISSION = 0x111;
    public static final int CODE_RESULT = 0x112;

    //根目录地址
    private final String url_root = Environment.getExternalStorageDirectory()+ File.separator;

    //当前目录地址
    private String url_cur;

    //上次访问的目录地址
    private LinkedList<String> list_urlback;

    //遭回退目录地址
    private LinkedList<String> list_urlforward;

    //显示路径
    private TextView textView_path;

    //滚动面板
    private ScrollView scrollView;
    //按钮
    private Button button_back;
    private Button button_forward;
    private Button button_up;
    private Button button_exit;

    //列表
    private ListView listView_detail;

    //文件路径列表，存储当前目录下所有文件与文件夹路径
    private List<String> paths;

    //文件名列表，存储当前目录下所有文件与文件夹名
    private List<String> names;

    //文件属性列表，包括路径与名称
    private List<Map<String,String>> fileproperties;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_file_chooser);

        if(getPermission()){
            initialize();
        }

    }

    //初始化
    private void initialize(){
        //获取控件
        textView_path = new TextView(this);

        scrollView = (ScrollView)findViewById(R.id.scrollView);
        scrollView.addView(textView_path);

        button_back = (Button)findViewById(R.id.button_back);
        button_forward = (Button)findViewById(R.id.button_forward);
        button_up = (Button)findViewById(R.id.button_up);
        button_exit = (Button)findViewById(R.id.button_exit);

        listView_detail = (ListView)findViewById(R.id.listView_details);

        //当前路径初始化为根目录
        url_cur = url_root;

        //初始化文件路径列表与文件名列表
        paths = new ArrayList<>();
        names = new ArrayList<>();
        fileproperties = new ArrayList<>();
        list_urlback = new LinkedList<>();
        list_urlforward = new LinkedList<>();

        //初始化界面
        updateView();

        //设置列表项单击事件
        listView_detail.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                url_cur = paths.get(position);
                File file = new File(url_cur);
                if(file.isDirectory()){
                    list_urlforward.clear();
                    list_urlback.add(url_cur);
                    updateView();
                }else{
                    Intent intent = getIntent();
                    intent.putExtra("path",url_cur);
                    setResult(CODE_RESULT,intent);
                    finish();
                }
            }
        });

        //设置后退按钮
        button_back.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(list_urlback.isEmpty()) return;
                list_urlforward.add(url_cur);
                url_cur = list_urlback.removeLast();
                updateView();
            }
        });

        //设置前进按钮
        button_forward.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(list_urlforward.isEmpty()) return;
                list_urlback.add(url_cur);
                url_cur = list_urlforward.removeLast();
                updateView();
            }
        });

        //设置向上按钮
        button_up.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                File file = new File(url_cur);
                if(file.getParentFile().exists() && file.getParentFile().canRead()){
                    list_urlback.add(url_cur);
                    url_cur = file.getParent();
                    updateView();
                }
            }
        });

        //设置当前目录按钮
        button_exit.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = getIntent();
                intent.putExtra("path",url_cur);
                setResult(CODE_RESULT,intent);
                finish();
            }
        });
    }

    //更新界面
    private void updateView(){
        setDetails(url_cur);
        updateList();
        setTextViewPath();
    }

    //更新列表
    private void updateList() {
        listView_detail.setAdapter(new ArrayAdapter<>(FileChooser.this,R.layout.details_textview,names));
    }

    //获取权限
    private boolean getPermission(){
        if(Build.VERSION.SDK_INT < Build.VERSION_CODES.M) return true;
        int permission_res = ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE);
        if(permission_res != PackageManager.PERMISSION_GRANTED){
            requestPermissions(new String[]{Manifest.permission.READ_EXTERNAL_STORAGE},CODE_PERMISSION);
            return false;
        }else{
            return true;
        }
    }

    //确认是否正常获取权限
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if(requestCode == CODE_PERMISSION){
            if(grantResults[0] == PackageManager.PERMISSION_GRANTED){
                Log.i("permission","granted");
                initialize();
            }else{
                Intent intent = getIntent();
                intent.putExtra("path","null");
                setResult(CODE_RESULT,intent);
                finish();
            }
        }
    }

    //读入指定路径内所有文件的路径
    private void setDetails(String url){
        //清空当前文件属性列表fileproperties
        fileproperties.clear();
        //读取输入路径内文件
        File file = new File(url);
        File[] files = file.listFiles();
        //输出文件属性至文件属性列表fileproperties
        for(int i=0;i<files.length;i++){
            HashMap<String,String> map = new HashMap<>();
            map.put("path",files[i].getPath());
            map.put("name",files[i].getName());
            fileproperties.add(map);
        }
        //按字典序排序
        Collections.sort(fileproperties, new Comparator<Map<String, String>>() {
            @Override
            public int compare(Map<String, String> o1, Map<String, String> o2) {
                String o1_value = o1.get("name");
                String o2_value = o2.get("name");
                return o1_value.compareTo(o2_value);
            }
        });
        //更新路径列表与名称列表
        paths.clear();
        names.clear();
        for(int i=0;i<fileproperties.size();i++){
            paths.add(fileproperties.get(i).get("path"));
            names.add(fileproperties.get(i).get("name"));
        }
    }

    //设置路径文本框
    private void setTextViewPath(){
        textView_path.setText(url_cur);
    }
}
