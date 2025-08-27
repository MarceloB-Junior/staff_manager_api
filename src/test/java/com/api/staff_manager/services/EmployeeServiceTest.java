package com.api.staff_manager.services;

import com.api.staff_manager.dtos.requests.EmployeeRequest;
import com.api.staff_manager.dtos.responses.EmployeeDetailsResponse;
import com.api.staff_manager.dtos.responses.EmployeeViewResponse;
import com.api.staff_manager.exceptions.custom.DepartmentNotFoundException;
import com.api.staff_manager.exceptions.custom.EmployeeExistsException;
import com.api.staff_manager.exceptions.custom.EmployeeNotFoundException;
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
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
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
    @DisplayName("Saving an employee request with a non-existing department throws DepartmentNotFoundException")
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

    @Test
    @DisplayName("Find all method returns a page of EmployeeViewResponse")
    void givenPageable_whenFindAll_thenReturnsPageOfEmployeeViewResponse() {
        Pageable pageable = PageRequest.of(0,2);

        var employee = new EmployeeModel();
        employee.setEmployeeId(UUID.randomUUID());
        employee.setName("John Doe");
        employee.setPosition("Developer");
        employee.setSalary(BigDecimal.valueOf(5000));

        var employeeViewResponse = new EmployeeViewResponse(employee.getEmployeeId(), employee.getName(),
                employee.getPosition(), employee.getSalary(), UUID.randomUUID());

        Page<EmployeeModel> employeePage = new PageImpl<>(List.of(employee), pageable,1);

        when(employeeRepository.findAll(pageable)).thenReturn(employeePage);
        when(employeeMapper.toViewResponse(employee)).thenReturn(employeeViewResponse);

        Page<EmployeeViewResponse> result = employeeService.findAll(pageable);

        assertNotNull(result);
        assertEquals(1, result.getTotalElements());
        assertEquals(employeeViewResponse, result.getContent().getFirst());
        verify(employeeRepository).findAll(pageable);
        verify(employeeMapper).toViewResponse(employee);
    }

    @Test
    @DisplayName("Find by id method returns EmployeeDetailsResponse when employee exists")
    void givenExistingEmployeeId_whenFindById_thenReturnEmployeeDetailsResponse() {
        UUID employeeId = UUID.randomUUID();
        var timestamp = LocalDateTime.now();

        var department = new DepartmentModel();
        department.setDepartmentId(UUID.randomUUID());

        var employee = new EmployeeModel();
        employee.setEmployeeId(employeeId);
        employee.setName("John Doe");
        employee.setPosition("Developer");
        employee.setSalary(BigDecimal.valueOf(5000));
        employee.setDepartment(department);
        employee.setCreatedAt(timestamp);
        employee.setUpdatedAt(timestamp);

        var employeeDetailsResponse = new EmployeeDetailsResponse(employeeId, "John Doe", "Developer",
                employee.getSalary(), null, department.getDepartmentId(), timestamp, timestamp);

        when(employeeRepository.findById(employeeId)).thenReturn(Optional.of(employee));
        when(employeeMapper.toDetailsResponse(employee)).thenReturn(employeeDetailsResponse);

        var response = employeeService.findById(employeeId);

        assertNotNull(response);
        assertEquals(employeeId, response.employeeId());
        assertEquals("John Doe", response.name());
        assertEquals("Developer", response.position());
        assertEquals(BigDecimal.valueOf(5000), response.salary());
        assertEquals(department.getDepartmentId(), response.departmentId());
        assertEquals(timestamp, response.createdAt());
        assertEquals(timestamp, response.updatedAt());
        verify(employeeRepository, times(1)).findById(employeeId);
        verify(employeeMapper, times(1)).toDetailsResponse(employee);
    }

    @Test
    @DisplayName("Find by id method throws EmployeeNotFoundException when employee does not exist")
    void givennNonExistingEmployeeId_whenFindById_thenThrowsEmployeeNotFoundException() {
        UUID employeeId = UUID.randomUUID();
        when(employeeRepository.findById(employeeId)).thenReturn(Optional.empty());

        var exception = assertThrows(EmployeeNotFoundException.class, () -> employeeService.findById(employeeId));

        assertEquals("Employee not found with id: " + employeeId, exception.getMessage());
        verify(employeeRepository, times(1)).findById(employeeId);
        verifyNoInteractions(employeeMapper);
    }
}