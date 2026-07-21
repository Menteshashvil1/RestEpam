package ge.epam.gymcrm.domain;

import jakarta.persistence.*;

@Entity
@Table(name = "training_types")
public class TrainingType {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "training_type_name", nullable = false, unique = true)
    private String trainingTypeName;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getTrainingTypeName() { return trainingTypeName; }
    public void setTrainingTypeName(String trainingTypeName) { this.trainingTypeName = trainingTypeName; }
}
