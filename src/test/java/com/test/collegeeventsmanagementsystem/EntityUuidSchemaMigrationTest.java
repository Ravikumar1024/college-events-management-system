package com.test.collegeeventsmanagementsystem;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;
import java.sql.Types;
import java.util.List;

import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;

class EntityUuidSchemaMigrationTest {
	@Test
	void createsUuidSchemaAndSeedsUuidIds() throws Exception {
		String url = "jdbc:h2:mem:uuid_schema_test;MODE=PostgreSQL;DB_CLOSE_DELAY=-1";
		Flyway.configure().dataSource(url, "sa", "").locations("classpath:db/migration/h2").load().migrate();

		try (Connection connection = DriverManager.getConnection(url, "sa", "")) {
			for (String table : List.of("roles", "users", "students", "event_categories", "events",
					"event_registrations", "email_verification_tokens", "event_invitations", "notifications")) {
				assertVarchar36(connection, table, "id");
			}
			for (String[] column : List.of(
					new String[] { "user_roles", "user_id" },
					new String[] { "user_roles", "role_id" },
					new String[] { "students", "user_id" },
					new String[] { "students", "department_id" },
					new String[] { "events", "category_id" },
					new String[] { "events", "organizer_id" },
					new String[] { "event_registrations", "event_id" },
					new String[] { "event_registrations", "student_id" },
					new String[] { "email_verification_tokens", "user_id" },
					new String[] { "event_invitations", "event_id" },
					new String[] { "event_invitations", "student_id" },
					new String[] { "event_invitations", "invited_by_user_id" },
					new String[] { "notifications", "recipient_user_id" },
					new String[] { "notifications", "event_id" },
					new String[] { "notifications", "reference_id" })) {
				assertVarchar36(connection, column[0], column[1]);
			}

			assertEquals(3, queryLong(connection, "SELECT COUNT(*) FROM roles"));
			assertEquals(6, queryLong(connection, "SELECT COUNT(*) FROM event_categories"));
			assertUuid(queryString(connection, "SELECT id FROM roles LIMIT 1"));
			assertUuid(queryString(connection, "SELECT id FROM event_categories LIMIT 1"));
		}
	}

	private void assertVarchar36(Connection connection, String table, String column) throws Exception {
		DatabaseMetaData metadata = connection.getMetaData();
		try (ResultSet result = metadata.getColumns(null, "PUBLIC", table.toUpperCase(), column.toUpperCase())) {
			assertTrue(result.next(), table + "." + column + " should exist");
			assertEquals(Types.VARCHAR, result.getInt("DATA_TYPE"), table + "." + column + " should be VARCHAR");
			assertEquals(36, result.getInt("COLUMN_SIZE"), table + "." + column + " should have length 36");
		}
	}

	private long queryLong(Connection connection, String sql) throws Exception {
		try (Statement statement = connection.createStatement(); ResultSet result = statement.executeQuery(sql)) {
			assertTrue(result.next());
			return result.getLong(1);
		}
	}

	private String queryString(Connection connection, String sql) throws Exception {
		try (Statement statement = connection.createStatement(); ResultSet result = statement.executeQuery(sql)) {
			assertTrue(result.next());
			return result.getString(1);
		}
	}

	private void assertUuid(String id) {
		assertTrue(id.matches("[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}"));
	}
}
