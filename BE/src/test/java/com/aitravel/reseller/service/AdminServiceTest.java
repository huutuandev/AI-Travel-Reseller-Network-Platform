package com.aitravel.reseller.service;

import com.aitravel.reseller.exception.*;
import com.aitravel.reseller.repository.admin.AdminRepository;
import com.aitravel.reseller.dto.request.admin.AdminRequests;
import com.aitravel.reseller.service.admin.AdminService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.*;
import org.springframework.aop.framework.ProxyFactory;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.AnnotationTransactionAttributeSource;
import org.springframework.transaction.interceptor.TransactionInterceptor;
import org.springframework.transaction.support.SimpleTransactionStatus;
import org.springframework.security.access.AccessDeniedException;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

// Pure unit tests: no Spring Context, datasource, Flyway or database.
class AdminServiceTest {
    AdminRepository db;
    AdminService service;
    UUID id = UUID.randomUUID();
    UUID actor = UUID.randomUUID();
    static final String UPDATE = "UPDATE travel_v2.merchants";
    static final String EVENT = "INSERT INTO travel_v2.platform_events";

    @BeforeEach void setup() { db = mock(AdminRepository.class); service = new AdminService(db); }

    void current(String status, long version) {
        when(db.rows(contains("FOR UPDATE"), anyMap())).thenReturn(List.of(Map.of("status",status,"version",version)));
    }

    void successfulReadsAndWrites() {
        when(db.rows(contains("m.id=:id"),anyMap())).thenReturn(List.of(Map.of("id",id)));
        when(db.rows(contains("activeCampaigns"),anyMap())).thenReturn(List.of(Map.of("activeCampaigns",0)));
        when(db.update(anyString(),anyMap())).thenReturn(1);
    }

    @ParameterizedTest
    @CsvSource({"ACTIVE,SUSPENDED,Maintenance", "SUSPENDED,ACTIVE,"})
    void validTransitionUpdatesMerchantThenEvent(String old, String next, String reason) {
        current(old,0);
        successfulReadsAndWrites();
        var response = service.changeStatus(id,new AdminRequests.StatusChange(next,reason,0L),actor);
        assertEquals(id,response.get("id"));
        var order = inOrder(db);
        order.verify(db).rows(contains("FOR UPDATE"),anyMap());
        order.verify(db).update(contains("WHERE id=:id AND version=:version"),argThat(p ->
                next.equals(p.get("status")) && Long.valueOf(0).equals(p.get("version"))));
        order.verify(db).update(contains(EVENT),argThat(p -> actor.equals(p.get("actor"))
                && old.equals(p.get("old")) && next.equals(p.get("next"))
                && id.equals(p.get("merchant")) && Objects.equals(reason,p.get("reason"))));
        verify(db,times(2)).update(anyString(),anyMap());
    }

    @ParameterizedTest @NullAndEmptySource @ValueSource(strings={" ","\t\n"})
    void suspensionRequiresReason(String reason) {
        assertThrows(IllegalArgumentException.class, () -> service.changeStatus(id,
                new AdminRequests.StatusChange("SUSPENDED",reason,0L),actor));
        verifyNoInteractions(db);
    }

    @Test void reasonLengthBoundary() {
        assertThrows(IllegalArgumentException.class, () -> service.changeStatus(id,
                new AdminRequests.StatusChange("SUSPENDED","x".repeat(1001),0L),actor));
        verifyNoInteractions(db);
        current("ACTIVE",0); successfulReadsAndWrites();
        assertDoesNotThrow(() -> service.changeStatus(id,
                new AdminRequests.StatusChange("SUSPENDED","x".repeat(1000),0L),actor));
    }

    @ParameterizedTest @CsvSource({"PENDING,ACTIVE","PENDING,SUSPENDED","ACTIVE,ACTIVE","SUSPENDED,SUSPENDED","REJECTED,ACTIVE"})
    void invalidTransitionNeverWrites(String old, String next) {
        current(old,0);
        assertThrows(IllegalStateException.class, () -> service.changeStatus(id,
                new AdminRequests.StatusChange(next,"reason",0L),actor));
        verify(db,never()).update(anyString(),anyMap());
    }

    @ParameterizedTest @NullAndEmptySource @ValueSource(strings={"PENDING","REJECTED","active","UNKNOWN"})
    void invalidTargetIsBadRequest(String next) {
        assertThrows(IllegalArgumentException.class, () -> service.changeStatus(id,
                new AdminRequests.StatusChange(next,"reason",0L),actor));
        verifyNoInteractions(db);
    }

    @Test void missingOrNegativeVersionDoesNotWrite() {
        for (Long version : Arrays.asList(null,-1L)) {
            assertThrows(IllegalArgumentException.class, () -> service.changeStatus(id,
                    new AdminRequests.StatusChange("ACTIVE",null,version),actor));
        }
        verifyNoInteractions(db);
    }

    @Test void missingMerchantIsNotFound() {
        assertThrows(ResourceNotFoundException.class, () -> service.changeStatus(id,
                new AdminRequests.StatusChange("ACTIVE",null,0L),actor));
        verify(db,never()).update(anyString(),anyMap());
    }

    @Test void staleSecondAdminCannotWrite() {
        when(db.rows(contains("FOR UPDATE"),anyMap())).thenReturn(
                List.of(Map.of("status","ACTIVE","version",0L)),
                List.of(Map.of("status","SUSPENDED","version",1L)));
        successfulReadsAndWrites();
        service.changeStatus(id,new AdminRequests.StatusChange("SUSPENDED","reason",0L),actor);
        assertThrows(IllegalStateException.class, () -> service.changeStatus(id,
                new AdminRequests.StatusChange("ACTIVE",null,0L),UUID.randomUUID()));
        verify(db,times(1)).update(contains(UPDATE),anyMap());
        verify(db,times(1)).update(contains(EVENT),anyMap());
    }

    @Test void failedConditionalUpdateDoesNotWriteEvent() {
        current("ACTIVE",0);
        assertThrows(IllegalStateException.class, () -> service.changeStatus(id,
                new AdminRequests.StatusChange("SUSPENDED","reason",0L),actor));
        verify(db,never()).update(contains(EVENT),anyMap());
    }

    AdminService transactionalProxy(PlatformTransactionManager manager) {
        var interceptor = new TransactionInterceptor();
        interceptor.setTransactionManager(manager);
        interceptor.setTransactionAttributeSource(new AnnotationTransactionAttributeSource());
        var factory = new ProxyFactory(service);
        factory.addAdvice(interceptor);
        return (AdminService) factory.getProxy();
    }

    @Test void successCommitsUpdateAndEventTogether() {
        var manager = mock(PlatformTransactionManager.class);
        var tx = new SimpleTransactionStatus();
        when(manager.getTransaction(any())).thenReturn(tx);
        current("ACTIVE",0); successfulReadsAndWrites();
        transactionalProxy(manager).changeStatus(id,new AdminRequests.StatusChange("SUSPENDED","reason",0L),actor);
        var order = inOrder(db,manager);
        order.verify(manager).getTransaction(argThat(def -> !def.isReadOnly()));
        order.verify(db).update(contains(UPDATE),anyMap());
        order.verify(db).update(contains(EVENT),anyMap());
        order.verify(manager).commit(tx);
        verify(manager,never()).rollback(any());
    }

    @Test void eventFailureRollsBackWholeTransaction() {
        var manager = mock(PlatformTransactionManager.class);
        var tx = new SimpleTransactionStatus();
        when(manager.getTransaction(any())).thenReturn(tx);
        current("ACTIVE",0);
        when(db.update(contains(UPDATE),anyMap())).thenReturn(1);
        when(db.update(contains(EVENT),anyMap())).thenThrow(new org.springframework.dao.DataIntegrityViolationException("event failed"));
        assertThrows(org.springframework.dao.DataIntegrityViolationException.class, () -> transactionalProxy(manager)
                .changeStatus(id,new AdminRequests.StatusChange("SUSPENDED","reason",0L),actor));
        var order = inOrder(db,manager);
        order.verify(db).update(contains(UPDATE),anyMap());
        order.verify(db).update(contains(EVENT),anyMap());
        order.verify(manager).rollback(tx);
        verify(manager,never()).commit(any());
    }

    @Test void exceptionHandlerUsesExpectedHttpStatuses() {
        var handler = new GlobalExceptionHandler();
        assertEquals(400,handler.handleIllegalArgument(new IllegalArgumentException()).getStatusCode().value());
        assertEquals(404,handler.handleResourceNotFound(new ResourceNotFoundException("missing")).getStatusCode().value());
        assertEquals(409,handler.handleIllegalState(new IllegalStateException()).getStatusCode().value());
        assertEquals(403,handler.handleAccessDenied(new AccessDeniedException("denied")).getStatusCode().value());
    }

    @Test void settingsConflictDoesNotWriteEvent() {
        assertThrows(IllegalStateException.class, () -> service.settings(new AdminRequests.SettingsChange(
                "SUBDOMAIN_PREVIEW","aitravel.example","caption","video",0L),actor));
        verify(db,never()).update(contains("platform_events"),anyMap());
    }

    @Test void paginationIsBounded() {
        assertThrows(IllegalArgumentException.class, () -> service.tenants(0,101,"",""));
        assertThrows(IllegalArgumentException.class, () -> service.logs(null,-1,20,false));
        verifyNoInteractions(db);
    }
}