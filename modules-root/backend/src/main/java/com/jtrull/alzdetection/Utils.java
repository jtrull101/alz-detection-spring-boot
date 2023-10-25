package com.jtrull.alzdetection;

import java.util.UUID;

public class Utils {

     /**
     * 
     * @param path
     * @return
     */
    public static Long generateIdFromPath(String path) {
        return UUID.nameUUIDFromBytes(path.getBytes()).getMostSignificantBits();
    }
}
