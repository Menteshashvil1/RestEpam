package ge.epam.gymcrm.dao;

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
public class TrainerDAO {

    private static final Logger log = LoggerFactory.getLogger(TrainerDAO.class);

    private SessionFactory sessionFactory;

    @Autowired
    public void setSessionFactory(SessionFactory sessionFactory) {
        this.sessionFactory = sessionFactory;
    }

    public void save(Trainer trainer) {
        sessionFactory.getCurrentSession().persist(trainer);
        log.debug("Saved trainer: {}", trainer.getUser().getUsername());
    }

    public Optional<Trainer> findById(Long id) {
        return Optional.ofNullable(sessionFactory.getCurrentSession().get(Trainer.class, id));
    }

    public Optional<Trainer> findByUsername(String username) {
        return sessionFactory.getCurrentSession()
                .createQuery("""
                        FROM Trainer t
                        LEFT JOIN FETCH t.trainees
                        WHERE t.user.username = :username
                        """, Trainer.class)
                .setParameter("username", username)
                .uniqueResultOptional();
    }

    public boolean existsByName(String firstName, String lastName) {
        return sessionFactory.getCurrentSession()
                .createQuery("""
                        SELECT COUNT(t) FROM Trainer t
                        WHERE LOWER(t.user.firstName) = LOWER(:firstName)
                          AND LOWER(t.user.lastName) = LOWER(:lastName)
                        """, Long.class)
                .setParameter("firstName", firstName)
                .setParameter("lastName", lastName)
                .uniqueResult() > 0;
    }

    public List<Trainer> findAll() {
        return sessionFactory.getCurrentSession()
                .createQuery("FROM Trainer", Trainer.class)
                .list();
    }

    public long count() {
        return sessionFactory.getCurrentSession()
                .createQuery("SELECT COUNT(t) FROM Trainer t", Long.class)
                .uniqueResult();
    }

    public void update(Trainer trainer) {
        sessionFactory.getCurrentSession().merge(trainer);
        log.debug("Updated trainer: {}", trainer.getUser().getUsername());
    }

    public List<Training> findTrainings(Long trainerId, LocalDate fromDate, LocalDate toDate,
                                         String traineeName) {
        StringBuilder jpql = new StringBuilder(
                "FROM Training t WHERE t.trainer.id = :trainerId");
        if (fromDate != null)    jpql.append(" AND t.trainingDate >= :fromDate");
        if (toDate != null)      jpql.append(" AND t.trainingDate <= :toDate");
        if (traineeName != null) jpql.append(" AND (LOWER(t.trainee.user.username) LIKE :traineeName")
                                     .append(" OR LOWER(t.trainee.user.firstName) LIKE :traineeName")
                                     .append(" OR LOWER(t.trainee.user.lastName) LIKE :traineeName)");

        var query = sessionFactory.getCurrentSession()
                .createQuery(jpql.toString(), Training.class)
                .setParameter("trainerId", trainerId);
        if (fromDate != null)    query.setParameter("fromDate", fromDate);
        if (toDate != null)      query.setParameter("toDate", toDate);
        if (traineeName != null) query.setParameter("traineeName", "%" + traineeName.toLowerCase() + "%");

        return query.list();
    }
}
