package com.aifilm.support;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import javax.sql.DataSource;

// 요청 한 번에 MySQL이 실제로 받은 SELECT 수를 센다. (Com_select 카운터)
// 같은 MySQL을 쓰는 다른 테스트 컨텍스트의 커넥션 초기화 쿼리가 섞일 수 있어서 여러 번 재고 가장 작은 값을 쓴다.
public final class SelectCounter {

	private static final int MEASURE_TIMES = 3;

	private final DataSource dataSource;

	public SelectCounter(DataSource dataSource) {
		this.dataSource = dataSource;
	}

	// 첫 호출의 준비 작업이 섞이지 않게 한 번 먼저 실행한 뒤 잰다
	public long measure(Request request) throws Exception {
		request.run();
		long fewest = Long.MAX_VALUE;
		for (int i = 0; i < MEASURE_TIMES; i++) {
			long before = count();
			request.run();
			fewest = Math.min(fewest, count() - before);
		}
		return fewest;
	}

	// 카운터를 읽는 문장 자체가 세어지는지 확인하려고, 요청 없이 두 번 읽은 차이를 돌려준다
	public long overhead() throws SQLException {
		long before = count();
		return count() - before;
	}

	private long count() throws SQLException {
		try (Connection connection = dataSource.getConnection();
			Statement statement = connection.createStatement();
			ResultSet rows = statement.executeQuery("SHOW GLOBAL STATUS LIKE 'Com_select'")) {
			rows.next();
			return rows.getLong("Value");
		}
	}

	@FunctionalInterface
	public interface Request {

		void run() throws Exception;

	}

}
