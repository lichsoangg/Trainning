package nextcore.employees_manager.service;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.modelmapper.ModelMapper;
import org.modelmapper.convention.MatchingStrategies;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import io.micrometer.common.util.StringUtils;
import jakarta.transaction.Transactional;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import nextcore.employees_manager.DTO.CertificationDTO;
import nextcore.employees_manager.DTO.EmployeeDto;
import nextcore.employees_manager.DTO.EmployeesCertificationsDTO;
import nextcore.employees_manager.DTO.EmployeesDTO;
import nextcore.employees_manager.entity.Certification;
import nextcore.employees_manager.entity.Department;
import nextcore.employees_manager.entity.Employee;
import nextcore.employees_manager.entity.EmployeesCertifications;
import nextcore.employees_manager.exception.FieldEmptyException;
import nextcore.employees_manager.exception.FieldFormatException;
import nextcore.employees_manager.exception.FieldLengthExceededException;
import nextcore.employees_manager.exception.MessageErrorValidate;
import nextcore.employees_manager.exception.ResourceNotFoundException;
import nextcore.employees_manager.respository.CertificationRepository;
import nextcore.employees_manager.respository.DepartmentRepository;
import nextcore.employees_manager.respository.EmployeeRepository;
import nextcore.employees_manager.respository.EmployeesCertificationsRepository;

@Service
@RequiredArgsConstructor
@Transactional
@Slf4j
public class EmployeeServiceImpl implements EmployeeService {

	@Autowired
	private EmployeeRepository eRepository;
	@Autowired
	private MessageErrorValidate mError;
	@Autowired
	private CertificationRepository cRepository;
	@Autowired
	private DepartmentRepository dRepository;
	@Autowired
	private EmployeesCertificationsRepository ecRepository;
	private PasswordEncoder passwordEncoder;

	public ResponseEntity<Object> validateEmployee(Employee employee) {
	    String employeeLoginId = employee.getEmployeeLoginId();
	    String employeeLoginPassword = employee.getEmployeeLoginPassword();
	    String employeeName = employee.getEmployeeName();
	    String employeeNameKana = employee.getEmployeeNameKana();
	    String employeeEmail = employee.getEmployeeEmail();
	    String employeeTelephone = employee.getEmployeeTelephone();
	    LocalDate employeeBirthdate = employee.getEmployeeBirthDate();
	    BigDecimal departmentId = employee.getDepartment().getDepartmentId();
	    if (employeeLoginId == null || employeeLoginId.isEmpty()) {
			return mError.messageErrorValidate("ER001");
		}
		if (employeeLoginId.length() > 50) {
			return mError.messageErrorValidate("ER006");
		}

		if (!employeeLoginId.matches("[a-zA-Z0-9_]+") || Character.isDigit(employeeLoginId.charAt(0))) {
			return mError.messageErrorValidate("ER019");
		}
		Optional<Employee> existingEmployeeLoginId = eRepository.findByEmployeeLoginId(employeeLoginId);
		Optional<Employee> existingEmployeeName = eRepository.findByEmployeeName(employeeName);

		if (employeeLoginId == null || employeeLoginId.isEmpty()) {
			return mError.messageErrorValidate("ER003");
		}

		if (employeeName == null || employeeName.isEmpty() || existingEmployeeName.isPresent()) {
			return mError.messageErrorValidate("ER001");
		}
		if (employeeName.length() > 125) {
			return mError.messageErrorValidate("ER006");
		}
		if (employeeNameKana == null || employeeNameKana.isEmpty()) {
			return mError.messageErrorValidate("ER001");
		}
		if (!employeeNameKana.matches("^[ァ-ンー－]*$")){
			return mError.messageErrorValidate("ER009");
		}
		if (employeeBirthdate == null) {
			return mError.messageErrorValidate("ER001");
		}
//        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
//        LocalDate birthdate = LocalDate.parse(employeeBirthdate, formatter);
//        if (birthdate == null) {
//            return mError.messageErrorValidate("ER011", "生年月日"); // Handle when employeeBirthdate is not a valid date
//        }
		if (employeeEmail == null || employeeEmail.isEmpty()) {
			return mError.messageErrorValidate("ER001");
		}
		if (employeeEmail.length() > 125) {
			return mError.messageErrorValidate("ER006");
		}
		if (employeeTelephone == null || employeeTelephone.isEmpty()) {
			return mError.messageErrorValidate("ER001");
		}
		if (employeeTelephone.length() > 50) {
			return mError.messageErrorValidate("ER006");
		}
		byte[] bytes = employeeTelephone.getBytes();
		for (byte b : bytes) {
			if (b < -128 || b > 127) {
				return mError.messageErrorValidate("ER008");
			}
		}
		if (employeeLoginPassword == null || employeeLoginPassword.isEmpty()) {
			return mError.messageErrorValidate("ER001");
		}
		if (employeeLoginPassword.length() < 8 || employeeLoginPassword.length() > 50) {
			return mError.messageErrorValidate("ER007");
		}
		if (departmentId == null || departmentId.compareTo(BigDecimal.ZERO) <= 0) {
		    return mError.messageErrorValidate("ER002"); 
		}
		Department existingDepartmentId = dRepository.findByDepartmentId(departmentId);
		if (existingDepartmentId == null) {
			return mError.messageErrorValidate("ER004");
		}
		if (departmentId.compareTo(BigDecimal.ZERO) <= 0 || departmentId.stripTrailingZeros().scale() != 0) {
			return mError.messageErrorValidate("ER018");
		}
	    return null;
	}
	@Transactional
	@Override
	public ResponseEntity<Object> addEmployee(Employee employee) {
		ResponseEntity<Object> errorResponse = validateEmployee(employee);
	    if (errorResponse != null) {
	        return errorResponse;
	    }
		eRepository.save(employee);
		Map<String, Object> map = new HashMap<>();
		map.put("code", 200);
		map.put("employees", employee);
		return new ResponseEntity<>(map, HttpStatus.OK);
	}

	@Override
	public Optional<EmployeesDTO> getEmployeeById(Long id) {

		Optional<EmployeesDTO> employeeOptional = eRepository.getEmployeeById(id);
		if (!employeeOptional.isPresent()) {
			throw new ResourceNotFoundException("Nhân viên có ID: " + id + " không tồn tại.");
		}
		return employeeOptional;
	}

	@Override
	public ResponseEntity<Object> updateEmployeeById(@Valid Employee employee, Long id) {
	    ResponseEntity<Object> errorResponse = validateEmployee(employee);
	    if (errorResponse != null) {
	        return errorResponse;
	    }

	    // Find the existing employee
	    Optional<Employee> existingEmployee = eRepository.findById(id);
	    if (!existingEmployee.isPresent()) {
	        return mError.messageErrorValidate("ER005"); 
	    }

	    Employee updatedEmployee = existingEmployee.get();
	    updatedEmployee.setEmployeeLoginId(employee.getEmployeeLoginId());
	    updatedEmployee.setEmployeeLoginPassword(employee.getEmployeeLoginPassword());
	    updatedEmployee.setEmployeeName(employee.getEmployeeName());
	    updatedEmployee.setEmployeeNameKana(employee.getEmployeeNameKana());
	    updatedEmployee.setEmployeeEmail(employee.getEmployeeEmail());
	    updatedEmployee.setEmployeeTelephone(employee.getEmployeeTelephone());
	    updatedEmployee.setEmployeeBirthDate(employee.getEmployeeBirthDate());
	    if (employee.getDepartment() == null) {
	        updatedEmployee.setDepartment(employee.getDepartment());
	        return mError.messageErrorValidate("ER005"); 
	    }
	    eRepository.save(updatedEmployee);

	    Map<String, Object> map = new HashMap<>();
	    map.put("code", 200);
	    map.put("employees", updatedEmployee);
	    return new ResponseEntity<>(map, HttpStatus.OK);
	}

	@Override
	public Optional<Employee> findbyEmployeeLoginId(String employeeLoginId) {
		if (employeeLoginId == null) {
			throw new ResourceNotFoundException("Could not find user");
		}
		return eRepository.findByEmployeeLoginId(employeeLoginId);

	}

	@Override
	public List<Employee> getEmployees() {
		return eRepository.findAll();
	}

	@Override
	public ResponseEntity<Object> getListEmployee(String employeeName, BigDecimal departmentId, String ordEmployeeName,
			String ordCertificationName, String ordEndDate, int offset, int limit) {
		if (ordEmployeeName != null) {
			if (!(ordEmployeeName.equalsIgnoreCase("ASC") || ordEmployeeName.equalsIgnoreCase("DESC"))) {
				return mError.messageErrorValidate("ER021", "Lỗi");
			}
		}
		if (ordCertificationName != null) {
			if (!(ordCertificationName.equalsIgnoreCase("ASC") || ordCertificationName.equalsIgnoreCase("DESC"))) {
				return mError.messageErrorValidate("ER021", "Lỗi");
			}
		}
		if (ordEndDate != null) {
			if (!(ordEndDate.equalsIgnoreCase("ASC") || ordEndDate.equalsIgnoreCase("DESC"))) {
				return mError.messageErrorValidate("ER021", "Lỗi");
			}
		}
		if (offset < 0) {
			return mError.messageErrorValidate("ER018", "Lỗi");
		}
		if (limit < 0) {
			return mError.messageErrorValidate("ER018", "Lỗi");
		}

		List<EmployeesDTO> employee = eRepository.listAllEmployee(employeeName, departmentId, ordEmployeeName,
				ordCertificationName, ordEndDate, offset, limit);

		int count = employee.size();
		Map<String, Object> map = new HashMap<>();
		map.put("code", 200);
		map.put("employees", employee);
		map.put("total record", count);
		return new ResponseEntity<>(map, HttpStatus.OK);
	}

	@Override
	public EmployeesCertificationsDTO loadEmployeeById(Long employeeId) {
		Employee employee = eRepository.findByEmployeeId(employeeId);
		if (employee == null) {
			throw new ResourceNotFoundException("Không tìm thấy nhân viên với id = " + employeeId);
		}
		Set<Certification> certificationList = employee.getCertifications();
		Set<EmployeesCertifications> employeesCertifications = employee.getEmployeesCertifications();
		List<Object> array = new ArrayList<>();
		for (Certification certification : certificationList) {
			Map<String, Object> list = new HashMap<String, Object>();
			list.put("certificationName", certification.getCertificationName());
			for (EmployeesCertifications employeesCertification : employeesCertifications) {
				list.put("startDate", employeesCertification.getStartDate());
				list.put("endDate", employeesCertification.getEndDate());
				list.put("score", employeesCertification.getScore());
			}
			array.add(list);
		}
		EmployeesCertificationsDTO employeeListDto = new EmployeesCertificationsDTO();
		employeeListDto.setEmployeeId(employeeId);
		employeeListDto.setEmployeeName(employee.getEmployeeName());
		employeeListDto.setEmployeeBirthDate(employee.getEmployeeBirthDate());
		employeeListDto.setDepartmentId(employee.getDepartment().getDepartmentId());
		employeeListDto.setDepartmentName(employee.getDepartment().getDepartmentName());
		employeeListDto.setEmployeeEmail(employee.getEmployeeEmail());
		employeeListDto.setEmployeeTelephone(employee.getEmployeeTelephone());
		employeeListDto.setEmployeeBirthDate(employee.getEmployeeBirthDate());
		employeeListDto.setCertifications(array);

		return employeeListDto;
	}

	@Override
	public ResponseEntity<Object> deleteEmployeeById(Long id) {
		Employee employee = eRepository.findByEmployeeId(id);
		if (employee == null) {
			return mError.messageErrorValidate("ER014");
		}
		ecRepository.deleteByEmployeeId(id);
		eRepository.deleteById(id);
		Map<String, Object> map = new HashMap<>();
		map.put("code", 200);
		map.put("employeeId", id);
		return new ResponseEntity<>(map, HttpStatus.OK);
	}

}
