package com.iwas.task.service;

import com.iwas.common.enums.ErrorCode;
import com.iwas.common.exception.AppException;
import com.iwas.project.service.ProjectService;
import com.iwas.task.dto.TaskRequest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class TaskServiceTest {

    @Mock ProjectService projectService;

    @InjectMocks TaskService service;

    private static final long PROJECT_ID = 100L;
    private static final long MANAGER_ID = 999L;
    private static final long REPORTER_ID = 1L;

    /** A project manager is a participant of their project but must never be a task assignee. */
    @Test
    void createTaskRejectsProjectManagerAsAssignee() {
        TaskRequest request = new TaskRequest();
        request.setProjectId(PROJECT_ID);
        request.setAssigneeId(MANAGER_ID);
        when(projectService.isProjectParticipant(PROJECT_ID, MANAGER_ID)).thenReturn(true);
        when(projectService.isManagerOf(PROJECT_ID, MANAGER_ID)).thenReturn(true);

        AppException ex = assertThrows(AppException.class,
                () -> service.createTask(request, REPORTER_ID));
        assertEquals(ErrorCode.TASK_ASSIGNEE_CANNOT_BE_MANAGER, ex.getErrorCode());
    }

    /** A user who is not even a participant fails earlier, with the existing membership error. */
    @Test
    void createTaskRejectsNonParticipantAssignee() {
        TaskRequest request = new TaskRequest();
        request.setProjectId(PROJECT_ID);
        request.setAssigneeId(MANAGER_ID);
        when(projectService.isProjectParticipant(PROJECT_ID, MANAGER_ID)).thenReturn(false);

        AppException ex = assertThrows(AppException.class,
                () -> service.createTask(request, REPORTER_ID));
        assertEquals(ErrorCode.TASK_ASSIGNEE_NOT_PROJECT_MEMBER, ex.getErrorCode());
    }
}
