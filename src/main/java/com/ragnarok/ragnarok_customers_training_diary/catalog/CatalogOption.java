package com.ragnarok.ragnarok_customers_training_diary.catalog;

/**
 * Lehké DTO katalogového cviku pro formuláře (select + inline JS dropdown).
 *
 * <p>Záměrně NEserializujeme celou {@link ExerciseCatalogItemEntity} do inline JS:
 * Jackson by narazil na Hibernate lazy proxy ({@code hibernateLazyInitializer}) a uťal
 * by odpověď uprostřed (rozbitá stránka), a navíc by mohl vytáhnout {@code createdBy}
 * (účet vč. hesla) do klientského JS. Tady posíláme jen to, co dropdown potřebuje.
 */
public record CatalogOption(Long id, String name, String equipment, String origin) {

    public static CatalogOption from(ExerciseCatalogItemEntity e) {
        return new CatalogOption(e.getId(), e.getName(), e.getEquipment(), CatalogLabels.origin(e));
    }

    public String selectLabel() {
        String suffix = equipment == null || equipment.isBlank()
                ? origin
                : origin + ", " + equipment;
        return name + " - " + suffix;
    }
}