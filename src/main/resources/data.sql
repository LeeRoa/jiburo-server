-- BEGINNER_DETECTIVE (0점)
INSERT INTO badge_policy (badge_level, required_score, title_key, description_key)
SELECT 'BEGINNER_DETECTIVE', 0, 'badge.beginner_detective.title', 'badge.beginner_detective.description'
WHERE NOT EXISTS (SELECT 1 FROM badge_policy WHERE badge_level = 'BEGINNER_DETECTIVE');

-- JUNIOR_DETECTIVE (30점)
INSERT INTO badge_policy (badge_level, required_score, title_key, description_key)
SELECT 'JUNIOR_DETECTIVE', 30, 'badge.junior_detective.title', 'badge.junior_detective.description'
WHERE NOT EXISTS (SELECT 1 FROM badge_policy WHERE badge_level = 'JUNIOR_DETECTIVE');

-- SENIOR_DETECTIVE (100점)
INSERT INTO badge_policy (badge_level, required_score, title_key, description_key)
SELECT 'SENIOR_DETECTIVE', 100, 'badge.senior_detective.title', 'badge.senior_detective.description'
WHERE NOT EXISTS (SELECT 1 FROM badge_policy WHERE badge_level = 'SENIOR_DETECTIVE');

-- MASTER_SHERLOCK (300점)
INSERT INTO badge_policy (badge_level, required_score, title_key, description_key)
SELECT 'MASTER_SHERLOCK', 300, 'badge.master_sherlock.title', 'badge.master_sherlock.description'
WHERE NOT EXISTS (SELECT 1 FROM badge_policy WHERE badge_level = 'MASTER_SHERLOCK');