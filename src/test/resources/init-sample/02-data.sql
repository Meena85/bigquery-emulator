INSERT INTO app.people (id, name, born, score, active, created)
VALUES (1, 'Ada; Lovelace', DATE '1815-12-10', 12.50, TRUE, TIMESTAMP '2020-01-01 00:00:00+00'),
       (2, 'Alan # Turing', DATE '1912-06-23', 7, FALSE, NULL);  # hash comment

INSERT app.events VALUES (1, 'it\'s -- not a comment');
UPDATE app.events SET payload = 'updated' WHERE id = 1;
INSERT INTO app.events SELECT id + 1, payload FROM app.events;
