# --- !Ups

alter table "CLIENTS" add COLUMN "scope" VARCHAR;

# --- !Downs

alter table "CLIENTS" drop COLUMN "scope";