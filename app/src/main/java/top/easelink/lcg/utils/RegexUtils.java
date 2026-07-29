package top.easelink.lcg.utils;

import android.text.TextUtils;

import java.util.HashSet;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import androidx.annotation.NonNull;

/**
 * author : junzhang
 * date   : 2019-07-12 19:24
 */
public class RegexUtils {

    // 缓存编译结果：模式来自固定常量表，每篇帖子正文都会遍历一遍
    private static final Map<String, Pattern> PATTERN_CACHE = new ConcurrentHashMap<>();

    @NonNull
    public static HashSet<String> extractInfoFrom(String content, String patternStr) {
        HashSet<String> urls = new HashSet<>();
        if (TextUtils.isEmpty(content) || TextUtils.isEmpty(patternStr)) {
            return urls;
        }
        // 不用 computeIfAbsent：API 24+，本项目 minSdk 23 且未开 desugaring
        Pattern pattern = PATTERN_CACHE.get(patternStr);
        if (pattern == null) {
            pattern = Pattern.compile(patternStr);
            PATTERN_CACHE.put(patternStr, pattern);
        }
        Matcher matcher = pattern.matcher(content);
        while (matcher.find()) {
            urls.add(matcher.group());
        }
        return urls;
    }
}
