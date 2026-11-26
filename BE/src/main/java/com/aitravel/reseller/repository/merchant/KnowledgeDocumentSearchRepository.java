package com.aitravel.reseller.repository.merchant;

import com.aitravel.reseller.dto.respone.merchant.KnowledgeDocumentDtos;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.UUID;

@Repository
@RequiredArgsConstructor
public class KnowledgeDocumentSearchRepository {
    private final NamedParameterJdbcTemplate jdbc;

    public Page<KnowledgeDocumentDtos.ListItem> searchForMerchant(
            UUID merchantId,
            String search,
            String status,
            UUID productId,
            Pageable pageable
    ) {
        StringBuilder where = new StringBuilder(
                " WHERE merchant_id = :merchantId "
        );

        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("merchantId", merchantId);

        if (search != null && !search.isBlank()) {
            where.append("""
                     AND POSITION(:search IN LOWER(title)) > 0
                    """);
            params.addValue("search", search.trim().toLowerCase());
        }

        if (status != null && !status.isBlank()) {
            where.append(" AND status = :status ");
            params.addValue("status", status);
        }

        if (productId != null) {
            where.append(" AND product_id = :productId ");
            params.addValue("productId", productId);
        }

        Long total = jdbc.queryForObject(
                "SELECT COUNT(*) FROM travel_v2.knowledge_documents"
                        + where,
                params,
                Long.class
        );

        String sql = """
                SELECT
                    id,
                    product_id,
                    title,
                    status,
                    CASE
                        WHEN content LIKE 'KBFILE_V1|%' THEN TRUE
                        ELSE FALSE
                    END AS file_uploaded,
                    created_at,
                    updated_at
                FROM travel_v2.knowledge_documents
                """
                + where
                + """
                ORDER BY created_at DESC, id ASC
                LIMIT :limit OFFSET :offset
                """;

        params.addValue("limit", pageable.getPageSize());
        params.addValue("offset", pageable.getOffset());

        List<KnowledgeDocumentDtos.ListItem> items = jdbc.query(
                sql,
                params,
                this::mapListItem
        );

        return new PageImpl<>(
                items,
                pageable,
                total == null ? 0 : total
        );
    }

    private KnowledgeDocumentDtos.ListItem mapListItem(
            ResultSet rs,
            int rowNum
    ) throws SQLException {
        var createdAt = rs.getTimestamp("created_at");
        var updatedAt = rs.getTimestamp("updated_at");

        return new KnowledgeDocumentDtos.ListItem(
                rs.getObject("id", UUID.class),
                rs.getObject("product_id", UUID.class),
                rs.getString("title"),
                rs.getString("status"),
                rs.getBoolean("file_uploaded"),
                createdAt == null ? null : createdAt.toInstant(),
                updatedAt == null ? null : updatedAt.toInstant()
        );
    }
}