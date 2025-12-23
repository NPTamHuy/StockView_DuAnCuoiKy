package com.example.stockview;

import android.content.Context;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStreamReader;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

public class FileFL {
    private static final String FILE_NAME = "watchlist.json";

    public static void saveWatchlist(Context context, List<String> symbols) {
        Gson gson = new Gson();
        String jsonString = gson.toJson(symbols);
        try (FileOutputStream fos = context.openFileOutput(FILE_NAME, Context.MODE_PRIVATE)) {
            fos.write(jsonString.getBytes());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static List<String> loadWatchlist(Context context) {
        try (FileInputStream fis = context.openFileInput(FILE_NAME);
             InputStreamReader isr = new InputStreamReader(fis)) {
            Gson gson = new Gson();
            Type type = new TypeToken<ArrayList<String>>() {}.getType();
            return gson.fromJson(isr, type);
        } catch (Exception e) {
            return new ArrayList<>();
        }
    }
}