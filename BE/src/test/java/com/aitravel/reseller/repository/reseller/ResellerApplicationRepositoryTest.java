package com.aitravel.reseller.repository.reseller;

import com.aitravel.reseller.dto.respone.reseller.ApplicationResponse;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class ResellerApplicationRepositoryTest {
    @Test
    void insertCopiesBothCampaignSnapshotValuesAndKeepsConflictGuard() {
        var jdbc = mock(NamedParameterJdbcTemplate.class);
        var repository = new ResellerApplicationRepository(jdbc);
        UUID campaignId = UUID.randomUUID();
        UUID resellerId = UUID.randomUUID();
        when(jdbc.query(anyString(), anyMap(), any(RowMapper.class))).thenReturn(List.<ApplicationResponse>of());

        assertTrue(repository.create(campaignId, resellerId).isEmpty());

        var sql = ArgumentCaptor.forClass(String.class);
        verify(jdbc).query(sql.capture(), anyMap(), any(RowMapper.class));
        String statement = sql.getValue();
        assertTrue(statement.contains("commission_type_snapshot, commission_rate_snapshot"));
        assertTrue(statement.contains("c.commission_type, c.commission_rate"));
        assertTrue(statement.contains("FROM travel_v2.campaigns c"));
        assertTrue(statement.contains("ON CONFLICT (campaign_id, reseller_id) DO NOTHING"));
    }
}
