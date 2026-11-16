package com.enterprise.aiassistant.backend.document.service;

import com.enterprise.aiassistant.backend.auth.security.UserPrincipal;
import com.enterprise.aiassistant.backend.auth.service.CurrentUserService;
import com.enterprise.aiassistant.backend.common.exception.ErrorCode;
import com.enterprise.aiassistant.backend.common.exception.business_exception.AuthorizationException;
import com.enterprise.aiassistant.backend.department.entity.Department;
import com.enterprise.aiassistant.backend.document.dto.DocumentAccessScope;
import com.enterprise.aiassistant.backend.document.entity.Document;
import com.enterprise.aiassistant.backend.document.helper.DocumentAuthorizationHelper;
import com.enterprise.aiassistant.backend.document.repository.DocumentAccessRepository;
import com.enterprise.aiassistant.backend.support.ErrorCodeAssertions;
import com.enterprise.aiassistant.backend.user.enums.Permission;
import com.enterprise.aiassistant.backend.user.enums.Role;
import com.enterprise.aiassistant.backend.user.entity.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.catchThrowable;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

// currentAccessScope() là nguồn duy nhất tạo DocumentAccessScope cho query layer:
// ADMIN/SUPERVISOR phải luôn ra unrestricted=true (không phải null), EMPLOYEE/MANAGER
// phải luôn bị bó hẹp theo owner/department/share.
class DocumentAuthorizationServiceTest {

    private static final long IT_DEPARTMENT_ID = 2L;

    private final CurrentUserService currentUserService = mock(CurrentUserService.class);

    private final DocumentAccessRepository documentAccessRepository = mock(DocumentAccessRepository.class);

    private DocumentAuthorizationService service;

    @BeforeEach
    void setUp() {
        service = new DocumentAuthorizationService(
                currentUserService,
                documentAccessRepository,
                new DocumentAuthorizationHelper()
        );
    }

    @Test
    void adminGetsUnrestrictedScopeNeverNull() {

        when(currentUserService.getCurrentPrincipal()).thenReturn(principal(1L, Role.ADMIN, null));

        DocumentAccessScope scope = service.currentAccessScope();

        assertNotNull(scope);
        assertTrue(scope.unrestricted());
        verify(documentAccessRepository, never()).findDocumentIdsSharedWithUser(any());
    }

    @Test
    void supervisorGetsUnrestrictedScopeNeverNull() {

        when(currentUserService.getCurrentPrincipal()).thenReturn(principal(2L, Role.SUPERVISOR, null));

        DocumentAccessScope scope = service.currentAccessScope();

        assertNotNull(scope);
        assertTrue(scope.unrestricted());
        verify(documentAccessRepository, never()).findDocumentIdsSharedWithUser(any());
    }

    @Test
    void employeeGetsScopeRestrictedToOwnUserDepartmentAndShares() {

        when(currentUserService.getCurrentPrincipal())
                .thenReturn(principal(10L, Role.EMPLOYEE, IT_DEPARTMENT_ID));
        when(documentAccessRepository.findDocumentIdsSharedWithUser(10L))
                .thenReturn(List.of(55L));

        DocumentAccessScope scope = service.currentAccessScope();

        assertFalse(scope.unrestricted());
        assertEquals(10L, scope.userId());
        assertEquals(IT_DEPARTMENT_ID, scope.departmentId());
        assertEquals(List.of(55L), scope.sharedDocumentIds());
    }

    @Test
    void managerGetsScopeRestrictedToOwnUserDepartmentAndShares() {

        when(currentUserService.getCurrentPrincipal())
                .thenReturn(principal(20L, Role.MANAGER, IT_DEPARTMENT_ID));
        when(documentAccessRepository.findDocumentIdsSharedWithUser(20L))
                .thenReturn(List.of());

        DocumentAccessScope scope = service.currentAccessScope();

        assertFalse(scope.unrestricted());
        assertEquals(20L, scope.userId());
        assertEquals(IT_DEPARTMENT_ID, scope.departmentId());
        assertTrue(scope.sharedDocumentIds().isEmpty());
    }

    // ===================== authorization gate =====================

    @Test
    void shouldDenyReadWhenPrincipalHasNoReadPermission() {

        when(currentUserService.getCurrentPrincipal())
                .thenReturn(principalWith(10L, Role.EMPLOYEE, IT_DEPARTMENT_ID, Set.of()));

        assertFalse(service.canRead(document(100L, 10L, IT_DEPARTMENT_ID)));
    }

    @Test
    void shouldAllowReadForOwnerWithReadPermission() {

        when(currentUserService.getCurrentPrincipal())
                .thenReturn(principalWith(10L, Role.EMPLOYEE, IT_DEPARTMENT_ID, Set.of(Permission.DOCUMENT_READ)));

        assertTrue(service.canRead(document(100L, 10L, IT_DEPARTMENT_ID)));
    }

    @Test
    void shouldNotQueryExplicitAccessWhenDocumentHasNoId() {

        when(currentUserService.getCurrentPrincipal())
                .thenReturn(principalWith(10L, Role.EMPLOYEE, null, Set.of(Permission.DOCUMENT_READ)));

        assertTrue(service.canRead(document(null, 10L, null)));

        verify(documentAccessRepository, never()).existsByDocumentIdAndUserId(any(), any());
    }

    @Test
    void shouldAllowReadThroughExplicitAccessFromAnotherDepartment() {

        when(currentUserService.getCurrentPrincipal())
                .thenReturn(principalWith(10L, Role.EMPLOYEE, IT_DEPARTMENT_ID, Set.of(Permission.DOCUMENT_READ)));
        when(documentAccessRepository.existsByDocumentIdAndUserId(100L, 10L)).thenReturn(true);

        assertTrue(service.canRead(document(100L, 99L, 999L)));

        verify(documentAccessRepository).existsByDocumentIdAndUserId(100L, 10L);
    }

    @Test
    void shouldThrowAccessDeniedOnRequireRead() {

        when(currentUserService.getCurrentPrincipal())
                .thenReturn(principalWith(10L, Role.EMPLOYEE, IT_DEPARTMENT_ID, Set.of()));

        Document document = document(100L, 99L, 999L);

        ErrorCodeAssertions.assertBusinessErrorOfType(
                catchThrowable(() -> service.requireRead(document)),
                AuthorizationException.class,
                ErrorCode.ACCESS_DENIED
        );
    }

    @Test
    void shouldThrowPermissionDeniedOnRequireCreate() {

        when(currentUserService.getCurrentPrincipal())
                .thenReturn(principalWith(10L, Role.EMPLOYEE, IT_DEPARTMENT_ID, Set.of()));

        ErrorCodeAssertions.assertBusinessErrorOfType(
                catchThrowable(() -> service.requireCreate()),
                AuthorizationException.class,
                ErrorCode.PERMISSION_DENIED
        );
    }

    @Test
    void shouldThrowAccessDeniedOnRequireDownloadWhenPermissionMissing() {

        when(currentUserService.getCurrentPrincipal())
                .thenReturn(principalWith(10L, Role.EMPLOYEE, IT_DEPARTMENT_ID, Set.of(Permission.DOCUMENT_READ)));

        Document document = document(100L, 10L, IT_DEPARTMENT_ID);

        ErrorCodeAssertions.assertBusinessErrorOfType(
                catchThrowable(() -> service.requireDownload(document)),
                AuthorizationException.class,
                ErrorCode.ACCESS_DENIED
        );
    }

    @Test
    void shouldThrowAccessDeniedOnRequireUpdateAndDeleteAndManageAccess() {

        when(currentUserService.getCurrentPrincipal())
                .thenReturn(principalWith(10L, Role.EMPLOYEE, IT_DEPARTMENT_ID,
                        Set.of(Permission.DOCUMENT_UPDATE, Permission.DOCUMENT_DELETE,
                                Permission.DOCUMENT_MANAGE_ACCESS)));

        Document colleagueDocument = document(100L, 99L, IT_DEPARTMENT_ID);

        ErrorCodeAssertions.assertBusinessErrorOfType(
                catchThrowable(() -> service.requireUpdate(colleagueDocument)),
                AuthorizationException.class, ErrorCode.ACCESS_DENIED);

        ErrorCodeAssertions.assertBusinessErrorOfType(
                catchThrowable(() -> service.requireDelete(colleagueDocument)),
                AuthorizationException.class, ErrorCode.ACCESS_DENIED);

        ErrorCodeAssertions.assertBusinessErrorOfType(
                catchThrowable(() -> service.requireManageAccess(colleagueDocument)),
                AuthorizationException.class, ErrorCode.ACCESS_DENIED);
    }

    @Test
    void shouldAllowCreateWhenPrincipalHasCreatePermission() {

        when(currentUserService.getCurrentPrincipal())
                .thenReturn(principalWith(10L, Role.EMPLOYEE, IT_DEPARTMENT_ID, Set.of(Permission.DOCUMENT_CREATE)));

        assertDoesNotThrow(() -> service.requireCreate());
    }

    @Test
    void shouldFilterReadableDocumentIdsForCurrentUser() {

        when(currentUserService.getCurrentPrincipal())
                .thenReturn(principalWith(10L, Role.EMPLOYEE, IT_DEPARTMENT_ID, Set.of(Permission.DOCUMENT_READ)));
        when(documentAccessRepository.findDocumentIdsSharedWithUser(10L)).thenReturn(List.of(3L));

        Document own = document(1L, 10L, IT_DEPARTMENT_ID);
        Document otherDepartment = document(2L, 99L, 999L);
        Document shared = document(3L, 99L, 999L);

        assertEquals(
                Set.of(1L, 3L),
                service.filterReadableDocumentIds(List.of(own, otherDepartment, shared))
        );
    }

    @Test
    void shouldReturnEmptySetWhenNothingIsReadable() {

        when(currentUserService.getCurrentPrincipal())
                .thenReturn(principalWith(10L, Role.EMPLOYEE, IT_DEPARTMENT_ID, Set.of()));
        when(documentAccessRepository.findDocumentIdsSharedWithUser(10L)).thenReturn(List.of());

        assertTrue(service.filterReadableDocumentIds(List.of()).isEmpty());
        assertTrue(service.filterReadableDocumentIds(List.of(document(5L, 99L, 999L))).isEmpty());
    }

    // Helper

    private UserPrincipal principalWith(
            Long userId,
            Role role,
            Long departmentId,
            Set<Permission> permissions
    ) {

        User user = User.builder()
                .id(userId)
                .username("user" + userId)
                .password("x")
                .role(role)
                .enabled(true)
                .build();

        if (departmentId != null) {
            user.setDepartment(Department.builder().id(departmentId).name("dept-" + departmentId).build());
        }

        return UserPrincipal.from(user, permissions);
    }

    private Document document(Long documentId, Long ownerId, Long departmentId) {

        return Document.builder()
                .id(documentId)
                .title("doc-" + documentId)
                .owner(User.builder().id(ownerId).build())
                .department(departmentId == null
                        ? null
                        : Department.builder().id(departmentId).name("dept-" + departmentId).build())
                .build();
    }

    private UserPrincipal principal(Long userId, Role role, Long departmentId) {

        User user = User.builder()
                .id(userId)
                .username("user" + userId)
                .password("x")
                .role(role)
                .enabled(true)
                .build();

        if (departmentId != null) {
            user.setDepartment(Department.builder().id(departmentId).name("dept-" + departmentId).build());
        }

        return UserPrincipal.from(user, Set.of());
    }
}
