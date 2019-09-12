package cn.keking.web.controller;

import cn.keking.model.FileAttribute;
import cn.keking.model.ResponseJson;
import cn.keking.service.FilePreview;
import cn.keking.service.FilePreviewFactory;
import cn.keking.service.cache.CacheService;
import cn.keking.utils.FileUtils;
import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.ui.Model;
import org.springframework.validation.support.BindingAwareModelMap;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.*;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author yudian-it
 */
@RestController
@RequestMapping("/rest")
public class OnlinePreviewRestController {

    private static final Logger LOGGER = LoggerFactory.getLogger(OnlinePreviewRestController.class);

    @Autowired
    FilePreviewFactory previewFactory;

    @Autowired
    CacheService cacheService;

    @Autowired
    private FileUtils fileUtils;

    /**
     * @param url
     * @param model
     * @return
     */
    @RequestMapping(value = "/onlinePreview", method = RequestMethod.GET)
    public ResponseJson onlinePreview(String url, Model model, HttpServletRequest req) {
        return buildResponseJson(url, model, req);
    }

    @RequestMapping(value = "/word/onlinePreview", method = RequestMethod.GET)
    public ResponseJson wordOnlinePreview(String url, Model model, HttpServletRequest req) {
        return buildResponseJson(url, model, req);
    }
    @RequestMapping(value = "/pdf/onlinePreview", method = RequestMethod.GET)
    public ResponseJson pdfOnlinePreview(String url, Model model, HttpServletRequest req) {
        return buildResponseJson(url, model, req);
    }

    private ResponseJson buildResponseJson(String url, Model model, HttpServletRequest req) {
        FileAttribute fileAttribute = fileUtils.getFileAttribute(url);
        req.setAttribute("fileKey", req.getParameter("fileKey"));
        model.addAttribute("officePreviewType", req.getParameter("officePreviewType"));
        model.addAttribute("originUrl", req.getRequestURL().toString());
        FilePreview filePreview = previewFactory.get(fileAttribute);
        String preview = filePreview.filePreviewHandle(url, model, fileAttribute);
        Map<String, Object> result = new HashMap<>();
        ((BindingAwareModelMap) model).entrySet().forEach(x -> result.put(x.getKey(), x.getValue()));
        if (result.isEmpty()) {
            return new ResponseJson(false, "服务器错误");
        }
        return new ResponseJson(true, result);
    }


    /**
     * 多图片切换预览
     *
     * @param model
     * @param req
     * @return
     * @throws UnsupportedEncodingException
     */
    @RequestMapping(value = "/picturesPreview", method = RequestMethod.GET)
    public Map<String, Object> picturesPreview(String urls, String currentUrl, Model model, HttpServletRequest req) throws UnsupportedEncodingException {
        // 路径转码
        String decodedUrl = URLDecoder.decode(urls, "utf-8");
        String decodedCurrentUrl = URLDecoder.decode(currentUrl, "utf-8");
        // 抽取文件并返回文件列表
        String[] imgs = decodedUrl.split("\\|");
        List imgurls = Arrays.asList(imgs);
        model.addAttribute("imgurls", imgurls);
        model.addAttribute("currentUrl",decodedCurrentUrl);

        Map<String, Object> result = new HashMap<>();
        ((BindingAwareModelMap) model).entrySet().forEach(x->result.put(x.getKey(),x.getValue()));

        return result;
    }

    @RequestMapping(value = "/picturesPreview", method = RequestMethod.POST)
    public Map<String, Object> picturesPreview(Model model, HttpServletRequest req) throws UnsupportedEncodingException {
        String urls = req.getParameter("urls");
        String currentUrl = req.getParameter("currentUrl");
        // 路径转码
        String decodedUrl = URLDecoder.decode(urls, "utf-8");
        String decodedCurrentUrl = URLDecoder.decode(currentUrl, "utf-8");
        // 抽取文件并返回文件列表
        String[] imgs = decodedUrl.split("\\|");
        List imgurls = Arrays.asList(imgs);
        model.addAttribute("imgurls", imgurls);
        model.addAttribute("currentUrl",decodedCurrentUrl);


        Map<String, Object> result = new HashMap<>();
        ((BindingAwareModelMap) model).entrySet().forEach(x->result.put(x.getKey(),x.getValue()));

        return result;
    }
    /**
     * 根据url获取文件内容
     * 当pdfjs读取存在跨域问题的文件时将通过此接口读取
     *
     * @param urlPath
     * @param resp
     */
    @RequestMapping(value = "/getCorsFile", method = RequestMethod.GET)
    public void getCorsFile(String urlPath, HttpServletResponse resp) {
        InputStream inputStream = null;
        try {
            String strUrl = urlPath.trim();
            URL url = new URL(new URI(strUrl).toASCIIString());
            //打开请求连接
            URLConnection connection = url.openConnection();
            HttpURLConnection httpURLConnection = (HttpURLConnection) connection;
            httpURLConnection.setRequestProperty("User-Agent", "Mozilla/4.0 (compatible; MSIE 5.0; Windows NT; DigExt)");
            inputStream = httpURLConnection.getInputStream();
            byte[] bs = new byte[1024];
            int len;
            while (-1 != (len = inputStream.read(bs))) {
                resp.getOutputStream().write(bs, 0, len);
            }
        } catch (IOException | URISyntaxException e) {
            LOGGER.error("下载pdf文件失败", e);
        } finally {
            if (inputStream != null) {
                IOUtils.closeQuietly(inputStream);
            }
        }
    }

    /**
     * 通过api接口入队
     * @param url 请编码后在入队
     */
    @GetMapping("/addTask")
    @ResponseBody
    public String addQueueTask(String url) {
        cacheService.addQueueTask(url);
        return "success";
    }

}
