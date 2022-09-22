/*
 * Copyright 2021 Google LLC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package cloudsql.tink;

import static com.google.common.truth.Truth.assertThat;
import static com.google.common.truth.Truth.assertWithMessage;

import com.google.crypto.tink.Aead;
import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.security.GeneralSecurityException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import javax.sql.DataSource;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

public class QueryDecryptDataIT {

  private static final String CLOUD_KMS_URI = System.getenv("CLOUD_KMS_URI");
  private static final String SQLSERVER_USER = System.getenv("SQLSERVER_USER");
  private static final String SQLSERVER_PASS = System.getenv("SQLSERVER_PASS");
  private static final String SQLSERVER_DB = System.getenv("SQLSERVER_DB");
  private static final String SQLSERVER_CONNECTION_NAME = System
      .getenv("SQLSERVER_CONNECTION_NAME");
  private static List<String> requiredEnvVars =
      Arrays
          .asList("SQLSERVER_USER", "SQLSERVER_PASS", "SQLSERVER_DB", "SQLSERVER_CONNECTION_NAME",
              "CLOUD_KMS_URI");
  private static DataSource pool;
  private static String tableName;
  private static Aead envAead;
  private ByteArrayOutputStream bout;
  private PrintStream originalOut = System.out;

  public static void checkEnvVars() {
    // Check that required env vars are set
    requiredEnvVars.forEach((varName) -> {
      assertWithMessage(
          String.format("Environment variable '%s' must be set to perform these tests.", varName))
          .that(System.getenv(varName)).isNotEmpty();
    });
  }

  @BeforeClass
  public static void setUp() throws GeneralSecurityException, SQLException {
    checkEnvVars();
    tableName = String.format("votes_%s", UUID.randomUUID().toString().replace("-", ""));

    pool = CloudSqlConnectionPool
        .createConnectionPool(SQLSERVER_USER, SQLSERVER_PASS, SQLSERVER_DB,
            SQLSERVER_CONNECTION_NAME);
    CloudSqlConnectionPool.createTable(pool, tableName);

    envAead = CloudKmsEnvelopeAead.get(CLOUD_KMS_URI);
    EncryptAndInsertData
        .encryptAndInsertData(pool, envAead, tableName, "TABS", "hello@example.com");
  }

  @AfterClass
  public static void tearDown() throws SQLException {
    if (pool != null) {
      try (Connection conn = pool.getConnection()) {
        String stmt = String.format("DROP TABLE %s;", tableName);
        try (PreparedStatement createTableStatement = conn.prepareStatement(stmt);) {
          createTableStatement.execute();
        }
      }
    }
  }

  @Before
  public void captureOutput() {
    bout = new ByteArrayOutputStream();
    System.setOut(new PrintStream(bout));
  }

  @After
  public void resetOutput() {
    System.setOut(originalOut);
    bout.reset();
  }

  @Test
  public void testQueryAndDecryptData() throws GeneralSecurityException, SQLException {
    QueryAndDecryptData.queryAndDecryptData(pool, envAead, tableName);
    String output = bout.toString();
    assertThat(output).contains("Team\tTime Cast\tEmail");
    assertThat(output).contains("hello@example.com");
  }

}