/*
 * The MIT License (MIT)
 *
 * Copyright (c) 2019 码之科技工作室
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of
 * this software and associated documentation files (the "Software"), to deal in
 * the Software without restriction, including without limitation the rights to
 * use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of
 * the Software, and to permit persons to whom the Software is furnished to do so,
 * subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS
 * FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR
 * COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER
 * IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN
 * CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */
package io.jpom.service.dblog;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.bean.copier.CopyOptions;
import cn.hutool.core.date.SystemClock;
import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.db.Db;
import cn.hutool.db.Entity;
import cn.hutool.db.Page;
import cn.hutool.db.PageResult;
import cn.hutool.db.sql.Direction;
import cn.hutool.db.sql.Order;
import cn.hutool.http.HttpStatus;
import cn.jiangzeyin.common.DefaultSystemLog;
import cn.jiangzeyin.common.JsonMessage;
import io.jpom.build.BuildUtil;
import io.jpom.model.data.BuildInfoModel;
import io.jpom.model.enums.BuildStatus;
import io.jpom.model.log.BuildHistoryLog;
import io.jpom.service.h2db.BaseWorkspaceService;
import io.jpom.system.ServerExtConfigBean;
import io.jpom.system.db.DbConfig;
import org.springframework.stereotype.Service;

import java.io.File;
import java.sql.SQLException;

/**
 * 构建历史db
 *
 * @author bwcx_jzy
 * @date 2019/7/20
 */
@Service
public class DbBuildHistoryLogService extends BaseWorkspaceService<BuildHistoryLog> {

	private final BuildInfoService buildService;

	public DbBuildHistoryLogService(BuildInfoService buildService) {
		this.buildService = buildService;
	}

	/**
	 * 根据 构建ID 删除构建历史
	 *
	 * @param buildDataId 构建ID
	 */
	public void delByBuildId(String buildDataId) {
		Entity where = new Entity(getTableName());
		where.set("buildDataId", buildDataId);
		del(where);
	}

	/**
	 * 更新状态
	 *
	 * @param logId  记录id
	 * @param status 状态
	 */
	public void updateLog(String logId, BuildStatus status) {
		if (logId == null) {
			return;
		}
		BuildHistoryLog buildHistoryLog = new BuildHistoryLog();
		buildHistoryLog.setId(logId);
		buildHistoryLog.setStatus(status.getCode());
		if (status != BuildStatus.PubIng) {
			// 结束
			buildHistoryLog.setEndTime(SystemClock.now());
		}
		this.update(buildHistoryLog);
	}

	/**
	 * 更新状态
	 *
	 * @param logId         记录id
	 * @param resultDirFile 构建产物目录
	 */
	public void updateResultDirFile(String logId, String resultDirFile) {
		if (logId == null || StrUtil.isEmpty(resultDirFile)) {
			return;
		}

		BuildHistoryLog buildHistoryLog = new BuildHistoryLog();
		buildHistoryLog.setId(logId);
		buildHistoryLog.setResultDirFile(resultDirFile);
		this.update(buildHistoryLog);
	}

	/**
	 * 清理文件并删除记录
	 *
	 * @param logId 记录id
	 * @return json
	 */
	public JsonMessage<String> deleteLogAndFile(String logId) {
		BuildHistoryLog buildHistoryLog = getByKey(logId);
		if (buildHistoryLog == null) {
			return new JsonMessage<>(405, "没有对应构建记录");
		}
		BuildInfoModel item = buildService.getByKey(buildHistoryLog.getBuildDataId());
		if (item != null) {
			File logFile = BuildUtil.getLogFile(item.getId(), buildHistoryLog.getBuildNumberId());
			if (logFile != null) {
				File dataFile = logFile.getParentFile();
				if (dataFile.exists()) {
					boolean s = FileUtil.del(dataFile);
					if (!s) {
						return new JsonMessage<>(500, "清理文件失败");
					}
				}
			}
		}
		int count = delByKey(logId);
		return new JsonMessage<>(200, "删除成功", count + "");
	}

	@Override
	public void insert(BuildHistoryLog buildHistoryLog) {
		super.insert(buildHistoryLog);
		// 清理总数据
		int buildMaxHistoryCount = ServerExtConfigBean.getInstance().getBuildMaxHistoryCount();
		DbConfig.autoClear(getTableName(), "startTime", buildMaxHistoryCount,
				aLong -> doClearPage(1, aLong, null));
		// 清理单个
		int buildItemMaxHistoryCount = ServerExtConfigBean.getInstance().getBuildItemMaxHistoryCount();
		DbConfig.autoClear(getTableName(), "startTime", buildItemMaxHistoryCount,
				entity -> entity.set("buildDataId", buildHistoryLog.getBuildDataId()),
				aLong -> doClearPage(1, aLong, buildHistoryLog.getBuildDataId()));
	}

	private void doClearPage(int pageNo, long time, String buildDataId) {
		Entity entity = Entity.create(getTableName());
		entity.set("startTime", "< " + time);
		if (buildDataId != null) {
			entity.set("buildDataId", buildDataId);
		}
		Page page = new Page(pageNo, 10);
		page.addOrder(new Order("startTime", Direction.DESC));
		PageResult<Entity> pageResult;
		try {
			pageResult = Db.use().setWrapper((Character) null).page(entity, page);
			if (pageResult.isEmpty()) {
				return;
			}
			pageResult.forEach(entity1 -> {
				CopyOptions copyOptions = new CopyOptions();
				copyOptions.setIgnoreError(true);
				copyOptions.setIgnoreCase(true);
				BuildHistoryLog v1 = BeanUtil.mapToBean(entity1, BuildHistoryLog.class, copyOptions);
				String id = v1.getId();
				JsonMessage<String> jsonMessage = deleteLogAndFile(id);
				if (jsonMessage.getCode() != HttpStatus.HTTP_OK) {
					DefaultSystemLog.getLog().info(jsonMessage.toString());
				}
			});
			if (pageResult.getTotalPage() > pageResult.getPage()) {
				doClearPage(pageNo + 1, time, buildDataId);
			}
		} catch (SQLException e) {
			DefaultSystemLog.getLog().error("数据库查询异常", e);
		}
	}
}
