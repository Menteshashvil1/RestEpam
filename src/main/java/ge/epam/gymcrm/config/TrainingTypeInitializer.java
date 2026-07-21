package ge.epam.gymcrm.config;

import ge.epam.gymcrm.dao.TrainingTypeDAO;
import ge.epam.gymcrm.domain.TrainingType;
import org.hibernate.SessionFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Component
public class TrainingTypeInitializer implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(TrainingTypeInitializer.class);
    private static final List<String> TRAINING_TYPES =
            List.of("Cardio", "Yoga", "Strength", "Pilates", "Boxing");

    private final SessionFactory sessionFactory;
    private final TrainingTypeDAO trainingTypeDAO;

    public TrainingTypeInitializer(SessionFactory sessionFactory, TrainingTypeDAO trainingTypeDAO) {
        this.sessionFactory = sessionFactory;
        this.trainingTypeDAO = trainingTypeDAO;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        for (String name : TRAINING_TYPES) {
            if (trainingTypeDAO.findByName(name).isEmpty()) {
                TrainingType type = new TrainingType();
                type.setTrainingTypeName(name);
                sessionFactory.getCurrentSession().persist(type);
                log.info("Seeded training type: {}", name);
            }
        }
    }
}
