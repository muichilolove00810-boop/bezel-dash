package com.grara.bezeldash.game;

import static com.grara.bezeldash.game.Defs.*;

/** 공식 레벨. 모두 자동 봇으로 클리어 가능 여부를 검증한다. */
public final class OfficialLevels {
    private OfficialLevels() {
    }

    public static final String[] NAMES = {"첫걸음", "네온 제트", "거꾸로 세상", "지그재그 폭풍", "베젤 엔진",
            "작은 거인", "거대 제트", "반전 미니", "최종 폭주",
            "트리거 쇼", "별빛 계단", "스텝 리듬", "중력 미로", "베젤 마스터"};
    public static final String[] DIFF = {"쉬움", "보통", "보통", "어려움", "보통", "보통", "보통", "어려움", "지옥",
            "쉬움", "보통", "보통", "어려움", "어려움"};

    public static Level get(int i) {
        Level l;
        switch (i) {
            case 0: l = first(); break;
            case 1: l = jet(); break;
            case 2: l = flip(); break;
            case 3: l = zig(); break;
            case 4: l = bezel(); break;
            case 5: l = giant(); break;
            case 6: l = bigJet(); break;
            case 7: l = flipMini(); break;
            case 8: l = finale(); break;
            case 9: l = triggerShow(); break;
            case 10: l = stairs(); break;
            case 11: l = stepRhythm(); break;
            case 12: l = gravMaze(); break;
            default: l = bezelMaster();
        }
        l.name = NAMES[i];
        l.official = true;
        return l;
    }

    public static int count() {
        return NAMES.length;
    }

    // ---------- 헬퍼 ----------

    private static void blocks(Level l, int x, int y, int w, int h) {
        for (int i = 0; i < w; i++) for (int j = 0; j < h; j++) l.add(BLOCK, x + i, y + j);
    }

    private static void spikes(Level l, int x, int n) {
        for (int i = 0; i < n; i++) l.add(SPIKE, x + i, 0);
    }

    private static Level.Obj portal(Level l, int type, int x, int y, int v) {
        Level.Obj o = l.add(type, x, y);
        o.p[0] = v;
        return o;
    }

    private static Level.Obj trig(Level l, int type, int x, int... ps) {
        Level.Obj o = l.add(type, x, 6);
        for (int i = 0; i < ps.length && i < o.p.length; i++) o.p[i] = ps[i];
        return o;
    }

    private static void deco(Level l, int x, int y, int shape, int group) {
        Level.Obj o = l.add(DECO, x, y);
        o.p[0] = shape;
        o.group = group;
    }

    // ---------- 1. 첫걸음 ----------
    private static Level first() {
        Level l = new Level();
        l.style = 0; l.bpm = 124; l.seed = 11; l.bg = 0;
        for (int x = 4; x < 110; x += 9) deco(l, x, 7 + (x % 3), 0, 0);
        spikes(l, 14, 1);
        spikes(l, 22, 1);
        trig(l, T_BG, 26, 1, 10);
        spikes(l, 30, 2);
        blocks(l, 38, 0, 2, 1);
        spikes(l, 43, 1);
        blocks(l, 48, 0, 1, 1);
        blocks(l, 51, 0, 1, 2);
        blocks(l, 54, 0, 1, 2);
        l.add(SMALL_SPIKE, 57, 0);
        l.add(SMALL_SPIKE, 58, 0);
        trig(l, T_FLASH, 62, 6, 3);
        l.add(PAD, 64, 0);
        spikes(l, 66, 2);
        trig(l, T_BG, 72, 6, 10);
        spikes(l, 76, 1);
        l.add(SMALL_SPIKE, 81, 0);
        spikes(l, 86, 2);
        blocks(l, 92, 0, 3, 1);
        spikes(l, 95, 2);
        trig(l, T_SHAKE, 100, 3, 6);
        return l;
    }

    // ---------- 2. 네온 제트 ----------
    private static Level jet() {
        Level l = new Level();
        l.style = 3; l.bpm = 138; l.seed = 404; l.bg = 1;
        spikes(l, 12, 1);
        spikes(l, 20, 2);
        trig(l, T_ZOOM, 26, 9, 8);
        portal(l, P_MODE, 30, 1, M_JET);
        trig(l, T_FLASH, 30, 1, 4);
        // 슬라롬
        blocks(l, 40, 0, 2, 5);
        l.add(SPIKE, 40, 5); l.add(SPIKE, 41, 5);
        blocks(l, 50, 8, 2, 6);
        Level.Obj s1 = l.add(SPIKE, 50, 7); s1.rot = 2;
        Level.Obj s2 = l.add(SPIKE, 51, 7); s2.rot = 2;
        blocks(l, 60, 0, 2, 6);
        blocks(l, 70, 6, 2, 8);
        l.add(SAW, 76, 3);
        blocks(l, 80, 0, 2, 4);
        trig(l, T_ZOOM, 84, 10, 8);
        portal(l, P_MODE, 88, 1, M_JUMP);
        trig(l, T_BG, 88, 2, 10);
        spikes(l, 96, 1);
        // 오브 구간
        spikes(l, 104, 5);
        l.add(ORB, 105, 2);
        spikes(l, 114, 1);
        blocks(l, 120, 0, 2, 2);
        spikes(l, 122, 2);
        l.add(ORB, 122, 3);
        return l;
    }

    // ---------- 3. 거꾸로 세상 ----------
    private static Level flip() {
        Level l = new Level();
        l.style = 2; l.bpm = 100; l.seed = 202; l.bg = 2;
        spikes(l, 12, 1);
        portal(l, P_MODE, 18, 1, M_FLIP);
        trig(l, T_BG, 18, 1, 6);
        // 바닥/천장 가시를 번갈아
        spikes(l, 28, 2);
        for (int i = 0; i < 2; i++) { Level.Obj o = l.add(SPIKE, 38 + i, 13); o.rot = 2; }
        spikes(l, 48, 2);
        for (int i = 0; i < 2; i++) { Level.Obj o = l.add(SPIKE, 58 + i, 13); o.rot = 2; }
        portal(l, P_MODE, 66, 1, M_JUMP);
        portal(l, P_GRAV, 66, 5, 0);
        portal(l, P_GRAV, 66, 10, 0);
        trig(l, T_FLASH, 66, 2, 4);
        spikes(l, 74, 1);
        portal(l, P_GRAV, 80, 1, 1);
        // 뒤집힌 점프: 천장 가시
        for (int x : new int[]{88, 96}) { Level.Obj o = l.add(SPIKE, x, 13); o.rot = 2; }
        portal(l, P_GRAV, 104, 10, 0);
        portal(l, P_GRAV, 104, 5, 0);
        trig(l, T_BG, 104, 0, 6);
        spikes(l, 112, 2);
        l.add(G_ORB, 120, 2);
        for (int i = 0; i < 6; i++) l.add(SPIKE, 120 + i, 0);
        portal(l, P_GRAV, 130, 10, 0);
        spikes(l, 138, 1);
        return l;
    }

    // ---------- 4. 지그재그 폭풍 ----------
    private static Level zig() {
        Level l = new Level();
        l.style = 4; l.bpm = 172; l.seed = 6006; l.bg = 3;
        l.startSpeed = 1;
        spikes(l, 12, 1);
        portal(l, P_MODE, 18, 1, M_ZIG);
        trig(l, T_SHAKE, 18, 3, 4);
        // 지그재그: 위아래 톱니
        l.add(SAW, 28, 1);
        l.add(SAW, 34, 6);
        l.add(SAW, 40, 1);
        l.add(SAW, 40, 2);
        l.add(SAW, 46, 7);
        l.add(SAW, 46, 6);
        portal(l, P_SPEED, 52, 2, 2);
        trig(l, T_FLASH, 52, 4, 3);
        l.add(SAW, 60, 1);
        l.add(SAW, 66, 7);
        l.add(SAW, 72, 2);
        portal(l, P_MODE, 80, 1, M_STEP);
        portal(l, P_SPEED, 80, 4, 1);
        trig(l, T_BG, 80, 4, 8);
        // 스텝: 낮은 가시 + 높은 톱니
        spikes(l, 88, 3);
        l.add(SAW, 96, 6);
        spikes(l, 100, 4);
        l.add(SAW, 106, 7);
        spikes(l, 110, 3);
        portal(l, P_MODE, 118, 1, M_JUMP);
        trig(l, T_SHAKE, 118, 5, 5);
        spikes(l, 126, 2);
        spikes(l, 134, 1);
        return l;
    }

    private static Level.Obj size(Level l, int x, int v) {
        return portal(l, P_SIZE, x, 1, v);
    }

    private static void ceilSpikes(Level l, int x, int n) {
        for (int i = 0; i < n; i++) { Level.Obj o = l.add(SPIKE, x + i, 13); o.rot = 2; }
    }

    // ---------- 6. 작은 거인 ----------
    private static Level giant() {
        Level l = new Level();
        l.style = 0; l.bpm = 128; l.seed = 61; l.bg = 5;
        spikes(l, 12, 1);
        size(l, 18, 0);
        trig(l, T_FLASH, 18, 2, 3);
        // 작은 몸: 낮은 터널 + 짧은 점프
        blocks(l, 24, 2, 10, 1);
        spikes(l, 27, 1);
        spikes(l, 31, 1);
        spikes(l, 38, 1);
        l.add(SMALL_SPIKE, 43, 0);
        l.add(SMALL_SPIKE, 44, 0);
        blocks(l, 48, 0, 1, 1);
        size(l, 54, 2);
        trig(l, T_ZOOM, 54, 8, 6);
        trig(l, T_BG, 54, 4, 8);
        // 큰 몸: 넓은 점프, 큰 히트박스
        spikes(l, 61, 1);
        spikes(l, 69, 2);
        blocks(l, 77, 0, 1, 1);
        blocks(l, 78, 0, 1, 2);
        spikes(l, 84, 1);
        size(l, 90, 1);
        trig(l, T_ZOOM, 90, 10, 6);
        spikes(l, 96, 1);
        spikes(l, 102, 2);
        return l;
    }

    // ---------- 7. 거대 제트 ----------
    private static Level bigJet() {
        Level l = new Level();
        l.style = 7; l.bpm = 150; l.seed = 77; l.bg = 1;
        spikes(l, 10, 1);
        portal(l, P_MODE, 16, 1, M_JET);
        size(l, 16, 5 > 0 ? 2 : 2);
        trig(l, T_ZOOM, 16, 8, 8);
        // 큰 제트: 넓은 기둥 사이
        blocks(l, 26, 0, 2, 4);
        blocks(l, 36, 9, 2, 5);
        blocks(l, 46, 0, 2, 5);
        blocks(l, 56, 8, 2, 6);
        size(l, 64, 0);
        trig(l, T_FLASH, 64, 2, 3);
        trig(l, T_ZOOM, 64, 11, 6);
        // 작은 제트: 좁은 통로 (y 4~7)
        blocks(l, 70, 0, 14, 4);
        blocks(l, 70, 7, 14, 7);
        l.add(SAW, 76, 4);
        l.add(SAW, 81, 6);
        size(l, 88, 1);
        portal(l, P_MODE, 88, 1, M_JUMP);
        trig(l, T_ZOOM, 88, 10, 6);
        spikes(l, 96, 1);
        spikes(l, 103, 2);
        return l;
    }

    // ---------- 8. 반전 미니 ----------
    private static Level flipMini() {
        Level l = new Level();
        l.style = 6; l.bpm = 140; l.seed = 88; l.bg = 2;
        spikes(l, 10, 1);
        portal(l, P_MODE, 16, 1, M_FLIP);
        size(l, 16, 0);
        trig(l, T_SHAKE, 16, 3, 4);
        spikes(l, 26, 2);
        ceilSpikes(l, 34, 2);
        spikes(l, 42, 2);
        ceilSpikes(l, 50, 2);
        size(l, 58, 2);
        trig(l, T_BG, 58, 3, 6);
        spikes(l, 66, 1);
        ceilSpikes(l, 76, 1);
        spikes(l, 86, 1);
        l.add(G_ORB, 94, 2);
        for (int i = 0; i < 5; i++) l.add(SPIKE, 94 + i, 0);
        size(l, 104, 1);
        portal(l, P_MODE, 104, 1, M_JUMP);
        portal(l, P_GRAV, 104, 10, 0);
        portal(l, P_GRAV, 104, 5, 0);
        spikes(l, 112, 1);
        return l;
    }

    // ---------- 9. 최종 폭주 ----------
    private static Level finale() {
        Level l = new Level();
        l.style = 4; l.bpm = 174; l.seed = 99; l.bg = 7;
        l.startSpeed = 2;
        spikes(l, 14, 1);
        spikes(l, 22, 2);
        size(l, 28, 0);
        spikes(l, 34, 1);
        spikes(l, 40, 1);
        size(l, 46, 1);
        portal(l, P_MODE, 46, 1, M_JET);
        trig(l, T_FLASH, 46, 3, 3);
        blocks(l, 56, 0, 2, 6);
        blocks(l, 66, 7, 2, 7);
        l.add(SAW, 74, 2);
        portal(l, P_MODE, 80, 1, M_ZIG);
        trig(l, T_SHAKE, 80, 4, 4);
        l.add(SAW, 90, 1);
        l.add(SAW, 97, 6);
        l.add(SAW, 104, 1);
        portal(l, P_MODE, 112, 1, M_STEP);
        spikes(l, 120, 3);
        l.add(SAW, 128, 7);
        spikes(l, 132, 3);
        portal(l, P_MODE, 140, 1, M_JUMP);
        size(l, 140, 2);
        trig(l, T_BG, 140, 3, 6);
        spikes(l, 148, 1);
        spikes(l, 156, 2);
        size(l, 162, 1);
        portal(l, P_SPEED, 162, 4, 3);
        spikes(l, 172, 1);
        spikes(l, 182, 2);
        trig(l, T_FLASH, 190, 0, 5);
        return l;
    }

    // ---------- 10. 트리거 쇼 (트리거로 길이 열리고 닫힘) ----------
    private static Level triggerShow() {
        Level l = new Level();
        l.style = 7; l.bpm = 120; l.seed = 1010; l.bg = 0;
        // 벽(그룹 1)이 이동 트리거로 위로 비켜남
        trig(l, T_MOVE, 14, 1, 0, 8, 10, 2);
        for (int j = 0; j < 3; j++) { Level.Obj o = l.add(BLOCK, 26, j); o.group = 1; }
        trig(l, T_BG, 14, 1, 8);
        spikes(l, 32, 1);
        // 숨겨진 다리(그룹 2)가 켜기 트리거로 나타남
        for (int i = 0; i < 6; i++) { Level.Obj o = l.add(BLOCK, 40 + i, 0); o.group = 2; o.hidden = true; }
        trig(l, T_TOGGLE, 34, 2, 1);
        trig(l, T_FLASH, 34, 6, 3);
        spikes(l, 40, 6);
        // 회전하는 장식 + 투명해지는 장식
        for (int i = 0; i < 5; i++) deco(l, 50 + i * 3, 5, 0, 3);
        trig(l, T_ROTATE, 48, 3, 24, 30);
        trig(l, T_ALPHA, 60, 3, 0, 15);
        spikes(l, 54, 1);
        spikes(l, 62, 1);
        // 가시 벽(그룹 4)이 내려와 길을 막지만 위를 지나감
        trig(l, T_SHAKE, 68, 4, 6);
        trig(l, T_ZOOM, 68, 8, 6);
        blocks(l, 74, 0, 3, 1);
        spikes(l, 80, 1);
        trig(l, T_ZOOM, 84, 10, 6);
        trig(l, T_BG, 84, 6, 8);
        spikes(l, 90, 2);
        trig(l, T_HAPTIC, 98, 6, 1);
        return l;
    }

    // ---------- 11. 별빛 계단 ----------
    private static Level stairs() {
        Level l = new Level();
        l.style = 3; l.bpm = 132; l.seed = 1111; l.bg = 1;
        for (int x = 6; x < 120; x += 7) deco(l, x, 8 + (x % 4), 0, 0);
        spikes(l, 12, 1);
        blocks(l, 20, 0, 2, 1);
        blocks(l, 23, 0, 2, 2);
        blocks(l, 26, 0, 2, 3);
        spikes(l, 28, 6);
        blocks(l, 30, 0, 3, 2);
        spikes(l, 36, 1);
        l.add(PAD, 42, 0);
        blocks(l, 46, 0, 3, 3);
        l.add(SMALL_SPIKE, 49, 0);
        l.add(SMALL_SPIKE, 50, 0);
        spikes(l, 56, 2);
        l.add(ORB, 57, 2);
        trig(l, T_FLASH, 62, 1, 3);
        blocks(l, 66, 0, 1, 1);
        blocks(l, 69, 0, 1, 2);
        spikes(l, 70, 3);
        blocks(l, 73, 0, 2, 2);
        spikes(l, 80, 1);
        spikes(l, 86, 2);
        l.add(PAD, 92, 0);
        spikes(l, 94, 2);
        blocks(l, 97, 0, 3, 1);
        spikes(l, 100, 2);
        spikes(l, 108, 1);
        return l;
    }

    // ---------- 12. 스텝 리듬 ----------
    private static Level stepRhythm() {
        Level l = new Level();
        l.style = 5; l.bpm = 150; l.seed = 1212; l.bg = 4;
        spikes(l, 10, 1);
        portal(l, P_MODE, 16, 1, M_STEP);
        trig(l, T_BG, 16, 4, 6);
        spikes(l, 24, 4);
        l.add(SAW, 32, 6);
        spikes(l, 34, 4);
        blocks(l, 42, 5, 4, 1);
        spikes(l, 42, 4);
        size(l, 50, 0);
        l.add(SAW, 56, 4);
        spikes(l, 58, 5);
        l.add(SAW, 66, 2);
        size(l, 72, 2);
        trig(l, T_ZOOM, 72, 8, 6);
        spikes(l, 78, 3);
        l.add(SAW, 86, 8);
        spikes(l, 88, 3);
        size(l, 96, 1);
        portal(l, P_MODE, 96, 1, M_JUMP);
        trig(l, T_ZOOM, 96, 10, 6);
        spikes(l, 104, 1);
        spikes(l, 110, 2);
        return l;
    }

    // ---------- 13. 중력 미로 ----------
    private static Level gravMaze() {
        Level l = new Level();
        l.style = 6; l.bpm = 140; l.seed = 1313; l.bg = 3;
        spikes(l, 12, 1);
        portal(l, P_GRAV, 18, 1, 1);
        trig(l, T_FLASH, 18, 4, 3);
        ceilSpikes(l, 26, 1);
        ceilSpikes(l, 33, 2);
        // 천장 블록 계단
        for (int i = 0; i < 3; i++) blocks(l, 40 + i * 3, 13 - i, 2, 1 + i);
        portal(l, P_GRAV, 52, 8, 0);
        portal(l, P_GRAV, 52, 4, 0);
        portal(l, P_GRAV, 52, 1, 0);
        spikes(l, 60, 2);
        l.add(G_ORB, 66, 2);
        for (int i = 0; i < 4; i++) l.add(SPIKE, 66 + i, 0);
        ceilSpikes(l, 76, 2);
        portal(l, P_GRAV, 84, 10, 0);
        portal(l, P_GRAV, 84, 6, 0);
        trig(l, T_BG, 84, 2, 6);
        spikes(l, 92, 1);
        spikes(l, 100, 2);
        return l;
    }

    // ---------- 14. 베젤 마스터 ----------
    private static Level bezelMaster() {
        Level l = new Level();
        l.style = 1; l.bpm = 132; l.seed = 1414; l.bg = 6;
        spikes(l, 10, 1);
        // 1) 벽 올리기
        trig(l, T_BEZEL, 14, 1, 2, 50);
        trig(l, T_HAPTIC, 14, 8, 1);
        for (int j = 0; j < 3; j++) { Level.Obj o = l.add(BLOCK, 30, j); o.group = 1; }
        spikes(l, 36, 1);
        // 2) 제트 + 문 올리기
        portal(l, P_MODE, 40, 1, M_JET);
        trig(l, T_BEZEL, 40, 2, 2, 50);
        trig(l, T_HAPTIC, 40, 8, 1);
        blocks(l, 50, 0, 2, 4);
        for (int j = 4; j < 14; j++) { Level.Obj o = l.add(BLOCK, 58, j); o.group = 2; }
        for (int j = 4; j < 14; j++) { Level.Obj o = l.add(BLOCK, 59, j); o.group = 2; }
        portal(l, P_MODE, 66, 1, M_JUMP);
        // 3) 다리 올리기
        trig(l, T_BEZEL, 68, 3, 2, 50);
        trig(l, T_HAPTIC, 68, 8, 1);
        for (int i = 0; i < 8; i++) { Level.Obj o = l.add(BLOCK, 80 + i, -3); o.group = 3; }
        spikes(l, 80, 8);
        trig(l, T_ZOOM, 78, 8, 6);
        trig(l, T_ZOOM, 92, 10, 6);
        spikes(l, 98, 1);
        spikes(l, 104, 2);
        return l;
    }

    // ---------- 5. 베젤 엔진 ----------
    private static Level bezel() {
        Level l = new Level();
        l.style = 7; l.bpm = 150; l.seed = 707; l.bg = 6;
        spikes(l, 12, 1);
        // 베젤로 벽 들어올리기 (그룹 1)
        trig(l, T_BEZEL, 18, 1, 2, 60);   // 1칸 = 1블록, 6초
        trig(l, T_HAPTIC, 18, 8, 1);
        for (int j = 0; j < 3; j++) { Level.Obj o = l.add(BLOCK, 40, j); o.group = 1; }
        for (int j = 0; j < 3; j++) { Level.Obj o = l.add(BLOCK, 41, j); o.group = 1; }
        deco(l, 40, 4, 3, 1);
        spikes(l, 48, 1);
        // 베젤로 다리 올리기 (그룹 2): 가시 구덩이 위
        trig(l, T_BEZEL, 52, 2, 2, 60);
        trig(l, T_HAPTIC, 52, 8, 1);
        for (int i = 0; i < 9; i++) { Level.Obj o = l.add(BLOCK, 66 + i, -3); o.group = 2; }
        spikes(l, 66, 9);
        trig(l, T_ZOOM, 64, 8, 6);
        trig(l, T_ZOOM, 78, 10, 6);
        // 회전 장식 + 투명도
        for (int i = 0; i < 4; i++) deco(l, 84 + i * 3, 6, 0, 3);
        trig(l, T_ROTATE, 80, 3, 24, 40);
        trig(l, T_ALPHA, 92, 3, 2, 10);
        spikes(l, 88, 1);
        blocks(l, 94, 0, 2, 1);
        spikes(l, 96, 2);
        trig(l, T_BG, 100, 5, 10);
        spikes(l, 104, 1);
        spikes(l, 110, 2);
        return l;
    }
}
