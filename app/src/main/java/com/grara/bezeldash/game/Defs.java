package com.grara.bezeldash.game;

/** 오브젝트 / 모드 / 트리거 정의 (이름은 모두 한국어). */
public final class Defs {
    private Defs() {
    }

    // ---- 게임 모드 ----
    public static final int M_JUMP = 0, M_JET = 1, M_FLIP = 2, M_STEP = 3, M_ZIG = 4;
    public static final String[] MODE_NAMES = {"점프", "제트", "반전", "스텝", "지그재그"};
    public static final String[] MODE_HELP = {
            "탭: 점프 · 누르고 있으면 연속 점프",
            "누르는 동안 상승, 떼면 하강",
            "탭: 중력 뒤집기",
            "탭할 때마다 위로 튀어오름",
            "누르면 위로, 떼면 아래로 대각선"
    };

    // ---- 속도 (블록/초) ----
    public static final float[] SPEEDS = {7.0f, 9.0f, 11.0f, 13.5f};
    // ---- 크기 ----
    public static final float[] SIZES = {0.6f, 1.0f, 1.4f};
    public static final float[] SIZE_JUMP = {0.82f, 1.0f, 1.06f};
    public static final String[] SIZE_NAMES = {"작게", "보통", "크게"};

    public static final String[] SPEED_NAMES = {"느림", "보통", "빠름", "매우 빠름"};

    // ---- 오브젝트 타입 ----
    public static final int BLOCK = 0, SPIKE = 1, SMALL_SPIKE = 2, PAD = 3, ORB = 4, G_ORB = 5,
            P_MODE = 6, P_GRAV = 7, P_SPEED = 8, DECO = 9, SAW = 10, P_SIZE = 11,
            T_MOVE = 20, T_ROTATE = 21, T_ALPHA = 22, T_TOGGLE = 23, T_BG = 24, T_FLASH = 25,
            T_SHAKE = 26, T_ZOOM = 27, T_HAPTIC = 28, T_BEZEL = 29;

    /** 에디터 팔레트 순서 */
    public static final int[] PALETTE = {
            BLOCK, SPIKE, SMALL_SPIKE, SAW, PAD, ORB, G_ORB, P_MODE, P_GRAV, P_SPEED, P_SIZE, DECO,
            T_MOVE, T_ROTATE, T_ALPHA, T_TOGGLE, T_BG, T_FLASH, T_SHAKE, T_ZOOM, T_HAPTIC, T_BEZEL
    };

    public static boolean isTrigger(int type) {
        return type >= 20;
    }

    public static boolean isPortal(int type) {
        return type == P_MODE || type == P_GRAV || type == P_SPEED || type == P_SIZE;
    }

    public static String name(int type) {
        switch (type) {
            case BLOCK: return "블록";
            case SPIKE: return "가시";
            case SMALL_SPIKE: return "작은 가시";
            case SAW: return "톱니";
            case PAD: return "점프 패드";
            case ORB: return "점프 오브";
            case G_ORB: return "중력 오브";
            case P_MODE: return "모드 포털";
            case P_GRAV: return "중력 포털";
            case P_SPEED: return "속도 포털";
            case P_SIZE: return "크기 포털";
            case DECO: return "장식";
            case T_MOVE: return "이동 트리거";
            case T_ROTATE: return "회전 트리거";
            case T_ALPHA: return "투명도 트리거";
            case T_TOGGLE: return "켜기/끄기 트리거";
            case T_BG: return "배경색 트리거";
            case T_FLASH: return "플래시 트리거";
            case T_SHAKE: return "흔들기 트리거";
            case T_ZOOM: return "줌 트리거";
            case T_HAPTIC: return "진동 트리거";
            case T_BEZEL: return "베젤 리프트";
            default: return "?";
        }
    }

    public static String help(int type) {
        switch (type) {
            case BLOCK: return "밟을 수 있음. 옆면에 부딪히면 실패";
            case SPIKE: return "닿으면 실패";
            case SMALL_SPIKE: return "낮은 가시. 닿으면 실패";
            case SAW: return "회전하는 톱니. 닿으면 실패";
            case PAD: return "닿으면 높이 튀어오름";
            case ORB: return "지나갈 때 탭하면 공중 점프";
            case G_ORB: return "지나갈 때 탭하면 중력 반전";
            case P_MODE: return "통과하면 게임 모드 변경";
            case P_GRAV: return "통과하면 중력 방향 변경";
            case P_SPEED: return "통과하면 속도 변경";
            case P_SIZE: return "작게: 점프 낮음 · 크게: 몸이 커짐";
            case DECO: return "충돌 없는 장식";
            case T_MOVE: return "그룹을 지정한 만큼 이동";
            case T_ROTATE: return "그룹을 회전 (보이기만)";
            case T_ALPHA: return "그룹 투명도 변경";
            case T_TOGGLE: return "그룹 켜기/끄기 (충돌 포함)";
            case T_BG: return "배경색 변경";
            case T_FLASH: return "화면을 번쩍";
            case T_SHAKE: return "화면 흔들기";
            case T_ZOOM: return "카메라 확대/축소";
            case T_HAPTIC: return "워치 진동";
            case T_BEZEL: return "시간 안에 베젤을 돌리면 그룹이 위아래로 움직임";
            default: return "";
        }
    }

    // ---- 파라미터 정의: {이름, 최소, 최대, 기본, 단위 배율(표시용 나눗셈)} ----
    public static final class Param {
        public final String name;
        public final int min, max, def;
        public final float scale;
        public final String[] labels;

        Param(String name, int min, int max, int def, float scale, String[] labels) {
            this.name = name;
            this.min = min;
            this.max = max;
            this.def = def;
            this.scale = scale;
            this.labels = labels;
        }

        public String show(int v) {
            if (labels != null) return labels[Math.max(0, Math.min(labels.length - 1, v))];
            if (scale == 1f) return String.valueOf(v);
            float f = v / scale;
            return (f == Math.round(f)) ? String.valueOf((int) f) : String.format("%.1f", f);
        }
    }

    private static Param p(String n, int min, int max, int def) {
        return new Param(n, min, max, def, 1f, null);
    }

    private static Param p(String n, int min, int max, int def, float scale) {
        return new Param(n, min, max, def, scale, null);
    }

    private static Param l(String n, int def, String... labels) {
        return new Param(n, 0, labels.length - 1, def, 1f, labels);
    }

    public static final String[] COLOR_NAMES = {"남색", "보라", "자홍", "빨강", "주황", "초록", "청록", "검정"};
    public static final int[] BG_COLORS = {
            0xFF16205A, 0xFF3A1A63, 0xFF5E1450, 0xFF5A1420, 0xFF5A3210, 0xFF124A2A, 0xFF0E4A55, 0xFF07070C
    };
    public static final int[] FLASH_COLORS = {
            0xFFFFFFFF, 0xFFB57BFF, 0xFFFF5CC8, 0xFFFF4D4D, 0xFFFFA63D, 0xFF5CFF8F, 0xFF3DE8FF, 0xFF000000
    };

    public static Param[] params(int type) {
        switch (type) {
            case P_MODE: return new Param[]{l("모드", 0, MODE_NAMES)};
            case P_GRAV: return new Param[]{l("중력", 1, "보통", "뒤집힘")};
            case P_SPEED: return new Param[]{l("속도", 1, SPEED_NAMES)};
            case P_SIZE: return new Param[]{l("크기", 0, SIZE_NAMES)};
            case DECO: return new Param[]{l("모양", 0, "별", "원", "선", "빛")};
            case T_MOVE: return new Param[]{p("대상 그룹", 1, 99, 1), p("X 이동", -40, 40, 0, 2f), p("Y 이동", -40, 40, 4, 2f),
                    p("지속시간(초)", 0, 50, 5, 10f), l("가속 곡선", 1, "일정", "부드럽게", "튕기기")};
            case T_ROTATE: return new Param[]{p("대상 그룹", 1, 99, 1), p("각도", -24, 24, 6, 1f / 15f), p("지속시간(초)", 0, 50, 10, 10f)};
            case T_ALPHA: return new Param[]{p("대상 그룹", 1, 99, 1), p("투명도", 0, 10, 0, 10f), p("지속시간(초)", 0, 50, 5, 10f)};
            case T_TOGGLE: return new Param[]{p("대상 그룹", 1, 99, 1), l("상태", 0, "끄기", "켜기")};
            case T_BG: return new Param[]{l("색", 1, COLOR_NAMES), p("지속시간(초)", 0, 50, 5, 10f)};
            case T_FLASH: return new Param[]{l("색", 0, COLOR_NAMES), p("지속시간(초)", 1, 30, 3, 10f)};
            case T_SHAKE: return new Param[]{p("세기", 1, 10, 4), p("지속시간(초)", 1, 50, 5, 10f)};
            case T_ZOOM: return new Param[]{p("배율", 5, 20, 13, 10f), p("지속시간(초)", 0, 50, 5, 10f)};
            case T_HAPTIC: return new Param[]{p("길이(ms)", 1, 50, 6, 0.1f), l("패턴", 0, "한 번", "두 번", "길게")};
            case T_BEZEL: return new Param[]{p("대상 그룹", 1, 99, 1), p("한 칸당 이동", 1, 8, 1, 2f), p("유지 시간(초)", 5, 100, 30, 10f)};
            default: return new Param[0];
        }
    }

    /** 포털 높이(블록) */
    public static float height(int type) {
        return isPortal(type) ? 3f : 1f;
    }
}
