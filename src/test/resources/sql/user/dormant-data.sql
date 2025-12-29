INSERT INTO Statuses (status_id, status_name)
VALUES (1, 'ACTIVE'),
       (2, 'DORMANT');

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

-- 휴면 대상 유저 (91일 전 로그인)
INSERT INTO Users (user_created_id)
VALUES (1);
INSERT INTO Accounts (login_id, user_created_id, last_login_at, current_status_id)
VALUES ('targetUser', 1, DATEADD('DAY', -91, CURRENT_DATE), 1);
INSERT INTO AccountStatusHistories (login_id, status_id, changed_at)
VALUES ('targetUser', 1, DATEADD('DAY', -91, CURRENT_DATE));

-- 일반 유저 (1일 전 로그인)
INSERT INTO Users (user_created_id)
VALUES (2);
INSERT INTO Accounts (login_id, user_created_id, last_login_at, current_status_id)
VALUES ('activeUser', 2, DATEADD('DAY', -1, CURRENT_DATE), 1);
INSERT INTO AccountStatusHistories (login_id, status_id, changed_at)
VALUES ('activeUser', 1, DATEADD('DAY', -1, CURRENT_DATE));