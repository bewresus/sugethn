package io.jpom.controller;

import cn.jiangzeyin.common.DefaultSystemLog;
import cn.jiangzeyin.common.JsonMessage;
import org.springframework.boot.autoconfigure.web.servlet.error.AbstractErrorController;
import org.springframework.boot.web.servlet.error.ErrorAttributes;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

import javax.servlet.RequestDispatcher;
import javax.servlet.http.HttpServletRequest;
import java.util.HashMap;
import java.util.Map;

/**
 * @author bwcx_jzy
 * @date 2021/3/17
 * @see org.springframework.boot.autoconfigure.web.servlet.error.BasicErrorController
 */
@Controller
@RequestMapping("${server.error.path:${error.path:/error}}")
public class MyErrorController extends AbstractErrorController {

	public MyErrorController(ErrorAttributes errorAttributes) {
		super(errorAttributes);
	}

	@RequestMapping
	public ResponseEntity<Map<String, Object>> error(HttpServletRequest request) {
		HttpStatus status = getStatus(request);
		if (status == HttpStatus.NO_CONTENT) {
			return new ResponseEntity<>(status);
		}
		Integer statusCode = (Integer) request.getAttribute(RequestDispatcher.ERROR_STATUS_CODE);
		String requestUri = (String) request.getAttribute(RequestDispatcher.ERROR_REQUEST_URI);
		DefaultSystemLog.getLog().error("发生异常：" + statusCode + "  " + requestUri);
		// 判断异常信息
		Object attribute = request.getAttribute(RequestDispatcher.ERROR_EXCEPTION);
		Map<String, Object> body = new HashMap<>(5);
		body.put(JsonMessage.CODE, HttpStatus.INTERNAL_SERVER_ERROR.value());
		String msg = "啊哦，好像哪里出错了，请稍候再试试吧~";
		if (attribute instanceof MaxUploadSizeExceededException) {
			// 上传文件大小异常
			msg = "上传文件太大了,请重新选择一个较小的文件上传吧";
		} else if (status == HttpStatus.NOT_FOUND) {
			msg = "没有找到对应的资源";
			body.put(JsonMessage.DATA, requestUri);
		}
		body.put(JsonMessage.MSG, msg);

		return new ResponseEntity<>(body, HttpStatus.OK);
	}

	@Override
	public String getErrorPath() {
		return null;
	}
}
