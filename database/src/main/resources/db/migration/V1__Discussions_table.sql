CREATE TABLE PUBLIC.team (
  team_id   NUMERIC PRIMARY KEY,
  name      VARCHAR NOT NULL
);

CREATE TABLE PUBLIC.discussion (
  team_id            NUMERIC REFERENCES team,
  discussion_id      NUMERIC NOT NULL,
  title              VARCHAR NOT NULL,
  author             VARCHAR NOT NULL,
  comments_count     NUMERIC NOT NULL,
  url                VARCHAR NOT NULL,
  created_at         TIMESTAMP WITH TIME ZONE NOT NULL,
  updated_at         TIMESTAMP WITH TIME ZONE NOT NULL,
  CONSTRAINT PK_DISCUSSION PRIMARY KEY (team_id, discussion_id)
);


CREATE TABLE PUBLIC.comment (
  team_id            NUMERIC NOT NULL,
  discussion_id      NUMERIC NOT NULL,
  comment_id         NUMERIC NOT NULL,
  author             VARCHAR NOT NULL,
  body               VARCHAR NOT NULL,
  url                VARCHAR NOT NULL,
  created_at         TIMESTAMP WITH TIME ZONE NOT NULL,
  FOREIGN KEY (team_id, discussion_id) REFERENCES discussion (team_id, discussion_id),
  CONSTRAINT PK_COMMENT PRIMARY KEY (team_id, discussion_id, comment_id)
);
