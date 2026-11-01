package cl.usach.siga_ipt.Repositories;

import cl.usach.siga_ipt.Entities.Career;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CareerRepository extends JpaRepository<Career, Long> {
    boolean existsByCode(String code);
}
