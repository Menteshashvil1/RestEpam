package ge.epam.gymcrm.dao;

import ge.epam.gymcrm.domain.Trainee;
import ge.epam.gymcrm.domain.Trainer;
import ge.epam.gymcrm.domain.Training;
import org.hibernate.SessionFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public class TraineeDAO {

    private static final Logger log = LoggerFactory.getLogger(TraineeDAO.class);

    private SessionFactory sessionFactory;

    @Autowired
    public void setSessionFactory(SessionFactory sessionFactory) {
        this.sessionFactory = sessionFactory;
    }

    public void save(Trainee trainee) {
        sessionFactory.getCurrentSession().persist(trainee);
        log.debug("Saved trainee: {}", trainee.getUser().getUsername());
    }

    public Optional<Trainee> findById(Long id) {
        return Optional.ofNullable(sessionFactory.getCurrentSession().get(Trainee.class, id));
    }

    /** The trainer list is fetched eagerly: the REST layer maps it outside the transaction. */
    public Optional<Trainee> findByUsername(String username) {
        return sessionFactory.getCurrentSession()
                .createQuery("""
                        FROM Trainee t
                        LEFT JOIN FETCH t.trainers
                        WHERE t.user.username = :username
                        """, Trainee.class)
                .setParameter("username", username)
                .uniqueResultOptional();
    }

    public boolean existsByName(String firstName, String lastName) {
        return sessionFactory.getCurrentSession()
                .createQuery("""
                        SELECT COUNT(t) FROM Trainee t
                        WHERE LOWER(t.user.firstName) = LOWER(:firstName)
                          AND LOWER(t.user.lastName) = LOWER(:lastName)
                        """, Long.class)
                .setParameter("firstName", firstName)
                .setParameter("lastName", lastName)
                .uniqueResult() > 0;
    }

    public List<Trainee> findAll() {
        return sessionFactory.getCurrentSession()
                .createQuery("FROM Trainee", Trainee.class)
                .list();
    }

    public void update(Trainee trainee) {
        sessionFactory.getCurrentSession().merge(trainee);
        log.debug("Updated trainee: {}", trainee.getUser().getUsername());
    }

    public void delete(Trainee trainee) {
        sessionFactory.getCurrentSession().remove(trainee);
        log.debug("Deleted trainee: {}", trainee.getUser().getUsername());
    }

    public List<Trainer> findNotAssignedActiveTrainers(Long traineeId) {
        return sessionFactory.getCurrentSession()
                .createQuery("""
                        FROM Trainer tr WHERE tr.user.isActive = true
                        AND tr.id NOT IN (
                            SELECT t.id FROM Trainee tn JOIN tn.trainers t WHERE tn.id = :traineeId
                        )
                        """, Trainer.class)
                .setParameter("traineeId", traineeId)
                .list();
    }

    public List<Training> findTrainings(Long traineeId, LocalDate fromDate, LocalDate toDate,
                                         String trainerName, String trainingType) {
        StringBuilder jpql = new StringBuilder(
                "FROM Training t WHERE t.trainee.id = :traineeId");
        if (fromDate != null)     jpql.append(" AND t.trainingDate >= :fromDate");
        if (toDate != null)       jpql.append(" AND t.trainingDate <= :toDate");
        if (trainerName != null)  jpql.append(" AND (LOWER(t.trainer.user.username) LIKE :trainerName")
                                      .append(" OR LOWER(t.trainer.user.firstName) LIKE :trainerName")
                                      .append(" OR LOWER(t.trainer.user.lastName) LIKE :trainerName)");
        if (trainingType != null) jpql.append(" AND LOWER(t.trainingType.trainingTypeName) = LOWER(:trainingType)");

        var query = sessionFactory.getCurrentSession()
                .createQuery(jpql.toString(), Training.class)
                .setParameter("traineeId", traineeId);
        if (fromDate != null)     query.setParameter("fromDate", fromDate);
        if (toDate != null)       query.setParameter("toDate", toDate);
        if (trainerName != null)  query.setParameter("trainerName", "%" + trainerName.toLowerCase() + "%");
        if (trainingType != null) query.setParameter("trainingType", trainingType);

        return query.list();
    }
}
