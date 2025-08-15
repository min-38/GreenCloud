package com.greencloud.server.config;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;

import javax.sql.DataSource;
import java.sql.Connection;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@AutoConfigureMockMvc
class DBConnTest {

    @Autowired
    private DataSource dataSource;

    @Test
    @DisplayName("application-dbconn.yml에 있는 정보로 DB 연결 테스트")
    void testDBConnection() throws Exception {
        try (Connection conn = dataSource.getConnection()) {
            assertThat(conn).isNotNull();
            System.out.println("DB 연결 성공: " + conn.getMetaData().getURL());
        }
    }
}