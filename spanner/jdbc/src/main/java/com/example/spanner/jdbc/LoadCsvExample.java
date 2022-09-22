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

package com.example.spanner.jdbc;

//[START spanner_jdbc_load_csv]
import com.google.cloud.spanner.Mutation;
import com.google.cloud.spanner.Mutation.WriteBuilder;
import com.google.cloud.spanner.Spanner;
import com.google.cloud.spanner.SpannerOptions;
import com.google.cloud.spanner.Value;
import com.google.cloud.spanner.jdbc.CloudSpannerJdbcConnection;
import com.google.spanner.v1.TypeCode;
import java.io.FileReader;
import java.io.Reader;
import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.Options;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;

/** Sample showing how to load CSV file data into Spanner */
class LoadCsvExample {
  static final String EXCEL = "EXCEL";
  static final String POSTGRESQL_CSV = "POSTGRESQL_CSV";
  static final String POSTGRESQL_TEXT = "POSTGRESQL_TEXT";

  static Boolean hasHeader = false;
  static Connection connection;
  static Map<String, TypeCode> tableColumns = new LinkedHashMap<>();

  static void loadCsv() throws Exception {
    // TODO(developer): Replace these variables before running the sample.
    String projectId = "my-project-id";;
    String instanceId = "my-instance-id";
    String databaseId = "my-database-id";
    String tableName = "my-table-name";
    String filePath = "my-file-path";
    String[] optFlags = {"my-opt-flag", "my-opt-arg"};
    loadCsv(projectId, instanceId, databaseId, tableName, filePath, optFlags);
  }

  static void loadCsv(String projectId, String instanceId, String databaseId,
      String tableName, String filePath, String[] optFlags) throws Exception {

    SpannerOptions options = SpannerOptions.newBuilder().build();
    Spanner spanner = options.getService();

    // Initialize option flags
    Options opt = new Options();
    opt.addOption("h", true, "File Contains Header");
    opt.addOption("f", true, "Format Type of Input File "
        + "(EXCEL, POSTGRESQL_CSV, POSTGRESQL_TEXT, DEFAULT)");
    opt.addOption("n", true, "String Representing Null Value");
    opt.addOption("d", true, "Character Separating Columns");
    opt.addOption("e", true, "Character To Escape");
    CommandLineParser clParser = new DefaultParser();
    CommandLine cmd = clParser.parse(opt, optFlags);

    try {
      // Initialize connection to Cloud Spanner
      connection = DriverManager.getConnection(
          String.format("jdbc:cloudspanner:/projects/%s/instances/%s/databases/%s",
              projectId, instanceId, databaseId));
      parseTableColumns(tableName);

      try (
          Reader in = new FileReader(filePath);
          CSVParser parser = CSVParser.parse(in, setFormat(cmd));
      ) {
        // If file has header, verify that header fields are valid
        if (hasHeader && !isValidHeader(parser)) {
          return;
        }

        // Write CSV record data to Cloud Spanner
        writeToSpanner(parser, tableName);

      } catch (SQLException e) {
        /* SQLExceptions are thrown when the table name cannot be queried for in the database
           or the connection established does not have permissions to write data into the table */
        System.out.println(e.getMessage());
      }

    } finally {
      spanner.close();
      connection.close();
    }
  }

  /** Return the data type of the column type **/
  static TypeCode parseSpannerDataType(String columnType) {
    if (columnType.matches("(?i)STRING(?:\\((?:MAX|[0-9]+)\\))?")) {
      return TypeCode.STRING;
    } else if (columnType.matches("(?i)BYTES(?:\\((?:MAX|[0-9]+)\\))?")) {
      return TypeCode.BYTES;
    } else if (columnType.equalsIgnoreCase("INT64")) {
      return TypeCode.INT64;
    } else if (columnType.equalsIgnoreCase("FLOAT64")) {
      return TypeCode.FLOAT64;
    } else if (columnType.equalsIgnoreCase("NUMERIC")) {
      return TypeCode.NUMERIC;
    } else if (columnType.equalsIgnoreCase("BOOL")) {
      return TypeCode.BOOL;
    } else if (columnType.equalsIgnoreCase("DATE")) {
      return TypeCode.DATE;
    } else if (columnType.equalsIgnoreCase("TIMESTAMP")) {
      return TypeCode.TIMESTAMP;
    } else {
      throw new IllegalArgumentException(
          "Unrecognized or unsupported column data type: " + columnType);
    }
  }

  /** Query database for column names and types in the table **/
  static void parseTableColumns(String tableName) throws SQLException {
    ResultSet spannerType = connection.createStatement()
        .executeQuery("SELECT column_name, spanner_type FROM information_schema.columns "
            + "WHERE table_name = \"" + tableName + "\" ORDER BY ordinal_position");
    while (spannerType.next()) {
      String columnName = spannerType.getString("column_name");
      TypeCode type = parseSpannerDataType(spannerType.getString("spanner_type"));
      tableColumns.put(columnName, type);
    }
  }

  /** Check that CSV file headers exist as a table column name **/
  static boolean isValidHeader(CSVParser parser) {
    List<String> csvHeaders = parser.getHeaderNames();
    for (String csvHeader : csvHeaders) {
      if (!tableColumns.containsKey(csvHeader)) {
        System.out.println(
            "File header " + csvHeader + " does not match any database table column name.");
        return false;
      }
    }
    return true;
  }

  /** Initialize CSV Parser format based on user specified option flags **/
  public static CSVFormat setFormat(CommandLine cmd) {
    CSVFormat parseFormat;
    // Set file format type
    if (cmd.hasOption("f")) {
      switch (cmd.getOptionValue("f").toUpperCase()) {
        case EXCEL:
          parseFormat = CSVFormat.EXCEL;
          break;
        case POSTGRESQL_CSV:
          parseFormat = CSVFormat.POSTGRESQL_CSV;
          break;
        case POSTGRESQL_TEXT:
          parseFormat = CSVFormat.POSTGRESQL_TEXT;
          break;
        default:
          parseFormat = CSVFormat.DEFAULT;
      }
    } else {
      parseFormat = CSVFormat.DEFAULT;
    }
    // Set null string representation
    if (cmd.hasOption("n")) {
      parseFormat = parseFormat.withNullString(cmd.getOptionValue("n"));
    }
    // Set delimiter character
    if (cmd.hasOption("d")) {
      if (cmd.getOptionValue("d").length() != 1) {
        throw new IllegalArgumentException("Invalid delimiter character entered.");
      }
      parseFormat = parseFormat.withDelimiter(cmd.getOptionValue("d").charAt(0));
    }
    // Set escape character
    if (cmd.hasOption("e")) {
      if (cmd.getOptionValue("e").length() != 1) {
        throw new IllegalArgumentException("Invalid escape character entered.");
      }
      parseFormat = parseFormat.withEscape(cmd.getOptionValue("e").charAt(0));
    }
    // Set parser to parse first row as headers
    if (cmd.hasOption("h") && cmd.getOptionValue("h").equalsIgnoreCase("True")) {
      parseFormat = parseFormat.withFirstRecordAsHeader();
      hasHeader = true;
    }
    return parseFormat;
  }

  /** Verifies that if file has a header, that the record is mapped to a column header name
   * and that the record itself is not null **/
  static boolean validHeaderField(CSVRecord record, String columnName) {
    return hasHeader && record.isMapped(columnName) && record.get(columnName) != null;
  }

  /** Verifies that if the file has no header, that the record at the given index is not null **/
  static boolean validNonHeaderField(CSVRecord record, int index) {
    return !hasHeader && record.get(index) != null;
  }

  /** Write CSV file data to Spanner using JDBC Mutation API **/
  static void writeToSpanner(Iterable<CSVRecord> records, String tableName)
      throws SQLException {
    System.out.println("Writing data into table...");
    List<Mutation> mutations = new ArrayList<>();
    for (CSVRecord record : records) {
      int index = 0;
      WriteBuilder builder = Mutation.newInsertOrUpdateBuilder(tableName);
      for (String columnName : tableColumns.keySet()) {
        // Iterates through columns in order. Assumes in order columns when no headers provided.
        TypeCode columnType = tableColumns.get(columnName);
        String recordValue = null;
        if (validHeaderField(record, columnName)) {
          recordValue = record.get(columnName).trim();
        } else if (validNonHeaderField(record, index)) {
          recordValue = record.get(index).trim();
          index++;
        }
        if (recordValue != null) {
          switch (columnType) {
            case STRING:
              builder.set(columnName).to(recordValue);
              break;
            case BYTES:
              builder.set(columnName).to(Byte.parseByte(recordValue));
              break;
            case INT64:
              builder.set(columnName).to(Integer.parseInt(recordValue));
              break;
            case FLOAT64:
              builder.set(columnName).to(Float.parseFloat(recordValue));
              break;
            case BOOL:
              builder.set(columnName).to(Boolean.parseBoolean(recordValue));
              break;
            case NUMERIC:
              builder.set(columnName).to(Value.numeric(BigDecimal.valueOf(
                  Double.parseDouble(recordValue))));
              break;
            case DATE:
              builder.set(columnName).to(com.google.cloud.Date.parseDate(recordValue));
              break;
            case TIMESTAMP:
              builder.set(columnName).to(com.google.cloud.Timestamp.parseTimestamp(recordValue));
              break;
            default:
              System.out.print("Invalid Type. This type is not supported.");
          }
        }
      }
      mutations.add(builder.build());
    }
    CloudSpannerJdbcConnection spannerConnection = connection
        .unwrap(CloudSpannerJdbcConnection.class);
    spannerConnection.write(mutations);
    spannerConnection.close();
    System.out.println("Data successfully written into table.");
  }
}
//[END spanner_jdbc_load_csv]
