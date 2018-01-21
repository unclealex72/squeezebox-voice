# --- !Ups

create table "USERS" (
    "USER_ID" SERIAL NOT NULL PRIMARY KEY,
    "USERNAME" VARCHAR NOT NULL UNIQUE,
    "HASHED_PASSWORD" VARCHAR NOT NULL,
    "CREATED_AT" TIMESTAMP NOT NULL);
create table "CLIENTS" (
    "CLIENT_ID" SERIAL NOT NULL PRIMARY KEY,
    "OWNER_ID" INTEGER NOT NULL,
    "CLIENT_PUBLIC_ID" VARCHAR NOT NULL UNIQUE,
    "CLIENT_SECRET" VARCHAR NOT NULL,
    "REDIRECT_URI" VARCHAR NOT NULL,
    "CREATED_AT" TIMESTAMP NOT NULL);
alter table "CLIENTS" add constraint "CLIENT_OWNER_FK" foreign key("OWNER_ID") references "USERS"("USER_ID") on update NO ACTION on delete NO ACTION;
create table "ACCESS_TOKENS" (
    "ACCESS_TOKEN_ID" SERIAL NOT NULL PRIMARY KEY,
    "USER_ID" INTEGER NOT NULL,
    "CLIENT_ID" INTEGER NOT NULL,
    "ACCESS_TOKEN" VARCHAR NOT NULL,
    "REFRESH_TOKEN" VARCHAR NOT NULL,
    "CREATED_AT" TIMESTAMP NOT NULL);
alter table "ACCESS_TOKENS" add constraint "ACCESS_TOKEN_CLIENT_FK" foreign key("CLIENT_ID") references "CLIENTS"("CLIENT_ID") on update NO ACTION on delete NO ACTION;
alter table "ACCESS_TOKENS" add constraint "ACCESS_TOKEN_USER_FK" foreign key("USER_ID") references "USERS"("USER_ID") on update NO ACTION on delete NO ACTION;
create table "CODES" (
    "AUTHORIZATION_ID" SERIAL NOT NULL PRIMARY KEY,
    "USER_ID" INTEGER NOT NULL,
    "CLIENT_ID" INTEGER NOT NULL,
    "CODE" VARCHAR NOT NULL,
    "CREATED_AT" TIMESTAMP NOT NULL);
alter table "CODES" add constraint "CODE_CLIENT_FK" foreign key("CLIENT_ID") references "CLIENTS"("CLIENT_ID") on update NO ACTION on delete NO ACTION;
alter table "CODES" add constraint "CODE_USER_FK" foreign key("USER_ID") references "USERS"("USER_ID") on update NO ACTION on delete NO ACTION;

# --- !Downs

alter table "CODES" drop constraint "CODE_CLIENT_FK";
alter table "CODES" drop constraint "CODE_USER_FK";
drop table "CODES";
alter table "ACCESS_TOKENS" drop constraint "ACCESS_TOKEN_CLIENT_FK";
alter table "ACCESS_TOKENS" drop constraint "ACCESS_TOKEN_USER_FK";
drop table "ACCESS_TOKENS";
alter table "CLIENTS" drop constraint "CLIENT_OWNER_FK";
drop table "CLIENTS";
drop table "USERS";
