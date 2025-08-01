package cn.keepbx.jpom.controller.node.tomcat;

import cn.hutool.core.util.StrUtil;
import cn.jiangzeyin.common.JsonMessage;
import cn.keepbx.jpom.common.BaseServerController;
import cn.keepbx.jpom.common.forward.NodeForward;
import cn.keepbx.jpom.common.forward.NodeUrl;
import cn.keepbx.jpom.common.interceptor.ProjectPermission;
import cn.keepbx.jpom.common.interceptor.UrlPermission;
import cn.keepbx.jpom.model.Role;
import cn.keepbx.jpom.model.data.NodeModel;
import cn.keepbx.jpom.model.data.UserModel;
import cn.keepbx.jpom.model.data.UserOperateLogV1;
import cn.keepbx.jpom.service.manage.TomcatService;
import cn.keepbx.jpom.system.OperateType;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.annotation.Resource;

/**
 * tomcat 管理
 *
 * @author lf
 */
@Controller
@RequestMapping(value = TomcatManageController.TOMCAT_URL)
public class TomcatManageController extends BaseServerController {

    public static final String TOMCAT_URL = "/node/tomcat/";


    @Resource
    private TomcatService tomcatService;

    /**
     * 查询tomcat列表
     *
     * @return tomcat列表
     */
    @RequestMapping(value = "tomcatManage", method = RequestMethod.GET, produces = MediaType.TEXT_HTML_VALUE)
    public String tomcatManage() {
        // 查询tomcat列表
        JSONArray tomcatInfos = tomcatService.getTomcatList(getNode());
        setAttribute("array", tomcatInfos);
        return "node/tomcat/list";
    }

    /**
     * 查询tomcat的项目
     *
     * @return tomcat的项目信息
     */
    @RequestMapping(value = "getTomcatProject", method = RequestMethod.POST, produces = MediaType.TEXT_HTML_VALUE)
    @ResponseBody
    public String getTomcatProject(String id) {
        // 查询tomcat管理的项目的列表
        JSONArray tomcatProjects = tomcatService.getTomcatProjectList(getNode(), id);
        return JsonMessage.getString(200, "查询成功", tomcatProjects);
    }

    /**
     * 获取编辑omcat页面
     *
     * @param id tomcat的id
     * @return 编辑tomcat信息页面
     */
    @RequestMapping(value = "edit.html", method = RequestMethod.GET, produces = MediaType.TEXT_HTML_VALUE)
    public String edit(String id) {
        if (StrUtil.isNotEmpty(id)) {
            JSONObject tomcatInfo = tomcatService.getTomcatInfo(getNode(), id);
            setAttribute("item", tomcatInfo);
        }
        return "node/tomcat/edit";
    }

    /**
     * 新增项目
     *
     * @param id tomcat id
     * @return 操作结果
     */
    @RequestMapping(value = "addProject.html", method = RequestMethod.GET, produces = MediaType.TEXT_HTML_VALUE)
    public String addProject(String id) {
        if (StrUtil.isNotEmpty(id)) {
            JSONObject tomcatInfo = tomcatService.getTomcatInfo(getNode(), id);
            setAttribute("item", tomcatInfo);
        }
        return "node/tomcat/addProject";
    }

    /**
     * tomcat项目管理
     *
     * @param id   tomcat id
     * @param path 项目路径
     * @return 项目管理面
     */
    @RequestMapping(value = "manage", method = RequestMethod.GET, produces = MediaType.TEXT_HTML_VALUE)
    public String manage(String id, String path) {
        setAttribute("id", id);
        setAttribute("project", path);
        return "node/tomcat/manage";
    }

    /**
     * 保存Tomcat信息
     *
     * @param id tomcat的id,如果id非空则更新，如果id是空则保存
     * @return 操作结果
     */
    @RequestMapping(value = "save", method = RequestMethod.POST, produces = MediaType.TEXT_HTML_VALUE)
    @ResponseBody
    @OperateType(UserOperateLogV1.OptType.Save_Tomcat)
    public String save(String id) {
        UserModel userName = getUser();
        NodeModel nodeModel = getNode();
        if (StrUtil.isEmpty(id)) {
            // 添加Tomcat信息
            if (!userName.isManage(nodeModel.getId())) {
                return JsonMessage.getString(400, "管理员才能添加Tomcat!");
            }
            return tomcatService.addTomcat(nodeModel, getRequest());
        } else {
            if (!userName.isTomcat(nodeModel.getId(), id)) {
                JsonMessage jsonMessage = new JsonMessage(300, "你没有改项目的权限");
                return jsonMessage.toString();
            }
            // 修改Tomcat信息
            return tomcatService.updateTomcat(nodeModel, getRequest());
        }
    }

    /**
     * 删除tomcat
     *
     * @return 操作结果
     */
    @RequestMapping(value = "delete", method = RequestMethod.POST, produces = MediaType.TEXT_HTML_VALUE)
    @ResponseBody
    @ProjectPermission(optType = UserOperateLogV1.OptType.Del_Tomcat)
    @UrlPermission(value = Role.System, optType = UserOperateLogV1.OptType.Del_Tomcat)
    public String delete() {
        return tomcatService.delete(getNode(), getRequest());
    }

    /**
     * 查询tomcat状态
     *
     * @return tomcat运行状态
     */
    @RequestMapping(value = "getTomcatStatus", method = RequestMethod.POST, produces = MediaType.TEXT_HTML_VALUE)
    @ResponseBody
    public String getStatus() {
        return tomcatService.getTomcatStatus(getNode(), getRequest());
    }

    /**
     * tomcat项目管理
     *
     * @return 操作结果
     */
    @RequestMapping(value = "tomcatProjectManage", method = RequestMethod.POST, produces = MediaType.TEXT_HTML_VALUE)
    @ResponseBody
    public String tomcatProjectManage() {
        return tomcatService.tomcatProjectManage(getNode(), getRequest());
    }

    /**
     * 启动tomcat
     *
     * @return 操作结果
     */
    @RequestMapping(value = "start", method = RequestMethod.POST, produces = MediaType.TEXT_HTML_VALUE)
    @ResponseBody
    @ProjectPermission(optType = UserOperateLogV1.OptType.Start_Tomcat)
    public String start() {
        return tomcatService.start(getNode(), getRequest());
    }

    /**
     * 重启tomcat
     *
     * @return 操作结果
     */
    @RequestMapping(value = "restart", method = RequestMethod.POST, produces = MediaType.TEXT_HTML_VALUE)
    @ResponseBody
    @ProjectPermission(optType = UserOperateLogV1.OptType.ReStart_Tomcat)
    public String restart() {
        return tomcatService.restart(getNode(), getRequest());
    }

    /**
     * 停止tomcat
     *
     * @return 操作结果
     */
    @RequestMapping(value = "stop", method = RequestMethod.POST, produces = MediaType.TEXT_HTML_VALUE)
    @ResponseBody
    @ProjectPermission(optType = UserOperateLogV1.OptType.Stop_Tomcat)
    public String stop() {
        return tomcatService.stop(getNode(), getRequest());
    }


    /**
     * 查询文件列表
     *
     * @return 文件列表
     */
    @RequestMapping(value = "getFileList", method = RequestMethod.POST, produces = MediaType.TEXT_HTML_VALUE)
    @ResponseBody
    public String getFileList() {
        return tomcatService.getFileList(getNode(), getRequest());
    }


    /**
     * 上传文件
     *
     * @return 操作结果
     */
    @RequestMapping(value = "upload", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
    @ResponseBody
    @ProjectPermission(checkUpload = true, optType = UserOperateLogV1.OptType.Upload_File_Tomcat)
    public String upload() {
        return NodeForward.requestMultipart(getNode(), getMultiRequest(), NodeUrl.Tomcat_File_Upload).toString();
    }

    /**
     * 上传War包
     *
     * @return 操作结果
     */
    @RequestMapping(value = "uploadWar", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
    @ResponseBody
    @ProjectPermission(checkUpload = true, optType = UserOperateLogV1.OptType.Upload_File_War_Tomcat)
    public String uploadWar() {
        return tomcatService.uploadWar(getNode(), getMultiRequest());
    }

    /**
     * 下载文件
     */
    @RequestMapping(value = "download", method = RequestMethod.GET)
    @ResponseBody
    @OperateType(UserOperateLogV1.OptType.Download_Tomcat)
    public void download() {
        tomcatService.download(getNode(), getRequest(), getResponse());
    }

    /**
     * 删除文件
     *
     * @return 操作结果
     */
    @RequestMapping(value = "deleteFile", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
    @ResponseBody
    @ProjectPermission(checkDelete = true, optType = UserOperateLogV1.OptType.Del_File_Tomcat)
    public String deleteFile() {
        return tomcatService.deleteFile(getNode(), getRequest());
    }
}