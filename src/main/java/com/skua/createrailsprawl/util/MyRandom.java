/* MIT License | Copyright (c) 2026 Sakura-Lhy0409 | 允许自由使用、修改、分发，需保留版权声明 */
package com.skua.createrailsprawl.util;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Random;

/**
 * 随机工具类
 */
public class MyRandom {

    /**
     * 在[0, range]范围内生成一个随机点
     */
    public static int[] generatePoints(long seed, int range) {
        Random random = new Random(seed);
        int quart = range / 4;
        int x1 = random.nextInt(quart, range - quart);
        int z1 = random.nextInt(quart, range - quart);
        return new int[]{x1, z1};
    }

    /**
     * 从Map中随机选择一个值
     */
    public static <K, V> V getRandomValueFromMap(Map<K, V> map, long seed) {
        if (map == null || map.isEmpty()) {
            return null;
        }
        Random random = new Random(seed);
        List<V> values = new ArrayList<>(map.values());
        return values.get(random.nextInt(values.size()));
    }
}
