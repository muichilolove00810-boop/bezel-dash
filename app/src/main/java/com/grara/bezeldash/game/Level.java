package com.grara.bezeldash.game;

import java.util.ArrayList;

/** 레벨 데이터 (에디터와 게임이 같은 구조를 사용). */
public final class Level {
    public static final int FORMAT_VERSION = 1;

    public static final class Obj {
        public int type;
        public int x, y;        // 칸 좌표 (왼쪽 아래)
        public int rot;         // 0,1,2,3 = 0/90/180/270도
        public int group;       // 0 = 없음
        public boolean hidden;  // 시작 시 꺼짐
        public int[] p;         // 파라미터

        public Obj(int type, int x, int y) {
            this.type = type;
            this.x = x;
            this.y = y;
            Defs.Param[] ps = Defs.params(type);
            p = new int[ps.length];
            for (int i = 0; i < ps.length; i++) p[i] = ps[i].def;
        }

        public Obj copy() {
            Obj o = new Obj(type, x, y);
            o.rot = rot;
            o.group = group;
            o.hidden = hidden;
            o.p = p.clone();
            return o;
        }

        public int param(int i) {
            return i < p.length ? p[i] : 0;
        }
    }

    public String name = "새 레벨";
    public int style = 0, bpm = 124, seed = 1;
    public int startMode = Defs.M_JUMP, startSpeed = 1, bg = 0, startSize = 1;
    public boolean official;
    public final ArrayList<Obj> objs = new ArrayList<>();

    public Obj add(int type, int x, int y) {
        Obj o = new Obj(type, x, y);
        objs.add(o);
        return o;
    }

    public int maxX() {
        int m = 0;
        for (Obj o : objs) m = Math.max(m, o.x);
        return m;
    }

    /** 끝 지점 (마지막 오브젝트 + 여유) */
    public float endX() {
        return Math.max(30, maxX() + 8);
    }

    public Level copy() {
        Level l = new Level();
        l.name = name;
        l.style = style;
        l.bpm = bpm;
        l.seed = seed;
        l.startMode = startMode;
        l.startSpeed = startSpeed;
        l.bg = bg;
        l.startSize = startSize;
        for (Obj o : objs) l.objs.add(o.copy());
        return l;
    }
}
