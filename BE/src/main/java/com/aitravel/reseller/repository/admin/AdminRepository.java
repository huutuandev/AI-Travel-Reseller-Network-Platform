package com.aitravel.reseller.repository.admin;

import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;
import java.util.*;

@Repository
@RequiredArgsConstructor
public class AdminRepository {
    private final NamedParameterJdbcTemplate jdbc;

    public List<Map<String, Object>> rows(String sql, Map<String, ?> params) {
        return jdbc.queryForList(sql, params);
    }

    public long count(String sql, Map<String, ?> params) {
        Long result = jdbc.queryForObject(sql, params, Long.class);
        return result == null ? 0 : result;
    }

    public int update(String sql, Map<String, ?> params) {
        return jdbc.update(sql, params);
    }
}
