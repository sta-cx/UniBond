package com.unibond.stats.entity;

public enum AchievementType {
    FIRST_BIND("命中注定"),
    STREAK_3("初识默契"),
    STREAK_7("默契升温"),
    STREAK_30("心有灵犀"),
    STREAK_100("灵魂伴侣"),
    PERFECT_MATCH("心意相通"),
    HIGH_SCORE_10("默契之星"),
    THEME_FOOD("美食知己"),
    THEME_TRAVEL("旅行搭档"),
    THEME_MEMORY("回忆收藏家"),
    ANNIVERSARY("甜蜜纪念");

    private final String displayName;
    AchievementType(String displayName) { this.displayName = displayName; }
    public String getDisplayName() { return displayName; }
}
