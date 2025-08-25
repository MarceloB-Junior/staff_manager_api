package com.api.staff_manager.services;

import com.api.staff_manager.dtos.requests.EmployeeRequest;
import com.api.staff_manager.dtos.responses.EmployeeViewResponse;
import com.api.staff_manager.exceptions.custom.DepartmentNotFoundException;
import com.api.staff_manager.exceptions.custom.EmployeeExistsException;
import com.api.staff_manager.mappers.EmployeeMapper;
import com.api.staff_manager.models.DepartmentModel;
import com.api.staff_manager.models.EmployeeModel;
import com.api.staff_manager.repositories.DepartmentRepository;
import com.api.staff_manager.repositories.EmployeeRepository;
import com.api.staff_manager.services.impl.EmployeeServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EmployeeServiceTest {

    @Mock
    private EmployeeRepository employeeRepository;

    @Mock
    private DepartmentRepository departmentRepository;

    @Mock
    private EmployeeMapper employeeMapper;

    @Mock
    private PhotoService photoService;

    private EmployeeService employeeService;

    @BeforeEach
    void setUp() {
        employeeService = new EmployeeServiceImpl(employeeRepository, departmentRepository, employeeMapper, photoService);
    }

    @Test
    @DisplayName("Save valid employee request returns EmployeeViewResponse")
    void givenValidEmployeeRequest_whenSave_thenReturnEmployeeViewResponse() {
        UUID departmentId = UUID.randomUUID();
        var request = new EmployeeRequest("John Doe", "Developer", BigDecimal.valueOf(5000), departmentId);

        var department = new DepartmentModel();
        department.setDepartmentId(departmentId);

        var employee = new EmployeeModel();
        var savedEmployee = new EmployeeModel();

        var employeeResponse = new EmployeeViewResponse(UUID.randomUUID(), "John Doe", "Developer",
                BigDecimal.valueOf(5000), departmentId);

        when(departmentRepository.findById(departmentId)).thenReturn(Optional.of(department));
        when(employeeRepository.existsByNameAndDepartment(request.name(), department)).thenReturn(false);
        when(employeeMapper.toEntity(request)).thenReturn(employee);
        when(employeeRepository.save(employee)).thenReturn(savedEmployee);
        when(employeeMapper.toViewResponse(savedEmployee)).thenReturn(employeeResponse);

        var actualResponse = employeeService.save(request);

        assertEquals(employeeResponse, actualResponse);
        verify(departmentRepository).findById(departmentId);
        verify(employeeRepository).existsByNameAndDepartment(request.name(), department);
        verify(employeeMapper).toEntity(request);
        verify(employeeRepository).save(employee);
        verify(employeeMapper).toViewResponse(savedEmployee);
    }

    @Test
    @DisplayName("Saving existing employee name in department throws EmployeeExistsException")
    void givenExistingEmployeeNameInDepartment_whenSave_thenThrowEmployeeExistsException(){
        UUID departmentId = UUID.randomUUID();
        var request = new EmployeeRequest("John Doe", "Developer", BigDecimal.valueOf(5000), departmentId);

        var department = new DepartmentModel();
        department.setDepartmentId(departmentId);

        when(departmentRepository.findById(departmentId)).thenReturn(Optional.of(department));
        when(employeeRepository.existsByNameAndDepartment(request.name(), department)).thenReturn(true);

        var exception = assertThrows(EmployeeExistsException.class, () -> employeeService.save(request));
        assertEquals("An employee with this name already exists in the department.", exception.getMessage());

        verify(departmentRepository).findById(departmentId);
        verify(employeeRepository).existsByNameAndDepartment(request.name(), department);
        verify(employeeMapper, never()).toEntity(any());
        verify(employeeRepository, never()).save(any());
    }

    @Test
    @DisplayName("Saving a employee request with non-existing department throws DepartmentNotFoundException")
    void givenInvalidDepartmentId_whenSave_thenThrowDepartmentNotFoundException() {
        UUID departmentId = UUID.randomUUID();
        var request = new EmployeeRequest("John Doe", "Developer", BigDecimal.valueOf(5000), departmentId);

        when(departmentRepository.findById(departmentId)).thenReturn(Optional.empty());

        var exception = assertThrows(DepartmentNotFoundException.class, () -> employeeService.save(request));

        assertEquals("Department not found with id: " + departmentId, exception.getMessage());
        verify(departmentRepository).findById(departmentId);
        verify(employeeRepository, never()).existsByNameAndDepartment(any(), any());
        verify(employeeMapper, never()).toEntity(any());
        verify(employeeRepository, never()).save(any());
    }
}