-- ============================================================================
-- V3: Seed katalogu cviků
--
-- Manuálně sestavený výběr ~80 cviků relevantních pro KB/funkční gym (Ragnarok
-- profil). Pokrývá: KB lift base, klasická síla (barbell/dumbbell), bodyweight,
-- core/mobility/OS-Resets, cardio/conditioning. Pro každý cvik vyplněno
-- body_region, movement_pattern, primary_muscle, equipment.
--
-- Pro pozdější rozšíření může admin v UI přidat custom cviky (is_system=false).
-- ============================================================================

-- =============================================================================
-- KETTLEBELL
-- =============================================================================
INSERT INTO exercise_catalog_item (name, body_region, movement_pattern, primary_muscle, equipment, description, is_system) VALUES
('Kettlebell Swing',                  'FULL_BODY',  'HINGE',     'glutes',       'KETTLEBELL', 'Two-handed Russian swing.', TRUE),
('KB Swing American',                 'FULL_BODY',  'HINGE',     'glutes',       'KETTLEBELL', 'Overhead swing.', TRUE),
('KB Single-Hand Swing',              'FULL_BODY',  'HINGE',     'glutes',       'KETTLEBELL', NULL, TRUE),
('KB Clean',                          'FULL_BODY',  'PULL',      'glutes',       'KETTLEBELL', NULL, TRUE),
('KB Double Clean',                   'FULL_BODY',  'PULL',      'glutes',       'KETTLEBELL', NULL, TRUE),
('KB Snatch',                         'FULL_BODY',  'PULL',      'glutes',       'KETTLEBELL', NULL, TRUE),
('KB Press',                          'UPPER_BODY', 'PUSH',      'shoulders',    'KETTLEBELL', 'Strict press from rack.', TRUE),
('KB Double Press',                   'UPPER_BODY', 'PUSH',      'shoulders',    'KETTLEBELL', NULL, TRUE),
('KB Push Press',                     'UPPER_BODY', 'PUSH',      'shoulders',    'KETTLEBELL', NULL, TRUE),
('KB Jerk',                           'FULL_BODY',  'PUSH',      'shoulders',    'KETTLEBELL', NULL, TRUE),
('KB Long Cycle (Clean & Jerk)',      'FULL_BODY',  'PUSH',      'shoulders',    'KETTLEBELL', NULL, TRUE),
('KB Front Squat',                    'LOWER_BODY', 'SQUAT',     'quadriceps',   'KETTLEBELL', 'Goblet position or rack.', TRUE),
('KB Goblet Squat',                   'LOWER_BODY', 'SQUAT',     'quadriceps',   'KETTLEBELL', NULL, TRUE),
('KB Double Front Squat',             'LOWER_BODY', 'SQUAT',     'quadriceps',   'KETTLEBELL', NULL, TRUE),
('KB Turkish Get-Up',                 'FULL_BODY',  'OTHER',     'core',         'KETTLEBELL', 'Full TGU floor to standing.', TRUE),
('KB Half Get-Up',                    'FULL_BODY',  'OTHER',     'core',         'KETTLEBELL', NULL, TRUE),
('KB Windmill',                       'CORE',       'ROTATION',  'obliques',     'KETTLEBELL', NULL, TRUE),
('KB Halo',                           'UPPER_BODY', 'ROTATION',  'shoulders',    'KETTLEBELL', NULL, TRUE),
('KB Farmer Carry',                   'FULL_BODY',  'CARRY',     'forearms',     'KETTLEBELL', NULL, TRUE),
('KB Suitcase Carry',                 'CORE',       'CARRY',     'obliques',     'KETTLEBELL', NULL, TRUE),
('KB Rack Carry',                     'FULL_BODY',  'CARRY',     'core',         'KETTLEBELL', NULL, TRUE),
('KB Overhead Carry',                 'FULL_BODY',  'CARRY',     'shoulders',    'KETTLEBELL', NULL, TRUE),
('KB Deadlift',                       'LOWER_BODY', 'HINGE',     'glutes',       'KETTLEBELL', NULL, TRUE),
('KB Single-Leg Deadlift',            'LOWER_BODY', 'HINGE',     'glutes',       'KETTLEBELL', NULL, TRUE),
('KB Row',                            'UPPER_BODY', 'PULL',      'lats',         'KETTLEBELL', NULL, TRUE),
('KB Renegade Row',                   'FULL_BODY',  'PULL',      'lats',         'KETTLEBELL', NULL, TRUE);


-- =============================================================================
-- BARBELL (Powerlifting / Strength)
-- =============================================================================
INSERT INTO exercise_catalog_item (name, body_region, movement_pattern, primary_muscle, equipment, description, is_system) VALUES
('Back Squat',                        'LOWER_BODY', 'SQUAT',     'quadriceps',   'BARBELL',    'High-bar or low-bar back squat.', TRUE),
('Front Squat',                       'LOWER_BODY', 'SQUAT',     'quadriceps',   'BARBELL',    NULL, TRUE),
('Deadlift',                          'FULL_BODY',  'HINGE',     'glutes',       'BARBELL',    'Conventional deadlift.', TRUE),
('Sumo Deadlift',                     'FULL_BODY',  'HINGE',     'glutes',       'BARBELL',    NULL, TRUE),
('Romanian Deadlift',                 'LOWER_BODY', 'HINGE',     'hamstrings',   'BARBELL',    NULL, TRUE),
('Bench Press',                       'UPPER_BODY', 'PUSH',      'chest',        'BARBELL',    NULL, TRUE),
('Incline Bench Press',               'UPPER_BODY', 'PUSH',      'chest',        'BARBELL',    NULL, TRUE),
('Overhead Press',                    'UPPER_BODY', 'PUSH',      'shoulders',    'BARBELL',    'Strict OHP standing.', TRUE),
('Bent-Over Row',                     'UPPER_BODY', 'PULL',      'lats',         'BARBELL',    NULL, TRUE),
('Pendlay Row',                       'UPPER_BODY', 'PULL',      'lats',         'BARBELL',    NULL, TRUE),
('Hip Thrust',                        'LOWER_BODY', 'HINGE',     'glutes',       'BARBELL',    NULL, TRUE),
('Barbell Lunge',                     'LOWER_BODY', 'LUNGE',     'quadriceps',   'BARBELL',    NULL, TRUE),
('Power Clean',                       'FULL_BODY',  'PULL',      'glutes',       'BARBELL',    NULL, TRUE),
('Clean & Jerk',                      'FULL_BODY',  'PUSH',      'shoulders',    'BARBELL',    NULL, TRUE),
('Snatch',                            'FULL_BODY',  'PULL',      'glutes',       'BARBELL',    NULL, TRUE);


-- =============================================================================
-- DUMBBELL
-- =============================================================================
INSERT INTO exercise_catalog_item (name, body_region, movement_pattern, primary_muscle, equipment, description, is_system) VALUES
('DB Bench Press',                    'UPPER_BODY', 'PUSH',      'chest',        'DUMBBELL',   NULL, TRUE),
('DB Shoulder Press',                 'UPPER_BODY', 'PUSH',      'shoulders',    'DUMBBELL',   NULL, TRUE),
('DB Row',                            'UPPER_BODY', 'PULL',      'lats',         'DUMBBELL',   'Single arm row.', TRUE),
('DB Goblet Squat',                   'LOWER_BODY', 'SQUAT',     'quadriceps',   'DUMBBELL',   NULL, TRUE),
('DB Lunge',                          'LOWER_BODY', 'LUNGE',     'quadriceps',   'DUMBBELL',   NULL, TRUE),
('DB Bulgarian Split Squat',          'LOWER_BODY', 'LUNGE',     'quadriceps',   'DUMBBELL',   NULL, TRUE),
('DB Romanian Deadlift',              'LOWER_BODY', 'HINGE',     'hamstrings',   'DUMBBELL',   NULL, TRUE),
('DB Lateral Raise',                  'UPPER_BODY', 'PUSH',      'shoulders',    'DUMBBELL',   NULL, TRUE),
('DB Curl',                           'UPPER_BODY', 'PULL',      'biceps',       'DUMBBELL',   NULL, TRUE);


-- =============================================================================
-- BODYWEIGHT
-- =============================================================================
INSERT INTO exercise_catalog_item (name, body_region, movement_pattern, primary_muscle, equipment, description, is_system) VALUES
('Push-Up',                           'UPPER_BODY', 'PUSH',      'chest',        'BODYWEIGHT', NULL, TRUE),
('Diamond Push-Up',                   'UPPER_BODY', 'PUSH',      'triceps',      'BODYWEIGHT', NULL, TRUE),
('Pike Push-Up',                      'UPPER_BODY', 'PUSH',      'shoulders',    'BODYWEIGHT', NULL, TRUE),
('Pull-Up',                           'UPPER_BODY', 'PULL',      'lats',         'BODYWEIGHT', NULL, TRUE),
('Chin-Up',                           'UPPER_BODY', 'PULL',      'biceps',       'BODYWEIGHT', NULL, TRUE),
('Dip',                               'UPPER_BODY', 'PUSH',      'triceps',      'BODYWEIGHT', NULL, TRUE),
('Inverted Row',                      'UPPER_BODY', 'PULL',      'lats',         'BODYWEIGHT', NULL, TRUE),
('Air Squat',                         'LOWER_BODY', 'SQUAT',     'quadriceps',   'BODYWEIGHT', NULL, TRUE),
('Pistol Squat',                      'LOWER_BODY', 'SQUAT',     'quadriceps',   'BODYWEIGHT', NULL, TRUE),
('Bulgarian Split Squat (BW)',        'LOWER_BODY', 'LUNGE',     'quadriceps',   'BODYWEIGHT', NULL, TRUE),
('Reverse Lunge',                     'LOWER_BODY', 'LUNGE',     'quadriceps',   'BODYWEIGHT', NULL, TRUE),
('Forward Lunge',                     'LOWER_BODY', 'LUNGE',     'quadriceps',   'BODYWEIGHT', NULL, TRUE),
('Burpee',                            'FULL_BODY',  'OTHER',     'full body',    'BODYWEIGHT', NULL, TRUE),
('Mountain Climber',                  'FULL_BODY',  'OTHER',     'core',         'BODYWEIGHT', NULL, TRUE),
('Plank',                             'CORE',       'ISOMETRIC', 'core',         'BODYWEIGHT', 'Hold position.', TRUE),
('Side Plank',                        'CORE',       'ISOMETRIC', 'obliques',     'BODYWEIGHT', NULL, TRUE),
('Hollow Hold',                       'CORE',       'ISOMETRIC', 'core',         'BODYWEIGHT', NULL, TRUE),
('Hanging Leg Raise',                 'CORE',       'PULL',      'core',         'BODYWEIGHT', NULL, TRUE),
('V-Up',                              'CORE',       'OTHER',     'core',         'BODYWEIGHT', NULL, TRUE),
('Russian Twist',                     'CORE',       'ROTATION',  'obliques',     'BODYWEIGHT', NULL, TRUE),
('Glute Bridge',                      'LOWER_BODY', 'HINGE',     'glutes',       'BODYWEIGHT', NULL, TRUE);


-- =============================================================================
-- PLYO / CONDITIONING
-- =============================================================================
INSERT INTO exercise_catalog_item (name, body_region, movement_pattern, primary_muscle, equipment, description, is_system) VALUES
('Box Jump',                          'LOWER_BODY', 'PLYO',      'quadriceps',   'NONE',       NULL, TRUE),
('Broad Jump',                        'LOWER_BODY', 'PLYO',      'quadriceps',   'BODYWEIGHT', NULL, TRUE),
('Jump Squat',                        'LOWER_BODY', 'PLYO',      'quadriceps',   'BODYWEIGHT', NULL, TRUE),
('Tuck Jump',                         'LOWER_BODY', 'PLYO',      'quadriceps',   'BODYWEIGHT', NULL, TRUE),
('Clap Push-Up',                      'UPPER_BODY', 'PLYO',      'chest',        'BODYWEIGHT', NULL, TRUE),
('Jumping Rope',                      'FULL_BODY',  'GAIT',      'calves',       'OTHER',      NULL, TRUE);


-- =============================================================================
-- CARDIO
-- =============================================================================
INSERT INTO exercise_catalog_item (name, body_region, movement_pattern, primary_muscle, equipment, description, is_system) VALUES
('Running',                           'FULL_BODY',  'GAIT',      'cardiovascular', 'NONE',     NULL, TRUE),
('Sprint',                            'FULL_BODY',  'GAIT',      'cardiovascular', 'NONE',     NULL, TRUE),
('Rowing (Ergometr)',                 'FULL_BODY',  'PULL',      'cardiovascular', 'MACHINE',  NULL, TRUE),
('Cycling',                           'LOWER_BODY', 'GAIT',      'cardiovascular', 'MACHINE',  NULL, TRUE),
('Assault Bike',                      'FULL_BODY',  'GAIT',      'cardiovascular', 'MACHINE',  NULL, TRUE),
('Ski Erg',                           'FULL_BODY',  'PULL',      'cardiovascular', 'MACHINE',  NULL, TRUE);


-- =============================================================================
-- MOBILITY / OS-RESETS
-- =============================================================================
INSERT INTO exercise_catalog_item (name, body_region, movement_pattern, primary_muscle, equipment, description, is_system) VALUES
('Crawling (Bear)',                   'FULL_BODY',  'OTHER',     'core',         'BODYWEIGHT', 'Original Strength Bear crawl.', TRUE),
('Crawling (Leopard)',                'FULL_BODY',  'OTHER',     'core',         'BODYWEIGHT', NULL, TRUE),
('Rolling',                           'CORE',       'ROTATION',  'core',         'BODYWEIGHT', 'Segmental rolls (OS Reset).', TRUE),
('Rocking',                           'FULL_BODY',  'OTHER',     'core',         'BODYWEIGHT', 'Quadruped rocking (OS Reset).', TRUE),
('Head Nods',                         'CORE',       'ISOMETRIC', 'neck',         'BODYWEIGHT', 'OS Reset.', TRUE),
('Cross Crawl',                       'FULL_BODY',  'GAIT',      'core',         'BODYWEIGHT', 'OS Reset standing.', TRUE),
('Cat-Cow',                           'CORE',       'OTHER',     'spine',        'BODYWEIGHT', 'Mobility.', TRUE),
('World''s Greatest Stretch',         'FULL_BODY',  'OTHER',     'full body',    'BODYWEIGHT', NULL, TRUE),
('90/90 Hip Stretch',                 'LOWER_BODY', 'ROTATION',  'hips',         'BODYWEIGHT', NULL, TRUE);
