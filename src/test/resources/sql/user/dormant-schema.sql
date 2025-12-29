/*
 * +++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++
 * + Copyright 2025. NHN Academy Corp. All rights reserved.
 * + * While every precaution has been taken in the preparation of this resource,  assumes no
 * + responsibility for errors or omissions, or for damages resulting from the use of the information
 * + contained herein
 * + No part of this resource may be reproduced, stored in a retrieval system, or transmitted, in any
 * + form or by any means, electronic, mechanical, photocopying, recording, or otherwise, without the
 * + prior written permission.
 * +++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++
 */

SET REFERENTIAL_INTEGRITY FALSE;

DROP TABLE IF EXISTS AccountStatusHistories CASCADE;
DROP TABLE IF EXISTS Accounts CASCADE;
DROP TABLE IF EXISTS Users CASCADE;
DROP TABLE IF EXISTS Statuses CASCADE;

CREATE TABLE Statuses (
    status_id   BIGINT AUTO_INCREMENT PRIMARY KEY,
    status_name VARCHAR(10) NOT NULL
);

CREATE TABLE Users (
    user_created_id BIGINT AUTO_INCREMENT PRIMARY KEY
);

CREATE TABLE Accounts (
    login_id           VARCHAR(50) PRIMARY KEY,
    user_created_id    BIGINT NOT NULL,
    last_login_at      TIMESTAMP NULL,
    current_status_id  BIGINT NOT NULL DEFAULT 1,
    FOREIGN KEY (user_created_id) REFERENCES Users(user_created_id),
    FOREIGN KEY (current_status_id) REFERENCES Statuses(status_id)
);

CREATE TABLE AccountStatusHistories (
    account_status_history_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    login_id                  VARCHAR(50) NOT NULL,
    status_id                 BIGINT NOT NULL,
    changed_at                TIMESTAMP NOT NULL,
    FOREIGN KEY (login_id) REFERENCES Accounts(login_id),
    FOREIGN KEY (status_id) REFERENCES Statuses(status_id)
);

SET REFERENTIAL_INTEGRITY TRUE;