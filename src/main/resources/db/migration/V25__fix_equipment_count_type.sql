-- Fix: V21 vytvořila equipment_count jako SMALLINT, ale entity má Java int →
-- Hibernate ddl-auto=validate očekává INTEGER. Převedeme na INTEGER.
-- (V21 už proběhla, proto samostatná oprava místo úpravy V21.)

ALTER TABLE training_exercise
    ALTER COLUMN equipment_count TYPE INTEGER;
