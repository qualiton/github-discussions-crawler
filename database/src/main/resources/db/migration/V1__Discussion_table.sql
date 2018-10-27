CREATE TABLE PUBLIC.discussion (
  team_id            NUMERIC,
  team_name          VARCHAR NOT NULL,
  discussion_id      NUMERIC NOT NULL,
  title              VARCHAR NOT NULL,
  author             VARCHAR NOT NULL,
  avatar_url         VARCHAR NOT NULL,
  body               VARCHAR NOT NULL,
  body_version       VARCHAR NOT NULL,
  discussion_url     VARCHAR NOT NULL,
  comments           JSONB DEFAULT '{}' :: json,
  created_at         TIMESTAMP WITH TIME ZONE NOT NULL,
  updated_at         TIMESTAMP WITH TIME ZONE NOT NULL,
  refreshed_at       TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now(),
  CONSTRAINT PK_DISCUSSION PRIMARY KEY (team_id, discussion_id)
);
