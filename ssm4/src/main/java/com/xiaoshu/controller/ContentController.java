package com.xiaoshu.controller;

import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.util.List;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.apache.log4j.Logger;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

import com.alibaba.fastjson.JSONObject;
import com.github.pagehelper.PageInfo;
import com.xiaoshu.config.util.ConfigUtil;
import com.xiaoshu.entity.Content;
import com.xiaoshu.entity.ContentVo;
import com.xiaoshu.entity.Operation;
import com.xiaoshu.entity.Role;
import com.xiaoshu.entity.User;
import com.xiaoshu.service.ContentService;
import com.xiaoshu.service.OperationService;
import com.xiaoshu.service.RoleService;
import com.xiaoshu.service.UserService;
import com.xiaoshu.util.StringUtil;
import com.xiaoshu.util.TimeUtil;
import com.xiaoshu.util.WriterUtil;

@Controller
@RequestMapping("content")
public class ContentController extends LogController{
	static Logger logger = Logger.getLogger(ContentController.class);

	@Autowired
	private UserService userService;
	
	@Autowired
	private RoleService roleService ;
	
	@Autowired
	private ContentService contentService;
	
	@Autowired
	private OperationService operationService;
	
	
	@RequestMapping("contentIndex")
	public String index(HttpServletRequest request,Integer menuid) throws Exception{
		List<Role> roleList = roleService.findRole(new Role());
		List<Operation> operationList = operationService.findOperationIdsByMenuid(menuid);
		request.setAttribute("operationList", operationList);
		request.setAttribute("lList", contentService.findCategory());
		return "content";
	}
	
	
	@RequestMapping(value="contentList",method=RequestMethod.POST)
	public void userList(ContentVo contentVo,HttpServletRequest request,HttpServletResponse response,String offset,String limit) throws Exception{
		try {
			
			Integer pageSize = StringUtil.isEmpty(limit)?ConfigUtil.getPageSize():Integer.parseInt(limit);
			Integer pageNum =  (Integer.parseInt(offset)/pageSize)+1;
			
			PageInfo<ContentVo> page = contentService.findPage(contentVo, pageNum, pageSize);
			
			
			JSONObject jsonObj = new JSONObject();
			jsonObj.put("total",page.getTotal() );
			jsonObj.put("rows", page.getList());
	        WriterUtil.write(response,jsonObj.toString());
		} catch (Exception e) {
			e.printStackTrace();
			logger.error("广告错误",e);
			throw e;
		}
	}
	
	
	// 新增或修改
	@RequestMapping("reserveUser")
	public void reserveUser(HttpServletRequest request,Content content,HttpServletResponse response){
		Integer contentid = content.getContentid();
		JSONObject result=new JSONObject();
		try {
			Content content2 = contentService.findByName(content.getContenttitle());
			
			if (contentid != null) {   // userId不为空 说明是修改
				if(content2==null || (content2 != null && content2.getContentid().equals(contentid))){
					
					contentService.updateContent(content);
					result.put("success", true);
				}else{
					result.put("success", true);
					result.put("errorMsg", "该广告名被使用");
				}
				
			}else {   // 添加
				if(content2==null){  // 没有重复可以添加
					contentService.addContent(content);
					result.put("success", true);
				} else {
					result.put("success", true);
					result.put("errorMsg", "该广告名被使用");
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
			logger.error("保存广告信息错误",e);
			result.put("success", true);
			result.put("errorMsg", "对不起，操作失败");
		}
		WriterUtil.write(response, result.toString());
	}
	
	
	@RequestMapping("deleteUser")
	public void delUser(HttpServletRequest request,HttpServletResponse response){
		JSONObject result=new JSONObject();
		try {
			String[] ids=request.getParameter("ids").split(",");
			for (String contentid : ids) {
				contentService.deleteContent(Integer.parseInt(contentid));
			}
			result.put("success", true);
			result.put("delNums", ids.length);
		} catch (Exception e) {
			e.printStackTrace();
			logger.error("删除用户信息错误",e);
			result.put("errorMsg", "对不起，删除失败");
		}
		WriterUtil.write(response, result.toString());
	}
	
	@RequestMapping("countContent")
	public void countContent(HttpServletRequest request,HttpServletResponse response){
		JSONObject result=new JSONObject();
		try {
			
			List<ContentVo> data = contentService.countContent();
			
			
			
			result.put("success", true);
			result.put("data", data);
		} catch (Exception e) {
			e.printStackTrace();
			logger.error("统计用户信息错误",e);
			result.put("errorMsg", "对不起，统计失败");
		}
		WriterUtil.write(response, result.toString());
	}
	@RequestMapping("exportContent")
	public void exportContent(ContentVo contentVo ,HttpServletRequest request,HttpServletResponse response){
		JSONObject result=new JSONObject();
		try {
			//导出
			List<ContentVo> list = contentService.findList(contentVo);
			//查询到的数据通过POI提供的 API把数据写入
			Workbook w = new HSSFWorkbook();
			//创建工作表对象
			Sheet sheet = w.createSheet();
			//创建行对象,存表头信息
			Row row1 = sheet.createRow(0);
			//第一行中创建单元格
			String[] bt = {"用户编号","标题","广告分类名称","广告链接","费用","广告状态","创建时间"};
			for (int i = 0; i < bt.length; i++) {
				Cell cell = row1.createCell(i);
				//向单元格写入数据
				cell.setCellValue(bt[i]);
			}
			//写入数据
			for (int i = 0; i < list.size(); i++) {
			//创建行对象
				Row row = sheet.createRow(i+1);//i+1从第二行开始写入数据
				
				ContentVo vo = list.get(i);
				
				//向单元格写入数据
				row.createCell(0).setCellValue(vo.getContentid());;
				row.createCell(1).setCellValue(vo.getContenttitle());;
				row.createCell(2).setCellValue(vo.getLname());;
				row.createCell(3).setCellValue(vo.getContenturl());;
				row.createCell(4).setCellValue(vo.getPrice());;
				row.createCell(5).setCellValue(vo.getStatus());;
				row.createCell(6).setCellValue(TimeUtil.formatTime(vo.getCreatetime(), "yyyy-MM-dd"));
			}
			//把w工作簿写入到磁盘
			OutputStream os;
			File file = new File("E:\\excelwendang\\1.xls");
			
			if (!file.exists()){//若此目录不存在，则创建之  
				file.createNewFile();  
				logger.debug("创建文件夹路径为："+ file.getPath());  
            } 
			os = new FileOutputStream(file);
			w.write(os);
			os.close( );

			
			
			result.put("success", true);
		} catch (Exception e) {
			e.printStackTrace();
			logger.error("导出用户信息错误",e);
			result.put("errorMsg", "对不起，导出失败");
		}
		WriterUtil.write(response, result.toString());
	}
	
	@RequestMapping("editPassword")
	public void editPassword(HttpServletRequest request,HttpServletResponse response){
		JSONObject result=new JSONObject();
		String oldpassword = request.getParameter("oldpassword");
		String newpassword = request.getParameter("newpassword");
		HttpSession session = request.getSession();
		User currentUser = (User) session.getAttribute("currentUser");
		if(currentUser.getPassword().equals(oldpassword)){
			User user = new User();
			user.setUserid(currentUser.getUserid());
			user.setPassword(newpassword);
			try {
				userService.updateUser(user);
				currentUser.setPassword(newpassword);
				session.removeAttribute("currentUser"); 
				session.setAttribute("currentUser", currentUser);
				result.put("success", true);
			} catch (Exception e) {
				e.printStackTrace();
				logger.error("修改密码错误",e);
				result.put("errorMsg", "对不起，修改密码失败");
			}
		}else{
			logger.error(currentUser.getUsername()+"修改密码时原密码输入错误！");
			result.put("errorMsg", "对不起，原密码输入错误！");
		}
		WriterUtil.write(response, result.toString());
	}
}
