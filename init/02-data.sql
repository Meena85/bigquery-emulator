INSERT INTO sample.people (id, name, born, score, active, created)
VALUES (1, 'Ada Lovelace', DATE '1815-12-10', 12.50, TRUE, TIMESTAMP '2020-01-01 00:00:00+00'),
       (2, 'Alan Turing', DATE '1912-06-23', 7, FALSE, NULL);

INSERT INTO sample.events (id, payload) VALUES (1, 'first event');
