package cl.usach.siga_ipt.Dtos;

import cl.usach.siga_ipt.Entities.Career;
import cl.usach.siga_ipt.Entities.CareerStatus;

public record CareerResponse(
        Long id,
        String code,
        String name,
        String description,
        CareerStatus status) {

    public static CareerResponse from(Career c) {
        return new CareerResponse(c.getId(), c.getCode(), c.getName(), c.getDescription(), c.getStatus());
    }
}
