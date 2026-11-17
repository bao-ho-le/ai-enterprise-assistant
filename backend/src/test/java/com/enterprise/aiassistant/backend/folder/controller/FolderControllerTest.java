package com.enterprise.aiassistant.backend.folder.controller;

import com.enterprise.aiassistant.backend.common.exception.GlobalExceptionHandler;
import com.enterprise.aiassistant.backend.folder.dto.request.CreateFolderRequest;
import com.enterprise.aiassistant.backend.folder.dto.request.MoveFolderRequest;
import com.enterprise.aiassistant.backend.folder.dto.request.RenameFolderRequest;
import com.enterprise.aiassistant.backend.folder.dto.response.FolderContentsResponse;
import com.enterprise.aiassistant.backend.folder.dto.response.FolderResponse;
import com.enterprise.aiassistant.backend.folder.enums.FolderStatus;
import com.enterprise.aiassistant.backend.folder.service.FolderService;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;

import static org.hamcrest.Matchers.containsString;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(
        controllers = FolderController.class,
        excludeFilters = @ComponentScan.Filter(
                type = FilterType.REGEX,
                pattern = "com\\.enterprise\\.aiassistant\\.backend\\.auth\\.security\\..*"
        )
)
@AutoConfigureMockMvc(addFilters = false)
@Import(GlobalExceptionHandler.class)
@TestPropertySource(properties = "api.prefix=/api/v1")
class FolderControllerTest {

    private static final String BASE = "/api/v1/folders";

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private FolderService folderService;

    // ---------- helper ----------

    private FolderResponse fakeFolder(Long id, String name) {
        return FolderResponse.builder()
                .id(id)
                .name(name)
                .parentId(1L)
                .status(FolderStatus.ACTIVE)
                .createdAt(LocalDateTime.of(2026, 1, 15, 9, 30))
                .ownerId(10L)
                .ownerName("Huy")
                .departmentId(20L)
                .departmentName("Engineering")
                .build();
    }

    private FolderContentsResponse fakeContents() {
        return FolderContentsResponse.builder()
                .currentFolder(fakeFolder(4L, "Docs"))
                .subfolders(List.of(fakeFolder(5L, "Sub A"), fakeFolder(6L, "Sub B")))
                .build();
    }

    // ================= CREATE =================

    @Test
    void createFolder_tra200_vaBodyDuocBindDungField() throws Exception {
        when(folderService.createFolder(any(CreateFolderRequest.class)))
                .thenReturn(fakeFolder(100L, "Docs"));

        mockMvc.perform(post(BASE)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Docs\",\"parentId\":1}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(100))
                .andExpect(jsonPath("$.name").value("Docs"))
                .andExpect(jsonPath("$.parentId").value(1))
                .andExpect(jsonPath("$.status").value("ACTIVE"))
                .andExpect(jsonPath("$.ownerName").value("Huy"))
                .andExpect(jsonPath("$.departmentName").value("Engineering"));

        ArgumentCaptor<CreateFolderRequest> captor = ArgumentCaptor.forClass(CreateFolderRequest.class);
        verify(folderService).createFolder(captor.capture());
        assertEquals("Docs", captor.getValue().getName());
        assertEquals(1L, captor.getValue().getParentId());
    }

    @Test
    void createFolder_bodyRong_khongCoValid_serviceNhanGiaTriNull() throws Exception {
        // Ghi nhận hành vi hiện tại: controller KHÔNG có @Valid nên không chặn ở đây.
        // Nếu sau này thêm @Valid + @NotBlank, đổi test này thành kỳ vọng 400.
        when(folderService.createFolder(any(CreateFolderRequest.class)))
                .thenReturn(fakeFolder(100L, "x"));

        mockMvc.perform(post(BASE)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isOk());

        ArgumentCaptor<CreateFolderRequest> captor = ArgumentCaptor.forClass(CreateFolderRequest.class);
        verify(folderService).createFolder(captor.capture());
        assertNull(captor.getValue().getName());
        assertNull(captor.getValue().getParentId());
    }

    @Test
    void createFolder_khongCoBody_tra400_MALFORMED_REQUEST_BODY() throws Exception {
        mockMvc.perform(post(BASE).contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("MALFORMED_REQUEST_BODY"));

        verifyNoInteractions(folderService);
    }

    @Test
    void createFolder_jsonSaiKieu_tra400() throws Exception {
        // parentId phải là số, gửi chuỗi -> không đọc được body
        mockMvc.perform(post(BASE)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Docs\",\"parentId\":\"abc\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("MALFORMED_REQUEST_BODY"));

        verifyNoInteractions(folderService);
    }

    // ================= RENAME / MOVE =================

    @Test
    void renameFolder_truyenDungFolderId_vaName() throws Exception {
        when(folderService.renameFolder(eq(5L), any(RenameFolderRequest.class)))
                .thenReturn(fakeFolder(5L, "Moi"));

        mockMvc.perform(put(BASE + "/5")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Moi\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(5))
                .andExpect(jsonPath("$.name").value("Moi"));

        ArgumentCaptor<RenameFolderRequest> captor = ArgumentCaptor.forClass(RenameFolderRequest.class);
        verify(folderService).renameFolder(eq(5L), captor.capture());
        assertEquals("Moi", captor.getValue().getName());
    }

    @Test
    void moveFolder_truyenDungFolderId_vaTargetParentId() throws Exception {
        when(folderService.moveFolder(eq(5L), any(MoveFolderRequest.class)))
                .thenReturn(fakeFolder(5L, "Docs"));

        mockMvc.perform(put(BASE + "/5/move")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"targetParentId\":9}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(5));

        ArgumentCaptor<MoveFolderRequest> captor = ArgumentCaptor.forClass(MoveFolderRequest.class);
        verify(folderService).moveFolder(eq(5L), captor.capture());
        assertEquals(9L, captor.getValue().getTargetParentId());
    }

    // ================= DELETE / RESTORE / HARD DELETE =================

    @Test
    void deleteFolder_tra204_goiDeleteMem() throws Exception {
        mockMvc.perform(delete(BASE + "/7"))
                .andExpect(status().isNoContent());

        verify(folderService).deleteFolder(7L);
        verify(folderService, never()).hardDeleteFolder(anyLong());
    }

    @Test
    void restoreFolder_tra200() throws Exception {
        when(folderService.restoreFolder(7L)).thenReturn(fakeFolder(7L, "Docs"));

        mockMvc.perform(post(BASE + "/7/restore"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(7));

        verify(folderService).restoreFolder(7L);
    }

    @Test
    void hardDeleteFolder_tra204_vaKhongGoiDeleteMem() throws Exception {
        mockMvc.perform(delete(BASE + "/7/hard"))
                .andExpect(status().isNoContent());

        verify(folderService).hardDeleteFolder(7L);
        verify(folderService, never()).deleteFolder(anyLong());
    }

    // ================= GET DETAIL =================

    @Test
    void getFolderDetail_tra200() throws Exception {
        when(folderService.getFolderDetail(3L)).thenReturn(fakeFolder(3L, "Docs"));

        mockMvc.perform(get(BASE + "/3"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(3))
                .andExpect(jsonPath("$.name").value("Docs"));
    }

    @Test
    void getFolderDetail_idKhongPhaiSo_tra400_TYPE_MISMATCH() throws Exception {
        mockMvc.perform(get(BASE + "/abc"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("TYPE_MISMATCH"))
                .andExpect(jsonPath("$.message").value(containsString("folderId")));

        verifyNoInteractions(folderService);
    }

    // ================= ROUTING: literal path không bị nuốt bởi /{folderId} =================

    @Test
    void deleted_duocRouteDen_getDeletedFolders_chuKhongPhaiGetFolderDetail() throws Exception {
        FolderResponse trashed = fakeFolder(1L, "Cu");
        trashed.setStatus(FolderStatus.DELETED);
        trashed.setDeletedAt(LocalDateTime.of(2026, 2, 1, 8, 0));
        trashed.setDeletedByName("Admin");
        when(folderService.getDeletedFolders(eq("abc"), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(trashed)));

        mockMvc.perform(get(BASE + "/deleted").param("keyword", "abc"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].name").value("Cu"))
                .andExpect(jsonPath("$.content[0].status").value("DELETED"))
                .andExpect(jsonPath("$.content[0].deletedByName").value("Admin"));

        verify(folderService, never()).getFolderDetail(anyLong());
    }

    @Test
    void deleted_khongCoKeyword_vanHopLe_keywordLaNull() throws Exception {
        when(folderService.getDeletedFolders(isNull(), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of()));

        mockMvc.perform(get(BASE + "/deleted"))
                .andExpect(status().isOk());

        verify(folderService).getDeletedFolders(isNull(), any(Pageable.class));
    }

    @Test
    void search_coKeyword_tra200() throws Exception {
        when(folderService.searchFolders(eq("doc"), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(fakeFolder(1L, "Docs"))));

        mockMvc.perform(get(BASE + "/search").param("keyword", "doc"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].name").value("Docs"));

        verify(folderService, never()).getFolderDetail(anyLong());
    }

    @Test
    void search_thieuKeyword_tra400_MISSING_PARAMETER() throws Exception {
        mockMvc.perform(get(BASE + "/search"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("MISSING_PARAMETER"))
                .andExpect(jsonPath("$.message").value(containsString("keyword")));

        verifyNoInteractions(folderService);
    }

    // ================= PHÂN TRANG =================

    @Test
    void search_macDinh_page0_size20() throws Exception {
        when(folderService.searchFolders(eq("doc"), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of()));

        mockMvc.perform(get(BASE + "/search").param("keyword", "doc"))
                .andExpect(status().isOk());

        ArgumentCaptor<Pageable> captor = ArgumentCaptor.forClass(Pageable.class);
        verify(folderService).searchFolders(eq("doc"), captor.capture());
        assertEquals(0, captor.getValue().getPageNumber());
        assertEquals(20, captor.getValue().getPageSize());
    }

    @Test
    void search_truyenPageVaSize_duocChuyenVaoService() throws Exception {
        when(folderService.searchFolders(eq("doc"), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of()));

        mockMvc.perform(get(BASE + "/search")
                        .param("keyword", "doc")
                        .param("page", "2")
                        .param("size", "5"))
                .andExpect(status().isOk());

        ArgumentCaptor<Pageable> captor = ArgumentCaptor.forClass(Pageable.class);
        verify(folderService).searchFolders(eq("doc"), captor.capture());
        assertEquals(2, captor.getValue().getPageNumber());
        assertEquals(5, captor.getValue().getPageSize());
    }

    // ================= CONTENTS =================

    @Test
    void getFolderContents_truyenDungFolderId_vaTraVeNoiDung() throws Exception {
        when(folderService.getFolderContents(eq(4L), any(Pageable.class)))
                .thenReturn(fakeContents());

        mockMvc.perform(get(BASE + "/4/contents"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.currentFolder.id").value(4))
                .andExpect(jsonPath("$.currentFolder.name").value("Docs"))
                .andExpect(jsonPath("$.subfolders.length()").value(2))
                .andExpect(jsonPath("$.subfolders[0].name").value("Sub A"))
                .andExpect(jsonPath("$.subfolders[1].name").value("Sub B"));

        verify(folderService).getFolderContents(eq(4L), any(Pageable.class));
    }

    @Test
    void getRootContents_goiServiceVoiFolderIdLaNull() throws Exception {
        when(folderService.getFolderContents(isNull(), any(Pageable.class)))
                .thenReturn(fakeContents());

        mockMvc.perform(get(BASE + "/root/contents"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.currentFolder.id").value(4));

        verify(folderService).getFolderContents(isNull(), any(Pageable.class));
        verify(folderService, never()).getFolderDetail(anyLong());
    }
}