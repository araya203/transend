package com.transend.araya.transendio;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

public class FileName {
    public static String filePath;

    public static String getFilePath() {
        return filePath;
    }

    public static void setFilePath(String filepath) {
        filePath = filepath;
    }


    public static void tryFilePathFromAction(Context ctx, Intent intent) {
        String action = intent.getAction();
        String type = intent.getType();
        if (Intent.ACTION_SEND.equals(action) && type != null) {
            setFilePath(handleSendFile(ctx, intent));
        }
        else if (Intent.ACTION_SEND_MULTIPLE.equals(action) && type != null) {
            setFilePath(handleSendMultipleFiles(ctx, intent)); // Handle multiple images being sent
        }
    }

    private static String handleSendFile(Context ctx, Intent intent) {
        String uri = intent.getParcelableExtra(Intent.EXTRA_STREAM).toString();

        Uri fileUri = (Uri) intent.getParcelableExtra(Intent.EXTRA_STREAM);

        if (uri != null) {

            String fileName =  uriGetter.getUriRealPath(ctx, fileUri);
//            Log.d("SHAREDFILE2:", fileName);
            return fileName;
        }
        return null;
    }

    private static String handleSendMultipleFiles(Context ctx, Intent intent) {
        ArrayList<Uri> fileUris = intent.getParcelableArrayListExtra(Intent.EXTRA_STREAM);
        if (fileUris != null) {
            zip(uriGetter.getInfoFromURIArray(ctx, fileUris), ctx.getFilesDir().getPath() +"MyZip.zip");
            String fileName = ctx.getFilesDir().getPath()+"MyZip.zip";
//            Log.d("Filename:", fileName);
            return fileName;
        }
        return null;
    }

    public static void zip(ArrayList<String> _files, String zipFileName) {
//        Log.d("ARRAY FOR ZIP:", _files.toString());
        try {
            File zipFile=new File( zipFileName );
            FileOutputStream fos = new FileOutputStream(zipFile);
            ZipOutputStream zos = new ZipOutputStream(fos);
            for( int i = 0; i < _files.size(); i++ ) {
                String file = _files.get(i);
                if (file != null) {
                    byte[] buffer = new byte[1024];
                    ZipEntry ze = new ZipEntry(file.substring(file.lastIndexOf("/") + 1));
                    zos.putNextEntry(ze);
                    FileInputStream in = new FileInputStream(file);

                    int len;
                    while ((len = in.read(buffer)) > 0) {
                        zos.write(buffer, 0, len);
                    }

                    in.close();
                    zos.closeEntry();
                }
            }
            zos.close();
            fos.close();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}