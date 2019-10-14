package es.uvigo.esei.dai.hybridserver.adt;

import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Collection;
import java.util.Map;
import java.util.Set;

public final class JDBCBackedHTMLResourceMap implements IOBackedMap<String, String> {
	private Connection dbConnection = null;

	public JDBCBackedHTMLResourceMap(final String dbUrl, final String dbUser, final String dbPassword) {
		try {
			this.dbConnection = DriverManager.getConnection(dbUrl, dbUser, dbPassword);
		} catch (final SQLException exc) {
			// Ignore for now, as we want to only throw exceptions when
			// obtaining data for responses
		}
	}

	@Override
	public int size() throws IOException {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public boolean isEmpty() throws IOException {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean containsKey(final String key) throws IOException {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean containsValue(final String value) throws IOException {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public String get(final String key) throws IOException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String put(final String key, final String value) throws IOException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String remove(final String key) throws IOException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void putAll(Map<? extends String, ? extends String> m) throws IOException {
		// TODO Auto-generated method stub

	}

	@Override
	public void clear() throws IOException {
		// TODO Auto-generated method stub

	}

	@Override
	public Set<String> keySet() throws IOException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Collection<String> values() throws IOException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Set<Map.Entry<String, String>> entrySet() throws IOException {
		// TODO Auto-generated method stub
		return null;
	}
}
