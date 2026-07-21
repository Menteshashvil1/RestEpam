package ge.epam.gymcrm.service;

import ge.epam.gymcrm.dao.TrainingTypeDAO;
import ge.epam.gymcrm.domain.TrainingType;
import ge.epam.gymcrm.exception.NotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Read only access to the training types: the table holds a constant list of values
 * that cannot be updated from the application.
 */
@Service
@Transactional(readOnly = true)
public class TrainingTypeService {

    private TrainingTypeDAO trainingTypeDAO;

    @Autowired
    public void setTrainingTypeDAO(TrainingTypeDAO trainingTypeDAO) {
        this.trainingTypeDAO = trainingTypeDAO;
    }

    public List<TrainingType> findAll() {
        return trainingTypeDAO.findAll();
    }

    public TrainingType getById(Long id) {
        return trainingTypeDAO.findById(id)
                .orElseThrow(() -> new NotFoundException("Training type not found: " + id));
    }
}
