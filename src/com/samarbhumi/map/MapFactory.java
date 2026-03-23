package com.samarbhumi.map;

import com.samarbhumi.core.Enums.TileType;

/**
 * Builds the 3 hand-crafted maps.
 * S=SOLID  .=AIR  P=PLATFORM  L=LADDER  W=WATER  B=SPAWN_BLUE  R=SPAWN_RED
 */
public final class MapFactory {
    private MapFactory() {}

    public static GameMap create(com.samarbhumi.core.Enums.MapId id) {
        return switch (id) {
            case WARZONE_ALPHA  -> buildWarzoneAlpha();
            case JUNGLE_RUINS   -> buildJungleRuins();
            case STEEL_FORTRESS -> buildSteelFortress();
            case CITY_RUINS     -> buildCityRuins();
        };
    }

    // ── MAP 1: Warzone Alpha — open grass field, wide platforms, water pits
    private static GameMap buildWarzoneAlpha() {
        String[] layout = {
        //  0         1         2         3         4         5         6         7
        //  0123456789012345678901234567890123456789012345678901234567890123456789012345678
            "SSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSS",
            "S.B............................................................................S",
            "S..............................................................................S",
            "S......PPPPPP..........................................PPPPPP...............B...S",
            "S..............................................................................S",
            "S.PPPPP......................................................PPPPP..............S",
            "S.................B...............................R.........................S",
            "S......PPPPPPPPPP....................PPPPPPPPPP.................................S",
            "S................B.........................................R...................S",
            "S...........PPPPPPPPPPPPPPPPPPPP.......................PPPP....................S",
            "S....B....................L.....L.....................................R........S",
            "S........PPPPPPPPPPPPPPPP.....PPPPPPPPPPPPP...................................S",
            "S...............................L..............................................S",
            "S..........PPPPPPPP....PPPPPP.....PPPPPPPPP....PPPPPP........................S",
            "S.B............................................................................S",
            "S..............................................................R.................S",
            "SSSS....SSSSSSSSSSS....SSSSSSSS....SSSSSSSSS....SSSSSSSSSS...................S",
            "S...WWWWWWW....WWWWWWWWWW....WWWWWWW....WWWWWWWW....WWWWWWWW...............S",
            "SSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSS",
        };
        GameMap m = parse("Warzone Alpha", layout);
        m.setMapStyle(0);
        return m;
    }

    // ── MAP 2: Jungle Ruins — vertical ladders, dense stone cover
    private static GameMap buildJungleRuins() {
        String[] layout = {
            "SSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSS",
            "S.B..................................................................R.........S",
            "S..........PPPP.......................................PPPP.................B....S",
            "S..L..........................................................L.............R...S",
            "SSPPPP.L.....................................................L.PPPSS..........S",
            "S...L.....PPPP...........................................PPPP...L................S",
            "S...L...B............................................L.......R..........S",
            "SSSS.LPPPPPPPP.......PPPPPPPPPPP..........PPPPPPPP.L.SSSS.......................S",
            "S....L...............L...........L................L...............................S",
            "S.PPPL.......PPPPP...L...........L......PPPPP....L.PPPP.........................S",
            "S....L....B..........L...........L....................L........R..................S",
            "SSSSSL.SSSSSSSS.SSSSSSL..PPPPL..SSSSSS..SSSSSS.LSSSSSS......................S",
            "S.WWWWL.......................................LWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWS",
            "SSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSS",
        };
        GameMap m = parse("Jungle Ruins", layout);
        m.setMapStyle(1);
        return m;
    }

    // ── MAP 3: Steel Fortress — symmetric metal bunker, tight corridors
    private static GameMap buildSteelFortress() {
        String[] layout = {
            "SSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSS",
            "S.B.................S......................................S..........R........S",
            "S...................S......................................S...................S",
            "S.PPPP....PPPP......S......................................S....PPPP....PPPP.S",
            "S..............B....S..PPPPPPPPPPPPPPPPPPPPPPPPPP.......S...R...............S",
            "SSSSSSSS.SSSSSSS....S......................................S.SSSSSSS.SSSSSSSS",
            "S..............B....PPSS....PPPP..............PPPP..SSPP..........R.........S",
            "S..........................L...............................L....................S",
            "S..PPPP....PPPP..........L.......PPPPPPPPPP.......L.....PPPP......PPPP......S",
            "S.........B..............L...............................L.......R...........S",
            "SSSSS.SSSSSSSSSSS.......SS...............................SS....SSSSSSSSS.SSSS",
            "S.WWW.......WWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWW...........WWWWWW.S",
            "SSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSS",
        };
        GameMap m = parse("Steel Fortress", layout);
        m.setMapStyle(2);
        return m;
    }

    // ── MAP 4: City Ruins — urban rooftops, fire escapes, alleyways ───────
    private static GameMap buildCityRuins() {
        String[] layout = {
            "SSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSS",
            "S.B......SSSSSS......SSSSSSSSSS......SSSSSSS.......SSSSSSSSS.....SSSSSS......RS",
            "S........S....S......S........S......S.....S.......S.......S.....S....S......S",
            "S....B...S....S......S....P...S......S..B..S.......S...R...S.....S....S...R..S",
            "S........PPPPPS......PPPPPPPPPS..B...PPPPPPP...P...PPPPPPPPP.....PPPPPP......S",
            "S....................................L..............L..........................S",
            "S.........PPPPP.....PPPPPP.....PPPL..PP....PP...LPPP...........PPPPP.........S",
            "S....B...........B...........B..L................L.....R....................R..S",
            "SSSSS.SSSSSS..SSSSSS..SSSSSS..SL....PPPPPPPP....LSSSSSSSS.SSSSSS.SSSSSS.SSSSS",
            "S.....S....S..S....S..S....S..SL................LS......S.S....S.S....S.....S",
            "S.....S.B..S..S....S..S.R..S..SL......B.R......LS......S.S.B..S.S..R.S.....S",
            "SPPPPPPPPPPPPPPPPPPPPPPPPPPPPPPP................SSSPPPPPPPPPPPPPPPPPPPPPPPPPPS",
            "S.....L.........L.............L..PPPP....PPPP...L.....L.............L........S",
            "S.....L....B....L.............L.................L.....L....B........L........S",
            "SSSSSSL.SSSSSS.LSSSSSS.SSSSSSL.SSSSSSSSSSSSSSSSL.SSSSL.SSSSSS.SSSSSSL.SSSSSS",
            "S.WWWWL....................WWWWL................LWWWWWWL.....................LS",
            "SSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSS",
        };
        GameMap m = parse("City Ruins", layout);
        m.setMapStyle(3);   // urban concrete
        return m;
    }

    private static GameMap parse(String name, String[] rows) {
        int maxW = 0;
        for (String r : rows) maxW = Math.max(maxW, r.length());
        TileType[][] tiles = new TileType[rows.length][maxW];
        for (int r = 0; r < rows.length; r++) {
            String row = rows[r];
            for (int c = 0; c < maxW; c++) {
                char ch = c < row.length() ? row.charAt(c) : '.';
                tiles[r][c] = switch (ch) {
                    case 'S' -> TileType.SOLID;
                    case 'P' -> TileType.PLATFORM;
                    case 'L' -> TileType.LADDER;
                    case 'W' -> TileType.WATER;
                    case 'B' -> TileType.SPAWN_BLUE;
                    case 'R' -> TileType.SPAWN_RED;
                    default  -> TileType.AIR;
                };
            }
        }
        return new GameMap(name, tiles);
    }
}
