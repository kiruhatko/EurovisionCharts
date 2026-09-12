-- Additional countries beyond the initial 39, needed for chart-studio flag
-- matching (spec: any artist's country -> matching heart-flag asset). Kept
-- separate from V18 so a possible future Eurovision Asia/Americas roster
-- doesn't require touching the original seed.
INSERT INTO eurovision_countries (name, iso_code) VALUES
    ('Andorra', 'AD'), ('Bosnia and Herzegovina', 'BA'), ('Brazil', 'BR'),
    ('Bulgaria', 'BG'), ('Canada', 'CA'), ('Hungary', 'HU'), ('Indonesia', 'ID'),
    ('Kazakhstan', 'KZ'), ('Kosovo', 'XK'), ('Lebanon', 'LB'), ('Monaco', 'MC'),
    ('Morocco', 'MA'), ('Slovakia', 'SK'), ('Tunisia', 'TN'), ('Turkey', 'TR'),
    ('United States', 'US');
