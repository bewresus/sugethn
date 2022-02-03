package cn.jiangzeyin.socket.top;

import cn.hutool.cron.CronUtil;
import cn.jiangzeyin.common.DefaultSystemLog;
import cn.jiangzeyin.common.spring.SpringUtil;
import cn.jiangzeyin.service.manage.CommandService;
import cn.jiangzeyin.socket.SocketSession;

import javax.websocket.Session;
import java.io.IOException;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

/**
 * Created by jiangzeyin on 2018/10/2.
 */
public class TopManager {

    private static final Set<Session> SESSIONS = new HashSet<>();
    private static final String CRON_ID = "topMonitor";
    private static CommandService commandService;
    private static boolean watch = false;

    public static void addMonitor(Session session) {
        SESSIONS.add(session);
        addCron();
    }

    public static void removeMonitor(Session session) {
        SESSIONS.remove(session);
        close(null);
    }

    private static void addCron() {
        if (watch) {
            return;
        }
        if (commandService == null) {
            commandService = SpringUtil.getBean(CommandService.class);
        }
        CronUtil.remove(CRON_ID);
        CronUtil.setMatchSecond(true);
        CronUtil.schedule(CRON_ID, "0/5 * * * * ?", () -> {
            String result = commandService.execCommand(CommandService.CommandOp.top, null, null);
            send(result);
        });
        CronUtil.restart();
        watch = true;
    }

    private static void send(String content) {
        synchronized (TopManager.class) {
            Iterator<Session> iterator = SESSIONS.iterator();
            while (iterator.hasNext()) {
                Session session = iterator.next();
                content = content.replaceAll("\n", "<br/>");
                content = content.replaceAll(" ", "&nbsp;&nbsp;");
                try {
                    SocketSession.send(session, content);
                } catch (IOException e) {
                    DefaultSystemLog.ERROR().error("消息失败", e);
                    try {
                        session.close();
                        iterator.remove();
                    } catch (IOException ignored) {
                    }
                }
            }
            close(null);
        }
    }

    private static void close(String msg) {
        // 如果没有队列就停止监听
        int size = SESSIONS.size();
        if (size > 0) {
            return;
        }
        if (msg != null) {
            Iterator<Session> iterator = SESSIONS.iterator();
            while (iterator.hasNext()) {
                Session session = iterator.next();
                try {
                    SocketSession.send(session, msg);
                } catch (IOException e) {
                    DefaultSystemLog.ERROR().error("消息失败", e);
                }
                try {
                    session.close();
                    iterator.remove();
                } catch (IOException ignored) {
                }
            }
        }
        CronUtil.remove(CRON_ID);
        watch = false;
    }
}
