package cl.usach.siga_ipt.Services;

import cl.usach.siga_ipt.Dtos.StudyPlanRequest;
import cl.usach.siga_ipt.Dtos.StudyPlanResponse;
import cl.usach.siga_ipt.Entities.Career;
import cl.usach.siga_ipt.Entities.StudyPlan;
import cl.usach.siga_ipt.Exceptions.ConflictException;
import cl.usach.siga_ipt.Exceptions.ResourceNotFoundException;
import cl.usach.siga_ipt.Repositories.CareerRepository;
import cl.usach.siga_ipt.Repositories.CourseRepository;
import cl.usach.siga_ipt.Repositories.StudentRepository;
import cl.usach.siga_ipt.Repositories.StudyPlanRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class StudyPlanService {

    private final StudyPlanRepository studyPlanRepository;
    private final CareerRepository careerRepository;
    private final StudentRepository studentRepository;
    private final CourseRepository courseRepository;

    @Transactional(readOnly = true)
    public List<StudyPlanResponse> findByCareer(Long careerId) {
        if (!careerRepository.existsById(careerId)) {
            throw new ResourceNotFoundException("Career " + careerId + " not found");
        }
        return studyPlanRepository.findByCareerId(careerId).stream()
                .map(StudyPlanResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public StudyPlanResponse findById(Long id) {
        return StudyPlanResponse.from(getPlan(id));
    }

    /**
     * Rules: the plan must belong to an existing career and have an identifier that is
     * unique within that career. If created as current, the previous current plan of the
     * same career becomes not current. Students of older plans are not touched.
     */
    @Transactional
    public StudyPlanResponse create(Long careerId, StudyPlanRequest request) {
        Career career = careerRepository.findById(careerId)
                .orElseThrow(() -> new ResourceNotFoundException("Career " + careerId + " not found"));
        String identifier = request.identifier().trim();
        if (studyPlanRepository.existsByCareerIdAndIdentifier(careerId, identifier)) {
            throw new ConflictException("Career already has a study plan '" + identifier + "'");
        }
        StudyPlan plan = studyPlanRepository.save(StudyPlan.builder()
                .identifier(identifier)
                .current(false)
                .career(career)
                .build());
        if (Boolean.TRUE.equals(request.current())) {
            setAsCurrent(plan);
        }
        return StudyPlanResponse.from(plan);
    }

    @Transactional
    public StudyPlanResponse makeCurrent(Long id) {
        StudyPlan plan = getPlan(id);
        setAsCurrent(plan);
        return StudyPlanResponse.from(plan);
    }

    /** A plan with students (or courses) cannot be deleted. */
    @Transactional
    public void delete(Long id) {
        StudyPlan plan = getPlan(id);
        if (studentRepository.existsByStudyPlanId(id)) {
            throw new ConflictException("A study plan with students cannot be deleted");
        }
        if (!courseRepository.findByStudyPlanId(id).isEmpty()) {
            throw new ConflictException("Delete the courses of the study plan before deleting it");
        }
        studyPlanRepository.delete(plan);
    }

    private void setAsCurrent(StudyPlan plan) {
        studyPlanRepository.findByCareerIdAndCurrentTrue(plan.getCareer().getId())
                .filter(previous -> !previous.getId().equals(plan.getId()))
                .ifPresent(previous -> {
                    previous.setCurrent(false);
                    studyPlanRepository.saveAndFlush(previous);
                });
        plan.setCurrent(true);
        studyPlanRepository.save(plan);
    }

    private StudyPlan getPlan(Long id) {
        return studyPlanRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Study plan " + id + " not found"));
    }
}
