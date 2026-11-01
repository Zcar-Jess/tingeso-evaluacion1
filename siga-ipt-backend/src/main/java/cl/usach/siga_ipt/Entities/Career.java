package cl.usach.siga_ipt.Entities;

import jakarta.persistence.*;
import lombok.*;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "careers")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Career {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String code;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false, length = 1000)
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private CareerStatus status;

    @OneToMany(mappedBy = "career")
    @Builder.Default
    private List<StudyPlan> studyPlans = new ArrayList<>();
}
