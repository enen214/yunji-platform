-- Round-robin planning prevents paused/future/instant-waiting users from starving later users.
ALTER TABLE reminder_control ADD COLUMN plan_after_user BIGINT NOT NULL DEFAULT 0;
