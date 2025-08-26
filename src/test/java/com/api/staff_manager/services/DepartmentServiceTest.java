package com.api.staff_manager.services;

import com.api.staff_manager.dtos.requests.DepartmentRequest;
import com.api.staff_manager.dtos.responses.DepartmentDetailsResponse;
import com.api.staff_manager.dtos.responses.DepartmentViewResponse;
import com.api.staff_manager.dtos.responses.EmployeeSummaryResponse;
import com.api.staff_manager.exceptions.custom.DepartmentExistsException;
import com.api.staff_manager.exceptions.custom.DepartmentNotFoundException;
import com.api.staff_manager.mappers.DepartmentMapper;
import com.api.staff_manager.models.DepartmentModel;
import com.api.staff_manager.models.EmployeeModel;
import com.api.staff_manager.repositories.DepartmentRepository;
import com.api.staff_manager.services.impl.DepartmentServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DepartmentServiceTest {

    @Mock
    private DepartmentRepository departmentRepository;

    @Mock
    private DepartmentMapper departmentMapper;

    @Mock
    private PhotoService photoService;

    private DepartmentService departmentService;

    @BeforeEach
    void setUp() {
        departmentService = new DepartmentServiceImpl(departmentRepository,departmentMapper,photoService);
    }

    @Test
    @DisplayName("Save valid department request returns DepartmentViewResponse")
    void givenValidDepartmentRequest_whenSave_thenReturnDepartmentViewResponse(){
        var request = new DepartmentRequest("Finance");
        when(departmentRepository.existsByName(request.name())).thenReturn(false);

        var department = new DepartmentModel();
        var savedDepartment = new DepartmentModel();
        when(departmentMapper.toEntity(request)).thenReturn(department);
        when(departmentRepository.save(department)).thenReturn(savedDepartment);

        var departmentResponse = new DepartmentViewResponse(UUID.randomUUID(),"Finance");
        when(departmentMapper.toViewResponse(savedDepartment)).thenReturn(departmentResponse);

        var response = departmentService.save(request);

        assertEquals(departmentResponse,response);
        verify(departmentRepository).existsByName(request.name());
        verify(departmentRepository).save(department);
    }

    @Test
    @DisplayName("Saving existing department name throws DepartmentExistsException")
    void givenExistingDepartmentName_whenSave_thenThrowDepartmentExistsException(){
        var request = new DepartmentRequest("Finance");
        when(departmentRepository.existsByName(request.name())).thenReturn(true);

        var exception = assertThrows(DepartmentExistsException.class, () -> departmentService.save(request));

        assertEquals("Department already exists.", exception.getMessage());
        verify(departmentRepository).existsByName(request.name());
        verify(departmentRepository, times(0)).save(any());
    }

    @Test
    @DisplayName("Find all method returns a page of DepartmentViewResponse")
    void givenPageable_whenFindAll_thenReturnsPageOfDepartmentViewResponse(){
        Pageable pageable = PageRequest.of(0,2);

        var departmentId1 = UUID.randomUUID();
        var departmentId2 = UUID.randomUUID();

        var department1 = new DepartmentModel();
        department1.setDepartmentId(departmentId1);
        department1.setName("Finance");

        var department2 = new DepartmentModel();
        department2.setDepartmentId(departmentId2);
        department2.setName("Human Resources");

        Page<DepartmentModel> departmentPage = new PageImpl<>(List.of(department1,department2), pageable, 2);

        var departmentResponse1 = new DepartmentViewResponse(departmentId1,"Finance");
        var departmentResponse2 = new DepartmentViewResponse(departmentId2,"Human Resources");

        when(departmentRepository.findAll(pageable)).thenReturn(departmentPage);
        when(departmentMapper.toViewResponse(department1)).thenReturn(departmentResponse1);
        when(departmentMapper.toViewResponse(department2)).thenReturn(departmentResponse2);

        Page<DepartmentViewResponse> result = departmentService.findAll(pageable);

        assertNotNull(result);
        assertEquals(2, result.getContent().size());
        assertEquals(departmentId1 , result.getContent().get(0).departmentId());
        assertEquals("Finance", result.getContent().get(0).name());
        assertEquals(departmentId2, result.getContent().get(1).departmentId());
        assertEquals("Human Resources", result.getContent().get(1).name());

        verify(departmentRepository).findAll(pageable);
        verify(departmentMapper).toViewResponse(department1);
        verify(departmentMapper).toViewResponse(department2);
    }

    @Test
    @DisplayName("Find by id method returns DepartmentDetailsResponse when department exists")
    void givenExistingDepartmentId_whenFindById_thenReturnDepartmentDetailsResponse(){
        var departmentId = UUID.randomUUID();
        var timestamp = LocalDateTime.now();

        var department = new DepartmentModel();
        department.setDepartmentId(departmentId);
        department.setName("Finance");
        department.setEmployees(null);
        department.setCreatedAt(timestamp);
        department.setUpdatedAt(timestamp);

        List<EmployeeSummaryResponse> employees = new ArrayList<>();

        var expectedResponse = new DepartmentDetailsResponse(departmentId,"Finance", employees,timestamp,timestamp);

        when(departmentRepository.findById(departmentId)).thenReturn(Optional.of(department));
        when(departmentMapper.toDetailsResponse(department)).thenReturn(expectedResponse);

        var actualResponse = departmentService.findById(departmentId);

        assertNotNull(actualResponse);
        assertEquals(expectedResponse.departmentId(), actualResponse.departmentId());
        assertEquals(expectedResponse.name(), actualResponse.name());
        assertEquals(expectedResponse.employees(), actualResponse.employees());
        assertEquals(expectedResponse.createdAt(), actualResponse.createdAt());
        assertEquals(expectedResponse.updatedAt(), actualResponse.updatedAt());

        verify(departmentRepository).findById(departmentId);
        verify(departmentMapper).toDetailsResponse(department);
    }

    @Test
    @DisplayName("Find by id method throws DepartmentNotFoundException when department does not exist")
    void givenNonExistingDepartmentId_whenFindById_thenThrowException() {
        var departmentId = UUID.randomUUID();
        when(departmentRepository.findById(departmentId)).thenReturn(Optional.empty());

        Exception exception = assertThrows(DepartmentNotFoundException.class, () -> departmentService.findById(departmentId));

        assertEquals("Department not found with id: " + departmentId, exception.getMessage());
        verify(departmentRepository).findById(departmentId);
        verifyNoInteractions(departmentMapper);
    }

    @Test
    @DisplayName("Update existing and valid department request returns DepartmentDetailsResponse")
    void givenExistingDepartmentAndValidRequest_whenUpdate_thenReturnDepartmentDetailsResponse() {
        var departmentId = UUID.randomUUID();
        var timestamp = LocalDateTime.now();
        var request = new DepartmentRequest("Finance 2");

        var existingDepartment = new DepartmentModel();
        existingDepartment.setDepartmentId(departmentId);
        existingDepartment.setName("Finance");
        existingDepartment.setEmployees(null);
        existingDepartment.setCreatedAt(timestamp);
        existingDepartment.setUpdatedAt(timestamp);

        var savedDepartment = new DepartmentModel();
        savedDepartment.setDepartmentId(departmentId);
        savedDepartment.setName("Finance 2");
        savedDepartment.setEmployees(null);
        savedDepartment.setCreatedAt(timestamp);
        savedDepartment.setUpdatedAt(timestamp);

        List<EmployeeSummaryResponse> employees = new ArrayList<>();

        var expectedResponse = new DepartmentDetailsResponse(departmentId,"Finance 2", employees,timestamp,timestamp);

        when(departmentRepository.findById(departmentId)).thenReturn(Optional.of(existingDepartment));
        /* The condition "departmentRepository.existsByName(request.name()) && !department.getName().equals(request.name())"
        is being mocked here to simplify this test */
        when(departmentRepository.existsByName(request.name())).thenReturn(false);
        when(departmentRepository.save(existingDepartment)).thenReturn(savedDepartment);
        when(departmentMapper.toDetailsResponse(savedDepartment)).thenReturn(expectedResponse);

        var actualResponse = departmentService.update(request, departmentId);

        assertEquals(expectedResponse, actualResponse);
        assertEquals("Finance 2", existingDepartment.getName());

        verify(departmentRepository).findById(departmentId);
        verify(departmentRepository).existsByName(request.name());
        verify(departmentRepository).save(existingDepartment);
        verify(departmentMapper).toDetailsResponse(savedDepartment);
    }

    @Test
    @DisplayName("Update with non-existent department id throws DepartmentNotFoundException")
    void givenNonExistentDepartmentId_whenUpdate_thenThrowDepartmentNotFoundException() {
        var departmentId = UUID.randomUUID();
        var request = new DepartmentRequest("Finance 2");

        when(departmentRepository.findById(departmentId)).thenReturn(Optional.empty());

        var exception = assertThrows(DepartmentNotFoundException.class, () -> departmentService.update(request, departmentId));
        assertEquals("Department not found with id: " + departmentId, exception.getMessage());

        verify(departmentRepository).findById(departmentId);
        verify(departmentRepository, never()).existsByName(any());
        verify(departmentRepository, never()).save(any());
        verifyNoInteractions(departmentMapper);
    }

    @Test
    @DisplayName("Update with existing name in another department throws DepartmentExistsException")
    void givenExistingNameInAnotherDepartment_whenUpdate_thenThrowDepartmentExistsException() {
        var departmentId = UUID.randomUUID();
        var request = new DepartmentRequest("Human Resources");

        var existingDepartment = new DepartmentModel();
        existingDepartment.setDepartmentId(departmentId);
        existingDepartment.setName("Finance");

        when(departmentRepository.findById(departmentId)).thenReturn(Optional.of(existingDepartment));
        /* The condition "departmentRepository.existsByName(request.name()) && !department.getName().equals(request.name())"
        is being mocked here to simplify this test */
        when(departmentRepository.existsByName(request.name())).thenReturn(true);

        var exception = assertThrows(DepartmentExistsException.class, () -> departmentService.update(request, departmentId));
        assertEquals("Department already exists.", exception.getMessage());

        verify(departmentRepository).findById(departmentId);
        verify(departmentRepository).existsByName(request.name());
        verify(departmentRepository, never()).save(any());
        verifyNoInteractions(departmentMapper);
    }

    @Test
    @DisplayName("Delete department removes all employees, deletes existing employee photos, and deletes the department")
    void givenExistingDepartmentId_whenDelete_thenDepartmentDeletedSuccessfully() {
        UUID departmentId = UUID.randomUUID();

        var employee1 = new EmployeeModel();
        employee1.setEmployeeId(UUID.randomUUID());
        employee1.setEmployeePhoto("http://localhost:8080/api/v1/employees/" + employee1.getEmployeeId() + "/photo");

        var employee2 = new EmployeeModel();
        employee2.setEmployeeId(UUID.randomUUID());
        employee2.setEmployeePhoto(null);

        var department = new DepartmentModel();
        department.setDepartmentId(departmentId);
        department.setEmployees(new ArrayList<>(List.of(employee1,employee2)));

        when(departmentRepository.findById(departmentId)).thenReturn(Optional.of(department));

        departmentService.delete(departmentId);
        assertTrue(department.getEmployees().isEmpty());

        verify(departmentRepository).delete(department);
        verify(photoService, never()).deletePhoto(employee2.getEmployeeId());
        verify(photoService).deletePhoto(employee1.getEmployeeId());
    }

    @Test
    @DisplayName("Delete department throws DepartmentNotFoundException when not found")
    void givenNonExistingDepartment_whenDelete_thenThrowDepartmentNotFoundException() {
        UUID departmentId = UUID.randomUUID();

        when(departmentRepository.findById(departmentId)).thenReturn(Optional.empty());

        var exception = assertThrows(DepartmentNotFoundException.class, () -> departmentService.delete(departmentId));

        assertEquals("Department not found with id: " + departmentId, exception.getMessage());

        verify(departmentRepository).findById(departmentId);
        verifyNoInteractions(photoService);
        verify(departmentRepository, never()).delete(any());
    }
}