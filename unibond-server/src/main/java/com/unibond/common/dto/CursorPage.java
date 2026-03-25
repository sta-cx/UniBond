package com.unibond.common.dto;

import java.util.List;

public record CursorPage<T>(List<T> data, String cursor, boolean hasMore) {}
