package com.example.multithreadfiledowm;

import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.RandomAccessFile;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import androidx.annotation.Nullable;

public class FileDownActivity extends Activity {
    private Context mContext;
    private Button button_down;
    private ProgressBar progressBar;
    //下载完成进程计数
    private int threadCount;
    //当前下载进度
    private int currentProcess=0;
    //总的下载进度大小
    private int totalProcess;
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.lay_filedown);
        mContext=FileDownActivity.this;
        button_down=findViewById(R.id.btn_down);
        progressBar=findViewById(R.id.pb1);
        button_down.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                new Thread(){
                    @Override
                    public void run() {
                        fileDownload();
                    }
                }.start();
            }
        });
    }

    //下载文件
    private void fileDownload(){
        int threadNum=3;
        threadCount=0;
        String urlStr="http://download.kugou.com/download/kugou_pc";
        try {
            URL url = new URL(urlStr);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            if (connection.getResponseCode() == 200) {
                //获取文件大小
                int fileLength = connection.getContentLength();
                totalProcess=fileLength;
                //文件File
                File file = new File(getFilesDir(), "kugou.exe");
                //通过RandomAccessFile创建带内容空间文件
                RandomAccessFile randomAccessFile = new RandomAccessFile(file, "rw");
                randomAccessFile.setLength(fileLength);
                //每个线程下载大小
                int eachSize = fileLength / threadNum;
                for (int i = 0; i < threadNum; i++) {
                    int startPosition = i * eachSize;
                    int endPosition = (i + 1) * eachSize - 1;
                    if (i == threadNum - 1) {
                        endPosition = fileLength - 1;
                    }
                    //开启下载线程
                    new DownloadThread(i, startPosition, endPosition, urlStr, randomAccessFile,threadNum).start();
                }
            }
        }catch (Exception e){
            e.printStackTrace();
        }
    }

    //文件下载类
    private class DownloadThread extends  Thread{
        //進程id
        private int threadId;
        //開始位置
        private int startPosition;
        //结束位置
        private int endPosition;
        //url
        private String urlStr;
        //file
        private RandomAccessFile file;
        //进程数
        private int threadNum;

        public DownloadThread(int threadId, int startPosition,
                              int endPosition,String url,
                              RandomAccessFile randomAccessFile,
                              int threadNum){
            this.threadId=threadId;
            this.startPosition=startPosition;
            this.endPosition=endPosition;
            this.urlStr=url;
            this.file=randomAccessFile;
            this.threadNum=threadNum;
        }

        @Override
        public void run() {
            try {
                //判断当前线程记录文件是否存在
                File recordFile=new File(getFilesDir(),"kugou"+threadId);
                if(recordFile.exists()&&recordFile.length()>0){
                    FileInputStream fileInputStream=new FileInputStream(recordFile);
                    BufferedReader reader=new BufferedReader(new InputStreamReader(fileInputStream));
                    startPosition=Integer.parseInt(reader.readLine());
                }
                //创建URL
                URL url=new URL(urlStr);
                HttpURLConnection connection= (HttpURLConnection) url.openConnection();
                //设置下载位置
                connection.setRequestProperty("Range","bytes="+startPosition+"-"+endPosition);
                //获取部分文件成功，206
                if (connection.getResponseCode()==206){
                    //获取输入流
                    InputStream is=connection.getInputStream();
                    File file = new File(getFilesDir(), "kugou.exe");
                    //通过RandomAccessFile创建带内容空间文件
                    RandomAccessFile randomAccessFile = new RandomAccessFile(file, "rw");
                    //文件读写位置
                    randomAccessFile.skipBytes(startPosition);
                    //创建输入流
                    int count;
                    //每个线程进度
                    int perCount=startPosition;
                    byte[] buffer = new byte[1024];
                    while ((count=is.read(buffer))>0){
                        randomAccessFile.write(buffer,0,count);

                        //单个线程下载进度
                        perCount+= count;
                        File file1 = new File(getFilesDir(),"kugou"+threadId);
                        //mode:''rwd'实时写入进度数据,保存进度文件
                        //当应用关闭时，再次下载从断点处下载
                        RandomAccessFile randomAccessFile1=new RandomAccessFile(file1,"rwd");
                        randomAccessFile1.write((perCount+"").getBytes());

                        //总体下载进度
                        currentProcess +=count;
                        File file2 = new File(getFilesDir(),"kugouProcess");
                        //mode:''rwd'实时写入进度数据,保存进度文件
                        //当应用关闭时，再次下载从断点处下载
                        RandomAccessFile randomAccessFile2=new RandomAccessFile(file2,"rwd");
                        randomAccessFile2.write((currentProcess+"").getBytes());

                        //更新UI进度条
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                progressBar.setMax(totalProcess);
                                progressBar.setProgress(currentProcess);
                            }
                        });
                    }
                    randomAccessFile.close();
                }
            } catch (Exception e) {
                e.printStackTrace();
            }finally {
                threadCount++;
                if (threadCount==threadNum){
                    Log.i("tag","下载完成！");
                }else {
                    Log.i("tag","已完成："+threadCount+",当前下载完成进程："+threadId);
                }
            }
        }
    }
}
