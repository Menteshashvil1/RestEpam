package ge.epam.gymcrm.dao;

import ge.epam.gymcrm.domain.TrainingType;
import org.hibernate.SessionFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public class TrainingTypeDAO {

    private static final Logger log = LoggerFactory.getLogger(TrainingTypeDAO.class);

    private SessionFactory sessionFactory;

    @Autowired
    public void setSessionFactory(SessionFactory sessionFactory) {
        this.sessionFactory = sessionFactory;
    }

    public List<TrainingType> findAll() {
        return sessionFactory.getCurrentSession()
                .createQuery("FROM TrainingType", TrainingType.class)
                .list();
    }

    public Optional<TrainingType> findById(Long id) {
        return Optional.ofNullable(sessionFactory.getCurrentSession().get(TrainingType.class, id));
    }

    public Optional<TrainingType> findByName(String name) {
        return sessionFactory.getCurrentSession()
                .createQuery("FROM TrainingType t WHERE t.trainingTypeName = :name", TrainingType.class)
                .setParameter("name", name)
                .uniqueResultOptional();
    }
}
