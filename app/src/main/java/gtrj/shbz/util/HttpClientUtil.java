package gtrj.shbz.util;

import android.util.Log;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.SocketTimeoutException;
import java.net.URL;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.CookieStore;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.cookie.Cookie;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;

/**
 * httpclient的网络请求方式
 * 已经弃用，改用okhttp
 */
public class HttpClientUtil {

    public static String getData(String method, Map<String, String> param) throws SessionOutOfTimeException {
        List<BasicNameValuePair> nvps = new ArrayList<>();
        for (Map.Entry<String, String> entry : param.entrySet()) {
            BasicNameValuePair nv = new BasicNameValuePair(entry.getKey(), entry.getValue());
            nvps.add(nv);
        }

        HttpPost httpPost = new HttpPost(ContextString.SERVER + method);

        try {
            httpPost.setEntity(new UrlEncodedFormEntity(nvps, "UTF-8"));
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }

        if (null != ContextString.SESSION) {
            httpPost.setHeader("Cookie", "JSESSIONID=" + ContextString.SESSION);
        }
        HttpClient httpClient = new DefaultHttpClient();
        HttpResponse response = null;
        try {
            response = httpClient.execute(httpPost);
        } catch (IOException e) {
            e.printStackTrace();
        }
        assert response != null;
        HttpEntity entity = response.getEntity();
        if (ContextString.SESSION == null) {
            CookieStore mCookieStore = ((DefaultHttpClient) httpClient).getCookieStore();
            List<Cookie> cookies = mCookieStore
                    .getCookies();
            for (int i = 0; i < cookies.size(); i++) {
                if ("JSESSIONID".equals(cookies.get(i)
                        .getName())) {
                    ContextString.SESSION = cookies.get(i)
                            .getValue();
                    break;
                }
            }
        }
        BufferedReader br = null;
        try {
            br = new BufferedReader(new InputStreamReader(entity.getContent()));
            String readLine;
            String result = "";
            while (((readLine = br.readLine()) != null)) {
                result += readLine;
            }
            if ("{\"IsLogin\":\"3\"}".equals(result)) {
                ContextString.SESSION = null;
                throw new SessionOutOfTimeException("session过期");
            }
            return result;
        } catch (Exception e) {
            ContextString.SESSION = null;
            throw new SessionOutOfTimeException("session过期");
        } finally {
            if (br != null)
                try {
                    br.close();
                } catch (Exception e) {
                    e.printStackTrace();
                }
        }
    }

    public static InputStream getImage(String method, Map<String, String> params) {
        HttpURLConnection conn;
        try {
            StringBuilder entity = new StringBuilder();
            if (params != null) {
                for (Map.Entry<String, String> entry : params.entrySet()) {
                    entity.append(entry.getKey())
                            .append('=')
                            .append(URLEncoder.encode(entry.getValue(), "UTF-8"))
                            .append('&');
                }
                entity.deleteCharAt(entity.length() - 1);
                byte[] entitydata = entity.toString().getBytes();
                conn = (HttpURLConnection) new URL(ContextString.SERVER + method).openConnection();
                conn.setRequestMethod("POST");
                conn.setUseCaches(false);
                conn.setDoOutput(true);
                conn.setDoInput(true);
                conn.setAllowUserInteraction(false);
                conn.setRequestProperty("Content-Type",
                        "application/x-www-form-urlencoded");
                conn.setRequestProperty("Content-Length",
                        String.valueOf(entitydata.length));
                conn.setRequestProperty("Connection", "close");
                if (null != ContextString.SESSION) {
                    conn.setRequestProperty("Cookie", "JSESSIONID="
                            + ContextString.SESSION);
                }
                OutputStream outStream = conn.getOutputStream();
                try {
                    outStream.write(entitydata);
                } catch (SocketTimeoutException e) {
                    e.printStackTrace();
                }
                if (conn.getResponseCode() != 200) {
                    return null;
                } else {
                    return conn.getInputStream();
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    public static String multipartRequest(String method, Map<String, String> parmas, String filepath, String filefield, String fileMimeType) throws SessionOutOfTimeException {
        Log.e("开始上传","alalalla");
        HttpURLConnection connection;
        DataOutputStream outputStream;
        InputStream inputStream;

        String twoHyphens = "--";
        String boundary = "*****" + Long.toString(System.currentTimeMillis()) + "*****";
        String lineEnd = "\r\n";

        String result;

        int bytesRead, bytesAvailable, bufferSize;
        byte[] buffer;
        int maxBufferSize = 1024 * 1024;

        String[] q = filepath.split("/");
        int idx = q.length - 1;

        try {
            File file = new File(filepath);
            FileInputStream fileInputStream = new FileInputStream(file);

            URL url = new URL(ContextString.SERVER + method);
            connection = (HttpURLConnection) url.openConnection();

            connection.setDoInput(true);
            connection.setDoOutput(true);
            connection.setUseCaches(false);

            connection.setRequestMethod("POST");
            connection.setRequestProperty("Connection", "Keep-Alive");
            connection.setRequestProperty("User-Agent", "Android Multipart HTTP Client 1.0");
            connection.setRequestProperty("Content-Type", "multipart/form-data; boundary=" + boundary);
            connection.setRequestProperty("Cookie", "JSESSIONID=" + ContextString.SESSION);

            outputStream = new DataOutputStream(connection.getOutputStream());
            outputStream.writeBytes(twoHyphens + boundary + lineEnd);
            outputStream.writeBytes("Content-Disposition: form-data; name=\"" + filefield + "\"; filename=\"" + q[idx] + "\"" + lineEnd);
            outputStream.writeBytes("Content-Type: " + fileMimeType + lineEnd);
            outputStream.writeBytes("Content-Transfer-Encoding: binary" + lineEnd);

            outputStream.writeBytes(lineEnd);

            bytesAvailable = fileInputStream.available();
            bufferSize = Math.min(bytesAvailable, maxBufferSize);
            buffer = new byte[bufferSize];

            bytesRead = fileInputStream.read(buffer, 0, bufferSize);
            while (bytesRead > 0) {
                outputStream.write(buffer, 0, bufferSize);
                bytesAvailable = fileInputStream.available();
                bufferSize = Math.min(bytesAvailable, maxBufferSize);
                bytesRead = fileInputStream.read(buffer, 0, bufferSize);
            }

            outputStream.writeBytes(lineEnd);

            // Upload POST Data
            for (String key : parmas.keySet()) {
                String value = parmas.get(key);

                outputStream.writeBytes(twoHyphens + boundary + lineEnd);
                outputStream.writeBytes("Content-Disposition: form-data; name=\"" + key + "\"" + lineEnd);
                outputStream.writeBytes("Content-Type: text/plain" + lineEnd);
                outputStream.writeBytes(lineEnd);
                outputStream.writeBytes(value);
                outputStream.writeBytes(lineEnd);
            }

            outputStream.writeBytes(twoHyphens + boundary + twoHyphens + lineEnd);

            inputStream = connection.getInputStream();

            result = convertStreamToString(inputStream);

            fileInputStream.close();
            inputStream.close();
            outputStream.flush();
            outputStream.close();
            if ("{\"IsLogin\":\"3\"}".equals(result)) {
                ContextString.SESSION = null;
                throw new SessionOutOfTimeException("session过期");
            }
            return result;
        } catch (Exception e) {
            e.printStackTrace();
            ContextString.SESSION = null;
            throw new SessionOutOfTimeException("session过期");
        }
    }


    private static String convertStreamToString(InputStream is) {
        BufferedReader reader = new BufferedReader(new InputStreamReader(is));
        StringBuilder sb = new StringBuilder();
        String line;
        try {
            while ((line = reader.readLine()) != null) {
                sb.append(line);
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                is.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return sb.toString();
    }

    public static class SessionOutOfTimeException extends Exception {
        public SessionOutOfTimeException(String e) {
            super(e);
        }
    }
}


