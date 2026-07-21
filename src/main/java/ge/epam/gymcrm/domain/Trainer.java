package ge.epam.gymcrm.domain;

import jakarta.persistence.*;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "trainers")
public class Trainer {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(cascade = CascadeType.ALL)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne
    @JoinColumn(name = "training_type_id")
    private TrainingType specialization;

    @ManyToMany(mappedBy = "trainers")
    private List<Trainee> trainees = new ArrayList<>();

    @OneToMany(mappedBy = "trainer")
    private List<Training> trainings = new ArrayList<>();

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public User getUser() { return user; }
    public void setUser(User user) { this.user = user; }

    public TrainingType getSpecialization() { return specialization; }
    public void setSpecialization(TrainingType specialization) { this.specialization = specialization; }

    public List<Trainee> getTrainees() { return trainees; }
    public void setTrainees(List<Trainee> trainees) { this.trainees = trainees; }

    public List<Training> getTrainings() { return trainings; }
    public void setTrainings(List<Training> trainings) { this.trainings = trainings; }
}
