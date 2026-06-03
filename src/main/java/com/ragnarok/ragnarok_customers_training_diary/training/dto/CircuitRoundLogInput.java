package com.ragnarok.ragnarok_customers_training_diary.training.dto;

import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Phase 12 / A3: form-backing pro skutečný záznam circuitu po kolech.
 * Plochý seznam buněk (kolo × krok) — formulář na detailu tréninku.
 */
@Getter
@Setter
@NoArgsConstructor
public class CircuitRoundLogInput {

    private List<EntryInput> entries = new ArrayList<>();

    @Getter
    @Setter
    @NoArgsConstructor
    public static class EntryInput {
        private Integer roundIndex;
        private Integer stepOrder;
        private boolean skipped;
        @Size(max = 128)
        private String substituteName;
        private Integer actualReps;
        private BigDecimal actualWeightKg;
        @Size(max = 255)
        private String note;

        /** Buňka má smysl uložit, jen když nese nějakou informaci. */
        public boolean hasData() {
            return skipped
                    || (substituteName != null && !substituteName.isBlank())
                    || actualReps != null
                    || actualWeightKg != null
                    || (note != null && !note.isBlank());
        }
    }
}
