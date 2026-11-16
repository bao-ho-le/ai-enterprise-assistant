package com.enterprise.aiassistant.backend.document.helper;

import com.enterprise.aiassistant.backend.auth.security.UserPrincipal;
import com.enterprise.aiassistant.backend.department.entity.Department;
import com.enterprise.aiassistant.backend.document.entity.Document;
import com.enterprise.aiassistant.backend.user.entity.User;
import com.enterprise.aiassistant.backend.user.enums.Permission;
import com.enterprise.aiassistant.backend.user.enums.Role;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Pure RBAC/ABAC matrix for {@link DocumentAuthorizationHelper}. No mocks, no context.
 */
class DocumentAuthorizationHelperTest {

    private static final long IT_DEPARTMENT = 1L;

    private static final long HR_DEPARTMENT = 2L;

    private final DocumentAuthorizationHelper helper = new DocumentAuthorizationHelper();

    @Test
    void shouldDenyReadWhenPrincipalHasNoReadPermission() {

        UserPrincipal principal = principal(10L, Role.EMPLOYEE, IT_DEPARTMENT, Set.of());

        assertThat(helper.canRead(principal, document(1L, 99L, IT_DEPARTMENT), false)).isFalse();
    }

    @Test
    void shouldAllowAdminAndSupervisorToReadAnyDocument() {

        UserPrincipal admin = principal(1L, Role.ADMIN, null, Set.of(Permission.DOCUMENT_READ));
        UserPrincipal supervisor = principal(2L, Role.SUPERVISOR, null, Set.of(Permission.DOCUMENT_READ));

        Document otherDepartment = document(50L, 99L, HR_DEPARTMENT);

        assertThat(helper.canRead(admin, otherDepartment, false)).isTrue();
        assertThat(helper.canRead(supervisor, otherDepartment, false)).isTrue();
    }

    @Test
    void shouldAllowOwnerToReadOwnDocument() {

        UserPrincipal employee = principal(10L, Role.EMPLOYEE, IT_DEPARTMENT, Set.of(Permission.DOCUMENT_READ));

        assertThat(helper.canRead(employee, document(1L, 10L, HR_DEPARTMENT), false)).isTrue();
    }

    @Test
    void shouldAllowExplicitlySharedDocument() {

        UserPrincipal employee = principal(10L, Role.EMPLOYEE, IT_DEPARTMENT, Set.of(Permission.DOCUMENT_READ));

        assertThat(helper.canRead(employee, document(1L, 99L, HR_DEPARTMENT), true)).isTrue();
    }

    @Test
    void shouldTreatDocumentWithoutDepartmentAsShared() {

        UserPrincipal employee = principal(10L, Role.EMPLOYEE, IT_DEPARTMENT, Set.of(Permission.DOCUMENT_READ));

        assertThat(helper.canRead(employee, document(1L, 99L, null), false)).isTrue();
    }

    @Test
    void shouldAllowSameDepartmentAndDenyOtherDepartment() {

        UserPrincipal employee = principal(10L, Role.EMPLOYEE, IT_DEPARTMENT, Set.of(Permission.DOCUMENT_READ));

        assertThat(helper.canRead(employee, document(1L, 99L, IT_DEPARTMENT), false)).isTrue();
        assertThat(helper.canRead(employee, document(2L, 99L, HR_DEPARTMENT), false)).isFalse();
    }

    @Test
    void shouldRequireDownloadPermissionInAdditionToReadAccess() {

        UserPrincipal readOnly = principal(10L, Role.EMPLOYEE, IT_DEPARTMENT, Set.of(Permission.DOCUMENT_READ));
        UserPrincipal withDownload = principal(10L, Role.EMPLOYEE, IT_DEPARTMENT,
                Set.of(Permission.DOCUMENT_READ, Permission.DOCUMENT_DOWNLOAD));

        Document ownDocument = document(1L, 10L, IT_DEPARTMENT);

        assertThat(helper.canDownload(readOnly, ownDocument, false)).isFalse();
        assertThat(helper.canDownload(withDownload, ownDocument, false)).isTrue();
    }

    @Test
    void shouldDenyDownloadWhenReadAccessIsDenied() {

        UserPrincipal withDownload = principal(10L, Role.EMPLOYEE, IT_DEPARTMENT,
                Set.of(Permission.DOCUMENT_READ, Permission.DOCUMENT_DOWNLOAD));

        assertThat(helper.canDownload(withDownload, document(1L, 99L, HR_DEPARTMENT), false)).isFalse();
    }

    @Test
    void shouldLetOwnerUpdateOwnDocumentWithoutDepartment() {

        UserPrincipal employee = principal(10L, Role.EMPLOYEE, IT_DEPARTMENT, Set.of(Permission.DOCUMENT_UPDATE));

        assertThat(helper.canUpdate(employee, document(1L, 10L, null))).isTrue();
    }

    @Test
    void shouldDenyOwnerUpdateWhenOwnerBelongsToAnotherDepartment() {

        UserPrincipal employeeWithoutDepartment = principal(
                10L, Role.EMPLOYEE, null, Set.of(Permission.DOCUMENT_UPDATE));

        assertThat(helper.canUpdate(employeeWithoutDepartment, document(1L, 10L, IT_DEPARTMENT))).isFalse();
    }

    @Test
    void shouldDenyEmployeeUpdateForColleagueDocument() {

        UserPrincipal employee = principal(10L, Role.EMPLOYEE, IT_DEPARTMENT, Set.of(Permission.DOCUMENT_UPDATE));

        assertThat(helper.canUpdate(employee, document(1L, 11L, IT_DEPARTMENT))).isFalse();
    }

    @Test
    void shouldLetManagerModifyOwnDepartmentDocuments() {

        UserPrincipal manager = principal(20L, Role.MANAGER, IT_DEPARTMENT,
                Set.of(Permission.DOCUMENT_UPDATE, Permission.DOCUMENT_DELETE, Permission.DOCUMENT_MANAGE_ACCESS));

        Document itDocument = document(1L, 11L, IT_DEPARTMENT);
        Document hrDocument = document(2L, 11L, HR_DEPARTMENT);

        assertThat(helper.canUpdate(manager, itDocument)).isTrue();
        assertThat(helper.canDelete(manager, itDocument)).isTrue();
        assertThat(helper.canManageAccess(manager, itDocument)).isTrue();

        assertThat(helper.canUpdate(manager, hrDocument)).isFalse();
        assertThat(helper.canDelete(manager, hrDocument)).isFalse();
    }

    @Test
    void shouldNeverLetSupervisorModifyDocuments() {

        UserPrincipal supervisor = principal(30L, Role.SUPERVISOR, null,
                Set.of(Permission.DOCUMENT_UPDATE, Permission.DOCUMENT_DELETE, Permission.DOCUMENT_MANAGE_ACCESS));

        Document itDocument = document(1L, 11L, IT_DEPARTMENT);

        assertThat(helper.canUpdate(supervisor, itDocument)).isFalse();
        assertThat(helper.canDelete(supervisor, itDocument)).isFalse();
        assertThat(helper.canManageAccess(supervisor, itDocument)).isFalse();
    }

    @Test
    void shouldLetAdminModifyAnyDocument() {

        UserPrincipal admin = principal(1L, Role.ADMIN, null,
                Set.of(Permission.DOCUMENT_UPDATE, Permission.DOCUMENT_DELETE, Permission.DOCUMENT_MANAGE_ACCESS));

        Document hrDocument = document(2L, 99L, HR_DEPARTMENT);

        assertThat(helper.canUpdate(admin, hrDocument)).isTrue();
        assertThat(helper.canDelete(admin, hrDocument)).isTrue();
        assertThat(helper.canManageAccess(admin, hrDocument)).isTrue();
    }

    @Test
    void shouldDenyModificationWhenPermissionIsMissingEvenForAdmin() {

        UserPrincipal adminWithoutPermission = principal(1L, Role.ADMIN, null, Set.of());

        assertThat(helper.canUpdate(adminWithoutPermission, document(1L, 99L, IT_DEPARTMENT))).isFalse();
    }

    @Test
    void shouldDenyModificationWhenDocumentHasNoOwner() {

        UserPrincipal employee = principal(10L, Role.EMPLOYEE, IT_DEPARTMENT, Set.of(Permission.DOCUMENT_UPDATE));

        Document orphanDocument = Document.builder().id(1L).title("orphan").build();

        assertThat(helper.canUpdate(employee, orphanDocument)).isFalse();
    }

    @Test
    void shouldCheckCreatePermission() {

        assertThat(helper.canCreate(principal(10L, Role.EMPLOYEE, IT_DEPARTMENT, Set.of()))).isFalse();
        assertThat(helper.canCreate(principal(10L, Role.EMPLOYEE, IT_DEPARTMENT,
                Set.of(Permission.DOCUMENT_CREATE)))).isTrue();
    }

    // ===================== helpers =====================

    private static UserPrincipal principal(
            long userId,
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

    private static Document document(Long id, Long ownerId, Long departmentId) {

        return Document.builder()
                .id(id)
                .title("doc-" + id)
                .owner(ownerId == null ? null : User.builder().id(ownerId).build())
                .department(departmentId == null
                        ? null
                        : Department.builder().id(departmentId).name("dept-" + departmentId).build())
                .build();
    }
}
