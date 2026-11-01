package cl.usach.siga_ipt.Dtos;

import cl.usach.siga_ipt.Entities.StudyPlan;

public record StudyPlanResponse(
        Long id,
        String identifier,
        boolean current,
        Long careerId,
        String careerCode) {

    public static StudyPlanResponse from(StudyPlan p) {
        return new StudyPlanResponse(p.getId(), p.getIdentifier(), p.isCurrent(),
                p.getCareer().getId(), p.getCareer().getCode());
    }
}
