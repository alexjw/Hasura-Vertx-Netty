-- Drop existing tables to avoid conflicts
DROP TABLE IF EXISTS squadron_members;
DROP TABLE IF EXISTS squadrons;
DROP TABLE IF EXISTS battle_participants;
DROP TABLE IF EXISTS battles;
DROP TABLE IF EXISTS player_vehicles;
DROP TABLE IF EXISTS players;
DROP TABLE IF EXISTS vehicles;
DROP TABLE IF EXISTS vehicle_types;
DROP TABLE IF EXISTS nations;

-- Create tables
CREATE TABLE nations (
                         id SERIAL PRIMARY KEY,
                         name VARCHAR(50) UNIQUE NOT NULL
);

CREATE TABLE vehicle_types (
                               id SERIAL PRIMARY KEY,
                               type VARCHAR(20) UNIQUE NOT NULL
);

CREATE TABLE vehicles (
                          id SERIAL PRIMARY KEY,
                          name VARCHAR(100) NOT NULL,
                          nation_id INTEGER REFERENCES nations(id),
                          type_id INTEGER REFERENCES vehicle_types(id),
                          tier INTEGER NOT NULL,
                          UNIQUE (name, nation_id)
);

CREATE TABLE players (
                         id SERIAL PRIMARY KEY,
                         username VARCHAR(50) UNIQUE NOT NULL,
                         level INTEGER NOT NULL DEFAULT 1
);

CREATE TABLE player_vehicles (
                                 player_id INTEGER REFERENCES players(id),
                                 vehicle_id INTEGER REFERENCES vehicles(id),
                                 PRIMARY KEY (player_id, vehicle_id)
);

CREATE TABLE battles (
                         id SERIAL PRIMARY KEY,
                         mode VARCHAR(20) NOT NULL,
                         start_time TIMESTAMP NOT NULL,
                         duration INTERVAL
);

CREATE TABLE battle_participants (
                                     battle_id INTEGER REFERENCES battles(id),
                                     player_id INTEGER REFERENCES players(id),
                                     team VARCHAR(20) NOT NULL,
                                     score INTEGER NOT NULL DEFAULT 0,
                                     PRIMARY KEY (battle_id, player_id)
);

CREATE TABLE squadrons (
                           id SERIAL PRIMARY KEY,
                           name VARCHAR(50) UNIQUE NOT NULL,
                           leader_id INTEGER REFERENCES players(id)
);

CREATE TABLE squadron_members (
                                  squadron_id INTEGER REFERENCES squadrons(id),
                                  player_id INTEGER REFERENCES players(id),
                                  PRIMARY KEY (squadron_id, player_id)
);

-- Insert sample data
INSERT INTO nations (name) VALUES ('USA'), ('Germany'), ('USSR');

INSERT INTO vehicle_types (type) VALUES ('tank'), ('plane'), ('ship');

INSERT INTO vehicles (name, nation_id, type_id, tier) VALUES
                                                          ('M4 Sherman', 1, 1, 3),
                                                          ('P-51 Mustang', 1, 2, 4),
                                                          ('Fletcher-class', 1, 3, 5),
                                                          ('Tiger I', 2, 1, 5),
                                                          ('Bf 109', 2, 2, 4),
                                                          ('U-boat', 2, 3, 4),
                                                          ('T-34', 3, 1, 3),
                                                          ('Yak-3', 3, 2, 4),
                                                          ('Sovetsky Soyuz', 3, 3, 5);

INSERT INTO players (username, level) VALUES
                                          ('AcePilot', 10),
                                          ('TankCommander', 8),
                                          ('NavalHero', 7),
                                          ('Rookie', 1);

INSERT INTO player_vehicles (player_id, vehicle_id) VALUES
                                                        (1, 2), (1, 5),
                                                        (2, 1), (2, 4),
                                                        (3, 3), (3, 6),
                                                        (4, 7);

INSERT INTO battles (mode, start_time, duration) VALUES
                                                     ('arcade', '2025-06-10 14:00:00', '00:15:00'),
                                                     ('realistic', '2025-06-10 15:00:00', '00:20:00');

INSERT INTO battle_participants (battle_id, player_id, team, score) VALUES
                                                                        (1, 1, 'allies', 500),
                                                                        (1, 2, 'axis', 300),
                                                                        (1, 4, 'allies', 100),
                                                                        (2, 1, 'allies', 700),
                                                                        (2, 3, 'axis', 400);

INSERT INTO squadrons (name, leader_id) VALUES ('Flying Aces', 1);

INSERT INTO squadron_members (squadron_id, player_id) VALUES (1, 1), (1, 3);


CREATE VIEW player_battle_summary AS
SELECT
    p.id AS player_id,
    p.username,
    COUNT(bp.battle_id) AS total_battles,
    SUM(bp.score) AS total_score,
    ROUND(AVG(bp.score)::numeric, 2) AS average_score,
    MODE() WITHIN GROUP (ORDER BY bp.team) AS preferred_team,
    MAX(b.start_time) AS last_battle_time
FROM players p
         LEFT JOIN battle_participants bp ON p.id = bp.player_id
         LEFT JOIN battles b ON bp.battle_id = b.id
GROUP BY p.id, p.username;