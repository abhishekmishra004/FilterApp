package com.application.databind.util;

import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.os.Environment;
import android.provider.MediaStore;
import android.util.Log;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;

public class FileUtils {

    public static String getPath(Context context, Uri uri) {
        Cursor cursor = context.getContentResolver().query(uri, null, null, null, null);
        cursor.moveToFirst();
        String document_id = cursor.getString(0);
        document_id = document_id.substring(document_id.lastIndexOf(":") + 1);
        cursor.close();

        cursor = context.getContentResolver().query(
                android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                null, MediaStore.Images.Media._ID + " = ? ", new String[]{document_id}, null);
        cursor.moveToFirst();
        String path = cursor.getString(cursor.getColumnIndex(MediaStore.Images.Media.DATA));
        cursor.close();

        return path;
    }

    public static void moveFile(String inputPath, String inputFile,boolean toDelete) {

        InputStream in = null;
        OutputStream out = null;
        try {


            File dir = new File(Environment.getExternalStorageDirectory()+"/"+Environment.DIRECTORY_PICTURES+"/Images");
            if (!dir.exists())
            {
                dir.mkdirs();
            }
            File file = new File(dir, inputFile);
            file.createNewFile();
            in = new FileInputStream(inputPath );
            out = new FileOutputStream(file.getAbsolutePath());

            byte[] buffer = new byte[1024];
            int read;
            while ((read = in.read(buffer)) != -1) {
                out.write(buffer, 0, read);
            }
            in.close();
            in = null;
            out.flush();
            out.close();
            out = null;

            if(toDelete)
                new File(inputPath).delete();


        } catch (Exception fnfe1) {
            Log.e("tag", fnfe1.getMessage());
        }

    }
}
