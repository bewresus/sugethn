package io.jpom.service.h2db;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.lang.Console;
import cn.hutool.db.ds.DSFactory;
import cn.hutool.db.sql.SqlLog;
import cn.hutool.setting.Setting;
import cn.jiangzeyin.common.spring.SpringUtil;
import io.jpom.ApplicationStartTest;
import io.jpom.JpomApplication;
import io.jpom.JpomServerApplication;
import io.jpom.common.JpomManifest;
import io.jpom.system.ExtConfigBean;
import io.jpom.system.ServerExtConfigBean;
import io.jpom.system.db.DbConfig;
import io.jpom.system.init.InitDb;
import org.h2.command.dml.RunScriptCommand;
import org.h2.engine.Database;
import org.h2.tools.Restore;
import org.h2.tools.RunScript;
import org.h2.tools.Script;
import org.h2.tools.Shell;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;

import javax.sql.DataSource;
import java.io.*;
import java.sql.Connection;
import java.sql.SQLException;

/**
 * @author Hotstrip
 * @since 2021-11-01
 * 测试 H2 数据库工具类
 */
public class H2ToolTest extends ApplicationStartTest {

	/**
	 * 备份 SQL 文件
	 * 如果抛出了异常或者指定的备份文件不存在就表示备份不成功
	 * @throws SQLException
	 */
	@Test
	public void testH2ShellForBackupSQL() throws SQLException {
		// 数据源参数
		String url = DbConfig.getInstance().getDbUrl();

		ServerExtConfigBean serverExtConfigBean = ServerExtConfigBean.getInstance();
		String user = serverExtConfigBean.getDbUserName();
		String pass = serverExtConfigBean.getDbUserPwd();

		// 备份 SQL 的目录
		File file = FileUtil.file(ExtConfigBean.getInstance().getPath(), "db", JpomApplication.getAppType().name());
		String path = FileUtil.getAbsolutePath(file);

		logger.info("url: {}", url);
		logger.info("user: {}", user);
		logger.info("pass: {}", pass);
		logger.info("backup sql path: {}", path);

		// 加载数据源
		DataSource dataSource = DSFactory.get();
		if (null == dataSource) {
			dataSource = initDataSource(url, user, pass);
		}
		Connection connection = dataSource.getConnection();

		// 执行 SQL 备份脚本
		Shell shell = new Shell();

		/**
		 * url 表示 h2 数据库的 jdbc url
		 * user 表示登录的用户名
		 * password 表示登录密码
		 * driver 是 jdbc 驱动
		 * sql 是备份的 sql 语句
		 * - 案例：script drop to ${fileName1} table ${tableName1},${tableName2}...
		 * - script drop to 表示备份数据库，drop 表示建表之前会先删除表
		 * - ${fileName1} 表示备份之后的文件名
		 * - table 表示需要备份的表名称，后面跟多个表名，用英文逗号分割
		 */
		String[] params = new String[] {
				"-url", url,
				"-user", user,
				"-password", pass,
				"-driver", "org.h2.Driver",
				"-sql", "script DROP to '"+ path +"/backup.sql' table BUILD_INFO,USEROPERATELOGV1"
		};
		shell.runTool(connection, params);
	}

	/**
	 * 还原 SQL 文件
	 */
	@Test
	public void testH2RunScriptWithBackupSQL() throws SQLException, FileNotFoundException {
		// 备份 SQL
		testH2ShellForBackupSQL();

		// 恢复之前先删除数据库数据，以免冲突
		testH2DropAllObjects();

		// 备份 SQL 的目录
		File file = FileUtil.file(ExtConfigBean.getInstance().getPath(), "db", JpomApplication.getAppType().name());
		String path = FileUtil.getAbsolutePath(file) + "/backup.sql";

		FileReader fileReader = new FileReader(path);

		// 加载数据源
		DataSource dataSource = DSFactory.get();
		Connection connection = dataSource.getConnection();

		RunScript.execute(connection, fileReader);
	}

	/**
	 * H2 drop all objects
	 * 删除 H2 数据库所有数据
	 * @throws SQLException
	 */
	@Test
	public void testH2DropAllObjects() throws SQLException {
		// 数据源参数
		String url = DbConfig.getInstance().getDbUrl();

		ServerExtConfigBean serverExtConfigBean = ServerExtConfigBean.getInstance();
		String user = serverExtConfigBean.getDbUserName();
		String pass = serverExtConfigBean.getDbUserPwd();

		// 加载数据源
		DataSource dataSource = DSFactory.get();
		Connection connection = dataSource.getConnection();

		// 执行 SQL 备份脚本
		Shell shell = new Shell();

		String[] params = new String[] {
				"-url", url,
				"-user", user,
				"-password", pass,
				"-driver", "org.h2.Driver",
				"-sql", "drop all objects"
		};
		shell.runTool(connection, params);
	}

	/**
	 * 初始化数据源
	 * @return
	 */
	private DataSource initDataSource(String url, String user, String pass) {
		// 数据源配置
		Setting setting = new Setting();
		setting.set("url", url);
		setting.set("user", user);
		setting.set("pass", pass);
		// 配置连接池大小
		setting.set("maxActive", "50");
		setting.set("initialSize", "1");
		setting.set("maxWait", "10");
		setting.set("minIdle", "1");
		// show sql
		setting.set(SqlLog.KEY_SHOW_SQL, "true");
		setting.set(SqlLog.KEY_SQL_LEVEL, "DEBUG");
		setting.set(SqlLog.KEY_SHOW_PARAMS, "true");
		Console.log("start load h2 db");
		// 创建连接
		DSFactory dsFactory = DSFactory.create(setting);
		return dsFactory.getDataSource();
	}

}
