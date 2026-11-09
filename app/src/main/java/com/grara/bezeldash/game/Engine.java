package com.grara.bezeldash.game;

import java.util.ArrayList;
import java.util.List;

/**
 * 게임 실행 엔진 (안드로이드 의존 없음 → PC 에서 봇으로 검증 가능).
 * 좌표 단위는 블록, y 는 위쪽이 +. 바닥 y=0, 천장 y=CEIL.
 */
public final class Engine {
    public static final float DT = 1f / 240f;
    public static final float CEIL = 14f;

    // 물리 상수
    static final float G_JUMP = 100f, V_JUMP = 19.5f, V_PAD = 27f, V_ORB = 19f;
    static final float G_FLIP = 70f, G_STEP = 55f, V_STEP = 13f, A_JET = 50f, V_JET = 12f;
    static final float MAX_FALL = 26f;

    public final Level level;
    public final Level.Obj[] objs;
    public final float endX;
    private final boolean hasGroups;

    // 플레이어
    public float px, py, vy, rotDeg;
    public int mode, grav = 1, speedIdx, sizeIdx = 1;
    public boolean grounded, holding, dead, finished;
    public float tapBuffer;
    public float time;
    public int jumps;

    // 오브젝트 상태
    final boolean[] used;
    final float[] padCool;

    // 그룹 상태 (0..99)
    public final float[] gdx = new float[100], gdy = new float[100], grot = new float[100], galpha = new float[100];
    public final float[] gbz = new float[100], gbzTarget = new float[100];
    public final int[] gon = new int[100];

    // 연출 상태
    public int bgFrom, bgTo;
    public float bgT = 1, bgDur = 0;
    public int flashColor;
    public float flashT, flashDur;
    public float shakeAmp, shakeT;
    public float zoom = 1, zoomFrom = 1, zoomTo = 1, zoomT = 1, zoomDur;
    public int bezelGroup = -1;
    public float bezelStep, bezelUntil;

    /** 렌더러가 가져가는 진동 요청 (ms, 패턴) */
    public int hapticMs, hapticPattern;
    /** 이벤트 플래그 (렌더러용) */
    public boolean evJump, evOrb, evPortal, evPad;

    private final ArrayList<Anim> anims = new ArrayList<>();

    private static final class Anim {
        int kind, group, easing;
        float a, b, t, dur, last;
    }

    public Engine(Level level) {
        this.level = level;
        objs = level.objs.toArray(new Level.Obj[0]);
        java.util.Arrays.sort(objs, (o1, o2) -> Integer.compare(o1.x, o2.x));
        used = new boolean[objs.length];
        padCool = new float[objs.length];
        endX = level.endX();
        boolean g = false;
        for (Level.Obj o : objs) if (o.group > 0) g = true;
        hasGroups = g;
        reset();
    }

    public void reset() {
        px = 0;
        py = 0.4f * Defs.SIZES[level.startSize];
        vy = 0;
        rotDeg = 0;
        mode = level.startMode;
        grav = 1;
        speedIdx = level.startSpeed;
        sizeIdx = level.startSize;
        grounded = true;
        holding = false;
        dead = finished = false;
        tapBuffer = 0;
        time = 0;
        jumps = 0;
        java.util.Arrays.fill(used, false);
        java.util.Arrays.fill(padCool, 0);
        java.util.Arrays.fill(gdx, 0);
        java.util.Arrays.fill(gdy, 0);
        java.util.Arrays.fill(grot, 0);
        java.util.Arrays.fill(galpha, 1);
        java.util.Arrays.fill(gbz, 0);
        java.util.Arrays.fill(gbzTarget, 0);
        java.util.Arrays.fill(gon, -1);
        anims.clear();
        bgFrom = bgTo = level.bg;
        bgT = 1;
        bgDur = 0;
        flashT = 0;
        shakeT = 0;
        zoom = zoomFrom = zoomTo = 1;
        zoomT = 1;
        bezelGroup = -1;
        hapticMs = 0;
    }

    public float half() {
        return (mode == Defs.M_ZIG ? 0.25f : 0.4f) * Defs.SIZES[sizeIdx];
    }

    public float speed() {
        return Defs.SPEEDS[speedIdx];
    }

    // ---------------- 입력 ----------------

    public void press() {
        holding = true;
        tapBuffer = 0.12f;
    }

    public void release() {
        holding = false;
    }

    /** 베젤 한 칸 (+1 시계 / -1 반시계) */
    public void bezel(int dir) {
        if (bezelGroup > 0 && time <= bezelUntil) {
            gbzTarget[bezelGroup] = Math.max(-12, Math.min(12, gbzTarget[bezelGroup] + dir * bezelStep));
        }
    }

    // ---------------- 오브젝트 위치 ----------------

    public float ox(Level.Obj o) {
        return o.x + (o.group > 0 ? gdx[o.group] : 0);
    }

    public float oy(Level.Obj o) {
        return o.y + (o.group > 0 ? gdy[o.group] + gbz[o.group] : 0);
    }

    public boolean visible(Level.Obj o) {
        if (o.group > 0 && gon[o.group] >= 0) return gon[o.group] == 1;
        return !o.hidden;
    }

    public float alpha(Level.Obj o) {
        return o.group > 0 ? galpha[o.group] : 1f;
    }

    public float rotation(Level.Obj o) {
        return o.rot * 90 + (o.group > 0 ? grot[o.group] : 0);
    }

    // ---------------- 진행 ----------------

    public void step() {
        if (dead || finished) return;
        evJump = evOrb = evPortal = evPad = false;
        float dt = DT;
        time += dt;
        tapBuffer = Math.max(0, tapBuffer - dt);
        float h = half();
        float prevY = py;

        px += speed() * dt;

        switch (mode) {
            case Defs.M_JUMP:
                vy -= grav * G_JUMP * dt;
                if (grounded && (holding || tapBuffer > 0)) {
                    vy = grav * V_JUMP * Defs.SIZE_JUMP[sizeIdx];
                    grounded = false;
                    tapBuffer = 0;
                    jumps++;
                    evJump = true;
                }
                break;
            case Defs.M_JET:
                vy += (holding ? 1 : -1) * grav * A_JET * dt;
                vy = clamp(vy, -V_JET, V_JET);
                break;
            case Defs.M_FLIP:
                vy -= grav * G_FLIP * dt;
                if (grounded && tapBuffer > 0) {
                    grav = -grav;
                    vy = -grav * 6f;
                    grounded = false;
                    tapBuffer = 0;
                    jumps++;
                    evJump = true;
                }
                break;
            case Defs.M_STEP:
                vy -= grav * G_STEP * dt;
                if (tapBuffer > 0) {
                    vy = grav * V_STEP * Defs.SIZE_JUMP[sizeIdx];
                    tapBuffer = 0;
                    jumps++;
                    evJump = true;
                }
                break;
            case Defs.M_ZIG:
                vy = (holding ? 1 : -1) * grav * speed();
                break;
        }
        vy = clamp(vy, -MAX_FALL, MAX_FALL);
        py += vy * dt;
        grounded = false;

        // 바닥 / 천장
        if (py - h < 0) {
            py = h;
            surface(true);
        }
        if (py + h > CEIL) {
            py = CEIL - h;
            surface(false);
        }

        float eps = Math.abs(vy) * dt + 0.03f;
        float left = px - h, right = px + h;
        for (int i = 0; i < objs.length; i++) {
            Level.Obj o = objs[i];
            float x = ox(o);
            if (x > right + 1) {
                // 정렬되어 있으므로 그룹 이동이 없는 오브젝트라면 더 볼 필요 없음
                if (!hasGroups) break;
                continue;
            }
            if (x + 1 < left - 1) continue;
            if (!visible(o)) continue;
            float y = oy(o);
            switch (o.type) {
                case Defs.BLOCK: {
                    if (right <= x || left >= x + 1 || py + h <= y || py - h >= y + 1) break;
                    if (prevY - h >= y + 1 - eps) {
                        py = y + 1 + h;
                        surface(true);
                    } else if (prevY + h <= y + eps) {
                        py = y - h;
                        surface(false);
                    } else {
                        die();
                        return;
                    }
                    break;
                }
                case Defs.SPIKE:
                case Defs.SMALL_SPIKE: {
                    float top = o.type == Defs.SPIKE ? 0.55f : 0.3f;
                    if (hitRot(o.rot, x, y, 0.36f, 0.64f, 0f, top, h * 0.85f)) {
                        die();
                        return;
                    }
                    break;
                }
                case Defs.SAW: {
                    float cx = x + 0.5f, cy = y + 0.5f;
                    float dx = Math.max(Math.abs(px - cx) - h * 0.8f, 0), dy = Math.max(Math.abs(py - cy) - h * 0.8f, 0);
                    if (dx * dx + dy * dy < 0.42f * 0.42f) {
                        die();
                        return;
                    }
                    break;
                }
                case Defs.PAD: {
                    if (padCool[i] > time) break;
                    if (hitRot(o.rot, x, y, 0.05f, 0.95f, 0f, 0.35f, h)) {
                        padCool[i] = time + 0.25f;
                        vy = grav * (mode == Defs.M_STEP ? 18f : mode == Defs.M_JET ? 10f : mode == Defs.M_FLIP ? 22f : V_PAD);
                        grounded = false;
                        evPad = true;
                    }
                    break;
                }
                case Defs.ORB:
                case Defs.G_ORB: {
                    if (used[i] || tapBuffer <= 0 || mode == Defs.M_ZIG) break;
                    float dx = px - (x + 0.5f), dy = py - (y + 0.5f);
                    if (dx * dx + dy * dy < 0.75f * 0.75f) {
                        used[i] = true;
                        tapBuffer = 0;
                        if (o.type == Defs.ORB) {
                            vy = grav * (mode == Defs.M_JET ? 10f : V_ORB);
                        } else {
                            grav = -grav;
                            vy = -grav * 8f;
                        }
                        grounded = false;
                        jumps++;
                        evOrb = true;
                    }
                    break;
                }
                case Defs.P_MODE:
                case Defs.P_GRAV:
                case Defs.P_SPEED:
                case Defs.P_SIZE: {
                    if (used[i]) break;
                    if (right > x + 0.2f && left < x + 0.8f && py + h > y - 1 && py - h < y + 2) {
                        used[i] = true;
                        evPortal = true;
                        if (o.type == Defs.P_MODE) {
                            mode = o.param(0);
                            if (mode == Defs.M_JET || mode == Defs.M_ZIG) vy = clamp(vy, -V_JET, V_JET);
                        } else if (o.type == Defs.P_GRAV) {
                            int ng = o.param(0) == 1 ? -1 : 1;
                            if (ng != grav) {
                                grav = ng;
                                vy *= 0.5f;
                            }
                        } else if (o.type == Defs.P_SIZE) {
                            int ns = Math.max(0, Math.min(2, o.param(0)));
                            if (ns != sizeIdx) {
                                float oldH = half();
                                sizeIdx = ns;
                                // 바닥/천장에 붙어 있으면 크기만큼 위치 보정
                                if (grounded) py += grav * (half() - oldH);
                            }
                        } else {
                            speedIdx = o.param(0);
                        }
                    }
                    break;
                }
                default:
                    if (Defs.isTrigger(o.type) && !used[i] && px >= x + 0.5f) {
                        used[i] = true;
                        fire(o);
                    }
            }
        }

        // 점프 모드 회전 연출
        if (mode == Defs.M_JUMP) {
            if (grounded) rotDeg = Math.round(rotDeg / 90f) * 90f;
            else rotDeg += grav * 400f * dt;
        } else if (mode == Defs.M_JET || mode == Defs.M_STEP) {
            rotDeg = -vy * 2.5f;
        } else if (mode == Defs.M_ZIG) {
            rotDeg = vy > 0 ? -45 : 45;
        } else {
            rotDeg += grav * 300f * dt;
        }

        updateAnims(dt);
        if (px >= endX) finished = true;
    }

    private void surface(boolean below) {
        boolean floor = (below && grav == 1) || (!below && grav == -1);
        if (floor) {
            grounded = true;
            if (vy * grav < 0) vy = 0;
        } else {
            if (vy * grav > 0) vy = 0;
        }
    }

    /** 회전된 칸 안의 사각 히트박스와 플레이어 박스 겹침 검사 */
    private boolean hitRot(int rot, float x, float y, float x0, float x1, float y0, float y1, float h) {
        float ax0, ax1, ay0, ay1;
        switch (rot & 3) {
            case 1: ax0 = y0; ax1 = y1; ay0 = 1 - x1; ay1 = 1 - x0; break; // 시계 90도: 오른쪽을 향함
            case 2: ax0 = 1 - x1; ax1 = 1 - x0; ay0 = 1 - y1; ay1 = 1 - y0; break; // 아래를 향함
            case 3: ax0 = 1 - y1; ax1 = 1 - y0; ay0 = x0; ay1 = x1; break; // 왼쪽을 향함
            default: ax0 = x0; ax1 = x1; ay0 = y0; ay1 = y1;
        }
        return px + h > x + ax0 && px - h < x + ax1 && py + h > y + ay0 && py - h < y + ay1;
    }

    private void die() {
        dead = true;
    }

    // ---------------- 트리거 ----------------

    private void fire(Level.Obj o) {
        switch (o.type) {
            case Defs.T_MOVE:
                anim(0, o.param(0), o.param(1) / 2f, o.param(3) / 10f, o.param(4));
                anim(1, o.param(0), o.param(2) / 2f, o.param(3) / 10f, o.param(4));
                break;
            case Defs.T_ROTATE:
                anim(2, o.param(0), o.param(1) * 15f, o.param(2) / 10f, 1);
                break;
            case Defs.T_ALPHA: {
                Anim a = new Anim();
                a.kind = 3;
                a.group = clampG(o.param(0));
                a.a = galpha[a.group];
                a.b = o.param(1) / 10f;
                a.dur = o.param(2) / 10f;
                a.easing = 0;
                anims.add(a);
                break;
            }
            case Defs.T_TOGGLE:
                gon[clampG(o.param(0))] = o.param(1);
                break;
            case Defs.T_BG:
                bgFrom = currentBgBlendTarget();
                bgTo = o.param(0);
                bgDur = o.param(1) / 10f;
                bgT = 0;
                break;
            case Defs.T_FLASH:
                flashColor = o.param(0);
                flashDur = Math.max(0.05f, o.param(1) / 10f);
                flashT = flashDur;
                break;
            case Defs.T_SHAKE:
                shakeAmp = o.param(0) * 0.04f;
                shakeT = o.param(1) / 10f;
                break;
            case Defs.T_ZOOM:
                zoomFrom = zoom;
                zoomTo = o.param(0) / 10f;
                zoomDur = o.param(1) / 10f;
                zoomT = 0;
                break;
            case Defs.T_HAPTIC:
                hapticMs = o.param(0) * 10;
                hapticPattern = o.param(1);
                break;
            case Defs.T_BEZEL:
                bezelGroup = clampG(o.param(0));
                bezelStep = o.param(1) / 2f;
                bezelUntil = time + o.param(2) / 10f;
                break;
        }
    }

    private int currentBgBlendTarget() {
        return bgT >= 1 ? bgTo : bgFrom;
    }

    private static int clampG(int g) {
        return Math.max(1, Math.min(99, g));
    }

    private void anim(int kind, int group, float amount, float dur, int easing) {
        Anim a = new Anim();
        a.kind = kind;
        a.group = clampG(group);
        a.b = amount;
        a.dur = dur;
        a.easing = easing;
        if (dur <= 0) {
            apply(a, amount);
            return;
        }
        anims.add(a);
    }

    private void apply(Anim a, float delta) {
        switch (a.kind) {
            case 0: gdx[a.group] += delta; break;
            case 1: gdy[a.group] += delta; break;
            case 2: grot[a.group] += delta; break;
        }
    }

    private static float ease(float t, int e) {
        t = Math.max(0, Math.min(1, t));
        switch (e) {
            case 1: return t * t * (3 - 2 * t);
            case 2: {
                // 튕기기 (끝에서 살짝 튀어오름)
                float n = 7.5625f, d = 2.75f;
                if (t < 1 / d) return n * t * t;
                if (t < 2 / d) { t -= 1.5f / d; return n * t * t + 0.75f; }
                if (t < 2.5 / d) { t -= 2.25f / d; return n * t * t + 0.9375f; }
                t -= 2.625f / d;
                return n * t * t + 0.984375f;
            }
            default: return t;
        }
    }

    private void updateAnims(float dt) {
        for (int i = anims.size() - 1; i >= 0; i--) {
            Anim a = anims.get(i);
            a.t += dt;
            float e = ease(a.t / a.dur, a.easing);
            if (a.kind == 3) {
                galpha[a.group] = a.a + (a.b - a.a) * e;
            } else {
                float v = a.b * e;
                apply(a, v - a.last);
                a.last = v;
            }
            if (a.t >= a.dur) anims.remove(i);
        }
        for (int g = 1; g < 100; g++) {
            if (gbz[g] != gbzTarget[g]) {
                gbz[g] += (gbzTarget[g] - gbz[g]) * Math.min(1f, dt * 14f);
                if (Math.abs(gbzTarget[g] - gbz[g]) < 0.002f) gbz[g] = gbzTarget[g];
            }
        }
        if (bgT < 1) bgT = bgDur <= 0 ? 1 : Math.min(1, bgT + dt / bgDur);
        flashT = Math.max(0, flashT - dt);
        shakeT = Math.max(0, shakeT - dt);
        if (zoomT < 1) {
            zoomT = zoomDur <= 0 ? 1 : Math.min(1, zoomT + dt / zoomDur);
            zoom = zoomFrom + (zoomTo - zoomFrom) * ease(zoomT, 1);
        }
    }

    private static float clamp(float v, float lo, float hi) {
        return Math.max(lo, Math.min(hi, v));
    }

    public float progress() {
        return Math.max(0, Math.min(1, px / endX));
    }

    // ---------------- 스냅샷 (연습 모드 체크포인트 / 봇) ----------------

    public static final class Snap {
        float px, py, vy, rotDeg, tapBuffer, time, bezelStep, bezelUntil, zoom;
        int mode, grav, speedIdx, sizeIdx, jumps, bezelGroup, bg;
        boolean grounded, holding;
        boolean[] used;
        float[] padCool, gdx, gdy, grot, galpha, gbz, gbzTarget;
        int[] gon;
        ArrayList<Anim> anims;
    }

    public Snap snap() {
        Snap s = new Snap();
        s.px = px; s.py = py; s.vy = vy; s.rotDeg = rotDeg; s.tapBuffer = tapBuffer; s.time = time;
        s.mode = mode; s.grav = grav; s.speedIdx = speedIdx; s.sizeIdx = sizeIdx; s.jumps = jumps;
        s.grounded = grounded; s.holding = holding;
        s.bezelGroup = bezelGroup; s.bezelStep = bezelStep; s.bezelUntil = bezelUntil;
        s.zoom = zoom; s.bg = bgT >= 1 ? bgTo : bgFrom;
        s.used = used.clone(); s.padCool = padCool.clone();
        s.gdx = gdx.clone(); s.gdy = gdy.clone(); s.grot = grot.clone(); s.galpha = galpha.clone();
        s.gbz = gbz.clone(); s.gbzTarget = gbzTarget.clone(); s.gon = gon.clone();
        s.anims = new ArrayList<>();
        for (Anim a : anims) {
            Anim b = new Anim();
            b.kind = a.kind; b.group = a.group; b.easing = a.easing; b.a = a.a; b.b = a.b; b.t = a.t; b.dur = a.dur; b.last = a.last;
            s.anims.add(b);
        }
        return s;
    }

    public void restore(Snap s) {
        px = s.px; py = s.py; vy = s.vy; rotDeg = s.rotDeg; tapBuffer = s.tapBuffer; time = s.time;
        mode = s.mode; grav = s.grav; speedIdx = s.speedIdx; sizeIdx = s.sizeIdx; jumps = s.jumps;
        grounded = s.grounded; holding = s.holding;
        bezelGroup = s.bezelGroup; bezelStep = s.bezelStep; bezelUntil = s.bezelUntil;
        System.arraycopy(s.used, 0, used, 0, used.length);
        System.arraycopy(s.padCool, 0, padCool, 0, padCool.length);
        System.arraycopy(s.gdx, 0, gdx, 0, 100);
        System.arraycopy(s.gdy, 0, gdy, 0, 100);
        System.arraycopy(s.grot, 0, grot, 0, 100);
        System.arraycopy(s.galpha, 0, galpha, 0, 100);
        System.arraycopy(s.gbz, 0, gbz, 0, 100);
        System.arraycopy(s.gbzTarget, 0, gbzTarget, 0, 100);
        System.arraycopy(s.gon, 0, gon, 0, 100);
        anims.clear();
        for (Anim a : s.anims) {
            Anim b = new Anim();
            b.kind = a.kind; b.group = a.group; b.easing = a.easing; b.a = a.a; b.b = a.b; b.t = a.t; b.dur = a.dur; b.last = a.last;
            anims.add(b);
        }
        zoom = zoomTo = zoomFrom = s.zoom;
        zoomT = 1;
        bgFrom = bgTo = s.bg;
        bgT = 1;
        flashT = shakeT = 0;
        dead = finished = false;
    }

    public List<Level.Obj> objectList() {
        return java.util.Arrays.asList(objs);
    }
}
