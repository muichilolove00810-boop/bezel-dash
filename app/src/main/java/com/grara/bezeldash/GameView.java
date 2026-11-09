package com.grara.bezeldash;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffColorFilter;
import android.graphics.RadialGradient;
import android.graphics.RectF;
import android.graphics.Shader;
import android.graphics.Typeface;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.view.MotionEvent;
import android.view.View;

import com.grara.bezeldash.audio.AudioPlayer;
import com.grara.bezeldash.audio.MapCode;
import com.grara.bezeldash.audio.Song;
import com.grara.bezeldash.audio.Updater;
import com.grara.bezeldash.game.Defs;
import com.grara.bezeldash.game.Engine;
import com.grara.bezeldash.game.Level;
import com.grara.bezeldash.game.OfficialLevels;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Random;

/** 베젤 대시: 메뉴 / 플레이 / 에디터를 모두 그리는 단일 뷰. */
public class GameView extends View {

    // ---------------- 화면 ----------------
    private static final int S_MENU = 0, S_LOADING = 1, S_PLAY = 2, S_RESULT = 3, S_EDIT = 4;

    private final Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint stroke = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint text = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final RectF r = new RectF();
    private final Path path = new Path();
    private final Matrix mtx = new Matrix();
    private final Random rnd = new Random();
    private final Handler main = new Handler(Looper.getMainLooper());
    private final SharedPreferences prefs;
    private final Vibrator vibrator;
    private Shader glowShader;

    Updater updater;

    private float W, H, cx, cy;
    private int screen = S_MENU;
    private boolean running;
    private long lastFrame;

    // ---------------- 메뉴 시스템 ----------------
    private interface Adj {
        String value();

        void adjust(int d);
    }

    private static final class MItem {
        final String label;
        String sub;
        final Runnable act;
        final Adj adj;
        boolean locked;

        MItem(String label, String sub, Runnable act) {
            this.label = label;
            this.sub = sub;
            this.act = act;
            this.adj = null;
        }

        MItem(String label, Adj adj) {
            this.label = label;
            this.sub = null;
            this.act = null;
            this.adj = adj;
        }
    }

    private final class Menu {
        String title;
        final ArrayList<MItem> items = new ArrayList<>();
        int sel;
        float pos;
        boolean editing;
        Runnable onBack;
        int accent = Color.rgb(61, 232, 255);

        Menu(String title) {
            this.title = title;
        }

        Menu add(MItem it) {
            items.add(it);
            return this;
        }
    }

    private Menu menu;
    private final ArrayList<Menu> menuStack = new ArrayList<>();

    // ---------------- 플레이 ----------------
    private Level level;          // 현재 플레이/편집 레벨
    private String levelId;       // 내 레벨 id (공식이면 null)
    private int officialIdx = -1;
    private Engine eng;
    private Song song;
    private AudioPlayer player;
    private boolean practice, fromEditor, paused;
    private float acc;
    private int attempts;
    private float deathT;         // 죽은 뒤 연출 시간
    private float bestThisSession;
    private final ArrayList<Engine.Snap> checkpoints = new ArrayList<>();
    private float sinceCheckpoint;
    private float camX, camY;
    private float startBannerT;
    private final ArrayList<Part> parts = new ArrayList<>();
    private final float[] trailX = new float[12], trailY = new float[12];
    private int trailN;
    private float trailT;
    private int syncMs;
    private boolean vibeOn;

    private static final class Part {
        float x, y, vx, vy, life, max, size;
        int color;
    }

    // ---------------- 에디터 ----------------
    private static final int T_PLACE = 0, T_SELECT = 1, T_DELETE = 2;
    private static final String[] TOOL_NAMES = {"배치", "선택", "삭제"};
    private int tool = T_PLACE;
    private int palIdx;
    private Level.Obj selected;
    private float edCamX = -2, edCamY = -1, edZoom = 1;
    private final ArrayList<Level> undo = new ArrayList<>();
    private boolean dirty;
    private String edMsg = "";
    private float edMsgT;

    // ---------------- 입력 ----------------
    private float downX, downY, lastX, lastY;
    private long downTime;
    private boolean moved, longFired;
    private float rotAcc;
    private long lastRot;
    private final Runnable longPress = this::onLongPress;

    // ---------------- 아이콘 ----------------
    private static final int[] ICON_COLORS = {
            0xFFFFD23F, 0xFF3DE8FF, 0xFF7CFF6B, 0xFFFF6B9E, 0xFFB57BFF, 0xFFFF8A3D, 0xFFFFFFFF, 0xFFFF3D3D
    };
    private static final String[] ICON_COLOR_NAMES = {"노랑", "하늘", "연두", "분홍", "보라", "주황", "흰색", "빨강"};
    private static final int[] COLOR_STARS = {0, 0, 0, 0, 1, 2, 3, 4};
    private static final String[] FACE_NAMES = {"기본", "졸린 눈", "윙크", "로봇", "별"};
    private static final int[] FACE_STARS = {0, 0, 2, 3, 5};
    private int col1, col2, face;
    private boolean trail;

    public GameView(Context ctx) {
        super(ctx);
        setFocusable(true);
        setFocusableInTouchMode(true);
        prefs = ctx.getSharedPreferences("dash", Context.MODE_PRIVATE);
        vibrator = ctx.getSystemService(Vibrator.class);
        syncMs = prefs.getInt("sync", 0);
        vibeOn = prefs.getBoolean("vibe", true);
        col1 = prefs.getInt("col1", 0);
        col2 = prefs.getInt("col2", 1);
        face = prefs.getInt("face", 0);
        trail = prefs.getBoolean("trail", true);
        text.setTextAlign(Paint.Align.CENTER);
        text.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));
        stroke.setStyle(Paint.Style.STROKE);
        stroke.setStrokeCap(Paint.Cap.ROUND);
        stroke.setStrokeJoin(Paint.Join.ROUND);
        openMain();
    }

    @Override
    protected void onSizeChanged(int w, int h, int ow, int oh) {
        W = w;
        H = h;
        cx = w / 2f;
        cy = h / 2f;
        glowShader = new RadialGradient(0, 0, 1, Color.WHITE, Color.TRANSPARENT, Shader.TileMode.CLAMP);
    }

    void resume() {
        running = true;
        lastFrame = 0;
        invalidate();
    }

    void pause() {
        running = false;
        if (screen == S_PLAY && !paused) openPause();
        if (screen == S_EDIT && dirty) saveLevel();
    }

    // =====================================================================
    // 메뉴들
    // =====================================================================

    private int stars() {
        int s = 0;
        for (int i = 0; i < OfficialLevels.count(); i++) if (prefs.getBoolean("done_" + i, false)) s++;
        return s;
    }

    private void showMenu(Menu m) {
        menu = m;
        screen = S_MENU;
    }

    private void pushMenu(Menu m) {
        if (menu != null && screen == S_MENU) menuStack.add(menu);
        showMenu(m);
    }

    private void popMenu() {
        if (!menuStack.isEmpty()) {
            menu = menuStack.remove(menuStack.size() - 1);
            screen = S_MENU;
        }
    }

    private void openMain() {
        menuStack.clear();
        Menu m = new Menu("BEZEL DASH");
        m.add(new MItem("플레이", "공식 레벨 " + OfficialLevels.count() + "개 · 별 " + stars(), this::openOfficial));
        m.add(new MItem("레벨 제작", "내 레벨 만들기 · 편집", this::openMine));
        m.add(new MItem("아이콘", "색 · 표정 · 궤적", this::openIcon));
        m.add(new MItem("설정", "싱크 · 진동 · 도움말", this::openSettings));
        m.onBack = null;
        showMenu(m);
    }

    private void openOfficial() {
        Menu m = new Menu("공식 레벨");
        for (int i = 0; i < OfficialLevels.count(); i++) {
            final int idx = i;
            int best = prefs.getInt("best_off_" + i, 0);
            boolean done = prefs.getBoolean("done_" + i, false);
            m.add(new MItem((done ? "★ " : "") + OfficialLevels.NAMES[i],
                    OfficialLevels.DIFF[i] + " · 최고 " + best + "%", () -> {
                officialIdx = idx;
                levelId = null;
                startLevel(OfficialLevels.get(idx), false, false);
            }));
        }
        m.onBack = this::popMenu;
        pushMenu(m);
    }

    private void openMine() {
        Menu m = new Menu("내 레벨");
        m.add(new MItem("+ 새 레벨", "빈 레벨에서 시작", () -> {
            Level l = new Level();
            l.name = "내 레벨 " + (LevelIO.list(getContext()).size() + 1);
            l.seed = rnd.nextInt(100000);
            levelId = LevelIO.newId();
            LevelIO.save(getContext(), levelId, l);
            openEditor(l);
        }));
        m.add(new MItem("+ 공식 레벨 복사", "공식 레벨을 고쳐보기", () -> {
            Menu c = new Menu("복사할 레벨");
            for (int i = 0; i < OfficialLevels.count(); i++) {
                final int idx = i;
                c.add(new MItem(OfficialLevels.NAMES[i], "편집용 복사본 만들기", () -> {
                    Level l = OfficialLevels.get(idx).copy();
                    l.name = OfficialLevels.NAMES[idx] + " 복사";
                    levelId = LevelIO.newId();
                    LevelIO.save(getContext(), levelId, l);
                    openEditor(l);
                }));
            }
            c.onBack = this::popMenu;
            pushMenu(c);
        }));
        for (LevelIO.Entry e : LevelIO.list(getContext())) {
            final LevelIO.Entry en = e;
            m.add(new MItem(e.level.name, e.level.objs.size() + "개 · 최고 " + prefs.getInt("best_" + e.id, 0) + "%",
                    () -> openLevelActions(en)));
        }
        m.onBack = this::popMenu;
        pushMenu(m);
    }

    private void openLevelActions(LevelIO.Entry en) {
        Menu m = new Menu(en.level.name);
        m.add(new MItem("플레이", null, () -> {
            levelId = en.id;
            officialIdx = -1;
            startLevel(en.level, false, false);
        }));
        m.add(new MItem("편집", null, () -> {
            levelId = en.id;
            openEditor(en.level);
        }));
        m.add(new MItem("복사", "같은 레벨을 하나 더", () -> {
            Level c = en.level.copy();
            c.name = en.level.name + " 복사";
            LevelIO.save(getContext(), LevelIO.newId(), c);
            popMenu();
            popMenu();
            openMine();
        }));
        m.add(new MItem("삭제", "탭하면 바로 삭제", () -> {
            LevelIO.delete(getContext(), en.id);
            popMenu();
            popMenu();
            openMine();
        }));
        m.onBack = this::popMenu;
        pushMenu(m);
    }

    private void openIcon() {
        final int st = stars();
        Menu m = new Menu("아이콘");
        m.add(new MItem("기본 색", new Adj() {
            public String value() {
                return ICON_COLOR_NAMES[col1] + (COLOR_STARS[col1] > st ? " 🔒" : "");
            }

            public void adjust(int d) {
                col1 = Math.floorMod(col1 + d, ICON_COLORS.length);
                saveIcon(st);
            }
        }));
        m.add(new MItem("보조 색", new Adj() {
            public String value() {
                return ICON_COLOR_NAMES[col2] + (COLOR_STARS[col2] > st ? " 🔒" : "");
            }

            public void adjust(int d) {
                col2 = Math.floorMod(col2 + d, ICON_COLORS.length);
                saveIcon(st);
            }
        }));
        m.add(new MItem("표정", new Adj() {
            public String value() {
                return FACE_NAMES[face] + (FACE_STARS[face] > st ? " 🔒 별 " + FACE_STARS[face] : "");
            }

            public void adjust(int d) {
                face = Math.floorMod(face + d, FACE_NAMES.length);
                saveIcon(st);
            }
        }));
        m.add(new MItem("궤적", new Adj() {
            public String value() {
                return trail ? "켜기" : "끄기";
            }

            public void adjust(int d) {
                trail = !trail;
                saveIcon(st);
            }
        }));
        m.onBack = () -> {
            // 잠긴 선택은 저장하지 않음
            col1 = prefs.getInt("col1", 0);
            col2 = prefs.getInt("col2", 1);
            face = prefs.getInt("face", 0);
            popMenu();
        };
        pushMenu(m);
    }

    private void saveIcon(int st) {
        SharedPreferences.Editor e = prefs.edit();
        if (COLOR_STARS[col1] <= st) e.putInt("col1", col1);
        if (COLOR_STARS[col2] <= st) e.putInt("col2", col2);
        if (FACE_STARS[face] <= st) e.putInt("face", face);
        e.putBoolean("trail", trail);
        e.apply();
    }

    private void openSettings() {
        Menu m = new Menu("설정");
        m.add(new MItem("음악 싱크", new Adj() {
            public String value() {
                return (syncMs > 0 ? "+" : "") + syncMs + "ms";
            }

            public void adjust(int d) {
                syncMs = Math.max(-300, Math.min(300, syncMs + d * 10));
                prefs.edit().putInt("sync", syncMs).apply();
            }
        }));
        m.add(new MItem("진동", new Adj() {
            public String value() {
                return vibeOn ? "켜기" : "끄기";
            }

            public void adjust(int d) {
                vibeOn = !vibeOn;
                prefs.edit().putBoolean("vibe", vibeOn).apply();
            }
        }));
        m.add(new MItem("도움말", "조작 방법 보기", this::openHelp));
        m.add(new MItem("버전", "v" + Updater.currentVersionName(getContext()), null));
        m.onBack = this::popMenu;
        pushMenu(m);
    }

    private void openHelp() {
        Menu m = new Menu("도움말");
        for (int i = 0; i < Defs.MODE_NAMES.length; i++) m.add(new MItem(Defs.MODE_NAMES[i] + " 모드", Defs.MODE_HELP[i], null));
        m.add(new MItem("베젤", "베젤 리프트 구간에서 돌리면 블록이 움직여요", null));
        m.add(new MItem("뒤로 버튼", "플레이 중: 일시정지", null));
        m.add(new MItem("에디터", "탭: 배치/선택 · 길게: 속성", null));
        m.add(new MItem("에디터 베젤", "배치: 오브젝트 고르기 · 선택: 이동", null));
        m.add(new MItem("에디터 드래그", "화면 이동", null));
        m.onBack = this::popMenu;
        pushMenu(m);
    }

    // =====================================================================
    // 플레이
    // =====================================================================

    private void startLevel(Level l, boolean prac, boolean editorTest) {
        level = l;
        practice = prac;
        fromEditor = editorTest;
        attempts = 0;
        bestThisSession = 0;
        screen = S_LOADING;
        final Level lv = l;
        new Thread(() -> {
            MapCode mc = new MapCode(lv.style, lv.bpm, 1, 0, Math.floorMod(lv.seed, 12), true, lv.seed);
            float dur = lv.endX() / Defs.SPEEDS[lv.startSpeed] + 6;
            int bars = (int) Math.ceil(dur / (4 * 60.0 / mc.bpm)) + 2;
            Song s = Song.generate(mc, bars);
            main.post(() -> {
                if (screen != S_LOADING || level != lv) return;
                song = s;
                eng = new Engine(lv);
                checkpoints.clear();
                newAttempt(true);
                screen = S_PLAY;
            });
        }, "song").start();
    }

    private void newAttempt(boolean fresh) {
        stopMusic();
        attempts++;
        parts.clear();
        trailN = 0;
        deathT = 0;
        paused = false;
        if (practice && !checkpoints.isEmpty() && !fresh) {
            eng.restore(checkpoints.get(checkpoints.size() - 1));
        } else {
            eng.reset();
            checkpoints.clear();
            if (!practice) {
                player = new AudioPlayer(song.pcm);
                player.start();
            }
        }
        sinceCheckpoint = 0;
        acc = 0;
        camX = eng.px - 3.2f;
        camY = Math.max(-1.5f, eng.py - 3f);
        startBannerT = 1.2f;
    }

    private void stopMusic() {
        if (player != null) {
            player.release();
            player = null;
        }
    }

    private void openPause() {
        paused = true;
        stopMusic();
        Menu m = new Menu("일시정지");
        m.add(new MItem("계속하기", practice ? "연습 모드" : "처음부터 다시 시작돼요", () -> {
            screen = S_PLAY;
            if (practice) {
                paused = false;
            } else {
                newAttempt(true);
            }
        }));
        m.add(new MItem("다시 시작", null, () -> {
            screen = S_PLAY;
            checkpoints.clear();
            newAttempt(true);
        }));
        m.add(new MItem(practice ? "연습 모드 끄기" : "연습 모드", "체크포인트 자동 저장 · 음악 없음", () -> {
            practice = !practice;
            screen = S_PLAY;
            checkpoints.clear();
            newAttempt(true);
        }));
        m.add(new MItem(fromEditor ? "에디터로" : "레벨 나가기", null, this::exitLevel));
        m.onBack = m.items.get(0).act;
        menuStack.clear();
        showMenu(m);
    }

    private void exitLevel() {
        stopMusic();
        eng = null;
        if (fromEditor) {
            screen = S_EDIT;
        } else if (levelId != null) {
            openMain();
            openMine();
        } else {
            openMain();
            openOfficial();
        }
    }

    private void onDeath() {
        deathT = 0.001f;
        int pct = Math.round(eng.progress() * 100);
        bestThisSession = Math.max(bestThisSession, pct);
        saveBest(pct, false);
        stopMusic();
        buzz(70);
        int c1 = ICON_COLORS[col1], c2 = ICON_COLORS[col2];
        for (int i = 0; i < 26; i++) {
            Part pt = new Part();
            pt.x = eng.px;
            pt.y = eng.py;
            double a = rnd.nextDouble() * Math.PI * 2;
            float v = 2 + rnd.nextFloat() * 7;
            pt.vx = (float) Math.cos(a) * v;
            pt.vy = (float) Math.sin(a) * v;
            pt.life = pt.max = 0.5f + rnd.nextFloat() * 0.4f;
            pt.size = 0.08f + rnd.nextFloat() * 0.14f;
            pt.color = i % 2 == 0 ? c1 : c2;
            parts.add(pt);
        }
    }

    private void saveBest(int pct, boolean done) {
        if (fromEditor || practice) return;
        String key = levelId != null ? "best_" + levelId : "best_off_" + officialIdx;
        SharedPreferences.Editor e = prefs.edit();
        if (pct > prefs.getInt(key, 0)) e.putInt(key, pct);
        if (done && officialIdx >= 0) e.putBoolean("done_" + officialIdx, true);
        e.apply();
    }

    private void onFinish() {
        stopMusic();
        saveBest(100, true);
        buzz(40);
        screen = S_RESULT;
        resultT = 0;
    }

    private float resultT;

    private void updatePlay(float dt) {
        if (paused || eng == null) return;
        if (deathT > 0) {
            deathT += dt;
            updateParts(dt);
            if (deathT > 0.75f) {
                if (practice && !checkpoints.isEmpty()) {
                    // 직전 체크포인트에서 바로 죽으면 하나 전으로
                    if (sinceCheckpoint < 0.6f && checkpoints.size() > 1) checkpoints.remove(checkpoints.size() - 1);
                }
                newAttempt(false);
            }
            return;
        }
        startBannerT = Math.max(0, startBannerT - dt);
        acc += Math.min(dt, 0.1f);
        while (acc >= Engine.DT) {
            acc -= Engine.DT;
            eng.step();
            sinceCheckpoint += Engine.DT;
            if (eng.evJump || eng.evOrb) spawnRing();
            if (eng.dead) {
                onDeath();
                return;
            }
            if (eng.finished) {
                onFinish();
                return;
            }
            if (practice && sinceCheckpoint > 1.5f && (eng.grounded || eng.mode == Defs.M_JET || eng.mode == Defs.M_ZIG)) {
                checkpoints.add(eng.snap());
                sinceCheckpoint = 0;
            }
        }
        if (eng.hapticMs > 0) {
            int ms = eng.hapticMs;
            eng.hapticMs = 0;
            if (vibeOn && vibrator != null) {
                if (eng.hapticPattern == 1) vibrator.vibrate(VibrationEffect.createWaveform(new long[]{0, ms, 80, ms}, -1));
                else vibrator.vibrate(VibrationEffect.createOneShot(eng.hapticPattern == 2 ? ms * 3 : ms, VibrationEffect.DEFAULT_AMPLITUDE));
            }
        }
        // 카메라
        float tx = eng.px - 3.2f;
        camX = tx;
        float rel = eng.py - camY;
        if (rel > 5.2f) camY += (rel - 5.2f) * Math.min(1, dt * 8);
        if (rel < 1.6f) camY += (rel - 1.6f) * Math.min(1, dt * 8);
        camY = Math.max(-1.5f, camY);
        // 궤적
        trailT += dt;
        if (trailT > 0.03f) {
            trailT = 0;
            System.arraycopy(trailX, 0, trailX, 1, trailX.length - 1);
            System.arraycopy(trailY, 0, trailY, 1, trailY.length - 1);
            trailX[0] = eng.px;
            trailY[0] = eng.py;
            trailN = Math.min(trailX.length, trailN + 1);
        }
        updateParts(dt);
    }

    private void spawnRing() {
        Part pt = new Part();
        pt.x = eng.px;
        pt.y = eng.py;
        pt.life = pt.max = 0.3f;
        pt.size = -1; // 링
        pt.color = ICON_COLORS[col2];
        parts.add(pt);
    }

    private void updateParts(float dt) {
        for (Iterator<Part> it = parts.iterator(); it.hasNext(); ) {
            Part pt = it.next();
            pt.life -= dt;
            pt.x += pt.vx * dt;
            pt.y += pt.vy * dt;
            pt.vy -= 9 * dt * (pt.size > 0 ? 1 : 0);
            if (pt.life <= 0) it.remove();
        }
    }

    // =====================================================================
    // 에디터
    // =====================================================================

    private void openEditor(Level l) {
        level = l;
        selected = null;
        undo.clear();
        dirty = false;
        edCamX = -2;
        edCamY = -1;
        tool = T_PLACE;
        screen = S_EDIT;
        edToast("드래그: 이동 · 베젤: 오브젝트 고르기");
    }

    private void edToast(String s) {
        edMsg = s;
        edMsgT = 2.2f;
    }

    private void pushUndo() {
        undo.add(level.copy());
        if (undo.size() > 40) undo.remove(0);
        dirty = true;
    }

    private void doUndo() {
        if (undo.isEmpty()) {
            edToast("되돌릴 것이 없어요");
            return;
        }
        Level prev = undo.remove(undo.size() - 1);
        level.objs.clear();
        level.objs.addAll(prev.objs);
        level.style = prev.style;
        level.bpm = prev.bpm;
        level.seed = prev.seed;
        level.startMode = prev.startMode;
        level.startSpeed = prev.startSpeed;
        level.bg = prev.bg;
        level.startSize = prev.startSize;
        selected = null;
        dirty = true;
        edToast("실행 취소");
    }

    private void saveLevel() {
        if (levelId != null) {
            LevelIO.save(getContext(), levelId, level);
            dirty = false;
        }
    }

    private float edScale() {
        return W / 9f * edZoom;
    }

    private float edWX(float sx) {
        return edCamX + sx / edScale();
    }

    private float edWY(float sy) {
        return edCamY + (H * 0.75f - sy) / edScale();
    }

    private Level.Obj objAt(int x, int y) {
        Level.Obj found = null;
        for (Level.Obj o : level.objs) {
            float h = Defs.height(o.type);
            int y0 = Defs.isPortal(o.type) ? o.y - 1 : o.y;
            if (o.x == x && y >= y0 && y < y0 + h) found = o;
        }
        return found;
    }

    private void edTap(float sx, float sy) {
        // 버튼들
        float br = W * 0.085f;
        if (dist(sx, sy, cx, H * 0.1f) < br * 1.2f) {
            tool = (tool + 1) % 3;
            selected = null;
            edToast(TOOL_NAMES[tool] + " 모드" + (tool == T_PLACE ? " · 베젤: 오브젝트" : tool == T_SELECT ? " · 베젤: 좌우 이동" : ""));
            return;
        }
        if (dist(sx, sy, W * 0.09f, cy) < br) {
            doUndo();
            return;
        }
        if (dist(sx, sy, W * 0.91f, cy) < br) {
            saveLevel();
            startLevel(level, false, true);
            return;
        }
        if (dist(sx, sy, cx, H * 0.91f) < br) {
            openEditorMenu();
            return;
        }
        int gx = (int) Math.floor(edWX(sx)), gy = (int) Math.floor(edWY(sy));
        if (gy < -3 || gy >= Engine.CEIL || gx < 0) return;
        Level.Obj hit = objAt(gx, gy);
        switch (tool) {
            case T_PLACE: {
                int type = Defs.PALETTE[palIdx];
                if (hit != null && hit.type == type) {
                    selected = hit;
                    return;
                }
                pushUndo();
                int py = Defs.isPortal(type) ? gy : gy;
                Level.Obj o = level.add(type, gx, py);
                if (Defs.isTrigger(type)) o.y = Math.max(gy, 0);
                selected = o;
                break;
            }
            case T_SELECT:
                selected = hit;
                if (hit != null) edToast(Defs.name(hit.type) + " · 길게 누르면 속성");
                break;
            case T_DELETE:
                if (hit != null) {
                    pushUndo();
                    level.objs.remove(hit);
                    if (selected == hit) selected = null;
                }
                break;
        }
    }

    private void onLongPress() {
        longFired = true;
        if (screen != S_EDIT) return;
        int gx = (int) Math.floor(edWX(downX)), gy = (int) Math.floor(edWY(downY));
        Level.Obj hit = objAt(gx, gy);
        if (hit == null) hit = selected;
        if (hit != null) {
            buzz(20);
            openProps(hit);
        }
    }

    private void openProps(final Level.Obj o) {
        selected = o;
        menuStack.clear();
        Menu m = new Menu(Defs.name(o.type));
        m.accent = Color.rgb(255, 210, 63);
        m.add(new MItem("설명", Defs.help(o.type), null));
        m.add(new MItem("X 위치", adjInt(() -> o.x, v -> o.x = Math.max(0, v), 1)));
        m.add(new MItem("Y 위치", adjInt(() -> o.y, v -> o.y = Math.max(-6, Math.min(13, v)), 1)));
        if (!Defs.isTrigger(o.type)) {
            m.add(new MItem("회전", new Adj() {
                public String value() {
                    return (o.rot * 90) + "°";
                }

                public void adjust(int d) {
                    o.rot = Math.floorMod(o.rot + d, 4);
                }
            }));
            m.add(new MItem("그룹 ID", new Adj() {
                public String value() {
                    return o.group == 0 ? "없음" : String.valueOf(o.group);
                }

                public void adjust(int d) {
                    o.group = Math.max(0, Math.min(99, o.group + d));
                }
            }));
            m.add(new MItem("시작 시 숨김", new Adj() {
                public String value() {
                    return o.hidden ? "켜기" : "끄기";
                }

                public void adjust(int d) {
                    o.hidden = !o.hidden;
                }
            }));
        }
        final Defs.Param[] ps = Defs.params(o.type);
        for (int i = 0; i < ps.length; i++) {
            final int k = i;
            m.add(new MItem(ps[i].name, new Adj() {
                public String value() {
                    return ps[k].show(o.p[k]);
                }

                public void adjust(int d) {
                    o.p[k] = Math.max(ps[k].min, Math.min(ps[k].max, o.p[k] + d));
                }
            }));
        }
        m.add(new MItem("복사해서 붙이기", "오른쪽 옆 칸에 하나 더", () -> {
            pushUndo();
            Level.Obj c = o.copy();
            c.x += 1;
            level.objs.add(c);
            selected = c;
            screen = S_EDIT;
        }));
        m.add(new MItem("같은 그룹 전체 복사", "그룹 오브젝트를 오른쪽에 복사", () -> {
            if (o.group == 0) {
                edToast("그룹이 없어요");
                screen = S_EDIT;
                return;
            }
            pushUndo();
            int minX = Integer.MAX_VALUE, maxX = 0;
            ArrayList<Level.Obj> g = new ArrayList<>();
            for (Level.Obj q : level.objs) if (q.group == o.group) {
                g.add(q);
                minX = Math.min(minX, q.x);
                maxX = Math.max(maxX, q.x);
            }
            int shift = maxX - minX + 2;
            for (Level.Obj q : g) {
                Level.Obj c = q.copy();
                c.x += shift;
                level.objs.add(c);
            }
            edToast(g.size() + "개 복사됨");
            screen = S_EDIT;
        }));
        m.add(new MItem("좌우 뒤집기", "회전을 거울처럼", () -> {
            pushUndo();
            if (o.rot == 1) o.rot = 3;
            else if (o.rot == 3) o.rot = 1;
            screen = S_EDIT;
        }));
        m.add(new MItem("삭제", null, () -> {
            pushUndo();
            level.objs.remove(o);
            selected = null;
            screen = S_EDIT;
        }));
        m.add(new MItem("완료", null, () -> screen = S_EDIT));
        m.onBack = () -> screen = S_EDIT;
        pushUndo(); // 속성 변경 전 상태 저장
        showMenu(m);
    }

    private interface IntGet {
        int get();
    }

    private interface IntSet {
        void set(int v);
    }

    private Adj adjInt(IntGet g, IntSet s, int step) {
        return new Adj() {
            public String value() {
                return String.valueOf(g.get());
            }

            public void adjust(int d) {
                s.set(g.get() + d * step);
            }
        };
    }

    private void openEditorMenu() {
        menuStack.clear();
        Menu m = new Menu("에디터 메뉴");
        m.add(new MItem("저장", null, () -> {
            saveLevel();
            edToast("저장했어요");
            screen = S_EDIT;
        }));
        m.add(new MItem("처음부터 테스트", null, () -> {
            saveLevel();
            startLevel(level, false, true);
        }));
        m.add(new MItem("레벨 설정", "음악 · 시작 모드 · 속도 · 배경", this::openLevelSettings));
        m.add(new MItem("확대/축소", edZoom == 1 ? "지금: 보통" : "지금: 넓게", () -> {
            edZoom = edZoom == 1 ? 0.6f : 1f;
            screen = S_EDIT;
        }));
        m.add(new MItem("처음으로 이동", null, () -> {
            edCamX = -2;
            screen = S_EDIT;
        }));
        m.add(new MItem("끝으로 이동", null, () -> {
            edCamX = level.maxX() - 4;
            screen = S_EDIT;
        }));
        m.add(new MItem("저장하고 나가기", null, () -> {
            saveLevel();
            openMain();
            openMine();
        }));
        m.onBack = () -> screen = S_EDIT;
        showMenu(m);
    }

    private void openLevelSettings() {
        Menu m = new Menu("레벨 설정");
        final Level l = level;
        m.add(new MItem("음악 장르", new Adj() {
            public String value() {
                return MapCode.STYLES[l.style];
            }

            public void adjust(int d) {
                l.style = Math.floorMod(l.style + d, 8);
                l.bpm = MapCode.STYLE_BPM[l.style];
                dirty = true;
            }
        }));
        m.add(new MItem("BPM", new Adj() {
            public String value() {
                return String.valueOf(l.bpm);
            }

            public void adjust(int d) {
                l.bpm = Math.max(80, Math.min(206, l.bpm + d * 2));
                dirty = true;
            }
        }));
        m.add(new MItem("멜로디 번호", new Adj() {
            public String value() {
                return String.valueOf(l.seed);
            }

            public void adjust(int d) {
                l.seed = Math.max(0, l.seed + d);
                dirty = true;
            }
        }));
        m.add(new MItem("시작 모드", new Adj() {
            public String value() {
                return Defs.MODE_NAMES[l.startMode];
            }

            public void adjust(int d) {
                l.startMode = Math.floorMod(l.startMode + d, Defs.MODE_NAMES.length);
                dirty = true;
            }
        }));
        m.add(new MItem("시작 속도", new Adj() {
            public String value() {
                return Defs.SPEED_NAMES[l.startSpeed];
            }

            public void adjust(int d) {
                l.startSpeed = Math.max(0, Math.min(3, l.startSpeed + d));
                dirty = true;
            }
        }));
        m.add(new MItem("시작 크기", new Adj() {
            public String value() {
                return Defs.SIZE_NAMES[l.startSize];
            }

            public void adjust(int d) {
                l.startSize = Math.max(0, Math.min(2, l.startSize + d));
                dirty = true;
            }
        }));
        m.add(new MItem("배경색", new Adj() {
            public String value() {
                return Defs.COLOR_NAMES[l.bg];
            }

            public void adjust(int d) {
                l.bg = Math.floorMod(l.bg + d, Defs.BG_COLORS.length);
                dirty = true;
            }
        }));
        m.add(new MItem("완료", null, () -> screen = S_EDIT));
        m.onBack = () -> screen = S_EDIT;
        showMenu(m);
    }

    // =====================================================================
    // 입력
    // =====================================================================

    void onRotary(float delta) {
        if (delta == 0) return;
        int dir = delta > 0 ? 1 : -1;
        if (screen == S_PLAY) {
            if (eng != null && deathT == 0 && !paused) eng.bezel(dir);
            return;
        }
        long now = SystemClock.uptimeMillis();
        if (now - lastRot > 300) rotAcc = 0;
        lastRot = now;
        rotAcc += delta;
        if (Math.abs(rotAcc) < 0.9f) return;
        rotAcc = 0;
        if (screen == S_MENU && menu != null) {
            if (menu.editing) {
                MItem it = menu.items.get(menu.sel);
                if (it.adj != null) it.adj.adjust(dir);
            } else {
                menu.sel = Math.max(0, Math.min(menu.items.size() - 1, menu.sel + dir));
            }
            tick();
        } else if (screen == S_EDIT) {
            if (tool == T_PLACE) {
                palIdx = Math.floorMod(palIdx + dir, Defs.PALETTE.length);
                edToast(Defs.name(Defs.PALETTE[palIdx]));
            } else if (tool == T_SELECT && selected != null) {
                pushUndo();
                selected.x = Math.max(0, selected.x + dir);
                float sx = (selected.x - edCamX) * edScale();
                if (sx < W * 0.15f) edCamX -= 1;
                if (sx > W * 0.85f) edCamX += 1;
            } else {
                edCamX += dir * 2;
            }
            tick();
        }
        invalidate();
    }

    boolean back() {
        switch (screen) {
            case S_PLAY:
                if (!paused) openPause();
                return true;
            case S_MENU:
                if (menu != null && menu.editing) {
                    menu.editing = false;
                    return true;
                }
                if (menu != null && menu.onBack != null) {
                    menu.onBack.run();
                    return true;
                }
                return false;
            case S_EDIT:
                openEditorMenu();
                return true;
            case S_RESULT:
                exitLevel();
                return true;
            case S_LOADING:
                return true;
            default:
                return false;
        }
    }

    @Override
    public boolean onTouchEvent(MotionEvent e) {
        int a = e.getActionMasked();
        if (screen == S_PLAY) {
            if (paused || eng == null) return true;
            if (a == MotionEvent.ACTION_DOWN || a == MotionEvent.ACTION_POINTER_DOWN) eng.press();
            else if (a == MotionEvent.ACTION_UP || a == MotionEvent.ACTION_CANCEL) eng.release();
            return true;
        }
        switch (a) {
            case MotionEvent.ACTION_DOWN:
                downX = lastX = e.getX();
                downY = lastY = e.getY();
                downTime = e.getEventTime();
                moved = false;
                longFired = false;
                if (screen == S_EDIT) main.postDelayed(longPress, 550);
                return true;
            case MotionEvent.ACTION_MOVE: {
                float dx = e.getX() - downX, dy = e.getY() - downY;
                if (!moved && Math.hypot(dx, dy) > W * 0.05f) {
                    moved = true;
                    main.removeCallbacks(longPress);
                }
                if (moved && screen == S_EDIT) {
                    edCamX -= (e.getX() - lastX) / edScale();
                    edCamY += (e.getY() - lastY) / edScale();
                    edCamX = Math.max(-4, edCamX);
                    edCamY = Math.max(-4, Math.min(10, edCamY));
                }
                if (moved && screen == S_MENU && menu != null && !menu.editing) {
                    float step = H * 0.12f;
                    if (Math.abs(e.getY() - lastY) > 0) {
                        float tot = e.getY() - downY;
                        int want = (int) (-tot / step);
                        if (want != 0) {
                            menu.sel = Math.max(0, Math.min(menu.items.size() - 1, menu.sel + want));
                            downY = e.getY();
                        }
                    }
                }
                lastX = e.getX();
                lastY = e.getY();
                invalidate();
                return true;
            }
            case MotionEvent.ACTION_UP: {
                main.removeCallbacks(longPress);
                if (longFired) return true;
                float dx = e.getX() - downX;
                if (moved) {
                    // 오른쪽 스와이프 = 뒤로 (메뉴에서만)
                    if (screen == S_MENU && dx > W * 0.25f && Math.abs(e.getY() - downY) < H * 0.15f) back();
                    return true;
                }
                onTap(e.getX(), e.getY());
                invalidate();
                return true;
            }
            case MotionEvent.ACTION_CANCEL:
                main.removeCallbacks(longPress);
                return true;
        }
        return super.onTouchEvent(e);
    }

    private void onTap(float x, float y) {
        switch (screen) {
            case S_MENU: {
                if (menu == null) return;
                float sp = H * 0.2f;
                int idx = Math.round(menu.pos + (y - cy) / sp);
                if (idx != menu.sel && !menu.editing) {
                    if (idx >= 0 && idx < menu.items.size()) menu.sel = idx;
                    return;
                }
                MItem it = menu.items.get(menu.sel);
                if (it.adj != null) {
                    menu.editing = !menu.editing;
                } else if (it.act != null) {
                    tick();
                    it.act.run();
                }
                break;
            }
            case S_EDIT:
                edTap(x, y);
                break;
            case S_RESULT:
                if (resultT < 0.6f) return;
                if (fromEditor) {
                    exitLevel();
                } else {
                    startLevel(level, false, false);
                }
                break;
        }
    }

    private static float dist(float x1, float y1, float x2, float y2) {
        return (float) Math.hypot(x1 - x2, y1 - y2);
    }

    private void tick() {
        buzz(5);
    }

    private void buzz(int ms) {
        if (vibeOn && vibrator != null) vibrator.vibrate(VibrationEffect.createOneShot(ms, VibrationEffect.DEFAULT_AMPLITUDE));
    }

    // =====================================================================
    // 그리기
    // =====================================================================

    @Override
    protected void onDraw(Canvas c) {
        long now = System.nanoTime();
        float dt = lastFrame == 0 ? 0.016f : Math.min(0.05f, (now - lastFrame) / 1e9f);
        lastFrame = now;
        edMsgT = Math.max(0, edMsgT - dt);
        resultT += dt;
        if (updater != null && screen == S_MENU) {
            updater.checkIfDue();
            if (updater.isReady()) updater.installIfReady();
        }
        switch (screen) {
            case S_MENU:
                drawMenu(c, dt);
                break;
            case S_LOADING:
                drawLoading(c);
                break;
            case S_PLAY:
                updatePlay(dt);
                if (screen == S_PLAY) drawPlay(c);
                else invalidate();
                break;
            case S_RESULT:
                drawResult(c);
                break;
            case S_EDIT:
                drawEditor(c);
                break;
        }
        if (running) postInvalidateOnAnimation();
    }

    private void glow(Canvas c, float x, float y, float rad, int color) {
        mtx.setScale(rad, rad);
        mtx.postTranslate(x, y);
        glowShader.setLocalMatrix(mtx);
        p.setShader(glowShader);
        p.setColorFilter(new PorterDuffColorFilter(color, PorterDuff.Mode.MULTIPLY));
        c.drawCircle(x, y, rad, p);
        p.setShader(null);
        p.setColorFilter(null);
    }

    private static int alpha(int color, int a) {
        return (color & 0x00FFFFFF) | (Math.max(0, Math.min(255, a)) << 24);
    }

    private static int lerpColor(int a, int b, float t) {
        int ar = (a >> 16) & 255, ag = (a >> 8) & 255, ab = a & 255;
        int br = (b >> 16) & 255, bg = (b >> 8) & 255, bb = b & 255;
        return Color.rgb((int) (ar + (br - ar) * t), (int) (ag + (bg - ag) * t), (int) (ab + (bb - ab) * t));
    }

    private static int shade(int color, float f) {
        int r = (int) (((color >> 16) & 255) * f), g = (int) (((color >> 8) & 255) * f), b = (int) ((color & 255) * f);
        return Color.rgb(Math.min(255, r), Math.min(255, g), Math.min(255, b));
    }

    // ---------- 메뉴 ----------

    private void drawMenu(Canvas c, float dt) {
        Menu m = menu;
        int bg = Color.rgb(12, 16, 40);
        c.drawColor(bg);
        float t = SystemClock.uptimeMillis() / 1000f;
        // 배경: 천천히 흐르는 블록 실루엣
        p.setColor(Color.rgb(20, 28, 64));
        for (int i = 0; i < 8; i++) {
            float x = ((i * 0.37f + t * 0.05f) % 1.2f) * W - W * 0.1f;
            float s = W * (0.08f + 0.05f * (i % 3));
            c.drawRect(x, H * 0.82f - s * (i % 2 + 1), x + s, H, p);
        }
        glow(c, cx, cy, W * 0.6f, alpha(m.accent, 40));
        m.pos += (m.sel - m.pos) * Math.min(1f, dt * 12f);
        float sp = H * 0.2f;
        for (int i = 0; i < m.items.size(); i++) {
            float y = cy + (i - m.pos) * sp;
            if (y < -sp || y > H + sp) continue;
            float d = Math.abs(i - m.pos);
            float k = Math.max(0, 1 - d * 0.5f);
            MItem it = m.items.get(i);
            boolean selected = i == m.sel;
            if (selected) {
                p.setColor(alpha(m.accent, m.editing ? 70 : 38));
                r.set(cx - W * 0.41f, y - sp * 0.42f, cx + W * 0.41f, y + sp * 0.42f);
                c.drawRoundRect(r, sp * 0.3f, sp * 0.3f, p);
                stroke.setColor(alpha(m.accent, 230));
                stroke.setStrokeWidth(W * (m.editing ? 0.012f : 0.007f));
                c.drawRoundRect(r, sp * 0.3f, sp * 0.3f, stroke);
            }
            String sub = it.adj != null ? it.adj.value() : it.sub;
            boolean hasSub = sub != null && !sub.isEmpty();
            text.setTextSize(W * (0.052f + 0.02f * k));
            text.setColor(alpha(Color.WHITE, (int) (90 + 165 * k)));
            c.drawText(it.label, cx, y + (hasSub ? -W * 0.008f : W * 0.022f), text);
            if (hasSub) {
                text.setTextSize(W * 0.038f);
                int sc = it.adj != null ? (selected && m.editing ? Color.rgb(255, 230, 120) : m.accent) : Color.rgb(170, 180, 210);
                text.setColor(alpha(sc, (int) (80 + 175 * k)));
                String shown = it.adj != null ? (selected && m.editing ? "◀ " + sub + " ▶" : sub) : sub;
                c.drawText(fit(shown, W * 0.74f), cx, y + W * 0.045f, text);
            }
        }
        // 제목 띠
        p.setColor(bg);
        c.drawRect(0, 0, W, H * 0.14f, p);
        text.setTextSize(W * 0.05f);
        text.setColor(m.accent);
        c.drawText(m.title, cx, H * 0.105f, text);
        if (m.editing) {
            text.setTextSize(W * 0.036f);
            text.setColor(Color.LTGRAY);
            c.drawText("베젤로 조절 · 탭하면 완료", cx, H * 0.95f, text);
        } else if (updater != null && !updater.status().isEmpty()) {
            text.setTextSize(W * 0.036f);
            text.setColor(Color.rgb(120, 220, 255));
            c.drawText(updater.status(), cx, H * 0.95f, text);
        }
        if (m.title.equals("아이콘")) drawPlayerIcon(c, cx, H * 0.86f, W * 0.07f, 0, Defs.M_JUMP);
    }

    private String fit(String s, float maxW) {
        if (text.measureText(s) <= maxW) return s;
        while (s.length() > 2 && text.measureText(s + "…") > maxW) s = s.substring(0, s.length() - 1);
        return s + "…";
    }

    private void drawLoading(Canvas c) {
        c.drawColor(Color.rgb(12, 16, 40));
        float t = SystemClock.uptimeMillis() / 1000f;
        drawPlayerIcon(c, cx, cy - W * 0.05f, W * 0.08f, t * 360 % 360, Defs.M_JUMP);
        text.setTextSize(W * 0.05f);
        text.setColor(Color.WHITE);
        c.drawText(level != null ? level.name : "", cx, H * 0.7f, text);
        text.setTextSize(W * 0.04f);
        text.setColor(Color.LTGRAY);
        c.drawText("음악 만드는 중…", cx, H * 0.78f, text);
    }

    // ---------- 월드 그리기 (플레이/에디터 공용) ----------

    private float vx0, vy0, vs; // 뷰 변환: 화면 = (월드 - vx0)*vs , y 는 기준선 vy0

    private float sx(float wx) {
        return (wx - vx0) * vs;
    }

    private float sy(float wy) {
        return vy0 - wy * vs;
    }

    private void drawBackground(Canvas c, int bgColor, float scroll) {
        c.drawColor(bgColor);
        int dark = shade(bgColor, 0.55f);
        p.setShader(new LinearGradient(0, 0, 0, H, bgColor, dark, Shader.TileMode.CLAMP));
        c.drawRect(0, 0, W, H, p);
        p.setShader(null);
        // 패럴랙스 도형
        p.setColor(alpha(shade(bgColor, 1.5f), 70));
        float s = W * 0.22f;
        for (int i = -1; i < 6; i++) {
            float x = ((i * s * 1.6f) - (scroll * vs * 0.25f) % (s * 1.6f));
            float y = H * 0.18f + (i % 3) * H * 0.1f;
            r.set(x, y, x + s, y + s);
            c.drawRoundRect(r, s * 0.1f, s * 0.1f, p);
        }
        p.setColor(alpha(shade(bgColor, 1.8f), 45));
        float s2 = W * 0.1f;
        for (int i = -1; i < 12; i++) {
            float x = ((i * s2 * 1.4f) - (scroll * vs * 0.5f) % (s2 * 1.4f));
            c.drawCircle(x, H * 0.42f + (i % 2) * H * 0.08f, s2 * 0.35f, p);
        }
    }

    private void drawGround(Canvas c, int bgColor) {
        float gy = sy(0);
        int gc = shade(bgColor, 0.7f);
        p.setColor(gc);
        c.drawRect(0, gy, W, H, p);
        stroke.setColor(alpha(shade(bgColor, 1.4f), 70));
        stroke.setStrokeWidth(W * 0.004f);
        float t = vs;
        for (float x = -((vx0 * t) % t); x < W; x += t) c.drawLine(x, gy, x - t * 0.6f, H, stroke);
        stroke.setColor(Color.WHITE);
        stroke.setStrokeWidth(W * 0.006f);
        c.drawLine(0, gy, W, gy, stroke);
        glow(c, cx, gy, W * 0.5f, alpha(Color.WHITE, 40));
        float cyl = sy(Engine.CEIL);
        if (cyl > -10) {
            p.setColor(gc);
            c.drawRect(0, 0, W, cyl, p);
            c.drawLine(0, cyl, W, cyl, stroke);
        }
    }

    private int portalColor(Level.Obj o) {
        switch (o.type) {
            case Defs.P_MODE: {
                int[] cs = {0xFF7CFF6B, 0xFFFF6BCB, 0xFFFF9F3D, 0xFFB57BFF, 0xFF3DE8FF};
                return cs[Math.max(0, Math.min(4, o.param(0)))];
            }
            case Defs.P_GRAV:
                return o.param(0) == 1 ? 0xFFFFD23F : 0xFF3D8BFF;
            case Defs.P_SIZE:
                return o.param(0) == 0 ? 0xFFFF5CC8 : o.param(0) == 2 ? 0xFF5CFF8F : 0xFFFFFFFF;
            default:
                return 0xFFFFFFFF;
        }
    }

    private void drawObj(Canvas c, Level.Obj o, float x, float y, float rotDeg, float alpha, boolean editor, int bgColor) {
        float s = vs;
        float left = sx(x), top = sy(y + 1);
        if (left > W + s * 2 || left < -s * 2) return;
        int a = (int) (255 * alpha);
        if (a <= 2 && !editor) return;
        if (editor && alpha < 0.3f) a = 90;
        float ccx = left + s / 2, ccy = top + s / 2;
        c.save();
        if (rotDeg != 0 && !Defs.isPortal(o.type)) c.rotate(rotDeg, ccx, ccy);
        switch (o.type) {
            case Defs.BLOCK: {
                int fill = shade(bgColor, 0.45f);
                p.setColor(alpha(fill, a));
                c.drawRect(left, top, left + s, top + s, p);
                stroke.setStrokeWidth(s * 0.07f);
                stroke.setColor(alpha(Color.WHITE, (int) (a * 0.9f)));
                c.drawRect(left + s * 0.04f, top + s * 0.04f, left + s * 0.96f, top + s * 0.96f, stroke);
                stroke.setStrokeWidth(s * 0.03f);
                stroke.setColor(alpha(shade(bgColor, 2.2f), (int) (a * 0.6f)));
                c.drawRect(left + s * 0.22f, top + s * 0.22f, left + s * 0.78f, top + s * 0.78f, stroke);
                break;
            }
            case Defs.SPIKE:
            case Defs.SMALL_SPIKE: {
                float hgt = o.type == Defs.SPIKE ? 0.95f : 0.5f;
                path.reset();
                path.moveTo(left + s * 0.06f, top + s);
                path.lineTo(left + s * 0.5f, top + s * (1 - hgt));
                path.lineTo(left + s * 0.94f, top + s);
                path.close();
                p.setColor(alpha(Color.rgb(10, 10, 16), a));
                c.drawPath(path, p);
                stroke.setStrokeWidth(s * 0.07f);
                stroke.setColor(alpha(Color.WHITE, a));
                c.drawPath(path, stroke);
                break;
            }
            case Defs.SAW: {
                float spin = (SystemClock.uptimeMillis() % 1000) * 0.36f;
                c.rotate(spin, ccx, ccy);
                p.setColor(alpha(Color.rgb(20, 20, 26), a));
                c.drawCircle(ccx, ccy, s * 0.42f, p);
                p.setColor(alpha(Color.WHITE, a));
                for (int k = 0; k < 8; k++) {
                    c.save();
                    c.rotate(k * 45, ccx, ccy);
                    path.reset();
                    path.moveTo(ccx - s * 0.08f, ccy - s * 0.4f);
                    path.lineTo(ccx, ccy - s * 0.52f);
                    path.lineTo(ccx + s * 0.08f, ccy - s * 0.4f);
                    path.close();
                    c.drawPath(path, p);
                    c.restore();
                }
                stroke.setStrokeWidth(s * 0.06f);
                stroke.setColor(alpha(Color.WHITE, a));
                c.drawCircle(ccx, ccy, s * 0.36f, stroke);
                c.drawCircle(ccx, ccy, s * 0.1f, p);
                break;
            }
            case Defs.PAD: {
                glow(c, ccx, top + s * 0.85f, s * 0.7f, alpha(0xFFFFD23F, a / 2));
                p.setColor(alpha(0xFFFFD23F, a));
                r.set(left + s * 0.08f, top + s * 0.7f, left + s * 0.92f, top + s * 1.1f);
                c.drawArc(r, 180, 180, true, p);
                break;
            }
            case Defs.ORB:
            case Defs.G_ORB: {
                int oc = o.type == Defs.ORB ? 0xFFFFD23F : 0xFF3DE8FF;
                float pulse = 0.85f + 0.15f * (float) Math.sin(SystemClock.uptimeMillis() / 120.0);
                glow(c, ccx, ccy, s * 0.9f * pulse, alpha(oc, a / 2));
                stroke.setStrokeWidth(s * 0.1f);
                stroke.setColor(alpha(oc, a));
                c.drawCircle(ccx, ccy, s * 0.32f * pulse, stroke);
                p.setColor(alpha(Color.WHITE, a));
                c.drawCircle(ccx, ccy, s * 0.12f, p);
                break;
            }
            case Defs.P_MODE:
            case Defs.P_GRAV:
            case Defs.P_SIZE:
            case Defs.P_SPEED: {
                float pt = sy(y + 2), pb = sy(y - 1);
                int pc = portalColor(o);
                if (o.type == Defs.P_SPEED) {
                    int n = o.param(0) + 1;
                    p.setColor(alpha(Color.WHITE, a));
                    text.setTextSize(s * 0.8f);
                    text.setColor(alpha(Color.rgb(255, 230, 120), a));
                    StringBuilder sb = new StringBuilder();
                    for (int k = 0; k < n; k++) sb.append('>');
                    c.drawText(sb.toString(), ccx, (pt + pb) / 2 + s * 0.28f, text);
                } else {
                    glow(c, ccx, (pt + pb) / 2, s * 1.4f, alpha(pc, a / 3));
                    stroke.setStrokeWidth(s * 0.14f);
                    stroke.setColor(alpha(pc, a));
                    r.set(left + s * 0.15f, pt, left + s * 0.85f, pb);
                    c.drawOval(r, stroke);
                    if (o.type == Defs.P_SIZE) {
                        text.setTextSize(s * 0.36f);
                        text.setColor(alpha(Color.WHITE, a));
                        c.drawText(Defs.SIZE_NAMES[o.param(0)].substring(0, 1), ccx, (pt + pb) / 2 + s * 0.13f, text);
                    } else if (o.type == Defs.P_MODE) {
                        text.setTextSize(s * 0.36f);
                        text.setColor(alpha(Color.WHITE, a));
                        c.drawText(Defs.MODE_NAMES[o.param(0)].substring(0, 1), ccx, (pt + pb) / 2 + s * 0.13f, text);
                    } else {
                        text.setTextSize(s * 0.5f);
                        text.setColor(alpha(Color.WHITE, a));
                        c.drawText(o.param(0) == 1 ? "▲" : "▼", ccx, (pt + pb) / 2 + s * 0.18f, text);
                    }
                }
                break;
            }
            case Defs.DECO: {
                int dc = alpha(shade(bgColor, 2.4f), (int) (a * 0.7f));
                p.setColor(dc);
                switch (o.param(0)) {
                    case 0: {
                        path.reset();
                        for (int k = 0; k < 10; k++) {
                            double ang = Math.PI / 2 + k * Math.PI / 5;
                            float rr = (k % 2 == 0 ? 0.45f : 0.2f) * s;
                            float px = ccx + (float) Math.cos(ang) * rr, py = ccy - (float) Math.sin(ang) * rr;
                            if (k == 0) path.moveTo(px, py);
                            else path.lineTo(px, py);
                        }
                        path.close();
                        c.drawPath(path, p);
                        break;
                    }
                    case 1:
                        c.drawCircle(ccx, ccy, s * 0.35f, p);
                        break;
                    case 2:
                        c.drawRect(left, ccy - s * 0.06f, left + s, ccy + s * 0.06f, p);
                        break;
                    default:
                        glow(c, ccx, ccy, s * 1.2f, dc);
                }
                break;
            }
            default:
                if (editor && Defs.isTrigger(o.type)) {
                    p.setColor(alpha(Color.rgb(255, 210, 63), 200));
                    c.drawCircle(ccx, ccy, s * 0.38f, p);
                    text.setTextSize(s * 0.3f);
                    text.setColor(Color.BLACK);
                    String nm = Defs.name(o.type);
                    c.drawText(nm.substring(0, Math.min(2, nm.length())), ccx, ccy + s * 0.1f, text);
                    if (o.p.length > 0 && o.type != Defs.T_BG && o.type != Defs.T_FLASH && o.type != Defs.T_SHAKE
                            && o.type != Defs.T_ZOOM && o.type != Defs.T_HAPTIC) {
                        text.setTextSize(s * 0.24f);
                        text.setColor(Color.WHITE);
                        c.drawText("G" + o.param(0), ccx, ccy + s * 0.62f, text);
                        // 트리거 위치 표시선
                        stroke.setStrokeWidth(1.5f);
                        stroke.setColor(alpha(Color.rgb(255, 210, 63), 70));
                        c.drawLine(ccx, 0, ccx, H, stroke);
                    }
                }
        }
        c.restore();
        if (editor && o.group > 0 && !Defs.isTrigger(o.type)) {
            text.setTextSize(s * 0.22f);
            text.setColor(Color.rgb(255, 210, 63));
            c.drawText("G" + o.group, ccx, top + s * 0.3f, text);
        }
    }

    private void drawPlayerIcon(Canvas c, float x, float y, float half, float rot, int mode) {
        int c1 = ICON_COLORS[col1], c2 = ICON_COLORS[col2];
        c.save();
        c.rotate(rot, x, y);
        switch (mode) {
            case Defs.M_JET: {
                path.reset();
                path.moveTo(x + half * 1.3f, y);
                path.lineTo(x - half, y - half * 0.8f);
                path.lineTo(x - half * 0.6f, y);
                path.lineTo(x - half, y + half * 0.8f);
                path.close();
                p.setColor(c1);
                c.drawPath(path, p);
                stroke.setStrokeWidth(half * 0.15f);
                stroke.setColor(Color.BLACK);
                c.drawPath(path, stroke);
                p.setColor(c2);
                c.drawCircle(x, y, half * 0.32f, p);
                glow(c, x - half * 1.1f, y, half * 0.8f, alpha(c2, 180));
                break;
            }
            case Defs.M_FLIP: {
                p.setColor(c1);
                c.drawCircle(x, y, half, p);
                stroke.setStrokeWidth(half * 0.14f);
                stroke.setColor(Color.BLACK);
                c.drawCircle(x, y, half, stroke);
                p.setColor(c2);
                c.drawCircle(x, y, half * 0.55f, p);
                drawFace(c, x, y, half * 0.6f);
                break;
            }
            case Defs.M_STEP: {
                p.setColor(c2);
                r.set(x - half * 0.6f, y - half * 1.0f, x + half * 0.6f, y + half * 0.2f);
                c.drawOval(r, p);
                p.setColor(c1);
                r.set(x - half * 1.2f, y - half * 0.2f, x + half * 1.2f, y + half * 0.6f);
                c.drawOval(r, p);
                stroke.setStrokeWidth(half * 0.12f);
                stroke.setColor(Color.BLACK);
                c.drawOval(r, stroke);
                break;
            }
            case Defs.M_ZIG: {
                path.reset();
                path.moveTo(x + half * 1.2f, y);
                path.lineTo(x - half * 0.8f, y - half * 0.9f);
                path.lineTo(x - half * 0.8f, y + half * 0.9f);
                path.close();
                p.setColor(c1);
                c.drawPath(path, p);
                stroke.setStrokeWidth(half * 0.14f);
                stroke.setColor(c2);
                c.drawPath(path, stroke);
                break;
            }
            default: {
                p.setColor(c1);
                r.set(x - half, y - half, x + half, y + half);
                c.drawRoundRect(r, half * 0.2f, half * 0.2f, p);
                stroke.setStrokeWidth(half * 0.14f);
                stroke.setColor(Color.BLACK);
                c.drawRoundRect(r, half * 0.2f, half * 0.2f, stroke);
                p.setColor(c2);
                r.set(x - half * 0.62f, y - half * 0.62f, x + half * 0.62f, y + half * 0.62f);
                c.drawRoundRect(r, half * 0.12f, half * 0.12f, p);
                drawFace(c, x, y, half * 0.62f);
            }
        }
        c.restore();
    }

    private void drawFace(Canvas c, float x, float y, float h) {
        p.setColor(Color.BLACK);
        stroke.setColor(Color.BLACK);
        stroke.setStrokeWidth(h * 0.18f);
        switch (face) {
            case 1: // 졸린 눈
                c.drawLine(x - h * 0.6f, y - h * 0.15f, x - h * 0.15f, y - h * 0.15f, stroke);
                c.drawLine(x + h * 0.15f, y - h * 0.15f, x + h * 0.6f, y - h * 0.15f, stroke);
                c.drawLine(x - h * 0.3f, y + h * 0.45f, x + h * 0.3f, y + h * 0.45f, stroke);
                break;
            case 2: // 윙크
                c.drawRect(x - h * 0.6f, y - h * 0.45f, x - h * 0.2f, y - h * 0.05f, p);
                c.drawLine(x + h * 0.2f, y - h * 0.2f, x + h * 0.6f, y - h * 0.2f, stroke);
                r.set(x - h * 0.4f, y + h * 0.1f, x + h * 0.4f, y + h * 0.6f);
                c.drawArc(r, 0, 180, false, stroke);
                break;
            case 3: // 로봇
                c.drawRect(x - h * 0.65f, y - h * 0.4f, x + h * 0.65f, y - h * 0.05f, p);
                p.setColor(Color.rgb(255, 60, 60));
                c.drawRect(x - h * 0.5f, y - h * 0.32f, x - h * 0.1f, y - h * 0.13f, p);
                c.drawRect(x + h * 0.1f, y - h * 0.32f, x + h * 0.5f, y - h * 0.13f, p);
                p.setColor(Color.BLACK);
                c.drawRect(x - h * 0.4f, y + h * 0.3f, x + h * 0.4f, y + h * 0.45f, p);
                break;
            case 4: // 별
                p.setColor(Color.WHITE);
                c.drawCircle(x, y, h * 0.5f, p);
                p.setColor(Color.BLACK);
                c.drawCircle(x, y, h * 0.22f, p);
                break;
            default:
                c.drawRect(x - h * 0.55f, y - h * 0.45f, x - h * 0.15f, y - h * 0.05f, p);
                c.drawRect(x + h * 0.15f, y - h * 0.45f, x + h * 0.55f, y - h * 0.05f, p);
                c.drawRect(x - h * 0.45f, y + h * 0.25f, x + h * 0.45f, y + h * 0.45f, p);
        }
    }

    // ---------- 플레이 화면 ----------

    private void drawPlay(Canvas c) {
        Engine e = eng;
        int bgCol = lerpColor(Defs.BG_COLORS[e.bgFrom], Defs.BG_COLORS[e.bgTo], e.bgT);
        vs = W / 9f * e.zoom;
        vx0 = camX;
        vy0 = H * 0.72f + camY * vs;
        c.save();
        if (e.shakeT > 0) c.translate((rnd.nextFloat() - 0.5f) * e.shakeAmp * vs, (rnd.nextFloat() - 0.5f) * e.shakeAmp * vs);
        drawBackground(c, bgCol, camX);
        drawGround(c, bgCol);
        for (Level.Obj o : e.objs) {
            float x = e.ox(o);
            if (x < camX - 3 || x > camX + 12 / e.zoom) continue;
            if (Defs.isTrigger(o.type) || !e.visible(o)) continue;
            drawObj(c, o, x, e.oy(o), e.rotation(o), e.alpha(o), false, bgCol);
        }
        // 체크포인트 (연습)
        if (practice) {
            p.setColor(Color.rgb(120, 255, 140));
            for (Engine.Snap s : checkpoints) {
                // 스냅 위치는 비공개라 마지막 것만 강조하지 않고 생략
            }
        }
        // 궤적
        float half = e.half() * vs;
        if (trail && deathT == 0) {
            for (int i = trailN - 1; i >= 1; i--) {
                float k = 1 - i / (float) trailX.length;
                p.setColor(alpha(ICON_COLORS[col2], (int) (120 * k)));
                float tsz = half * 0.5f * k;
                c.drawRect(sx(trailX[i]) - tsz, sy(trailY[i]) - tsz, sx(trailX[i]) + tsz, sy(trailY[i]) + tsz, p);
            }
        }
        if (deathT == 0) {
            glow(c, sx(e.px), sy(e.py), half * 2.4f, alpha(ICON_COLORS[col1], 90));
            drawPlayerIcon(c, sx(e.px), sy(e.py), half, e.rotDeg * (e.mode == Defs.M_JUMP ? e.grav : 1), e.mode);
            if (e.mode == Defs.M_FLIP && e.grav == -1 || e.grav == -1) {
                // 중력 반전 표시
                p.setColor(alpha(0xFFFFD23F, 160));
                c.drawCircle(sx(e.px), sy(e.py) + half * 1.5f, half * 0.18f, p);
            }
        }
        for (Part pt : parts) {
            float k = pt.life / pt.max;
            if (pt.size < 0) {
                stroke.setStrokeWidth(vs * 0.06f);
                stroke.setColor(alpha(pt.color, (int) (220 * k)));
                c.drawCircle(sx(pt.x), sy(pt.y), vs * (0.4f + (1 - k) * 0.7f), stroke);
            } else {
                p.setColor(alpha(pt.color, (int) (255 * k)));
                float z = pt.size * vs;
                c.drawRect(sx(pt.x) - z, sy(pt.y) - z, sx(pt.x) + z, sy(pt.y) + z, p);
            }
        }
        c.restore();

        if (e.flashT > 0) {
            p.setColor(alpha(Defs.FLASH_COLORS[e.flashColor], (int) (200 * e.flashT / e.flashDur)));
            c.drawRect(0, 0, W, H, p);
        }
        if (deathT > 0) {
            p.setColor(alpha(Color.WHITE, (int) (120 * Math.max(0, 1 - deathT * 4))));
            c.drawRect(0, 0, W, H, p);
            text.setTextSize(W * 0.09f);
            text.setColor(Color.WHITE);
            c.drawText(Math.round(e.progress() * 100) + "%", cx, H * 0.4f, text);
        }
        // 진행 바 (위쪽 호)
        float ro = W * 0.48f;
        r.set(cx - ro, cy - ro, cx + ro, cy + ro);
        stroke.setStrokeWidth(W * 0.016f);
        stroke.setColor(Color.argb(70, 255, 255, 255));
        c.drawArc(r, 225, 90, false, stroke);
        stroke.setColor(practice ? Color.rgb(120, 255, 140) : Color.rgb(255, 210, 63));
        c.drawArc(r, 225, 90 * e.progress(), false, stroke);
        text.setTextSize(W * 0.04f);
        text.setColor(Color.WHITE);
        c.drawText(Math.round(e.progress() * 100) + "%", cx, H * 0.1f, text);
        if (startBannerT > 0) {
            text.setTextSize(W * 0.06f);
            text.setColor(alpha(Color.WHITE, (int) (255 * Math.min(1, startBannerT))));
            c.drawText((practice ? "연습 · " : "") + "시도 " + attempts, cx, H * 0.28f, text);
            if (attempts == 1) {
                text.setTextSize(W * 0.038f);
                c.drawText(Defs.MODE_HELP[e.mode], cx, H * 0.35f, text);
            }
        }
        if (e.bezelGroup > 0 && e.time <= e.bezelUntil) {
            float left = e.bezelUntil - e.time;
            text.setTextSize(W * 0.05f);
            text.setColor(Color.rgb(61, 232, 255));
            c.drawText("⟳ 베젤을 돌려요! " + String.format("%.1f", left), cx, H * 0.9f, text);
            stroke.setColor(Color.rgb(61, 232, 255));
            stroke.setStrokeWidth(W * 0.02f);
            c.drawArc(r, -60, 120 * Math.min(1, left / 6f), false, stroke);
        }
    }

    // ---------- 결과 ----------

    private void drawResult(Canvas c) {
        c.drawColor(Color.rgb(12, 16, 40));
        glow(c, cx, cy, W * 0.6f, alpha(0xFFFFD23F, 60));
        float k = Math.min(1, resultT / 0.4f);
        drawPlayerIcon(c, cx, H * 0.2f, W * 0.06f * (0.5f + 0.5f * k), resultT * 200, Defs.M_JUMP);
        text.setTextSize(W * 0.1f);
        text.setColor(Color.rgb(255, 210, 63));
        c.drawText("레벨 완료!", cx, H * 0.38f, text);
        text.setTextSize(W * 0.05f);
        text.setColor(Color.WHITE);
        c.drawText(level.name, cx, H * 0.47f, text);
        text.setTextSize(W * 0.045f);
        text.setColor(Color.LTGRAY);
        c.drawText("시도 " + attempts + "회 · 점프 " + eng.jumps + "번", cx, H * 0.57f, text);
        int sec = (int) eng.time;
        c.drawText("플레이 시간 " + sec / 60 + ":" + String.format("%02d", sec % 60), cx, H * 0.64f, text);
        if (officialIdx >= 0 && !fromEditor) {
            text.setColor(Color.rgb(255, 210, 63));
            c.drawText("★ 별 " + stars() + "개", cx, H * 0.71f, text);
        }
        text.setTextSize(W * 0.04f);
        text.setColor(Color.GRAY);
        c.drawText(fromEditor ? "탭: 에디터로" : "탭: 다시 · 뒤로: 나가기", cx, H * 0.84f, text);
    }

    // ---------- 에디터 ----------

    private void drawEditor(Canvas c) {
        int bgCol = Defs.BG_COLORS[level.bg];
        vs = edScale();
        vx0 = edCamX;
        vy0 = H * 0.75f + edCamY * vs;
        drawBackground(c, bgCol, edCamX);
        // 그리드
        stroke.setStrokeWidth(1);
        stroke.setColor(Color.argb(40, 255, 255, 255));
        for (float x = (float) Math.floor(edCamX); x < edCamX + W / vs + 1; x++) c.drawLine(sx(x), 0, sx(x), H, stroke);
        for (float y = (float) Math.floor(edCamY - 1); y < edCamY + H / vs + 1; y++) c.drawLine(0, sy(y), W, sy(y), stroke);
        // 박자 가이드 (시작 속도 기준)
        float beatBlocks = 60f / level.bpm * Defs.SPEEDS[level.startSpeed];
        stroke.setStrokeWidth(2);
        int b0 = (int) Math.floor(edCamX / beatBlocks);
        for (int b = Math.max(0, b0); b * beatBlocks < edCamX + W / vs; b++) {
            stroke.setColor(b % 4 == 0 ? Color.argb(110, 255, 210, 63) : Color.argb(45, 255, 210, 63));
            c.drawLine(sx(b * beatBlocks), 0, sx(b * beatBlocks), H, stroke);
        }
        drawGround(c, bgCol);
        // 시작선
        stroke.setColor(Color.rgb(120, 255, 140));
        stroke.setStrokeWidth(3);
        c.drawLine(sx(0), 0, sx(0), H, stroke);
        for (Level.Obj o : level.objs) {
            if (o.x < edCamX - 3 || o.x > edCamX + W / vs + 2) continue;
            drawObj(c, o, o.x, o.y, o.rot * 90, o.hidden ? 0.35f : 1f, true, bgCol);
        }
        if (selected != null && level.objs.contains(selected)) {
            float h = Defs.height(selected.type);
            float y0 = Defs.isPortal(selected.type) ? selected.y - 1 : selected.y;
            stroke.setColor(Color.rgb(61, 232, 255));
            stroke.setStrokeWidth(W * 0.01f);
            c.drawRect(sx(selected.x), sy(y0 + h), sx(selected.x + 1), sy(y0), stroke);
        }
        // 끝 지점
        stroke.setColor(Color.rgb(255, 90, 120));
        stroke.setStrokeWidth(3);
        c.drawLine(sx(level.endX()), 0, sx(level.endX()), H, stroke);

        // UI 버튼
        float br = W * 0.075f;
        drawButton(c, cx, H * 0.1f, br * 1.25f, TOOL_NAMES[tool], tool == T_PLACE ? Color.rgb(120, 255, 140) : tool == T_SELECT ? Color.rgb(61, 232, 255) : Color.rgb(255, 90, 120));
        drawButton(c, W * 0.09f, cy, br, "↶", Color.rgb(200, 200, 220));
        drawButton(c, W * 0.91f, cy, br, "▶", Color.rgb(120, 255, 140));
        drawButton(c, cx, H * 0.91f, br, "≡", Color.rgb(255, 210, 63));
        if (tool == T_PLACE) {
            // 팔레트 미리보기
            int type = Defs.PALETTE[palIdx];
            float px = cx, py = H * 0.22f;
            p.setColor(Color.argb(170, 0, 0, 0));
            r.set(cx - W * 0.25f, py - W * 0.05f, cx + W * 0.25f, py + W * 0.05f);
            c.drawRoundRect(r, W * 0.05f, W * 0.05f, p);
            text.setTextSize(W * 0.042f);
            text.setColor(Color.WHITE);
            c.drawText(Defs.name(type), px, py + W * 0.015f, text);
        }
        if (edMsgT > 0) {
            text.setTextSize(W * 0.038f);
            text.setColor(alpha(Color.WHITE, (int) (255 * Math.min(1, edMsgT))));
            p.setColor(alpha(Color.BLACK, (int) (150 * Math.min(1, edMsgT))));
            float tw = text.measureText(edMsg) / 2 + W * 0.03f;
            r.set(cx - tw, H * 0.74f, cx + tw, H * 0.8f);
            c.drawRoundRect(r, W * 0.03f, W * 0.03f, p);
            c.drawText(edMsg, cx, H * 0.785f, text);
        }
        text.setTextSize(W * 0.032f);
        text.setColor(Color.argb(160, 255, 255, 255));
        c.drawText(level.objs.size() + "개 · X " + Math.round(edCamX + 4), cx, H * 0.83f, text);
    }

    private void drawButton(Canvas c, float x, float y, float rad, String label, int color) {
        p.setColor(Color.argb(190, 10, 12, 24));
        c.drawCircle(x, y, rad, p);
        stroke.setColor(color);
        stroke.setStrokeWidth(W * 0.007f);
        c.drawCircle(x, y, rad, stroke);
        text.setTextSize(label.length() > 1 ? rad * 0.62f : rad * 0.95f);
        text.setColor(color);
        c.drawText(label, x, y + text.getTextSize() * 0.35f, text);
    }
}
