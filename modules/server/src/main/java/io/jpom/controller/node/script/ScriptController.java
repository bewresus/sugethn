package io.jpom.controller.node.script;

import cn.jiangzeyin.common.JsonMessage;
import io.jpom.common.BaseServerController;
import io.jpom.common.forward.NodeForward;
import io.jpom.common.forward.NodeUrl;
import io.jpom.model.PageResultDto;
import io.jpom.model.data.NodeModel;
import io.jpom.model.data.ScriptModel;
import io.jpom.permission.SystemPermission;
import io.jpom.plugin.ClassFeature;
import io.jpom.plugin.Feature;
import io.jpom.plugin.MethodFeature;
import io.jpom.service.node.script.ScriptServer;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

/**
 * 脚本管理
 *
 * @author jiangzeyin
 * @date 2019/4/24
 */
@RestController
@RequestMapping(value = "/node/script")
@Feature(cls = ClassFeature.NODE_SCRIPT)
@SystemPermission
public class ScriptController extends BaseServerController {

	private final ScriptServer scriptServer;

	public ScriptController(ScriptServer scriptServer) {
		this.scriptServer = scriptServer;
	}

	/**
	 * get script list
	 *
	 * @return json
	 * @author Hotstrip
	 */
	@RequestMapping(value = "list", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_VALUE)
	public String scriptList() {
		PageResultDto<ScriptModel> pageResultDto = scriptServer.listPageNode(getRequest());
		return JsonMessage.getString(200, "success", pageResultDto);
	}


	@GetMapping(value = "item.json", produces = MediaType.APPLICATION_JSON_VALUE)
	@Feature(method = MethodFeature.LIST)
	public String item() {
		return NodeForward.request(getNode(), getRequest(), NodeUrl.Script_Item).toString();
	}

	/**
	 * 保存脚本
	 *
	 * @return json
	 */
	@RequestMapping(value = "save.json", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_VALUE)
	@Feature(method = MethodFeature.EDIT)
	public String save() {
		NodeModel node = getNode();
		JsonMessage<Object> request = NodeForward.request(node, getRequest(), NodeUrl.Script_Save);
		if (request.getCode() == HttpStatus.OK.value()) {
			scriptServer.syncNode(node);
		}
		return request.toString();
	}

	@RequestMapping(value = "del.json", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_VALUE)
	@Feature(method = MethodFeature.DEL)
	public String del() {
		NodeModel node = getNode();
		JsonMessage<Object> request = NodeForward.request(node, getRequest(), NodeUrl.Script_Del);
		if (request.getCode() == HttpStatus.OK.value()) {
			scriptServer.syncNode(node);
		}
		return request.toString();
	}

	/**
	 * 导入脚本
	 *
	 * @return json
	 */
	@RequestMapping(value = "upload", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_VALUE)
	@Feature(method = MethodFeature.UPLOAD)
	public String upload() {
		NodeModel node = getNode();
		JsonMessage<String> stringJsonMessage = NodeForward.requestMultipart(node, getMultiRequest(), NodeUrl.Script_Upload);
		if (stringJsonMessage.getCode() == HttpStatus.OK.value()) {
			scriptServer.syncNode(node);
		}
		return stringJsonMessage.toString();
	}

	/**
	 * 同步脚本模版
	 *
	 * @return json
	 */
	@GetMapping(value = "sync", produces = MediaType.APPLICATION_JSON_VALUE)
	public String syncProject() {
		//
		NodeModel node = getNode();
		int cache = scriptServer.delCache(node.getId(), getRequest());
		String msg = scriptServer.syncExecuteNode(node);
		return JsonMessage.getString(200, msg);
	}

}
