SET REFERENTIAL_INTEGRITY FALSE;

DROP TABLE IF EXISTS UserGradeHistories CASCADE;
DROP TABLE IF EXISTS OrderDetails CASCADE;
DROP TABLE IF EXISTS Orders CASCADE;
DROP TABLE IF EXISTS Accounts CASCADE;
DROP TABLE IF EXISTS Users CASCADE;
DROP TABLE IF EXISTS Grades CASCADE;
DROP TABLE IF EXISTS Statuses CASCADE;

-- 1. 기초 정보 테이블
CREATE TABLE Statuses (
    status_id   BIGINT AUTO_INCREMENT PRIMARY KEY,
    status_name VARCHAR(10) NOT NULL
);

CREATE TABLE Grades (
    grade_id   BIGINT AUTO_INCREMENT PRIMARY KEY,
    grade_name VARCHAR(10) NOT NULL,
    point_rate DECIMAL(4, 2) NOT NULL
);

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

-- 2. 회원 정보 테이블
CREATE TABLE Users (
    user_created_id     BIGINT AUTO_INCREMENT PRIMARY KEY,
    current_grade_id    BIGINT NOT NULL DEFAULT 1,
    FOREIGN KEY (current_grade_id) REFERENCES Grades(grade_id)
);

CREATE TABLE Accounts (
    login_id           VARCHAR(50) PRIMARY KEY,
    user_created_id    BIGINT NOT NULL,
    current_status_id  BIGINT NOT NULL DEFAULT 1,
    FOREIGN KEY (user_created_id) REFERENCES Users(user_created_id),
    FOREIGN KEY (current_status_id) REFERENCES Statuses(status_id)
);

-- 3. 주문 관련 테이블
CREATE TABLE Orders (
    order_id         BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_created_id  BIGINT,
    order_date       TIMESTAMP NOT NULL,
    total_price      BIGINT NOT NULL,
    FOREIGN KEY (user_created_id) REFERENCES Users(user_created_id)
);

CREATE TABLE OrderDetails (
    order_detail_id     BIGINT AUTO_INCREMENT PRIMARY KEY,
    order_id            BIGINT NOT NULL,
    price               BIGINT NOT NULL,
    order_detail_status VARCHAR(20) NOT NULL,
    FOREIGN KEY (order_id) REFERENCES Orders(order_id)
);

-- 4. 이력 테이블
CREATE TABLE UserGradeHistories (
    user_grade_history_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_created_id       BIGINT NOT NULL,
    grade_id              BIGINT NOT NULL,
    reason                VARCHAR(100),
    changed_at            TIMESTAMP NOT NULL,
    FOREIGN KEY (user_created_id) REFERENCES Users(user_created_id),
    FOREIGN KEY (grade_id) REFERENCES Grades(grade_id)
);

SET REFERENTIAL_INTEGRITY TRUE;