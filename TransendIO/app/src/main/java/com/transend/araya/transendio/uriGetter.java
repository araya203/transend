package com.transend.araya.transendio;

import android.annotation.SuppressLint;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.Context;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.provider.DocumentsContract;
import android.provider.MediaStore;
import android.util.Log;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.StringWriter;
import java.net.URLConnection;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;

import org.apache.commons.io.FileUtils;
import org.apache.tika.Tika;

import javax.activation.MimetypesFileTypeMap;


public class uriGetter {
    private static String getUriRealPathAboveKitkat(Context ctx, Uri uri)
    {

        String ret = "";
        Log.d("IS SAMSUNG?", Boolean.toString(isSamsungPhoto(uri.toString())));
//        Log.d("URI to string:", uri.toString());
//        Log.d("URI getPath:", uri.getPath());
//        Log.d("URI getAuthority:", uri.getAuthority());
//        Log.d("URI to getPathSegments:", uri.getPathSegments().toString());
//        Log.d("getEncodedAuthority:", uri.getEncodedAuthority());
//        Log.d("getEncodedFragment:", uri.getEncodedFragment());
//        Log.d("URI to getEncodedPath:", uri.getEncodedPath());
//        Log.d("URI to getEncodedQuery:", uri.getEncodedQuery());
//        Log.d("getEncodedSchemeSpecif:", uri.getEncodedSchemeSpecificPart());
//        Log.d("URI getEncodedUserInfo:", uri.getEncodedUserInfo());
//        Log.d("URI to getFragment:", uri.getFragment());
//        Log.d("URI to getHost:", uri.getHost());
//        Log.d("URI getLastPathSegment:", uri.getLastPathSegment());
//        Log.d("URI to getQuery:", uri.getQuery());
//        Log.d("URI to getScheme:", uri.getScheme());
//        Log.d("getQueryParameterNames:", uri.getQueryParameterNames().toString());



        if(ctx != null && uri != null) {
            Log.d("URI:",uri.getAuthority());
            Log.d("SCHEME:",uri.getScheme());

            try {
                if(isContentUri(uri))
                {
                    if(isGooglePhotoDoc(uri.getAuthority()))
                    {
                        ret = uri.getLastPathSegment();
                    }
                    else if(isGoogleDriveDoc(uri.getAuthority())) {
                        Cursor cursor = null;
                        final String column = "_display_name";
                        final String[] projection = {
                                column
                        };

                        try {
                            cursor = ctx.getContentResolver().query(uri, projection, null, null, null);
                            Log.d("GOOGLE DRIVE NAME: ", "HERE");
                            if (cursor != null && cursor.moveToFirst()) {
                                final int column_index = cursor.getColumnIndexOrThrow(column);
                                ret = makeFileFromContentURI(ctx, uri, cursor.getString(column_index));
                                Log.d("GOOGLE DRIVE NAME: ", ret);

                            }
                        } finally {
                            if (cursor != null)
                                cursor.close();
                        }
                    }
                    else {
                        ret = getImageRealPath(ctx.getContentResolver(), uri, null);
                    }
                }else if(isFileUri(uri)) {
                    ret = uri.getPath();
                }
                else if(isDocumentUri(ctx, uri)) {

                    // Get uri related document id.
                    @SuppressLint("NewApi")
                    String documentId = DocumentsContract.getDocumentId(uri);

                    // Get uri authority.
                    String uriAuthority = uri.getAuthority();

                    if (isMediaDoc(uriAuthority)) {
                        String idArr[] = documentId.split(":");
                        if (idArr.length == 2) {
                            // First item is document type.
                            String docType = idArr[0];

                            // Second item is document real id.
                            String realDocId = idArr[1];

                            // Get content uri by document type.
                            Uri mediaContentUri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI;
                            if ("image".equals(docType)) {
                                mediaContentUri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI;
                            } else if ("video".equals(docType)) {
                                mediaContentUri = MediaStore.Video.Media.EXTERNAL_CONTENT_URI;
                            } else if ("audio".equals(docType)) {
                                mediaContentUri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI;
                            }

                            // Get where clause with real document id.
                            String whereClause = MediaStore.Images.Media._ID + " = " + realDocId;

                            ret = getImageRealPath(ctx.getContentResolver(), mediaContentUri, whereClause);
                        }

                    } else if (isDownloadDoc(uriAuthority)) {
                        // Build download uri.
                        Uri downloadUri = Uri.parse("content://downloads/public_downloads");

                        // Append download document id at uri end.
                        Uri downloadUriAppendId = ContentUris.withAppendedId(downloadUri, Long.valueOf(documentId));

                        ret = getImageRealPath(ctx.getContentResolver(), downloadUriAppendId, null);

                    } else if (isExternalStoreDoc(uriAuthority)) {
                        String idArr[] = documentId.split(":");
                        if (idArr.length == 2) {
                            String type = idArr[0];
                            String realDocId = idArr[1];

                            if ("primary".equalsIgnoreCase(type)) {
                                ret = Environment.getExternalStorageDirectory() + "/" + realDocId;
                            }
                        }
                    }
                }
            }
            catch (IllegalStateException e) {
                ret = makeFileFromContentURI(ctx, uri, null);
            }
        }
        return ret;
    }

    /* Check whether current android os version is bigger than kitkat or not. */
    private static boolean isAboveKitKat()
    {
        boolean ret = false;
        ret = Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT;
        return ret;
    }

    private static String makeFileFromContentURI(Context ctx, Uri uri, String filename){
        String ret = "";
        try {
            InputStream is = ctx.getContentResolver().openInputStream(Uri.parse(uri.toString()));

            ByteArrayOutputStream baos = new ByteArrayOutputStream();

            int byteToBeRead = -1;
            while ((byteToBeRead = is.read()) != -1) {
                baos.write(byteToBeRead);
            }
            byte[] mybytearray = baos.toByteArray();
            String newfileName = filename;
            if (filename == null) {
                String contentType = new Tika().detect(mybytearray);
                String ext = "." + MimeTypes.lookupExt(contentType);
                Log.d("MIMETYPE:", contentType);
                newfileName = "FILE_" +
                        new SimpleDateFormat("yyyyMMddHHmmss").format(new Date()) +
                        ext;
            }
            File f = new File(ctx.getFilesDir().toString() + "/" +newfileName);

            FileUtils.writeByteArrayToFile(f, mybytearray);
            Log.d("IMAGEFILEPATH: ", f.getAbsolutePath());
            ret = f.getAbsolutePath();
        }
        catch (IOException e) {
            e.printStackTrace();
        }
        return ret;
    }

    /* Check whether this uri represent a document or not. */
    @SuppressLint("NewApi")
    private static boolean isDocumentUri(Context ctx, Uri uri)
    {
        boolean ret = false;
        if(ctx != null && uri != null) {
            ret = DocumentsContract.isDocumentUri(ctx, uri);
        }
        return ret;
    }

    /* Check whether this uri is a content uri or not.
     *  content uri like content://media/external/images/media/1302716
     *  */
    private static boolean isContentUri(Uri uri)
    {
        boolean ret = false;
        if(uri != null) {
            String uriSchema = uri.getScheme();
            if("content".equalsIgnoreCase(uriSchema))
            {
                ret = true;
            }
        }
        return ret;
    }

    /* Check whether this uri is a file uri or not.
     *  file uri like file:///storage/41B7-12F1/DCIM/Camera/IMG_20180211_095139.jpg
     * */
    private static boolean isFileUri(Uri uri)
    {
        boolean ret = false;
        if(uri != null) {
            String uriSchema = uri.getScheme();
            if("file".equalsIgnoreCase(uriSchema))
            {
                ret = true;
            }
        }
        return ret;
    }


    /* Check whether this document is provided by ExternalStorageProvider. */
    private static boolean isExternalStoreDoc(String uriAuthority)
    {
        boolean ret = false;

        if("com.android.externalstorage.documents".equals(uriAuthority))
        {
            ret = true;
        }

        return ret;
    }

    private static boolean isGoogleDriveDoc(String uriAuthority)
    {
        boolean ret = false;

        if("com.google.android.apps.docs.storage.legacy".equals(uriAuthority))
        {
            ret = true;
        }

        return ret;
    }

    /* Check whether this document is provided by DownloadsProvider. */
    private static boolean isDownloadDoc(String uriAuthority)
    {
        boolean ret = false;

        if("com.android.providers.downloads.documents".equals(uriAuthority))
        {
            ret = true;
        }

        return ret;
    }

    /* Check whether this document is provided by MediaProvider. */
    private static boolean isMediaDoc(String uriAuthority)
    {
        boolean ret = false;

        if("com.android.providers.media.documents".equals(uriAuthority))
        {
            ret = true;
        }

        return ret;
    }

    /* Check whether this document is provided by google photos. */
    private static boolean isGooglePhotoDoc(String uriAuthority)
    {
        boolean ret = false;

        if("com.google.android.apps.photos.content".equals(uriAuthority))
        {
            ret = true;
        }

        return ret;
    }

    private static boolean isWhatsappPhotoDoc(String uri)
    {
        boolean ret = false;

        if(uri.contains("com.whatsapp.provider.media"))
        {
            ret = true;
        }

        return ret;
    }

    private static boolean isSamsungPhoto(String uri)
    {
        boolean ret = false;

        if(uri.contains("android.gallery3d.provider"))
        {
            ret = true;
        }

        return ret;
    }

    /* Return uri represented document file real local path.*/
    private static String getImageRealPath(ContentResolver contentResolver, Uri uri, String whereClause)
    {
        String ret = "";

        // Query the uri with condition.
        Cursor cursor = contentResolver.query(uri, null, whereClause, null, null);

        if(cursor!=null)
        {
            boolean moveToFirst = cursor.moveToFirst();
            if(moveToFirst)
            {

                // Get columns name by uri type.
                String columnName = MediaStore.Images.Media.DATA;

                if( uri==MediaStore.Images.Media.EXTERNAL_CONTENT_URI )
                {
                    columnName = MediaStore.Images.Media.DATA;
                }else if( uri==MediaStore.Audio.Media.EXTERNAL_CONTENT_URI )
                {
                    columnName = MediaStore.Audio.Media.DATA;
                }else if( uri==MediaStore.Video.Media.EXTERNAL_CONTENT_URI )
                {
                    columnName = MediaStore.Video.Media.DATA;
                }

                // Get column index.
                int imageColumnIndex = cursor.getColumnIndex(columnName);

                // Get column value which is the uri related file local path.
                ret = cursor.getString(imageColumnIndex);
            }
        }

        return ret;
    }

    public static String getUriRealPath(Context ctx, Uri uri)
    {
        Log.d("REALURI", uri.toString());

        String ret = "";

        if( isAboveKitKat() )
        {
            // Android OS above sdk version 19.
            ret = getUriRealPathAboveKitkat(ctx, uri);
        }else
        {
            // Android OS below sdk version 19
            ret = getImageRealPath(ctx.getContentResolver(), uri, null);
        }

        return ret;
    }


    public static ArrayList<String> getInfoFromURIArray(Context ctx, ArrayList<Uri> Uris) {
        ArrayList<String> results = new ArrayList<>();
        for (int i=0; i<Uris.size(); i++) {
//            Log.d("Multiple SHared:", getRealPathFromURI(Uris.get(i)));
            results.add(getUriRealPath(ctx, Uris.get(i)));
        }
        Log.d("ARRAY FROM URIARRAY:", results.toString());

        return results;
    }
}
