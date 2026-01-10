package com.skua.createrailsprawl.util;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Random;

public class MyRandom {
    
    public static int[] generatePoints(long seed, int range) {
        Random random = new Random(seed);
        int quart = range / 4;
        int x1 = random.nextInt(quart, range - quart);
        int z1 = random.nextInt(quart, range - quart);
        return new int[]{x1, z1};
    }

    public static <K, V> V getRandomValueFromMap(Map<K, V> map, long seed) {
        if (map == null || map.isEmpty()) {
            return null;
        }
        Random random = new Random(seed);
        List<V> values = new ArrayList<>(map.values());
        int randomIndex = random.nextInt(values.size());
        return values.get(randomIndex);
    }
}
