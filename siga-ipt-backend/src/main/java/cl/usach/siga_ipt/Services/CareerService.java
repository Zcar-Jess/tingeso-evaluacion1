package cl.usach.siga_ipt.Services;

import cl.usach.siga_ipt.Dtos.CareerRequest;
import cl.usach.siga_ipt.Dtos.CareerResponse;
import cl.usach.siga_ipt.Entities.Career;
import cl.usach.siga_ipt.Entities.CareerStatus;
import cl.usach.siga_ipt.Exceptions.ConflictException;
import cl.usach.siga_ipt.Exceptions.ResourceNotFoundException;
import cl.usach.siga_ipt.Repositories.CareerRepository;
import cl.usach.siga_ipt.Repositories.StudentRepository;
import cl.usach.siga_ipt.Repositories.StudyPlanRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class CareerService {

    private final CareerRepository careerRepository;
    private final StudyPlanRepository studyPlanRepository;
    private final StudentRepository studentRepository;

    @Transactional(readOnly = true)
    public List<CareerResponse> findAll() {
        return careerRepository.findAll().stream().map(CareerResponse::from).toList();
    }

    @Transactional(readOnly = true)
    public CareerResponse findById(Long id) {
        return CareerResponse.from(getCareer(id));
    }

    /** Rules: unique code; a new career starts as ACTIVE. */
    @Transactional
    public CareerResponse create(CareerRequest request) {
        String code = request.code().trim();
        if (careerRepository.existsByCode(code)) {
            throw new ConflictException("A career with code '" + code + "' already exists");
        }
        Career career = Career.builder()
                .code(code)
                .name(request.name().trim())
                .description(request.description().trim())
                .status(CareerStatus.ACTIVE)
                .build();
        return CareerResponse.from(careerRepository.save(career));
    }

    @Transactional
    public CareerResponse update(Long id, CareerRequest request) {
        Career career = getCareer(id);
        String code = request.code().trim();
        if (!career.getCode().equals(code) && careerRepository.existsByCode(code)) {
            throw new ConflictException("A career with code '" + code + "' already exists");
        }
        career.setCode(code);
        career.setName(request.name().trim());
        career.setDescription(request.description().trim());
        return CareerResponse.from(careerRepository.save(career));
    }

    @Transactional
    public CareerResponse changeStatus(Long id, CareerStatus status) {
        Career career = getCareer(id);
        career.setStatus(status);
        return CareerResponse.from(careerRepository.save(career));
    }

    /** A career with students or study plans cannot be deleted; only its status can change. */
    @Transactional
    public void delete(Long id) {
        Career career = getCareer(id);
        if (studentRepository.existsByCareerId(id) || studyPlanRepository.existsByCareerId(id)) {
            throw new ConflictException(
                    "A career with students or study plans cannot be deleted; change its status to INACTIVE instead");
        }
        careerRepository.delete(career);
    }

    private Career getCareer(Long id) {
        return careerRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Career " + id + " not found"));
    }
}
