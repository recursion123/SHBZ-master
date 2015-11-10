package gtrj.shbz.util;


import com.squareup.okhttp.FormEncodingBuilder;
import com.squareup.okhttp.MediaType;
import com.squareup.okhttp.MultipartBuilder;
import com.squareup.okhttp.OkHttpClient;
import com.squareup.okhttp.Request;
import com.squareup.okhttp.RequestBody;
import com.squareup.okhttp.Response;

import java.io.File;
import java.io.IOException;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * Created by zhang77555 on 2015/7/28.
 * 网络请求util
 */
public class OkHttpUtil {
    public static OkHttpClient client;

    static {
        if (client == null) {
            client = new OkHttpClient();
            client.setConnectTimeout(10, TimeUnit.SECONDS);
            client.setWriteTimeout(10, TimeUnit.SECONDS);
            client.setReadTimeout(30, TimeUnit.SECONDS);
        }
    }


    /**
     * post请求
     * @param method 调用的方法名
     * @param params 传递的参数
     * @return
     * @throws IOException
     * @throws SessionOutOfTimeException 如果返回islogin=3，则抛出session超时异常
     */
    public static String Post(String method, Map<String, String> params) throws IOException, SessionOutOfTimeException {
        FormEncodingBuilder formEncodingBuilder = new FormEncodingBuilder();
        for (Map.Entry<String, String> entry : params.entrySet()) {
            formEncodingBuilder.add(entry.getKey(), entry.getValue());
        }
        RequestBody formBody = formEncodingBuilder.build();
        Request request = new Request.Builder()
                .url(ContextString.SERVER + method).header("Cookie", "JSESSIONID=" + ContextString.SESSION)
                .post(formBody)
                .build();

        Response response = client.newCall(request).execute();
        if (!response.isSuccessful()) throw new IOException("Unexpected code " + response);
        if (ContextString.SESSION == null)
            ContextString.SESSION = response.headers().toMultimap().get("Set-Cookie").get(0).split("=")[1];
        String result = response.body().string();
        if ("{\"IsLogin\":\"3\"}".equals(result)) {
            throw new SessionOutOfTimeException("session过期");
        }
        return result;
    }


    /**
     * 上传文件请求
     * @param method 调用的方法名字
     * @param parmas 传递的参数
     * @param filepath 要上传的文件的路径
     * @param filefield 要上传的文件的参数名称
     * @param fileMimeType 上传文件的类型
     * @return
     * @throws SessionOutOfTimeException 如果返回islogin=3，则抛出session超时异常
     * @throws IOException
     */
    public static String uploadFile(String method, Map<String, String> parmas, String filepath, String filefield, String fileMimeType) throws SessionOutOfTimeException, IOException {
        MediaType type = MediaType.parse(fileMimeType);
        MultipartBuilder multipartBuilder = new MultipartBuilder();
        multipartBuilder.type(MultipartBuilder.FORM);
        for (Map.Entry<String, String> entry : parmas.entrySet()) {
            multipartBuilder.addFormDataPart(entry.getKey(), entry.getValue());
        }
        multipartBuilder.addFormDataPart(filefield, "tempFile", RequestBody.create(type, new File(filepath)));
        RequestBody requestBody = multipartBuilder.build();
        Request request = new Request.Builder()
                .url(ContextString.SERVER + method).header("Cookie", "JSESSIONID=" + ContextString.SESSION)
                .post(requestBody)
                .build();
        Response response = client.newCall(request).execute();
        if (!response.isSuccessful()) throw new IOException("Unexpected code " + response);
        if (ContextString.SESSION == null)
            ContextString.SESSION = response.headers().toMultimap().get("Set-Cookie").get(0).split("=")[1];
        String result = response.body().string();
        if ("{\"IsLogin\":\"3\"}".equals(result)) {
            throw new SessionOutOfTimeException("session过期");
        }

        return result;
    }


    public static class SessionOutOfTimeException extends Exception {
        public SessionOutOfTimeException(String e) {
            super(e);
            ContextString.SESSION = null;
        }
    }
}
