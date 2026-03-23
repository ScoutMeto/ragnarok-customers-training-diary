package com.ragnarok.ragnarok_customers_training_diary.service;

import com.ragnarok.ragnarok_customers_training_diary.dto.EmomDTO;
import java.util.List;

public interface EmomService {

    EmomDTO createEmom(EmomDTO emomDTO);

    EmomDTO getEmomById(Long emomId);

    List<EmomDTO> getEmomsByTrainingId(Long trainingId);

    EmomDTO updateEmom(Long emomId, EmomDTO emomDTO);

    void deleteEmom(Long emomId);
}