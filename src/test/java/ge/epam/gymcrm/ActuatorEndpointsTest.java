package ge.epam.gymcrm;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = "management.prometheus.metrics.export.enabled=true")
@AutoConfigureMockMvc
class ActuatorEndpointsTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void healthExposesCustomIndicators() throws Exception {
        mockMvc.perform(get("/actuator/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("UP"))
                .andExpect(jsonPath("$.components.database.status").value("UP"))
                .andExpect(jsonPath("$.components.trainingTypes.status").value("UP"))
                .andExpect(jsonPath("$.components.trainingTypes.details.count").isNumber());
    }

    @Test
    void prometheusEndpointExposesCustomMetrics() throws Exception {
        mockMvc.perform(get("/actuator/prometheus"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("gym_trainee_registrations_total")))
                .andExpect(content().string(containsString("gym_trainees_count")));
    }
}
