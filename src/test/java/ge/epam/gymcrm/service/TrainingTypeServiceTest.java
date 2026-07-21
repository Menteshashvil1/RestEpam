package ge.epam.gymcrm.service;

import ge.epam.gymcrm.dao.TrainingTypeDAO;
import ge.epam.gymcrm.domain.TrainingType;
import ge.epam.gymcrm.exception.NotFoundException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TrainingTypeServiceTest {

    @Mock
    private TrainingTypeDAO trainingTypeDAO;

    @InjectMocks
    private TrainingTypeService trainingTypeService;

    @Test
    void findAllReturnsTheConstantList() {
        TrainingType cardio = new TrainingType();
        cardio.setId(1L);
        cardio.setTrainingTypeName("Cardio");
        when(trainingTypeDAO.findAll()).thenReturn(List.of(cardio));

        assertThat(trainingTypeService.findAll())
                .extracting(TrainingType::getTrainingTypeName)
                .containsExactly("Cardio");
    }

    @Test
    void getByIdThrowsForUnknownType() {
        when(trainingTypeDAO.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> trainingTypeService.getById(99L))
                .isInstanceOf(NotFoundException.class);
    }
}
