CREATE TABLE reviewer_scope (
 user_id BIGINT NOT NULL,organization_id BIGINT NOT NULL,PRIMARY KEY(user_id,organization_id),
 FOREIGN KEY(user_id) REFERENCES sys_user(user_id),FOREIGN KEY(organization_id) REFERENCES organization(id)
);
