package com.example.quicknotes.model;

import android.content.Context;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.lang.reflect.Type;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class KeywordTagDictionary {
    private static final String FILE_NAME = "keyword_tags.json";



    /**
     * Loads the tag map from a JSON file.
     * @param ctx the context of the application
     * @return a map of keywords to tags, or an empty map if the file does not exist
     */
    public static Map<String, String> loadTagMap(Context ctx) {
        try (InputStream is = ctx.getAssets().open(FILE_NAME);
             Reader reader = new InputStreamReader(is)) {

            Type listType = new TypeToken<List<KeywordTag>>(){}.getType();
            List<KeywordTag> list = new Gson().fromJson(reader, listType);

            Map<String,String> map = new HashMap<>();
            if (list != null) {
                for (KeywordTag kt : list) {
                    if (kt.keyword != null && kt.tag != null) {
                        map.put(kt.keyword.toLowerCase(), kt.tag);
                    }
                }
            }
            return map;

        } catch (Exception e) {
            e.printStackTrace();
            return Collections.emptyMap();
        }
    }

    private static class KeywordTag {
        String keyword;
        String tag;
    }


}
