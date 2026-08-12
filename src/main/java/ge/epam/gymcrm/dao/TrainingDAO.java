package ge.epam.gymcrm.dao;

import ge.epam.gymcrm.domain.Training;
import org.hibernate.SessionFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public class TrainingDAO {

    private static final Logger log = LoggerFactory.getLogger(TrainingDAO.class);

    private SessionFactory sessionFactory;

    @Autowired
    public void setSessionFactory(SessionFactory sessionFactory) {
        this.sessionFactory = sessionFactory;
    }

    public void save(Training training) {
        sessionFactory.getCurrentSession().persist(training);
        log.debug("Saved training: {}", training.getTrainingName());
    }

    public Optional<Training> findById(Long id) {
        return Optional.ofNullable(sessionFactory.getCurrentSession().get(Training.class, id));
    }

    public void delete(Training training) {
        sessionFactory.getCurrentSession().remove(training);
        log.debug("Deleted training: {}", training.getTrainingName());
    }

    public List<Training> findAll() {
        return sessionFactory.getCurrentSession()
                .createQuery("FROM Training", Training.class)
                .list();
    }
}
