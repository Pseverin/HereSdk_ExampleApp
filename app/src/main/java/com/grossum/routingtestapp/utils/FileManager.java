package com.grossum.routingtestapp.utils;

import android.content.Context;
import android.support.v4.util.Pair;
import android.util.Log;

import com.annimon.stream.Stream;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * @author Severyn Parkhomenko <sparkhomenko@grossum.com>
 * @copyright (c) Grossum. (http://www.grossum.com)
 * @project RoutingTestApp
 */
public class FileManager {

    private final static List<Pair<String, String>> ROUTE_PLAN = new ArrayList<Pair<String, String>>() {{
        add(new Pair<>("50.413115", "30.533302"));
        add(new Pair<>("50.412414", "30.531000"));
        add(new Pair<>("50.411460", "30.527750"));
        add(new Pair<>("50.412533", "30.525765"));
        add(new Pair<>("50.414773", "30.524413"));
        add(new Pair<>("50.416301", "30.523469"));
        add(new Pair<>("50.416690", "30.521575"));
        add(new Pair<>("50.416191", "30.519574"));
        add(new Pair<>("50.418040", "30.517418"));
    }};

    public static String creteFakeGPSRoute(Context context) {
        Calendar calendar = new GregorianCalendar();

        StringBuilder builder = new StringBuilder();

        String header = "<gpx>\n" +
            "  <metadata>\n" +
            "   <name>london</name>\n" +
            "   <time>2017-01-19T17:41:11Z</time>\n" +
            "  </metadata>\n" +
            "<trk>\n" +
            "  <name>test</name>\n" +
            "<trkseg>\n";

        String footer = "</trkseg>\n" +
            "</trk>\n" +
            "</gpx>";

        builder.append(header);
        Stream.of(ROUTE_PLAN).forEach(stringStringPair -> {

            String lat = stringStringPair.first;
            String lng = stringStringPair.second;
            calendar.setTime(new Date(calendar.getTime().getTime() + TimeUnit.SECONDS.toMillis(1)));

            String date = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss").format(calendar.getTime()) + "Z";
            builder.append(String.format(" <trkpt lat=\"%1$s\" lon=\"%2$s\">\n" +
                "    <ele>8.0000000</ele>\n" +
                "    <time>%3$s</time>\n" +
                "    <hdop>33</hdop></trkpt>\n", lat, lng, date));
        });
        builder.append(footer);
        Log.i("TAG", builder.toString());
        return saveFileToExternal(builder.toString(), "fake_gps.txt", context);
    }

    public static String saveFileToExternal(String fileText, String fileName, Context context) {
        if (!PermissionsUtils.checkReadWriteExtStoragePermissionGranted(context)) {
            return null;
        }
        try {
            File file = new File(context.getExternalFilesDir(null)
                + File.separator +
                fileName);

            InputStream inputStream = null;
            OutputStream outputStream = null;

            try {
                file.getParentFile().mkdirs();

                byte[] fileReader = new byte[1024];

                long fileSize = fileText.getBytes().length;

                long fileSizeDownloaded = 0;

                inputStream = new ByteArrayInputStream(fileText.getBytes());
                outputStream = new FileOutputStream(file);

                while (true) {
                    int read = inputStream.read(fileReader);

                    if (read == -1) {
                        break;
                    }

                    outputStream.write(fileReader, 0, read);

                    fileSizeDownloaded += read;
                }

                outputStream.flush();


                return file.getAbsolutePath();
            } catch (IOException e) {
                e.printStackTrace();
                return null;
            } finally {
                if (inputStream != null) {
                    inputStream.close();
                }

                if (outputStream != null) {
                    outputStream.close();
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

}
