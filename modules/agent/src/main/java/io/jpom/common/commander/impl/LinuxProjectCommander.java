package io.jpom.common.commander.impl;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.text.StrSpliter;
import cn.hutool.core.util.StrUtil;
import io.jpom.common.commander.AbstractProjectCommander;
import io.jpom.common.commander.AbstractSystemCommander;
import io.jpom.model.data.ProjectInfoModel;
import io.jpom.model.system.NetstatModel;
import io.jpom.util.CommandUtil;
import io.jpom.util.JvmUtil;

import java.util.ArrayList;
import java.util.List;

/**
 * linux
 *
 * @author Administrator
 */
public class LinuxProjectCommander extends AbstractProjectCommander {

    @Override
    public String buildCommand(ProjectInfoModel projectInfoModel) {
        String path = ProjectInfoModel.getClassPathLib(projectInfoModel);
        if (StrUtil.isBlank(path)) {
            return null;
        }
        return String.format("nohup java %s %s" +
                        " %s  %s  %s >> %s 2>&1 &",
                projectInfoModel.getJvm(), JvmUtil.getJpomPidTag(projectInfoModel.getId(), projectInfoModel.allLib()),
                path,
                projectInfoModel.getMainClass(),
                projectInfoModel.getArgs(),
                projectInfoModel.getAbsoluteLog());
    }

    @Override
    public String stop(ProjectInfoModel projectInfoModel) throws Exception {
        String result = super.stop(projectInfoModel);
        int pid = parsePid(result);
        if (pid > 0) {
            AbstractSystemCommander.getInstance().kill(FileUtil.file(projectInfoModel.allLib()), pid);
            if (loopCheckRun(projectInfoModel.getId(), false)) {
                // 强制杀进程
                String cmd = String.format("kill -9 %s", pid);
                CommandUtil.asyncExeLocalCommand(FileUtil.file(projectInfoModel.allLib()), cmd);
            }
            String tag = projectInfoModel.getId();
            result = status(tag);
        }
        return result;
    }

    @Override
    public List<NetstatModel> listNetstat(int pId, boolean listening) {
        String cmd;
        if (listening) {
            cmd = "netstat -antup | grep " + pId + " |grep \"LISTEN\" | head -20";
        } else {
            cmd = "netstat -antup | grep " + pId + " |grep -v \"CLOSE_WAIT\" | head -20";
        }
        String result = CommandUtil.execSystemCommand(cmd);
        List<String> netList = StrSpliter.splitTrim(result, "\n", true);
        if (netList == null || netList.size() <= 0) {
            return null;
        }
        List<NetstatModel> array = new ArrayList<>();
        for (String str : netList) {
            List<String> list = StrSpliter.splitTrim(str, " ", true);
            if (list.size() < 5) {
                continue;
            }
            NetstatModel netstatModel = new NetstatModel();
            netstatModel.setProtocol(list.get(0));
            netstatModel.setReceive(list.get(1));
            netstatModel.setSend(list.get(2));
            netstatModel.setLocal(list.get(3));
            netstatModel.setForeign(list.get(4));
            if ("tcp".equalsIgnoreCase(netstatModel.getProtocol())) {
                netstatModel.setStatus(list.get(5));
                netstatModel.setName(list.get(6));
            } else {
                netstatModel.setStatus(StrUtil.DASHED);
                netstatModel.setName(list.get(5));
            }
            array.add(netstatModel);
        }
        return array;
    }
}
