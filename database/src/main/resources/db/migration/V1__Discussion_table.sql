CREATE TABLE PUBLIC.team (
  id                 NUMERIC,
  name               VARCHAR NOT NULL,
  description        VARCHAR,
  created_at         TIMESTAMP WITH TIME ZONE NOT NULL,
  updated_at         TIMESTAMP WITH TIME ZONE NOT NULL,
  refreshed_at       TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now(),
  CONSTRAINT PK_TEAM PRIMARY KEY (id)
);

CREATE TABLE PUBLIC.discussion (
  team_id            NUMERIC,
  discussion_id      NUMERIC,
  title              VARCHAR NOT NULL,
  created_at         TIMESTAMP WITH TIME ZONE NOT NULL,
  updated_at         TIMESTAMP WITH TIME ZONE NOT NULL,
  refreshed_at       TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now(),
  CONSTRAINT PK_DISCUSSION UNIQUE (team_id, discussion_id),
  CONSTRAINT FK_TEAM_ID FOREIGN KEY (team_id) REFERENCES team(id) ON DELETE CASCADE
);

CREATE TABLE PUBLIC.author (
  id                 NUMERIC,
  name               VARCHAR NOT NULL,
  avatar_url         VARCHAR NOT NULL,
  refreshed_at       TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now(),
  CONSTRAINT PK_AUTHOR PRIMARY KEY (id)
);

CREATE TABLE PUBLIC.comment (
  team_id            NUMERIC,
  discussion_id      NUMERIC,
  comment_id         NUMERIC,
  author_id          NUMERIC,
  url                VARCHAR NOT NULL,
  body               VARCHAR NOT NULL,
  body_version       VARCHAR NOT NULL,
  created_at         TIMESTAMP WITH TIME ZONE NOT NULL,
  updated_at         TIMESTAMP WITH TIME ZONE NOT NULL,
  refreshed_at       TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now(),
  CONSTRAINT PK_COMMENT PRIMARY KEY (team_id, discussion_id, comment_id),
  CONSTRAINT FK_DISCUSSION FOREIGN KEY (team_id, discussion_id) REFERENCES discussion(team_id, discussion_id) ON DELETE CASCADE,
  CONSTRAINT FK_AUTHOR FOREIGN KEY (author_id) REFERENCES author(id)
);

CREATE VIEW discussions_flat_view as
  SELECT t.id as team_id, t.name as team_name, t.description as team_description, t.created_at as team_created_at, t.updated_at as team_updated_at,
         d.discussion_id, d.title as discussion_title, d.created_at as discussion_created_at, d.updated_at as discussion_updated_at,
         c.comment_id, a.id as author_id, a.name as author_name, a.avatar_url as author_avatar_url, c.url as comment_url, c.body, c.body_version, c.created_at as comment_created_at, c.updated_at as comment_updated_at
  FROM team t
      LEFT OUTER JOIN discussion d ON t.id = d.team_id
      LEFT OUTER JOIN comment c ON c.team_id = d.team_id AND c.discussion_id = d.discussion_id
      LEFT OUTER JOIN author a ON c.author_id = a.id;

