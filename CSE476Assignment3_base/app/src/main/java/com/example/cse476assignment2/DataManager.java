package com.example.cse476assignment2;

import android.content.Context;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.List;

public class DataManager {

    private static final String FILENAME = "posts.dat";

    public static void savePosts(Context context, List<Post> posts) {
        File file = new File(context.getFilesDir(), FILENAME);
        try (FileOutputStream fos = new FileOutputStream(file);
             ObjectOutputStream oos = new ObjectOutputStream(fos)) {
            oos.writeObject(posts);
        } catch (Exception e) {
            e.printStackTrace(); // Log error
        }
    }

    @SuppressWarnings("unchecked")
    public static List<Post> loadPosts(Context context) {
        File file = new File(context.getFilesDir(), FILENAME);
        if (!file.exists()) {
            return new ArrayList<>(); // Return empty list if no file exists
        }
        try (FileInputStream fis = new FileInputStream(file);
             ObjectInputStream ois = new ObjectInputStream(fis)) {
            return (List<Post>) ois.readObject();
        } catch (Exception e) {
            e.printStackTrace(); // Log error
            return new ArrayList<>(); // Return empty list on error
        }
    }
}