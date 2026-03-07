CREATE TABLE `users`
(
    `id`            INT          NOT NULL AUTO_INCREMENT,
    `name`          varchar(250) NOT NULL,
    `phone_no`      varchar(15)  NOT NULL UNIQUE,
    `email`         varchar(250) NOT NULL UNIQUE,
    `profile_pic`   varchar(512) NOT NULL,
    `firebase_uuid` varchar(256) NOT NULL UNIQUE,
    PRIMARY KEY (`id`)
);

CREATE TABLE `accounts`
(
    `id`      INT          NOT NULL AUTO_INCREMENT,
    `name`    varchar(250) NOT NULL,
    `user_id` INT          NOT NULL,
    PRIMARY KEY (`id`)
);

CREATE TABLE `categories`
(
    `id`      INT          NOT NULL AUTO_INCREMENT,
    `name`    varchar(250) NOT NULL,
    `user_id` INT          NOT NULL,
    PRIMARY KEY (`id`)
);

CREATE TABLE `transactions`
(
    `id`          INT      NOT NULL AUTO_INCREMENT,
    `amount`      DECIMAL  NOT NULL,
    `date`        DATETIME NOT NULL,
    `account_id`  INT      NOT NULL,
    `category_id` INT      NOT NULL,
    PRIMARY KEY (`id`)
);

CREATE TABLE `splits`
(
    `id`             INT     NOT NULL AUTO_INCREMENT,
    `amount`         DECIMAL NOT NULL,
    `transaction_id` INT     NOT NULL,
    `friend_user_id` INT     NOT NULL,
    PRIMARY KEY (`id`)
);

CREATE TABLE `groups`
(
    `id`   INT          NOT NULL AUTO_INCREMENT,
    `name` varchar(250) NOT NULL,
    PRIMARY KEY (`id`)
);

CREATE TABLE `chat_messages`
(
    `to_group_id`  INT(250) NOT NULL,
    `from_user_id` INT          NOT NULL,
    `message`      varchar(512) NOT NULL,
    `sent_on`      TIMESTAMP    NOT NULL,
    `read`         INT          NOT NULL
);

CREATE TABLE `user_groups`
(
    `user_id`  INT NOT NULL,
    `group_id` INT NOT NULL
);

ALTER TABLE `accounts`
    ADD CONSTRAINT `accounts_fk0` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`);

ALTER TABLE `categories`
    ADD CONSTRAINT `categories_fk0` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`);

ALTER TABLE `transactions`
    ADD CONSTRAINT `transactions_fk0` FOREIGN KEY (`account_id`) REFERENCES `accounts` (`id`);

ALTER TABLE `transactions`
    ADD CONSTRAINT `transactions_fk1` FOREIGN KEY (`category_id`) REFERENCES `categories` (`id`);

ALTER TABLE `splits`
    ADD CONSTRAINT `splits_fk0` FOREIGN KEY (`transaction_id`) REFERENCES `transactions` (`id`);

ALTER TABLE `splits`
    ADD CONSTRAINT `splits_fk1` FOREIGN KEY (`friend_user_id`) REFERENCES `users` (`id`);

ALTER TABLE `chat_messages`
    ADD CONSTRAINT `chat_messages_fk0` FOREIGN KEY (`to_group_id`) REFERENCES `groups` (`id`);

ALTER TABLE `chat_messages`
    ADD CONSTRAINT `chat_messages_fk1` FOREIGN KEY (`from_user_id`) REFERENCES `users` (`id`);

ALTER TABLE `user_groups`
    ADD CONSTRAINT `user_groups_fk0` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`);

ALTER TABLE `user_groups`
    ADD CONSTRAINT `user_groups_fk1` FOREIGN KEY (`group_id`) REFERENCES `groups` (`id`);

