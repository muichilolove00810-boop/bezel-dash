package com.grara.bezeldash;

import android.content.Context;

import com.grara.bezeldash.game.Level;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/** 레벨 저장/불러오기 (버전 포함 JSON, 앱 내부 저장소). */
public final class LevelIO {
    private LevelIO() {
    }

    public static final class Entry {
        public final String id;
        public final Level level;

        Entry(String id, Level level) {
            this.id = id;
            this.level = level;
        }
    }

    private static File dir(Context c) {
        File d = new File(c.getFilesDir(), "levels");
        d.mkdirs();
        return d;
    }

    public static String toJson(Level l) throws Exception {
        JSONObject o = new JSONObject();
        o.put("v", Level.FORMAT_VERSION);
        o.put("name", l.name);
        o.put("style", l.style);
        o.put("bpm", l.bpm);
        o.put("seed", l.seed);
        o.put("mode", l.startMode);
        o.put("speed", l.startSpeed);
        o.put("bg", l.bg);
        o.put("size", l.startSize);
        JSONArray arr = new JSONArray();
        for (Level.Obj ob : l.objs) {
            JSONArray a = new JSONArray();
            a.put(ob.type).put(ob.x).put(ob.y).put(ob.rot).put(ob.group).put(ob.hidden ? 1 : 0);
            JSONArray ps = new JSONArray();
            for (int v : ob.p) ps.put(v);
            a.put(ps);
            arr.put(a);
        }
        o.put("objs", arr);
        return o.toString();
    }

    public static Level fromJson(String s) throws Exception {
        JSONObject o = new JSONObject(s);
        Level l = new Level();
        l.name = o.optString("name", "레벨");
        l.style = o.optInt("style", 0);
        l.bpm = o.optInt("bpm", 124);
        l.seed = o.optInt("seed", 1);
        l.startMode = o.optInt("mode", 0);
        l.startSpeed = o.optInt("speed", 1);
        l.bg = o.optInt("bg", 0);
        l.startSize = o.optInt("size", 1);
        JSONArray arr = o.optJSONArray("objs");
        if (arr != null) {
            for (int i = 0; i < arr.length(); i++) {
                JSONArray a = arr.getJSONArray(i);
                Level.Obj ob = new Level.Obj(a.getInt(0), a.getInt(1), a.getInt(2));
                ob.rot = a.optInt(3);
                ob.group = a.optInt(4);
                ob.hidden = a.optInt(5) == 1;
                JSONArray ps = a.optJSONArray(6);
                if (ps != null) for (int k = 0; k < ps.length() && k < ob.p.length; k++) ob.p[k] = ps.getInt(k);
                l.objs.add(ob);
            }
        }
        return l;
    }

    public static void save(Context c, String id, Level l) {
        try (FileOutputStream out = new FileOutputStream(new File(dir(c), id + ".json"))) {
            out.write(toJson(l).getBytes(StandardCharsets.UTF_8));
            // 자동 백업 (직전 버전)
        } catch (Exception ignored) {
        }
    }

    public static List<Entry> list(Context c) {
        ArrayList<Entry> out = new ArrayList<>();
        File[] fs = dir(c).listFiles((d, n) -> n.endsWith(".json"));
        if (fs == null) return out;
        Arrays.sort(fs, (a, b) -> Long.compare(b.lastModified(), a.lastModified()));
        for (File f : fs) {
            try (InputStream in = new FileInputStream(f)) {
                byte[] b = new byte[(int) f.length()];
                int n = 0;
                while (n < b.length) {
                    int r = in.read(b, n, b.length - n);
                    if (r < 0) break;
                    n += r;
                }
                String id = f.getName().substring(0, f.getName().length() - 5);
                out.add(new Entry(id, fromJson(new String(b, 0, n, StandardCharsets.UTF_8))));
            } catch (Exception ignored) {
            }
        }
        return out;
    }

    public static void delete(Context c, String id) {
        new File(dir(c), id + ".json").delete();
    }

    public static String newId() {
        return "L" + System.currentTimeMillis();
    }
}
