package nextcore.employees_manager.respository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import nextcore.employees_manager.DTO.EmployeesDTO;
import nextcore.employees_manager.entity.Department;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.List;

@Repository
public interface DepartmentRepository extends JpaRepository<Department, Long> {
//	@Query("SELECT COUNT(d) FROM Department d WHERE d.departmentId = :departmentId")
//	Optional<Department> getEmployeeBId(@Param("id") Long id);
	Department findByDepartmentId(BigDecimal departmentId);
}