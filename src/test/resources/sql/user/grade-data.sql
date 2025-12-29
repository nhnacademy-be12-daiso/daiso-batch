INSERT INTO Statuses (status_id, status_name)
VALUES (1, 'ACTIVE'),
       (2, 'DORMANT');
INSERT INTO Grades (grade_id, grade_name, point_rate)
VALUES (1, 'GENERAL', 1.0),
       (2, 'ROYAL', 2.0),
       (3, 'GOLD', 3.0),
       (4, 'PLATINUM', 5.0);

-- 유저 1: ROYAL 등급 대상 (15만원 구매)
INSERT INTO Users (user_created_id, current_grade_id)
VALUES (10, 1);
INSERT INTO Accounts (login_id, user_created_id, current_status_id)
VALUES ('royalUser', 10, 1);
INSERT INTO Orders (order_id, user_created_id, order_date, total_price)
VALUES (100, 10, CURRENT_TIMESTAMP, 150000);
INSERT INTO OrderDetails (order_id, price, order_detail_status)
VALUES (100, 150000, 'COMPLETED');

-- 유저 2: PLATINUM 등급 대상 (35만원 구매)
INSERT INTO Users (user_created_id, current_grade_id)
VALUES (20, 1);
INSERT INTO Accounts (login_id, user_created_id, current_status_id)
VALUES ('platinumUser', 20, 1);
INSERT INTO Orders (order_id, user_created_id, order_date, total_price)
VALUES (200, 20, CURRENT_TIMESTAMP, 350000);
INSERT INTO OrderDetails (order_id, price, order_detail_status)
VALUES (200, 350000, 'COMPLETED');

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

-- 유저 3: 제외 대상 (PENDING 상태 주문)
INSERT INTO Users (user_created_id, current_grade_id)
VALUES (30, 1);
INSERT INTO Accounts (login_id, user_created_id, current_status_id)
VALUES ('pendingUser', 30, 1);
INSERT INTO Orders (order_id, user_created_id, order_date, total_price)
VALUES (300, 30, CURRENT_TIMESTAMP, 500000);
INSERT INTO OrderDetails (order_id, price, order_detail_status)
VALUES (300, 500000, 'PENDING');